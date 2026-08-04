package com.lissafi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================
// TYPOGRAPHIE — Hiérarchie claire pour caisse/POS
// ============================================================
val LissafiTypography = Typography(
    // Titres d'écran — 24sp bold
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    // Titres de section — 20sp semibold
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Sous-titres, totaux — 18sp semibold
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Titres de carte — 16sp medium
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // Labels secondaires — 14sp medium
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Petits labels — 12sp medium
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    // Corps de texte — 16sp regular
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Corps standard — 14sp regular
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // Petit texte — 12sp regular
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Boutons — 14sp semibold, tout en majuscules via le composant
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    // Petits boutons — 12sp medium
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // Chips, badges — 11sp medium
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================================
// FORMES — Coins arrondis cohérents (design mobile moderne)
// ============================================================
val LissafiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

// ============================================================
// COLOR SCHEME — Light uniquement pour le MVP
// ============================================================
private val LissafiLightColorScheme = lightColorScheme(
    primary            = Green600,
    onPrimary          = White,
    primaryContainer   = Green100,
    onPrimaryContainer = Green900,
    secondary          = Orange400,
    onSecondary        = Black,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange900,
    tertiary           = Orange600,
    onTertiary         = White,
    tertiaryContainer  = Orange50,
    onTertiaryContainer = Orange800,
    background         = Cream100,
    onBackground       = Black,
    surface            = White,
    onSurface          = Black,
    surfaceVariant     = SurfaceDim,
    onSurfaceVariant   = Neutral700,
    surfaceContainerLowest = White,
    surfaceContainerLow    = OffWhite,
    surfaceContainer       = SurfaceDim,
    surfaceContainerHigh   = Cream200,
    surfaceContainerHighest = Cream300,
    outline            = Neutral300,
    outlineVariant     = Neutral200,
    error              = Danger,
    onError            = White,
    errorContainer     = DangerLight,
    onErrorContainer   = DangerDark,
    inverseSurface     = Neutral900,
    inverseOnSurface   = White,
    inversePrimary     = Green300,
    scrim              = OverlayMedium
)

// ============================================================
// THÈME PRINCIPAL
// ============================================================
@Composable
fun LissafiTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LissafiLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barre de statut assortie au header vert
            @Suppress("DEPRECATION")
            window.statusBarColor = Green800.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LissafiTypography,
        shapes      = LissafiShapes,
        content     = content
    )
}
