package com.luc4n3x.derbis.ui

import com.luc4n3x.derbis.data.fullResolutionArtworkUrl
import com.luc4n3x.derbis.domain.DerbisPersonalOrbit
import com.luc4n3x.derbis.domain.Track

internal fun preferredPlayerArtworkUrl(track: Track): String {
    val candidates = sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    val albumArtwork = candidates.firstOrNull { !DerbisPersonalOrbit.isVideoFrameArtworkUrl(it) }
    return highResolutionPlayerArtworkUrl(albumArtwork ?: candidates.firstOrNull().orEmpty())
}

internal fun highResolutionPlayerArtworkUrl(url: String): String = fullResolutionArtworkUrl(url)
