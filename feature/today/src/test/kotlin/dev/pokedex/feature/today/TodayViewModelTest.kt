package dev.pokedex.feature.today

import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.domain.TodayPick
import dev.pokedex.core.domain.TodayPicker
import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.ThemeMode
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.UserPreferences
import dev.pokedex.core.model.UserState
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val pokemon = (1..40).map { Pokemon(it, "Pokemon $it", emptyList(), hasDetails = false) }
    private val repository = FakePokemonRepository(pokemon)
    private val daily = FakeDailyRepository()
    private val preferences = FakePreferences()
    private val day = LocalDate.now().toEpochDay()

    /** Stands in for the context engine: always the same answer, like a saved pick. */
    private val picker = TodayPicker {
        TodayPick(
            epochDay = day,
            answer = pokemon[6],
            choices = listOf(pokemon[2], pokemon[6], pokemon[11], pokemon[20]),
            reason = "Rain in Leeds today: Water weather.",
            hint = "Rainy in Leeds today.",
            weather = null,
            city = "Leeds",
            scene = WeatherScene.Rain,
            isNight = false,
        )
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel(): TodayViewModel {
        val vm = TodayViewModel(repository, daily, preferences, picker)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `shows the pick with its weather hint and scene`() = runTest(dispatcher) {
        val state = newViewModel().state.value
        assertEquals(pokemon[6], state.answer)
        assertEquals("Rainy in Leeds today.", state.weatherHint)
        assertEquals(WeatherScene.Rain, state.scene)
        assertFalse(state.revealed)
    }

    @Test
    fun `progress survives a new ViewModel and a right guess catches it`() = runTest(dispatcher) {
        val vm = newViewModel()
        val answer = vm.state.value.answer!!
        val wrong = vm.state.value.choices.first { it.id != answer.id }

        vm.guess(wrong)
        vm.useHint()
        advanceUntilIdle()
        assertEquals(setOf(wrong.id), vm.state.value.wrongGuesses)
        assertEquals(1, vm.state.value.hints.size)

        val reopened = newViewModel()
        assertEquals(setOf(wrong.id), reopened.state.value.wrongGuesses)
        assertEquals(1, reopened.state.value.hintsLeft)

        reopened.guess(answer)
        advanceUntilIdle()
        assertTrue(reopened.state.value.revealed)
        assertEquals(1, reopened.state.value.streakDays)
        assertEquals("Rain in Leeds today: Water weather.", reopened.state.value.reason)
        assertEquals(listOf(answer.id), repository.caught)
    }

    @Test
    fun `the reminder is offered once, after the reveal`() = runTest(dispatcher) {
        val vm = newViewModel()
        assertFalse(vm.state.value.offerReminder)
        vm.guess(vm.state.value.answer!!)
        advanceUntilIdle()
        assertTrue(vm.state.value.offerReminder)
        vm.answerReminder(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.offerReminder)
    }

    @Test
    fun `hints stop at the limit`() = runTest(dispatcher) {
        val vm = newViewModel()
        repeat(4) { vm.useHint() }
        advanceUntilIdle()
        assertEquals(0, vm.state.value.hintsLeft)
        assertEquals(TodayViewModel.MAX_HINTS, vm.state.value.hints.size)
    }
}

private class FakePokemonRepository(pokemon: List<Pokemon>) : PokemonRepository {
    private val all = MutableStateFlow(pokemon)
    val caught = mutableListOf<Int>()
    override fun pokedex(): Flow<List<Pokemon>> = all
    override fun pokemon(id: Int): Flow<Pokemon?> = all.map { list -> list.firstOrNull { it.id == id } }
    override fun userStates(): Flow<Map<Int, UserState>> = flowOf(emptyMap())
    override fun typeChart(): Flow<TypeChart> = flowOf(TypeChart.Empty)
    override fun evolution(chainId: Int): Flow<List<EvolutionStep>> = flowOf(emptyList())
    override suspend fun refreshIndex(): Result<Unit> = Result.success(Unit)
    override suspend fun ensureDetails(id: Int) = Unit
    override suspend fun ensureEvolution(chainId: Int) = Unit
    override suspend fun setCaught(id: Int, caught: Boolean) {
        if (caught) this.caught += id
    }
    override suspend fun toggleFavourite(id: Int) = Unit
    override suspend fun markSeen(id: Int) = Unit
}

private class FakeDailyRepository : DailyRepository {
    private val results = MutableStateFlow<Map<Long, DailyResult>>(emptyMap())
    override fun result(epochDay: Long): Flow<DailyResult?> = results.map { it[epochDay] }
    override suspend fun get(epochDay: Long): DailyResult? = results.value[epochDay]
    override suspend fun save(result: DailyResult) {
        results.value = results.value + (result.epochDay to result)
    }
    override suspend fun recentPokemonIds(epochDay: Long, days: Int): Set<Int> = emptySet()
    override fun streak(todayEpochDay: Long): Flow<Int> = results.map { map ->
        var day = todayEpochDay
        var streak = 0
        while (map[day]?.solved == true) { streak++; day-- }
        streak
    }
}

private class FakePreferences : UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = state
    override suspend fun setTheme(theme: ThemeMode) { state.value = state.value.copy(theme = theme) }
    override suspend fun setUsageStats(enabled: Boolean) { state.value = state.value.copy(usageStats = enabled) }
    override suspend fun setCrashReports(enabled: Boolean) { state.value = state.value.copy(crashReports = enabled) }
    override suspend fun setCity(city: String?) { state.value = state.value.copy(city = city) }
    override suspend fun completeOnboarding() { state.value = state.value.copy(onboardingDone = true) }
    override suspend fun setReminder(enabled: Boolean) { state.value = state.value.copy(reminder = enabled, reminderAsked = true) }
    override suspend fun setCityLocation(city: String, latitude: Double, longitude: Double) {
        state.value = state.value.copy(cityLatitude = latitude, cityLongitude = longitude)
    }
}
