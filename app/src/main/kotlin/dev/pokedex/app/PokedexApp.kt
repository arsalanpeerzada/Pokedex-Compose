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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import dev.pokedex.core.designsystem.component.NavItem
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.motion.LocalNavAnimatedScope
import dev.pokedex.core.designsystem.motion.LocalSharedTransitionScope
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.feature.collection.CollectionRoute
import dev.pokedex.feature.detail.PokemonDetailRoute
import dev.pokedex.feature.pokedex.PokedexRoute
import dev.pokedex.feature.today.TodayRoute

sealed interface Destination
data object TodayKey : Destination
data object PokedexKey : Destination
data object TeamsKey : Destination
data object CollectionKey : Destination
data class DetailKey(val pokemonId: Int) : Destination

private val TopLevel = listOf(TodayKey, PokedexKey, TeamsKey, CollectionKey)
private val NavItems = listOf(
    NavItem("Today", DexIcons.Sun),
    NavItem("Pokédex", DexIcons.Grid),
    NavItem("Teams", DexIcons.Team),
    NavItem("Collection", DexIcons.Star),
)

/**
 * App shell: four tabs plus the shared detail page, on Navigation 3 (we own the back stack).
 * Each destination gets its own ViewModel store, and artwork flies between screens as a shared element.
 */
@Composable
fun PokedexApp() {
    val backStack = remember { mutableStateListOf<Destination>(TodayKey) }
    val showNav = backStack.lastOrNull() !is DetailKey
    val openDetail: (Int) -> Unit = { id -> backStack.add(DetailKey(id)) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showNav,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250)),
            ) {
                DexNavigationBar(
                    items = NavItems,
                    selectedIndex = TopLevel.indexOf(backStack.first()),
                    onSelect = { i ->
                        if (backStack.size != 1 || backStack.first() != TopLevel[i]) {
                            backStack.clear()
                            backStack.add(TopLevel[i])
                        }
                    },
                )
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    sharedTransitionScope = this,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    popTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    predictivePopTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    entryProvider = entryProvider {
                        entry<TodayKey> {
                            Animated {
                                TodayRoute(onOpenEntry = { openDetail(it.id) }, onSettings = {}, contentPadding = padding)
                            }
                        }
                        entry<PokedexKey> {
                            Animated {
                                PokedexRoute(onPokemonClick = { openDetail(it.id) }, onSettings = {}, contentPadding = padding)
                            }
                        }
                        entry<TeamsKey> {
                            ComingSoon("Team Builder", "Designed next, once Figma is available again.", padding)
                        }
                        entry<CollectionKey> {
                            Animated {
                                CollectionRoute(onPokemonClick = { openDetail(it.id) }, onSettings = {}, contentPadding = padding)
                            }
                        }
                        entry<DetailKey> { key ->
                            Animated {
                                PokemonDetailRoute(pokemonId = key.pokemonId, onBack = { backStack.removeLastOrNull() })
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Hands this destination's enter and exit animation to shared elements further down. */
@Composable
private fun Animated(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}

@Composable
private fun ComingSoon(title: String, body: String, padding: PaddingValues) {
    val colors = DexTheme.colors
    Column(
        Modifier.fillMaxSize().background(colors.brandBrush()).padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = DexTheme.type.headlineMedium, color = colors.text)
        Text(body, style = DexTheme.type.bodyLarge, color = colors.textSecondary)
    }
}
