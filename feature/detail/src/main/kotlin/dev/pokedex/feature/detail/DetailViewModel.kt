package dev.pokedex.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.UserState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val pokemon: Pokemon? = null,
    val user: UserState = UserState(),
)

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val pokemonId: Int,
    private val repository: PokemonRepository,
) : ViewModel() {

    val state: StateFlow<DetailUiState> = combine(
        repository.pokemon(pokemonId),
        repository.userStates().map { it[pokemonId] ?: UserState() },
    ) { pokemon, user -> DetailUiState(pokemon, user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch {
            repository.markSeen(pokemonId)
            repository.ensureDetails(pokemonId)
        }
    }

    fun toggleCaught() {
        viewModelScope.launch { repository.setCaught(pokemonId, !state.value.user.caught) }
    }

    fun toggleFavourite() {
        viewModelScope.launch { repository.toggleFavourite(pokemonId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(pokemonId: Int): DetailViewModel
    }
}
