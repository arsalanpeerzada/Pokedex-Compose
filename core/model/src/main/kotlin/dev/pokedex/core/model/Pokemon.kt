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
}

data class Pokemon(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
    val category: String? = null,
    val heightMetres: Double? = null,
    val weightKilograms: Double? = null,
    val catchRate: Int? = null,
) {
    /** Four-digit Pokédex number, for example "0025". */
    val number: String get() = id.toString().padStart(4, '0')

    val primaryType: PokemonType get() = types.first()

    /** Official artwork served by PokeAPI's sprite repository. */
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}
