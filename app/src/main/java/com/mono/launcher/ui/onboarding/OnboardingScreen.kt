package com.mono.launcher.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.AppKey
import com.mono.launcher.data.MAX_FAVORITES
import com.mono.launcher.notifications.MonoNotificationListener
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.CountBadge
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.Mo
import com.mono.launcher.ui.common.MoPose
import com.mono.launcher.ui.common.PrimaryButton
import com.mono.launcher.ui.common.TextAction
import com.mono.launcher.ui.theme.Mono

/** Three steps: pick favorites → notification access → set as default launcher. */
@Composable
fun OnboardingScreen(apps: List<AppEntry>, onDone: (List<AppKey>) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<AppKey>() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 20.dp),
    ) {
        StepHeader(step)
        when (step) {
            0 -> PickFavorites(apps, picked) { step = 1 }
            1 -> NotificationStep { step = 2 }
            else -> DefaultStep { onDone(picked.toList()) }
        }
    }
}

@Composable
private fun StepHeader(step: Int) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Step ${step + 1} of 3", fontSize = 13.sp, color = c.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clearAndSetSemantics { }) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(24.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) Mono.palette.accent else Mono.palette.track),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PickFavorites(apps: List<AppEntry>, picked: MutableList<AppKey>, onNext: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val sorted = remember(apps) {
        apps.sortedWith(compareByDescending<AppEntry> { it.launchCount }.thenBy { it.displayLabel.lowercase() })
    }
    Row(Modifier.fillMaxWidth().padding(top = 40.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text("Pick your favorites", fontSize = 28.sp, fontWeight = FontWeight.Light)
            Text("Up to $MAX_FAVORITES apps you open most.", fontSize = 15.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        Mo(MoPose.WAVE, 64.dp)
    }
    Box(Modifier.weight(1f).fillMaxWidth().padding(top = 28.dp)) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(sorted, key = { "${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                val checked = app.key in picked
                val enabled = checked || picked.size < MAX_FAVORITES
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(enabled = enabled, role = Role.Checkbox) {
                            if (checked) picked.remove(app.key) else picked.add(app.key)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundCheck(checked, enabled)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        app.displayLabel + if (app.isWorkProfile) " (work)" else "",
                        fontSize = 17.sp,
                        color = c.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
                    )
                }
            }
        }
        // Fade the list into the footer.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Brush.verticalGradient(listOf(c.background.copy(alpha = 0f), c.background))),
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.onBackground, fontWeight = FontWeight.Medium)) { append("${picked.size}") }
                append(" / $MAX_FAVORITES")
            },
            fontSize = 15.sp,
            color = c.onSurfaceVariant,
        )
        PrimaryButton("Next", onNext)
    }
}

@Composable
private fun RoundCheck(checked: Boolean, enabled: Boolean) {
    val c = MaterialTheme.colorScheme
    val outline = if (enabled) c.outline else c.outline.copy(alpha = 0.4f)
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) Mono.palette.accent else c.background.copy(alpha = 0f))
            .then(if (checked) Modifier else Modifier.border(1.5.dp, outline, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) LineIcon(Icons.Check, 12.dp, Mono.palette.onAccent, strokeWidth = 3.5f)
    }
}

@Composable
private fun ColumnScope.NotificationStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    var granted by remember { mutableStateOf(MonoNotificationListener.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        granted = MonoNotificationListener.isEnabled(context)
        onPauseOrDispose { }
    }
    Column(
        Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mo(MoPose.NOTICING, 132.dp, description = "Mo noticing a notification")
        // Example of an inline notification on Home.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clearAndSetSemantics { },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon(Icons.Chat, 24.dp, c.onBackground)
                Spacer(Modifier.width(18.dp))
                Text("WhatsApp", fontSize = 21.sp)
                Spacer(Modifier.width(18.dp))
                CountBadge(2)
            }
            Text("Riya · see you at 6", fontSize = 14.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 42.dp, top = 6.dp))
        }
    }
    Text("See notifications in place", fontSize = 28.sp, fontWeight = FontWeight.Light)
    Text(
        "The latest message shows as one line under its app. Swipe it left to clear.",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = c.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        LineIcon(Icons.Lock, 16.dp, c.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text("Stays on this phone. No account, no tracking.", fontSize = 13.sp, color = c.onSurfaceVariant)
    }
    Spacer(Modifier.height(28.dp))
    if (granted) {
        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LineIcon(Icons.Check, 18.dp, c.onBackground)
            Spacer(Modifier.width(8.dp))
            Text("Notification access granted", fontSize = 16.sp)
        }
    } else {
        PrimaryButton("Grant notification access", { SystemActions.openNotificationAccessSettings(context) }, Modifier.fillMaxWidth())
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        if (!granted) TextAction("Skip", onNext) else Spacer(Modifier.width(1.dp))
        TextAction("Next", onNext, emphasized = true)
    }
}

@Composable
private fun ColumnScope.DefaultStep(onDone: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    var isDefault by remember { mutableStateOf(SystemActions.isDefaultLauncher(context)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = SystemActions.isDefaultLauncher(context)
    }
    LifecycleResumeEffect(Unit) {
        isDefault = SystemActions.isDefaultLauncher(context)
        onPauseOrDispose { }
    }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Mo(MoPose.DONE, 150.dp, description = "Mo celebrating")
    }
    Text("Make Mono your Home", fontSize = 28.sp, fontWeight = FontWeight.Light)
    Text(
        "Press Home and you land here. Three things worth knowing:",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = c.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Tip("Swipe up to search") { LineIcon(Icons.ArrowUp, 18.dp, c.onBackground) }
        Tip("Drag the letters to reach any app") { Text("A–Z", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        Tip("Long-press empty space for settings") { LineIcon(Icons.LongPress, 18.dp, c.onBackground) }
    }
    Spacer(Modifier.height(28.dp))
    if (isDefault) {
        PrimaryButton("Finish", onDone, Modifier.fillMaxWidth())
    } else {
        PrimaryButton("Set as default", {
            val intent = SystemActions.requestHomeRoleIntent(context)
            if (intent != null) roleLauncher.launch(intent) else SystemActions.openHomeSettings(context)
        }, Modifier.fillMaxWidth())
        TextAction("Finish anyway", onDone, Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun Tip(text: String, icon: @Composable () -> Unit) {
    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(16.dp))
        Text(text, fontSize = 16.sp)
    }
}
