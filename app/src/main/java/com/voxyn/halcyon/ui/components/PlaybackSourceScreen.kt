package com.voxyn.halcyon.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.voxyn.halcyon.data.PlaybackSourceNavigation

@Composable
internal fun RememberPlaybackSourceScreen(key: String) {
    DisposableEffect(key) {
        PlaybackSourceNavigation.setActiveScreen(key)
        onDispose { PlaybackSourceNavigation.clearActiveScreen(key) }
    }
}
