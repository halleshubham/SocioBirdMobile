# SocioBird Mobile: read this before you build anything

Native Android app (Kotlin, Compose, Hilt). Single module `:app`, code namespace `com.postiz.mobile`
(kept from the Postiz fork; it is NOT the application id).

## Product flavors (this is the thing people get wrong)

The `edition` flavor dimension has two flavors. Always pick one explicitly.

| Flavor | applicationId | Server | Sign-in | Cleartext HTTP | Ships to |
|---|---|---|---|---|---|
| **`sociobird`** | `com.shackyapps.sociobird` | fixed: `https://go.sociobird.app` (`BuildConfig.LOCKED_SERVER_URL`) | **Email + password** (`LoginScreen`/`LoginViewModel`) | no (`src/sociobird/res/xml/network_security_config.xml`) | **Google Play** |
| `generic` | `com.shackyapps.sociobird.byo` (versionName `-byo`) | user types Server URL | Server URL + API key (`ConnectScreen`) | yes (`src/main/res/xml/network_security_config.xml`) | nobody; self-hosters / debugging |

How the `sociobird` sign-in works: `POST /api/auth/login` (`provider: LOCAL`) returns the session JWT
(`auth` header or `Set-Cookie`), then `GET /api/user/self` returns `publicApi`, the organisation's API key
(org admins only). The key is stored and used for the public API. Accounts created with Google have no
password, so `LoginScreen` also offers **Continue with Google** and, as a last resort, "Prefer an API key? Use it instead"
(`ConnectScreen`, server locked, `PostizNavGraph` toggles between the two).

Google sign-in (no Google client id lives in the app): the app calls `GET /api/auth/oauth/GOOGLE?redirect_uri=https://go.sociobird.app/api/auth/oauth-mobile-callback`,
opens the returned URL in a Chrome Custom Tab, Google redirects to that backend callback, which 302s to
`com.shackyapps.sociobird://auth/callback?code=..&state=..` (needs `MOBILE_APP_SCHEME=com.shackyapps.sociobird://auth/callback`
on the production backend, and the callback URL as an authorised redirect URI on the Google **Web** OAuth client
the backend uses as `YOUTUBE_CLIENT_ID`). `MainActivity` hands the deep link to `OAuthCallbackBus`, `LoginViewModel` posts
`{code, redirect_uri, state}` to `/api/auth/oauth/GOOGLE/exists` (existing accounts only; a new Google user is told to
register on the web) and then reads the org API key exactly like the email sign-in. The deep-link intent filter lives in
`app/src/sociobird/AndroidManifest.xml`.

The Google Play app is ALWAYS the `sociobird` flavor. Never upload a `generic` build to Play.

## UI foundation (Material 3 + SocioBird purple)

The app follows a Material 3 design system; read this before touching a screen.

- **Theme** (`ui/theme`): full tonal colour scheme anchored on brand purple `#612AD5` (light) and a lighter step of it (dark),
  fixed (no Material You dynamic colour), 5-step shape scale (8/12/16/24/32dp), M3 type scale with nothing below 12sp.
  Use colour *roles* (`MaterialTheme.colorScheme.primaryContainer`, `surfaceContainerLow`, `onSurfaceVariant`, `outlineVariant`...),
  never hex values in screens. Hairlines are `outlineVariant`. Sentence case labels; `labelLarge` is only the legacy small spaced label.
- **Shared components** (`ui/components/SbComponents.kt`): `SbCard`, `SectionHeader`, `StatusChip` (post state: DRAFT / QUEUE / PUBLISHED / ERROR),
  `PlatformAvatar` (profile picture + platform logo badge), `Shimmer` (loading), `SbEmptyState`. Reuse them; add new shared pieces there.
- **Platform logos**: `res/drawable-nodpi/ic_platform_<provider_id_with_underscores>.png`, mapped in `platformIconRes()`. A new provider needs a PNG
  and one line in that `when`. Only providers SocioBird actually supports on the web are bundled.
- **Navigation** (`ui/main/MainScaffold.kt`): M3 `NavigationBar` with Home, Calendar (`Screen.Posts`), a centre Create action, Channels, More (Settings).
  Start destination is **Home** (`Screen.Home`). Detail routes (create post, add channel, analytics) hide the bar. The scaffold applies
  `statusBarsPadding`; screens must not add their own status-bar gap, and anything with a text field needs `imePadding()`.
- **Home** (`ui/screens/home`): built only from `GET /posts` and `GET /integrations` (no backend change): getting-started checklist for new accounts,
  "Needs attention" (failed posts = state `ERROR` in the last 30 days, disabled channels), up to 3 "Up next" scheduled posts, this-week counts and a
  7-day strip, quick actions, channel avatars. The 7-day performance snapshot is deliberately not there yet (planned with Insights).
- **Calendar** (`ui/screens/posts/PostsListScreen.kt`, `PostsViewModel`): Week/Month toggle with per-day post counts, status filter chips
  (All / Scheduled / Published / Drafts / Failed with a count) and brand chips, post cards (`PlatformAvatar`, time, 3-line preview, `StatusChip`),
  tap = detail bottom sheet (analytics for published, draft/queue toggle, delete), swipe-to-delete with an **Undo snackbar** (the delete really
  happens in the view model after 4.5 s, so leaving the screen does not lose it), pull-to-refresh. `CalendarFocus` is a singleton that lets
  Home's "Review" open the calendar on the Failed filter. The calendar cannot edit a post (no API for it).
- **Old screens** (composer, channels, add channel, analytics, settings, sign-in) still use the earlier "editorial" layout on the new
  theme and are being migrated phase by phase (calendar, composer, channels/insights, polish). When you change one, move it to `SbComponents`.

## Build commands

```bash
./gradlew bundleSociobirdRelease      # signed Play bundle -> app/build/outputs/bundle/sociobirdRelease/app-sociobird-release.aab
./gradlew assembleSociobirdDebug      # debug APK of the Play app
./gradlew assembleGenericDebug        # debug APK of the bring-your-own-server app
```

Do NOT run plain `bundleRelease` / `assembleRelease`: with flavors that builds both editions, and the
`generic` one is the wrong artifact for Play.

CI (`.github/workflows`):
- `build-apk.yml`: debug APKs of both flavors on push.
- `build-aab.yml`: manual run, builds the signed `sociobird` release bundle from GitHub secrets
  (`ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Inputs `version_code` /
  `version_name` override the defaults.

## Releasing to Google Play

- Package name `com.shackyapps.sociobird` is permanent once uploaded.
- `targetSdk`/`compileSdk` 36. Google requires new apps and updates to target API 36 (since 2026-08-31),
  so never lower it. That needs AGP 8.9.1 and Gradle 8.11.1 (already set).
- `versionCode` must be higher than the last upload to Play. The default is in `app/build.gradle.kts`
  (`?: 11`); bump it for every release, or pass `-PversionCode=N -PversionName=x.y.z`.
- Signing: upload key from `keystore.properties` at the repo root (git-ignored) with
  `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Without it the release build is unsigned. The keystore
  (`sociobird-upload.jks`, alias `sociobird-upload`) is held by the owner, never committed. Play App Signing is on.
- Check a build: `jarsigner -verify <aab>` and `keytool -printcert -jarfile <aab>`.
- Store listing text and graphics are not in this repo.

## Play Console upload warnings (expected, read before worrying)

- **"No deobfuscation file"**: fixed. Release builds run R8 (`isMinifyEnabled`/`isShrinkResources`), and AGP puts
  `mapping.txt` inside the AAB (`BUNDLE-METADATA/.../proguard.map`), so nothing has to be uploaded by hand. If this warning comes
  back, R8 was turned off or the build was not a release build. R8 rules live in `app/proguard-rules.pro`; after changing
  dependencies, test the release build on a device (sign in, post list, create/schedule, upload) because R8 problems only show at runtime.
- **"Native code but no debug symbols"**: harmless, cannot be fixed from this repo. The only native library is
  AndroidX DataStore's `libdatastore_shared_counter.so` (a few KB, already stripped by AndroidX), so there are no symbols to
  extract; `ndk { debugSymbolLevel = ... }` produces nothing for it. Ignore the warning unless the app adds its own native code,
  in which case set `debugSymbolLevel = "SYMBOL_TABLE"` (or `"FULL"`) and install the NDK in the build environment.

## Building inside a cloud Claude Code session

No Android SDK is preinstalled. Outbound HTTPS goes through a proxy; these all worked:

```bash
# SDK (keep it outside the repo, e.g. in the scratchpad)
curl -o cmd.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmd.zip; mkdir -p $SDK/cmdline-tools; mv cmdline-tools $SDK/cmdline-tools/latest
yes | $SDK/cmdline-tools/latest/bin/sdkmanager --licenses
$SDK/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
export ANDROID_HOME=$SDK            # local.properties (sdk.dir=...) is git-ignored
```

- Maven Central can answer HTTP 429 through the proxy. Redirect it to Google's mirror with an init script at
  `~/.gradle/init.d/mirror.gradle` that rewrites `https://repo.maven.apache.org/maven2` to
  `https://maven-central.storage-download.googleapis.com/maven2/` (for both `pluginManagement` and
  `dependencyResolutionManagement` repositories in `settingsEvaluated`).
- Run `./gradlew --no-daemon -q bundleSociobirdRelease` with a ~10 minute timeout (first build about 4 minutes;
  AGP also auto-installs Build-Tools 35).
- Do not commit `app/build/`, `local.properties`, `keystore.properties`, `*.jks`.

## Git history lesson

The email/password `sociobird` flavor first lived only on the `develop` branch and was never merged, so a Play
bundle was once built from `master` without it. Before building a release, run `git fetch --all`,
`git branch -a` and `git log --all --oneline`, and confirm the flavor you build contains the sign-in you expect.
`master` is the release branch.
