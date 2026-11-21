package com.focus.core.timer

import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus

data class TimerState(
    val phase: TimerPhase = TimerPhase.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalMillis: Long = 25 * 60_000L,
    val remainingMillis: Long = 25 * 60_000L,
    /** Focus sessions completed in the current cycle (reset after the long break). */
    val completedFocusInCycle: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    /** Instant (SystemClock.elapsedRealtime) when the phase ends, while running. */
    val endAtElapsed: Long? = null,
) {
    val progress: Float
        get() = if (totalMillis <= 0) 0f else 1f - remainingMillis.toFloat() / totalMillis

    val isActive: Boolean get() = status != TimerStatus.IDLE
}
