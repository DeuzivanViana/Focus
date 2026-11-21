plugins {
    alias(libs.plugins.focus.android.library)
    alias(libs.plugins.focus.android.compose)
}

android {
    namespace = "com.focus.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
