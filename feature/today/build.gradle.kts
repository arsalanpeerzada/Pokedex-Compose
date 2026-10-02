plugins {
    alias(libs.plugins.pokedex.android.feature)
}

android {
    namespace = "dev.pokedex.feature.today"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
}
