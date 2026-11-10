package com.mono.launcher.ui.sheets

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.MAX_FAVORITES
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.home.WorkBadge

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

/** Long-press sheet: shortcuts, favorite, rename, hide, mute, app info, uninstall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    favoritesCount: Int,
    muted: Boolean,
    iconStyle: IconStyle,
    actions: AppSheetActions,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shortcuts by produceState(emptyList<ShortcutInfo>(), app.key) { value = actions.loadShortcuts(app) }

    ModalBottomSheet(onDismissRequest = actions.dismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, if (iconStyle == IconStyle.NONE) IconStyle.SYSTEM else iconStyle, 32.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.displayLabel, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                if (app.isWorkProfile) WorkBadge()
            }

            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { sc ->
                    ActionRow((sc.shortLabel ?: sc.longLabel ?: sc.id).toString(), secondary = true) { actions.launchShortcut(sc) }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 24.dp))
            }

            val full = !app.isFavorite && favoritesCount >= MAX_FAVORITES
            ActionRow(
                when {
                    app.isFavorite -> "Remove from favorites"
                    full -> "Favorites full ($MAX_FAVORITES)"
                    else -> "Add to favorites"
                },
                enabled = !full,
            ) { actions.toggleFavorite(app); actions.dismiss() }
            ActionRow("Rename") { actions.rename(app) }
            ActionRow(if (app.isHidden) "Unhide" else "Hide") { actions.setHidden(app, !app.isHidden); actions.dismiss() }
            ActionRow(if (muted) "Show notifications on Home" else "Mute notifications on Home") { actions.toggleMute(app.packageName); actions.dismiss() }
            ActionRow("App info") { actions.appInfo(app); actions.dismiss() }
            ActionRow("Uninstall") { actions.uninstall(app); actions.dismiss() }
        }
    }
}

@Composable
private fun ActionRow(label: String, secondary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 17.sp,
        color = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            secondary -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
fun RenameDialog(app: AppEntry, onRename: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(app.displayLabel) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Spacer(Modifier.height(4.dp))
                Text("Original: ${app.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onRename(text) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (app.customLabel != null) TextButton(onClick = { onRename(null) }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
