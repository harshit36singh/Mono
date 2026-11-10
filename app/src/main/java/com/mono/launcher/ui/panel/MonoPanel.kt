package com.mono.launcher.ui.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.data.FocusState
import com.mono.launcher.system.QuickControls
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.LineIconSpec
import com.mono.launcher.ui.common.rememberHaptics
import com.mono.launcher.ui.theme.Mono
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mono's own swipe-down panel: quick tiles, brightness and volume, and a way through to the
 * system notification shade. Slides down from the top; swipe up, tap outside or Back to close.
 */
@Composable
fun MonoPanel(
    focus: FocusState,
    haptics: Boolean,
    onStartFocus: () -> Unit,
    onEndFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val controls = remember { QuickControls(context.applicationContext) }
    val haptic = rememberHaptics()
    BackHandler(onBack = onClose)

    var torch by remember { mutableStateOf(false) }
    var ringer by remember { mutableStateOf(controls.ringer()) }
    var dnd by remember { mutableStateOf(controls.isDndOn()) }
    var brightness by remember { mutableFloatStateOf(controls.brightness()) }
    var volume by remember { mutableFloatStateOf(controls.volume()) }

    DisposableEffect(Unit) { onDispose(controls.observeTorch { torch = it }) }
    LifecycleResumeEffect(Unit) {
        ringer = controls.ringer()
        dnd = controls.isDndOn()
        brightness = controls.brightness()
        volume = controls.volume()
        onPauseOrDispose { }
    }
    fun tap() {
        if (haptics) haptic.tick()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(c.surface)
                // Swallow taps on the sheet; swipe up to close.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { _, dy -> total += dy },
                        onDragEnd = { if (total < -60f) onClose() },
                    )
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text("Quick settings", fontSize = 22.sp, modifier = Modifier.weight(1f))
                if (focus.active) {
                    val until = focus.until?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                    Text("Focus${until?.let { " · until $it" } ?: ""}", fontSize = 13.sp, color = Mono.palette.accent)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Tiles: 4 per row.
            val tiles = buildList {
                if (controls.hasTorch) add(Tile("Flashlight", Icons.Flashlight, torch) { controls.setTorch(!torch) })
                add(
                    Tile(
                        when (ringer) { QuickControls.Ringer.RING -> "Sound"; QuickControls.Ringer.VIBRATE -> "Vibrate"; QuickControls.Ringer.SILENT -> "Silent" },
                        when (ringer) { QuickControls.Ringer.RING -> Icons.Speaker; QuickControls.Ringer.VIBRATE -> Icons.Vibrate; QuickControls.Ringer.SILENT -> Icons.Mute },
                        ringer != QuickControls.Ringer.RING,
                    ) { ringer = controls.cycleRinger() },
                )
                add(Tile("Do not disturb", Icons.Moon, dnd) {
                    if (controls.setDnd(!dnd)) dnd = controls.isDndOn() else controls.openPolicyAccess()
                })
                add(Tile("Focus", Icons.Target, focus.active) { if (focus.active) onEndFocus() else onStartFocus() })
                add(Tile("Wi-Fi", Icons.Wifi, false) { controls.openWifi() })
                add(Tile("Mobile data", Icons.Data, false) { controls.openData() })
                add(Tile("Bluetooth", Icons.Bluetooth, false) { controls.openBluetooth() })
                add(Tile("Mono settings", Icons.Sliders, false, onOpenSettings))
            }
            tiles.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { t -> QuickTile(t) { tap(); t.onClick() } }
                    repeat(4 - row.size) { Spacer(Modifier.width(72.dp)) }
                }
            }

            // Sliders.
            SliderRow(Icons.Sun, "Brightness", brightness, enabled = controls.canWriteSettings, onLocked = { controls.openWriteSettings() }) {
                brightness = it
                controls.setBrightness(it)
            }
            SliderRow(Icons.Speaker, "Media volume", volume, enabled = true, onLocked = {}) {
                volume = it
                controls.setVolume(it)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.surfaceContainer)
                    .clickable(role = Role.Button) {
                        onClose()
                        SystemActions.expandNotifications(context)
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LineIcon(Icons.Bell, 18.dp, c.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("System notifications", fontSize = 15.sp, modifier = Modifier.weight(1f))
                LineIcon(Icons.ChevronRight, 16.dp, c.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.outline))
        }
    }
}

private class Tile(val label: String, val icon: LineIconSpec, val on: Boolean, val onClick: () -> Unit)

@Composable
private fun QuickTile(t: Tile, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val p = Mono.palette
    Column(
        Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics { stateDescription = if (t.on) "On" else "Off" }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (t.on) p.accent else c.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) { LineIcon(t.icon, 22.dp, if (t.on) p.onAccent else c.onSurface) }
        Text(
            t.label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SliderRow(
    icon: LineIconSpec,
    label: String,
    value: Float,
    enabled: Boolean,
    onLocked: () -> Unit,
    onChange: (Float) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val p = Mono.palette
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (enabled) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = "Allow", onClick = onLocked)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(icon, 20.dp, c.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        if (enabled) {
            Slider(
                value = value,
                onValueChange = onChange,
                colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.track),
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
            )
        } else {
            Text("$label · tap to allow Mono to change it", fontSize = 13.sp, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}
