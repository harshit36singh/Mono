package com.mono.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconStyle
import com.mono.launcher.notifications.NotificationState
import com.mono.launcher.ui.common.AppIcon
import kotlinx.coroutines.launch

/**
 * One favorite on Home: large name, optional icon, and the latest notification beneath it.
 * Gestures on the row itself are attached by the caller via [modifier].
 */
@Composable
fun AppRow(
    app: AppEntry,
    notification: NotificationState?,
    iconStyle: IconStyle,
    fontScale: Float,
    onOpenNotification: () -> Unit,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = buildString {
        append(app.displayLabel)
        if (app.isWorkProfile) append(", work app")
        notification?.let {
            append(". ")
            append(if (it.count > 1) "${it.count} new notifications" else "New notification")
            append(": ${it.title} ${it.text}")
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (notification != null) {
                    customActions = listOf(
                        CustomAccessibilityAction("Open notification") { onOpenNotification(); true },
                        CustomAccessibilityAction("Dismiss notification") { onDismissNotification(); true },
                    )
                }
            }
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconStyle != IconStyle.NONE) {
                AppIcon(app, iconStyle, (24 * fontScale).dp)
                Spacer(Modifier.width(18.dp))
            }
            Text(
                text = app.displayLabel,
                fontSize = (21 * fontScale).sp,
                fontWeight = FontWeight.Normal,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.isWorkProfile) WorkBadge()
            if (notification != null) {
                Spacer(Modifier.width(10.dp))
                CountBadge(notification.count)
            }
        }
        if (notification != null) {
            NotificationLine(
                notification = notification,
                indent = if (iconStyle != IconStyle.NONE) (24 * fontScale + 18).dp else 0.dp,
                fontScale = fontScale,
                onOpen = onOpenNotification,
                onDismiss = onDismissNotification,
            )
        }
    }
}

@Composable
private fun NotificationLine(
    notification: NotificationState,
    indent: androidx.compose.ui.unit.Dp,
    fontScale: Float,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val offset = remember(notification.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val text = listOf(notification.title, notification.text).filter { it.isNotBlank() }.joinToString(" · ")
    Text(
        text = text.ifEmpty { "New notification" },
        fontSize = (14 * fontScale).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = indent, top = 2.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (-offset.value / (threshold * 2)).coerceIn(0f, 0.8f)
            }
            // Swipe left to dismiss.
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceAtMost(0f)) } },
                onDragStopped = {
                    if (offset.value < -threshold) {
                        offset.animateTo(-threshold * 4, tween(150))
                        onDismiss()
                    } else {
                        offset.animateTo(0f, tween(150))
                    }
                },
            )
            .clickable(onClick = onOpen),
    )
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = if (count > 1) "$count new" else "•",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = if (count > 1) 7.dp else 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
fun WorkBadge() {
    Row(horizontalArrangement = Arrangement.Start) {
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text("work", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
        }
    }
}
