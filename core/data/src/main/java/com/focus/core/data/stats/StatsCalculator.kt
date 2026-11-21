package com.focus.core.data.stats

import com.focus.core.model.DayStat
import com.focus.core.model.FocusSession
import com.focus.core.model.FocusStats
import com.focus.core.model.Subject
import com.focus.core.model.SubjectTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Pure functions (no Android) so metrics are easy to test and evolve. */
object StatsCalculator {

    fun compute(
        sessions: List<FocusSession>,
        dailyGoalMinutes: Int,
        today: LocalDate,
        zone: ZoneId,
        subjects: List<Subject> = emptyList(),
        restDays: Set<DayOfWeek> = emptySet(),
    ): FocusStats {
        val byDay = HashMap<LocalDate, DayStat>()
        val hours = LongArray(24)
        for (s in sessions) {
            val start = Instant.ofEpochMilli(s.startedAt).atZone(zone)
            val date = start.toLocalDate()
            val prev = byDay[date]
            byDay[date] = DayStat(
                date = date,
                focusedSeconds = (prev?.focusedSeconds ?: 0) + s.focusedSeconds,
                sessions = (prev?.sessions ?: 0) + 1,
            )
            hours[start.hour] += s.focusedSeconds
        }

        fun dayStat(d: LocalDate) = byDay[d] ?: DayStat(d, 0, 0)
        val last7 = (6 downTo 0).map { dayStat(today.minusDays(it.toLong())) }
        val last30 = (29 downTo 0).map { dayStat(today.minusDays(it.toLong())) }
        val prev7 = (13 downTo 7).map { dayStat(today.minusDays(it.toLong())) }
        val prev30 = (59 downTo 30).map { dayStat(today.minusDays(it.toLong())) }
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekSeconds = byDay.values
            .filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
            .sumOf { it.focusedSeconds }
        val goalSeconds = dailyGoalMinutes * 60L

        return FocusStats(
            todaySeconds = dayStat(today).focusedSeconds,
            weekSeconds = weekSeconds,
            totalSeconds = sessions.sumOf { it.focusedSeconds },
            currentStreak = currentStreak(byDay.keys, today, restDays),
            bestStreak = bestStreak(byDay.keys, restDays),
            completedSessions = sessions.count { it.completed },
            totalSessions = sessions.size,
            dailyGoalMinutes = dailyGoalMinutes,
            averageDailySeconds30 = last30.sumOf { it.focusedSeconds } / 30,
            goalDaysLast7 = last7.count { goalSeconds > 0 && it.focusedSeconds >= goalSeconds },
            bestDay = byDay.values.maxByOrNull { it.focusedSeconds },
            last7Days = last7,
            last30Days = last30,
            previous7Days = prev7,
            previous30Days = prev30,
            hourDistribution = hours.toList(),
            recentSessions = sessions.sortedByDescending { it.startedAt }.take(10),
            subjectTimes = subjectTimes(sessions, subjects, today, zone),
            yearDays = heatmapRange(today).map(::dayStat),
            restDays = restDays,
        )
    }

    fun subjectTimes(sessions: List<FocusSession>, subjects: List<Subject>, today: LocalDate, zone: ZoneId): List<SubjectTime> {
        val byId = subjects.associateBy { it.id }
        val from7 = today.minusDays(6)
        val from30 = today.minusDays(29)
        data class Acc(var d7: Long = 0, var d30: Long = 0, var total: Long = 0)
        val acc = LinkedHashMap<Long?, Acc>()
        subjects.filterNot { it.archived }.forEach { acc[it.id] = Acc() }
        for (s in sessions) {
            val key = s.subjectId?.takeIf { it in byId }
            val a = acc.getOrPut(key) { Acc() }
            val date = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()
            a.total += s.focusedSeconds
            if (!date.isBefore(from30)) a.d30 += s.focusedSeconds
            if (!date.isBefore(from7)) a.d7 += s.focusedSeconds
        }
        return acc.map { (id, a) -> SubjectTime(id?.let { byId[it] }, a.d7, a.d30, a.total) }
            .filter { it.subject != null || it.totalSeconds > 0 }
            .sortedWith(compareByDescending<SubjectTime> { it.last30DaysSeconds }.thenByDescending { it.totalSeconds })
    }

    /** 53 week columns ending today, starting on a Monday. */
    fun heatmapRange(today: LocalDate): List<LocalDate> {
        val start = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
    }

    /**
     * Consecutive days with focus up to today. If there is no focus today yet,
     * yesterday's streak still counts (the user still has the day to keep it).
     * A planned rest day without focus is skipped: it neither breaks nor adds to the streak.
     * A rest day with focus counts like any other day.
     */
    fun currentStreak(activeDays: Set<LocalDate>, today: LocalDate, restDays: Set<DayOfWeek> = emptySet()): Int {
        val earliest = activeDays.minOrNull() ?: return 0
        var day = today
        var count = 0
        while (!day.isBefore(earliest)) {
            when {
                day in activeDays -> count++
                day.dayOfWeek in restDays || day == today -> Unit
                else -> break
            }
            day = day.minusDays(1)
        }
        return count
    }

    fun bestStreak(activeDays: Set<LocalDate>, restDays: Set<DayOfWeek> = emptySet()): Int {
        val sorted = activeDays.sorted()
        if (sorted.isEmpty()) return 0
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in sorted) {
            run = if (prev != null && onlyRestDaysBetween(prev, d, restDays)) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    /** True when every day strictly between [from] and [to] is a planned rest day. */
    private fun onlyRestDaysBetween(from: LocalDate, to: LocalDate, restDays: Set<DayOfWeek>): Boolean {
        var d = from.plusDays(1)
        while (d.isBefore(to)) {
            if (d.dayOfWeek !in restDays) return false
            d = d.plusDays(1)
        }
        return true
    }
}
