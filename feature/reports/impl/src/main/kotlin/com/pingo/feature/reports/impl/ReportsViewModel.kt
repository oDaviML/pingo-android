package com.pingo.feature.reports.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.projecao.RelatoriosProjection
import com.pingo.core.model.assinatura.ChaveAssinatura
import com.pingo.core.model.assinatura.SugestaoDeAssinatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val relatoriosProjection: RelatoriosProjection,
    private val assinaturaRepository: AssinaturaRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val contaRepository: ContaRepository,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ViewModel() {
    private val mesSelecionado = MutableStateFlow(YearMonth.now(clock))
    private val tentativa = MutableStateFlow(0)
    private val estadoBase = MutableStateFlow<EstadoBase>(EstadoBase.Carregando(mesSelecionado.value))
    private val processamento = MutableStateFlow<Set<String>>(emptySet())
    private val confirmacaoCancelamento = MutableStateFlow<AssinaturaConfirmadaUi?>(null)
    private val mensagemErroAcao = MutableStateFlow<String?>(null)
    private val chavesEmProcessamento = mutableSetOf<String>()
    private var sugestoesPorChave: Map<String, SugestaoDeAssinatura> = emptyMap()

    private val mutableUiState = MutableStateFlow(
        ReportsUiState(
            carregando = true,
            mes = mesSelecionado.value,
            rotuloMes = rotuloMesCompleto(mesSelecionado.value),
        ),
    )

    val uiState: StateFlow<ReportsUiState> = mutableUiState.asStateFlow()

    init {
        observarDados()
        recomporEstado()
    }

    fun onEvent(evento: ReportsEvent) {
        when (evento) {
            ReportsEvent.MesAnterior -> alterarMes(mesSelecionado.value.minusMonths(1))
            ReportsEvent.MesProximo -> {
                val proximo = mesSelecionado.value.plusMonths(1)
                if (proximo <= YearMonth.now(clock)) alterarMes(proximo)
            }
            is ReportsEvent.SelecionarMes -> {
                if (evento.mes <= YearMonth.now(clock)) alterarMes(evento.mes)
            }
            ReportsEvent.TentarDeNovo -> tentativa.update { it + 1 }
            is ReportsEvent.ConfirmarSugestao -> confirmarSugestao(evento.chave)
            is ReportsEvent.RecusarSugestao -> recusarSugestao(evento.chave)
            is ReportsEvent.SolicitarCancelamentoAssinatura -> abrirConfirmacaoCancelamento(evento.publicId)
            ReportsEvent.ConfirmarCancelamentoAssinatura -> confirmarCancelamento()
            ReportsEvent.DispensarCancelamentoAssinatura -> confirmacaoCancelamento.value = null
            ReportsEvent.DispensarMensagemErro -> mensagemErroAcao.value = null
        }
    }

    private fun alterarMes(novoMes: YearMonth) {
        if (novoMes != mesSelecionado.value) {
            mesSelecionado.value = novoMes
        }
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(mesSelecionado, tentativa) { mes, _ -> mes }
                .flatMapLatest { mes ->
                    observarMes(mes)
                        .onStart {
                            if (estadoBase.value !is EstadoBase.Sucesso) {
                                emit(EstadoBase.Carregando(mes))
                            }
                        }
                }
                .collect { base ->
                    if (base is EstadoBase.Sucesso) {
                        sugestoesPorChave = base.sugestoes
                    }
                    estadoBase.value = base
                }
        }
    }

    private fun observarMes(mes: YearMonth) = combine(
        relatoriosProjection.observeRelatorioMensal(mes),
        assinaturaRepository.observeConfirmadas(),
        assinaturaRepository.observeSugestoes(),
        contaRepository.observeContas(),
        instrumentoRepository.observeInstrumentos(),
    ) { detalhado, confirmadas, sugestoes, contas, instrumentos ->
        val ui = mapearEstadoRelatorio(
            mes = mes,
            mesAtual = YearMonth.now(clock),
            detalhado = detalhado,
            confirmadas = confirmadas,
            sugestoes = sugestoes,
            contas = contas,
            instrumentos = instrumentos,
        )
        EstadoBase.Sucesso(
            ui = ui,
            sugestoes = sugestoes.associateBy { chaveTextoDe(it.chave) },
        ) as EstadoBase
    }
        .flowOn(dispatchers.default)
        .catch {
            emit(EstadoBase.Erro(mes))
        }

    private fun recomporEstado() {
        viewModelScope.launch {
            combine(
                estadoBase,
                processamento,
                confirmacaoCancelamento,
                mensagemErroAcao,
            ) { base, emProcessamento, confirmacao, erroAcao ->
                when (base) {
                    is EstadoBase.Carregando -> ReportsUiState(
                        carregando = true,
                        mes = base.mes,
                        rotuloMes = rotuloMesCompleto(base.mes),
                        podeAvancarMes = base.mes < YearMonth.now(clock),
                    )
                    is EstadoBase.Erro -> ReportsUiState(
                        carregando = false,
                        falha = true,
                        mes = base.mes,
                        rotuloMes = rotuloMesCompleto(base.mes),
                        podeAvancarMes = base.mes < YearMonth.now(clock),
                    )
                    is EstadoBase.Sucesso -> base.ui.copy(
                        assinaturasEmProcessamento = emProcessamento,
                        confirmacaoCancelamento = confirmacao,
                        mensagemErroAcao = erroAcao,
                    )
                }
            }.collect { estado ->
                mutableUiState.value = estado
            }
        }
    }

    private fun confirmarSugestao(chave: ChaveAssinatura) {
        val chaveTexto = chaveTextoDe(chave)
        val sugestao = sugestoesPorChave[chaveTexto] ?: return
        executarAcaoAssinatura(chaveTexto, "Não foi possível confirmar a assinatura.") {
            assinaturaRepository.confirmarSugestao(sugestao)
        }
    }

    private fun recusarSugestao(chave: ChaveAssinatura) {
        val chaveTexto = chaveTextoDe(chave)
        val sugestao = sugestoesPorChave[chaveTexto] ?: return
        executarAcaoAssinatura(chaveTexto, "Não foi possível recusar a sugestão.") {
            assinaturaRepository.recusarSugestao(sugestao)
        }
    }

    private fun abrirConfirmacaoCancelamento(publicId: String) {
        val sucesso = estadoBase.value as? EstadoBase.Sucesso ?: return
        val assinatura = sucesso.ui.assinaturasConfirmadas.firstOrNull { it.publicId == publicId } ?: return
        confirmacaoCancelamento.value = assinatura
    }

    private fun confirmarCancelamento() {
        val alvo = confirmacaoCancelamento.value ?: return
        confirmacaoCancelamento.value = null
        executarAcaoAssinatura(alvo.publicId, "Não foi possível cancelar a assinatura.") {
            assinaturaRepository.cancelarAssinatura(alvo.publicId)
        }
    }

    private fun executarAcaoAssinatura(chave: String, mensagemDeErro: String, acao: suspend () -> Unit) {
        if (!chavesEmProcessamento.add(chave)) return
        processamento.value = chavesEmProcessamento.toSet()
        viewModelScope.launch {
            try {
                acao()
            } catch (_: Throwable) {
                mensagemErroAcao.value = mensagemDeErro
            } finally {
                chavesEmProcessamento.remove(chave)
                processamento.value = chavesEmProcessamento.toSet()
            }
        }
    }

    private sealed interface EstadoBase {
        data class Carregando(val mes: YearMonth) : EstadoBase

        data class Erro(val mes: YearMonth) : EstadoBase

        data class Sucesso(val ui: ReportsUiState, val sugestoes: Map<String, SugestaoDeAssinatura>) : EstadoBase
    }
}
