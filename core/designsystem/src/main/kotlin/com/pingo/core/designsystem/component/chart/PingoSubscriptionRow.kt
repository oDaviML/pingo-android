package com.pingo.core.designsystem.component.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoSubscriptionRow(state: PingoSubscriptionRowUi, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = PingoLayout.minTouchTarget)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(vertical = PingoSpacing.md)
        .semantics(mergeDescendants = true) {}

    SubscriptionItemRow(
        title = state.title,
        subtitle = state.subtitle,
        amountFormatted = state.amountFormatted,
        nextChargeLabel = state.nextChargeLabel,
        modifier = rowModifier,
    )
}

@Composable
fun PingoSubscriptionSuggestionCard(
    state: PingoSubscriptionSuggestionUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PingoTheme.colors
    PingoCard(modifier = modifier, gap = PingoSpacing.md) {
        PingoStatusTag(
            label = state.tagLabel,
            containerColor = colors.warningContainer,
            contentColor = colors.onWarningContainer,
        )
        SubscriptionItemRow(
            title = state.merchant,
            subtitle = state.instrumentName,
            amountFormatted = state.amountFormatted,
            nextChargeLabel = state.nextChargeLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        SuggestionActionsRow(
            dismissLabel = state.dismissLabel,
            confirmLabel = state.confirmLabel,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun SubscriptionItemRow(
    title: String,
    subtitle: String,
    amountFormatted: String,
    nextChargeLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        CategoryAvatar(icon = PingoIcons.Repeat)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        ) {
            SubscriptionTitleAmountRow(title = title, amountFormatted = amountFormatted)
            if (subtitle.isNotBlank()) {
                PingoText(
                    text = subtitle,
                    style = PingoTheme.typography.caption,
                    color = PingoTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!nextChargeLabel.isNullOrBlank()) {
                NextChargeBadge(label = nextChargeLabel)
            }
        }
    }
}

@Composable
private fun SubscriptionTitleAmountRow(title: String, amountFormatted: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = title,
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PingoText(
            text = amountFormatted,
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun NextChargeBadge(label: String) {
    Row(
        modifier = Modifier
            .background(
                color = PingoTheme.colors.primaryContainer,
                shape = RoundedCornerShape(PingoRadius.sm),
            )
            .padding(horizontal = PingoSpacing.sm, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.CalendarDays,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = PingoTheme.colors.onPrimaryContainer,
        )
        PingoText(
            text = label,
            style = PingoTheme.typography.labelSmall,
            color = PingoTheme.colors.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun SuggestionActionsRow(dismissLabel: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoButton(
            text = dismissLabel,
            onClick = onDismiss,
            type = PingoButtonType.Outline,
            size = PingoButtonSize.Small,
            modifier = Modifier.weight(1f),
            fillWidth = true,
        )
        PingoButton(
            text = confirmLabel,
            onClick = onConfirm,
            type = PingoButtonType.Primary,
            size = PingoButtonSize.Small,
            modifier = Modifier.weight(1f),
            fillWidth = true,
        )
    }
}
