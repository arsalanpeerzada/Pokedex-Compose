package dev.pokedex.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.detailPalette
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType

private val Tabs = listOf("About", "Stats", "Evolution", "Matchups", "Forms")

/** One detail layout, shared by the Pokédex and Today. Colours come from the Pokémon itself. */
@Composable
fun PokemonDetailScreen(
    pokemon: Pokemon,
    caught: Boolean,
    onBack: () -> Unit,
    onToggleCaught: () -> Unit,
    onAddToTeam: () -> Unit,
    onFavourite: () -> Unit,
    onShare: () -> Unit,
    onPlayCry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DexTheme.colors
    val palette = pokemon.primaryType.detailPalette(colors.isDark)
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().background(palette.background)) {
        DexTopBar(
            contentColor = palette.content,
            navigation = { DexIconButton(DexIcons.Back, "Back", onBack, tint = palette.content, glass = true) },
        ) {
            DexIconButton(DexIcons.Heart, "Add to favourites", onFavourite, tint = palette.content, glass = true)
            DexIconButton(DexIcons.Share, "Share", onShare, tint = palette.content, glass = true)
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
            PokemonArtwork(pokemon, size = 230.dp, glowAlpha = if (colors.isDark) 0.5f else 0.6f)
            Text("#${pokemon.number}", style = DexTheme.type.numberLarge, color = palette.contentSecondary)
            Text(pokemon.name, style = DexTheme.type.displayLarge, color = palette.content)
            pokemon.category?.let { Text(it, style = DexTheme.type.labelLarge, color = palette.contentSecondary) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pokemon.types.forEach { TypeBadge(it, icon = if (it == PokemonType.Electric) DexIcons.Bolt else null) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DexGlassButton(
                    text = if (caught) "Caught" else "Mark as caught",
                    onClick = onToggleCaught,
                    icon = if (caught) DexIcons.Check else DexIcons.Plus,
                    contentColor = palette.content,
                    modifier = Modifier.weight(1f),
                )
                DexPrimaryButton("Add to team", onAddToTeam, icon = DexIcons.Plus, modifier = Modifier.weight(1f))
            }
            TabRow(selected = tab, onSelect = { tab = it }, content = palette.content, contentSecondary = palette.contentSecondary)
            GlassCard {
                Text(
                    "The latest Pokédex entry appears here, with the name of the game it comes from.",
                    style = DexTheme.type.bodyLarge,
                    color = palette.content,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact("Height", pokemon.heightMetres?.let { "%.1f m".format(it) }, palette.content, palette.contentSecondary, Modifier.weight(1f))
                    Fact("Weight", pokemon.weightKilograms?.let { "%.1f kg".format(it) }, palette.content, palette.contentSecondary, Modifier.weight(1f))
                    Fact("Catch rate", pokemon.catchRate?.toString(), palette.content, palette.contentSecondary, Modifier.weight(1f))
                }
            }
            GlassCard(contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledIconButton(
                        onClick = onPlayCry,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                        modifier = Modifier.size(48.dp),
                    ) { Icon(DexIcons.Play, contentDescription = "Play ${pokemon.name}'s cry", modifier = Modifier.size(18.dp)) }
                    Column {
                        Text("Cry", style = DexTheme.type.titleMedium, color = palette.content)
                        Text("Modern · Original", style = DexTheme.type.labelMedium, color = palette.contentSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRow(selected: Int, onSelect: (Int) -> Unit, content: Color, contentSecondary: Color) {
    val colors = DexTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DexShape.full)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassEdge, DexShape.full)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tabs.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(DexShape.full)
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .selectable(selected = isSelected, onClick = { onSelect(i) }, role = Role.Tab)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = DexTheme.type.labelMedium, color = if (isSelected) content else contentSecondary, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String?, content: Color, contentSecondary: Color, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    Column(
        modifier
            .clip(DexShape.large)
            .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = DexTheme.type.labelSmall, color = contentSecondary)
        Text(value ?: "Unknown", style = DexTheme.type.titleMedium, color = content)
    }
}

@DexPreviews
@Composable
private fun PokemonDetailScreenPreview() {
    DexTheme {
        PokemonDetailScreen(
            SampleData.pikachu, caught = false,
            onBack = {}, onToggleCaught = {}, onAddToTeam = {}, onFavourite = {}, onShare = {}, onPlayCry = {},
        )
    }
}
