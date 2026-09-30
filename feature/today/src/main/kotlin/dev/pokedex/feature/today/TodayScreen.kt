package dev.pokedex.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexSceneButton
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.component.PokemonSilhouette
import dev.pokedex.core.designsystem.component.SceneCard
import dev.pokedex.core.designsystem.component.SceneChip
import dev.pokedex.core.designsystem.component.rememberNotificationPermission
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.StaggeredEntrance
import dev.pokedex.core.designsystem.motion.rememberPulse
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.SceneSilhouette
import dev.pokedex.core.designsystem.theme.brush
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.StoryEntry
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TodayRoute(
    onOpenEntry: (Pokemon) -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val askForNotifications = rememberNotificationPermission { granted -> viewModel.answerReminder(granted) }
    TodayScreen(
        state = state,
        onGuess = { viewModel.guess(it) },
        onUseHint = viewModel::useHint,
        onRetry = viewModel::refresh,
        onOpenEntry = onOpenEntry,
        onSettings = onSettings,
        onReminder = { yes -> if (yes) askForNotifications() else viewModel.answerReminder(false) },
        contentPadding = contentPadding,
    )
}

/** Today: guess the silhouette from four choices, then the reveal. */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onGuess: (Pokemon) -> Unit,
    onUseHint: () -> Unit,
    onRetry: () -> Unit,
    onOpenEntry: (Pokemon) -> Unit,
    onSettings: () -> Unit,
    onReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = DexTheme.colors
    Box(modifier.fillMaxSize().background(state.scene.brush(colors.isDark))) {
        when (state.scene) {
            WeatherScene.Storm -> Lightning(Modifier.fillMaxSize())
            WeatherScene.Night -> Stars(Modifier.fillMaxSize())
            else -> Unit
        }
        Column(Modifier.fillMaxSize()) {
            DexTopBar(
                contentColor = colors.onScene,
                leading = {
                    Column {
                        Text("Today", style = DexTheme.type.titleLarge, color = colors.onScene, modifier = Modifier.semantics { heading() })
                        Text(state.dateLabel, style = DexTheme.type.labelMedium, color = colors.onScene)
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
                WeatherRow(state, onSettings)
                val answer = state.answer
                when {
                    state.failed -> OfflineCard(onRetry)
                    answer == null -> MysteryCard(pokemon = null, revealed = false, wrongCount = 0, hint = null)
                    else -> {
                        MysteryCard(answer, state.revealed, state.wrongGuesses.size, state.weatherHint)
                        AnimatedContent(
                            targetState = state.revealed,
                            transitionSpec = { fadeIn(tween(300, delayMillis = 150)) togetherWith fadeOut(tween(150)) },
                            label = "todayActions",
                        ) { revealed ->
                            if (revealed) {
                                Revealed(answer, state, onOpenEntry, onReminder)
                            } else {
                                Guessing(state, onGuess, onUseHint)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Weather and time of day, with the provider's attribution in the same place (their terms ask for that). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherRow(state: TodayUiState, onSettings: () -> Unit) {
    val colors = DexTheme.colors
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.weatherLine?.let { line ->
                val clear = state.scene == WeatherScene.Clear || state.scene == WeatherScene.Heat
                SceneChip(state.city?.let { "$line · $it" } ?: line, if (clear) DexIcons.Sun else DexIcons.Storm)
            }
            SceneChip(if (state.isNight) "Night" else "Day", if (state.isNight) DexIcons.Moon else DexIcons.Sun)
        }
        val provider = state.weatherProvider
        when {
            provider != null -> Text(
                provider.attribution,
                style = DexTheme.type.labelSmall,
                color = colors.onSceneSecondary,
                modifier = provider.link?.let { link ->
                    Modifier.clickable(role = Role.Button, onClickLabel = "Open ${provider.attribution}") { uriHandler.openUri(link) }
                } ?: Modifier,
            )
            state.city == null && !state.loading -> TextButton(onClick = onSettings) {
                Text("Add your city to bring the weather in", style = DexTheme.type.labelLarge, color = colors.onScene)
            }
            state.city != null && !state.loading -> Text(
                "No weather for ${state.city} right now, so the season and time of day chose.",
                style = DexTheme.type.labelSmall,
                color = colors.onSceneSecondary,
            )
        }
    }
}

@Composable
private fun MysteryCard(pokemon: Pokemon?, revealed: Boolean, wrongCount: Int, hint: String?) {
    val colors = DexTheme.colors
    // A wrong guess shakes the card.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wrongCount) {
        if (wrongCount > 0) {
            shake.animateTo(0f, keyframes {
                durationMillis = 420
                -18f at 60; 16f at 130; -12f at 200; 8f at 270; -4f at 340
            })
        }
    }
    val glow = rememberPulse(from = 0.85f, to = 1.15f)
    SceneCard(Modifier.graphicsLayer { translationX = shake.value }) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer { scaleX = glow; scaleY = glow }
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = if (revealed) 0.45f else 0.3f), Color.Transparent))),
            )
            if (pokemon != null) {
                AnimatedContent(
                    targetState = revealed,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), initialScale = 0.6f)) togetherWith
                            fadeOut(tween(200))
                    },
                    label = "reveal",
                ) { isRevealed ->
                    if (isRevealed) {
                        PokemonArtwork(pokemon, size = 214.dp, glowAlpha = 0f, animated = true)
                    } else {
                        PokemonSilhouette(pokemon, size = 214.dp, colour = SceneSilhouette)
                    }
                }
            }
        }
        val title = when {
            pokemon == null -> "Finding today's Pokémon…"
            revealed -> "It's ${pokemon.name}!"
            else -> "Who's today's Pokémon?"
        }
        Text(
            title,
            style = DexTheme.type.titleLarge,
            color = colors.onScene,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (revealed) {
            pokemon?.category?.let { Text(it, style = DexTheme.type.labelLarge, color = colors.onSceneSecondary) }
        } else {
            hint?.let { Text(it, style = DexTheme.type.labelLarge, color = colors.onSceneSecondary) }
        }
    }
}

@Composable
private fun Guessing(state: TodayUiState, onGuess: (Pokemon) -> Unit, onUseHint: () -> Unit) {
    val colors = DexTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.choices.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { choice ->
                    DexSceneButton(
                        choice.name,
                        onClick = { onGuess(choice) },
                        enabled = choice.id !in state.wrongGuesses,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.hints.forEach { hint ->
                key(hint) {
                    StaggeredEntrance(0) { Text(hint, style = DexTheme.type.labelLarge, color = colors.onScene) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (state.hintsLeft == 1) "1 hint left" else "${state.hintsLeft} hints left",
                style = DexTheme.type.labelMedium,
                color = colors.onHighlight,
                modifier = Modifier.clip(DexShape.full).background(colors.highlight).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            TextButton(onClick = onUseHint, enabled = state.hintsLeft > 0) {
                Text("Use a hint", style = DexTheme.type.labelLarge, color = if (state.hintsLeft > 0) colors.onScene else colors.onSceneSecondary)
            }
        }
    }
}

@Composable
private fun Revealed(answer: Pokemon, state: TodayUiState, onOpenEntry: (Pokemon) -> Unit, onReminder: (Boolean) -> Unit) {
    val colors = DexTheme.colors
    val streakDays = state.streakDays
    val remaining by produceState(untilMidnight()) {
        while (true) {
            delay(1_000)
            value = untilMidnight()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Added to your collection.", style = DexTheme.type.bodyLarge, color = colors.onScene)
        if (state.offerReminder) ReminderOffer(onReminder)
        StoryCard(answer, state.reason)
        if (streakDays > 0) {
            Text(
                if (streakDays == 1) "1-day streak. Come back tomorrow to keep it going." else "$streakDays-day streak.",
                style = DexTheme.type.labelLarge,
                color = colors.onHighlight,
                modifier = Modifier.clip(DexShape.full).background(colors.highlight).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        DexPrimaryButton("Open full entry", onClick = { onOpenEntry(answer) }, modifier = Modifier.fillMaxWidth())
        val seconds = remaining.seconds.coerceAtLeast(0)
        Text(
            "Next Pokémon in %02d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60),
            style = DexTheme.type.numberLarge,
            color = colors.onSceneSecondary,
        )
    }
}

/** Offered once, right after the first reveal (plan, section 3). */
@Composable
private fun ReminderOffer(onAnswer: (Boolean) -> Unit) {
    val colors = DexTheme.colors
    SceneCard {
        Text("Get a daily reminder?", style = DexTheme.type.titleMedium, color = colors.onScene)
        Text(
            "A nudge at 9:00 each morning when a new Pokémon is waiting. It never gives the answer away.",
            style = DexTheme.type.bodyMedium,
            color = colors.onSceneSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DexPrimaryButton("Turn on", { onAnswer(true) })
            TextButton(onClick = { onAnswer(false) }) { Text("Not now", style = DexTheme.type.labelLarge, color = colors.onScene) }
        }
    }
}

/** The story after the reveal: why today, where it began, and what the Pokédex says. */
@Composable
private fun StoryCard(pokemon: Pokemon, reason: String?) {
    val colors = DexTheme.colors
    SceneCard(spacing = 12.dp) {
        reason?.let { StorySection("Why today?", it) }
        val firstSeen = listOfNotNull(pokemon.generation.label, pokemon.firstGame?.let { "Pokémon $it" }).joinToString(", in ")
        StorySection("First appeared", firstSeen)
        pokemon.habitat?.let { StorySection("Habitat", it) }
        val entries = pokemon.storyEntries
        when {
            entries == null -> StorySection("Its story", "Loading the Pokédex entries…")
            entries.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Its story", style = DexTheme.type.titleMedium, color = colors.onScene, modifier = Modifier.semantics { heading() })
                entries.forEach { entry ->
                    Text(entry.text, style = DexTheme.type.bodyMedium, color = colors.onScene)
                    Text("Pokémon ${entry.game}", style = DexTheme.type.labelSmall, color = colors.onSceneSecondary)
                }
            }
        }
    }
}

@Composable
private fun StorySection(title: String, body: String) {
    val colors = DexTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = DexTheme.type.titleMedium, color = colors.onScene, modifier = Modifier.semantics { heading() })
        Text(body, style = DexTheme.type.bodyMedium, color = colors.onScene)
    }
}

@Composable
private fun OfflineCard(onRetry: () -> Unit) {
    SceneCard {
        Text("Today's Pokémon needs the Pokédex", style = DexTheme.type.titleMedium, color = DexTheme.colors.onScene)
        Text("Connect once to download it. After that, Today works offline.", style = DexTheme.type.bodyMedium, color = DexTheme.colors.onSceneSecondary)
        DexPrimaryButton("Try again", onRetry)
    }
}

private fun untilMidnight(): Duration {
    val now = LocalDateTime.now()
    return Duration.between(now, LocalDate.now().plusDays(1).atStartOfDay())
}

/** Decorative lightning that flickers now and then. */
@Composable
private fun Lightning(modifier: Modifier) {
    val flicker = if (LocalInspectionMode.current) {
        1f
    } else {
        val value by rememberInfiniteTransition(label = "lightning").animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(keyframes {
                durationMillis = 4200
                1f at 0; 1f at 3000; 0.2f at 3080; 1f at 3160; 0.35f at 3240; 1f at 3400
            }),
            label = "flicker",
        )
        value
    }
    Canvas(modifier.graphicsLayer { alpha = flicker }) {
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

/** A few slowly twinkling stars for the night scene. Positions are fractions of the screen. */
private val StarField = listOf(0.12f to 0.08f, 0.3f to 0.16f, 0.52f to 0.06f, 0.7f to 0.14f, 0.88f to 0.1f, 0.2f to 0.3f, 0.8f to 0.34f, 0.62f to 0.26f)

@Composable
private fun Stars(modifier: Modifier) {
    val phase = if (LocalInspectionMode.current) {
        0.5f
    } else {
        val value by rememberInfiniteTransition(label = "stars").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
            label = "twinkle",
        )
        value
    }
    Canvas(modifier) {
        StarField.forEachIndexed { i, (fx, fy) ->
            val twinkle = if (i % 2 == 0) phase else 1f - phase
            drawCircle(
                Color.White.copy(alpha = 0.25f + 0.55f * twinkle),
                radius = (1.5f + (i % 3)).dp.toPx(),
                center = Offset(size.width * fx, size.height * fy),
            )
        }
    }
}

@DexPreviews
@Composable
private fun TodayScreenPreview() {
    DexTheme {
        TodayScreen(
            state = TodayUiState(
                loading = false,
                dateLabel = SampleData.today.dateLabel,
                scene = WeatherScene.Storm,
                isNight = true,
                answer = SampleData.today.answer,
                choices = SampleData.today.choices,
                city = "London",
                weatherLine = "Thunderstorm · 17°C",
                weatherProvider = WeatherProvider.OpenMeteo,
                weatherHint = SampleData.today.weatherHint,
                wrongGuesses = setOf(SampleData.gengar.id),
                hints = listOf("It first appeared in Generation I."),
                hintsLeft = 1,
            ),
            onGuess = {}, onUseHint = {}, onRetry = {}, onOpenEntry = {}, onSettings = {}, onReminder = {},
        )
    }
}

@DexPreviews
@Composable
private fun TodayRevealedPreview() {
    val pikachu = SampleData.pikachu.copy(
        firstGame = "Red",
        habitat = "Forest",
        storyEntries = listOf(StoryEntry("Sample game", "Sample Pokédex entry text for the preview.")),
    )
    DexTheme {
        TodayScreen(
            state = TodayUiState(
                loading = false,
                dateLabel = SampleData.today.dateLabel,
                scene = WeatherScene.Storm,
                isNight = true,
                city = "London",
                weatherLine = "Thunderstorm · 17°C",
                weatherProvider = WeatherProvider.Google,
                answer = pikachu,
                choices = SampleData.today.choices,
                revealed = true,
                streakDays = 3,
                reason = "Thunderstorm in London tonight: Electric weather.",
                offerReminder = true,
            ),
            onGuess = {}, onUseHint = {}, onRetry = {}, onOpenEntry = {}, onSettings = {}, onReminder = {},
        )
    }
}
