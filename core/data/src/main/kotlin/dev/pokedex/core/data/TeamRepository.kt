package dev.pokedex.core.data

import dev.pokedex.core.database.TeamDao
import dev.pokedex.core.database.TeamEntity
import dev.pokedex.core.database.TeamMemberEntity
import dev.pokedex.core.model.TEAM_SIZE
import dev.pokedex.core.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

interface TeamRepository {
    fun teams(): Flow<List<Team>>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)

    /** Puts [pokemonId] in [slot] (0 to 5), or empties the slot when it's null. */
    suspend fun setMember(teamId: Long, slot: Int, pokemonId: Int?)
}

@Singleton
class RoomTeamRepository @Inject constructor(
    private val dao: TeamDao,
    private val pokemon: PokemonRepository,
) : TeamRepository {

    override fun teams(): Flow<List<Team>> = combine(dao.observeAll(), pokemon.pokedex()) { rows, pokedex ->
        val byId = pokedex.associateBy { it.id }
        rows.map { row ->
            val bySlot = row.members.associate { it.slot to byId[it.pokemonId] }
            Team(row.team.id, row.team.name, List(TEAM_SIZE) { slot -> bySlot[slot] })
        }
    }

    override suspend fun create(name: String): Long =
        dao.insert(TeamEntity(name = name.trim().ifEmpty { "New team" }, createdAt = System.currentTimeMillis()))

    override suspend fun rename(id: Long, name: String) {
        name.trim().takeIf { it.isNotEmpty() }?.let { dao.rename(id, it) }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setMember(teamId: Long, slot: Int, pokemonId: Int?) {
        require(slot in 0 until TEAM_SIZE) { "Slot $slot is outside the team" }
        if (pokemonId == null) dao.removeMember(teamId, slot) else dao.upsertMember(TeamMemberEntity(teamId, slot, pokemonId))
    }
}
