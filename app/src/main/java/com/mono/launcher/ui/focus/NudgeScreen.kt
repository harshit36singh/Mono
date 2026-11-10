package com.mono.launcher.ui.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.ui.common.Mo
import com.mono.launcher.ui.common.MoPose
import com.mono.launcher.ui.common.PrimaryButton
import com.mono.launcher.ui.common.TextAction
import com.mono.launcher.ui.theme.Mono
import kotlin.math.ceil

/**
 * Gentle pause before an app the user marked as distracting. "Open anyway" unlocks after a
 * short countdown; "Not now" goes back to Home.
 */
@Composable
fun NudgeScreen(app: AppEntry, seconds: Int, onOpen: () -> Unit, onCancel: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val progress = remember(app.key) { Animatable(0f) }
    LaunchedEffect(app.key) {
        progress.animateTo(1f, tween(seconds.coerceAtLeast(1) * 1000, easing = LinearEasing))
    }
    BackHandler(onBack = onCancel)
    val done = progress.value >= 1f
    val remaining = ceil((1f - progress.value) * seconds).toInt().coerceAtLeast(0)
    val track = Mono.palette.track
    val accent = Mono.palette.accent

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(196.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(accent, -90f, 360f * progress.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Mo(MoPose.RESTING, 120.dp)
        }
        Spacer(Modifier.height(32.dp))
        Text("Take a breath", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text(
            "You marked ${app.displayLabel} as distracting.\nStill want to open it?",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            if (done) "Ready when you are" else "$remaining",
            fontSize = if (done) 13.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (done) c.onSurfaceVariant else c.onBackground,
            modifier = Modifier.padding(top = 20.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(if (done) "Open ${app.displayLabel}" else "Open anyway", onOpen, Modifier.fillMaxWidth(), enabled = done)
            TextAction("Not now", onCancel, Modifier.fillMaxWidth(), emphasized = true)
        }
    }
}
