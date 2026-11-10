package com.mono.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.dataStore

    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val railSide = stringPreferencesKey("rail_side")
        val iconStyle = stringPreferencesKey("icon_style_v2")
        val railStyle = stringPreferencesKey("rail_style")
        val fontScale = floatPreferencesKey("font_scale")
        val theme = stringPreferencesKey("theme")
        val colorMode = stringPreferencesKey("color_mode")
        val font = stringPreferencesKey("font")
        val swipeDown = stringPreferencesKey("gesture_swipe_down")
        val doubleTap = stringPreferencesKey("gesture_double_tap")
        val swipeRight = stringPreferencesKey("gesture_swipe_right")
        val muted = stringSetPreferencesKey("muted_packages")
        val mediaCard = booleanPreferencesKey("media_card")
        val haptics = booleanPreferencesKey("haptics")
        val wallpaper = stringPreferencesKey("wallpaper_id")
        val infoWidget = booleanPreferencesKey("info_widget")
        val calendar = booleanPreferencesKey("calendar_on_home")
        val widgetId = intPreferencesKey("widget_id")
        val widgetBelow = booleanPreferencesKey("widget_below")
        val focusApps = stringSetPreferencesKey("focus_apps")
        val focusEnabled = booleanPreferencesKey("focus_enabled")
        val focusStart = intPreferencesKey("focus_start")
        val focusEnd = intPreferencesKey("focus_end")
        val focusDays = stringSetPreferencesKey("focus_days")
        val focusUntil = longPreferencesKey("focus_until")
        val distracting = stringSetPreferencesKey("distracting_apps")
        val nudgeSeconds = intPreferencesKey("nudge_seconds")
    }

    val settings: Flow<Settings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            onboardingDone = this[K.onboarding] ?: d.onboardingDone,
            railSide = enumOr(this[K.railSide], d.railSide),
            railStyle = enumOr(this[K.railStyle], d.railStyle),
            iconStyle = enumOr(this[K.iconStyle], d.iconStyle),
            fontScale = this[K.fontScale] ?: d.fontScale,
            theme = enumOr(this[K.theme], d.theme),
            colorMode = enumOr(this[K.colorMode], d.colorMode),
            fontId = this[K.font] ?: d.fontId,
            swipeDown = enumOr(this[K.swipeDown], d.swipeDown),
            doubleTap = enumOr(this[K.doubleTap], d.doubleTap),
            swipeRight = enumOr(this[K.swipeRight], d.swipeRight),
            mutedPackages = this[K.muted] ?: d.mutedPackages,
            showMediaCard = this[K.mediaCard] ?: d.showMediaCard,
            haptics = this[K.haptics] ?: d.haptics,
            wallpaperId = this[K.wallpaper],
            infoWidget = this[K.infoWidget] ?: d.infoWidget,
            calendarOnHome = this[K.calendar] ?: d.calendarOnHome,
            widgetId = this[K.widgetId] ?: d.widgetId,
            widgetBelow = this[K.widgetBelow] ?: d.widgetBelow,
            focusApps = this[K.focusApps] ?: d.focusApps,
            focusSchedule = FocusSchedule(
                enabled = this[K.focusEnabled] ?: d.focusSchedule.enabled,
                start = this[K.focusStart] ?: d.focusSchedule.start,
                end = this[K.focusEnd] ?: d.focusSchedule.end,
                days = this[K.focusDays]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: d.focusSchedule.days,
            ),
            focusUntil = this[K.focusUntil] ?: d.focusUntil,
            distractingApps = this[K.distracting] ?: d.distractingApps,
            nudgeSeconds = this[K.nudgeSeconds] ?: d.nudgeSeconds,
        )
    }

    private fun MutablePreferences.write(s: Settings) {
        this[K.onboarding] = s.onboardingDone
        this[K.railSide] = s.railSide.name
        this[K.railStyle] = s.railStyle.name
        this[K.iconStyle] = s.iconStyle.name
        this[K.fontScale] = s.fontScale
        this[K.theme] = s.theme.name
        this[K.colorMode] = s.colorMode.name
        this[K.font] = s.fontId
        this[K.swipeDown] = s.swipeDown.name
        this[K.doubleTap] = s.doubleTap.name
        this[K.swipeRight] = s.swipeRight.name
        this[K.muted] = s.mutedPackages
        this[K.mediaCard] = s.showMediaCard
        this[K.haptics] = s.haptics
        if (s.wallpaperId == null) remove(K.wallpaper) else this[K.wallpaper] = s.wallpaperId
        this[K.infoWidget] = s.infoWidget
        this[K.calendar] = s.calendarOnHome
        this[K.widgetId] = s.widgetId
        this[K.widgetBelow] = s.widgetBelow
        this[K.focusApps] = s.focusApps
        this[K.focusEnabled] = s.focusSchedule.enabled
        this[K.focusStart] = s.focusSchedule.start
        this[K.focusEnd] = s.focusSchedule.end
        this[K.focusDays] = s.focusSchedule.days.map { it.toString() }.toSet()
        this[K.focusUntil] = s.focusUntil
        this[K.distracting] = s.distractingApps
        this[K.nudgeSeconds] = s.nudgeSeconds
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        raw?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}
