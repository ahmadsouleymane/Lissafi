package com.lissafi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LissafiGreen,
    onPrimary = LissafiWhite,
    primaryContainer = LissafiGreenLight,
    onPrimaryContainer = LissafiGreenDark,
    secondary = LissafiOrange,
    onSecondary = LissafiBlack,
    secondaryContainer = LissafiOrangeLight,
    onSecondaryContainer = LissafiOrangeDark,
    background = LissafiCream,
    onBackground = LissafiBlack,
    surface = LissafiWhite,
    onSurface = LissafiBlack,
    error = LissafiDanger,
    onError = LissafiWhite,
)

@Composable
fun LissafiTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LissafiGreenDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
