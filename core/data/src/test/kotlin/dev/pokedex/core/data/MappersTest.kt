package dev.pokedex.core.data

import dev.pokedex.core.network.model.ChainLink
import dev.pokedex.core.network.model.DamageRelations
import dev.pokedex.core.network.model.EvolutionChainDto
import dev.pokedex.core.network.model.EvolutionDetail
import dev.pokedex.core.network.model.NamedResource
import dev.pokedex.core.network.model.TypeDto
import dev.pokedex.core.network.model.TypePokemon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    private fun resource(kind: String, name: String, id: Int = 1) = NamedResource(name, "https://pokeapi.co/api/v2/$kind/$id/")

    @Test
    fun `api names become readable`() {
        assertEquals("Mr Mime", displayName("mr-mime"))
        assertEquals("Thunder Stone", displayName("thunder-stone"))
    }

    @Test
    fun `type lists merge into slot-ordered types and skip forms`() {
        val grass = TypeDto(
            name = "grass",
            damageRelations = DamageRelations(),
            pokemon = listOf(TypePokemon(1, resource("pokemon", "bulbasaur", 1))),
        )
        val poison = TypeDto(
            name = "poison",
            damageRelations = DamageRelations(),
            pokemon = listOf(
                TypePokemon(2, resource("pokemon", "bulbasaur", 1)),
                TypePokemon(1, resource("pokemon", "some-form", 10_050)),
            ),
        )
        assertEquals(mapOf(1 to "grass,poison"), typesBySpecies(listOf(poison, grass)))
    }

    @Test
    fun `damage relations become efficacy rows`() {
        val electric = TypeDto(
            name = "electric",
            damageRelations = DamageRelations(
                doubleDamageTo = listOf(resource("type", "water")),
                halfDamageTo = listOf(resource("type", "grass")),
                noDamageTo = listOf(resource("type", "ground")),
            ),
        )
        val rows = electric.toEfficacyRows().associate { it.defender to it.factor }
        assertEquals(mapOf("water" to 2f, "grass" to 0.5f, "ground" to 0f), rows)
    }

    @Test
    fun `chains flatten with parents, depth and methods`() {
        val levelUp = resource("evolution-trigger", "level-up")
        val chain = EvolutionChainDto(
            id = 10,
            chain = ChainLink(
                species = resource("pokemon-species", "pichu", 172),
                evolvesTo = listOf(
                    ChainLink(
                        species = resource("pokemon-species", "pikachu", 25),
                        evolutionDetails = listOf(EvolutionDetail(trigger = levelUp, minHappiness = 220)),
                        evolvesTo = listOf(
                            ChainLink(
                                species = resource("pokemon-species", "raichu", 26),
                                evolutionDetails = listOf(
                                    EvolutionDetail(trigger = resource("evolution-trigger", "use-item"), item = resource("item", "thunder-stone")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val rows = flattenChain(chain)
        assertEquals(listOf(172, 25, 26), rows.map { it.speciesId })
        assertEquals(listOf(null, 172, 25), rows.map { it.fromId })
        assertEquals(listOf(0, 1, 2), rows.map { it.depth })
        assertNull(rows[0].method)
        assertEquals("High friendship", rows[1].method)
        assertEquals("Use Thunder Stone", rows[2].method)
    }

    @Test
    fun `evolution methods read naturally`() {
        val levelUp = resource("evolution-trigger", "level-up")
        assertEquals("Level 16", EvolutionDetail(trigger = levelUp, minLevel = 16).describe())
        assertEquals("High friendship, at night", EvolutionDetail(trigger = levelUp, minHappiness = 160, timeOfDay = "night").describe())
        assertEquals("Trade", EvolutionDetail(trigger = resource("evolution-trigger", "trade")).describe())
        assertEquals("Level up", EvolutionDetail(trigger = levelUp, timeOfDay = "").describe())
    }

    @Test
    fun `streak counts back from today, or from yesterday if today is open`() {
        assertEquals(3, streakFrom(listOf(100, 99, 98, 96), today = 100))
        assertEquals(3, streakFrom(listOf(99, 98, 97), today = 100))
        assertEquals(0, streakFrom(listOf(97), today = 100))
        assertEquals(0, streakFrom(emptyList(), today = 100))
    }
}
