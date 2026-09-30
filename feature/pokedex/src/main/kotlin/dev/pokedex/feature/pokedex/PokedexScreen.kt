@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.heading
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
import dev.pokedex.core.designsystem.component.FilterPill
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.TypeTag
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.pressScale
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.Generation
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType

/** Stateful entry point, wired to the repository through Hilt. */
@Composable
fun PokedexRoute(
    onPokemonClick: (Pokemon) -> Unit,
    onTypeChart: () -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PokedexScreen(
        state = state,
        actions = PokedexActions(
            onPokemonClick = onPokemonClick,
            onPokemonShown = viewModel::onShown,
            onRetry = viewModel::refresh,
            onQueryChange = viewModel::onQueryChange,
            onToggleType = viewModel::toggleType,
            onToggleGeneration = viewModel::toggleGeneration,
            onToggleCaughtOnly = viewModel::toggleCaughtOnly,
            onToggleFavouritesOnly = viewModel::toggleFavouritesOnly,
            onSort = viewModel::setSort,
            onClearFilters = viewModel::clearFilters,
            onTypeChart = onTypeChart,
            onSettings = onSettings,
        ),
        contentPadding = contentPadding,
    )
}

/** Everything the Pokédex screen can ask for, so the screen stays stateless and previewable. */
class PokedexActions(
    val onPokemonClick: (Pokemon) -> Unit = {},
    val onPokemonShown: (Int) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onToggleType: (PokemonType) -> Unit = {},
    val onToggleGeneration: (Generation) -> Unit = {},
    val onToggleCaughtOnly: () -> Unit = {},
    val onToggleFavouritesOnly: () -> Unit = {},
    val onSort: (PokedexSort) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onTypeChart: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

/** The reference browser: every Pokémon in full, on cards in its type colour. */
@Composable
fun PokedexScreen(
    state: PokedexUiState,
    actions: PokedexActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    var query by rememberSaveable { mutableStateOf(state.query) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    val f = state.filters

    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = "Pokédex") {
            DexIconButton(DexIcons.TypeChart, "Type chart", actions.onTypeChart)
            Box {
                DexIconButton(DexIcons.Sort, "Sort, currently ${f.sort.label}", { showSort = true })
                SortMenu(expanded = showSort, current = f.sort, onDismiss = { showSort = false }, onSelect = { actions.onSort(it); showSort = false })
            }
            DexIconButton(DexIcons.Settings, "Settings", actions.onSettings)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DexSearchField(
                        value = query,
                        onValueChange = { query = it; actions.onQueryChange(it) },
                        placeholder = "Search name or number",
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DexChip("Filters", { showFilters = true }, icon = DexIcons.Filter, badge = f.sheetCount)
                        FilterPill("Caught", f.caughtOnly, actions.onToggleCaughtOnly, multiSelect = true)
                        FilterPill("Favourites", f.favouritesOnly, actions.onToggleFavouritesOnly, multiSelect = true)
                        if (!f.isDefault) {
                            TextButton(onClick = actions.onClearFilters) { Text("Clear", style = DexTheme.type.labelLarge, color = colors.accent) }
                        }
                    }
                    if (state.totalCount > 0) {
                        val filtered = state.pokemon.size != state.totalCount
                        Text(
                            if (filtered) "%,d of %,d Pokémon".format(state.pokemon.size, state.totalCount) else "%,d Pokémon".format(state.totalCount),
                            style = DexTheme.type.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            when {
                state.failed -> item(span = { GridItemSpan(maxLineSpan) }) { ErrorCard(actions.onRetry) }
                state.loading -> items(8) { SkeletonCard() }
                state.pokemon.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyResults(query = query.trim(), filtered = !f.isDefault, onClear = {
                        query = ""
                        actions.onQueryChange("")
                        actions.onClearFilters()
                    })
                }
                else -> items(state.pokemon, key = { it.id }) { p ->
                    LaunchedEffect(p.id, p.hasDetails) { if (!p.hasDetails) actions.onPokemonShown(p.id) }
                    TypeCard(p, caught = p.id in state.caughtIds, onClick = { actions.onPokemonClick(p) }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(state, actions, onDismiss = { showFilters = false })
    }
}

@Composable
private fun SortMenu(expanded: Boolean, current: PokedexSort, onDismiss: () -> Unit, onSelect: (PokedexSort) -> Unit) {
    val colors = DexTheme.colors
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = colors.surface) {
        PokedexSort.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.label, style = DexTheme.type.bodyLarge, color = colors.text) },
                onClick = { onSelect(sort) },
                trailingIcon = {
                    if (sort == current) Icon(DexIcons.Check, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@Composable
private fun FiltersSheet(state: PokedexUiState, actions: PokedexActions, onDismiss: () -> Unit) {
    val colors = DexTheme.colors
    val f = state.filters
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Filters", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            Text("Type", style = DexTheme.type.titleMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PokemonType.entries.forEach { type ->
                    val c = type.colour()
                    FilterPill(
                        type.displayName,
                        selected = type in f.types,
                        onClick = { actions.onToggleType(type) },
                        multiSelect = true,
                        selectedContainer = c.container,
                        selectedContent = c.content,
                    )
                }
            }
            Text("Generation", style = DexTheme.type.titleMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Generation.entries.forEach { gen ->
                    FilterPill(gen.label.removePrefix("Generation "), gen in f.generations, { actions.onToggleGeneration(gen) }, multiSelect = true)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions.onClearFilters, enabled = !f.isDefault) {
                    Text("Clear all", style = DexTheme.type.labelLarge, color = if (f.isDefault) colors.textSecondary else colors.accent)
                }
                DexPrimaryButton(
                    if (state.pokemon.size == 1) "Show 1 Pokémon" else "Show %,d Pokémon".format(state.pokemon.size),
                    onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TypeCard(pokemon: Pokemon, caught: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = DexTheme.colors.isDark
    val c = pokemon.primaryType.colour()
    // Cards start neutral and blend into their type colour once types arrive.
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

@Composable
private fun EmptyResults(query: String, filtered: Boolean, onClear: () -> Unit) {
    val colors = DexTheme.colors
    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            when {
                query.isNotEmpty() && filtered -> "No Pokémon match '$query' with these filters."
                query.isNotEmpty() -> "No Pokémon match '$query'."
                else -> "No Pokémon match these filters."
            },
            style = DexTheme.type.bodyLarge,
            color = colors.textSecondary,
        )
        TextButton(onClick = onClear) { Text("Clear search and filters", style = DexTheme.type.labelLarge, color = colors.accent) }
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
            state = PokedexUiState(
                pokemon = SampleData.generationOne.take(6),
                totalCount = 1025,
                caughtIds = SampleData.caught,
                filters = PokedexFilters(types = setOf(PokemonType.Grass, PokemonType.Fire)),
                loading = false,
            ),
            actions = PokedexActions(),
        )
    }
}
