package com.focus.core.model

import java.time.LocalDate

data class DayStat(
    val date: LocalDate,
    val focusedSeconds: Long,
    val sessions: Int,
)

data class FocusStats(
    val todaySeconds: Long = 0,
    val weekSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedSessions: Int = 0,
    val totalSessions: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val averageDailySeconds30: Long = 0,
    val goalDaysLast7: Int = 0,
    val bestDay: DayStat? = null,
    val last7Days: List<DayStat> = emptyList(),
    val last30Days: List<DayStat> = emptyList(),
    /** The 7 days right before [last7Days] (for comparison). */
    val previous7Days: List<DayStat> = emptyList(),
    /** The 30 days right before [last30Days] (for comparison). */
    val previous30Days: List<DayStat> = emptyList(),
    /** Seconds focused per hour of day (0..23) across the whole history. */
    val hourDistribution: List<Long> = List(24) { 0L },
    val recentSessions: List<FocusSession> = emptyList(),
    /** Focus time per subject, most studied first. */
    val subjectTimes: List<SubjectTime> = emptyList(),
    /** Every day of the heatmap: from the first day of the week 52 weeks ago to today. */
    val yearDays: List<DayStat> = emptyList(),
    /** Planned rest days in effect (for the heatmap). */
    val restDays: Set<java.time.DayOfWeek> = emptySet(),
) {
    val completionRate: Float
        get() = if (totalSessions == 0) 0f else completedSessions.toFloat() / totalSessions

    val todayGoalProgress: Float
        get() = if (dailyGoalMinutes <= 0) 0f else (todaySeconds / 60f) / dailyGoalMinutes

    val peakHour: Int?
        get() = hourDistribution.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
}
