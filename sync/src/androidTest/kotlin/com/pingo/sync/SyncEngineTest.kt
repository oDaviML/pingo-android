package com.pingo.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.network.ApiProfile
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.sync.ContaSnapshotDto
import com.pingo.core.network.sync.LancamentoChangeDto
import com.pingo.core.network.sync.LancamentoSnapshotDto
import com.pingo.core.network.sync.PublicIdAliasDto
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SyncEngineTest {
    private val init = Instant.parse("2026-10-23T10:00:00Z")

    private lateinit var database: PingoDatabase
    private lateinit var api: FakeApi
    private lateinit var preferences: FakePreferences
    private lateinit var engine: SyncEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PingoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeApi()
        preferences = FakePreferences()
        engine = SyncEngine(api, database, preferences, FakeAuth(), DeviceActivator(api, preferences))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `pull aplica mudancas do servidor no banco local`() = runTest {
        api.pullResult = PullChangesResult(
            proximoCursor = "cursor-1",
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init)),
        )

        engine.run()

        val conta = database.contaDao().buscar("conta-1")
        assertNotNull(conta)
        assertEquals("Conta 1", conta?.apelido)
        assertEquals("cursor-1", preferences.pullCursor())
    }

    @Test
    fun `pull preserva alteracao local mais nova`() = runTest {
        val nova = Instant.parse("2026-10-23T12:00:00Z")
        database.contaDao().upsert(
            ContaEntity(
                publicId = "conta-1",
                instituicao = InstituicaoFinanceira.INTER,
                apelido = "Local",
                tipo = TipoConta.CORRENTE,
                arquivada = false,
                criadoEm = nova,
                atualizadoEm = nova,
                excluidoEm = null,
            ),
        )
        api.pullResult = PullChangesResult(
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init, apelido = "Servidor")),
        )

        engine.run()

        assertEquals("Local", database.contaDao().buscar("conta-1")?.apelido)
    }

    @Test
    fun `push envia mudancas locais e avanca o cursor`() = runTest {
        database.contaDao().upsert(
            ContaEntity(
                publicId = "conta-1",
                instituicao = InstituicaoFinanceira.NUBANK,
                apelido = "Nubank",
                tipo = TipoConta.CORRENTE,
                arquivada = false,
                criadoEm = init,
                atualizadoEm = init,
                excluidoEm = null,
            ),
        )

        engine.run()

        assertEquals(1, api.pushed?.contas?.size)
        assertEquals("Nubank", api.pushed?.contas?.first()?.instituicao)
        assertNotNull(preferences.pushCursor())
        assertEquals(SyncStatus.IDLE, preferences.status)
        assertNotNull(preferences.lastSyncAt())
    }

    @Test
    fun `aliases apontam o duplicado para o dono e removem a duplicata`() = runTest {
        val conta = contaEntity("conta-1")
        database.contaDao().upsert(conta)
        database.instrumentoDao().upsert(instrumentoEntity("instrumento-1", "conta-1"))
        database.lancamentoDao().upsert(lancamentoEntity("dono", estornoDe = null))
        database.lancamentoDao().upsert(lancamentoEntity("duplicado", estornoDe = null))
        api.pushResult = PushChangesResult(aliases = listOf(PublicIdAliasDto("duplicado", "dono")))

        engine.run()

        assertNull(database.lancamentoDao().buscar("duplicado"))
        assertNotNull(database.lancamentoDao().buscar("dono"))
    }

    @Test
    fun `falha da api marca status failed e preserva os dados locais`() = runTest {
        database.contaDao().upsert(contaEntity("conta-1"))
        api.failure = IOException("sem rede")

        runCatching { engine.run() }

        assertEquals(SyncStatus.FAILED, preferences.status)
        assertNotNull(database.contaDao().buscar("conta-1"))
    }

    @Test
    fun `falha no push mantem o cursor e reenvia no retry`() = runTest {
        database.contaDao().upsert(contaEntity("conta-1"))
        api.pushFailure = IOException("sem rede")

        runCatching { engine.run() }

        assertNull(preferences.pushCursorValue)
        assertEquals(0, api.pushCalls)

        api.pushFailure = null
        engine.run()

        assertEquals(1, api.pushCalls)
        assertEquals(1, api.pushed?.contas?.size)
        assertNotNull(preferences.pushCursorValue)
    }

    @Test
    fun `sem sessao nao sincroniza`() = runTest {
        val semSessao = SyncEngine(api, database, preferences, FakeAuth(SessionState.LoggedOut), DeviceActivator(api, preferences))

        semSessao.run()

        assertEquals(0, api.pullCalls)
        assertEquals(0, api.pushCalls)
    }

    @Test
    fun `ativa o aparelho antes de restaurar e sincronizar`() = runTest {
        api.pullResult = PullChangesResult(
            proximoCursor = "cursor-1",
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init)),
        )

        engine.run()

        assertEquals(1, api.activateCalls)
        assertEquals(preferences.deviceId(), api.activatedDeviceId)
    }

    @Test
    fun `ativa o aparelho apenas uma vez por dispositivo`() = runTest {
        engine.run()
        engine.run()

        assertEquals(1, api.activateCalls)

        DeviceActivator(api, preferences).activate()

        assertEquals(1, api.activateCalls)
    }

    @Test
    fun `restauracao completa aplica contas instrumentos e lancamentos na ordem certa`() = runTest {
        api.pullResult = PullChangesResult(
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init)),
            instrumentos = listOf(
                com.pingo.core.network.sync.InstrumentoSnapshotDto(
                    publicId = "instrumento-1",
                    contaPublicId = "conta-1",
                    tipo = "Debito",
                    apelido = "Conta",
                    arquivado = false,
                    criadoEm = init.toString(),
                    atualizadoEm = init.toString(),
                ),
            ),
            lancamentos = listOf(
                com.pingo.core.network.sync.LancamentoSnapshotDto(
                    publicId = "lancamento-1",
                    instrumentoPublicId = "instrumento-1",
                    valor = 42.9,
                    dataHora = init.toString(),
                    tipo = "Despesa",
                    status = "Confirmado",
                    origem = "Manual",
                    confianca = 1.0,
                    criadoEm = init.toString(),
                    atualizadoEm = init.toString(),
                ),
            ),
        )

        engine.run()

        assertNotNull(database.contaDao().buscar("conta-1"))
        assertNotNull(database.instrumentoDao().buscar("instrumento-1"))
        assertEquals(4290L, database.lancamentoDao().buscar("lancamento-1")?.valorCentavos)
        assertEquals(0, api.pushCalls)
    }

    @Test
    fun `pull aplica tombstone do servidor`() = runTest {
        api.pullResult = PullChangesResult(
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init).copy(excluidoEm = init.toString())),
        )

        engine.run()

        assertNotNull(database.contaDao().buscar("conta-1")?.excluidoEm)
    }

    private fun contaSnapshot(publicId: String, atualizadoEm: Instant, apelido: String = "Conta 1") = ContaSnapshotDto(
        publicId = publicId,
        instituicao = "Nubank",
        apelido = apelido,
        tipo = "Corrente",
        arquivada = false,
        criadoEm = init.toString(),
        atualizadoEm = atualizadoEm.toString(),
    )

    private fun contaEntity(publicId: String) = ContaEntity(
        publicId = publicId,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank",
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = init,
        atualizadoEm = init,
        excluidoEm = null,
    )

    private fun instrumentoEntity(publicId: String, contaPublicId: String) = InstrumentoEntity(
        publicId = publicId,
        contaPublicId = contaPublicId,
        tipo = TipoInstrumento.DEBITO,
        apelido = "Conta",
        ultimosDigitos = null,
        arquivado = false,
        fechamentoDia = null,
        vencimentoDia = null,
        beneficio = null,
        saldoCentavos = 0,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = init,
        atualizadoEm = init,
        excluidoEm = null,
    )

    private fun lancamentoEntity(publicId: String, estornoDe: String?) = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = "instrumento-1",
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = estornoDe,
        transferenciaId = null,
        valorCentavos = 1000,
        dataHora = init,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Loja",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = init,
        atualizadoEm = init,
        excluidoEm = null,
    )

    private class FakeApi : PingoApiClient {
        var pullResult = PullChangesResult(servidorEm = Instant.EPOCH.toString())
        var pushResult = PushChangesResult()
        var failure: Exception? = null
        var pushFailure: Exception? = null
        var activateFailure: Exception? = null
        var pushed: PushChangesPayload? = null
        var activatedDeviceId: String? = null
        var pullCalls = 0
        var pushCalls = 0
        var activateCalls = 0

        override suspend fun healthCheck(): Boolean = true

        override suspend fun getProfile(): ApiProfile = error("nao usado")

        override suspend fun activateDevice(deviceId: String, deviceName: String?): com.pingo.core.network.ApiDevice {
            (activateFailure ?: failure)?.let { throw it }
            activateCalls++
            activatedDeviceId = deviceId
            return com.pingo.core.network.ApiDevice(deviceId, deviceName, Instant.EPOCH.toString())
        }

        override suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult {
            (pushFailure ?: failure)?.let { throw it }
            pushCalls++
            pushed = payload
            return pushResult
        }

        override suspend fun pullChanges(cursor: String?): PullChangesResult {
            failure?.let { throw it }
            pullCalls++
            return pullResult
        }
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val statusFlow = MutableStateFlow(SyncStatus.IDLE)
        var status: SyncStatus
            get() = statusFlow.value
            private set(value) {
                statusFlow.value = value
            }
        private var pull: String? = null
        private var push: Instant? = null
        private var last: Instant? = null
        private var activated: String? = null

        override val syncStatus: Flow<SyncStatus> = statusFlow

        override suspend fun setSyncStatus(status: SyncStatus) {
            this.status = status
        }

        override suspend fun pullCursor(): String? = pull

        override suspend fun setPullCursor(cursor: String?) {
            pull = cursor
        }

        override suspend fun pushCursor(): Instant? = push

        val pushCursorValue: Instant?
            get() = push

        override suspend fun setPushCursor(instant: Instant) {
            push = instant
        }

        override suspend fun lastSyncAt(): Instant? = last

        override suspend fun setLastSyncAt(instant: Instant) {
            last = instant
        }

        override suspend fun activatedDeviceId(): String? = activated

        override suspend fun setActivatedDeviceId(deviceId: String) {
            activated = deviceId
        }

        override suspend fun deviceId(): String = "device-test"
    }

    private class FakeAuth(private val state: SessionState = SessionState.LoggedIn) : AuthRepository {
        override val session = MutableStateFlow(state)

        override suspend fun loginWithGoogle(activity: android.app.Activity): LoginResult = LoginResult.Success

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = "token"
    }
}
