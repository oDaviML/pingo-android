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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import kotlin.math.abs
import kotlin.math.roundToInt

fun computeComparisonTrend(currentAmount: Double, previousAmount: Double): Pair<PingoComparisonTrend, Int?> {
    if (!currentAmount.isFinite() || !previousAmount.isFinite() || previousAmount <= 0.0) {
        return PingoComparisonTrend.NoBase to null
    }
    val pct = (((currentAmount - previousAmount) / previousAmount) * 100.0).roundToInt()
    return when {
        pct > 0 -> PingoComparisonTrend.Up to pct
        pct < 0 -> PingoComparisonTrend.Down to abs(pct)
        else -> PingoComparisonTrend.Equal to 0
    }
}

@Composable
fun PingoComparisonRow(state: PingoComparisonRowUi, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = PingoLayout.minTouchTarget)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(vertical = PingoSpacing.sm)
        .semantics(mergeDescendants = true) {}

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = state.icon)
        Column(modifier = Modifier.weight(1f)) {
            PingoText(
                text = state.categoryName,
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
        ComparisonTrendBadge(trend = state.trend, badgeText = state.badgeText)
    }
}

@Composable
private fun ComparisonTrendBadge(trend: PingoComparisonTrend, badgeText: String) {
    val style = comparisonBadgeStyle(trend, badgeText)
    Row(
        modifier = Modifier
            .background(color = style.container, shape = RoundedCornerShape(PingoRadius.sm))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = style.accessibilityText },
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (style.icon != null) {
            PingoIcon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = style.content,
            )
        }
        PingoText(
            text = badgeText,
            style = PingoTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = style.content,
            maxLines = 1,
        )
    }
}

private data class ComparisonBadgeStyle(val container: Color, val content: Color, val icon: ImageVector?, val accessibilityText: String)

@Composable
private fun comparisonBadgeStyle(trend: PingoComparisonTrend, badgeText: String): ComparisonBadgeStyle {
    val colors = PingoTheme.colors
    return when (trend) {
        PingoComparisonTrend.Up -> ComparisonBadgeStyle(
            container = colors.expenseContainer,
            content = colors.expense,
            icon = PingoIcons.TrendingUp,
            accessibilityText = "Aumento de $badgeText",
        )
        PingoComparisonTrend.Down -> ComparisonBadgeStyle(
            container = colors.incomeContainer,
            content = colors.income,
            icon = PingoIcons.TrendingDown,
            accessibilityText = "Queda de $badgeText",
        )
        PingoComparisonTrend.Equal -> ComparisonBadgeStyle(
            container = colors.surfaceVariant,
            content = colors.onSurfaceVariant,
            icon = null,
            accessibilityText = "Igual ao mês anterior",
        )
        PingoComparisonTrend.NoBase -> ComparisonBadgeStyle(
            container = colors.surfaceVariant,
            content = colors.onSurfaceVariant,
            icon = null,
            accessibilityText = "Sem base no mês anterior",
        )
    }
}
