package com.mono.launcher.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Crisp haptics driven by the vibrator directly. Compose's built-in feedback types are
 * faint or skipped on several OEM skins, which made the A–Z rail feel dead.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val tickEffect: VibrationEffect? = effect(VibrationEffect.EFFECT_TICK, 8, 60)
    private val clickEffect: VibrationEffect? = effect(VibrationEffect.EFFECT_CLICK, 14, 140)
    private val heavyEffect: VibrationEffect? = effect(VibrationEffect.EFFECT_HEAVY_CLICK, 22, 200)

    /** Letter change on the rail, hovering an app, reorder swaps. */
    fun tick() = play(tickEffect)

    /** Launching an app. */
    fun click() = play(clickEffect)

    /** Long-press lift. */
    fun heavy() = play(heavyEffect)

    private fun play(effect: VibrationEffect?) {
        val v = vibrator ?: return
        if (effect == null || !v.hasVibrator()) return
        runCatching { v.vibrate(effect) }
    }

    private fun effect(predefined: Int, fallbackMs: Long, amplitude: Int): VibrationEffect? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(predefined)
        } else {
            VibrationEffect.createOneShot(fallbackMs, amplitude)
        }
    }.getOrNull()
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context.applicationContext) }
}
