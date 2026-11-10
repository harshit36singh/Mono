package com.mono.launcher.data

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lazily loads app icons and keeps them in a bounded memory cache. */
@Singleton
class IconCache @Inject constructor(private val repository: AppRepository) {
    // Measured in kilobytes so the cache is bounded by memory, not entry count.
    private val cache = object : LruCache<AppKey, ImageBitmap>(MAX_KB) {
        override fun sizeOf(key: AppKey, value: ImageBitmap): Int =
            (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }

    fun peek(key: AppKey): ImageBitmap? = cache.get(key)

    suspend fun load(app: AppEntry, sizePx: Int): ImageBitmap? {
        cache.get(app.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val info = repository.activityInfo(app) ?: return@withContext null
            runCatching {
                info.getBadgedIcon(0).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()?.also { cache.put(app.key, it) }
        }
    }

    fun clear() = cache.evictAll()

    private companion object {
        const val MAX_KB = 8 * 1024
    }
}
