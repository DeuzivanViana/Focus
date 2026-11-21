package com.focus.core.designsystem.util

/** "1h 05m", "45m", "0m". */
fun formatDuration(seconds: Long): String {
    val totalMinutes = seconds / 60
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 -> "%dh %02dm".format(h, m)
        else -> "%dm".format(m)
    }
}

/** "25:00" from milliseconds (rounds up, like a timer). */
fun formatClock(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
