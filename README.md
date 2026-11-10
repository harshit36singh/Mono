# Mono

**A minimal, one-handed, text-first Android launcher.**
Your favorite apps sit at the bottom of the screen within thumb reach, every other app is one slide away on the A–Z rail, and notifications appear as a single line under the app they belong to. No account, no ads, no tracking: everything stays on the phone.

![Mono: Home in Colour and Mono, the A–Z wave and list, search, app actions, settings and wallpapers](docs/images/collage.jpg)

---

## Features

### Home
- **Favorites:** up to 8 apps in a calm vertical list near the bottom of the screen. Long-press and drag to reorder; long-press in place for app actions.
- **Clock and date:** tap the time to open Clock, tap the date to open Calendar. Updates from the system's minute tick, with no timers.
- **Info widget:** a thin timeline under the clock showing:
  - **Next alarm** (e.g. "Alarm · Tomorrow 6:30 AM"). Tap to open Clock.
  - **Today's next calendar event** (optional, needs calendar access). Tap to open Calendar.
  - **Battery level and charging state.** Tap to open battery settings.
- **Inline notifications:** the latest notification for each favorite shows under its name, with a "3 new" badge.
  - Tap the line to open it; swipe it left to dismiss.
  - Ongoing and silent notifications are skipped, and you can mute any app on Home.
- **Media card:** now-playing controls (previous, play/pause, next) from whichever app is playing music.
- **Search button:** a round button in the bottom corner, on the same side as the rail.

### A–Z rail, three behaviours
Pick one in **Settings → Alphabet behaviour**:

| Style | What happens when you slide your thumb on the letters |
|---|---|
| **Wave** (default) | The letters bend into a smooth curve away from your thumb. That letter's apps replace your favorites while the clock stays put. Slide sideways onto an app and let go to open it. |
| **Popout** | The letter magnifies and a compact card of its apps pops out next to your thumb. |
| **List** | A full, scrollable A–Z list with letter headings. Dragging the rail jumps the list to that letter. |

In all three styles:
- ★ at the top shows your favorites, `#` groups apps starting with a digit or symbol, and ▣ is a separate section for work-profile apps.
- Letters with no apps are faded and skipped.
- You feel a crisp vibration tick for every letter, a click when an app opens, and a heavier bump when you lift a favorite to reorder.
- Letting go on the rail keeps the list open so you can tap or scroll it. This is also how it works with TalkBack.
- The letters follow the phone's language: non-Latin scripts get their own index letters, and the rail mirrors for right-to-left languages.

### Search
- Swipe up anywhere, or tap the search button.
- Matching is forgiving: start of the name, start of any word, initials ("gm" finds Google Maps), letters in order ("wtsp" finds WhatsApp), and it ignores accents.
- Results grow upward so the best match sits right above the keyboard, highlighted with a **Go →** hint. The keyboard's Go key opens it.
- With nothing typed, it shows your most-used apps. There's also a "Search the web for…" row, and "Mono settings" appears as a result when it matches.

### App actions (long-press any app)
- App shortcuts (such as "New chat" or "Search") as chips.
- Add to or remove from favorites, rename, hide, and mute notifications on Home.
- App info and uninstall.

### Look and feel
- **Style:** **Mono** (black and white) or **Colour**. Colour shows full-colour icons and takes an accent colour from your wallpaper (Material You on Android 12+, a soft blue on older phones).
- **Icons:**
  - **Text only.**
  - **Minimal:** single-colour glyphs. Mono uses the app's themed icon when it has one, and otherwise builds a glyph from its logo.
  - **Greyscale.**
  - **Full colour.**
- **Fonts:** 13 Google Fonts (System, Inter, Manrope, DM Sans, Outfit, Sora, Space Grotesk, IBM Plex Sans, Lexend, Poppins, JetBrains Mono, Fraunces, Playfair), fetched on demand through Google Play services.
- **Theme:** Light, Dark, follow the system, or Material You.
- **Text size:** 80–140%, on top of the system font size.
- **Rail side:** right or left edge, for left-handed use.
- **Wallpapers:** 29 hand-picked wallpapers built in. Preview each one with the clock on top, then set it for the Home screen, the Lock screen or both.
- **Mo,** a small pebble mascot, appears in onboarding, empty states and the Settings footer, and is the app icon (with a themed version for Android's matching-icon mode).

### Gestures
| Gesture | Action |
|---|---|
| Tap an app | Open it |
| Long-press an app | App actions |
| Long-press and drag a favorite | Reorder |
| Swipe up | Search |
| Swipe down | Notification shade *(configurable)* |
| Double-tap empty space | Lock screen *(configurable; needs Mono's accessibility service)* |
| Swipe right | Nothing by default *(configurable)* |
| Long-press empty space | Mono settings |

### Setup and data
- **Onboarding** in three steps: pick favorites, allow notification access (optional), set Mono as your Home app.
- **Hidden apps:** hide an app from the rail and search; unhide it in Settings.
- **Backup and restore:** export favorites, renames, hidden apps and settings to a JSON file, and import them again.
- **Work profile:** work apps carry a "work" tag and get their own section on the rail.

---

## Privacy
- **No account, no server, no analytics.** Mono has no internet permission of its own.
- Notification text and calendar events are read on the phone and never stored or sent anywhere.
- Fonts download through Google Play services' font provider, the same way system apps do.
- Optional permissions, all off until you grant them:

| Permission | Used for |
|---|---|
| Notification access | Inline notifications and the media card |
| Calendar | "Today's next event" in the info widget |
| Accessibility service | Double-tap to lock only; it never reads screen content |

Mono finds your apps through Android's launcher interface, so it doesn't need permission to see every installed package.

## Performance
- No polling: Mono updates only when Android reports a change (app installed or removed, new notification, alarm, battery level, minute tick).
- Dragging along the rail updates only the drawing, not the whole screen, so it stays smooth. Icons load lazily and are cached in memory.
- If the app database ever fails, Home falls back to an in-memory app list, so it never comes up blank.
- The release app is about 7 MB, including the 29 wallpapers.

---

## Tech
| | |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | Single activity, MVVM, StateFlow, Hilt |
| Storage | Room (apps, favorites, renames, launch counts), DataStore (settings) |
| System APIs | LauncherApps, NotificationListenerService, MediaSessionManager, RoleManager, WallpaperManager, AccessibilityService, CalendarContract |
| Background | WorkManager (an occasional app-list refresh, as a safety net only) |
| Min / target SDK | 26 (Android 8.0) / 36 |

```
app/src/main/java/com/mono/launcher/
├── data/            Room, DataStore, app repository, search, letter index, icons, backup
├── notifications/   notification listener and in-memory notification state
├── media/           now-playing session tracking
├── wallpaper/       built-in wallpaper catalog and setter
├── accessibility/   lock-screen service
├── system/          system intents (clock, calendar, shade, default launcher)
└── ui/              home, A–Z rail, search, sheets, settings, onboarding, theme, components
```

## Build and install
Requirements: JDK 17 and the Android SDK (compileSdk 36).

```sh
./gradlew installRelease       # smoothest build; install on a connected phone
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # unit tests
```

After installing, press Home and pick **Mono**, or finish the in-app onboarding.

> The release build is signed with the local debug key so it can be shared and installed directly. Set up a real signing key before publishing to the Play Store.

## Design
- [`docs/DESIGN_BRIEF.md`](docs/DESIGN_BRIEF.md) describes every screen and state, with the original sizes, colours and motion.

## Roadmap
- A slot for widgets from other apps, above or below favorites.
- Focus mode: scheduled quiet hours that hide chosen apps and their notifications.
- Gentle "are you sure?" delay before opening apps you mark as distracting.
- A Mono swipe-down panel with quick toggles.
