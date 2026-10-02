import java.util.Properties

plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The Google Weather key never goes in Git: it comes from secrets.properties (ignored) or,
// in CI, an environment variable. Without one, the app uses Open-Meteo, which needs no key.
val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val googleWeatherKey: String = secrets.getProperty("GOOGLE_WEATHER_API_KEY") ?: System.getenv("GOOGLE_WEATHER_API_KEY").orEmpty()

android {
    namespace = "dev.pokedex.core.weather"
    defaultConfig {
        buildConfigField("String", "GOOGLE_WEATHER_API_KEY", "\"$googleWeatherKey\"")
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.serialization.json)
}
