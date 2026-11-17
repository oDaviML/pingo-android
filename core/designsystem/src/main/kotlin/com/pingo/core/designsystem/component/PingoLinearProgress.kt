package com.pingo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion

@Composable
fun PingoLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PingoTheme.colors.primary,
    trackColor: Color = PingoTheme.colors.surfaceVariant,
    height: Dp = 6.dp,
    animate: Boolean = true,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val reducedMotion = rememberReducedMotion()
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (!animate || reducedMotion) {
            snap()
        } else {
            tween(durationMillis = PingoMotion.MediumMs, easing = PingoMotion.StandardEasing)
        },
        label = "linear-progress",
    )
    val shape = RoundedCornerShape(PingoRadius.full)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(color = trackColor, shape = shape)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            },
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(color = color, shape = shape),
            )
        }
    }
}
