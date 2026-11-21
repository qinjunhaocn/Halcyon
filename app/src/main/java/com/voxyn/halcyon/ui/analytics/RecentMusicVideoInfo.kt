package com.voxyn.halcyon.ui.analytics

import android.net.Uri
import androidx.compose.runtime.Composable
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.ui.player.DynamicCoverSource
import com.voxyn.halcyon.ui.player.MusicVideoInfoDialog

@Composable
internal fun RecentMusicVideoInfo(song: Song, onDismiss: () -> Unit) {
    MusicVideoInfoDialog(DynamicCoverSource(Uri.parse(song.path), song.path), song.title, onDismiss)
}
