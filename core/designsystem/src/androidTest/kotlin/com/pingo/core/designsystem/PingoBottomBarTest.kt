package com.pingo.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingoBottomBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersAllTabsAndAddDropWithAccessibleTouchTargets() {
        val selectedTabs = mutableListOf<AbaInferior>()
        var addClicked = 0

        composeRule.setContent {
            PingoTheme {
                PingoBottomBar(
                    selecionada = AbaInferior.INICIO,
                    onSelecionar = { selectedTabs += it },
                    onNovo = { addClicked++ },
                )
            }
        }

        composeRule.onNodeWithTag("aba-INICIO").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("aba-LANCAMENTOS").assertIsDisplayed()
        composeRule.onNodeWithTag("aba-CONTAS").assertIsDisplayed()
        composeRule.onNodeWithTag("aba-RELATORIOS").assertIsDisplayed()
        composeRule.onNodeWithTag("novo-lancamento")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        AbaInferior.entries.forEach { aba ->
            composeRule.onNodeWithTag("aba-${aba.name}").assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithText(aba.rotulo).assertIsDisplayed()
        }

        composeRule.onNodeWithTag("aba-CONTAS").performClick()
        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
        composeRule.onNodeWithContentDescription("Novo lançamento").performClick()

        assertEquals(listOf(AbaInferior.CONTAS, AbaInferior.RELATORIOS), selectedTabs)
        assertEquals(1, addClicked)
    }
}
