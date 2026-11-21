package com.focus.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.focus.core.model.AccentColor

/** Neutral colors of one theme. The accent is added on top by [AccentPalette]. */
@Immutable
data class FocusPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val comparison: Color,
    val error: Color,
)

/**
 * Near-black base with tonal surfaces (Material 3 "surface containers"): each level is a
 * clearly lighter step, so cards separate from the page by tone alone and need no border.
 * Outlines are kept only for hairline dividers inside a card.
 */
val DarkPalette = FocusPalette(
    isDark = true,
    background = Color(0xFF0C0C0E),
    surface = Color(0xFF17171A),
    surfaceRaised = Color(0xFF1D1D21),
    surfaceHigh = Color(0xFF26262B),
    outline = Color(0xFF26262B),
    outlineStrong = Color(0xFF3A3A41),
    textPrimary = Color(0xFFEDEDF0),
    textSecondary = Color(0xFF8B8B95),
    textTertiary = Color(0xFF5A5A63),
    comparison = Color(0xFF6E6E78),
    error = Color(0xFFFF5A5F),
)

/**
 * OLED: the page is pure black, so unused pixels stay off. Cards keep a very low
 * tonal step (still no borders) and text is slightly dimmed to avoid harsh glare.
 */
val OledPalette = DarkPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0F0F11),
    surfaceRaised = Color(0xFF151518),
    surfaceHigh = Color(0xFF1D1D21),
    outline = Color(0xFF1D1D21),
    outlineStrong = Color(0xFF2E2E34),
    textPrimary = Color(0xFFE4E4E8),
)

/**
 * Light mirror of the dark palette: a faint grey page with white cards (the elevation
 * order is inverted, as on paper), same cool neutral hue, text tuned for AA contrast.
 */
val LightPalette = FocusPalette(
    isDark = false,
    background = Color(0xFFF1F1F5),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEAEAEF),
    outline = Color(0xFFEAEAEF),
    outlineStrong = Color(0xFFCFCFD7),
    textPrimary = Color(0xFF131316),
    textSecondary = Color(0xFF5E5E68),
    textTertiary = Color(0xFF94949E),
    comparison = Color(0xFFA3A3AD),
    error = Color(0xFFD7373F),
)

/** Colors derived from the accent the user picked, for the current theme. */
@Immutable
data class AccentPalette(val accent: Color, val surface: Color = DarkPalette.surface) {
    /** Accent blended into the surface: tracks, subtle selected states. */
    val muted: Color = lerp(surface, accent, 0.30f)
    val dim: Color = lerp(surface, accent, 0.16f)
    /** Lighter version (same hue) for highlights on top of the accent. */
    val bright: Color = lerp(accent, Color.White, 0.25f)
}

val LocalFocusPalette = staticCompositionLocalOf { DarkPalette }
val LocalAccentPalette = staticCompositionLocalOf { AccentPalette(Color(AccentColor.Default.argb)) }

/** Theme-aware tone of a subject or accent color. */
@Composable
@ReadOnlyComposable
fun AccentColor.toColor(): Color = Color(argb(LocalFocusPalette.current.isDark))

/** Neutral palette of the current theme + the current accent. */
object FocusColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.surface
    val SurfaceRaised: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.surfaceRaised
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.surfaceHigh
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.outline
    val OutlineStrong: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.outlineStrong

    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.textTertiary

    /** Comparison series (previous period): neutral, so it never competes with the accent. */
    val Comparison: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.comparison

    val Error: Color @Composable @ReadOnlyComposable get() = LocalFocusPalette.current.error

    /** Content drawn on top of the accent (button labels). */
    val OnAccent: Color @Composable @ReadOnlyComposable get() =
        if (LocalFocusPalette.current.isDark) LocalFocusPalette.current.background else Color.White

    val Accent: Color @Composable @ReadOnlyComposable get() = LocalAccentPalette.current.accent
    val AccentMuted: Color @Composable @ReadOnlyComposable get() = LocalAccentPalette.current.muted

    /** Faint accent tint for selected chips and status pills (replaces outlined states). */
    val AccentDim: Color @Composable @ReadOnlyComposable get() = LocalAccentPalette.current.dim
}
