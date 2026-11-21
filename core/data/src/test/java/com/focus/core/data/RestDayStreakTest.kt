package com.focus.core.data

import com.focus.core.data.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RestDayStreakTest {
    // 2026-11-16 is a Monday.
    private val mon = LocalDate.of(2026, 11, 16)
    private fun days(vararg offsets: Long) = offsets.map { mon.plusDays(it) }.toSet()

    @Test
    fun restDayWithoutFocusKeepsStreak() {
        // Mon..Fri focus, Sat rest (no focus), Sun focus.
        val active = days(0, 1, 2, 3, 4, 6)
        assertEquals(6, StatsCalculator.currentStreak(active, mon.plusDays(6), setOf(DayOfWeek.SATURDAY)))
        assertEquals(6, StatsCalculator.bestStreak(active, setOf(DayOfWeek.SATURDAY)))
    }

    @Test
    fun withoutRestDaysTheGapBreaks() {
        val active = days(0, 1, 2, 3, 4, 6)
        assertEquals(1, StatsCalculator.currentStreak(active, mon.plusDays(6)))
        assertEquals(5, StatsCalculator.bestStreak(active))
    }

    @Test
    fun missedWorkdayStillBreaks() {
        // Wed missed (not a rest day).
        val active = days(0, 1, 3, 4)
        assertEquals(2, StatsCalculator.currentStreak(active, mon.plusDays(4), setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
    }

    @Test
    fun todayRestDayAndYesterdayGrace() {
        // Focus Mon..Fri; today is Sat (rest), nothing yet: streak still 5.
        val active = days(0, 1, 2, 3, 4)
        assertEquals(5, StatsCalculator.currentStreak(active, mon.plusDays(5), setOf(DayOfWeek.SATURDAY)))
        // Sunday not a rest day and nothing today: grace for today, Saturday skipped.
        assertEquals(5, StatsCalculator.currentStreak(active, mon.plusDays(6), setOf(DayOfWeek.SATURDAY)))
        // Monday next week without focus on Sunday (not rest): broken.
        assertEquals(0, StatsCalculator.currentStreak(active, mon.plusDays(7), setOf(DayOfWeek.SATURDAY)))
    }

    @Test
    fun heatmapStartsOnMondayAndEndsToday() {
        val today = LocalDate.of(2026, 11, 21)
        val range = StatsCalculator.heatmapRange(today)
        assertEquals(DayOfWeek.MONDAY, range.first().dayOfWeek)
        assertEquals(today, range.last())
        assertEquals(53, (range.size + 6) / 7)
    }
}
