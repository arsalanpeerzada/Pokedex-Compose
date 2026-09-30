package dev.pokedex.core.domain

import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.PokemonType.Bug
import dev.pokedex.core.model.PokemonType.Dark
import dev.pokedex.core.model.PokemonType.Dragon
import dev.pokedex.core.model.PokemonType.Electric
import dev.pokedex.core.model.PokemonType.Fairy
import dev.pokedex.core.model.PokemonType.Fire
import dev.pokedex.core.model.PokemonType.Flying
import dev.pokedex.core.model.PokemonType.Ghost
import dev.pokedex.core.model.PokemonType.Grass
import dev.pokedex.core.model.PokemonType.Ground
import dev.pokedex.core.model.PokemonType.Ice
import dev.pokedex.core.model.PokemonType.Normal
import dev.pokedex.core.model.PokemonType.Poison
import dev.pokedex.core.model.PokemonType.Steel
import dev.pokedex.core.model.PokemonType.Water
import dev.pokedex.core.model.WeatherBucket
import dev.pokedex.core.model.WeatherReading
import dev.pokedex.core.model.WeatherScene
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import kotlin.random.Random

/** Everything the engine knows about "today" for one user. */
data class Signals(
    val date: LocalDate,
    val time: LocalTime,
    val city: String?,
    val latitude: Double?,
    val weather: WeatherReading?,
)

enum class Season(val label: String) { Spring("spring"), Summer("summer"), Autumn("autumn"), Winter("winter") }

enum class TimeOfDay(val phrase: String) { Morning("this morning"), Day("today"), Evening("this evening"), Night("tonight") }

/** Bundled special days (plan, section 4). National days and religious festivals are open decisions. */
enum class SpecialDay(val label: String, val day: MonthDay, val types: Set<PokemonType>, val speciesId: Int? = null) {
    PokemonDay("Pokémon Day", MonthDay.of(2, 27), setOf(Electric), speciesId = 25),
    Halloween("Halloween", MonthDay.of(10, 31), setOf(Ghost)),
    NewYearsEve("New Year's Eve", MonthDay.of(12, 31), setOf(Fire)),
}

/** The engine's answer: who, why, and a hint that mentions only the weather. */
data class Pick(val answer: Pokemon, val decoys: List<Pokemon>, val reason: String, val hint: String)

/**
 * The context engine. Weather, time of day, season and special days tilt the odds towards
 * certain types; they never dictate. Pure and deterministic: the same inputs always give the
 * same Pokémon, so everyone in a city shares one each day. Weights are a first draft (plan, section 4).
 */
object ContextEngine {

    /** "Hot" for the clear-and-hot bucket, by feels-like temperature. First draft, tunable. */
    const val HOT_CELSIUS = 27.0

    private const val BASE = 1.0
    private const val WEATHER = 6.0
    private const val NIGHT = 3.0
    private const val SEASON = 2.0
    private const val SPECIAL = 12.0
    private const val SPECIAL_SPECIES = 40.0
    private const val LEGENDARY_FACTOR = 0.05
    private const val LEGENDARY_MOMENT = 3.0

    /** No species repeats within this many days in a city. */
    const val NO_REPEAT_DAYS = 60

    private val weatherTypes = mapOf(
        WeatherKind.ClearHot to setOf(Fire, Ground),
        WeatherKind.ClearMild to setOf(Grass, Normal, Bug),
        WeatherKind.Cloudy to setOf(Normal, Fairy, Poison),
        WeatherKind.Rain to setOf(Water, Bug),
        WeatherKind.Thunder to setOf(Electric),
        WeatherKind.Snow to setOf(Ice, Steel),
        WeatherKind.Wind to setOf(Flying, Dragon),
    )

    /** Rare, weather-bound legendaries: Moltres and Groudon, Kyogre, Zapdos, Articuno, Rayquaza. */
    private val legendaryMoments = mapOf(
        WeatherKind.ClearHot to setOf(146, 383),
        WeatherKind.Rain to setOf(382),
        WeatherKind.Thunder to setOf(145),
        WeatherKind.Snow to setOf(144),
        WeatherKind.Wind to setOf(384),
    )

    private val seasonTypes = mapOf(
        Season.Spring to setOf(Grass, Bug, Fairy),
        Season.Summer to setOf(Fire, Water),
        Season.Autumn to setOf(Ground, Normal),
        Season.Winter to setOf(Ice, Steel),
    )

    private val nightTypes = setOf(Dark, Ghost)

    internal enum class WeatherKind { ClearHot, ClearMild, Cloudy, Rain, Thunder, Snow, Wind }

    internal fun weatherKind(weather: WeatherReading): WeatherKind = when (weather.bucket) {
        WeatherBucket.Clear -> if (weather.feelsLikeCelsius >= HOT_CELSIUS) WeatherKind.ClearHot else WeatherKind.ClearMild
        WeatherBucket.Cloudy -> WeatherKind.Cloudy
        WeatherBucket.Rain -> WeatherKind.Rain
        WeatherBucket.Thunder -> WeatherKind.Thunder
        WeatherBucket.Snow -> WeatherKind.Snow
        WeatherBucket.Wind -> WeatherKind.Wind
    }

    /** Meteorological seasons, flipped south of the equator. Unknown latitude counts as northern. */
    fun season(date: LocalDate, latitude: Double?): Season {
        val northern = when (date.monthValue) {
            3, 4, 5 -> Season.Spring
            6, 7, 8 -> Season.Summer
            9, 10, 11 -> Season.Autumn
            else -> Season.Winter
        }
        if (latitude == null || latitude >= 0) return northern
        return when (northern) {
            Season.Spring -> Season.Autumn
            Season.Summer -> Season.Winter
            Season.Autumn -> Season.Spring
            Season.Winter -> Season.Summer
        }
    }

    /** From the clock, but the weather's own day or night wins when we have it. */
    fun timeOfDay(time: LocalTime, weather: WeatherReading?): TimeOfDay {
        val byClock = when (time.hour) {
            in 5..10 -> TimeOfDay.Morning
            in 11..16 -> TimeOfDay.Day
            in 17..20 -> TimeOfDay.Evening
            else -> TimeOfDay.Night
        }
        return when {
            weather == null -> byClock
            !weather.isDaytime -> TimeOfDay.Night
            byClock == TimeOfDay.Night -> TimeOfDay.Evening
            else -> byClock
        }
    }

    fun specialDay(date: LocalDate): SpecialDay? = SpecialDay.entries.firstOrNull { it.day == MonthDay.from(date) }

    /** Today's sky, for the background: current conditions, or the time of day without weather. */
    fun scene(signals: Signals): WeatherScene {
        val night = timeOfDay(signals.time, signals.weather) == TimeOfDay.Night
        val weather = signals.weather ?: return if (night) WeatherScene.Night else WeatherScene.Clear
        return when (weatherKind(weather)) {
            WeatherKind.Thunder -> WeatherScene.Storm
            WeatherKind.Rain -> WeatherScene.Rain
            WeatherKind.Snow -> WeatherScene.Snow
            WeatherKind.Wind -> WeatherScene.Wind
            WeatherKind.Cloudy -> if (night) WeatherScene.Night else WeatherScene.Cloud
            WeatherKind.ClearHot -> if (night) WeatherScene.Night else WeatherScene.Heat
            WeatherKind.ClearMild -> if (night) WeatherScene.Night else WeatherScene.Clear
        }
    }

    /** Same date and city, same seed; the city's name is normalised so "Leeds" and "leeds " match. */
    internal fun seed(date: LocalDate, city: String?): Long =
        date.toEpochDay() * 1_000_003L + (city?.trim()?.lowercase()?.hashCode() ?: 0)

    /**
     * Picks today's Pokémon from a list in National Dex order. Returns null only for an empty list.
     * [recent] ids are skipped so nothing repeats within [NO_REPEAT_DAYS].
     */
    fun pick(pokedex: List<Pokemon>, signals: Signals, recent: Set<Int> = emptySet()): Pick? {
        if (pokedex.isEmpty()) return null
        val kind = signals.weather?.let(::weatherKind)
        val season = season(signals.date, signals.latitude)
        val timeOfDay = timeOfDay(signals.time, signals.weather)
        val special = specialDay(signals.date)
        val favouredByWeather = kind?.let { weatherTypes[it] }.orEmpty()
        val moments = kind?.let { legendaryMoments[it] }.orEmpty()

        fun weight(p: Pokemon): Double {
            if (p.id in recent) return 0.0
            if (p.id in moments) return LEGENDARY_MOMENT
            var w = BASE
            if (p.types.any { it in favouredByWeather }) w += WEATHER
            if (timeOfDay == TimeOfDay.Night && p.types.any { it in nightTypes }) w += NIGHT
            if (p.types.any { it in seasonTypes.getValue(season) }) w += SEASON
            if (special != null && p.types.any { it in special.types }) w += SPECIAL
            if (special?.speciesId == p.id) w += SPECIAL_SPECIES
            if (p.isLegendary || p.isMythical) w *= LEGENDARY_FACTOR
            return w
        }

        val random = Random(seed(signals.date, signals.city))
        val weights = pokedex.map(::weight)
        val total = weights.sum()
        val answer = if (total <= 0.0) {
            pokedex[random.nextInt(pokedex.size)]
        } else {
            var target = random.nextDouble() * total
            pokedex.indices.firstOrNull { i -> target -= weights[i]; target < 0 }?.let(pokedex::get) ?: pokedex.last()
        }
        return Pick(
            answer = answer,
            decoys = decoys(pokedex, answer, signals.date),
            reason = reason(answer, signals, kind, season, timeOfDay, special, favouredByWeather, answer.id in moments),
            hint = hint(signals, timeOfDay, season, special),
        )
    }

    /** Three other Pokémon, the same for the whole day once the answer is known. */
    fun decoys(pokedex: List<Pokemon>, answer: Pokemon, date: LocalDate): List<Pokemon> {
        val random = Random(date.toEpochDay() * 7_919L + answer.id)
        return pokedex.filter { it.id != answer.id }.shuffled(random).take(3)
    }

    private fun reason(
        answer: Pokemon,
        signals: Signals,
        kind: WeatherKind?,
        season: Season,
        timeOfDay: TimeOfDay,
        special: SpecialDay?,
        favouredByWeather: Set<PokemonType>,
        legendaryMoment: Boolean,
    ): String {
        val place = signals.city?.let { " in $it" }.orEmpty()
        val prefix = signals.weather?.let { "${it.description}$place ${timeOfDay.phrase}" }
            ?: "${season.label.replaceFirstChar { it.uppercase() }}$place, ${timeOfDay.phrase}"
        val because = buildList {
            if (special != null && (special.speciesId == answer.id || answer.types.any { it in special.types })) add(special.label)
            if (legendaryMoment) add("a rare legendary moment")
            val weatherMatch = answer.types.filter { it in favouredByWeather }
            if (kind != null && weatherMatch.isNotEmpty()) add("${weatherMatch.joinToString(" and ") { it.displayName }} weather")
            if (timeOfDay == TimeOfDay.Night && answer.types.any { it in nightTypes }) add("night-time")
            if (answer.types.any { it in seasonTypes.getValue(season) }) add("${season.label} favourite")
        }
        return if (because.isEmpty()) "$prefix. A lucky pick: today's signals didn't favour its type." else "$prefix: ${because.joinToString(", ")}."
    }

    private fun hint(signals: Signals, timeOfDay: TimeOfDay, season: Season, special: SpecialDay?): String {
        val place = signals.city?.let { " in $it" }.orEmpty()
        val weather = signals.weather
        val base = if (weather != null) {
            val mood = when (weatherKind(weather)) {
                WeatherKind.ClearHot -> "Hot and sunny"
                WeatherKind.ClearMild -> if (timeOfDay == TimeOfDay.Night) "Clear skies" else "Sunny"
                WeatherKind.Cloudy -> "Cloudy"
                WeatherKind.Rain -> "Rainy"
                WeatherKind.Thunder -> "Stormy"
                WeatherKind.Snow -> "Snowy"
                WeatherKind.Wind -> "Windy"
            }
            "$mood$place ${timeOfDay.phrase}."
        } else {
            val article = if (season == Season.Autumn) "An" else "A"
            "$article ${season.label} day$place."
        }
        return special?.let { "$base It's ${it.label}." } ?: base
    }
}
