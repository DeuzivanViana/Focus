package com.focus.feature.blocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Checks and settings shortcuts for the two special permissions the blocker may use. */
object BlockerPermissions {

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canShowBlockScreen(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * True when Android 13+ probably greys out Usage Access behind "Restricted settings":
     * the system does that for apps installed from a downloaded APK instead of a store.
     * There is no API to read the restriction itself, so this checks how the app was
     * installed. The user lifts it in App info > menu > "Allow restricted settings".
     */
    fun usageAccessMayBeRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (hasUsageAccess(context)) return false
        val source = runCatching { context.packageManager.getInstallSourceInfo(context.packageName) }.getOrNull() ?: return true
        return source.packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
            source.packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
            (source.packageSource == PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED && source.installingPackageName == null)
    }

    /** This app's "App info" page, where the "Allow restricted settings" menu lives. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun usageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            // Some versions open this app's page directly when the package is given.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) data = Uri.fromParts("package", context.packageName, null)
        }

    /** Fallback when a device rejects the package-specific usage access screen. */
    fun usageAccessFallbackIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null))
}
