package com.focus.app

import android.app.Application
import com.focus.core.timer.TimerNotifier
import com.focus.feature.achievements.AchievementWatcher
import com.focus.feature.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import com.focus.feature.blocker.BlockerMonitor
import com.focus.feature.reminders.ReminderScheduler
import com.focus.feature.sounds.FocusSoundPlayer
import com.focus.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class FocusApplication : Application() {
    @Inject lateinit var notifier: TimerNotifier
    @Inject lateinit var achievementWatcher: AchievementWatcher
    @Inject lateinit var widgetUpdater: WidgetUpdater
    @Inject lateinit var blockerMonitor: BlockerMonitor
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var soundPlayer: FocusSoundPlayer

    override fun onCreate() {
        super.onCreate()
        // AppCompat does not persist night mode: restore it before the first activity draws.
        // A one-off read of a tiny local DataStore file (a few milliseconds).
        applyThemeMode(runBlocking { settingsRepository.settings.first() }.themeMode)
        notifier.createChannels()
        achievementWatcher.start()
        widgetUpdater.start()
        blockerMonitor.start()
        reminderScheduler.start()
        soundPlayer.start()
    }
}
