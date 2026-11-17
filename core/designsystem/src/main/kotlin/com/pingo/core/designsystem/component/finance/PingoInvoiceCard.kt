package com.pingo.core.designsystem.component.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pingo.core.designsystem.component.InstitutionAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoInvoiceCard(
    state: PingoInvoiceCardUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onCompleteDatesClick: (() -> Unit)? = null,
) {
    val cardModifier = modifier
        .heightIn(min = PingoLayout.minTouchTarget)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) {}

    PingoCard(modifier = cardModifier, gap = PingoSpacing.md) {
        InvoiceCardHeader(state = state)
        if (state.status == PingoInvoiceStatus.Incompleta) {
            InvoiceIncompleteBody(
                message = state.incompleteMessage,
                ctaLabel = state.completeDatesLabel,
                onCtaClick = onCompleteDatesClick ?: onClick,
            )
        } else {
            InvoiceActiveBody(state = state)
        }
    }
}

@Composable
private fun InvoiceCardHeader(state: PingoInvoiceCardUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstitutionAvatar(institution = state.institution)
        Column(modifier = Modifier.weight(1f)) {
            PingoText(
                text = state.title,
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!state.subtitle.isNullOrBlank()) {
                PingoText(
                    text = state.subtitle,
                    style = PingoTheme.typography.caption,
                    color = PingoTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        InvoiceStatusTag(status = state.status)
    }
}

@Composable
private fun InvoiceStatusTag(status: PingoInvoiceStatus) {
    val colors = PingoTheme.colors
    val (container, content) = when (status) {
        PingoInvoiceStatus.Aberta -> colors.primaryContainer to colors.onPrimaryContainer
        PingoInvoiceStatus.Fechada -> colors.warningContainer to colors.onWarningContainer
        PingoInvoiceStatus.Proxima -> colors.surfaceVariant to colors.onSurfaceVariant
        PingoInvoiceStatus.Incompleta -> colors.warningContainer to colors.onWarningContainer
    }
    PingoStatusTag(
        label = status.defaultLabel,
        containerColor = container,
        contentColor = content,
    )
}

@Composable
private fun InvoiceActiveBody(state: PingoInvoiceCardUi) {
    val colors = PingoTheme.colors
    val highlightWarning = state.status == PingoInvoiceStatus.Fechada || state.isOverdue
    val accentColor = if (highlightWarning) colors.warning else colors.primary

    if (!state.amountFormatted.isNullOrBlank()) {
        PingoText(
            text = state.amountFormatted,
            style = PingoTheme.typography.headline.copy(fontFeatureSettings = "tnum"),
            color = colors.onBackground,
            maxLines = 1,
        )
    }
    if (state.cycleProgress != null) {
        PingoLinearProgress(progress = state.cycleProgress, color = accentColor)
    }
    if (!state.cyclePeriodText.isNullOrBlank() || !state.dueCountdownText.isNullOrBlank()) {
        InvoiceFooterRow(
            cyclePeriodText = state.cyclePeriodText.orEmpty(),
            dueCountdownText = state.dueCountdownText,
            accentColor = accentColor,
        )
    }
}

@Composable
private fun InvoiceFooterRow(cyclePeriodText: String, dueCountdownText: String?, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = cyclePeriodText,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!dueCountdownText.isNullOrBlank()) {
            PingoText(
                text = dueCountdownText,
                style = PingoTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InvoiceIncompleteBody(message: String, ctaLabel: String, onCtaClick: (() -> Unit)?) {
    PingoText(
        text = message,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
    if (onCtaClick != null) {
        PingoButton(
            text = ctaLabel,
            onClick = onCtaClick,
            type = PingoButtonType.Tonal,
            size = PingoButtonSize.Small,
        )
    }
}
