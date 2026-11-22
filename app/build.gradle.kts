import java.util.Properties

plugins {
    alias(libs.plugins.focus.android.application)
    alias(libs.plugins.focus.android.compose)
    alias(libs.plugins.focus.hilt)
    // Generates res/raw/aboutlibraries.json (dependencies and their licenses) at build time,
    // shown in Settings > Open source licenses.
    alias(libs.plugins.aboutlibraries)
}

// Release signing (the Play upload key) comes from keystore.properties (kept out of git)
// or, on CI, from the FOCUS_KEYSTORE_* environment variables. Never commit either.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    } else if (System.getenv("FOCUS_KEYSTORE_FILE") != null) {
        setProperty("storeFile", System.getenv("FOCUS_KEYSTORE_FILE"))
        setProperty("storePassword", System.getenv("FOCUS_KEYSTORE_PASSWORD"))
        setProperty("keyAlias", System.getenv("FOCUS_KEY_ALIAS"))
        setProperty("keyPassword", System.getenv("FOCUS_KEY_PASSWORD"))
    }
}

android {
    namespace = "com.focus.app"

    defaultConfig {
        applicationId = "com.deuzivan.focus"
        versionCode = 15
        versionName = "1.0.1"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }

    testOptions.unitTests.isIncludeAndroidResources = true
    // Exported Room schemas, read by the migration test (debug builds only, never in release).
    sourceSets.getByName("debug").assets.srcDir(rootProject.file("core/database/schemas"))

    dependenciesInfo {
        // Not in the APK. In the bundle it is encrypted and only read by Google Play,
        // which uses it to flag outdated or vulnerable SDKs in Play Console.
        includeInApk = false
        includeInBundle = true
    }

    bundle {
        // Keep every language in every install: the in-app language picker must be able
        // to switch to Portuguese or English even if the device uses the other one.
        language { enableSplit = false }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:timer"))
    implementation(project(":feature:focus"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:subjects"))
    implementation(project(":feature:achievements"))
    implementation(project(":feature:widget"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:reflection"))
    implementation(project(":feature:blocker"))
    implementation(project(":feature:reminders"))
    implementation(project(":feature:routine"))
    implementation(project(":feature:review"))
    implementation(project(":feature:sounds"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Screen rendering tests (Robolectric, runs on the JVM without an emulator).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
