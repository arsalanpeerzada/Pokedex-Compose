package dev.pokedex.core.data

import dev.pokedex.core.database.EvolutionDao
import dev.pokedex.core.database.PokemonDao
import dev.pokedex.core.database.PokemonEntity
import dev.pokedex.core.database.TypeDao
import dev.pokedex.core.database.UserStateDao
import dev.pokedex.core.database.UserStateEntity
import dev.pokedex.core.model.BaseStats
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.UserState
import dev.pokedex.core.network.PokeApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstPokemonRepository @Inject constructor(
    private val api: PokeApi,
    private val pokemonDao: PokemonDao,
    private val userStateDao: UserStateDao,
    private val typeDao: TypeDao,
    private val evolutionDao: EvolutionDao,
) : PokemonRepository {

    private val indexMutex = Mutex()
    // Be gentle with PokeAPI: at most four requests at once, and never the same resource twice.
    private val permits = Semaphore(4)
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    override fun pokedex(): Flow<List<Pokemon>> = pokemonDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun pokemon(id: Int): Flow<Pokemon?> = pokemonDao.observe(id).map { it?.toModel() }

    override fun userStates(): Flow<Map<Int, UserState>> = userStateDao.observeAll().map { rows ->
        rows.associate { it.id to UserState(caughtAt = it.caughtAt, seen = it.seen, favourite = it.favourite) }
    }

    override fun typeChart(): Flow<TypeChart> = typeDao.observeAll().map { rows ->
        TypeChart(
            rows.mapNotNull { row ->
                val attacker = PokemonType.fromApiName(row.attacker) ?: return@mapNotNull null
                val defender = PokemonType.fromApiName(row.defender) ?: return@mapNotNull null
                (attacker to defender) to row.factor
            }.toMap(),
        )
    }

    override fun evolution(chainId: Int): Flow<List<EvolutionStep>> = evolutionDao.observeChain(chainId).map { rows ->
        rows.map { EvolutionStep(it.speciesId, it.name, it.fromId, it.method) }
    }

    override suspend fun refreshIndex(): Result<Unit> = indexMutex.withLock {
        try {
            if (pokemonDao.count() < MIN_EXPECTED_SPECIES) {
                val index = api.speciesIndex()
                pokemonDao.insertIndex(index.results.map { PokemonEntity(id = it.id, name = displayName(it.name)) })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock Result.failure(e)
        }
        // The type index is a bonus: without it, types still arrive with each Pokémon's details.
        runCatchingNonCancel { refreshTypeIndex() }
        Result.success(Unit)
    }

    /** 18 requests give every Pokémon its types and the whole type chart. */
    private suspend fun refreshTypeIndex() {
        if (typeDao.count() > 0 && pokemonDao.countWithoutTypes() == 0) return
        val types = coroutineScope {
            PokemonType.entries.map { type ->
                async { permits.withPermit { api.type(type.name.lowercase()) } }
            }.awaitAll()
        }
        typeDao.upsert(types.flatMap { it.toEfficacyRows() })
        pokemonDao.setTypes(typesBySpecies(types))
    }

    override suspend fun ensureDetails(id: Int) {
        if (pokemonDao.needsDetails(id) != true) return
        fetchOnce("details-$id") {
            coroutineScope {
                val pokemon = async { api.pokemon(id) }
                val species = async { api.species(id) }
                val p = pokemon.await()
                val s = species.await()
                val entry = s.latestEnglishEntry()
                val stats = p.stats.associate { it.stat.name to it.baseStat }
                val name = s.names.firstOrNull { it.language.name == "en" }?.name ?: displayName(s.name)
                pokemonDao.update(
                    PokemonEntity(
                        id = id,
                        name = name,
                        types = p.types.sortedBy { it.slot }.joinToString(",") { it.type.name },
                        heightDecimetres = p.height,
                        weightHectograms = p.weight,
                        category = s.genera.firstOrNull { it.language.name == "en" }?.genus,
                        catchRate = s.captureRate,
                        flavourText = entry?.first,
                        flavourVersion = entry?.second,
                        cryUrl = p.cries?.latest,
                        isLegendary = s.isLegendary,
                        isMythical = s.isMythical,
                        detailsLoaded = true,
                        hp = stats["hp"],
                        attack = stats["attack"],
                        defense = stats["defense"],
                        specialAttack = stats["special-attack"],
                        specialDefense = stats["special-defense"],
                        speed = stats["speed"],
                        evolutionChainId = s.evolutionChain?.id,
                        forms = s.alternateForms(name).encode(),
                        habitat = s.habitat?.let { displayName(it.name) },
                        firstGame = s.firstGame(),
                        storyEntries = s.storyEntries().encodeEntries(),
                    ),
                )
            }
        }
    }

    override fun downloadProgress(): Flow<DownloadProgress> =
        combine(pokemonDao.observeCompleteCount(), pokemonDao.observeCount(), ::DownloadProgress)

    override suspend fun downloadAll(): Int {
        // ensureDetails shares the four request permits, so this never exceeds four at once.
        coroutineScope {
            pokemonDao.idsNeedingDetails().forEach { id -> launch { ensureDetails(id) } }
        }
        return pokemonDao.idsNeedingDetails().size
    }

    override suspend fun ensureEvolution(chainId: Int) {
        if (evolutionDao.count(chainId) > 0) return
        fetchOnce("chain-$chainId") {
            evolutionDao.upsert(flattenChain(api.evolutionChain(chainId)))
        }
    }

    /** Runs [block] under a request permit unless the same key is already running. Offline errors are swallowed. */
    private suspend fun fetchOnce(key: String, block: suspend () -> Unit) {
        if (!inFlight.add(key)) return
        try {
            permits.withPermit { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or rate-limited: we try again the next time it's needed.
        } finally {
            inFlight.remove(key)
        }
    }

    override suspend fun setCaught(id: Int, caught: Boolean) {
        val current = userStateDao.get(id) ?: UserStateEntity(id)
        userStateDao.upsert(current.copy(caughtAt = if (caught) current.caughtAt ?: System.currentTimeMillis() else null, seen = true))
    }

    override suspend fun toggleFavourite(id: Int) {
        val current = userStateDao.get(id) ?: UserStateEntity(id)
        userStateDao.upsert(current.copy(favourite = !current.favourite))
    }

    override suspend fun markSeen(id: Int) {
        val current = userStateDao.get(id) ?: UserStateEntity(id)
        if (!current.seen) userStateDao.upsert(current.copy(seen = true))
    }

    private companion object {
        const val MIN_EXPECTED_SPECIES = 1000
    }
}

private suspend fun runCatchingNonCancel(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
}

private fun PokemonEntity.toModel() = Pokemon(
    id = id,
    name = name,
    types = types.split(',').mapNotNull { PokemonType.fromApiName(it.trim()) },
    category = category,
    heightMetres = heightDecimetres?.div(10.0),
    weightKilograms = weightHectograms?.div(10.0),
    catchRate = catchRate,
    flavourText = flavourText,
    flavourVersion = flavourVersion,
    cryUrl = cryUrl,
    isLegendary = isLegendary,
    isMythical = isMythical,
    stats = baseStats(),
    evolutionChainId = evolutionChainId,
    forms = decodeForms(forms),
    habitat = habitat,
    firstGame = firstGame,
    storyEntries = decodeEntries(storyEntries),
    hasDetails = detailsLoaded,
)

private fun PokemonEntity.baseStats(): BaseStats? {
    return BaseStats(
        hp = hp ?: return null,
        attack = attack ?: return null,
        defense = defense ?: return null,
        specialAttack = specialAttack ?: return null,
        specialDefense = specialDefense ?: return null,
        speed = speed ?: return null,
    )
}
