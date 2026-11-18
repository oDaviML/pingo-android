package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsentimentoCheckboxAcessibilidadeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun checkboxDoTermoLgpdTemAlvoDeToqueDePeloMenosQuarentaEOitoDp() {
        var alternou = false

        composeRule.setContent {
            PingoTheme {
                OnboardingContent(
                    state = OnboardingUiState(currentStep = 3, consentChecked = false),
                    onStart = {},
                    onLogin = {},
                    onToggleConsent = { alternou = it },
                    onAcceptConsent = {},
                    onShowTerm = {},
                )
            }
        }

        val checkbox = composeRule.onNode(isToggleable())
        checkbox.assertIsToggleable()
        checkbox.assertHeightIsAtLeast(48.dp)
        checkbox.assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Li e aceito o termo de privacidade (LGPD)").performClick()

        assertTrue(alternou)
    }
}
