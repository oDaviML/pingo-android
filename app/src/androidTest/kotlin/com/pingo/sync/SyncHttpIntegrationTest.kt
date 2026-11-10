package com.pingo.sync

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.categoria.CategoriaSeeder
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.network.HttpPingoApiClient
import com.pingo.core.network.PingoApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SyncHttpIntegrationTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val host: String? = InstrumentationRegistry.getArguments().getString(ARG_HOST)
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.IO

        override val default = Dispatchers.Default

        override val main = Dispatchers.Main
    }

    @Test
    fun pendenteSemInstrumentoSincronizaComOrigemSemTextoBrutoEResolucaoPosterior() = runTest(timeout = 120.seconds) {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())
        val sub = "auth0|sync-${UUID.randomUUID()}"
        val token = sub
        val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth(token), dispatchers)
        assertTrue("host de teste inacessível em $host", api.healthCheck())

        val fonte = bancoEmMemoria()
        val preferencias = FakePreferences()
        val engine = motor(api, fonte, preferencias, token)
        val publicId = UUID.randomUUID().toString()
        val hash = UUID.randomUUID().toString().replace("-", "")
        val agora = Instant.now()
        val textoLocal = "Olá FULANO, você acaba de comprar no débito no DL*99 99 o valor de R$ 37,90."

        fonte.lancamentoDao().upsert(lancamentoPendente(publicId, hash, agora, textoLocal))

        engine.run()

        val pendenteLocal = fonte.lancamentoDao().buscar(publicId)
        assertEquals(textoLocal, pendenteLocal?.textoBruto)
        assertNull(pendenteLocal?.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.INTER, pendenteLocal?.instituicaoOrigem)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, pendenteLocal?.motivoPendencia)

        val primeiroPull = api.pullChanges(null)
        val pendenteRemoto = primeiroPull.lancamentos.singleOrNull { it.publicId == publicId }
        assertEquals(publicId, pendenteRemoto?.publicId)
        assertNull(pendenteRemoto?.instrumentoPublicId)
        assertEquals("Pendente", pendenteRemoto?.status)
        assertEquals("Notificacao", pendenteRemoto?.origem)
        assertEquals("Inter", pendenteRemoto?.instituicaoOrigem)
        assertEquals("INSTRUMENTO_NAO_ENCONTRADO", pendenteRemoto?.motivoPendencia)
        assertNull(pendenteRemoto?.transferenciaId)

        engine.run()

        val reposicao = api.pullChanges(null).lancamentos.filter { it.publicId == publicId }
        assertEquals(1, reposicao.size)
        assertEquals(textoLocal, fonte.lancamentoDao().buscar(publicId)?.textoBruto)

        val restauracao = bancoEmMemoria()
        motor(api, restauracao, FakePreferences(), token).run()

        val restaurado = restauracao.lancamentoDao().buscar(publicId)
        assertEquals(publicId, restaurado?.publicId)
        assertNull(restaurado?.instrumentoPublicId)
        assertNull(restaurado?.textoBruto)
        assertEquals(InstituicaoFinanceira.INTER, restaurado?.instituicaoOrigem)
        assertEquals("INSTRUMENTO_NAO_ENCONTRADO", restaurado?.motivoPendencia)

        val resolvidoEm = Instant.now().plusSeconds(10)
        val contaPublicId = UUID.randomUUID().toString()
        val instrumentoPublicId = UUID.randomUUID().toString()
        val contas = ContaRepositoryImpl(fonte.contaDao(), fonte.instrumentoDao(), transacaoDe(fonte))
        contas.salvar(
            Conta(
                publicId = contaPublicId,
                instituicao = InstituicaoFinanceira.INTER,
                apelido = "Inter",
                tipo = TipoConta.CORRENTE,
                criadoEm = resolvidoEm,
                atualizadoEm = resolvidoEm,
            ),
        )
        InstrumentoRepositoryImpl(fonte.instrumentoDao()).salvar(
            Instrumento(
                publicId = instrumentoPublicId,
                contaPublicId = contaPublicId,
                tipo = TipoInstrumento.DEBITO,
                apelido = "Débito Inter",
                ultimosDigitos = "9999",
                criadoEm = resolvidoEm,
                atualizadoEm = resolvidoEm,
            ),
        )
        val pendente = fonte.lancamentoDao().buscar(publicId)!!
        LancamentoRepositoryImpl(fonte.lancamentoDao()).salvar(
            pendente.toModel().copy(
                instrumentoPublicId = instrumentoPublicId,
                status = StatusLancamento.CONFIRMADO,
                motivoPendencia = null,
                atualizadoEm = resolvidoEm,
            ),
        )

        engine.run()

        val resolvidoRemoto = api.pullChanges(null).lancamentos.single { it.publicId == publicId }
        assertEquals(instrumentoPublicId, resolvidoRemoto.instrumentoPublicId)
        assertEquals("Confirmado", resolvidoRemoto.status)
        assertEquals("Inter", resolvidoRemoto.instituicaoOrigem)
        assertEquals(textoLocal, fonte.lancamentoDao().buscar(publicId)?.textoBruto)
        assertEquals(instrumentoPublicId, fonte.lancamentoDao().buscar(publicId)?.instrumentoPublicId)
    }

    private fun motor(api: HttpPingoApiClient, banco: PingoDatabase, preferencias: FakePreferences, token: String) = SyncEngine(
        api = api,
        database = banco,
        preferences = preferencias,
        authRepository = FixtureAuth(token),
        deviceActivator = DeviceActivator(api, preferencias),
        categoriaSeeder = CategoriaSeeder(CategoriaRepositoryImpl(banco.categoriaDao()), UsuarioAutenticado(FakeTokenStore())),
    )

    private fun bancoEmMemoria(): PingoDatabase = Room.inMemoryDatabaseBuilder(contexto, PingoDatabase::class.java)
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            },
        )
        .build()

    private fun lancamentoPendente(publicId: String, hash: String, agora: Instant, texto: String) = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = null,
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = 3790,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "DL*99 99",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.INTER,
        confianca = 0.6,
        motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        hashDuplicata = hash,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
        textoBruto = texto,
    )

    private fun transacaoDe(banco: PingoDatabase) = object : TransacaoLocal {
        override suspend fun <T> executar(bloco: suspend () -> T): T = banco.withTransaction { bloco() }
    }

    private class FixtureAuth(private val token: String) : AuthRepository {
        override val session: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.LoggedIn)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Failure

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = token
    }

    private class FakeTokenStore : com.pingo.core.auth.TokenStore {
        override suspend fun read(): com.pingo.core.auth.StoredTokens? = null

        override suspend fun write(tokens: com.pingo.core.auth.StoredTokens) = Unit

        override suspend fun clear() = Unit
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val status = MutableStateFlow(SyncStatus.IDLE)
        private var pull: String? = null
        private var push: Instant? = null
        private var last: Instant? = null
        private val dispositivo = UUID.randomUUID().toString()
        private var ativado: String? = null
        private var consentimento = "2026-10"

        override val syncStatus: Flow<SyncStatus> = status

        override suspend fun setSyncStatus(status: SyncStatus) {
            this.status.value = status
        }

        override suspend fun pullCursor(): String? = pull

        override suspend fun setPullCursor(cursor: String?) {
            pull = cursor
        }

        override suspend fun pushCursor(): Instant? = push

        override suspend fun setPushCursor(instant: Instant) {
            push = instant
        }

        override suspend fun lastSyncAt(): Instant? = last

        override suspend fun setLastSyncAt(instant: Instant) {
            last = instant
        }

        override suspend fun deviceId(): String = dispositivo

        override suspend fun activatedDeviceId(): String? = ativado

        override suspend fun setActivatedDeviceId(deviceId: String) {
            ativado = deviceId
        }

        override suspend fun consentVersion(): String? = consentimento

        override suspend fun consentAt(): Instant? = Instant.parse("2026-10-01T00:00:00Z")

        override suspend fun setConsent(version: String, instant: Instant) {
            consentimento = version
        }

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun clear() {
            pull = null
            push = null
            last = null
            ativado = null
        }
    }

    private companion object {
        const val ARG_HOST = "pingo.syncHost"
    }
}
