package dev.pokedex.app

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.component.DexNavigationBar
import dev.pokedex.core.designsystem.component.NavItem
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.brandBrush
import dev.pokedex.feature.collection.CollectionScreen
import dev.pokedex.feature.detail.PokemonDetailScreen
import dev.pokedex.feature.pokedex.PokedexScreen
import dev.pokedex.feature.today.TodayScreen

sealed interface Route
data object TodayRoute : Route
data object PokedexRoute : Route
data object TeamsRoute : Route
data object CollectionRoute : Route
data class DetailRoute(val pokemonId: Int) : Route

private val TopLevel = listOf(TodayRoute, PokedexRoute, TeamsRoute, CollectionRoute)
private val NavItems = listOf(
    NavItem("Today", DexIcons.Sun),
    NavItem("Pokédex", DexIcons.Grid),
    NavItem("Teams", DexIcons.Team),
    NavItem("Collection", DexIcons.Star),
)

/**
 * App shell: four tabs plus the shared detail page, on Navigation 3 (we own the back stack).
 * Data is still the hand-written sample set until the PokeAPI + Room layer lands.
 */
@Composable
fun PokedexApp() {
    val backStack = remember { mutableStateListOf<Route>(TodayRoute) }
    val caught = remember { mutableStateListOf<Int>().apply { addAll(SampleData.caught) } }
    val showNav = backStack.lastOrNull() !is DetailRoute
    val openDetail: (Int) -> Unit = { id -> backStack.add(DetailRoute(id)) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showNav) {
                DexNavigationBar(
                    items = NavItems,
                    selectedIndex = TopLevel.indexOf(backStack.first()),
                    onSelect = { i ->
                        backStack.clear()
                        backStack.add(TopLevel[i])
                    },
                )
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<TodayRoute> {
                    TodayScreen(
                        pick = SampleData.today,
                        onGuess = { choice -> if (choice.id == SampleData.today.answer.id) openDetail(choice.id) },
                        onUseHint = {},
                        onSettings = {},
                        contentPadding = padding,
                    )
                }
                entry<PokedexRoute> {
                    PokedexScreen(
                        pokemon = SampleData.generationOne,
                        caughtIds = caught.toSet(),
                        totalCount = 1025,
                        onPokemonClick = { openDetail(it.id) },
                        onTypeChart = {}, onSort = {}, onSettings = {}, onFilters = {},
                        contentPadding = padding,
                    )
                }
                entry<TeamsRoute> { ComingSoon("Team Builder", "Designed next, once Figma is available again.", padding) }
                entry<CollectionRoute> {
                    CollectionScreen(
                        summary = SampleData.collectionSummary,
                        entries = SampleData.collection,
                        onPokemonClick = { openDetail(it.id) },
                        onSearch = {}, onSettings = {},
                        contentPadding = padding,
                    )
                }
                entry<DetailRoute> { key ->
                    val pokemon = SampleData.pokemon(key.pokemonId) ?: SampleData.pikachu
                    PokemonDetailScreen(
                        pokemon = pokemon,
                        caught = pokemon.id in caught,
                        onBack = { backStack.removeLastOrNull() },
                        onToggleCaught = { if (!caught.remove(pokemon.id)) caught.add(pokemon.id) },
                        onAddToTeam = {}, onFavourite = {}, onShare = {}, onPlayCry = {},
                    )
                }
            },
        )
    }
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
