package com.focus.core.model

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val dailyGoalMinutes: Int = 120,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val accent: AccentColor = AccentColor.Default,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val timerStyle: TimerStyle = TimerStyle.RING,
    /** Planned rest days: a day off on one of these does not break the streak. */
    val restDays: Set<java.time.DayOfWeek> = emptySet(),
    val focusSound: FocusSound = FocusSound.NONE,
    /** Focus sound volume, 0..100. */
    val soundVolume: Int = 60,
) {
    fun minutesFor(phase: TimerPhase): Int = when (phase) {
        TimerPhase.FOCUS -> focusMinutes
        TimerPhase.SHORT_BREAK -> shortBreakMinutes
        TimerPhase.LONG_BREAK -> longBreakMinutes
    }

    /** Makes sure no out-of-range value ever gets into the app. */
    fun sanitized(): TimerSettings = copy(
        focusMinutes = focusMinutes.coerceIn(FOCUS_RANGE),
        shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_BREAK_RANGE),
        longBreakMinutes = longBreakMinutes.coerceIn(LONG_BREAK_RANGE),
        sessionsBeforeLongBreak = sessionsBeforeLongBreak.coerceIn(CYCLE_RANGE),
        dailyGoalMinutes = dailyGoalMinutes.coerceIn(GOAL_RANGE),
        restDays = restDays.sorted().take(MAX_REST_DAYS).toSet(),
        soundVolume = soundVolume.coerceIn(0, 100),
    )

    companion object {
        val FOCUS_RANGE = 5..120
        val SHORT_BREAK_RANGE = 1..30
        val LONG_BREAK_RANGE = 5..60
        val CYCLE_RANGE = 2..8
        val GOAL_RANGE = 15..720

        /** At most two rest days a week, so the streak still means a habit. */
        const val MAX_REST_DAYS = 2

        fun restDaysToMask(days: Set<java.time.DayOfWeek>): Int = days.fold(0) { m, d -> m or (1 shl (d.value - 1)) }
        fun maskToRestDays(mask: Int): Set<java.time.DayOfWeek> =
            java.time.DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
    }
}

/** Background sound during a focus. All are synthesized on the device: no audio files. */
enum class FocusSound {
    NONE, RAIN, WHITE, PINK, BROWN, OCEAN;

    companion object {
        fun fromName(name: String?): FocusSound = entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** Look of the countdown on the focus screen. Purely cosmetic. */
enum class TimerStyle {
    /** Thin ring that fills clockwise. */
    RING,

    /** Sixty radial ticks, one per "second" of the ring. */
    TICKS,

    /** Thick 270 degree gauge, open at the bottom. */
    GAUGE,

    /** Analog dial: a filled wedge sweeps like a kitchen timer. */
    DIAL,

    /** No circle: large digits over a short line. */
    MINIMAL;

    companion object {
        fun fromName(name: String?): TimerStyle = entries.firstOrNull { it.name == name } ?: RING
    }
}

/**
 * Appearance. SYSTEM follows the device setting. OLED is the dark theme on pure black,
 * which turns those pixels off on OLED screens (less power, deeper contrast).
 * Stored by name, so the order here is only the order shown in Settings.
 */
enum class ThemeMode {
    SYSTEM, DARK, OLED, LIGHT;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
