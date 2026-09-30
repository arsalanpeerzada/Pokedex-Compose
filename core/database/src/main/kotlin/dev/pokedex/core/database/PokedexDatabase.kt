package dev.pokedex.core.database

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One row per species. The index fills id and name in a single request, and the type index fills
 * types for everyone in 18 more. The rest arrives lazily, the first time a Pokémon is on screen,
 * and is then kept for good (offline-first).
 */
@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** Comma-separated type names in slot order, empty until the type index or details load. */
    val types: String = "",
    val heightDecimetres: Int? = null,
    val weightHectograms: Int? = null,
    val category: String? = null,
    val catchRate: Int? = null,
    val flavourText: String? = null,
    val flavourVersion: String? = null,
    val cryUrl: String? = null,
    val isLegendary: Boolean = false,
    val isMythical: Boolean = false,
    val detailsLoaded: Boolean = false,
    val hp: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val specialAttack: Int? = null,
    val specialDefense: Int? = null,
    val speed: Int? = null,
    val evolutionChainId: Int? = null,
    /** Alternate forms as "id:label" pairs joined by "|"; empty when there are none, null until loaded. */
    val forms: String? = null,
    val habitat: String? = null,
    val firstGame: String? = null,
    /** Story entries as "game\ttext" joined by a unit separator; null until loaded. */
    val storyEntries: String? = null,
)

/** What the user has done: caught, seen, favourite. Kept apart so a data refresh never touches it. */
@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val id: Int,
    val caughtAt: Long? = null,
    val seen: Boolean = false,
    val favourite: Boolean = false,
)

/** A non-neutral damage multiplier: 0, 0.5 or 2. */
@Entity(tableName = "type_efficacy", primaryKeys = ["attacker", "defender"])
data class TypeEfficacyEntity(val attacker: String, val defender: String, val factor: Float)

/** One stage of an evolution chain. Each species belongs to exactly one chain. */
@Entity(tableName = "evolution")
data class EvolutionEntity(
    @PrimaryKey val speciesId: Int,
    val chainId: Int,
    val fromId: Int?,
    val method: String?,
    /** Position in the chain, so stages list in order. */
    val depth: Int,
)

data class EvolutionRow(val speciesId: Int, val fromId: Int?, val method: String?, val name: String)

@Entity(tableName = "daily_result")
data class DailyResultEntity(
    @PrimaryKey val epochDay: Long,
    val pokemonId: Int,
    val solved: Boolean,
    /** Comma-separated species ids. */
    val wrongGuesses: String,
    val hintsUsed: Int,
    val reason: String? = null,
    val hint: String? = null,
)

@Entity(tableName = "team")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** One filled slot (0 to 5) of a team. Empty slots have no row. */
@Entity(tableName = "team_member", primaryKeys = ["teamId", "slot"])
data class TeamMemberEntity(val teamId: Long, val slot: Int, val pokemonId: Int)

data class TeamWithMembers(
    @Embedded val team: TeamEntity,
    @Relation(parentColumn = "id", entityColumn = "teamId") val members: List<TeamMemberEntity>,
)

@Dao
interface PokemonDao {
    @Query("SELECT * FROM pokemon ORDER BY id")
    fun observeAll(): Flow<List<PokemonEntity>>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    fun observe(id: Int): Flow<PokemonEntity?>

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    /** Null if the species isn't in the index yet. Rows cached before stats existed count as needing details. */
    @Query("SELECT (detailsLoaded = 0 OR hp IS NULL OR forms IS NULL OR storyEntries IS NULL) FROM pokemon WHERE id = :id")
    suspend fun needsDetails(id: Int): Boolean?

    @Query("SELECT COUNT(*) FROM pokemon WHERE types = ''")
    suspend fun countWithoutTypes(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndex(items: List<PokemonEntity>)

    @Update
    suspend fun update(item: PokemonEntity)

    @Query("UPDATE pokemon SET types = :types WHERE id = :id AND detailsLoaded = 0")
    suspend fun setTypes(id: Int, types: String)

    @Transaction
    suspend fun setTypes(typesById: Map<Int, String>) {
        typesById.forEach { (id, types) -> setTypes(id, types) }
    }
}

@Dao
interface UserStateDao {
    @Query("SELECT * FROM user_state")
    fun observeAll(): Flow<List<UserStateEntity>>

    @Query("SELECT * FROM user_state WHERE id = :id")
    suspend fun get(id: Int): UserStateEntity?

    @Upsert
    suspend fun upsert(state: UserStateEntity)
}

@Dao
interface TypeDao {
    @Query("SELECT * FROM type_efficacy")
    fun observeAll(): Flow<List<TypeEfficacyEntity>>

    @Query("SELECT COUNT(*) FROM type_efficacy")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(rows: List<TypeEfficacyEntity>)
}

@Dao
interface EvolutionDao {
    @Query(
        "SELECT e.speciesId, e.fromId, e.method, p.name FROM evolution e " +
            "JOIN pokemon p ON p.id = e.speciesId WHERE e.chainId = :chainId ORDER BY e.depth, e.speciesId",
    )
    fun observeChain(chainId: Int): Flow<List<EvolutionRow>>

    @Query("SELECT COUNT(*) FROM evolution WHERE chainId = :chainId")
    suspend fun count(chainId: Int): Int

    @Upsert
    suspend fun upsert(rows: List<EvolutionEntity>)
}

@Dao
interface DailyResultDao {
    @Query("SELECT * FROM daily_result WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<DailyResultEntity?>

    @Query("SELECT epochDay FROM daily_result WHERE solved = 1 ORDER BY epochDay DESC")
    fun observeSolvedDays(): Flow<List<Long>>

    @Query("SELECT * FROM daily_result WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): DailyResultEntity?

    @Query("SELECT pokemonId FROM daily_result WHERE epochDay >= :sinceEpochDay AND epochDay < :beforeEpochDay")
    suspend fun pokemonIdsBetween(sinceEpochDay: Long, beforeEpochDay: Long): List<Int>

    @Upsert
    suspend fun upsert(result: DailyResultEntity)
}

@Dao
interface TeamDao {
    @Transaction
    @Query("SELECT * FROM team ORDER BY createdAt")
    fun observeAll(): Flow<List<TeamWithMembers>>

    @Insert
    suspend fun insert(team: TeamEntity): Long

    @Query("UPDATE team SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM team WHERE id = :id")
    suspend fun deleteTeam(id: Long)

    @Query("DELETE FROM team_member WHERE teamId = :id")
    suspend fun deleteMembers(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        deleteMembers(id)
        deleteTeam(id)
    }

    @Upsert
    suspend fun upsertMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_member WHERE teamId = :teamId AND slot = :slot")
    suspend fun removeMember(teamId: Long, slot: Int)
}

@Database(
    entities = [
        PokemonEntity::class,
        UserStateEntity::class,
        TypeEfficacyEntity::class,
        EvolutionEntity::class,
        DailyResultEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class PokedexDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun userStateDao(): UserStateDao
    abstract fun typeDao(): TypeDao
    abstract fun evolutionDao(): EvolutionDao
    abstract fun dailyResultDao(): DailyResultDao
    abstract fun teamDao(): TeamDao
}
