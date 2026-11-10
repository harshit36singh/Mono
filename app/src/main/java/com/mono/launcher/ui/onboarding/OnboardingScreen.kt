package com.mono.launcher.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.AppKey
import com.mono.launcher.data.MAX_FAVORITES
import com.mono.launcher.notifications.MonoNotificationListener
import com.mono.launcher.system.SystemActions

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
            .padding(24.dp),
    ) {
        Text("Step ${step + 1} of 3", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        when (step) {
            0 -> PickFavorites(apps, picked, Modifier.weight(1f)) { step = 1 }
            1 -> NotificationStep(Modifier.weight(1f)) { step = 2 }
            else -> DefaultStep(Modifier.weight(1f)) { onDone(picked.toList()) }
        }
    }
}

@Composable
private fun PickFavorites(apps: List<AppEntry>, picked: MutableList<AppKey>, modifier: Modifier, onNext: () -> Unit) {
    val sorted = remember(apps) {
        apps.sortedWith(compareByDescending<AppEntry> { it.launchCount }.thenBy { it.displayLabel.lowercase() })
    }
    Text("Pick your favorites", fontSize = 28.sp, fontWeight = FontWeight.Light)
    Text(
        "Up to $MAX_FAVORITES apps you open most. Everything else is one swipe away on the A–Z rail.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
    )
    LazyColumn(modifier.fillMaxWidth()) {
        items(sorted, key = { "${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
            val checked = app.key in picked
            val enabled = checked || picked.size < MAX_FAVORITES
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, role = Role.Checkbox) {
                        if (checked) picked.remove(app.key) else picked.add(app.key)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Text(
                    app.displayLabel + if (app.isWorkProfile) " (work)" else "",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${picked.size} / $MAX_FAVORITES", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onNext) { Text("Next") }
    }
}

@Composable
private fun NotificationStep(modifier: Modifier, onNext: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(MonoNotificationListener.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        granted = MonoNotificationListener.isEnabled(context)
        onPauseOrDispose { }
    }
    Column(modifier) {
        Text("See notifications in place", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text(
            "Mono shows the latest notification right under the app it belongs to, so you can act without opening the shade.\n\n" +
                "Notification content stays on this phone. Mono has no account, no server and no tracking.\n\n" +
                "This is optional — everything else works without it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
        if (granted) {
            Text("✓ Notification access granted", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        } else {
            Button(onClick = { SystemActions.openNotificationAccessSettings(context) }) { Text("Grant notification access") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (!granted) TextButton(onClick = onNext) { Text("Skip") }
        Button(onClick = onNext, enabled = granted) { Text("Next") }
    }
}

@Composable
private fun DefaultStep(modifier: Modifier, onDone: () -> Unit) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(SystemActions.isDefaultLauncher(context)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = SystemActions.isDefaultLauncher(context)
    }
    LifecycleResumeEffect(Unit) {
        isDefault = SystemActions.isDefaultLauncher(context)
        onPauseOrDispose { }
    }
    Column(modifier) {
        Text("Make Mono your Home", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text(
            "Set Mono as the default Home app so the Home button brings you here.\n\n" +
                "Tips: long-press empty space for settings, swipe up to search, and drag along the letters on the edge to reach any app.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
        if (isDefault) {
            Text("✓ Mono is your default Home app", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        } else {
            Button(onClick = {
                val intent = SystemActions.requestHomeRoleIntent(context)
                if (intent != null) roleLauncher.launch(intent) else SystemActions.openHomeSettings(context)
            }) { Text("Set as default") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(onClick = onDone) { Text(if (isDefault) "Finish" else "Finish anyway") }
    }
}
