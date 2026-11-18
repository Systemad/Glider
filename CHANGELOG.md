# Changelog

## v0.1-beta

Initial beta of Glider's native Android rewrite. It brings the Hacker News reader to Kotlin and Jetpack Compose with a refreshed Material 3 interface. Requires Android 12 or newer.

- Added Android dynamic colors, light and dark themes, and grouped settings with easier-to-scan controls.
- Refined discussions with clearer reply nesting, optional rainbow thread lines, more readable quotes, and a single button to collapse or expand all comments.
- Improved discussion scrolling and kept comment text selection available from the long-press actions menu.
- Improved loading with bulk thread fetching, a cache for recently opened discussions, and feed prefetching behind the **Show more** button.
- Added optional article thumbnails in discussions.
- Gave the debug APK its own name and icon so it can be identified alongside the release build.

**Known limitation:** Hacker News login is untested in this beta.
