package com.mono.launcher.ui.search

import android.app.SearchManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.FuzzyMatcher
import com.mono.launcher.data.IconStyle
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.Mo
import com.mono.launcher.ui.common.MoPose
import com.mono.launcher.ui.common.WorkTag
import com.mono.launcher.ui.common.rowGestures

/**
 * Full-screen search. Input sits at the bottom (thumb reach); results grow upward and the
 * best match sits closest to the field, highlighted with a "Go" hint. Enter launches it.
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
    val c = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val showSettingsEntry = query.isNotBlank() && (FuzzyMatcher.score(query, "Mono settings") ?: 0) >= 600

    fun webSearch() {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (SystemActions.start(context, intent)) onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mo(MoPose.RESTING, 72.dp)
                        Hint("No apps match “$query”")
                    }
                }
            }
            itemsIndexed(results, key = { _, it -> "${it.packageName}/${it.activityName}/${it.userSerial}" }) { i, app ->
                ResultRow(
                    label = highlightMatch(app.displayLabel, query),
                    fontScale = fontScale,
                    best = i == 0 && query.isNotBlank(),
                    leading = { AppIcon(app, iconStyle, 28.dp) },
                    work = app.isWorkProfile,
                    modifier = Modifier.rowGestures(app, onTap = { onLaunch(app) }, onLongPress = { onLongPress(app) }),
                )
            }
            if (showSettingsEntry) {
                item {
                    ResultRow(
                        AnnotatedString("Mono settings"),
                        fontScale,
                        leading = { LineIcon(Icons.LongPress, 24.dp, c.onBackground) },
                        modifier = Modifier.rowGestures("settings", onOpenSettings, onOpenSettings),
                    )
                }
            }
            if (query.isNotBlank()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { webSearch() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIcon(Icons.Globe, 22.dp, c.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Search the web for “$query”", fontSize = 15.sp, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SearchField(
            query = query,
            onQuery = onQuery,
            onGo = { if (!onLaunchTop() && query.isNotBlank()) webSearch() },
            modifier = Modifier.focusRequester(focus),
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onGo: () -> Unit, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(c.surfaceContainer)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Icons.Search, 20.dp, c.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search apps", fontSize = 18.sp, color = c.onSurfaceVariant)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = c.onBackground),
                cursorBrush = SolidColor(c.onBackground),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = modifier.fillMaxWidth().semantics { contentDescription = "Search apps" },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Clear search") { onQuery("") },
                contentAlignment = Alignment.Center,
            ) { LineIcon(Icons.Close, 18.dp, c.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ResultRow(
    label: AnnotatedString,
    fontScale: Float,
    modifier: Modifier = Modifier,
    best: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    work: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (best) c.onBackground.copy(alpha = 0.08f) else c.background.copy(alpha = 0f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(16.dp))
        }
        Text(
            label,
            fontSize = (20 * fontScale).sp,
            color = c.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = !work && best),
        )
        if (work) {
            Spacer(Modifier.width(16.dp))
            WorkTag()
            if (best) Spacer(Modifier.weight(1f))
        }
        if (best) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Go", fontSize = 12.sp, color = c.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                LineIcon(Icons.ArrowRight, 14.dp, c.onSurfaceVariant)
            }
        }
    }
}

/** Bold the typed prefix when the name starts with it. */
private fun highlightMatch(label: String, query: String): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty() || !label.startsWith(q, ignoreCase = true)) return AnnotatedString(label)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(label.substring(0, q.length)) }
        append(label.substring(q.length))
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp))
}
