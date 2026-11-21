package com.focus.app

import androidx.appcompat.app.AppCompatDelegate
import com.focus.core.model.ThemeMode

/**
 * Applies the appearance through AppCompat's night mode, so window backgrounds,
 * system bars and every AppCompat activity (including the block screen) agree.
 */
fun applyThemeMode(mode: ThemeMode) {
    val night = when (mode) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.DARK, ThemeMode.OLED -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    }
    if (AppCompatDelegate.getDefaultNightMode() != night) AppCompatDelegate.setDefaultNightMode(night)
}
