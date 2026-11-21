package com.focus.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import com.focus.core.model.AccentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Corner scale, after Material 3's shape scale: one radius per role, larger containers get
 * rounder corners. Nested elements use a smaller radius than their container
 * (inner radius = outer radius - padding), so corners stay concentric.
 */
val FocusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun typography(p: FocusPalette): Typography {
    val base = TextStyle(fontFamily = FontFamily.SansSerif, color = p.textPrimary)
    return Typography(
        displayLarge = base.copy(fontSize = 72.sp, fontWeight = FontWeight.Thin, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
        headlineLarge = base.copy(fontSize = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp),
        headlineMedium = base.copy(fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.2).sp, fontFeatureSettings = "tnum"),
        titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Normal),
        titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
        titleSmall = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
        bodyLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal),
        bodyMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
        bodySmall = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = p.textSecondary),
        labelLarge = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
        labelMedium = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
        labelSmall = base.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    )
}

private fun colorScheme(n: FocusPalette, p: AccentPalette): ColorScheme {
    val onAccent = if (n.isDark) n.background else Color.White
    return if (n.isDark) {
        darkColorScheme(
        primary = p.accent,
        onPrimary = onAccent,
        primaryContainer = p.muted,
        onPrimaryContainer = n.textPrimary,
        secondary = n.textSecondary,
        onSecondary = onAccent,
        background = n.background,
        onBackground = n.textPrimary,
        surface = n.surface,
        onSurface = n.textPrimary,
        surfaceVariant = n.surfaceRaised,
        onSurfaceVariant = n.textSecondary,
        surfaceTint = Color.Transparent,
        surfaceContainer = n.surfaceRaised,
        surfaceContainerHigh = n.surfaceHigh,
        surfaceContainerHighest = n.surfaceHigh,
        surfaceContainerLow = n.surface,
        surfaceContainerLowest = n.background,
        outline = n.outlineStrong,
        outlineVariant = n.outline,
        error = n.error,
        )
    } else {
        lightColorScheme(
        primary = p.accent,
        onPrimary = onAccent,
        primaryContainer = p.muted,
        onPrimaryContainer = n.textPrimary,
        secondary = n.textSecondary,
        onSecondary = onAccent,
        background = n.background,
        onBackground = n.textPrimary,
        surface = n.surface,
        onSurface = n.textPrimary,
        surfaceVariant = n.surfaceRaised,
        onSurfaceVariant = n.textSecondary,
        surfaceTint = Color.Transparent,
        surfaceContainer = n.surfaceRaised,
        surfaceContainerHigh = n.surfaceHigh,
        surfaceContainerHighest = n.surfaceHigh,
        surfaceContainerLow = n.surface,
        surfaceContainerLowest = n.background,
        outline = n.outlineStrong,
        outlineVariant = n.outline,
        error = n.error,
        )
    }
}

/**
 * App theme. [dark] defaults to the system setting; Settings can force one through
 * AppCompat's night mode, which also drives [isSystemInDarkTheme]. Accent changes
 * cross-fade instead of jumping.
 */
@Composable
fun FocusTheme(
    accent: AccentColor = AccentColor.Default,
    dark: Boolean = isSystemInDarkTheme(),
    oled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val neutral = when {
        !dark -> LightPalette
        oled -> OledPalette
        else -> DarkPalette
    }
    val accentColor by animateColorAsState(Color(accent.argb(dark)), tween(400), label = "accent")
    val palette = remember(accentColor, neutral) { AccentPalette(accentColor, neutral.surface) }
    val typography = remember(neutral) { typography(neutral) }
    CompositionLocalProvider(LocalFocusPalette provides neutral, LocalAccentPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme(neutral, palette),
            typography = typography,
            shapes = FocusShapes,
            content = content,
        )
    }
}
