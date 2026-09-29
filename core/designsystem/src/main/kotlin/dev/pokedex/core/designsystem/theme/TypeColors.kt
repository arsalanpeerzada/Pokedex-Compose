package dev.pokedex.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.pokedex.core.model.PokemonType

/** A type's colour and the label colour that passes 4.5:1 on it (Figma collection "Types"). */
@Immutable
data class TypeColour(val container: Color, val content: Color)

private val Ink = Color(0xFF1C1426)
private val White = Color(0xFFFFFFFF)

fun PokemonType.colour(): TypeColour = when (this) {
    PokemonType.Normal -> TypeColour(Color(0xFFA39E93), Ink)
    PokemonType.Fire -> TypeColour(Color(0xFFFF7A1A), Ink)
    PokemonType.Water -> TypeColour(Color(0xFF2D8CFF), Ink)
    PokemonType.Electric -> TypeColour(Color(0xFFFFCC1A), Ink)
    PokemonType.Grass -> TypeColour(Color(0xFF3DC864), Ink)
    PokemonType.Ice -> TypeColour(Color(0xFF46D6E6), Ink)
    PokemonType.Fighting -> TypeColour(Color(0xFFB83A2E), White)
    PokemonType.Poison -> TypeColour(Color(0xFF9B3FD1), White)
    PokemonType.Ground -> TypeColour(Color(0xFFD9A13B), Ink)
    PokemonType.Flying -> TypeColour(Color(0xFF8AA8FF), Ink)
    PokemonType.Psychic -> TypeColour(Color(0xFFFF5C9A), Ink)
    PokemonType.Bug -> TypeColour(Color(0xFF9BC31F), Ink)
    PokemonType.Rock -> TypeColour(Color(0xFFB8A05A), Ink)
    PokemonType.Ghost -> TypeColour(Color(0xFF6E56CF), White)
    PokemonType.Dragon -> TypeColour(Color(0xFF5B3DF5), White)
    PokemonType.Dark -> TypeColour(Color(0xFF5A4B45), White)
    PokemonType.Steel -> TypeColour(Color(0xFF7E9CB3), Ink)
    PokemonType.Fairy -> TypeColour(Color(0xFFFF8AD8), Ink)
}
