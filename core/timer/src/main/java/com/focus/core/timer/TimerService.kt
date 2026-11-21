package com.focus.core.timer

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.focus.core.model.TimerStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the process alive while a cycle is active and shows the ongoing
 * countdown notification. All logic lives in [TimerController].
 */
@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject lateinit var controller: TimerController
    @Inject lateinit var notifier: TimerNotifier

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        lifecycleScope.launch {
            controller.state
                .map { NotificationKey(it.phase, it.status, it.endAtElapsed, if (it.status == TimerStatus.PAUSED) it.remainingMillis else 0) }
                .distinctUntilChanged()
                .collect { render() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isRunning = true
        // startForeground must be called right after the service is started.
        promoteToForeground()
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.start()
            ACTION_SKIP -> controller.skip()
            ACTION_STOP -> controller.stop()
            ACTION_TOGGLE -> controller.toggle()
        }
        if (!controller.state.value.isActive) shutdown()
        return START_NOT_STICKY
    }

    private fun render() {
        val st = controller.state.value
        if (!st.isActive) {
            shutdown()
            return
        }
        promoteToForeground()
        if (st.status == TimerStatus.RUNNING) acquireWakeLock(st.remainingMillis) else releaseWakeLock()
    }

    private fun promoteToForeground() {
        val notification = notifier.buildOngoing(controller.state.value)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, TimerNotifier.ID_ONGOING, notification, type)
    }

    /** Partial WakeLock with a timeout: makes sure the end of the phase fires on time. */
    private fun acquireWakeLock(remainingMs: Long) {
        releaseWakeLock()
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Focus:timer").apply {
            setReferenceCounted(false)
            acquire(remainingMs + WAKE_LOCK_MARGIN_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun shutdown() {
        isRunning = false
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        releaseWakeLock()
        super.onDestroy()
    }

    private data class NotificationKey(val phase: Any, val status: TimerStatus, val end: Long?, val pausedRemaining: Long)

    companion object {
        const val ACTION_PAUSE = "com.focus.timer.PAUSE"
        const val ACTION_RESUME = "com.focus.timer.RESUME"
        const val ACTION_SKIP = "com.focus.timer.SKIP"
        const val ACTION_STOP = "com.focus.timer.STOP"
        const val ACTION_TOGGLE = "com.focus.timer.TOGGLE"
        private const val WAKE_LOCK_MARGIN_MS = 5_000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
