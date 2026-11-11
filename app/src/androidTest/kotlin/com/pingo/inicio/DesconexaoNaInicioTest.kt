package com.pingo.inicio

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.notifications.di.CapturaStatusEntryPoint
import com.pingo.feature.capturestatus.api.CaptureStatusRoute
import com.pingo.feature.capturestatus.impl.AtualizacaoDeRegras
import com.pingo.feature.capturestatus.impl.CaptureStatusScreen
import com.pingo.feature.capturestatus.impl.CaptureStatusViewModel
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.home.impl.HomeContent
import com.pingo.feature.home.impl.HomeViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DesconexaoNaInicioTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val injecao: CapturaStatusEntryPoint =
        EntryPointAccessors.fromApplication(contexto, CapturaStatusEntryPoint::class.java)
    private val captura: CapturaEntryPoint =
        EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val dataSource: CapturaDataSource = injecao.capturaDataSource()
    private val acesso: AcessoNotificacoes = AcessoNotificacoesDoSistema(contexto)
    private val controller: CapturaController = injecao.capturaController()
    private val clock: Clock = Clock.systemDefaultZone()

    @Before
    fun preparaOListener() {
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
        shell("cmd notification allow_listener $COMPONENTE")
        aguardarAcessoConcedido()
        runBlocking { controller.reconectar() }
        aguardarEstado(EstadoListener.ATIVO)
    }

    @After
    fun limpaOEstadoDaCaptura() {
        shell("cmd notification allow_listener $COMPONENTE")
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
        runBlocking { controller.reconectar() }
    }

    @Test
    fun desconexaoComAcessoValidoELeituraRecenteApareceNaInicioEAbreOT10() {
        val agora = Instant.now()
        runBlocking {
            dataSource.registrarAtivo(agora.minus(Duration.ofDays(2)))
            dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.minus(Duration.ofMinutes(30)))
            dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofMinutes(5)))
        }

        val inicio = HomeViewModel(
            acessoNotificacoes = acesso,
            saudeDaCaptura = MonitorDaCaptura(dataSource, clock),
            lancamentoRepository = captura.lancamentoRepository(),
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            clock = clock,
        )
        val status = modeloDoStatus()

        composeRule.setContent {
            PingoTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = HomeRoute.ROUTE) {
                    composable(HomeRoute.ROUTE) {
                        val state by inicio.uiState.collectAsStateWithLifecycle()
                        HomeContent(
                            state = state,
                            onOpenSettings = {},
                            onOpenCaptura = { navController.navigate(CaptureStatusRoute.ROUTE) },
                            onOpenRevisao = {},
                            onOpenNovo = {},
                            onMesAnterior = inicio::mesAnterior,
                            onMesProximo = inicio::mesProximo,
                        )
                    }
                    composable(CaptureStatusRoute.ROUTE) {
                        CaptureStatusScreen(onBack = { navController.popBackStack() }, viewModel = status)
                    }
                }
            }
        }

        composeRule.waitUntil(TEMPO_ESPERA) {
            !inicio.uiState.value.carregando && inicio.uiState.value.capturaParada
        }
        val state = inicio.uiState.value
        assertTrue(state.capturaAutomatica)
        assertFalse(state.semLeituraHa48h)
        assertEquals(SituacaoDaCaptura.PARADA, state.situacao)
        assertEquals(MotivoParada.DESCONECTADO, state.motivo)

        composeRule.onNodeWithText("Captura parada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("O Android encerrou o serviço de captura. Toque em Corrigir para reconectar.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Nenhuma notificação lida há 2 dias", substring = true).assertCountEquals(0)
        salvarCaptura("inicio-desconectado-acesso-valido.png")

        composeRule.onNodeWithText("Corrigir").performScrollTo().performClick()

        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithText("Status da captura").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("O Android encerrou o serviço").assertIsDisplayed()
        composeRule.onAllNodesWithText("Sem leitura há 2 dias").assertCountEquals(0)
        salvarCaptura("status-desconectado-acesso-valido.png")
    }

    private fun modeloDoStatus(): CaptureStatusViewModel = CaptureStatusViewModel(
        capturaDataSource = dataSource,
        saudeDaCaptura = injecao.saudeDaCaptura(),
        regraParserRepository = injecao.regraParserRepository(),
        atualizacaoDeRegras = SemAtualizacao,
        capturaController = controller,
        acessoNotificacoes = injecao.acessoNotificacoes(),
        notificacoesDoSistema = injecao.notificacoesDoSistema(),
        bateriaDoAparelho = injecao.bateriaDoAparelho(),
        clock = clock,
    )

    private fun aguardarEstado(estado: EstadoListener, motivo: MotivoParada? = null) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.estadoListener.first { atual -> atual.estado == estado && atual.motivo == motivo }
        }
    }

    private fun aguardarAcessoConcedido() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            if (acesso.concedido()) {
                return
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("acesso às notificações não foi concedido")
    }

    private fun salvarCaptura(nome: String) {
        shell("screencap -p /sdcard/Download/$nome")
        val saida = shell("ls -l /sdcard/Download/$nome")
        assertTrue("screenshot não gerado: $nome ($saida)", saida.contains(nome))
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private object SemAtualizacao : AtualizacaoDeRegras {
        override suspend fun atualizar(): ResultadoAtualizacaoRegras = ResultadoAtualizacaoRegras.SemAlteracao(1)
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
    }
}
