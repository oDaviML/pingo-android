package com.pingo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.rememberReducedMotion

private const val DROP_PATH_DATA = "M24 2 C24 2 46 27 46 36 A22 22 0 1 1 2 36 C2 27 24 2 24 2 Z"
private const val DROP_VIEWPORT_WIDTH = 44f
private const val DROP_VIEWPORT_HEIGHT = 56f
private const val DROP_VIEWPORT_OFFSET_X = -2f
private const val DROP_VIEWPORT_OFFSET_Y = -2f

private val DropGradientColors = listOf(
    Color(0xFF1AA7BC),
    Color(0xFF088395),
    Color(0xFF09637E),
)

private val WavePrimaryColor = Color(0xFF088395)
private val WaveSecondaryColor = Color(0xFF7AB2B2)

internal data class PingarFrame(
    val dropAlpha: Float,
    val dropWidthDp: Float,
    val dropHeightDp: Float,
    val dropBottomDp: Float,
    val wave1Alpha: Float,
    val wave1WidthDp: Float,
    val wave2Alpha: Float,
    val wave2WidthDp: Float,
    val contentAlpha: Float,
    val contentOffsetYDp: Float,
    val highlightAlpha: Float,
)

internal fun pingarFrameAt(progress: Float): PingarFrame {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p <= 0.30f -> frameBeforeImpact(p / 0.30f)
        p <= 0.50f -> frameAtImpact((p - 0.30f) / 0.20f)
        p <= 0.75f -> frameWithWaves((p - 0.50f) / 0.25f)
        else -> frameSettling((p - 0.75f) / 0.25f)
    }
}

private fun frameBeforeImpact(t: Float): PingarFrame = PingarFrame(
    dropAlpha = lerp(0f, 1f, t),
    dropWidthDp = lerp(20f, 18f, t),
    dropHeightDp = lerp(24f, 30f, t),
    dropBottomDp = lerp(-32f, -10f, t),
    wave1Alpha = 0f,
    wave1WidthDp = 0f,
    wave2Alpha = 0f,
    wave2WidthDp = 0f,
    contentAlpha = 0f,
    contentOffsetYDp = 8f,
    highlightAlpha = 0f,
)

private fun frameAtImpact(t: Float): PingarFrame = PingarFrame(
    dropAlpha = 1f,
    dropWidthDp = lerp(18f, 30f, t),
    dropHeightDp = lerp(30f, 18f, t),
    dropBottomDp = lerp(-10f, 2f, t),
    wave1Alpha = lerp(0f, 0.9f, t),
    wave1WidthDp = lerp(0f, 44f, t),
    wave2Alpha = 0f,
    wave2WidthDp = 0f,
    contentAlpha = lerp(0f, 0.2f, t),
    contentOffsetYDp = lerp(8f, 6f, t),
    highlightAlpha = 0f,
)

private fun frameWithWaves(t: Float): PingarFrame = PingarFrame(
    dropAlpha = lerp(1f, 0f, t),
    dropWidthDp = lerp(30f, 8f, t),
    dropHeightDp = lerp(18f, 6f, t),
    dropBottomDp = lerp(2f, 4f, t),
    wave1Alpha = lerp(0.9f, 0.35f, t),
    wave1WidthDp = lerp(44f, 170f, t),
    wave2Alpha = lerp(0f, 0.8f, t),
    wave2WidthDp = lerp(0f, 84f, t),
    contentAlpha = lerp(0.2f, 0.7f, t),
    contentOffsetYDp = lerp(6f, 2.5f, t),
    highlightAlpha = lerp(0f, 0.6f, t),
)

private fun frameSettling(t: Float): PingarFrame = PingarFrame(
    dropAlpha = 0f,
    dropWidthDp = 8f,
    dropHeightDp = 6f,
    dropBottomDp = 4f,
    wave1Alpha = lerp(0.35f, 0f, t),
    wave1WidthDp = lerp(170f, 240f, t),
    wave2Alpha = lerp(0.8f, 0f, t),
    wave2WidthDp = lerp(84f, 160f, t),
    contentAlpha = lerp(0.7f, 1f, t),
    contentOffsetYDp = lerp(2.5f, 0f, t),
    highlightAlpha = lerp(0.6f, 0f, t),
)

private fun reducedMotionFrame(progress: Float): PingarFrame = PingarFrame(
    dropAlpha = 0f,
    dropWidthDp = 0f,
    dropHeightDp = 0f,
    dropBottomDp = 0f,
    wave1Alpha = 0f,
    wave1WidthDp = 0f,
    wave2Alpha = 0f,
    wave2WidthDp = 0f,
    contentAlpha = progress,
    contentOffsetYDp = 0f,
    highlightAlpha = 0f,
)

@Composable
fun PingoDropAnimation(
    trigger: Boolean = true,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    onFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val progress = rememberPingarProgress(trigger = trigger, reducedMotion = reducedMotion, onFinished = onFinished)
    val baseDropPath = remember { PathParser().parsePathString(DROP_PATH_DATA).toNodes().toPath() }
    val p = progress.value
    val active = trigger && p < 1f
    val frame = remember(p, reducedMotion) {
        if (reducedMotion) reducedMotionFrame(p) else pingarFrameAt(p)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (active) {
                        alpha = frame.contentAlpha
                        translationY = frame.contentOffsetYDp.dp.toPx()
                    }
                },
        ) {
            content()
        }

        if (active && !reducedMotion) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawPingarOverlay(frame = frame, baseDropPath = baseDropPath)
            }
        }
    }
}

@Composable
private fun rememberPingarProgress(
    trigger: Boolean,
    reducedMotion: Boolean,
    onFinished: (() -> Unit)?,
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (trigger) 0f else 1f) }
    LaunchedEffect(trigger, reducedMotion) {
        if (!trigger) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        val duration = if (reducedMotion) PingoMotion.REDUCED_FADE_MS else PingoMotion.LONG_MS
        val easing = if (reducedMotion) LinearEasing else PingoMotion.StandardEasing
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = easing),
        )
        onFinished?.invoke()
    }
    return progress
}

private fun DrawScope.drawPingarOverlay(frame: PingarFrame, baseDropPath: Path) {
    val centerX = size.width / 2f
    val impactY = 2.dp.toPx()
    val strokeWidth = 2.dp.toPx()

    if (frame.highlightAlpha > 0f) {
        val radius = PingoRadius.lg.toPx()
        drawRoundRect(
            color = WavePrimaryColor.copy(alpha = frame.highlightAlpha),
            topLeft = Offset(0f, frame.contentOffsetYDp.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeWidth),
        )
    }

    drawWave(Offset(centerX, impactY), frame.wave1WidthDp.dp.toPx(), WavePrimaryColor, frame.wave1Alpha, strokeWidth)
    drawWave(Offset(centerX, impactY), frame.wave2WidthDp.dp.toPx(), WaveSecondaryColor, frame.wave2Alpha, strokeWidth)
    drawFallingDrop(frame = frame, baseDropPath = baseDropPath, centerX = centerX)
}

private fun DrawScope.drawWave(center: Offset, widthPx: Float, color: Color, alpha: Float, strokeWidth: Float) {
    if (alpha <= 0f || widthPx <= 0f) return
    val heightPx = widthPx / 4f
    drawOval(
        color = color.copy(alpha = alpha),
        topLeft = Offset(center.x - widthPx / 2f, center.y - heightPx / 2f),
        size = Size(widthPx, heightPx),
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.drawFallingDrop(frame: PingarFrame, baseDropPath: Path, centerX: Float) {
    if (frame.dropAlpha <= 0f) return
    val w = frame.dropWidthDp.dp.toPx()
    val h = frame.dropHeightDp.dp.toPx()
    val top = frame.dropBottomDp.dp.toPx() - h
    val left = centerX - w / 2f
    val scaledPath = Path().apply {
        addPath(baseDropPath)
        val matrix = Matrix().apply {
            translate(left, top)
            scale(w / DROP_VIEWPORT_WIDTH, h / DROP_VIEWPORT_HEIGHT)
            translate(DROP_VIEWPORT_OFFSET_X, DROP_VIEWPORT_OFFSET_Y)
        }
        transform(matrix)
    }
    drawPath(
        path = scaledPath,
        brush = Brush.verticalGradient(colors = DropGradientColors, startY = top, endY = top + h),
        alpha = frame.dropAlpha,
    )
}
