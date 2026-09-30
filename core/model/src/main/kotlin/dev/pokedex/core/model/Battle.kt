package dev.pokedex.core.model

data class BaseStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int,
) {
    val total: Int get() = hp + attack + defense + specialAttack + specialDefense + speed

    fun asList(): List<Pair<String, Int>> = listOf(
        "HP" to hp,
        "Attack" to attack,
        "Defence" to defense,
        "Sp. Atk" to specialAttack,
        "Sp. Def" to specialDefense,
        "Speed" to speed,
    )
}

/**
 * Damage multipliers from PokeAPI's type data. Only non-neutral pairs are stored;
 * everything else is 1x.
 */
class TypeChart(private val factors: Map<Pair<PokemonType, PokemonType>, Float>) {

    val isEmpty: Boolean get() = factors.isEmpty()

    fun factor(attacker: PokemonType, defender: PokemonType): Float = factors[attacker to defender] ?: 1f

    /** How hard each attacking type hits a Pokémon with these types (dual types multiply). */
    fun defending(types: List<PokemonType>): Map<PokemonType, Float> =
        PokemonType.entries.associateWith { attacker -> types.fold(1f) { acc, t -> acc * factor(attacker, t) } }

    companion object {
        val Empty = TypeChart(emptyMap())
    }
}

/** One Pokémon in an evolution chain. [fromId] is null for the first stage. */
data class EvolutionStep(
    val speciesId: Int,
    val name: String,
    val fromId: Int?,
    val method: String?,
)

/** An alternate form. [id] is PokeAPI's pokemon id (above 10,000), which also finds its artwork. */
data class PokemonForm(val id: Int, val label: String) {
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}

/** The user's result for one day's Pokémon, keyed by the local date. */
data class DailyResult(
    val epochDay: Long,
    val pokemonId: Int,
    val solved: Boolean,
    val wrongGuesses: Set<Int>,
    val hintsUsed: Int,
)
