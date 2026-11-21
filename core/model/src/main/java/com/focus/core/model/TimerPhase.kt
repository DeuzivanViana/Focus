package com.focus.core.model

enum class TimerPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK;

    val isBreak: Boolean get() = this != FOCUS
}

enum class TimerStatus { IDLE, RUNNING, PAUSED }
