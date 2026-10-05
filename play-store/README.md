# Google Play release kit

Everything Play Console asks for when you publish Pokedex, in one place.

| What | Where |
|---|---|
| Name, short and full description, release notes, category, contact details | [store-listing.md](store-listing.md) |
| Answers for App content: Data safety, content rating, target audience, ads, app access, advertising ID | [app-content.md](app-content.md) |
| Privacy policy (the URL Play needs, also linked from Settings) | [../docs/privacy-policy.md](../docs/privacy-policy.md) |
| Icon, feature graphic, phone screenshots | [graphics/](graphics/) |

## Before you submit
- [ ] **Trademark risk (said once, your call).** The name, the Poké Ball icon and the Pokémon artwork belong to Nintendo and The Pokémon Company. Play can reject or remove the app under its intellectual property and impersonation policies, and a removal counts against your developer account. The "unofficial fan app" line near the top of the description helps, but doesn't prevent it.
- [ ] **Privacy policy:** get the draft reviewed, fill in the two placeholders (developer name and contact email), remove the draft note, and merge it into `main`.
- [ ] **Contact email:** decide on a public address for the listing and the policy.
- [ ] **Version:** the app is 0.9.0 (versionCode 2). Bump it to 1.0.0 in `app/build.gradle.kts` if this is the public release. Every upload needs a higher versionCode.
- [ ] **Upload key:** create a keystore that stays outside the repo, and point `keystore.properties` at it (see `keystore.properties.example`). Back up the keystore and its passwords. Turn on Play App Signing when Play offers it, which is the default for new apps.
- [ ] **Build the bundle:** Play takes an Android App Bundle (`.aab`), not an APK.
  ```
  gradlew :app:bundleRelease
  ```
  The output is `app/build/outputs/bundle/release/app-release.aab`.
- [ ] **Merged manifest:** checked on 5 October 2026 (see app-content.md). Check it again after adding any library, especially Firebase.
- [ ] **Test the minified release build** on a device or emulator before uploading it.

## In Play Console, in order
1. **Create a developer account** (personal) on your own Google account. Play charges a one-off registration fee and verifies your identity.
2. **Create app:** name "Pokedex", default language English (United Kingdom), App, Free. Accept the declarations.
3. **App content:** go through every item using [app-content.md](app-content.md).
4. **Store listing:** paste in the text from [store-listing.md](store-listing.md) and upload the graphics.
5. **Store settings:** category and contact details.
6. **Testing > Closed testing:** upload the `.aab`, add testers and roll it out. Personal developer accounts created after November 2023 must run a closed test before production. At the time of writing, that's at least 12 testers opted in for 14 days in a row. Check the current numbers in Play Console Help, because Google has changed them before.
7. **Apply for production access,** then create a production release with the release notes. Review usually takes a few days.

## Regenerating the graphics
The icon, feature graphic and screenshots are rendered from the app's own Compose screens, fonts and launcher drawables with Robolectric. That's `app/src/test/kotlin/dev/pokedex/app/PlayStoreAssets.kt`. CI skips it, so run it locally when screens change:

```
PLAY_ASSETS=1 gradlew :app:testDebugUnitTest --tests "*PlayStoreAssets*"
```

It downloads the artwork it needs from PokeAPI's sprite repository once, into `app/build/play-artwork`.
