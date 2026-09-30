package dev.pokedex.feature.pokedex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.theme.DexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// The same phone size as the previews; Robolectric's default screen is much smaller.
@Config(sdk = [35], qualifiers = "w412dp-h917dp")
class PokedexScreenTest {

    @get:Rule val compose = createComposeRule()

    private val state = PokedexUiState(
        pokemon = SampleData.generationOne.take(4),
        totalCount = 1025,
        caughtIds = setOf(1),
        loading = false,
    )

    /** Inspection mode turns off the endless decorative animations, so the test clock can idle. */
    private fun show(content: @Composable () -> Unit) = compose.setContent {
        CompositionLocalProvider(LocalInspectionMode provides true) { DexTheme { content() } }
    }

    @Test
    fun cards_describe_the_pokemon_for_screen_readers() {
        show { PokedexScreen(state, PokedexActions()) }
        compose.onNode(hasContentDescription("Bulbasaur, number 0001, Grass and Poison, caught")).assertIsDisplayed()
        compose.onNodeWithText("4 of 1,025 Pokémon").assertIsDisplayed()
    }

    @Test
    fun searching_reports_the_query() {
        var query = ""
        show { PokedexScreen(state, PokedexActions(onQueryChange = { query = it })) }
        compose.onNode(hasContentDescription("Search name or number")).performTextInput("char")
        assertEquals("char", query)
    }

    @Test
    fun the_caught_toggle_is_a_checkbox_and_reports_taps() {
        var toggled = false
        show { PokedexScreen(state, PokedexActions(onToggleCaughtOnly = { toggled = true })) }
        compose.onNodeWithText("Caught").assertIsOff().performClick()
        assertTrue(toggled)
    }

    @Test
    fun the_filters_sheet_shows_types_generations_and_the_result_count() {
        show { PokedexScreen(state, PokedexActions()) }
        compose.onNodeWithText("Filters").performClick()
        compose.onNode(hasText("Show 4 Pokémon")).assertIsDisplayed()
        compose.onNodeWithText("Generation").assertIsDisplayed()
    }

    @Test
    fun an_empty_result_offers_a_way_back() {
        var cleared = false
        show {
            PokedexScreen(
                state.copy(pokemon = emptyList(), filters = PokedexFilters(caughtOnly = true)),
                PokedexActions(onClearFilters = { cleared = true }),
            )
        }
        compose.onNodeWithText("No Pokémon match these filters.").assertIsDisplayed()
        compose.onNodeWithText("Clear search and filters").performClick()
        assertTrue(cleared)
    }
}
