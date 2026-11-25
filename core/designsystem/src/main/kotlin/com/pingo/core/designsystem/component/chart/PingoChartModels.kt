package com.pingo.core.designsystem.component.chart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class PingoDonutSlice(val id: String, val label: String, val amount: Double, val percentage: Int, val color: Color? = null)

@Immutable
data class DonutArcSpec(val id: String, val startAngle: Float, val sweepAngle: Float, val sourceIndex: Int)

enum class PingoComparisonTrend {
    Up,
    Down,
    Equal,
    NoBase,
}

@Immutable
data class PingoComparisonRowUi(
    val icon: ImageVector,
    val categoryName: String,
    val subtitle: String,
    val trend: PingoComparisonTrend,
    val badgeText: String,
)

@Immutable
data class PingoIncomeExpensePoint(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val incomeFormatted: String,
    val expenseFormatted: String,
    val isHighlighted: Boolean = false,
)

@Immutable
data class PingoSubscriptionRowUi(
    val title: String,
    val subtitle: String,
    val amountFormatted: String,
    val nextChargeLabel: String? = null,
)

@Immutable
data class PingoSubscriptionSuggestionUi(
    val merchant: String,
    val instrumentName: String,
    val amountFormatted: String,
    val tagLabel: String = "Parece assinatura",
    val confirmLabel: String = "Confirmar",
    val dismissLabel: String = "Recusar",
    val nextChargeLabel: String? = null,
)
