package com.mono.launcher.ui.home

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.mono.launcher.data.RailStyle
import com.mono.launcher.ui.FAVORITES_GROUP
import com.mono.launcher.ui.WORK_GROUP
import com.mono.launcher.ui.common.AppIcon
import com.mono.launcher.ui.common.rememberHaptics
import com.mono.launcher.ui.common.rowGestures
import com.mono.launcher.ui.theme.LocalIsDark
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt

private val RailWidth = 30.dp
private val EdgeInset = 14.dp
private val HeaderHeight = 44.dp
private val MaxItemHeight = 46.dp
private val MinItemHeight = 34.dp
private val PopoutWidth = 232.dp
private val Gap = 12.dp
private val PanelInset = 20.dp
private val WavePanelWidth = 228.dp
private val WaveBase = 92.dp
private val BubbleSize = 52.dp
private val TouchRing = 56.dp

/** Pixel geometry shared between composition and the pointer handler (not observed). */
private class Geometry {
    var overlayW = 0f
    var overlayH = 0f
    var railLeft = 0f
    var railTop = 0f
    var railW = 0f
    var railH = 0f
    var panelLeft = 0f
    var panelTop = 0f
    var panelW = 0f
    var headerH = 0f
    var itemH = 0f
    var fit = 0
    var waveBase = 0f
    var waveMax = 0f
    var gap = 0f
    val railCenterX get() = railLeft + railW / 2f
}

/**
 * The A–Z rail and everything it opens, in one of three [RailStyle]s:
 *
 * - WAVE: letters bend away from the thumb in a smooth curve, a bubble shows the letter,
 *   and that letter's apps appear on the open side of the screen. Slide sideways onto an
 *   app and release to open it.
 * - POPOUT: the letter magnifies and a compact card of its apps opens beside the thumb.
 * - LIST: a full, scrollable A–Z list; dragging the rail jumps it to the letter.
 *
 * In WAVE/POPOUT, releasing on the rail keeps the apps open for tapping (also the TalkBack
 * path). During a drag only draw/layout phases update, so the rail stays at 60 fps.
 */
@Composable
fun AppIndex(
    groups: List<LetterGroup>,
    style: RailStyle,
    side: RailSide,
    iconStyle: IconStyle,
    fontScale: Float,
    haptics: Boolean,
    resetTick: Int,
    bottomInset: Dp,
    onActiveChange: (Boolean) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) return
    val haptic = rememberHaptics()
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // RTL mirrors the rail: RIGHT means the end edge.
    val onRight = (side == RailSide.RIGHT) != rtl
    val scope = rememberCoroutineScope()
    val geo = remember { Geometry() }
    val coords = remember { arrayOfNulls<LayoutCoordinates>(2) } // [overlay, rail]

    var touching by remember { mutableStateOf(false) }
    val wave = remember { mutableStateOf(Offset.Zero) } // last rail-zone touch; drives the curve
    val finger = remember { mutableStateOf(Offset.Zero) } // live finger position, for the touch ring
    var inList by remember { mutableStateOf(false) }
    var activeIndex by remember { mutableIntStateOf(-1) }
    var lastIndex by remember { mutableIntStateOf(-1) }
    var hovered by remember { mutableIntStateOf(-1) }
    var pinned by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var popAnchorY by remember { mutableFloatStateOf(0f) }

    val strength: State<Float> = animateFloatAsState(
        targetValue = if (touching && style == RailStyle.WAVE) 1f else 0f,
        // Out: soft and springy. Back: a little quicker so the rail settles promptly.
        animationSpec = spring(
            dampingRatio = if (touching) 0.72f else 0.85f,
            stiffness = if (touching) Spring.StiffnessMediumLow else Spring.StiffnessMedium,
        ),
        label = "wave",
    )

    val listState = rememberLazyListState()
    // Lazy-list layout for LIST mode: header index per group, and group per lazy index.
    val drawerIndex = remember(groups) {
        val headerAt = IntArray(groups.size) { -1 }
        val groupAt = ArrayList<Int>()
        groups.forEachIndexed { gi, g ->
            if (g.apps.isNotEmpty()) {
                headerAt[gi] = groupAt.size
                repeat(g.apps.size + 1) { groupAt += gi }
            }
        }
        headerAt to groupAt
    }
    val currentGroups by rememberUpdatedState(groups)
    val currentDrawerIndex by rememberUpdatedState(drawerIndex)
    val currentLaunch by rememberUpdatedState(onLaunch)

    fun tick() {
        if (haptics) haptic.tick()
    }

    fun close() {
        touching = false
        activeIndex = -1
        hovered = -1
        pinned = false
        drawerOpen = false
        inList = false
    }

    LaunchedEffect(resetTick, style) { close() }
    val active = touching || pinned || drawerOpen
    LaunchedEffect(active, style) { onActiveChange(active && style != RailStyle.POPOUT) }
    BackHandler(enabled = pinned || drawerOpen) { close() }

    val scrolledGroup by remember {
        derivedStateOf { currentDrawerIndex.second.getOrNull(listState.firstVisibleItemIndex) ?: -1 }
    }
    val highlight = when {
        activeIndex >= 0 -> activeIndex
        style == RailStyle.LIST && drawerOpen -> scrolledGroup
        else -> -1
    }

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { coords[0] = it }) {
        val overlayW = constraints.maxWidth.toFloat()
        val overlayH = constraints.maxHeight.toFloat()
        val px = { d: Dp -> with(density) { d.toPx() } }

        val shownIndex = if (activeIndex >= 0) activeIndex else lastIndex
        val items = groups.getOrNull(shownIndex)?.apps.orEmpty()
        val headerPx = if (style == RailStyle.WAVE) px(HeaderHeight) else 0f
        val maxListH = overlayH * (if (style == RailStyle.WAVE) 0.62f else 0.78f) - headerPx
        val itemPx = if (items.isEmpty()) px(MaxItemHeight) else (maxListH / items.size).coerceIn(px(MinItemHeight), px(MaxItemHeight))
        val fit = minOf(items.size, floor(maxListH / itemPx).toInt().coerceAtLeast(1))
        val panelH = headerPx + fit * itemPx
        val railW = px(RailWidth)
        val panelW: Float
        val panelLeft: Float
        val panelTop: Float
        if (style == RailStyle.WAVE) {
            panelW = px(WavePanelWidth).coerceAtMost(overlayW * 0.62f)
            panelLeft = if (onRight) px(PanelInset) else overlayW - px(PanelInset) - panelW
            panelTop = (overlayH * 0.46f - panelH / 2f).coerceIn(0f, (overlayH - panelH).coerceAtLeast(0f))
        } else {
            panelW = px(PopoutWidth).coerceAtMost(overlayW - railW - px(Gap) * 2)
            panelLeft = if (onRight) overlayW - railW - px(Gap) - panelW else railW + px(Gap)
            panelTop = (popAnchorY - panelH / 2f).coerceIn(0f, (overlayH - panelH).coerceAtLeast(0f))
        }
        SideEffect {
            geo.overlayW = overlayW
            geo.overlayH = overlayH
            geo.panelLeft = panelLeft
            geo.panelTop = panelTop
            geo.panelW = panelW
            geo.headerH = headerPx
            geo.itemH = itemPx
            geo.fit = fit
            geo.gap = px(Gap)
            geo.waveBase = px(WaveBase)
            // Keep the curve clear of the app list.
            geo.waveMax = ((if (onRight) geo.railCenterX - (panelLeft + panelW) else panelLeft - geo.railCenterX) - px(Gap))
                .coerceAtLeast(geo.waveBase)
        }

        // Tap-outside to close the pinned list.
        if (pinned) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            )
        }

        // --- LIST: full scrollable A–Z ----------------------------------------------------
        if (style == RailStyle.LIST) {
            AnimatedVisibility(
                visible = drawerOpen,
                enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 24 },
                exit = fadeOut(tween(120)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = if (onRight) PanelInset else RailWidth + 20.dp,
                            end = if (onRight) RailWidth + 20.dp else PanelInset,
                            top = 24.dp,
                            bottom = 120.dp,
                        ),
                    ) {
                        groups.forEach { g ->
                            if (g.apps.isEmpty()) return@forEach
                            item(key = "h/${g.letter}") { GroupHeader(g.letter, Modifier.height(HeaderHeight)) }
                            items(g.apps, key = { "${g.letter}/${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                                var bounds by remember { mutableStateOf<Rect?>(null) }
                                AppListRow(
                                    app = app,
                                    height = MaxItemHeight,
                                    highlighted = false,
                                    iconStyle = iconStyle,
                                    fontScale = fontScale,
                                    modifier = Modifier
                                        .onGloballyPositioned { bounds = it.boundsInWindow().toRect() }
                                        .rowGestures(app, onTap = { onLaunch(app, bounds) }, onLongPress = { close(); onLongPress(app) }),
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- WAVE / POPOUT: the letter's apps ---------------------------------------------
        if (style != RailStyle.LIST) {
            AnimatedVisibility(
                visible = (touching || pinned) && activeIndex >= 0 && items.isNotEmpty(),
                enter = fadeIn(tween(110)) + scaleIn(
                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                    initialScale = if (style == RailStyle.WAVE) 0.96f else 0.86f,
                    transformOrigin = TransformOrigin(if (onRight == (style == RailStyle.POPOUT)) 1f else 0f, 0.5f),
                ),
                exit = fadeOut(tween(110)) + scaleOut(tween(120), targetScale = 0.96f),
                modifier = Modifier.offset {
                    IntOffset((if (rtl) overlayW - panelLeft - panelW else panelLeft).roundToInt(), panelTop.roundToInt())
                },
            ) {
                AnimatedContent(
                    targetState = shownIndex,
                    transitionSpec = {
                        (fadeIn(tween(120)) + slideInVertically(tween(180)) { it / 16 }) togetherWith fadeOut(tween(70))
                    },
                    label = "letter",
                ) { idx ->
                    val group = groups.getOrNull(idx) ?: return@AnimatedContent
                    LetterPanel(
                        group = group,
                        style = style,
                        width = with(density) { panelW.toDp() },
                        itemHeight = with(density) { itemPx.toDp() },
                        fit = fit,
                        hovered = if (idx == activeIndex) hovered else -1,
                        pinned = pinned,
                        iconStyle = iconStyle,
                        fontScale = fontScale,
                        onTap = { app, rect -> close(); onLaunch(app, rect) },
                        onLongPress = { app -> close(); onLongPress(app) },
                    )
                }
            }
        }

        // --- WAVE: letter bubble beside the thumb -----------------------------------------
        if (style == RailStyle.WAVE && shownIndex >= 0) {
            val bubblePx = px(BubbleSize)
            Box(
                Modifier
                    .offset {
                        val w = wave.value
                        val amp = (geo.waveBase + pullOf(w, geo, onRight)).coerceAtMost(geo.waveMax)
                        val cx = if (onRight) geo.railCenterX - amp else geo.railCenterX + amp
                        val absX = cx - bubblePx / 2f
                        IntOffset((if (rtl) overlayW - absX - bubblePx else absX).roundToInt(), (w.y - bubblePx / 2f).roundToInt())
                    }
                    .graphicsLayer {
                        val s = strength.value
                        alpha = s * (if (inList) 0.35f else 1f)
                        scaleX = 0.6f + 0.4f * s
                        scaleY = 0.6f + 0.4f * s
                    }
                    .size(BubbleSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    groups.getOrNull(shownIndex)?.letter.orEmpty(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // --- WAVE: soft ring under the finger ---------------------------------------------
        if (style == RailStyle.WAVE) {
            val ringPx = px(TouchRing)
            val ringColor = MaterialTheme.colorScheme.onBackground
            Box(
                Modifier
                    .offset {
                        val f = finger.value
                        val absX = f.x - ringPx / 2f
                        IntOffset((if (rtl) overlayW - absX - ringPx else absX).roundToInt(), (f.y - ringPx / 2f).roundToInt())
                    }
                    .graphicsLayer { alpha = strength.value }
                    .size(TouchRing)
                    .background(ringColor.copy(alpha = 0.18f), CircleShape)
                    .border(1.5.dp, ringColor.copy(alpha = 0.4f), CircleShape),
            )
        }

        // --- The rail ----------------------------------------------------------------------
        Column(
            Modifier
                .align(if (side == RailSide.RIGHT) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(bottom = bottomInset)
                // Stay clear of the system back-gesture strip on the screen edge.
                .padding(horizontal = EdgeInset)
                .width(RailWidth)
                .fillMaxHeight(0.6f)
                .onGloballyPositioned { c ->
                    coords[1] = c
                    val overlay = coords[0] ?: return@onGloballyPositioned
                    val pos = overlay.localPositionOf(c, Offset.Zero)
                    geo.railLeft = pos.x
                    geo.railTop = pos.y
                    geo.railW = c.size.width.toFloat()
                    geo.railH = c.size.height.toFloat()
                }
                // Android honours up to 200dp per edge; covers the lower part of the rail.
                .systemGestureExclusion()
                .pointerInput(style, onRight) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        pinned = false
                        hovered = -1
                        inList = false
                        if (style == RailStyle.LIST) drawerOpen = true
                        touching = true

                        fun toOverlay(local: Offset): Offset {
                            val o = coords[0]
                            val r = coords[1]
                            return if (o != null && r != null) o.localPositionOf(r, local) else local
                        }

                        fun letterAt(localY: Float): Int {
                            val gs = currentGroups
                            val raw = (localY / (size.height.toFloat() / gs.size)).toInt().coerceIn(0, gs.lastIndex)
                            if (!gs[raw].isEmpty) return raw
                            // Letters without apps are skipped: snap to the nearest one with apps.
                            for (d in 1..gs.size) {
                                if (raw + d <= gs.lastIndex && !gs[raw + d].isEmpty) return raw + d
                                if (raw - d >= 0 && !gs[raw - d].isEmpty) return raw - d
                            }
                            return -1
                        }

                        fun update(local: Offset) {
                            val p = toOverlay(local)
                            finger.value = p
                            val listZone = activeIndex >= 0 && when (style) {
                                RailStyle.LIST -> false
                                RailStyle.WAVE -> if (onRight) p.x < geo.panelLeft + geo.panelW else p.x > geo.panelLeft
                                RailStyle.POPOUT -> if (onRight) p.x < geo.railLeft - geo.gap / 2 else p.x > geo.railLeft + geo.railW + geo.gap / 2
                            }
                            if (!listZone) {
                                inList = false
                                if (hovered != -1) hovered = -1
                                wave.value = p
                                val idx = letterAt(local.y)
                                if (idx >= 0 && idx != activeIndex) {
                                    activeIndex = idx
                                    lastIndex = idx
                                    popAnchorY = p.y
                                    tick()
                                    if (style == RailStyle.LIST) {
                                        val target = currentDrawerIndex.first.getOrNull(idx) ?: -1
                                        if (target >= 0) scope.launch { listState.scrollToItem(target) }
                                    }
                                }
                            } else {
                                inList = true
                                val top = geo.panelTop + geo.headerH
                                val i = floor((p.y - top) / geo.itemH).toInt()
                                val h = if (p.y >= top && i in 0 until geo.fit) i else -1
                                if (h != hovered) {
                                    hovered = h
                                    if (h >= 0) tick()
                                }
                            }
                        }

                        var released = false
                        try {
                            update(down.position)
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (change.changedToUp() || !change.pressed) break
                                update(change.position)
                            }
                            released = true
                        } finally {
                            // Always let the letters spring back, even when the system cancels the touch
                            // (edge back-gesture, notification shade, window focus loss).
                            touching = false
                            if (!released) {
                                hovered = -1
                                inList = false
                                if (style != RailStyle.LIST) activeIndex = -1
                            }
                        }

                        val group = currentGroups.getOrNull(activeIndex)
                        val target = group?.apps?.getOrNull(hovered)
                        when {
                            target != null -> {
                                if (haptics) haptic.click()
                                val rect = coords[0]?.let { o ->
                                    val tl = o.localToWindow(Offset(geo.panelLeft, geo.panelTop + geo.headerH + hovered * geo.itemH))
                                    Rect(tl.x.toInt(), tl.y.toInt(), (tl.x + geo.panelW).toInt(), (tl.y + geo.itemH).toInt())
                                }
                                close()
                                currentLaunch(target, rect)
                            }
                            style == RailStyle.LIST -> activeIndex = -1 // list stays open
                            !inList && group != null && !group.isEmpty -> pinned = true
                            else -> close()
                        }
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val n = groups.size
            groups.forEachIndexed { i, g ->
                RailLetter(
                    group = g,
                    index = i,
                    count = n,
                    style = style,
                    active = i == highlight,
                    onRight = onRight,
                    geo = geo,
                    wave = wave,
                    strength = strength,
                    modifier = Modifier.weight(1f),
                    onAccessibilityOpen = {
                        activeIndex = i
                        lastIndex = i
                        if (style == RailStyle.LIST) {
                            drawerOpen = true
                            val target = drawerIndex.first.getOrNull(i) ?: -1
                            if (target >= 0) scope.launch { listState.scrollToItem(target) }
                        } else {
                            pinned = true
                        }
                    },
                )
            }
        }
    }
}

/** How far the thumb has moved inward from the rail; the curve follows it. */
@Suppress("UNUSED_PARAMETER")
private fun pullOf(w: Offset, geo: Geometry, onRight: Boolean): Float = 0f

@Composable
private fun RailLetter(
    group: LetterGroup,
    index: Int,
    count: Int,
    style: RailStyle,
    active: Boolean,
    onRight: Boolean,
    geo: Geometry,
    wave: State<Offset>,
    strength: State<Float>,
    modifier: Modifier,
    onAccessibilityOpen: () -> Unit,
) {
    val magnify by animateFloatAsState(
        targetValue = when {
            !active -> 1f
            style == RailStyle.POPOUT -> 2.2f
            style == RailStyle.LIST -> 1.5f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "magnify",
    )
    val shiftPx = with(LocalDensity.current) { 26.dp.toPx() }
    val label = when (group.letter) {
        WORK_GROUP -> "Work apps"
        FAVORITES_GROUP -> "Favorites"
        else -> "Apps starting with ${group.letter}"
    }
    val color = MaterialTheme.colorScheme.onBackground
    val shadowColor = if (LocalIsDark.current) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
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
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = color.copy(alpha = if (group.isEmpty) 0.28f else if (active) 1f else 0.85f),
            // Soft shadow keeps letters legible over bright wallpapers.
            style = TextStyle(shadow = Shadow(color = shadowColor, blurRadius = 8f)),
            modifier = Modifier.graphicsLayer {
                if (style == RailStyle.WAVE) {
                    // Read in the draw phase only: dragging never recomposes the rail.
                    val s = strength.value
                    if (s > 0.001f && geo.railH > 0f) {
                        val w = wave.value
                        val cy = geo.railTop + (index + 0.5f) * geo.railH / count
                        val dy = cy - w.y
                        val sigma = 2.4f * geo.railH / count
                        val g = exp(-(dy * dy) / (2f * sigma * sigma))
                        val amp = (geo.waveBase + pullOf(w, geo, onRight)).coerceAtMost(geo.waveMax)
                        translationX = (if (onRight) -1f else 1f) * amp * g * s
                        val sc = 1f + 0.5f * g * s
                        scaleX = sc
                        scaleY = sc
                    } else {
                        translationX = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                } else {
                    scaleX = magnify
                    scaleY = magnify
                    val shift = shiftPx * (magnify - 1f) / 1.2f
                    translationX = if (onRight) -shift else shift
                }
            },
        )
    }
}

@Composable
private fun LetterPanel(
    group: LetterGroup,
    style: RailStyle,
    width: Dp,
    itemHeight: Dp,
    fit: Int,
    hovered: Int,
    pinned: Boolean,
    iconStyle: IconStyle,
    fontScale: Float,
    onTap: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val content: @Composable () -> Unit = {
        Column(Modifier.width(width)) {
            if (style == RailStyle.WAVE) GroupHeader(group.letter, Modifier.height(HeaderHeight))
            if (pinned) {
                LazyColumn(Modifier.height(itemHeight * fit)) {
                    items(group.apps, key = { "${it.packageName}/${it.activityName}/${it.userSerial}" }) { app ->
                        var bounds by remember { mutableStateOf<Rect?>(null) }
                        AppListRow(
                            app, itemHeight, false, iconStyle, fontScale,
                            Modifier
                                .onGloballyPositioned { bounds = it.boundsInWindow().toRect() }
                                .rowGestures(app, { onTap(app, bounds) }, { onLongPress(app) }),
                        )
                    }
                }
            } else {
                group.apps.take(fit).forEachIndexed { i, app ->
                    AppListRow(app, itemHeight, i == hovered, iconStyle, fontScale, Modifier)
                }
            }
        }
    }
    if (style == RailStyle.POPOUT) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            content = content,
        )
    } else {
        content()
    }
}

@Composable
private fun GroupHeader(letter: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().semantics { heading() }, contentAlignment = Alignment.CenterStart) {
        Text(
            text = when (letter) {
                FAVORITES_GROUP -> "Favorites"
                WORK_GROUP -> "Work"
                else -> letter
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun AppListRow(
    app: AppEntry,
    height: Dp,
    highlighted: Boolean,
    iconStyle: IconStyle,
    fontScale: Float,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lift by animateFloatAsState(if (highlighted) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "hover")
    val iconSize = (height * 0.56f).coerceAtMost(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                val s = 1f + 0.02f * lift
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .background(colors.onBackground.copy(alpha = 0.08f * lift), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconStyle != IconStyle.NONE) {
            AppIcon(app, iconStyle, iconSize)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            app.displayLabel,
            fontSize = ((if (highlighted) 17 else 16) * fontScale).sp,
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
            color = colors.onBackground.copy(alpha = if (highlighted) 1f else 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (app.isWorkProfile) WorkBadge()
    }
}

private fun androidx.compose.ui.geometry.Rect.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
