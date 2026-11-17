package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
fun PingoSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(
                    color = PingoTheme.colors.outline,
                    shape = RoundedCornerShape(PingoRadius.full),
                ),
        )
    }
}

@Composable
fun PingoBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PingoTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onDismissRequest),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .pingoElevation(level = PingoElevationLevel.Level3, shape = SheetShape, offsetY = (-8).dp)
                .background(color = colors.surface, shape = SheetShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = {})
                .padding(start = PingoSpacing.xl, end = PingoSpacing.xl, top = PingoSpacing.md, bottom = PingoSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        ) {
            PingoSheetHandle()
            BottomSheetHeader(title = title, subtitle = subtitle)
            content()
        }
    }
}

@Composable
private fun BottomSheetHeader(title: String?, subtitle: String?) {
    if (title == null && subtitle == null) return
    val colors = PingoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        if (title != null) {
            PingoText(text = title, style = PingoTheme.typography.title, color = colors.onBackground)
        }
        if (subtitle != null) {
            PingoText(text = subtitle, style = PingoTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
