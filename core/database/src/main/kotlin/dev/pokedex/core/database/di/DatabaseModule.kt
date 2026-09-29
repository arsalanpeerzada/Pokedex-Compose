package dev.pokedex.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.database.PokedexDatabase
import dev.pokedex.core.database.PokemonDao
import dev.pokedex.core.database.UserStateDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PokedexDatabase =
        Room.databaseBuilder(context, PokedexDatabase::class.java, "pokedex.db").build()

    @Provides
    fun pokemonDao(db: PokedexDatabase): PokemonDao = db.pokemonDao()

    @Provides
    fun userStateDao(db: PokedexDatabase): UserStateDao = db.userStateDao()
}
