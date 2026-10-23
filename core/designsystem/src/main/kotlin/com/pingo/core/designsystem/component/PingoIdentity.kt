package com.pingo.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = PingoTheme.colors.onBackground,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun PingoMascot(modifier: Modifier = Modifier, contentDescription: String? = "Mascote do Pingo", width: Dp = 96.dp) {
    Image(
        painter = rememberVectorPainter(PingoArtwork.mascot),
        contentDescription = contentDescription,
        modifier = modifier.size(width = width, height = width * 1.25f),
    )
}

private fun pathOf(path: String) = PathParser().parsePathString(path).toNodes()

object PingoArtwork {
    val mascot: ImageVector by lazy {
        ImageVector.Builder(
            name = "PingoMascot",
            defaultWidth = 96.dp,
            defaultHeight = 120.dp,
            viewportWidth = 96f,
            viewportHeight = 120f,
        )
            .addPath(
                pathData = pathOf(
                    "M52 8C52 8 96 58 96 76C96 100.301 76.301 120 52 120C27.699 120 8 100.301 8 76C8 58 52 8 52 8Z",
                ),
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF1AA7BC),
                        0.5f to Color(0xFF088395),
                        1f to Color(0xFF09637E),
                    ),
                    start = androidx.compose.ui.geometry.Offset(44f, 0f),
                    end = androidx.compose.ui.geometry.Offset(44f, 112f),
                ),
            )
            .addGroup(
                translationX = 27.443f,
                translationY = 48.394f,
                rotate = 24f,
                pivotX = 0f,
                pivotY = 0f,
            )
            .addPath(
                pathData = pathOf("M6.5 0A6.5 12 0 1 0 6.5 24A6.5 12 0 1 0 6.5 0Z"),
                fill = SolidColor(Color(0x66FFFFFF)),
            )
            .clearGroup()
            .addPath(
                pathData = pathOf("M38.5 70A4.5 6.5 0 1 0 38.5 83A4.5 6.5 0 1 0 38.5 70Z"),
                fill = SolidColor(Color(0xFF04222A)),
            )
            .addPath(
                pathData = pathOf("M58.5 70A4.5 6.5 0 1 0 58.5 83A4.5 6.5 0 1 0 58.5 70Z"),
                fill = SolidColor(Color(0xFF04222A)),
            )
            .addGroup(translationX = 41f, translationY = 89f)
            .addPath(
                pathData = pathOf("M0 0C5 4.667 10 4.667 15 0"),
                fill = null,
                stroke = SolidColor(Color(0xFF04222A)),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .clearGroup()
            .build()
    }
}
