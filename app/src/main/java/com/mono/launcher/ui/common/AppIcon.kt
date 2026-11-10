package com.mono.launcher.ui.common

import com.mono.launcher.ui.theme.Mono
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconCache
import com.mono.launcher.data.IconStyle
import com.mono.launcher.data.LoadedIcon

val LocalIconCache = staticCompositionLocalOf<IconCache> { error("IconCache not provided") }

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Draws the app icon per [style]; renders nothing for [IconStyle.NONE].
 * MINIMAL uses the app's themed glyph tinted to the text colour, so the list reads as one
 * calm, single-colour column; apps without a glyph fall back to a soft greyscale icon.
 */
@Composable
fun AppIcon(app: AppEntry, iconStyle: IconStyle, size: Dp, modifier: Modifier = Modifier) {
    if (iconStyle == IconStyle.NONE) return
    // Colour mode shows every icon in full colour.
    val style = if (Mono.palette.colorful) IconStyle.SYSTEM else iconStyle
    val cache = LocalIconCache.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val minimal = style == IconStyle.MINIMAL
    // produceState keeps its value across key changes, so always resolve for the current style;
    // otherwise switching Mono -> Colour would keep showing the old glyph.
    val icon: LoadedIcon? by produceState(cache.peek(app.key, minimal, px), app.key, px, minimal) {
        value = cache.peek(app.key, minimal, px) ?: cache.load(app, px, minimal)
    }
    val tint = MaterialTheme.colorScheme.onBackground
    val glyphFilter = remember(tint) { ColorFilter.tint(tint, BlendMode.SrcIn) }
    val loaded = icon
    if (loaded == null) {
        Spacer(modifier.size(size))
        return
    }
    val (filter, alpha) = when {
        loaded.isGlyph -> glyphFilter to 1f
        style == IconStyle.MINIMAL -> grayscale to 0.85f
        style == IconStyle.MONOCHROME -> grayscale to 1f
        else -> null to 1f
    }
    Image(
        bitmap = loaded.bitmap,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = filter,
        alpha = alpha,
    )
}
