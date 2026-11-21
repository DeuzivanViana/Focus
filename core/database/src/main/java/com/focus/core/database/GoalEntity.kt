package com.focus.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.focus.core.model.Goal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subject_id") val subjectId: Long?,
    @ColumnInfo(name = "target_minutes") val targetMinutes: Int,
    /** Dates stored as epoch days. */
    @ColumnInfo(name = "start_day") val startDay: Long,
    @ColumnInfo(name = "deadline_day") val deadlineDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun GoalEntity.asModel() = Goal(id, title, subjectId, targetMinutes, LocalDate.ofEpochDay(startDay), LocalDate.ofEpochDay(deadlineDay), createdAt)
fun Goal.asEntity() = GoalEntity(id, title, subjectId, targetMinutes, startDate.toEpochDay(), deadline.toEpochDay(), createdAt)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY deadline_day ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getGoals(): List<GoalEntity>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)
}
