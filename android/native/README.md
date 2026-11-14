# Glider for Android

Android-only Kotlin / Jetpack Compose implementation. Open this directory as a Gradle project in Android Studio.

## Build

Requirements: JDK 17+, Android SDK 37, Android SDK Build Tools 37.0.0. Set `ANDROID_HOME` or `sdk.dir` in an untracked `local.properties`.

```powershell
./gradlew.bat ktlintCheck :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Source

- `app`: application, theme, Navigation 3 shell, dependency injection entry point.
- `feature`: stories, search/catch-up, item/discussion, favorites, inbox, user, auth, submit/edit/reply, settings.
- `shared/data`: Firebase/Algolia requests, HN website actions, Room cache, DataStore preferences, legacy import, encrypted session, fonts.
- `shared/ui`: shared item rows, HTML text, author identicons, link launching.
- `gradle/libs.versions.toml`: pinned runtime dependencies.

Feeds, search, and discussions use lifecycle ViewModels. Search derives its state through Coroutines Flow (`flatMapLatest` / `stateIn`); Compose collects state with `collectAsStateWithLifecycle`. Feed/search rendering components receive state and callbacks. Composable-local state handles small forms and dialogs. Room caches feed IDs and item JSON; DataStore persists preferences and interaction IDs. Account cookies use Android Keystore AES/GCM. Password entry stays inside the HN login WebView.

Noto Sans and Noto Sans Mono are bundled. Other original font families download from Google Fonts' GitHub repository on first selection, then load from app-private storage; bundled Noto Sans is the offline fallback.

## Migration and verification

Local migration notes and verification checklists live in the ignored `plans/` directory at the repository root. The original Flutter implementation remains available in Git history.

Use the **T3 Code Android emulator** for integration/UI checks, then the **T3 Code connected Google Pixel 7 Pro** for screenshot comparison, gestures, wallpaper colors and manual account verification. Keep authentication actions and real posting under manual verification with the user's account. Never store account credentials in fixtures.

This debug project uses `nl.viter.glider`, the original application ID. An in-place update requires matching signing credentials. Do not uninstall the original app to bypass a signature mismatch: uninstalling deletes its private preferences. A fresh native installation cannot import another installation's data.

The version code is a development placeholder; choose the release version and signing configuration before publication.
