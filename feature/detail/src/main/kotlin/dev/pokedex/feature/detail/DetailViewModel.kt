package dev.pokedex.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.TeamRepository
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.model.Units
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.Team
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val pokemon: Pokemon? = null,
    val user: UserState = UserState(),
    val chart: TypeChart = TypeChart.Empty,
    val evolution: List<EvolutionStep> = emptyList(),
    val teams: List<Team> = emptyList(),
    val units: Units = Units.Metric,
    /** A short confirmation after adding to a team, shown once. */
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val pokemonId: Int,
    private val repository: PokemonRepository,
    private val teams: TeamRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    private val pokemon = repository.pokemon(pokemonId)
    private val chainId = pokemon.map { it?.evolutionChainId }.distinctUntilChanged()
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<DetailUiState> = combine(
        combine(pokemon, repository.userStates().map { it[pokemonId] ?: UserState() }, preferences.preferences.map { it.units }, ::Triple),
        repository.typeChart(),
        chainId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.evolution(id) },
        teams.teams(),
        message,
    ) { (p, user, units), chart, evolution, teamList, msg -> DetailUiState(p, user, chart, evolution, teamList, units, msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch {
            repository.markSeen(pokemonId)
            repository.ensureDetails(pokemonId)
        }
        // The chain id arrives with the details; fetch the chain the first time it's known.
        chainId.mapNotNull { it }.onEach { repository.ensureEvolution(it) }.launchIn(viewModelScope)
    }

    fun toggleCaught() {
        viewModelScope.launch { repository.setCaught(pokemonId, !state.value.user.caught) }
    }

    fun toggleFavourite() {
        viewModelScope.launch { repository.toggleFavourite(pokemonId) }
    }

    /** Adds this Pokémon to the team's first empty slot. */
    fun addToTeam(team: Team) {
        val slot = team.members.indexOfFirst { it == null }
        val name = state.value.pokemon?.name ?: return
        if (slot < 0) return
        viewModelScope.launch {
            teams.setMember(team.id, slot, pokemonId)
            message.value = "$name added to ${team.name}"
        }
    }

    fun addToNewTeam() {
        val name = state.value.pokemon?.name ?: return
        viewModelScope.launch {
            val teamName = "Team ${state.value.teams.size + 1}"
            val id = teams.create(teamName)
            teams.setMember(id, 0, pokemonId)
            message.value = "$name added to $teamName"
        }
    }

    fun messageShown() {
        message.value = null
    }

    @AssistedFactory
    interface Factory {
        fun create(pokemonId: Int): DetailViewModel
    }
}
