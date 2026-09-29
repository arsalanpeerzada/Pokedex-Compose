package dev.pokedex.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexSceneButton
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.PokemonSilhouette
import dev.pokedex.core.designsystem.component.SceneCard
import dev.pokedex.core.designsystem.component.SceneChip
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.SceneSilhouette
import dev.pokedex.core.designsystem.theme.brush
import dev.pokedex.core.model.DailyPick
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherScene

/** Today, before the reveal: silhouette, four mixed choices and a weather-only hint. */
@Composable
fun TodayScreen(
    pick: DailyPick,
    onGuess: (Pokemon) -> Unit,
    onUseHint: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    val weather = pick.weather
    Box(modifier.fillMaxSize().background(weather.scene.brush(colors.isDark))) {
        if (weather.scene == WeatherScene.Storm) Lightning(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            DexTopBar(
                contentColor = colors.onScene,
                leading = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(DexIcons.Pin, contentDescription = null, tint = colors.onScene, modifier = Modifier.size(18.dp))
                            Text(pick.city, style = DexTheme.type.titleLarge, color = colors.onScene, modifier = Modifier.padding(start = 6.dp).semantics { heading() })
                        }
                        Text(pick.dateLabel, style = DexTheme.type.labelMedium, color = colors.onScene)
                    }
                },
            ) {
                DexIconButton(DexIcons.Settings, "Settings", onSettings, tint = colors.onScene)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SceneChip("${weather.description} · ${weather.temperatureCelsius}°C", if (weather.scene == WeatherScene.Storm) DexIcons.Storm else DexIcons.Sun)
                    SceneChip(if (weather.isNight) "Night" else "Day", if (weather.isNight) DexIcons.Moon else DexIcons.Sun)
                }
                SceneCard {
                    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(220.dp)
                                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))),
                        )
                        PokemonSilhouette(pick.answer, size = 214.dp, colour = SceneSilhouette)
                    }
                    Text("Who's today's Pokémon?", style = DexTheme.type.titleLarge, color = colors.onScene)
                    Text(pick.weatherHint, style = DexTheme.type.labelLarge, color = colors.onSceneSecondary)
                }
                pick.choices.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { choice -> DexSceneButton(choice.name, onClick = { onGuess(choice) }, modifier = Modifier.weight(1f)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${pick.hintsLeft} hints left",
                        style = DexTheme.type.labelMedium,
                        color = colors.onHighlight,
                        modifier = Modifier.clip(DexShape.full).background(colors.highlight).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    TextButton(onClick = onUseHint) { Text("Use a hint", style = DexTheme.type.labelLarge, color = colors.onScene) }
                }
                Text("Weather data attribution sits here", style = DexTheme.type.labelSmall, color = colors.onSceneSecondary)
            }
        }
    }
}

/** Decorative lightning, drawn behind the content. */
@Composable
private fun Lightning(modifier: Modifier) {
    Canvas(modifier) {
        bolt(x = size.width - 90.dp.toPx(), y = 120.dp.toPx(), height = 72.dp.toPx())
        bolt(x = size.width - 60.dp.toPx(), y = size.height - 190.dp.toPx(), height = 40.dp.toPx())
    }
}

private fun DrawScope.bolt(x: Float, y: Float, height: Float) {
    val s = height / 64f
    val path = Path().apply {
        moveTo(30f * s, 0f); lineTo(8f * s, 34f * s); lineTo(24f * s, 34f * s); lineTo(14f * s, 64f * s)
        lineTo(44f * s, 24f * s); lineTo(28f * s, 24f * s); lineTo(38f * s, 0f); close()
    }
    translate(x, y) {
        drawPath(path, Color(0xFFFFD23F).copy(alpha = 0.25f), style = Stroke(width = 10f * s))
        drawPath(path, Color(0xFFFFD23F).copy(alpha = 0.9f))
    }
}

@DexPreviews
@Composable
private fun TodayScreenPreview() {
    DexTheme { TodayScreen(SampleData.today, onGuess = {}, onUseHint = {}, onSettings = {}) }
}
