package dev.pokedex.core.data

import dev.pokedex.core.database.EvolutionEntity
import dev.pokedex.core.database.TypeEfficacyEntity
import dev.pokedex.core.network.model.ChainLink
import dev.pokedex.core.network.model.EvolutionChainDto
import dev.pokedex.core.network.model.EvolutionDetail
import dev.pokedex.core.network.model.SpeciesDto
import dev.pokedex.core.network.model.TypeDto

/** Species ids stop here; higher ids in PokeAPI are alternate forms. */
internal const val MAX_SPECIES_ID = 10_000

/** "mr-mime" becomes "Mr Mime" until the proper English name arrives with the details. */
internal fun displayName(apiName: String) =
    apiName.split('-').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

/** The newest English Pokédex entry, with line breaks from the games cleaned up. */
internal fun SpeciesDto.latestEnglishEntry(): Pair<String, String>? =
    flavorTextEntries.lastOrNull { it.language.name == "en" }?.let { entry ->
        entry.text.replace(Regex("[\\n\\u000c\\r]+"), " ").replace(Regex("\\s+"), " ").trim() to displayName(entry.version.name)
    }

internal fun TypeDto.toEfficacyRows(): List<TypeEfficacyEntity> =
    damageRelations.doubleDamageTo.map { TypeEfficacyEntity(name, it.name, 2f) } +
        damageRelations.halfDamageTo.map { TypeEfficacyEntity(name, it.name, 0.5f) } +
        damageRelations.noDamageTo.map { TypeEfficacyEntity(name, it.name, 0f) }

/** Turns 18 type lists into "grass,poison" per species id, in slot order. Forms are skipped. */
internal fun typesBySpecies(types: List<TypeDto>): Map<Int, String> =
    types.flatMap { type -> type.pokemon.map { Triple(it.pokemon.id, it.slot, type.name) } }
        .filter { it.first < MAX_SPECIES_ID }
        .groupBy({ it.first }, { it.second to it.third })
        .mapValues { (_, slots) -> slots.sortedBy { it.first }.joinToString(",") { it.second } }

/** Walks the chain breadth-first, so every stage knows what it evolves from and how. */
internal fun flattenChain(dto: EvolutionChainDto): List<EvolutionEntity> {
    val rows = mutableListOf<EvolutionEntity>()
    fun visit(link: ChainLink, fromId: Int?, depth: Int) {
        rows += EvolutionEntity(
            speciesId = link.species.id,
            chainId = dto.id,
            fromId = fromId,
            method = if (fromId == null) null else link.evolutionDetails.firstOrNull()?.describe(),
            depth = depth,
        )
        link.evolvesTo.forEach { visit(it, link.species.id, depth + 1) }
    }
    visit(dto.chain, fromId = null, depth = 0)
    return rows
}

/** A short, plain description: "Level 16", "Use Thunder Stone", "High friendship, at night". */
internal fun EvolutionDetail.describe(): String {
    val parts = mutableListOf<String>()
    when (val triggerName = trigger?.name) {
        "level-up" -> {
            minLevel?.let { parts += "Level $it" }
            if (minHappiness != null) parts += "High friendship"
            knownMove?.let { parts += "Knowing ${displayName(it.name)}" }
            heldItem?.let { parts += "Holding ${displayName(it.name)}" }
            if (parts.isEmpty()) parts += "Level up"
        }
        "use-item" -> parts += item?.let { "Use ${displayName(it.name)}" } ?: "Use an item"
        "trade" -> parts += heldItem?.let { "Trade holding ${displayName(it.name)}" } ?: "Trade"
        null -> parts += "Special"
        else -> parts += displayName(triggerName)
    }
    when (timeOfDay) {
        "day" -> parts += "during the day"
        "night" -> parts += "at night"
    }
    return parts.joinToString(", ")
}

/** Consecutive solved days ending today, or yesterday if today isn't solved yet. */
internal fun streakFrom(solvedDaysDescending: List<Long>, today: Long): Int {
    val days = solvedDaysDescending.toSet()
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak++
        day--
    }
    return streak
}
