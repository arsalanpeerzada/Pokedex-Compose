package dev.pokedex.feature.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.TEAM_SIZE
import dev.pokedex.core.model.Team
import dev.pokedex.core.model.TeamAnalysis
import dev.pokedex.core.model.TypeChart

@Composable
fun TeamsRoute(
    onOpenTeam: (Long) -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: TeamsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TeamsScreen(
        state = state,
        onOpenTeam = onOpenTeam,
        onNewTeam = { viewModel.create(onOpenTeam) },
        onSettings = onSettings,
        contentPadding = contentPadding,
    )
}

/** Every saved team, each with its six slots and a one-line verdict. */
@Composable
fun TeamsScreen(
    state: TeamsUiState,
    onOpenTeam: (Long) -> Unit,
    onNewTeam: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    Column(modifier.fillMaxSize().background(colors.brandBrush())) {
        DexTopBar(title = "Teams") {
            DexIconButton(DexIcons.Settings, "Settings", onSettings)
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.loading && state.teams.isEmpty()) {
                item(key = "empty") {
                    GlassCard(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.animateItem()) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                            SampleData.generationOne.filter { it.id in setOf(1, 4, 7) }.forEach { PokemonAvatar(it, 56.dp) }
                        }
                        Text("Build your first team", style = DexTheme.type.titleLarge, color = colors.text)
                        Text(
                            "Pick up to six Pokémon and see their shared weaknesses and which types they hit hard.",
                            style = DexTheme.type.bodyMedium,
                            color = colors.textSecondary,
                        )
                        DexPrimaryButton("New team", onNewTeam, icon = DexIcons.Plus, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else if (state.teams.isNotEmpty()) {
                item(key = "new") {
                    DexPrimaryButton("New team", onNewTeam, icon = DexIcons.Plus, modifier = Modifier.fillMaxWidth())
                }
            }
            items(state.teams, key = { it.id }) { team ->
                TeamCard(team, state.chart, onClick = { onOpenTeam(team.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun TeamCard(team: Team, chart: TypeChart, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    val verdict = when {
        team.filled.isEmpty() -> "Empty. Tap to add Pokémon."
        chart.isEmpty -> "${team.filled.size} of $TEAM_SIZE"
        else -> TeamAnalysis.of(team.filled, chart).sharedWeaknesses().firstOrNull()
            ?.let { (type, count) -> "$count members are weak to ${type.displayName}" }
            ?: "No shared weaknesses"
    }
    GlassCard(
        modifier = modifier
            .clip(DexShape.card)
            .clickable(role = Role.Button, onClickLabel = "Open team", onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(team.name, style = DexTheme.type.titleLarge, color = colors.text, modifier = Modifier.weight(1f))
            Text("${team.filled.size} / $TEAM_SIZE", style = DexTheme.type.numberSmall, color = colors.textSecondary)
        }
        SlotStrip(team.members)
        Text(verdict, style = DexTheme.type.labelMedium, color = colors.textSecondary)
    }
}

/** Six small circles: an avatar for each member, an outline for each empty slot. */
@Composable
internal fun SlotStrip(members: List<Pokemon?>) {
    val colors = DexTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = members.filterNotNull().joinToString(", ") { it.name }.ifEmpty { "No members yet" }
        },
    ) {
        members.forEach { member ->
            if (member != null) {
                PokemonAvatar(member, 44.dp)
            } else {
                Box(Modifier.size(44.dp).clip(DexShape.full).border(1.5.dp, colors.outline, DexShape.full))
            }
        }
    }
}

@DexPreviews
@Composable
private fun TeamsPreview() {
    val g = SampleData.generationOne
    DexTheme {
        TeamsScreen(
            state = TeamsUiState(
                loading = false,
                teams = listOf(
                    Team(1, "Starters", listOf(g[0], g[3], g[6], null, null, null)),
                    Team(2, "Bug catchers", listOf(g[9], g[11], g[14], SampleData.pikachu, null, null)),
                ),
            ),
            onOpenTeam = {},
            onNewTeam = {},
            onSettings = {},
        )
    }
}

@DexPreviews
@Composable
private fun TeamsEmptyPreview() {
    DexTheme { TeamsScreen(TeamsUiState(loading = false), onOpenTeam = {}, onNewTeam = {}, onSettings = {}) }
}
