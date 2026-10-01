package dev.pokedex.feature.pokedex

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.FilterPill
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.TypeBadge
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.TypeChart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TypeChartViewModel @Inject constructor(private val repository: PokemonRepository) : ViewModel() {
    val chart: StateFlow<TypeChart?> = repository.typeChart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        viewModelScope.launch { repository.refreshIndex() }
    }
}

@Composable
fun TypeChartRoute(onBack: () -> Unit, viewModel: TypeChartViewModel = hiltViewModel()) {
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    TypeChartScreen(chart, onBack, onRetry = viewModel::refresh)
}

/** Colours for multipliers. Each pairs with a symbol, so the chart never relies on colour alone. */
private data class FactorStyle(val background: Color, val content: Color, val symbol: String, val spoken: String)

private fun factorStyle(factor: Float): FactorStyle? = when {
    factor >= 2f -> FactorStyle(Color(0xFF1E7A46), Color.White, "2", "super effective")
    factor == 0f -> FactorStyle(Color(0xFF2B2438), Color.White, "0", "no effect")
    factor <= 0.5f -> FactorStyle(Color(0xFFB3261E), Color.White, "½", "not very effective")
    else -> null
}

private val Cell = 36.dp
private val Header = 64.dp

/** Phones get two focused views; the full grid is there for anyone who wants it (plan, section 3). */
private enum class ChartMode(val label: String) { Attack("Attack"), Defend("Defend"), Full("Full chart") }

@Composable
fun TypeChartScreen(chart: TypeChart?, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    var mode by rememberSaveable { mutableStateOf(ChartMode.Attack) }
    var selected by rememberSaveable { mutableStateOf<PokemonType?>(null) }
    var attacker by rememberSaveable { mutableStateOf(PokemonType.Fire) }
    var defenders by rememberSaveable { mutableStateOf(listOf(PokemonType.Water)) }
    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = "Type chart", navigation = { DexIconButton(DexIcons.Back, "Back", onBack) })
        when {
            chart == null -> Unit
            chart.isEmpty -> Column(Modifier.padding(16.dp)) {
                GlassCard {
                    Text("The type chart downloads with the Pokédex", style = DexTheme.type.titleMedium, color = colors.text)
                    Text("Connect once and it stays on your phone.", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
                    DexPrimaryButton("Try again", onRetry)
                }
            }
            else -> Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChartMode.entries.forEach { m -> FilterPill(m.label, selected = mode == m, onClick = { mode = m }) }
                }
                AnimatedContent(targetState = mode, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "chartMode") { m ->
                    when (m) {
                        ChartMode.Attack -> AttackView(chart, attacker, onPick = { attacker = it })
                        ChartMode.Defend -> DefendView(chart, defenders, onToggle = { type ->
                            defenders = when {
                                type in defenders && defenders.size > 1 -> defenders - type
                                type in defenders -> defenders
                                // Two types at most; a third replaces the oldest pick.
                                defenders.size == 2 -> listOf(defenders[1], type)
                                else -> defenders + type
                            }
                        })
                        ChartMode.Full -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Legend(Modifier.padding(horizontal = 16.dp))
                            Text(
                                "Rows attack, columns defend. Tap a type to highlight it.",
                                style = DexTheme.type.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Grid(chart, selected, onSelect = { selected = if (selected == it) null else it })
                        }
                    }
                }
            }
        }
    }
}

/** One attacking type: what it hits hard, what shrugs it off, and what it can't touch. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttackView(chart: TypeChart, attacker: PokemonType, onPick: (PokemonType) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TypePicker("Attacking type", selected = setOf(attacker), multiSelect = false, onPick = onPick)
        val factors = PokemonType.entries.associateWith { chart.factor(attacker, it) }
        GlassCard {
            Group("Super effective against", factors.filterValues { it >= 2f }.keys, "2×")
            Group("Not very effective against", factors.filterValues { it > 0f && it < 1f }.keys, "½×")
            Group("No effect on", factors.filterValues { it == 0f }.keys, "0×")
        }
    }
}

/** One or two defending types, as on a dual-type Pokémon: weaknesses multiply. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefendView(chart: TypeChart, defenders: List<PokemonType>, onToggle: (PokemonType) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TypePicker("Defending types (up to two)", selected = defenders.toSet(), multiSelect = true, onPick = onToggle)
        val defending = chart.defending(defenders)
        GlassCard {
            Group("4× weak to", defending.filterValues { it >= 4f }.keys, "4×")
            Group("2× weak to", defending.filterValues { it >= 2f && it < 4f }.keys, "2×")
            Group("Resists", defending.filterValues { it > 0.25f && it < 1f }.keys, "½×")
            Group("Strongly resists", defending.filterValues { it > 0f && it <= 0.25f }.keys, "¼×")
            Group("Immune to", defending.filterValues { it == 0f }.keys, "0×")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypePicker(title: String, selected: Set<PokemonType>, multiSelect: Boolean, onPick: (PokemonType) -> Unit) {
    Text(title, style = DexTheme.type.titleMedium, color = DexTheme.colors.text, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PokemonType.entries.forEach { type ->
            val c = type.colour()
            FilterPill(
                type.displayName,
                selected = type in selected,
                onClick = { onPick(type) },
                multiSelect = multiSelect,
                selectedContainer = c.container,
                selectedContent = c.content,
            )
        }
    }
}

/** A heading and its types; empty groups are left out rather than shown as "None". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(title: String, types: Collection<PokemonType>, factor: String) {
    if (types.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title ($factor)", style = DexTheme.type.titleMedium, color = DexTheme.colors.text, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { TypeBadge(it) }
        }
    }
}

@Composable
private fun Grid(chart: TypeChart, selected: PokemonType?, onSelect: (PokemonType) -> Unit) {
    val types = PokemonType.entries
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Spacer(Modifier.size(Header, Cell))
                types.forEach { TypeHeader(it, width = Cell, selected = it == selected, onSelect = onSelect) }
            }
            types.forEach { attacker ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TypeHeader(attacker, width = Header, selected = attacker == selected, onSelect = onSelect, full = true)
                    types.forEach { defender ->
                        val dimmed = selected != null && attacker != selected && defender != selected
                        FactorCell(attacker, defender, chart.factor(attacker, defender), dimmed)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeHeader(type: PokemonType, width: Dp, selected: Boolean, onSelect: (PokemonType) -> Unit, full: Boolean = false) {
    val c = type.colour()
    Box(
        Modifier
            .size(width, Cell)
            .clip(DexShape.small)
            .background(c.container)
            .clickable(role = Role.Button, onClickLabel = if (selected) "Clear highlight" else "Highlight") { onSelect(type) }
            .semantics { contentDescription = type.displayName },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (full) type.displayName else type.displayName.take(3),
            style = if (selected) DexTheme.type.labelMedium else DexTheme.type.labelSmall,
            color = c.content,
            maxLines = 1,
        )
    }
}

@Composable
private fun FactorCell(attacker: PokemonType, defender: PokemonType, factor: Float, dimmed: Boolean) {
    val colors = DexTheme.colors
    val style = factorStyle(factor)
    val background by animateColorAsState(
        (style?.background ?: colors.surfaceGlass).copy(alpha = if (dimmed) 0.25f else 1f),
        label = "cellBackground",
    )
    Box(
        Modifier
            .size(Cell)
            .clip(DexShape.small)
            .background(background)
            .semantics { contentDescription = "${attacker.displayName} on ${defender.displayName}: ${style?.spoken ?: "normal damage"}" },
        contentAlignment = Alignment.Center,
    ) {
        if (style != null) {
            Text(style.symbol, style = DexTheme.type.numberSmall, color = style.content.copy(alpha = if (dimmed) 0.4f else 1f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2f, 0.5f, 0f).forEach { factor ->
            val style = factorStyle(factor)!!
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(DexShape.small).background(style.background), contentAlignment = Alignment.Center) {
                    Text(style.symbol, style = DexTheme.type.numberSmall, color = style.content)
                }
                Text(style.spoken.replaceFirstChar { it.uppercase() }, style = DexTheme.type.labelMedium, color = DexTheme.colors.text)
            }
        }
    }
}

@DexPreviews
@Composable
private fun TypeChartPreview() {
    DexTheme {
        TypeChartScreen(
            chart = TypeChart(
                mapOf(
                    (PokemonType.Electric to PokemonType.Water) to 2f,
                    (PokemonType.Electric to PokemonType.Ground) to 0f,
                    (PokemonType.Fire to PokemonType.Water) to 0.5f,
                ),
            ),
            onBack = {},
            onRetry = {},
        )
    }
}
