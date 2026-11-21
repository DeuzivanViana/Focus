package com.focus.core.data

import com.focus.core.data.stats.StatsCalculator
import com.focus.core.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class StatsCalculatorTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 21) // Saturday

    private fun session(date: LocalDate, hour: Int, minutes: Long, completed: Boolean = true): FocusSession {
        val start = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return FocusSession(startedAt = start, endedAt = start + minutes * 60_000, focusedSeconds = minutes * 60, completed = completed)
    }

    @Test
    fun streakKeepsAliveWhenTodayIsEmpty() {
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, StatsCalculator.currentStreak(days, today))
    }

    @Test
    fun streakBreaksAfterMissedDay() {
        val days = setOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, StatsCalculator.currentStreak(days, today))
    }

    @Test
    fun bestStreakFindsLongestRun() {
        val days = setOf(1, 2, 3, 5, 6, 7, 8, 10).map { today.minusDays(it.toLong()) }.toSet()
        assertEquals(4, StatsCalculator.bestStreak(days))
    }

    @Test
    fun aggregatesTodayWeekAndHours() {
        val sessions = listOf(
            session(today, 9, 25),
            session(today, 9, 25, completed = false),
            session(today.minusDays(5), 14, 50), // Monday of this week
            session(today.minusDays(6), 20, 25), // previous Sunday: outside this week
        )
        val stats = StatsCalculator.compute(sessions, dailyGoalMinutes = 50, today = today, zone = zone)
        assertEquals(50 * 60L, stats.todaySeconds)
        assertEquals(100 * 60L, stats.weekSeconds)
        assertEquals(125 * 60L, stats.totalSeconds)
        assertEquals(3, stats.completedSessions)
        assertEquals(4, stats.totalSessions)
        assertEquals(2, stats.goalDaysLast7)
        assertEquals(9, stats.peakHour)
        assertEquals(7, stats.last7Days.size)
        assertEquals(1, stats.currentStreak)
        assertEquals(7, stats.previous7Days.size)
        assertEquals(today.minusDays(7), stats.previous7Days.last().date)
        assertEquals(25 * 60L, stats.last7Days.first().focusedSeconds) // previous Sunday is part of the last 7 days
        assertEquals(30, stats.previous30Days.size)
    }
}
