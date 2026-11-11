package com.pingo.feature.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun semAcessoMostraAvisoFixoDeCapturaParada() {
        var correcoes = 0

        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = HomeUiState(capturaAutomatica = false),
                    onOpenSettings = {},
                    onOpenCaptura = { correcoes++ },
                )
            }
        }

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações, o Pingo está no modo manual.").assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }

    @Test
    fun comAcessoAtivoNaoMostraOAviso() {
        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = HomeUiState(capturaAutomatica = true),
                    onOpenSettings = {},
                    onOpenCaptura = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Captura parada").assertCountEquals(0)
    }
}
