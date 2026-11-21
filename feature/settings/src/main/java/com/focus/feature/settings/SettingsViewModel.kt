package com.focus.feature.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.backup.ImportResult
import com.focus.core.data.backup.ProgressBackup
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.ReminderSettingsRepository
import com.focus.core.model.ReminderSettings
import com.focus.core.model.TimerSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupEvent {
    data class Exported(val count: Int) : BackupEvent
    data object ExportFailed : BackupEvent
    data class Imported(val result: ImportResult) : BackupEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val backup: ProgressBackup,
    private val reminderRepository: ReminderSettingsRepository,
) : ViewModel() {
    val reminders: StateFlow<ReminderSettings> = reminderRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    fun updateReminders(transform: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch { reminderRepository.update(transform) }
    }

    val settings: StateFlow<TimerSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerSettings())

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = _events.receiveAsFlow()

    fun update(transform: (TimerSettings) -> TimerSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun clearHistory() {
        viewModelScope.launch { sessionRepository.clearAll() }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val event = runCatching { backup.export(uri) }
                .onFailure { Log.w(TAG, "Export failed", it) }
                .fold({ BackupEvent.Exported(it) }, { BackupEvent.ExportFailed })
            _events.send(event)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching { backup.import(uri) }.getOrElse { ImportResult.Invalid }
            _events.send(BackupEvent.Imported(result))
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
