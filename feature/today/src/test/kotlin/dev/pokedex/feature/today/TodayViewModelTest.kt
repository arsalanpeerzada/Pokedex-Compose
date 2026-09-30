package dev.pokedex.feature.today

import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.model.DailyResult
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.UserState
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

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel(): TodayViewModel {
        val vm = TodayViewModel(repository, daily)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `the pick is the same for a date and has four distinct choices`() {
        val date = LocalDate.of(2026, 9, 30)
        val first = TodayViewModel.dailyPick(pokemon, date)
        assertEquals(first, TodayViewModel.dailyPick(pokemon, date))
        assertEquals(4, first.second.map { it.id }.toSet().size)
        assertTrue(first.first in first.second)
    }

    @Test
    fun `progress survives a new ViewModel and a right guess catches it`() = runTest(dispatcher) {
        val vm = newViewModel()
        val state = vm.state.value
        val answer = state.answer!!
        val wrong = state.choices.first { it.id != answer.id }

        vm.guess(wrong)
        vm.useHint()
        advanceUntilIdle()
        assertEquals(setOf(wrong.id), vm.state.value.wrongGuesses)
        assertEquals(1, vm.state.value.hints.size)

        val reopened = newViewModel()
        assertEquals(setOf(wrong.id), reopened.state.value.wrongGuesses)
        assertEquals(1, reopened.state.value.hintsLeft)
        assertFalse(reopened.state.value.revealed)

        reopened.guess(answer)
        advanceUntilIdle()
        assertTrue(reopened.state.value.revealed)
        assertEquals(1, reopened.state.value.streakDays)
        assertEquals(listOf(answer.id), repository.caught)
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
    override suspend fun save(result: DailyResult) {
        results.value = results.value + (result.epochDay to result)
    }
    override fun streak(todayEpochDay: Long): Flow<Int> = results.map { map ->
        var day = todayEpochDay
        var streak = 0
        while (map[day]?.solved == true) { streak++; day-- }
        streak
    }
}
