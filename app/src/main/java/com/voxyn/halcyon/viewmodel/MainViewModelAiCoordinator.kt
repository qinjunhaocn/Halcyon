package com.voxyn.halcyon.viewmodel

import android.app.Application
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.PlaybackHistoryEntry
import com.voxyn.halcyon.data.SongPlaybackStats
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.ai.OpenAiLibraryChatAssistant
import com.voxyn.halcyon.data.ai.OpenAiLibraryChatInput
import com.voxyn.halcyon.data.ai.OpenAiPlaylistRecommendationInput
import com.voxyn.halcyon.data.ai.OpenAiPlaylistRecommender
import com.voxyn.halcyon.data.ai.OpenAiSongInterpretationConfig
import com.voxyn.halcyon.data.ai.OpenAiSongInterpretationInput
import com.voxyn.halcyon.data.ai.OpenAiSongInterpreter
import com.voxyn.halcyon.data.ai.resolveAiApiProtocol
import com.voxyn.halcyon.data.detailedAudioInfo
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.data.model.playlistIdentityKey
import com.voxyn.halcyon.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class MainViewModelAiCoordinator(
    private val application: Application,
    private val settingsManager: SettingsManager,
    private val repository: MusicRepository
) {
    private val songInterpreter = OpenAiSongInterpreter(application)
    private val playlistRecommender = OpenAiPlaylistRecommender(application)
    private val libraryChatAssistant = OpenAiLibraryChatAssistant(application)

    suspend fun interpretSong(song: Song): String = withContext(Dispatchers.IO) {
        val lyricSourceMode = settingsManager.lyricSourceMode.first()
        val tagInfo = repository.getSongTagInfo(song)
        val audioInfo = runCatching { repository.getAudioInfo(song) }.getOrNull()
        val lyrics = repository.getLyrics(song, lyricSourceMode)
        songInterpreter.interpret(
            config = openAiConfig(),
            input = OpenAiSongInterpretationInput(
                song = song,
                tagInfo = tagInfo,
                audioInfo = audioInfo,
                audioInfoText = audioInfo?.let { detailedAudioInfo(it) }.orEmpty(),
                lyrics = lyrics
            )
        )
    }

    suspend fun recommendPlaylist(
        librarySongs: List<Song>,
        playbackStats: List<SongPlaybackStats>,
        playbackHistory: List<PlaybackHistoryEntry>,
        maxItems: Int
    ): AiPlaylistRecommendationResult = withContext(Dispatchers.IO) {
        if (librarySongs.isEmpty()) error(application.getString(R.string.error_library_empty))

        val candidates = buildOpenAiRecommendationCandidates(
            library = librarySongs,
            stats = playbackStats,
            history = playbackHistory
        )
        val recommendation = playlistRecommender.recommend(
            config = openAiConfig(),
            input = OpenAiPlaylistRecommendationInput(
                songs = candidates,
                playbackStats = playbackStats,
                playbackHistory = playbackHistory,
                maxItems = maxItems.coerceIn(5, 50)
            )
        )
        val libraryByKey = librarySongs.associateBy { it.playlistIdentityKey() }
        val recommendedSongs = recommendation.songKeys
            .mapNotNull { key -> libraryByKey[key] }
            .distinctBy { it.playlistIdentityKey() }
            .take(maxItems.coerceAtLeast(1))
        if (recommendedSongs.isEmpty()) error(application.getString(R.string.error_ai_no_playable_songs))

        AiPlaylistRecommendationResult(
            title = recommendation.title.ifBlank { application.getString(R.string.ai_default_playlist_title) },
            reason = recommendation.reason,
            songs = recommendedSongs
        )
    }

    suspend fun chatWithLibrary(
        librarySongs: List<Song>,
        playbackStats: List<SongPlaybackStats>,
        playbackHistory: List<PlaybackHistoryEntry>,
        message: String,
        conversationHistory: List<Pair<String, String>>,
        maxPlayableItems: Int
    ): AiLibraryChatResult = withContext(Dispatchers.IO) {
        if (librarySongs.isEmpty()) error(application.getString(R.string.error_library_empty))
        val candidates = buildOpenAiRecommendationCandidates(
            library = librarySongs,
            stats = playbackStats,
            history = playbackHistory
        )
        val response = libraryChatAssistant.chat(
            config = openAiConfig(),
            input = OpenAiLibraryChatInput(
                songs = candidates,
                playbackStats = playbackStats,
                playbackHistory = playbackHistory,
                userMessage = message,
                maxPlayableItems = maxPlayableItems.coerceIn(1, 50),
                conversationHistory = conversationHistory
            )
        )
        val libraryByKey = librarySongs.associateBy { it.playlistIdentityKey() }
        AiLibraryChatResult(
            answer = response.answer,
            songs = response.songKeys
                .mapNotNull { key -> libraryByKey[key] }
                .distinctBy { it.playlistIdentityKey() }
                .take(maxPlayableItems.coerceAtLeast(1)),
            playlistName = response.playlistName.ifBlank { application.getString(R.string.ai_chat_playlist_name) }
        )
    }

    private suspend fun openAiConfig(): OpenAiSongInterpretationConfig {
        val baseUrl = settingsManager.openAiBaseUrl.first()
        return OpenAiSongInterpretationConfig(
            apiKey = settingsManager.openAiApiKey.first(),
            baseUrl = baseUrl,
            model = settingsManager.openAiModel.first(),
            protocol = resolveAiApiProtocol(settingsManager.aiApiProtocol.first(), baseUrl)
        )
    }
}
