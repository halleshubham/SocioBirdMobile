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
password, so `LoginScreen` also offers "Signed up with Google? Use your API key instead", which opens the
API-key screen (`ConnectScreen`, server locked, `PostizNavGraph` toggles between the two).

The Google Play app is ALWAYS the `sociobird` flavor. Never upload a `generic` build to Play.

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
  (`?: 7`); bump it for every release, or pass `-PversionCode=N -PversionName=x.y.z`.
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
