package com.focus.feature.blocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable?)

/** Launchable apps on the device, except this one. Visible thanks to the manifest's <queries>. */
internal suspend fun loadLaunchableApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val activities = if (android.os.Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    activities.asSequence()
        .map { it.activityInfo.packageName to it }
        .filter { (pkg, _) -> pkg != context.packageName }
        .distinctBy { it.first }
        .map { (pkg, info) -> InstalledApp(pkg, info.loadLabel(pm).toString(), runCatching { info.loadIcon(pm) }.getOrNull()) }
        .sortedBy { it.label.lowercase() }
        .toList()
}

internal fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
