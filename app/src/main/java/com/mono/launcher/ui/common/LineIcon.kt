package com.mono.launcher.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/** One stroke of a 24×24 outline icon, in SVG path syntax. */
data class IconStroke(val d: String, val dashed: Boolean = false)

/** An outline icon from the design: SVG paths on a 24-unit grid, drawn as round-capped strokes. */
class LineIconSpec(val strokes: List<IconStroke>, val strokeWidth: Float = 1.7f) {
    constructor(vararg d: String, strokeWidth: Float = 1.7f) : this(d.map { IconStroke(it) }, strokeWidth)
}

private val pathCache = HashMap<String, Path>()

internal fun svgPath(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** SVG circle as a path (two arcs). */
fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

@Composable
fun LineIcon(spec: LineIconSpec, size: Dp, color: Color, modifier: Modifier = Modifier, strokeWidth: Float = spec.strokeWidth) {
    val paths = remember(spec) { spec.strokes.map { svgPath(it.d) to it.dashed } }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 24f, pivot = Offset.Zero) {
            for ((path, dashed) in paths) {
                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(2.5f, 2.5f)) else null,
                    ),
                )
            }
        }
    }
}

/** Icon set from the Mono design handoff. */
object Icons {
    val Chat = LineIconSpec("M4 12a8 8 0 1 1 3.5 6.6L4 20l1.3-3.6A8 8 0 0 1 4 12z", strokeWidth = 1.6f)
    val Check = LineIconSpec("M5 12.5l4.5 4.5L19 7.5", strokeWidth = 2f)
    val ChevronRight = LineIconSpec("M9 6l6 6-6 6", strokeWidth = 2f)
    val ArrowUp = LineIconSpec("M12 19V5M6 11l6-6 6 6", strokeWidth = 1.8f)
    val ArrowRight = LineIconSpec("M5 12h14M13 6l6 6-6 6", strokeWidth = 2f)
    val Lock = LineIconSpec(
        "M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z",
        "M8 11V8a4 4 0 0 1 8 0v3",
        strokeWidth = 1.8f,
    )
    val LongPress = LineIconSpec(listOf(IconStroke(circle(12f, 12f, 3f)), IconStroke(circle(12f, 12f, 7.5f), dashed = true)), 1.8f)
    val Globe = LineIconSpec(
        circle(12f, 12f, 8.5f),
        "M3.5 12h17M12 3.5c2.5 2.5 3.5 5.5 3.5 8.5s-1 6-3.5 8.5c-2.5-2.5-3.5-5.5-3.5-8.5s1-6 3.5-8.5z",
        strokeWidth = 1.6f,
    )
    val Search = LineIconSpec(
        "M10.6 4.2c3.6-.3 6.4 2.4 6.3 5.9-.1 3.5-2.8 6.1-6.2 6-3.4 0-6-2.7-5.9-6 .1-3.1 2.6-5.6 5.8-5.9z",
        "M15.4 15.2l4.4 4.6",
        strokeWidth = 1.8f,
    )
    val Close = LineIconSpec("M6 6l12 12M18 6L6 18", strokeWidth = 2f)
    val Star = LineIconSpec("M12 4l2.4 5 5.6.8-4 3.9 1 5.5-5-2.7-5 2.7 1-5.5-4-3.9 5.6-.8z")
    val Pencil = LineIconSpec("M4 20h4l10-10-4-4L4 16zM13 7l4 4")
    val EyeOff = LineIconSpec("M3 12s3.5-6 9-6 9 6 9 6-3.5 6-9 6-9-6-9-6zM4 4l16 16")
    val Eye = LineIconSpec("M3 12s3.5-6 9-6 9 6 9 6-3.5 6-9 6-9-6-9-6z", circle(12f, 12f, 2.5f))
    val Bell = LineIconSpec("M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4")
    val Info = LineIconSpec("M12 3.5a8.5 8.5 0 1 1 0 17 8.5 8.5 0 0 1 0-17zM12 11v5M12 8h.01")
    val Trash = LineIconSpec("M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13")
    val Plus = LineIconSpec("M12 5v14M5 12h14", strokeWidth = 1.8f)
    val Shortcut = LineIconSpec("M7 17L17 7M9 7h8v8", strokeWidth = 1.8f)
    val Flashlight = LineIconSpec("M8 3h8l-1 5H9z", "M9 8h6v3.5l-1 9.5h-4l-1-9.5z", "M12 13v3")
    val Speaker = LineIconSpec("M5 9h3l4-4v14l-4-4H5z", "M16 9a4 4 0 0 1 0 6", "M18.5 6.5a7.5 7.5 0 0 1 0 11")
    val Vibrate = LineIconSpec("M9 4h6a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z", "M4.5 9v6M19.5 9v6")
    val Mute = LineIconSpec("M5 9h3l4-4v14l-4-4H5z", "M16 9.5l5 5M21 9.5l-5 5")
    val Moon = LineIconSpec("M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z")
    val Target = LineIconSpec(circle(12f, 12f, 8f), circle(12f, 12f, 3.5f))
    val Wifi = LineIconSpec("M3 9.5a13 13 0 0 1 18 0", "M6 13a8.5 8.5 0 0 1 12 0", "M9 16.5a4 4 0 0 1 6 0", "M12 19.8h.01", strokeWidth = 1.8f)
    val Data = LineIconSpec("M8 20V5M4.5 8.5L8 5l3.5 3.5", "M16 4v15M12.5 15.5L16 19l3.5-3.5")
    val Bluetooth = LineIconSpec("M7 7.5l10 9-5 4.5V3l5 4.5-10 9")
    val Sun = LineIconSpec(circle(12f, 12f, 4f), "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4")
    val Sliders = LineIconSpec("M4 7h9M17 7h3M4 17h3M11 17h9", circle(15f, 7f, 2f), circle(9f, 17f, 2f))
    val Widget = LineIconSpec(
        "M5 4h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M14 4h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M5 13h14a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
    )
    val Hourglass = LineIconSpec("M7 3h10M7 21h10", "M8 3c0 5 8 5 8 9s-8 4-8 9", "M16 3c0 5-8 5-8 9s8 4 8 9")
    val Alarm = LineIconSpec(circle(12f, 13f, 7f), "M12 9.5v3.5l2 2", "M5 4.5L3 6.5M19 4.5l2 2", strokeWidth = 1.7f)
    val Calendar = LineIconSpec(
        "M6 5.5h12a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7.5a2 2 0 0 1 2-2z",
        "M4 10h16M8.5 3.5v4M15.5 3.5v4",
        strokeWidth = 1.7f,
    )
    val Battery = LineIconSpec(
        "M5 8h12a1.5 1.5 0 0 1 1.5 1.5v5A1.5 1.5 0 0 1 17 16H5a1.5 1.5 0 0 1-1.5-1.5v-5A1.5 1.5 0 0 1 5 8z",
        "M21 11v2",
        strokeWidth = 1.7f,
    )
    val BatteryCharging = LineIconSpec(
        "M5 8h12a1.5 1.5 0 0 1 1.5 1.5v5A1.5 1.5 0 0 1 17 16H5a1.5 1.5 0 0 1-1.5-1.5v-5A1.5 1.5 0 0 1 5 8z",
        "M21 11v2M11.5 9.5l-2 3h3l-2 3",
        strokeWidth = 1.7f,
    )
}
