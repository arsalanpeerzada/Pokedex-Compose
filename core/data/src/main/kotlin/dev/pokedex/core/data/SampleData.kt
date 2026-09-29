package dev.pokedex.core.data

import dev.pokedex.core.model.CollectionEntry
import dev.pokedex.core.model.CollectionState
import dev.pokedex.core.model.CollectionSummary
import dev.pokedex.core.model.DailyPick
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType.Bug
import dev.pokedex.core.model.PokemonType.Electric
import dev.pokedex.core.model.PokemonType.Fire
import dev.pokedex.core.model.PokemonType.Flying
import dev.pokedex.core.model.PokemonType.Ghost
import dev.pokedex.core.model.PokemonType.Grass
import dev.pokedex.core.model.PokemonType.Normal
import dev.pokedex.core.model.PokemonType.Poison
import dev.pokedex.core.model.PokemonType.Water
import dev.pokedex.core.model.Weather
import dev.pokedex.core.model.WeatherScene

/**
 * Hand-written sample data matching the Figma designs, used until the PokeAPI + Room data layer lands.
 * Pikachu's facts come from PokeAPI (checked 29 September 2026); other entries carry names and types only.
 */
object SampleData {
    val pikachu = Pokemon(25, "Pikachu", listOf(Electric), category = "Mouse Pokémon", heightMetres = 0.4, weightKilograms = 6.0, catchRate = 190)
    val eevee = Pokemon(133, "Eevee", listOf(Normal))
    val gengar = Pokemon(94, "Gengar", listOf(Ghost, Poison))

    val generationOne: List<Pokemon> = listOf(
        Pokemon(1, "Bulbasaur", listOf(Grass, Poison)),
        Pokemon(2, "Ivysaur", listOf(Grass, Poison)),
        Pokemon(3, "Venusaur", listOf(Grass, Poison)),
        Pokemon(4, "Charmander", listOf(Fire)),
        Pokemon(5, "Charmeleon", listOf(Fire)),
        Pokemon(6, "Charizard", listOf(Fire, Flying)),
        Pokemon(7, "Squirtle", listOf(Water)),
        Pokemon(8, "Wartortle", listOf(Water)),
        Pokemon(9, "Blastoise", listOf(Water)),
        Pokemon(10, "Caterpie", listOf(Bug)),
        Pokemon(11, "Metapod", listOf(Bug)),
        Pokemon(12, "Butterfree", listOf(Bug, Flying)),
        Pokemon(13, "Weedle", listOf(Bug, Poison)),
        Pokemon(14, "Kakuna", listOf(Bug, Poison)),
        Pokemon(15, "Beedrill", listOf(Bug, Poison)),
        Pokemon(16, "Pidgey", listOf(Normal, Flying)),
    )

    private val caughtIds = setOf(1, 2, 3, 4)
    private val seenIds = setOf(5, 6, 7)

    val caught: Set<Int> get() = caughtIds

    val collection: List<CollectionEntry> = generationOne.map { pokemon ->
        val state = when (pokemon.id) {
            in caughtIds -> CollectionState.Caught
            in seenIds -> CollectionState.Seen
            else -> CollectionState.Unseen
        }
        CollectionEntry(pokemon, state)
    }

    val collectionSummary = CollectionSummary(
        caught = 212,
        total = 1025,
        streakDays = 12,
        latestCaught = listOf(gengar, eevee, pikachu),
        generationLabel = "Generation I",
        generationCaught = 58,
        generationTotal = 151,
    )

    val today = DailyPick(
        city = "London",
        dateLabel = "Tuesday 29 September",
        weather = Weather(WeatherScene.Storm, "Thunderstorm", temperatureCelsius = 17, isNight = true),
        answer = pikachu,
        choices = listOf(pikachu, eevee, gengar, generationOne[3]),
        weatherHint = "Stormy in London tonight.",
        hintsLeft = 2,
    )

    fun pokemon(id: Int): Pokemon? = (generationOne + listOf(pikachu, eevee, gengar)).firstOrNull { it.id == id }
}
