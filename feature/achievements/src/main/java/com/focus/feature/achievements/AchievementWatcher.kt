package com.focus.feature.achievements

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focus.core.data.di.ApplicationScope
import com.focus.core.data.repository.GamificationRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.model.GamificationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.achievementStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

/**
 * Watches XP/achievements and posts a notification when something new is
 * unlocked. The set of already-announced achievements is persisted, so each
 * one is announced exactly once. On the very first run everything already
 * earned is stored silently (no notification burst for existing users).
 */
@Singleton
class AchievementWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gamification: GamificationRepository,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val store = context.achievementStore
    private val announcedKey = stringSetPreferencesKey("announced")
    private val levelKey = intPreferencesKey("announced_level")

    fun start() {
        createChannel()
        scope.launch {
            gamification.observe().collect { state -> handle(state) }
        }
    }

    private suspend fun handle(state: GamificationState) {
        val prefs = store.data.first()
        val unlocked = state.achievements.filter { it.unlocked }.map { it.achievement.name }.toSet()
        val announced = prefs[announcedKey]
        val lastLevel = prefs[levelKey]
        store.edit {
            it[announcedKey] = (announced.orEmpty() + unlocked)
            it[levelKey] = maxOf(lastLevel ?: 0, state.level)
        }
        if (announced == null || lastLevel == null) return // first run: store silently

        val accent = settings.settings.first().accent.argb.toInt()
        state.achievements
            .filter { it.unlocked && it.achievement.name !in announced }
            .forEach { p ->
                val text = p.achievement.text()
                notify(
                    id = NOTIFICATION_BASE + p.achievement.ordinal,
                    title = context.getString(R.string.achievements_notify_unlocked, context.getString(text.title)),
                    body = context.getString(text.description),
                    accent = accent,
                )
            }
        if (state.level > lastLevel) {
            notify(
                id = NOTIFICATION_LEVEL,
                title = context.getString(R.string.achievements_notify_level, state.level),
                body = context.getString(R.string.achievements_notify_level_text),
                accent = accent,
            )
        }
    }

    private fun createChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.achievements_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.achievements_channel_desc)
            },
        )
    }

    private fun notify(id: Int, title: String, body: String, accent: Int) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = launch?.let { PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_trophy)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(accent)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private companion object {
        const val CHANNEL = "achievements"
        const val NOTIFICATION_BASE = 2000
        const val NOTIFICATION_LEVEL = 1999
    }
}
