plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
}

android {
    namespace = "dev.pokedex.feature.reminder"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
}
