package com.pingo.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

object PingoMotion {
    const val ShortMs: Int = 150
    const val MediumMs: Int = 250
    const val LongMs: Int = 400
    const val ReducedFadeMs: Int = 100
    const val SHORT: Int = ShortMs
    const val MEDIUM: Int = MediumMs
    const val LONG: Int = LongMs

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val DecelerateEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val AccelerateEasing: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val LinearEasing: Easing = androidx.compose.animation.core.LinearEasing
    val GentleSpring: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 380f)

    val standard: Easing = StandardEasing
    val decelerate: Easing = DecelerateEasing
    val accelerate: Easing = AccelerateEasing
    val linear: Easing = LinearEasing

    fun <T> gentleSpring(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
}

fun isAnimatorDisabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}.getOrDefault(false)

@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) { isAnimatorDisabled(context) }
}
