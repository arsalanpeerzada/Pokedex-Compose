package dev.pokedex.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.model.ThemeMode
import dev.pokedex.core.model.UserPreferences
import dev.pokedex.feature.onboarding.OnboardingRoute
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Null until DataStore answers, so a returning user never sees onboarding flash up.
            val loaded by preferences.preferences.collectAsStateWithLifecycle<UserPreferences?>(null)
            val prefs = loaded ?: UserPreferences()
            val dark = when (prefs.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Keep status and navigation bar icons readable when the app theme differs from the system's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            DexTheme(darkTheme = dark) {
                when {
                    loaded == null -> Box(Modifier.fillMaxSize().background(DexTheme.colors.background))
                    else -> AnimatedContent(
                        targetState = prefs.onboardingDone,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                        label = "onboarding",
                    ) { done -> if (done) PokedexApp() else OnboardingRoute() }
                }
            }
        }
    }
}
