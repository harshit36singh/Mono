package com.mono.launcher.wallpaper

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class Wallpaper(
    val id: String,
    val name: String,
    @param:DrawableRes val fullRes: Int,
    @param:DrawableRes val thumbRes: Int,
)

enum class WallpaperTarget { HOME, LOCK, BOTH }

/** Applies a bundled wallpaper through WallpaperManager (needs the normal SET_WALLPAPER permission). */
@Singleton
class WallpaperSetter @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun apply(wallpaper: Wallpaper, target: WallpaperTarget): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val which = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            // Reads the bundled .webp file as-is (no decode); valid for drawable-nodpi files.
            @SuppressLint("ResourceType")
            val input = context.resources.openRawResource(wallpaper.fullRes)
            input.use { stream ->
                WallpaperManager.getInstance(context).setStream(stream, null, true, which)
            }
            Unit
        }
    }
}

private val bitmapCache = object : LruCache<Int, ImageBitmap>(24 * 1024) {
    override fun sizeOf(key: Int, value: ImageBitmap) = (value.width * value.height * 4 / 1024).coerceAtLeast(1)
}

/** Decodes a drawable resource off the main thread and caches it. */
@Composable
fun rememberResourceBitmap(@DrawableRes res: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState(bitmapCache.get(res), res) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeResource(context.resources, res).asImageBitmap() }.getOrNull()
            }?.also { bitmapCache.put(res, it) }
        }
    }
    return bitmap
}
