# Pokedex privacy policy

> **Draft, not yet reviewed.** This describes what the app does as of 1 October 2026. It is not legal advice. Have it reviewed before publishing it or releasing the app to the public, especially the "Children" section below.

Pokedex is an unofficial, non-commercial fan app built as a personal learning project. It has no accounts, no adverts and no in-app purchases.

## What stays on your phone
- **The downloaded Pokédex:** names, types, stats, entries, forms and evolution chains, cached from PokeAPI. Artwork and cries are also cached so the app works offline.
- **Your activity:**
  - Your collection: caught, seen and favourite Pokémon.
  - Your teams.
  - Each day's Pokémon with your guesses, hints, streak, and that day's "Why today?" line.
  - The "Why today?" line can name your city and describe the weather in general words, for example "Rain in Leeds today".
- **Your settings:**
  - The city you typed, if you added one, with its position rounded to about 1 km and its country code.
  - Your theme, units, reminder and privacy choices.
- **Share cards:** when you tap Share, the card image is saved briefly in the app's cache and shared only with the app you choose. Each new card replaces the last.

App backups are switched off, so none of this is copied to cloud backups or to a new phone. Uninstalling the app, or clearing its storage in Android's settings, deletes all of it.

The home-screen widget and the live wallpaper only show what's already on your phone. They send nothing anywhere.

## What leaves your phone, and why
- **Pokémon data** is requested from PokeAPI (pokeapi.co): each Pokémon the first time you open it, or all of them at once if you choose "Download all" in Settings. Artwork and cries are loaded from PokeAPI's public repositories on GitHub (raw.githubusercontent.com). These requests contain no personal information, but, like any web request, they reveal your IP address to those services.
- **Weather, only if you add a city:**
  - **Finding your city:** the city name you type is turned into a position by Android's built-in geocoder. On most phones this service is provided by Google Play services.
  - **Fetching the weather:** your city's rounded position (about 1 km) is sent to the weather service. That's Open-Meteo (open-meteo.com), or Google's Weather API if the build has a Google key.
  - **Keeping it:** readings are kept in memory only, never written to storage. Google's are kept for up to one hour and Open-Meteo's for up to three.
- **Opening links:** links in Settings, such as this policy, open in your browser.

## What the app doesn't do
- It doesn't use your device's location or GPS; you type a city instead.
- It has no accounts and collects no names, email addresses or contacts.
- This version sends no usage statistics and no crash reports. Settings has switches for both, which are off unless you turn them on. If reporting is added in a later version (Firebase Analytics and Crashlytics), it will run only when you've switched it on, and this policy will be updated first.
- The daily reminder is a notification scheduled on your phone. It is off unless you turn it on, and it never names the Pokémon.

## Children
Pokémon is popular with children. The app has no accounts, no adverts, no chat and no tracking. The only thing it sends that you type yourself is the rough position of an optional city, used for the weather. *For review: confirm whether anything more is needed under the UK Age Appropriate Design Code and similar rules before a public release.*

## Changes to this policy
If the app's handling of data changes, this page will be updated before the change ships, with the date at the top.

## Contact
Questions or requests: open an issue at https://github.com/arsalanpeerzada/Pokedex-Compose/issues.

Pokémon and all related names are trademarks of their respective owners. Pokedex is not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company.
