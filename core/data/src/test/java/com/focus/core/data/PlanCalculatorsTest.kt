package com.focus.core.data

import com.focus.core.data.plan.ReminderEvent
import com.focus.core.data.plan.ReminderPlanner
import com.focus.core.data.plan.WeeklySummaryCalculator
import com.focus.core.model.FocusSession
import com.focus.core.model.ReminderSettings
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import com.focus.core.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class PlanCalculatorsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 11, 16)
    private fun millis(d: LocalDate, h: Int, m: Int = 0) = LocalDateTime.of(d, java.time.LocalTime.of(h, m)).toInstant(zone).toEpochMilli()

    @Test
    fun reviewScheduleIs1_7_30() {
        val r = ReviewItem(title = "Ch. 3", studiedOn = monday)
        assertEquals(monday.plusDays(1), r.dueOn)
        assertEquals(monday.plusDays(7), r.copy(stage = 1).dueOn)
        assertEquals(monday.plusDays(30), r.copy(stage = 2).dueOn)
        assertNull(r.copy(stage = 3).dueOn)
        assertTrue(r.copy(stage = 3).isFinished)
    }

    @Test
    fun nextRoutineReminderRespectsDaysAndLeadTime() {
        val block = RoutineBlock(id = 1, title = "Math", days = setOf(DayOfWeek.WEDNESDAY), startMinute = 19 * 60, durationMinutes = 50, remindBefore = 10)
        val next = ReminderPlanner.next(millis(monday, 12), zone, listOf(block), emptyList(), ReminderSettings(weeklySummary = false))
        val e = next.single() as ReminderEvent.Routine
        assertEquals(millis(monday.plusDays(2), 18, 50), e.at)
    }

    @Test
    fun reviewReminderOnlyWhenSomethingIsDueAndWeeklyOnSunday() {
        val review = ReviewItem(id = 1, title = "Ch. 3", studiedOn = monday) // due Tuesday
        val events = ReminderPlanner.upcoming(millis(monday, 12), zone, emptyList(), listOf(review), ReminderSettings())
        val firstReview = events.first { it is ReminderEvent.Reviews }
        assertEquals(millis(monday.plusDays(1), 9), firstReview.at)
        val weekly = events.first { it is ReminderEvent.WeeklySummary }
        assertEquals(millis(monday.plusDays(6), 19), weekly.at)
        // Everything off: nothing scheduled.
        assertTrue(ReminderPlanner.upcoming(millis(monday, 12), zone, emptyList(), listOf(review), ReminderSettings(false, false, 540, false)).isEmpty())
    }

    @Test
    fun weeklySummaryComparesWithPreviousWeek() {
        val sunday = monday.plusDays(6)
        fun s(d: LocalDate, min: Long, subject: Long? = null) = FocusSession(startedAt = millis(d, 10), endedAt = millis(d, 11), focusedSeconds = min * 60, completed = true, subjectId = subject)
        val sessions = listOf(s(monday, 120, 1), s(monday.plusDays(2), 60, 2), s(sunday, 30, 1), s(monday.minusDays(3), 100))
        val w = WeeklySummaryCalculator.compute(sessions, listOf(Subject(1, "English"), Subject(2, "Math")), 60, sunday, zone)
        assertEquals(210 * 60L, w.totalSeconds)
        assertEquals(100 * 60L, w.previousSeconds)
        assertEquals(110, w.changePercent)
        assertEquals(2, w.daysGoalMet)
        assertEquals("English", w.topSubject?.name)
    }
}
