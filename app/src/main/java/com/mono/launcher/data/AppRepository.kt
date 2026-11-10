package com.mono.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.mono.launcher.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_FAVORITES = 8

/**
 * Source of truth for installed apps.
 *
 * Pipeline: LauncherApps (+ callback) -> diff against Room -> upsert -> Flow<List<AppEntry>>.
 * If Room fails for any reason, an in-memory copy of the last good list keeps Home usable.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppDao,
    @AppScope private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val syncLock = Mutex()
    private val fallback = MutableStateFlow<List<AppEntry>>(emptyList())
    private var started = false

    val apps: Flow<List<AppEntry>> = dao.observeAll()
        .onEach { fallback.value = it }
        .catch { e ->
            Log.e(TAG, "Room failed; serving fallback list", e)
            if (fallback.value.isEmpty()) fallback.value = queryInstalled()
            emit(fallback.value)
        }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            scope.launch { runCatching { dao.deletePackage(packageName, serialOf(user)) } }
        }
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    fun start() {
        if (started) return
        started = true
        launcherApps.registerCallback(callback)
        refresh()
    }

    fun refresh() {
        scope.launch { sync() }
    }

    /** Diffs the installed activity list against the database, keeping user customisations. */
    suspend fun sync() = syncLock.withLock {
        withContext(Dispatchers.IO) {
            val installed = queryInstalled()
            runCatching {
                val existing = dao.getAll().associateBy { it.key }
                val merged = installed.map { fresh ->
                    existing[fresh.key]?.copy(label = fresh.label, isWorkProfile = fresh.isWorkProfile) ?: fresh
                }
                dao.upsertAll(merged)
                val installedKeys = installed.mapTo(HashSet()) { it.key }
                existing.keys.filterNot { it in installedKeys }
                    .forEach { dao.delete(it.packageName, it.activityName, it.userSerial) }
            }.onFailure {
                Log.e(TAG, "sync failed", it)
                fallback.value = installed
            }
        }
    }

    private fun queryInstalled(): List<AppEntry> {
        val me = Process.myUserHandle()
        return userManager.userProfiles.flatMap { user ->
            val serial = serialOf(user)
            runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .filter { it.componentName.packageName != context.packageName || user != me }
                .map { info ->
                    AppEntry(
                        packageName = info.componentName.packageName,
                        activityName = info.componentName.className,
                        userSerial = serial,
                        label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                        isWorkProfile = user != me,
                    )
                }
        }
    }

    fun serialOf(user: UserHandle): Long = userManager.getSerialNumberForUser(user)
    fun userOf(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    fun activityInfo(app: AppEntry): LauncherActivityInfo? {
        val user = userOf(app.userSerial) ?: return null
        return runCatching {
            launcherApps.getActivityList(app.packageName, user)
                .firstOrNull { it.componentName.className == app.activityName }
        }.getOrNull()
    }

    fun launch(app: AppEntry, bounds: Rect? = null): Boolean {
        val user = userOf(app.userSerial) ?: return false
        return try {
            launcherApps.startMainActivity(ComponentName(app.packageName, app.activityName), user, bounds, null)
            scope.launch { runCatching { dao.recordLaunch(app.packageName, app.activityName, app.userSerial, System.currentTimeMillis()) } }
            true
        } catch (e: Exception) {
            Log.w(TAG, "launch failed for ${app.packageName}", e)
            refresh()
            false
        }
    }

    fun openAppInfo(app: AppEntry) {
        val user = userOf(app.userSerial) ?: return
        runCatching { launcherApps.startAppDetailsActivity(ComponentName(app.packageName, app.activityName), user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (app.isWorkProfile) {
            userOf(app.userSerial)?.let { intent.putExtra(Intent.EXTRA_USER, it) }
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openAppInfo(app)
        }
    }

    val canUseShortcuts: Boolean get() = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    fun shortcuts(app: AppEntry): List<ShortcutInfo> {
        if (!canUseShortcuts) return emptyList()
        val user = userOf(app.userSerial) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return runCatching { launcherApps.getShortcuts(query, user).orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.isEnabled }
            .sortedWith(compareBy({ !it.isDynamic }, { it.rank }))
            .take(5)
    }

    fun launchShortcut(shortcut: ShortcutInfo) {
        runCatching { launcherApps.startShortcut(shortcut, null, null) }
            .onFailure { Log.w(TAG, "shortcut launch failed", it) }
    }

    suspend fun setHidden(app: AppEntry, hidden: Boolean) = safe {
        dao.setHidden(app.packageName, app.activityName, app.userSerial, hidden)
        if (hidden && app.isFavorite) removeFavorite(app)
    }

    suspend fun rename(app: AppEntry, label: String?) = safe {
        dao.setCustomLabel(app.packageName, app.activityName, app.userSerial, label?.trim()?.ifEmpty { null })
    }

    /** Returns false when the favorites list is already full. */
    suspend fun addFavorite(app: AppEntry): Boolean {
        var added = false
        safe {
            val favs = dao.favorites()
            if (favs.size < MAX_FAVORITES && favs.none { it.key == app.key }) {
                dao.setFavorite(app.packageName, app.activityName, app.userSerial, true, favs.size)
                added = true
            }
        }
        return added
    }

    suspend fun removeFavorite(app: AppEntry) = safe {
        dao.setFavorite(app.packageName, app.activityName, app.userSerial, false, null)
        dao.reorderFavorites(dao.favorites().map { it.key })
    }

    suspend fun reorderFavorites(ordered: List<AppKey>) = safe { dao.reorderFavorites(ordered) }

    suspend fun setFavorites(ordered: List<AppKey>) = safe {
        dao.favorites().filter { it.key !in ordered }
            .forEach { dao.setFavorite(it.packageName, it.activityName, it.userSerial, false, null) }
        dao.reorderFavorites(ordered.take(MAX_FAVORITES))
    }

    suspend fun allEntries(): List<AppEntry> = runCatching { dao.getAll() }.getOrDefault(fallback.value)

    suspend fun upsert(entries: List<AppEntry>) = safe { dao.upsertAll(entries) }

    private suspend fun safe(block: suspend () -> Unit) {
        withContext(Dispatchers.IO) {
            runCatching { block() }.onFailure { Log.e(TAG, "db write failed", it) }
        }
    }

    private companion object {
        const val TAG = "Mono/Apps"
    }
}
