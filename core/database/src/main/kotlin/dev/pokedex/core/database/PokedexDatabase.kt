package dev.pokedex.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One row per species. The index fills id and name in a single request; the rest arrives
 * lazily, the first time a Pokémon is on screen, and is then kept for good (offline-first).
 */
@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** Comma-separated type names in slot order, empty until details load. */
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
)

/** What the user has done: caught, seen, favourite. Kept apart so a data refresh never touches it. */
@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val id: Int,
    val caughtAt: Long? = null,
    val seen: Boolean = false,
    val favourite: Boolean = false,
)

@Dao
interface PokemonDao {
    @Query("SELECT * FROM pokemon ORDER BY id")
    fun observeAll(): Flow<List<PokemonEntity>>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    fun observe(id: Int): Flow<PokemonEntity?>

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    @Query("SELECT detailsLoaded FROM pokemon WHERE id = :id")
    suspend fun detailsLoaded(id: Int): Boolean?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndex(items: List<PokemonEntity>)

    @Update
    suspend fun update(item: PokemonEntity)
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

@Database(entities = [PokemonEntity::class, UserStateEntity::class], version = 1, exportSchema = true)
abstract class PokedexDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun userStateDao(): UserStateDao
}
