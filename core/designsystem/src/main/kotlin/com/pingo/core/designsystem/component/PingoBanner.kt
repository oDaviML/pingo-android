package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

enum class PingoBannerTone {
    Info,
    Warning,
    Error,
    Success,
}

data class PingoBannerColors(val container: Color, val content: Color, val defaultIcon: ImageVector)

@Composable
fun PingoBanner(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: PingoBannerTone = PingoBannerTone.Info,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    showTrailingChevron: Boolean = onClick != null,
) {
    val style = bannerColors(tone)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = if (pressed && onClick != null) style.container.copy(alpha = 0.85f) else style.container

    Row(
        modifier = bannerContainerModifier(
            modifier = modifier,
            containerColor = containerColor,
            interactionSource = interactionSource,
            onClick = onClick,
        ),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = icon ?: style.defaultIcon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = style.content,
        )
        BannerTexts(title = title, description = description, color = style.content, modifier = Modifier.weight(1f))
        if (showTrailingChevron) {
            PingoIcon(
                imageVector = PingoIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = style.content,
            )
        }
    }
}

private fun bannerContainerModifier(
    modifier: Modifier,
    containerColor: Color,
    interactionSource: MutableInteractionSource,
    onClick: (() -> Unit)?,
): Modifier {
    val shape = RoundedCornerShape(PingoRadius.lg)
    return modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.heightIn(min = PingoLayout.minTouchTarget) else Modifier)
        .clip(shape)
        .background(color = containerColor, shape = shape)
        .then(
            if (onClick == null) {
                Modifier
            } else {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
            },
        )
        .padding(start = PingoSpacing.lg, end = 14.dp, top = 14.dp, bottom = 14.dp)
}

@Composable
private fun BannerTexts(title: String, description: String?, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PingoText(
            text = title,
            style = PingoTheme.typography.label,
            color = color,
        )
        if (!description.isNullOrBlank()) {
            PingoText(
                text = description,
                style = PingoTheme.typography.caption,
                color = color,
            )
        }
    }
}

@Composable
private fun bannerColors(tone: PingoBannerTone): PingoBannerColors {
    val colors = PingoTheme.colors
    return when (tone) {
        PingoBannerTone.Info -> PingoBannerColors(
            container = colors.primaryContainer,
            content = colors.onPrimaryContainer,
            defaultIcon = PingoIcons.Bell,
        )
        PingoBannerTone.Warning -> PingoBannerColors(
            container = colors.warningContainer,
            content = colors.onWarningContainer,
            defaultIcon = PingoIcons.TriangleAlert,
        )
        PingoBannerTone.Error -> PingoBannerColors(
            container = colors.expenseContainer,
            content = colors.expense,
            defaultIcon = PingoIcons.BellOff,
        )
        PingoBannerTone.Success -> PingoBannerColors(
            container = colors.incomeContainer,
            content = colors.income,
            defaultIcon = PingoIcons.CircleCheck,
        )
    }
}
