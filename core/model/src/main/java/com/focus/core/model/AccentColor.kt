package com.focus.core.model

/**
 * Available accent colors. Each has two tones with the same OKLCH hue:
 * - [argb] for the dark theme: about L 0.72, C 0.15, so every choice has the
 *   same contrast on black.
 * - [argbLight] for the light theme: darker (L 0.55-0.59), picked so each one
 *   reaches at least 4.5:1 contrast on white (WCAG AA for text).
 * Either way the UI stays a neutral base plus a single accent.
 */
enum class AccentColor(val argb: Long, val argbLight: Long) {
    BLUE(0xFF4C8DFF, 0xFF3271E1),
    INDIGO(0xFF7B83FF, 0xFF6469E2),
    VIOLET(0xFFA884FF, 0xFF8660D7),
    CYAN(0xFF22BFE0, 0xFF0A8098),
    TEAL(0xFF2CC9A0, 0xFF018568),
    GREEN(0xFF6CCB5F, 0xFF258815),
    AMBER(0xFFF0B03F, 0xFF9C6D0F),
    CORAL(0xFFFF7E67, 0xFFC94E39),
    ROSE(0xFFF06EAA, 0xFFC64885);

    fun argb(dark: Boolean): Long = if (dark) argb else argbLight

    companion object {
        val Default = BLUE
        fun fromName(name: String?): AccentColor = entries.firstOrNull { it.name == name } ?: Default
    }
}
