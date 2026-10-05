package dev.pokedex.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.model.BaseStats
import dev.pokedex.core.model.CollectionSummary
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.Team
import dev.pokedex.core.model.TeamAnalysis
import dev.pokedex.core.model.TeamSuggestions
import dev.pokedex.core.model.TypeChart
import dev.pokedex.core.model.Units
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherScene
import dev.pokedex.feature.collection.CollectionScreen
import dev.pokedex.feature.detail.PokemonDetailScreen
import dev.pokedex.feature.pokedex.PokedexActions
import dev.pokedex.feature.pokedex.PokedexFilters
import dev.pokedex.feature.pokedex.PokedexScreen
import dev.pokedex.feature.pokedex.PokedexUiState
import dev.pokedex.feature.teams.TeamEditorScreen
import dev.pokedex.feature.today.TodayScreen
import dev.pokedex.feature.today.TodayUiState
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.net.URL
import kotlin.math.roundToInt

/**
 * Renders the Google Play graphics into play-store/graphics: the 512 px icon, the 1024 x 500 feature
 * graphic and the phone screenshots. Skipped unless PLAY_ASSETS is set, so CI never runs it:
 *
 *     PLAY_ASSETS=1 gradlew :app:testDebugUnitTest --tests "*PlayStoreAssets*"
 *
 * Artwork is downloaded once from PokeAPI's sprite repository into build/play-artwork.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application, so Hilt, WorkManager and the rest of the real app stay out of it.
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayStoreAssets {

    @get:Rule val compose = createComposeRule()

    private val out = File("../play-store/graphics")
    private val artworkCache = File("build/play-artwork")

    private val starters = SampleData.generationOne
    private val pikachu = SampleData.pikachu.copy(
        firstGame = "Red",
        habitat = "Forest",
        // No made-up entries; an empty list hides "Its story" rather than showing it loading.
        storyEntries = emptyList(),
        stats = BaseStats(hp = 35, attack = 55, defense = 40, specialAttack = 50, specialDefense = 50, speed = 90),
    )
    private val featured = listOf(starters[0], pikachu, starters[3], starters[6], SampleData.eevee, SampleData.gengar)

    @Before
    fun onlyWhenAsked() {
        assumeTrue("Set PLAY_ASSETS to render the Play graphics", System.getenv("PLAY_ASSETS") != null)
        (starters + featured).distinctBy { it.id }.forEach { cached(it.artworkUrl) }
    }

    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun icon() = render("icon-512.png") {
        // The launcher's own layers, cropped like a launcher mask so the ball fills the icon.
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().scale(1.4f))
        }
    }

    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() = render("feature-graphic-1024x500.jpg") {
        Row(
            Modifier.fillMaxSize().background(BrandGradient).padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(112.dp).scale(1.6f))
                    Spacer(Modifier.width(12.dp))
                    Text("Pokedex", style = DexTheme.type.displayLarge.copy(fontSize = 76.sp, lineHeight = 80.sp), color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "A new Pokémon every day,\npicked by your weather.",
                    style = DexTheme.type.titleLarge.copy(fontSize = 30.sp, lineHeight = 38.sp),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Box(Modifier.size(420.dp, 420.dp)) {
                PokemonArtwork(starters[0], 190.dp, Modifier.align(Alignment.TopStart).padding(top = 20.dp), glowAlpha = 0.8f)
                PokemonArtwork(starters[6], 170.dp, Modifier.align(Alignment.BottomStart), glowAlpha = 0.8f)
                PokemonArtwork(starters[3], 180.dp, Modifier.align(Alignment.TopEnd), glowAlpha = 0.8f)
                PokemonArtwork(pikachu, 250.dp, Modifier.align(Alignment.BottomEnd), glowAlpha = 0.8f)
            }
        }
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot1Guess() = screenshot("phone-1-guess.jpg", "Guess today's Pokémon from its silhouette", dark = true) {
        TodayScreen(
            state = today.copy(wrongGuesses = setOf(SampleData.gengar.id), hints = listOf("It first appeared in Generation I."), hintsLeft = 1),
            onGuess = {}, onUseHint = {}, onRetry = {}, onOpenEntry = {}, onSettings = {}, onReminder = {},
        )
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot2Reveal() = screenshot("phone-2-why-today.jpg", "Picked by your weather, the time and the season", dark = true) {
        TodayScreen(
            state = today.copy(revealed = true, streakDays = 12, reason = "Thunderstorm in London tonight: Electric weather."),
            onGuess = {}, onUseHint = {}, onRetry = {}, onOpenEntry = {}, onSettings = {}, onReminder = {},
        )
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot3Pokedex() = screenshot("phone-3-pokedex.jpg", "All 1,025 Pokémon, offline once downloaded", dark = false) {
        PokedexScreen(
            state = PokedexUiState(pokemon = starters.take(9), totalCount = 1025, caughtIds = SampleData.caught, filters = PokedexFilters(), loading = false),
            actions = PokedexActions(),
        )
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot4Detail() = screenshot("phone-4-detail.jpg", "Stats, evolutions, matchups and forms", dark = false, prepare = { onNodeWithText("Stats").performClick() }) {
        PokemonDetailScreen(
            pikachu, caught = true, favourite = true, cryPlaying = false,
            chart = TypeChart.Empty, evolution = emptyList(), teams = emptyList(), units = Units.Metric, onAddToTeam = {}, onAddToNewTeam = {},
            onBack = {}, onOpenPokemon = {}, onToggleCaught = {}, onCompare = {}, onFavourite = {}, onShare = {}, onPlayCry = {},
        )
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot5Team() = screenshot("phone-5-team-builder.jpg", "Build a team of six and check its weaknesses", dark = true) {
        val team = Team(1, "Starters", listOf(starters[0], starters[3], starters[6], pikachu, null, null))
        TeamEditorScreen(
            team, TeamAnalysis.of(team.filled, chart), emptyList(), TeamSuggestions.suggest(team.filled, starters, chart),
            onBack = {}, onSetMember = { _, _ -> }, onAddSuggestion = {}, onRename = {}, onDelete = {}, onOpenPokemon = {},
        )
    }

    @Test
    @Config(qualifiers = Phone)
    fun screenshot6Collection() = screenshot("phone-6-collection.jpg", "Fill your sticker album and keep your streak", dark = false) {
        CollectionScreen(
            CollectionSummary(caught = 212, total = 1025, streakDays = 12, latestCaught = listOf(SampleData.gengar, SampleData.eevee, pikachu)),
            SampleData.collection, onPokemonClick = {}, onPokemonShown = {}, onSettings = {},
        )
    }

    private val today = TodayUiState(
        loading = false,
        dateLabel = "Monday 5 October",
        scene = WeatherScene.Storm,
        isNight = true,
        city = "London",
        weatherLine = "Thunderstorm · 17°C",
        weatherProvider = WeatherProvider.OpenMeteo,
        answer = pikachu,
        choices = SampleData.today.choices,
        weatherHint = "Stormy in London tonight.",
        hintsLeft = 2,
    )

    // A few real factors, enough for the team's weaknesses and suggestions to show.
    private val chart = TypeChart(
        mapOf(
            (PokemonType.Electric to PokemonType.Water) to 2f,
            (PokemonType.Ground to PokemonType.Electric) to 2f,
            (PokemonType.Ground to PokemonType.Fire) to 2f,
            (PokemonType.Water to PokemonType.Fire) to 2f,
            (PokemonType.Fire to PokemonType.Grass) to 2f,
            (PokemonType.Grass to PokemonType.Water) to 2f,
        ),
    )

    /** A caption over the screen, laid out at full phone size and scaled into a phone frame that runs off the bottom. */
    private fun screenshot(name: String, caption: String, dark: Boolean, prepare: ComposeContentTestRule.() -> Unit = {}, screen: @Composable () -> Unit) = render(name, prepare) {
        Column(Modifier.fillMaxSize().background(BrandGradient), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(44.dp))
            Text(
                caption,
                style = DexTheme.type.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp),
            )
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopCenter, unbounded = true)
                    .border(6.dp, Color(0xFF0B0D2E), PhoneShape)
                    .padding(6.dp)
                    .clip(PhoneShape)
                    .phoneScale(0.8f),
            ) {
                DexTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxSize().background(DexTheme.colors.background)) { screen() }
                }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun render(name: String, prepare: ComposeContentTestRule.() -> Unit = {}, content: @Composable () -> Unit) {
        val artwork = AsyncImagePreviewHandler { request -> BitmapFactory.decodeFile(cached(request.data as String).path).asImage() }
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides artwork) {
                DexTheme(darkTheme = false, content = content)
            }
        }
        compose.waitForIdle()
        compose.prepare()
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        out.mkdirs()
        File(out, name).outputStream().use {
            // Play takes a 32-bit PNG for the icon, and JPEG (no alpha) for everything else.
            if (name.endsWith(".png")) bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) else bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
    }

    private fun cached(url: String): File {
        val file = File(artworkCache, url.substringAfter("official-artwork/").replace('/', '-'))
        if (!file.exists()) {
            artworkCache.mkdirs()
            URL(url).openStream().use { input -> file.outputStream().use(input::copyTo) }
        }
        return file
    }

    private companion object {
        // 1236 x 2196 px: 9:16, which Play accepts (the long side may be at most twice the short one).
        const val Phone = "w412dp-h732dp-xxhdpi"
        val PhoneShape = RoundedCornerShape(36.dp)
        val BrandGradient = Brush.linearGradient(listOf(Color(0xFF3B3F9E), Color(0xFF0F1240)))
    }
}

/** Measures the content at the 412 x 917 dp phone size the previews use, then draws it scaled down. */
private fun Modifier.phoneScale(scale: Float) = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(412.dp.roundToPx(), 917.dp.roundToPx()))
    layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

