package com.focus.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.FocusSelection
import com.focus.core.data.repository.GamificationRepository
import com.focus.core.data.repository.SelectionRepository
import com.focus.core.data.repository.StatsRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.FocusStats
import com.focus.core.model.TimerStyle
import com.focus.core.model.FocusSound
import com.focus.feature.sounds.FocusSoundPlayer
import com.focus.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.focus.core.model.GamificationState
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject
import com.focus.core.timer.TimerController
import com.focus.core.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val timer: TimerState = TimerState(),
    val stats: FocusStats = FocusStats(),
    val gamification: GamificationState = GamificationState(),
    val subjects: List<Subject> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val selection: FocusSelection = FocusSelection(),
    val timerStyle: TimerStyle = TimerStyle.RING,
    val sound: FocusSound = FocusSound.NONE,
    val soundVolume: Int = 60,
) {
    val selectedSubject: Subject? get() = subjects.firstOrNull { it.id == selection.subjectId }
    val selectedTask: StudyTask? get() = tasks.firstOrNull { it.id == selection.taskId && it.subjectId == selection.subjectId }
}

private data class Prefs(val timerStyle: TimerStyle, val sound: FocusSound, val soundVolume: Int)

private data class Library(val subjects: List<Subject>, val tasks: List<StudyTask>, val selection: FocusSelection, val prefs: Prefs)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: TimerController,
    private val selectionRepository: SelectionRepository,
    statsRepository: StatsRepository,
    gamificationRepository: GamificationRepository,
    subjectRepository: SubjectRepository,
    private val settingsRepository: SettingsRepository,
    private val soundPlayer: FocusSoundPlayer,
) : ViewModel() {

    private val library = combine(
        subjectRepository.observeSubjects(),
        subjectRepository.observeTasks(),
        selectionRepository.selection,
        settingsRepository.settings.map { Prefs(it.timerStyle, it.focusSound, it.soundVolume) }.distinctUntilChanged(),
    ) { s, t, sel, prefs -> Library(s.filterNot { it.archived }, t, sel, prefs) }

    val uiState: StateFlow<FocusUiState> =
        combine(controller.state, statsRepository.observeStats(), gamificationRepository.observe(), library) { timer, stats, game, lib ->
            FocusUiState(timer, stats, game, lib.subjects, lib.tasks, lib.selection, lib.prefs.timerStyle, lib.prefs.sound, lib.prefs.soundVolume)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState(timer = controller.state.value))

    fun onToggle() = controller.toggle()
    fun onStop() = controller.stop()
    fun onSkip() = controller.skip()

    fun onSound(sound: FocusSound) {
        viewModelScope.launch { settingsRepository.update { it.copy(focusSound = sound) } }
    }

    fun onVolume(percent: Int) {
        soundPlayer.setPreviewVolume(percent)
        viewModelScope.launch { settingsRepository.update { it.copy(soundVolume = percent) } }
    }

    /** Plays [sound] while the picker is open; null ends the preview. */
    fun onPreview(sound: FocusSound?) = soundPlayer.preview(sound)

    override fun onCleared() {
        soundPlayer.preview(null)
    }

    fun onSelect(selection: FocusSelection) {
        viewModelScope.launch { selectionRepository.select(selection) }
    }
}
