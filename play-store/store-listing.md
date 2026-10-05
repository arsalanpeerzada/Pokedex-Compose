# Store listing (Play Console > Grow users > Store presence > Main store listing)

Language: English (United Kingdom), en-GB. Character counts are checked against Play's limits.

## App name (30 characters max)

```
Pokedex
```

7 characters. This matches `app_name`. Play rejects names with emoji, ALL CAPS (unless it's the brand), or words such as "best", "#1", "free" or "new".

## Short description (80 characters max)

```
A new Pokémon each day, picked by your weather. Guess it, then explore them all.
```

## Full description (4,000 characters max)

```
Pokedex picks a Pokémon for you every day, based on your weather, the time of day, the season and special days. Guess it from its silhouette, find out why it was picked, then explore all 1,025 Pokémon, even offline.

Unofficial fan app. Pokedex is not affiliated with, endorsed or sponsored by Nintendo, Game Freak, Creatures or The Pokémon Company. Pokémon and all related names are trademarks of their respective owners.

TODAY'S POKÉMON
• A new pick every day, chosen from the weather in your city, the time of day, the season and special days, including national days.
• Guess it from its silhouette, with a weather-only hint if you're stuck.
• After the reveal: "Why today?", its story, your streak and a countdown to tomorrow.
• Share a card of today's Pokémon with the app of your choice.

THE FULL POKÉDEX
• All 1,025 Pokémon, with search and filters for type, generation, legendary, mythical, caught and favourites.
• Sort by number or by name, and look up any match-up in the type chart.
• Works offline: each Pokémon is kept once you've opened it, or you can download them all in Settings.

EVERY DETAIL
• About, Stats, Evolution, Matchups and Forms for every Pokémon.
• Shiny artwork, a size comparison, its cry, and a side-by-side Compare.

COLLECTION AND TEAMS
• A sticker album by generation, with your guess streak.
• Build teams of six and see shared weaknesses, attack coverage and suggestions.

ON YOUR HOME SCREEN
• A widget with today's mystery Pokémon, revealed once you've guessed it.
• A live wallpaper with today's weather and today's Pokémon.
• An optional daily reminder that never gives the answer away.

FOR EVERY SCREEN
Light and dark themes, with layouts for phones, foldables and tablets.

PRIVATE BY DESIGN
• No accounts, no adverts and no in-app purchases.
• No tracking. This version sends no usage statistics and no crash reports.
• The app never asks for your location. Weather uses only the rough position (about 1 km) of a city you type, and you can skip it.

Pokémon data, artwork and cries come from PokeAPI. Weather data comes from Open-Meteo.
```

If the release build includes a Google Weather key, change the last line to "Weather data comes from Open-Meteo or Google."

## Release notes for the first release (500 characters max)

```
First release: today's Pokémon picked by your weather, the full Pokédex with offline download, collection, team builder, home-screen widget, live wallpaper and an optional daily reminder.
```

## Categorisation (Store settings)

- **App or game:** App
- **Category:** Entertainment, which fits the daily guess, widget and wallpaper. Books & Reference is the other sensible choice.
- **Tags:** choose up to five from the list Play Console offers. Pick the ones closest to "trivia", "reference" and "collection". I haven't listed exact tag names because Play's list changes.

## Store listing contact details (shown publicly)

- **Email (required):** `[your public contact email]`. Use a personal address you're happy to publish, not a work one.
- **Website (optional):** https://github.com/arsalanpeerzada/Pokedex-Compose
- **Phone (optional):** leave blank.

## Privacy policy URL (App content > Privacy policy)

```
https://github.com/arsalanpeerzada/Pokedex-Compose/blob/main/docs/privacy-policy.md
```

This is the same link the app opens from Settings. Merge the updated policy into `main` before you submit.

## Graphics (in `graphics/`)

| Asset | File | Play requirement |
|---|---|---|
| App icon | `icon-512.png` | 512 x 512 px, 32-bit PNG, up to 1 MB. Play adds the rounded corners, so the file is a full square. |
| Feature graphic | `feature-graphic-1024x500.jpg` | 1024 x 500 px, JPEG or 24-bit PNG with no alpha. Required. |
| Phone screenshots | `phone-1-guess.jpg` to `phone-6-collection.jpg` | 2 to 8 images, JPEG or 24-bit PNG. Each side between 320 and 3,840 px, and the long side at most twice the short side. These are 1236 x 2196 px (9:16). |

Upload the screenshots in number order. The first three show most often in search results.

Tablet screenshots (7-inch and 10-inch) are optional. Without them, Play may show the app less prominently on tablets. They can be added later from a tablet emulator.
