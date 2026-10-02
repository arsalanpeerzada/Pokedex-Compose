pluginManagement {
    // Convention plugins: shared module set-up (SDK levels, Java, Compose, Hilt, tests).
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

rootProject.name = "Pokedex"

include(
    ":app",
    ":core:model",
    ":core:network",
    ":core:database",
    ":core:data",
    ":core:designsystem",
    ":core:weather",
    ":core:domain",
    ":core:sync",
    ":feature:today",
    ":feature:pokedex",
    ":feature:detail",
    ":feature:collection",
    ":feature:settings",
    ":feature:onboarding",
    ":feature:teams",
    ":feature:widget",
    ":feature:reminder",
    ":feature:wallpaper",
)
