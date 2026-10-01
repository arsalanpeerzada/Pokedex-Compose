@file:OptIn(ExperimentalLayoutApi::class)

package dev.pokedex.feature.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.component.TypeBadge
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.EvolutionStep
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.TypeChart

/** The highest base stat in the games is 255, so bars share one scale across every Pokémon. */
private const val MAX_BASE_STAT = 255f

@Composable
internal fun StatsCard(pokemon: Pokemon, content: Color, secondary: Color) {
    GlassCard {
        val stats = pokemon.stats
        if (stats == null) {
            Text(if (pokemon.hasDetails) "No stats yet." else "Loading stats…", style = DexTheme.type.bodyLarge, color = secondary)
            return@GlassCard
        }
        val bar = pokemon.primaryType.colour().container
        stats.asList().forEachIndexed { index, (label, value) -> StatRow(label, value, index, bar, content, secondary) }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = DexTheme.type.titleMedium, color = content, modifier = Modifier.weight(1f))
            Text(stats.total.toString(), style = DexTheme.type.numberLarge, color = content)
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, index: Int, bar: Color, content: Color, secondary: Color) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(value) { fill.animateTo(value / MAX_BASE_STAT, tween(700, delayMillis = 60 * index, easing = FastOutSlowInEasing)) }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$label $value" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = DexTheme.type.labelMedium, color = secondary, modifier = Modifier.width(64.dp))
        Text(value.toString(), style = DexTheme.type.numberSmall, color = content, textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(DexShape.full).background(DexTheme.colors.track)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value.coerceIn(0f, 1f)).clip(DexShape.full).background(bar))
        }
    }
}

@Composable
internal fun MatchupsCard(pokemon: Pokemon, chart: TypeChart, content: Color, secondary: Color) {
    GlassCard {
        if (chart.isEmpty || pokemon.types.isEmpty()) {
            Text("Matchups appear once the type chart has downloaded.", style = DexTheme.type.bodyLarge, color = secondary)
            return@GlassCard
        }
        val defending = chart.defending(pokemon.types)
        val weak = defending.filterValues { it > 1f }.entries.sortedByDescending { it.value }
        val resists = defending.filterValues { it > 0f && it < 1f }.entries.sortedBy { it.value }
        val immune = defending.filterValues { it == 0f }.keys
        MatchupGroup("Weak to", weak.map { it.key to it.value }, content, secondary)
        MatchupGroup("Resists", resists.map { it.key to it.value }, content, secondary)
        MatchupGroup("Immune to", immune.map { it to 0f }, content, secondary)
    }
}

@Composable
private fun MatchupGroup(title: String, entries: List<Pair<PokemonType, Float>>, content: Color, secondary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = DexTheme.type.titleMedium, color = content, modifier = Modifier.semantics { heading() })
        if (entries.isEmpty()) {
            Text("None", style = DexTheme.type.bodyMedium, color = secondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { (type, factor) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                    ) {
                        TypeBadge(type)
                        Text(factorLabel(factor), style = DexTheme.type.numberSmall, color = content)
                    }
                }
            }
        }
    }
}

private fun factorLabel(factor: Float): String = when (factor) {
    0f -> "0×"
    0.25f -> "¼×"
    0.5f -> "½×"
    else -> "${factor.toInt()}×"
}

/** A reference person for scale. Their height is a stated assumption, not an average. */
private const val PERSON_METRES = 1.7

/** The Pokémon next to a 1.7 m person, both to scale. Artwork has some padding, so it's approximate. */
@Composable
internal fun SizeComparison(pokemon: Pokemon, content: Color, secondary: Color) {
    val height = pokemon.heightMetres ?: return
    val tallest = maxOf(height, PERSON_METRES)
    val area = 140.dp
    val personHeight = area * (PERSON_METRES / tallest).toFloat()
    val pokemonHeight = (area * (height / tallest).toFloat()).coerceAtLeast(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Size", style = DexTheme.type.titleMedium, color = content, modifier = Modifier.semantics { heading() })
        Row(
            Modifier
                .fillMaxWidth()
                .height(area + 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${pokemon.name} is %.1f metres tall, next to a %.1f metre person".format(height, PERSON_METRES)
                },
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            PersonFigure(Modifier.height(personHeight).width(personHeight * 0.38f), content.copy(alpha = 0.55f))
            AsyncImage(
                model = pokemon.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(pokemonHeight),
            )
        }
        Text(
            "To scale beside a %.1f m person (approximate).".format(PERSON_METRES),
            style = DexTheme.type.labelSmall,
            color = secondary,
        )
    }
}

/** A simple standing figure: head, body and legs. */
@Composable
private fun PersonFigure(modifier: Modifier, colour: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val head = w * 0.28f
        drawCircle(colour, radius = head, center = Offset(w / 2, head))
        drawRoundRect(colour, topLeft = Offset(w * 0.12f, head * 2.2f), size = Size(w * 0.76f, h * 0.42f), cornerRadius = CornerRadius(w * 0.3f))
        val legTop = head * 2.2f + h * 0.38f
        drawRoundRect(colour, topLeft = Offset(w * 0.2f, legTop), size = Size(w * 0.26f, h - legTop), cornerRadius = CornerRadius(w * 0.13f))
        drawRoundRect(colour, topLeft = Offset(w * 0.54f, legTop), size = Size(w * 0.26f, h - legTop), cornerRadius = CornerRadius(w * 0.13f))
    }
}

@Composable
internal fun FormsCard(pokemon: Pokemon, content: Color, secondary: Color) {
    GlassCard {
        val forms = pokemon.forms
        when {
            forms == null -> Text("Loading forms…", style = DexTheme.type.bodyLarge, color = secondary)
            forms.isEmpty() -> Text("${pokemon.name} has no alternate forms.", style = DexTheme.type.bodyLarge, color = content)
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                forms.forEach { form ->
                    Column(
                        Modifier.width(132.dp).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AsyncImage(model = form.artworkUrl, contentDescription = null, modifier = Modifier.size(112.dp))
                        Text(form.label, style = DexTheme.type.labelMedium, color = content, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EvolutionCard(pokemon: Pokemon, steps: List<EvolutionStep>, onOpen: (Int) -> Unit, content: Color, secondary: Color) {
    GlassCard(spacing = 8.dp) {
        when {
            steps.isEmpty() && (!pokemon.hasDetails || pokemon.evolutionChainId != null) ->
                Text("Loading the evolution chain…", style = DexTheme.type.bodyLarge, color = secondary)
            steps.size <= 1 -> Text("${pokemon.name} doesn't evolve.", style = DexTheme.type.bodyLarge, color = content)
            else -> {
                val names = steps.associate { it.speciesId to it.name }
                steps.forEach { step ->
                    EvolutionRow(
                        step = step,
                        fromName = step.fromId?.let(names::get),
                        current = step.speciesId == pokemon.id,
                        onOpen = onOpen,
                        content = content,
                        secondary = secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionRow(step: EvolutionStep, fromName: String?, current: Boolean, onOpen: (Int) -> Unit, content: Color, secondary: Color) {
    val subtitle = when {
        step.fromId == null -> "First stage"
        step.method != null && fromName != null -> "From $fromName · ${step.method}"
        fromName != null -> "From $fromName"
        else -> step.method.orEmpty()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DexShape.large)
            .background(if (current) Color.White.copy(alpha = if (DexTheme.colors.isDark) 0.1f else 0.55f) else Color.Transparent)
            .clickable(enabled = !current, role = Role.Button, onClickLabel = "Open ${step.name}") { onOpen(step.speciesId) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PokemonAvatar(Pokemon(step.speciesId, step.name, emptyList()), 48.dp)
        Column(Modifier.weight(1f)) {
            Text(step.name, style = DexTheme.type.titleMedium, color = content)
            Text(subtitle, style = DexTheme.type.labelMedium, color = secondary)
        }
        if (current) Text("This one", style = DexTheme.type.labelSmall, color = secondary)
    }
}
