package dev.pokedex.feature.today

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherProvider
import dev.pokedex.core.model.WeatherScene
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
class TodayScreenTest {

    @get:Rule val compose = createComposeRule()

    private val base = TodayUiState(
        loading = false,
        dateLabel = "Wednesday 30 September",
        scene = WeatherScene.Rain,
        city = "Leeds",
        weatherLine = "Slight rain · 14°C",
        weatherProvider = WeatherProvider.OpenMeteo,
        answer = SampleData.pikachu,
        choices = SampleData.today.choices,
        weatherHint = "Rainy in Leeds today.",
        hintsLeft = 2,
    )

    private fun show(state: TodayUiState, onGuess: (Pokemon) -> Unit = {}, onReminder: (Boolean) -> Unit = {}) = compose.setContent {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            DexTheme {
                TodayScreen(state, onGuess = onGuess, onUseHint = {}, onRetry = {}, onOpenEntry = {}, onSettings = {}, onReminder = onReminder)
            }
        }
    }

    @Test
    fun before_the_reveal_the_hint_is_weather_only_and_attribution_is_shown() {
        show(base)
        compose.onNodeWithText("Weather data by Open-Meteo.com").assertIsDisplayed()
        compose.onNodeWithText("Who's today's Pokémon?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rainy in Leeds today.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun a_wrong_guess_is_disabled_and_others_stay_available() {
        val wrong = SampleData.gengar
        show(base.copy(wrongGuesses = setOf(wrong.id)))
        compose.onNodeWithText(wrong.name).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(SampleData.eevee.name).performScrollTo().assertIsEnabled()
    }

    @Test
    fun guessing_reports_the_choice() {
        var guessed: Pokemon? = null
        show(base, onGuess = { guessed = it })
        compose.onNodeWithText("Pikachu").performScrollTo().performClick()
        assertEquals(SampleData.pikachu, guessed)
    }

    @Test
    fun after_the_reveal_the_story_and_the_reminder_offer_appear() {
        var reminder: Boolean? = null
        show(base.copy(revealed = true, reason = "Slight rain in Leeds today: Water weather.", offerReminder = true), onReminder = { reminder = it })
        compose.onNodeWithText("It's Pikachu!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Why today?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        assertTrue(reminder == false)
    }
}
