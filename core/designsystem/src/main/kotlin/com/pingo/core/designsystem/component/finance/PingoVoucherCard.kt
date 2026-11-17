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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pingo.core.designsystem.component.InstitutionAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoVoucherCard(
    state: PingoVoucherCardUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onAdjustBalanceClick: (() -> Unit)? = null,
) {
    val cardModifier = modifier
        .heightIn(min = PingoLayout.minTouchTarget)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) {}

    PingoCard(modifier = cardModifier, gap = PingoSpacing.md) {
        VoucherHeader(state = state)
        PingoText(
            text = state.balanceFormatted,
            style = PingoTheme.typography.headline.copy(fontFeatureSettings = "tnum"),
            color = PingoTheme.colors.income,
            maxLines = 1,
        )
        if (!state.lastRechargeText.isNullOrBlank() || onAdjustBalanceClick != null) {
            VoucherFooter(
                lastRechargeText = state.lastRechargeText,
                adjustLabel = state.adjustLabel,
                onAdjustBalanceClick = onAdjustBalanceClick,
            )
        }
    }
}

@Composable
private fun VoucherHeader(state: PingoVoucherCardUi) {
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
            PingoText(
                text = state.subtitle,
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VoucherFooter(lastRechargeText: String?, adjustLabel: String, onAdjustBalanceClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = lastRechargeText.orEmpty(),
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onAdjustBalanceClick != null) {
            PingoButton(
                text = adjustLabel,
                onClick = onAdjustBalanceClick,
                type = PingoButtonType.Text,
                size = PingoButtonSize.Small,
            )
        }
    }
}
