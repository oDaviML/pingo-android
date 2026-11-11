package com.pingo.feature.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
                    onOpenNovo = {},
                )
            }
        }

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações, o Pingo está no modo manual.").assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }

    @Test
    fun botaoNovoLancamentoDaInicioAbreOTerceiroPasso() {
        var aberturas = 0
        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = HomeUiState(capturaAutomatica = true),
                    onOpenSettings = {},
                    onOpenCaptura = {},
                    onOpenNovo = { aberturas++ },
                )
            }
        }

        composeRule.onNodeWithTag("novo-lancamento").performClick()

        assertEquals(1, aberturas)
    }

    @Test
    fun comAcessoAtivoNaoMostraOAviso() {
        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = HomeUiState(capturaAutomatica = true),
                    onOpenSettings = {},
                    onOpenCaptura = {},
                    onOpenNovo = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Captura parada").assertCountEquals(0)
    }

    @Test
    fun semLeituraHaDoisDiasMostraOAvisoDeCapturaParada() {
        var correcoes = 0

        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = HomeUiState(capturaAutomatica = true, semLeituraHa48h = true),
                    onOpenSettings = {},
                    onOpenCaptura = { correcoes++ },
                    onOpenNovo = {},
                )
            }
        }

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhuma notificação lida há 2 dias. O Pingo pode não estar vendo seus bancos.").assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }
}
