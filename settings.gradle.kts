pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Focus"

include(":app")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:designsystem")
include(":core:timer")
include(":feature:focus")
include(":feature:stats")
include(":feature:settings")
include(":feature:subjects")
include(":feature:achievements")
include(":feature:widget")
include(":feature:goals")
include(":feature:reflection")
include(":feature:blocker")
include(":feature:reminders")
include(":feature:routine")
include(":feature:review")
include(":feature:sounds")
