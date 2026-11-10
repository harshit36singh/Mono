package com.mono.launcher.data

enum class RailSide { RIGHT, LEFT }
enum class IconStyle { NONE, SYSTEM, MONOCHROME }
enum class ThemeMode { SYSTEM, DARK, LIGHT, DYNAMIC }
enum class GestureAction { NONE, NOTIFICATION_SHADE, LOCK_SCREEN, SEARCH, OPEN_SETTINGS }

data class Settings(
    val onboardingDone: Boolean = false,
    val railSide: RailSide = RailSide.RIGHT,
    val iconStyle: IconStyle = IconStyle.NONE,
    val fontScale: Float = 1f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val swipeDown: GestureAction = GestureAction.NOTIFICATION_SHADE,
    val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
    val swipeRight: GestureAction = GestureAction.NONE,
    val mutedPackages: Set<String> = emptySet(),
    val showMediaCard: Boolean = true,
    val haptics: Boolean = true,
)
