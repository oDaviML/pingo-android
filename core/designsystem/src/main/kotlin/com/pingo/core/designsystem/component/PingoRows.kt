package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = PingoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxMark(checked = checked)
        PingoText(
            text = label,
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onBackground,
        )
    }
}

@Composable
private fun CheckboxMark(checked: Boolean) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.sm)

    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (checked) {
                    Modifier.background(color = colors.primary, shape = shape)
                } else {
                    Modifier
                        .background(color = colors.surface, shape = shape)
                        .border(width = 2.dp, color = colors.outline, shape = shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            PingoIcon(
                imageVector = PingoIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.onPrimary,
            )
        }
    }
}

@Composable
fun PingoCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = PingoSpacing.xl,
    gap: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surface, shape = RoundedCornerShape(PingoRadius.xl))
            .border(
                width = 1.dp,
                color = PingoTheme.colors.outlineVariant,
                shape = RoundedCornerShape(PingoRadius.xl),
            )
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    PingoText(
        text = title,
        style = PingoTheme.typography.subtitle,
        color = PingoTheme.colors.onBackground,
        modifier = modifier.fillMaxWidth(),
    )
}

enum class SettingsRowStyle {
    Navigation,
    Danger,
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SettingsRowStyle = SettingsRowStyle.Navigation,
) {
    val danger = style == SettingsRowStyle.Danger
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon = icon, danger = danger)
        SettingsTexts(title = title, subtitle = subtitle, danger = danger, modifier = Modifier.weight(1f))

        if (!danger) {
            PingoIcon(
                imageVector = PingoIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, danger: Boolean) {
    val colors = PingoTheme.colors

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (danger) colors.expenseContainer else colors.primaryContainer,
                shape = RoundedCornerShape(PingoRadius.full),
            ),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (danger) colors.expense else colors.onPrimaryContainer,
        )
    }
}

@Composable
private fun SettingsTexts(title: String, subtitle: String, danger: Boolean, modifier: Modifier = Modifier) {
    val colors = PingoTheme.colors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PingoText(
            text = title,
            style = PingoTheme.typography.bodyStrong,
            color = if (danger) colors.expense else colors.onBackground,
        )
        PingoText(
            text = subtitle,
            style = PingoTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}
