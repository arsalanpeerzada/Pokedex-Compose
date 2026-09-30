package dev.pokedex.core.model

const val TEAM_SIZE = 6

/** A team of up to six. [members] always has six entries; null is an empty slot. */
data class Team(val id: Long, val name: String, val members: List<Pokemon?>) {
    val filled: List<Pokemon> get() = members.filterNotNull()
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
