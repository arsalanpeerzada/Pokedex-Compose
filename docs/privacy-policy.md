# Pokedex privacy policy

> **Draft, not yet reviewed.** This describes what the app does as of 30 September 2026. It is not legal advice. Have it reviewed before publishing it or releasing the app to the public.

Pokedex is an unofficial, non-commercial fan app built as a personal learning project. It has no accounts, no adverts and no in-app purchases.

## What stays on your phone
- The downloaded Pokédex: names, types, stats, entries and evolution chains, cached from PokeAPI.
- Your collection (caught, seen and favourite Pokémon), your teams, and your daily guesses and streak.
- Your settings, including the city you typed, if you added one, and its position rounded to about 1 km.

App backups are switched off, so none of this is copied to cloud backups or to a new phone. Uninstalling the app, or clearing its storage in Android's settings, deletes all of it.

## What leaves your phone, and why
- **Pokémon data** is requested from PokeAPI (pokeapi.co). Artwork and cries are loaded from PokeAPI's public repositories on GitHub (raw.githubusercontent.com). These requests contain no personal information, but, like any web request, they reveal your IP address to those services.
- **Weather, only if you add a city:**
  - Your city's rough position is sent to the weather service. That's Open-Meteo (open-meteo.com), or Google's Weather API if the build has a Google key.
  - To turn the city name into a position, the app uses Android's built-in geocoder. On most phones this is provided by Google Play services.
  - Weather readings are kept in memory only, for up to three hours.

## What the app doesn't do
- It doesn't use your device's location or GPS; you type a city instead.
- This version sends no usage statistics and no crash reports. Settings has switches for both, which are off unless you turn them on. If reporting is added in a later version (Firebase Analytics and Crashlytics), it will only run when you've switched it on, and this policy will be updated first.
- The daily reminder is a notification scheduled on your phone. It is off unless you turn it on.

## Contact
Questions or requests: open an issue at https://github.com/arsalanpeerzada/Pokedex-Compose/issues.

Pokémon and all related names are trademarks of their respective owners. Pokedex is not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company.
