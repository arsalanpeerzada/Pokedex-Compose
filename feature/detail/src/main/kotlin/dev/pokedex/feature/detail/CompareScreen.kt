@file:OptIn(ExperimentalMaterial3Api::class)

package dev.pokedex.feature.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexSearchField
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.component.TypeBadge
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.BaseStats
import dev.pokedex.core.model.Measures
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompareUiState(
    val first: Pokemon? = null,
    val second: Pokemon? = null,
    val chart: TypeChart = TypeChart.Empty,
    val units: Units = Units.Metric,
    val pokedex: List<Pokemon> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CompareViewModel.Factory::class)
class CompareViewModel @AssistedInject constructor(
    @Assisted firstId: Int,
    private val repository: PokemonRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    private val secondId = MutableStateFlow<Int?>(null)

    val state: StateFlow<CompareUiState> = combine(
        repository.pokemon(firstId),
        secondId.flatMapLatest { id -> if (id == null) flowOf(null) else repository.pokemon(id) },
        repository.typeChart(),
        preferences.preferences.map { it.units },
        repository.pokedex(),
    ) { first, second, chart, units, pokedex -> CompareUiState(first, second, chart, units, pokedex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState())

    init {
        viewModelScope.launch { repository.ensureDetails(firstId) }
    }

    fun choose(id: Int) {
        secondId.value = id
        viewModelScope.launch { repository.ensureDetails(id) }
    }

    @AssistedFactory
    interface Factory {
        fun create(firstId: Int): CompareViewModel
    }
}

@Composable
fun CompareRoute(firstId: Int, onBack: () -> Unit) {
    val viewModel = hiltViewModel<CompareViewModel, CompareViewModel.Factory>(key = "compare-$firstId", creationCallback = { it.create(firstId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompareScreen(state, onBack, onChoose = viewModel::choose)
}

/** Two Pokémon side by side: types, size, base stats, and how their types fare against each other. */
@Composable
fun CompareScreen(state: CompareUiState, onBack: () -> Unit, onChoose: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    var picking by rememberSaveable { mutableStateOf(state.second == null) }
    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = "Compare", navigation = { DexIconButton(DexIcons.Back, "Back", onBack) })
        val a = state.first ?: return@Column
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Contender(a, state.units, Modifier.weight(1f))
                AnimatedContent(
                    targetState = state.second,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    contentKey = { it?.id },
                    label = "secondContender",
                    modifier = Modifier.weight(1f),
                ) { b ->
                    if (b == null) {
                        ChooseSlot(onClick = { picking = true })
                    } else {
                        Contender(b, state.units, Modifier.clickable(role = Role.Button, onClickLabel = "Choose another Pokémon") { picking = true })
                    }
                }
            }
            val b = state.second
            if (b != null) {
                StatsComparison(a, b)
                if (!state.chart.isEmpty && a.types.isNotEmpty() && b.types.isNotEmpty()) TypeFaceOff(a, b, state.chart)
            }
        }
    }
    if (picking) {
        ComparePicker(
            pokedex = state.pokedex,
            exclude = state.first?.id,
            onPick = { onChoose(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun Contender(pokemon: Pokemon, units: Units, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(DexShape.card)
            .background(pokemon.primaryType.colour().container.copy(alpha = if (colors.isDark) 0.28f else 0.2f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PokemonArtwork(pokemon, size = 112.dp, glowAlpha = 0.4f)
        Text(pokemon.name, style = DexTheme.type.titleMedium, color = colors.text, textAlign = TextAlign.Center, maxLines = 2)
        Text("#${pokemon.number}", style = DexTheme.type.numberSmall, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pokemon.types.forEach { TypeBadge(it) } }
        val size = listOfNotNull(pokemon.heightMetres?.let { Measures.height(it, units) }, pokemon.weightKilograms?.let { Measures.weight(it, units) })
        if (size.isNotEmpty()) Text(size.joinToString(" · "), style = DexTheme.type.labelMedium, color = colors.textSecondary)
    }
}

@Composable
private fun ChooseSlot(onClick: () -> Unit) {
    val colors = DexTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(DexShape.card)
            .border(1.5.dp, colors.outline, DexShape.card)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(DexIcons.Plus, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(32.dp))
        Text("Choose a Pokémon", style = DexTheme.type.labelLarge, color = colors.textSecondary)
    }
}

/** Mirrored bars: the first Pokémon grows left, the second grows right. The higher value is bold. */
@Composable
private fun StatsComparison(a: Pokemon, b: Pokemon) {
    val colors = DexTheme.colors
    GlassCard {
        Text("Base stats", style = DexTheme.type.titleLarge, color = colors.text, modifier = Modifier.semantics { heading() })
        val sa = a.stats
        val sb = b.stats
        if (sa == null || sb == null) {
            Text("Loading stats…", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
            return@GlassCard
        }
        val rows = sa.asList().zip(sb.asList()) + listOf(("Total" to sa.total) to ("Total" to sb.total))
        rows.forEachIndexed { i, (left, right) ->
            val max = if (left.first == "Total") maxOf(left.second, right.second).toFloat() else 255f
            StatPair(left.first, left.second, right.second, max, a.primaryType.colour().container, b.primaryType.colour().container, i, a.name, b.name)
        }
    }
}

@Composable
private fun StatPair(label: String, left: Int, right: Int, max: Float, leftColour: Color, rightColour: Color, index: Int, leftName: String, rightName: String) {
    val colors = DexTheme.colors
    val grow = remember { Animatable(0f) }
    LaunchedEffect(left, right) { grow.animateTo(1f, tween(700, delayMillis = 50 * index, easing = FastOutSlowInEasing)) }
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $leftName $left, $rightName $right" },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = DexTheme.type.labelMedium, color = colors.textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(left.toString(), style = DexTheme.type.numberSmall.copy(fontWeight = if (left > right) FontWeight.Bold else FontWeight.Normal), color = colors.text, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            Box(Modifier.weight(1f).height(10.dp).clip(DexShape.full).background(colors.track), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((left / max * grow.value).coerceIn(0f, 1f)).clip(DexShape.full).background(leftColour))
            }
            Box(Modifier.weight(1f).height(10.dp).clip(DexShape.full).background(colors.track), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((right / max * grow.value).coerceIn(0f, 1f)).clip(DexShape.full).background(rightColour))
            }
            Text(right.toString(), style = DexTheme.type.numberSmall.copy(fontWeight = if (right > left) FontWeight.Bold else FontWeight.Normal), color = colors.text, modifier = Modifier.width(36.dp))
        }
    }
}

/** How hard each one's own types hit the other, at best. Types only; moves aren't counted. */
@Composable
private fun TypeFaceOff(a: Pokemon, b: Pokemon, chart: TypeChart) {
    val colors = DexTheme.colors
    fun best(attacker: Pokemon, defender: Pokemon): Pair<String, Float> {
        val against = chart.defending(defender.types)
        val top = attacker.types.maxBy { against[it] ?: 1f }
        return top.displayName to (against[top] ?: 1f)
    }
    GlassCard {
        Text("Type face-off", style = DexTheme.type.titleLarge, color = colors.text, modifier = Modifier.semantics { heading() })
        listOf(a to b, b to a).forEach { (attacker, defender) ->
            val (type, factor) = best(attacker, defender)
            Text(
                "${attacker.name}'s best type, $type, hits ${defender.name} for ${multiplier(factor)}.",
                style = DexTheme.type.bodyLarge,
                color = colors.text,
            )
        }
        Text("Based on types only; moves aren't counted.", style = DexTheme.type.labelSmall, color = colors.textSecondary)
    }
}

private fun multiplier(factor: Float): String = when (factor) {
    0f -> "no damage"
    0.25f -> "¼× damage"
    0.5f -> "½× damage"
    1f -> "normal damage"
    else -> "${factor.toInt()}× damage"
}

@Composable
private fun ComparePicker(pokedex: List<Pokemon>, exclude: Int?, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = DexTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val shown = pokedex.filter { it.id != exclude && (q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.number.contains(q)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Compare with", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            DexSearchField(query, { query = it }, placeholder = "Search name or number")
            LazyColumn(Modifier.fillMaxWidth().height(460.dp)) {
                items(shown, key = { it.id }) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(DexShape.large)
                            .clickable(role = Role.Button, onClickLabel = "Compare with ${p.name}") { onPick(p.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PokemonAvatar(p, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = DexTheme.type.titleMedium, color = colors.text)
                            Text("#${p.number}", style = DexTheme.type.numberSmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@DexPreviews
@Composable
private fun ComparePreview() {
    val stats = BaseStats(35, 55, 40, 50, 50, 90)
    DexTheme {
        CompareScreen(
            CompareUiState(
                first = SampleData.pikachu.copy(stats = stats),
                second = SampleData.eevee.copy(heightMetres = 0.3, weightKilograms = 6.5, stats = BaseStats(55, 55, 50, 45, 65, 55)),
            ),
            onBack = {},
            onChoose = {},
        )
    }
}
