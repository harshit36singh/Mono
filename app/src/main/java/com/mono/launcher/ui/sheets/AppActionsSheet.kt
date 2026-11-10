package com.mono.launcher.ui.sheets

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.MAX_FAVORITES
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.LineIconSpec
import com.mono.launcher.ui.common.WorkTag
import com.mono.launcher.ui.theme.Mono

class AppSheetActions(
    val loadShortcuts: suspend (AppEntry) -> List<ShortcutInfo>,
    val launchShortcut: (ShortcutInfo) -> Unit,
    val toggleFavorite: (AppEntry) -> Unit,
    val rename: (AppEntry) -> Unit,
    val setHidden: (AppEntry, Boolean) -> Unit,
    val toggleMute: (String) -> Unit,
    val appInfo: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val dismiss: () -> Unit,
)

/** Long-press sheet: shortcuts as chips, then favorite, rename, hide, mute, app info, uninstall. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    favoritesCount: Int,
    muted: Boolean,
    iconStyle: IconStyle,
    actions: AppSheetActions,
) {
    val c = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shortcuts by produceState(emptyList<ShortcutInfo>(), app.key) { value = actions.loadShortcuts(app) }

    ModalBottomSheet(
        onDismissRequest = actions.dismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.outline))
        },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, if (iconStyle == IconStyle.NONE) IconStyle.MINIMAL else iconStyle, 32.dp)
                Spacer(Modifier.width(16.dp))
                Text(app.displayLabel, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (app.isWorkProfile) WorkTag()
            }

            if (shortcuts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    shortcuts.forEach { sc ->
                        Row(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(c.surfaceContainer)
                                .clickable(role = Role.Button) { actions.launchShortcut(sc) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LineIcon(Icons.Shortcut, 16.dp, c.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text((sc.shortLabel ?: sc.longLabel ?: sc.id).toString(), fontSize = 15.sp, maxLines = 1)
                        }
                    }
                }
            }

            Box(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp).fillMaxWidth().height(1.dp).background(Mono.palette.divider))

            val full = !app.isFavorite && favoritesCount >= MAX_FAVORITES
            ActionRow(
                Icons.Star,
                when {
                    app.isFavorite -> "Remove from favorites"
                    full -> "Favorites full ($MAX_FAVORITES)"
                    else -> "Add to favorites"
                },
                enabled = !full,
            ) { actions.toggleFavorite(app); actions.dismiss() }
            ActionRow(Icons.Pencil, "Rename") { actions.rename(app) }
            ActionRow(if (app.isHidden) Icons.Eye else Icons.EyeOff, if (app.isHidden) "Unhide" else "Hide") { actions.setHidden(app, !app.isHidden); actions.dismiss() }
            ActionRow(Icons.Bell, if (muted) "Show notifications on Home" else "Mute notifications on Home") { actions.toggleMute(app.packageName); actions.dismiss() }
            ActionRow(Icons.Info, "App info") { actions.appInfo(app); actions.dismiss() }
            ActionRow(Icons.Trash, "Uninstall") { actions.uninstall(app); actions.dismiss() }
        }
    }
}

@Composable
private fun ActionRow(icon: LineIconSpec, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.38f
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(icon, 20.dp, c.onSurfaceVariant.copy(alpha = alpha))
        Spacer(Modifier.width(18.dp))
        Text(label, fontSize = 17.sp, color = c.onSurface.copy(alpha = alpha))
    }
}

@Composable
fun RenameDialog(app: AppEntry, onRename: (String?) -> Unit, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(app.displayLabel) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rename", fontSize = 22.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name on Home", fontSize = 12.sp, color = c.onSurfaceVariant)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = c.onSurface),
                    cursorBrush = SolidColor(c.onSurface),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surfaceContainer)
                                .border(1.5.dp, c.onSurface, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { inner() }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Name on Home" },
                )
                Text("Original: ${app.label}", fontSize = 13.sp, color = c.onSurfaceVariant)
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (app.customLabel != null) DialogButton("Reset", c.onSurfaceVariant) { onRename(null) }
                Spacer(Modifier.weight(1f))
                DialogButton("Cancel", c.onSurface, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.onSurface)
                        .clickable(role = Role.Button) { onRename(text) }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Save", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.surface) }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 15.sp, color = color) }
}
