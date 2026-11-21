package com.focus.core.data

import com.focus.core.data.goals.GoalCalculator
import com.focus.core.model.FocusSession
import com.focus.core.model.Goal
import com.focus.core.model.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GoalCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val start = LocalDate.of(2026, 11, 1)
    private val goal = Goal(id = 1, title = "100h English", subjectId = 7, targetMinutes = 100 * 60, startDate = start, deadline = start.plusDays(99))

    private fun hours(date: LocalDate, h: Long, subject: Long? = 7) = date.atTime(9, 0).toInstant(zone).toEpochMilli().let {
        FocusSession(startedAt = it, endedAt = it + h * 3_600_000, focusedSeconds = h * 3600, completed = true, subjectId = subject)
    }

    @Test
    fun onTrackPaceProjectsFinishOnDeadline() {
        val today = start.plusDays(9) // day 10 of 100
        val sessions = (0..9).map { hours(start.plusDays(it.toLong()), 1) } + hours(today, 5, subject = 3) // other subject ignored
        val p = GoalCalculator.progress(goal, null, sessions, today, zone)
        assertEquals(10 * 3600L, p.doneSeconds)
        assertEquals(10, p.daysElapsed)
        assertEquals(91, p.daysRemaining)
        assertEquals(GoalStatus.ON_TRACK, p.status)
        assertEquals(start.plusDays(99), p.projectedFinish)
        assertTrue(p.neededSecondsPerDay in 3500L..3600L) // about 1h/day
    }

    @Test
    fun behindAndAhead() {
        val today = start.plusDays(9)
        val slow = GoalCalculator.progress(goal, null, listOf(hours(start, 2)), today, zone)
        assertEquals(GoalStatus.BEHIND, slow.status)
        val fast = GoalCalculator.progress(goal, null, (0..9).map { hours(start.plusDays(it.toLong()), 3) }, today, zone)
        assertEquals(GoalStatus.AHEAD, fast.status)
    }

    @Test
    fun completedAndExpired() {
        val done = GoalCalculator.progress(goal.copy(targetMinutes = 60), null, listOf(hours(start, 2)), start.plusDays(1), zone)
        assertEquals(GoalStatus.COMPLETED, done.status)
        val late = GoalCalculator.progress(goal, null, listOf(hours(start, 2)), start.plusDays(120), zone)
        assertEquals(GoalStatus.EXPIRED, late.status)
    }
}
