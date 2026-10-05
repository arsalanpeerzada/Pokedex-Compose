# Pokedex

A living, context-aware Pokédex for Android, built with Jetpack Compose as a personal learning project. The full plan is in [docs/implementation-plan.md](docs/implementation-plan.md).

**Status (2 October 2026):** feature-complete for v1 (version 0.9.0). Every planned v1 screen is built and covered by CI. The next step is a check on a real device, then 1.0.0. See [Where things stand](#where-things-stand).

## What it does
- **Today:**
  - **The pick:** a Pokémon of the day chosen from your weather, time of day, season and special days, including national days.
  - **The game:** guess it from a silhouette, with a weather-only hint, then the reveal.
  - **After the reveal:** "Why today?", its story, a streak, a share card and a countdown to tomorrow.
- **Pokédex:** all 1,025 Pokémon, offline after the first download.
  - Search, plus filters for type, generation, legendary, mythical, caught and favourites.
  - Sort, and a type chart.
- **Detail:**
  - Tabs: About, Stats, Evolution, Matchups and Forms.
  - Shiny artwork, a size comparison, the cry, and Compare.
- **Collection:** a sticker album by generation, and the guess streak.
- **Team Builder:** teams of six, shared weaknesses, attack coverage and suggestions.
- **Outside the app:** a home-screen widget, a live wallpaper, and an optional daily reminder.
- **Layouts:** phones, foldables and tablets, in light and dark.

## Stack
- Kotlin 2.4, AGP 9.4 (Gradle 9.6, JDK 17), compile SDK 37, min SDK 26
- Jetpack Compose (BOM 2026.09.00), Material 3 and its adaptive list-detail layout, Navigation 3
- Hilt, Room, DataStore, Ktor, Coil 3, WorkManager, Glance
- Tests: JUnit, coroutines-test, Robolectric (Compose UI tests and drawing tests run on the JVM)

Application ID: `io.github.arsalanpeerzada.pokedex`.

## Modules
| Module | What's in it |
|---|---|
| `:app` | Application, activity, the Navigation 3 shell, the navigation rail and list-detail on wide screens |
| `build-logic` | Convention plugins: shared module set-up for SDK levels, Java, Compose, Hilt and tests |
| `:core:model` | Pokémon, types, generations, teams, units and other plain models |
| `:core:network` | PokeAPI client (Ktor) |
| `:core:database` | Room: the cached Pokédex, user state, teams and daily results, with automatic migrations |
| `:core:data` | Offline-first repositories and settings, plus sample data for previews |
| `:core:weather` | Weather for the user's city: Google Weather with a key, Open-Meteo without |
| `:core:domain` | The context engine and the saved daily pick |
| `:core:sync` | The optional "download every Pokémon" background job |
| `:core:designsystem` | Colours, fonts, shapes, icons, motion and components from the Figma foundations |
| `:feature:*` | One module per screen or surface: onboarding, today, pokedex, detail, collection, teams, settings, widget, reminder and wallpaper. Screens have light and dark previews, which are the design reference |

## Data and privacy
- **Offline-first,** following PokeAPI's fair-use policy:
  - The species index is one request and the type index 18 more, each made once.
  - Each Pokémon's details are fetched the first time they're needed, at most four requests at a time, and kept in Room.
- **No accounts, adverts or tracking.**
  - Usage statistics, crash reports and the reminder are off unless turned on.
  - Weather uses only the rough position (about 1 km) of a city you type.
- See the draft [privacy policy](docs/privacy-policy.md).

## Build
Open the folder in Android Studio, or run `gradlew :app:assembleDebug`.

- **Weather** works with no setup, using Open-Meteo. To use Google Weather instead:
  - Copy `secrets.properties.example` to `secrets.properties` and add your key.
  - In Google Cloud, restrict the key to the Weather API and to this app's package name and signing certificate, and set a daily quota cap.
- **Release signing:** copy `keystore.properties.example` to `keystore.properties` and point it at your keystore. Without it, `gradlew :app:assembleRelease` builds an unsigned APK.
- **Formatting:** `gradlew spotlessCheck` reports formatting against ktlint, and `gradlew spotlessApply` fixes it. It isn't part of CI yet.

Git ignores `secrets.properties`, `keystore.properties` and keystores. Never commit them.

## Checks
- **CI (GitHub Actions)** runs `gradlew assembleDebug testDebugUnitTest lintDebug :app:assembleRelease` on every push to `main` and `day-*` branches, and on pull requests.
- **Dependabot** opens weekly pull requests for library and GitHub Actions updates.

## Where things stand
**Done:** every v1 screen and surface in the plan. They're built, unit-tested and UI-tested on the JVM, and the debug and minified release builds pass in CI.

**Before 1.0.0 (needs a device, an account or a person):**
1. Run it on a real phone or emulator: every screen, the widget, the wallpaper, the reminder, sharing, and the minified release build.
2. A TalkBack walk-through, then Baseline Profiles and screenshot tests, which also need a device.
3. Firebase on the owner's personal account: Crashlytics and Analytics (opt-in, switches already in Settings) and Remote Config for the engine's weights.
4. A qualified review of the draft privacy policy, and a check of Google's full weather caching terms if a Google key is used.
5. A signing key, then Firebase App Distribution, then Play. The store listing, graphics and App content answers are ready in [play-store/](play-store/README.md).

**Later (v2):** city search with Places Autocomplete, the "World today" map, real-size AR, and Tap-to-Dex.

## Notices
Unofficial fan project for learning. Pokémon and all related names are trademarks of their respective owners. The launcher icon is a classic Poké Ball, the owner's choice for personal builds. The Poké Ball design is a trademark of Nintendo and The Pokémon Company, so replace the icon with an original design before publishing the app anywhere. Not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company. Pokémon data, artwork and cries come from PokeAPI.

Weather data comes from Open-Meteo (CC BY 4.0), or from Google when a Google key is configured. The app shows each provider's attribution next to the weather.

Fonts: Fredoka, Nunito and Silkscreen, under the SIL Open Font Licence (see `core/designsystem/src/main/assets/licenses`).
