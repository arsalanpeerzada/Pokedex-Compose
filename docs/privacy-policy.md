# Pokedex privacy policy

> **Draft, not yet reviewed.** This describes what the app does as of 5 October 2026. It is not legal advice. Have it reviewed before publishing it or releasing the app to the public, especially the "Children" section below. Remove this note, and fill in the two placeholders under "Who makes Pokedex", once it has been reviewed.

**Last updated:** 5 October 2026

Pokedex is an unofficial, non-commercial fan app built as a personal learning project. It has no accounts, no adverts and no in-app purchases.

## Who makes Pokedex
Pokedex is made and published on Google Play by [developer name, as shown on Google Play], an individual developer. That developer is responsible for the app's handling of your data. Contact details are at the end of this policy.

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

## Keeping and deleting your data
- **On your phone:** everything above stays until you delete it. Clearing the app's storage in Android's settings, or uninstalling the app, deletes all of it. Removing the city in Settings deletes the city and its position. Earlier days' "Why today?" lines that named it stay in your history until you clear the app's storage.
- **Off your phone:** the developer runs no server and keeps no copy of your data, so there is nothing to ask the developer to delete. The services the app contacts (PokeAPI, GitHub, Open-Meteo, Google) handle requests under their own privacy policies.

## Security
Every request the app makes uses an encrypted connection (HTTPS).

## What the app doesn't do
- It doesn't use your device's location or GPS; you type a city instead.
- It has no accounts and collects no names, email addresses or contacts.
- This version sends no usage statistics and no crash reports. Settings has switches for both, which are off unless you turn them on. If reporting is added in a later version (Firebase Analytics and Crashlytics), it will run only when you've switched it on, and this policy will be updated first.
- The daily reminder is a notification scheduled on your phone. It is off unless you turn it on, and it never names the Pokémon.

## Children
Pokémon is popular with children. The app has no accounts, no adverts, no chat and no tracking. The only thing it sends that you type yourself is the rough position of an optional city, used for the weather. *For review: confirm whether anything more is needed under the UK Age Appropriate Design Code and similar rules before a public release.*

## Changes to this policy
If the app's handling of data changes, this page will be updated before the change ships, with the date at the top.

## Google Play
If you install Pokedex from Google Play, Google handles the download, updates and any reviews you leave, under Google's own privacy policy. Through Google Play Console, the developer sees the statistics Google gives every developer: aggregated figures such as install counts, and crash reports from phones whose owners let Android share diagnostics with Google. Reviews are public.

## Contact
Questions or requests about this policy or your data:
- Email: [contact email]
- Or open an issue at https://github.com/arsalanpeerzada/Pokedex-Compose/issues. Issues are public, so don't include personal details.

Pokémon and all related names are trademarks of their respective owners. Pokedex is not affiliated with or endorsed by Nintendo, Game Freak or The Pokémon Company.
