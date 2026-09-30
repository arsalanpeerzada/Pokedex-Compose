package dev.pokedex.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.OfflineFirstPokemonRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.RoomDailyRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun pokemonRepository(impl: OfflineFirstPokemonRepository): PokemonRepository

    @Binds
    abstract fun dailyRepository(impl: RoomDailyRepository): DailyRepository
}
