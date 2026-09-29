# Pokedex

A living, context-aware Pokédex for Android, built with Jetpack Compose as a personal learning project. The full plan is in [docs/implementation-plan.md](docs/implementation-plan.md).

## Stack
- Kotlin 2.4, AGP 9.4 (Gradle 9.6, JDK 17), compile SDK 37
- Jetpack Compose (BOM 2026.09.00), Material 3, Navigation 3
- Hilt for dependency injection, Room for the offline cache, Ktor for PokeAPI
- Coil 3 for artwork

Application ID: `io.github.arsalanpeerzada.pokedex`.

## Modules
| Module | What's in it |
|---|---|
| `:app` | Application, activity, Navigation 3 shell with shared-element transitions |
| `:core:model` | Pokémon, types, generations, collection and daily-pick models |
| `:core:network` | PokeAPI client (Ktor) |
| `:core:database` | Room database: the cached Pokédex and the user's caught, seen and favourite state |
| `:core:data` | Offline-first repository, plus sample data for previews |
| `:core:designsystem` | Colours, fonts, shapes, icons, motion and components from the Figma foundations |
| `:feature:today`, `:feature:pokedex`, `:feature:detail`, `:feature:collection` | One screen and ViewModel each, with light and dark previews |

## Data
The app is offline-first, following PokeAPI's fair-use policy. The species index is one request, made once. Types and facts for each Pokémon are fetched the first time it is shown (at most four at a time) and kept in Room for good.

## Build
Open the folder in Android Studio, or run `gradlew :app:assembleDebug`.

## Notices
Unofficial fan project for learning. Pokémon and all related names are trademarks of their respective owners. Not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company. Artwork is loaded from PokeAPI's sprite repository.

Fonts: Fredoka, Nunito and Silkscreen, under the SIL Open Font Licence (see `core/designsystem/src/main/assets/licenses`).
