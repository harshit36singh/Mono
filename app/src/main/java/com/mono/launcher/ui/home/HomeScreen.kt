package com.mono.launcher.ui.home

import android.graphics.Rect
import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.AppKey
import com.mono.launcher.data.GestureAction
import com.mono.launcher.data.RailSide
import com.mono.launcher.system.SystemActions
import com.mono.launcher.ui.HomeUiState
import com.mono.launcher.ui.common.rowGestures
import com.mono.launcher.ui.theme.LocalIsDark
import kotlin.math.abs

/** Callbacks Home needs from the view model / activity. */
class HomeActions(
    val launch: (AppEntry, Rect?, Bundle?) -> Unit,
    val showActions: (AppEntry) -> Unit,
    val openSearch: () -> Unit,
    val openSettings: () -> Unit,
    val openNotification: (AppEntry) -> Unit,
    val dismissNotification: (String) -> Unit,
    val reorderFavorites: (List<AppKey>) -> Unit,
    val mediaPrevious: () -> Unit,
    val mediaPlayPause: () -> Unit,
    val mediaNext: () -> Unit,
)

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, resetTick: Int) {
    val context = LocalContext.current
    val view = LocalView.current
    // Apps open with a clip-reveal from the row that was touched.
    val launch: (AppEntry, Rect?) -> Unit = { app, r -> actions.launch(app, r, revealOptions(view, r)) }
    var indexActive by remember { mutableStateOf(false) }
    val homeAlpha by animateFloatAsState(if (indexActive) 0f else 1f, tween(if (indexActive) 120 else 200), label = "homeAlpha")
    val haptic = LocalHapticFeedback.current
    val settings = state.settings
    val dark = LocalIsDark.current
    val currentSettings by rememberUpdatedState(settings)

    fun perform(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.NOTIFICATION_SHADE -> SystemActions.expandNotifications(context)
            GestureAction.LOCK_SCREEN -> SystemActions.lockScreen(context)
            GestureAction.SEARCH -> actions.openSearch()
            GestureAction.OPEN_SETTINGS -> actions.openSettings()
        }
    }

    val scrim = if (dark) Color.Black else Color.White
    val railOnRight = settings.railSide == RailSide.RIGHT

    Box(
        Modifier
            .fillMaxSize()
            // Gentle scrim so text stays readable (≥ 4.5:1) on any wallpaper.
            .background(Brush.verticalGradient(listOf(scrim.copy(alpha = 0.25f), scrim.copy(alpha = 0.55f))))
            .pointerInput(Unit) {
                // Swipes anywhere on Home (rows leave unconsumed movement for us).
                val threshold = 56.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var last = down.position
                    var stolen = false
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed && !change.changedToUp()) stolen = true
                        last = change.position
                        if (change.changedToUp()) break
                    }
                    if (stolen) return@awaitEachGesture
                    val d = last - down.position
                    when {
                        abs(d.y) > abs(d.x) && d.y < -threshold -> actions.openSearch()
                        abs(d.y) > abs(d.x) && d.y > threshold -> perform(currentSettings.swipeDown)
                        abs(d.x) > abs(d.y) && d.x > threshold -> perform(currentSettings.swipeRight)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { perform(currentSettings.doubleTap) },
                    onLongPress = {
                        if (currentSettings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actions.openSettings()
                    },
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha }
                .padding(
                    start = if (railOnRight) 28.dp else 56.dp,
                    end = if (railOnRight) 56.dp else 28.dp,
                    top = 40.dp,
                    bottom = 16.dp,
                ),
        ) {
            ClockHeader(settings.fontScale)
            // Top third stays empty by design: everything interactive sits within thumb reach.
            Spacer(Modifier.weight(1f))
            state.media?.let {
                MediaCard(it, actions.mediaPrevious, actions.mediaPlayPause, actions.mediaNext, Modifier.padding(bottom = 20.dp))
            }
            Favorites(state, actions, launch)
            Spacer(Modifier.height(SearchButtonSize + 12.dp))
        }

        SearchButton(
            onClick = actions.openSearch,
            modifier = Modifier
                .align(if (railOnRight) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )

        AppIndex(
            groups = state.apps.groups,
            style = settings.railStyle,
            side = settings.railSide,
            iconStyle = settings.iconStyle,
            fontScale = settings.fontScale,
            haptics = settings.haptics,
            resetTick = resetTick,
            bottomInset = SearchButtonSize + 24.dp,
            onActiveChange = { indexActive = it },
            onLaunch = launch,
            onLongPress = actions.showActions,
        )
    }
}

/** Up to 8 favorites. Long-press + drag reorders; long-press in place opens app actions. */
@Composable
private fun Favorites(state: HomeUiState, actions: HomeActions, launch: (AppEntry, Rect?) -> Unit) {
    val settings = state.settings
    val haptic = LocalHapticFeedback.current
    val favorites = state.apps.favorites

    if (favorites.isEmpty()) {
        Text(
            "Long-press any app in the A–Z rail or search to pin it here.",
            fontSize = (15 * settings.fontScale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }

    // Local order while dragging; re-synced from the database otherwise.
    val order = remember { mutableStateListOf<AppKey>() }
    var dragging by remember { mutableStateOf<AppKey?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<AppKey, Int>() }
    val bounds = remember { HashMap<AppKey, Rect>() }

    if (dragging == null) {
        val keys = favorites.map { it.key }
        if (order.toList() != keys) {
            order.clear()
            order.addAll(keys)
        }
    }
    val byKey = favorites.associateBy { it.key }

    fun hapticTick() {
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(Modifier.fillMaxWidth()) {
        order.forEach { key ->
            val app = byKey[key] ?: return@forEach
            val isDragged = dragging == key
            AppRow(
                app = app,
                notification = state.notifications[app.packageName],
                iconStyle = settings.iconStyle,
                fontScale = settings.fontScale,
                onOpenNotification = { actions.openNotification(app) },
                onDismissNotification = { actions.dismissNotification(app.packageName) },
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffset else 0f
                        scaleX = if (isDragged) 1.03f else 1f
                        scaleY = if (isDragged) 1.03f else 1f
                    }
                    .onSizeChanged { heights[key] = it.height }
                    .onGloballyPositioned { c ->
                        val r = c.boundsInWindow()
                        bounds[key] = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                    }
                    .rowGestures(
                        key = key,
                        onTap = {
                            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            launch(app, bounds[key])
                        },
                        onLongPress = { actions.showActions(app) },
                        onLongPressStart = {
                            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStart = {
                            dragging = key
                            dragOffset = 0f
                        },
                        onDrag = { dy ->
                            dragOffset += dy
                            val idx = order.indexOf(key)
                            // Swap with neighbours once dragged past half their height.
                            if (dragOffset > 0 && idx < order.lastIndex) {
                                val next = heights[order[idx + 1]] ?: 0
                                if (dragOffset > next / 2f) {
                                    order.removeAt(idx); order.add(idx + 1, key)
                                    dragOffset -= next
                                    hapticTick()
                                }
                            } else if (dragOffset < 0 && idx > 0) {
                                val prev = heights[order[idx - 1]] ?: 0
                                if (-dragOffset > prev / 2f) {
                                    order.removeAt(idx); order.add(idx - 1, key)
                                    dragOffset += prev
                                    hapticTick()
                                }
                            }
                        },
                        onDragEnd = {
                            actions.reorderFavorites(order.toList())
                            dragging = null
                            dragOffset = 0f
                        },
                    ),
            )
        }
    }
}

private val SearchButtonSize = 52.dp

/** Minimal round search button; the glyph is drawn so no icon library is needed. */
@Composable
private fun SearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glyph = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        modifier = modifier
            .size(SearchButtonSize)
            .semantics { contentDescription = "Search apps" },
    ) {
        Canvas(Modifier.padding(16.dp)) {
            val stroke = 2.dp.toPx()
            val r = size.minDimension * 0.36f
            val c = Offset(size.width * 0.44f, size.height * 0.44f)
            drawCircle(glyph, radius = r, center = c, style = Stroke(stroke))
            val start = c + Offset(r * 0.72f, r * 0.72f)
            drawLine(glyph, start, Offset(size.width * 0.92f, size.height * 0.92f), stroke, StrokeCap.Round)
        }
    }
}

private fun revealOptions(view: View, r: Rect?): Bundle? = r?.let {
    ActivityOptions.makeClipRevealAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
}
