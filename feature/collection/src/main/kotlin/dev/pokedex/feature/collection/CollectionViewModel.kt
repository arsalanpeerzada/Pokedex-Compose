package dev.pokedex.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.CollectionEntry
import dev.pokedex.core.model.CollectionState
import dev.pokedex.core.model.CollectionSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val summary: CollectionSummary = CollectionSummary(caught = 0, total = 0, streakDays = 0, latestCaught = emptyList()),
    val entries: List<CollectionEntry> = emptyList(),
)

@HiltViewModel
class CollectionViewModel @Inject constructor(private val repository: PokemonRepository) : ViewModel() {

    val state: StateFlow<CollectionUiState> = combine(repository.pokedex(), repository.userStates()) { pokemon, users ->
        val entries = pokemon.map { p ->
            val user = users[p.id]
            val collectionState = when {
                user?.caught == true -> CollectionState.Caught
                user?.seen == true -> CollectionState.Seen
                else -> CollectionState.Unseen
            }
            CollectionEntry(p, collectionState, favourite = user?.favourite == true)
        }
        val byId = pokemon.associateBy { it.id }
        CollectionUiState(
            summary = CollectionSummary(
                caught = users.values.count { it.caught },
                total = pokemon.size,
                streakDays = 0,
                latestCaught = users.filterValues { it.caught }.entries
                    .sortedByDescending { it.value.caughtAt }
                    .take(3)
                    .mapNotNull { byId[it.key] },
            ),
            entries = entries,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    init {
        viewModelScope.launch { repository.refreshIndex() }
    }

    fun onShown(id: Int) {
        viewModelScope.launch { repository.ensureDetails(id) }
    }
}
