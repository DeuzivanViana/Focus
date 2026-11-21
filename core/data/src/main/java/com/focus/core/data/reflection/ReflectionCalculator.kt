package com.focus.core.data.reflection

import com.focus.core.model.DayPart
import com.focus.core.model.FocusSession
import com.focus.core.model.ReflectionInsights
import com.focus.core.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReflectionCalculator {
    fun insights(sessions: List<FocusSession>, subjects: List<Subject>, today: LocalDate, zone: ZoneId): ReflectionInsights {
        val rated = sessions.filter { it.rating != null }
        if (rated.isEmpty()) return ReflectionInsights()
        fun List<FocusSession>.avg(): Float? = if (isEmpty()) null else map { it.rating!! }.average().toFloat()

        val from30 = today.minusDays(29)
        val last30 = rated.filter { !Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate().isBefore(from30) }
        val byPart = rated.groupBy { DayPart.of(Instant.ofEpochMilli(it.startedAt).atZone(zone).hour) }
            .mapValues { (_, l) -> l.avg()!! }
        val byId = subjects.associateBy { it.id }
        val bySubject = rated.groupBy { it.subjectId?.takeIf { id -> id in byId } }
            .entries.sortedByDescending { it.value.size }
            .map { (id, l) -> id?.let { byId[it] } to l.avg()!! }

        return ReflectionInsights(
            ratedSessions = rated.size,
            averageRating30Days = last30.avg(),
            byDayPart = byPart,
            bySubject = bySubject,
            completedAverage = rated.filter { it.completed }.avg(),
            interruptedAverage = rated.filter { !it.completed }.avg(),
            recentNotes = rated.filter { !it.note.isNullOrBlank() }.sortedByDescending { it.startedAt }.take(8),
        )
    }
}
