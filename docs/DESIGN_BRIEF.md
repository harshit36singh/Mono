# Mono — Design brief

Mono is a minimal, one-handed Android launcher. Home is text first and calm: a clock at the top, up to eight favorite apps near the bottom where the thumb rests, and an A–Z rail on the screen edge that reaches every other app. Notifications appear as one line under the app they belong to.

This brief describes every screen and state that exists in the app today, with the sizes, colours and motion currently in the code. Treat those values as a starting point, not a constraint. The aim is a cohesive visual design that keeps the same structure and behaviour.

**Design principles**

- **Calm over clever.** Little chrome, no cards unless needed, one accent at most.
- **Thumb first.** Everything interactive sits in the bottom two-thirds of the screen. The top third is mostly empty on purpose.
- **Wallpaper is the background.** Home is transparent over the user's wallpaper with a soft gradient scrim. The design must stay readable on any wallpaper (contrast at least 4.5:1).
- **Single colour.** Icons are drawn as one-colour glyphs by default, so lists read as a single column of text and shapes.

**Platform and constraints**

- Android phones only, portrait, 360–480dp wide. No tablet layouts.
- Dark, Light, Follow system, and Material You (wallpaper-based colour) themes.
- Must work at system font scale up to 200%, and with Mono's own text-size setting (80–140%).
- Mirrors for left-handed use (rail on the left) and for right-to-left languages.
- Every interactive element needs a TalkBack label.
- All transitions take 200ms or less. Springs are used for the rail and its list.

---

## 1. Home — resting

The default screen. It sits transparently over the wallpaper.

```
┌──────────────────────────────┐
│ status bar                   │
│                              │
│  7:13                        │  ← Clock
│  Tuesday 10 November         │  ← Date
│                              │
│        (empty by design)     │
│                              │
│  ┌──────────────────────┐    │  ← Media card (only while music plays)
│  │ ▣  Song · Artist ⏮⏯⏭ │  ★ │
│  └──────────────────────┘  # │
│  ◎  Phone                  A │  ← Favorites (up to 8)
│  ◎  WhatsApp   2 new       B │
│      Riya · see you at 6   C │  ← Notification line
│  ◎  Gmail                  … │
│  ◎  Instagram              Z │  ← A–Z rail
│  ◎  YouTube                ▣ │
│                         (⌕)  │  ← Search button
└──────────────────────────────┘
```

**Elements**

| Element | Current spec | Notes for design |
|---|---|---|
| Scrim | Vertical gradient over the wallpaper: black at 25% opacity at the top to 55% at the bottom (white in the light theme) | Must keep text legible on bright wallpapers |
| Clock | 56sp, Light weight, 12- or 24-hour per system | Tap opens the Clock app |
| Date | 16sp, secondary colour, e.g. "Tuesday 10 November" | Tap opens Calendar |
| Favorites | App name 21sp Regular, icon 24dp, 18dp gap, about 12dp vertical rhythm | Up to 8 rows, bottom-aligned |
| Notification badge | Pill after the name: "2 new", or a dot for a single notification. 11sp Medium, inverted colours | |
| Notification line | 14sp secondary colour, one line, "Title · text", ends with "…" | Indented to line up with the app name |
| Work badge | Small "work" tag, 10sp, rounded 4dp | For apps in the work profile |
| A–Z rail | 30dp wide column, 14dp in from the edge, 11sp Medium letters, covering about 60% of the screen height from the bottom | Order: ★ (favorites), #, A–Z, ▣ (work apps). Letters with no apps are faded to 28% |
| Search button | 52dp circle, surface colour at 75% opacity, hand-drawn magnifier glyph | Bottom corner on the rail's side |
| Page margins | 28dp on the outside, 72dp on the rail side | |

**States to design**

- Empty favorites: the hint "Long-press any app in the A–Z rail or search to pin it here."
- With and without the media card.
- Favorite rows with 0, 1 and several notifications.
- Icon styles: Minimal glyph (default), text only, greyscale, full colour.
- Light theme, and rail on the left.
- Text size at 140%.

**Gestures (no visible UI, but useful to know)**

- Swipe up: Search.
- Swipe down: notification shade (configurable).
- Double-tap empty space: lock screen (configurable).
- Swipe right: configurable, nothing by default.
- Long-press empty space: Settings.

---

## 2. Favorites — reorder state

Long-press a favorite and then drag to reorder it. Long-press without dragging opens the App actions sheet instead.

- The lifted row scales to 103% and follows the finger above the other rows.
- Other rows swap places as the dragged row passes half their height, with a haptic tick on each swap.
- **Design:** the lifted look (shadow? highlight?) and how the swap is shown.

---

## 3. Notification line — dismiss

- Swipe left on the notification line: it follows the finger and fades. Past 96dp it flies off and the notification is dismissed.
- Tap the line: opens that notification.
- **Design:** the swipe feedback and what is left after dismissal (the row collapses back to just the app name).

---

## 4. A–Z rail — three behaviours (user setting)

The rail is the signature interaction. The user picks one of three behaviours in Settings. All three share: a haptic tick on each letter change, letters with no apps are skipped, and ★ at the top shows favorites.

### 4a. Wave (default)

```
│  R                          │  ← Letter heading (18sp Medium)
│  ◎  Rapido                  │
│  ◎  realme Link             │  ← The letter's apps, left side,
│  ◎  Recorder                │    about 56% of the screen width
│ ┌────────────────────┐      │
│ │ ◎  redBus          │      │  ← Highlighted row (finger is on it)
│ └────────────────────┘      │
│  ◎  Reddit             L    │
│                       M     │
│                      N      │
│              (R)    O       │  ← Letter bubble
│                     P       │
│                      R      │  ← Letters bend away from the thumb
│                       S     │
│                        T    │
```

- **Touch the rail:** Home content fades out in 120ms. Letters bend into a smooth curve away from the thumb. The bulge follows the finger, letters nearest the finger are pushed furthest and scaled up to 150%, and the curve is a bell shape.
- **Letter bubble:** a 52dp circle showing the current letter (22sp Medium), placed just inside the curve beside the thumb.
- **App list:** the letter's apps appear on the open side, centred vertically around 46% of the screen height. There's a heading (the letter, "Favorites" or "Work"), then rows 34–52dp tall depending on how many apps there are, each with a 28dp icon and a 16sp label. Changing letter cross-fades the list with a small upward slide.
- **Slide onto the list:** the row under the finger is highlighted (background at 8% opacity, rounded 12dp, label grows to 106% and turns Medium weight). The curve freezes and the bubble fades to 35%.
- **Let go on a row:** the app opens with a reveal from that row.
- **Let go on the rail:** the list stays open (pinned) and its rows can be tapped. It scrolls if there are many apps. Tap outside or press Back to close.
- **Let go anywhere else:** cancel. The letters spring back (springy on the way out, quicker and firmer on the way back).

**Design:** the curve's depth and shape, the bubble, the list heading, the highlighted row, the pinned state, and transitions.

### 4b. Popout

- The touched letter magnifies to 220% and shifts inward.
- A card (232dp wide, rounded 18dp, surface colour, 8dp elevation) pops out beside the thumb, centred on the finger. It springs in from 86% scale.
- Hover, launch and pin work as in Wave. Home content stays visible behind the card.

**Design:** the card style and the magnified letter.

### 4c. List

- Touching the rail opens a full-screen list of all apps grouped by letter, with headings (★ Favorites first), 52dp rows, 28dp icons and 16sp labels.
- Dragging the rail jumps the list to the letter. The current letter is highlighted on the rail at 150% scale, and the highlight follows normal scrolling too.
- The list stays open after letting go. Back or Home closes it.

**Design:** section headings, row style, how the list opens (it currently fades in with a small slide up), and scroll edges.

---

## 5. Search

Opens by swiping up on Home or tapping the search button.

```
│  Search the web for "wh"    │  ← Muted, smaller
│  ◎  WhatsApp Business       │
│  ◎  WhatsApp                │  ← Best match sits nearest the field
│                             │
│ ┌─────────────────────────┐ │
│ │ wh|                     │ │  ← Field, pill shaped, at the bottom
│ └─────────────────────────┘ │
│ [ keyboard ]                │
```

- Full-screen background (theme background at 96% opacity). The field sits at the bottom above the keyboard, so results grow upwards and the best match is closest to the thumb.
- Result rows: 28dp icon and 20sp label.
- Empty query: the most-used apps, or the hint "Type to search your apps".
- No match: "No apps match "…"", plus a "Search the web for …" row.
- "Mono settings" appears as a result when the query matches it.
- The keyboard's Go key opens the top result.
- Enter animation: fade plus a small slide up (150–180ms).

**Design:** the field, rows, the empty and no-results states, and the web-search row.

---

## 6. App actions (long-press sheet)

A bottom sheet, opened by long-pressing any app (favorite, rail list, search result).

```
│ ─────                       │
│ ◎  WhatsApp          work   │  ← Header: 32dp icon, 22sp Medium name
│ New chat                    │  ← App shortcuts (up to 5, accent colour)
│ Status                      │
│ ─────────────────────────── │
│ Add to favorites            │  ← or "Remove from favorites" / "Favorites full (8)"
│ Rename                      │
│ Hide                        │
│ Mute notifications on Home  │
│ App info                    │
│ Uninstall                   │
```

- Rows are 17sp with about 14dp vertical padding. Disabled rows are at 38% opacity.

**Design:** the sheet, header, how shortcuts are told apart from actions, and whether actions get icons.

### Rename dialog

- Title "Rename", a text field filled with the current name, and the caption "Original: …". Buttons: Reset (only if renamed), Cancel, Save.

---

## 7. Onboarding (first launch, 3 steps)

Shown once, full screen. A small "Step n of 3" label sits at the top, and a 28sp Light heading leads each step.

1. **Pick your favorites.** "Up to 8 apps you open most…". A checkbox list of all apps, a counter ("3 / 8"), and Next. Rows are disabled once 8 are picked.
2. **See notifications in place.** Explains the inline notifications and privacy ("stays on this phone, no account, no tracking"), with a "Grant notification access" button. Once granted, it shows "✓ Notification access granted". Skip / Next.
3. **Make Mono your Home.** Explains the Home role, plus quick tips (long-press for settings, swipe up to search, drag the letters). "Set as default", then Finish (or "Finish anyway").

**Design:** a lightweight illustration or animation per step would help, especially one demonstrating the rail. Also step progress, and the button layout.

---

## 8. Settings

Full screen, with a 30sp Light "Settings" title and a "Done" button. Sections have small uppercase headings in the accent colour.

| Section | Items |
|---|---|
| Setup | Default Home app · Notification access · Double-tap to lock (status shown in each subtitle) |
| Layout | Alphabet rail (Right / Left edge) · Alphabet behaviour (Wave / Popout / List) · Icons (Text only / Minimal / Greyscale / Full colour) · Text size slider 80–140% with an "Aa" preview · Theme · Media card toggle · Haptics toggle |
| Gestures | Swipe down · Double-tap empty space · Swipe right, each with: Do nothing / Notification shade / Lock screen / Search / Mono settings |
| Hidden apps | List of hidden apps, tap to unhide |
| Muted notifications | List of muted apps, tap to unmute |
| Backup | Export settings · Import settings (JSON file) |
| About | Version, and "All data stays on this device. No account, no ads, no tracking." |

- Choices currently open a small dropdown menu.
- **Design:** a nicer picker for Alphabet behaviour and Icons. Small visual previews of Wave, Popout and List would help users choose. Also the section and list styling.

---

## 9. Shared components

| Component | Where it's used |
|---|---|
| App row (icon + label ± badge ± notification line) | Home, rail lists, search, drawer |
| Letter heading | Wave list, List drawer |
| Count badge ("2 new", dot) | Favorites |
| Work badge | Everywhere an app appears |
| Media card (art 44dp, title 15sp, artist 13sp, ⏮ ⏯ ⏭) | Home |
| Round button (52dp) | Search |
| Bottom sheet, dialog, dropdown, toggle, slider | Actions, rename, settings |

**Icon styles**

- **Minimal (default):** the app's themed (monochrome) icon tinted with the text colour. When an app doesn't have one, Mono builds a single-colour glyph from its logo, and falls back to soft greyscale if that fails.
- **Greyscale:** the full icon without colour.
- **Full colour:** the original icon.
- **Text only:** no icons.

---

## 10. Colour and type (current)

| Token | Dark | Light |
|---|---|---|
| Background | #0E0E0E | #F7F7F7 |
| Surface | #161616 | #FFFFFF |
| Surface container (cards, bubble, field) | #1E1E1E | #F0F0F0 |
| Text | #F2F2F2 | #141414 |
| Secondary text | #B8B8B8 | #4F4F4F |
| Accent (primary) | #F2F2F2 | #141414 |
| Outline | #5A5A5A | #9E9E9E |

- **Type:** system font (Roboto, or the phone maker's font). Weights used: Light (clock, headings), Regular, Medium.
- **Material You:** when chosen, colours come from the wallpaper (Android 12+).

---

## 11. Motion and haptics

| Moment | Motion | Haptic |
|---|---|---|
| Rail letter change | — | Light tick |
| Wave curve out / back | Spring (soft out, firmer back) | — |
| Rail list appears | Fade 110ms + spring scale from 96% | — |
| Letter change in list | Cross-fade + small slide up | — |
| Row highlight | Spring scale to 106% | Light tick |
| Launch app | Reveal from the touched row | Firm tap |
| Favorite lifted / swapped | Scale to 103% | Firm on lift, tick on swap |
| Home fade while rail active | 120ms out, 200ms in | — |
| Search / Settings open | Fade + small slide up, 150–200ms | — |

---

## 12. Deliverables wanted

1. Home, resting: dark and light, with and without icons, with notifications and the media card.
2. The rail in all three behaviours, with key frames: touch, dragging, hovering an app, pinned.
3. Search: empty, typing, no results.
4. App actions sheet and rename dialog.
5. The three onboarding steps.
6. Settings, including the picker for rail behaviour and icon style.
7. Component sheet: app row variants, badges, bubble, buttons, media card.
8. A motion note or prototype for the wave curve.
9. **App icon.** The current placeholder is three white bars of uneven length on #111111, standing for a list. It should work as an adaptive icon and as a themed (monochrome) icon.
10. Optional: Mono's own swipe-down panel (quick toggles and notifications), if that direction is picked.
