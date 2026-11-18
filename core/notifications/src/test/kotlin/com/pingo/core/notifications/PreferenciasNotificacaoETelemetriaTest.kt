package com.pingo.core.notifications

import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.analytics.TelemetriaDeCaptura
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasNotificacaoETelemetriaTest {
    private val agora = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun observadorNaoPostaECancelaPendenciasQuandoDesligado() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val prefs = FakePreferencesNotificacao(revisar = false)
        val observador = ObservadorDePendencias(
            lancamentoRepository = FakeLancamentoRepository(),
            categoriaRepository = FakeCategoriaRepository(),
            notificacoes = notificacoes,
            preferences = prefs,
            dispatchers = DefaultAppDispatchers,
        )
        val pendente = Lancamento(
            publicId = "pend-1",
            instrumentoPublicId = "inst-1",
            categoriaPublicId = "cat-1",
            valorCentavos = 1500,
            dataHora = agora,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = "Mercado",
            status = StatusLancamento.PENDENTE,
            origem = OrigemLancamento.NOTIFICACAO,
            instituicaoOrigem = InstituicaoFinanceira.ITAU,
            confianca = 0.8,
            motivoPendencia = "BAIXA_CONFIANCA",
            criadoEm = agora,
            atualizadoEm = agora,
        )
        val cat = Categoria("cat-1", "Mercado", TipoCategoria.DESPESA, criadoEm = agora, atualizadoEm = agora)

        observador.sincronizar(listOf(pendente), listOf(cat), ativo = false)

        assertTrue(notificacoes.postagens.isEmpty())
        assertEquals(listOf(emptySet<String>()), notificacoes.ausentes)
    }

    @Test
    fun gatilhoDeCapturaParadaRespeitaPreferenciaDesligada() = runTest {
        val capturaDs = FakeCapturaDataSource().apply {
            registrarAtivo(agora.minus(Duration.ofDays(2)))
            observarDesde(agora.minus(Duration.ofDays(2)))
            registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(60)))
        }
        val notificador = object : NotificadorDeCapturaParada {
            var chamadas = 0

            override fun capturaParada() {
                chamadas++
            }
        }
        val clock = Clock.fixed(agora, ZoneOffset.UTC)
        val gatilho = GatilhoDeCapturaParada(
            monitorDaCaptura = MonitorDaCaptura(capturaDs, clock),
            capturaDataSource = capturaDs,
            notificador = notificador,
            preferences = FakePreferencesNotificacao(alertaCaptura = false),
            clock = clock,
            dispatchers = FakeAppDispatchers(StandardTestDispatcher(testScheduler)),
        )

        gatilho.verificar()

        assertEquals(0, notificador.chamadas)
    }

    @Test
    fun pipelineRegistraTelemetriaDeNotificacaoIgnoradaENaoEntendidaSemTextoBruto() = runTest {
        val eventos = mutableListOf<Pair<String, Map<String, String>>>()
        val analytics = object : AnalyticsFacade {
            override fun setAnalyticsEnabled(enabled: Boolean) = Unit

            override fun logEvent(name: String, attributes: Map<String, String>) {
                eventos += name to attributes
            }

            override fun recordError(throwable: Throwable) = Unit
        }
        val pipeline = PipelineDeCaptura(
            instituicaoResolver = PacoteInstituicaoResolver(),
            parserEngine = DefaultParserEngine(),
            regrasParaCaptura = RegrasParaCaptura(FakeRegraParserRepository()),
            capturaDataSource = FakeCapturaDataSource(),
            processadorDeCaptura = FakeProcessadorDeCaptura(),
            analytics = analytics,
            clock = Clock.fixed(agora, ZoneId.of("America/Sao_Paulo")),
            dispatchers = FakeAppDispatchers(StandardTestDispatcher(testScheduler)),
        )

        pipeline.processar(
            NotificacaoRecebida(
                pacote = "com.itau",
                tag = null,
                titulo = "Aviso desconhecido",
                texto = "TEXTO_SENSIVEL_DO_USUARIO_R$99",
                bigText = null,
                postTime = agora,
            ),
        )

        assertEquals(1, eventos.size)
        assertEquals(TelemetriaDeCaptura.EVENTO_CAPTURA_PROCESSADA, eventos[0].first)
        assertEquals("itau", eventos[0].second["instituicao"])
        assertEquals("nao_entendida", eventos[0].second["resultado"])
        assertTrue(eventos[0].second.values.none { it.contains("SENSIVEL") })
    }

    private class FakePreferencesNotificacao(revisar: Boolean = true, alertaCaptura: Boolean = true) : UserPreferencesDataSource {
        override val revisarPelaNotificacaoAtivo: Flow<Boolean> = MutableStateFlow(revisar)
        override val alertaCapturaParadaAtivo: Flow<Boolean> = MutableStateFlow(alertaCaptura)
        override val syncStatus: Flow<SyncStatus> = MutableStateFlow(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev-1"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
