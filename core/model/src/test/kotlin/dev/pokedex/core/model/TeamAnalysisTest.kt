package dev.pokedex.core.model

import dev.pokedex.core.model.PokemonType.Electric
import dev.pokedex.core.model.PokemonType.Fire
import dev.pokedex.core.model.PokemonType.Flying
import dev.pokedex.core.model.PokemonType.Grass
import dev.pokedex.core.model.PokemonType.Ground
import dev.pokedex.core.model.PokemonType.Water
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamAnalysisTest {

    // A small, made-up chart: enough to test the counting, not a copy of the games' chart.
    private val chart = TypeChart(
        mapOf(
            (Electric to Water) to 2f,
            (Electric to Flying) to 2f,
            (Electric to Ground) to 0f,
            (Water to Fire) to 2f,
            (Fire to Grass) to 2f,
            (Grass to Water) to 2f,
            (Grass to Grass) to 0.5f,
        ),
    )

    private fun p(id: Int, vararg types: PokemonType) = Pokemon(id, "P$id", types.toList())

    @Test
    fun `shared weaknesses need two members and no resist`() {
        val team = listOf(p(1, Water), p(2, Water, Flying), p(3, Fire))
        val analysis = TeamAnalysis.of(team, chart)
        assertEquals(2, analysis.weakCounts[Electric])
        assertTrue(analysis.sharedWeaknesses().contains(Electric to 2))
    }

    @Test
    fun `an immune member counts as covering the weakness`() {
        val team = listOf(p(1, Water), p(2, Water, Flying), p(3, Ground))
        assertTrue(TeamAnalysis.of(team, chart).sharedWeaknesses().none { it.first == Electric })
    }

    @Test
    fun `a resisting member cancels the warning`() {
        val team = listOf(p(1, Water), p(2, Water), p(3, Grass))
        val analysis = TeamAnalysis.of(team, chart)
        assertEquals(2, analysis.weakCounts[Grass])
        assertEquals(1, analysis.resistCounts[Grass])
        assertTrue(analysis.sharedWeaknesses().none { it.first == Grass })
    }

    @Test
    fun `coverage comes from the members' own types`() {
        val analysis = TeamAnalysis.of(listOf(p(1, Water), p(2, Fire)), chart)
        assertEquals(setOf(Fire, Grass), analysis.covered)
        assertTrue(Water in analysis.uncovered)
    }

    @Test
    fun `members without types yet are ignored`() {
        val analysis = TeamAnalysis.of(listOf(Pokemon(9, "Unknown", emptyList())), chart)
        assertTrue(analysis.weakCounts.isEmpty())
        assertTrue(analysis.covered.isEmpty())
    }
}
