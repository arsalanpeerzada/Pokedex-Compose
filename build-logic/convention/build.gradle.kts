plugins {
    `kotlin-dsl`
}

group = "dev.pokedex.buildlogic"

dependencies {
    // Only the Android plugin's API is needed to compile; the plugins themselves come from the root build.
    compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "pokedex.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "pokedex.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "pokedex.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("androidFeature") {
            id = "pokedex.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
