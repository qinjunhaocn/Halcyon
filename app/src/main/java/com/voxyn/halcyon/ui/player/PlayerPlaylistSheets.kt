package com.voxyn.halcyon.ui.player

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.model.FAVORITES_PLAYLIST_ID
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.UserPlaylist
import com.voxyn.halcyon.ui.components.AddToPlaylistSheet
import com.voxyn.halcyon.ui.components.CreatePlaylistAndAddSheet
import com.voxyn.halcyon.ui.components.EllaMiuixBottomSheet
import com.voxyn.halcyon.ui.components.createPlaylistOrShowDuplicateToast
import com.voxyn.halcyon.viewmodel.MainViewModel

@Composable
internal fun PlayerPlaylistSheets(
    context: Context,
    mainViewModel: MainViewModel,
    playlists: List<UserPlaylist>,
    playlistPickerSong: Song?,
    onPlaylistPickerSongChange: (Song?) -> Unit,
    playlistPickerSongs: List<Song>?,
    onPlaylistPickerSongsChange: (List<Song>?) -> Unit,
    createPlaylistSong: Song?,
    onCreatePlaylistSongChange: (Song?) -> Unit,
    createPlaylistSongs: List<Song>?,
    onCreatePlaylistSongsChange: (List<Song>?) -> Unit
) {
    playlistPickerSong?.let { currentSong ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_add_to_playlist),
            onDismissRequest = { onPlaylistPickerSongChange(null) }
        ) {
            AddToPlaylistSheet(
                playlists = playlists.sortedForPlayerSheet(),
                songsToAdd = listOf(currentSong),
                onDismiss = { onPlaylistPickerSongChange(null) },
                onCreatePlaylist = {
                    onCreatePlaylistSongChange(currentSong)
                    onPlaylistPickerSongChange(null)
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, listOf(currentSong), appendToEnd)
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlists, selectedPlaylists.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    onPlaylistPickerSongChange(null)
                }
            )
        }
    }

    playlistPickerSongs?.let { songsToAdd ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_add_to_playlist),
            onDismissRequest = { onPlaylistPickerSongsChange(null) }
        ) {
            AddToPlaylistSheet(
                playlists = playlists.sortedForPlayerSheet(),
                songsToAdd = songsToAdd,
                onDismiss = { onPlaylistPickerSongsChange(null) },
                onCreatePlaylist = {
                    onCreatePlaylistSongsChange(songsToAdd)
                    onPlaylistPickerSongsChange(null)
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd, appendToEnd)
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlists, selectedPlaylists.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    onPlaylistPickerSongsChange(null)
                }
            )
        }
    }

    createPlaylistSong?.let { currentSong ->
        CreatePlaylistAndAddSheet(
            onDismiss = { onCreatePlaylistSongChange(null) },
            onCreate = { name ->
                mainViewModel.createPlaylistOrShowDuplicateToast(context, name) { playlist ->
                    mainViewModel.addSongsToPlaylist(playlist.id, listOf(currentSong))
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlist_named, playlist.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    onCreatePlaylistSongChange(null)
                }
            }
        )
    }

    createPlaylistSongs?.let { songsToAdd ->
        CreatePlaylistAndAddSheet(
            onDismiss = { onCreatePlaylistSongsChange(null) },
            onCreate = { name ->
                mainViewModel.createPlaylistOrShowDuplicateToast(context, name) { playlist ->
                    mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd)
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlist_named, playlist.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    onCreatePlaylistSongsChange(null)
                }
            }
        )
    }
}

private fun List<UserPlaylist>.sortedForPlayerSheet(): List<UserPlaylist> =
    sortedWith(
        compareByDescending<UserPlaylist> { it.id == FAVORITES_PLAYLIST_ID }
            .thenByDescending { it.createdAt }
    )
