package com.yasuda.derbis.ui.library

import com.yasuda.derbis.ui.components.DerbisPlayAllIconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.data.locallibrary.LocalAlbumGroup
import com.yasuda.derbis.data.locallibrary.LocalArtistGroup
import com.yasuda.derbis.data.local.LocalMediaEntity
import com.yasuda.derbis.data.locallibrary.LocalFolderGroup
import com.yasuda.derbis.data.locallibrary.isSafeTagEditorFormat
import com.yasuda.derbis.data.locallibrary.matchesFullTagQuery
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.localLibraryRecentFilterLabel
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisGlass
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPanelSoft
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.viewmodel.LocalLibraryUiState
import java.util.Locale

internal enum class LocalLibraryTab { Songs, Albums, Artists, Folders }

internal enum class LocalLibraryQualityFilter { All, Lossless, HighBitrate, Recent }

internal data class LocalLibraryAlphabetTarget(
    val letter: Char,
    val contentIndex: Int
)

internal data class LocalLibraryAlphabetIndex(
    val targets: List<LocalLibraryAlphabetTarget> = emptyList(),
    val itemCount: Int = 0
) {
    val visible: Boolean
        get() = itemCount >= LOCAL_ALPHABET_MIN_ITEMS && targets.size >= LOCAL_ALPHABET_MIN_LETTERS
}

internal fun LocalLibraryTab.label(strings: DerbisStrings): String = when (this) {
    LocalLibraryTab.Songs -> strings.songsPlain
    LocalLibraryTab.Albums -> strings.albumsPlain
    LocalLibraryTab.Artists -> strings.artists
    LocalLibraryTab.Folders -> strings.localFolders
}

internal data class LocalLibraryCallbacks(
    val onPlay: (List<Track>, Track) -> Unit,
    val onAddToQueue: (List<Track>) -> Unit,
    val onToggleFavorite: (Track) -> Unit,
    val onQuickScan: () -> Unit,
    val onFullScan: () -> Unit,
    val onRebuildDerbis: () -> Unit,
    val onGrantPermission: () -> Unit,
    val onToggleFolderHidden: (String, Boolean) -> Unit,
    val onEditTags: (Track) -> Unit
)

internal fun LazyListScope.localLibrarySection(
    library: LocalLibraryUiState,
    tab: LocalLibraryTab,
    onTab: (LocalLibraryTab) -> Unit,
    qualityFilter: LocalLibraryQualityFilter,
    onQualityFilter: (LocalLibraryQualityFilter) -> Unit,
    nowMs: Long,
    expandedGroupKey: String?,
    onExpandGroup: (String?) -> Unit,
    query: String,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    callbacks: LocalLibraryCallbacks
) {
    item(key = "local-actions", contentType = "local-actions") {
        LocalLibraryActions(library, callbacks)
    }
    if (!library.permissionGranted) {
        item(key = "local-permission", contentType = "local-permission") {
            LocalLibraryPermissionCard(callbacks.onGrantPermission)
        }
        return
    }
    item(key = "local-tabs", contentType = "local-tabs") {
        LocalLibraryTabs(
            selected = tab,
            onSelect = { entry ->
                onExpandGroup(null)
                onTab(entry)
            }
        )
    }
    if (library.catalog.totalCount == 0) {
        item(key = "local-empty", contentType = "local-empty") {
            LibraryEmpty(Icons.Rounded.LibraryMusic, LocalDerbisStrings.current.localEmpty)
        }
        return
    }
    val availableQualityFilters = localLibraryQualityFilters(
        library.catalog.mediaByUri.values,
        nowMs
    )
    val effectiveQualityFilter = qualityFilter.takeIf { it in availableQualityFilters }
        ?: LocalLibraryQualityFilter.All
    if (availableQualityFilters.size > 1) {
        item(key = "local-quality-filters", contentType = "local-quality-filters") {
            LocalLibraryQualityFilters(
                filters = availableQualityFilters,
                selected = effectiveQualityFilter,
                onSelect = onQualityFilter
            )
        }
    }
    when (tab) {
        LocalLibraryTab.Songs -> {
            val songs = library.catalog.songs
                .filterLocalTracks(query, library.catalog.mediaByUri)
                .filterByLocalQuality(effectiveQualityFilter, library.catalog.mediaByUri, nowMs)
            localTrackItems(
                keyPrefix = "local-song",
                tracks = songs,
                context = songs,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Albums -> items(
            library.catalog.albums
                .filterLocalAlbums(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(effectiveQualityFilter, library.catalog.mediaByUri, nowMs) },
            key = { "local-album-${it.key}" },
            contentType = { "local-group" }
        ) { album ->
            LocalGroupBlock(
                title = album.title.ifBlank { LocalDerbisStrings.current.localUnknownAlbum },
                subtitle = localGroupSubtitle(album.artist, album.tracks.size, album.year),
                artworkModel = album.artworkModel,
                icon = Icons.Rounded.Album,
                expanded = expandedGroupKey == album.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == album.key) null else album.key) },
                onPlay = { album.tracks.firstOrNull()?.let { callbacks.onPlay(album.tracks, it) } },
                tracks = album.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Artists -> items(
            library.catalog.artists
                .filterLocalArtists(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(effectiveQualityFilter, library.catalog.mediaByUri, nowMs) },
            key = { "local-artist-${it.key}" },
            contentType = { "local-group" }
        ) { artist ->
            val strings = LocalDerbisStrings.current
            LocalGroupBlock(
                title = artist.name.ifBlank { strings.localUnknownArtist },
                subtitle = localGroupSubtitle(
                    "${artist.albumCount} ${strings.albumsPlain}",
                    artist.tracks.size,
                    ""
                ),
                artworkModel = artist.artworkModel,
                icon = Icons.Rounded.Person,
                expanded = expandedGroupKey == artist.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == artist.key) null else artist.key) },
                onPlay = { artist.tracks.firstOrNull()?.let { callbacks.onPlay(artist.tracks, it) } },
                tracks = artist.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Folders -> items(
            library.catalog.folders
                .filterLocalFolders(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(effectiveQualityFilter, library.catalog.mediaByUri, nowMs) },
            key = { "local-folder-${it.key}" },
            contentType = { "local-group" }
        ) { folder ->
            LocalGroupBlock(
                title = folder.name.ifBlank { folder.volumeName },
                subtitle = localGroupSubtitle(folder.path, folder.tracks.size, ""),
                artworkModel = folder.tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
                icon = Icons.Rounded.Folder,
                expanded = expandedGroupKey == folder.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == folder.key) null else folder.key) },
                onPlay = { folder.tracks.firstOrNull()?.let { callbacks.onPlay(folder.tracks, it) } },
                tracks = folder.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks,
                onHide = { callbacks.onToggleFolderHidden(folder.key, true) }
            )
        }
    }
    if (library.catalog.hiddenDuplicateCount > 0) {
        item(key = "local-duplicates", contentType = "local-note") {
            Text(
                text = "${LocalDerbisStrings.current.localDuplicatesHidden} · ${library.catalog.hiddenDuplicateCount}",
                color = DerbisMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun LazyListScope.localTrackItems(
    keyPrefix: String,
    tracks: List<Track>,
    context: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    mediaByUri: Map<String, LocalMediaEntity>,
    callbacks: LocalLibraryCallbacks
) {
    items(tracks, key = { "$keyPrefix-${it.streamUrl}" }, contentType = { "local-track" }) { track ->
        LibraryTrackRow(
            track = track,
            selected = false,
            selectionActive = false,
            isCurrent = currentTrack?.streamUrl == track.streamUrl,
            isPlaying = isPlaying && currentTrack?.streamUrl == track.streamUrl,
            isFavorite = track.id in favoriteIds,
            isDownloaded = true,
            downloadProgress = null,
            metadata = if (track.streamUrl in unavailableUris) {
                LocalDerbisStrings.current.localFileUnavailable
            } else {
                mediaByUri[track.streamUrl]?.localAudioSummary()
            },
            onClick = { callbacks.onPlay(context, track) },
            onLongClick = { callbacks.onAddToQueue(listOf(track)) },
            onFavorite = { callbacks.onToggleFavorite(track) },
            onDownload = {},
            onQueue = { callbacks.onAddToQueue(listOf(track)) },
            onEditTags = mediaByUri[track.streamUrl]
                ?.takeIf { it.isSafeTagEditorFormat() }
                ?.let { { callbacks.onEditTags(track) } }
        )
    }
}

@Composable
private fun LocalLibraryTabs(
    selected: LocalLibraryTab,
    onSelect: (LocalLibraryTab) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocalLibraryTab.entries.forEach { entry ->
            val active = entry == selected
            Column(
                modifier = Modifier.clickable { onSelect(entry) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.label(strings),
                    color = if (active) DerbisText else DerbisMuted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 2.dp)
                        .background(
                            if (active) DerbisCyan else Color.Transparent,
                            RoundedCornerShape(99.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun LocalLibraryQualityFilters(
    filters: List<LocalLibraryQualityFilter>,
    selected: LocalLibraryQualityFilter,
    onSelect: (LocalLibraryQualityFilter) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val active = filter == selected
            val label = when (filter) {
                LocalLibraryQualityFilter.All -> strings.all
                LocalLibraryQualityFilter.Lossless -> "LOSSLESS"
                LocalLibraryQualityFilter.HighBitrate -> "320K+"
                LocalLibraryQualityFilter.Recent -> strings.localLibraryRecentFilterLabel()
            }
            FilterChip(
                selected = active,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                shapes = FilterChipDefaults.shapes(
                    shape = MaterialTheme.shapes.small,
                    selectedShape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.medium
                ),
                colors = FilterChipDefaults.tonalFilterChipColors(),
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
}

internal fun localLibraryQualityFilters(
    media: Collection<LocalMediaEntity>,
    nowMs: Long
): List<LocalLibraryQualityFilter> = buildList {
    add(LocalLibraryQualityFilter.All)
    if (media.any { it.isLosslessLocalMedia() }) add(LocalLibraryQualityFilter.Lossless)
    if (media.any { it.bitrate >= LOCAL_HIGH_BITRATE_BPS }) add(LocalLibraryQualityFilter.HighBitrate)
    if (media.any { it.dateAddedMs >= nowMs - LOCAL_RECENT_WINDOW_MS }) add(LocalLibraryQualityFilter.Recent)
}

internal fun LocalMediaEntity.matchesLocalQualityFilter(
    filter: LocalLibraryQualityFilter,
    nowMs: Long = System.currentTimeMillis()
): Boolean = when (filter) {
    LocalLibraryQualityFilter.All -> true
    LocalLibraryQualityFilter.Lossless -> isLosslessLocalMedia()
    LocalLibraryQualityFilter.HighBitrate -> bitrate >= LOCAL_HIGH_BITRATE_BPS
    LocalLibraryQualityFilter.Recent -> dateAddedMs >= nowMs - LOCAL_RECENT_WINDOW_MS
}

private fun LocalMediaEntity.isLosslessLocalMedia(): Boolean {
    val mime = mimeType.lowercase()
    val extension = displayName.substringAfterLast('.', "").lowercase()
    return mime.contains("flac") ||
        mime.contains("alac") ||
        mime.contains("wav") ||
        mime.contains("aiff") ||
        mime.contains("ape") ||
        extension in LOCAL_LOSSLESS_EXTENSIONS
}

private fun List<Track>.filterByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>,
    nowMs: Long
): List<Track> {
    if (filter == LocalLibraryQualityFilter.All) return this
    return filter { track -> mediaByUri[track.streamUrl]?.matchesLocalQualityFilter(filter, nowMs) == true }
}

private fun LocalAlbumGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>,
    nowMs: Long
): LocalAlbumGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri, nowMs)
    if (filtered.isEmpty()) return null
    return copy(
        artworkModel = filtered.firstOrNull()?.thumbnailUrl.orEmpty(),
        durationMs = filtered.sumOf { it.durationMs.coerceAtLeast(0L) },
        tracks = filtered
    )
}

private fun LocalArtistGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>,
    nowMs: Long
): LocalArtistGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri, nowMs)
    if (filtered.isEmpty()) return null
    val filteredAlbumCount = filtered
        .map { it.album.trim().lowercase() }
        .filter(String::isNotEmpty)
        .distinct()
        .size
    return copy(
        artworkModel = filtered.firstOrNull()?.thumbnailUrl.orEmpty(),
        albumCount = filteredAlbumCount.takeIf { it > 0 } ?: albumCount,
        tracks = filtered
    )
}

private fun LocalFolderGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>,
    nowMs: Long
): LocalFolderGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri, nowMs)
    if (filtered.isEmpty()) return null
    return copy(
        durationMs = filtered.sumOf { it.durationMs.coerceAtLeast(0L) },
        tracks = filtered
    )
}

private fun LocalMediaEntity.localAudioSummary(): String? {
    val extension = displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
    val format = when {
        extension.isNotBlank() -> extension
        mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
        mimeType.contains("flac", ignoreCase = true) -> "FLAC"
        mimeType.contains("wav", ignoreCase = true) -> "WAV"
        mimeType.contains("ogg", ignoreCase = true) -> "OGG"
        mimeType.contains("opus", ignoreCase = true) -> "OPUS"
        else -> mimeType.substringAfter('/').substringBefore(';').uppercase(Locale.ROOT)
    }
    val bitrateLabel = bitrate
        .takeIf { it > 0 }
        ?.div(1_000)
        ?.let { "$it kbps" }
    return listOf(format, bitrateLabel)
        .filterNotNull()
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
}

private const val LOCAL_HIGH_BITRATE_BPS = 320_000
private const val LOCAL_RECENT_WINDOW_MS = 30L * 24L * 60L * 60L * 1_000L
private val LOCAL_LOSSLESS_EXTENSIONS = setOf("flac", "alac", "wav", "wave", "aiff", "aif", "ape")

internal fun localLibraryLeadingItemCount(
    library: LocalLibraryUiState,
    nowMs: Long
): Int {
    if (!library.permissionGranted || library.catalog.totalCount == 0) return 0
    val qualityFilterCount = localLibraryQualityFilters(
        library.catalog.mediaByUri.values,
        nowMs
    ).size
    return 2 + if (qualityFilterCount > 1) 1 else 0
}

internal fun localLibraryAlphabetIndex(
    library: LocalLibraryUiState,
    tab: LocalLibraryTab,
    qualityFilter: LocalLibraryQualityFilter,
    query: String,
    nowMs: Long,
    songsAlphabetical: Boolean
): LocalLibraryAlphabetIndex {
    if (
        !library.permissionGranted ||
        library.catalog.totalCount == 0 ||
        tab == LocalLibraryTab.Folders ||
        tab == LocalLibraryTab.Songs && !songsAlphabetical
    ) {
        return LocalLibraryAlphabetIndex()
    }
    val availableFilters = localLibraryQualityFilters(library.catalog.mediaByUri.values, nowMs)
    val effectiveFilter = qualityFilter.takeIf { it in availableFilters } ?: LocalLibraryQualityFilter.All
    val labels = when (tab) {
        LocalLibraryTab.Songs -> library.catalog.songs
            .filterLocalTracks(query, library.catalog.mediaByUri)
            .filterByLocalQuality(effectiveFilter, library.catalog.mediaByUri, nowMs)
            .map(Track::title)
        LocalLibraryTab.Albums -> library.catalog.albums
            .filterLocalAlbums(query, library.catalog.mediaByUri)
            .mapNotNull { it.filteredByLocalQuality(effectiveFilter, library.catalog.mediaByUri, nowMs) }
            .map(LocalAlbumGroup::title)
        LocalLibraryTab.Artists -> library.catalog.artists
            .filterLocalArtists(query, library.catalog.mediaByUri)
            .mapNotNull { it.filteredByLocalQuality(effectiveFilter, library.catalog.mediaByUri, nowMs) }
            .map(LocalArtistGroup::name)
        LocalLibraryTab.Folders -> emptyList()
    }
    val targets = buildList {
        var previous: Char? = null
        labels.forEachIndexed { index, label ->
            val letter = localAlphabetLetter(label) ?: return@forEachIndexed
            if (letter != previous) {
                add(LocalLibraryAlphabetTarget(letter, index))
                previous = letter
            }
        }
    }
    return LocalLibraryAlphabetIndex(targets = targets, itemCount = labels.size)
}

@Composable
internal fun LocalLibraryAlphabetRail(
    index: LocalLibraryAlphabetIndex,
    activeLetter: Char?,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!index.visible) return
    fun jumpAt(y: Float, height: Int) {
        if (height <= 0 || index.targets.isEmpty()) return
        val slot = height.toFloat() / index.targets.size
        val targetIndex = (y / slot).toInt().coerceIn(0, index.targets.lastIndex)
        onJump(index.targets[targetIndex].contentIndex)
    }
    Surface(
        color = DerbisPanelSoft.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, DerbisMuted.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .width(28.dp)
            .pointerInput(index.targets) {
                detectDragGestures(
                    onDragStart = { offset -> jumpAt(offset.y, size.height) },
                    onDrag = { change, _ ->
                        change.consume()
                        jumpAt(change.position.y, size.height)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            index.targets.forEach { target ->
                val active = target.letter == activeLetter
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(14.dp)
                        .clickable { onJump(target.contentIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = target.letter.toString(),
                        color = if (active) DerbisCyan else DerbisMuted,
                        fontSize = 8.5.sp,
                        fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun localAlphabetLetter(value: String): Char? = normalizeLibraryText(value)
    .trimStart()
    .firstOrNull()
    ?.takeIf { it in 'a'..'z' }
    ?.uppercaseChar()

private const val LOCAL_ALPHABET_MIN_ITEMS = 50
private const val LOCAL_ALPHABET_MIN_LETTERS = 5

@Composable
private fun LocalLibraryActions(library: LocalLibraryUiState, callbacks: LocalLibraryCallbacks) {
    val strings = LocalDerbisStrings.current
    Surface(
        color = DerbisGlass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.localOnDevice, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = library.message.ifBlank { strings.localOnDeviceSubtitle },
                        color = DerbisMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (library.scanning) {
                    CircularProgressIndicator(color = DerbisCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    IconButton(onClick = callbacks.onQuickScan) {
                        Icon(Icons.Rounded.Refresh, strings.localScanQuick, tint = DerbisCyan)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryCategoryChip(strings.localScanFull, selected = false, onClick = callbacks.onFullScan)
                LibraryCategoryChip(strings.localRebuildDerbis, selected = false, onClick = callbacks.onRebuildDerbis)
            }
        }
    }
}

@Composable
private fun LocalLibraryPermissionCard(onGrant: () -> Unit) {
    val strings = LocalDerbisStrings.current
    Surface(
        color = DerbisPanelSoft,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onGrant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = DerbisCyan, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.localPermissionRequired,
                    color = DerbisText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(strings.localGrantPermission, color = DerbisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocalGroupBlock(
    title: String,
    subtitle: String,
    artworkModel: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    mediaByUri: Map<String, LocalMediaEntity>,
    callbacks: LocalLibraryCallbacks,
    onHide: (() -> Unit)? = null
) {
    val strings = LocalDerbisStrings.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = if (expanded) DerbisCyan.copy(alpha = 0.08f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (artworkModel.isNotBlank()) {
                    LibraryArtwork(
                        url = artworkModel,
                        title = title,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        selected = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(DerbisPanelSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = DerbisMuted, modifier = Modifier.size(24.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = DerbisText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = DerbisMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onHide != null) {
                    IconButton(onClick = onHide, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            strings.localHideFolder,
                            tint = DerbisMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DerbisPlayAllIconButton(onClick = onPlay, size = 30.dp)
            }
        }
        if (expanded) {
            tracks.forEach { track ->
                LibraryTrackRow(
                    track = track,
                    selected = false,
                    selectionActive = false,
                    isCurrent = currentTrack?.streamUrl == track.streamUrl,
                    isPlaying = isPlaying && currentTrack?.streamUrl == track.streamUrl,
                    isFavorite = track.id in favoriteIds,
                    isDownloaded = true,
                    downloadProgress = null,
                    metadata = if (track.streamUrl in unavailableUris) {
                        strings.localFileUnavailable
                    } else {
                        mediaByUri[track.streamUrl]?.localAudioSummary()
                    },
                    onClick = { callbacks.onPlay(tracks, track) },
                    onLongClick = { callbacks.onAddToQueue(listOf(track)) },
                    onFavorite = { callbacks.onToggleFavorite(track) },
                    onDownload = {},
                    onQueue = { callbacks.onAddToQueue(listOf(track)) },
                    onEditTags = mediaByUri[track.streamUrl]
                        ?.takeIf { it.isSafeTagEditorFormat() }
                        ?.let { { callbacks.onEditTags(track) } },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

private fun localGroupSubtitle(detail: String, trackCount: Int, year: String): String = listOf(
    detail.trim(),
    "$trackCount",
    year.trim()
).filter { it.isNotEmpty() }.joinToString(" · ")

internal fun List<Track>.filterLocalTracks(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<Track> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { track ->
        mediaByUri[track.streamUrl]?.matchesFullTagQuery(query) == true ||
            normalizeLibraryText(track.title).contains(clean) ||
            normalizeLibraryText(track.artist).contains(clean) ||
            normalizeLibraryText(track.album).contains(clean)
    }
}

internal fun List<LocalAlbumGroup>.filterLocalAlbums(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalAlbumGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.title).contains(clean) ||
            normalizeLibraryText(group.artist).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}

internal fun List<LocalArtistGroup>.filterLocalArtists(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalArtistGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.name).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}

internal fun List<LocalFolderGroup>.filterLocalFolders(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalFolderGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.name).contains(clean) ||
            normalizeLibraryText(group.path).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}
