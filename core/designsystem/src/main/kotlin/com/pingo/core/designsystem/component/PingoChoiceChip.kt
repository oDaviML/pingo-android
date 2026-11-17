package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.full)

    Box(
        modifier = modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .background(color = if (selected) colors.primaryContainer else colors.surface, shape = shape)
            .border(width = 1.5.dp, color = if (selected) colors.primary else colors.outline, shape = shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = PingoSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = text,
            style = PingoTheme.typography.label,
            color = if (selected) colors.onPrimaryContainer else colors.onBackground,
            maxLines = 1,
        )
    }
}
