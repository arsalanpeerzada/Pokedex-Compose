plugins {
    alias(libs.plugins.pokedex.android.feature)
}

android {
    namespace = "dev.pokedex.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
