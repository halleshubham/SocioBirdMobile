# SociobirdMobile

A "bring your own instance" Android client for **SocioBird** (built on
**Postiz**, the open-source social-media scheduler, and specifically the
[`halleshubham/shacky-postiz`](https://github.com/halleshubham/shacky-postiz)
fork). Instead of hard-coding a server, the self-hoster types their own
**Server URL** and **API key** on first launch, and everything else in the
app is driven by [Postiz's Public API](https://docs.postiz.com/public-api/introduction).

Styling (colors, typography) matches the real Postiz web app, pulled directly
from its own source rather than invented.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Hilt for DI
- Retrofit + OkHttp + kotlinx.serialization
- Jetpack DataStore (Preferences) for storing the server URL / API key
- Navigation-Compose
- Coil for image loading

## How auth works

- On the Connect screen the user enters:
  - **Server URL** — e.g. `postiz.mycompany.com` or `192.168.1.20:5000` (a bare
    domain/IP is fine; the app adds `https://` and the right path).
  - **API key** — from their Postiz web app under *Settings → Developer*.
  - A **"This is Postiz Cloud"** switch, off by default, since the base
    path differs: self-hosted is `https://{domain}/api/public/v1`, cloud is
    `https://api.postiz.com/public/v1`.
- `PostizApiProvider` builds the Retrofit client from those values at
  *runtime* (not compile time) and rebuilds it if they change.
- The Connect screen saves the values, then calls `GET /is-connected` to
  validate them; on failure it clears the half-saved session so the user
  isn't left in a broken state.
- Everything downstream just reads/writes through `PostizRepository`,
  which wraps calls in a `Resource<T>` (Loading/Success/Error) sealed class,
  surfacing the server's actual error message rather than a generic one.

## What's implemented (v1.0.0)

- Connect / validate server screen
- Footer nav: **Posts**, **Channels**, **Settings**
- **Posts**: weekly day-strip navigation, brand/customer filter (`GET
  /groups`), list with delete, per-post Draft ⇄ Queue status toggle
  (`PUT /posts/:id/status`), tap a post for its analytics
- **Create/schedule post**: multi-select channels, image attach (device
  picker or by URL), real `DatePicker`/`TimePicker` with correct
  device-timezone → UTC conversion, "Suggest a time" (`GET
  /find-slot/:id`), live character counter from each channel's real
  limit (`GET /integration-settings/:id`)
- **Channels**: list with avatar/platform/group/status, connect a new
  channel via the server's OAuth flow in the browser (`GET
  /social/:integration`), disconnect with confirmation, tap a channel
  for its analytics
- **Analytics**: per-channel and per-post screens (`GET
  /analytics/:integration`, `GET /analytics/post/:postId`)
- Settings: view server/masked key, disconnect

## What's intentionally left as a TODO

- **Notifications.** `GET /notifications` is wired into `PostizApiService`
  but there's no screen for it yet.
- **Editing an already-scheduled post's settings** (`PUT
  /posts/:id/settings`). There's no public `GET /posts/:id` endpoint to
  safely fetch a post's current settings before editing, so this was left
  out rather than risk clobbering them.
- **Per-platform post settings** beyond X/Instagram. Most providers only
  need `{"__type": "<platform>"}`, but a few require more
  (see `docs.postiz.com/public-api/providers/<platform>`); only X's
  `who_can_reply_post` and Instagram's `post_type` are currently defaulted.
- AI video generation (`POST /generate-video`, `POST /video/function`) and
  generic provider-tool invocation (`POST /integration-trigger/:id`) —
  large scope, low value for this client.
- The `network_security_config.xml` currently allows cleartext HTTP for
  *any* host, since self-hosted instances are often reached over plain
  HTTP/LAN before a reverse proxy is set up. Tighten this before a Play
  Store submission (it flags broad cleartext permission).
- Encrypt the stored API key (e.g. move from Preferences DataStore to
  `EncryptedSharedPreferences`/Android Keystore) rather than plaintext DataStore.
- No automated tests yet.

## Running it / getting an APK

**Option A — Android Studio (easiest, gives you a device install directly)**
1. Open the `SociobirdMobile/` folder in Android Studio (Iguana or newer).
   Studio will offer to add the Gradle wrapper automatically on sync —
   accept that (the wrapper jar itself isn't checked in here since it's a
   binary). Alternatively run `gradle wrapper` yourself if you have Gradle
   installed.
2. Let Gradle sync, then Run on a device/emulator, or
   *Build → Build Bundle(s)/APK(s) → Build APK(s)* for a standalone file.
3. On first launch, enter your Postiz server URL and API key.

**Option B — GitHub Actions (no local Android Studio needed)**
1. Push this folder to a GitHub repo.
2. `.github/workflows/build-apk.yml` (included) builds a debug APK on every
   push, or on demand via the "Run workflow" button.
3. Download the `sociobird-mobile-debug-apk` artifact from the completed run,
   or grab the latest one from [Releases](../../releases), and install it
   on a device (enable "install unknown apps" first).

Every build (including tagged releases) is currently **debug-signed** —
fine for direct APK distribution/sideloading, which is how this app is
meant to be used, but GitHub Actions doesn't persist a debug keystore
between runs, so **each build has a different signing key**: uninstall the
previous version before installing a new one, or the install will silently
fail. A real release keystore (for Play Store or update-in-place
distribution) isn't set up yet.

## Project layout

```
app/src/main/java/com/postiz/mobile/
  data/local/SessionManager.kt        # DataStore-backed server URL + token
  data/remote/PostizApiProvider.kt    # builds Retrofit at runtime from the session
  data/remote/PostizApiService.kt     # the Public API surface (Retrofit interface)
  data/remote/dto/Dtos.kt             # request/response models (see provenance notes)
  data/repository/PostizRepository.kt # Resource<T>-wrapped calls, error mapping
  ui/navigation/                      # root graph (Connect vs main app)
  ui/main/MainScaffold.kt             # footer-nav shell
  ui/screens/connect/                 # server URL + API key entry
  ui/screens/posts/                   # list + create/schedule
  ui/screens/integrations/            # channels list + add/disconnect
  ui/screens/analytics/               # channel + post analytics
  ui/screens/settings/                # view/disconnect session
```

## License

[GNU AGPLv3](LICENSE) — matching the license of the
[`shacky-postiz`](https://github.com/halleshubham/shacky-postiz) backend
this app is a client for.
