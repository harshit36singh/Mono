package com.mono.launcher.ui

import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.AppKey
import com.mono.launcher.data.AppRepository
import com.mono.launcher.data.BackupManager
import com.mono.launcher.data.FuzzyMatcher
import com.mono.launcher.data.LetterGroup
import com.mono.launcher.data.LetterIndex
import com.mono.launcher.data.MAX_FAVORITES
import com.mono.launcher.data.Settings
import com.mono.launcher.data.SettingsRepository
import com.mono.launcher.media.MediaRepository
import com.mono.launcher.media.MediaState
import com.mono.launcher.notifications.NotificationState
import com.mono.launcher.notifications.NotificationStore
import com.mono.launcher.wallpaper.Wallpaper
import com.mono.launcher.wallpaper.WallpaperSetter
import com.mono.launcher.wallpaper.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Group label used for the separate work-profile section of the A–Z rail. */
const val WORK_GROUP = "▣"

/** Rail entry for the favorites list, shown first. */
const val FAVORITES_GROUP = "★"

data class AppLists(
    val favorites: List<AppEntry> = emptyList(),
    val visible: List<AppEntry> = emptyList(),
    val hidden: List<AppEntry> = emptyList(),
    val groups: List<LetterGroup> = emptyList(),
)

data class HomeUiState(
    val loaded: Boolean = false,
    val settings: Settings = Settings(),
    val apps: AppLists = AppLists(),
    val notifications: Map<String, NotificationState> = emptyMap(),
    val media: MediaState? = null,
)

sealed interface Overlay {
    data object None : Overlay
    data object Search : Overlay
    data object Settings : Overlay
    data class Actions(val app: AppEntry) : Overlay
    data class Rename(val app: AppEntry) : Overlay
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AppRepository,
    private val settingsRepo: SettingsRepository,
    private val notificationStore: NotificationStore,
    private val mediaRepo: MediaRepository,
    private val backup: BackupManager,
    private val wallpapers: WallpaperSetter,
) : ViewModel() {

    private val appLists = repo.apps.map { all ->
        val visible = all.filterNot { it.isHidden }
        val personal = visible.filterNot { it.isWorkProfile }
        val work = visible.filter { it.isWorkProfile }
        val favorites = visible.filter { it.isFavorite }.sortedBy { it.favoriteOrder ?: Int.MAX_VALUE }
        val groups = buildList {
            add(LetterGroup(FAVORITES_GROUP, favorites))
            addAll(LetterIndex.group(personal))
            if (work.isNotEmpty()) add(LetterGroup(WORK_GROUP, LetterIndex.group(work).flatMap { it.apps }))
        }
        AppLists(
            favorites = favorites,
            visible = visible,
            hidden = all.filter { it.isHidden }.sortedBy { it.displayLabel.lowercase() },
            groups = groups,
        )
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<HomeUiState> = combine(
        appLists,
        settingsRepo.settings,
        notificationStore.byPackage,
        mediaRepo.state,
    ) { lists, settings, notifs, media ->
        HomeUiState(
            loaded = true,
            settings = settings,
            apps = lists,
            notifications = notifs.filterKeys { it !in settings.mutedPackages },
            media = media.takeIf { settings.showMediaCard },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private val _overlay = MutableStateFlow<Overlay>(Overlay.None)
    val overlay: StateFlow<Overlay> = _overlay.asStateFlow()

    /** Bumped when Home should return to its resting state (Home pressed, app opened). */
    private val _resetTick = MutableStateFlow(0)
    val resetTick: StateFlow<Int> = _resetTick.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: StateFlow<List<AppEntry>> = combine(_query, appLists) { q, lists ->
        if (q.isBlank()) {
            lists.visible.filter { it.launchCount > 0 }
                .sortedWith(compareByDescending<AppEntry> { it.launchCount }.thenByDescending { it.lastLaunched })
                .take(MAX_FAVORITES)
        } else {
            FuzzyMatcher.rank(q, lists.visible, { it.displayLabel }, { it.launchCount })
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- overlays -------------------------------------------------------------------------

    fun show(overlay: Overlay) {
        if (overlay == Overlay.Search) _query.value = ""
        _overlay.value = overlay
    }

    fun closeOverlay() {
        _overlay.value = when (val o = _overlay.value) {
            is Overlay.Rename -> Overlay.Actions(o.app)
            else -> Overlay.None
        }
    }

    /** Home button while already on Home: reset to a clean screen. */
    fun onHomePressed() {
        _overlay.value = Overlay.None
        _query.value = ""
        _resetTick.value++
    }

    /** Leaving Home (e.g. an app opened): drop transient overlays but keep Settings open. */
    fun onStop() {
        if (_overlay.value != Overlay.Settings) _overlay.value = Overlay.None
        _resetTick.value++
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    // --- apps -----------------------------------------------------------------------------

    fun launch(app: AppEntry, bounds: Rect? = null, options: Bundle? = null): Boolean {
        val ok = repo.launch(app, bounds, options)
        if (ok && _overlay.value != Overlay.Settings) _overlay.value = Overlay.None
        return ok
    }

    fun launchTopResult(): Boolean = searchResults.value.firstOrNull()?.let { launch(it) } ?: false

    fun openAppInfo(app: AppEntry) = repo.openAppInfo(app)
    fun uninstall(app: AppEntry) = repo.uninstall(app)

    suspend fun shortcuts(app: AppEntry): List<ShortcutInfo> = withContext(Dispatchers.IO) { repo.shortcuts(app) }
    fun launchShortcut(shortcut: ShortcutInfo) {
        repo.launchShortcut(shortcut)
        _overlay.value = Overlay.None
    }

    fun toggleFavorite(app: AppEntry, onFull: () -> Unit = {}) = viewModelScope.launch {
        if (app.isFavorite) repo.removeFavorite(app) else if (!repo.addFavorite(app)) onFull()
    }

    fun reorderFavorites(order: List<AppKey>) = viewModelScope.launch { repo.reorderFavorites(order) }

    fun setHidden(app: AppEntry, hidden: Boolean) = viewModelScope.launch { repo.setHidden(app, hidden) }

    fun rename(app: AppEntry, label: String?) = viewModelScope.launch {
        repo.rename(app, label)
        _overlay.value = Overlay.None
    }

    // --- notifications & media ------------------------------------------------------------

    fun openNotification(app: AppEntry) {
        if (notificationStore.open(app.packageName)) {
            notificationStore.dismissIfAutoCancel(app.packageName)
        } else {
            launch(app)
        }
    }

    fun dismissNotification(packageName: String) = notificationStore.dismiss(packageName)

    fun toggleMute(packageName: String) = updateSettings { s ->
        s.copy(mutedPackages = if (packageName in s.mutedPackages) s.mutedPackages - packageName else s.mutedPackages + packageName)
    }

    fun mediaPlayPause() = mediaRepo.playPause()
    fun mediaNext() = mediaRepo.next()
    fun mediaPrevious() = mediaRepo.previous()

    // --- settings, onboarding, backup -----------------------------------------------------

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch { settingsRepo.update(transform) }

    fun completeOnboarding(favorites: List<AppKey>) = viewModelScope.launch {
        repo.setFavorites(favorites)
        settingsRepo.update { it.copy(onboardingDone = true) }
    }

    fun exportBackup(uri: Uri, done: (Result<Unit>) -> Unit) = viewModelScope.launch { done(backup.export(uri)) }
    fun importBackup(uri: Uri, done: (Result<Unit>) -> Unit) = viewModelScope.launch { done(backup.import(uri)) }

    fun applyWallpaper(wallpaper: Wallpaper, target: WallpaperTarget, done: (Result<Unit>) -> Unit) = viewModelScope.launch {
        val result = wallpapers.apply(wallpaper, target)
        if (result.isSuccess) settingsRepo.update { it.copy(wallpaperId = wallpaper.id) }
        done(result)
    }
}
