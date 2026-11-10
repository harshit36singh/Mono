package com.mono.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mono.launcher.system.SystemActions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time + date. Updates from the system TIME_TICK broadcast — no timers or polling. */
@Composable
fun ClockHeader(fontScale: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = LocalDateTime.now()
        onDispose { context.unregisterReceiver(receiver) }
    }

    val locale = Locale.getDefault()
    val timeFormatter = remember(locale, DateFormat.is24HourFormat(context)) {
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale)
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }

    Column(modifier) {
        Text(
            text = now.format(timeFormatter),
            fontSize = (56 * fontScale).sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open clock") { SystemActions.openClock(context) },
        )
        Text(
            text = now.format(dateFormatter),
            fontSize = (16 * fontScale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable(role = Role.Button, onClickLabel = "Open calendar") { SystemActions.openCalendar(context) },
        )
    }
}
