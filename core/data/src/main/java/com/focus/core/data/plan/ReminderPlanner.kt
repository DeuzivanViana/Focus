package com.focus.core.data.plan

import com.focus.core.model.ReminderSettings
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Something to notify about, at [at] (epoch millis). */
sealed interface ReminderEvent {
    val at: Long

    data class Routine(override val at: Long, val block: RoutineBlock, val day: LocalDate) : ReminderEvent
    data class Reviews(override val at: Long, val due: List<ReviewItem>) : ReminderEvent
    data class WeeklySummary(override val at: Long, val weekEnd: LocalDate) : ReminderEvent
}

/** Works out the upcoming reminders. Pure, so it is easy to test. */
object ReminderPlanner {
    /** Sunday evening, when the week is wrapping up. */
    val WEEKLY_SUMMARY_TIME: LocalTime = LocalTime.of(19, 0)
    private const val HORIZON_DAYS = 8L

    /** All events strictly after [afterMillis] within the next 8 days, earliest first. */
    fun upcoming(
        afterMillis: Long,
        zone: ZoneId,
        blocks: List<RoutineBlock>,
        reviews: List<ReviewItem>,
        settings: ReminderSettings,
    ): List<ReminderEvent> {
        val start = java.time.Instant.ofEpochMilli(afterMillis).atZone(zone).toLocalDate()
        val out = ArrayList<ReminderEvent>()
        for (offset in -1L..HORIZON_DAYS) { // -1: a reminder before midnight for a block just after it
            val day = start.plusDays(offset)
            if (settings.routine) {
                blocks.filter { day.dayOfWeek in it.days && it.remindBefore != null }.forEach { b ->
                    val at = at(day, b.startMinute - b.remindBefore!!, zone)
                    if (at > afterMillis) out += ReminderEvent.Routine(at, b, day)
                }
            }
            if (settings.reviews) {
                val due = reviews.filter { r -> r.dueOn?.let { !it.isAfter(day) } == true }
                val at = at(day, settings.reviewMinute, zone)
                if (due.isNotEmpty() && at > afterMillis) out += ReminderEvent.Reviews(at, due)
            }
            if (settings.weeklySummary && day.dayOfWeek == DayOfWeek.SUNDAY) {
                val at = ZonedDateTime.of(day, WEEKLY_SUMMARY_TIME, zone).toInstant().toEpochMilli()
                if (at > afterMillis) out += ReminderEvent.WeeklySummary(at, day)
            }
        }
        return out.sortedBy { it.at }
    }

    /** The earliest events (several can share the same minute). */
    fun next(afterMillis: Long, zone: ZoneId, blocks: List<RoutineBlock>, reviews: List<ReviewItem>, settings: ReminderSettings): List<ReminderEvent> {
        val all = upcoming(afterMillis, zone, blocks, reviews, settings)
        val first = all.firstOrNull() ?: return emptyList()
        return all.takeWhile { it.at == first.at }
    }

    /** Minutes can be negative (reminder the day before) or past midnight. */
    private fun at(day: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
        day.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()
}
