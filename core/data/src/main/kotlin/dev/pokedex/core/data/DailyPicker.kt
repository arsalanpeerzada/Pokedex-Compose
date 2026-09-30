package dev.pokedex.core.data

import dev.pokedex.core.model.Pokemon
import java.time.LocalDate
import kotlin.random.Random

/**
 * Today's Pokémon, the same for everyone on a given date, shared by Today and the home-screen widget.
 * Until the context engine lands (Sprint 3), the date is the only input.
 */
object DailyPicker {
    /** One answer and three decoys, shuffled, from a list in National Dex order. */
    fun pick(pokemon: List<Pokemon>, date: LocalDate): Pair<Pokemon, List<Pokemon>> {
        val random = Random(date.toEpochDay())
        val picks = pokemon.shuffled(random).take(4)
        return picks.first() to picks.shuffled(random)
    }
}
