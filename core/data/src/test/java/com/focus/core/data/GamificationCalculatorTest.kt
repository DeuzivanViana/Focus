package com.focus.core.data

import com.focus.core.data.gamification.GamificationCalculator
import com.focus.core.model.Achievement
import com.focus.core.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GamificationCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 11, 20)

    private fun pomodoro(date: LocalDate, hour: Int) =
        date.atTime(hour, 0).toInstant(zone).toEpochMilli().let {
            FocusSession(startedAt = it, endedAt = it + 25 * 60_000, focusedSeconds = 25 * 60, completed = true)
        }

    @Test
    fun levelCurve() {
        assertEquals(1, GamificationCalculator.levelFor(0))
        assertEquals(1, GamificationCalculator.levelFor(99))
        assertEquals(2, GamificationCalculator.levelFor(100))
        assertEquals(3, GamificationCalculator.levelFor(300))
        assertEquals(4, GamificationCalculator.levelFor(600))
    }

    @Test
    fun xpAndAchievements() {
        val sessions = (8..11).map { pomodoro(day, it) } + pomodoro(day.minusDays(1), 6)
        val state = GamificationCalculator.compute(sessions, dailyGoalMinutes = 100, zone = zone)
        // 125 min + 5 pomodoros * 10 + 1 goal day * 25 = 200
        assertEquals(200L, state.xp)
        assertEquals(2, state.level)
        val unlocked = state.achievements.filter { it.unlocked }.map { it.achievement }.toSet()
        assertTrue(Achievement.FIRST_FOCUS in unlocked)
        assertTrue(Achievement.FULL_CYCLE in unlocked)
        assertTrue(Achievement.STREAK_3 !in unlocked)
        assertFalse(Achievement.EARLY_BIRD in unlocked)
        assertEquals(1, state.achievements.first { it.achievement == Achievement.EARLY_BIRD }.current)
    }
}
