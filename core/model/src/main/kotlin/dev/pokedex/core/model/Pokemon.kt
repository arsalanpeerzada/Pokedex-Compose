package dev.pokedex.core.model

enum class PokemonType(val displayName: String) {
    Normal("Normal"),
    Fire("Fire"),
    Water("Water"),
    Electric("Electric"),
    Grass("Grass"),
    Ice("Ice"),
    Fighting("Fighting"),
    Poison("Poison"),
    Ground("Ground"),
    Flying("Flying"),
    Psychic("Psychic"),
    Bug("Bug"),
    Rock("Rock"),
    Ghost("Ghost"),
    Dragon("Dragon"),
    Dark("Dark"),
    Steel("Steel"),
    Fairy("Fairy"),
    ;

    companion object {
        fun fromApiName(name: String): PokemonType? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

data class Pokemon(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
    val category: String? = null,
    val heightMetres: Double? = null,
    val weightKilograms: Double? = null,
    val catchRate: Int? = null,
    val flavourText: String? = null,
    val flavourVersion: String? = null,
    val cryUrl: String? = null,
    val isLegendary: Boolean = false,
    val isMythical: Boolean = false,
    val stats: BaseStats? = null,
    val evolutionChainId: Int? = null,
    /** False until the lazy detail fetch has filled the facts. Types can arrive earlier, from the type index. */
    val hasDetails: Boolean = true,
) {
    /** Four-digit Pokédex number, for example "0025". */
    val number: String get() = id.toString().padStart(4, '0')

    /** Normal until details load, so cards stay neutral rather than guessing. */
    val primaryType: PokemonType get() = types.firstOrNull() ?: PokemonType.Normal

    val generation: Generation get() = Generation.of(id)

    /** Official artwork served by PokeAPI's sprite repository. */
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}

/** Generations by National Pokédex range. Browsing is by generation, never by region. */
enum class Generation(val label: String, val range: IntRange) {
    I("Generation I", 1..151),
    II("Generation II", 152..251),
    III("Generation III", 252..386),
    IV("Generation IV", 387..493),
    V("Generation V", 494..649),
    VI("Generation VI", 650..721),
    VII("Generation VII", 722..809),
    VIII("Generation VIII", 810..905),
    IX("Generation IX", 906..1025),
    ;

    val size: Int get() = range.last - range.first + 1

    companion object {
        fun of(id: Int): Generation = entries.firstOrNull { id in it.range } ?: IX
    }
}

data class UserState(
    val caughtAt: Long? = null,
    val seen: Boolean = false,
    val favourite: Boolean = false,
) {
    val caught: Boolean get() = caughtAt != null
}
