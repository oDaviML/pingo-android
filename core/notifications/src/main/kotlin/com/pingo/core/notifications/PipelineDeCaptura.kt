package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.parser.ParserEngine
import com.pingo.core.parser.ResultadoParse
import com.pingo.core.parser.TransacaoExtraida
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineDeCaptura @Inject constructor(
    private val instituicaoResolver: InstituicaoResolver,
    private val parserEngine: ParserEngine,
    private val regrasParaCaptura: RegrasParaCaptura,
    private val capturaDataSource: CapturaDataSource,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {
    private val transacoesRecentes =
        MutableSharedFlow<TransacaoCapturada>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val transacoes: SharedFlow<TransacaoCapturada> = transacoesRecentes.asSharedFlow()

    suspend fun processar(notificacao: NotificacaoRecebida): ResultadoCaptura = withContext(dispatchers.io) {
        val instituicao = instituicaoResolver.resolver(notificacao.pacote, notificacao.tag)
            ?: return@withContext ResultadoCaptura.PacoteDesconhecido

        when (val resultado = parserEngine.classificar(notificacao.bruta, regrasParaCaptura.versaoDe(instituicao))) {
            is ResultadoParse.Transacao -> registrarTransacao(instituicao, notificacao, resultado.dados)
            ResultadoParse.Ignorada -> registrarContador(instituicao, notificacao, ContadorCaptura.IGNORADAS, ResultadoCaptura.Ignorada)
            ResultadoParse.NaoEntendida ->
                registrarContador(instituicao, notificacao, ContadorCaptura.NAO_ENTENDIDAS, ResultadoCaptura.NaoEntendida)
            ResultadoParse.Descartada -> ResultadoCaptura.Descartada
        }
    }

    private suspend fun registrarTransacao(
        instituicao: InstituicaoFinanceira,
        notificacao: NotificacaoRecebida,
        extraida: TransacaoExtraida,
    ): ResultadoCaptura {
        capturaDataSource.registrarLeitura(instituicao, clock.instant())

        val capturada = TransacaoCapturada(
            instituicao = instituicao,
            pacote = notificacao.pacote,
            titulo = notificacao.titulo,
            texto = notificacao.texto,
            bigText = notificacao.bigText,
            postTime = notificacao.postTime,
            transacao = extraida,
        )

        transacoesRecentes.tryEmit(capturada)

        return ResultadoCaptura.Transacao(capturada)
    }

    private suspend fun registrarContador(
        instituicao: InstituicaoFinanceira,
        notificacao: NotificacaoRecebida,
        contador: ContadorCaptura,
        resultado: ResultadoCaptura,
    ): ResultadoCaptura {
        capturaDataSource.registrarLeitura(instituicao, clock.instant())
        capturaDataSource.incrementarContador(YearMonth.from(notificacao.postTime.atZone(clock.zone)), contador)
        return resultado
    }
}
