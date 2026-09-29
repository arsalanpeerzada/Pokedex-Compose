pluginManagement {
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
    ":core:data",
    ":core:designsystem",
    ":feature:today",
    ":feature:pokedex",
    ":feature:detail",
    ":feature:collection",
)
