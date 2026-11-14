# Glider for Hacker News

Android Hacker News client built with Kotlin, Jetpack Compose, Material 3 dynamic
color, and Coroutines/Flow.

## Build

Open `android/native/` in Android Studio. Requires JDK 17+, Android SDK 37, and
Build Tools 37.0.0.

```powershell
cd android/native
./gradlew.bat ktlintCheck :app:assembleDebug
```

See [Android project documentation](android/native/README.md) for structure and
device verification. Release signing is not configured.

The original Flutter application is available in Git history. Store metadata and
historical changelogs remain in `fastlane/metadata/`.
