package com.yasuda.derbis.data.subsonic

/**
 * What the listener typed into the server form.
 *
 * The password never leaves this object except to be written to the Keystore — it is not part of
 * any persisted settings blob, and it is not copied into a [Track].
 */
data class SubsonicCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    val isComplete: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

/** One search hit, before it is turned into a [com.yasuda.derbis.domain.Track]. */
data class SubsonicSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val artistId: String,
    val coverArtId: String,
    val durationSeconds: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: String,
    val genre: String,
    val sizeBytes: Long,
    val suffix: String,
    val bitRate: Int,
    val contentType: String
)

data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArtId: String,
    val songCount: Int,
    val year: String,
    val genre: String
)

data class SubsonicArtist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String
)

data class SubsonicPlaylist(
    val id: String,
    val name: String,
    val owner: String,
    val songCount: Int,
    val coverArtId: String
)

data class SubsonicAlbumDetail(
    val album: SubsonicAlbum,
    val songs: List<SubsonicSong>
)

data class SubsonicPlaylistDetail(
    val playlist: SubsonicPlaylist,
    val songs: List<SubsonicSong>
)

/** What `ping` told us about the server, which is also what the settings screen shows. */
data class SubsonicServerInfo(
    val version: String,
    val serverType: String,
    val openSubsonic: Boolean
)

sealed interface SubsonicResult<out T> {
    data class Ok<T>(val value: T) : SubsonicResult<T>

    /** The server answered and refused: bad credentials, unknown id, feature disabled. */
    data class Failed(val code: Int, val message: String) : SubsonicResult<Nothing>

    /** The request never produced a usable answer: no network, bad URL, unparseable body. */
    data class Unreachable(val message: String) : SubsonicResult<Nothing>

    val success: Boolean get() = this is Ok
}

internal fun SubsonicResult<*>.errorText(fallback: String): String = when (this) {
    is SubsonicResult.Ok -> fallback
    is SubsonicResult.Failed -> message.ifBlank { fallback }
    is SubsonicResult.Unreachable -> message.ifBlank { fallback }
}
