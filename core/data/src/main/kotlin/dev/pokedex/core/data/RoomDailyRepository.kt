package dev.pokedex.core.data

import dev.pokedex.core.database.DailyResultDao
import dev.pokedex.core.database.DailyResultEntity
import dev.pokedex.core.model.DailyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDailyRepository @Inject constructor(private val dao: DailyResultDao) : DailyRepository {

    override fun result(epochDay: Long): Flow<DailyResult?> = dao.observe(epochDay).map { it?.toModel() }

    override suspend fun get(epochDay: Long): DailyResult? = dao.get(epochDay)?.toModel()

    override suspend fun save(result: DailyResult) {
        dao.upsert(
            DailyResultEntity(
                epochDay = result.epochDay,
                pokemonId = result.pokemonId,
                solved = result.solved,
                wrongGuesses = result.wrongGuesses.joinToString(","),
                hintsUsed = result.hintsUsed,
                reason = result.reason,
                hint = result.hint,
            ),
        )
    }

    override suspend fun recentPokemonIds(epochDay: Long, days: Int): Set<Int> =
        dao.pokemonIdsBetween(epochDay - days, epochDay).toSet()

    override fun streak(todayEpochDay: Long): Flow<Int> = dao.observeSolvedDays().map { streakFrom(it, todayEpochDay) }
}

private fun DailyResultEntity.toModel() = DailyResult(
    epochDay = epochDay,
    pokemonId = pokemonId,
    solved = solved,
    wrongGuesses = wrongGuesses.split(',').mapNotNull(String::toIntOrNull).toSet(),
    hintsUsed = hintsUsed,
    reason = reason,
    hint = hint,
)
