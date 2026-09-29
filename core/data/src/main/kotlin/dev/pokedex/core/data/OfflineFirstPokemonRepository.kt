package dev.pokedex.core.data

import dev.pokedex.core.database.PokemonDao
import dev.pokedex.core.database.PokemonEntity
import dev.pokedex.core.database.UserStateDao
import dev.pokedex.core.database.UserStateEntity
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.UserState
import dev.pokedex.core.network.PokeApi
import dev.pokedex.core.network.model.SpeciesDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
) : PokemonRepository {

    private val indexMutex = Mutex()
    // Be gentle with PokeAPI: at most four detail fetches at once, and never the same one twice.
    private val detailPermits = Semaphore(4)
    private val inFlight = ConcurrentHashMap.newKeySet<Int>()

    override fun pokedex(): Flow<List<Pokemon>> = pokemonDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun pokemon(id: Int): Flow<Pokemon?> = pokemonDao.observe(id).map { it?.toModel() }

    override fun userStates(): Flow<Map<Int, UserState>> = userStateDao.observeAll().map { rows ->
        rows.associate { it.id to UserState(caughtAt = it.caughtAt, seen = it.seen, favourite = it.favourite) }
    }

    override suspend fun refreshIndex(): Result<Unit> = indexMutex.withLock {
        try {
            if (pokemonDao.count() >= MIN_EXPECTED_SPECIES) return@withLock Result.success(Unit)
            val index = api.speciesIndex()
            pokemonDao.insertIndex(index.results.map { PokemonEntity(id = it.id, name = displayName(it.name)) })
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureDetails(id: Int) {
        if (pokemonDao.detailsLoaded(id) != false) return
        if (!inFlight.add(id)) return
        try {
            detailPermits.withPermit {
                coroutineScope {
                    val pokemon = async { api.pokemon(id) }
                    val species = async { api.species(id) }
                    val p = pokemon.await()
                    val s = species.await()
                    val entry = s.latestEnglishEntry()
                    pokemonDao.update(
                        PokemonEntity(
                            id = id,
                            name = s.names.firstOrNull { it.language.name == "en" }?.name ?: displayName(s.name),
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
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or rate-limited: the card stays neutral and we try again next time it's shown.
        } finally {
            inFlight.remove(id)
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
    hasDetails = detailsLoaded,
)

/** "mr-mime" becomes "Mr Mime" until the proper English name arrives with the details. */
private fun displayName(apiName: String) =
    apiName.split('-').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

/** The newest English Pokédex entry, with line breaks from the games cleaned up. */
private fun SpeciesDto.latestEnglishEntry(): Pair<String, String>? =
    flavorTextEntries.lastOrNull { it.language.name == "en" }?.let { entry ->
        entry.text.replace(Regex("[\\n\\u000c\\r]+"), " ").replace(Regex("\\s+"), " ").trim() to displayName(entry.version.name)
    }
