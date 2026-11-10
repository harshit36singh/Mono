package com.mono.launcher.ui.settings

import android.app.TimePickerDialog
import android.appwidget.AppWidgetProviderInfo
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.FocusState
import com.mono.launcher.data.Settings
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.Segmented
import com.mono.launcher.ui.common.SettingGroup
import com.mono.launcher.ui.theme.Mono
import com.mono.launcher.widget.LocalWidgetHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- Widget ------------------------------------------------------------------------------------

@Composable
internal fun WidgetSettings(settings: Settings, actions: SettingsActions, onOpenPicker: () -> Unit) {
    val hosts = LocalWidgetHost.current
    val context = LocalContext.current
    val info = remember(settings.widgetId) { hosts?.info(settings.widgetId) }
    Section("Widget") {
        SettingGroup {
            if (info == null) {
                ValueRow("Add a widget", "Choose") { onOpenPicker() }
            } else {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(info.loadLabel(context.packageManager), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(appLabel(context, info), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Remove",
                        fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = actions.removeWidget).padding(8.dp),
                    )
                }
                ValueRow("Change widget", null) { onOpenPicker() }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Segmented(
                        listOf(false, true),
                        settings.widgetBelow,
                        { if (it) "Below favorites" else "Above favorites" },
                        { v -> actions.update { it.copy(widgetBelow = v) } },
                    )
                }
            }
        }
    }
}

private fun appLabel(context: android.content.Context, info: AppWidgetProviderInfo): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
}.getOrDefault(info.provider.packageName)

/** Full-screen list of installed widgets, grouped by app. */
@Composable
internal fun WidgetPicker(onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) {
    val hosts = LocalWidgetHost.current
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    BackHandler(onBack = onClose)
    val grouped by produceState(emptyList<Pair<String, List<AppWidgetProviderInfo>>>()) {
        value = withContext(Dispatchers.IO) {
            hosts?.providers().orEmpty()
                .groupBy { appLabel(context, it) }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (k, v) -> k to v.sortedBy { it.loadLabel(context.packageManager) } }
        }
    }
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Choose a widget", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Close", onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { LineIcon(Icons.Close, 18.dp, c.onSurfaceVariant) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (grouped.isEmpty()) item { Text("Loading widgets…", color = c.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            items(grouped, key = { it.first }) { (app, widgets) ->
                Column {
                    Text(app.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
                    SettingGroup {
                        widgets.forEach { w -> WidgetRow(w) { onPick(w); onClose() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val density = context.resources.displayMetrics.densityDpi
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                (info.loadPreviewImage(context, density) ?: info.loadIcon(context, density))?.toBitmap(160, 160)?.asImageBitmap()
            }.getOrNull()
        }
    }
    // minWidth/minHeight are in pixels; Android's cell formula is roughly (dp + 30) / 70.
    val d = context.resources.displayMetrics.density
    val cells = "${((info.minWidth / d + 30) / 70).toInt().coerceAtLeast(1)}×${((info.minHeight / d + 30) / 70).toInt().coerceAtLeast(1)}"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceContainer), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.size(48.dp)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(info.loadLabel(context.packageManager), fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(cells, fontSize = 13.sp, color = c.onSurfaceVariant)
        }
    }
}

// --- Focus -------------------------------------------------------------------------------------

private val dayLetters = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")

@Composable
internal fun FocusSettings(settings: Settings, focus: FocusState, allApps: List<AppEntry>, actions: SettingsActions) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val is24 = DateFormat.is24HourFormat(context)
    val fmt = DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
    fun label(minutes: Int) = LocalTime.of(minutes / 60, minutes % 60).format(fmt)
    fun pickTime(initial: Int, onSet: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onSet(h * 60 + m) }, initial / 60, initial % 60, is24).show()
    }
    val schedule = settings.focusSchedule

    Section("Focus") {
        SettingGroup {
            // Status + quick start.
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Icons.Target, 20.dp, if (focus.active) Mono.palette.accent else c.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (focus.active) "Focus is on" + (focus.until?.let { " · until ${it.format(fmt)}" } ?: "") else "Focus is off",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (focus.manual) {
                        Text("End", fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = actions.endFocus).padding(8.dp))
                    }
                }
                Text(
                    "Hides the apps you choose, and their notifications, from Home, the A–Z rail and search.",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (m, t) ->
                        Box(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(c.surfaceContainer)
                                .clickable(role = Role.Button) { actions.startFocus(m) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(t, fontSize = 14.sp) }
                    }
                }
            }
            SwitchRowPadded("Scheduled focus", schedule.enabled) { v -> actions.update { it.copy(focusSchedule = it.focusSchedule.copy(enabled = v)) } }
            if (schedule.enabled) {
                ValueRow("Starts", label(schedule.start)) {
                    pickTime(schedule.start) { m -> actions.update { it.copy(focusSchedule = it.focusSchedule.copy(start = m)) } }
                }
                ValueRow("Ends", label(schedule.end) + if (schedule.end <= schedule.start) " (next day)" else "") {
                    pickTime(schedule.end) { m -> actions.update { it.copy(focusSchedule = it.focusSchedule.copy(end = m)) } }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayLetters.forEach { (d, l) ->
                        val on = d in schedule.days
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (on) Mono.palette.accent else c.surfaceContainer)
                                .clickable(role = Role.Checkbox) {
                                    actions.update { s ->
                                        val days = s.focusSchedule.days
                                        s.copy(focusSchedule = s.focusSchedule.copy(days = if (d in days) days - d else days + d))
                                    }
                                }
                                .semantics {
                                    contentDescription = java.time.DayOfWeek.of(d).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                                    stateDescription = if (on) "On" else "Off"
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(l, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) Mono.palette.onAccent else c.onSurfaceVariant) }
                    }
                }
            }
            AppToggleList(
                title = "Apps hidden during focus",
                selected = settings.focusApps,
                allApps = allApps,
                emptyHint = "None yet. Tick the apps that pull you away.",
                onToggle = actions.toggleFocusApp,
            )
        }
    }
}

// --- Pause before opening -------------------------------------------------------------------

@Composable
internal fun PauseSettings(settings: Settings, allApps: List<AppEntry>, actions: SettingsActions) {
    Section("Pause before opening") {
        SettingGroup {
            Text(
                "Apps here show a short breathing pause before they open.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
            )
            AppToggleList(
                title = "Distracting apps",
                selected = settings.distractingApps,
                allApps = allApps,
                emptyHint = "None yet. You can also long-press any app and choose \"Pause before opening\".",
                onToggle = actions.toggleDistracting,
            )
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text("Pause length", fontSize = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
                Segmented(listOf(3, 5, 10), settings.nudgeSeconds, { "$it s" }, { v -> actions.update { it.copy(nudgeSeconds = v) } })
            }
        }
    }
}

// --- shared bits --------------------------------------------------------------------------

@Composable
private fun SwitchRowPadded(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(Modifier.padding(horizontal = 18.dp)) { SwitchRow(title, checked, onChange) }
}

/** Expandable list of every app with a round tick; selected apps sort first. */
@Composable
private fun AppToggleList(
    title: String,
    selected: Set<String>,
    allApps: List<AppEntry>,
    emptyHint: String,
    onToggle: (String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    // One row per package (an app can have several launcher activities / profiles).
    val apps = remember(allApps) { allApps.distinctBy { it.packageName } }
    val ordered = remember(apps, selected) { apps.sortedWith(compareByDescending<AppEntry> { it.packageName in selected }.thenBy { it.displayLabel.lowercase() }) }
    var open by androidx.compose.runtime.saveable.rememberSaveable(title) { androidx.compose.runtime.mutableStateOf(false) }
    ValueRow(title, if (open) "Done" else "${selected.size}") { open = !open }
    if (open && selected.isEmpty()) {
        Text(emptyHint, fontSize = 13.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
    }
    if (open) {
        Column(Modifier.padding(bottom = 8.dp)) {
            ordered.forEach { app ->
                val on = app.packageName in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Checkbox) { onToggle(app.packageName) }
                        .semantics { stateDescription = if (on) "Selected" else "Not selected" }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (on) Mono.palette.accent else c.surface)
                            .then(if (on) Modifier else Modifier.border(1.5.dp, c.outline, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) LineIcon(Icons.Check, 11.dp, Mono.palette.onAccent, strokeWidth = 3.5f) }
                    Spacer(Modifier.width(14.dp))
                    Text(app.displayLabel, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
