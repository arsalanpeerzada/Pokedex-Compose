package dev.pokedex.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

data class TodayUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val dateLabel: String = "",
    val scene: WeatherScene = WeatherScene.Clear,
    val isNight: Boolean = false,
    val answer: Pokemon? = null,
    val choices: List<Pokemon> = emptyList(),
    val wrongGuesses: Set<Int> = emptySet(),
    val revealed: Boolean = false,
    val hints: List<String> = emptyList(),
    val hintsLeft: Int = 0,
)

/**
 * Today's Pokémon, the same for everyone on a given date. Until the context engine lands (Sprint 3),
 * the pick is seeded by the date alone and the scene follows the time of day, with no invented weather.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: PokemonRepository) : ViewModel() {

    private data class Session(val wrong: Set<Int> = emptySet(), val revealed: Boolean = false, val hintsUsed: Int = 0)

    private val session = MutableStateFlow(Session())
    private val syncFailed = MutableStateFlow(false)
    private val today = LocalDate.now()
    private val night = LocalTime.now().let { it.hour >= 19 || it.hour < 6 }
    private var detailsRequested = false

    val state: StateFlow<TodayUiState> = combine(
        repository.pokedex(),
        repository.userStates(),
        session,
        syncFailed,
    ) { pokemon, users, s, failed ->
        if (pokemon.isEmpty()) return@combine TodayUiState(loading = !failed, failed = failed)
        val (answer, choices) = dailyPick(pokemon, today)
        requestDetails(choices)
        val caughtToday = users[answer.id]?.caughtAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == today } == true
        val hints = hintsFor(answer).take(s.hintsUsed)
        TodayUiState(
            loading = false,
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)),
            scene = if (night) WeatherScene.Night else WeatherScene.Clear,
            isNight = night,
            answer = answer,
            choices = choices,
            wrongGuesses = s.wrong,
            revealed = s.revealed || caughtToday,
            hints = hints,
            hintsLeft = MAX_HINTS - s.hintsUsed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            syncFailed.value = false
            syncFailed.value = repository.refreshIndex().isFailure
        }
    }

    /** Returns true when the guess is right, so the screen can celebrate or shake. */
    fun guess(pokemon: Pokemon): Boolean {
        val answer = state.value.answer ?: return false
        if (pokemon.id == answer.id) {
            session.update { it.copy(revealed = true) }
            viewModelScope.launch { repository.setCaught(answer.id, caught = true) }
            return true
        }
        session.update { it.copy(wrong = it.wrong + pokemon.id) }
        return false
    }

    fun useHint() {
        session.update { if (it.hintsUsed < MAX_HINTS) it.copy(hintsUsed = it.hintsUsed + 1) else it }
    }

    private fun requestDetails(choices: List<Pokemon>) {
        if (detailsRequested) return
        detailsRequested = true
        choices.forEach { p -> viewModelScope.launch { repository.ensureDetails(p.id) } }
    }

    private fun hintsFor(answer: Pokemon): List<String> = listOf(
        "It first appeared in ${answer.generation.label}.",
        if (answer.types.isEmpty()) "Its type is on its way…" else "It's ${answer.types.joinToString(" and ") { it.displayName }} type.",
    )

    companion object {
        const val MAX_HINTS = 2

        /** Deterministic for a date: one answer and three decoys, shuffled. */
        fun dailyPick(pokemon: List<Pokemon>, date: LocalDate): Pair<Pokemon, List<Pokemon>> {
            val random = Random(date.toEpochDay())
            val picks = pokemon.shuffled(random).take(4)
            return picks.first() to picks.shuffled(random)
        }
    }
}
