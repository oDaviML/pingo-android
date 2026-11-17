package com.pingo.core.designsystem.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

fun computeDonutAngles(slices: List<PingoDonutSlice>, gapDegrees: Float = 3f): List<DonutArcSpec> {
    val active = slices.mapIndexedNotNull { index, slice ->
        if (slice.amount.isFinite() && slice.amount > 0.0) index to slice else null
    }
    val total = active.sumOf { it.second.amount }
    if (active.isEmpty() || !total.isFinite() || total <= 0.0) return emptyList()

    if (active.size == 1) {
        val (idx, slice) = active.first()
        return listOf(DonutArcSpec(id = slice.id, startAngle = -90f, sweepAngle = 360f, sourceIndex = idx))
    }

    val safeGap = gapDegrees.coerceIn(0f, 12f)
    val availableSweep = (360f - safeGap * active.size).coerceAtLeast(1f)
    var currentStart = -90f
    return active.map { (idx, slice) ->
        val sweep = ((slice.amount / total) * availableSweep).toFloat().coerceAtLeast(0.5f)
        val spec = DonutArcSpec(id = slice.id, startAngle = currentStart, sweepAngle = sweep, sourceIndex = idx)
        currentStart += sweep + safeGap
        spec
    }
}

fun findTappedSliceId(tapOffset: Offset, size: Size, strokeWidthPx: Float, arcs: List<DonutArcSpec>): String? {
    if (arcs.isEmpty() || size.width <= 0f || size.height <= 0f) return null
    val dx = tapOffset.x - size.width / 2f
    val dy = tapOffset.y - size.height / 2f
    val radius = (minOf(size.width, size.height) - strokeWidthPx) / 2f
    val dist = hypot(dx, dy)
    val minR = (radius - strokeWidthPx * 0.75f).coerceAtLeast(0f)
    val maxR = radius + strokeWidthPx * 0.75f
    if (dist !in minR..maxR) return null

    var angle = (atan2(dy, dx) * 180.0 / PI).toFloat()
    if (angle < -90f) angle += 360f
    return arcs.firstOrNull { angle >= it.startAngle && angle <= it.startAngle + it.sweepAngle }?.id
}

@Composable
fun PingoDonutChart(
    slices: List<PingoDonutSlice>,
    totalFormatted: String,
    modifier: Modifier = Modifier,
    centerCaption: String = "Despesas",
    selectedSliceId: String? = null,
    onSelectCategory: ((String) -> Unit)? = null,
) {
    val colors = PingoTheme.colors
    val palette = listOf(colors.brand, colors.primary, colors.secondary, colors.warning, colors.outline)
    val arcs = computeDonutAngles(slices)
    val chartDescription = buildDonutDescription(centerCaption, totalFormatted, slices)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DonutRingBox(
            slices = slices,
            arcs = arcs,
            palette = palette,
            selectedSliceId = selectedSliceId,
            chartDescription = chartDescription,
            onSelectCategory = onSelectCategory,
        ) {
            DonutCenterLabel(centerCaption = centerCaption, totalFormatted = totalFormatted)
        }
        DonutLegend(
            slices = slices,
            palette = palette,
            selectedSliceId = selectedSliceId,
            onSelectCategory = onSelectCategory,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DonutRingBox(
    slices: List<PingoDonutSlice>,
    arcs: List<DonutArcSpec>,
    palette: List<Color>,
    selectedSliceId: String?,
    chartDescription: String,
    onSelectCategory: ((String) -> Unit)?,
    centerContent: @Composable () -> Unit,
) {
    val colors = PingoTheme.colors
    val strokePx = with(LocalDensity.current) { 24.dp.toPx() }
    val tapModifier = if (onSelectCategory != null && arcs.isNotEmpty()) {
        Modifier.pointerInput(arcs) {
            detectTapGestures { offset ->
                val chartSize = Size(size.width.toFloat(), size.height.toFloat())
                findTappedSliceId(offset, chartSize, strokePx, arcs)?.let(onSelectCategory)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(144.dp)
            .semantics { contentDescription = chartDescription }
            .then(tapModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(144.dp)) {
            drawDonutArcs(slices, arcs, palette, selectedSliceId, colors.surfaceVariant, strokePx)
        }
        centerContent()
    }
}

private fun DrawScope.drawDonutArcs(
    slices: List<PingoDonutSlice>,
    arcs: List<DonutArcSpec>,
    palette: List<Color>,
    selectedSliceId: String?,
    emptyColor: Color,
    strokePx: Float,
) {
    val inset = strokePx / 2f
    val arcSize = Size(size.width - strokePx, size.height - strokePx)
    if (arcs.isEmpty()) {
        drawArc(
            color = emptyColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Butt),
        )
        return
    }
    arcs.forEach { arc ->
        val slice = slices[arc.sourceIndex]
        val baseColor = slice.color ?: palette[arc.sourceIndex % palette.size]
        val alpha = if (selectedSliceId == null || selectedSliceId == slice.id) 1f else 0.35f
        drawArc(
            color = baseColor.copy(alpha = alpha),
            startAngle = arc.startAngle,
            sweepAngle = arc.sweepAngle,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Butt),
        )
    }
}

@Composable
private fun DonutCenterLabel(centerCaption: String, totalFormatted: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PingoText(
            text = centerCaption,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        PingoText(
            text = totalFormatted,
            style = PingoTheme.typography.title.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun DonutLegend(
    slices: List<PingoDonutSlice>,
    palette: List<Color>,
    selectedSliceId: String?,
    onSelectCategory: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (onSelectCategory != null) PingoSpacing.xs else PingoSpacing.sm),
    ) {
        slices.forEachIndexed { index, slice ->
            val dotColor = slice.color ?: palette[index % palette.size]
            val selected = selectedSliceId == null || selectedSliceId == slice.id
            DonutLegendItem(
                slice = slice,
                dotColor = dotColor,
                selected = selected,
                onClick = onSelectCategory?.let { callback -> { callback(slice.id) } },
            )
        }
    }
}

@Composable
private fun DonutLegendItem(slice: PingoDonutSlice, dotColor: Color, selected: Boolean, onClick: (() -> Unit)?) {
    val alpha = if (selected) 1f else 0.45f
    val clickModifier = if (onClick != null) {
        Modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = PingoSpacing.xs)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .semantics(mergeDescendants = true) { contentDescription = "${slice.label}: ${slice.percentage}%" },
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = dotColor.copy(alpha = alpha), shape = RoundedCornerShape(PingoRadius.xs)),
        )
        PingoText(
            text = slice.label,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onBackground.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PingoText(
            text = "${slice.percentage}%",
            style = PingoTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun buildDonutDescription(centerCaption: String, totalFormatted: String, slices: List<PingoDonutSlice>): String {
    if (slices.isEmpty()) return "$centerCaption: $totalFormatted, sem dados"
    val details = slices.joinToString(", ") { "${it.label} ${it.percentage}%" }
    return "$centerCaption: $totalFormatted. $details"
}
