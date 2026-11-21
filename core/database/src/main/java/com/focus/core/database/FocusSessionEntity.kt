package com.focus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focus.core.model.FocusSession

@Entity(
    tableName = "focus_sessions",
    indices = [Index("started_at"), Index("subject_id")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "focused_seconds") val focusedSeconds: Long,
    @ColumnInfo(name = "completed") val completed: Boolean,
    // No foreign key on purpose: deleting a subject keeps its history (sessions become "no subject").
    @ColumnInfo(name = "subject_id") val subjectId: Long? = null,
    @ColumnInfo(name = "task_id") val taskId: Long? = null,
    @ColumnInfo(name = "rating") val rating: Int? = null,
    @ColumnInfo(name = "note") val note: String? = null,
)

fun FocusSessionEntity.asModel() = FocusSession(id, startedAt, endedAt, focusedSeconds, completed, subjectId, taskId, rating, note)

fun FocusSession.asEntity() = FocusSessionEntity(id, startedAt, endedAt, focusedSeconds, completed, subjectId, taskId, rating, note)
