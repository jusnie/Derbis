/*
 * The Liquid Glass "Music Home" from the glass-skin mock, fed by real Derbis state.
 *
 * Layout, top to bottom: logo + gear, two-line greeting, goo filter chips (All / Music /
 * Podcasts / Derbis Jam), a jump-back-in grid, a Made-for-you album rail, New releases for you,
 * a Recently played list and a Your artists circle row — every section backed by live data and
 * hidden when it has none. There is intentionally no "Trending community playlists" rail: Derbis
 * has no community-playlist source to feed it, and the real_data scope forbids shipping the
 * mock's invented catalog. The Derbis Jam chip swaps the page for a live or idle Jam card; the
 * heavy lifting stays in the app's existing DerbisJamOverlay.
 *
 * The backdrop itself is painted by DerbisHomeAtmosphere (root) via GlassBackgroundEngine;
 * this screen draws only translucent chrome above it.
 */
package com.yasuda.derbis.ui

import androidx.compose.animation.animateColorAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Icon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yasuda.derbis.data.buildPersonalizedHomeAlbumShelf
import com.yasuda.derbis.domain.AlbumHit
import com.yasuda.derbis.domain.ArtistHit

import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.viewmodel.HomeRenderSnapshot
import com.yasuda.derbis.viewmodel.HomeViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val GlassInk = Color(0xFFF5F5F7)
private val GlassDim = Color(0xFFA1A1AA)
private val GlassCard = Color.White.copy(alpha = 0.055f)
private val GlassCardStrong = Color.White.copy(alpha = 0.09f)
private val GlassLine = Color.White.copy(alpha = 0.13f)
private val GlassPillRadius = 999.dp

private enum class GlassHomeFilter { All, Music, Podcasts, Jam }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun GlassMusicHomeScreen(
    viewModel: HomeViewModel,
    renderSnapshot: HomeRenderSnapshot,
    homeListState: LazyListState,
    onPullRefresh: () -> Unit,
    onTrackActions: (Track) -> Unit = {}
) {
    val state = renderSnapshot.state
    val derived = renderSnapshot.derived
    val strings = LocalDerbisStrings.current
    val clipboard = LocalClipboardManager.current
    val homeRefreshing by viewModel.homeRefreshing.collectAsStateWithLifecycle()
    val homePullState = rememberPullToRefreshState()
    val hour by rememberCurrentGreetingHour()

    var filter by rememberSaveable { mutableStateOf(GlassHomeFilter.All) }

    val greetingLines = remember(state.userName, strings, hour) {
        strings.formatGreeting(state.userName, hour)
            .split(", ", limit = 2)
            .map { it.uppercase() }
    }

    val recent = remember(state.recentListens) { state.recentListens.distinctBy { it.id } }
    val jumpTracks = remember(recent) { recent.take(6) }
    val newReleaseTracks = remember(derived.newReleases) { derived.newReleases?.tracks.orEmpty() }
    val homeAlbums = remember(
        state.homeAlbums,
        state.personalOrbitTracks,
        state.recentListens,
        state.favorites,
        derived.quickPicks?.tracks,
        newReleaseTracks,
        state.homeSections,
        state.charts,
        state.tracks,
        state.languageCode
    ) {
        buildPersonalizedHomeAlbumShelf(
            primaryAlbums = state.homeAlbums,
            personalTracks = state.personalOrbitTracks,
            recentTracks = state.recentListens,
            favoriteTracks = state.favorites,
            quickPickTracks = derived.quickPicks?.tracks.orEmpty(),
            localizedReleaseTracks = newReleaseTracks,
            localizedSections = state.homeSections,
            chartTracks = state.charts,
            fallbackTracks = state.tracks
        ).take(20)
    }
    val artists: List<ArtistHit> = remember(state.homeArtists, state.followedArtists) {
        val followedHits = state.followedArtists.map { followed ->
            ArtistHit(
                name = followed.name,
                subscribers = "",
                thumbnailUrl = followed.thumbnailUrl,
                accentStart = 0,
                accentEnd = 0,
                browseId = followed.browseId
            )
        }
        (followedHits + state.homeArtists)
            .distinctBy { it.browseId.ifBlank { it.name } }
            .take(14)
    }
    val recentRows = remember(recent) { recent.take(6) }
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomInset = (if (state.currentTrack != null) 158.dp else 92.dp) + navigationBarInset
    val topInset = derbisSafeTopInset() + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = onPullRefresh),
            contentPadding = PaddingValues(top = topInset, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "glass-header", contentType = "glass-header") {
                GlassHomeHeader(
                    greetingLines = greetingLines,
                    subtitle = "Pick up where you left off.",
                    onSearch = viewModel::openSearch,
                    onSettings = viewModel::openSettings
                )
            }
            item(key = "glass-chips", contentType = "glass-chips") {
                GlassHomeChips(
                    filter = filter,
                    onSelect = { filter = it }
                )
            }

            when (filter) {
                GlassHomeFilter.Jam -> {
                    item(key = "glass-jam", contentType = "glass-jam") {
                        GlassJamPreview(
                            jam = state.jam,
                            onOpenJam = viewModel::openJam,
                            onCopyCode = { code ->
                                clipboard.setText(AnnotatedString("Join my Derbis Jam: $code"))
                            }
                        )
                    }
                }
                GlassHomeFilter.Podcasts -> {
                    item(key = "glass-podcasts", contentType = "glass-podcasts") {
                        GlassEmptyNote("There's nothing from podcasts on your home yet.\nShows you follow will land here.")
                    }
                }
                GlassHomeFilter.All, GlassHomeFilter.Music -> {
                    if (jumpTracks.isNotEmpty()) {
                        item(key = "glass-jump-title", contentType = "glass-section-title") {
                            GlassSectionHeader(strings.continueListening)
                        }
                        item(key = "glass-jump-grid", contentType = "glass-jump-grid") {
                            GlassQuickPickGrid(
                                tracks = jumpTracks,
                                currentTrackId = state.currentTrack?.id,
                                onPlay = { track -> viewModel.playFrom(jumpTracks, track) },
                                onTrackActions = onTrackActions
                            )
                        }
                    }
                    if (homeAlbums.isNotEmpty()) {
                        item(key = "glass-made-title", contentType = "glass-section-title") {
                            GlassSectionHeader("Made for you")
                        }
                        item(key = "glass-made-rail", contentType = "glass-made-rail") {
                            GlassAlbumRail(
                                albums = homeAlbums,
                                onOpen = { album -> viewModel.openAlbum(album) }
                            )
                        }
                    }
                    if (newReleaseTracks.isNotEmpty()) {
                        item(key = "glass-new-title", contentType = "glass-section-title") {
                            GlassSectionHeader(strings.newReleases)
                        }
                        item(key = "glass-new-rail", contentType = "glass-new-rail") {
                            GlassReleaseRail(
                                tracks = newReleaseTracks,
                                onPlay = { track -> viewModel.playFrom(newReleaseTracks, track) },
                                onTrackActions = onTrackActions
                            )
                        }
                    }
                    // No "Trending community playlists" rail: Derbis has no community-playlist
                    // source, and shipping the mock's invented one was explicitly ruled out.
                    if (recentRows.isNotEmpty()) {
                        item(key = "glass-recent-title", contentType = "glass-section-title") {
                            GlassSectionHeader("Recently played")
                        }
                        item(key = "glass-recent-list", contentType = "glass-recent-list") {
                            GlassRecentList(
                                tracks = recentRows,
                                currentTrackId = state.currentTrack?.id,
                                onPlay = { track -> viewModel.playFrom(recentRows, track) },
                                onTrackActions = onTrackActions
                            )
                        }
                    }
                    if (artists.isNotEmpty()) {
                        item(key = "glass-artists-title", contentType = "glass-section-title") {
                            GlassSectionHeader(strings.topArtistsTitle)
                        }
                        item(key = "glass-artists-rail", contentType = "glass-artists-rail") {
                            GlassArtistCircleRow(
                                artists = artists,
                                onOpen = { hit -> viewModel.openArtistFromHit(hit) }
                            )
                        }
                    }
                    if (jumpTracks.isEmpty() && homeAlbums.isEmpty() && newReleaseTracks.isEmpty() && recentRows.isEmpty() && artists.isEmpty()) {
                        item(key = "glass-empty", contentType = "glass-empty") {
                            GlassEmptyNote("Nothing here yet.\nPlay a few tracks and your home will fill in.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassHomeHeader(
    greetingLines: List<String>,
    subtitle: String,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onSearch),
                contentAlignment = Alignment.CenterStart
            ) {
                com.yasuda.derbis.ui.components.DerbisVectorLogo(size = 40.dp, tint = GlassInk)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassCardStrong, RoundedCornerShape(14.dp))
                    .border(1.dp, GlassLine, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = GlassInk, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        greetingLines.forEach { line ->
            androidx.compose.material3.Text(
                text = line,
                color = GlassInk,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp,
                lineHeight = 42.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.Text(
            text = subtitle,
            color = GlassDim,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun GlassHomeChips(
    filter: GlassHomeFilter,
    onSelect: (GlassHomeFilter) -> Unit
) {
    val options = listOf(
        GlassHomeFilter.All to "All",
        GlassHomeFilter.Music to "Music",
        GlassHomeFilter.Podcasts to "Podcasts",
        GlassHomeFilter.Jam to "Derbis Jam"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (option, label) ->
            val selected = option == filter
            val background by animateColorAsState(
                targetValue = if (selected) Color.Transparent else GlassCard,
                label = "glass-chip-bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Color.Black else GlassInk,
                label = "glass-chip-fg"
            )
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(GlassPillRadius))
                    .then(
                        if (selected) {
                            Modifier.background(GlassInk)
                        } else {
                            Modifier
                                .background(background)
                                .border(1.dp, GlassLine, RoundedCornerShape(GlassPillRadius))
                        }
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (option == GlassHomeFilter.Jam) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp).padding(end = 0.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                androidx.compose.material3.Text(
                    text = label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GlassSectionHeader(title: String) {
    androidx.compose.material3.Text(
        text = title,
        color = GlassInk,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun GlassArtwork(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp = 11.dp
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context).data(url.takeIf { it.isNotBlank() }).crossfade(true).build()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(GlassCardStrong)
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassQuickPickGrid(
    tracks: List<Track>,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tracks.chunked(2).forEach { rowTracks ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassCard)
                            .border(1.dp, GlassLine, RoundedCornerShape(16.dp))
                            .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onTrackActions(track) })
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassArtwork(
                            url = track.thumbnailUrl,
                            contentDescription = track.title,
                            modifier = Modifier.size(44.dp),
                            corner = 11.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        androidx.compose.material3.Text(
                            text = track.title,
                            color = if (track.id == currentTrackId) GlassInk else GlassInk.copy(alpha = 0.92f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                if (rowTracks.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GlassAlbumRail(
    albums: List<AlbumHit>,
    onOpen: (AlbumHit) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        albums.forEach { album ->
            Column(modifier = Modifier.width(152.dp).clickable { onOpen(album) }) {
                GlassArtwork(
                    url = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxWidth().height(152.dp),
                    corner = 18.dp
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Text(
                    text = album.title,
                    color = GlassInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.material3.Text(
                    text = listOfNotNull(album.artist.takeIf { it.isNotBlank() }, album.year.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = GlassDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassReleaseRail(
    tracks: List<Track>,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tracks.forEach { track ->
            Column(
                modifier = Modifier
                    .width(152.dp)
                    .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onTrackActions(track) })
            ) {
                Box {
                    GlassArtwork(
                        url = track.thumbnailUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxWidth().height(152.dp),
                        corner = 18.dp
                    )
                    TextGlassBadge(text = "New", modifier = Modifier.padding(8.dp).align(Alignment.TopStart))
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Text(
                    text = track.title,
                    color = GlassInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.material3.Text(
                    text = listOfNotNull(track.artist.takeIf { it.isNotBlank() }, track.year.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = GlassDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TextGlassBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassPillRadius))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = Color.Black,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassRecentList(
    tracks: List<Track>,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(GlassCard)
            .border(1.dp, GlassLine, RoundedCornerShape(22.dp))
            .padding(6.dp)
    ) {
        tracks.forEach { track ->
            val current = track.id == currentTrackId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onTrackActions(track) })
                    .padding(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassArtwork(
                    url = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.size(44.dp),
                    corner = 8.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        text = track.title,
                        color = if (current) GlassInk else GlassInk.copy(alpha = 0.95f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    androidx.compose.material3.Text(
                        text = track.artist,
                        color = GlassDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                androidx.compose.material3.Text(
                    text = formatGlassTrackTime(track.durationMs),
                    color = GlassDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GlassArtistCircleRow(
    artists: List<ArtistHit>,
    onOpen: (ArtistHit) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        artists.forEach { artist ->
            Column(
                modifier = Modifier.width(76.dp).clickable { onOpen(artist) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassArtwork(
                    url = artist.thumbnailUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.size(76.dp),
                    corner = 999.dp
                )
                Spacer(Modifier.height(7.dp))
                androidx.compose.material3.Text(
                    text = artist.name,
                    color = GlassInk,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassJamPreview(
    jam: com.yasuda.derbis.feature.jam.JamUiState,
    onOpenJam: () -> Unit,
    onCopyCode: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val active = jam.isActive
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassCardStrong)
                .border(1.dp, GlassLine, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            if (!active) {
                androidx.compose.material3.Text(
                    text = "Listen together",
                    color = GlassInk,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Text(
                    text = "Start a jam and everyone hears the same song at the same moment. Share the code so friends can join.",
                    color = GlassDim,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(16.dp))
                GlassPrimaryButton(text = "Start a jam", onClick = onOpenJam)
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Text(
                    text = "Join with a code from a friend — open the jam from here.",
                    color = GlassDim,
                    fontSize = 11.5.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF4D6D)))
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Text(
                        text = "LIVE",
                        color = Color(0xFFFF4D6D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.material3.Text(
                        text = "${jam.session?.participants?.size ?: 1} listening",
                        color = GlassDim,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Text(
                    text = jam.code.ifBlank { "—" },
                    color = GlassInk,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = if (jam.isHost) "You're hosting. Friends join with this code." else "You're in this jam.",
                    color = GlassDim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPrimaryButton(text = "Open jam", onClick = onOpenJam, modifier = Modifier.weight(1f))
                    GlassSecondaryButton(text = "Copy code", onClick = {
                        if (jam.code.isNotBlank()) onCopyCode(jam.code)
                    })
                }
            }
        }
    }
}

@Composable
private fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = Color.Black,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCard)
            .border(1.dp, GlassLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = GlassInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlassEmptyNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(GlassCard)
            .border(1.dp, GlassLine, RoundedCornerShape(22.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = GlassDim,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}


private fun formatGlassTrackTime(durationMs: Long): String {
    if (durationMs <= 0L) return ""
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toInt()
    return "$minutes:" + if (seconds < 10) "0$seconds" else "$seconds"
}

