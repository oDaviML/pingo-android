package com.pingo.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.InstitutionAvatar
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoCaptureStatus
import com.pingo.core.designsystem.component.PingoDropAnimation
import com.pingo.core.designsystem.component.PingoHomeTopBar
import com.pingo.core.designsystem.component.PingoInstitution
import com.pingo.core.designsystem.component.PingoLargeTopBar
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoModalTopBar
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingoComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun topBarVariantsAndSectionHeaderTriggerActions() {
        var notifications = false
        var settings = false
        var saved = false
        var closed = false
        var seeAll = false

        composeRule.setContent {
            PingoTheme {
                androidx.compose.foundation.layout.Column {
                    PingoHomeTopBar(
                        greeting = "Boa tarde,",
                        name = "Lucas",
                        hasUnreadNotifications = true,
                        onNotificationsClick = { notifications = true },
                        onSettingsClick = { settings = true },
                    )
                    PingoLargeTopBar(title = "Lançamentos")
                    PingoModalTopBar(
                        title = "Novo lançamento",
                        onClose = { closed = true },
                        actionLabel = "Salvar",
                        onAction = { saved = true },
                    )
                    SectionHeader(
                        title = "Últimos lançamentos",
                        actionLabel = "Ver tudo",
                        onAction = { seeAll = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Boa tarde,").assertIsDisplayed()
        composeRule.onNodeWithText("Lucas").assertIsDisplayed()
        composeRule.onNodeWithText("Lançamentos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Notificações").performClick()
        composeRule.onNodeWithContentDescription("Configurações").performClick()
        composeRule.onNodeWithContentDescription("Fechar").performClick()
        composeRule.onNodeWithText("Salvar").performClick()
        composeRule.onNodeWithText("Ver tudo").performClick()

        assertTrue(notifications)
        assertTrue(settings)
        assertTrue(closed)
        assertTrue(saved)
        assertTrue(seeAll)
    }

    @Test
    fun prototypeFeedbackAvatarsAndAnimationsRenderCleanly() {
        var finished = false

        composeRule.setContent {
            PingoTheme {
                androidx.compose.foundation.layout.Column {
                    InstitutionAvatar(institution = PingoInstitution.Nubank)
                    InstitutionAvatar(institution = PingoInstitution.Outra)
                    CategoryAvatar(icon = PingoIcons.Utensils, contentDescription = "Alimentação")
                    PingoStatusTag(status = PingoCaptureStatus.Active)
                    PingoLinearProgress(progress = 0.6f)
                    PingoBanner(
                        title = "3 lançamentos para revisar",
                        description = "Toque para conferir.",
                        tone = PingoBannerTone.Info,
                    )
                    PingoDropAnimation(
                        trigger = true,
                        reducedMotion = true,
                        onFinished = { finished = true },
                    ) {
                        PingoText(text = "Padaria do Zé")
                    }
                    PingoSkeletonList(count = 2)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("N").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Alimentação").assertIsDisplayed()
        composeRule.onNodeWithText("Captura ativa").assertIsDisplayed()
        composeRule.onNodeWithText("3 lançamentos para revisar").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        assertTrue(finished)
    }

    @Test
    fun bottomSheetRendersTitleSubtitleAndContent() {
        composeRule.setContent {
            PingoTheme {
                PingoBottomSheet(
                    onDismissRequest = {},
                    title = "Trocar categoria",
                    subtitle = "Padaria do Zé",
                ) {
                    PingoText(text = "Opções")
                }
            }
        }

        composeRule.onNodeWithText("Trocar categoria").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onNodeWithText("Opções").assertIsDisplayed()
    }
}
