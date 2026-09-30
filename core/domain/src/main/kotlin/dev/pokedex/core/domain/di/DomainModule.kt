package dev.pokedex.core.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.domain.TodayPickUseCase
import dev.pokedex.core.domain.TodayPicker

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    abstract fun todayPicker(impl: TodayPickUseCase): TodayPicker
}
