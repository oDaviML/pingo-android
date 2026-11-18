package com.pingo.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Assinatura
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.JanelaDeAvisoDeAssinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class AvisoDeAssinaturaWorkManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    @Before
    fun inicializarWorkManager() {
        val configuracao = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuracao)
    }

    @Test
    fun reconciliacaoAgendaNoWorkManagerECancelaQuandoDesligaOuMudaData() = runBlocking {
        val dataInicial = LocalDate.of(2026, 11, 20)
        val dataAtualizada = LocalDate.of(2026, 12, 20)
        val preferences = FakePreferences(ativo = true)
        val repo = FakeAssinaturaRepository(listOf(assinatura("sub-1", dataInicial)))
        val agora = Instant.parse("2026-11-17T12:00:00Z")
        val agendador = WorkManagerAgendadorDeAvisosDeAssinatura(
            context = context,
            preferences = preferences,
            assinaturaRepository = repo,
            clock = Clock.fixed(agora, ZoneOffset.UTC),
            dispatchers = DefaultAppDispatchers,
        )

        agendador.reconciliarTodas()

        val nomeInicial = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", dataInicial)
        val infosIniciais = workManager.getWorkInfosForUniqueWork(nomeInicial).get()
        assertEquals(1, infosIniciais.size)
        assertEquals(WorkInfo.State.ENQUEUED, infosIniciais.single().state)
        assertTrue(WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL in infosIniciais.single().tags)

        repo.atualizar(listOf(assinatura("sub-1", dataAtualizada)))
        agendador.reconciliarTodas()

        val nomeNovo = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", dataAtualizada)
        assertEquals(WorkInfo.State.CANCELLED, workManager.getWorkInfosForUniqueWork(nomeInicial).get().single().state)
        assertEquals(WorkInfo.State.ENQUEUED, workManager.getWorkInfosForUniqueWork(nomeNovo).get().single().state)

        preferences.setAvisoAssinaturasAtivo(false)
        agendador.reconciliarTodas()

        assertEquals(WorkInfo.State.CANCELLED, workManager.getWorkInfosForUniqueWork(nomeNovo).get().single().state)
    }

    @Test
    fun workerExecutaJanelaMesmoDiaEBloqueiaAtrasadoSemTextoBruto() = runBlocking {
        val dataPrevista = LocalDate.of(2026, 11, 20)
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(dataPrevista)
        val preferences = FakePreferences(ativo = true)
        val repo = FakeAssinaturaRepository(listOf(assinatura("sub-1", dataPrevista)))
        val notificador = FakeNotificador()

        val executorNoDia = ExecutorDeAvisoDeAssinatura(
            preferences = preferences,
            assinaturaRepository = repo,
            notificadorDeAssinatura = notificador,
            clock = Clock.fixed(Instant.parse("2026-11-19T02:59:59Z"), ZoneOffset.UTC),
        )
        val resultadoNoDia = construirWorker(
            executor = executorNoDia,
            dados = AvisoDeAssinaturaWorker.dadosDeEntrada("sub-1", dataPrevista, alvo),
        ).doWork()

        assertEquals(ListenableWorker.Result.success(), resultadoNoDia)
        assertEquals(1, notificador.postadas.size)

        val executorDiaSeguinte = ExecutorDeAvisoDeAssinatura(
            preferences = preferences,
            assinaturaRepository = repo,
            notificadorDeAssinatura = notificador,
            clock = Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneOffset.UTC),
        )
        val resultadoAtrasado = construirWorker(
            executor = executorDiaSeguinte,
            dados = AvisoDeAssinaturaWorker.dadosDeEntrada("sub-1", dataPrevista, alvo),
        ).doWork()

        assertEquals(ListenableWorker.Result.success(), resultadoAtrasado)
        assertEquals(1, notificador.postadas.size)
    }

    @Test
    fun notificacaoConstruidaNaoUsaNewTaskNemExpoeTextoBruto() {
        val item = assinatura("sub-1", LocalDate.of(2026, 11, 20))
        val intencao = NotificacoesDeAssinatura.intencaoDeAbertura(context)
        assertEquals(0, intencao.flags and Intent.FLAG_ACTIVITY_NEW_TASK)

        val notificacao = NotificacoesDeAssinatura.construir(context, item, LocalDate.of(2026, 11, 20))
        assertNotNull(notificacao.contentIntent)
        assertEquals(NotificacoesDeAssinatura.CANAL, notificacao.channelId)
        val texto = notificacao.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        assertTrue(texto.contains("Netflix"))
        assertTrue(texto.contains("R$ 55,90"))
        assertTrue(texto.contains("20/11/2026"))
        assertFalse(notificacao.extras.keySet().any { it.contains("textoBruto", ignoreCase = true) })
    }

    @Test
    fun workerComNotificadorRealEntregaNoNotificationManagerEDumpsysSemTextoBruto() = runBlocking {
        val gerenciador = context.getSystemService(NotificationManager::class.java)
        val item = assinatura("sub-real-1", LocalDate.of(2026, 11, 20))
        val idNotificacao = NotificacoesDeAssinatura.idDe(item.publicId)
        val markerBruto = "MARCADOR_TEXTO_BRUTO_PROIBIDO_9981"
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 11, 20))
        val preferences = FakePreferences(ativo = true)
        val repo = FakeAssinaturaRepository(listOf(item))
        val notificadorReal = NotificadorDeAssinaturaDoAndroid(context, NotificacoesDoSistemaDoAndroid(context))

        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        executarShell("appops set ${context.packageName} POST_NOTIFICATION allow")
        gerenciador.cancel(idNotificacao)
        assertTrue("Notificador deve estar habilitado", notificadorReal.podeNotificar())

        try {
            val agendador = WorkManagerAgendadorDeAvisosDeAssinatura(
                context = context,
                preferences = preferences,
                assinaturaRepository = repo,
                clock = Clock.fixed(alvo.minusSeconds(3600), ZoneOffset.UTC),
                dispatchers = DefaultAppDispatchers,
            )
            agendador.reconciliarTodas()
            val nomeUnico = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico(item.publicId, LocalDate.of(2026, 11, 20))
            val workInfo = workManager.getWorkInfosForUniqueWork(nomeUnico).get().single()
            assertFalse(workInfo.tags.any { it.contains(markerBruto) })
            assertFalse(workInfo.outputData.keyValueMap.toString().contains(markerBruto))

            val executor = ExecutorDeAvisoDeAssinatura(
                preferences = preferences,
                assinaturaRepository = repo,
                notificadorDeAssinatura = notificadorReal,
                clock = Clock.fixed(alvo, ZoneOffset.UTC),
            )
            val resultado = construirWorker(
                executor = executor,
                dados = AvisoDeAssinaturaWorker.dadosDeEntrada(item.publicId, LocalDate.of(2026, 11, 20), alvo),
            ).doWork()

            assertEquals(ListenableWorker.Result.success(), resultado)
            var ativa = gerenciador.activeNotifications.firstOrNull { it.id == idNotificacao }
            var tentativas = 0
            while (ativa == null && tentativas < 30) {
                kotlinx.coroutines.delay(100L)
                ativa = gerenciador.activeNotifications.firstOrNull { it.id == idNotificacao }
                tentativas++
            }
            assertNotNull("Notificação não apareceu em activeNotifications", ativa)
            val textoAtivo = ativa!!.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            assertTrue(textoAtivo.contains("Netflix"))
            assertTrue(textoAtivo.contains("R$ 55,90"))
            assertTrue(textoAtivo.contains("20/11/2026"))
            assertFalse(textoAtivo.contains(markerBruto))
            assertFalse(ativa.notification.extras.keySet().any { it.contains("textoBruto", ignoreCase = true) })

            val dumpsys = executarShell("dumpsys notification --noredact")
            assertTrue(dumpsys.contains(context.packageName))
            assertFalse(dumpsys.contains(markerBruto))

            gerenciador.cancel(idNotificacao)
            var canceladas = 0
            while (gerenciador.activeNotifications.any { it.id == idNotificacao } && canceladas < 30) {
                kotlinx.coroutines.delay(50L)
                canceladas++
            }
            preferences.setAvisoAssinaturasAtivo(false)
            assertFalse(executor.executar(item.publicId, LocalDate.of(2026, 11, 20).toString(), alvo.toEpochMilli()))
            assertFalse(gerenciador.activeNotifications.any { it.id == idNotificacao })
        } finally {
            gerenciador.cancel(idNotificacao)
        }
    }

    private fun executarShell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private fun construirWorker(executor: ExecutorDeAvisoDeAssinatura, dados: androidx.work.Data): AvisoDeAssinaturaWorker =
        TestListenableWorkerBuilder<AvisoDeAssinaturaWorker>(context)
            .setInputData(dados)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker? = if (workerClassName == AvisoDeAssinaturaWorker::class.java.name) {
                        AvisoDeAssinaturaWorker(appContext, workerParameters, executor)
                    } else {
                        null
                    }
                },
            )
            .build()

    private fun assinatura(publicId: String, proxima: LocalDate): Assinatura = Assinatura(
        publicId = publicId,
        estabelecimento = "Netflix",
        estabelecimentoNormalizado = "netflix",
        valorReferenciaCentavos = 5590,
        ultimaCobranca = LocalDate.of(2026, 10, 20),
        proximaCobrancaEstimada = proxima,
        status = StatusAssinatura.CONFIRMADA,
        criadoEm = Instant.parse("2026-10-20T12:00:00Z"),
        atualizadoEm = Instant.parse("2026-10-20T12:00:00Z"),
    )

    private class FakeNotificador : NotificadorDeAssinatura {
        val postadas = mutableListOf<Pair<Assinatura, LocalDate>>()

        override fun podeNotificar(): Boolean = true

        override fun postar(assinatura: Assinatura, dataPrevista: LocalDate): Boolean {
            postadas += assinatura to dataPrevista
            return true
        }
    }

    private class FakePreferences(ativo: Boolean) : UserPreferencesDataSource {
        private val estado = MutableStateFlow(ativo)
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = estado

        override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
            estado.value = ativo
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev"

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

    private class FakeAssinaturaRepository(iniciais: List<Assinatura>) : AssinaturaRepository {
        private val fluxo = MutableStateFlow(iniciais)

        fun atualizar(novas: List<Assinatura>) {
            fluxo.value = novas
        }

        override fun observeAssinaturas(): Flow<List<Assinatura>> = fluxo

        override suspend fun buscar(publicId: String): Assinatura? = fluxo.value.firstOrNull { it.publicId == publicId }

        override suspend fun salvar(assinatura: Assinatura) = Unit

        override suspend fun salvarTodas(assinaturas: List<Assinatura>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
    }
}
