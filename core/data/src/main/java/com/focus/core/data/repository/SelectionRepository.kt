package com.focus.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** What the next focus session will be linked to. */
data class FocusSelection(val subjectId: Long? = null, val taskId: Long? = null)

interface SelectionRepository {
    val selection: Flow<FocusSelection>
    suspend fun select(selection: FocusSelection)
}

private val Context.selectionStore: DataStore<Preferences> by preferencesDataStore(name = "focus_selection")

internal class DataStoreSelectionRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SelectionRepository {
    private val store = context.selectionStore
    private val subjectKey = longPreferencesKey("subject_id")
    private val taskKey = longPreferencesKey("task_id")

    override val selection: Flow<FocusSelection> = store.data
        .map { FocusSelection(it[subjectKey], it[taskKey]) }
        .distinctUntilChanged()

    override suspend fun select(selection: FocusSelection) {
        store.edit {
            if (selection.subjectId != null) it[subjectKey] = selection.subjectId else it.remove(subjectKey)
            if (selection.taskId != null) it[taskKey] = selection.taskId else it.remove(taskKey)
        }
    }
}
