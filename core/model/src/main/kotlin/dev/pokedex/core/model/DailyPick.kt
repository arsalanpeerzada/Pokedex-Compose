package dev.pokedex.core.model

enum class WeatherScene { Clear, Heat, Cloud, Rain, Storm, Snow, Wind, Night }

data class Weather(
    val scene: WeatherScene,
    val description: String,
    val temperatureCelsius: Int,
    val isNight: Boolean,
)

/** Today's Pokémon, before it is revealed. The hint mentions only the weather, never the type. */
data class DailyPick(
    val city: String,
    val dateLabel: String,
    val weather: Weather,
    val answer: Pokemon,
    val choices: List<Pokemon>,
    val weatherHint: String,
    val hintsLeft: Int,
)
