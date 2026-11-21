package com.focus.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors

/**
 * Offline copy of the privacy policy. The app has no INTERNET permission, so the policy
 * is bundled instead of linked. Keep it in sync with the hosted version for Google Play.
 */
private val SECTIONS = listOf(
    R.string.privacy_s1_title to R.string.privacy_s1_body,
    R.string.privacy_s2_title to R.string.privacy_s2_body,
    R.string.privacy_s3_title to R.string.privacy_s3_body,
    R.string.privacy_s4_title to R.string.privacy_s4_body,
    R.string.privacy_s5_title to R.string.privacy_s5_body,
    R.string.privacy_s6_title to R.string.privacy_s6_body,
    R.string.privacy_s7_title to R.string.privacy_s7_body,
    R.string.privacy_s8_title to R.string.privacy_s8_body,
)

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(FocusIcons.ArrowLeft, stringResource(R.string.privacy_cd_back), onBack)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.privacy_effective, PRIVACY_EFFECTIVE_DATE), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.bodyMedium)
        }
        SECTIONS.forEach { (title, body) ->
            Spacer(Modifier.height(18.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
        }
        Spacer(Modifier.height(24.dp))
    }
}

internal const val PRIVACY_EFFECTIVE_DATE = "2026-11-21"
