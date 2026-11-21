package com.focus.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focus.core.model.AccentColor
import com.focus.core.model.ThemeMode
import com.focus.core.model.TimerStyle
import com.focus.core.model.FocusSound
import com.focus.core.model.TimerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

interface SettingsRepository {
    val settings: Flow<TimerSettings>
    suspend fun update(transform: (TimerSettings) -> TimerSettings)
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

internal class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val store = context.settingsStore

    override val settings: Flow<TimerSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (TimerSettings) -> TimerSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings()).sanitized()
            prefs[Keys.FOCUS] = next.focusMinutes
            prefs[Keys.SHORT] = next.shortBreakMinutes
            prefs[Keys.LONG] = next.longBreakMinutes
            prefs[Keys.CYCLE] = next.sessionsBeforeLongBreak
            prefs[Keys.GOAL] = next.dailyGoalMinutes
            prefs[Keys.AUTO_BREAK] = next.autoStartBreaks
            prefs[Keys.AUTO_FOCUS] = next.autoStartFocus
            prefs[Keys.ACCENT] = next.accent.name
            prefs[Keys.THEME] = next.themeMode.name
            prefs[Keys.TIMER_STYLE] = next.timerStyle.name
            prefs[Keys.REST_DAYS] = TimerSettings.restDaysToMask(next.restDays)
            prefs[Keys.SOUND] = next.focusSound.name
            prefs[Keys.SOUND_VOLUME] = next.soundVolume
        }
    }

    private fun Preferences.toSettings(): TimerSettings {
        val d = TimerSettings()
        return TimerSettings(
            focusMinutes = this[Keys.FOCUS] ?: d.focusMinutes,
            shortBreakMinutes = this[Keys.SHORT] ?: d.shortBreakMinutes,
            longBreakMinutes = this[Keys.LONG] ?: d.longBreakMinutes,
            sessionsBeforeLongBreak = this[Keys.CYCLE] ?: d.sessionsBeforeLongBreak,
            dailyGoalMinutes = this[Keys.GOAL] ?: d.dailyGoalMinutes,
            autoStartBreaks = this[Keys.AUTO_BREAK] ?: d.autoStartBreaks,
            autoStartFocus = this[Keys.AUTO_FOCUS] ?: d.autoStartFocus,
            accent = AccentColor.fromName(this[Keys.ACCENT]),
            themeMode = ThemeMode.fromName(this[Keys.THEME]),
            timerStyle = TimerStyle.fromName(this[Keys.TIMER_STYLE]),
            restDays = TimerSettings.maskToRestDays(this[Keys.REST_DAYS] ?: 0),
            focusSound = FocusSound.fromName(this[Keys.SOUND]),
            soundVolume = this[Keys.SOUND_VOLUME] ?: 60,
        ).sanitized()
    }

    private object Keys {
        val FOCUS = intPreferencesKey("focus_minutes")
        val SHORT = intPreferencesKey("short_break_minutes")
        val LONG = intPreferencesKey("long_break_minutes")
        val CYCLE = intPreferencesKey("sessions_before_long_break")
        val GOAL = intPreferencesKey("daily_goal_minutes")
        val AUTO_BREAK = booleanPreferencesKey("auto_start_breaks")
        val AUTO_FOCUS = booleanPreferencesKey("auto_start_focus")
        val ACCENT = stringPreferencesKey("accent_color")
        val THEME = stringPreferencesKey("theme_mode")
        val TIMER_STYLE = stringPreferencesKey("timer_style")
        val REST_DAYS = intPreferencesKey("rest_days")
        val SOUND = stringPreferencesKey("focus_sound")
        val SOUND_VOLUME = intPreferencesKey("sound_volume")
    }
}
