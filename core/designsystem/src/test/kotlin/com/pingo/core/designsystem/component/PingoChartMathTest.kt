package com.pingo.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pingo.core.designsystem.component.chart.PingoComparisonTrend
import com.pingo.core.designsystem.component.chart.PingoDonutSlice
import com.pingo.core.designsystem.component.chart.PingoIncomeExpensePoint
import com.pingo.core.designsystem.component.chart.computeBarFractions
import com.pingo.core.designsystem.component.chart.computeComparisonTrend
import com.pingo.core.designsystem.component.chart.computeDonutAngles
import com.pingo.core.designsystem.component.chart.findTappedSliceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingoChartMathTest {
    @Test
    fun donutAnglesHandleEmptyZeroAndInvalidSlices() {
        assertTrue(computeDonutAngles(emptyList()).isEmpty())

        val zeroSlices = listOf(
            PingoDonutSlice("1", "Alimentação", 0.0, 0),
            PingoDonutSlice("2", "Mercado", -10.0, 0),
            PingoDonutSlice("3", "Transporte", Double.NaN, 0),
        )
        assertTrue(computeDonutAngles(zeroSlices).isEmpty())
    }

    @Test
    fun donutAnglesProduceFullCircleForSingleSlice() {
        val arcs = computeDonutAngles(
            listOf(PingoDonutSlice("food", "Alimentação", 500.0, 100)),
        )
        assertEquals(1, arcs.size)
        assertEquals(-90f, arcs[0].startAngle, 0.01f)
        assertEquals(360f, arcs[0].sweepAngle, 0.01f)
    }

    @Test
    fun donutAnglesDistributeProportionallyWithGap() {
        val arcs = computeDonutAngles(
            slices = listOf(
                PingoDonutSlice("a", "A", 75.0, 75),
                PingoDonutSlice("b", "B", 25.0, 25),
            ),
            gapDegrees = 4f,
        )
        assertEquals(2, arcs.size)
        val totalSweep = arcs.sumOf { it.sweepAngle.toDouble() }.toFloat()
        assertEquals(352f, totalSweep, 0.01f)
        assertTrue(arcs[0].sweepAngle > arcs[1].sweepAngle)
        assertEquals(arcs[0].startAngle + arcs[0].sweepAngle + 4f, arcs[1].startAngle, 0.01f)
    }

    @Test
    fun findTappedSliceDetectsRingHitsAndIgnoresCenterOrOutside() {
        val arcs = computeDonutAngles(
            listOf(
                PingoDonutSlice("top-right", "A", 50.0, 50),
                PingoDonutSlice("left", "B", 50.0, 50),
            ),
        )
        val size = Size(200f, 200f)
        val stroke = 40f

        assertNull(findTappedSliceId(Offset(100f, 100f), size, stroke, arcs))
        assertNull(findTappedSliceId(Offset(5f, 5f), size, stroke, arcs))
        assertEquals("top-right", findTappedSliceId(Offset(180f, 100f), size, stroke, arcs))
        assertEquals("left", findTappedSliceId(Offset(20f, 100f), size, stroke, arcs))
    }

    @Test
    fun barFractionsHandleEmptyZeroAndScaleAgainstMax() {
        assertTrue(computeBarFractions(emptyList()).isEmpty())

        val zeroPoints = listOf(
            PingoIncomeExpensePoint("mai", 0.0, 0.0, "R$ 0", "R$ 0"),
            PingoIncomeExpensePoint("jun", -50.0, Double.NaN, "R$ 0", "R$ 0"),
        )
        assertEquals(listOf(0f to 0f, 0f to 0f), computeBarFractions(zeroPoints))

        val points = listOf(
            PingoIncomeExpensePoint("mai", 2000.0, 1000.0, "R$ 2.000", "R$ 1.000"),
            PingoIncomeExpensePoint("jun", 4000.0, 3000.0, "R$ 4.000", "R$ 3.000", isHighlighted = true),
        )
        val fractions = computeBarFractions(points)
        assertEquals(0.5f, fractions[0].first, 0.001f)
        assertEquals(0.25f, fractions[0].second, 0.001f)
        assertEquals(1f, fractions[1].first, 0.001f)
        assertEquals(0.75f, fractions[1].second, 0.001f)
    }

    @Test
    fun comparisonTrendComputesUpDownEqualAndNoBase() {
        assertEquals(PingoComparisonTrend.NoBase to null, computeComparisonTrend(500.0, 0.0))
        assertEquals(PingoComparisonTrend.NoBase to null, computeComparisonTrend(500.0, -10.0))
        assertEquals(PingoComparisonTrend.Equal to 0, computeComparisonTrend(500.0, 500.0))
        assertEquals(PingoComparisonTrend.Up to 12, computeComparisonTrend(1120.0, 1000.0))
        assertEquals(PingoComparisonTrend.Down to 8, computeComparisonTrend(920.0, 1000.0))
    }
}
