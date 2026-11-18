package com.pingo.feature.accounts.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.fatura.CompletarDatasDoCartaoUseCase
import com.pingo.core.data.fatura.ResultadoCompletarDatasCartao
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.feature.accounts.api.AccountsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class FaturasViewModel internal constructor(
    private val instrumentoPublicId: String,
    private val contasFinanceiroProjection: ContasFinanceiroProjection,
    private val categoriaRepository: CategoriaRepository,
    private val completarDatas: suspend (String, Int, Int) -> ResultadoCompletarDatasCartao,
    private val clock: Clock,
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        contasFinanceiroProjection: ContasFinanceiroProjection,
        categoriaRepository: CategoriaRepository,
        completarDatasDoCartaoUseCase: CompletarDatasDoCartaoUseCase,
        clock: Clock,
    ) : this(
        instrumentoPublicId = savedStateHandle.get<String>(AccountsRoute.ARG_INSTRUMENTO_ID).orEmpty(),
        contasFinanceiroProjection = contasFinanceiroProjection,
        categoriaRepository = categoriaRepository,
        completarDatas = { id, fechamento, vencimento -> completarDatasDoCartaoUseCase(id, fechamento, vencimento) },
        clock = clock,
    )

    private val tentativa = MutableStateFlow(0)
    private val mutableUiState = MutableStateFlow(
        FaturasUiState(
            instrumentoPublicId = instrumentoPublicId,
            hoje = hojeAtual(),
            zona = CalendarioDeFatura.ZONA_SAO_PAULO,
        ),
    )

    val uiState: StateFlow<FaturasUiState> = mutableUiState.asStateFlow()

    init {
        if (instrumentoPublicId.isBlank()) {
            mutableUiState.update { it.copy(carregando = false, naoEncontrada = true) }
        } else {
            observar()
        }
    }

    fun onEvent(event: FaturasEvent) {
        when (event) {
            FaturasEvent.TentarDeNovo -> tentativa.update { it + 1 }
            is FaturasEvent.SelecionarAba -> mutableUiState.update { it.copy(abaSelecionada = event.aba) }
            is FaturasEvent.SelecionarCiclo -> selecionarCiclo(event.competencia)
            FaturasEvent.AbrirCompletarDatas -> abrirCompletarDatas()
            is FaturasEvent.AlterarFechamentoModal -> atualizarModalDatas { it.copy(fechamentoDia = event.valor, erro = null) }
            is FaturasEvent.AlterarVencimentoModal -> atualizarModalDatas { it.copy(vencimentoDia = event.valor, erro = null) }
            FaturasEvent.FecharModalCompletarDatas -> mutableUiState.update { it.copy(modalCompletarDatas = null) }
            FaturasEvent.SalvarDatasCartao -> salvarDatasCartao()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observar() {
        viewModelScope.launch {
            tentativa
                .flatMapLatest {
                    mutableUiState.update { it.copy(carregando = true, falha = false, naoEncontrada = false) }
                    combine(
                        contasFinanceiroProjection.observeFaturasDoCartao(instrumentoPublicId),
                        categoriaRepository.observeCategorias(),
                    ) { faturas, categorias -> faturas to categorias }
                        .catch {
                            mutableUiState.update { it.copy(carregando = false, falha = true) }
                        }
                }
                .collect { (faturas, categorias) ->
                    aplicarDados(faturas, categorias)
                }
        }
    }

    private fun aplicarDados(faturas: FaturasDoCartao?, categorias: List<Categoria>) {
        if (faturas == null) {
            mutableUiState.update { it.copy(carregando = false, falha = false, naoEncontrada = true) }
            return
        }
        val abertos = faturas.ciclos.filter { it.tipoCiclo == TipoCicloFatura.ABERTA }
        val proximos = faturas.ciclos.filter { it.tipoCiclo == TipoCicloFatura.PROXIMA }.sortedBy { it.competencia }
        val fechados = faturas.ciclos.filter { it.tipoCiclo == TipoCicloFatura.FECHADA }.sortedByDescending { it.competencia }
        val titulo = listOfNotNull(faturas.conta?.apelido, faturas.instrumento.apelido)
            .distinct()
            .joinToString(" · ")
            .ifBlank { "Faturas" }

        mutableUiState.update { atual ->
            atual.copy(
                carregando = false,
                falha = false,
                naoEncontrada = false,
                tituloCartao = titulo,
                instituicao = faturas.conta?.instituicao ?: InstituicaoFinanceira.OUTRA,
                ultimosDigitos = faturas.instrumento.ultimosDigitos,
                configuracaoIncompleta = faturas.configuracaoIncompleta,
                ciclosAbertos = abertos,
                ciclosProximos = proximos,
                ciclosFechados = fechados,
                competenciaSelecionadaProximas = atual.competenciaSelecionadaProximas
                    ?.takeIf { comp -> proximos.any { it.competencia == comp } }
                    ?: proximos.firstOrNull()?.competencia,
                competenciaSelecionadaFechadas = atual.competenciaSelecionadaFechadas
                    ?.takeIf { comp -> fechados.any { it.competencia == comp } }
                    ?: fechados.firstOrNull()?.competencia,
                categoriasPorId = categorias.associateBy { it.publicId },
                hoje = hojeAtual(),
            )
        }
    }

    private fun selecionarCiclo(competencia: LocalDate) {
        mutableUiState.update { atual ->
            when (atual.abaSelecionada) {
                AbaFaturas.PROXIMAS -> atual.copy(competenciaSelecionadaProximas = competencia)
                AbaFaturas.FECHADAS -> atual.copy(competenciaSelecionadaFechadas = competencia)
                AbaFaturas.ABERTA -> atual
            }
        }
    }

    private fun abrirCompletarDatas() {
        val atual = mutableUiState.value
        mutableUiState.update {
            it.copy(
                modalCompletarDatas = ModalCompletarDatasUiState(
                    instrumentoPublicId = atual.instrumentoPublicId,
                    tituloCartao = atual.tituloCartao,
                ),
            )
        }
    }

    private fun atualizarModalDatas(transformar: (ModalCompletarDatasUiState) -> ModalCompletarDatasUiState) {
        mutableUiState.update { estado ->
            estado.copy(modalCompletarDatas = estado.modalCompletarDatas?.let(transformar))
        }
    }

    private fun salvarDatasCartao() {
        val modal = mutableUiState.value.modalCompletarDatas ?: return
        val fechamento = modal.fechamentoInt
        val vencimento = modal.vencimentoInt
        if (!modal.podeSalvar || fechamento == null || vencimento == null) {
            return
        }
        viewModelScope.launch {
            atualizarModalDatas { it.copy(salvando = true, erro = null) }
            val resultado = runCatching { completarDatas(modal.instrumentoPublicId, fechamento, vencimento) }
            when (resultado.getOrNull()) {
                is ResultadoCompletarDatasCartao.Sucesso -> mutableUiState.update { it.copy(modalCompletarDatas = null) }
                ResultadoCompletarDatasCartao.DadosInvalidos ->
                    atualizarModalDatas { it.copy(salvando = false, erro = "Informe dias válidos entre 1 e 31.") }
                ResultadoCompletarDatasCartao.NaoEncontrado ->
                    atualizarModalDatas { it.copy(salvando = false, erro = "Cartão não encontrado.") }
                null ->
                    atualizarModalDatas { it.copy(salvando = false, erro = "Não foi possível salvar as datas. Tente de novo.") }
            }
        }
    }

    private fun hojeAtual(): LocalDate = ZonedDateTime.ofInstant(clock.instant(), CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
}
