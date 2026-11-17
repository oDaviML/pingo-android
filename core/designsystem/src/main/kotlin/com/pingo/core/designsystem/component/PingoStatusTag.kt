package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

enum class PingoCaptureStatus(val defaultLabel: String) {
    Active("Captura ativa"),
    Stopped("Captura parada"),
    Syncing("Sincronizando"),
}

data class PingoStatusTagStyle(val containerColor: Color, val contentColor: Color)

@Composable
fun PingoStatusTag(
    status: PingoCaptureStatus,
    modifier: Modifier = Modifier,
    label: String = status.defaultLabel,
    showDot: Boolean = true,
) {
    val style = captureStatusStyle(status)
    PingoStatusTag(
        label = label,
        containerColor = style.containerColor,
        contentColor = style.contentColor,
        modifier = modifier,
        dotColor = if (showDot) style.contentColor else null,
    )
}

@Composable
private fun captureStatusStyle(status: PingoCaptureStatus): PingoStatusTagStyle {
    val colors = PingoTheme.colors
    return when (status) {
        PingoCaptureStatus.Active -> PingoStatusTagStyle(colors.incomeContainer, colors.income)
        PingoCaptureStatus.Stopped -> PingoStatusTagStyle(colors.expenseContainer, colors.expense)
        PingoCaptureStatus.Syncing -> PingoStatusTagStyle(colors.primaryContainer, colors.onPrimaryContainer)
    }
}

@Composable
fun PingoStatusTag(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    shape: Shape = RoundedCornerShape(PingoRadius.full),
) {
    Row(
        modifier = modifier
            .background(color = containerColor, shape = shape)
            .padding(
                start = if (dotColor != null) PingoSpacing.sm else 10.dp,
                end = 10.dp,
                top = 5.dp,
                bottom = 5.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = dotColor, shape = CircleShape),
            )
        }
        PingoText(
            text = label,
            style = PingoTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
fun PingoTag(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PingoRadius.sm),
) {
    Box(
        modifier = modifier
            .background(color = containerColor, shape = shape)
            .padding(horizontal = PingoSpacing.sm, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = label,
            style = PingoTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
fun PingoBadgeDot(modifier: Modifier = Modifier, color: Color = PingoTheme.colors.expense) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = color, shape = CircleShape),
    )
}

@Composable
fun PingoBadgeCount(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = PingoTheme.colors.expense,
    contentColor: Color = PingoTheme.colors.onPrimary,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(color = containerColor, shape = RoundedCornerShape(PingoRadius.full))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = count.toString(),
            style = PingoTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}
