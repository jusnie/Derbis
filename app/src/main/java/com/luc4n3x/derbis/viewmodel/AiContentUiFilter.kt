package com.luc4n3x.derbis.viewmodel

import com.luc4n3x.derbis.domain.AiContentFilter

internal fun DerbisUiState.withAiContentBlocked(): DerbisUiState {
    if (!interfaceSettings.blockAiContent) return this
    fun filtered(items: List<com.luc4n3x.derbis.domain.Track>) = AiContentFilter.filter(items, enabled = true)
    fun blocked(vararg metadata: String) = AiContentFilter.blocksMetadata(*metadata)
    return copy(
        quickPickSeeds = filtered(quickPickSeeds),
        recognitionMatch = recognitionMatch?.takeUnless(AiContentFilter::blocks),
        tracks = filtered(tracks),
        queue = filtered(queue),
        pendingQueueAddTracks = filtered(pendingQueueAddTracks),
        queuePlaylistDraft = queuePlaylistDraft?.let { it.copy(tracks = filtered(it.tracks)) },
        searchResults = filtered(searchResults),
        recentSearches = filtered(recentSearches),
        personalOrbitTracks = filtered(personalOrbitTracks),
        charts = filtered(charts),
        homeSections = homeSections.map { it.copy(tracks = filtered(it.tracks)) }.filter { it.tracks.isNotEmpty() },
        homeAlbums = homeAlbums.filterNot { blocked(it.title, it.artist, it.metadataProvider) },
        homeArtists = homeArtists.filterNot { blocked(it.name) },
        homeResonanceTracks = filtered(homeResonanceTracks),
        albumDetail = albumDetail?.let { detail ->
            if (blocked(detail.album.title, detail.album.artist, detail.album.metadataProvider)) null
            else detail.copy(
                tracks = filtered(detail.tracks),
                otherVersions = detail.otherVersions.filterNot { blocked(it.title, it.artist, it.metadataProvider) }
            )
        },
        artistProfile = artistProfile?.let { profile ->
            if (blocked(profile.name)) null
            else profile.copy(topSongs = filtered(profile.topSongs), videos = filtered(profile.videos))
        },
        favorites = filtered(favorites),
        forgottenFavorites = filtered(forgottenFavorites),
        playlists = playlists
            .filterNot { blocked(it.name) }
            .map { it.copy(tracks = filtered(it.tracks)) },
        openPlaylist = openPlaylist?.let { playlist ->
            if (blocked(playlist.name)) null else playlist.copy(tracks = filtered(playlist.tracks))
        },
        playlistHitPreview = playlistHitPreview?.let { preview ->
            if (blocked(preview.hit.title, preview.hit.author)) null
            else preview.copy(tracks = filtered(preview.tracks))
        },
        similarSongs = filtered(similarSongs),
        exploreTracks = filtered(exploreTracks),
        exploreFreshTracks = filtered(exploreFreshTracks),
        exploreWorldFreshTracks = filtered(exploreWorldFreshTracks),
        exploreVideos = filtered(exploreVideos),
        exploreSamples = filtered(exploreSamples),
        recentListens = filtered(recentListens),
        mostPlayedTracks = filtered(mostPlayedTracks),
        currentTrack = currentTrack?.takeUnless(AiContentFilter::blocks),
        searchData = searchData.copy(
            topTrack = searchData.topTrack?.takeUnless(AiContentFilter::blocks),
            songs = filtered(searchData.songs),
            artists = searchData.artists.filterNot { blocked(it.name) },
            albums = searchData.albums.filterNot { blocked(it.title, it.artist, it.metadataProvider) },
            playlists = searchData.playlists.filterNot { blocked(it.title, it.author) },
            videos = filtered(searchData.videos),
            discography = searchData.discography?.let { discography ->
                if (blocked(discography.artist.name)) null
                else discography.copy(
                    albums = discography.albums.filterNot { album -> blocked(album.title, album.artist, album.metadataProvider) },
                    singles = discography.singles.filterNot { album -> blocked(album.title, album.artist, album.metadataProvider) },
                    compilations = discography.compilations.filterNot { album -> blocked(album.title, album.artist, album.metadataProvider) },
                    playlists = discography.playlists.filterNot { playlist -> blocked(playlist.title, playlist.author) }
                )
            }
        )
    )
}
