package com.mono.launcher.widget

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Shows the user's chosen app widget; renders nothing when none is set or it was uninstalled. */
@Composable
fun WidgetSlot(widgetId: Int, modifier: Modifier = Modifier) {
    val hosts = LocalWidgetHost.current ?: return
    if (widgetId < 0) return
    val info = remember(widgetId) { hosts.info(widgetId) } ?: return
    // minHeight is in pixels; keep the slot within a calm range so it never dominates Home.
    val height = with(LocalDensity.current) { info.minHeight.toDp() }.coerceIn(72.dp, 280.dp)
    key(widgetId) {
        AndroidView(
            factory = { ctx -> hosts.createView(ctx, widgetId) ?: View(ctx) },
            modifier = modifier.fillMaxWidth().height(height),
        )
    }
}
