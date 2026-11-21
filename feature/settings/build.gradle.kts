plugins {
    alias(libs.plugins.focus.feature)
}

android {
    namespace = "com.focus.feature.settings"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.aboutlibraries.core)
}
