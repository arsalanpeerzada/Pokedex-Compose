package dev.pokedex.core.weather

import android.content.Context
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherReading
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Where the user's city is, as typed, looked up on the device. */
data class CityLocation(val name: String, val latitude: Double, val longitude: Double, val country: String? = null)

private data class Geocoded(val latitude: Double, val longitude: Double, val country: String?)

/**
 * How long a reading may be kept, and how often a provider may be asked. Readings live in memory
 * only. Google's terms allow current conditions to be cached temporarily; a summary of its service
 * terms gives one hour, which we follow (the full terms page couldn't be read on 1 October 2026).
 * Google is asked at most every three hours, to stay inside the free monthly calls (plan, section 6).
 */
internal data class CachePolicy(val maxAgeMillis: Long, val minFetchIntervalMillis: Long) {

    /** True when [cachedAt] is recent enough to show. */
    fun fresh(cachedAt: Long, now: Long) = now - cachedAt < maxAgeMillis

    /** True when the provider may be asked again, given the last successful fetch. */
    fun mayFetch(lastFetchAt: Long, now: Long) = lastFetchAt == 0L || now - lastFetchAt >= minFetchIntervalMillis

    companion object {
        private const val HOUR = 60 * 60 * 1000L
        fun of(provider: WeatherProvider) = when (provider) {
            WeatherProvider.Google -> CachePolicy(maxAgeMillis = HOUR, minFetchIntervalMillis = 3 * HOUR)
            WeatherProvider.OpenMeteo -> CachePolicy(maxAgeMillis = 3 * HOUR, minFetchIntervalMillis = 0)
        }
    }
}

/**
 * Current weather for the user's city. Google Weather when a key is configured, otherwise
 * Open-Meteo, each under its [CachePolicy]. Between Google's one-hour expiry and the next allowed
 * fetch there is simply no weather, and Today falls back to the time of day and season.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    client: HttpClient,
    private val preferences: UserPreferencesRepository,
) {
    private val source: WeatherSource =
        BuildConfig.GOOGLE_WEATHER_API_KEY.takeIf { it.isNotBlank() }?.let { GoogleWeatherSource(client, it) }
            ?: OpenMeteoWeatherSource(client)

    val provider: WeatherProvider get() = source.provider

    private val policy = CachePolicy.of(source.provider)
    private val lock = Mutex()
    private var cached: Pair<CityLocation, WeatherReading>? = null
    private var lastFetchAt = 0L
    private var lastFetchCity: CityLocation? = null

    /** The user's city with its position, looking it up once if needed. Null without a city. */
    suspend fun city(): CityLocation? {
        val prefs = preferences.preferences.first()
        val name = prefs.city ?: return null
        val lat = prefs.cityLatitude
        val lon = prefs.cityLongitude
        if (lat != null && lon != null) return CityLocation(name, lat, lon, prefs.cityCountry)
        val found = geocode(name) ?: return null
        preferences.setCityLocation(name, found.latitude, found.longitude, found.country)
        val saved = preferences.preferences.first()
        return CityLocation(name, saved.cityLatitude ?: found.latitude, saved.cityLongitude ?: found.longitude, saved.cityCountry ?: found.country)
    }

    /** Current weather, or null when there's no city, no connection, or the provider fails. */
    suspend fun current(): WeatherReading? = lock.withLock {
        val city = city() ?: return@withLock null
        val now = System.currentTimeMillis()
        cached?.let { (at, reading) -> if (at == city && policy.fresh(reading.fetchedAtMillis, now)) return@withLock reading }
        // Expired readings are dropped, never shown.
        cached = null
        // A new city always gets a fetch; otherwise respect the provider's interval.
        if (city != lastFetchCity) lastFetchAt = 0L
        if (!policy.mayFetch(lastFetchAt, now)) return@withLock null
        val fresh = try {
            withTimeoutOrNull(TIMEOUT_MILLIS) { source.current(city.latitude, city.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            cached = city to fresh
            lastFetchAt = now
            lastFetchCity = city
        }
        fresh
    }

    /** Android's own geocoder. Returns null if the city isn't found or no geocoder is available. */
    private suspend fun geocode(name: String): Geocoded? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.UK)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(TIMEOUT_MILLIS) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(name, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                cont.resume(addresses.firstOrNull()?.let { Geocoded(it.latitude, it.longitude, it.countryCode) })
                            }

                            override fun onError(errorMessage: String?) {
                                cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(name, 1)?.firstOrNull()?.let { Geocoded(it.latitude, it.longitude, it.countryCode) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 8_000L
    }
}
