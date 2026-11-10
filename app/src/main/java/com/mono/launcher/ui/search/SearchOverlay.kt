package com.mono.launcher.ui.search

import android.app.SearchManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.FuzzyMatcher
import com.mono.launcher.data.IconStyle
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.common.rowGestures
import com.mono.launcher.ui.home.WorkBadge

/**
 * Full-screen search. Input sits at the bottom (thumb reach); best result is closest to it.
 * Enter launches the top result.
 */
@Composable
fun SearchOverlay(
    query: String,
    results: List<AppEntry>,
    iconStyle: IconStyle,
    fontScale: Float,
    onQuery: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLaunchTop: () -> Boolean,
    onLongPress: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val showSettingsEntry = query.isNotBlank() &&
        (FuzzyMatcher.score(query, "Mono settings") ?: 0) >= 600

    fun webSearch() {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (SystemActions.start(context, intent)) onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
        ) {
            if (query.isBlank() && results.isEmpty()) {
                item { Hint("Type to search your apps") }
            }
            if (query.isNotBlank() && results.isEmpty()) {
                item { Hint("No apps match “$query”") }
            }
            items(results, key = { "${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                ResultRow(
                    label = app.displayLabel,
                    fontScale = fontScale,
                    leading = { AppIcon(app, iconStyle, 28.dp) },
                    work = app.isWorkProfile,
                    modifier = Modifier.rowGestures(app, onTap = { onLaunch(app) }, onLongPress = { onLongPress(app) }),
                )
            }
            if (showSettingsEntry) {
                item { ResultRow("Mono settings", fontScale, modifier = Modifier.rowGestures("settings", onOpenSettings, onOpenSettings)) }
            }
            if (query.isNotBlank()) {
                item {
                    ResultRow(
                        "Search the web for “$query”",
                        fontScale * 0.8f,
                        modifier = Modifier.rowGestures("web", { webSearch() }, { webSearch() }),
                        muted = true,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text("Search apps") },
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onGo = { if (!onLaunchTop() && query.isNotBlank()) webSearch() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    fontScale: Float,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    work: Boolean = false,
    muted: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box { leading() }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            fontSize = (20 * fontScale).sp,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (work) WorkBadge()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
}
