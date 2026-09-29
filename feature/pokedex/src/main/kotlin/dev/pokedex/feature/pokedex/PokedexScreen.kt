package dev.pokedex.feature.pokedex

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexChip
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexSearchField
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.TypeTag
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.pressScale
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.Pokemon

/** Stateful entry point, wired to the repository through Hilt. */
@Composable
fun PokedexRoute(
    onPokemonClick: (Pokemon) -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PokedexScreen(
        state = state,
        onPokemonClick = onPokemonClick,
        onPokemonShown = viewModel::onShown,
        onRetry = viewModel::refresh,
        onTypeChart = {}, onSort = {}, onSettings = onSettings, onFilters = {},
        contentPadding = contentPadding,
    )
}

/** The reference browser: every Pokémon in full, on cards in its type colour. */
@Composable
fun PokedexScreen(
    state: PokedexUiState,
    onPokemonClick: (Pokemon) -> Unit,
    onPokemonShown: (Int) -> Unit,
    onRetry: () -> Unit,
    onTypeChart: () -> Unit,
    onSort: () -> Unit,
    onSettings: () -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val shown = if (q.isEmpty()) state.pokemon else state.pokemon.filter { it.name.contains(q, ignoreCase = true) || it.number.contains(q) }

    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = "Pokédex") {
            DexIconButton(DexIcons.TypeChart, "Type chart", onTypeChart)
            DexIconButton(DexIcons.Sort, "Sort", onSort)
            DexIconButton(DexIcons.Settings, "Settings", onSettings)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DexSearchField(value = query, onValueChange = { query = it }, placeholder = "Search name or number")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DexChip("Filters", onFilters, icon = DexIcons.Filter)
                        DexChip("Type", onFilters)
                        DexChip("Generation", onFilters)
                        DexChip("Legendary", onFilters)
                    }
                    if (state.pokemon.isNotEmpty()) {
                        Text("%,d Pokémon".format(state.pokemon.size), style = DexTheme.type.labelMedium, color = colors.textSecondary)
                    }
                }
            }
            when {
                state.failed -> item(span = { GridItemSpan(maxLineSpan) }) { ErrorCard(onRetry) }
                state.loading -> items(8) { SkeletonCard() }
                shown.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("No Pokémon match '$q'.", style = DexTheme.type.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(vertical = 24.dp))
                }
                else -> items(shown, key = { it.id }) { p ->
                    LaunchedEffect(p.id, p.hasDetails) { if (!p.hasDetails) onPokemonShown(p.id) }
                    TypeCard(p, caught = p.id in state.caughtIds, onClick = { onPokemonClick(p) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun TypeCard(pokemon: Pokemon, caught: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = DexTheme.colors.isDark
    val c = pokemon.primaryType.colour()
    // Cards start neutral and blend into their type colour once details arrive.
    val top by animateColorAsState(c.container, tween(500), label = "cardTop")
    val content by animateColorAsState(c.content, tween(500), label = "cardContent")
    val bottom = if (dark) lerp(top, Color(0xFF110C1A), 0.2f) else lerp(top, Color.White, 0.28f)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction)
            .height(156.dp)
            .clip(DexShape.card)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(pokemon.name).append(", number ").append(pokemon.number)
                    if (pokemon.types.isNotEmpty()) append(", ").append(pokemon.types.joinToString(" and ") { it.displayName })
                    if (caught) append(", caught")
                }
            },
    ) {
        PokemonArtwork(
            pokemon,
            size = 104.dp,
            glow = Color.White,
            glowAlpha = 0.35f,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-2).dp),
        )
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("#${pokemon.number}", style = DexTheme.type.numberSmall, color = content)
            Text(pokemon.name, style = DexTheme.type.titleLarge, color = content, maxLines = 1)
            pokemon.types.forEach { TypeTag(it, content) }
        }
        if (caught) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(26.dp)
                    .clip(DexShape.full)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DexIcons.Check, contentDescription = null, tint = Color(0xFF1C1426), modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** Pulsing placeholder while the first index download runs. */
@Composable
private fun SkeletonCard() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        Modifier
            .height(156.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(DexShape.card)
            .background(DexTheme.colors.surface),
    )
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    GlassCard {
        Text("Couldn't download the Pokédex", style = DexTheme.type.titleMedium, color = DexTheme.colors.text)
        Text("Check your connection. Once it's downloaded, it works offline.", style = DexTheme.type.bodyMedium, color = DexTheme.colors.textSecondary)
        DexPrimaryButton("Try again", onRetry)
    }
}

@DexPreviews
@Composable
private fun PokedexScreenPreview() {
    DexTheme {
        PokedexScreen(
            state = PokedexUiState(pokemon = SampleData.generationOne.take(6), caughtIds = SampleData.caught, loading = false),
            onPokemonClick = {}, onPokemonShown = {}, onRetry = {}, onTypeChart = {}, onSort = {}, onSettings = {}, onFilters = {},
        )
    }
}
