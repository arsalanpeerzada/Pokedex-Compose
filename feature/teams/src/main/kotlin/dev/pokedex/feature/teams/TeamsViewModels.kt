package dev.pokedex.feature.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.TeamRepository
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.Team
import dev.pokedex.core.model.TeamAnalysis
import dev.pokedex.core.model.TypeChart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamsUiState(val loading: Boolean = true, val teams: List<Team> = emptyList(), val chart: TypeChart = TypeChart.Empty)

@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teams: TeamRepository,
    pokemon: PokemonRepository,
) : ViewModel() {

    val state: StateFlow<TeamsUiState> = combine(teams.teams(), pokemon.typeChart()) { list, chart ->
        TeamsUiState(loading = false, teams = list, chart = chart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamsUiState())

    /** Creates a team, then hands its id to [onCreated] so the screen can open it. */
    fun create(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val name = "Team ${state.value.teams.size + 1}"
            onCreated(teams.create(name))
        }
    }
}

data class TeamEditorUiState(
    val team: Team? = null,
    val missing: Boolean = false,
    val analysis: TeamAnalysis? = null,
    val pokedex: List<Pokemon> = emptyList(),
)

@HiltViewModel(assistedFactory = TeamEditorViewModel.Factory::class)
class TeamEditorViewModel @AssistedInject constructor(
    @Assisted private val teamId: Long,
    private val teams: TeamRepository,
    pokemon: PokemonRepository,
) : ViewModel() {

    private var loaded = false

    val state: StateFlow<TeamEditorUiState> = combine(teams.teams(), pokemon.typeChart(), pokemon.pokedex()) { list, chart, pokedex ->
        val team = list.firstOrNull { it.id == teamId }
        if (team != null) loaded = true
        TeamEditorUiState(
            team = team,
            // Only "missing" once it has existed, so the screen can close after a delete.
            missing = team == null && loaded,
            analysis = team?.let { if (chart.isEmpty) null else TeamAnalysis.of(it.filled, chart) },
            pokedex = pokedex,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamEditorUiState())

    fun setMember(slot: Int, pokemonId: Int?) {
        viewModelScope.launch { teams.setMember(teamId, slot, pokemonId) }
    }

    fun rename(name: String) {
        viewModelScope.launch { teams.rename(teamId, name) }
    }

    fun delete() {
        viewModelScope.launch { teams.delete(teamId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(teamId: Long): TeamEditorViewModel
    }
}
