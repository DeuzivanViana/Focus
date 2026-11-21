plugins {
    alias(libs.plugins.focus.android.library)
    alias(libs.plugins.focus.hilt)
}

android {
    namespace = "com.focus.feature.sounds"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:timer"))
    testImplementation(libs.junit)
}
