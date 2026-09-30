package dev.pokedex.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.pokedex.core.model.ThemeMode
import dev.pokedex.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setTheme(theme: ThemeMode)
    suspend fun setUsageStats(enabled: Boolean)
    suspend fun setCrashReports(enabled: Boolean)
    suspend fun setCity(city: String?)
    suspend fun completeOnboarding()
    suspend fun setReminder(enabled: Boolean)
}

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = store.data.map { prefs ->
        UserPreferences(
            theme = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.System,
            usageStats = prefs[USAGE_STATS] ?: false,
            crashReports = prefs[CRASH_REPORTS] ?: false,
            onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            city = prefs[CITY],
            reminder = prefs[REMINDER] ?: false,
            reminderAsked = prefs[REMINDER_ASKED] ?: false,
        )
    }

    override suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    override suspend fun setUsageStats(enabled: Boolean) {
        store.edit { it[USAGE_STATS] = enabled }
    }

    override suspend fun setCrashReports(enabled: Boolean) {
        store.edit { it[CRASH_REPORTS] = enabled }
    }

    override suspend fun setCity(city: String?) {
        store.edit { prefs ->
            val trimmed = city?.trim().orEmpty()
            if (trimmed.isEmpty()) prefs.remove(CITY) else prefs[CITY] = trimmed
        }
    }

    override suspend fun completeOnboarding() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    /** Either answer counts as asked, so Today never offers the reminder twice. */
    override suspend fun setReminder(enabled: Boolean) {
        store.edit {
            it[REMINDER] = enabled
            it[REMINDER_ASKED] = true
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val USAGE_STATS = booleanPreferencesKey("usage_stats")
        val CRASH_REPORTS = booleanPreferencesKey("crash_reports")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CITY = stringPreferencesKey("city")
        val REMINDER = booleanPreferencesKey("reminder")
        val REMINDER_ASKED = booleanPreferencesKey("reminder_asked")
    }
}
