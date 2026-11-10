package com.mono.launcher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.BuildConfig
import com.mono.launcher.accessibility.LockAccessibilityService
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.GestureAction
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.RailSide
import com.mono.launcher.data.Settings
import com.mono.launcher.data.ThemeMode
import com.mono.launcher.notifications.MonoNotificationListener
import com.mono.launcher.system.SystemActions
import java.time.LocalDate

class SettingsActions(
    val update: ((Settings) -> Settings) -> Unit,
    val unhide: (AppEntry) -> Unit,
    val toggleMute: (String) -> Unit,
    val export: (android.net.Uri, (Result<Unit>) -> Unit) -> Unit,
    val import: (android.net.Uri, (Result<Unit>) -> Unit) -> Unit,
    val close: () -> Unit,
)

@Composable
fun SettingsScreen(
    settings: Settings,
    hidden: List<AppEntry>,
    allApps: List<AppEntry>,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    // Re-read permission state whenever we come back from system settings.
    val notifGranted = remember(tick) { MonoNotificationListener.isEnabled(context) }
    val a11yEnabled = remember(tick) { LockAccessibilityService.isEnabled(context) }
    val isDefault = remember(tick) { SystemActions.isDefaultLauncher(context) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    fun report(what: String) = { r: Result<Unit> ->
        val msg = r.fold({ "$what done" }, { "$what failed: ${it.message}" })
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { actions.export(it, report("Export")) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { actions.import(it, report("Import")) }
    }

    val labelFor = remember(allApps) { allApps.associate { it.packageName to it.displayLabel } }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            TextButton(onClick = actions.close) { Text("Done") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            section("Setup")
            item {
                ActionItem("Default Home app", if (isDefault) "Mono is the default" else "Tap to set Mono as default") {
                    val intent = SystemActions.requestHomeRoleIntent(context)
                    if (intent != null) roleLauncher.launch(intent) else SystemActions.openHomeSettings(context)
                }
            }
            item {
                ActionItem("Notification access", if (notifGranted) "Granted — inline notifications and media card on" else "Off — tap to grant") {
                    SystemActions.openNotificationAccessSettings(context)
                }
            }
            item {
                ActionItem("Double-tap to lock", if (a11yEnabled) "Accessibility service on" else "Needs Mono's accessibility service — tap to enable") {
                    SystemActions.openAccessibilitySettings(context)
                }
            }

            section("Layout")
            item {
                ChoiceItem("Alphabet rail", RailSide.entries, settings.railSide, { if (it == RailSide.RIGHT) "Right edge" else "Left edge" }) { v ->
                    actions.update { it.copy(railSide = v) }
                }
            }
            item {
                ChoiceItem("Icons", IconStyle.entries, settings.iconStyle, {
                    when (it) {
                        IconStyle.NONE -> "No icons"
                        IconStyle.SYSTEM -> "System icons"
                        IconStyle.MONOCHROME -> "Monochrome icons"
                    }
                }) { v -> actions.update { it.copy(iconStyle = v) } }
            }
            item { FontScaleItem(settings.fontScale) { v -> actions.update { it.copy(fontScale = v) } } }
            item {
                ChoiceItem("Theme", ThemeMode.entries, settings.theme, {
                    when (it) {
                        ThemeMode.SYSTEM -> "Follow system"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DYNAMIC -> "Material You colors"
                    }
                }) { v -> actions.update { it.copy(theme = v) } }
            }
            item {
                SwitchItem("Media card", "Now playing controls on Home", settings.showMediaCard) { v -> actions.update { it.copy(showMediaCard = v) } }
            }
            item {
                SwitchItem("Haptics", "Ticks on letters, reorder and launch", settings.haptics) { v -> actions.update { it.copy(haptics = v) } }
            }

            section("Gestures")
            item { GestureItem("Swipe down", settings.swipeDown) { v -> actions.update { it.copy(swipeDown = v) } } }
            item { GestureItem("Double-tap empty space", settings.doubleTap) { v -> actions.update { it.copy(doubleTap = v) } } }
            item { GestureItem("Swipe right", settings.swipeRight) { v -> actions.update { it.copy(swipeRight = v) } } }

            section("Hidden apps")
            if (hidden.isEmpty()) {
                item { Caption("No hidden apps. Long-press any app and choose Hide.") }
            } else {
                items(hidden, key = { "h/${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                    ActionItem(app.displayLabel + if (app.isWorkProfile) " (work)" else "", "Tap to unhide") { actions.unhide(app) }
                }
            }

            section("Muted notifications")
            if (settings.mutedPackages.isEmpty()) {
                item { Caption("No muted apps. Long-press an app to mute its notifications on Home.") }
            } else {
                items(settings.mutedPackages.sorted(), key = { "m/$it" }) { pkg ->
                    ActionItem(labelFor[pkg] ?: pkg, "Tap to unmute") { actions.toggleMute(pkg) }
                }
            }

            section("Backup")
            item { ActionItem("Export settings", "Save favorites, names and settings to a JSON file") { exporter.launch("mono-backup-${LocalDate.now()}.json") } }
            item { ActionItem("Import settings", "Restore from a Mono JSON file") { importer.launch(arrayOf("application/json", "text/plain", "*/*")) } }

            section("About")
            item { Caption("Mono ${BuildConfig.VERSION_NAME}\nAll data stays on this device. No account, no ads, no tracking.") }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private fun LazyListScope.section(title: String) {
    item(key = "section/$title") {
        Text(
            title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

@Composable
private fun ActionItem(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(title, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> ChoiceItem(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ActionItem(title, label(selected)) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(label(o) + if (o == selected) "  ✓" else "") },
                    onClick = { onSelect(o); open = false },
                )
            }
        }
    }
}

@Composable
private fun GestureItem(title: String, selected: GestureAction, onSelect: (GestureAction) -> Unit) =
    ChoiceItem(title, GestureAction.entries, selected, {
        when (it) {
            GestureAction.NONE -> "Do nothing"
            GestureAction.NOTIFICATION_SHADE -> "Open notification shade"
            GestureAction.LOCK_SCREEN -> "Lock screen"
            GestureAction.SEARCH -> "Search"
            GestureAction.OPEN_SETTINGS -> "Mono settings"
        }
    }, onSelect)

@Composable
private fun FontScaleItem(value: Float, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("Text size", fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("Aa  ${(local * 100).toInt()}%", fontSize = (14 * local).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = 0.8f..1.4f,
            steps = 5,
        )
    }
}
