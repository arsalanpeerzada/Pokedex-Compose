plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
}

android {
    namespace = "dev.pokedex.core.sync"
}

dependencies {
    api(project(":core:data"))
    implementation(libs.androidx.work.runtime)
}
