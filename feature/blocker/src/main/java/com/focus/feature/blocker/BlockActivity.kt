package com.focus.feature.blocker

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.designsystem.component.PrimaryButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.theme.FocusTheme
import com.focus.core.designsystem.util.formatClock
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus
import com.focus.core.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shown over a blocked app during a focus. It closes by itself when the focus ends. */
@AndroidEntryPoint
class BlockActivity : AppCompatActivity() {

    @Inject lateinit var timer: TimerController
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Back never returns to the blocked app: it goes to the home screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                timer.state.collect { st ->
                    if (st.status != TimerStatus.RUNNING || st.phase != TimerPhase.FOCUS) finish()
                }
            }
        }
        val label = intent.getStringExtra(EXTRA_PACKAGE)?.let { appLabel(this, it) }.orEmpty()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            FocusTheme(accent = current.accent, oled = current.themeMode == com.focus.core.model.ThemeMode.OLED) {
                BlockScreen(
                    appLabel = label,
                    remainingMillis = { (timer.state.value.endAtElapsed ?: 0L) - SystemClock.elapsedRealtime() },
                    onBackToFocus = ::openFocus,
                    onHome = ::goHome,
                )
            }
        }
    }

    private fun openFocus() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}

@Composable
fun BlockScreen(appLabel: String, remainingMillis: () -> Long, onBackToFocus: () -> Unit, onHome: () -> Unit) {
    var remaining by remember { mutableLongStateOf(remainingMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = remainingMillis()
            delay(500)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(FocusIcons.Shield, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.blocker_screen_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.blocker_screen_text, appLabel.ifEmpty { stringResource(R.string.blocker_this_app) }),
            style = MaterialTheme.typography.bodyMedium,
            color = FocusColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Text(formatClock(remaining), style = MaterialTheme.typography.displayLarge, color = FocusColors.TextPrimary)
        Text(stringResource(R.string.blocker_screen_left), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        Spacer(Modifier.height(36.dp))
        PrimaryButton(stringResource(R.string.blocker_back_to_focus), onBackToFocus, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onHome) { Text(stringResource(R.string.blocker_go_home), color = FocusColors.TextSecondary) }
    }
}
