package com.pingo.feature.review.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.detalhe.EdicaoDoLancamento
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.model.Lancamento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    private val confirmarPendencias: ConfirmarPendencias,
    private val editarLancamento: EditarLancamento,
    private val marcarTransferencia: MarcarTransferencia,
    private val clock: Clock,
) : ViewModel() {
    private val base = MutableStateFlow(BaseDaRevisao())
    private val controle = MutableStateFlow(ControleDaRevisao())
    private val mutableUiState = MutableStateFlow(ReviewUiState())

    val uiState: StateFlow<ReviewUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                contaRepository.observeContas(),
                instrumentoRepository.observeInstrumentos(),
                categoriaRepository.observeCategorias(),
            ) { lancamentos, contas, instrumentos, categorias ->
                BaseDaRevisao(
                    carregado = true,
                    lancamentos = lancamentos,
                    contas = contas,
                    instrumentos = instrumentos,
                    categorias = categorias,
                )
            }.collect { baseAtual -> base.value = baseAtual }
        }
        viewModelScope.launch {
            combine(base, controle) { baseAtual, controleAtual ->
                montarRevisao(baseAtual, controleAtual, clock.instant(), clock.zone)
            }.collect { estado -> mutableUiState.value = estado }
        }
    }

    fun onEvent(evento: ReviewEvent) {
        when (evento) {
            ReviewEvent.ConfirmarTodos -> confirmarTodos()
            ReviewEvent.AlternarAplicarATodos -> alternarAplicarATodos()
            ReviewEvent.AplicarCategoria -> aplicarCategoria()
            ReviewEvent.FecharTrocaDeCategoria -> fecharTrocaDeCategoria()
            ReviewEvent.FecharMarcarTransferencia -> controle.update { it.copy(painelDeTransferencia = null) }
            ReviewEvent.DispensarMensagem -> controle.update { it.copy(mensagem = null) }
            is ReviewEvent.Confirmar -> confirmar(evento.publicId)
            is ReviewEvent.Descartar -> descartar(evento.publicId)
            is ReviewEvent.AbrirTrocaDeCategoria -> abrirTrocaDeCategoria(evento.publicId)
            is ReviewEvent.SelecionarCategoria -> selecionarCategoria(evento.publicId)
            is ReviewEvent.AbrirMarcarTransferencia -> abrirMarcarTransferencia(evento.publicId)
            is ReviewEvent.SelecionarDestino -> selecionarDestino(evento.instrumentoPublicId)
        }
    }

    private fun confirmar(publicId: String) {
        if (controle.value.ocupado) {
            return
        }

        viewModelScope.launch {
            controle.update { it.copy(ocupado = true, mensagem = null) }
            val resultado = confirmarPendencias.confirmar(listOf(publicId))
            controle.update { it.copy(ocupado = false, mensagem = mensagemDe(resultado)) }
        }
    }

    private fun confirmarTodos() {
        if (controle.value.ocupado) {
            return
        }

        viewModelScope.launch {
            controle.update { it.copy(ocupado = true, mensagem = null) }
            val resultado = confirmarPendencias.confirmarTodos()
            controle.update { it.copy(ocupado = false, mensagem = mensagemDe(resultado)) }
        }
    }

    private fun descartar(publicId: String) {
        if (controle.value.ocupado) {
            return
        }

        viewModelScope.launch {
            controle.update { it.copy(ocupado = true, mensagem = null) }
            lancamentoRepository.excluir(publicId, clock.instant())
            controle.update { it.copy(ocupado = false) }
        }
    }

    private fun abrirTrocaDeCategoria(publicId: String) {
        val lancamento = lancamentoAtual(publicId) ?: return
        val opcoes = categoriasDoTipo(base.value.categorias, lancamento.tipo)
        val selecionada = lancamento.categoriaPublicId?.takeIf { atual -> opcoes.any { it.publicId == atual } }

        controle.update {
            it.copy(
                painelDeCategoria = TrocaDeCategoriaDaRevisao(
                    lancamentoPublicId = lancamento.publicId,
                    estabelecimento = lancamento.estabelecimento.orEmpty(),
                    valorCentavos = lancamento.valorCentavos,
                    opcoes = opcoes,
                    selecionadaPublicId = selecionada,
                ),
                mensagem = null,
            )
        }
    }

    private fun selecionarCategoria(categoriaPublicId: String) {
        controle.update { atual ->
            val painel = atual.painelDeCategoria ?: return@update atual

            atual.copy(painelDeCategoria = painel.copy(selecionadaPublicId = categoriaPublicId))
        }
    }

    private fun alternarAplicarATodos() {
        controle.update { atual ->
            val painel = atual.painelDeCategoria ?: return@update atual

            atual.copy(painelDeCategoria = painel.copy(aplicarATodos = !painel.aplicarATodos))
        }
    }

    private fun aplicarCategoria() {
        val painel = controle.value.painelDeCategoria
        val lancamento = painel?.let { lancamentoAtual(it.lancamentoPublicId) }
        val categoriaPublicId = painel?.selecionadaPublicId
        val podeAplicar = painel != null && lancamento != null && categoriaPublicId != null && !controle.value.ocupado

        if (podeAplicar) {
            viewModelScope.launch {
                controle.update { it.copy(ocupado = true, mensagem = null) }
                val resultado = editarLancamento.editar(
                    EdicaoDoLancamento(
                        publicId = lancamento.publicId,
                        tipo = lancamento.tipo,
                        valorCentavos = lancamento.valorCentavos,
                        instrumentoPublicId = lancamento.instrumentoPublicId,
                        categoriaPublicId = categoriaPublicId,
                        dataHora = lancamento.dataHora,
                        estabelecimento = lancamento.estabelecimento,
                        aplicarCategoriaATodos = painel.aplicarATodos,
                    ),
                )
                controle.update { it.copy(ocupado = false, painelDeCategoria = null, mensagem = mensagemDe(resultado)) }
            }
        }
    }

    private fun fecharTrocaDeCategoria() {
        controle.update { it.copy(painelDeCategoria = null) }
    }

    private fun abrirMarcarTransferencia(publicId: String) {
        val lancamento = lancamentoAtual(publicId) ?: return

        controle.update {
            it.copy(
                painelDeTransferencia = MarcarTransferenciaDaRevisao(
                    lancamentoPublicId = lancamento.publicId,
                    estabelecimento = lancamento.estabelecimento.orEmpty(),
                    valorCentavos = lancamento.valorCentavos,
                    opcoes = instrumentosParaTransferencia(base.value, lancamento),
                ),
                mensagem = null,
            )
        }
    }

    private fun selecionarDestino(instrumentoPublicId: String) {
        val painel = controle.value.painelDeTransferencia ?: return
        if (controle.value.ocupado) {
            return
        }

        viewModelScope.launch {
            controle.update { it.copy(ocupado = true, mensagem = null) }
            val resultado = marcarTransferencia.marcar(painel.lancamentoPublicId, instrumentoPublicId)
            controle.update { it.copy(ocupado = false, painelDeTransferencia = null, mensagem = mensagemDe(resultado)) }
        }
    }

    private fun lancamentoAtual(publicId: String): Lancamento? = base.value.lancamentos
        .firstOrNull { it.publicId == publicId && it.excluidoEm == null }
}
