package dev.pokedex.core.model

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }

/**
 * App settings. Usage statistics and crash reports are off until the user turns them on
 * (plan, section 1). Nothing is collected in builds without Firebase.
 */
data class UserPreferences(
    val theme: ThemeMode = ThemeMode.System,
    val usageStats: Boolean = false,
    val crashReports: Boolean = false,
    val onboardingDone: Boolean = false,
    /** Typed by the user, for Today's weather once the context engine lands. Never sent anywhere yet. */
    val city: String? = null,
)
