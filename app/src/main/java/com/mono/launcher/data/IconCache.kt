package com.mono.launcher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
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
                (if (minimal) glyph(info.getIcon(0), sizePx) else null)
                    ?: LoadedIcon(info.getBadgedIcon(0).toBitmap(sizePx, sizePx).asImageBitmap(), isGlyph = false)
            }.getOrNull()?.also { cache.put(key, it) }
        }
    }

    /** Renders the adaptive icon's monochrome layer (Android 13+ themed icons), filling the frame. */
    private fun glyph(drawable: android.graphics.drawable.Drawable, size: Int): LoadedIcon? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val mono = (drawable as? AdaptiveIconDrawable)?.monochrome ?: return null
        val bitmap: Bitmap = createBitmap(size, size)
        // Adaptive layers are 108dp with a 72dp visible safe zone; scale 1.5x so the glyph fills the frame.
        val inset = size / 4
        mono.setBounds(-inset, -inset, size + inset, size + inset)
        mono.draw(Canvas(bitmap))
        return LoadedIcon(bitmap.asImageBitmap(), isGlyph = true)
    }

    fun clear() = cache.evictAll()

    private companion object {
        const val MAX_KB = 8 * 1024
    }
}
