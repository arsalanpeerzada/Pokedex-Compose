package dev.pokedex.feature.collection

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexProgressBar
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.FilterPill
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.component.PokemonSilhouette
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.pressScale
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.CollectionEntry
import dev.pokedex.core.model.CollectionState
import dev.pokedex.core.model.CollectionSummary
import dev.pokedex.core.model.Pokemon

private enum class AlbumFilter(val label: String) { All("All"), Caught("Caught"), Missing("Missing"), Favourites("Favourites") }

@Composable
fun CollectionRoute(
    onPokemonClick: (Pokemon) -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectionScreen(
        summary = state.summary,
        entries = state.entries,
        onPokemonClick = onPokemonClick,
        onPokemonShown = viewModel::onShown,
        onSearch = {},
        onSettings = onSettings,
        contentPadding = contentPadding,
    )
}

/** Collection as a sticker album: caught in colour, seen as silhouettes, unseen as numbers. */
@Composable
fun CollectionScreen(
    summary: CollectionSummary,
    entries: List<CollectionEntry>,
    onPokemonClick: (Pokemon) -> Unit,
    onPokemonShown: (Int) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    var filter by rememberSaveable { mutableStateOf(AlbumFilter.All) }
    val shown = when (filter) {
        AlbumFilter.All -> entries
        AlbumFilter.Caught -> entries.filter { it.state == CollectionState.Caught }
        AlbumFilter.Missing -> entries.filter { it.state != CollectionState.Caught }
        AlbumFilter.Favourites -> entries.filter { it.favourite }
    }
    val sections = shown.groupBy { it.pokemon.generation }
    val caughtByGeneration = entries.filter { it.state == CollectionState.Caught }.groupingBy { it.pokemon.generation }.eachCount()

    Column(modifier.fillMaxSize().background(colors.brandBrush())) {
        DexTopBar(title = "Collection") {
            DexIconButton(DexIcons.Search, "Search the collection", onSearch)
            DexIconButton(DexIcons.Settings, "Settings", onSettings)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fullWidth("summary") { SummaryCard(summary) }
            fullWidth("filters") {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlbumFilter.entries.forEach { f -> FilterPill(f.label, selected = f == filter, onClick = { filter = f }) }
                }
            }
            if (shown.isEmpty()) {
                fullWidth("empty") {
                    Text(
                        if (filter == AlbumFilter.Favourites) "No favourites yet. Tap the heart on any Pokémon to add it." else "Nothing here yet.",
                        style = DexTheme.type.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            sections.forEach { (generation, items) ->
                fullWidth("header-${generation.name}") {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp).animateItem(), verticalAlignment = Alignment.CenterVertically) {
                        Text(generation.label, style = DexTheme.type.titleLarge, color = colors.text, modifier = Modifier.weight(1f))
                        Text("${caughtByGeneration[generation] ?: 0} / ${generation.size}", style = DexTheme.type.numberLarge, color = colors.textSecondary)
                    }
                }
                items(items, key = { it.pokemon.id }) { entry ->
                    if (entry.state != CollectionState.Unseen && !entry.pokemon.hasDetails) {
                        LaunchedEffect(entry.pokemon.id) { onPokemonShown(entry.pokemon.id) }
                    }
                    AlbumCard(entry, onClick = { onPokemonClick(entry.pokemon) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable LazyGridItemScope.() -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun SummaryCard(summary: CollectionSummary) {
    val colors = DexTheme.colors
    // Counts up from zero the first time the album opens.
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(summary.caught) { target = summary.caught }
    val shownCount by animateIntAsState(target, tween(1100), label = "caughtCount")
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(shownCount.toString(), style = DexTheme.type.displayLarge, color = colors.text)
                    Text("of %,d caught".format(summary.total), style = DexTheme.type.labelMedium, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                DexProgressBar(if (summary.total == 0) 0f else summary.caught / summary.total.toFloat(), Modifier.fillMaxWidth(0.85f))
                Text(
                    if (summary.streakDays > 0) "${summary.streakDays}-day guess streak" else "Guess today's Pokémon to start a streak",
                    style = DexTheme.type.labelSmall,
                    color = colors.textSecondary,
                )
            }
            if (summary.latestCaught.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Latest", style = DexTheme.type.labelSmall, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                        summary.latestCaught.forEach { PokemonAvatar(it, 48.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(entry: CollectionEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    val pokemon = entry.pokemon
    val tint = pokemon.primaryType.colour().container.copy(alpha = if (colors.isDark) 0.3f else 0.22f)
    val interaction = remember { MutableInteractionSource() }
    val description = when (entry.state) {
        CollectionState.Caught -> "${pokemon.name}, number ${pokemon.number}, caught"
        CollectionState.Seen -> "Number ${pokemon.number}, seen but not caught"
        CollectionState.Unseen -> "Number ${pokemon.number}, not seen yet"
    }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .height(108.dp)
            .clip(DexShape.album)
            .background(if (entry.state == CollectionState.Unseen) colors.surfaceGlass.copy(alpha = colors.surfaceGlass.alpha * 0.55f) else colors.surfaceGlass)
            .then(if (entry.state == CollectionState.Caught) Modifier.background(tint) else Modifier)
            .border(1.dp, colors.surfaceGlassEdge, DexShape.album)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = entry.state != CollectionState.Unseen, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (entry.state) {
            CollectionState.Caught -> PokemonArtwork(pokemon, 64.dp)
            CollectionState.Seen -> PokemonSilhouette(pokemon, 64.dp, colors.silhouette)
            CollectionState.Unseen -> Unit
        }
        if (entry.state != CollectionState.Unseen) Spacer(Modifier.height(4.dp))
        Text(pokemon.number, style = DexTheme.type.numberSmall, color = if (entry.state == CollectionState.Unseen) colors.textSecondary else colors.text)
    }
}

@DexPreviews
@Composable
private fun CollectionScreenPreview() {
    DexTheme {
        CollectionScreen(SampleData.collectionSummary, SampleData.collection, onPokemonClick = {}, onPokemonShown = {}, onSearch = {}, onSettings = {})
    }
}
