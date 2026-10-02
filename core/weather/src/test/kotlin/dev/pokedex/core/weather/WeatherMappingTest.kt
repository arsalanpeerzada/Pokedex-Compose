package dev.pokedex.core.weather

import dev.pokedex.core.model.WeatherBucket
import dev.pokedex.core.model.WeatherProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMappingTest {

    @Test
    fun `google condition types follow the plan's table`() {
        val cases = mapOf(
            "CLEAR" to WeatherBucket.Clear,
            "MOSTLY_CLEAR" to WeatherBucket.Clear,
            "PARTLY_CLOUDY" to WeatherBucket.Cloudy,
            "CLOUDY" to WeatherBucket.Cloudy,
            "LIGHT_RAIN" to WeatherBucket.Rain,
            "SCATTERED_SHOWERS" to WeatherBucket.Rain,
            "THUNDERSTORM" to WeatherBucket.Thunder,
            "LIGHT_THUNDERSTORM_RAIN" to WeatherBucket.Thunder,
            "HEAVY_SNOW" to WeatherBucket.Snow,
            "RAIN_AND_SNOW" to WeatherBucket.Snow,
            "HAIL_SHOWERS" to WeatherBucket.Snow,
            "WINDY" to WeatherBucket.Wind,
            "WIND_AND_RAIN" to WeatherBucket.Wind,
            "SOMETHING_NEW" to WeatherBucket.Cloudy,
        )
        cases.forEach { (type, bucket) -> assertEquals(type, bucket, googleBucket(type)) }
    }

    @Test
    fun `wmo codes map to buckets, with wind only when nothing else is happening`() {
        assertEquals(WeatherBucket.Clear, openMeteoBucket(0, 5.0))
        assertEquals(WeatherBucket.Cloudy, openMeteoBucket(3, 5.0))
        assertEquals(WeatherBucket.Cloudy, openMeteoBucket(45, 5.0))
        assertEquals(WeatherBucket.Rain, openMeteoBucket(53, 5.0))
        assertEquals(WeatherBucket.Rain, openMeteoBucket(81, 60.0))
        assertEquals(WeatherBucket.Snow, openMeteoBucket(86, 5.0))
        assertEquals(WeatherBucket.Thunder, openMeteoBucket(95, 5.0))
        assertEquals(WeatherBucket.Wind, openMeteoBucket(1, WINDY_KMH))
    }

    @Test
    fun `google readings expire after an hour and google is asked at most every three`() {
        val hour = 60 * 60 * 1000L
        val google = CachePolicy.of(WeatherProvider.Google)
        assertEquals(true, google.fresh(cachedAt = 0, now = hour - 1))
        assertEquals(false, google.fresh(cachedAt = 0, now = hour))
        assertEquals(true, google.mayFetch(lastFetchAt = 0, now = 5))
        assertEquals(false, google.mayFetch(lastFetchAt = hour, now = 3 * hour))
        assertEquals(true, google.mayFetch(lastFetchAt = hour, now = 4 * hour))
    }

    @Test
    fun `open-meteo readings last three hours and it can be asked any time`() {
        val hour = 60 * 60 * 1000L
        val openMeteo = CachePolicy.of(WeatherProvider.OpenMeteo)
        assertEquals(true, openMeteo.fresh(cachedAt = 0, now = 3 * hour - 1))
        assertEquals(true, openMeteo.mayFetch(lastFetchAt = hour, now = hour + 1))
    }

    @Test
    fun `every documented code has a description`() {
        listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 97, 99)
            .forEach { code -> assert(wmoDescription(code) != "Current weather") { "No description for $code" } }
    }
}
