package com.focus.feature.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.mikepenz.aboutlibraries.Libs

/** One bundled dependency, reduced to what the screen shows. */
data class LicensedLibrary(
    val id: String,
    val name: String,
    val version: String?,
    val author: String?,
    val licenseNames: List<String>,
    val licenseText: String?,
)

/**
 * Reads res/raw/aboutlibraries.json, generated at build time by the AboutLibraries Gradle
 * plugin from the release dependency graph, so the list never goes stale.
 */
internal fun loadLicensedLibraries(context: Context): List<LicensedLibrary> = runCatching {
    @Suppress("DiscouragedApi")
    val id = context.resources.getIdentifier("aboutlibraries", "raw", context.packageName)
    if (id == 0) return emptyList()
    val json = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    Libs.Builder().withJson(json).build().libraries
        .map { lib ->
            LicensedLibrary(
                id = lib.uniqueId,
                name = lib.name,
                version = lib.artifactVersion,
                author = lib.organization?.name ?: lib.developers.firstOrNull()?.name,
                licenseNames = lib.licenses.map { it.name },
                licenseText = lib.licenses.firstOrNull()?.licenseContent,
            )
        }
        // Multiplatform libraries list both the common and the Android artifact: show one.
        .distinctBy { it.name to it.version }
        .sortedBy { it.name.lowercase() }
}.getOrDefault(emptyList())

@Composable
fun LicensesRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val libraries = remember { loadLicensedLibraries(context) }
    LicensesScreen(libraries, onBack)
}

/** Open source notices: every library with its license; tap one to read the full text. */
@Composable
fun LicensesScreen(libraries: List<LicensedLibrary>, onBack: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolIconButton(FocusIcons.ArrowLeft, stringResource(R.string.privacy_cd_back), onBack)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.licenses_title), style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.licenses_subtitle, libraries.size), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
        if (libraries.isEmpty()) {
            item { Text(stringResource(R.string.licenses_unavailable), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary) }
        }
        items(libraries, key = { it.id }) { lib ->
            val open = expanded == lib.id
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(FocusColors.Surface)
                    .clickable { expanded = if (open) null else lib.id }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lib.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    lib.version?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary) }
                }
                Spacer(Modifier.height(2.dp))
                val meta = listOfNotNull(lib.author, lib.licenseNames.joinToString().ifEmpty { null }).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall)
                AnimatedVisibility(open && lib.licenseText != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Text(
                        lib.licenseText.orEmpty().trim(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp),
                        color = FocusColors.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
