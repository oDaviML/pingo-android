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
    fun mostraBoasVindasComoPrimeiroPasso() {
        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(),
                    onStart = {},
                    onLogin = {},
                    onToggleConsent = {},
                    onAcceptConsent = {},
                    onShowTerm = {},
                )
            }
        }

        composeRule.onNodeWithText("Passo 1 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Seu dinheiro,\ngota a gota").assertIsDisplayed()
        composeRule.onNodeWithText("Começar").assertIsDisplayed()
    }

    @Test
    fun mostraPassoDeLoginComIdentidadeEGuiaDePrivacidade() {
        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(currentStep = 2),
                    onStart = {},
                    onLogin = {},
                    onToggleConsent = {},
                    onAcceptConsent = {},
                    onShowTerm = {},
                )
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
                    state = OnboardingUiState(currentStep = 2, login = LoginUiState.Error),
                    onStart = {},
                    onLogin = { attempts++ },
                    onToggleConsent = {},
                    onAcceptConsent = {},
                    onShowTerm = {},
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
                    state = OnboardingUiState(currentStep = 2, login = LoginUiState.Loading),
                    onStart = {},
                    onLogin = { attempts++ },
                    onToggleConsent = {},
                    onAcceptConsent = {},
                    onShowTerm = {},
                )
            }
        }

        composeRule.onNodeWithText("Continuar com Google").performClick()

        assertEquals(0, attempts)
    }

    @Test
    fun mostraTermoLgpdNoPassoTres() {
        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(currentStep = 3),
                    onStart = {},
                    onLogin = {},
                    onToggleConsent = {},
                    onAcceptConsent = {},
                    onShowTerm = {},
                )
            }
        }

        composeRule.onNodeWithText("Passo 3 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Antes de começar").assertIsDisplayed()
        composeRule.onNodeWithText("O que o Pingo lê").assertIsDisplayed()
        composeRule.onNodeWithText("Li e aceito o termo de privacidade (LGPD)").assertIsDisplayed()
    }

    @Test
    fun aceitarSoFicaDisponivelComOConsentimentoMarcado() {
        var accepted = 0

        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(currentStep = 3),
                    onStart = {},
                    onLogin = {},
                    onToggleConsent = {},
                    onAcceptConsent = { accepted++ },
                    onShowTerm = {},
                )
            }
        }

        composeRule.onNodeWithText("Aceitar e continuar").performClick()

        assertEquals(0, accepted)
    }
}
