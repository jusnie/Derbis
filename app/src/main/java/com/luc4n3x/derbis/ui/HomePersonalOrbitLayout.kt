package com.luc4n3x.derbis.ui

import com.luc4n3x.derbis.domain.DerbisPersonalOrbit
import com.luc4n3x.derbis.domain.Track
import java.util.Locale

internal const val HOME_PERSONAL_ORBIT_GRID_COLUMNS = 3
private const val HOME_PERSONAL_ORBIT_PAGE_SIZE = HOME_PERSONAL_ORBIT_GRID_COLUMNS * HOME_PERSONAL_ORBIT_GRID_COLUMNS
internal const val HOME_PERSONAL_ORBIT_MAX_TRACKS = HOME_PERSONAL_ORBIT_PAGE_SIZE * 2

internal fun homePersonalOrbitPages(
    tracks: List<Track>,
    limit: Int = DerbisPersonalOrbit.DISPLAY_LIMIT
): List<List<List<Track>>> {
    val distinct = DerbisPersonalOrbit.distinctWorks(
        DerbisPersonalOrbit.distinctRecordings(tracks)
    ).take(limit.coerceAtLeast(0))
    val visibleCount = distinct.size.coerceAtMost(HOME_PERSONAL_ORBIT_MAX_TRACKS)
    return distinct
        .take(visibleCount)
        .chunked(HOME_PERSONAL_ORBIT_PAGE_SIZE)
        .map { page -> page.chunked(HOME_PERSONAL_ORBIT_GRID_COLUMNS) }
}

internal fun homePersonalOrbitFilledTracks(
    tracks: List<Track>,
    recommendations: List<Track>,
    excludedTracks: List<Track>
): List<Track> {
    if (tracks.isEmpty()) return emptyList()
    val blocked = DerbisPersonalOrbit.distinctWorks(
        DerbisPersonalOrbit.distinctRecordings(excludedTracks)
    )
    val candidates = tracks + recommendations.filter(DerbisPersonalOrbit::isReliableMusicCandidate)
    return DerbisPersonalOrbit.distinctWorks(
        DerbisPersonalOrbit.distinctRecordings(blocked + candidates)
    ).drop(blocked.size).take(HOME_PERSONAL_ORBIT_MAX_TRACKS)
}

internal fun homePersonalOrbitInitial(userName: String): String? {
    val trimmed = userName.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase(Locale.getDefault())
}
