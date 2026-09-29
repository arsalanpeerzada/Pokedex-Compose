package dev.pokedex.core.data

import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.UserState
import kotlinx.coroutines.flow.Flow

/** Offline-first: everything is read from Room; the network only ever fills Room. */
interface PokemonRepository {
    fun pokedex(): Flow<List<Pokemon>>
    fun pokemon(id: Int): Flow<Pokemon?>
    fun userStates(): Flow<Map<Int, UserState>>

    /** Downloads the species index once (a single request). Safe to call repeatedly. */
    suspend fun refreshIndex(): Result<Unit>

    /** Fetches types and facts the first time a Pokémon is needed, then never again. */
    suspend fun ensureDetails(id: Int)

    suspend fun setCaught(id: Int, caught: Boolean)
    suspend fun toggleFavourite(id: Int)
    suspend fun markSeen(id: Int)
}
