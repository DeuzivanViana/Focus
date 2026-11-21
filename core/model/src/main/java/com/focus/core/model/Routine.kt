package com.focus.core.model

import java.time.DayOfWeek

/**
 * A fixed study block in the weekly routine, e.g. "Calculus, Mon/Wed 19:00, 50 min".
 * [days] holds the weekdays it repeats on.
 */
data class RoutineBlock(
    val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val days: Set<DayOfWeek>,
    /** Minutes after midnight, local time. */
    val startMinute: Int,
    val durationMinutes: Int,
    /** Minutes before the start to remind; null = no reminder. */
    val remindBefore: Int? = 10,
) {
    val endMinute: Int get() = startMinute + durationMinutes

    companion object {
        const val MAX_TITLE_LENGTH = 50
        val DURATION_RANGE = 5..240
        val REMIND_OPTIONS = listOf(0, 5, 10, 15, 30)

        /** Bit 0 = Monday ... bit 6 = Sunday. */
        fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
        fun maskToDays(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
    }
}

/**
 * Something studied that should be reviewed after 1, 7 and 30 days (spaced repetition).
 * [stage] counts the reviews already done (0..3); 3 means finished.
 */
data class ReviewItem(
    val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val studiedOn: java.time.LocalDate,
    val stage: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isFinished: Boolean get() = stage >= INTERVALS.size

    /** Date of the next review, or null when finished. */
    val dueOn: java.time.LocalDate? get() = INTERVALS.getOrNull(stage)?.let { studiedOn.plusDays(it.toLong()) }

    companion object {
        const val MAX_TITLE_LENGTH = 60
        /** Days after the study day: the classic 1-7-30 schedule. */
        val INTERVALS = listOf(1, 7, 30)
    }
}

/** User choices for reminders. All local notifications. */
data class ReminderSettings(
    val routine: Boolean = true,
    val reviews: Boolean = true,
    /** Minutes after midnight for the daily "reviews due" reminder. */
    val reviewMinute: Int = 9 * 60,
    val weeklySummary: Boolean = true,
)
