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
data class CityLocation(val name: String, val latitude: Double, val longitude: Double)

/**
 * Current weather for the user's city. Google Weather when a key is configured, otherwise
 * Open-Meteo. Readings are kept in memory only, for at most three hours: Google allows only
 * temporary caching, and the exact limit in its terms is still to be confirmed.
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

    private val lock = Mutex()
    private var cached: Pair<CityLocation, WeatherReading>? = null

    /** The user's city with its position, looking it up once if needed. Null without a city. */
    suspend fun city(): CityLocation? {
        val prefs = preferences.preferences.first()
        val name = prefs.city ?: return null
        val lat = prefs.cityLatitude
        val lon = prefs.cityLongitude
        if (lat != null && lon != null) return CityLocation(name, lat, lon)
        val found = geocode(name) ?: return null
        preferences.setCityLocation(name, found.first, found.second)
        val saved = preferences.preferences.first()
        return CityLocation(name, saved.cityLatitude ?: found.first, saved.cityLongitude ?: found.second)
    }

    /** Current weather, or null when there's no city, no connection, or the provider fails. */
    suspend fun current(): WeatherReading? = lock.withLock {
        val city = city() ?: return@withLock null
        val now = System.currentTimeMillis()
        cached?.let { (at, reading) -> if (at == city && now - reading.fetchedAtMillis < MAX_AGE_MILLIS) return@withLock reading }
        val fresh = try {
            withTimeoutOrNull(TIMEOUT_MILLIS) { source.current(city.latitude, city.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (fresh != null) cached = city to fresh
        fresh ?: cached?.takeIf { it.first == city && now - it.second.fetchedAtMillis < MAX_AGE_MILLIS }?.second
    }

    /** Android's own geocoder. Returns null if the city isn't found or no geocoder is available. */
    private suspend fun geocode(name: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.UK)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(TIMEOUT_MILLIS) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(name, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                cont.resume(addresses.firstOrNull()?.let { it.latitude to it.longitude })
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
                    geocoder.getFromLocationName(name, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val MAX_AGE_MILLIS = 3 * 60 * 60 * 1000L
        const val TIMEOUT_MILLIS = 8_000L
    }
}
