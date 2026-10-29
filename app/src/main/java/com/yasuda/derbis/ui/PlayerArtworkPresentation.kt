package com.yasuda.derbis.ui

import com.yasuda.derbis.data.fullResolutionArtworkUrl
import com.yasuda.derbis.domain.DerbisPersonalOrbit
import com.yasuda.derbis.domain.Track

internal fun preferredPlayerArtworkUrl(track: Track): String {
    val candidates = sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    val albumArtwork = candidates.firstOrNull { !DerbisPersonalOrbit.isVideoFrameArtworkUrl(it) }
    return highResolutionPlayerArtworkUrl(albumArtwork ?: candidates.firstOrNull().orEmpty())
}

internal fun highResolutionPlayerArtworkUrl(url: String): String = fullResolutionArtworkUrl(url)
