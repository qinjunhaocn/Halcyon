package com.voxyn.halcyon.ui.components

import com.voxyn.halcyon.data.model.AudioInfo
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.albumIdentityId
import com.voxyn.halcyon.data.splitArtistNames
import com.voxyn.halcyon.data.splitGenreNames
import com.voxyn.halcyon.ui.analytics.formatLabel
import com.voxyn.halcyon.ui.analytics.qualityLabel
import com.voxyn.halcyon.ui.navigation.Screen
import com.voxyn.halcyon.viewmodel.extractYear
import com.voxyn.halcyon.viewmodel.parentFolderPath

internal enum class SongInfoJump {
    Path,
    Directory,
    Format,
    Bitrate,
    Artist,
    Album,
    AlbumArtist,
    Genre,
    Year,
    Composer,
    Arranger,
    Lyricist,
    Producer
}

internal fun songInfoJumpChoices(jump: SongInfoJump, value: String): List<String> {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return emptyList()
    return when (jump) {
        SongInfoJump.Artist, SongInfoJump.AlbumArtist, SongInfoJump.Composer,
        SongInfoJump.Arranger, SongInfoJump.Lyricist, SongInfoJump.Producer -> splitArtistNames(trimmed)
        SongInfoJump.Genre -> splitGenreNames(trimmed)
        else -> listOf(trimmed)
    }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
}

internal fun songInfoJumpRoute(
    jump: SongInfoJump,
    song: Song,
    audioInfo: AudioInfo? = null,
    chosenValue: String = ""
): String? {
    val chosen = chosenValue.trim()
    return when (jump) {
        SongInfoJump.Path -> song.parentFolderPath()?.let {
            Screen.MetadataCategoryDetail.createRoute("folder", it)
        }
        SongInfoJump.Directory -> song.parentFolderPath()?.let {
            Screen.FolderDetail.createRoute(it)
        }
        SongInfoJump.Format -> audioInfo?.let {
            Screen.LibraryAnalysis.createBucketRoute(quality = false, label = formatLabel(song, it))
        }
        SongInfoJump.Bitrate -> audioInfo?.let {
            Screen.LibraryAnalysis.createBucketRoute(quality = true, label = qualityLabel(song, it))
        }
        SongInfoJump.Artist, SongInfoJump.AlbumArtist -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.ArtistDetail.createRoute(it)
        }
        SongInfoJump.Album -> song.albumIdentityId().takeIf { it > 0L }?.let {
            Screen.AlbumDetail.createRoute(it)
        }
        SongInfoJump.Genre -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.MetadataCategoryDetail.createRoute("genre", it)
        }
        SongInfoJump.Year -> (chosen.extractYear() ?: chosen.takeIf { it.isNotBlank() })?.let {
            Screen.MetadataCategoryDetail.createRoute("year", it)
        }
        SongInfoJump.Composer -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.MetadataCategoryDetail.createRoute("composer", it)
        }
        SongInfoJump.Arranger -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.MetadataCategoryDetail.createRoute("arranger", it)
        }
        SongInfoJump.Lyricist -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.MetadataCategoryDetail.createRoute("lyricist", it)
        }
        SongInfoJump.Producer -> chosen.takeIf { it.isNotBlank() }?.let {
            Screen.MetadataCategoryDetail.createRoute("producer", it)
        }
    }
}
