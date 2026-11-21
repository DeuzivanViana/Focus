plugins {
    alias(libs.plugins.focus.android.library)
    alias(libs.plugins.focus.hilt)
}

android {
    namespace = "com.focus.core.timer"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
}
