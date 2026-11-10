package com.mono.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Used for exactly two global actions: lock screen (double-tap) and, if enabled,
 * opening the notification shade. It never reads window content.
 */
class LockAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private var instance: WeakReference<LockAccessibilityService>? = null

        val isRunning: Boolean get() = instance?.get() != null

        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return instance?.get()?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
        }

        fun openNotifications(): Boolean =
            instance?.get()?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(context, LockAccessibilityService::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
