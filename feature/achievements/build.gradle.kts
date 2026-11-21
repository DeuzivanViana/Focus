plugins {
    alias(libs.plugins.focus.feature)
}

android {
    namespace = "com.focus.feature.achievements"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
}
