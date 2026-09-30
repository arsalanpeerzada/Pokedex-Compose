package dev.pokedex.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.pokedex.feature.widget.TodayWidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class PokedexApplication : Application() {

    @Inject lateinit var widgetSync: TodayWidgetSync

    /** Lives as long as the process; used only for light background work like keeping the widget current. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        widgetSync.start(appScope)
    }
}
