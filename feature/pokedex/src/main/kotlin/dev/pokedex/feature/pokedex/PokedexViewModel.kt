package dev.pokedex.feature.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.Pokemon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PokedexUiState(
    val pokemon: List<Pokemon> = emptyList(),
    val caughtIds: Set<Int> = emptySet(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class PokedexViewModel @Inject constructor(private val repository: PokemonRepository) : ViewModel() {

    private data class Sync(val running: Boolean, val failed: Boolean)

    private val sync = MutableStateFlow(Sync(running = true, failed = false))

    val state: StateFlow<PokedexUiState> = combine(repository.pokedex(), repository.userStates(), sync) { pokemon, users, s ->
        PokedexUiState(
            pokemon = pokemon,
            caughtIds = users.filterValues { it.caught }.keys,
            loading = pokemon.isEmpty() && s.running,
            failed = pokemon.isEmpty() && s.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PokedexUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            sync.value = Sync(running = true, failed = false)
            val result = repository.refreshIndex()
            sync.value = Sync(running = false, failed = result.isFailure)
        }
    }

    /** Called when a card scrolls into view: fetches its types and facts once. */
    fun onShown(id: Int) {
        viewModelScope.launch { repository.ensureDetails(id) }
    }
}
