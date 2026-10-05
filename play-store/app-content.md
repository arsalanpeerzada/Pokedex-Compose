# App content answers (Play Console > Policy and programmes > App content)

> **Draft, not reviewed.** These answers describe what version 0.9.0 does, based on its code on 5 October 2026. They aren't legal advice. Re-check them against the questions Play Console actually shows, because the wording changes, and against any change to the app (Firebase in particular).

## Privacy policy
- **URL:** `https://github.com/arsalanpeerzada/Pokedex-Compose/blob/main/docs/privacy-policy.md`

## Ads
- **Does your app contain ads?** No.

## App access
- **All functionality is available without special access.** There's no sign-in, no account and no paywall.

## Content rating (IARC questionnaire)
- **Email:** your contact email.
- **Category:** "All other app types" (it's not a game, social or communication app, or a store).
- **Answers:**
  - **Violence, blood, fear, sexuality, language, controlled substances, crude humour:** No. The app shows creature artwork, stats and type match-ups. It has no battles or fighting scenes.
  - **Gambling, or simulated gambling:** No.
  - **Do users interact or exchange content with each other?** No. Sharing the reveal card hands an image to an app the user chooses through Android's share sheet, and nothing goes through any service run by the app.
  - **Does it share the user's physical location with other users?** No.
  - **Digital purchases:** No.
  - **Is it a web browser or search engine?** No.
- **Expected result:** the lowest age ratings (for example PEGI 3, ESRB Everyone). The questionnaire sets the actual ratings, so don't promise one in the listing.

## Target audience and content
This one needs a decision from you, and ideally a qualified review.

- **Recommended answer:** target ages **13 to 15, 16 to 17 and 18 and over**. Don't include any under-13 group.
- **"Could your app unintentionally appeal to children?"** Answer this honestly. Pokémon appeals to children, so Play may decide the app is child-appealing whatever you choose. If it does, the Families policy applies:
  - It needs a privacy policy that covers children (the current one has a short "Children" section).
  - It can't use advertising SDKs that aren't Families-certified (the app has none).
  - It can't send precise location (the app sends none; only an optional city rounded to about 1 km).
- **If you include under-13 groups,** the Families policy applies in full, and the "Children" section of the privacy policy needs a proper review (UK Age Appropriate Design Code, COPPA in the US).

## Data safety
Play counts data as **collected** when the app sends it off the device, even if the developer never sees or stores it.

### Overview
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (approximate location, only if the user adds a city) |
| Is all of the user data collected by your app encrypted in transit? | **Yes.** PokeAPI, Open-Meteo and Google Weather are all called over HTTPS. |
| Do you provide a way for users to request that their data is deleted? | **No.** The app stores nothing off the device, so there's nothing to delete on a server. Data on the phone is removed by clearing the app's storage or uninstalling it. Say so in the free-text field if Play offers one. |

### Data types
| Data type | Collected | Shared | Processed ephemerally | Required or optional | Purpose |
|---|---|---|---|---|---|
| Location > **Approximate location** | Yes | No | Yes | Optional (the city can be left out) | App functionality |

Why approximate location: the city the user types is turned into a position on the device, rounded to about 1 km, and sent to the weather service. The readings stay in memory only and are never written to storage.

Why "not shared": Open-Meteo and Google receive the position only to answer the weather request, on the app's behalf. Play treats that as a transfer to a service provider, which isn't counted as sharing. Typing the city into Android's geocoder works the same way.

**Everything else: not collected.** That covers personal info, financial info, health, messages, photos and videos, audio, files, calendar, contacts, app activity, web browsing, app info and performance, and device or other IDs.
- The share card is saved in the app's cache and handed to the app the user picks. The app itself doesn't send it anywhere.
- The usage statistics and crash report switches in Settings don't send anything in this version.

**When Firebase Analytics or Crashlytics is added,** declare the following, even though both are opt-in:
- App activity, App info and performance (crash logs, diagnostics) and Device or other IDs, for the purposes Analytics and App functionality.
- Mark them optional, and update the privacy policy first.

### Security practices
- **Independent security review:** No (don't claim it).

## Advertising ID
- **Does your app use an advertising ID?** No. The merged release manifest doesn't declare the `AD_ID` permission, and no advertising or analytics library is included. Check the merged manifest again after adding Firebase.

## Government apps, financial features, health, news, COVID-19 contact tracing
- No to each.

## Permissions and special declarations
- **`POST_NOTIFICATIONS`** is a normal runtime permission, asked for only when the reminder is turned on. It doesn't need a declaration.
- **`INTERNET`** doesn't need a declaration.
- **Live wallpaper (`BIND_WALLPAPER`)** protects the wallpaper service. It isn't a permission the app asks for.
- **Merged release manifest (checked 5 October 2026):** `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`. The last four come from WorkManager. There's no `AD_ID`, location, contacts, SMS, storage or exact-alarm permission.
- **Foreground services:** WorkManager adds the generic `FOREGROUND_SERVICE` permission, but no `FOREGROUND_SERVICE_*` type permission and no `foregroundServiceType`. Play's foreground service declaration is tied to those types, so it shouldn't be asked for. If Play Console asks anyway, the app doesn't start a foreground service itself.
