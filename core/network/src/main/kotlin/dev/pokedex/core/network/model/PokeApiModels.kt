package dev.pokedex.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Only the fields we use; everything else in PokeAPI's responses is ignored.

@Serializable
data class NamedResource(val name: String, val url: String) {
    /** PokeAPI URLs end with the resource ID, for example ".../pokemon-species/25/". */
    val id: Int get() = url.trimEnd('/').substringAfterLast('/').toInt()
}

@Serializable
data class ResourceList(val count: Int, val results: List<NamedResource>)

@Serializable
data class PokemonDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<TypeSlot>,
    val stats: List<StatDto> = emptyList(),
    val cries: Cries? = null,
)

@Serializable
data class TypeSlot(val slot: Int, val type: NamedResource)

@Serializable
data class StatDto(@SerialName("base_stat") val baseStat: Int, val stat: NamedResource)

/** One elemental type: what it hits hard, and which Pokémon have it. */
@Serializable
data class TypeDto(
    val name: String,
    @SerialName("damage_relations") val damageRelations: DamageRelations,
    val pokemon: List<TypePokemon> = emptyList(),
)

@Serializable
data class DamageRelations(
    @SerialName("double_damage_to") val doubleDamageTo: List<NamedResource> = emptyList(),
    @SerialName("half_damage_to") val halfDamageTo: List<NamedResource> = emptyList(),
    @SerialName("no_damage_to") val noDamageTo: List<NamedResource> = emptyList(),
)

@Serializable
data class TypePokemon(val slot: Int, val pokemon: NamedResource)

@Serializable
data class EvolutionChainDto(val id: Int, val chain: ChainLink)

@Serializable
data class ChainLink(
    val species: NamedResource,
    @SerialName("evolution_details") val evolutionDetails: List<EvolutionDetail> = emptyList(),
    @SerialName("evolves_to") val evolvesTo: List<ChainLink> = emptyList(),
)

/** Only the conditions we describe. Location and region fields are deliberately not read. */
@Serializable
data class EvolutionDetail(
    val trigger: NamedResource? = null,
    val item: NamedResource? = null,
    @SerialName("held_item") val heldItem: NamedResource? = null,
    @SerialName("min_level") val minLevel: Int? = null,
    @SerialName("min_happiness") val minHappiness: Int? = null,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("known_move") val knownMove: NamedResource? = null,
)

@Serializable
data class Cries(val latest: String? = null, val legacy: String? = null)

@Serializable
data class SpeciesDto(
    val id: Int,
    val name: String,
    @SerialName("capture_rate") val captureRate: Int,
    @SerialName("is_legendary") val isLegendary: Boolean = false,
    @SerialName("is_mythical") val isMythical: Boolean = false,
    val genera: List<Genus> = emptyList(),
    val names: List<LocalName> = emptyList(),
    @SerialName("flavor_text_entries") val flavorTextEntries: List<FlavorText> = emptyList(),
    @SerialName("evolution_chain") val evolutionChain: UrlResource? = null,
    val varieties: List<Variety> = emptyList(),
)

/** One form of a species; the default is the species itself. */
@Serializable
data class Variety(@SerialName("is_default") val isDefault: Boolean, val pokemon: NamedResource)

@Serializable
data class UrlResource(val url: String) {
    val id: Int get() = url.trimEnd('/').substringAfterLast('/').toInt()
}

@Serializable
data class Genus(val genus: String, val language: NamedResource)

@Serializable
data class LocalName(val name: String, val language: NamedResource)

@Serializable
data class FlavorText(
    @SerialName("flavor_text") val text: String,
    val language: NamedResource,
    val version: NamedResource,
)
