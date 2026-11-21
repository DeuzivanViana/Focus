plugins {
    alias(libs.plugins.focus.android.library)
    alias(libs.plugins.focus.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.focus.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
