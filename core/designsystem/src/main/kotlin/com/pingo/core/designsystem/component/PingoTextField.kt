package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Suppress("LongParameterList")
@Composable
fun PingoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = PingoTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        if (showLabel) {
            PingoText(
                text = label,
                style = PingoTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        PingoTextFieldBox(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
        )

        if (supportingText != null) {
            PingoText(
                text = supportingText,
                style = PingoTheme.typography.caption,
                color = if (isError) colors.expense else colors.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun PingoTextFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.md)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .background(color = colors.surface, shape = shape)
            .border(width = 1.5.dp, color = if (isError) colors.expense else colors.outline, shape = shape)
            .semantics { contentDescription = label },
        textStyle = PingoTheme.typography.body.copy(color = if (enabled) colors.onBackground else colors.onSurfaceVariant),
        cursorBrush = SolidColor(colors.primary),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}
