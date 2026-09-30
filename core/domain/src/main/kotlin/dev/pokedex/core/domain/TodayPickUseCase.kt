package dev.pokedex.core.domain

import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherReading
import dev.pokedex.core.model.WeatherScene
import dev.pokedex.core.weather.WeatherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Today's Pokémon with everything the screens need around it. */
data class TodayPick(
    val epochDay: Long,
    val answer: Pokemon,
    val choices: List<Pokemon>,
    val reason: String?,
    val hint: String?,
    val weather: WeatherReading?,
    val city: String?,
    val scene: WeatherScene,
    val isNight: Boolean,
)

/** Today's pick, as seen by the screens and the widget. Null until the Pokédex has downloaded. */
fun interface TodayPicker {
    suspend operator fun invoke(): TodayPick?
}

/**
 * Makes the day's pick once and saves it, so Today, the widget and the reminder agree all day.
 * The weather and scene are refreshed each time; the Pokémon isn't.
 */
@Singleton
class TodayPickUseCase @Inject constructor(
    private val pokemon: PokemonRepository,
    private val daily: DailyRepository,
    private val weather: WeatherRepository,
) : TodayPicker {
    private val lock = Mutex()

    override suspend fun invoke(): TodayPick? = pick(LocalDateTime.now())

    suspend fun pick(now: LocalDateTime): TodayPick? = lock.withLock {
        val pokedex = pokemon.pokedex().first()
        if (pokedex.isEmpty()) return@withLock null
        val date = now.toLocalDate()
        val day = date.toEpochDay()
        val city = weather.city()
        val reading = weather.current()
        val signals = Signals(date, now.toLocalTime(), city?.name, city?.latitude, reading)

        val saved = daily.get(day)
        val savedAnswer = saved?.let { s -> pokedex.firstOrNull { it.id == s.pokemonId } }
        val (answer, decoys, reason, hint) = if (saved != null && savedAnswer != null) {
            Quad(savedAnswer, ContextEngine.decoys(pokedex, savedAnswer, date), saved.reason, saved.hint)
        } else {
            val pick = ContextEngine.pick(pokedex, signals, daily.recentPokemonIds(day, ContextEngine.NO_REPEAT_DAYS)) ?: return@withLock null
            daily.save(DailyResult(day, pick.answer.id, solved = false, wrongGuesses = emptySet(), hintsUsed = 0, reason = pick.reason, hint = pick.hint))
            Quad(pick.answer, pick.decoys, pick.reason, pick.hint)
        }
        TodayPick(
            epochDay = day,
            answer = answer,
            choices = (decoys + answer).shuffled(kotlin.random.Random(day * 31 + answer.id)),
            reason = reason,
            hint = hint,
            weather = reading,
            city = city?.name,
            scene = ContextEngine.scene(signals),
            isNight = ContextEngine.timeOfDay(signals.time, reading) == TimeOfDay.Night,
        )
    }

    private data class Quad(val answer: Pokemon, val decoys: List<Pokemon>, val reason: String?, val hint: String?)
}
