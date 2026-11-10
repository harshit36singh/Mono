package com.mono.launcher.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.mono.launcher.ui.theme.Mono

/** Poses of Mo, Mono's mascot. Shapes come from the design handoff (120-unit grid). */
enum class MoPose { WAVE, RESTING, NOTICING, DONE, OUTLINE }

private const val BODY = "M58 20 C82 18 104 38 102 64 C100 90 82 104 58 103 C34 102 18 86 19 62 C20 38 36 22 58 20 Z"
private const val BODY_LOW = "M58 24 C82 22 104 42 102 68 C100 94 82 108 58 107 C34 106 18 90 19 66 C20 42 36 26 58 24 Z"

/**
 * Mo: a single-colour pebble. Body uses the text colour and features the background colour,
 * so it tints with the theme. Used in onboarding, empty states and the Settings footer.
 */
@Composable
fun Mo(
    pose: MoPose,
    size: Dp,
    modifier: Modifier = Modifier,
    body: Color = MaterialTheme.colorScheme.onBackground,
    features: Color = MaterialTheme.colorScheme.background,
    description: String? = null,
) {
    val accent = Mono.palette.tertiaryText
    Canvas(
        modifier
            .size(size)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        scale(this.size.minDimension / 120f, pivot = Offset.Zero) {
            when (pose) {
                MoPose.WAVE -> {
                    fill(BODY, body)
                    line("M99 56 Q111 48 111 34", body, 8f)
                    oval(48f, 58f, 3.6f, 5f, features)
                    oval(70f, 58f, 3.6f, 5f, features)
                    line("M51 71 Q59 78 67 71", features, 3.6f)
                }
                MoPose.RESTING -> {
                    fill(BODY, body)
                    line("M43 59 Q48 63 53 59", features, 3f)
                    line("M65 59 Q70 63 75 59", features, 3f)
                    oval(59f, 74f, 2.2f, 2.2f, features)
                    line("M90 8 H100 L90 18 H100", accent, 2.5f)
                }
                MoPose.NOTICING -> {
                    fill(BODY, body)
                    oval(96f, 20f, 8f, 8f, body)
                    oval(48f, 57f, 3.6f, 5.4f, features)
                    oval(70f, 57f, 3.6f, 5.4f, features)
                    oval(59f, 75f, 3.2f, 4f, features)
                }
                MoPose.DONE -> {
                    fill(BODY_LOW, body)
                    line("M26 56 Q14 44 16 28", body, 7f)
                    line("M98 56 Q108 44 104 28", body, 7f)
                    line("M43 63 L48 58 L53 63", features, 3f)
                    line("M65 63 L70 58 L75 63", features, 3f)
                    fill("M50 74 Q59 86 68 74 Z", features)
                }
                MoPose.OUTLINE -> {
                    line(BODY, body, 6f)
                    oval(48f, 58f, 4f, 5.5f, body)
                    oval(70f, 58f, 4f, 5.5f, body)
                }
            }
        }
    }
}

private fun DrawScope.fill(d: String, color: Color) = drawPath(svgPath(d), color)

private fun DrawScope.line(d: String, color: Color, width: Float) =
    drawPath(svgPath(d), color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
    drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
