package com.pingo.inicio

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.ui.BloqueioDoAppScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloqueioDoAppOverlayTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exibeCamadaDeBloqueioEDisparaAcoesDeDesbloqueioESaida() {
        var desbloquear = 0
        var sair = 0

        composeRule.setContent {
            PingoTheme {
                BloqueioDoAppScreen(
                    mensagemErro = "Não foi possível validar.",
                    autenticando = false,
                    onDesbloquear = { desbloquear++ },
                    onSairDaConta = { sair++ },
                    onFecharApp = {},
                )
            }
        }

        composeRule.onNodeWithText("Pingo bloqueado").assertIsDisplayed()
        composeRule.onNodeWithText("Não foi possível validar.").assertIsDisplayed()
        composeRule.onNodeWithTag("botao_desbloquear_app").performClick()
        composeRule.onNodeWithTag("botao_sair_tela_bloqueio").performClick()

        assertEquals(1, desbloquear)
        assertEquals(1, sair)
    }
}
