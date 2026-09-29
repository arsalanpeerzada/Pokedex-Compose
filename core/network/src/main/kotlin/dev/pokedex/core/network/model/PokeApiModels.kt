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
    val cries: Cries? = null,
)

@Serializable
data class TypeSlot(val slot: Int, val type: NamedResource)

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
)

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
