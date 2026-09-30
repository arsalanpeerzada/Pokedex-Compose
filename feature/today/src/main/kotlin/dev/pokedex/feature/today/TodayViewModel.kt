package dev.pokedex.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.DailyPicker
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

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
    val streakDays: Int = 0,
)

/**
 * Today's Pokémon, the same for everyone on a given date. Progress is saved per date, so leaving
 * the app never resets a guess. Until the context engine lands (Sprint 3), the pick is seeded by
 * the date alone and the scene follows the time of day, with no invented weather.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: PokemonRepository,
    private val daily: DailyRepository,
) : ViewModel() {

    private val syncFailed = MutableStateFlow(false)
    private val today = LocalDate.now()
    private val day = today.toEpochDay()
    private val night = LocalTime.now().let { it.hour >= 19 || it.hour < 6 }
    private val writeLock = Mutex()
    private var detailsRequested = false

    val state: StateFlow<TodayUiState> = combine(
        repository.pokedex(),
        daily.result(day),
        daily.streak(day),
        syncFailed,
    ) { pokemon, result, streak, failed ->
        if (pokemon.isEmpty()) return@combine TodayUiState(loading = !failed, failed = failed)
        val (answer, choices) = dailyPick(pokemon, today)
        requestDetails(choices)
        val hintsUsed = result?.hintsUsed ?: 0
        TodayUiState(
            loading = false,
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)),
            scene = if (night) WeatherScene.Night else WeatherScene.Clear,
            isNight = night,
            answer = answer,
            choices = choices,
            wrongGuesses = result?.wrongGuesses.orEmpty(),
            revealed = result?.solved == true,
            hints = hintsFor(answer).take(hintsUsed),
            hintsLeft = MAX_HINTS - hintsUsed,
            streakDays = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            syncFailed.value = false
            syncFailed.value = repository.refreshIndex().isFailure
        }
    }

    fun guess(pokemon: Pokemon) {
        val answer = state.value.answer ?: return
        update(answer) { current ->
            if (current.solved) return@update current
            if (pokemon.id == answer.id) current.copy(solved = true) else current.copy(wrongGuesses = current.wrongGuesses + pokemon.id)
        }
        if (pokemon.id == answer.id) viewModelScope.launch { repository.setCaught(answer.id, caught = true) }
    }

    fun useHint() {
        val answer = state.value.answer ?: return
        update(answer) { if (it.hintsUsed < MAX_HINTS) it.copy(hintsUsed = it.hintsUsed + 1) else it }
    }

    /** Reads the saved result, applies [change] and writes it back, one change at a time. */
    private fun update(answer: Pokemon, change: (DailyResult) -> DailyResult) {
        viewModelScope.launch {
            writeLock.withLock {
                val current = daily.result(day).first() ?: DailyResult(day, answer.id, solved = false, wrongGuesses = emptySet(), hintsUsed = 0)
                val next = change(current)
                if (next != current) daily.save(next)
            }
        }
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

        /** Deterministic for a date: one answer and three decoys, shuffled. The widget uses the same rule. */
        fun dailyPick(pokemon: List<Pokemon>, date: LocalDate): Pair<Pokemon, List<Pokemon>> = DailyPicker.pick(pokemon, date)
    }
}
