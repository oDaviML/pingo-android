package com.pingo.status

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.impl.RegraParserRepositoryImpl
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.AlertaDeCaptura
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.network.ApiConsent
import com.pingo.core.network.ApiDevice
import com.pingo.core.network.ApiProfile
import com.pingo.core.network.HttpPingoApiClient
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiConfig
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.AtualizadorDeRegras
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.RegrasParaCaptura
import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.NotificacaoBruta
import com.pingo.core.parser.PadraoRegra
import com.pingo.core.parser.PadroesRegraCodec
import com.pingo.core.parser.RegrasParserSeed
import com.pingo.core.parser.ResultadoParse
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.feature.capturestatus.impl.AtualizacaoDeRegrasDoPingo
import com.pingo.feature.capturestatus.impl.CaptureStatusScreen
import com.pingo.feature.capturestatus.impl.CaptureStatusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class StatusDaCapturaRegrasTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val host: String? = InstrumentationRegistry.getArguments().getString(ARG_HOST)
    private val dispatchers = DefaultAppDispatchers

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun atualizarRegrasPelaTelaAplicaPadroesPublicadosSemNovoApk() {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        val banco = bancoEmMemoria()
        val repositorio = RegraParserRepositoryImpl(banco.regraParserDao())
        val regras = RegrasParaCaptura(repositorio)
        val captura = FakeCapturaDataSource()
        val api = HttpPingoApiClient(
            PingoApiConfig(host!!),
            FixtureAuth("status-regras-${System.nanoTime()}"),
            dispatchers,
        )
        val itauV1 = RegrasParserSeed.regras.single { it.instituicao == InstituicaoFinanceira.ITAU }

        try {
            runBlocking {
                assertTrue("host de teste inacessível em $host", api.healthCheck())
                publicarNoServidor(itauV2(itauV1))
            }

            val modelo = CaptureStatusViewModel(
                capturaDataSource = captura,
                saudeDaCaptura = MonitorDaCaptura(captura, Clock.systemDefaultZone()),
                regraParserRepository = repositorio,
                atualizacaoDeRegras = AtualizacaoDeRegrasDoPingo(
                    AtualizadorDeRegras(api, repositorio, regras, captura, Clock.systemUTC(), dispatchers),
                ),
                capturaController = ControladorSemEfeito,
                acessoNotificacoes = AcessoConcedido,
                notificacoesDoSistema = NotificacoesHabilitadas,
                bateriaDoAparelho = BateriaSemRestricao,
                clock = Clock.systemDefaultZone(),
            )

            composeRule.setContent {
                PingoTheme {
                    CaptureStatusScreen(onBack = {}, viewModel = modelo)
                }
            }

            aguardarTexto("Versão 1", substring = true)

            val antes = runBlocking {
                DefaultParserEngine().classificar(
                    NotificacaoBruta(texto = TEXTO_NOVO_DA_V2),
                    regras.versaoDe(InstituicaoFinanceira.ITAU),
                )
            }
            assertTrue("o texto novo da v2 não deveria ser entendido antes da atualização", antes is ResultadoParse.NaoEntendida)

            composeRule.onNodeWithText("Atualizar regras").performScrollTo().performClick()

            aguardarTexto("Regras atualizadas para a versão 2.")
            aguardarTexto("Versão 2", substring = true)

            val (versaoMaxima, versaoDoItau, transacao) = runBlocking {
                Triple(
                    banco.regraParserDao().versaoAtivaMaxima(),
                    banco.regraParserDao().observeAtivas().first()
                        .single { it.instituicao == InstituicaoFinanceira.ITAU }.versao,
                    DefaultParserEngine().classificar(
                        NotificacaoBruta(texto = TEXTO_NOVO_DA_V2),
                        regras.versaoDe(InstituicaoFinanceira.ITAU),
                    ),
                )
            }

            assertEquals(2, versaoMaxima)
            assertEquals(2, versaoDoItau)
            assertEquals(1234L, (transacao as ResultadoParse.Transacao).dados.valorCentavos)
        } finally {
            runBlocking { publicarNoServidor(itauV1) }
            banco.close()
        }
    }

    private fun aguardarTexto(texto: String, substring: Boolean = false) {
        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithText(texto, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun publicarNoServidor(regra: RegraParser) = withContext(Dispatchers.IO) {
        val conexao = URL("${host!!.trimEnd('/')}/test/parser-rules").openConnection() as HttpURLConnection

        try {
            conexao.requestMethod = "POST"
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", "application/json")
            conexao.setRequestProperty("Authorization", "Bearer publicacao-${System.nanoTime()}")
            val corpo = """{"instituicao":"${regra.instituicao.name}","versao":${regra.versao},"padroes":${regra.padroes}}"""
            conexao.outputStream.use { saida -> saida.write(corpo.toByteArray()) }
            assertEquals(204, conexao.responseCode)
        } finally {
            conexao.disconnect()
        }
    }

    private fun itauV2(regra: RegraParser): RegraParser = regra.copy(
        versao = 2,
        padroes = PadroesRegraCodec.codificar(
            PadroesRegraCodec.decodificar(regra.padroes) + PadraoRegra(
                id = "itau-pagamento-fora-do-padrao",
                tipoLancamento = TipoLancamentoPadrao.COMPRA,
                direcao = DirecaoPadrao.DESPESA,
                ancoras = listOf("Pagamento fora do padrão de R$"),
                extracoes = mapOf("valor" to "Pagamento fora do padrão de R$ {valor} em"),
            ),
        ),
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

    private object AcessoConcedido : AcessoNotificacoes {
        override fun concedido(): Boolean = true
    }

    private object NotificacoesHabilitadas : NotificacoesDoSistema {
        override fun habilitadas(): Boolean = true
    }

    private object BateriaSemRestricao : BateriaDoAparelho {
        override fun semRestricaoDeBateria(): Boolean = true
    }

    private object ControladorSemEfeito : CapturaController {
        override fun iniciarServico() = Unit

        override fun pararServico() = Unit

        override fun registrarConectado() = Unit

        override fun registrarDesconectado() = Unit

        override suspend fun reconectar(): EstadoListenerPersistido = EstadoListenerPersistido(EstadoListener.ATIVO, null, Instant.now())

        override suspend fun reconciliar(): EstadoListenerPersistido = EstadoListenerPersistido(EstadoListener.ATIVO, null, Instant.now())
    }

    private class FixtureAuth(private val token: String) : AuthRepository {
        override val session: StateFlow<SessionState> = MutableStateFlow<SessionState>(SessionState.LoggedIn)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Failure

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = token
    }

    private class FakeCapturaDataSource : CapturaDataSource {
        private val estado = MutableStateFlow(EstadoListenerPersistido(EstadoListener.ATIVO, null, Instant.now()))
        private val leituras = MutableStateFlow(mapOf(InstituicaoFinanceira.ITAU to Instant.now().minus(Duration.ofMinutes(5))))
        private val desde = MutableStateFlow<Instant?>(Instant.now().minus(Duration.ofDays(1)))
        private val alerta = MutableStateFlow<AlertaDeCaptura?>(null)
        private val regras = MutableStateFlow(EstadoAtualizacaoRegras())

        override val estadoListener: Flow<EstadoListenerPersistido> = estado.asStateFlow()

        override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = leituras.asStateFlow()

        override val observandoDesde: Flow<Instant?> = desde.asStateFlow()

        override val estadoAtualizacaoRegras: Flow<EstadoAtualizacaoRegras> = regras.asStateFlow()

        override val alertaDeCaptura: Flow<AlertaDeCaptura?> = alerta.asStateFlow()

        override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> =
            flowOf(ContadoresDoMes(mes = mes, reconhecidas = 7, pendentes = 2, ignoradas = 3, naoEntendidas = 1))

        override suspend fun registrarAtivo(em: Instant) {
            estado.value = EstadoListenerPersistido(EstadoListener.ATIVO, null, em)
        }

        override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
            estado.value = EstadoListenerPersistido(EstadoListener.PARADO, motivo, em)
        }

        override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) {
            leituras.value = leituras.value + (instituicao to em)
        }

        override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) = Unit

        override suspend fun registrarRegrasAtualizadas(em: Instant) {
            regras.value = EstadoAtualizacaoRegras(atualizadoEm = em)
        }

        override suspend fun registrarFalhaAoAtualizarRegras(motivo: String, em: Instant) {
            regras.value = EstadoAtualizacaoRegras(falhaEm = em, motivoFalha = motivo)
        }

        override suspend fun registrarAlertaDeCaptura(referencia: Instant, em: Instant) {
            alerta.value = AlertaDeCaptura(referencia, em)
        }

        override suspend fun clear() = Unit
    }

    private companion object {
        const val ARG_HOST = "pingo.syncHost"
        const val TEMPO_ESPERA = 30_000L
        const val TEXTO_NOVO_DA_V2 = "Pagamento fora do padrão de R$ 12,34 em 05/11"
    }
}
