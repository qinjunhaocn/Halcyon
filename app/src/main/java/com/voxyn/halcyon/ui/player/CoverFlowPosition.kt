package com.voxyn.halcyon.ui.player

import com.voxyn.halcyon.data.model.playlistIdentityKey

/** Prefer the queue cursor so repeated occurrences of one song do not jump back to the first. */
internal fun resolveCoverFlowQueueIndex(
    songs: List<com.voxyn.halcyon.data.model.Song>, songKey: String?, queueIndex: Int
): Int = queueIndex.takeIf {
    songs.getOrNull(it)?.playlistIdentityKey() == songKey && it in songs.indices
} ?: songs.indexOfFirst { it.playlistIdentityKey() == songKey }.coerceAtLeast(0)
