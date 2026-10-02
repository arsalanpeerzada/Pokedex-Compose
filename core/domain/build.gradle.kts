plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
}

android {
    namespace = "dev.pokedex.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:weather"))
}
