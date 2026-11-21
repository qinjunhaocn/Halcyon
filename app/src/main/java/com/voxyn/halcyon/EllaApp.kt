package com.voxyn.halcyon

import android.app.Application
import android.os.Build
import com.voxyn.halcyon.data.AppLogcatCollector
import com.voxyn.halcyon.data.AppLogStore
import com.voxyn.halcyon.data.AppIconManager
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.webdav.WebDavClient
import com.voxyn.halcyon.data.repository.RemoteAudioCache
import com.voxyn.halcyon.mcp.McpServerService
import com.voxyn.halcyon.web.WebMusicService
import com.voxyn.halcyon.oem.AppMemoryTrimAdapter
import com.voxyn.halcyon.player.PlaybackWidgetUpdater
import com.voxyn.halcyon.oem.HyperOsFairMemoryAdapter
import com.voxyn.halcyon.ui.LibrarySortUiState
import com.voxyn.halcyon.ui.settings.WebDavAutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

class EllaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before anything touches DataStore: tells a clean install apart from an over-install.
        runCatching { com.voxyn.halcyon.data.AppUpgradeNotice.capture(this) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
                .onFailure { AppLogStore.warn(this, "EllaApp", "Unable to exempt hidden APIs", it) }
        }
        WebDavClient.initContext(this)
        RemoteAudioCache.init(this)
        runCatching { com.voxyn.halcyon.data.sourceplugin.SourcePluginRegistry.init(this) }
            .onFailure { AppLogStore.warn(this, "EllaApp", "Source-plugin registry init failed", it) }
        AppLogStore.install(this)
        AppLogcatCollector.start(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogStore.crash(this, thread.name, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        AppLogStore.info(this, "EllaApp", "Application started")
        HyperOsFairMemoryAdapter.initialize(this)
        AppMemoryTrimAdapter.initialize(this)
        com.voxyn.halcyon.plugin.i18n.PluginLocales.initialize(this)

        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settingsManager = SettingsManager.getInstance(this)
        WebDavAutoBackupScheduler.start(this, appScope)

        // Warm the process-wide sort singleton off the main thread. runBlocking here previously
        // stalled Application.onCreate (and cold start) whenever DataStore was slow — especially
        // painful after a huge Navidrome sync left a large prefs/cache footprint.
        // Lists may briefly use defaults then recompose once warmUp finishes (#210/#126/#133).
        appScope.launch {
            runCatching { LibrarySortUiState.warmUp(settingsManager) }
        }

        // Auto-start MCP server if previously enabled
        appScope.launch {
            if (settingsManager.mcpServerEnabled.first()) {
                McpServerService.start(this@EllaApp)
            }
        }
        appScope.launch {
            if (settingsManager.webMusicServerEnabled.first()) {
                if (!WebMusicService.start(this@EllaApp)) {
                    settingsManager.setWebMusicServerEnabled(false)
                }
            }
        }
        appScope.launch {
            settingsManager.appIconStyle
                .distinctUntilChanged()
                .collect { style ->
                    AppIconManager.apply(this@EllaApp, style)
                }
        }
        appScope.launch {
            settingsManager.widgetSafeLayout
                .distinctUntilChanged()
                .collect { enabled ->
                    PlaybackWidgetUpdater.setSafeLayout(this@EllaApp, enabled)
                }
        }
    }
}
