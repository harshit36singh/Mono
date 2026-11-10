package com.mono.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.launcher.data.IconCache
import com.mono.launcher.ui.HomeViewModel
import com.mono.launcher.ui.Overlay
import com.mono.launcher.ui.common.LocalIconCache
import com.mono.launcher.ui.home.HomeActions
import com.mono.launcher.ui.home.HomeScreen
import com.mono.launcher.ui.focus.NudgeScreen
import com.mono.launcher.ui.onboarding.OnboardingScreen
import com.mono.launcher.ui.panel.MonoPanel
import com.mono.launcher.ui.search.SearchOverlay
import com.mono.launcher.ui.settings.SettingsActions
import com.mono.launcher.ui.settings.SettingsScreen
import com.mono.launcher.ui.sheets.AppActionsSheet
import com.mono.launcher.ui.sheets.AppSheetActions
import com.mono.launcher.ui.sheets.RenameDialog
import com.mono.launcher.ui.theme.MonoTheme
import com.mono.launcher.widget.LocalWidgetHost
import com.mono.launcher.widget.WidgetFlow
import com.mono.launcher.widget.WidgetHostManager
import androidx.compose.runtime.mutableStateOf
import com.mono.launcher.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var iconCache: IconCache
    @Inject lateinit var widgetHosts: WidgetHostManager
    private val vm: HomeViewModel by viewModels()
    private lateinit var widgets: WidgetFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgets = WidgetFlow(this, widgetHosts, currentId = { vm.state.value.settings.widgetId }, save = vm::setWidget)
        // A launcher never finishes on Back; Back only closes whatever is open.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = vm.closeOverlay()
        })
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            MonoTheme(state.settings.theme, state.settings.colorMode, state.settings.fontId) {
                val dark = isDark(state.settings.theme)
                LaunchedEffect(dark) {
                    val transparent = Color.Transparent.toArgb()
                    val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                CompositionLocalProvider(LocalIconCache provides iconCache, LocalWidgetHost provides widgetHosts) {
                    MonoRoot(vm, widgets)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }

    override fun onStart() {
        super.onStart()
        widgetHosts.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHosts.stopListening()
        vm.onStop()
    }

    @Deprecated("AppWidgetHost's configure flow still reports through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.onActivityResult(requestCode, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) iconCache.clear()
    }
}

@Composable
private fun MonoRoot(vm: HomeViewModel, widgets: WidgetFlow) {
    val state by vm.state.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val resetTick by vm.resetTick.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (!state.loaded) return
    if (!state.settings.onboardingDone) {
        OnboardingScreen(state.apps.visible) { vm.completeOnboarding(it) }
        return
    }

    val homeActions = remember(vm) {
        HomeActions(
            launch = { app, bounds, options -> vm.launch(app, bounds, options) },
            showActions = { vm.show(Overlay.Actions(it)) },
            openSearch = { vm.show(Overlay.Search) },
            openSettings = { vm.show(Overlay.Settings) },
            openPanel = { vm.show(Overlay.Panel) },
            openNotification = vm::openNotification,
            dismissNotification = vm::dismissNotification,
            reorderFavorites = { vm.reorderFavorites(it) },
            mediaPrevious = vm::mediaPrevious,
            mediaPlayPause = vm::mediaPlayPause,
            mediaNext = vm::mediaNext,
        )
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(state, homeActions, resetTick)

        AnimatedVisibility(
            visible = overlay == Overlay.Search,
            enter = fadeIn(tween(150)) + slideInVertically(tween(180)) { it / 6 },
            exit = fadeOut(tween(120)),
        ) {
            SearchOverlay(
                query = query,
                results = results,
                iconStyle = state.settings.iconStyle,
                fontScale = state.settings.fontScale,
                onQuery = vm::setQuery,
                onLaunch = { vm.launch(it) },
                onLaunchTop = vm::launchTopResult,
                onLongPress = { vm.show(Overlay.Actions(it)) },
                onOpenSettings = { vm.show(Overlay.Settings) },
                onClose = vm::closeOverlay,
            )
        }

        AnimatedVisibility(
            visible = overlay == Overlay.Settings,
            enter = fadeIn(tween(150)) + slideInVertically(tween(200)) { it / 8 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(150)) { it / 8 },
        ) {
            SettingsScreen(
                settings = state.settings,
                hidden = state.apps.hidden,
                allApps = state.apps.all,
                focus = state.focus,
                actions = SettingsActions(
                    update = { vm.updateSettings(it) },
                    unhide = { vm.setHidden(it, false) },
                    toggleMute = vm::toggleMute,
                    export = { uri, done -> vm.exportBackup(uri, done) },
                    import = { uri, done -> vm.importBackup(uri, done) },
                    applyWallpaper = { wp, target, done -> vm.applyWallpaper(wp, target, done) },
                    toggleFocusApp = vm::toggleFocusApp,
                    toggleDistracting = vm::toggleDistracting,
                    startFocus = { vm.startFocus(it) },
                    endFocus = vm::endFocus,
                    pickWidget = { widgets.pick(it) },
                    removeWidget = { widgets.remove() },
                    close = vm::closeOverlay,
                ),
            )
        }

        AnimatedVisibility(
            visible = overlay == Overlay.Panel,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 3 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(160)) { -it / 3 },
        ) {
            MonoPanel(
                focus = state.focus,
                haptics = state.settings.haptics,
                onStartFocus = { vm.startFocus(60) },
                onEndFocus = vm::endFocus,
                onOpenSettings = { vm.show(Overlay.Settings) },
                onClose = vm::closeOverlay,
            )
        }

        val nudge = overlay as? Overlay.Nudge
        AnimatedVisibility(visible = nudge != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(140))) {
            val last = remember { mutableStateOf<Overlay.Nudge?>(null) }
            if (nudge != null) last.value = nudge
            last.value?.let { n ->
                NudgeScreen(
                    app = n.app,
                    seconds = state.settings.nudgeSeconds,
                    onOpen = { vm.launchNow(n.app, n.bounds, n.options) },
                    onCancel = vm::closeOverlay,
                )
            }
        }
    }

    when (val o = overlay) {
        is Overlay.Actions -> {
            // Re-read the entry so the sheet reflects live favorite/hidden state.
            val app = (state.apps.visible + state.apps.hidden).firstOrNull { it.key == o.app.key } ?: o.app
            AppActionsSheet(
                app = app,
                favoritesCount = state.apps.favorites.size,
                muted = app.packageName in state.settings.mutedPackages,
                distracting = app.packageName in state.settings.distractingApps,
                focusHidden = app.packageName in state.settings.focusApps,
                iconStyle = state.settings.iconStyle,
                actions = AppSheetActions(
                    loadShortcuts = vm::shortcuts,
                    launchShortcut = vm::launchShortcut,
                    toggleFavorite = { a ->
                        vm.toggleFavorite(a) { Toast.makeText(context, "Favorites are full", Toast.LENGTH_SHORT).show() }
                    },
                    rename = { vm.show(Overlay.Rename(it)) },
                    setHidden = { a, h -> vm.setHidden(a, h) },
                    toggleMute = vm::toggleMute,
                    toggleDistracting = vm::toggleDistracting,
                    toggleFocusApp = vm::toggleFocusApp,
                    appInfo = vm::openAppInfo,
                    uninstall = vm::uninstall,
                    dismiss = vm::closeOverlay,
                ),
            )
        }
        is Overlay.Rename -> RenameDialog(o.app, onRename = { vm.rename(o.app, it) }, onDismiss = vm::closeOverlay)
        else -> Unit
    }
}
