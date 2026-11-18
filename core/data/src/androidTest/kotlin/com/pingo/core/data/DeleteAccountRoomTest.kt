package com.pingo.core.data

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.impl.AccountRepositoryImpl
import com.pingo.core.data.impl.LimpezaLocalDeConta
import com.pingo.core.database.DatabaseKeyProvider
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.PingoDatabaseFactory
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.UsuarioEntity
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSourceImpl
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.network.ApiConsent
import com.pingo.core.network.ApiDevice
import com.pingo.core.network.ApiProfile
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiException
import com.pingo.core.network.rules.ParserRulesResponse
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DeleteAccountRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PingoDatabase
    private lateinit var preferences: UserPreferencesDataSourceImpl
    private lateinit var capturaDataSource: CapturaDataSourceImpl
    private lateinit var permissoesDataSource: PermissoesDataSourceImpl

    @Before
    fun setUp() {
        context.deleteDatabase(PingoDatabase.NAME)
        database = PingoDatabaseFactory.create(context, DatabaseKeyProvider(context))
        preferences = UserPreferencesDataSourceImpl(context)
        capturaDataSource = CapturaDataSourceImpl(context)
        permissoesDataSource = PermissoesDataSourceImpl(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            preferences.clear()
            capturaDataSource.clear()
            permissoesDataSource.clear()
        }
        database.close()
        context.deleteDatabase(PingoDatabase.NAME)
    }

    @Test
    fun falhaNaApiPreservaBancoCriptografadoPreferenciasESessao() = runBlocking {
        semearDadosLocais()
        val auth = FakeAuthRepository()
        val api = FakePingoApiClient(erro = PingoApiException(500, "Falha no servidor"))
        val analytics = FakeAnalyticsFacade(ativo = true)
        val repository = accountRepository(api, auth, analytics)

        val erro = runCatching { repository.deleteAccount() }.exceptionOrNull()

        assertTrue(erro is PingoApiException)
        assertTrue(api.deleteChamado)
        assertEquals(SessionState.LoggedIn, auth.session.value)
        assertTrue(analytics.ativo)
        assertEquals(1, database.contaDao().observeAtivas().first().size)
        assertEquals(1, database.lancamentoDao().observeAtivos().first().size)
        assertNotNull(preferences.consentVersion())
        assertTrue(preferences.biometriaAtiva.first())
    }

    @Test
    fun sucessoNaApiLimpaSqlCipherPreferenciasTelemetriaESessao() = runBlocking {
        semearDadosLocais()
        val deviceIdAnterior = preferences.deviceId()
        val auth = FakeAuthRepository()
        val api = FakePingoApiClient()
        val analytics = FakeAnalyticsFacade(ativo = true)
        val repository = accountRepository(api, auth, analytics)

        repository.deleteAccount()

        assertTrue(api.deleteChamado)
        assertEquals(SessionState.LoggedOut, auth.session.value)
        assertFalse(analytics.ativo)
        assertTrue(database.contaDao().observeAtivas().first().isEmpty())
        assertTrue(database.categoriaDao().listarAtivas().isEmpty())
        assertTrue(database.lancamentoDao().observeAtivos().first().isEmpty())
        assertNull(preferences.consentVersion())
        assertFalse(preferences.contasConfiguradas())
        assertFalse(preferences.biometriaAtiva.first())
        assertFalse(preferences.telemetriaOptIn.first())
        assertTrue(capturaDataSource.ultimaLeituraPorApp.first().isEmpty())
        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, permissoesDataSource.acessoAsNotificacoes.first())
        assertFalse(preferences.deviceId() == deviceIdAnterior)
    }

    @Test
    fun respostaNotFoundNaApiTambemConcluiLimpezaLocal() = runBlocking {
        semearDadosLocais()
        val auth = FakeAuthRepository()
        val api = FakePingoApiClient(erro = PingoApiException(404, "Já removida"))
        val analytics = FakeAnalyticsFacade(ativo = true)
        val repository = accountRepository(api, auth, analytics)

        repository.deleteAccount()

        assertEquals(SessionState.LoggedOut, auth.session.value)
        assertTrue(database.lancamentoDao().observeAtivos().first().isEmpty())
        assertFalse(analytics.ativo)
    }

    private fun accountRepository(
        api: FakePingoApiClient,
        auth: FakeAuthRepository,
        analytics: FakeAnalyticsFacade,
    ): AccountRepositoryImpl = AccountRepositoryImpl(
        api = api,
        authRepository = auth,
        preferences = preferences,
        limpezaLocal = LimpezaLocalDeConta(
            database = database,
            preferences = preferences,
            capturaDataSource = capturaDataSource,
            permissoesDataSource = permissoesDataSource,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(),
            analytics = analytics,
        ),
        dispatchers = DispatchersDeTeste,
    )

    private suspend fun semearDadosLocais() {
        val agora = Instant.parse("2026-11-18T12:00:00Z")
        database.usuarioDao().upsert(
            UsuarioEntity("u1", "auth0|1", "ana@pingo.app", "Ana", agora, "1.0", true, agora, "dev-1", "Pixel", agora, agora, agora, null),
        )
        database.contaDao().upsert(
            ContaEntity("conta-1", InstituicaoFinanceira.ITAU, "Itaú", TipoConta.CORRENTE, false, agora, agora, null),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-1",
                contaPublicId = "conta-1",
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão",
                ultimosDigitos = "1234",
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.categoriaDao().upsert(CategoriaEntity("cat-1", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null))
        database.lancamentoDao().upsert(lancamentoEntity(agora))
        semearPreferencias(agora)
    }

    private fun lancamentoEntity(agora: Instant): LancamentoEntity = LancamentoEntity(
        publicId = "lanc-1",
        instrumentoPublicId = "inst-1",
        categoriaPublicId = "cat-1",
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = 2500L,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Padaria",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.ITAU,
        confianca = 0.9,
        motivoPendencia = null,
        hashDuplicata = "hash-1",
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
        textoBruto = null,
    )

    private suspend fun semearPreferencias(agora: Instant) {
        preferences.clear()
        preferences.setConsent("1.0", agora)
        preferences.setContasConfiguradas(true)
        preferences.setBiometriaAtiva(true)
        preferences.setTelemetriaOptIn(true)
        preferences.setSyncStatus(SyncStatus.IDLE)
        preferences.deviceId()
        capturaDataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora)
        permissoesDataSource.registrarAcessoAsNotificacoes(PermissaoDoPingo.PERMITIDA)
    }

    private object DispatchersDeTeste : AppDispatchers {
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeAuthRepository : AuthRepository {
        private val mutableSession = MutableStateFlow(SessionState.LoggedIn)
        override val session: StateFlow<SessionState> = mutableSession

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Success

        override suspend fun logout() {
            mutableSession.value = SessionState.LoggedOut
        }

        override suspend fun accessToken(forceRefresh: Boolean): String? = "token"
    }

    private class FakeAnalyticsFacade(var ativo: Boolean) : AnalyticsFacade {
        override fun setAnalyticsEnabled(enabled: Boolean) {
            ativo = enabled
        }

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }

    private class FakePingoApiClient(private val erro: Throwable? = null) : PingoApiClient {
        var deleteChamado = false

        override suspend fun healthCheck(): Boolean = true

        override suspend fun getProfile(): ApiProfile = error("nao usado")

        override suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult = PushChangesResult()

        override suspend fun pullChanges(cursor: String?): PullChangesResult = PullChangesResult(servidorEm = "2026-11-18T12:00:00Z")

        override suspend fun getParserRules(): ParserRulesResponse = ParserRulesResponse(1, emptyList())

        override suspend fun activateDevice(deviceId: String, deviceName: String?): ApiDevice = ApiDevice(deviceId, deviceName, null)

        override suspend fun recordConsent(version: String): ApiConsent = ApiConsent(null, version)

        override suspend fun deleteAccount() {
            deleteChamado = true
            erro?.let { throw it }
        }
    }
}
