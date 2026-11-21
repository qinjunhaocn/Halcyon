package com.voxyn.halcyon.ui.search

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.UserPlaylist
import com.voxyn.halcyon.ui.components.ActionMenuCommonIcons
import com.voxyn.halcyon.ui.components.AddToPlaylistSheet
import com.voxyn.halcyon.ui.components.ConfirmDangerDialog
import com.voxyn.halcyon.ui.components.EllaMiuixBottomSheet
import com.voxyn.halcyon.ui.components.LibraryEntityAction
import com.voxyn.halcyon.ui.components.LibraryEntityActionSheet
import com.voxyn.halcyon.ui.components.LibraryEntityActions
import com.voxyn.halcyon.ui.components.SongMoreActionHost
import com.voxyn.halcyon.ui.components.createPlaylistOrShowDuplicateToast
import com.voxyn.halcyon.ui.components.requestPinnedEllaShortcut
import com.voxyn.halcyon.ui.components.shareLocalSongs
import com.voxyn.halcyon.ui.folder.FolderBlockDialog
import com.voxyn.halcyon.ui.folder.LinkToFolderPlaylistSheet
import com.voxyn.halcyon.ui.folder.normalizeFolderPath
import com.voxyn.halcyon.viewmodel.MainViewModel
import com.voxyn.halcyon.viewmodel.PlayerViewModel
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun LibrarySearchAuxiliarySurfaces(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    settingsManager: SettingsManager,
    allSongs: List<Song>,
    playlists: List<UserPlaylist>,
    blockedFolders: List<String>,
    actionSong: Song?,
    onActionSongChange: (Song?) -> Unit,
    actionTarget: SearchActionTarget?,
    onActionTargetChange: (SearchActionTarget?) -> Unit,
    playlistPickerSongs: List<Song>?,
    onPlaylistPickerSongsChange: (List<Song>?) -> Unit,
    createPlaylistSongs: List<Song>?,
    onCreatePlaylistSongsChange: (List<Song>?) -> Unit,
    showClearHistoryConfirm: Boolean,
    onShowClearHistoryConfirmChange: (Boolean) -> Unit,
    onClearHistoryConfirmed: () -> Unit,
    pendingDeleteSongs: List<Song>,
    onPendingDeleteSongsChange: (List<Song>) -> Unit,
    onRequestDeleteSongs: (List<Song>) -> Unit,
    onFinishSelectionMode: () -> Unit,
    songsForActionTarget: (SearchActionTarget) -> List<Song>,
    shortcutIdForActionTarget: (SearchActionTarget) -> String,
    shortcutRouteForActionTarget: (SearchActionTarget) -> String,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderToBlock by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val folderPlaylists by settingsManager.folderPlaylists.collectAsState(initial = emptyList())
    var associateFolderPath by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var batchEditTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SearchActionTarget?>(null) }

    actionTarget?.let { target ->
        val imageSearchName = when (target) {
            is SearchActionTarget.ArtistTarget -> target.artist.name
            is SearchActionTarget.CategoryTarget ->
                target.item.name.takeIf { target.type in com.voxyn.halcyon.ui.category.ARTIST_IMAGE_CATEGORY_TYPES }
            else -> null
        }
        val batchSpecs = when (target) {
            is SearchActionTarget.AlbumTarget -> com.voxyn.halcyon.ui.category.ALBUM_BATCH_FIELD_SPECS
            is SearchActionTarget.ArtistTarget -> com.voxyn.halcyon.ui.category.ARTIST_BATCH_FIELD_SPECS
            is SearchActionTarget.CategoryTarget -> com.voxyn.halcyon.ui.category.batchFieldSpecs(target.type, target.item.name)
            else -> emptyList()
        }
        val isFolder = target is SearchActionTarget.CategoryTarget && target.type == "folder"
        LibraryEntityActionSheet(
            title = target.title,
            onDismissRequest = { onActionTargetChange(null) },
            actions = listOfNotNull(
                LibraryEntityActions.share {
                    shareLocalSongs(context, songsForActionTarget(target))
                    onActionTargetChange(null)
                },
                if (isFolder) {
                    LibraryEntityActions.associate {
                        associateFolderPath = target.item.name.normalizeFolderPath()
                        onActionTargetChange(null)
                    }
                } else {
                    null
                },
                LibraryEntityActions.addToPlaylist {
                    onPlaylistPickerSongsChange(songsForActionTarget(target))
                    onActionTargetChange(null)
                },
                LibraryEntityActions.addToQueue {
                    playerViewModel.addToPlaylist(songsForActionTarget(target))
                    Toast.makeText(context, context.getString(R.string.song_more_added_to_queue), Toast.LENGTH_SHORT).show()
                    onActionTargetChange(null)
                },
                LibraryEntityActions.playNext {
                    playerViewModel.playNext(songsForActionTarget(target))
                    Toast.makeText(context, context.getString(R.string.song_more_added_to_play_next), Toast.LENGTH_SHORT).show()
                    onActionTargetChange(null)
                },
                LibraryEntityActions.desktopShortcut {
                    val ok = requestPinnedEllaShortcut(
                        context = context,
                        id = shortcutIdForActionTarget(target),
                        label = target.title,
                        route = shortcutRouteForActionTarget(target)
                    )
                    Toast.makeText(
                        context,
                        if (ok) context.getString(R.string.playlist_shortcut_requested, target.title) else context.getString(R.string.playlist_shortcut_unsupported),
                        Toast.LENGTH_SHORT
                    ).show()
                    onActionTargetChange(null)
                },
                if (isFolder) {
                    LibraryEntityActions.block {
                        folderToBlock = target.item.name.normalizeFolderPath()
                        onActionTargetChange(null)
                    }
                } else {
                    null
                },
                imageSearchName?.let { name ->
                    LibraryEntityActions.searchImage {
                        com.voxyn.halcyon.ArtistImageSearchActivity.open(context, name)
                        onActionTargetChange(null)
                    }
                },
                if (batchSpecs.isNotEmpty()) {
                    LibraryEntityAction(
                        title = stringResource(R.string.category_batch_edit),
                        icon = ActionMenuCommonIcons.edit,
                        onClick = {
                            batchEditTarget = target
                            onActionTargetChange(null)
                        }
                    )
                } else {
                    null
                },
                LibraryEntityActions.deletePermanently {
                    onPendingDeleteSongsChange(songsForActionTarget(target))
                    onActionTargetChange(null)
                },
            )
        )
    }

    associateFolderPath?.let { folderPath ->
        LinkToFolderPlaylistSheet(
            show = true,
            songs = allSongs,
            selectedFolderCount = 1,
            folderPlaylists = folderPlaylists,
            onDismiss = { associateFolderPath = null },
            onLink = { targets ->
                scope.launch {
                    targets.forEach { target ->
                        mainViewModel.settingsManager.upsertFolderPlaylist(
                            target.id,
                            target.name,
                            (target.folders + folderPath).distinctBy { it.lowercase() }
                        )
                    }
                    Toast.makeText(
                        context,
                        if (targets.size == 1) {
                            context.getString(R.string.folder_playlist_associate_done, targets.first().name)
                        } else {
                            context.getString(R.string.folder_playlist_associate_multi_done, targets.size)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                associateFolderPath = null
            },
            onCreatePlaylist = { name ->
                scope.launch {
                    mainViewModel.settingsManager.upsertFolderPlaylist(null, name, listOf(folderPath))
                }
                associateFolderPath = null
            }
        )
    }

    folderToBlock?.let { folderPath ->
        FolderBlockDialog(
            folderPath = folderPath,
            onDismiss = { folderToBlock = null },
            onBlock = {
                scope.launch {
                    val nextBlockedFolders = (blockedFolders + folderPath)
                        .distinctBy { it.normalizeFolderPath().lowercase(Locale.ROOT) }
                    settingsManager.setScanExcludeFolders(nextBlockedFolders.joinToString("；"))
                    mainViewModel.scanMusic()
                }
                folderToBlock = null
            }
        )
    }

    batchEditTarget?.let { batchTarget ->
        val specs = when (batchTarget) {
            is SearchActionTarget.AlbumTarget -> com.voxyn.halcyon.ui.category.ALBUM_BATCH_FIELD_SPECS
            is SearchActionTarget.ArtistTarget -> com.voxyn.halcyon.ui.category.ARTIST_BATCH_FIELD_SPECS
            is SearchActionTarget.CategoryTarget -> com.voxyn.halcyon.ui.category.batchFieldSpecs(batchTarget.type, batchTarget.item.name)
            else -> emptyList()
        }
        val batchType = when (batchTarget) {
            is SearchActionTarget.AlbumTarget -> "album"
            is SearchActionTarget.ArtistTarget -> "artist"
            is SearchActionTarget.CategoryTarget -> batchTarget.type
            else -> "search"
        }
        if (specs.isNotEmpty()) {
            com.voxyn.halcyon.ui.category.BatchTagEditSheet(
                type = batchType,
                category = batchTarget.title,
                songs = songsForActionTarget(batchTarget),
                specs = specs,
                mainViewModel = mainViewModel,
                onDismiss = { batchEditTarget = null }
            )
        }
    }

    playlistPickerSongs?.let { songsToAdd ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_add_to_playlist_title),
            onDismissRequest = { onPlaylistPickerSongsChange(null) }
        ) {
            AddToPlaylistSheet(
                playlists = playlists,
                songsToAdd = songsToAdd,
                songCount = songsToAdd.size,
                onDismiss = { onPlaylistPickerSongsChange(null) },
                onCreatePlaylist = {
                    onCreatePlaylistSongsChange(songsToAdd)
                    onPlaylistPickerSongsChange(null)
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd, appendToEnd)
                    }
                    onPlaylistPickerSongsChange(null)
                    Toast.makeText(context, context.getString(R.string.player_added_to_playlists, selectedPlaylists.size), Toast.LENGTH_SHORT).show()
                    onFinishSelectionMode()
                }
            )
        }
    }

    createPlaylistSongs?.let { songsToAdd ->
        com.voxyn.halcyon.ui.playlist.CreatePlaylistDialog(
            onDismiss = { onCreatePlaylistSongsChange(null) },
            onCreate = { playlistName ->
                mainViewModel.createPlaylistOrShowDuplicateToast(context, playlistName) { playlist ->
                    mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd)
                    onCreatePlaylistSongsChange(null)
                    onFinishSelectionMode()
                }
            }
        )
    }

    SongMoreActionHost(
        actionSong = actionSong,
        mainViewModel = mainViewModel,
        playerViewModel = playerViewModel,
        onDismissAction = { onActionSongChange(null) },
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist
    )

    ConfirmDangerDialog(
        show = showClearHistoryConfirm,
        title = stringResource(R.string.library_search_clear_history_title),
        message = stringResource(R.string.library_search_clear_history_message),
        confirmText = stringResource(R.string.common_clear),
        onDismiss = { onShowClearHistoryConfirmChange(false) },
        onConfirm = {
            onClearHistoryConfirmed()
            onShowClearHistoryConfirmChange(false)
        }
    )

    ConfirmDangerDialog(
        show = pendingDeleteSongs.isNotEmpty(),
        title = stringResource(R.string.song_more_delete_song_title),
        message = stringResource(R.string.library_delete_selected_message, pendingDeleteSongs.size),
        confirmText = stringResource(R.string.song_more_delete_permanently),
        onDismiss = { onPendingDeleteSongsChange(emptyList()) },
        onConfirm = {
            onRequestDeleteSongs(pendingDeleteSongs)
            onPendingDeleteSongsChange(emptyList())
            onFinishSelectionMode()
        }
    )
}
