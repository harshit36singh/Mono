package com.mono.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.LetterGroup
import com.mono.launcher.data.RailSide
import com.mono.launcher.ui.WORK_GROUP
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.common.rowGestures
import kotlin.math.roundToInt

private val RailWidth = 36.dp
private val PopoutWidth = 232.dp
private val PopoutGap = 12.dp
private val MaxItemHeight = 48.dp
private val MinItemHeight = 34.dp

/**
 * A–Z rail on the screen edge plus the popout list that opens beside the thumb.
 *
 * Drag on the rail -> letter under the thumb is selected (haptic tick, magnified),
 * its apps pop out next to the thumb. Slide sideways into the popout and release on an app
 * to launch it; release on empty space to cancel. Releasing on the rail itself pins the
 * popout open so it can be tapped (also the TalkBack path).
 *
 * Fills its parent; only the rail strip and the open popout take touches.
 */
@Composable
fun AlphabetRail(
    groups: List<LetterGroup>,
    side: RailSide,
    iconStyle: IconStyle,
    fontScale: Float,
    haptics: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    // RTL mirrors the rail: "RIGHT" means the end edge.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val onRight = (side == RailSide.RIGHT) != rtl

    var activeIndex by remember { mutableIntStateOf(-1) }
    var hovered by remember { mutableIntStateOf(-1) }
    var pinned by remember { mutableStateOf(false) }
    var thumbY by remember { mutableFloatStateOf(0f) }
    var inPopout by remember { mutableStateOf(false) }

    var overlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var railCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val currentGroups by rememberUpdatedState(groups)
    val currentLaunch by rememberUpdatedState(onLaunch)

    fun tick() {
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun close() {
        activeIndex = -1
        hovered = -1
        pinned = false
        inPopout = false
    }

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { overlayCoords = it }) {
        val overlayH = with(density) { maxHeight.toPx() }
        val overlayW = with(density) { maxWidth.toPx() }
        val maxPopH = overlayH * 0.8f

        val active = groups.getOrNull(activeIndex)
        val items = active?.apps.orEmpty()
        val itemHPx = with(density) {
            if (items.isEmpty()) MaxItemHeight.toPx()
            else (maxPopH / items.size).coerceIn(MinItemHeight.toPx(), MaxItemHeight.toPx())
        }
        // While dragging only what fits is reachable; pinned mode scrolls.
        val dragVisible = (maxPopH / itemHPx).toInt().coerceAtLeast(1)
        val shownCount = if (pinned) items.size else minOf(items.size, dragVisible)
        val popH = (shownCount * itemHPx).coerceAtMost(maxPopH)
        val popTop = (thumbY - popH / 2f).coerceIn(0f, (overlayH - popH).coerceAtLeast(0f))
        val railPx = with(density) { RailWidth.toPx() }
        val popWPx = with(density) { PopoutWidth.toPx() }.coerceAtMost(overlayW - railPx - with(density) { PopoutGap.toPx() } * 2)
        val gapPx = with(density) { PopoutGap.toPx() }
        // Popout x in absolute (left-origin) overlay coordinates.
        val popLeft = if (onRight) overlayW - railPx - gapPx - popWPx else railPx + gapPx

        // Tap-outside scrim for the pinned popout.
        if (pinned) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            )
        }

        // --- Popout -------------------------------------------------------------------
        AnimatedVisibility(
            visible = active != null && items.isNotEmpty(),
            enter = fadeIn(tween(90)) + scaleIn(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(if (onRight) 1f else 0f, 0.5f),
            ),
            exit = fadeOut(tween(90)) + scaleOut(tween(120), targetScale = 0.9f),
            modifier = Modifier.offset { IntOffset(popLeftForLayout(popLeft, popWPx, overlayW, rtl).roundToInt(), popTop.roundToInt()) },
        ) {
            Popout(
                items = items,
                shownCount = shownCount,
                itemHeight = with(density) { itemHPx.toDp() },
                width = with(density) { popWPx.toDp() },
                hovered = hovered,
                pinned = pinned,
                iconStyle = iconStyle,
                fontScale = fontScale,
                onTap = { app -> close(); onLaunch(app) },
                onLongPress = { app -> close(); onLongPress(app) },
            )
        }

        // --- Rail ---------------------------------------------------------------------
        Column(
            Modifier
                .align(if (side == RailSide.RIGHT) Alignment.CenterEnd else Alignment.CenterStart)
                .width(RailWidth)
                .fillMaxHeight()
                .onGloballyPositioned { railCoords = it }
                .pointerInput(onRight) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        pinned = false
                        inPopout = false
                        hovered = -1
                        val overlay = overlayCoords
                        val rail = railCoords
                        fun toOverlay(local: Offset): Offset =
                            if (overlay != null && rail != null) overlay.localPositionOf(rail, local) else local

                        fun letterAt(localY: Float): Int {
                            val gs = currentGroups
                            val slot = size.height.toFloat() / gs.size
                            val raw = (localY / slot).toInt().coerceIn(0, gs.lastIndex)
                            if (!gs[raw].isEmpty) return raw
                            // Empty letters are skipped: snap to the nearest letter with apps.
                            var best = -1
                            for (d in 1..gs.size) {
                                val up = raw - d
                                val dn = raw + d
                                if (dn <= gs.lastIndex && !gs[dn].isEmpty) { best = dn; break }
                                if (up >= 0 && !gs[up].isEmpty) { best = up; break }
                            }
                            return best
                        }

                        fun update(local: Offset) {
                            val p = toOverlay(local)
                            val railEdge = if (onRight) overlayW - railPx else railPx
                            val intoPopout = if (onRight) p.x < railEdge - gapPx / 2 else p.x > railEdge + gapPx / 2
                            if (!intoPopout) {
                                inPopout = false
                                if (hovered != -1) hovered = -1
                                val idx = letterAt(local.y)
                                thumbY = p.y
                                if (idx != activeIndex && idx >= 0) {
                                    activeIndex = idx
                                    tick()
                                }
                            } else {
                                inPopout = true
                                val n = currentGroups.getOrNull(activeIndex)?.apps?.size ?: 0
                                val visible = minOf(n, dragVisible)
                                val h = if (n == 0) itemHPx else (maxPopH / n).coerceIn(with(density) { MinItemHeight.toPx() }, with(density) { MaxItemHeight.toPx() })
                                val ph = visible * h
                                val top = (thumbY - ph / 2f).coerceIn(0f, (overlayH - ph).coerceAtLeast(0f))
                                val i = ((p.y - top) / h).toInt()
                                val newHover = if (p.y >= top && i in 0 until visible) i else -1
                                if (newHover != hovered) {
                                    hovered = newHover
                                    if (newHover >= 0) tick()
                                }
                            }
                        }

                        update(down.position)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (change.changedToUp()) break
                            update(change.position)
                        }

                        val group = currentGroups.getOrNull(activeIndex)
                        val target = group?.apps?.getOrNull(hovered)
                        when {
                            target != null -> {
                                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                close()
                                currentLaunch(target)
                            }
                            !inPopout && group != null -> pinned = true
                            else -> close()
                        }
                    }
                }
                .padding(vertical = 4.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            groups.forEachIndexed { i, g ->
                RailLetter(
                    group = g,
                    active = i == activeIndex,
                    onRight = onRight,
                    modifier = Modifier.weight(1f),
                    onAccessibilityOpen = {
                        activeIndex = i
                        pinned = true
                    },
                )
            }
        }
    }
}

/** offset{} is applied in layout-direction-aware coordinates; convert from absolute x. */
private fun popLeftForLayout(absLeft: Float, width: Float, overlayW: Float, rtl: Boolean): Float =
    if (rtl) overlayW - absLeft - width else absLeft

@Composable
private fun RailLetter(
    group: LetterGroup,
    active: Boolean,
    onRight: Boolean,
    modifier: Modifier,
    onAccessibilityOpen: () -> Unit,
) {
    val scale by animateFloatAsState(
        if (active) 2.2f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "letterScale",
    )
    val shift = with(LocalDensity.current) { 28.dp.toPx() } * (scale - 1f) / 1.2f
    val label = if (group.letter == WORK_GROUP) "Work apps" else "Apps starting with ${group.letter}"
    Box(
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (group.isEmpty) "$label, none" else "$label, ${group.apps.size}"
                if (!group.isEmpty) onClick { onAccessibilityOpen(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = group.letter,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (group.isEmpty) 0.28f else 0.85f),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = if (onRight) -shift else shift
            },
        )
    }
}

@Composable
private fun Popout(
    items: List<AppEntry>,
    shownCount: Int,
    itemHeight: Dp,
    width: Dp,
    hovered: Int,
    pinned: Boolean,
    iconStyle: IconStyle,
    fontScale: Float,
    onTap: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = Modifier.width(width),
    ) {
        if (pinned) {
            LazyColumn(Modifier.height(itemHeight * shownCount.coerceAtMost(14))) {
                items(items, key = { "${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                    PopoutRow(app, false, itemHeight, iconStyle, fontScale, Modifier.rowGestures(app, { onTap(app) }, { onLongPress(app) }))
                }
            }
        } else {
            Column {
                items.take(shownCount).forEachIndexed { i, app ->
                    PopoutRow(app, i == hovered, itemHeight, iconStyle, fontScale, Modifier)
                }
            }
        }
    }
}

@Composable
private fun PopoutRow(
    app: AppEntry,
    highlighted: Boolean,
    height: Dp,
    iconStyle: IconStyle,
    fontScale: Float,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(if (highlighted) colors.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconStyle != IconStyle.NONE) {
            AppIcon(app, iconStyle, (height * 0.55f).coerceAtMost(26.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            app.displayLabel,
            fontSize = (16 * fontScale).sp * (if (highlighted) 1.06f else 1f),
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (app.isWorkProfile) WorkBadge()
    }
}
