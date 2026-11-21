package com.voxyn.halcyon.player

import com.voxyn.halcyon.data.PlaybackHistoryEntry
import com.voxyn.halcyon.data.model.Song
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

/**
 * Listening signals for one song inside the preference-shuffle window.
 *
 * [recentPlays] only counts listens inside the window, so long-term totals can never snowball
 * into an ever-growing weight.
 */
internal data class ShuffleListeningProfile(
    val recentPlays: Int,
    val completion: Double,
    val daysSinceLastPlay: Double
)

/**
 * Tuning for the preference shuffle.
 *
 * [windowDays] 15 keeps the window short: what you liked two months ago stops dominating.
 * [exploreShare] is the freshness budget: that fraction of every round is drawn uniformly,
 * including songs you have never played.
 */
internal data class PreferenceShufflePolicy(
    val windowDays: Int = 15,
    val exploreShare: Double = 0.3,
    val minWeight: Double = 0.5,
    val maxWeight: Double = 2.5,
    val noveltyBonus: Double = 1.0,
    val freshnessTauDays: Double = 14.0,
    val favoriteBonus: Double = 1.15
) {
    companion object {
        /**
         * Maps the two user-facing sliders (0-100) onto the policy knobs.
         *
         * Strength: 0 keeps a near-uniform shuffle (60% exploration), 100 leans hard on the
         * preference weights (10% exploration).
         * Freshness: grows the novelty bonus and shortens the recency decay, so long-unheard
         * songs come back sooner.
         */
        fun forSettings(strength: Int, freshness: Int): PreferenceShufflePolicy {
            val s = strength.coerceIn(0, 100) / 100.0
            val f = freshness.coerceIn(0, 100) / 100.0
            return PreferenceShufflePolicy(
                exploreShare = 0.6 - 0.5 * s,
                noveltyBonus = 0.4 + 1.6 * f,
                freshnessTauDays = 30.0 - 22.0 * f
            )
        }
    }
}

private const val DAY_MS = 86_400_000.0

/**
 * Preference weight for one song.
 *
 * Every term is bounded on purpose:
 * - play counts go through `ln`, so 15 plays and 150 plays weigh almost the same,
 * - the whole weight is clamped into `[minWeight, maxWeight]`, so a favourite can never exceed
 *   2.5x a cold song,
 * - songs with no recent listens still keep a novelty bonus so they stay reachable.
 */
internal fun preferenceWeight(
    profile: ShuffleListeningProfile?,
    favorite: Boolean,
    policy: PreferenceShufflePolicy = PreferenceShufflePolicy()
): Double {
    val preference = if (profile == null) {
        0.0
    } else {
        (ln(1.0 + profile.recentPlays) / ln(16.0)).coerceIn(0.0, 1.0)
    }
    val completion = (profile?.completion ?: 0.6).coerceIn(0.0, 1.5)
    val freshness = if (profile == null) {
        1.0 + policy.noveltyBonus
    } else {
        1.0 + policy.noveltyBonus * exp(-profile.daysSinceLastPlay / policy.freshnessTauDays)
    }
    val favoriteFactor = if (favorite) policy.favoriteBonus else 1.0
    return ((1.0 + preference) * (0.6 + 0.8 * completion) * freshness * favoriteFactor)
        .coerceIn(policy.minWeight, policy.maxWeight)
}

/**
 * Weighted sampling without replacement plus an explicit exploration share.
 *
 * The weighted pass decides *which* songs enter the round (Efraimidis-Spirakis keys, one
 * `pow` per song), the exploration pass refills from a uniform draw, and the finished round is
 * shuffled again so preference never pushes favourites to the front of the queue.
 */
internal fun weightedShuffleWithoutReplacement(
    songs: List<Song>,
    weightOf: (Song) -> Double,
    exploreShare: Double,
    rng: Random
): List<Song> {
    if (songs.size <= 1) return songs
    val exploreCount = (songs.size * exploreShare.coerceIn(0.0, 1.0)).toInt()
    val weightedCount = (songs.size - exploreCount).coerceAtLeast(0)
    val keyed = songs.map { song ->
        val weight = weightOf(song).coerceAtLeast(1e-6)
        rng.nextDouble().pow(1.0 / weight) to song
    }.sortedByDescending { it.first }
    val preferred = keyed.take(weightedCount).map { it.second }
    val remainder = keyed.drop(weightedCount).map { it.second }.shuffled(rng)
    return (preferred + remainder).shuffled(rng)
}

/**
 * Aggregates raw listen history into per-song profiles inside the policy window.
 *
 * Listens that were skipped early count much less than listens that reached the end, so a song
 * you keep skipping does not gain weight just because it started playing.
 */
internal fun buildShuffleProfiles(
    history: List<PlaybackHistoryEntry>,
    now: Long,
    policy: PreferenceShufflePolicy = PreferenceShufflePolicy()
): Map<Long, ShuffleListeningProfile> {
    val windowStart = now - (policy.windowDays * 86_400_000L)
    val plays = HashMap<Long, Int>()
    val completionSum = HashMap<Long, Double>()
    val completionCount = HashMap<Long, Int>()
    val lastPlayed = HashMap<Long, Long>()
    for (entry in history) {
        if (entry.songId <= 0L) continue
        if (entry.playedAt < windowStart) continue
        val songId = entry.songId
        plays[songId] = (plays[songId] ?: 0) + 1
        val duration = entry.durationMs
        if (duration > 0L && entry.listenedMs > 0L) {
            completionSum[songId] = (completionSum[songId] ?: 0.0) +
                entry.listenedMs.toDouble() / duration
            completionCount[songId] = (completionCount[songId] ?: 0) + 1
        }
        if (entry.playedAt > (lastPlayed[songId] ?: 0L)) lastPlayed[songId] = entry.playedAt
    }
    if (plays.isEmpty()) return emptyMap()
    return plays.mapValues { (songId, count) ->
        val summed = completionSum[songId]
        val counted = completionCount[songId]
        ShuffleListeningProfile(
            recentPlays = count,
            completion = if (summed != null && counted != null && counted > 0) summed / counted else 0.6,
            daysSinceLastPlay = ((now - (lastPlayed[songId] ?: now)).coerceAtLeast(0L)) / DAY_MS
        )
    }
}
