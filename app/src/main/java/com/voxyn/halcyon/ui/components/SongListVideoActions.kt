package com.voxyn.halcyon.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.decodeNeteaseKey
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.SongTagInfo
import com.voxyn.halcyon.data.neteaseMvUrl
import com.voxyn.halcyon.ui.player.DynamicCoverSource
import com.voxyn.halcyon.ui.player.musicVideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal data class SongListVideoActions(
    val localSource: DynamicCoverSource? = null,
    val onlineUrl: String? = null
)

/** MV folder and tag probing is intentionally tightly bounded across every song-list surface. */
private val SongListVideoActionLimiter = Semaphore(2)

@Composable
internal fun rememberSongListVideoActions(
    song: Song,
    loadSongTagInfo: ((Song) -> SongTagInfo)?,
    enabled: Boolean
): SongListVideoActions {
    val context = LocalContext.current
    val settingsManager = remember(context) { SettingsManager.getInstance(context) }
    val showLocal by settingsManager.showLocalMusicVideoInLists.collectAsState(initial = true)
    val showOnline by settingsManager.showOnlineMusicVideoInLists.collectAsState(initial = true)
    val dynamicFolders by settingsManager.dynamicCoverCustomFolders.collectAsState(initial = emptyList())
    val videoFolders by settingsManager.musicVideoCustomFolders.collectAsState(initial = emptyList())
    val actions by produceState(
        initialValue = SongListVideoActions(),
        song.id,
        song.path,
        song.onlineMvId,
        song.dateModified,
        song.fileSize,
        dynamicFolders,
        videoFolders,
        showLocal,
        showOnline,
        loadSongTagInfo,
        enabled
    ) {
        if (!enabled || (!showLocal && !showOnline)) {
            value = SongListVideoActions()
            return@produceState
        }
        value = SongListVideoActionLimiter.withPermit {
            withContext(Dispatchers.IO) {
                val localSource = if (showLocal) {
                    song.musicVideoSource(
                        context = context,
                        customRootPaths = dynamicFolders,
                        musicVideoCustomFolders = videoFolders
                    )
                } else null
                val onlineUrl = if (localSource == null && showOnline) {
                    val pluginId = com.voxyn.halcyon.data.sourceplugin.sourcePluginKeyOf(song.onlineSource)
                    when {
                        pluginId.isNotBlank() && song.onlineMvId.isNotBlank() ->
                            com.voxyn.halcyon.data.sourceplugin.SourcePluginRegistry.config(context, pluginId)
                                ?.takeIf { it.hasCapability("mv") }
                                ?.let { "halcyon-plugin-mv://$pluginId/${android.net.Uri.encode(song.onlineMvId)}" }
                        else ->
                            (song.onlineMvId.takeIf { song.onlineSource == "netease" && it.isNotBlank() }
                                ?: decodeNeteaseKey(loadSongTagInfo?.invoke(song)?.neteaseKey.orEmpty())?.mvId)
                                ?.takeIf { it.toLongOrNull()?.let { id -> id > 0L } == true }
                                ?.let(::neteaseMvUrl)
                    }
                } else null
                SongListVideoActions(localSource, onlineUrl)
            }
        }
    }
    return actions
}
