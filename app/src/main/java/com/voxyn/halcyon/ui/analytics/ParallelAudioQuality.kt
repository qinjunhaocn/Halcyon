package com.voxyn.halcyon.ui.analytics

import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.AudioInfo
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

internal suspend fun loadLibraryQualityRows(songs: List<Song>, load: (Song) -> AudioInfo): List<SongWithInfo> = coroutineScope {
    val next = AtomicInteger()
    val rows = arrayOfNulls<SongWithInfo>(songs.size)
    List(minOf(4, songs.size)) {
        async(Dispatchers.IO) {
            while (true) {
                ensureActive()
                val index = next.getAndIncrement()
                if (index >= songs.size) break
                rows[index] = SongWithInfo(songs[index], load(songs[index]))
            }
        }
    }.awaitAll()
    rows.map { requireNotNull(it) }
}
