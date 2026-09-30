import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// The Google Weather key never goes in Git: it comes from secrets.properties (ignored) or,
// in CI, an environment variable. Without one, the app uses Open-Meteo, which needs no key.
val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val googleWeatherKey: String = secrets.getProperty("GOOGLE_WEATHER_API_KEY") ?: System.getenv("GOOGLE_WEATHER_API_KEY").orEmpty()

android {
    namespace = "dev.pokedex.core.weather"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "GOOGLE_WEATHER_API_KEY", "\"$googleWeatherKey\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
