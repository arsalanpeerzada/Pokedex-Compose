package dev.pokedex.core.model

import dev.pokedex.core.model.PokemonType.Electric
import dev.pokedex.core.model.PokemonType.Flying
import dev.pokedex.core.model.PokemonType.Ground
import dev.pokedex.core.model.PokemonType.Normal
import dev.pokedex.core.model.PokemonType.Water
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {

    @Test
    fun `generation boundaries follow the National Dex ranges`() {
        assertEquals(Generation.I, Generation.of(1))
        assertEquals(Generation.I, Generation.of(151))
        assertEquals(Generation.II, Generation.of(152))
        assertEquals(Generation.IX, Generation.of(1025))
        assertEquals(1025, Generation.entries.sumOf { it.size })
    }

    @Test
    fun `number is padded to four digits`() {
        assertEquals("0025", Pokemon(25, "Pikachu", listOf(Electric)).number)
    }

    @Test
    fun `primary type is Normal until types load`() {
        assertEquals(Normal, Pokemon(1, "Bulbasaur", emptyList()).primaryType)
    }

    @Test
    fun `dual types multiply and missing pairs are neutral`() {
        val chart = TypeChart(
            mapOf(
                (Electric to Water) to 2f,
                (Electric to Flying) to 2f,
                (Electric to Ground) to 0f,
            ),
        )
        val defending = chart.defending(listOf(Water, Flying))
        assertEquals(4f, defending.getValue(Electric))
        assertEquals(1f, defending.getValue(Normal))
        assertEquals(0f, chart.defending(listOf(Ground, Flying)).getValue(Electric))
    }

    @Test
    fun `base stat total adds all six`() {
        assertEquals(21, BaseStats(1, 2, 3, 4, 5, 6).total)
    }
}
