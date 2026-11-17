package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.FabricanteDoSistema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPermissoesContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun passo4ExplicaComoLiberarEContinuaSemBloquear() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(eventos)

        composeRule.onNodeWithText("Passo 4 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Libere o acesso às notificações").assertIsDisplayed()
        composeRule.onNodeWithText("É assim que o Pingo vê suas compras. O Android pede essa permissão em Ajustes.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Acesso a notificações").assertIsDisplayed()
        composeRule.onNodeWithText("Falta ativar o Pingo nos Ajustes do Android").assertIsDisplayed()
        composeRule.onNodeWithText("Toque em Abrir ajustes.").assertIsDisplayed()
        composeRule.onNodeWithText("Procure Pingo na lista de apps com acesso.").assertIsDisplayed()
        composeRule.onNodeWithText("Ative a chave e confirme o aviso do Android.").assertIsDisplayed()

        composeRule.onNodeWithText("Abrir ajustes").performClick()
        composeRule.onNodeWithText("Agora não").performClick()

        assertEquals(
            listOf(
                OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes,
                OnboardingPermissoesEvent.NegarAcesso,
            ),
            eventos,
        )
    }

    @Test
    fun passo4NegadoMostraModoManualComAvisoFixo() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(eventos, OnboardingPermissoesUiState(modoManual = true))

        composeRule.onNodeWithText("Tudo bem, você decide").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações").assertIsDisplayed()
        composeRule.onNodeWithText("O Pingo vai funcionar só no modo manual até você liberar.").assertIsDisplayed()
        composeRule.onNodeWithText("Sem isso o Pingo fica só no manual").assertIsDisplayed()
        composeRule.onNodeWithText("Você lança cada gasto").assertIsDisplayed()
        composeRule.onNodeWithText("Sem captura nem revisão").assertIsDisplayed()
        composeRule.onNodeWithText("Dá para ativar depois").assertIsDisplayed()

        composeRule.onNodeWithText("Tentar de novo").performClick()
        composeRule.onNodeWithText("Continuar só no manual").performClick()

        assertEquals(
            listOf(
                OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes,
                OnboardingPermissoesEvent.ContinuarSemAcesso,
            ),
            eventos,
        )
    }

    @Test
    fun passo4ConcedidoOfereceContinuar() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(eventos, OnboardingPermissoesUiState(acessoConcedido = true))

        composeRule.onNodeWithText("Tudo certo, o Pingo já pode ler as notificações dos bancos.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Abrir ajustes").assertCountEquals(0)

        composeRule.onNodeWithText("Continuar").performClick()

        assertEquals(listOf(OnboardingPermissoesEvent.ContinuarComAcesso), eventos)
    }

    @Test
    fun passo5MostraExemploEPedePermissaoParaNotificar() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(eventos, OnboardingPermissoesUiState(passo = PassoDePermissoes.NOTIFICACOES_DO_PINGO))

        composeRule.onNodeWithText("Passo 5 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Deixe o Pingo avisar você").assertIsDisplayed()
        composeRule.onNodeWithText("Revisar compra de R$ 42,90?").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé · Nubank crédito. Alimentação?").assertIsDisplayed()
        composeRule.onNodeWithText("Notificações do Pingo").assertIsDisplayed()
        composeRule.onNodeWithText("Para revisar compras pela notificação").assertIsDisplayed()

        composeRule.onNodeWithText("Permitir notificações").performClick()
        composeRule.onNodeWithText("Agora não").performClick()

        assertEquals(
            listOf(
                OnboardingPermissoesEvent.PedirNotificacoes,
                OnboardingPermissoesEvent.PularNotificacoes,
            ),
            eventos,
        )
    }

    @Test
    fun passo5ComPermissaoConcedidaSoOfereceContinuar() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(
            eventos,
            OnboardingPermissoesUiState(
                passo = PassoDePermissoes.NOTIFICACOES_DO_PINGO,
                notificacoesHabilitadas = true,
            ),
        )

        composeRule.onNodeWithText("Tudo certo, o Pingo pode avisar você.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Agora não").assertCountEquals(0)

        composeRule.onNodeWithText("Continuar").performClick()

        assertEquals(listOf(OnboardingPermissoesEvent.PedirNotificacoes), eventos)
    }

    @Test
    fun passo6DetectaXiaomiEPedeInicioAutomatico() {
        val eventos = mutableListOf<OnboardingPermissoesEvent>()
        mostrar(
            eventos,
            OnboardingPermissoesUiState(
                passo = PassoDePermissoes.BATERIA,
                fabricanteDetectado = FabricanteDoSistema.XIAOMI,
                fabricanteSelecionado = FabricanteDoSistema.XIAOMI,
            ),
        )

        composeRule.onNodeWithText("Passo 6 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Mantenha a captura ligada").assertIsDisplayed()
        composeRule.onNodeWithText("XIAOMI · MIUI E HYPEROS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ative Início automático.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem restrição de bateria").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Início automático").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("O texto lido não sai do aparelho. A leitura acontece aqui; nada do texto é enviado.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Samsung").performScrollTo().performClick()
        composeRule.onNodeWithText("Abrir ajustes de bateria").performClick()
        composeRule.onNodeWithText("Pular por enquanto").performClick()

        assertEquals(
            listOf(
                OnboardingPermissoesEvent.SelecionarFabricante(FabricanteDoSistema.SAMSUNG),
                OnboardingPermissoesEvent.AbrirAjustesDeBateria,
                OnboardingPermissoesEvent.Concluir,
            ),
            eventos,
        )
    }

    @Test
    fun passo6MostraAsInstrucoesEspecificasDaSamsung() {
        mostrar(
            mutableListOf(),
            OnboardingPermissoesUiState(
                passo = PassoDePermissoes.BATERIA,
                fabricanteSelecionado = FabricanteDoSistema.SAMSUNG,
            ),
        )

        composeRule.onNodeWithText("SAMSUNG · ONE UI").assertIsDisplayed()
        composeRule.onNodeWithText("Adicione o Pingo em Apps nunca suspensos.").assertIsDisplayed()

        assertTrue(
            composeRule.onAllNodesWithText("Corrigir").fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun passo6SemFabricanteConhecidoMostraInstrucoesGenericas() {
        mostrar(
            mutableListOf(),
            OnboardingPermissoesUiState(
                passo = PassoDePermissoes.BATERIA,
                fabricanteSelecionado = FabricanteDoSistema.OUTRO,
            ),
        )

        composeRule.onNodeWithText("INSTRUÇÕES GERAIS").assertIsDisplayed()
        composeRule.onNodeWithText("Se o aparelho tiver início automático, permita o Pingo iniciar sozinho.")
            .assertIsDisplayed()
    }

    private fun mostrar(
        eventos: MutableList<OnboardingPermissoesEvent>,
        state: OnboardingPermissoesUiState = OnboardingPermissoesUiState(),
    ) {
        composeRule.setContent {
            PingoTheme {
                OnboardingPermissoesContent(state = state, onEvent = { eventos.add(it) })
            }
        }
    }
}
