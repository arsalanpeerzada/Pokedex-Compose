# Pokedex: implementation plan

Version 4, 29 September 2026. Status: design phase, no code yet.
Personal learning project.

Facts, versions and terms below were checked against primary sources on 29 September 2026 (see Sources). Exact dependency versions are pinned when we scaffold.

Changes in version 4:
- No Pokémon regions anywhere.
- A vibrant visual direction, with light and dark designed together and themed fonts (section 7).
- Fixes from the wireframe review folded into the v1 scope (section 3).

Version 3 named the app Pokedex, made it English only and moved design to Figma.

---

## 1. Ground rules

- **Personal accounts only.** Code goes to your personal GitHub; Firebase, Google Cloud and Figma run on your personal accounts.
- **Design before code.** Nothing is implemented until the v1 screens are signed off in Figma (section 7).
- **Language:** English only. All text still lives in string resources (standard practice), so adding a language later stays cheap.
- **No Pokémon regions, anywhere.** No region screens, filters, labels, colours or grouping. People browse by type and generation instead. Alternate forms whose data names include a region get neutral labels, such as "Vulpix, alternate form".
- **Secrets never in Git.** `google-services.json`, Maps Platform keys and the signing keystore live in git-ignored `local.properties` locally and in GitHub Actions secrets in CI.
- **Accounts are yours to create.** You create the Figma account, Firebase project, Google Cloud billing account, API keys and keystore. I give step-by-step instructions and wire up the code.
- **Outside services need your OK first.** Before any batch of writes to your Figma file, I say what I'm about to create or change and wait for you to confirm.
- **Pokémon IP hygiene.**
  - The name Pokedex is the franchise's own term, which reinforces keeping the app off the Play Store and non-commercial.
  - Use an original icon: no Poké Ball, and no official Pokédex device art.
  - Put a trademark notice in the README and the About screen.
  - No ads, payments or assets ripped from the games.
- **Privacy basics for real users.** Crash reports and Analytics both stay off until the user opts in (decided 29 September 2026). Location is approximate only and never sent to Analytics. A short privacy policy is published on GitHub Pages.
- **Fair use of free APIs.** PokeAPI's fair-use policy asks clients to cache locally: we fetch the full index once and cache every detail in Room.

## 2. Vision and quality bar

**The best Pokédex around: a living, context-aware Pokédex.**

Signature experiences:
1. **Today:** a Pokémon of the day chosen from your weather, time of day, season, city and special days, with guess, reveal and story.
2. **A complete, instant, offline Pokédex:** 1,025 species, 1,351 entries including forms.
3. **Team Builder** with type-coverage and weakness analysis.
4. **Beyond the app:** widgets, live wallpaper, real-size AR and Tap-to-Dex.

Quality bars (measured, not claimed):
- Fully usable offline after the first sync.
- Smooth scrolling and fast start-up, proven with Macrobenchmark and Baseline Profiles.
- Crash-free users above a target we set once we have a baseline.
- Complete TalkBack support and 200% font scaling.
- Vibrant colour in both light and dark, with text contrast of at least 4.5:1 and control outlines of at least 3:1.
- Phone, foldable and tablet layouts.

## 3. Scope by release

| Area | v1 | v2 | Later |
|---|---|---|---|
| Onboarding | Two steps: Welcome and your city. The offline download starts in the background. Notification permission is asked right after the first reveal. The analytics question comes later as a one-time card; usage stats stay off until then | | |
| Today | Context engine; daily guess with right and wrong feedback; the reveal moment; a countdown to tomorrow's Pokémon; "Why today?" after the reveal (before it, the hint mentions only the weather); story sections; share card | Terrain signal (elevation), air quality signal | AI-written story grounded in official entries |
| Pokédex | Grid, search, filters (type, generation, legendary, mythical), sort, caught marks on cards, offline | | |
| Detail | One layout shared with Today: about, stats, evolution, forms and shiny, matchups, cry, 2D size comparison, caught toggle, add to team | Compare | |
| Team Builder | Six slots, several teams, coverage grid, weak spots with 2× and 4× shown, suggestions | Share a team by deep link or NFC | |
| Collection | Sticker album: caught Pokémon in colour, seen as silhouettes, unseen as numbers. Also progress by generation, latest caught, favourites and the guess streak | | |
| Type chart | Phones: pick an attacking type to see grouped lists, plus a calculator for defence against two types. Tablets: the full 18 x 18 grid | | |
| Settings | Reachable from every tab: city, notifications, theme, units, privacy and data, privacy policy, data sources and attribution, licences, trademark notice | | |
| Outside the app | Widgets (small, medium, large) with before-reveal and after-reveal states; daily notification | Live wallpaper | |
| Maps | City search (Places Autocomplete) | "World today" map | |
| AR | | Real-size AR standee using official artwork | True 3D, only with a clean model source |
| NFC | | Tap-to-Dex | |
| Platform | Opt-in Crashlytics, opt-in Analytics, Remote Config, App Distribution | | |

## 4. Context engine (the headline feature)

### Signals

| Signal | Source | Notes |
|---|---|---|
| Special days | Bundled calendar | Pokémon Day (27 February), Halloween (Ghost), New Year's Eve (Fire, for the fireworks), plus national days per country. Religious festivals: open decision. National days built on 1 October 2026, each checked against an official source and favouring Fire and Fairy ("celebration", first draft): Germany 3 October ([bundesregierung.de](https://www.bundesregierung.de/breg-de/service/tag-der-deutschen-einheit-442686)), France 14 July ([elysee.fr](https://www.elysee.fr/en/french-presidency/bastille-day-14-july)), the Netherlands 27 April, or 26 April when the 27th is a Sunday ([koninklijkhuis.nl](https://www.koninklijkhuis.nl/onderwerpen/activiteiten-en-werkzaamheden/koningsdag)), the United States 4 July ([usa.gov](https://www.usa.gov/holidays)), and Pakistan 23 March and 14 August (confirmed by the project owner; no official source could be read that day). Religious festivals: not included (decided 1 October 2026) |
| Season | Date plus hemisphere from latitude | Works in both hemispheres |
| Time of day | Google Weather `isDaytime`, sunrise and sunset | Dawn, day, dusk, night |
| Weather | Google Weather current conditions and daily forecast | 40 condition types grouped into the buckets below |
| Temperature | `feelsLikeTemperature` | "Hot" threshold lives in Remote Config |
| City | Places Autocomplete, or approximate location snapped to the nearest city | Seeds the daily pick; terrain (elevation) in v2 |

### Weather to type mapping (first draft, tunable)

| Bucket | Google condition types | Favoured types | Legendary moment (rare) |
|---|---|---|---|
| Clear and hot | CLEAR, MOSTLY_CLEAR with a high feels-like temperature | Fire, Ground | Moltres, Groudon |
| Clear and mild | CLEAR, MOSTLY_CLEAR | Grass, Normal, Bug | |
| Cloudy | PARTLY_CLOUDY, MOSTLY_CLOUDY, CLOUDY | Normal, Fairy, Poison | |
| Rain | all RAIN and SHOWERS types | Water, Bug | Kyogre in heavy rain |
| Thunder | THUNDERSTORM, THUNDERSHOWER, SCATTERED_THUNDERSTORMS, LIGHT_THUNDERSTORM_RAIN, HEAVY_THUNDERSTORM | Electric | Zapdos |
| Snow and hail | all SNOW types, HAIL, HAIL_SHOWERS, BLOWING_SNOW, RAIN_AND_SNOW | Ice, Steel | Articuno |
| Wind | WINDY, WIND_AND_RAIN | Flying, Dragon | Rayquaza |
| Night | `isDaytime` false, combined with the bucket above | Dark, Ghost, nocturnal species | |

Google's condition list has no fog, haze or dust type, so those need another signal (v2 air quality or visibility).

### Picking rules

- **Weighted random, seeded by date and city,** so everyone in the same city gets the same Pokémon that day.
- **Weather tilts the odds; it doesn't dictate.** A variety rule stops one type dominating a heatwave or a wet week, and no species repeats within 60 days in a city.
- **The daily pick uses the day's forecast bucket; the scene follows current conditions.** The sky, light, rain and lightning around the Pokémon change through the day. A forecast that changes during the morning could split a city's pick; that's acceptable for v1.
- **Legendaries are rare and conditional,** so they feel like events.
- **A "Why today?" line** is built from the signals that won, for example "Thunderstorm at 21:00: Electric weather, night-time".
- **Silhouette first.** Widgets, the notification and the wallpaper show a silhouette until the user reveals it in the app.
- **Remote Config holds all weights and thresholds,** so the engine can be tuned without a release.
- **Pure Kotlin in `:core:domain`,** with table-driven unit tests for every bucket and rule.

## 5. Tech stack

| Area | Choice | Notes |
|---|---|---|
| Language and build | Kotlin 2.4, AGP 9.4 (Gradle 9.6, JDK 17), version catalog, convention plugins, KSP | AGP 9's built-in Kotlin doesn't work with kapt |
| UI | Compose, Material 3 with our own vibrant scheme, edge-to-edge, shared-element transitions | Wallpaper-based dynamic colour is an optional setting, off by default. Expressive APIs are still experimental in stable Material 3 (1.4.0) |
| Theme | Generated from the Figma foundations with Material Theme Builder. Detail pages build a scheme from the artwork's colour (Palette plus Material Color Utilities) | Exports Compose `Color.kt` and `Theme.kt`, so design and code share one source |
| Fonts | With the Adventure pairing: Fredoka (headings), Nunito (text), Silkscreen (Pokédex numbers) | Bundled in the app so they work offline; all are open-source Google Fonts |
| Navigation | Navigation 3 | Stable since 19 November 2025, now 1.2.0; adaptive list-detail |
| Architecture | UI, domain and data layers; MVVM with unidirectional data flow | Domain holds the context engine, type chart and team analysis |
| DI | Hilt | |
| Networking | Ktor client with kotlinx.serialization | PokeAPI and Google Weather |
| Storage | Room 2.8 (full-text search), DataStore | Offline-first source of truth |
| Images and audio | Coil 3, Palette, Media3 | Media3 plays the cries (.ogg) |
| Background | WorkManager | Daily pick, weather refresh, data sync |
| Widgets | Glance | |
| Location and maps | Fused Location (approximate only), Places SDK Autocomplete, Maps Compose (v2) | No background location permission, ever |
| Weather | Google Weather API behind a `WeatherSource` interface | Open-Meteo as a switchable fallback |
| Firebase | Crashlytics (opt-in, collection disabled until consent), Analytics (opt-in), Remote Config, App Distribution | One project on your Gmail; it is also the Google Cloud project for Maps and Weather |
| AR (v2) | ARCore with SceneView (Compose, Filament, glTF) | Develop in the emulator's virtual scene, test on a phone |
| Live wallpaper (v2) | WallpaperService, Canvas, AGSL shaders (Android 13+) | Compose can't draw wallpapers |
| NFC (v2) | NFC reader mode, NDEF deep links | Needs a real phone |
| Testing | JUnit, coroutines-test, Turbine, fakes over mocks, Compose UI tests, Roborazzi | |
| Performance | Baseline Profiles, Macrobenchmark, R8 | |
| Quality and CI | GitHub Actions, Spotless with ktlint, Detekt, Compose lint rules, Renovate | Secrets come from GitHub Actions secrets |

Deliberately not used: kapt, LiveData, XML layouts, RxJava, Paging 3 (the full index is one request), and PokeAPI's GraphQL as the main source (beta, 100 calls an hour per IP).

## 6. Google Maps Platform and Weather

**Why Google Weather:**
- It returns what the engine needs: `isDaytime`, feels-like temperature, `thunderstormProbability`, 40 condition types, and sunrise and sunset.
- It covers every country except China, Cuba, Iran, North Korea and Syria.
- Your Firebase project is already a Google Cloud project, so it's one console and one bill.

**Costs (free monthly usage at time of checking):**
- Weather API: 10,000 calls a month.
- Maps SDK for Android: no usage charge.
- Places Autocomplete: session usage is free, and city search happens rarely anyway.
- Maps Platform requires a billing account on the project, even within free usage.

**Call budget.** 10,000 calls a month is about 330 a day. Hourly refresh (24 calls per user per day) covers only about a dozen daily users. Refreshing every three hours plus one daily forecast (about 9 calls per user per day) covers about 35. So:
- Refresh every three hours, cache in Room, and refresh on open only when the data is stale.
- Put a per-day quota cap on each API. A budget alert only warns; a quota cap actually stops spending.
- Remote Config switches `WeatherSource` to Open-Meteo if usage grows. Its free tier is for non-commercial use, which a personal, ad-free project should meet.
- The v2 "World today" map multiplies weather calls, so it's the first candidate for Open-Meteo or a small server-side cache.

**Key hygiene.**
- Restrict each key to its APIs and to the app's package name and signing certificate.
- Keep keys in `local.properties` and CI secrets, never in Git.

**Terms that shape the design:**
- **Attribution:** Google's weather attribution must be visible wherever weather data appears, so the designs reserve space for it (exact wording and styling from the Weather API policies page).
- **Caching:**
  - Weather content may only be cached temporarily.
  - On 1 October 2026, Google's own search summary of its [Service Specific Terms](https://cloud.google.com/maps-platform/terms/maps-service-terms) gave these periods: current conditions and hourly forecasts one hour, daily forecasts 24 hours, and "today's forecast" 30 days. The terms page itself still came back truncated, so re-check it before release.
  - The app follows the stricter reading:
    - Google's current conditions are kept in memory for one hour.
    - The saved "Why today?" line and hint use the app's own weather words, never the provider's text.
- **Attribution as built (30 September 2026):**
  - Google: "Source: Includes weather data from Google", shown next to the weather on Today.
  - Open-Meteo: "Weather data by Open-Meteo.com", with a link. Open-Meteo is the fallback when no Google key is set, and is licensed CC BY 4.0.
- **Primary purpose:** the terms bar apps whose primary purpose is providing weather information. Ours is a Pokédex that uses weather as an input, so that's fine.

## 7. Design in Figma (Sprint 0, before any code)

### Setup (done 29 September 2026)
The Figma connector is signed in with your personal account. All work happens in your **Peerzadas** team, on the Starter plan with a Full seat.

### Figma MCP limits (checked 29 September 2026)

| What | Limit on Starter with a Full seat | Notes |
|---|---|---|
| Writing to the canvas (`use_figma`) | Allowed with a Full seat, but it **does count** towards the 20 calls a month | Confirmed 29 September 2026: the limit was reached after about 20 `use_figma` calls. Only `create_new_file`, `whoami` and `add_code_connect_map` are exempt |
| Reading (`get_screenshot`, `get_design_context`, `get_variable_defs`, `get_metadata`) | Up to 20 calls a month | Professional with a Full or Dev seat raises this to 200 a day |
| Current write limitations | 20 KB response per call; no image import through `use_figma`; no custom fonts | Artwork goes in with `upload_assets`; Figma's built-in Google Fonts are fine |

How we work on Starter:
- I build directly in your file.
- Each build call reports back what it created, so checking my work doesn't cost a read.
- You review in Figma and paste screenshots into chat when something needs discussion. Pasted screenshots cost nothing.
- The monthly reads are saved for build time (tokens and final screens).
- Upgrade to Professional only if the 20 reads a month get in the way.

### File structure
The file is [Pokedex](https://www.figma.com/design/GhxZSeok2qh19z4X74MnEv), in your Drafts. Starter allows three pages per file, so each page is split into sections.

| Page | Sections |
|---|---|
| Cover | The cover frame: title, status and contents |
| Design system | **Visual direction:** colour and font boards. **Artwork:** the official artwork used in mockups. **Foundations:** colour, typography, spacing and radius variables. **Components:** Material 3 Design Kit parts plus our own (Pokémon card, type badge, stat bar, stats radar, evolution node, weather scene, "Why today?" line, widget layouts) |
| Screens | Wireframes; Visual direction (sample screens in light and dark); Phone (compact); Foldable (medium); Tablet (expanded); Flows (prototype start points); Archive |

Screens and flows share a page on purpose: Figma prototype links only work between frames on the same page.

Other Starter limits:
- **One mode per variable collection.** Light and dark colours live in two collections ("Colour / Light" and "Colour / Dark") instead of two modes of one.
- **Failed scripts leave the file unchanged** (seen on 29 September 2026), so retries are safe.

### Visual direction: "Living Dex", bright and colourful (29 September 2026)
Your requirements: vibrant colour, light and dark mode, and fonts that suit the theme.

**References.** Your Pokémon GO screenshots. We borrow ideas, not the look:
- Collection as a sticker album: caught in colour, seen as silhouettes, unseen as numbers.
- A progress header with the latest catches.
- Rounded, see-through cards on bright gradients.
- A big number and name on the detail page, with round type badges.
- Pill-shaped filters.

We don't copy Pokémon GO's layouts, icons (the Poké Ball, the official type symbols) or colours.

**Colour.**
- **Brand:** Dex Red (#F2385A) for highlights and gradients. Buttons use a deeper red (#D62246) so white text passes 4.5:1. Dark mode uses #FF6B81 with dark text.
- **Highlights:** Electric Yellow (#FFD23F) for streaks and "new" labels.
- **Types:** 18 bright type colours for badges and card tints. Each gets black or white text, whichever passes 4.5:1.
- **Weather:** backgrounds for Today: clear, heat, cloud, rain, storm, snow, wind and night.
- **Detail pages:** colours taken from the artwork and turned into a full scheme for light and dark.
- **Light mode:** a warm white base (#FFF8F1) with plum ink text (#1C1426).
- **Dark mode:** a deep plum-black base (#110C1A) with near-white text (#F7F2FF).
- **Cards:** see-through in both modes.

**Fonts.** Three themed pairings are compared on the Design system page:
- **Adventure (default):** Fredoka, Nunito and Silkscreen. Pixelify Sans was dropped because its 5 reads as an S, so #0025 looked like #002S (see "Numbers check" on the Design system page).
- **Arcade:** Lilita One, Rubik and Press Start 2P.
- **Device:** Chakra Petch, Rubik and JetBrains Mono.

**Fixes from the wireframe review, applied in the detailed screens:**
- The complete daily loop.
- Widgets that don't give the answer away.
- Two-step onboarding.
- A type chart that works on phones.
- One shared detail layout.
- Settings on every tab.
- Outlines of at least 3:1.
- One button height.
- Material 3's shape and type scales.

### Workflow
1. **Wireframes** for every v1 screen and the four key flows (onboarding, daily guess and reveal, team building, changing city), then review.
2. **Visual direction:** "Living Dex", as above. Four sample screens in light and dark, plus colour and font boards, then pick the font pairing.
3. **Foundations:** Material Theme Builder (Figma plugin) generates the Material 3 colour scheme from a seed colour. Later it exports the Compose theme code, so design and code share one source.
4. **High-fidelity screens** in light and dark, tablet layouts where relevant, with Google's weather attribution placed.
5. **Prototype,** tested on a phone through the Figma mobile app.
6. **Handoff:** frames and components are named to match the Compose components, so each screen maps directly to code.

Exit criteria: every v1 screen signed off in light and dark at phone size, plus tablet layouts for the Pokédex, Today and the Team Builder.

Progress:
- **29 September 2026, step 1 done:** 15 greyscale wireframes are in the [Wireframes section](https://www.figma.com/design/GhxZSeok2qh19z4X74MnEv?node-id=5-2) on the Screens page, in three rows: onboarding, core screens and more screens. Each has a review note underneath. Region mentions were removed.
- **29 September 2026, step 2 built:**
  - The [colour board, font board and numbers check](https://www.figma.com/design/GhxZSeok2qh19z4X74MnEv?node-id=15-2) are on the Design system page.
  - Ten official artwork images are in the Artwork section on the same page.
  - Eight [sample screens](https://www.figma.com/design/GhxZSeok2qh19z4X74MnEv?node-id=16-2) are on the Screens page: Collection, Pokédex, Pikachu's detail page and Today, each in light and dark.
  - Adventure is used as the default font pairing until you pick another.
- **29 September 2026, step 3 foundations built** (Foundations board on the Design system page):
  - **Variable collections:** Colour / Light (19), Colour / Dark (19), Types (36) and Spacing and shape (13). Each variable has its scope set and an Android name for the Compose theme.
  - **Text styles:** 11 Adventure styles, from Display/Large down to Number/Small.
  - **Effect styles:** Glass/Background blur and Elevation/Card.
- **30 September 2026, change of approach:** the remaining screens are designed directly in Compose, not in Figma. The foundations above are already in the code design system (`:core:designsystem`). Each new screen has light and dark `@DexPreviews`, and those previews are the design reference. The Figma file stays as the record of the foundations and the first sample screens. This also avoids Starter's 20 calls a month.

## 8. Build sprints (after design sign-off)

Two-week sprints, each ending with a Substack issue.

| Sprint | Focus | Demo for posts |
|---|---|---|
| 0 | Design in Figma (section 7) | Wireframe to high-fidelity, the visual direction reveal |
| 1 | Foundations: project setup, CI, theme from the Figma foundations, Crashlytics, PokeAPI index into Room, Pokédex grid | Scrolling the full Pokédex in airplane mode |
| 2 | Detail screen, search and filters, type chart, cries | Card-to-detail transition |
| 3 | Today: context engine and tests, Google Weather, city search, guess and reveal, story sections, Remote Config, opt-in Analytics | The daily reveal |
| 4 | Team Builder, Collection, widgets, daily notification | Widget on the home screen |
| 5 | Hardening: UI and screenshot tests, Baseline Profiles, benchmarks, accessibility pass, v1.0 via App Distribution | Before-and-after performance numbers |
| 6 to 8 | v2: live wallpaper, real-size AR, Tap-to-Dex, World today map, Compare, air quality | AR and wallpaper clips |

**Sprint 1 progress (29 September 2026).**
- Done:
  - Project setup and the theme from the Figma foundations.
  - PokeAPI index into Room, with lazy, cached details (Hilt, Ktor, Room, `:core:network` and `:core:database`).
  - The Pokédex grid on real data.
  - Collection, Detail and Today reading real data.
  - Motion: shared-element artwork between screens, staggered entrances, press feedback, an animated navigation bar, and the Today shake and reveal.
- Pulled forward from Sprint 2: cries and card-to-detail transitions.
- Pulled forward from Sprint 3: a basic guess and reveal, with the pick seeded by date only and no weather yet.
- Still to do in Sprint 1: Crashlytics (needs the Firebase project on the personal Gmail account).

**Day 2 (30 September 2026), branch `day-2`.**
- Done:
  - CI on GitHub Actions: build, unit tests and lint.
  - 24 unit tests, covering the model, the mappers, the offline-first repository, the Pokédex filters and Today's saved progress.
  - A type index (18 requests) that gives every Pokémon its types, plus the type chart.
  - Room v2 with an auto-migration.
- Sprint 2 work done early:
  - Search, type and generation filters, and sort.
  - The type chart screen.
  - All five Detail tabs: About, Stats, Evolution, Matchups and Forms.
- Also done:
  - Today saves its progress per date and keeps a real streak.
  - A Settings screen: theme, privacy choices (both off) and About with the font licences.
- Later the same day, with the screens designed in Compose rather than Figma:
  - Onboarding: welcome, an optional city (kept on the phone) and privacy choices (both off). The Pokédex downloads in the background meanwhile.
  - Team Builder: teams of six, a Pokémon picker, rename and delete, and a type check covering shared weaknesses and attack coverage. Room v3 adds the team tables.
  - Home-screen widget (Glance): a silhouette until you guess, then the artwork and name. It refreshes when you solve Today, and hourly.
  - With this, every v1 screen exists in code. What's left is Sprint 3's context engine and weather, the daily notification, and hardening.
- Sprint 3 core, also on 30 September 2026:
  - **`:core:domain`:** the context engine, with 12 table-driven tests.
    - Signals: weather buckets per the table above, night, season (flipped south of the equator) and three special days.
    - Picking: a weighted pick seeded by date and city, no repeats within 60 days, rare legendaries with weather moments, and "Why today?" plus a weather-only hint.
    - All weights are a first draft.
  - **`:core:weather`:** Google Weather when a key is in `secrets.properties`, otherwise Open-Meteo. The city is geocoded on the device and stored rounded to about 1 km.
  - **The day's pick is saved,** so Today, the widget and the reminder agree all day.
  - **Story after the reveal:** "Why today?", the first game, habitat, and up to three Pokédex entries from different games.
  - Also: the daily reminder at 9:00 (off by default, offered once after the first reveal), and "Add to team" on Detail.
  - Not done yet:
    - Remote Config and opt-in Analytics, which need the Firebase project.
    - National days, which need the country.
    - Religious festivals, still an open decision.
    - Places Autocomplete: the city is typed.
- Sprint 5 hardening, started 30 September 2026:
  - **Compose UI tests on the JVM** with Robolectric 4.17, at the previews' phone size. They cover the Pokédex (screen-reader text, search, the Caught checkbox, the filters sheet, empty results) and Today (the weather-only hint, attribution, wrong guesses disabled, the story and the reminder offer).
  - **Accessibility:** cards grow instead of clipping at 200% font size, chips meet the 48dp touch target, and confirmations are announced.
  - **CI** now also builds the R8-minified release, which is 3.1 MB unsigned.
  - **A draft privacy policy** is in `docs/privacy-policy.md`, linked from Settings. It needs review before publishing.
  - To confirm with Google's terms: the day's saved "Why today?" line includes the weather description, which is stored on the phone.
  - Needs a device or emulator: Baseline Profiles and Macrobenchmark, screenshot tests, a TalkBack walk-through, and checking the minified release at runtime.
- Forms follow the no-regions rule.

**Day 3 (1 October 2026), branch `day-3`.** `day-2` was fast-forwarded into `main` first.
- **Share card:** after the reveal, a 1080 x 1350 image with the day's sky, the artwork, the name, the number and "Why today?".
- **National days:** Germany, France, the Netherlands and the United States, using the country from the city lookup (sources in section 4). Pakistan is waiting on an official source.
- **Detail:**
  - A shiny toggle, using PokeAPI's shiny official artwork.
  - A size comparison against a 1.7 m person, which is a stated assumption, not an average.
- **Units:** metric or imperial for height, weight and temperature.
- **Adaptive layouts:**
  - From 600dp wide, a navigation rail replaces the bottom bar.
  - Lists show their detail alongside where there's room, via Material 3 adaptive's list-detail scene strategy 1.3.0.
  - The grids add columns as the window grows.
  - Shared-element transitions are phone-only, because side by side the same artwork can be on screen twice. A form whose name includes a region gets a neutral label, such as "Vulpix, alternate form". Other forms read naturally, for example "Mega Charizard X" or "Rotom (Heat)".

## 9. Distribution

- **v1:** Firebase App Distribution for testers, plus signed APKs on GitHub Releases for followers.
- **Play Store:** not planned, because of the Pokémon IP.
- **Developer verification:** Android's developer verification reaches sideloaded apps on certified devices worldwide in 2027 (from 30 September 2026 in Brazil, Indonesia, Singapore and Thailand). A free limited-distribution account covers up to 20 devices; beyond that you'd verify as a developer. Decide before v2.

## 10. Build in public

- **Every day (about 5 minutes):** a 10-20 second clip, a screenshot and three lines in `DEVLOG.md`. Post an Instagram Story.
- **TikTok and Instagram Reels:** 3-4 a week.
- **LinkedIn:** 2-3 a week, one lesson per post.
- **Substack:** at the end of each sprint.
- **Sprint 0 has its own content:** Figma timelapses, wireframe-to-final comparisons and a font vote. Consider letting followers pick the font pairing.
- **Recording:** capture only the emulator or the Figma canvas, with notifications off.

## 11. Risks

| Risk | Mitigation |
|---|---|
| Pokémon IP | Original icon, trademark notice, no monetisation, no ripped assets, no Play Store |
| Looking too much like Pokémon GO | Borrow ideas only; use our own layouts, icons and colours |
| Maps Platform costs or a leaked key | Restricted keys, daily quota caps, cached weather, Remote Config provider switch |
| Figma reads capped at 20 a month on Starter | Build calls report what they created, you paste screenshots for review, reads saved for build time; upgrade only if needed |
| Scope creep in the name of "best" | v1 and v2 gates, measured quality bars |
| Monotonous picks in heatwaves or wet spells | Variety rule, weights in Remote Config |
| Live wallpaper battery drain | Pause when hidden, frame cap, respect battery saver |
| Sideloading rules from 2027 | Limited-distribution account or developer verification |
| Posting burnout | Capture daily, publish on a rhythm |

## 12. Open decisions

None open at the moment.

Decided:
- Crash reports are off by default, like usage stats (29 September 2026).
- The application ID is `io.github.arsalanpeerzada.pokedex` (29 September 2026). Code namespaces stay `dev.pokedex.*`.
- **Font pairing:** Adventure, with Fredoka, Nunito and Silkscreen (1 October 2026).
- **Religious festivals:** not included. Special days stay secular (1 October 2026).
- **Weather refresh:**
  - Google: at most every three hours, with readings kept for one hour, per the summary of Google's terms.
  - Open-Meteo: readings kept for up to three hours.
  - Readings stay in memory only (1 October 2026).
- **Pakistan's national days:** Pakistan Day (23 March) and Independence Day (14 August), confirmed by the project owner (1 October 2026).
- **The old `DailyPicker`** was removed; the context engine replaced it (1 October 2026).

The Figma plan is settled for now: Starter with a Full seat, revisited only if reads run short (section 7).

## Sources

- **Android:** [Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3), [Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3), [AGP](https://developer.android.com/build/releases/gradle-plugin), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [Room](https://developer.android.com/jetpack/androidx/releases/room), [Kotlin](https://kotlinlang.org/docs/releases.html), [developer verification](https://developer.android.com/developer-verification), [Compose theming codelab](https://developer.android.com/codelabs/jetpack-compose-theming)
- **PokeAPI:** [docs and fair use](https://pokeapi.co/docs/v2), [GraphQL](https://pokeapi.co/docs/graphql), [species count](https://pokeapi.co/api/v2/pokemon-species?limit=1), [habitat example](https://pokeapi.co/api/v2/pokemon-species/sprigatito), [cries and sprites example](https://pokeapi.co/api/v2/pokemon/ditto)
- **Google Maps Platform:** [Weather overview](https://developers.google.com/maps/documentation/weather/overview), [coverage](https://developers.google.com/maps/documentation/weather/coverage), [policies and attribution](https://developers.google.com/maps/documentation/weather/policies), [current conditions](https://developers.google.com/maps/documentation/weather/reference/rest/v1/currentConditions/lookup), [condition types](https://developers.google.com/maps/documentation/weather/weather-condition-icons), [pricing](https://developers.google.com/maps/billing-and-pricing/pricing), [getting started and billing](https://developers.google.com/maps/get-started), [service-specific terms](https://cloud.google.com/maps-platform/terms/maps-service-terms), [Maps Compose](https://github.com/googlemaps/android-maps-compose)
- **Figma:** [MCP server](https://developers.figma.com/docs/figma-mcp-server/), [tools](https://developers.figma.com/docs/figma-mcp-server/tools-and-prompts/), [rate limits and access](https://developers.figma.com/docs/figma-mcp-server/rate-limits-access/), [Material Theme Builder](https://m3.material.io/blog/material-theme-builder)
- **Other:** [Open-Meteo terms](https://open-meteo.com/en/terms), [SceneView](https://github.com/SceneView/sceneview-android), [ARCore on the emulator](https://developers.google.com/ar/develop/java/emulator), [Firebase automatic events](https://support.google.com/analytics/answer/9234069)
