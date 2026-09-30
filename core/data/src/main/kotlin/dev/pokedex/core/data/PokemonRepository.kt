package dev.pokedex.core.data

import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.UserState
import kotlinx.coroutines.flow.Flow

/** Offline-first: everything is read from Room; the network only ever fills Room. */
interface PokemonRepository {
    fun pokedex(): Flow<List<Pokemon>>
    fun pokemon(id: Int): Flow<Pokemon?>
    fun userStates(): Flow<Map<Int, UserState>>
    fun typeChart(): Flow<TypeChart>
    fun evolution(chainId: Int): Flow<List<EvolutionStep>>

    /**
     * Downloads the species index (one request) and the type index (18 requests), once each.
     * Safe to call repeatedly. Fails only if the species index is still missing.
     */
    suspend fun refreshIndex(): Result<Unit>

    /** Fetches stats and facts the first time a Pokémon is needed, then never again. */
    suspend fun ensureDetails(id: Int)

    /** Fetches an evolution chain the first time it's needed. */
    suspend fun ensureEvolution(chainId: Int)

    suspend fun setCaught(id: Int, caught: Boolean)
    suspend fun toggleFavourite(id: Int)
    suspend fun markSeen(id: Int)
}

/** Today's guessing game: one result per local date, and the streak built from them. */
interface DailyRepository {
    fun result(epochDay: Long): Flow<DailyResult?>
    suspend fun save(result: DailyResult)

    /** Consecutive solved days ending today, or yesterday if today isn't solved yet. */
    fun streak(todayEpochDay: Long): Flow<Int>
}
