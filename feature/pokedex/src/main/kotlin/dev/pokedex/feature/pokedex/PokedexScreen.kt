package dev.pokedex.feature.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexChip
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexSearchField
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.TypeTag
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.Pokemon

/** The reference browser: every Pokémon in full, on cards in its type colour. */
@Composable
fun PokedexScreen(
    pokemon: List<Pokemon>,
    caughtIds: Set<Int>,
    totalCount: Int,
    onPokemonClick: (Pokemon) -> Unit,
    onTypeChart: () -> Unit,
    onSort: () -> Unit,
    onSettings: () -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val shown = pokemon.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.number.contains(query.trim()) }

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
                    Text("%,d Pokémon · all available offline".format(totalCount), style = DexTheme.type.labelMedium, color = colors.textSecondary)
                }
            }
            items(shown, key = { it.id }) { p -> TypeCard(p, caught = p.id in caughtIds, onClick = { onPokemonClick(p) }) }
        }
    }
}

@Composable
private fun TypeCard(pokemon: Pokemon, caught: Boolean, onClick: () -> Unit) {
    val dark = DexTheme.colors.isDark
    val c = pokemon.primaryType.colour()
    val bottom = if (dark) lerp(c.container, Color(0xFF110C1A), 0.2f) else lerp(c.container, Color.White, 0.28f)
    Box(
        Modifier
            .height(156.dp)
            .clip(DexShape.card)
            .background(Brush.verticalGradient(listOf(c.container, bottom)))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(pokemon.name).append(", number ").append(pokemon.number).append(", ")
                    append(pokemon.types.joinToString(" and ") { it.displayName })
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
            Text("#${pokemon.number}", style = DexTheme.type.numberSmall, color = c.content)
            Text(pokemon.name, style = DexTheme.type.titleLarge, color = c.content)
            pokemon.types.forEach { TypeTag(it, c.content) }
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

@DexPreviews
@Composable
private fun PokedexScreenPreview() {
    DexTheme {
        PokedexScreen(
            pokemon = SampleData.generationOne.take(6),
            caughtIds = SampleData.caught,
            totalCount = 1025,
            onPokemonClick = {}, onTypeChart = {}, onSort = {}, onSettings = {}, onFilters = {},
        )
    }
}
