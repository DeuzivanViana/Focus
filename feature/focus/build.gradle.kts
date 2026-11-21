plugins {
    alias(libs.plugins.focus.feature)
}

android {
    namespace = "com.focus.feature.focus"
}

dependencies {
    implementation(project(":core:timer"))
    implementation(project(":feature:sounds"))
}
