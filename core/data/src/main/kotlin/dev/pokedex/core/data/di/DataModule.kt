package dev.pokedex.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.DataStoreUserPreferencesRepository
import dev.pokedex.core.data.OfflineFirstPokemonRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.RoomDailyRepository
import dev.pokedex.core.data.RoomTeamRepository
import dev.pokedex.core.data.TeamRepository
import dev.pokedex.core.data.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun pokemonRepository(impl: OfflineFirstPokemonRepository): PokemonRepository

    @Binds
    abstract fun dailyRepository(impl: RoomDailyRepository): DailyRepository

    @Binds
    abstract fun teamRepository(impl: RoomTeamRepository): TeamRepository

    @Binds
    abstract fun userPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun preferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    }
}
