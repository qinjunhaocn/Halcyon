package com.voxyn.halcyon.ui.components

import android.content.Context
import android.widget.Toast
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.model.UserPlaylist
import com.voxyn.halcyon.viewmodel.MainViewModel

internal fun MainViewModel.createPlaylistOrShowDuplicateToast(
    context: Context,
    name: String,
    onCreated: (UserPlaylist) -> Unit
) {
    if (name.isBlank()) return
    createPlaylist(name) { playlist ->
        if (playlist == null) {
            Toast.makeText(context, R.string.playlist_name_exists, Toast.LENGTH_SHORT).show()
        } else {
            onCreated(playlist)
        }
    }
}
