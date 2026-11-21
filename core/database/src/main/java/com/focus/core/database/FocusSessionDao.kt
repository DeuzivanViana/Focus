package com.focus.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY started_at DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY started_at ASC")
    suspend fun getAll(): List<FocusSessionEntity>

    @Query("SELECT started_at FROM focus_sessions")
    suspend fun getAllStartTimes(): List<Long>

    @Insert
    suspend fun insertAll(sessions: List<FocusSessionEntity>)

    @Query("UPDATE focus_sessions SET rating = :rating, note = :note WHERE id = :id")
    suspend fun setReflection(id: Long, rating: Int, note: String?)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
