package com.mono.launcher.data

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import kotlin.math.abs

/**
 * Turns an adaptive icon's foreground layer into an alpha-only glyph that the UI tints,
 * for apps that don't ship a themed (monochrome) icon.
 *
 * - Sparse foreground (a logo on transparency): the logo's shape is the glyph.
 * - Dense foreground (the layer carries its own backdrop): keep only pixels that stand out
 *   from the dominant tone, which is the logo drawn on that backdrop.
 */
object GlyphMaker {
    private const val SPARSE = 0.5f
    private const val MIN_COVERAGE = 0.04f

    /** Legacy icons are a logo on a tile: keep the parts that contrast with the tile. */
    fun fromLegacy(src: Bitmap): Bitmap? = build(src, forceContrast = true)

    fun fromForeground(src: Bitmap): Bitmap? = build(src, forceContrast = false)

    private fun build(src: Bitmap, forceContrast: Boolean): Bitmap? {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        var opaque = 0
        var lumSum = 0f
        for (p in px) {
            if (p ushr 24 > 128) {
                opaque++
                lumSum += luma(p)
            }
        }
        if (opaque == 0) return null
        val coverage = opaque.toFloat() / px.size

        val out = IntArray(px.size)
        var kept = 0
        if (!forceContrast && coverage < SPARSE) {
            for (i in px.indices) {
                val a = px[i] ushr 24
                out[i] = (a shl 24) or 0xFFFFFF
                if (a > 128) kept++
            }
        } else {
            val mean = lumSum / opaque
            for (i in px.indices) {
                val p = px[i]
                val a = p ushr 24
                val contrast = ((abs(luma(p) - mean) - 0.12f) / 0.25f).coerceIn(0f, 1f)
                val alpha = (contrast * a).toInt()
                out[i] = (alpha shl 24) or 0xFFFFFF
                if (alpha > 128) kept++
            }
        }
        if (kept.toFloat() / px.size < MIN_COVERAGE) return null
        return createBitmap(w, h).apply { setPixels(out, 0, w, 0, 0, w, h) }
    }

    private fun luma(p: Int): Float {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }
}
