package com.mono.launcher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [isGlyph] = the bitmap is a single-colour shape (alpha only) that the UI tints to the theme,
 * taken from the app's themed-icon layer. Otherwise it's the regular icon.
 */
data class LoadedIcon(val bitmap: ImageBitmap, val isGlyph: Boolean)

/** Lazily loads app icons and keeps them in a bounded memory cache. */
@Singleton
class IconCache @Inject constructor(private val repository: AppRepository) {
    private data class Key(val app: AppKey, val minimal: Boolean, val size: Int)

    // Measured in kilobytes so the cache is bounded by memory, not entry count.
    private val cache = object : LruCache<Key, LoadedIcon>(MAX_KB) {
        override fun sizeOf(key: Key, value: LoadedIcon): Int =
            (value.bitmap.width * value.bitmap.height * 4 / 1024).coerceAtLeast(1)
    }

    fun peek(app: AppKey, minimal: Boolean, sizePx: Int): LoadedIcon? = cache.get(Key(app, minimal, sizePx))

    suspend fun load(app: AppEntry, sizePx: Int, minimal: Boolean): LoadedIcon? {
        val key = Key(app.key, minimal, sizePx)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val info = repository.activityInfo(app) ?: return@withContext null
            runCatching {
                (if (minimal) runCatching { glyph(info.getIcon(0), sizePx) }.getOrNull() else null)
                    ?: LoadedIcon(info.getBadgedIcon(0).toBitmap(sizePx, sizePx).asImageBitmap(), isGlyph = false)
            }.getOrNull()?.also { cache.put(key, it) }
        }
    }

    /**
     * A single-colour glyph for the icon:
     * 1. the app's own themed-icon layer (Android 13+), else
     * 2. one generated from the adaptive icon's foreground (logo without its backdrop).
     * 3. for legacy icons, the logo pulled off its tile by contrast.
     * If nothing usable comes out, the icon is shown in soft greyscale.
     */
    private fun glyph(drawable: Drawable, size: Int): LoadedIcon? {
        val adaptive = drawable as? AdaptiveIconDrawable
            ?: return GlyphMaker.fromLegacy(drawable.toBitmap(size, size))?.let { LoadedIcon(it.asImageBitmap(), isGlyph = true) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adaptive.monochrome?.let { return LoadedIcon(renderLayer(it, size).asImageBitmap(), isGlyph = true) }
        }
        val fg = adaptive.foreground ?: return null
        return GlyphMaker.fromForeground(renderLayer(fg, size))?.let { LoadedIcon(it.asImageBitmap(), isGlyph = true) }
    }

    /** Adaptive layers are 108dp with a 72dp visible zone; draw at 1.5x so the visible part fills the frame. */
    private fun renderLayer(layer: Drawable, size: Int): Bitmap {
        val bitmap: Bitmap = createBitmap(size, size)
        val inset = size / 4
        layer.setBounds(-inset, -inset, size + inset, size + inset)
        layer.draw(Canvas(bitmap))
        return bitmap
    }

    fun clear() = cache.evictAll()

    private companion object {
        const val MAX_KB = 8 * 1024
    }
}
