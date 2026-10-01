package dev.pokedex.core.model

const val TEAM_SIZE = 6

/** A team of up to six. [members] always has six entries; null is an empty slot. */
data class Team(val id: Long, val name: String, val members: List<Pokemon?>) {
    val filled: List<Pokemon> get() = members.filterNotNull()
}

/** A Pokémon that would help, and why, in a few words. */
data class TeamSuggestion(val pokemon: Pokemon, val covers: List<PokemonType>, val hits: List<PokemonType>) {
    val reason: String
        get() = listOfNotNull(
            covers.takeIf { it.isNotEmpty() }?.let { "Resists ${it.joinToString(", ") { t -> t.displayName }}" },
            hits.takeIf { it.isNotEmpty() }?.let { "hits ${it.joinToString(", ") { t -> t.displayName }}" },
        ).joinToString(" · ").replaceFirstChar { it.uppercase() }
}

object TeamSuggestions {
    /**
     * Up to [limit] Pokémon that patch the team's holes: resisting its shared weaknesses counts
     * double, adding attack coverage counts once, and piling onto a shared weakness counts against.
     * Legendary and mythical Pokémon are left out, and ties go to the lower Pokédex number.
     */
    fun suggest(team: List<Pokemon>, pokedex: List<Pokemon>, chart: TypeChart, limit: Int = 5): List<TeamSuggestion> {
        if (team.isEmpty() || team.size >= TEAM_SIZE || chart.isEmpty) return emptyList()
        val analysis = TeamAnalysis.of(team, chart)
        val shared = analysis.sharedWeaknesses().map { it.first }
        val uncovered = analysis.uncovered.toSet()
        val inTeam = team.map { it.id }.toSet()
        return pokedex.asSequence()
            .filter { it.id !in inTeam && it.types.isNotEmpty() && !it.isLegendary && !it.isMythical }
            .map { candidate ->
                val defending = chart.defending(candidate.types)
                val covers = shared.filter { (defending[it] ?: 1f) < 1f }
                val hits = uncovered.filter { target -> candidate.types.any { chart.factor(it, target) >= 2f } }
                val worsens = shared.count { (defending[it] ?: 1f) > 1f }
                Triple(TeamSuggestion(candidate, covers, hits.sortedBy { it.ordinal }), covers.size * 2 + hits.size - worsens * 2, candidate.id)
            }
            .filter { (_, score, _) -> score > 0 }
            .sortedWith(compareByDescending<Triple<TeamSuggestion, Int, Int>> { it.second }.thenBy { it.third })
            .take(limit)
            .map { it.first }
            .toList()
    }
}

/**
 * How a team holds up, from its members' types alone (moves aren't modelled):
 * - defence: for each attacking type, how many members take extra or reduced damage;
 * - offence: which defending types at least one member's own type hits for double damage.
 */
data class TeamAnalysis(
    val weakCounts: Map<PokemonType, Int>,
    val resistCounts: Map<PokemonType, Int>,
    val covered: Set<PokemonType>,
) {
    /** Attacking types that hit at least [threshold] members hard, with nobody resisting. */
    fun sharedWeaknesses(threshold: Int = 2): List<Pair<PokemonType, Int>> =
        weakCounts.filter { (type, count) -> count >= threshold && (resistCounts[type] ?: 0) == 0 }
            .toList()
            .sortedByDescending { it.second }

    val uncovered: List<PokemonType> get() = PokemonType.entries.filter { it !in covered }

    companion object {
        fun of(members: List<Pokemon>, chart: TypeChart): TeamAnalysis {
            val typed = members.filter { it.types.isNotEmpty() }
            val defending = typed.map { chart.defending(it.types) }
            val weak = PokemonType.entries.associateWith { attacker -> defending.count { (it[attacker] ?: 1f) > 1f } }
            val resist = PokemonType.entries.associateWith { attacker -> defending.count { (it[attacker] ?: 1f) < 1f } }
            val attackingTypes = typed.flatMap { it.types }.toSet()
            val covered = PokemonType.entries.filter { defender -> attackingTypes.any { chart.factor(it, defender) >= 2f } }.toSet()
            return TeamAnalysis(weak.filterValues { it > 0 }, resist.filterValues { it > 0 }, covered)
        }
    }
}
