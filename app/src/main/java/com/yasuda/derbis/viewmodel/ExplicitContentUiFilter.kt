package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.DerbisExplicitFilter
import com.yasuda.derbis.domain.Track

/**
 * Applies the listener's explicit-content choice to everything the UI is about to draw.
 *
 * It sits beside [withAiContentBlocked] and for the same reason: one choke point, in the same
 * shape, rather than a filter sprinkled through every screen that happens to show a track. The
 * state is read once at the root, so a screen cannot forget to ask.
 *
 * Two properties are deliberate.
 *
 * First, [DerbisExplicitFilter.Off] returns the identical instance rather than a copy, so the cost
 * of having the setting is exactly zero for the overwhelming majority of listeners.
 *
 * Second, the filter only ever *removes* — it never reorders or invents. A queue that loses its
 * current track because that track is explicit keeps playing it: [DerbisUiState.currentTrack] is
 * carried through untouched, because silencing the song already in someone's ears while they flip
 * a switch in settings would be a bug, not a filter. The queue itself is filtered so the next
 * track is already clean, and the player simply finishes what it started.
 */
internal fun DerbisUiState.withExplicitFiltered(): DerbisUiState {
    val setting = interfaceSettings.explicitFilter
    if (setting == DerbisExplicitFilter.Off) return this

    val allows: (Track) -> Boolean = { track -> setting.allows(track.explicit) }
    fun filtered(items: List<Track>) = if (items.isEmpty()) items else items.filter(allows)

    // An album is only "explicit" through its tracks, and only some of them carry the flag, so an
    // album is hidden when nothing playable would be left — not when one track out of twelve is
    // explicit. That is why this is computed from the filtered list rather than from a hit field.
    fun albumTracks(tracks: List<Track>) = filtered(tracks)

    return copy(
        quickPickSeeds = filtered(quickPickSeeds),
        recognitionMatch = recognitionMatch?.takeIf { allows(it) },
        tracks = filtered(tracks),
        queue = filtered(queue),
        pendingQueueAddTracks = filtered(pendingQueueAddTracks),
        queuePlaylistDraft = queuePlaylistDraft?.let { it.copy(tracks = filtered(it.tracks)) },
        searchResults = filtered(searchResults),
        // Search history is kept intact on purpose: it records what was searched, not what was
        // played, and erasing it would make the setting look like it lost the listener's data.
        personalOrbitTracks = filtered(personalOrbitTracks),
        charts = filtered(charts),
        homeSections = homeSections
            .map { it.copy(tracks = filtered(it.tracks)) }
            .filter { it.tracks.isNotEmpty() },
        homeResonanceTracks = filtered(homeResonanceTracks),
        albumDetail = albumDetail?.let { detail ->
            val kept = albumTracks(detail.tracks)
            if (kept.isEmpty()) null
            else detail.copy(tracks = kept, otherVersions = detail.otherVersions)
        },
        artistProfile = artistProfile?.let { profile ->
            val kept = filtered(profile.topSongs)
            val videos = filtered(profile.videos)
            if (kept.isEmpty() && videos.isEmpty()) null
            else profile.copy(topSongs = kept, videos = videos)
        },
        favorites = filtered(favorites),
        forgottenFavorites = filtered(forgottenFavorites),
        playlists = playlists
            .map { it.copy(tracks = filtered(it.tracks)) }
            .filter { it.tracks.isNotEmpty() },
        openPlaylist = openPlaylist?.let { playlist ->
            val kept = filtered(playlist.tracks)
            if (kept.isEmpty()) null else playlist.copy(tracks = kept)
        },
        playlistHitPreview = playlistHitPreview?.let { preview ->
            val kept = filtered(preview.tracks)
            // A preview whose tracks have all been filtered is dropped rather than shown as an
            // empty sheet: the sheet has nothing to say at that point.
            if (kept.isEmpty() && !preview.loading) null else preview.copy(tracks = kept)
        },
        similarSongs = filtered(similarSongs),
        exploreTracks = filtered(exploreTracks),
        exploreFreshTracks = filtered(exploreFreshTracks),
        exploreWorldFreshTracks = filtered(exploreWorldFreshTracks),
        exploreVideos = filtered(exploreVideos),
        exploreSamples = filtered(exploreSamples),
        recentListens = filtered(recentListens),
        mostPlayedTracks = filtered(mostPlayedTracks),
        searchData = searchData.copy(
            topTrack = searchData.topTrack?.takeIf { allows(it) } ?: filtered(searchData.songs).firstOrNull(),
            songs = filtered(searchData.songs),
            videos = filtered(searchData.videos)
        )
    )
}

/** One track's answer, in one place: every surface above asks this and nothing else. */
internal fun DerbisExplicitFilter.allows(explicit: Boolean): Boolean = when (this) {
    DerbisExplicitFilter.Off -> true
    DerbisExplicitFilter.HideExplicit -> !explicit
    DerbisExplicitFilter.OnlyExplicit -> explicit
}
