package dev.pokedex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pokedex.core.designsystem.motion.dexSharedElement
import dev.pokedex.core.designsystem.motion.floating
import dev.pokedex.core.designsystem.motion.rememberPulse
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.Pokemon

/** Shared-element key for a Pokémon's artwork, used by every screen that shows it. */
fun artworkKey(pokemonId: Int) = "artwork-$pokemonId"

/**
 * Official artwork with a soft radial glow behind it. With [animated], the glow breathes and the
 * artwork floats. The image is a shared element, so it flies between screens.
 */
@Composable
fun PokemonArtwork(
    pokemon: Pokemon,
    size: Dp,
    modifier: Modifier = Modifier,
    glow: Color = pokemon.primaryType.colour().container,
    glowAlpha: Float = 0.5f,
    animated: Boolean = false,
) {
    val pulse = if (animated) rememberPulse() else 1f
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .background(Brush.radialGradient(listOf(glow.copy(alpha = glowAlpha), Color.Transparent))),
        )
        AsyncImage(
            model = pokemon.artworkUrl,
            contentDescription = pokemon.name,
            modifier = Modifier
                .fillMaxSize()
                .then(if (animated) Modifier.floating() else Modifier)
                .dexSharedElement(artworkKey(pokemon.id)),
        )
    }
}

/** The same artwork tinted to a flat colour. The description never gives the answer away. */
@Composable
fun PokemonSilhouette(
    pokemon: Pokemon,
    size: Dp,
    colour: Color,
    modifier: Modifier = Modifier,
    contentDescription: String = "Mystery Pokémon",
) {
    AsyncImage(
        model = pokemon.artworkUrl,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(colour, BlendMode.SrcIn),
        modifier = modifier.size(size),
    )
}

/** Circular avatar on the type colour, with a ring in the surface colour. */
@Composable
fun PokemonAvatar(pokemon: Pokemon, size: Dp, modifier: Modifier = Modifier) {
    val ring = if (DexTheme.colors.isDark) DexTheme.colors.surface else Color.White
    AsyncImage(
        model = pokemon.artworkUrl,
        contentDescription = pokemon.name,
        modifier = modifier
            .size(size)
            .border(3.dp, ring, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(pokemon.primaryType.colour().container),
    )
}
