# Pokedex

A living, context-aware Pokédex for Android, built with Jetpack Compose as a personal learning project. The full plan is in [docs/implementation-plan.md](docs/implementation-plan.md).

## Stack
- Kotlin 2.4, AGP 9.4 (Gradle 9.6, JDK 17), compile SDK 37
- Jetpack Compose (BOM 2026.09.00), Material 3, Navigation 3
- Coil 3 for artwork

## Modules
| Module | What's in it |
|---|---|
| `:app` | Activity, navigation, app shell |
| `:core:model` | Pokémon, types, collection and daily-pick models |
| `:core:data` | Sample data (the PokeAPI + Room layer comes next) |
| `:core:designsystem` | Colours, fonts, shapes, icons and components from the Figma foundations |
| `:feature:today`, `:feature:pokedex`, `:feature:detail`, `:feature:collection` | One screen each, with light and dark previews |

## Build
Open the folder in Android Studio, or run `gradlew :app:assembleDebug`.

## Notices
Unofficial fan project for learning. Pokémon and all related names are trademarks of their respective owners. Not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company. Artwork is loaded from PokeAPI's sprite repository.

Fonts: Fredoka, Nunito and Silkscreen, under the SIL Open Font Licence (see `core/designsystem/src/main/assets/licenses`).
