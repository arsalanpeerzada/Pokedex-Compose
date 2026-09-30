@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dev.pokedex.feature.teams

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexProgressBar
import dev.pokedex.core.designsystem.component.DexSearchField
import dev.pokedex.core.designsystem.component.DexTextField
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.component.TypeBadge
import dev.pokedex.core.designsystem.component.TypeTag
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.pressScale
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.TEAM_SIZE
import dev.pokedex.core.model.Team
import dev.pokedex.core.model.TeamAnalysis
import dev.pokedex.core.model.TypeChart

@Composable
fun TeamEditorRoute(teamId: Long, onBack: () -> Unit, onOpenPokemon: (Int) -> Unit) {
    val viewModel = hiltViewModel<TeamEditorViewModel, TeamEditorViewModel.Factory>(
        key = "team-$teamId",
        creationCallback = { it.create(teamId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.missing) { if (state.missing) onBack() }
    val team = state.team ?: return
    TeamEditorScreen(
        team = team,
        analysis = state.analysis,
        pokedex = state.pokedex,
        onBack = onBack,
        onSetMember = viewModel::setMember,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onOpenPokemon = onOpenPokemon,
    )
}

@Composable
fun TeamEditorScreen(
    team: Team,
    analysis: TeamAnalysis?,
    pokedex: List<Pokemon>,
    onBack: () -> Unit,
    onSetMember: (slot: Int, pokemonId: Int?) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenPokemon: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DexTheme.colors
    var pickingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = team.name, navigation = { DexIconButton(DexIcons.Back, "Back", onBack) })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { renaming = true }) { Text("Rename", style = DexTheme.type.labelLarge, color = colors.accent) }
                TextButton(onClick = { confirmingDelete = true }) { Text("Delete team", style = DexTheme.type.labelLarge, color = colors.accent) }
            }
            team.members.chunked(3).forEachIndexed { row, members ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    members.forEachIndexed { column, member ->
                        val slot = row * 3 + column
                        SlotCard(
                            member = member,
                            slot = slot,
                            onAdd = { pickingSlot = slot },
                            onOpen = { member?.let { onOpenPokemon(it.id) } },
                            onRemove = { onSetMember(slot, null) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            AnalysisCard(team, analysis)
        }
    }

    pickingSlot?.let { slot ->
        PokemonPicker(
            pokedex = pokedex,
            alreadyIn = team.filled.map { it.id }.toSet(),
            onPick = { onSetMember(slot, it.id); pickingSlot = null },
            onDismiss = { pickingSlot = null },
        )
    }
    if (renaming) {
        RenameDialog(team.name, onConfirm = { onRename(it); renaming = false }, onDismiss = { renaming = false })
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${team.name}?", style = DexTheme.type.titleLarge, color = colors.text) },
            text = { Text("The team is removed from this phone. This can't be undone.", style = DexTheme.type.bodyMedium, color = colors.text) },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete() }) { Text("Delete", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel", color = colors.text) } },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun SlotCard(member: Pokemon?, slot: Int, onAdd: () -> Unit, onOpen: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    val interaction = remember { MutableInteractionSource() }
    AnimatedContent(
        targetState = member,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.8f)) togetherWith fadeOut(tween(120))
        },
        contentKey = { it?.id },
        label = "slot$slot",
        modifier = modifier,
    ) { pokemon ->
        if (pokemon == null) {
            Column(
                Modifier
                    .pressScale(interaction)
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(DexShape.album)
                    .border(1.5.dp, colors.outline, DexShape.album)
                    .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClickLabel = "Add a Pokémon to slot ${slot + 1}", onClick = onAdd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(DexIcons.Plus, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
                Text("Add", style = DexTheme.type.labelMedium, color = colors.textSecondary)
            }
        } else {
            val tint = pokemon.primaryType.colour().container.copy(alpha = if (colors.isDark) 0.3f else 0.22f)
            Box(
                Modifier
                    .pressScale(interaction)
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(DexShape.album)
                    .background(colors.surfaceGlass)
                    .background(tint)
                    .border(1.dp, colors.surfaceGlassEdge, DexShape.album)
                    .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClickLabel = "Open ${pokemon.name}", onClick = onOpen),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 6.dp, end = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PokemonArtwork(pokemon, size = 72.dp, glowAlpha = 0.35f)
                    Text(pokemon.name, style = DexTheme.type.labelLarge, color = colors.text, maxLines = 1, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        pokemon.types.forEach { TypeTag(it, colors.text) }
                    }
                }
                DexIconButton(DexIcons.Close, "Remove ${pokemon.name}", onRemove, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun AnalysisCard(team: Team, analysis: TeamAnalysis?) {
    val colors = DexTheme.colors
    GlassCard(modifier = Modifier.animateContentSize()) {
        Text("Type check", style = DexTheme.type.titleLarge, color = colors.text, modifier = Modifier.semantics { heading() })
        when {
            team.filled.isEmpty() -> Text("Add Pokémon to see how the team holds up.", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
            analysis == null -> Text("The type check appears once the type chart has downloaded.", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
            else -> {
                Text("Shared weaknesses", style = DexTheme.type.titleMedium, color = colors.text)
                val shared = analysis.sharedWeaknesses()
                if (shared.isEmpty()) {
                    Text("None. No type hits two or more members hard without someone resisting it.", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shared.forEach { (type, count) -> CountBadge(type, "$count weak") }
                    }
                }
                Text("Attack coverage", style = DexTheme.type.titleMedium, color = colors.text, modifier = Modifier.padding(top = 4.dp))
                val covered = analysis.covered.size
                Text(
                    "Their own types hit $covered of ${PokemonType.entries.size} types for double damage.",
                    style = DexTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
                DexProgressBar(covered / PokemonType.entries.size.toFloat(), Modifier.fillMaxWidth())
                if (analysis.uncovered.isNotEmpty()) {
                    Text("Not covered", style = DexTheme.type.labelMedium, color = colors.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        analysis.uncovered.forEach { TypeBadge(it) }
                    }
                }
                Text("Based on types only; moves aren't counted yet.", style = DexTheme.type.labelSmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun CountBadge(type: PokemonType, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        TypeBadge(type)
        Text(label, style = DexTheme.type.labelMedium, color = DexTheme.colors.text)
    }
}

@Composable
private fun PokemonPicker(pokedex: List<Pokemon>, alreadyIn: Set<Int>, onPick: (Pokemon) -> Unit, onDismiss: () -> Unit) {
    val colors = DexTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val shown = if (q.isEmpty()) pokedex else pokedex.filter { it.name.contains(q, ignoreCase = true) || it.number.contains(q) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add a Pokémon", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            DexSearchField(query, { query = it }, placeholder = "Search name or number")
            LazyColumn(Modifier.fillMaxWidth().height(460.dp)) {
                items(shown, key = { it.id }) { p ->
                    val inTeam = p.id in alreadyIn
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(DexShape.large)
                            .clickable(enabled = !inTeam, role = Role.Button, onClickLabel = "Add ${p.name}") { onPick(p) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${p.name}, number ${p.number}" + if (inTeam) ", already in the team" else ""
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PokemonAvatar(p, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = DexTheme.type.titleMedium, color = if (inTeam) colors.textSecondary else colors.text)
                            Text("#${p.number}", style = DexTheme.type.numberSmall, color = colors.textSecondary)
                        }
                        if (inTeam) Text("In team", style = DexTheme.type.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = DexTheme.colors
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename team", style = DexTheme.type.titleLarge, color = colors.text) },
        text = {
            DexTextField(name, { name = it.take(40) }, placeholder = "Team name", capitalization = KeyboardCapitalization.Words, onImeAction = { onConfirm(name) })
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.text) } },
        containerColor = colors.surface,
    )
}

@DexPreviews
@Composable
private fun TeamEditorPreview() {
    val g = SampleData.generationOne
    val team = Team(1, "Starters", listOf(g[0], g[3], g[6], SampleData.pikachu, null, null))
    val chart = TypeChart(
        mapOf(
            (PokemonType.Electric to PokemonType.Water) to 2f,
            (PokemonType.Ground to PokemonType.Electric) to 2f,
            (PokemonType.Ground to PokemonType.Fire) to 2f,
            (PokemonType.Water to PokemonType.Fire) to 2f,
            (PokemonType.Fire to PokemonType.Grass) to 2f,
            (PokemonType.Grass to PokemonType.Water) to 2f,
        ),
    )
    DexTheme {
        TeamEditorScreen(team, TeamAnalysis.of(team.filled, chart), emptyList(), {}, { _, _ -> }, {}, {}, {})
    }
}
