package com.mono.launcher.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

private const val TAP = 0
private const val MOVED = 1

/**
 * Tap / long-press / long-press-then-drag on one element, in a single detector so the
 * three never fire together.
 *
 * - Movement before the long-press timeout is left unconsumed, so parents can still
 *   treat it as a swipe.
 * - With [onDrag] set, a long press followed by vertical movement becomes a drag; a long
 *   press released in place calls [onLongPress]. Without it, [onLongPress] fires at the timeout.
 */
fun Modifier.rowGestures(
    key: Any?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressStart: () -> Unit = {},
    onDragStart: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.isConsumed) return@awaitEachGesture
        // Claim the press so empty-area handlers (double-tap, long-press) on parents ignore it.
        down.consume()
        val slop = viewConfiguration.touchSlop

        val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var r = MOVED
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUp()) {
                    if (!change.isConsumed) {
                        change.consume()
                        r = TAP
                    }
                    break
                }
                if (change.isConsumed || (change.position - down.position).getDistance() > slop) break
            }
            r
        }

        when (early) {
            TAP -> onTap()
            MOVED -> Unit
            null -> {
                onLongPressStart()
                if (onDrag == null) {
                    onLongPress()
                    // Swallow the rest of the gesture.
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (change.changedToUp()) break
                    }
                } else {
                    var dragging = false
                    var total = 0f
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) {
                            change.consume()
                            break
                        }
                        val dy = change.positionChange().y
                        change.consume()
                        total += dy
                        if (!dragging && abs(total) > slop) {
                            dragging = true
                            onDragStart?.invoke()
                            onDrag(total)
                        } else if (dragging) {
                            onDrag(dy)
                        }
                    }
                    if (dragging) onDragEnd?.invoke() else onLongPress()
                }
            }
        }
    }
}
