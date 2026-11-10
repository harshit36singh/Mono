package com.mono.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val iconStyle = stringPreferencesKey("icon_style")
        val fontScale = floatPreferencesKey("font_scale")
        val theme = stringPreferencesKey("theme")
        val swipeDown = stringPreferencesKey("gesture_swipe_down")
        val doubleTap = stringPreferencesKey("gesture_double_tap")
        val swipeRight = stringPreferencesKey("gesture_swipe_right")
        val muted = stringSetPreferencesKey("muted_packages")
        val mediaCard = booleanPreferencesKey("media_card")
        val haptics = booleanPreferencesKey("haptics")
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
            iconStyle = enumOr(this[K.iconStyle], d.iconStyle),
            fontScale = this[K.fontScale] ?: d.fontScale,
            theme = enumOr(this[K.theme], d.theme),
            swipeDown = enumOr(this[K.swipeDown], d.swipeDown),
            doubleTap = enumOr(this[K.doubleTap], d.doubleTap),
            swipeRight = enumOr(this[K.swipeRight], d.swipeRight),
            mutedPackages = this[K.muted] ?: d.mutedPackages,
            showMediaCard = this[K.mediaCard] ?: d.showMediaCard,
            haptics = this[K.haptics] ?: d.haptics,
        )
    }

    private fun MutablePreferences.write(s: Settings) {
        this[K.onboarding] = s.onboardingDone
        this[K.railSide] = s.railSide.name
        this[K.iconStyle] = s.iconStyle.name
        this[K.fontScale] = s.fontScale
        this[K.theme] = s.theme.name
        this[K.swipeDown] = s.swipeDown.name
        this[K.doubleTap] = s.doubleTap.name
        this[K.swipeRight] = s.swipeRight.name
        this[K.muted] = s.mutedPackages
        this[K.mediaCard] = s.showMediaCard
        this[K.haptics] = s.haptics
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        raw?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}
