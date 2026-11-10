package com.mono.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mono.launcher.data.ThemeMode

private val MonoDark = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFBDBDBD),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF232323),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    outline = Color(0xFF5A5A5A),
)

private val MonoLight = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4A4A4A),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFEAEAEA),
    onSurfaceVariant = Color(0xFF4F4F4F),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    outline = Color(0xFF9E9E9E),
)

/** Whether Home text sits on a dark scrim (light text) or a light one. */
val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM, ThemeMode.DYNAMIC -> isSystemInDarkTheme()
}

@Composable
fun MonoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        mode == ThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> MonoDark
        else -> MonoLight
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
