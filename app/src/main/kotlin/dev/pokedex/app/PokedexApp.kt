package dev.pokedex.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import dev.pokedex.core.designsystem.component.DexNavigationBar
import dev.pokedex.core.designsystem.component.DexNavigationRail
import dev.pokedex.core.designsystem.component.NavItem
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.LocalNavAnimatedScope
import dev.pokedex.core.designsystem.motion.LocalSharedTransitionScope
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.feature.collection.CollectionRoute
import dev.pokedex.feature.detail.PokemonDetailRoute
import dev.pokedex.feature.pokedex.PokedexRoute
import dev.pokedex.feature.pokedex.TypeChartRoute
import dev.pokedex.feature.settings.SettingsRoute
import dev.pokedex.feature.teams.TeamEditorRoute
import dev.pokedex.feature.teams.TeamsRoute
import dev.pokedex.feature.today.TodayRoute

sealed interface Destination
data object TodayKey : Destination
data object PokedexKey : Destination
data object TeamsKey : Destination
data object CollectionKey : Destination
data class DetailKey(val pokemonId: Int) : Destination
data object TypeChartKey : Destination
data object SettingsKey : Destination
data class TeamKey(val teamId: Long) : Destination

private val TopLevel = listOf(TodayKey, PokedexKey, TeamsKey, CollectionKey)
private val NavItems = listOf(
    NavItem("Today", DexIcons.Sun),
    NavItem("Pokédex", DexIcons.Grid),
    NavItem("Teams", DexIcons.Team),
    NavItem("Collection", DexIcons.Star),
)

/** Windows at least this wide get a navigation rail and, where there's room, list and detail side by side. */
private val WideWidth = 600.dp

/**
 * App shell: four tabs plus the shared detail page, on Navigation 3 (we own the back stack).
 * Each destination gets its own ViewModel store, and artwork flies between screens as a shared element.
 * On tablets, foldables and landscape, a rail replaces the bottom bar and lists show their detail alongside.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PokedexApp() {
    val backStack = remember { mutableStateListOf<Destination>(TodayKey) }
    val openSettings: () -> Unit = { if (backStack.lastOrNull() != SettingsKey) backStack.add(SettingsKey) }
    val selectTab: (Int) -> Unit = { i ->
        if (backStack.size != 1 || backStack.first() != TopLevel[i]) {
            backStack.clear()
            backStack.add(TopLevel[i])
        }
    }
    val listDetail = rememberListDetailSceneStrategy<Destination>()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideWidth
        val showBottomBar = !wide && backStack.lastOrNull()?.let { it in TopLevel } == true
        // From a list, a new Pokémon replaces the one beside it on wide screens instead of stacking up.
        val openDetail: (Int) -> Unit = { id -> backStack.add(DetailKey(id)) }
        val openFromList: (Int) -> Unit = { id ->
            if (wide && backStack.lastOrNull() is DetailKey) backStack[backStack.lastIndex] = DetailKey(id) else openDetail(id)
        }

        Row(Modifier.fillMaxSize()) {
            if (wide) DexNavigationRail(NavItems, TopLevel.indexOf(backStack.first()), selectTab)
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250)),
                    ) {
                        DexNavigationBar(items = NavItems, selectedIndex = TopLevel.indexOf(backStack.first()), onSelect = selectTab)
                    }
                },
            ) { padding ->
                SharedTransitionLayout {
                    // Side by side, the same artwork can be on screen twice, so shared elements are phone-only.
                    CompositionLocalProvider(LocalSharedTransitionScope provides if (wide) null else this) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            sceneStrategies = listOf(listDetail),
                            sharedTransitionScope = this,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            popTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            predictivePopTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            entryProvider = entryProvider {
                                entry<TodayKey> {
                                    Animated {
                                        TodayRoute(onOpenEntry = { openDetail(it.id) }, onSettings = openSettings, contentPadding = padding)
                                    }
                                }
                                entry<PokedexKey>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder("Choose a Pokémon to see it here.") })) {
                                    Animated {
                                        PokedexRoute(
                                            onPokemonClick = { openFromList(it.id) },
                                            onTypeChart = { backStack.add(TypeChartKey) },
                                            onSettings = openSettings,
                                            contentPadding = padding,
                                        )
                                    }
                                }
                                entry<TeamsKey>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder("Choose a team to edit it here.") })) {
                                    Animated {
                                        TeamsRoute(onOpenTeam = { backStack.add(TeamKey(it)) }, onSettings = openSettings, contentPadding = padding)
                                    }
                                }
                                entry<TeamKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                                    Animated {
                                        TeamEditorRoute(teamId = key.teamId, onBack = { backStack.removeLastOrNull() }, onOpenPokemon = openDetail)
                                    }
                                }
                                entry<CollectionKey>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder("Choose a Pokémon to see it here.") })) {
                                    Animated {
                                        CollectionRoute(onPokemonClick = { openFromList(it.id) }, onSettings = openSettings, contentPadding = padding)
                                    }
                                }
                                entry<TypeChartKey> {
                                    TypeChartRoute(onBack = { backStack.removeLastOrNull() })
                                }
                                entry<SettingsKey> {
                                    SettingsRoute(onBack = { backStack.removeLastOrNull() })
                                }
                                entry<DetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                                    Animated {
                                        PokemonDetailRoute(
                                            pokemonId = key.pokemonId,
                                            onBack = { backStack.removeLastOrNull() },
                                            onOpenPokemon = openDetail,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Shown beside a list on wide screens before anything is chosen. */
@Composable
private fun DetailPlaceholder(text: String) {
    val colors = DexTheme.colors
    Box(Modifier.fillMaxSize().background(colors.brandBrush()), contentAlignment = Alignment.Center) {
        Text(text, style = DexTheme.type.titleMedium, color = colors.textSecondary, modifier = Modifier.padding(32.dp))
    }
}

/** Hands this destination's enter and exit animation to shared elements further down. */
@Composable
private fun Animated(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}
