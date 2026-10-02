plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.android.compose)
    alias(libs.plugins.pokedex.hilt)
}

android {
    namespace = "dev.pokedex.feature.widget"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
}
