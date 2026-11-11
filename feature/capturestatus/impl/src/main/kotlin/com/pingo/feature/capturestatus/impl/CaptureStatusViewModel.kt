package com.pingo.feature.capturestatus.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class CaptureStatusViewModel @Inject constructor(
    private val capturaDataSource: CapturaDataSource,
    private val saudeDaCaptura: SaudeDaCaptura,
    private val regraParserRepository: RegraParserRepository,
    private val atualizacaoDeRegras: AtualizacaoDeRegras,
    private val capturaController: CapturaController,
    private val acessoNotificacoes: AcessoNotificacoes,
    private val notificacoesDoSistema: NotificacoesDoSistema,
    private val bateriaDoAparelho: BateriaDoAparelho,
    private val clock: Clock,
) : ViewModel() {
    private val mes = YearMonth.now(clock)

    private val mutableUiState = MutableStateFlow(
        CaptureStatusUiState(agora = clock.instant(), permissoes = permissoesAtuais()),
    )

    val uiState: StateFlow<CaptureStatusUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                saudeDaCaptura.observar(),
                capturaDataSource.ultimaLeituraPorApp,
                capturaDataSource.contadoresDoMes(mes),
                regraParserRepository.observeVersaoVigente(),
                capturaDataSource.estadoAtualizacaoRegras,
            ) { diagnostico, leituras, contadores, versao, regras ->
                DadosDaTela(diagnostico, leituras, contadores, versao, regras)
            }.collect(::aplicar)
        }
    }

    fun atualizarEstados() {
        viewModelScope.launch { atualizarEstadosSuspenso() }
    }

    fun reconectar() {
        if (mutableUiState.value.reconectando) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(reconectando = true, mensagem = null) }
            val estado = runCatching { capturaController.reconectar() }.getOrNull()
            atualizarEstadosSuspenso()
            val mensagem = if (estado?.estado == EstadoListener.ATIVO) {
                "Captura reconectada."
            } else {
                "Não foi possível reconectar. Verifique as permissões."
            }
            mutableUiState.update { it.copy(reconectando = false, mensagem = mensagem) }
        }
    }

    fun atualizarRegras() {
        if (mutableUiState.value.atualizandoRegras) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(atualizandoRegras = true, mensagem = null) }
            val resultado = runCatching { atualizacaoDeRegras.atualizar() }
                .getOrElse { ResultadoAtualizacaoRegras.Falha(MOTIVO_FALHA_PADRAO) }
            atualizarEstadosSuspenso()
            mutableUiState.update { it.copy(atualizandoRegras = false, mensagem = mensagemDe(resultado)) }
        }
    }

    private suspend fun atualizarEstadosSuspenso() {
        val diagnostico = saudeDaCaptura.atual()
        val situacao = situacaoEfetiva(diagnostico)

        mutableUiState.update { atual ->
            atual.copy(
                situacao = situacao,
                motivo = motivoEfetivo(situacao, diagnostico),
                ultimaLeituraEm = diagnostico.ultimaLeituraEm,
                ultimaLeituraInstituicao = diagnostico.ultimaLeituraInstituicao,
                semLeituraDesde = diagnostico.semLeituraDesde,
                agora = clock.instant(),
                permissoes = permissoesAtuais(),
            )
        }
    }

    private fun aplicar(dados: DadosDaTela) {
        val situacao = situacaoEfetiva(dados.diagnostico)

        mutableUiState.update { atual ->
            atual.copy(
                carregando = false,
                situacao = situacao,
                motivo = motivoEfetivo(situacao, dados.diagnostico),
                ultimaLeituraEm = dados.diagnostico.ultimaLeituraEm,
                ultimaLeituraInstituicao = dados.diagnostico.ultimaLeituraInstituicao,
                semLeituraDesde = dados.diagnostico.semLeituraDesde,
                agora = clock.instant(),
                contadores = dados.contadores.paraTela(),
                leiturasPorApp = INSTITUICOES.map { instituicao -> LeituraPorApp(instituicao, dados.leituras[instituicao]) },
                permissoes = permissoesAtuais(),
                versaoDasRegras = dados.versao,
                regrasAtualizadasEm = dados.regras.atualizadoEm,
                falhaAoAtualizarRegras = dados.regras.motivoFalha,
            )
        }
    }

    private fun situacaoEfetiva(diagnostico: DiagnosticoDaCaptura): SituacaoDaCaptura =
        if (acessoNotificacoes.concedido()) diagnostico.situacao else SituacaoDaCaptura.PARADA

    private fun motivoEfetivo(situacao: SituacaoDaCaptura, diagnostico: DiagnosticoDaCaptura): MotivoParada? = when {
        situacao != SituacaoDaCaptura.PARADA -> null
        !acessoNotificacoes.concedido() -> MotivoParada.PERMISSAO_REVOGADA
        else -> diagnostico.motivo
    }

    private fun permissoesAtuais(): List<PermissaoDaCaptura> = listOf(
        PermissaoDaCaptura(ItemDePermissao.ACESSO_A_NOTIFICACOES, acessoNotificacoes.concedido()),
        PermissaoDaCaptura(ItemDePermissao.NOTIFICACOES_DO_PINGO, notificacoesDoSistema.habilitadas()),
        PermissaoDaCaptura(ItemDePermissao.BATERIA, bateriaDoAparelho.semRestricaoDeBateria()),
    )

    private fun mensagemDe(resultado: ResultadoAtualizacaoRegras): String = when (resultado) {
        is ResultadoAtualizacaoRegras.Atualizado -> "Regras atualizadas para a versão ${resultado.versao}."
        is ResultadoAtualizacaoRegras.SemAlteracao -> "As regras já estão na versão ${resultado.versao}."
        is ResultadoAtualizacaoRegras.Falha -> "Não foi possível atualizar as regras."
    }

    private fun ContadoresDoMes.paraTela(): ContadoresDaCaptura = ContadoresDaCaptura(
        reconhecidas = reconhecidas,
        paraRevisao = pendentes,
        ignoradas = ignoradas,
        naoEntendidas = naoEntendidas,
    )

    private data class DadosDaTela(
        val diagnostico: DiagnosticoDaCaptura,
        val leituras: Map<InstituicaoFinanceira, Instant>,
        val contadores: ContadoresDoMes,
        val versao: Int?,
        val regras: EstadoAtualizacaoRegras,
    )

    private companion object {
        const val MOTIVO_FALHA_PADRAO = "sem_conexao"

        val INSTITUICOES = InstituicaoFinanceira.entries.filter { instituicao -> instituicao != InstituicaoFinanceira.OUTRA }
    }
}
