package dev.pokedex.core.domain

import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.WeatherBucket
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherReading
import dev.pokedex.core.model.WeatherScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ContextEngineTest {

    private val types = PokemonType.entries
    /** 360 made-up Pokémon, 20 of each type, so type effects are easy to measure. */
    private val pokedex = (1..360).map { id -> Pokemon(id, "P$id", listOf(types[(id - 1) % types.size])) }

    private fun weather(bucket: WeatherBucket, feelsLike: Double = 15.0, day: Boolean = true) =
        WeatherReading(bucket, bucket.name, feelsLike, feelsLike, day, WeatherProvider.OpenMeteo, 0L)

    private fun signals(date: LocalDate = LocalDate.of(2026, 9, 30), time: LocalTime = LocalTime.NOON, weather: WeatherReading? = null, city: String? = "Leeds", lat: Double? = 53.8) =
        Signals(date, time, city, lat, weather)

    /** Share of 400 consecutive days whose pick has one of [wanted]. */
    private fun share(wanted: Set<PokemonType>, make: (LocalDate) -> Signals): Double {
        val start = LocalDate.of(2026, 1, 1)
        val hits = (0 until 400).count { i -> ContextEngine.pick(pokedex, make(start.plusDays(i.toLong())))!!.answer.types.any { it in wanted } }
        return hits / 400.0
    }

    @Test
    fun `weather tilts the odds without dictating`() {
        val cases = mapOf(
            WeatherBucket.Thunder to setOf(PokemonType.Electric),
            WeatherBucket.Rain to setOf(PokemonType.Water, PokemonType.Bug),
            WeatherBucket.Snow to setOf(PokemonType.Ice, PokemonType.Steel),
            WeatherBucket.Wind to setOf(PokemonType.Flying, PokemonType.Dragon),
        )
        cases.forEach { (bucket, favoured) ->
            val baseline = favoured.size / 18.0
            val observed = share(favoured) { signals(date = it, weather = weather(bucket)) }
            assertTrue("$bucket should raise $favoured above $baseline, was $observed", observed > baseline * 2)
            assertTrue("$bucket should not dictate, was $observed", observed < 0.95)
        }
    }

    @Test
    fun `hot clear weather favours Fire and Ground, mild favours Grass, Normal and Bug`() {
        val hot = share(setOf(PokemonType.Fire, PokemonType.Ground)) { signals(date = it, weather = weather(WeatherBucket.Clear, feelsLike = 32.0)) }
        val mild = share(setOf(PokemonType.Fire, PokemonType.Ground)) { signals(date = it, weather = weather(WeatherBucket.Clear, feelsLike = 15.0)) }
        assertTrue(hot > mild * 2)
    }

    @Test
    fun `the same date and city always give the same Pokemon`() {
        val a = ContextEngine.pick(pokedex, signals(weather = weather(WeatherBucket.Rain)))!!
        val b = ContextEngine.pick(pokedex, signals(weather = weather(WeatherBucket.Rain), city = "  leeds "))!!
        assertEquals(a.answer, b.answer)
        assertEquals(a.decoys, b.decoys)
    }

    @Test
    fun `recent picks never repeat`() {
        val first = ContextEngine.pick(pokedex, signals())!!.answer
        val second = ContextEngine.pick(pokedex, signals(), recent = setOf(first.id))!!.answer
        assertNotEquals(first, second)
    }

    @Test
    fun `decoys are three others`() {
        val pick = ContextEngine.pick(pokedex, signals())!!
        assertEquals(3, pick.decoys.size)
        assertFalse(pick.answer in pick.decoys)
        assertEquals(3, pick.decoys.toSet().size)
    }

    @Test
    fun `legendaries are rare unless it's their weather`() {
        val legends = pokedex.map { if (it.id % 18 == 0) it.copy(isLegendary = true) else it }
        val start = LocalDate.of(2026, 1, 1)
        val legendaryDays = (0 until 400).count { i -> ContextEngine.pick(legends, signals(date = start.plusDays(i.toLong())))!!.answer.isLegendary }
        assertTrue("Legendaries picked on $legendaryDays of 400 days", legendaryDays < 20)
    }

    @Test
    fun `seasons flip south of the equator`() {
        val july = LocalDate.of(2026, 7, 1)
        assertEquals(Season.Summer, ContextEngine.season(july, 53.8))
        assertEquals(Season.Winter, ContextEngine.season(july, -33.9))
        assertEquals(Season.Summer, ContextEngine.season(july, null))
    }

    @Test
    fun `the weather's night beats the clock`() {
        assertEquals(TimeOfDay.Night, ContextEngine.timeOfDay(LocalTime.of(18, 0), weather(WeatherBucket.Clear, day = false)))
        assertEquals(TimeOfDay.Morning, ContextEngine.timeOfDay(LocalTime.of(7, 0), null))
        assertEquals(TimeOfDay.Night, ContextEngine.timeOfDay(LocalTime.of(23, 0), null))
    }

    @Test
    fun `scenes follow the weather and the night`() {
        assertEquals(WeatherScene.Storm, ContextEngine.scene(signals(weather = weather(WeatherBucket.Thunder))))
        assertEquals(WeatherScene.Heat, ContextEngine.scene(signals(weather = weather(WeatherBucket.Clear, feelsLike = 30.0))))
        assertEquals(WeatherScene.Night, ContextEngine.scene(signals(weather = weather(WeatherBucket.Clear, day = false))))
        assertEquals(WeatherScene.Night, ContextEngine.scene(signals(time = LocalTime.of(23, 30))))
    }

    @Test
    fun `special days are recognised and named in the hint`() {
        assertEquals(SpecialDay.Halloween, ContextEngine.specialDay(LocalDate.of(2026, 10, 31)))
        val pick = ContextEngine.pick(pokedex, signals(date = LocalDate.of(2026, 10, 31)))!!
        assertTrue(pick.hint.endsWith("It's Halloween."))
    }

    @Test
    fun `national days apply only in their country`() {
        val unity = LocalDate.of(2026, 10, 3)
        assertEquals(SpecialDay.GermanUnityDay, ContextEngine.specialDay(unity, "DE"))
        assertEquals(SpecialDay.GermanUnityDay, ContextEngine.specialDay(unity, "de"))
        assertEquals(null, ContextEngine.specialDay(unity, "GB"))
        assertEquals(null, ContextEngine.specialDay(unity, null))
        assertEquals(SpecialDay.BastilleDay, ContextEngine.specialDay(LocalDate.of(2026, 7, 14), "FR"))
        assertEquals(SpecialDay.IndependenceDayUs, ContextEngine.specialDay(LocalDate.of(2026, 7, 4), "US"))
    }

    @Test
    fun `king's day moves to Saturday when 27 April is a Sunday`() {
        // 27 April 2025 was a Sunday; 27 April 2026 is a Monday.
        assertEquals(SpecialDay.KingsDay, ContextEngine.specialDay(LocalDate.of(2025, 4, 26), "NL"))
        assertEquals(null, ContextEngine.specialDay(LocalDate.of(2025, 4, 27), "NL"))
        assertEquals(SpecialDay.KingsDay, ContextEngine.specialDay(LocalDate.of(2026, 4, 27), "NL"))
    }

    @Test
    fun `a national day is named in the hint and favours celebration types`() {
        val unity = LocalDate.of(2026, 10, 3)
        val pick = ContextEngine.pick(pokedex, signals(date = unity, city = "Berlin").copy(country = "DE"))!!
        assertTrue(pick.hint.endsWith("It's German Unity Day."))
        // Same day, 300 different cities (so 300 different seeds).
        val hits = (1..300).count { i ->
            val p = ContextEngine.pick(pokedex, signals(date = unity, city = "City $i", lat = 52.5).copy(country = "DE"))!!
            p.answer.types.any { it == PokemonType.Fire || it == PokemonType.Fairy }
        }
        assertTrue("Celebration types should be favoured, were $hits of 300", hits / 300.0 > 2 * 2 / 18.0)
    }

    @Test
    fun `hints mention the weather but never a type`() {
        val pick = ContextEngine.pick(pokedex, signals(weather = weather(WeatherBucket.Thunder, day = false), time = LocalTime.of(22, 0)))!!
        assertEquals("Stormy in Leeds tonight.", pick.hint)
        PokemonType.entries.forEach { assertFalse(pick.hint.contains(it.displayName)) }
    }

    @Test
    fun `the reason names the signals that won`() {
        val thunder = weather(WeatherBucket.Thunder)
        val start = LocalDate.of(2026, 3, 1)
        val electricDay = (0 until 60).map { start.plusDays(it.toLong()) }
            .first { ContextEngine.pick(pokedex, signals(date = it, weather = thunder))!!.answer.types.contains(PokemonType.Electric) }
        val pick = ContextEngine.pick(pokedex, signals(date = electricDay, weather = thunder))!!
        assertTrue(pick.reason, pick.reason.contains("Electric weather"))
        assertTrue(pick.reason, pick.reason.startsWith("Thunder in Leeds"))
    }
}
