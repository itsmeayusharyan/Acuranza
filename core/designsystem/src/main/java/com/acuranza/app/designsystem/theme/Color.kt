package com.acuranza.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * The Acuranza palette: cool slate-navy surfaces, off-white ink, one trust-blue accent for the brand
 * and interactions, and green reserved for money coming in. There are deliberately no gradients;
 * the old gradient names resolve to flat colours.
 */
object BrandPalette {
    // Neutrals (light)
    val Paper = Color(0xFFF4F6FA)
    val Card = Color(0xFFFFFFFF)
    val Hairline = Color(0xFFE3E8F0)
    val Divider = Color(0xFFEAEEF5)
    val Ink = Color(0xFF0F172A)
    val InkSecondary = Color(0xFF475569)
    val InkTertiary = Color(0xFF8A94A6)
    val Fill = Color(0xFFEDF0F7)

    // Neutrals (dark) — deep navy, not black
    val Night = Color(0xFF0F172A)
    val NightCard = Color(0xFF192134)
    val NightHairline = Color(0xFF2A3A5C)
    val NightFill = Color(0xFF1E2942)
    val Snow = Color(0xFFF1F5F9)
    val SnowSecondary = Color(0xFF94A3B8)
    val SnowTertiary = Color(0xFF64748B)

    // Accent — the brand blue
    val Blue = Color(0xFF2563EB)
    val BlueDeep = Color(0xFF1D4ED8)
    val BlueBright = Color(0xFF60A5FA)
    val BlueTint = Color(0xFFEFF6FF)
    val BlueTintDark = Color(0xFF17263F)

    // Money in — the only green
    val Emerald = Color(0xFF059669)
    val EmeraldDeep = Color(0xFF047857)
    val EmeraldBright = Color(0xFF34D399)
    val EmeraldTint = Color(0xFFECFDF5)
    val EmeraldTintDark = Color(0xFF0D2A21)

    // Status
    val Red = Color(0xFFDC2626)
    val RedTint = Color(0xFFFEE2E2)
    val Amber = Color(0xFFB45309)
    val AmberTint = Color(0xFFFFFBEB)

    /** Kept for compatibility; Acuranza uses no gradients, so this is flat ink. */
    val brandGradient: Brush get() = SolidColor(Ink)

    /** Kept for compatibility; flat brand blue. */
    val goldGradient: Brush get() = SolidColor(Blue)
}

/**
 * Semantic colours that Material3's [androidx.compose.material3.ColorScheme] has no slot for.
 * Read them through [AcuranzaTheme.colors].
 */
@Immutable
data class AcuranzaColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warning: Color,
    val alert: Color,
    val good: Color,
    val tip: Color,
    val gold: Color,
    val cardSurface: Color,
    val cardSurfaceHigh: Color,
    val glass: Color,
    val glassBorder: Color,
    val subtleText: Color,
    val divider: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val isDark: Boolean,
    /** Primary text / ink buttons. */
    val ink: Color,
    /** Text on ink buttons. */
    val onInk: Color,
    val tertiaryText: Color,
    /** 1dp card outline. */
    val hairline: Color,
    val accent: Color,
    val accentTint: Color,
    val alertTint: Color,
    val warningTint: Color,
    val tipTint: Color,
    /** Neutral fill for icon tiles, chips and inputs. */
    val fill: Color,
) {
    /** Kept for compatibility; a flat surface instead of a gradient. */
    val heroGradient: Brush get() = SolidColor(cardSurface)
}

internal val LightAcuranzaColors = AcuranzaColors(
    income = BrandPalette.Emerald,
    expense = BrandPalette.Ink,
    transfer = BrandPalette.InkSecondary,
    warning = BrandPalette.Amber,
    alert = BrandPalette.Red,
    good = BrandPalette.Emerald,
    tip = BrandPalette.Blue,
    gold = BrandPalette.Blue,
    cardSurface = BrandPalette.Card,
    cardSurfaceHigh = BrandPalette.Fill,
    glass = BrandPalette.Card,
    glassBorder = BrandPalette.Hairline,
    subtleText = BrandPalette.InkSecondary,
    divider = BrandPalette.Divider,
    shimmerBase = Color(0xFFE9EDF4),
    shimmerHighlight = BrandPalette.Paper,
    isDark = false,
    ink = BrandPalette.Ink,
    onInk = Color.White,
    tertiaryText = BrandPalette.InkTertiary,
    hairline = BrandPalette.Hairline,
    accent = BrandPalette.Blue,
    accentTint = BrandPalette.BlueTint,
    alertTint = BrandPalette.RedTint,
    warningTint = BrandPalette.AmberTint,
    tipTint = BrandPalette.BlueTint,
    fill = BrandPalette.Fill,
)

internal val DarkAcuranzaColors = AcuranzaColors(
    income = BrandPalette.EmeraldBright,
    expense = BrandPalette.Snow,
    transfer = BrandPalette.SnowSecondary,
    warning = Color(0xFFF59E0B),
    alert = Color(0xFFF87171),
    good = BrandPalette.EmeraldBright,
    tip = BrandPalette.BlueBright,
    gold = BrandPalette.BlueBright,
    cardSurface = BrandPalette.NightCard,
    cardSurfaceHigh = BrandPalette.NightFill,
    glass = BrandPalette.NightCard,
    glassBorder = BrandPalette.NightHairline,
    subtleText = BrandPalette.SnowSecondary,
    divider = BrandPalette.NightHairline,
    shimmerBase = BrandPalette.NightCard,
    shimmerHighlight = BrandPalette.NightFill,
    isDark = true,
    ink = BrandPalette.Snow,
    onInk = BrandPalette.Night,
    tertiaryText = BrandPalette.SnowTertiary,
    hairline = BrandPalette.NightHairline,
    accent = BrandPalette.BlueBright,
    accentTint = BrandPalette.BlueTintDark,
    alertTint = Color(0xFF2A1414),
    warningTint = Color(0xFF2A2010),
    tipTint = Color(0xFF14202F),
    fill = BrandPalette.NightFill,
)

val LocalAcuranzaColors = staticCompositionLocalOf { LightAcuranzaColors }
