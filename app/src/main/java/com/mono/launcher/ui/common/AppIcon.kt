package com.mono.launcher.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mono.launcher.data.AppEntry
import com.mono.launcher.data.IconCache
import com.mono.launcher.data.IconStyle

val LocalIconCache = staticCompositionLocalOf<IconCache> { error("IconCache not provided") }

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Draws the app icon per [style]; renders nothing for [IconStyle.NONE]. */
@Composable
fun AppIcon(app: AppEntry, style: IconStyle, size: Dp, modifier: Modifier = Modifier) {
    if (style == IconStyle.NONE) return
    val cache = LocalIconCache.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap: ImageBitmap? by produceState(cache.peek(app.key), app.key, px) {
        if (value == null) value = cache.load(app, px)
    }
    val bmp = bitmap
    if (bmp == null) {
        Spacer(modifier.size(size))
    } else {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = modifier.size(size),
            colorFilter = if (style == IconStyle.MONOCHROME) grayscale else null,
        )
    }
}
