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

    override fun result(epochDay: Long): Flow<DailyResult?> = dao.observe(epochDay).map { row ->
        row?.let {
            DailyResult(
                epochDay = it.epochDay,
                pokemonId = it.pokemonId,
                solved = it.solved,
                wrongGuesses = it.wrongGuesses.split(',').mapNotNull(String::toIntOrNull).toSet(),
                hintsUsed = it.hintsUsed,
            )
        }
    }

    override suspend fun save(result: DailyResult) {
        dao.upsert(
            DailyResultEntity(
                epochDay = result.epochDay,
                pokemonId = result.pokemonId,
                solved = result.solved,
                wrongGuesses = result.wrongGuesses.joinToString(","),
                hintsUsed = result.hintsUsed,
            ),
        )
    }

    override fun streak(todayEpochDay: Long): Flow<Int> = dao.observeSolvedDays().map { streakFrom(it, todayEpochDay) }
}
