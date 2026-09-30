package dev.pokedex.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.feature.reminder.ReminderScheduler
import dev.pokedex.feature.widget.TodayWidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class PokedexApplication : Application() {

    @Inject lateinit var widgetSync: TodayWidgetSync
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var reminders: ReminderScheduler

    /** Lives as long as the process; used only for light background work like keeping the widget current. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        widgetSync.start(appScope)
        // The reminder follows the saved setting, including after an update or a restore.
        preferences.preferences.map { it.reminder }.distinctUntilChanged()
            .onEach { reminders.apply(it) }
            .launchIn(appScope)
    }
}
