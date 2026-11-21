import java.util.Properties

plugins {
    alias(libs.plugins.focus.android.library)
    alias(libs.plugins.focus.hilt)
}

// AES-256 key for backup files: 64 hex characters from local.properties (focus.backupKey)
// or the FOCUS_BACKUP_KEY environment variable. Kept out of the repository. Without one,
// a public development key is used (fine for local builds, not for published ones).
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val devBackupKey = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"
val backupKey: String = (localProps.getProperty("focus.backupKey") ?: System.getenv("FOCUS_BACKUP_KEY") ?: devBackupKey).trim()
require(backupKey.matches(Regex("^[0-9a-fA-F]{64}$"))) { "focus.backupKey must be 64 hex characters" }
if (backupKey == devBackupKey) logger.warn("Focus: no focus.backupKey set, backups use the public development key.")

android {
    namespace = "com.focus.core.data"
    buildFeatures.buildConfig = true
    defaultConfig {
        buildConfigField("String", "BACKUP_KEY", "\"$backupKey\"")
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.android)
}
