package dev.pokedex.core.model

/** Sticker-album state: caught shows in colour, seen as a silhouette, unseen as just the number. */
enum class CollectionState { Caught, Seen, Unseen }

data class CollectionEntry(
    val pokemon: Pokemon,
    val state: CollectionState,
)

data class CollectionSummary(
    val caught: Int,
    val total: Int,
    val streakDays: Int,
    val latestCaught: List<Pokemon>,
    val generationLabel: String,
    val generationCaught: Int,
    val generationTotal: Int,
)
