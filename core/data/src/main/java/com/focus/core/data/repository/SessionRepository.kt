package com.focus.core.data.repository

import com.focus.core.database.FocusSessionDao
import com.focus.core.database.asEntity
import com.focus.core.database.asModel
import com.focus.core.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SessionRepository {
    fun observeSessions(): Flow<List<FocusSession>>
    suspend fun record(session: FocusSession)
    suspend fun clearAll()
    suspend fun getAll(): List<FocusSession>

    /** Inserts sessions, skipping ones that already exist (same start). Returns how many were added. */
    suspend fun importMerging(sessions: List<FocusSession>): Int
}

internal class OfflineSessionRepository @Inject constructor(
    private val dao: FocusSessionDao,
) : SessionRepository {
    override fun observeSessions(): Flow<List<FocusSession>> =
        dao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun record(session: FocusSession) {
        // Defensive validation: no negative or absurd durations in the database.
        require(session.focusedSeconds in 0..MAX_SESSION_SECONDS)
        require(session.endedAt >= session.startedAt)
        dao.insert(session.asEntity())
    }

    override suspend fun clearAll() = dao.deleteAll()

    override suspend fun getAll(): List<FocusSession> = dao.getAll().map { it.asModel() }

    override suspend fun importMerging(sessions: List<FocusSession>): Int {
        val existing = dao.getAllStartTimes().toHashSet()
        val fresh = sessions
            .filter { it.focusedSeconds in 0..MAX_SESSION_SECONDS && it.endedAt >= it.startedAt }
            .filter { existing.add(it.startedAt) }
            .map { it.copy(id = 0, rating = it.rating?.takeIf { r -> r in FocusSession.RATING_RANGE }, note = it.note?.let(::cleanNote)).asEntity() }
        if (fresh.isNotEmpty()) dao.insertAll(fresh)
        return fresh.size
    }

    private fun cleanNote(note: String): String? =
        note.filterNot { it.isISOControl() && it != '\n' }.trim().take(FocusSession.MAX_NOTE_LENGTH).ifBlank { null }

    private companion object {
        const val MAX_SESSION_SECONDS = 24L * 60 * 60
    }
}
