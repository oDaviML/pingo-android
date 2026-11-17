package com.pingo.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion

private const val SHIMMER_DURATION_MS = 1200

@Composable
fun rememberSkeletonBrush(): Brush {
    val colors = PingoTheme.colors
    val base = colors.surfaceVariant
    if (rememberReducedMotion()) {
        return SolidColor(base)
    }

    val highlight = lerp(base, colors.surface, 0.55f)
    val transition = rememberInfiniteTransition(label = "pingo-skeleton")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pingo-skeleton-shimmer",
    )
    val startX = progress * 400f
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(startX, 0f),
        end = Offset(startX + 300f, 120f),
    )
}

@Composable
fun PingoSkeletonBox(modifier: Modifier = Modifier, cornerRadius: Dp = 6.dp, brush: Brush = rememberSkeletonBrush()) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = brush, shape = shape),
    )
}

@Composable
fun PingoSkeletonItem(modifier: Modifier = Modifier, brush: Brush = rememberSkeletonBrush()) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surface)
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoSkeletonBox(
            modifier = Modifier.size(44.dp),
            cornerRadius = PingoRadius.md,
            brush = brush,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            PingoSkeletonBox(
                modifier = Modifier
                    .width(160.dp)
                    .height(12.dp),
                brush = brush,
            )
            PingoSkeletonBox(
                modifier = Modifier
                    .width(110.dp)
                    .height(10.dp),
                brush = brush,
            )
        }
        PingoSkeletonBox(
            modifier = Modifier
                .width(64.dp)
                .height(14.dp),
            brush = brush,
        )
    }
}

@Composable
fun PingoSkeletonList(count: Int = 5, modifier: Modifier = Modifier) {
    val brush = rememberSkeletonBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(count) {
            PingoSkeletonItem(brush = brush)
        }
    }
}
