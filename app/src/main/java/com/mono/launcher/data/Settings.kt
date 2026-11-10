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
enum class GestureAction { NONE, NOTIFICATION_SHADE, LOCK_SCREEN, SEARCH, OPEN_SETTINGS }

data class Settings(
    val onboardingDone: Boolean = false,
    val railSide: RailSide = RailSide.RIGHT,
    val railStyle: RailStyle = RailStyle.WAVE,
    val iconStyle: IconStyle = IconStyle.MINIMAL,
    val fontScale: Float = 1f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val swipeDown: GestureAction = GestureAction.NOTIFICATION_SHADE,
    val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
    val swipeRight: GestureAction = GestureAction.NONE,
    val mutedPackages: Set<String> = emptySet(),
    val showMediaCard: Boolean = true,
    val haptics: Boolean = true,
)
