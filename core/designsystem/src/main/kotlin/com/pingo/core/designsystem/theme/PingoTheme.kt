package com.pingo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPingoColors = staticCompositionLocalOf { PingoLightColors }

private val LocalPingoTypography = staticCompositionLocalOf { DefaultPingoTypography }

object PingoTheme {
    val colors: PingoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPingoColors.current

    val typography: PingoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalPingoTypography.current
}

@Composable
fun PingoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPingoColors provides if (darkTheme) PingoDarkColors else PingoLightColors,
        LocalPingoTypography provides DefaultPingoTypography,
        content = content,
    )
}
