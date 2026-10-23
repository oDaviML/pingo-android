package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mostraPassoDeLoginComIdentidadeEGuiaDePrivacidade() {
        composeRule.setContent {
            PingoTheme {
                OnboardingContent(state = OnboardingUiState(), onLogin = {}, onSkip = {})
            }
        }

        composeRule.onNodeWithText("Passo 2 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Entre com sua conta Google").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar com Google").assertIsDisplayed()
        composeRule.onNodeWithText("Usamos a conta só para sincronizar e restaurar.").assertIsDisplayed()
    }

    @Test
    fun mostraErroEofereceTentarDeNovo() {
        var attempts = 0

        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(login = LoginUiState.Error),
                    onLogin = { attempts++ },
                    onSkip = {},
                )
            }
        }

        composeRule.onNodeWithText("Não foi possível entrar, tente de novo").assertIsDisplayed()

        composeRule.onNodeWithText("Continuar com Google").performClick()

        assertEquals(1, attempts)
    }

    @Test
    fun naoDisparaLoginEnquantoAutentica() {
        var attempts = 0

        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(login = LoginUiState.Loading),
                    onLogin = { attempts++ },
                    onSkip = {},
                )
            }
        }

        composeRule.onNodeWithText("Continuar com Google").performClick()

        assertEquals(0, attempts)
    }
}
