package com.voxyn.halcyon.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.ActionMenuIds
import com.voxyn.halcyon.data.ActionMenuLayout
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.model.Song

@Composable
internal fun SongMoreActionSheet(
    song: Song,
    extraTopContent: (@Composable ColumnScope.() -> Unit)?,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onSpectrum: () -> Unit,
    onInfo: () -> Unit,
    onRating: () -> Unit,
    onAiInterpret: () -> Unit,
    onArtist: () -> Unit,
    onAlbum: () -> Unit,
    onEditTag: (() -> Unit)?,
    onLyricTiming: (() -> Unit)?,
    onAudioTools: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onDeleteSingleRecentPlayback: (() -> Unit)? = null,
    onClearRecentPlayback: (() -> Unit)? = null,
    showSpectrum: Boolean,
    showAddToQueue: Boolean
) {
    val context = LocalContext.current
    val settingsManager = remember(context) { SettingsManager.getInstance(context) }
    val neteaseLinks = rememberNeteaseSongLinks(song)
    val neteaseMvId = neteaseLinks.mvId
    val pluginMvId = song?.let {
        it.onlineMvId.takeIf { mvId ->
            com.voxyn.halcyon.data.sourceplugin.sourcePluginKeyOf(it.onlineSource).isNotBlank() && mvId.isNotBlank()
        }
    }.orEmpty()
    val savedLayout by settingsManager.listActionMenuLayout.collectAsState(initial = "")
    val visibleActions = remember(savedLayout, song) {
        ActionMenuLayout.parse(savedLayout, ActionMenuIds.listDefaults)
            .visibleIds(ActionMenuIds.listDefaults)
            // Spectrum, rating, tag editing and lyric timing need a local file.
            .filter { ActionMenuIds.isAvailableFor(it, song) }
    }
    EllaMiuixSheetColumn(
        verticalPadding = 8.dp,
        spacing = 12.dp,
        showHandle = false
    ) {
        extraTopContent?.invoke(this)
        EllaMiuixActionMenuGroup {
            visibleActions.forEach { actionId ->
                when (actionId) {
                ActionMenuIds.SONG_WIKI -> if (neteaseLinks.songId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.netease_link_song_wiki), {
                        onDismiss()
                        com.voxyn.halcyon.data.netease.NeteaseLinks.open(context, com.voxyn.halcyon.data.netease.NeteaseLinkKind.SongWiki, neteaseLinks.songId)
                    }, icon = actionMenuIcon(actionId))
                }
                ActionMenuIds.SONG_COMMENTS -> if (neteaseLinks.songId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.player_view_song_comments), {
                        onDismiss()
                        com.voxyn.halcyon.data.netease.NeteaseLinks.open(context, com.voxyn.halcyon.data.netease.NeteaseLinkKind.Comment, neteaseLinks.songId)
                    }, icon = actionMenuIcon(actionId))
                }
                ActionMenuIds.VIEW_MV -> if (neteaseMvId.isNotBlank() || pluginMvId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.player_view_music_video), {
                        onDismiss()
                        if (pluginMvId.isNotBlank()) {
                            com.voxyn.halcyon.MusicVideoLauncher.openSourcePlugin(
                                context, song,
                                com.voxyn.halcyon.data.sourceplugin.sourcePluginKeyOf(song?.onlineSource.orEmpty()),
                                pluginMvId
                            )
                        } else {
                            com.voxyn.halcyon.MusicVideoLauncher.openNetease(context, song, neteaseMvId)
                        }
                    }, icon = actionMenuIcon(actionId))
                }
                ActionMenuIds.MV_COMMENTS -> if (neteaseMvId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.player_view_mv_comments), {
                        onDismiss()
                        com.voxyn.halcyon.data.netease.NeteaseLinks.open(context, com.voxyn.halcyon.data.netease.NeteaseLinkKind.MusicVideoComment, neteaseMvId)
                    }, icon = actionMenuIcon(actionId))
                }
                ActionMenuIds.REMOVE_FROM_RECENT_PLAYBACK,
                ActionMenuIds.DELETE_SINGLE_RECENT_PLAYBACK -> onDeleteSingleRecentPlayback?.let {
                    SongMenuItem(
                        stringResource(R.string.recent_playback_remove_from_recent),
                        it,
                        danger = true,
                        icon = actionMenuIcon(ActionMenuIds.REMOVE_FROM_RECENT_PLAYBACK)
                    )
                }
                ActionMenuIds.CLEAR_RECENT_PLAYBACK -> onClearRecentPlayback?.let {
                    SongMenuItem(
                        stringResource(R.string.recent_playback_remove_from_recent),
                        it,
                        danger = true,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.ADD_TO_PLAYLIST -> SongMenuItem(
                    stringResource(R.string.song_more_add_to_playlist),
                    onAddToPlaylist,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.ADD_TO_QUEUE -> if (showAddToQueue) {
                    SongMenuItem(
                        stringResource(R.string.common_add_to_queue),
                        onAddToQueue,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.PLAY_NEXT -> SongMenuItem(
                    stringResource(R.string.song_more_play_next),
                    onPlayNext,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.DOWNLOAD -> when {
                    song.onlineSource == "netease" -> {
                        SongMenuItem(stringResource(R.string.netease_download_song), {
                            com.voxyn.halcyon.data.netease.NeteaseDownloadService.enqueue(context, song)
                            onDismiss()
                        }, icon = actionMenuIcon(actionId))
                    }
                    com.voxyn.halcyon.data.sourceplugin.sourcePluginKeyOf(song.onlineSource).isNotBlank() -> {
                        SongMenuItem(stringResource(R.string.netease_download_song), {
                            com.voxyn.halcyon.data.sourceplugin.SourcePluginDownloadService.enqueue(context, song)
                            onDismiss()
                        }, icon = actionMenuIcon(actionId))
                    }
                }
                ActionMenuIds.DOWNLOAD_MV -> if (neteaseMvId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.netease_download_mv), {
                        com.voxyn.halcyon.data.netease.NeteaseDownloadService.enqueue(context, song, neteaseMvId)
                        onDismiss()
                    }, icon = actionMenuIcon(actionId))
                } else if (pluginMvId.isNotBlank()) {
                    SongMenuItem(stringResource(R.string.netease_download_mv), {
                        com.voxyn.halcyon.data.sourceplugin.SourcePluginDownloadService.enqueueMv(context, song, pluginMvId)
                        onDismiss()
                    }, icon = actionMenuIcon(actionId))
                }
                ActionMenuIds.SHARE -> SongMenuItem(
                    stringResource(R.string.common_share),
                    onShare,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.SPECTRUM -> if (showSpectrum) {
                    SongMenuItem(
                        stringResource(R.string.song_more_view_spectrum),
                        onSpectrum,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.AI -> SongMenuItem(
                    stringResource(R.string.song_more_ai_title),
                    onAiInterpret,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.INFO -> SongMenuItem(
                    stringResource(R.string.song_more_view_song_info),
                    onInfo,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.RATING -> SongMenuItem(
                    stringResource(R.string.song_more_set_rating),
                    onRating,
                    icon = actionMenuIcon(actionId)
                )
                ActionMenuIds.EDIT_TAGS -> onEditTag?.let {
                    SongMenuItem(
                        stringResource(R.string.song_more_edit_tags_title),
                        it,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.LYRIC_TIMING -> onLyricTiming?.let {
                    SongMenuItem(
                        stringResource(R.string.song_more_lyric_timing),
                        it,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.AUDIO_TOOLS -> onAudioTools?.let {
                    SongMenuItem(
                        stringResource(R.string.song_more_audio_tools),
                        it,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.REMOVE_FROM_PLAYLIST -> onRemoveFromPlaylist?.let {
                    SongMenuItem(
                        stringResource(R.string.playlist_remove_song_title),
                        it,
                        danger = true,
                        icon = actionMenuIcon(actionId)
                    )
                }
                ActionMenuIds.DELETE -> onDelete?.let {
                    SongMenuItem(
                        stringResource(R.string.song_more_delete_permanently),
                        it,
                        danger = true,
                        icon = actionMenuIcon(actionId)
                    )
                }
                }
            }
        }
    }
}

@Composable
internal fun SongTagEditorSheet(
    song: Song,
    options: List<TagEditorOption>,
    onDismiss: () -> Unit,
    onOptionClick: (TagEditorOption) -> Unit
) {
    EllaMiuixSheetColumn(
        verticalPadding = 8.dp,
        spacing = 0.dp,
        showHandle = false
    ) {
        EllaMiuixActionMenuGroup {
            options.forEach { option ->
                SongMenuItem(option.label, onClick = { onOptionClick(option) })
            }
        }
    }
}
