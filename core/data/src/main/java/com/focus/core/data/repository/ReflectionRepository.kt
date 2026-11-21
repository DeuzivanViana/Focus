package com.focus.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focus.core.data.reflection.ReflectionCalculator
import com.focus.core.database.FocusSessionDao
import com.focus.core.model.FocusSession
import com.focus.core.model.ReflectionInsights
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reflectionStore: DataStore<Preferences> by preferencesDataStore(name = "reflection")

@Singleton
class ReflectionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: FocusSessionDao,
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
) {
    private val store = context.reflectionStore
    private val lastHandledKey = longPreferencesKey("last_handled_session")

    /**
     * The session to ask about, if any: the newest one that ended in the last
     * [PROMPT_WINDOW_MS], lasted at least [MIN_SECONDS], has no rating and was
     * not already answered or skipped.
     */
    fun observePending(): Flow<FocusSession?> =
        combine(sessions.observeSessions(), store.data.map { it[lastHandledKey] ?: 0L }) { list, lastHandled ->
            val now = System.currentTimeMillis()
            list.filter { it.id > lastHandled && it.rating == null && it.focusedSeconds >= MIN_SECONDS && now - it.endedAt <= PROMPT_WINDOW_MS }
                .maxByOrNull { it.endedAt }
        }

    fun observeInsights(): Flow<ReflectionInsights> =
        combine(sessions.observeSessions(), subjects.observeSubjects()) { list, subs ->
            val zone = ZoneId.systemDefault()
            ReflectionCalculator.insights(list, subs, LocalDate.now(zone), zone)
        }.flowOn(Dispatchers.Default)

    suspend fun submit(sessionId: Long, rating: Int, note: String?) {
        require(rating in FocusSession.RATING_RANGE)
        val clean = note?.filterNot { it.isISOControl() && it != '\n' }?.trim()?.take(FocusSession.MAX_NOTE_LENGTH)?.ifBlank { null }
        dao.setReflection(sessionId, rating, clean)
        markHandled(sessionId)
    }

    suspend fun skip(sessionId: Long) = markHandled(sessionId)

    private suspend fun markHandled(id: Long) {
        store.edit { prefs -> prefs[lastHandledKey] = maxOf(prefs[lastHandledKey] ?: 0L, id) }
    }

    private companion object {
        const val MIN_SECONDS = 5 * 60L
        const val PROMPT_WINDOW_MS = 3 * 60 * 60 * 1000L
    }
}
