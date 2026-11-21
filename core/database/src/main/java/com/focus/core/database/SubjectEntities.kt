package com.focus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focus.core.model.AccentColor
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: String,
    @ColumnInfo(name = "archived", defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subject_id")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "done", defaultValue = "0") val done: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun SubjectEntity.asModel() = Subject(id, name, AccentColor.fromName(color), archived, createdAt)
fun Subject.asEntity() = SubjectEntity(id, name, color.name, archived, createdAt)
fun TaskEntity.asModel() = StudyTask(id, subjectId, title, done, createdAt)
fun StudyTask.asEntity() = TaskEntity(id, subjectId, title, done, createdAt)
