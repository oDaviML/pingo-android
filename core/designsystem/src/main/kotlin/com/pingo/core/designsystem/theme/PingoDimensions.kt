package com.pingo.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PingoRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val full: Dp = 999.dp
}

object PingoSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

object PingoElevation {
    val level1: Dp = 2.dp
    val level2: Dp = 6.dp
    val level3: Dp = 12.dp
}

object PingoMotion {
    const val SHORT: Int = 150
    const val MEDIUM: Int = 250
    const val LONG: Int = 400
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val accelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val linear: Easing = LinearEasing
}

object PingoLayout {
    val screenHorizontal: Dp = 24.dp
    val screenVertical: Dp = 24.dp
    val contentGap: Dp = 16.dp
    val minTouchTarget: Dp = 48.dp
}
