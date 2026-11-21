package com.voxyn.halcyon.viewmodel

import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.playlistIdentityKey
import com.voxyn.halcyon.player.LyriconBridge
import com.voxyn.halcyon.player.SuperLyricBridge

internal fun replayGainPrefetchSongs(
    playlist: List<Song>,
    currentSong: Song,
    count: Int
): List<Song> {
    if (playlist.isEmpty() || count <= 0) return emptyList()
    val currentKey = currentSong.playlistIdentityKey()
    val currentIndex = playlist.indexOfFirst { it.playlistIdentityKey() == currentKey }
    if (currentIndex < 0) return emptyList()
    return (1..count.coerceAtMost(playlist.lastIndex))
        .map { offset -> playlist[(currentIndex + offset) % playlist.size] }
        .filterNot { it.playlistIdentityKey() == currentKey }
        .distinctBy { it.playlistIdentityKey() }
}

internal fun lyriconSecondaryMode(
    translationEnabled: Boolean,
    pronunciationEnabled: Boolean
): LyriconBridge.SecondaryMode = when {
    pronunciationEnabled -> LyriconBridge.SecondaryMode.Pronunciation
    translationEnabled -> LyriconBridge.SecondaryMode.Translation
    else -> LyriconBridge.SecondaryMode.Off
}

internal fun superLyricSecondaryMode(
    translationEnabled: Boolean,
    pronunciationEnabled: Boolean
): SuperLyricBridge.SecondaryMode = when {
    pronunciationEnabled -> SuperLyricBridge.SecondaryMode.Pronunciation
    translationEnabled -> SuperLyricBridge.SecondaryMode.Translation
    else -> SuperLyricBridge.SecondaryMode.Off
}

internal fun shouldReplayFromPreviousButton(
    manualSeekAfterPreviousButton: Boolean,
    previousButtonAction: Int,
    currentPositionMs: Long
): Boolean =
    !manualSeekAfterPreviousButton &&
        previousButtonAction == SettingsManager.PREVIOUS_BUTTON_REPLAY_CURRENT &&
        currentPositionMs >= SettingsManager.PREVIOUS_REPLAY_THRESHOLD_MS
