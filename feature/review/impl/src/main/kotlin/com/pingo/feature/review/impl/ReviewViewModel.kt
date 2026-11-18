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
import com.pingo.core.data.voucher.ResolverRecargaVoucherUseCase
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.voucher.CandidatosRevisaoVoucher
import com.pingo.core.model.voucher.ResultadoResolucaoRecarga
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ReviewViewModel private constructor(
    private val lancamentoRepository: LancamentoRepository,
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    private val confirmarPendencias: ConfirmarPendencias,
    private val editarLancamento: EditarLancamento,
    private val marcarTransferencia: MarcarTransferencia,
    private val clock: Clock,
    private val resolverRecargaVoucher: ResolverRecargaVoucherUseCase?,
) : ViewModel() {
    @Inject
    constructor(
        lancamentoRepository: LancamentoRepository,
        contaRepository: ContaRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        confirmarPendencias: ConfirmarPendencias,
        editarLancamento: EditarLancamento,
        marcarTransferencia: MarcarTransferencia,
        resolverRecargaVoucher: ResolverRecargaVoucherUseCase,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        contaRepository = contaRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        confirmarPendencias = confirmarPendencias,
        editarLancamento = editarLancamento,
        marcarTransferencia = marcarTransferencia,
        clock = clock,
        resolverRecargaVoucher = resolverRecargaVoucher as ResolverRecargaVoucherUseCase?,
    )

    constructor(
        lancamentoRepository: LancamentoRepository,
        contaRepository: ContaRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        confirmarPendencias: ConfirmarPendencias,
        editarLancamento: EditarLancamento,
        marcarTransferencia: MarcarTransferencia,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        contaRepository = contaRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        confirmarPendencias = confirmarPendencias,
        editarLancamento = editarLancamento,
        marcarTransferencia = marcarTransferencia,
        clock = clock,
        resolverRecargaVoucher = null,
    )

    private val base = MutableStateFlow(BaseDaRevisao())
    private val controle = MutableStateFlow(ControleDaRevisao())
    private val candidatosVoucher = MutableStateFlow<Map<String, CandidatosRevisaoVoucher>>(emptyMap())
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
        if (resolverRecargaVoucher != null) {
            viewModelScope.launch {
                base
                    .map { baseAtual ->
                        val pendentesVoucher = baseAtual.lancamentos
                            .filter {
                                it.status == StatusLancamento.PENDENTE &&
                                    it.excluidoEm == null &&
                                    ehPendenciaVoucher(it.motivoPendencia)
                            }
                            .map { it.publicId to it.motivoPendencia }
                        pendentesVoucher to baseAtual.instrumentos
                    }
                    .distinctUntilChanged()
                    .collectLatest { (pendentesVoucher, _) ->
                        val carregados = buildMap {
                            pendentesVoucher.forEach { (publicId, _) ->
                                resolverRecargaVoucher.listarCandidatos(publicId)?.let { put(publicId, it) }
                            }
                        }
                        candidatosVoucher.value = carregados
                    }
            }
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
            else -> onVoucherEvent(evento)
        }
    }

    private fun onVoucherEvent(evento: ReviewEvent) {
        when (evento) {
            ReviewEvent.ConfirmarResolucaoVoucher -> confirmarResolucaoVoucher()
            ReviewEvent.FecharResolucaoVoucher -> controle.update { it.copy(painelDeVoucher = null) }
            is ReviewEvent.AbrirResolucaoVoucher -> abrirResolucaoVoucher(evento.publicId)
            is ReviewEvent.SelecionarVoucher -> selecionarVoucher(evento.instrumentoPublicId)
            is ReviewEvent.SelecionarDecisaoVoucher -> selecionarDecisaoVoucher(evento.decisao)
            is ReviewEvent.SelecionarOcorrenciaVoucher -> selecionarOcorrenciaVoucher(evento.ocorrenciaPublicId)
            else -> Unit
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
            val mensagem = if (resultado.mantidos > 0) {
                val restantes = lancamentoRepository.observeLancamentos().first()
                    .filter { it.status == StatusLancamento.PENDENTE && it.excluidoEm == null }
                val vouchersRestantes = restantes.count { ehPendenciaVoucher(it.motivoPendencia) }
                if (vouchersRestantes > 0) {
                    MensagemDaRevisao.VoucherPendenteManual(
                        restantes = resultado.mantidos,
                        apenasVouchers = vouchersRestantes == restantes.size,
                    )
                } else {
                    mensagemDe(resultado)
                }
            } else {
                null
            }
            controle.update { it.copy(ocupado = false, mensagem = mensagem) }
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

    private fun abrirResolucaoVoucher(publicId: String) {
        val useCase = resolverRecargaVoucher ?: return
        viewModelScope.launch {
            val candidatos = useCase.listarCandidatos(publicId) ?: candidatosVoucher.value[publicId] ?: return@launch
            candidatosVoucher.update { it + (publicId to candidatos) }
            val painel = montarPainelDeVoucher(candidatos, base.value.contas, clock.zone)
            controle.update { it.copy(painelDeVoucher = painel, mensagem = null) }
        }
    }

    private fun selecionarVoucher(instrumentoPublicId: String) {
        controle.update { atual ->
            val painel = atual.painelDeVoucher ?: return@update atual
            val lancamento = lancamentoAtual(painel.lancamentoPublicId)
            val dataRef = (lancamento?.dataHora ?: clock.instant()).atZone(clock.zone).toLocalDate()
            val ocorrenciasDoVoucher = painel.ocorrenciasCandidatas.filter { it.instrumentoPublicId == instrumentoPublicId }
            val ocorrenciaPadrao = ocorrenciaMaisProxima(ocorrenciasDoVoucher, dataRef)
            atual.copy(
                painelDeVoucher = painel.copy(
                    voucherSelecionadoPublicId = instrumentoPublicId,
                    ocorrenciaSelecionadaPublicId = ocorrenciaPadrao,
                    decisaoSelecionada = null,
                ),
            )
        }
    }

    private fun selecionarDecisaoVoucher(decisao: TipoDecisaoVoucher) {
        controle.update { atual ->
            val painel = atual.painelDeVoucher ?: return@update atual
            val lancamento = lancamentoAtual(painel.lancamentoPublicId)
            val dataRef = (lancamento?.dataHora ?: clock.instant()).atZone(clock.zone).toLocalDate()
            val ocorrenciaId = if (decisao == TipoDecisaoVoucher.SUBSTITUIR_PREVISAO) {
                painel.ocorrenciaSelecionadaPublicId ?: ocorrenciaMaisProxima(painel.ocorrenciasDoVoucherSelecionado, dataRef)
            } else {
                painel.ocorrenciaSelecionadaPublicId
            }
            atual.copy(
                painelDeVoucher = painel.copy(
                    decisaoSelecionada = decisao,
                    ocorrenciaSelecionadaPublicId = ocorrenciaId,
                ),
            )
        }
    }

    private fun selecionarOcorrenciaVoucher(ocorrenciaPublicId: String) {
        controle.update { atual ->
            val painel = atual.painelDeVoucher ?: return@update atual
            atual.copy(
                painelDeVoucher = painel.copy(
                    ocorrenciaSelecionadaPublicId = ocorrenciaPublicId,
                    decisaoSelecionada = painel.decisaoSelecionada ?: TipoDecisaoVoucher.SUBSTITUIR_PREVISAO,
                ),
            )
        }
    }

    private fun confirmarResolucaoVoucher() {
        val useCase = resolverRecargaVoucher
        val painel = controle.value.painelDeVoucher
        val instrumentoPublicId = painel?.voucherSelecionadoPublicId
        val decisao = painel?.decisaoParaResolver()
        val podeResolver = useCase != null &&
            painel != null &&
            instrumentoPublicId != null &&
            decisao != null &&
            !controle.value.ocupado
        if (!podeResolver) {
            return
        }

        viewModelScope.launch {
            controle.update { it.copy(ocupado = true, mensagem = null) }
            val mensagem = when (useCase.resolver(painel.lancamentoPublicId, instrumentoPublicId, decisao)) {
                is ResultadoResolucaoRecarga.Resolvido -> null
                ResultadoResolucaoRecarga.NaoEncontrado, ResultadoResolucaoRecarga.Invalido -> MensagemDaRevisao.FalhaAoSalvar
            }
            controle.update {
                it.copy(
                    ocupado = false,
                    painelDeVoucher = if (mensagem == null) null else it.painelDeVoucher,
                    mensagem = mensagem,
                )
            }
        }
    }

    private fun lancamentoAtual(publicId: String): Lancamento? = base.value.lancamentos
        .firstOrNull { it.publicId == publicId && it.excluidoEm == null }
}
