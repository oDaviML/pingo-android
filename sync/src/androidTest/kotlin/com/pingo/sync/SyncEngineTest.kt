package com.pingo.sync

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.categoria.CategoriaSeeder
import com.pingo.core.data.impl.CategoriaRepositoryImpl
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
import com.pingo.core.network.sync.CategoriaSnapshotDto
import com.pingo.core.network.sync.ContaSnapshotDto
import com.pingo.core.network.sync.PublicIdAliasDto
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.util.Base64

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
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
        api = FakeApi()
        preferences = FakePreferences()
        engine = SyncEngine(api, database, preferences, FakeAuth(), DeviceActivator(api, preferences), seederDe(database, null))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pullAplicaMudancasDoServidorNoBancoLocal() = runTest {
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
    fun pullPreservaAlteracaoLocalMaisNova() = runTest {
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
    fun pushEnviaMudancasLocaisEAvancaOCursor() = runTest {
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
    fun aliasesApontamODuplicadoParaODonoERemovemADuplicata() = runTest {
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
    fun falhaDaApiMarcaStatusFailedEPreservaOsDadosLocais() = runTest {
        database.contaDao().upsert(contaEntity("conta-1"))
        api.failure = IOException("sem rede")

        runCatching { engine.run() }

        assertEquals(SyncStatus.FAILED, preferences.status)
        assertNotNull(database.contaDao().buscar("conta-1"))
    }

    @Test
    fun falhaNoPushMantemOCursorEReenviaNoRetry() = runTest {
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
    fun semSessaoNaoSincroniza() = runTest {
        val semSessao = SyncEngine(
            api,
            database,
            preferences,
            FakeAuth(SessionState.LoggedOut),
            DeviceActivator(api, preferences),
            seederDe(database, null),
        )

        semSessao.run()

        assertEquals(0, api.pullCalls)
        assertEquals(0, api.pushCalls)
    }

    @Test
    fun semConsentimentoNaoSincronizaNemAtivaOAparelho() = runTest {
        val semConsentimento = FakePreferences(consent = null)
        val engineSemConsentimento = SyncEngine(
            api,
            database,
            semConsentimento,
            FakeAuth(),
            DeviceActivator(api, semConsentimento),
            seederDe(database, null),
        )

        engineSemConsentimento.run()

        assertEquals(0, api.activateCalls)
        assertEquals(0, api.pullCalls)
        assertEquals(0, api.pushCalls)
    }

    @Test
    fun primeiroSyncSemeiaAsCategoriasPadraoDepoisDoPull() = runTest {
        engineDoUsuario().run()

        val categorias = database.categoriaDao().observeAtivas().first()
        assertEquals(15, categorias.size)
        assertEquals(15, categorias.count { it.padrao && !it.arquivada })
    }

    @Test
    fun seedNaoRecriaPadraoQueOServidorJaRenomeou() = runTest {
        engineDoUsuario().run()
        val lazer = database.categoriaDao().observeAtivas().first().single { it.nome == "Lazer" }
        database.clearAllTables()
        api.pullResult = PullChangesResult(
            servidorEm = init.toString(),
            categorias = listOf(categoriaSnapshot(publicId = lazer.publicId, nome = "Diversão")),
        )

        engineDoUsuario().run()

        val categorias = database.categoriaDao().observeAtivas().first()
        assertEquals(15, categorias.size)
        assertEquals("Diversão", database.categoriaDao().buscar(lazer.publicId)?.nome)
        assertEquals(0, categorias.count { it.nome == "Lazer" })
    }

    @Test
    fun ativaOAparelhoAntesDeRestaurarESincronizar() = runTest {
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
    fun ativaOAparelhoApenasUmaVezPorDispositivo() = runTest {
        engine.run()
        engine.run()

        assertEquals(1, api.activateCalls)

        DeviceActivator(api, preferences).activate()

        assertEquals(1, api.activateCalls)
    }

    @Test
    fun restauracaoCompletaAplicaContasInstrumentosELancamentosNaOrdemCerta() = runTest {
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
    fun restauracaoInterrompidaPelaRedeRetomaSemDuplicarDados() = runTest {
        api.pullResult = PullChangesResult(
            proximoCursor = "cursor-1",
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init)),
        )
        api.failure = IOException("sem rede")

        runCatching { engine.run() }

        assertNull(preferences.pullCursor())
        assertNull(database.contaDao().buscar("conta-1"))

        api.failure = null
        engine.run()

        assertEquals("cursor-1", preferences.pullCursor())
        assertEquals(1, api.pullCalls)
        assertNotNull(database.contaDao().buscar("conta-1"))
    }

    @Test
    fun falhaNaAtivacaoNaoImpedeASincronizacao() = runTest {
        api.activateFailure = IOException("sem rede")
        api.pullResult = PullChangesResult(
            proximoCursor = "cursor-1",
            servidorEm = init.toString(),
            contas = listOf(contaSnapshot("conta-1", atualizadoEm = init)),
        )

        engine.run()

        assertEquals(1, api.activateCalls)
        assertNull(preferences.activatedDeviceId())
        assertEquals("cursor-1", preferences.pullCursor())
        assertEquals(SyncStatus.IDLE, preferences.status)
    }

    @Test
    fun pullAplicaTombstoneDoServidor() = runTest {
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

    private fun engineDoUsuario(): SyncEngine = SyncEngine(
        api,
        database,
        preferences,
        FakeAuth(),
        DeviceActivator(api, preferences),
        seederDe(database, tokensDo("auth0|lucas")),
    )

    private fun seederDe(database: PingoDatabase, tokens: StoredTokens?): CategoriaSeeder = CategoriaSeeder(
        CategoriaRepositoryImpl(database.categoriaDao()),
        UsuarioAutenticado(FakeTokenStore(tokens)),
    )

    private fun tokensDo(sub: String): StoredTokens = StoredTokens(
        accessToken = jwtComSub(sub),
        refreshToken = null,
        expiresAtEpochMillis = Long.MAX_VALUE,
    )

    private fun jwtComSub(sub: String): String {
        val codificador = Base64.getUrlEncoder().withoutPadding()
        val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())
        val corpo = codificador.encodeToString("""{"sub":"$sub"}""".toByteArray())

        return "$cabecalho.$corpo.assinatura"
    }

    private fun categoriaSnapshot(publicId: String, nome: String): CategoriaSnapshotDto = CategoriaSnapshotDto(
        publicId = publicId,
        nome = nome,
        tipo = "Despesa",
        padrao = true,
        arquivada = false,
        criadoEm = init.toString(),
        atualizadoEm = init.toString(),
        excluidoEm = null,
    )

    private class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
        override suspend fun read(): StoredTokens? = tokens

        override suspend fun write(tokens: StoredTokens) {
            this.tokens = tokens
        }

        override suspend fun clear() {
            tokens = null
        }
    }

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
            activateCalls++
            (activateFailure ?: failure)?.let { throw it }
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

        override suspend fun recordConsent(version: String): com.pingo.core.network.ApiConsent =
            com.pingo.core.network.ApiConsent(consentedAt = Instant.EPOCH.toString(), version = version)

        override suspend fun deleteAccount() = Unit
    }

    private class FakePreferences(private val consent: String? = "2026-10") : UserPreferencesDataSource {
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

        override suspend fun consentVersion(): String? = consent

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeAuth(private val state: SessionState = SessionState.LoggedIn) : AuthRepository {
        override val session = MutableStateFlow(state)

        override suspend fun loginWithGoogle(activity: android.app.Activity): LoginResult = LoginResult.Success

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = "token"
    }
}
