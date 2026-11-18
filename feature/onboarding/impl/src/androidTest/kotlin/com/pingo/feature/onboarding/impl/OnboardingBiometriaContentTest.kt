package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingBiometriaContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exibePassoOitoComOpcoesDeAtivarOuAgoraNao() {
        var ativarCliques = 0
        var pularCliques = 0

        composeRule.setContent {
            PingoTheme {
                OnboardingBiometriaContent(
                    state = OnboardingBiometriaUiState(),
                    onAtivar = { ativarCliques++ },
                    onAgoraNao = { pularCliques++ },
                    onAbrirConfiguracoesDeSeguranca = {},
                )
            }
        }

        composeRule.onNodeWithText("Passo 8 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Proteja seus gastos com biometria").assertIsDisplayed()

        composeRule.onNodeWithTag("botao_ativar_biometria").performClick()
        composeRule.onNodeWithTag("botao_agora_nao_biometria").performClick()

        assertEquals(1, ativarCliques)
        assertEquals(1, pularCliques)
    }

    @Test
    fun mostraAtalhoDeConfiguracoesQuandoNaoHaCadastroNoAparelho() {
        var abriuConfiguracoes = 0

        composeRule.setContent {
            PingoTheme {
                OnboardingBiometriaContent(
                    state = OnboardingBiometriaUiState(
                        disponibilidade = DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO,
                        semCadastroNoAparelho = true,
                    ),
                    onAtivar = {},
                    onAgoraNao = {},
                    onAbrirConfiguracoesDeSeguranca = { abriuConfiguracoes++ },
                )
            }
        }

        composeRule.onNodeWithTag("botao_configurar_biometria_aparelho").performScrollTo().performClick()

        assertEquals(1, abriuConfiguracoes)
    }
}
