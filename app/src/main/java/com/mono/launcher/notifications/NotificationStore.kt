package com.mono.launcher.notifications

import android.app.PendingIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/** Latest notification for one package. In memory only, never persisted. */
data class NotificationState(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val count: Int,
    val keys: List<String>,
    val contentIntent: PendingIntent?,
)

@Singleton
class NotificationStore @Inject constructor() {
    private val _byPackage = MutableStateFlow<Map<String, NotificationState>>(emptyMap())
    val byPackage: StateFlow<Map<String, NotificationState>> = _byPackage.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var listener: WeakReference<MonoNotificationListener>? = null

    internal fun attach(service: MonoNotificationListener) {
        listener = WeakReference(service)
        _connected.value = true
    }

    internal fun detach() {
        listener = null
        _connected.value = false
        _byPackage.value = emptyMap()
    }

    internal fun publish(map: Map<String, NotificationState>) {
        _byPackage.value = map
    }

    fun dismiss(packageName: String) {
        val state = _byPackage.value[packageName] ?: return
        listener?.get()?.dismiss(state.keys)
        _byPackage.value = _byPackage.value - packageName
    }

    /** Opens the notification's own target; returns false if there was nothing to send. */
    fun open(packageName: String): Boolean {
        val intent = _byPackage.value[packageName]?.contentIntent ?: return false
        return runCatching { intent.send() }.isSuccess
    }
}
