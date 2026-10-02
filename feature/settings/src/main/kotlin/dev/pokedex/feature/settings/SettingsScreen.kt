package dev.pokedex.feature.settings

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pokedex.core.data.DownloadProgress
import dev.pokedex.core.data.PokemonRepository
import dev.pokedex.core.data.UserPreferencesRepository
import dev.pokedex.core.designsystem.component.DexGlassButton
import dev.pokedex.core.designsystem.component.DexIconButton
import dev.pokedex.core.designsystem.component.DexPreviews
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.DexProgressBar
import dev.pokedex.core.designsystem.component.DexSwitchRow
import dev.pokedex.core.designsystem.component.DexTextField
import dev.pokedex.core.designsystem.component.DexTopBar
import dev.pokedex.core.designsystem.component.FilterPill
import dev.pokedex.core.designsystem.component.GlassCard
import dev.pokedex.core.designsystem.component.rememberNotificationPermission
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.model.ThemeMode
import dev.pokedex.core.model.Units
import dev.pokedex.core.model.UserPreferences
import dev.pokedex.core.sync.OfflineDownload
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    pokemon: PokemonRepository,
    private val download: OfflineDownload,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun setTheme(theme: ThemeMode) = viewModelScope.launch { repository.setTheme(theme) }
    fun setUnits(units: Units) = viewModelScope.launch { repository.setUnits(units) }
    fun setUsageStats(enabled: Boolean) = viewModelScope.launch { repository.setUsageStats(enabled) }
    fun setCrashReports(enabled: Boolean) = viewModelScope.launch { repository.setCrashReports(enabled) }
    fun setCity(city: String) = viewModelScope.launch { repository.setCity(city) }
    fun setReminder(enabled: Boolean) = viewModelScope.launch { repository.setReminder(enabled) }

    val offline: StateFlow<OfflineState> = combine(pokemon.downloadProgress(), download.active) { progress, active -> OfflineState(progress, active) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfflineState())

    fun startDownload(wifiOnly: Boolean) = download.start(wifiOnly)
    fun stopDownload() = download.cancel()
}

/** The optional full download: how far it's got, and whether it's queued or running. */
data class OfflineState(val progress: DownloadProgress = DownloadProgress(0, 0), val active: Boolean = false)

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        preferences = prefs,
        offline = offline,
        onDownload = { viewModel.startDownload(it) },
        onStopDownload = { viewModel.stopDownload() },
        versionName = remember { context.versionName() },
        onBack = onBack,
        onTheme = { viewModel.setTheme(it) },
        onUnits = { viewModel.setUnits(it) },
        onUsageStats = { viewModel.setUsageStats(it) },
        onCrashReports = { viewModel.setCrashReports(it) },
        onCity = { viewModel.setCity(it) },
        onReminder = { viewModel.setReminder(it) },
        onSetWallpaper = { context.openLiveWallpaperPicker() },
        readLicence = { name -> context.assets.open("licenses/$name").bufferedReader().use { it.readText() } },
    )
}

/** In the public repository; readable once the docs are merged to main. */
private const val PRIVACY_POLICY_URL = "https://github.com/arsalanpeerzada/Pokedex-Compose/blob/main/docs/privacy-policy.md"

private val FontLicences =listOf("Fredoka" to "Fredoka-OFL.txt", "Nunito" to "Nunito-OFL.txt", "Silkscreen" to "Silkscreen-OFL.txt")

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    offline: OfflineState,
    onDownload: (wifiOnly: Boolean) -> Unit,
    onStopDownload: () -> Unit,
    versionName: String,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (Units) -> Unit,
    onUsageStats: (Boolean) -> Unit,
    onCrashReports: (Boolean) -> Unit,
    onCity: (String) -> Unit,
    onReminder: (Boolean) -> Unit,
    readLicence: (String) -> String,
    onSetWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DexTheme.colors
    val uriHandler = LocalUriHandler.current
    var notificationsBlocked by rememberSaveable { mutableStateOf(false) }
    val askForNotifications = rememberNotificationPermission { granted ->
        notificationsBlocked = !granted
        if (granted) onReminder(true)
    }
    var licence by rememberSaveable { mutableStateOf<String?>(null) }
    var city by rememberSaveable(preferences.city) { mutableStateOf(preferences.city.orEmpty()) }
    val cityChanged = city.trim() != preferences.city.orEmpty()

    Column(modifier.fillMaxSize().background(colors.background)) {
        DexTopBar(title = "Settings", navigation = { DexIconButton(DexIcons.Back, "Back", onBack) })
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Appearance") {
                Text("Theme", style = DexTheme.type.titleMedium, color = colors.text)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterPill(mode.label, selected = preferences.theme == mode, onClick = { onTheme(mode) })
                    }
                }
                Text("Units", style = DexTheme.type.titleMedium, color = colors.text, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Units.entries.forEach { units ->
                        FilterPill(units.label, selected = preferences.units == units, onClick = { onUnits(units) })
                    }
                }
                Text(
                    if (preferences.units == Units.Metric) "Metres, kilograms and °C." else "Feet and inches, pounds and °F.",
                    style = DexTheme.type.labelMedium,
                    color = colors.textSecondary,
                )
            }
            Section("Today") {
                Text("Your city", style = DexTheme.type.titleMedium, color = colors.text)
                Text(
                    "Today picks a Pokémon to suit your local weather. Only your city's rough position (about 1 km) is sent to the weather service.",
                    style = DexTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
                DexTextField(
                    value = city,
                    onValueChange = { city = it },
                    placeholder = "City, for example Leeds",
                    icon = DexIcons.Pin,
                    capitalization = KeyboardCapitalization.Words,
                    onImeAction = { if (cityChanged) onCity(city) },
                )
                if (cityChanged) {
                    TextButton(onClick = { onCity(city) }) {
                        Text(if (city.isBlank()) "Remove city" else "Save city", style = DexTheme.type.labelLarge, color = colors.accent)
                    }
                }
            }
            Section("Notifications") {
                DexSwitchRow(
                    title = "Daily reminder",
                    body = "Each morning at 9:00, unless you've already guessed. It never gives the answer away.",
                    checked = preferences.reminder,
                    onChange = { on -> if (on) askForNotifications() else onReminder(false) },
                )
                if (notificationsBlocked) {
                    Text(
                        "Notifications are turned off for Pokedex. You can allow them in your phone's settings.",
                        style = DexTheme.type.labelMedium,
                        color = colors.accent,
                    )
                }
            }
            Section("Offline") {
                OfflineSection(offline, onDownload, onStopDownload)
            }
            Section("Home screen") {
                Text("Live wallpaper", style = DexTheme.type.titleMedium, color = colors.text)
                Text(
                    "Today's sky and weather, with today's Pokémon as a silhouette until you guess it.",
                    style = DexTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
                DexGlassButton("Set as live wallpaper", onSetWallpaper, icon = DexIcons.Star)
            }
            Section("Privacy") {
                DexSwitchRow(
                    title = "Share usage statistics",
                    body = "Anonymous counts of which screens are used. Never your location.",
                    checked = preferences.usageStats,
                    onChange = onUsageStats,
                )
                DexSwitchRow(
                    title = "Send crash reports",
                    body = "Helps fix crashes. Off unless you turn it on.",
                    checked = preferences.crashReports,
                    onChange = onCrashReports,
                )
                Text(
                    "This build collects nothing yet. Your choices are saved and will apply once reporting is added.",
                    style = DexTheme.type.labelMedium,
                    color = colors.textSecondary,
                )
            }
            Section("About") {
                Text("Pokedex $versionName", style = DexTheme.type.titleMedium, color = colors.text)
                Text(
                    "Unofficial fan project for learning. Pokémon and all related names are trademarks of their respective owners. " +
                        "Not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company.",
                    style = DexTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
                Text("Pokémon data, artwork and cries come from PokeAPI.", style = DexTheme.type.bodyMedium, color = colors.textSecondary)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Open the privacy policy") { uriHandler.openUri(PRIVACY_POLICY_URL) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Privacy policy", style = DexTheme.type.bodyLarge, color = colors.accent)
                }
                Text("Font licences (SIL Open Font Licence)", style = DexTheme.type.titleMedium, color = colors.text, modifier = Modifier.padding(top = 4.dp))
                FontLicences.forEach { (font, file) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button, onClickLabel = "Read licence") { licence = file },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(font, style = DexTheme.type.bodyLarge, color = colors.accent)
                    }
                }
            }
        }
    }

    licence?.let { file ->
        val text = remember(file) { runCatching { readLicence(file) }.getOrDefault("The licence text couldn't be opened.") }
        AlertDialog(
            onDismissRequest = { licence = null },
            confirmButton = { TextButton(onClick = { licence = null }) { Text("Close", color = colors.accent) } },
            title = { Text(FontLicences.first { it.second == file }.first, style = DexTheme.type.titleLarge, color = colors.text) },
            text = {
                Text(
                    text,
                    style = DexTheme.type.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun OfflineSection(offline: OfflineState, onDownload: (Boolean) -> Unit, onStop: () -> Unit) {
    val colors = DexTheme.colors
    val progress = offline.progress
    var wifiOnly by rememberSaveable { mutableStateOf(true) }
    Text("Download every Pokémon", style = DexTheme.type.titleMedium, color = colors.text)
    Text(
        "Normally each Pokémon downloads the first time you open it. This fetches them all now, " +
            "so search, filters and Today work fully offline. It's about 2,000 small requests to PokeAPI, sent gently in the background.",
        style = DexTheme.type.bodyMedium,
        color = colors.textSecondary,
    )
    if (progress.total > 0) {
        DexProgressBar(progress.complete / progress.total.toFloat(), Modifier.fillMaxWidth())
        Text(
            when {
                progress.done -> "All %,d Pokémon are downloaded.".format(progress.total)
                offline.active -> "Downloading: %,d of %,d. You can leave this screen.".format(progress.complete, progress.total)
                else -> "%,d of %,d Pokémon downloaded.".format(progress.complete, progress.total)
            },
            style = DexTheme.type.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (!progress.done) {
        DexSwitchRow(
            title = "Wi-Fi only",
            body = "Waits for Wi-Fi before downloading.",
            checked = wifiOnly,
            onChange = { wifiOnly = it },
        )
        if (offline.active) {
            DexGlassButton("Stop download", onStop, icon = DexIcons.Close)
        } else {
            DexPrimaryButton("Download all", { onDownload(wifiOnly) }, icon = DexIcons.Plus)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = DexTheme.type.labelLarge, color = DexTheme.colors.textSecondary, modifier = Modifier.semantics { heading() })
        GlassCard { content() }
    }
}

/** Opens the system preview for our wallpaper, or the general live wallpaper list if a phone has no preview. */
private fun Context.openLiveWallpaperPicker() {
    val ours = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
        ComponentName(packageName, "dev.pokedex.feature.wallpaper.TodayWallpaperService"),
    )
    try {
        startActivity(ours)
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
    }
}

private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

@DexPreviews
@Composable
private fun SettingsPreview() {
    DexTheme {
        SettingsScreen(
            UserPreferences(), OfflineState(DownloadProgress(412, 1025), active = true), onDownload = {}, onStopDownload = {},
            versionName = "0.1.0", onBack = {}, onTheme = {}, onUnits = {}, onUsageStats = {}, onCrashReports = {}, onCity = {}, onReminder = {}, readLicence = { "" }, onSetWallpaper = {},
        )
    }
}
