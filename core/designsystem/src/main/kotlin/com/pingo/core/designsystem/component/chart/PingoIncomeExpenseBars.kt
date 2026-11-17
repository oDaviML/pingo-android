package com.pingo.core.designsystem.component.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

fun computeBarFractions(points: List<PingoIncomeExpensePoint>): List<Pair<Float, Float>> {
    if (points.isEmpty()) return emptyList()
    val maxVal = points.maxOf { point ->
        maxOf(sanitizeAmount(point.income), sanitizeAmount(point.expense))
    }
    if (maxVal <= 0.0) return List(points.size) { 0f to 0f }
    return points.map { point ->
        val inc = (sanitizeAmount(point.income) / maxVal).toFloat().coerceIn(0f, 1f)
        val exp = (sanitizeAmount(point.expense) / maxVal).toFloat().coerceIn(0f, 1f)
        inc to exp
    }
}

private fun sanitizeAmount(value: Double): Double = if (value.isFinite() && value > 0.0) value else 0.0

@Composable
fun PingoIncomeExpenseBars(
    points: List<PingoIncomeExpensePoint>,
    modifier: Modifier = Modifier,
    incomeLabel: String = "Receitas",
    expenseLabel: String = "Despesas",
    barChartHeight: Dp = 140.dp,
    onSelectMonth: ((Int) -> Unit)? = null,
) {
    val colors = PingoTheme.colors
    val fractions = computeBarFractions(points)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        IncomeExpenseLegend(
            incomeLabel = incomeLabel,
            expenseLabel = expenseLabel,
            incomeColor = colors.income,
            expenseColor = colors.expense,
        )
        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEachIndexed { index, point ->
                    val (incomeFrac, expenseFrac) = fractions[index]
                    MonthBarColumn(
                        point = point,
                        incomeFraction = incomeFrac,
                        expenseFraction = expenseFrac,
                        barChartHeight = barChartHeight,
                        modifier = Modifier.weight(1f),
                        onClick = onSelectMonth?.let { callback -> { callback(index) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseLegend(incomeLabel: String, expenseLabel: String, incomeColor: Color, expenseColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDotItem(color = incomeColor, label = incomeLabel)
        LegendDotItem(color = expenseColor, label = expenseLabel)
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = RoundedCornerShape(PingoRadius.xs)),
        )
        PingoText(
            text = label,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthBarColumn(
    point: PingoIncomeExpensePoint,
    incomeFraction: Float,
    expenseFraction: Float,
    barChartHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = PingoTheme.colors
    val expenseColor = if (point.isHighlighted) colors.expense else colors.expense.copy(alpha = 0.22f)
    val labelColor = if (point.isHighlighted) colors.primary else colors.onSurfaceVariant
    val labelStyle = if (point.isHighlighted) {
        PingoTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
    } else {
        PingoTheme.typography.caption
    }
    val clickMod = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier

    Column(
        modifier = modifier
            .then(clickMod)
            .semantics(mergeDescendants = true) {
                contentDescription = "${point.monthLabel}: receitas ${point.incomeFormatted}, despesas ${point.expenseFormatted}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        Row(
            modifier = Modifier.height(barChartHeight),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            SingleBar(fraction = incomeFraction, color = colors.income)
            SingleBar(fraction = expenseFraction, color = expenseColor)
        }
        PingoText(text = point.monthLabel, style = labelStyle, color = labelColor, maxLines = 1)
    }
}

@Composable
private fun SingleBar(fraction: Float, color: Color) {
    val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight(fraction.coerceIn(0f, 1f))
            .background(color = color, shape = barShape),
    )
}
