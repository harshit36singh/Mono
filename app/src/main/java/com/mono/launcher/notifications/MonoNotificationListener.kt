package com.mono.launcher.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mono.launcher.media.MediaRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Reads active notifications so Home can show them inline on the app row.
 * Ongoing, silent and group-summary notifications are ignored. Nothing leaves the device.
 */
@AndroidEntryPoint
class MonoNotificationListener : NotificationListenerService() {
    @Inject lateinit var store: NotificationStore
    @Inject lateinit var media: MediaRepository

    override fun onListenerConnected() {
        store.attach(this)
        rebuild()
        media.start()
    }

    override fun onListenerDisconnected() {
        store.detach()
        media.stop()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = rebuild()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = rebuild()
    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = rebuild()

    fun dismiss(keys: List<String>) {
        runCatching { cancelNotifications(keys.toTypedArray()) }
    }

    private fun rebuild() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val ranking = runCatching { currentRanking }.getOrNull()
        val r = Ranking()
        val visible = active.filter { sbn ->
            val n = sbn.notification
            val flags = n.flags
            val silent = ranking != null && ranking.getRanking(sbn.key, r) &&
                r.importance <= NotificationManager.IMPORTANCE_LOW
            sbn.packageName != packageName &&
                sbn.isClearable &&
                !sbn.isOngoing &&
                flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                flags and Notification.FLAG_FOREGROUND_SERVICE == 0 &&
                !silent
        }
        val map = visible.groupBy { it.packageName }.mapValues { (pkg, list) ->
            val latest = list.maxBy { it.postTime }
            val extras = latest.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
            NotificationState(
                key = latest.key,
                packageName = pkg,
                title = title,
                text = text.lineSequence().firstOrNull().orEmpty(),
                postedAt = latest.postTime,
                count = list.size,
                keys = list.map { it.key },
                contentIntent = latest.notification.contentIntent,
            )
        }
        store.publish(map)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, MonoNotificationListener::class.java)

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = component(context)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
