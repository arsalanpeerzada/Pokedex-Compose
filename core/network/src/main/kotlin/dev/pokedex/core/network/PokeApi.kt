package dev.pokedex.core.network

import dev.pokedex.core.network.model.PokemonDto
import dev.pokedex.core.network.model.ResourceList
import dev.pokedex.core.network.model.SpeciesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PokeAPI v2. Its fair-use policy asks clients to cache locally, so callers store every
 * response in Room and never fetch the same resource twice.
 */
@Singleton
class PokeApi @Inject constructor(private val client: HttpClient) {

    /** Every species (1,025 today) in one request: names and IDs only. */
    suspend fun speciesIndex(): ResourceList =
        client.get("pokemon-species") { parameter("limit", 2000) }.body()

    suspend fun pokemon(id: Int): PokemonDto = client.get("pokemon/$id").body()

    suspend fun species(id: Int): SpeciesDto = client.get("pokemon-species/$id").body()
}
