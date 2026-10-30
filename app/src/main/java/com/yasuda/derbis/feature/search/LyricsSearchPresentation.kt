package com.yasuda.derbis.feature.search

import com.yasuda.derbis.domain.LyricsSearchMatch
import com.yasuda.derbis.domain.SearchResults

internal enum class LyricsSearchContent {
    Prompt,
    Loading,
    Matches,
    Failed,
    Empty
}

internal fun lyricsSearchMatches(results: SearchResults, loaded: Boolean = true): List<LyricsSearchMatch> =
    if (loaded) results.lyricsMatches.distinctBy { it.track.id } else emptyList()

internal fun lyricsSearchContent(
    query: String,
    matchCount: Int,
    loaded: Boolean,
    loading: Boolean,
    failed: Boolean,
    pending: Boolean
): LyricsSearchContent = when {
    query.trim().length < MIN_SEARCH_QUERY_CHARS || searchQueryKey(query).isEmpty() -> LyricsSearchContent.Prompt
    loaded && matchCount > 0 -> LyricsSearchContent.Matches
    loading -> LyricsSearchContent.Loading
    failed -> LyricsSearchContent.Failed
    !loaded && pending -> LyricsSearchContent.Loading
    else -> LyricsSearchContent.Empty
}
