package com.focus.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focus.core.model.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

@Singleton
class ReminderSettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.reminderStore

    val settings: Flow<ReminderSettings> = store.data.map { p ->
        ReminderSettings(
            routine = p[ROUTINE] ?: true,
            reviews = p[REVIEWS] ?: true,
            reviewMinute = (p[REVIEW_MINUTE] ?: 9 * 60).coerceIn(0, 24 * 60 - 1),
            weeklySummary = p[WEEKLY] ?: true,
        )
    }

    suspend fun update(transform: (ReminderSettings) -> ReminderSettings) {
        store.edit { p ->
            val cur = ReminderSettings(p[ROUTINE] ?: true, p[REVIEWS] ?: true, p[REVIEW_MINUTE] ?: 9 * 60, p[WEEKLY] ?: true)
            val next = transform(cur)
            p[ROUTINE] = next.routine
            p[REVIEWS] = next.reviews
            p[REVIEW_MINUTE] = next.reviewMinute.coerceIn(0, 24 * 60 - 1)
            p[WEEKLY] = next.weeklySummary
        }
    }

    private companion object {
        val ROUTINE = booleanPreferencesKey("routine")
        val REVIEWS = booleanPreferencesKey("reviews")
        val REVIEW_MINUTE = intPreferencesKey("review_minute")
        val WEEKLY = booleanPreferencesKey("weekly_summary")
    }
}
