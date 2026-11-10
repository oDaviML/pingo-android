package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ProcessadorDeCaptura
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.StatusLancamento
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
    private val processadorDeCaptura: ProcessadorDeCaptura,
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

        return when (val resultado = processadorDeCaptura.processar(capturada.paraProcessar())) {
            is ResultadoProcessamento.Gravado -> registrarGravado(notificacao, capturada, resultado)
            ResultadoProcessamento.Duplicada -> ResultadoCaptura.Duplicada
            ResultadoProcessamento.SemInstrumento -> ResultadoCaptura.SemInstrumento
        }
    }

    private suspend fun registrarGravado(
        notificacao: NotificacaoRecebida,
        capturada: TransacaoCapturada,
        resultado: ResultadoProcessamento.Gravado,
    ): ResultadoCaptura {
        val contador = if (resultado.lancamento.status == StatusLancamento.CONFIRMADO) {
            ContadorCaptura.RECONHECIDAS
        } else {
            ContadorCaptura.PENDENTES
        }
        capturaDataSource.incrementarContador(mesDaCaptura(notificacao), contador)
        transacoesRecentes.tryEmit(capturada)

        return ResultadoCaptura.Transacao(capturada)
    }

    private fun TransacaoCapturada.paraProcessar(): CapturaParaProcessar = CapturaParaProcessar(
        instituicao = instituicao,
        transacao = transacao,
        postTime = postTime,
        textoBruto = textoInterpretado(),
    )

    private fun TransacaoCapturada.textoInterpretado(): String? {
        val corpo = listOf(texto, bigText).firstOrNull { !it.isNullOrBlank() }

        return listOfNotNull(titulo?.takeIf(String::isNotBlank), corpo)
            .joinToString("\n")
            .takeIf(String::isNotEmpty)
    }

    private fun mesDaCaptura(notificacao: NotificacaoRecebida): YearMonth = YearMonth.from(notificacao.postTime.atZone(clock.zone))

    private suspend fun registrarContador(
        instituicao: InstituicaoFinanceira,
        notificacao: NotificacaoRecebida,
        contador: ContadorCaptura,
        resultado: ResultadoCaptura,
    ): ResultadoCaptura {
        capturaDataSource.registrarLeitura(instituicao, clock.instant())
        capturaDataSource.incrementarContador(mesDaCaptura(notificacao), contador)
        return resultado
    }
}
