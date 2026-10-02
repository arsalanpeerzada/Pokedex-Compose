plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
}

android {
    namespace = "dev.pokedex.feature.wallpaper"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
}
