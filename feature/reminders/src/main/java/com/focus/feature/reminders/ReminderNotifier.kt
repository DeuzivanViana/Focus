package com.focus.feature.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focus.core.data.plan.ReminderEvent
import com.focus.core.data.plan.WeeklySummaryCalculator
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.SubjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
    private val settings: SettingsRepository,
) {
    fun createChannel() {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    suspend fun post(event: ReminderEvent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val (id, title, text) = when (event) {
            is ReminderEvent.Routine -> {
                val b = event.block
                val subject = b.subjectId?.let { id -> subjects.getSubjects().firstOrNull { it.id == id }?.name }
                val name = listOfNotNull(b.title, subject?.takeIf { it != b.title }).joinToString(" · ")
                val title = if (b.remindBefore == 0) {
                    context.getString(R.string.reminders_routine_now, b.title)
                } else {
                    context.getString(R.string.reminders_routine_soon, b.title, b.remindBefore)
                }
                Triple(ID_ROUTINE + (b.id % 500).toInt(), title, context.getString(R.string.reminders_routine_text, clock(b.startMinute), clock(b.endMinute), name))
            }
            is ReminderEvent.Reviews -> {
                val names = event.due.take(3).joinToString(", ") { it.title } + if (event.due.size > 3) "…" else ""
                Triple(
                    ID_REVIEWS,
                    context.resources.getQuantityString(R.plurals.reminders_reviews_title, event.due.size, event.due.size),
                    names,
                )
            }
            is ReminderEvent.WeeklySummary -> {
                val zone = ZoneId.systemDefault()
                val s = WeeklySummaryCalculator.compute(
                    sessions.getAll(), subjects.getSubjects(), settings.settings.first().dailyGoalMinutes, event.weekEnd, zone,
                )
                val parts = buildList {
                    s.changePercent?.let { add(context.getString(if (it >= 0) R.string.reminders_weekly_up else R.string.reminders_weekly_down, kotlin.math.abs(it))) }
                    add(context.getString(R.string.reminders_weekly_goal, s.daysGoalMet))
                    s.topSubject?.let { add(context.getString(R.string.reminders_weekly_top, it.name)) }
                }
                Triple(ID_WEEKLY, context.getString(R.string.reminders_weekly_title, duration(s.totalSeconds)), parts.joinToString(" · "))
            }
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.focus.core.timer.R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private fun clock(minute: Int): String = "%02d:%02d".format((minute / 60) % 24, minute % 60)

    private fun duration(seconds: Long): String {
        val m = seconds / 60
        return if (m >= 60) "%dh %02dm".format(m / 60, m % 60) else "${m}m"
    }

    private companion object {
        const val CHANNEL = "reminders"
        const val ID_ROUTINE = 5000
        const val ID_REVIEWS = 5900
        const val ID_WEEKLY = 5950
    }
}
