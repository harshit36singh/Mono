package com.mono.launcher.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.ui.common.Icons
import com.mono.launcher.ui.common.LineIcon
import com.mono.launcher.ui.common.Segmented
import com.mono.launcher.ui.home.ClockHeader
import com.mono.launcher.wallpaper.Wallpaper
import com.mono.launcher.wallpaper.WallpaperCatalog
import com.mono.launcher.wallpaper.WallpaperTarget
import com.mono.launcher.wallpaper.rememberResourceBitmap

/** Horizontal carousel of bundled wallpapers; the applied one has a 2dp text-colour border. */
@Composable
fun WallpaperCarousel(selectedId: String?, onOpen: (Wallpaper) -> Unit) {
    val c = MaterialTheme.colorScheme
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        val i = WallpaperCatalog.all.indexOfFirst { it.id == selectedId }
        if (i > 0) state.scrollToItem(i)
    }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(WallpaperCatalog.all, key = { it.id }) { wp ->
            val on = wp.id == selectedId
            val thumb = rememberResourceBitmap(wp.thumbRes)
            Box(
                Modifier
                    .size(96.dp, 172.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surfaceContainer)
                    .border(2.dp, if (on) c.onBackground else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "Preview") { onOpen(wp) }
                    .semantics {
                        contentDescription = wp.name
                        if (on) stateDescription = "Current wallpaper"
                    },
            ) {
                if (thumb != null) {
                    Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                if (on) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(c.onBackground),
                        contentAlignment = Alignment.Center,
                    ) { LineIcon(Icons.Check, 12.dp, c.background, strokeWidth = 3.5f) }
                }
            }
        }
    }
}

/** Full-screen preview with a sample clock, a Home / Lock / Both picker and an apply button. */
@Composable
fun WallpaperPreview(
    wallpaper: Wallpaper,
    fontScale: Float,
    onApply: (Wallpaper, WallpaperTarget, (Result<Unit>) -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var target by remember { mutableStateOf(WallpaperTarget.BOTH) }
    var applying by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    val image = rememberResourceBitmap(wallpaper.fullRes) ?: rememberResourceBitmap(wallpaper.thumbRes)

    // The preview is always dark-on-image: white text over a dark scrim, like Home on these wallpapers.
    val ink = Color(0xFFF2F2F2)
    val paper = Color(0xFF0E0E0E)
    Box(Modifier.fillMaxSize().background(paper)) {
        if (image != null) {
            Image(image, wallpaper.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(paper.copy(alpha = 0.25f), paper.copy(alpha = 0.15f), paper.copy(alpha = 0.85f)))))

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(paper.copy(alpha = 0.5f))
                    .clickable(role = Role.Button, onClickLabel = "Close preview", onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { LineIcon(Icons.Close, 18.dp, ink) }

            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides ink) {
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onBackground = ink, onSurfaceVariant = Color(0xFFB8B8B8))) {
                    ClockHeader(fontScale, Modifier.padding(start = 8.dp, top = 24.dp))
                }
            }

            Spacer(Modifier.weight(1f))
            Text(wallpaper.name, fontSize = 15.sp, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = paper.copy(alpha = 0.6f),
                    onBackground = ink,
                    onSurfaceVariant = Color(0xFFB8B8B8),
                ),
            ) {
                Segmented(
                    WallpaperTarget.entries,
                    target,
                    { when (it) { WallpaperTarget.HOME -> "Home"; WallpaperTarget.LOCK -> "Lock"; WallpaperTarget.BOTH -> "Both" } },
                    { target = it },
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (applying) ink.copy(alpha = 0.5f) else ink)
                    .clickable(enabled = !applying, role = Role.Button) {
                        applying = true
                        onApply(wallpaper, target) { r ->
                            applying = false
                            Toast.makeText(context, if (r.isSuccess) "Wallpaper set" else "Couldn't set wallpaper: ${r.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            if (r.isSuccess) onClose()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (applying) "Setting…" else "Set wallpaper", fontSize = 16.sp, color = paper, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
        }
    }
}
