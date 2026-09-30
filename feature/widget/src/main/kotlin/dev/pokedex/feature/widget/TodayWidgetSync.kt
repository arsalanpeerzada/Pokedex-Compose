package dev.pokedex.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes the widget straight away when something it shows changes: the Pokédex finishing its
 * first download, or today's guess being solved. The hourly refresh covers the change of day.
 */
@Singleton
class TodayWidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pokemon: PokemonRepository,
    private val daily: DailyRepository,
) {
    fun start(scope: CoroutineScope) {
        val today = LocalDate.now().toEpochDay()
        combine(
            pokemon.pokedex().map { it.isNotEmpty() }.distinctUntilChanged(),
            daily.result(today).map { it?.solved == true }.distinctUntilChanged(),
        ) { hasData, solved -> hasData to solved }
            .distinctUntilChanged()
            .drop(1) // The widget drew itself with the current values already.
            .onEach { TodayWidget().updateAll(context) }
            .launchIn(scope)
    }
}
