package com.pingo.core.notifications

import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PipelineDeCapturaTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val capturaDataSource = FakeCapturaDataSource()
    private val regraParserRepository = FakeRegraParserRepository()

    @Test
    fun extraiCompraEEntregaNoCanalEmMemoria() = runTest {
        val pipeline = pipeline()
        val recebidas = mutableListOf<TransacaoCapturada>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            pipeline.transacoes.collect { transacao -> recebidas += transacao }
        }

        val resultado = pipeline.processar(
            notificacao(pacote = "com.itau", titulo = "Sua compra foi aprovada", texto = RegrasDeTeste.TEXTO_COMPRA_ITAU),
        )

        val capturada = (resultado as ResultadoCaptura.Transacao).capturada
        assertEquals(InstituicaoFinanceira.ITAU, capturada.instituicao)
        assertEquals(3790L, capturada.transacao.valorCentavos)
        assertEquals("CLARO *31987905690", capturada.transacao.estabelecimento)
        assertEquals(listOf(capturada), recebidas)
        assertEquals(clock.instant(), capturaDataSource.ultimaLeitura(InstituicaoFinanceira.ITAU))
    }

    @Test
    fun transacaoNaoIncrementaContadoresDaCaptura() = runTest {
        val pipeline = pipeline()

        pipeline.processar(notificacao(pacote = "com.itau", texto = RegrasDeTeste.TEXTO_COMPRA_ITAU))

        val mes = YearMonth.of(2026, 11)
        assertEquals(0, capturaDataSource.valorDoContador(mes, ContadorCaptura.IGNORADAS))
        assertEquals(0, capturaDataSource.valorDoContador(mes, ContadorCaptura.NAO_ENTENDIDAS))
        assertEquals(0, capturaDataSource.valorDoContador(mes, ContadorCaptura.RECONHECIDAS))
    }

    @Test
    fun notificacaoNaoFinanceiraContaComoIgnoradaEAtualizaUltimaLeitura() = runTest {
        val pipeline = pipeline()

        val resultado = pipeline.processar(
            notificacao(pacote = "com.itau", titulo = "Seu limite", texto = RegrasDeTeste.TEXTO_RUIDO_ITAU),
        )

        assertEquals(ResultadoCaptura.Ignorada, resultado)
        assertEquals(1, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.IGNORADAS))
        assertEquals(clock.instant(), capturaDataSource.ultimaLeitura(InstituicaoFinanceira.ITAU))
    }

    @Test
    fun textoForaDosPadroesContaComoNaoEntendida() = runTest {
        val pipeline = pipeline()

        val resultado = pipeline.processar(
            notificacao(pacote = "br.com.intermedium", titulo = "Aviso", texto = RegrasDeTeste.TEXTO_DESCONHECIDO),
        )

        assertEquals(ResultadoCaptura.NaoEntendida, resultado)
        assertEquals(1, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.NAO_ENTENDIDAS))
        assertEquals(clock.instant(), capturaDataSource.ultimaLeitura(InstituicaoFinanceira.INTER))
    }

    @Test
    fun pacoteDesconhecidoNaoContaNemRegistraLeitura() = runTest {
        val pipeline = pipeline()

        val resultado = pipeline.processar(notificacao(pacote = "com.app.desconhecido", texto = RegrasDeTeste.TEXTO_COMPRA_ITAU))

        assertEquals(ResultadoCaptura.PacoteDesconhecido, resultado)
        assertEquals(0, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.NAO_ENTENDIDAS))
        assertNull(capturaDataSource.ultimaLeitura(InstituicaoFinanceira.ITAU))
    }

    @Test
    fun notificacaoVaziaEDescartadaSemContadores() = runTest {
        val pipeline = pipeline()

        val resultado = pipeline.processar(notificacao(pacote = "com.itau", titulo = null, texto = null, bigText = null))

        assertEquals(ResultadoCaptura.Descartada, resultado)
        assertEquals(0, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.NAO_ENTENDIDAS))
        assertEquals(0, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.IGNORADAS))
        assertNull(capturaDataSource.ultimaLeitura(InstituicaoFinanceira.ITAU))
    }

    @Test
    fun contaNoMesDoPostTimeDaNotificacao() = runTest {
        val pipeline = pipeline()

        pipeline.processar(
            notificacao(
                pacote = "com.nu.production",
                texto = "Seu limite evolui com você",
                postTime = Instant.parse("2026-11-01T02:30:00Z"),
            ),
        )

        assertEquals(1, capturaDataSource.valorDoContador(YearMonth.of(2026, 10), ContadorCaptura.IGNORADAS))
        assertEquals(0, capturaDataSource.valorDoContador(YearMonth.of(2026, 11), ContadorCaptura.IGNORADAS))
    }

    @Test
    fun usaBigTextQuandoTextoVazio() = runTest {
        val pipeline = pipeline()

        val resultado = pipeline.processar(
            notificacao(pacote = "com.itau", titulo = "Sua compra foi aprovada", texto = "", bigText = RegrasDeTeste.TEXTO_COMPRA_ITAU),
        )

        val capturada = (resultado as ResultadoCaptura.Transacao).capturada
        assertEquals(3790L, capturada.transacao.valorCentavos)
        assertTrue(capturada.bigText!!.isNotBlank())
    }

    private fun TestScope.pipeline(): PipelineDeCaptura = PipelineDeCaptura(
        instituicaoResolver = PacoteInstituicaoResolver(),
        parserEngine = DefaultParserEngine(),
        regrasParaCaptura = RegrasParaCaptura(regraParserRepository),
        capturaDataSource = capturaDataSource,
        clock = clock,
        dispatchers = FakeAppDispatchers(StandardTestDispatcher(testScheduler)),
    )

    private fun notificacao(
        pacote: String,
        tag: String? = null,
        titulo: String? = "Notificação",
        texto: String? = RegrasDeTeste.TEXTO_COMPRA_ITAU,
        bigText: String? = null,
        postTime: Instant = Instant.parse("2026-11-10T12:00:00Z"),
    ): NotificacaoRecebida = NotificacaoRecebida(
        pacote = pacote,
        tag = tag,
        titulo = titulo,
        texto = texto,
        bigText = bigText,
        postTime = postTime,
    )
}
