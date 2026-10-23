package com.pingo.feature.settings.impl

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
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mostraPrivacidadeEExclusaoDeConta() {
        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = {},
                )
            }
        }

        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeRule.onNodeWithText("Privacidade e dados").assertIsDisplayed()
        composeRule.onNodeWithText("Termo de privacidade").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir conta e dados").assertIsDisplayed()
    }

    @Test
    fun abreOConfirmacaoDeExclusao() {
        var deleteClicks = 0

        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = { deleteClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Excluir conta e dados").performClick()

        assertEquals(1, deleteClicks)
    }
}
