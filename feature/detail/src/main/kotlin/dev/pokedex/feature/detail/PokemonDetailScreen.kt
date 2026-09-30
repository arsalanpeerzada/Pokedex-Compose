package dev.pokedex.feature.detail

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexGlassButton
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.TypeBadge
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.StaggeredEntrance
import dev.pokedex.core.designsystem.motion.rememberPulse
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.detailPalette
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType

private val Tabs = listOf("About", "Stats", "Evolution", "Matchups", "Forms")

@Composable
fun PokemonDetailRoute(pokemonId: Int, onBack: () -> Unit) {
    val viewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        key = "detail-$pokemonId",
        creationCallback = { it.create(pokemonId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Until Room answers, the artwork (which needs only the id) is enough for the shared-element flight.
    val pokemon = state.pokemon ?: Pokemon(pokemonId, name = "", types = emptyList(), hasDetails = false)
    val context = LocalContext.current
    val cry = rememberCryPlayer()
    PokemonDetailScreen(
        pokemon = pokemon,
        caught = state.user.caught,
        favourite = state.user.favourite,
        cryPlaying = cry.playing,
        onBack = onBack,
        onToggleCaught = viewModel::toggleCaught,
        onFavourite = viewModel::toggleFavourite,
        onShare = {
            val text = buildString {
                append(pokemon.name).append(", #").append(pokemon.number)
                pokemon.category?.let { append(", the ").append(it) }
                append('.')
            }
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, null))
        },
        onPlayCry = { pokemon.cryUrl?.let(cry::play) },
    )
}

/** One detail layout, shared by the Pokédex, the collection and Today. Colours come from the Pokémon itself. */
@Composable
fun PokemonDetailScreen(
    pokemon: Pokemon,
    caught: Boolean,
    favourite: Boolean,
    cryPlaying: Boolean,
    onBack: () -> Unit,
    onToggleCaught: () -> Unit,
    onFavourite: () -> Unit,
    onShare: () -> Unit,
    onPlayCry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DexTheme.colors
    val palette = pokemon.primaryType.detailPalette(colors.isDark)
    // The page starts neutral and blends into the type colour once details arrive.
    val top by animateColorAsState(palette.top, tween(600), label = "detailTop")
    val bottom by animateColorAsState(palette.bottom, tween(600), label = "detailBottom")
    val content by animateColorAsState(palette.content, tween(600), label = "detailContent")
    val secondary by animateColorAsState(palette.contentSecondary, tween(600), label = "detailSecondary")
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        DexTopBar(
            contentColor = content,
            navigation = { DexIconButton(DexIcons.Back, "Back", onBack, tint = content, glass = true) },
        ) {
            FavouriteButton(favourite, onFavourite, content)
            DexIconButton(DexIcons.Share, "Share", onShare, tint = content, glass = true)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PokemonArtwork(pokemon, size = 230.dp, glowAlpha = if (colors.isDark) 0.5f else 0.6f, animated = true)
            StaggeredEntrance(0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#${pokemon.number}", style = DexTheme.type.numberLarge, color = secondary)
                    Text(pokemon.name, style = DexTheme.type.displayLarge, color = content, textAlign = TextAlign.Center)
                    pokemon.category?.let { Text(it, style = DexTheme.type.labelLarge, color = secondary) }
                }
            }
            StaggeredEntrance(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pokemon.types.forEach { TypeBadge(it, icon = if (it == PokemonType.Electric) DexIcons.Bolt else null) }
                    if (pokemon.isLegendary) RarityLabel("Legendary")
                    if (pokemon.isMythical) RarityLabel("Mythical")
                }
            }
            StaggeredEntrance(2) {
                AnimatedContent(
                    targetState = caught,
                    transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(120)) },
                    label = "caughtButton",
                    modifier = Modifier.fillMaxWidth(),
                ) { isCaught ->
                    if (isCaught) {
                        DexGlassButton("Caught", onToggleCaught, icon = DexIcons.Check, contentColor = content, modifier = Modifier.fillMaxWidth())
                    } else {
                        DexPrimaryButton("Mark as caught", onToggleCaught, icon = DexIcons.Plus, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            StaggeredEntrance(3) { TabRow(selected = tab, onSelect = { tab = it }, content = content, contentSecondary = secondary) }
            StaggeredEntrance(4) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "detailTab",
                ) { selected ->
                    if (selected == 0) {
                        AboutCard(pokemon, content, secondary)
                    } else {
                        GlassCard {
                            Text("${Tabs[selected]} are coming soon.", style = DexTheme.type.bodyLarge, color = content)
                        }
                    }
                }
            }
            StaggeredEntrance(5) { CryCard(pokemon, cryPlaying, onPlayCry, content, secondary) }
        }
    }
}

@Composable
private fun RarityLabel(text: String) {
    val colors = DexTheme.colors
    Row(
        Modifier.clip(DexShape.full).background(colors.highlight).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(DexIcons.Star, contentDescription = null, tint = colors.onHighlight, modifier = Modifier.size(14.dp))
        Text(text, style = DexTheme.type.labelMedium, color = colors.onHighlight)
    }
}

/** The heart pops when a Pokémon becomes a favourite. */
@Composable
private fun FavouriteButton(favourite: Boolean, onClick: () -> Unit, content: Color) {
    val scale = remember { Animatable(1f) }
    // Only pop on a change, not when the screen opens on an existing favourite.
    val previous = remember { booleanArrayOf(favourite) }
    LaunchedEffect(favourite) {
        if (favourite && !previous[0]) {
            scale.animateTo(1.35f, tween(120))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        previous[0] = favourite
    }
    val tint by animateColorAsState(if (favourite) DexTheme.colors.accent else content, label = "heartTint")
    DexIconButton(
        DexIcons.Heart,
        if (favourite) "Remove from favourites" else "Add to favourites",
        onClick,
        tint = tint,
        glass = true,
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}

@Composable
private fun AboutCard(pokemon: Pokemon, content: Color, secondary: Color) {
    GlassCard {
        val entry = pokemon.flavourText
        when {
            entry != null -> {
                Text(entry, style = DexTheme.type.bodyLarge, color = content)
                pokemon.flavourVersion?.let { Text("Pokémon $it", style = DexTheme.type.labelSmall, color = secondary) }
            }
            pokemon.hasDetails -> Text("No Pokédex entry yet.", style = DexTheme.type.bodyLarge, color = secondary)
            else -> Text("Loading the Pokédex entry…", style = DexTheme.type.bodyLarge, color = secondary, modifier = Modifier.graphicsLayer { alpha = 0.8f })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact("Height", pokemon.heightMetres?.let { "%.1f m".format(it) }, pokemon.hasDetails, content, secondary, Modifier.weight(1f))
            Fact("Weight", pokemon.weightKilograms?.let { "%.1f kg".format(it) }, pokemon.hasDetails, content, secondary, Modifier.weight(1f))
            Fact("Catch rate", pokemon.catchRate?.toString(), pokemon.hasDetails, content, secondary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CryCard(pokemon: Pokemon, playing: Boolean, onPlay: () -> Unit, content: Color, secondary: Color) {
    val colors = DexTheme.colors
    val pulse = rememberPulse(from = 1f, to = if (playing) 1.12f else 1f, periodMillis = 600)
    GlassCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(
                onClick = onPlay,
                enabled = pokemon.cryUrl != null && !playing,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = if (playing) 1f else 0.4f),
                    disabledContentColor = colors.onPrimary,
                ),
                modifier = Modifier.size(48.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
            ) { Icon(DexIcons.Play, contentDescription = "Play ${pokemon.name}'s cry", modifier = Modifier.size(18.dp)) }
            Column {
                Text("Cry", style = DexTheme.type.titleMedium, color = content)
                Text(
                    when {
                        playing -> "Playing…"
                        pokemon.cryUrl != null -> "Tap to listen"
                        pokemon.hasDetails -> "No cry available"
                        else -> "Loading…"
                    },
                    style = DexTheme.type.labelMedium,
                    color = secondary,
                )
            }
        }
    }
}

/** Pill tabs with an indicator that slides between them. */
@Composable
private fun TabRow(selected: Int, onSelect: (Int) -> Unit, content: Color, contentSecondary: Color) {
    val colors = DexTheme.colors
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(DexShape.full)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassEdge, DexShape.full)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / Tabs.size
        val indicatorOffset by animateDpAsState(tabWidth * selected, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "tabIndicator")
        Box(
            Modifier
                .matchParentSize()
                .wrapContentWidth(Alignment.Start)
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(tabWidth)
                .clip(DexShape.full)
                .background(colors.surface),
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            Tabs.forEachIndexed { i, label ->
                val isSelected = i == selected
                val tint by animateColorAsState(if (isSelected) content else contentSecondary, label = "tabText")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(DexShape.full)
                        .selectable(selected = isSelected, onClick = { onSelect(i) }, role = Role.Tab)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = DexTheme.type.labelMedium, color = tint, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String?, loaded: Boolean, content: Color, contentSecondary: Color, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    Column(
        modifier
            .clip(DexShape.large)
            .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = DexTheme.type.labelSmall, color = contentSecondary)
        Text(value ?: if (loaded) "Unknown" else "…", style = DexTheme.type.titleMedium, color = content)
    }
}

@DexPreviews
@Composable
private fun PokemonDetailScreenPreview() {
    DexTheme {
        PokemonDetailScreen(
            SampleData.pikachu, caught = false, favourite = true, cryPlaying = false,
            onBack = {}, onToggleCaught = {}, onFavourite = {}, onShare = {}, onPlayCry = {},
        )
    }
}
