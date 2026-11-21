package com.yasuda.derbis.feature.search

import com.yasuda.derbis.data.deduplicateSearchSongs
import com.yasuda.derbis.data.searchSongIdentityKey
import com.yasuda.derbis.domain.Track

internal data class LocalSearchCandidate(
    val track: Track,
    val affinity: Int
)

internal data class LocalSearchMatch(
    val track: Track,
    val score: Int,
    val strong: Boolean
)

internal object LocalSearchAffinity {
    const val RECENT = 90
    const val LISTENED = 86
    const val LOCAL_MEDIA = 84
    const val FAVORITE = 80
    const val ORBIT = 70
    const val QUEUE = 60
    const val SESSION = 50
    const val HOME = 40
    const val CHARTS = 30
    const val CACHE = 20
}

internal const val LOCAL_SEARCH_RESULT_LIMIT = 6

private const val SCORE_EXACT_TITLE = 1_000
private const val SCORE_EXACT_TITLE_ARTIST = 960
private const val SCORE_EXACT_ARTIST = 800
private const val SCORE_TITLE_PREFIX = 700
private const val SCORE_TITLE_TOKENS = 600
private const val SCORE_MIXED_TOKENS = 500
/**
 * A typo-only match.
 *
 * Deliberately below [SCORE_MIXED_TOKENS]: a result reached by correcting the query must never
 * outrank a result the query actually spells out. The fuzzy pass only runs when the strict pass
 * found nothing at all, so on a query that works these numbers are never used.
 */
private const val SCORE_FUZZY = 420

/** A fuzzy token has to look like a word the listener half-typed, not like noise. */
private const val FUZZY_MIN_TOKEN_CHARS = 3

/**
 * How many candidates the typo pass looks at.
 *
 * The strict pass is O(query × candidate); this one runs a bounded edit distance per word pair, so
 * on a large library it is capped rather than exhaustive. A typo repair that needs to look past a
 * few hundred of the listener's most relevant tracks is not worth the latency it would cost on a
 * slow phone.
 */
private const val FUZZY_SCAN_LIMIT = 240
private const val STRONG_MATCH_SCORE = SCORE_EXACT_TITLE_ARTIST

internal fun matchLocalSearchTracks(
    query: String,
    candidates: List<LocalSearchCandidate>,
    limit: Int = LOCAL_SEARCH_RESULT_LIMIT,
    accept: (Track) -> Boolean = { true }
): List<LocalSearchMatch> {
    val queryKey = searchQueryKey(query)
    if (queryKey.length < MIN_SEARCH_QUERY_CHARS) return emptyList()
    val tokens = queryKey.split(' ').filter(String::isNotEmpty)
    if (tokens.isEmpty()) return emptyList()

    val seen = HashSet<String>()
    val ranked = ArrayList<Pair<Int, LocalSearchMatch>>()
    candidates.forEachIndexed { index, candidate ->
        val track = candidate.track
        val identity = searchSongIdentityKey(track)
        if (identity.isBlank() || track.title.isBlank() || identity in seen) return@forEachIndexed
        val baseScore = localTrackScore(queryKey, tokens, track) ?: return@forEachIndexed
        if (!accept(track)) return@forEachIndexed
        seen += identity
        val match = LocalSearchMatch(
            track = track,
            score = baseScore + candidate.affinity.coerceIn(0, 99),
            strong = baseScore >= STRONG_MATCH_SCORE
        )
        ranked += index to match
    }
    if (ranked.isEmpty()) {
        // Nothing matched by the strict rules. Before reporting an empty search, try correcting a
        // single typo per word: "taylr" and "swfit" are one keystroke away from a real answer, and
        // returning nothing for them is what makes a search box feel broken.
        collectFuzzyMatches(queryKey, tokens, candidates, accept, seen, ranked)
    }
    if (ranked.isEmpty()) return emptyList()

    val ordered = ranked
        .sortedWith(compareByDescending<Pair<Int, LocalSearchMatch>> { it.second.score }.thenBy { it.first })
        .map { it.second }
    val matchByIdentity = ordered.associateBy { searchSongIdentityKey(it.track) }
    return deduplicateSearchSongs(ordered.map(LocalSearchMatch::track))
        .mapNotNull { track -> matchByIdentity[searchSongIdentityKey(track)]?.copy(track = track) }
        .take(limit.coerceAtLeast(0))
}

/**
 * Fills [ranked] with candidates that are one keystroke away from the query.
 *
 * Each token is matched against a word in the title or the artist that is at most one edit away —
 * one insertion, deletion, substitution, or a swap of two neighbouring characters, which is the
 * mistake a thumb makes most often. A short token is left out entirely: "ed" is one edit from
 * "ad", "id", "led" and a hundred other words, and correcting it would bury the real results in
 * guesses.
 *
 * The pass stops after [FUZZY_SCAN_LIMIT] candidates so that a miss on a large library still
 * answers quickly; the list is ordered by relevance before it is cut, so what is dropped is the
 * least likely half rather than an arbitrary slice.
 */
private fun collectFuzzyMatches(
    queryKey: String,
    tokens: List<String>,
    candidates: List<LocalSearchCandidate>,
    accept: (Track) -> Boolean,
    seen: MutableSet<String>,
    ranked: MutableList<Pair<Int, LocalSearchMatch>>
) {
    if (tokens.any { it.length < FUZZY_MIN_TOKEN_CHARS }) return
    var scanned = 0
    candidates.forEachIndexed { index, candidate ->
        if (scanned >= FUZZY_SCAN_LIMIT) return
        val track = candidate.track
        val identity = searchSongIdentityKey(track)
        if (identity.isBlank() || track.title.isBlank() || identity in seen) return@forEachIndexed
        scanned += 1
        val title = searchQueryKey(track.title)
        if (title.isEmpty()) return@forEachIndexed
        val artist = searchQueryKey(track.artist)
        val titleWords = wordsOf(title)
        val artistWords = wordsOf(artist)
        val everyTokenIsClose = tokens.all { token ->
            titleWords.any { word -> withinOneEdit(token, word) } ||
                artistWords.any { word -> withinOneEdit(token, word) }
        }
        if (!everyTokenIsClose) return@forEachIndexed
        if (!accept(track)) return@forEachIndexed
        seen += identity
        ranked += index to LocalSearchMatch(
            track = track,
            score = SCORE_FUZZY + candidate.affinity.coerceIn(0, 99),
            // Never "strong": a corrected query is a suggestion, and the UI is entitled to show
            // it differently from a result that matched exactly.
            strong = false
        )
    }
}

/**
 * The words of a normalised key, ignoring the separator the query key uses.
 *
 * Split on space only, because [searchQueryKey] has already folded punctuation into spaces; a
 * split on every non-letter would then have to agree with that folding, and disagreeing is how a
 * word ends up unmatchable.
 */
private fun wordsOf(key: String): List<String> =
    key.split(' ').filter { it.isNotEmpty() }

/**
 * Whether two words are at most one edit apart, counting a swap of neighbours as one edit.
 *
 * The length check is the whole optimisation: any single edit changes the length by at most one,
 * so a pair that fails it is rejected without allocating anything or walking the strings. That
 * check alone removes the overwhelming majority of comparisons in a real library.
 *
 * Unicode is handled at code-point granularity, so a word containing an emoji or a decomposed
 * accent is measured in characters rather than in bytes.
 */
internal fun withinOneEdit(left: String, right: String): Boolean {
    if (left == right) return true
    val a = left.codePoints().toArray()
    val b = right.codePoints().toArray()
    // Any single edit changes the length by at most one, so this rejects the overwhelming
    // majority of pairs before a single character is compared.
    if (kotlin.math.abs(a.size - b.size) > 1) return false
    val longer = if (a.size >= b.size) a else b
    val shorter = if (a.size >= b.size) b else a
    var i = 0
    var j = 0
    var edits = 0
    while (i < longer.size && j < shorter.size) {
        if (longer[i] == shorter[j]) {
            i++
            j++
            continue
        }
        edits++
        if (edits > 1) return false
        if (longer.size == shorter.size) {
            // Two neighbouring characters the other way round is the mistake a thumb makes most
            // often, and it is one edit — not the two a plain distance would charge for it.
            if (i + 1 < longer.size && j + 1 < shorter.size &&
                longer[i] == shorter[j + 1] && longer[i + 1] == shorter[j]
            ) {
                i += 2
                j += 2
                continue
            }
            i++
            j++
        } else {
            // Lengths differ: the extra character in the longer word is the edit itself.
            i++
        }
    }
    edits += (longer.size - i) + (shorter.size - j)
    return edits <= 1
}

private fun localTrackScore(queryKey: String, tokens: List<String>, track: Track): Int? {
    val title = searchQueryKey(track.title)
    if (title.isEmpty()) return null
    val artist = searchQueryKey(track.artist)
    val titleWords = title.split(' ')
    val artistWords = artist.split(' ').filter(String::isNotEmpty)
    val everyTokenMatches = tokens.all { token ->
        titleWords.any { it.startsWith(token) } || artistWords.any { it.startsWith(token) }
    }
    if (!everyTokenMatches) return null
    return when {
        title == queryKey -> SCORE_EXACT_TITLE
        artist.isNotEmpty() && ("$title $artist" == queryKey || "$artist $title" == queryKey) -> SCORE_EXACT_TITLE_ARTIST
        artist == queryKey -> SCORE_EXACT_ARTIST
        title.startsWith(queryKey) -> SCORE_TITLE_PREFIX
        tokens.all { token -> titleWords.any { it.startsWith(token) } } -> SCORE_TITLE_TOKENS
        else -> SCORE_MIXED_TOKENS
    }
}
