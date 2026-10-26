package com.luc4n3x.derbis.domain

import kotlin.math.abs

enum class DerbisMixKind {
    SimilarTrack,
    SimilarArtist,
    Rediscover,
    CurrentRotation,
    Genre,
    SurpriseMe,
    Personalized
}

data class DerbisMixSummary(
    val kind: DerbisMixKind,
    val label: String,
    val seedTrackId: String = "",
    val trackCount: Int = 0
)

data class DerbisMixCandidate(
    val track: Track,
    val familiarity: Float,
    val affinity: Float,
    val recentlyPlayed: Boolean
)

object DerbisMixDefaults {
    const val Familiarity: Float = 0.5f
    const val MaxCandidates: Int = 160
    const val MixSize: Int = 30
    const val CanonicalSourceLimit: Int = 512
    const val RecentExclusionMs: Long = 3L * 60L * 60L * 1000L
}

object DerbisMixRanker {

    private const val FamiliarityWeight = 0.62f
    private const val AffinityWeight = 0.26f
    private const val FreshnessWeight = 0.12f

    fun rank(
        candidates: List<DerbisMixCandidate>,
        familiarityBias: Float,
        limit: Int = DerbisMixDefaults.MixSize,
        excludeRecent: Boolean = false
    ): List<Track> {
        if (candidates.isEmpty() || limit <= 0) return emptyList()
        val bias = familiarityBias.finiteOr(DerbisMixDefaults.Familiarity).coerceIn(0f, 1f)
        val seen = HashSet<String>(candidates.size)
        val scored = ArrayList<ScoredCandidate>(candidates.size)
        for (candidate in candidates) {
            if (excludeRecent && candidate.recentlyPlayed) continue
            val key = mixTrackKey(candidate.track)
            if (key.isEmpty() || !seen.add(key)) continue
            scored.add(ScoredCandidate(candidate.track, score(candidate, bias)))
        }
        if (scored.isEmpty()) return emptyList()
        scored.sortByDescending { it.score }
        val result = ArrayList<Track>(minOf(limit, scored.size))
        for (index in 0 until minOf(limit, scored.size)) {
            result.add(scored[index].track)
        }
        return result
    }

    private fun score(candidate: DerbisMixCandidate, bias: Float): Float {
        val familiarity = candidate.familiarity.finiteOr(0f).coerceIn(0f, 1f)
        val affinity = candidate.affinity.finiteOr(0f).coerceIn(0f, 1f)
        val match = 1f - abs(familiarity - bias)
        val freshness = 1f - familiarity
        val affinityPull = affinity * (0.35f + bias * 0.65f)
        return match * FamiliarityWeight +
            affinityPull * AffinityWeight +
            freshness * FreshnessWeight * (1f - bias)
    }

    private class ScoredCandidate(val track: Track, val score: Float)
}

fun listenEventMixKey(event: ListenEvent): String {
    val id = event.trackId.trim()
    if (id.isNotEmpty()) return id
    return compositeMixKey(event.title, event.artist)
}

private fun compositeMixKey(title: String, artist: String): String {
    val cleanTitle = title.trim().lowercase()
    val cleanArtist = artist.trim().lowercase()
    if (cleanTitle.isEmpty() && cleanArtist.isEmpty()) return ""
    return "$cleanTitle|$cleanArtist"
}

fun mixTrackKey(track: Track): String {
    val id = track.id.trim()
    if (id.isNotEmpty()) return id
    return compositeMixKey(track.title, track.artist)
}

fun prepareMixPlaybackTracks(
    selected: List<Track>,
    canonicalSources: List<Track>
): List<Track> {
    if (selected.isEmpty()) return emptyList()
    if (canonicalSources.isEmpty()) {
        return DerbisPersonalOrbit.distinctRecordings(
            selected.map { DerbisPersonalOrbit.prepareForOrbit(it, emptyList()) }
        )
    }

    val canonical = DerbisPersonalOrbit.distinctRecordings(
        canonicalSources.take(DerbisMixDefaults.CanonicalSourceLimit)
    )
    val prepared = ArrayList<Track>(selected.size)
    for (candidate in selected) {
        val knownRecording = canonical.firstOrNull { source ->
            DerbisPersonalOrbit.sameRecording(source, candidate)
        }
        val resolved = if (knownRecording != null) {
            DerbisPersonalOrbit.prepareForOrbit(knownRecording, listOf(candidate))
        } else {
            DerbisPersonalOrbit.prepareForOrbit(candidate, canonical)
        }
        prepared.add(resolved)
    }
    return DerbisPersonalOrbit.distinctRecordings(prepared)
}

fun buildMixCandidates(
    tracks: List<Track>,
    listens: List<ListenEvent>,
    nowMs: Long,
    recentExclusionMs: Long = DerbisMixDefaults.RecentExclusionMs
): List<DerbisMixCandidate> {
    if (tracks.isEmpty()) return emptyList()
    val playsByTrack = HashMap<String, Int>()
    val playsByArtist = HashMap<String, Int>()
    var lastPlayedByTrack = HashMap<String, Long>()
    var maxTrackPlays = 0
    var maxArtistPlays = 0
    for (event in listens) {
        val trackKey = listenEventMixKey(event)
        if (trackKey.isNotEmpty()) {
            val plays = (playsByTrack[trackKey] ?: 0) + 1
            playsByTrack[trackKey] = plays
            if (plays > maxTrackPlays) maxTrackPlays = plays
            val previous = lastPlayedByTrack[trackKey] ?: 0L
            if (event.startedAt > previous) lastPlayedByTrack[trackKey] = event.startedAt
        }
        val artistKey = event.artist.trim().lowercase()
        if (artistKey.isNotEmpty()) {
            val plays = (playsByArtist[artistKey] ?: 0) + 1
            playsByArtist[artistKey] = plays
            if (plays > maxArtistPlays) maxArtistPlays = plays
        }
    }
    val trackScale = maxTrackPlays.coerceAtLeast(1).toFloat()
    val artistScale = maxArtistPlays.coerceAtLeast(1).toFloat()
    val candidates = ArrayList<DerbisMixCandidate>(minOf(tracks.size, DerbisMixDefaults.MaxCandidates))
    for (track in tracks) {
        if (candidates.size >= DerbisMixDefaults.MaxCandidates) break
        val key = mixTrackKey(track)
        if (key.isEmpty()) continue
        val plays = playsByTrack[key] ?: 0
        val artistPlays = playsByArtist[track.artist.trim().lowercase()] ?: 0
        val lastPlayed = lastPlayedByTrack[key] ?: 0L
        candidates.add(
            DerbisMixCandidate(
                track = track,
                familiarity = (plays / trackScale).coerceIn(0f, 1f),
                affinity = (artistPlays / artistScale).coerceIn(0f, 1f),
                recentlyPlayed = lastPlayed > 0L && nowMs - lastPlayed <= recentExclusionMs
            )
        )
    }
    return candidates
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
