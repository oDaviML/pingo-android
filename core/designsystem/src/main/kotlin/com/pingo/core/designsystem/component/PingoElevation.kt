package com.pingo.core.designsystem.component

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Paint as AndroidPaint

data class PingoElevationSpec(val color: Color, val offsetY: Dp, val blurRadius: Dp, val spread: Dp)

enum class PingoElevationLevel(val spec: PingoElevationSpec) {
    Level1(
        PingoElevationSpec(
            color = Color(0x140B2A33),
            offsetY = 1.dp,
            blurRadius = 3.dp,
            spread = 0.dp,
        ),
    ),
    Level2(
        PingoElevationSpec(
            color = Color(0x1A0B2A33),
            offsetY = 4.dp,
            blurRadius = 12.dp,
            spread = 0.dp,
        ),
    ),
    Level3(
        PingoElevationSpec(
            color = Color(0x260B2A33),
            offsetY = 8.dp,
            blurRadius = 24.dp,
            spread = (-4).dp,
        ),
    ),
    ;

    val color: Color get() = spec.color
    val offsetY: Dp get() = spec.offsetY
    val blurRadius: Dp get() = spec.blurRadius
    val spread: Dp get() = spec.spread
}

fun Modifier.pingoElevation(
    level: PingoElevationLevel,
    shape: Shape = RectangleShape,
    offsetX: Dp = 0.dp,
    offsetY: Dp = level.offsetY,
): Modifier = drawWithCache {
    val spreadPx = level.spread.toPx()
    val shadowWidth = (size.width + spreadPx * 2f).coerceAtLeast(0f)
    val shadowHeight = (size.height + spreadPx * 2f).coerceAtLeast(0f)
    val blurPx = level.blurRadius.toPx()
    val dx = offsetX.toPx() - spreadPx
    val dy = offsetY.toPx() - spreadPx
    val shadowPath = if (shadowWidth > 0f && shadowHeight > 0f) {
        val outline = shape.createOutline(Size(shadowWidth, shadowHeight), layoutDirection, this)
        Path().apply { addOutline(outline) }.asAndroidPath()
    } else {
        null
    }
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = level.color.toArgb()
        if (blurPx > 0f) {
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    onDrawBehind {
        if (shadowPath != null) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(dx, dy)
                canvas.nativeCanvas.drawPath(shadowPath, paint)
                canvas.nativeCanvas.restore()
            }
        }
    }
}
