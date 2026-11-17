package com.pingo.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val acessoNotificacoes: AcessoNotificacoes,
    private val saudeDaCaptura: SaudeDaCaptura,
    lancamentoRepository: LancamentoRepository,
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    private val clock: Clock,
) : ViewModel() {
    private val dados = MutableStateFlow(BaseDoInicio())
    private val mes = MutableStateFlow(mesAtual())
    private var capturaAutomatica = acessoNotificacoes.concedido()
    private var situacao = if (capturaAutomatica) SituacaoDaCaptura.ATIVA else SituacaoDaCaptura.PARADA
    private var motivo = if (capturaAutomatica) null else MotivoParada.PERMISSAO_REVOGADA
    private var semLeituraHa48h = false
    private val mutableUiState = MutableStateFlow(
        HomeUiState(
            capturaAutomatica = capturaAutomatica,
            situacao = situacao,
            motivo = motivo,
        ),
    )

    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        recompor()
        viewModelScope.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                contaRepository.observeContas(),
                instrumentoRepository.observeInstrumentos(),
                categoriaRepository.observeCategorias(),
            ) { lancamentos, contas, instrumentos, categorias ->
                BaseDoInicio(
                    carregado = true,
                    lancamentos = lancamentos,
                    contas = contas,
                    instrumentos = instrumentos,
                    categorias = categorias,
                )
            }.collect { base ->
                dados.value = base
                recompor()
            }
        }
        viewModelScope.launch {
            saudeDaCaptura.observar().collect(::aplicarDiagnostico)
        }
    }

    fun atualizarCaptura() {
        capturaAutomatica = acessoNotificacoes.concedido()
        viewModelScope.launch { aplicarDiagnostico(saudeDaCaptura.atual()) }
        recompor()
    }

    fun mesAnterior() {
        moverMes(-1)
    }

    fun mesProximo() {
        moverMes(1)
    }

    private fun moverMes(delta: Long) {
        val alvo = mes.value.plusMonths(delta)
        val limites = limites()

        if (alvo < limites.first || alvo > limites.second) {
            return
        }

        mes.value = alvo
        recompor()
    }

    private fun aplicarDiagnostico(diagnostico: DiagnosticoDaCaptura) {
        semLeituraHa48h = diagnostico.semLeituraDesde != null
        situacao = if (capturaAutomatica) diagnostico.situacao else SituacaoDaCaptura.PARADA
        motivo = when {
            situacao != SituacaoDaCaptura.PARADA -> null
            capturaAutomatica -> diagnostico.motivo
            else -> MotivoParada.PERMISSAO_REVOGADA
        }
        recompor()
    }

    private fun recompor() {
        val base = dados.value
        val mesEscolhido = mes.value
        val ativos = base.lancamentos.filter { it.excluidoEm == null }
        val categoriasPorId = base.categorias.associateBy { it.publicId }
        val instrumentosPorId = base.instrumentos.associateBy { it.publicId }
        val contasPorId = base.contas.associateBy { it.publicId }
        val resumo = resumoDoMes(ativos, mesEscolhido, clock.zone)
        val limites = limites(base.lancamentos)

        mutableUiState.value = HomeUiState(
            carregando = !base.carregado,
            saudacao = saudacaoAtual(),
            capturaAutomatica = capturaAutomatica,
            situacao = situacao,
            motivo = motivo,
            semLeituraHa48h = semLeituraHa48h,
            mes = mesEscolhido,
            podeVoltarMes = mesEscolhido > limites.first,
            podeAvancarMes = mesEscolhido < limites.second,
            saldoCentavos = resumo.saldoCentavos,
            receitasCentavos = resumo.receitasCentavos,
            despesasCentavos = resumo.despesasCentavos,
            pendentes = contarPendentes(ativos),
            topCategorias = categoriasDoTopo(ativos, mesEscolhido, clock.zone, categoriasPorId),
            ultimos = ultimosLancamentos(ativos, categoriasPorId, instrumentosPorId, contasPorId),
            vazia = base.carregado && ativos.isEmpty(),
        )
    }

    private fun limites(lancamentos: List<Lancamento> = dados.value.lancamentos): Pair<YearMonth, YearMonth> {
        val atual = mesAtual()
        val meses = mesesDosLancamentos(lancamentos, clock.zone)
        val minimo = listOfNotNull(meses.firstOrNull(), atual).min()
        val maximo = listOfNotNull(meses.lastOrNull(), atual).max()

        return minimo to maximo
    }

    private fun mesAtual(): YearMonth = YearMonth.from(clock.instant().atZone(clock.zone))

    private fun saudacaoAtual(): String = when (clock.instant().atZone(clock.zone).hour) {
        in HORA_INICIO_DIA..HORA_FIM_DIA -> "Bom dia,"
        in HORA_INICIO_TARDE..HORA_FIM_TARDE -> "Boa tarde,"
        else -> "Boa noite,"
    }

    private companion object {
        const val HORA_INICIO_DIA = 5
        const val HORA_FIM_DIA = 11
        const val HORA_INICIO_TARDE = 12
        const val HORA_FIM_TARDE = 17
    }

    private data class BaseDoInicio(
        val carregado: Boolean = false,
        val lancamentos: List<Lancamento> = emptyList(),
        val contas: List<Conta> = emptyList(),
        val instrumentos: List<Instrumento> = emptyList(),
        val categorias: List<Categoria> = emptyList(),
    )
}
