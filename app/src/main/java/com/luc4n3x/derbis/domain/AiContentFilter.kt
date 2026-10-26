package com.luc4n3x.derbis.domain

/**
 * Conservative, offline content gate. It blocks only explicit AI-generation disclosures and
 * established generator/provider identifiers carried in track metadata; it never guesses from
 * artwork, genre, voice, or a creator's identity.
 */
internal object AiContentFilter {
    private val disclosure = Regex(
        """\b(?:ai[ -]?(?:generated|made|created|music|video|art)|generated\s+by\s+ai|made\s+with\s+ai|artificial\s+intelligence|synthetic\s+(?:voice|music|video)|suno(?:\s+ai)?|udio|boomy|aiva|soundful|mubert|amper)\b""",
        RegexOption.IGNORE_CASE
    )

    fun blocksMetadata(vararg values: String): Boolean =
        disclosure.containsMatchIn(values.joinToString(" "))

    fun blocks(track: Track): Boolean = blocksMetadata(
        track.title,
        track.artist,
        track.album,
        track.albumArtist,
        track.composer,
        track.copyright,
        track.source,
        track.videoType,
        track.metadataProvider,
        track.moodTags.joinToString(" ")
    )

    fun filter(tracks: List<Track>, enabled: Boolean): List<Track> =
        if (enabled) tracks.filterNot(::blocks) else tracks
}
