package dev.pokedex.core.weather

import dev.pokedex.core.model.WeatherBucket
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherReading
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Current conditions at a point. Implementations normalise to metric and to our buckets. */
interface WeatherSource {
    val provider: WeatherProvider
    suspend fun current(latitude: Double, longitude: Double): WeatherReading
}

/** Google Weather API, current conditions (checked against Google's docs on 30 September 2026). */
internal class GoogleWeatherSource(private val client: HttpClient, private val apiKey: String) : WeatherSource {
    override val provider = WeatherProvider.Google

    override suspend fun current(latitude: Double, longitude: Double): WeatherReading {
        val dto: GoogleConditions = client.get("https://weather.googleapis.com/v1/currentConditions:lookup") {
            // A header rather than the query string, so the key never shows up in URLs or logs.
            header("X-Goog-Api-Key", apiKey)
            parameter("location.latitude", latitude)
            parameter("location.longitude", longitude)
        }.body()
        return WeatherReading(
            bucket = googleBucket(dto.weatherCondition.type),
            description = dto.weatherCondition.description?.text ?: "Current weather",
            temperatureCelsius = dto.temperature.degrees,
            feelsLikeCelsius = dto.feelsLikeTemperature?.degrees ?: dto.temperature.degrees,
            isDaytime = dto.isDaytime,
            provider = provider,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }
}

/** Open-Meteo forecast API, current block (free for non-commercial use, no key, CC BY 4.0). */
internal class OpenMeteoWeatherSource(private val client: HttpClient) : WeatherSource {
    override val provider = WeatherProvider.OpenMeteo

    override suspend fun current(latitude: Double, longitude: Double): WeatherReading {
        val dto: OpenMeteoResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m")
            parameter("timezone", "auto")
        }.body()
        val c = dto.current
        return WeatherReading(
            bucket = openMeteoBucket(c.weatherCode, c.windSpeed),
            description = wmoDescription(c.weatherCode),
            temperatureCelsius = c.temperature,
            feelsLikeCelsius = c.apparentTemperature,
            isDaytime = c.isDay == 1,
            provider = provider,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }
}

/** Google's condition types grouped as in the plan's table (section 4). */
internal fun googleBucket(type: String): WeatherBucket = when {
    "THUNDER" in type -> WeatherBucket.Thunder
    "SNOW" in type || "HAIL" in type -> WeatherBucket.Snow
    type == "WINDY" || type == "WIND_AND_RAIN" -> WeatherBucket.Wind
    "RAIN" in type || "SHOWERS" in type -> WeatherBucket.Rain
    type == "CLEAR" || type == "MOSTLY_CLEAR" -> WeatherBucket.Clear
    else -> WeatherBucket.Cloudy
}

/** Wind strong enough to count as "windy" when nothing else is happening. First draft, tunable. */
internal const val WINDY_KMH = 40.0

/** WMO weather codes as used by Open-Meteo. */
internal fun openMeteoBucket(code: Int, windKmh: Double): WeatherBucket = when (code) {
    in 95..99 -> WeatherBucket.Thunder
    in 71..77, 85, 86 -> WeatherBucket.Snow
    in 51..67, in 80..82 -> WeatherBucket.Rain
    else -> when {
        windKmh >= WINDY_KMH -> WeatherBucket.Wind
        code == 0 || code == 1 -> WeatherBucket.Clear
        else -> WeatherBucket.Cloudy
    }
}

/** Descriptions from Open-Meteo's WMO table. */
internal fun wmoDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Depositing rime fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56 -> "Light freezing drizzle"
    57 -> "Dense freezing drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66 -> "Light freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snowfall"
    73 -> "Moderate snowfall"
    75 -> "Heavy snowfall"
    77 -> "Snow grains"
    80 -> "Slight rain showers"
    81 -> "Moderate rain showers"
    82 -> "Violent rain showers"
    85 -> "Slight snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    97 -> "Heavy thunderstorm"
    99 -> "Thunderstorm with heavy hail"
    else -> "Current weather"
}

@Serializable
internal data class GoogleConditions(
    val isDaytime: Boolean = true,
    val weatherCondition: GoogleCondition,
    val temperature: GoogleTemperature,
    val feelsLikeTemperature: GoogleTemperature? = null,
)

@Serializable
internal data class GoogleCondition(val type: String, val description: GoogleText? = null)

@Serializable
internal data class GoogleText(val text: String)

@Serializable
internal data class GoogleTemperature(val degrees: Double)

@Serializable
internal data class OpenMeteoResponse(val current: OpenMeteoCurrent)

@Serializable
internal data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("is_day") val isDay: Int,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
)
