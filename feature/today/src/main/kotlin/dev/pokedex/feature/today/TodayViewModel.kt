package dev.pokedex.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.domain.TodayPick
import dev.pokedex.core.domain.TodayPicker
import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class TodayUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val dateLabel: String = "",
    val scene: WeatherScene = WeatherScene.Clear,
    val isNight: Boolean = false,
    val city: String? = null,
    /** For example "Light rain · 14°C". Null without weather. */
    val weatherLine: String? = null,
    val weatherProvider: WeatherProvider? = null,
    val answer: Pokemon? = null,
    val choices: List<Pokemon> = emptyList(),
    /** Weather only; never names a type (plan, section 3). */
    val weatherHint: String? = null,
    val wrongGuesses: Set<Int> = emptySet(),
    val revealed: Boolean = false,
    val hints: List<String> = emptyList(),
    val hintsLeft: Int = 0,
    val streakDays: Int = 0,
    /** "Why today?", shown after the reveal. */
    val reason: String? = null,
    /** Offer the daily reminder once, after the first reveal. */
    val offerReminder: Boolean = false,
)

/**
 * Today's Pokémon, chosen by the context engine from the weather, time, season and special days.
 * The pick is saved for the day, and so is the user's progress, so leaving the app never resets anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: PokemonRepository,
    private val daily: DailyRepository,
    private val preferences: UserPreferencesRepository,
    private val picker: TodayPicker,
) : ViewModel() {

    private val today = LocalDate.now()
    private val day = today.toEpochDay()
    private val pick = MutableStateFlow<TodayPick?>(null)
    private val syncFailed = MutableStateFlow(false)
    private val writeLock = Mutex()
    private var detailsRequested = false

    private val answer = pick.map { it?.answer?.id }.distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.pokemon(id) }

    val state: StateFlow<TodayUiState> = combine(
        pick,
        answer,
        daily.result(day),
        daily.streak(day),
        combine(preferences.preferences, syncFailed, ::Pair),
    ) { p, liveAnswer, result, streak, (prefs, failed) ->
        if (p == null) return@combine TodayUiState(loading = !failed, failed = failed, dateLabel = dateLabel())
        val answer = liveAnswer ?: p.answer
        val hintsUsed = result?.hintsUsed ?: 0
        val revealed = result?.solved == true
        TodayUiState(
            loading = false,
            dateLabel = dateLabel(),
            scene = p.scene,
            isNight = p.isNight,
            city = p.city,
            weatherLine = p.weather?.let { "${it.description} · ${it.temperatureCelsius.roundToInt()}°C" },
            weatherProvider = p.weather?.provider,
            answer = answer,
            choices = p.choices.map { if (it.id == answer.id) answer else it },
            weatherHint = p.hint,
            wrongGuesses = result?.wrongGuesses.orEmpty(),
            revealed = revealed,
            hints = hintsFor(answer).take(hintsUsed),
            hintsLeft = MAX_HINTS - hintsUsed,
            streakDays = streak,
            reason = result?.reason ?: p.reason,
            offerReminder = revealed && !prefs.reminderAsked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        refresh()
        // If the Pokédex finishes downloading while Today is open, make the pick then.
        repository.pokedex().map { it.isNotEmpty() }.distinctUntilChanged().filter { it }
            .onEach { if (pick.value == null) loadPick() }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            syncFailed.value = false
            syncFailed.value = repository.refreshIndex().isFailure
            loadPick()
        }
    }

    private suspend fun loadPick() {
        val loaded = picker() ?: return
        pick.value = loaded
        if (!detailsRequested) {
            detailsRequested = true
            loaded.choices.forEach { p -> viewModelScope.launch { repository.ensureDetails(p.id) } }
        }
    }

    fun guess(pokemon: Pokemon) {
        val answer = pick.value?.answer ?: return
        update { current ->
            if (current.solved) return@update current
            if (pokemon.id == answer.id) current.copy(solved = true) else current.copy(wrongGuesses = current.wrongGuesses + pokemon.id)
        }
        if (pokemon.id == answer.id) viewModelScope.launch { repository.setCaught(answer.id, caught = true) }
    }

    fun useHint() = update { if (it.hintsUsed < MAX_HINTS) it.copy(hintsUsed = it.hintsUsed + 1) else it }

    /** Either answer counts as asked, so the offer appears once. */
    fun answerReminder(enabled: Boolean) {
        viewModelScope.launch { preferences.setReminder(enabled) }
    }

    /** Reads the saved result, applies [change] and writes it back, one change at a time. */
    private fun update(change: (DailyResult) -> DailyResult) {
        val p = pick.value ?: return
        viewModelScope.launch {
            writeLock.withLock {
                val current = daily.result(day).first()
                    ?: DailyResult(day, p.answer.id, solved = false, wrongGuesses = emptySet(), hintsUsed = 0, reason = p.reason, hint = p.hint)
                val next = change(current)
                if (next != current) daily.save(next)
            }
        }
    }

    private fun dateLabel() = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK))

    private fun hintsFor(answer: Pokemon): List<String> = listOf(
        "It first appeared in ${answer.generation.label}.",
        if (answer.types.isEmpty()) "Its type is on its way…" else "It's ${answer.types.joinToString(" and ") { it.displayName }} type.",
    )

    companion object {
        const val MAX_HINTS = 2
    }
}
