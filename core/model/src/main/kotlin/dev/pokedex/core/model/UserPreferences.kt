package dev.pokedex.core.model

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }

/**
 * App settings. Usage statistics, crash reports and the daily reminder are off until the user
 * turns them on (plan, section 1). Nothing is collected in builds without Firebase.
 */
data class UserPreferences(
    val theme: ThemeMode = ThemeMode.System,
    val units: Units = Units.Metric,
    val usageStats: Boolean = false,
    val crashReports: Boolean = false,
    val onboardingDone: Boolean = false,
    /** Typed by the user. Used to look up the weather for Today. */
    val city: String? = null,
    /** The city's position, rounded to two decimal places (about 1 km). Null until looked up. */
    val cityLatitude: Double? = null,
    val cityLongitude: Double? = null,
    /** ISO 3166 country code from the same lookup, for national days. */
    val cityCountry: String? = null,
    val reminder: Boolean = false,
    /** Whether Today has already offered the reminder, so it asks once. */
    val reminderAsked: Boolean = false,
)
