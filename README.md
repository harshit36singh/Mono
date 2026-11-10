# Mono

A minimal, one-handed, text-first Android launcher. Favorites sit in the bottom of the screen within thumb reach, every other app is one slide away on the A–Z rail, and notifications show up under the app they belong to.

## Features

| PRD | Feature | Status |
|-----|---------|--------|
| F1 | Favorites list: up to 8, long-press + drag to reorder | ✅ |
| F2 | A–Z rail: haptic tick per letter, popout beside the thumb, release on an app to launch | ✅ |
| F3 | Search: swipe up or tap the pill, fuzzy matching, Enter launches the top result | ✅ |
| F4 | Inline notifications: count badge + latest line, tap to open, swipe left to dismiss | ✅ |
| F5 | Clock & date header: tap opens Clock / Calendar | ✅ |
| F6 | App actions: shortcuts, favorite, rename, hide, mute, app info, uninstall | ✅ |
| F7 | Hidden apps, revealed in settings | ✅ |
| F8 | 3-step onboarding: favorites → notification access → set as default | ✅ |
| F9 | Work profile apps with badge, separate rail section | ✅ |
| F10 | Gestures: double-tap to lock, swipe down for the shade, configurable swipe right | ✅ |
| F11 | Media card driven by the active MediaSession | ✅ |
| F12 | Icons (none / system / monochrome), font size, dark / light / system / Material You | ✅ |
| F13 | Backup & restore as JSON | ✅ |
| F14–F17 | Widget slot, Focus mode, usage nudges, Pro theming | Not started |

## Tech

Kotlin, Jetpack Compose, Material 3. It's a single activity using MVVM with StateFlow. Hilt handles DI, Room stores apps, favorites, renames and launch counts, and DataStore holds settings. A WorkManager job refreshes the app list as a fallback. Apps come from `LauncherApps` (so `QUERY_ALL_PACKAGES` isn't needed), notifications from `NotificationListenerService`, and lock-on-double-tap uses an optional `AccessibilityService` that never reads window content.

Everything stays on the device: there is no backend and no analytics.

## Build

Requirements: JDK 17 and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device
./gradlew testDebugUnitTest    # unit tests
```

After installing, press Home and pick **Mono**, or finish the in-app onboarding.
