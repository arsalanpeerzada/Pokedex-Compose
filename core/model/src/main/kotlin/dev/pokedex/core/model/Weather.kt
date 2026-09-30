package dev.pokedex.core.model

/** The weather groups the context engine reasons about (plan, section 4). */
enum class WeatherBucket { Clear, Cloudy, Rain, Thunder, Snow, Wind }

/** Where a reading came from. Each has its own attribution, shown next to the weather. */
enum class WeatherProvider(val attribution: String, val link: String?) {
    Google("Source: Includes weather data from Google", null),
    OpenMeteo("Weather data by Open-Meteo.com", "https://open-meteo.com/"),
}

/** Current conditions for the user's city, normalised across providers. Metric units. */
data class WeatherReading(
    val bucket: WeatherBucket,
    val description: String,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val isDaytime: Boolean,
    val provider: WeatherProvider,
    val fetchedAtMillis: Long,
)
