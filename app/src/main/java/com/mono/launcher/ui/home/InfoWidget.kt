package com.mono.launcher.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.LineIconSpec
import com.mono.launcher.ui.theme.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BatteryInfo(val percent: Int, val charging: Boolean, val full: Boolean)
private data class EventInfo(val title: String, val begin: Long, val end: Long, val allDay: Boolean)

/**
 * Mono's info block under the clock: next alarm, today's next event, battery.
 * Event-driven only (system broadcasts + calendar observer); nothing leaves the device.
 */
@Composable
fun InfoWidget(showCalendar: Boolean, fontScale: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var alarm by remember { mutableStateOf(nextAlarm(context)) }
    var battery by remember { mutableStateOf<BatteryInfo?>(null) }
    var event by remember { mutableStateOf<EventInfo?>(null) }
    val calendarAllowed = showCalendar &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> battery = intent.toBattery()
                    else -> {
                        alarm = nextAlarm(context)
                        tick++
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current state immediately.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)?.let { battery = it.toBattery() }
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(calendarAllowed) {
        if (!calendarAllowed) return@DisposableEffect onDispose { }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                tick++
            }
        }
        runCatching { context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer) }
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    LifecycleResumeEffect(Unit) {
        alarm = nextAlarm(context)
        tick++
        onPauseOrDispose { }
    }

    LaunchedEffect(calendarAllowed, tick) {
        event = if (calendarAllowed) withContext(Dispatchers.IO) { nextEventToday(context) } else null
    }

    val is24 = DateFormat.is24HourFormat(context)
    Row(modifier.height(IntrinsicSize.Min)) {
        // The timeline rule.
        Spacer(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 6.dp).background(Mono.palette.track))
        Column(Modifier.padding(start = 14.dp)) {
            alarm?.let { at ->
                InfoRow(Icons.Alarm, "Alarm", relativeTime(at, is24), fontScale) { SystemActions.openClock(context) }
            }
            if (showCalendar) {
                val e = event
                when {
                    !calendarAllowed -> Unit
                    e == null -> InfoRow(Icons.Calendar, "Today", "nothing else planned", fontScale) { SystemActions.openCalendar(context) }
                    else -> InfoRow(Icons.Calendar, e.title, eventTime(e, is24), fontScale) { SystemActions.openCalendar(context) }
                }
            }
            battery?.let { b ->
                val detail = when {
                    b.full -> "${b.percent}% · full"
                    b.charging -> "${b.percent}% · charging"
                    else -> "${b.percent}%"
                }
                InfoRow(if (b.charging) Icons.BatteryCharging else Icons.Battery, "Battery", detail, fontScale) {
                    if (!SystemActions.start(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))) {
                        SystemActions.start(context, Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: LineIconSpec, lead: String, detail: String, fontScale: Float, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = "Open", onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(icon, 16.dp, if (Mono.palette.colorful) Mono.palette.accent else c.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.onBackground, fontWeight = FontWeight.Medium)) { append(lead) }
                withStyle(SpanStyle(color = c.onSurfaceVariant)) { append(" · $detail") }
            },
            fontSize = (14 * fontScale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Intent.toBattery(): BatteryInfo? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return BatteryInfo(
        percent = level * 100 / scale,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
        full = status == BatteryManager.BATTERY_STATUS_FULL,
    )
}

private fun nextAlarm(context: Context): Long? =
    runCatching { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }.getOrNull()

private fun timeFormatter(is24: Boolean) = DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())

/** "6:30 AM", "Tomorrow 6:30 AM", or "Wed 6:30 AM". */
private fun relativeTime(epochMs: Long, is24: Boolean): String {
    val zone = ZoneId.systemDefault()
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone)
    val today = LocalDate.now(zone)
    val time = at.format(timeFormatter(is24))
    return when (at.toLocalDate()) {
        today -> time
        today.plusDays(1) -> "Tomorrow $time"
        else -> at.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) + " " + time
    }
}

private fun eventTime(e: EventInfo, is24: Boolean): String {
    if (e.allDay) return "all day"
    val now = System.currentTimeMillis()
    if (e.begin <= now && now < e.end) return "now"
    return relativeTime(e.begin, is24)
}

/** Next timed event that hasn't ended today; falls back to an all-day event. */
private fun nextEventToday(context: Context): EventInfo? = runCatching {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, startOfDay)
        ContentUris.appendId(it, endOfDay)
    }.build()
    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
    )
    val events = mutableListOf<EventInfo>()
    context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cur ->
        while (cur.moveToNext()) {
            events += EventInfo(
                title = cur.getString(0)?.takeIf { it.isNotBlank() } ?: "Event",
                begin = cur.getLong(1),
                end = cur.getLong(2),
                allDay = cur.getInt(3) == 1,
            )
        }
    }
    events.firstOrNull { !it.allDay && it.end > now } ?: events.firstOrNull { it.allDay }
}.getOrNull()
