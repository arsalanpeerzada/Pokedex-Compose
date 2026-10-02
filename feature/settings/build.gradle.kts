plugins {
    alias(libs.plugins.pokedex.android.feature)
}

android {
    namespace = "dev.pokedex.feature.settings"
}

dependencies {
    implementation(project(":core:sync"))
}
