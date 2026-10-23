package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

enum class PingoButtonType {
    Primary,
    Tonal,
    Outline,
    Text,
    Danger,
}

enum class PingoButtonSize {
    Medium,
    Small,
}

@Composable
fun PingoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: PingoButtonType = PingoButtonType.Primary,
    size: PingoButtonSize = PingoButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val active = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val appearance = buttonAppearance(type = type, pressed = pressed && active, enabled = active)

    Box(
        modifier = modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ButtonSurface(appearance = appearance, size = size, loading = loading, text = text)
    }
}

@Composable
private fun ButtonSurface(appearance: ButtonAppearance, size: PingoButtonSize, loading: Boolean, text: String) {
    val visualHeight = if (size == PingoButtonSize.Medium) 48.dp else 36.dp
    val textStyle = if (size == PingoButtonSize.Medium) {
        PingoTheme.typography.label
    } else {
        PingoTheme.typography.labelSmall
    }
    val shape = RoundedCornerShape(PingoRadius.md)

    Row(
        modifier = Modifier
            .height(visualHeight)
            .background(color = appearance.container, shape = shape)
            .borderWhen(appearance.border, shape)
            .padding(horizontal = horizontalPadding(size)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            PingoSpinner(size = 16.dp, color = appearance.content)
            Spacer(modifier = Modifier.width(PingoSpacing.sm))
        }

        PingoText(text = text, style = textStyle, color = appearance.content, maxLines = 1)
    }
}

private fun Modifier.borderWhen(color: Color?, shape: Shape): Modifier =
    if (color == null) this else border(width = 1.5.dp, color = color, shape = shape)

private fun horizontalPadding(size: PingoButtonSize): Dp = if (size == PingoButtonSize.Medium) PingoSpacing.xl else 14.dp

private data class ButtonAppearance(val container: Color, val content: Color, val border: Color?)

@Composable
private fun buttonAppearance(type: PingoButtonType, pressed: Boolean, enabled: Boolean): ButtonAppearance {
    val colors = PingoTheme.colors

    if (!enabled) {
        return ButtonAppearance(
            container = if (type == PingoButtonType.Text) Color.Transparent else colors.disabledContainer,
            content = colors.onDisabled,
            border = if (type == PingoButtonType.Outline) colors.disabledContainer else null,
        )
    }

    return when (type) {
        PingoButtonType.Primary -> ButtonAppearance(
            container = if (pressed) colors.primaryPressed else colors.primary,
            content = colors.onPrimary,
            border = null,
        )

        PingoButtonType.Tonal -> ButtonAppearance(
            container = if (pressed) colors.secondary else colors.primaryContainer,
            content = colors.onPrimaryContainer,
            border = null,
        )

        PingoButtonType.Outline -> ButtonAppearance(
            container = if (pressed) colors.surfaceVariant else Color.Transparent,
            content = colors.onBackground,
            border = colors.outline,
        )

        PingoButtonType.Text -> ButtonAppearance(
            container = if (pressed) colors.primaryContainer else Color.Transparent,
            content = colors.brand,
            border = null,
        )

        PingoButtonType.Danger -> ButtonAppearance(
            container = if (pressed) colors.expensePressed else colors.expense,
            content = colors.onPrimary,
            border = null,
        )
    }
}
