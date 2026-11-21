package com.voxyn.halcyon.ui.player

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.exception.WritePermissionRequiredException
import com.voxyn.halcyon.data.model.LyricLine
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.UserPlaylist
import com.voxyn.halcyon.ui.components.ArtistPickerContent
import com.voxyn.halcyon.ui.components.EllaMiuixBottomSheet
import com.voxyn.halcyon.ui.components.LyricSharePicker
import com.voxyn.halcyon.ui.components.LyricShareOptions
import com.voxyn.halcyon.ui.components.SongAiInterpretationSheet
import com.voxyn.halcyon.ui.components.SongInfoSheet
import com.voxyn.halcyon.ui.components.SongMoreTagActionSheets
import com.voxyn.halcyon.ui.components.TagEditorOptionKind
import com.voxyn.halcyon.ui.components.openSongWithMediaInfo
import com.voxyn.halcyon.viewmodel.MainViewModel
import com.voxyn.halcyon.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun PlayerScreenSheetHost(
    context: android.content.Context,
    scope: CoroutineScope,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    song: Song?,
    playlists: List<UserPlaylist>,
    artistChoices: List<String>,
    onArtistChoicesChange: (List<String>) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    songInfoExpanded: Boolean,
    onSongInfoExpandedChange: (Boolean) -> Unit,
    dynamicCoverSheetSong: Song?,
    onDynamicCoverSheetSongChange: (Song?) -> Unit,
    ratingSheetSong: Song?,
    onRatingSheetSongChange: (Song?) -> Unit,
    aiSheetSong: Song?,
    onAiSheetSongChange: (Song?) -> Unit,
    deleteConfirmSong: Song?,
    onDeleteConfirmSongChange: (Song?) -> Unit,
    lyricMatchSong: Song?,
    onLyricMatchSongChange: (Song?) -> Unit,
    tagEditorSong: Song?,
    onTagEditorSongChange: (Song?) -> Unit,
    tagEditorKind: TagEditorOptionKind,
    onTagEditorKindChange: (TagEditorOptionKind) -> Unit,
    metadataEditorId: String,
    lyricTimingEditorId: String,
    metadataEditorSong: Song?,
    onMetadataEditorSongChange: (Song?) -> Unit,
    lyricTimingEditorSong: Song?,
    onLyricTimingEditorSongChange: (Song?) -> Unit,
    onWritePermissionRequired: (WritePermissionRequiredException, suspend () -> Unit) -> Unit,
    playlistPickerSong: Song?,
    onPlaylistPickerSongChange: (Song?) -> Unit,
    playlistPickerSongs: List<Song>?,
    onPlaylistPickerSongsChange: (List<Song>?) -> Unit,
    createPlaylistSong: Song?,
    onCreatePlaylistSongChange: (Song?) -> Unit,
    createPlaylistSongs: List<Song>?,
    onCreatePlaylistSongsChange: (List<Song>?) -> Unit
) {
    if (artistChoices.isNotEmpty()) {
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_select_artist),
            onDismissRequest = { onArtistChoicesChange(emptyList()) }
        ) {
            ArtistPickerContent(
                artists = artistChoices,
                mainViewModel = mainViewModel,
                onArtistSelected = { artist ->
                    onArtistChoicesChange(emptyList())
                    onNavigateToArtist(artist)
                },
                onDismiss = { onArtistChoicesChange(emptyList()) }
            )
        }
    }

    if (songInfoExpanded && song != null) {
        val pickerTitle = androidx.compose.runtime.remember(song.id) { androidx.compose.runtime.mutableStateOf<String?>(null) }
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = pickerTitle.value ?: stringResource(R.string.player_song_info),
            onDismissRequest = { onSongInfoExpandedChange(false) }
        ) {
            SongInfoSheet(
                onPickerTitleChange = { pickerTitle.value = it },
                mainViewModel = mainViewModel,
                song = song,
                audioInfoLoader = playerViewModel::getAudioInfo,
                tagInfoLoader = playerViewModel::getSongTagInfo,
                onOpenMediaInfo = {
                    onSongInfoExpandedChange(false)
                    openSongWithMediaInfo(context, song)
                },
                onDismiss = { onSongInfoExpandedChange(false) },
                onUpdateModifiedTime = { millis ->
                    song?.let { mainViewModel.updateSongModifiedTime(it, millis) } ?: false
                }
            )
        }
    }

    if (dynamicCoverSheetSong != null) {
        DynamicCoverWebViewSheet(
            show = true,
            song = dynamicCoverSheetSong,
            onDismissRequest = { onDynamicCoverSheetSongChange(null) }
        )
    }

    PlayerLibraryActionSheets(
        context = context,
        scope = scope,
        mainViewModel = mainViewModel,
        ratingSheetSong = ratingSheetSong,
        onRatingSheetSongChange = onRatingSheetSongChange,
        deleteConfirmSong = deleteConfirmSong,
        onDeleteConfirmSongChange = onDeleteConfirmSongChange,
        onWritePermissionRequired = onWritePermissionRequired
    )

    SongMoreTagActionSheets(
        context = context,
        scope = scope,
        mainViewModel = mainViewModel,
        playerViewModel = playerViewModel,
        tagEditorSong = tagEditorSong,
        onTagEditorSongChange = onTagEditorSongChange,
        tagEditorKind = tagEditorKind,
        metadataEditorId = metadataEditorId,
        lyricTimingEditorId = lyricTimingEditorId,
        editTagTitle = stringResource(R.string.song_more_edit_tags_title),
        lyricTimingTitle = stringResource(R.string.song_more_lyric_timing),
        metadataEditorSong = metadataEditorSong,
        onMetadataEditorSongChange = onMetadataEditorSongChange,
        lyricTimingEditorSong = lyricTimingEditorSong,
        onLyricTimingEditorSongChange = onLyricTimingEditorSongChange,
        onWritePermissionRequired = onWritePermissionRequired
    )

    aiSheetSong?.let { currentSong ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_ai_title),
            onDismissRequest = { onAiSheetSongChange(null) }
        ) {
            SongAiInterpretationSheet(
                song = currentSong,
                mainViewModel = mainViewModel,
                onDismiss = { onAiSheetSongChange(null) }
            )
        }
    }

    lyricMatchSong?.let { currentSong ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_match_online_lyrics),
            onDismissRequest = { onLyricMatchSongChange(null) }
        ) {
            PluginLyricsMatchSheet(
                song = currentSong,
                mainViewModel = mainViewModel,
                onWriteMetadata = { targetSong, tags ->
                    playerViewModel.writeMetadataWithoutInterruptingPlayback(targetSong) {
                        mainViewModel.writeSongMetadata(targetSong, tags)
                    }
                },
                onDismiss = { onLyricMatchSongChange(null) },
                onWritePermissionRequired = onWritePermissionRequired,
                onSongUpdated = {},
                onSidecarSaved = { savedSong ->
                    val current = playerViewModel.currentSong.value
                    if (current != null && current.path == savedSong.path) {
                        playerViewModel.reloadCurrentLyrics()
                    }
                }
            )
        }
    }

    PlayerPlaylistSheets(
        context = context,
        mainViewModel = mainViewModel,
        playlists = playlists,
        playlistPickerSong = playlistPickerSong,
        onPlaylistPickerSongChange = onPlaylistPickerSongChange,
        playlistPickerSongs = playlistPickerSongs,
        onPlaylistPickerSongsChange = onPlaylistPickerSongsChange,
        createPlaylistSong = createPlaylistSong,
        onCreatePlaylistSongChange = onCreatePlaylistSongChange,
        createPlaylistSongs = createPlaylistSongs,
        onCreatePlaylistSongsChange = onCreatePlaylistSongsChange
    )
}

@Composable
internal fun PlayerLyricShareHost(
    request: LyricShareRequest?,
    onDismiss: () -> Unit,
    onShare: (List<LyricLine>, LyricShareOptions) -> Unit,
    onCopy: (List<LyricLine>, LyricShareOptions) -> Unit,
    onSaveImage: (List<LyricLine>, LyricShareOptions) -> Unit,
    onVideoShare: ((List<LyricLine>, LyricShareOptions) -> Unit)? = null
) {
    request?.let { shareRequest ->
        LyricSharePicker(
            song = shareRequest.song,
            lyrics = shareRequest.lyrics,
            initialLine = shareRequest.initialLine,
            cover = shareRequest.cover,
            backgroundColors = shareRequest.backgroundColors,
            contentColor = shareRequest.contentColor,
            annotation = shareRequest.annotation,
            customInfo = shareRequest.customInfo,
            shareTypeface = shareRequest.shareTypeface,
            onDismiss = onDismiss,
            onShare = onShare,
            onCopy = onCopy,
            onSaveImage = onSaveImage,
            onVideoShare = onVideoShare
        )
    }
}

/** Immutable snapshot used by lyric sharing so a track switch cannot mutate the picker. */
internal data class LyricShareRequest(
    val song: Song?,
    val lyrics: List<LyricLine>,
    val initialLine: LyricLine,
    val cover: Bitmap?,
    val backgroundColors: List<Color>,
    val contentColor: Color,
    val annotation: String,
    val customInfo: String,
    val exportFolderUri: String,
    val shareTypeface: Typeface?
)
