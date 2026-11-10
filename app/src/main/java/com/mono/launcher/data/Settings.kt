package com.mono.launcher.data

enum class RailSide { RIGHT, LEFT }

/** How the A–Z rail behaves while the thumb is on it. */
enum class RailStyle {
    /** Letters bend away from the thumb in a curve; the letter's apps show on the open side. */
    WAVE,
    /** Letter magnifies and a compact card with its apps pops out beside the thumb. */
    POPOUT,
    /** A full, scrollable A–Z list; the rail jumps the list to the letter. */
    LIST,
}

/** NONE = text only; MINIMAL = single-colour glyphs (themed icons), grey fallback; MONOCHROME = greyscale; SYSTEM = full colour. */
enum class IconStyle { NONE, MINIMAL, MONOCHROME, SYSTEM }
enum class ThemeMode { SYSTEM, DARK, LIGHT, DYNAMIC }

/** MONO = black & white with greyscale glyph icons; COLOUR = full-colour icons and an accent colour. */
enum class ColorMode { MONO, COLOUR }
enum class GestureAction { NONE, MONO_PANEL, NOTIFICATION_SHADE, LOCK_SCREEN, SEARCH, OPEN_SETTINGS }

/** Scheduled focus window. Minutes from midnight; [end] < [start] means it runs past midnight. */
data class FocusSchedule(
    val enabled: Boolean = false,
    val start: Int = 9 * 60,
    val end: Int = 17 * 60,
    /** ISO days, Monday = 1 … Sunday = 7. */
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
)

data class Settings(
    val onboardingDone: Boolean = false,
    val railSide: RailSide = RailSide.RIGHT,
    val railStyle: RailStyle = RailStyle.WAVE,
    val iconStyle: IconStyle = IconStyle.MINIMAL,
    val fontScale: Float = 1f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val colorMode: ColorMode = ColorMode.MONO,
    /** Id from FontChoices; "system" = the phone's own font. */
    val fontId: String = "system",
    val swipeDown: GestureAction = GestureAction.MONO_PANEL,
    val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
    val swipeRight: GestureAction = GestureAction.NONE,
    val mutedPackages: Set<String> = emptySet(),
    val showMediaCard: Boolean = true,
    val haptics: Boolean = true,
    /** Mono's info block under the clock (alarm, calendar, battery). */
    val infoWidget: Boolean = true,
    /** Show today's next calendar event in the info block (needs READ_CALENDAR). */
    val calendarOnHome: Boolean = false,
    /** Id of the bundled wallpaper last applied from Mono, if any. */
    val wallpaperId: String? = null,
    /** AppWidgetHost id of the widget on Home, or -1 for none. */
    val widgetId: Int = -1,
    /** Widget sits below the favorites instead of above them. */
    val widgetBelow: Boolean = false,
    /** Packages hidden from Home, rail, search and notifications while focus is on. */
    val focusApps: Set<String> = emptySet(),
    val focusSchedule: FocusSchedule = FocusSchedule(),
    /** Manual "focus now" end time (epoch ms); 0 = off. */
    val focusUntil: Long = 0L,
    /** Packages that get a short pause before opening. */
    val distractingApps: Set<String> = emptySet(),
    val nudgeSeconds: Int = 5,
)
