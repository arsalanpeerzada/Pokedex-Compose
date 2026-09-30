package dev.pokedex.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexSwitchRow
import dev.pokedex.core.designsystem.component.DexTextField
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.PokemonArtwork
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.StaggeredEntrance
import dev.pokedex.core.designsystem.motion.floating
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    repository: PokemonRepository,
) : ViewModel() {

    val state: StateFlow<UserPreferences> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    init {
        // Start the one-off Pokédex download now, so it's ready when onboarding ends.
        viewModelScope.launch { repository.refreshIndex() }
    }

    fun setUsageStats(enabled: Boolean) = viewModelScope.launch { preferences.setUsageStats(enabled) }
    fun setCrashReports(enabled: Boolean) = viewModelScope.launch { preferences.setCrashReports(enabled) }

    fun finish(city: String) = viewModelScope.launch {
        preferences.setCity(city)
        preferences.completeOnboarding()
    }
}

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        preferences = prefs,
        onUsageStats = { viewModel.setUsageStats(it) },
        onCrashReports = { viewModel.setCrashReports(it) },
        onFinish = { viewModel.finish(it) },
    )
}

private const val PAGES = 3

/** Three short steps: welcome, city (optional) and privacy choices (both off). */
@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    onUsageStats: (Boolean) -> Unit,
    onCrashReports: (Boolean) -> Unit,
    onFinish: (city: String) -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val colors = DexTheme.colors
    val pager = rememberPagerState(initialPage = initialPage) { PAGES }
    val scope = rememberCoroutineScope()
    var city by rememberSaveable { mutableStateOf(preferences.city.orEmpty()) }
    val next: () -> Unit = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.brandBrush())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HorizontalPager(state = pager, userScrollEnabled = false, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> CityPage(city, onCityChange = { city = it }, onDone = next)
                    else -> PrivacyPage(preferences, onUsageStats, onCrashReports)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PageDots(current = pager.currentPage)
            when (pager.currentPage) {
                0 -> DexPrimaryButton("Get started", next, modifier = Modifier.fillMaxWidth())
                1 -> {
                    DexPrimaryButton(if (city.isBlank()) "Continue" else "Save and continue", next, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { city = ""; next() }) { Text("Skip for now", style = DexTheme.type.labelLarge, color = colors.text) }
                }
                else -> DexPrimaryButton("Start exploring", { onFinish(city) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    val colors = DexTheme.colors
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        StarterArt(Pokemon(1, "Bulbasaur", listOf(PokemonType.Grass)), 150.dp, Modifier.offset(x = (-96).dp, y = 44.dp), period = 2900)
        StarterArt(Pokemon(7, "Squirtle", listOf(PokemonType.Water)), 150.dp, Modifier.offset(x = 96.dp, y = 44.dp), period = 3300)
        StarterArt(Pokemon(4, "Charmander", listOf(PokemonType.Fire)), 180.dp, Modifier.offset(y = (-24).dp), period = 2500)
    }
    StaggeredEntrance(0) {
        Text("Pokedex", style = DexTheme.type.displayLarge, color = colors.text, modifier = Modifier.semantics { heading() })
    }
    StaggeredEntrance(1) {
        Text(
            "Every Pokémon, in full, and a new one to guess every day.",
            style = DexTheme.type.titleMedium,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
    }
    StaggeredEntrance(2) {
        Text(
            "It works offline once the Pokédex has downloaded.",
            style = DexTheme.type.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StarterArt(pokemon: Pokemon, size: Dp, modifier: Modifier, period: Int) {
    // Decorative: the welcome text says what the app is, so screen readers skip the art.
    Box(modifier.floating(periodMillis = period).clearAndSetSemantics {}) {
        PokemonArtwork(pokemon, size = size, glowAlpha = 0.45f)
    }
}

@Composable
private fun CityPage(city: String, onCityChange: (String) -> Unit, onDone: () -> Unit) {
    val colors = DexTheme.colors
    Text("Where are you?", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
    Text(
        "Soon, Today will pick a Pokémon to suit your weather: rain brings Water types, storms bring Electric. " +
            "Add your city now, or later in Settings.",
        style = DexTheme.type.bodyLarge,
        color = colors.text,
        textAlign = TextAlign.Center,
    )
    DexTextField(
        value = city,
        onValueChange = onCityChange,
        placeholder = "City, for example Leeds",
        icon = DexIcons.Pin,
        capitalization = KeyboardCapitalization.Words,
        onImeAction = onDone,
    )
    Text("Your city stays on this phone.", style = DexTheme.type.labelMedium, color = colors.textSecondary)
}

@Composable
private fun PrivacyPage(preferences: UserPreferences, onUsageStats: (Boolean) -> Unit, onCrashReports: (Boolean) -> Unit) {
    val colors = DexTheme.colors
    Text("Your choice", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
    Text(
        "Both are off unless you turn them on, and you can change them any time in Settings.",
        style = DexTheme.type.bodyLarge,
        color = colors.text,
        textAlign = TextAlign.Center,
    )
    GlassCard {
        DexSwitchRow(
            title = "Share usage statistics",
            body = "Anonymous counts of which screens are used. Never your location.",
            checked = preferences.usageStats,
            onChange = onUsageStats,
        )
        DexSwitchRow(
            title = "Send crash reports",
            body = "Helps fix crashes.",
            checked = preferences.crashReports,
            onChange = onCrashReports,
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun PageDots(current: Int) {
    val colors = DexTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Step ${current + 1} of $PAGES" },
    ) {
        repeat(PAGES) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 28.dp else 8.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dotWidth")
            val colour by animateColorAsState(if (selected) colors.primary else colors.outline, label = "dotColour")
            Box(Modifier.size(width = width, height = 8.dp).clip(DexShape.full).background(colour))
        }
    }
}

@DexPreviews
@Composable
private fun WelcomePreview() {
    DexTheme { OnboardingScreen(UserPreferences(), {}, {}, {}) }
}

@DexPreviews
@Composable
private fun CityPreview() {
    DexTheme { OnboardingScreen(UserPreferences(city = "Leeds"), {}, {}, {}, initialPage = 1) }
}

@DexPreviews
@Composable
private fun PrivacyPreview() {
    DexTheme { OnboardingScreen(UserPreferences(), {}, {}, {}, initialPage = 2) }
}
