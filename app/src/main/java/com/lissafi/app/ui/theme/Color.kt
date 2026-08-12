package com.lissafi.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// LISSAFI — Palette redesign 2026
// Vert forêt profond + orange brûlé + fond blanc cassé
// Style SaaS premium, inspiration Fintech POS moderne
// ============================================================

// ── Primary — Vert forêt moderne (identique clair/sombre) ──
val Primary = Color(0xFF0F6E46)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer: Color get() = if (ThemeManager.isDark) Color(0xFF123825) else Color(0xFFE8F5E9)
val OnPrimaryContainer: Color get() = if (ThemeManager.isDark) Color(0xFFA8E6C4) else Color(0xFF0A3D24)

// ── Secondary — Orange brûlé (dettes, alertes) ─────────────
val Secondary = Color(0xFFE67E22)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer: Color get() = if (ThemeManager.isDark) Color(0xFF3D2610) else Color(0xFFFFF3E8)
val OnSecondaryContainer: Color get() = if (ThemeManager.isDark) Color(0xFFFFD9AE) else Color(0xFF5C2D0E)

// ── Background ──────────────────────────────────────────────
val Background: Color get() = if (ThemeManager.isDark) Color(0xFF121212) else Color(0xFFF8F9FA)
val OnBackground: Color get() = if (ThemeManager.isDark) Color(0xFFF5F5F5) else Color(0xFF121212)

// ── Surface — Cartes ─────────────────────────────────────────
val Surface: Color get() = if (ThemeManager.isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
val OnSurface: Color get() = if (ThemeManager.isDark) Color(0xFFF5F5F5) else Color(0xFF121212)
val SurfaceAlt: Color get() = if (ThemeManager.isDark) Color(0xFF2A2A2A) else Color(0xFFF4F5F6)

// ── Texte ───────────────────────────────────────────────────
val TextSecondary: Color get() = if (ThemeManager.isDark) Color(0xFFAEB4BC) else Color(0xFF6B7280)
val TextTertiary: Color get() = if (ThemeManager.isDark) Color(0xFF7D8590) else Color(0xFF9CA3AF)

// ── Border ──────────────────────────────────────────────────
val Border: Color get() = if (ThemeManager.isDark) Color(0xFF33383F) else Color(0xFFE5E7EB)

// ── Error / Success ─────────────────────────────────────────
val Error = Color(0xFFEF4444)
val ErrorContainer: Color get() = if (ThemeManager.isDark) Color(0xFF4A1515) else Color(0xFFFEE2E2)
val Success = Color(0xFF10B981)
val SuccessContainer: Color get() = if (ThemeManager.isDark) Color(0xFF0F3D2A) else Color(0xFFD1FAE5)

// ── Info ────────────────────────────────────────────────────
val Info = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)

// ── Overlay ─────────────────────────────────────────────────
val Scrim = Color(0x4D000000)

// ── Alias compatibilité ─────────────────────────────────────
val LissafiGreen = Primary
val LissafiGreenLight = Primary
val LissafiGreenDark = Primary
val LissafiOrange = Secondary
val LissafiOrangeLight = Secondary
val LissafiOrangeDark = Secondary
val LissafiCream: Color get() = Background
val LissafiWhite: Color get() = Surface
val LissafiBlack: Color get() = OnBackground
val LissafiWarning = Secondary
val LissafiDanger = Error
val LissafiSuccess = Success

// ── Neutres ─────────────────────────────────────────────────
val Neutral50 = Color(0xFFF8F9FA)
val Neutral100 = Color(0xFFF1F3F5)
val Neutral200 = Color(0xFFE8EBEE)
val Neutral300 = Color(0xFFDEE2E6)
val Neutral400 = Color(0xFF6B7280)
val Neutral500 = Color(0xFF6B7280)
val Neutral600 = Color(0xFF9CA3AF)
val Neutral700 = Color(0xFF495057)
val Neutral800 = Color(0xFF343A40)
val Neutral900 = Color(0xFF212529)

val White = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFFCFCFB)
val Black = Color(0xFF121212)
val SoftBlack = Color(0xFF2D2D2D)

// ── Verts ───────────────────────────────────────────────────
val Green50 = Color(0xFFE8F5E9)
val Green100 = Color(0xFFC8E6C9)
val Green200 = Color(0xFFA5D6A7)
val Green300 = Color(0xFF81C784)
val Green400 = Color(0xFF66BB6A)
val Green500 = Color(0xFF4CAF50)
val Green600 = Primary
val Green700 = Color(0xFF0D5E38)
val Green800 = Color(0xFF0A4D2C)
val Green900 = Color(0xFF073C20)

// ── Oranges ─────────────────────────────────────────────────
val Orange50 = SecondaryContainer
val Orange100 = Color(0xFFFFE0C0)
val Orange200 = Color(0xFFFFCD98)
val Orange300 = Color(0xFFF5A623)
val Orange400 = Secondary
val Orange500 = Color(0xFFE89540)
val Orange600 = Color(0xFFD4893F)
val Orange700 = Color(0xFFB07030)
val Orange800 = Color(0xFF8C5820)
val Orange900 = Color(0xFF684010)

// ── Crèmes ──────────────────────────────────────────────────
val Cream50 = Color(0xFFFEFDFB)
val Cream100 = Background
val Cream200 = Color(0xFFF5F2EB)
val Cream300 = Color(0xFFEDE8DD)
val Cream400 = Color(0xFFE5DFD0)
val Cream500 = Color(0xFFD8D0BD)

// ── Surfaces Material ───────────────────────────────────────
val SurfaceDefault = Surface
val SurfaceDim = SurfaceAlt
val SurfaceContainer = SurfaceAlt
val SurfaceContainerLowest = Surface
val SurfaceContainerLow = OffWhite
val SurfaceContainerHigh = Color(0xFFF5F2EB)
val SurfaceContainerHighest = Color(0xFFEDE8DD)

// ── Sémantiques ─────────────────────────────────────────────
val SuccessLight = SuccessContainer
val SuccessDark = Color(0xFF065F46)
val Warning = Secondary
val WarningLight = SecondaryContainer
val WarningDark = Color(0xFF5C2D0E)
val Danger = Error
val DangerLight = ErrorContainer
val DangerDark = Color(0xFFB91C1C)

val OverlayLight = Color(0x26000000)
val OverlayMedium = Scrim
val OverlayDark = Color(0x80000000)
