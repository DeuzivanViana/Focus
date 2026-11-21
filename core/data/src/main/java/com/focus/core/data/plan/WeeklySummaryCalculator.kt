package com.focus.core.data.plan

import com.focus.core.model.FocusSession
import com.focus.core.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeeklySummary(
    val totalSeconds: Long,
    val previousSeconds: Long,
    val daysGoalMet: Int,
    val sessions: Int,
    val topSubject: Subject?,
) {
    /** Change vs the previous week in percent, null when there is nothing to compare. */
    val changePercent: Int? get() = if (previousSeconds <= 0) null else (((totalSeconds - previousSeconds) * 100) / previousSeconds).toInt()
}

object WeeklySummaryCalculator {
    /** The 7 days ending on [weekEnd] (a Sunday for the weekly notification) vs the 7 before. */
    fun compute(sessions: List<FocusSession>, subjects: List<Subject>, dailyGoalMinutes: Int, weekEnd: LocalDate, zone: ZoneId): WeeklySummary {
        val start = weekEnd.minusDays(6)
        val prevStart = start.minusDays(7)
        fun day(s: FocusSession) = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()
        val week = sessions.filter { val d = day(it); !d.isBefore(start) && !d.isAfter(weekEnd) }
        val prev = sessions.filter { val d = day(it); !d.isBefore(prevStart) && d.isBefore(start) }
        val goalMet = week.groupBy(::day).count { (_, l) -> l.sumOf { it.focusedSeconds } >= dailyGoalMinutes * 60L }
        val top = week.filter { it.subjectId != null }.groupBy { it.subjectId }
            .maxByOrNull { (_, l) -> l.sumOf { it.focusedSeconds } }?.key
        return WeeklySummary(
            totalSeconds = week.sumOf { it.focusedSeconds },
            previousSeconds = prev.sumOf { it.focusedSeconds },
            daysGoalMet = goalMet,
            sessions = week.size,
            topSubject = subjects.firstOrNull { it.id == top },
        )
    }
}
