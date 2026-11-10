package com.mono.launcher.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Mono's AppWidgetHost: lists providers, allocates ids and creates host views. */
@Singleton
class WidgetHostManager @Inject constructor(@ApplicationContext private val context: Context) {
    val host = AppWidgetHost(context, HOST_ID)
    private val manager: AppWidgetManager = AppWidgetManager.getInstance(context)

    fun providers(): List<AppWidgetProviderInfo> = runCatching { manager.installedProviders }.getOrDefault(emptyList())

    fun info(id: Int): AppWidgetProviderInfo? = if (id < 0) null else runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(id: Int) {
        if (id >= 0) runCatching { host.deleteAppWidgetId(id) }
    }

    fun bindIfAllowed(id: Int, info: AppWidgetProviderInfo): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)

    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        return runCatching { host.createView(context, id, info).apply { setAppWidget(id, info) } }.getOrNull()
    }

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    companion object {
        const val HOST_ID = 1024
        const val REQUEST_CONFIGURE = 4711
    }
}

/**
 * Activity-scoped add/remove flow: allocate → bind (asks the user if needed) → configure
 * (if the widget has a setup screen) → save. On any failure the id is released.
 */
class WidgetFlow(
    private val activity: ComponentActivity,
    private val hosts: WidgetHostManager,
    private val currentId: () -> Int,
    private val save: (Int) -> Unit,
) {
    private var pendingId = -1
    private var pendingInfo: AppWidgetProviderInfo? = null

    private val bindLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) configure() else cancel()
    }

    fun pick(info: AppWidgetProviderInfo) {
        cancel()
        val id = hosts.allocate()
        pendingId = id
        pendingInfo = info
        if (hosts.bindIfAllowed(id, info)) {
            configure()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            runCatching { bindLauncher.launch(intent) }.onFailure { cancel() }
        }
    }

    private fun configure() {
        val info = pendingInfo ?: return cancel()
        if (info.configure == null) return finish()
        runCatching {
            hosts.host.startAppWidgetConfigureActivityForResult(activity, pendingId, 0, WidgetHostManager.REQUEST_CONFIGURE, null)
        }.onFailure {
            Log.w("Mono/Widget", "configure failed; adding without setup", it)
            finish()
        }
    }

    /** Forwarded from Activity.onActivityResult. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != WidgetHostManager.REQUEST_CONFIGURE) return false
        if (resultCode == Activity.RESULT_OK) finish() else cancel()
        return true
    }

    private fun finish() {
        val id = pendingId
        if (id < 0) return
        val old = currentId()
        if (old >= 0 && old != id) hosts.delete(old)
        save(id)
        pendingId = -1
        pendingInfo = null
    }

    private fun cancel() {
        if (pendingId >= 0) hosts.delete(pendingId)
        pendingId = -1
        pendingInfo = null
    }

    fun remove() {
        hosts.delete(currentId())
        save(-1)
    }
}

val LocalWidgetHost = staticCompositionLocalOf<WidgetHostManager?> { null }
