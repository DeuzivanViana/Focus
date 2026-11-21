package com.focus.app

import com.focus.core.model.ThemeMode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.designsystem.theme.FocusColors
import javax.inject.Inject
import com.focus.app.navigation.FocusApp
import com.focus.core.designsystem.theme.FocusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* the timer works without it too */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // auto(): light or dark system bar icons follow the (possibly overridden) night mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            // Wait for settings to load (milliseconds) so the wrong accent never flashes.
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings
            if (current == null) {
                FocusTheme { Box(Modifier.fillMaxSize().background(FocusColors.Background)) }
            } else {
                // A theme change recreates the activity through AppCompat (no-op when already applied).
                LaunchedEffect(current.themeMode) { applyThemeMode(current.themeMode) }
                FocusTheme(accent = current.accent, oled = current.themeMode == ThemeMode.OLED) {
                    FocusApp(onStartRequested = ::requestNotificationPermissionIfNeeded)
                }
            }
        }
    }

    /** Ask for notification permission only when it makes sense: on the first focus. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
