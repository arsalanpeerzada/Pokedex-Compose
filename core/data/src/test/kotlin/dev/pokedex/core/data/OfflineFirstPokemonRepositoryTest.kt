package dev.pokedex.core.data

import dev.pokedex.core.database.EvolutionDao
import dev.pokedex.core.database.EvolutionEntity
import dev.pokedex.core.database.EvolutionRow
import dev.pokedex.core.database.PokemonDao
import dev.pokedex.core.database.PokemonEntity
import dev.pokedex.core.database.TypeDao
import dev.pokedex.core.database.TypeEfficacyEntity
import dev.pokedex.core.database.UserStateDao
import dev.pokedex.core.database.UserStateEntity
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.network.PokeApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstPokemonRepositoryTest {

    private val requests = mutableListOf<String>()
    private var offline = false

    private val engine = MockEngine { request ->
        val path = request.url.encodedPath.removePrefix("/api/v2/")
        requests += path
        if (offline) return@MockEngine respond("", HttpStatusCode.ServiceUnavailable)
        val body = when {
            path == "pokemon-species" -> """{"count":2,"results":[
                {"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon-species/1/"},
                {"name":"mr-mime","url":"https://pokeapi.co/api/v2/pokemon-species/122/"}]}"""
            path == "type/grass" -> typeJson("grass", """[{"slot":1,"pokemon":{"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon/1/"}}]""")
            path == "type/poison" -> typeJson("poison", """[{"slot":2,"pokemon":{"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon/1/"}}]""")
            path.startsWith("type/") -> typeJson(path.removePrefix("type/"), "[]")
            path == "pokemon/1" -> """{"id":1,"name":"bulbasaur","height":7,"weight":69,
                "types":[{"slot":1,"type":{"name":"grass","url":"x/12/"}},{"slot":2,"type":{"name":"poison","url":"x/4/"}}],
                "stats":[{"base_stat":45,"stat":{"name":"hp","url":"x/1/"}},{"base_stat":49,"stat":{"name":"attack","url":"x/2/"}},
                  {"base_stat":49,"stat":{"name":"defense","url":"x/3/"}},{"base_stat":65,"stat":{"name":"special-attack","url":"x/4/"}},
                  {"base_stat":65,"stat":{"name":"special-defense","url":"x/5/"}},{"base_stat":45,"stat":{"name":"speed","url":"x/6/"}}]}"""
            path == "pokemon-species/1" -> """{"id":1,"name":"bulbasaur","capture_rate":45,
                "names":[{"name":"Bulbasaur","language":{"name":"en","url":"x/9/"}}],
                "genera":[{"genus":"Seed Pokémon","language":{"name":"en","url":"x/9/"}}],
                "flavor_text_entries":[{"flavor_text":"A strange seed\nwas planted.","language":{"name":"en","url":"x/9/"},"version":{"name":"red","url":"x/1/"}}],
                "evolution_chain":{"url":"https://pokeapi.co/api/v2/evolution-chain/1/"}}"""
            else -> error("Unexpected request: $path")
        }
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }

    private fun typeJson(name: String, pokemon: String) =
        """{"name":"$name","damage_relations":{"double_damage_to":[],"half_damage_to":[],"no_damage_to":[]},"pokemon":$pokemon}"""

    private val client = HttpClient(engine) {
        expectSuccess = true
        defaultRequest { url("https://pokeapi.co/api/v2/") }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val pokemonDao = FakePokemonDao()
    private val repository = OfflineFirstPokemonRepository(PokeApi(client), pokemonDao, FakeUserStateDao(), FakeTypeDao(), FakeEvolutionDao())

    @Test
    fun `index and type index fill names and types`() = runTest {
        assertTrue(repository.refreshIndex().isSuccess)
        val pokedex = repository.pokedex().first()
        assertEquals(listOf("Bulbasaur", "Mr Mime"), pokedex.map { it.name })
        assertEquals(listOf(PokemonType.Grass, PokemonType.Poison), pokedex.first().types)
        assertEquals(1 + PokemonType.entries.size, requests.size)
    }

    @Test
    fun `details are fetched once and cached`() = runTest {
        repository.refreshIndex()
        repository.ensureDetails(1)
        repository.ensureDetails(1)
        val bulbasaur = repository.pokemon(1).first()!!
        assertEquals("Seed Pokémon", bulbasaur.category)
        assertEquals("A strange seed was planted.", bulbasaur.flavourText)
        assertEquals(318, bulbasaur.stats?.total)
        assertEquals(1, bulbasaur.evolutionChainId)
        assertEquals(1, requests.count { it == "pokemon/1" })
    }

    @Test
    fun `offline index reports failure without crashing`() = runTest {
        offline = true
        assertTrue(repository.refreshIndex().isFailure)
        repository.ensureDetails(1)
        assertTrue(repository.pokedex().first().isEmpty())
    }
}

private class FakePokemonDao : PokemonDao {
    val rows = MutableStateFlow<Map<Int, PokemonEntity>>(emptyMap())
    override fun observeAll(): Flow<List<PokemonEntity>> = rows.map { it.values.sortedBy(PokemonEntity::id) }
    override fun observe(id: Int): Flow<PokemonEntity?> = rows.map { it[id] }
    override suspend fun count() = rows.value.size
    override suspend fun needsDetails(id: Int) = rows.value[id]?.let { !it.detailsLoaded || it.hp == null || it.forms == null }
    override suspend fun countWithoutTypes() = rows.value.values.count { it.types.isEmpty() }
    override suspend fun insertIndex(items: List<PokemonEntity>) {
        rows.value = rows.value + items.filter { it.id !in rows.value }.associateBy { it.id }
    }
    override suspend fun update(item: PokemonEntity) {
        if (item.id in rows.value) rows.value = rows.value + (item.id to item)
    }
    override suspend fun setTypes(id: Int, types: String) {
        val row = rows.value[id] ?: return
        if (!row.detailsLoaded) rows.value = rows.value + (id to row.copy(types = types))
    }
}

private class FakeUserStateDao : UserStateDao {
    val rows = MutableStateFlow<Map<Int, UserStateEntity>>(emptyMap())
    override fun observeAll(): Flow<List<UserStateEntity>> = rows.map { it.values.toList() }
    override suspend fun get(id: Int) = rows.value[id]
    override suspend fun upsert(state: UserStateEntity) {
        rows.value = rows.value + (state.id to state)
    }
}

private class FakeTypeDao : TypeDao {
    val rows = MutableStateFlow<List<TypeEfficacyEntity>>(emptyList())
    override fun observeAll(): Flow<List<TypeEfficacyEntity>> = rows
    override suspend fun count() = rows.value.size
    override suspend fun upsert(rows: List<TypeEfficacyEntity>) {
        this.rows.value = this.rows.value + rows
    }
}

private class FakeEvolutionDao : EvolutionDao {
    val rows = MutableStateFlow<List<EvolutionEntity>>(emptyList())
    override fun observeChain(chainId: Int): Flow<List<EvolutionRow>> =
        rows.map { list -> list.filter { it.chainId == chainId }.map { EvolutionRow(it.speciesId, it.fromId, it.method, "") } }
    override suspend fun count(chainId: Int) = rows.value.count { it.chainId == chainId }
    override suspend fun upsert(rows: List<EvolutionEntity>) {
        this.rows.value = this.rows.value + rows
    }
}
