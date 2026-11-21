plugins {
    alias(libs.plugins.focus.feature)
}

android {
    namespace = "com.focus.feature.blocker"
}

dependencies {
    implementation(project(":core:timer"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
}
