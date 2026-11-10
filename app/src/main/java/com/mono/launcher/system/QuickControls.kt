package com.mono.launcher.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Thin wrappers for the Mono panel's quick controls. Things apps may change directly
 * (torch, ringer, DND with access, brightness with WRITE_SETTINGS, media volume) are changed
 * in place; Wi-Fi, data and Bluetooth open Android's own small panels (Android 10+ rule).
 */
class QuickControls(private val context: Context) {
    private val camera = context.getSystemService(CameraManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    enum class Ringer { RING, VIBRATE, SILENT }

    // --- flashlight -------------------------------------------------------------------------

    private val torchId: String? = runCatching {
        camera?.cameraIdList?.firstOrNull { id ->
            camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    val hasTorch: Boolean get() = torchId != null

    /** Registers a torch listener; returns an unregister function. */
    fun observeTorch(onChange: (Boolean) -> Unit): () -> Unit {
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) onChange(enabled)
            }
        }
        runCatching { camera?.registerTorchCallback(cb, Handler(Looper.getMainLooper())) }
        return { runCatching { camera?.unregisterTorchCallback(cb) } }
    }

    fun setTorch(on: Boolean) {
        val id = torchId ?: return
        runCatching { camera?.setTorchMode(id, on) }
    }

    // --- ringer & do not disturb ----------------------------------------------------------

    val hasPolicyAccess: Boolean get() = notifications?.isNotificationPolicyAccessGranted == true

    fun ringer(): Ringer = when (audio?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> Ringer.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> Ringer.SILENT
        else -> Ringer.RING
    }

    /** Ring → Vibrate → Silent → Ring. Silent needs DND access on most phones; skipped without it. */
    fun cycleRinger(): Ringer {
        val next = when (ringer()) {
            Ringer.RING -> Ringer.VIBRATE
            Ringer.VIBRATE -> if (hasPolicyAccess) Ringer.SILENT else Ringer.RING
            Ringer.SILENT -> Ringer.RING
        }
        runCatching {
            audio?.ringerMode = when (next) {
                Ringer.RING -> AudioManager.RINGER_MODE_NORMAL
                Ringer.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                Ringer.SILENT -> AudioManager.RINGER_MODE_SILENT
            }
        }
        return ringer()
    }

    fun isDndOn(): Boolean =
        (notifications?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL) != NotificationManager.INTERRUPTION_FILTER_ALL

    /** Returns false when DND access is missing (caller should open the access screen). */
    fun setDnd(on: Boolean): Boolean {
        if (!hasPolicyAccess) return false
        notifications?.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        return true
    }

    fun openPolicyAccess() = SystemActions.start(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    // --- brightness ---------------------------------------------------------------------------

    val canWriteSettings: Boolean get() = Settings.System.canWrite(context)

    /** 0f..1f */
    fun brightness(): Float = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f)

    fun setBrightness(value: Float) {
        if (!canWriteSettings) return
        runCatching {
            val cr = context.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (value.coerceIn(0.02f, 1f) * 255).toInt())
        }
    }

    fun openWriteSettings() = SystemActions.start(
        context,
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    // --- media volume -------------------------------------------------------------------------

    fun volume(): Float {
        val a = audio ?: return 0f
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return a.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun setVolume(value: Float) {
        val a = audio ?: return
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).toInt().coerceIn(0, max), 0) }
    }

    // --- connectivity (system panels) ---------------------------------------------------------

    fun openWifi() = openPanel(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)
    fun openData() = openPanel(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_DATA_ROAMING_SETTINGS,
    )
    fun openBluetooth() = openPanel(Settings.ACTION_BLUETOOTH_SETTINGS)

    private fun openPanel(action: String) {
        if (!SystemActions.start(context, Intent(action))) SystemActions.start(context, Intent(Settings.ACTION_SETTINGS))
    }
}
