package dev.pokedex.feature.pokedex

import dev.pokedex.core.model.Generation
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType.Electric
import dev.pokedex.core.model.PokemonType.Fire
import dev.pokedex.core.model.PokemonType.Flying
import dev.pokedex.core.model.PokemonType.Grass
import dev.pokedex.core.model.PokemonType.Poison
import dev.pokedex.core.model.UserState
import dev.pokedex.feature.pokedex.PokedexViewModel.Companion.applyFilters
import org.junit.Assert.assertEquals
import org.junit.Test

class PokedexFiltersTest {

    private val bulbasaur = Pokemon(1, "Bulbasaur", listOf(Grass, Poison))
    private val charizard = Pokemon(6, "Charizard", listOf(Fire, Flying))
    private val pikachu = Pokemon(25, "Pikachu", listOf(Electric))
    private val chikorita = Pokemon(152, "Chikorita", listOf(Grass))
    private val all = listOf(bulbasaur, charizard, pikachu, chikorita)

    private fun ids(filters: PokedexFilters, query: String = "", users: Map<Int, UserState> = emptyMap()) =
        applyFilters(all, users, filters, query).map { it.id }

    @Test
    fun `no filters keeps number order`() {
        assertEquals(listOf(1, 6, 25, 152), ids(PokedexFilters()))
    }

    @Test
    fun `types match any selected type`() {
        assertEquals(listOf(1, 6, 152), ids(PokedexFilters(types = setOf(Grass, Fire))))
    }

    @Test
    fun `filters combine across groups`() {
        assertEquals(listOf(1), ids(PokedexFilters(types = setOf(Grass), generations = setOf(Generation.I))))
    }

    @Test
    fun `caught and favourites use the user's state`() {
        val users = mapOf(25 to UserState(caughtAt = 1L, favourite = true), 6 to UserState(favourite = true))
        assertEquals(listOf(25), ids(PokedexFilters(caughtOnly = true), users = users))
        assertEquals(listOf(6, 25), ids(PokedexFilters(favouritesOnly = true), users = users))
    }

    @Test
    fun `legendary and mythical match either when both are on`() {
        val mew = Pokemon(151, "Mew", listOf(dev.pokedex.core.model.PokemonType.Psychic), isMythical = true)
        val zapdos = Pokemon(145, "Zapdos", listOf(Electric, Flying), isLegendary = true)
        val list = all + listOf(zapdos, mew)
        fun ids(f: PokedexFilters) = applyFilters(list, emptyMap(), f, "").map { it.id }
        assertEquals(listOf(145), ids(PokedexFilters(legendaryOnly = true)))
        assertEquals(listOf(151), ids(PokedexFilters(mythicalOnly = true)))
        assertEquals(listOf(145, 151), ids(PokedexFilters(legendaryOnly = true, mythicalOnly = true)))
        assertEquals(2, PokedexFilters(legendaryOnly = true, mythicalOnly = true).sheetCount)
    }

    @Test
    fun `search matches names and numbers`() {
        assertEquals(listOf(25), ids(PokedexFilters(), query = "pika"))
        assertEquals(listOf(25), ids(PokedexFilters(), query = "25"))
        assertEquals(listOf(152), ids(PokedexFilters(), query = "0152"))
    }

    @Test
    fun `sorting by name`() {
        assertEquals(listOf(1, 6, 152, 25), ids(PokedexFilters(sort = PokedexSort.NameAscending)))
        assertEquals(listOf(25, 152, 6, 1), ids(PokedexFilters(sort = PokedexSort.NameDescending)))
    }

    @Test
    fun `sheet count and default ignore sort`() {
        val f = PokedexFilters(types = setOf(Grass), generations = setOf(Generation.II), sort = PokedexSort.NameAscending)
        assertEquals(2, f.sheetCount)
        assertEquals(true, PokedexFilters(sort = PokedexSort.NameDescending).isDefault)
    }
}
