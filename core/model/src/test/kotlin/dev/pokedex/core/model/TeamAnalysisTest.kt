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
    fun `suggestions patch shared weaknesses first and skip legendaries`() {
        val team = listOf(p(1, Water), p(2, Water, Flying))
        val pokedex = listOf(
            p(10, Ground), // immune to Electric: covers the shared weakness
            p(11, Grass), // resists the shared Grass weakness and hits Water
            p(12, Ground).copy(isLegendary = true),
            p(13, Water), // already weak to Electric and Grass: makes things worse
            p(1, Water), // already in the team
        )
        val suggestions = TeamSuggestions.suggest(team, pokedex, chart)
        assertEquals(listOf(11, 10), suggestions.map { it.pokemon.id })
        val ground = suggestions.first { it.pokemon.id == 10 }
        assertEquals(listOf(Electric), ground.covers)
        assertEquals("Resists Electric", ground.reason)
        assertEquals("Resists Grass · hits Water", suggestions.first().reason)
    }

    @Test
    fun `no suggestions for an empty or full team`() {
        val pokedex = listOf(p(10, Ground))
        assertTrue(TeamSuggestions.suggest(emptyList(), pokedex, chart).isEmpty())
        assertTrue(TeamSuggestions.suggest((1..6).map { p(it, Water) }, pokedex, chart).isEmpty())
    }

    @Test
    fun `members without types yet are ignored`() {
        val analysis = TeamAnalysis.of(listOf(Pokemon(9, "Unknown", emptyList())), chart)
        assertTrue(analysis.weakCounts.isEmpty())
        assertTrue(analysis.covered.isEmpty())
    }
}
