package com.pingo.sync

import android.app.Activity
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.common.AppDispatchers
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.categoria.CategoriaSeeder
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.network.HttpPingoApiClient
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiConfig
import com.pingo.core.network.sync.jsonDeSync
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.ConexaoDoListener
import com.pingo.core.notifications.EscopoDeCaptura
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BetaE2EInstituicoesETrocaAparelhoTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val host: String? = InstrumentationRegistry.getArguments().getString(ARG_HOST)
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val capturaDataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val acessoNotificacoes = AcessoNotificacoesDoSistema(contexto)
    private val capturaController = CapturaControllerImpl(
        capturaDataSource = capturaDataSource,
        acessoNotificacoes = acessoNotificacoes,
        rebindDeListener = RebindDeListenerDoSistema(contexto),
        servicoDeCaptura = ServicoDeCapturaDoAndroid(contexto),
        conexaoDoListener = ConexaoDoListener(),
        escopoDeCaptura = EscopoDeCaptura(DefaultAppDispatchers),
        clock = Clock.systemDefaultZone(),
    )

    @After
    fun limparBanco() {
        runBlocking {
            captura.database().clearAllTables()
            capturaDataSource.clear()
        }
    }

    @Test
    fun capturaCincoBancosEPixSincronizaERestauraEmTrocaDeAparelhoSemPerdas() = runBlocking {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        withTimeout(TEMPO_TOTAL) {
            val token = "auth0|beta-${UUID.randomUUID()}"
            val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth(token), DispatchersReais)
            assertTrue("host inacessivel em $host", api.healthCheck())

            val bancoA = captura.database()
            bancoA.clearAllTables()
            val regraId = UUID.randomUUID().toString()
            semearCincoContasECategoria(regraId)
            garantirListenerConectado()

            val esperados = enviarNotificacoesCincoBancos()
            assertEquals(esperados.size, esperados.count { it.textoBruto?.isNotBlank() == true })

            val prefsA = FakePreferences()
            motor(api, bancoA, prefsA, token).run()
            verificarServidorSemTextoBruto(api, esperados)

            val bancoB = bancoEmMemoria()
            try {
                val prefsB = FakePreferences()
                assertTrue(prefsA.deviceId() != prefsB.deviceId())
                motor(api, bancoB, prefsB, token).run()
                verificarRestauracaoCompleta(bancoB, esperados, regraId)
            } finally {
                bancoB.close()
            }
        }
    }

    private suspend fun semearCincoContasECategoria(regraId: String) {
        val agora = Instant.now()
        captura.contaRepository().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(TipoInstrumento.CREDITO, "Roxinho", "5678", 5, 12)),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(TipoInstrumento.CREDITO, "Black", "0809", 6, 15)),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.ITAU,
                    apelido = "Itaú",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(TipoInstrumento.CREDITO, "Click", "4640", 7, 17)),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.MERCADO_PAGO,
                    apelido = "Mercado Pago",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(TipoInstrumento.DEBITO, "Conta MP")),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.VOUCHER, "Caju Alim", beneficio = BeneficioVoucher.VA, saldoCentavos = 50_000L),
                    ),
                ),
            ),
            agora,
        )
        val categoria = (
            captura.categoriaRepository().criar(
                "Assinaturas Beta",
                TipoCategoria.DESPESA,
                agora,
            ) as ResultadoCategoria.Sucesso
            ).categoria
        RegraCategoriaRepositoryImpl(captura.database().regraCategoriaDao()).salvar(
            com.pingo.core.model.RegraCategoria(regraId, categoria.publicId, "padaria beta", agora, agora),
        )
    }

    private suspend fun enviarNotificacoesCincoBancos(): List<Lancamento> {
        val v = 10_000L + Math.floorMod(System.nanoTime(), 70_000L)
        return enviarNubankEInter(v) + enviarItauMercadoPagoECaju(v)
    }

    private suspend fun enviarNubankEInter(v: Long): List<Lancamento> = listOf(
        capturar(InstituicaoFinanceira.NUBANK, v + 1, "Compra aprovada", "nubank-1") {
            "Compra de R$ ${fmt(it)} APROVADA em PADARIA NUBANK para o cartão com final 5678."
        },
        capturar(InstituicaoFinanceira.NUBANK, v + 2, "Transferência recebida", "nubank-2") {
            "Você recebeu uma transferência de R$ ${fmt(it)} de CARLOS NUBANK."
        },
        capturar(InstituicaoFinanceira.INTER, v + 3, "Compra no crédito", "inter-1") {
            "Olá, Davi. Você acaba de comprar R$ ${fmt(it)} em FARMACIA INTER. A compra foi no crédito nacional, com o cartão final 0809."
        },
        capturar(InstituicaoFinanceira.INTER, v + 4, "Pix Automático realizado", "inter-2") {
            "Tudo certo com o pagamento de R$ ${fmt(it)} feito à EBANX INTER em 18/11/2026."
        },
        capturar(InstituicaoFinanceira.INTER, v + 5, "Pix recebido", "inter-3") {
            "MARIA INTER te enviou um Pix de R$ ${fmt(it)} creditado na sua conta final ***28074-0."
        },
    )

    private suspend fun enviarItauMercadoPagoECaju(v: Long): List<Lancamento> = listOf(
        capturar(InstituicaoFinanceira.ITAU, v + 6, "Sua compra foi aprovada", "itau-1") {
            "Olá! Compra aprovada de R$ ${fmt(it)} em SUPERMERCADO ITAU no dia 18/11 às 10:05 no cartão final 4640. Boas compras."
        },
        capturar(InstituicaoFinanceira.ITAU, v + 7, "Feito. Pix enviado", "itau-2") {
            "Você enviou R$ ${fmt(it)} para JOAO ITAU, CPF XXX.456.789-XX."
        },
        capturar(InstituicaoFinanceira.ITAU, v + 8, "Pix recebido", "itau-3") {
            "Você recebeu R$ ${fmt(it)} de PAULA ITAU, CPF XXX.456.789-XX."
        },
        capturar(InstituicaoFinanceira.MERCADO_PAGO, v + 9, "Você pagou UBER MERCADO", "mp-1") {
            "Debitamos R$ ${fmt(it)} da sua conta."
        },
        capturar(InstituicaoFinanceira.CAJU, v + 10, "Pagamento aprovado", "caju-1") {
            "Compra de R$ ${fmt(it)} em RESTAURANTE CAJU APROVADA no CRÉDITO, seu saldo em AUXÍLIO ALIMENTAÇÃO é R$ 320,00."
        },
        capturar(InstituicaoFinanceira.CAJU, v + 11, "Seu Caju caiu!", "caju-2") {
            "Oba! R$ ${fmt(it)} disponíveis no seu Caju. Aproveite!"
        },
    )

    private suspend fun capturar(
        instituicao: InstituicaoFinanceira,
        valorCentavos: Long,
        titulo: String,
        sufixoTag: String,
        corpo: (Long) -> String,
    ): Lancamento {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        val texto = corpo(valorCentavos)
        val tag = "pingo-teste-${instituicao.name}-$sufixoTag"

        while (System.currentTimeMillis() < limite) {
            postar(titulo, tag, texto)
            val achado = withTimeoutOrNull(INTERVALO_SONDAGEM) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lista -> lista.any { it.valorCentavos == valorCentavos && it.origem == OrigemLancamento.NOTIFICACAO } }
                    .first { it.valorCentavos == valorCentavos }
            }
            if (achado != null) {
                return achado
            }
        }
        throw AssertionError("Timeout aguardando captura de ${instituicao.name} ($valorCentavos)")
    }

    private suspend fun verificarServidorSemTextoBruto(api: HttpPingoApiClient, esperados: List<Lancamento>) {
        val pull = api.pullChanges(null)
        val json = jsonDeSync.encodeToString(pull)
        assertFalse(json.contains("PADARIA NUBANK") && json.contains("textoBruto"))
        esperados.forEach { local ->
            val remoto = pull.lancamentos.singleOrNull { it.publicId == local.publicId }
            assertNotNull("Faltando ${local.publicId} no servidor", remoto)
            assertEquals(local.valorCentavos, remoto?.valor?.let { (it * 100).roundToLong() })
        }
        assertFalse(json.contains("\"textoBruto\""))
    }

    private suspend fun verificarRestauracaoCompleta(bancoB: PingoDatabase, esperados: List<Lancamento>, regraId: String) {
        val contasB = bancoB.contaDao().observeAtivas().first()
        val instsB = bancoB.instrumentoDao().observeAtivos().first()
        val lancsB = bancoB.lancamentoDao().observeAtivos().first()
        val catsB = bancoB.categoriaDao().listarAtivas()
        val regraB = bancoB.regraCategoriaDao().buscar(regraId)

        assertEquals(5, contasB.size)
        assertEquals(
            InstituicaoFinanceira.entries.filter {
                it != InstituicaoFinanceira.OUTRA
            }.toSet(),
            contasB.map { it.instituicao }.toSet(),
        )
        assertEquals(5, instsB.size)
        assertEquals(esperados.map { it.publicId }.toSet(), lancsB.map { it.publicId }.toSet())
        assertTrue(catsB.any { it.nome == "Assinaturas Beta" })
        assertNotNull(regraB)
    }

    private suspend fun garantirListenerConectado() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        prepararListenerDeCaptura(::shell)
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite && !acessoNotificacoes.concedido()) {
            delay(100L)
        }
        capturaController.reconectar()
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_beta_e2e.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
    }

    private fun fmt(centavos: Long): String = "${centavos / 100},${(centavos % 100).toString().padStart(2, '0')}"

    private fun motor(api: PingoApiClient, banco: PingoDatabase, preferencias: FakePreferences, token: String) = SyncEngine(
        api = api,
        database = banco,
        preferences = preferencias,
        authRepository = FixtureAuth(token),
        deviceActivator = DeviceActivator(api, preferencias),
        categoriaSeeder = CategoriaSeeder(captura.categoriaRepository(), UsuarioAutenticado(FakeTokenStore())),
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

    private object DispatchersReais : AppDispatchers {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Main
    }

    private class FixtureAuth(private val token: String) : AuthRepository {
        override val session: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.LoggedIn)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Failure

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = token
    }

    private class FakeTokenStore : TokenStore {
        override suspend fun read(): StoredTokens? = null

        override suspend fun write(tokens: StoredTokens) = Unit

        override suspend fun clear() = Unit
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val status = MutableStateFlow(SyncStatus.IDLE)
        private var pull: String? = null
        private var push: Instant? = null
        private var last: Instant? = null
        private val dispositivo = UUID.randomUUID().toString()
        private var ativado: String? = null

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

        override suspend fun consentVersion(): String? = "2026-10"

        override suspend fun consentAt(): Instant? = Instant.parse("2026-10-01T00:00:00Z")

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = true

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }

    private companion object {
        const val ARG_HOST = "pingo.syncHost"
        const val PACOTE = "com.pingo"
        val TEMPO_TOTAL = 240.seconds
        const val TEMPO_ESPERA = 20_000L
        val INTERVALO_SONDAGEM = 2.seconds
    }
}
