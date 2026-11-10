package com.mono.launcher.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Exports/imports settings and per-app customisations as a plain JSON file (no cloud). */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val apps: AppRepository,
) {
    suspend fun export(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settings.current()
            val json = JSONObject()
                .put("format", FORMAT)
                .put("version", 1)
                .put("settings", JSONObject()
                    .put("railSide", s.railSide.name)
                    .put("railStyle", s.railStyle.name)
                    .put("iconStyle", s.iconStyle.name)
                    .put("fontScale", s.fontScale.toDouble())
                    .put("theme", s.theme.name)
                    .put("swipeDown", s.swipeDown.name)
                    .put("doubleTap", s.doubleTap.name)
                    .put("swipeRight", s.swipeRight.name)
                    .put("mutedPackages", JSONArray(s.mutedPackages.toList()))
                    .put("showMediaCard", s.showMediaCard)
                    .put("haptics", s.haptics)
                    .put("infoWidget", s.infoWidget))
                .put("apps", JSONArray().apply {
                    apps.allEntries()
                        .filter { it.isFavorite || it.isHidden || it.customLabel != null }
                        .forEach { a ->
                            put(JSONObject()
                                .put("package", a.packageName)
                                .put("activity", a.activityName)
                                .put("work", a.isWorkProfile)
                                .put("customLabel", a.customLabel ?: JSONObject.NULL)
                                .put("hidden", a.isHidden)
                                .put("favoriteOrder", a.favoriteOrder ?: JSONObject.NULL))
                        }
                })
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toString(2).toByteArray()) }
        }
    }

    suspend fun import(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            val json = JSONObject(text)
            require(json.optString("format") == FORMAT) { "Not a Mono backup file" }

            json.optJSONObject("settings")?.let { o ->
                settings.update { s ->
                    s.copy(
                        railSide = enumOr(o.optString("railSide"), s.railSide),
                        railStyle = enumOr(o.optString("railStyle"), s.railStyle),
                        iconStyle = enumOr(o.optString("iconStyle"), s.iconStyle),
                        fontScale = o.optDouble("fontScale", s.fontScale.toDouble()).toFloat().coerceIn(0.8f, 1.4f),
                        theme = enumOr(o.optString("theme"), s.theme),
                        swipeDown = enumOr(o.optString("swipeDown"), s.swipeDown),
                        doubleTap = enumOr(o.optString("doubleTap"), s.doubleTap),
                        swipeRight = enumOr(o.optString("swipeRight"), s.swipeRight),
                        mutedPackages = o.optJSONArray("mutedPackages")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }.toSet()
                        } ?: s.mutedPackages,
                        showMediaCard = o.optBoolean("showMediaCard", s.showMediaCard),
                        haptics = o.optBoolean("haptics", s.haptics),
                        infoWidget = o.optBoolean("infoWidget", s.infoWidget),
                    )
                }
            }

            val arr = json.optJSONArray("apps") ?: JSONArray()
            val installed = apps.allEntries()
            val updated = mutableListOf<AppEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val match = installed.firstOrNull {
                    it.packageName == o.getString("package") &&
                        it.activityName == o.getString("activity") &&
                        it.isWorkProfile == o.optBoolean("work")
                } ?: continue
                val order = if (o.isNull("favoriteOrder")) null else o.getInt("favoriteOrder")
                updated += match.copy(
                    customLabel = if (o.isNull("customLabel")) null else o.getString("customLabel"),
                    isHidden = o.optBoolean("hidden"),
                    isFavorite = order != null,
                    favoriteOrder = order,
                )
            }
            apps.upsert(updated)
            apps.setFavorites(updated.filter { it.isFavorite }.sortedBy { it.favoriteOrder }.map { it.key })
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        raw?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    private companion object {
        const val FORMAT = "mono-launcher-backup"
    }
}
