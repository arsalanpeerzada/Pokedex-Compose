package dev.pokedex.feature.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.DownloadProgress
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.Generation
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PokedexSort(val label: String) {
    Number("Number"),
    NameAscending("Name, A to Z"),
    NameDescending("Name, Z to A"),
}

data class PokedexFilters(
    val types: Set<PokemonType> = emptySet(),
    val generations: Set<Generation> = emptySet(),
    val caughtOnly: Boolean = false,
    val favouritesOnly: Boolean = false,
    val legendaryOnly: Boolean = false,
    val mythicalOnly: Boolean = false,
    val sort: PokedexSort = PokedexSort.Number,
) {
    /** Filters set in the sheet; the quick toggles show their own state. */
    val sheetCount: Int get() = types.size + generations.size + (if (legendaryOnly) 1 else 0) + (if (mythicalOnly) 1 else 0)
    val isDefault: Boolean get() = this == PokedexFilters(sort = sort)
}

data class PokedexUiState(
    val pokemon: List<Pokemon> = emptyList(),
    val totalCount: Int = 0,
    val caughtIds: Set<Int> = emptySet(),
    val filters: PokedexFilters = PokedexFilters(),
    val query: String = "",
    val loading: Boolean = true,
    val failed: Boolean = false,
    /** Legendary and Mythical come with each Pokémon's details, so they only cover what's downloaded. */
    val download: DownloadProgress = DownloadProgress(0, 0),
)

@HiltViewModel
class PokedexViewModel @Inject constructor(private val repository: PokemonRepository) : ViewModel() {

    private data class Sync(val running: Boolean, val failed: Boolean)

    private val sync = MutableStateFlow(Sync(running = true, failed = false))
    private val filters = MutableStateFlow(PokedexFilters())
    private val query = MutableStateFlow("")

    val state: StateFlow<PokedexUiState> = combine(
        repository.pokedex(),
        repository.userStates(),
        combine(sync, repository.downloadProgress(), ::Pair),
        filters,
        query,
    ) { pokemon, users, (s, download), f, q ->
        PokedexUiState(
            pokemon = applyFilters(pokemon, users, f, q),
            totalCount = pokemon.size,
            caughtIds = users.filterValues { it.caught }.keys,
            filters = f,
            query = q,
            loading = pokemon.isEmpty() && s.running,
            failed = pokemon.isEmpty() && s.failed,
            download = download,
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

    fun onQueryChange(value: String) { query.value = value }

    fun toggleType(type: PokemonType) = filters.update { it.copy(types = it.types.toggle(type)) }
    fun toggleGeneration(generation: Generation) = filters.update { it.copy(generations = it.generations.toggle(generation)) }
    fun toggleCaughtOnly() = filters.update { it.copy(caughtOnly = !it.caughtOnly) }
    fun toggleFavouritesOnly() = filters.update { it.copy(favouritesOnly = !it.favouritesOnly) }
    fun toggleLegendaryOnly() = filters.update { it.copy(legendaryOnly = !it.legendaryOnly) }
    fun toggleMythicalOnly() = filters.update { it.copy(mythicalOnly = !it.mythicalOnly) }
    fun setSort(sort: PokedexSort) = filters.update { it.copy(sort = sort) }
    fun clearFilters() = filters.update { PokedexFilters(sort = it.sort) }

    /** Called when a card scrolls into view: fetches its facts once. */
    fun onShown(id: Int) {
        viewModelScope.launch { repository.ensureDetails(id) }
    }

    companion object {
        /** Pure, so the rules are easy to test: any selected type, any selected generation, then search and sort. */
        fun applyFilters(pokemon: List<Pokemon>, users: Map<Int, UserState>, f: PokedexFilters, query: String): List<Pokemon> {
            val q = query.trim()
            val filtered = pokemon.filter { p ->
                (f.types.isEmpty() || p.types.any { it in f.types }) &&
                    (f.generations.isEmpty() || p.generation in f.generations) &&
                    (!f.caughtOnly || users[p.id]?.caught == true) &&
                    (!f.favouritesOnly || users[p.id]?.favourite == true) &&
                    // With both on, either counts: "legendary or mythical".
                    (!(f.legendaryOnly || f.mythicalOnly) || (f.legendaryOnly && p.isLegendary) || (f.mythicalOnly && p.isMythical)) &&
                    (q.isEmpty() || p.name.contains(q, ignoreCase = true) || p.number.contains(q) || p.id.toString() == q)
            }
            return when (f.sort) {
                PokedexSort.Number -> filtered
                PokedexSort.NameAscending -> filtered.sortedBy { it.name.lowercase() }
                PokedexSort.NameDescending -> filtered.sortedByDescending { it.name.lowercase() }
            }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
