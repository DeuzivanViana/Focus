package com.focus.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY archived ASC, created_at ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects")
    suspend fun getSubjects(): List<SubjectEntity>

    @Insert
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Query("UPDATE focus_sessions SET subject_id = NULL, task_id = NULL WHERE subject_id = :subjectId")
    suspend fun detachSessions(subjectId: Long)

    @Query("SELECT * FROM tasks ORDER BY done ASC, created_at ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getTasks(): List<TaskEntity>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE focus_sessions SET task_id = NULL WHERE task_id = :taskId")
    suspend fun detachTask(taskId: Long)
}
