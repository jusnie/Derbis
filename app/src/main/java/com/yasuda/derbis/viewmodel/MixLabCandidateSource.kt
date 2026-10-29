package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.data.FollowedArtistsStore
import com.yasuda.derbis.data.ListeningPulseStore
import com.yasuda.derbis.domain.HomeSection
import com.yasuda.derbis.domain.DerbisPersonalOrbit
import com.yasuda.derbis.domain.MixLabCandidate
import com.yasuda.derbis.domain.SimilarSongsSelector
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.domain.listenEventMixKey
import com.yasuda.derbis.domain.mixTrackKey

object MixLabCandidateSource {

    private const val HistoryWindowDays = 180
    private const val SimilarSeedLimit = 6
    private const val PoolBudget = 300

    suspend fun assemble(
        favorites: List<Track>,
        homeSections: List<HomeSection>,
        charts: List<Track>,
        followedArtistsStore: FollowedArtistsStore,
        listeningPulseStore: ListeningPulseStore
    ): List<MixLabCandidate> {
        val listens = runCatching { listeningPulseStore.eventsWindow(HistoryWindowDays) }.getOrDefault(emptyList())
        val playCounts = HashMap<String, Int>()
        val listenedMsByTrack = HashMap<String, Long>()
        val lastPlayedByTrack = HashMap<String, Long>()
        for (event in listens) {
            val key = listenEventMixKey(event)
            if (key.isEmpty()) continue
            playCounts[key] = (playCounts[key] ?: 0) + 1
            listenedMsByTrack[key] = (listenedMsByTrack[key] ?: 0L) + event.listenedMs.coerceAtLeast(0L)
            val previous = lastPlayedByTrack[key] ?: 0L
            if (event.startedAt > previous) lastPlayedByTrack[key] = event.startedAt
        }

        val favoriteIdentities = favorites.map(DerbisPersonalOrbit::identityKey).toSet()
        val followedArtists = runCatching { followedArtistsStore.load() }.getOrDefault(emptyList())
        val followedKeys = followedArtists
            .flatMap { listOf(it.key, it.name) }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        val basePool = LinkedHashMap<String, Track>()
        fun offer(track: Track) {
            if (!DerbisPersonalOrbit.isReliableMusicCandidate(track)) return
            val identity = DerbisPersonalOrbit.identityKey(track)
            if (identity.isEmpty() || identity in basePool) return
            if (basePool.size >= PoolBudget) return
            basePool[identity] = track
        }

        favorites.forEach(::offer)
        homeSections.forEach { section -> section.tracks.forEach(::offer) }
        charts.forEach(::offer)

        val historySeeds = runCatching {
            listeningPulseStore.mostPlayedTracks(days = HistoryWindowDays, limit = SimilarSeedLimit)
        }.getOrDefault(emptyList())
        val seedTracks = (favorites.take(SimilarSeedLimit) + historySeeds)
            .distinctBy(DerbisPersonalOrbit::identityKey)
            .take(SimilarSeedLimit)

        val similarSeedIdentities = HashSet<String>()
        val candidatePoolForSimilar = basePool.values.toList()
        seedTracks.forEach { seed ->
            SimilarSongsSelector.select(
                candidates = candidatePoolForSimilar,
                seed = seed,
                excludedIdentities = emptySet(),
                limit = SimilarSongsSelector.POOL_SIZE
            ).forEach { related ->
                offer(related)
                similarSeedIdentities += DerbisPersonalOrbit.identityKey(related)
            }
        }

        return basePool.values.map { track ->
            val historyKey = mixTrackKey(track)
            val artistKey = DerbisPersonalOrbit.artistKeys(track).firstOrNull() ?: track.artist.trim().lowercase()
            MixLabCandidate(
                track = track,
                playCount = playCounts[historyKey] ?: 0,
                listenedMs = listenedMsByTrack[historyKey] ?: 0L,
                lastPlayedAt = lastPlayedByTrack[historyKey] ?: 0L,
                isFavorite = DerbisPersonalOrbit.identityKey(track) in favoriteIdentities,
                isFollowedArtist = artistKey.isNotEmpty() && artistKey in followedKeys,
                isSimilarSeed = DerbisPersonalOrbit.identityKey(track) in similarSeedIdentities
            )
        }
    }
}
