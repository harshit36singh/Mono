package com.mono.launcher.system

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import com.mono.launcher.accessibility.LockAccessibilityService

/** Thin wrappers around system intents and services used from Home. */
object SystemActions {

    fun openClock(context: Context) =
        start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openCalendar(context: Context) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .let { ContentUris.appendId(it, System.currentTimeMillis()) }.build()
        if (!start(context, Intent(Intent.ACTION_VIEW, uri))) {
            start(context, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
        }
    }

    /** Uses the accessibility service when enabled, else the (normal, auto-granted) EXPAND_STATUS_BAR route. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        if (LockAccessibilityService.openNotifications()) return
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    fun lockScreen(context: Context) {
        if (LockAccessibilityService.lockScreen()) return
        val msg = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            "Lock needs Android 9 or newer"
        } else {
            "Enable Mono in Accessibility settings to lock with a double-tap"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun openNotificationAccessSettings(context: Context) =
        start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAccessibilitySettings(context: Context) =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            return rm?.isRoleHeld(RoleManager.ROLE_HOME) == true
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, 0)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Intent for RoleManager (API 29+) — launch with an activity result launcher. Null means use [openHomeSettings]. */
    fun requestHomeRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val rm = context.getSystemService(RoleManager::class.java) ?: return null
        if (!rm.isRoleAvailable(RoleManager.ROLE_HOME) || rm.isRoleHeld(RoleManager.ROLE_HOME)) return null
        return rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    fun openHomeSettings(context: Context) {
        if (!start(context, Intent(Settings.ACTION_HOME_SETTINGS))) {
            start(context, Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openUrl(context: Context, url: String) = start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun start(context: Context, intent: Intent): Boolean = try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
