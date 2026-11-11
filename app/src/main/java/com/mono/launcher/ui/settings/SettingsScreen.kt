package com.mono.launcher.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.BuildConfig
import com.mono.launcher.accessibility.LockAccessibilityService
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.ColorMode
import com.mono.launcher.data.GestureAction
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.RailSide
import com.mono.launcher.data.RailStyle
import com.mono.launcher.data.Settings
import com.mono.launcher.data.ThemeMode
import com.mono.launcher.notifications.MonoNotificationListener
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.Mo
import com.mono.launcher.ui.common.MoPose
import com.mono.launcher.ui.common.MonoSwitch
import com.mono.launcher.ui.common.SectionHeading
import com.mono.launcher.ui.common.Segmented
import com.mono.launcher.ui.common.SettingGroup
import com.mono.launcher.ui.common.Tile
import com.mono.launcher.ui.theme.Mono
import com.mono.launcher.wallpaper.Wallpaper
import com.mono.launcher.wallpaper.WallpaperTarget
import java.time.LocalDate

class SettingsActions(
    val update: ((Settings) -> Settings) -> Unit,
    val unhide: (AppEntry) -> Unit,
    val toggleMute: (String) -> Unit,
    val export: (android.net.Uri, (Result<Unit>) -> Unit) -> Unit,
    val import: (android.net.Uri, (Result<Unit>) -> Unit) -> Unit,
    val applyWallpaper: (Wallpaper, WallpaperTarget, (Result<Unit>) -> Unit) -> Unit,
    val toggleFocusApp: (String) -> Unit,
    val toggleDistracting: (String) -> Unit,
    val startFocus: (Int) -> Unit,
    val endFocus: () -> Unit,
    val pickWidget: (android.appwidget.AppWidgetProviderInfo) -> Unit,
    val removeWidget: () -> Unit,
    val close: () -> Unit,
)

private enum class Status { DONE, ATTENTION, ACTION }

@Composable
fun SettingsScreen(
    settings: Settings,
    hidden: List<AppEntry>,
    allApps: List<AppEntry>,
    focus: com.mono.launcher.data.FocusState,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val c = MaterialTheme.colorScheme
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
    val calendarGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        if (granted) actions.update { it.copy(calendarOnHome = true) }
        else Toast.makeText(context, "Calendar access is needed to show events", Toast.LENGTH_SHORT).show()
    }
    var previewing by remember { mutableStateOf<Wallpaper?>(null) }
    var pickingWidget by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, c.outline, RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button, onClick = actions.close)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Done", fontSize = 15.sp) }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                Section("Setup") {
                    SettingGroup {
                        StatusRow("Default Home app", if (isDefault) "Mono is your Home" else "Tap to set Mono as default", if (isDefault) Status.DONE else Status.ACTION) {
                            val intent = SystemActions.requestHomeRoleIntent(context)
                            if (intent != null) roleLauncher.launch(intent) else SystemActions.openHomeSettings(context)
                        }
                        StatusRow(
                            "Notification access",
                            if (notifGranted) "Granted · inline notifications on" else "Not granted · tap to allow",
                            if (notifGranted) Status.DONE else Status.ATTENTION,
                        ) { SystemActions.openNotificationAccessSettings(context) }
                        StatusRow(
                            "Double-tap to lock",
                            if (a11yEnabled) "Accessibility service on" else "Needs accessibility service",
                            if (a11yEnabled) Status.DONE else Status.ACTION,
                        ) { SystemActions.openAccessibilitySettings(context) }
                    }
                }
            }

            item {
                Section("Wallpaper") {
                    SettingGroup(contentPadding = 0) {
                        WallpaperCarousel(settings.wallpaperId) { previewing = it }
                    }
                }
            }

            item {
                Section("Layout") {
                    SettingGroup(contentPadding = 0) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            Labeled("Style") {
                                Segmented(
                                    listOf(ColorMode.MONO, ColorMode.COLOUR),
                                    settings.colorMode,
                                    { if (it == ColorMode.MONO) "Mono" else "Colour" },
                                    { v -> actions.update { it.copy(colorMode = v) } },
                                )
                            }
                            Labeled("Font") { FontPicker(settings.fontId) { id -> actions.update { it.copy(fontId = id) } } }
                            Labeled("Alphabet behaviour") {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RailStyle.entries.forEach { style ->
                                        val on = style == settings.railStyle
                                        Tile(on, { actions.update { it.copy(railStyle = style) } }, Modifier.weight(1f).semantics { stateDescription = if (on) "Selected" else "" }) {
                                            Spacer(Modifier.height(10.dp))
                                            RailPreview(style)
                                            Text(
                                                railStyleName(style),
                                                fontSize = 13.sp,
                                                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                                                color = if (on) c.onBackground else c.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Labeled("Icons") {
                                if (settings.colorMode == ColorMode.COLOUR) {
                                    Text("Colour style shows icons in full colour. These apply in Mono.", fontSize = 13.sp, color = c.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(IconStyle.NONE, IconStyle.MINIMAL, IconStyle.MONOCHROME, IconStyle.SYSTEM).forEach { style ->
                                        val on = style == settings.iconStyle
                                        Tile(on, { actions.update { it.copy(iconStyle = style) } }, Modifier.weight(1f).height(64.dp), radius = 12) {
                                            IconStylePreview(style)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                iconStyleName(style),
                                                fontSize = 12.sp,
                                                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                                                color = if (on) c.onBackground else c.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            FontScaleItem(settings.fontScale) { v -> actions.update { it.copy(fontScale = v) } }
                            Labeled("Theme") {
                                Segmented(
                                    listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM, ThemeMode.DYNAMIC),
                                    settings.theme,
                                    { when (it) { ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"; ThemeMode.SYSTEM -> "System"; ThemeMode.DYNAMIC -> "You" } },
                                    { v -> actions.update { it.copy(theme = v) } },
                                )
                            }
                            Labeled("Alphabet rail") {
                                Segmented(
                                    listOf(RailSide.LEFT, RailSide.RIGHT),
                                    settings.railSide,
                                    { if (it == RailSide.LEFT) "Left edge" else "Right edge" },
                                    { v -> actions.update { it.copy(railSide = v) } },
                                )
                            }
                            Column {
                                SwitchRow("Info widget", settings.infoWidget) { v -> actions.update { it.copy(infoWidget = v) } }
                                if (settings.infoWidget) {
                                    SwitchRow("Calendar on Home", settings.calendarOnHome && calendarGranted) { v ->
                                        if (v && !calendarGranted) calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                                        else actions.update { it.copy(calendarOnHome = v) }
                                    }
                                }
                                SwitchRow("Media card", settings.showMediaCard) { v -> actions.update { it.copy(showMediaCard = v) } }
                                SwitchRow("Haptics", settings.haptics) { v -> actions.update { it.copy(haptics = v) } }
                            }
                        }
                    }
                }
            }

            item { WidgetSettings(settings, actions) { pickingWidget = true } }
            item { FocusSettings(settings, focus, allApps, actions) }
            item { PauseSettings(settings, allApps, actions) }

            item {
                Section("Gestures") {
                    SettingGroup {
                        GestureRow("Swipe down", settings.swipeDown) { v -> actions.update { it.copy(swipeDown = v) } }
                        GestureRow("Double-tap", settings.doubleTap) { v -> actions.update { it.copy(doubleTap = v) } }
                        GestureRow("Swipe right", settings.swipeRight) { v -> actions.update { it.copy(swipeRight = v) } }
                    }
                }
            }

            item {
                Section("Apps & data") {
                    SettingGroup {
                        ExpandableRow("Hidden apps", hidden.size, "No hidden apps. Long-press any app and choose Hide.") {
                            hidden.forEach { app ->
                                SubRow(app.displayLabel + if (app.isWorkProfile) " (work)" else "", "Unhide") { actions.unhide(app) }
                            }
                        }
                        ExpandableRow("Muted notifications", settings.mutedPackages.size, "No muted apps. Long-press an app to mute it on Home.") {
                            settings.mutedPackages.sorted().forEach { pkg ->
                                SubRow(labelFor[pkg] ?: pkg, "Unmute") { actions.toggleMute(pkg) }
                            }
                        }
                        ValueRow("Export settings") { exporter.launch("mono-backup-${LocalDate.now()}.json") }
                        ValueRow("Import settings") { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
                        ValueRow("Privacy policy") { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } }
                    }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Mo(MoPose.OUTLINE, 40.dp, body = Mono.palette.tertiaryText)
                    Text(
                        "Mono ${BuildConfig.VERSION_NAME} · All data stays on this device.\nNo account, no ads, no tracking.",
                        fontSize = 13.sp,
                        color = Mono.palette.tertiaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }

    if (pickingWidget) {
        WidgetPicker(onPick = actions.pickWidget, onClose = { pickingWidget = false })
    }
    previewing?.let { wp ->
        WallpaperPreview(wp, settings.fontScale, actions.applyWallpaper) { previewing = null }
    }
    }
}

private const val PRIVACY_POLICY_URL = "https://github.com/harshit36singh/Mono/blob/main/PRIVACY.md"

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeading(title, Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp))
        content()
    }
}

@Composable
internal fun Labeled(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 16.sp)
        content()
    }
}

@Composable
private fun StatusRow(title: String, subtitle: String, status: Status, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        when (status) {
            Status.DONE -> LineIcon(Icons.Check, 18.dp, Mono.palette.accent)
            Status.ATTENTION -> Box(Modifier.size(8.dp).clip(CircleShape).background(Mono.palette.accent))
            Status.ACTION -> LineIcon(Icons.ChevronRight, 18.dp, c.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ValueRow(title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ExpandableRow(title: String, count: Int, emptyText: String, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        ValueRow(title, "$count") { open = !open }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(bottom = 6.dp)) {
                if (count == 0) {
                    Text(emptyText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
internal fun SubRow(title: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick)
            .padding(start = 30.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(action, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .semantics { stateDescription = if (checked) "On" else "Off" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        MonoSwitch(checked)
    }
}

@Composable
private fun GestureRow(title: String, selected: GestureAction, onSelect: (GestureAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ValueRow(title, gestureName(selected)) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            GestureAction.entries.forEach { o ->
                DropdownMenuItem(
                    text = { Text(gestureName(o), fontWeight = if (o == selected) FontWeight.Medium else FontWeight.Normal) },
                    trailingIcon = if (o == selected) ({ LineIcon(Icons.Check, 16.dp, MaterialTheme.colorScheme.onSurface) }) else null,
                    onClick = { onSelect(o); open = false },
                )
            }
        }
    }
}

@Composable
private fun FontScaleItem(value: Float, onChange: (Float) -> Unit) {
    val c = MaterialTheme.colorScheme
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Text size", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${(local * 100).toInt()}%", fontSize = 13.sp, color = c.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aa", fontSize = 13.sp, color = c.onSurfaceVariant)
            Slider(
                value = local,
                onValueChange = { local = it },
                onValueChangeFinished = { onChange(local) },
                valueRange = 0.8f..1.4f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = c.onBackground,
                    activeTrackColor = c.onBackground,
                    inactiveTrackColor = Mono.palette.track,
                    activeTickColor = c.onBackground.copy(alpha = 0f),
                    inactiveTickColor = c.onBackground.copy(alpha = 0f),
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp).semantics { contentDescription = "Text size" },
            )
            Text("Aa", fontSize = 22.sp)
        }
    }
}

/** Horizontal row of font tiles, each previewing "Aa" in its own face. */
@Composable
private fun FontPicker(selectedId: String, onSelect: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(com.mono.launcher.ui.theme.FontChoices.all.size) { i ->
            val f = com.mono.launcher.ui.theme.FontChoices.all[i]
            val on = f.id == selectedId
            val family = remember(f.id) { com.mono.launcher.ui.theme.FontChoices.family(f) }
            Tile(on, { onSelect(f.id) }, Modifier.size(84.dp, 76.dp).semantics { contentDescription = f.name }, radius = 12) {
                Text("Aa", fontSize = 24.sp, fontFamily = family, color = c.onBackground)
                Text(
                    f.name,
                    fontSize = 11.sp,
                    fontFamily = family,
                    maxLines = 1,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) c.onBackground else c.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
                )
            }
        }
    }
}

// --- Previews drawn from the design's mini illustrations (72×90 grid) ------------------------

@Composable
private fun RailPreview(style: RailStyle) {
    val c = MaterialTheme.colorScheme
    val p = Mono.palette
    val fg = c.onBackground
    val dim = c.outline
    val mid = c.onSurfaceVariant
    val track = p.track
    val tertiary = p.tertiaryText
    Canvas(Modifier.size(72.dp, 90.dp)) {
        scale(size.width / 72f, pivot = Offset.Zero) {
            when (style) {
                RailStyle.WAVE -> {
                    bar(6f, 30f, 30f, 5f, dim)
                    bar(6f, 41f, 34f, 9f, track)
                    bar(9f, 43.5f, 22f, 4f, fg)
                    bar(6f, 56f, 26f, 5f, dim)
                    listOf(64f to 8f, 63f to 18f, 59f to 28f, 52f to 38f, 52f to 54f, 59f to 64f, 63f to 74f, 64f to 84f)
                        .forEachIndexed { i, (x, y) -> drawCircle(mid, radius = listOf(1.6f, 1.6f, 1.8f, 2.2f, 2.2f, 1.8f, 1.6f, 1.6f)[i], center = Offset(x, y)) }
                    drawCircle(fg, 2.6f, Offset(49f, 46f))
                }
                RailStyle.POPOUT -> {
                    bar(4f, 28f, 44f, 36f, track, 8f)
                    bar(10f, 36f, 26f, 4f, fg)
                    bar(10f, 45f, 30f, 4f, tertiary)
                    bar(10f, 54f, 22f, 4f, tertiary)
                    listOf(8f, 18f, 28f, 38f, 54f, 64f, 74f, 84f).forEach { drawCircle(mid, 1.6f, Offset(64f, it)) }
                    drawCircle(fg, 4f, Offset(58f, 46f))
                }
                RailStyle.LIST -> {
                    listOf(6f, 38f, 79f).forEach { bar(6f, it, 8f, 4f, mid) }
                    listOf(16f to 36f, 25f to 30f, 48f to 40f, 57f to 28f, 66f to 34f).forEach { (y, w) -> bar(6f, y, w, 4f, dim) }
                    listOf(10f, 22f, 34f, 58f, 70f, 82f).forEach { drawCircle(mid, 1.6f, Offset(64f, it)) }
                    drawCircle(fg, 2.6f, Offset(64f, 46f))
                }
            }
        }
    }
}

private fun DrawScope.bar(x: Float, y: Float, w: Float, h: Float, color: Color, r: Float = h / 2f) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))

@Composable
private fun IconStylePreview(style: IconStyle) {
    val c = MaterialTheme.colorScheme
    when (style) {
        IconStyle.NONE -> Text("Aa", fontSize = 15.sp, color = c.onBackground)
        IconStyle.MINIMAL -> LineIcon(Icons.Chat, 20.dp, c.onBackground, strokeWidth = 1.8f)
        IconStyle.MONOCHROME -> Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color(0xFF9A9A9A), Color(0xFF5E5E5E)))))
        IconStyle.SYSTEM -> Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color(0xFF5AA9E6), Color(0xFFE58B4A)))))
    }
}

private fun railStyleName(s: RailStyle) = when (s) {
    RailStyle.WAVE -> "Wave"
    RailStyle.POPOUT -> "Popout"
    RailStyle.LIST -> "List"
}

private fun iconStyleName(s: IconStyle) = when (s) {
    IconStyle.NONE -> "Text"
    IconStyle.MINIMAL -> "Minimal"
    IconStyle.MONOCHROME -> "Grey"
    IconStyle.SYSTEM -> "Colour"
}

private fun gestureName(g: GestureAction) = when (g) {
    GestureAction.NONE -> "Do nothing"
    GestureAction.MONO_PANEL -> "Mono panel"
    GestureAction.NOTIFICATION_SHADE -> "Notification shade"
    GestureAction.LOCK_SCREEN -> "Lock screen"
    GestureAction.SEARCH -> "Search"
    GestureAction.OPEN_SETTINGS -> "Mono settings"
}
