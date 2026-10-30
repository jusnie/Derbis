@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.components.DerbisBackButton
import com.yasuda.derbis.ui.components.DerbisSectionHeader
import com.yasuda.derbis.ui.components.DerbisPlayAllIconButton
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import com.yasuda.derbis.ui.icons.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.derbisPressable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.AlbumHit
import com.yasuda.derbis.domain.ExploreCategory
import com.yasuda.derbis.domain.ExploreZone
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.carouselDepthEnabled
import com.yasuda.derbis.ui.components.derbisCarouselDepth
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.theme.DerbisBlack
import com.yasuda.derbis.ui.theme.DerbisBlue
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisOrange
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisPanelSoft
import com.yasuda.derbis.ui.theme.DerbisPink
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm
import com.yasuda.derbis.ui.theme.DerbisViolet
import java.nio.charset.StandardCharsets
import java.util.Base64

internal const val ExploreNewReleasesDestination = "explore-destination-new-releases"
internal const val ExploreMoodsDestination = "explore-destination-moods"
private const val ExploreMoodDestinationPrefix = "explore-destination-mood:"
private const val ExploreCategoryDestinationPrefix = "explore-destination-provider-category:"
private val ExploreDestinationHeaderHeight = 66.dp
private val ExploreMoodsHeroHeight = 184.dp
private val ExploreDestinationBottomInset = 130.dp

internal fun exploreMoodDestination(zoneId: String): String = "$ExploreMoodDestinationPrefix$zoneId"
internal fun exploreMoodDestinationId(destination: String?): String? =
    destination?.takeIf { it.startsWith(ExploreMoodDestinationPrefix) }
        ?.removePrefix(ExploreMoodDestinationPrefix)
        ?.takeIf(String::isNotBlank)

internal fun exploreCategoryDestination(category: ExploreCategory): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val fields = listOf(category.params, category.title, category.section).map { value ->
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }
    return ExploreCategoryDestinationPrefix + fields.joinToString(".") + ".${category.sectionIndex}"
}

internal fun exploreCategoryDestinationValue(destination: String?): ExploreCategory? {
    val payload = destination
        ?.takeIf { value -> value.startsWith(ExploreCategoryDestinationPrefix) }
        ?.removePrefix(ExploreCategoryDestinationPrefix)
        ?: return null
    val fields = payload.split('.', limit = 4)
    if (fields.size != 4) return null
    return runCatching {
        val decoder = Base64.getUrlDecoder()
        fun decode(value: String): String = String(decoder.decode(value), StandardCharsets.UTF_8)
        ExploreCategory(
            params = decode(fields[0]),
            title = decode(fields[1]),
            section = decode(fields[2]),
            sectionIndex = fields[3].toInt()
        )
    }.getOrNull()?.takeIf { category -> category.params.isNotBlank() && category.title.isNotBlank() }
}

@Composable
internal fun ExploreCollectionDestinationScreen(
    identity: String,
    title: String,
    subtitle: String?,
    zone: ExploreZone?,
    tracks: List<Track>,
    isLoading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: DerbisStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    bottomInset: Dp = ExploreDestinationBottomInset
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val rotationBucket = remember(identity) {
        exploreGenreRotationBucket(System.currentTimeMillis())
    }
    val trackStructure = remember(tracks) { tracks.map { track -> track.id } }
    val editorialStructure = remember(trackStructure, identity, rotationBucket) {
        buildExploreGenreEditorial(
            tracks = tracks,
            zoneId = zone?.id ?: identity,
            rotationBucket = rotationBucket
        )
    }
    val editorial = remember(editorialStructure, tracks) {
        refreshExploreGenreEditorialMetadata(editorialStructure, tracks)
    }
    val categoryPalette = exploreCategoryPalette(identity)
    val accent = zone?.let { value -> Color(value.accentStart) } ?: categoryPalette.first
    val headerColor = remember(accent) { lerp(accent, DerbisBlack, ExploreCollectionHeaderShade) }
    val listState = rememberLazyListState()
    val heroScrolledAway by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    ExploreDestinationSurface(
        title = title,
        subtitle = subtitle,
        strings = strings,
        onBack = onBack,
        headerColor = headerColor,
        titleVisible = tracks.isEmpty() || heroScrolledAway,
        trailing = if (tracks.isNotEmpty() && heroScrolledAway) {
            { ExploreCollectionPlayAllButton(strings = strings, onPlayAll = onPlayAll) }
        } else {
            null
        },
        bottomInset = bottomInset
    ) { contentPadding ->
        ExploreCollectionDestinationContent(
            title = title,
            subtitle = subtitle,
            tracks = tracks,
            editorial = editorial,
            isLoading = isLoading,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            contentPadding = contentPadding,
            listState = listState,
            headerColor = headerColor,
            onPlayAll = onPlayAll,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

@Composable
private fun ExploreCollectionPlayAllButton(
    strings: DerbisStrings,
    onPlayAll: () -> Unit,
    size: Dp = 42.dp
) {
    DerbisPlayAllIconButton(onClick = onPlayAll, size = size, contentDescription = strings.playAll)
}

@Composable
private fun ExploreCollectionDestinationContent(
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    isLoading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: DerbisStrings,
    contentPadding: PaddingValues,
    listState: LazyListState,
    headerColor: Color,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    when {
        isLoading && tracks.isEmpty() -> ExploreCollectionLoading(contentPadding)
        tracks.isEmpty() -> ExploreCollectionEmpty(
            contentPadding = contentPadding,
            message = strings.exploreEmpty
        )
        else -> ExploreCollectionList(
            title = title,
            subtitle = subtitle,
            tracks = tracks,
            editorial = editorial,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            contentPadding = contentPadding,
            listState = listState,
            headerColor = headerColor,
            onPlayAll = onPlayAll,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

@Composable
private fun ExploreCollectionLoading(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = DerbisCyan, strokeWidth = 3.dp)
    }
}

@Composable
private fun ExploreCollectionEmpty(
    contentPadding: PaddingValues,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = DerbisMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExploreCollectionList(
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: DerbisStrings,
    contentPadding: PaddingValues,
    listState: LazyListState,
    headerColor: Color,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ExploreCollectionGutter,
            end = ExploreCollectionGutter,
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(ExploreSegmentGap)
    ) {
        exploreCollectionHeroItem(
            title = title,
            subtitle = subtitle,
            headerColor = headerColor,
            topInset = contentPadding.calculateTopPadding(),
            track = tracks.first(),
            strings = strings,
            onPlayAll = onPlayAll,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreFeaturedItems(
            tracks = editorial.featured,
            currentTrackId = currentTrackId,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreArtistItems(
            artists = editorial.artists,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreAlbumItems(
            albums = editorial.albums,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreEssentialItems(
            tracks = editorial.essentials,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

private fun LazyListScope.exploreCollectionHeroItem(
    title: String,
    subtitle: String?,
    headerColor: Color,
    topInset: Dp,
    track: Track,
    strings: DerbisStrings,
    onPlayAll: () -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    item(key = "explore-collection-hero") {
        LaunchedEffect(track.id) {
            onRequestTrackArtwork(track)
        }
        ExploreCollectionHero(
            title = title,
            subtitle = subtitle,
            headerColor = headerColor,
            topInset = topInset,
            artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
            strings = strings,
            onPlayAll = onPlayAll
        )
    }
}

private fun LazyListScope.exploreFeaturedItems(
    tracks: List<Track>,
    currentTrackId: String?,
    strings: DerbisStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "explore-genre-popular-header") {
        ExploreGenreSectionHeader(strings.popularTracks)
    }
    item(key = "explore-genre-popular") {
        val featuredState = rememberLazyListState()
        val featuredDepth = carouselDepthEnabled()
        LazyRow(
            state = featuredState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = tracks,
                key = { track -> "explore-featured-${track.id}" }
            ) { track ->
                LaunchedEffect(track.id) {
                    onRequestTrackArtwork(track)
                }
                Box(
                    modifier = Modifier.derbisCarouselDepth(
                        featuredState,
                        "explore-featured-${track.id}",
                        featuredDepth
                    )
                ) {
                    ExploreGenreTrackCard(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = { onPlayTrack(track) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.exploreArtistItems(
    artists: List<ExploreGenreArtistCard>,
    strings: DerbisStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (artists.isEmpty()) return
    item(key = "explore-genre-artists-header") {
        ExploreGenreSectionHeader(strings.artists)
    }
    item(key = "explore-genre-artists") {
        val artistState = rememberLazyListState()
        val artistDepth = carouselDepthEnabled()
        LazyRow(
            state = artistState,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            itemsIndexed(
                items = artists,
                key = { _, artist -> "explore-artist-${artist.key}" }
            ) { index, artist ->
                LaunchedEffect(artist.track.id) {
                    onRequestTrackArtwork(artist.track)
                }
                Box(
                    modifier = Modifier.derbisCarouselDepth(
                        artistState,
                        "explore-artist-${artist.key}",
                        artistDepth
                    )
                ) {
                    ExploreGenreArtistCard(
                        artist = artist,
                        shape = exploreArtistShape(index),
                        onClick = { onPlayTrack(artist.track) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.exploreAlbumItems(
    albums: List<ExploreGenreAlbumCard>,
    strings: DerbisStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (albums.isEmpty()) return
    item(key = "explore-genre-albums-header") {
        ExploreGenreSectionHeader(strings.albumsPlain)
    }
    item(key = "explore-genre-albums") {
        val albumState = rememberLazyListState()
        val albumDepth = carouselDepthEnabled()
        LazyRow(
            state = albumState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = albums,
                key = { album -> "explore-album-${album.key}" }
            ) { album ->
                LaunchedEffect(album.track.id) {
                    onRequestTrackArtwork(album.track)
                }
                Box(
                    modifier = Modifier.derbisCarouselDepth(
                        albumState,
                        "explore-album-${album.key}",
                        albumDepth
                    )
                ) {
                    ExploreGenreAlbumCard(
                        album = album,
                        onClick = { onPlayTrack(album.track) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.exploreEssentialItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: DerbisStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    item(key = "explore-genre-songs-header") {
        ExploreGenreSectionHeader(strings.songsPlain)
    }
    itemsIndexed(
        items = tracks,
        key = { _, track -> "explore-destination-track-${track.id}" }
    ) { index, track ->
        LaunchedEffect(track.id) {
            onRequestTrackArtwork(track)
        }
        ExploreDestinationTrackRow(
            track = track,
            shape = exploreSegmentShape(index = index, count = tracks.size),
            isCurrent = track.id == currentTrackId,
            isPlaying = isPlaying && track.id == currentTrackId,
            onClick = { onPlayTrack(track) }
        )
    }
}

@Composable
private fun ExploreGenreSectionHeader(title: String) {
    DerbisSectionHeader(title = title, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun ExploreGenreTrackCard(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val artwork = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    val artworkShape = if (isCurrent) MaterialShapes.Cookie9Sided.toShape() else RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .width(148.dp)
            .derbisPressable(
                onClick = onClick,
                pressedScale = DerbisPressScale.Tile,
                role = Role.Button,
                onClickLabel = track.title
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(artworkShape)
                .background(DerbisPanel)
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(38.dp)
                    .background(
                        if (isCurrent) DerbisCyan else DerbisBlack.copy(alpha = 0.78f),
                        if (isCurrent) MaterialShapes.Cookie4Sided.toShape() else CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrent) DerbisBlack else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = track.title,
            color = DerbisText,
            fontSize = 13.5.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(13.5.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = DerbisMuted,
            fontSize = 11.5.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(11.5.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreGenreArtistCard(
    artist: ExploreGenreArtistCard,
    shape: Shape,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .derbisPressable(
                onClick = onClick,
                pressedScale = DerbisPressScale.Tile,
                role = Role.Button,
                onClickLabel = artist.name
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(112.dp)
                .clip(shape)
                .background(DerbisPanel)
        )
        Text(
            text = artist.name,
            color = DerbisText,
            fontSize = 12.5.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(12.5.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExploreGenreAlbumCard(
    album: ExploreGenreAlbumCard,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .derbisPressable(
                onClick = onClick,
                pressedScale = DerbisPressScale.Tile,
                role = Role.Button,
                onClickLabel = album.title
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DerbisPanel)
        )
        Text(
            text = album.title,
            color = DerbisText,
            fontSize = 13.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(13.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (album.artist.isNotBlank()) {
            Text(
                text = album.artist,
                color = DerbisMuted,
                fontSize = 11.5.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(11.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreCollectionHero(
    title: String,
    subtitle: String?,
    headerColor: Color,
    topInset: Dp,
    artworkUrl: String,
    strings: DerbisStrings,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .exploreFullBleed(ExploreCollectionGutter)
            .height(topInset + ExploreCollectionHeroHeight)
            .background(Brush.verticalGradient(listOf(headerColor, DerbisBlack)))
    ) {
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to headerColor.copy(alpha = 0.55f),
                        0.35f to Color.Transparent,
                        0.68f to DerbisBlack.copy(alpha = 0.62f),
                        1f to DerbisBlack
                    )
                )
        )
        ExploreExpressiveShape(
            shape = MaterialShapes.SoftBurst.toShape(),
            color = lerp(headerColor, Color.White, 0.18f).copy(alpha = 0.42f),
            size = 196.dp,
            rotation = 14f,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-58).dp, y = 34.dp)
        )
        ExploreExpressiveShape(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            color = lerp(headerColor, Color.White, 0.30f).copy(alpha = 0.34f),
            size = 54.dp,
            rotation = -10f,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-96).dp, y = (-118).dp)
        )
        ExploreCollectionHeroText(
            title = title,
            subtitle = subtitle,
            strings = strings,
            onPlayAll = onPlayAll,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = ExploreCollectionGutter, end = ExploreCollectionGutter, bottom = 12.dp)
        )
    }
}

@Composable
private fun ExploreCollectionHeroText(
    title: String,
    subtitle: String?,
    strings: DerbisStrings,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = DerbisText,
            fontSize = 40.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(40.sp),
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = subtitle.orEmpty(),
                color = DerbisText.copy(alpha = 0.78f),
                fontSize = 14.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(14.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ExploreCollectionPlayAllButton(
                strings = strings,
                onPlayAll = onPlayAll,
                size = 56.dp
            )
        }
    }
}

private fun Modifier.exploreFullBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val gutterPx = gutter.roundToPx()
    val width = constraints.maxWidth + gutterPx * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-gutterPx, 0)
    }
}

@Composable
private fun exploreSegmentShape(index: Int, count: Int): Shape {
    val outer = ExploreSegmentOuterRadius
    val inner = ExploreSegmentInnerRadius
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return remember(top, bottom) {
        RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }
}

@Composable
private fun exploreArtistShape(index: Int): Shape {
    val shapes = remember {
        listOf(
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Puffy
        )
    }
    return shapes[index % shapes.size].toShape()
}

private val ExploreSegmentGap = 3.dp
private val ExploreSegmentOuterRadius = 26.dp
private val ExploreSegmentInnerRadius = 6.dp
private val ExploreCollectionGutter = 18.dp
private val ExploreCollectionHeroHeight = 260.dp
private const val ExploreCollectionHeaderShade = 0.32f

@Composable
internal fun ExploreNewReleasesDestinationScreen(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: DerbisStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit,
    bottomInset: Dp = ExploreDestinationBottomInset
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    ExploreDestinationSurface(
        title = strings.exploreNewReleases,
        subtitle = null,
        strings = strings,
        onBack = onBack,
        bottomInset = bottomInset
    ) { contentPadding ->
        when {
            isLoading && releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DerbisCyan, strokeWidth = 3.dp)
            }
            releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.exploreEmpty,
                    color = DerbisMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + 10.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = releases,
                    key = { release -> "ytm-release-${release.browseId.ifBlank { release.title + release.artist }}" }
                ) { release ->
                    ExploreDestinationReleaseRow(
                        release = release,
                        onClick = { onOpenRelease(release) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationReleaseRow(
    release: AlbumHit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(shape)
            .background(DerbisPanel.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = release.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = release.title,
                color = DerbisText,
                fontSize = 15.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(15.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(release.artist, release.year)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                color = DerbisMuted,
                fontSize = 12.5.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = DerbisText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun ExploreMoodsDestinationScreen(
    zones: List<ExploreZone>,
    categories: List<ExploreCategory>,
    categoryArtwork: Map<String, String>,
    isLoading: Boolean,
    strings: DerbisStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit,
    bottomInset: Dp = ExploreDestinationBottomInset
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val sections = remember(categories) { buildExploreCategorySections(categories) }
    val fallbackGenres = remember(zones) {
        destinationCuratedExploreZones(zones)
    }
    val listState = rememberLazyListState()
    val heroScrolledAway by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    ExploreDestinationSurface(
        title = strings.exploreMoods,
        subtitle = strings.exploreSubtitle,
        strings = strings,
        onBack = onBack,
        headerColor = DerbisBlack,
        titleVisible = heroScrolledAway,
        bottomInset = bottomInset
    ) { contentPadding ->
        ExploreMoodsList(
            contentPadding = contentPadding,
            listState = listState,
            sections = sections,
            fallbackGenres = fallbackGenres,
            categoryArtwork = categoryArtwork,
            isLoading = isLoading,
            strings = strings,
            onOpenZone = onOpenZone,
            onOpenCategory = onOpenCategory,
            onRequestCategoryArtwork = onRequestCategoryArtwork
        )
    }
}

internal fun destinationCuratedExploreZones(zones: List<ExploreZone>): List<ExploreZone> =
    zones.distinctBy { zone -> zone.id }

@Composable
private fun ExploreMoodsList(
    contentPadding: PaddingValues,
    listState: LazyListState,
    sections: List<ExploreCategorySection>,
    fallbackGenres: List<ExploreZone>,
    categoryArtwork: Map<String, String>,
    isLoading: Boolean,
    strings: DerbisStrings,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit
) {
    val bridgeZone = remember(sections, fallbackGenres) {
        exploreCuratedBridgeZone(sections, fallbackGenres)
    }
    val remainingFallbackGenres = remember(fallbackGenres, bridgeZone) {
        if (bridgeZone == null) fallbackGenres else fallbackGenres.drop(1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "moods-and-genres-hero") {
            ExploreMoodsHero(title = strings.exploreMoods, subtitle = strings.exploreSubtitle)
        }
        exploreMoodLoadingItems(isLoading, sections, strings)
        exploreProviderCategoryItems(
            sections = sections,
            categoryArtwork = categoryArtwork,
            strings = strings,
            bridgeZone = bridgeZone,
            onOpenZone = onOpenZone,
            onOpenCategory = onOpenCategory,
            onRequestCategoryArtwork = onRequestCategoryArtwork
        )
        exploreFallbackGenreItems(
            fallbackGenres = remainingFallbackGenres,
            onOpenZone = onOpenZone
        )
        exploreMoodEmptyItem(
            isLoading = isLoading,
            sections = sections,
            fallbackGenres = fallbackGenres,
            strings = strings
        )
    }
}

@Composable
private fun ExploreMoodsHero(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ExploreMoodsHeroHeight)
    ) {
        ExploreExpressiveShape(
            shape = MaterialShapes.Sunny.toShape(),
            color = colors.primaryContainer.copy(alpha = 0.62f),
            size = 112.dp,
            rotation = 18f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 26.dp, y = (-8).dp)
        )
        ExploreExpressiveShape(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = colors.tertiaryContainer.copy(alpha = 0.78f),
            size = 54.dp,
            rotation = -12f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-104).dp, y = 10.dp)
        )
        ExploreExpressiveShape(
            shape = MaterialShapes.Clover4Leaf.toShape(),
            color = colors.secondaryContainer.copy(alpha = 0.86f),
            size = 34.dp,
            rotation = 24f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 96.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = colors.onBackground,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .semantics { heading() }
            )
            Text(
                text = subtitle,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreExpressiveShape(
    shape: Shape,
    color: Color,
    size: Dp,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .rotate(rotation)
            .background(color, shape)
            .clearAndSetSemantics { }
    )
}

private fun LazyListScope.exploreMoodLoadingItems(
    isLoading: Boolean,
    sections: List<ExploreCategorySection>,
    strings: DerbisStrings
) {
    if (!isLoading || sections.isNotEmpty()) return
    item(key = "provider-moods-loading-title") {
        ExploreCategorySectionHeader(strings.exploreMoodSection)
    }
    items(
        count = 2,
        key = { index -> "provider-mood-loading-row-$index" }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(108.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExploreDiscoveryCategoryPlaceholder(Modifier.weight(1f).fillMaxHeight())
            ExploreDiscoveryCategoryPlaceholder(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

internal fun exploreCuratedBridgeZone(
    sections: List<ExploreCategorySection>,
    fallbackGenres: List<ExploreZone>
): ExploreZone? = fallbackGenres.firstOrNull()?.takeIf {
    sections.lastOrNull()?.categories?.size?.rem(2) == 1
}

private fun LazyListScope.exploreProviderCategoryItems(
    sections: List<ExploreCategorySection>,
    categoryArtwork: Map<String, String>,
    strings: DerbisStrings,
    bridgeZone: ExploreZone?,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit
) {
    sections.forEachIndexed { index, section ->
        exploreProviderCategorySection(
            section = section,
            categoryArtwork = categoryArtwork,
            strings = strings,
            trailingZone = bridgeZone.takeIf { index == sections.lastIndex },
            onOpenZone = onOpenZone,
            onOpenCategory = onOpenCategory,
            onRequestCategoryArtwork = onRequestCategoryArtwork
        )
    }
}

private fun LazyListScope.exploreProviderCategorySection(
    section: ExploreCategorySection,
    categoryArtwork: Map<String, String>,
    strings: DerbisStrings,
    trailingZone: ExploreZone?,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit
) {
    val title = exploreProviderSectionTitle(section, strings)
    val prominent = section.presentation == ExploreCategoryPresentation.Atmospheric
    item(key = "${section.key}-header") {
        ExploreCategorySectionHeader(title)
    }
    val pairs = section.categories.chunked(2)
    items(
        count = pairs.size,
        key = { index -> "${section.key}-pair-$index" }
    ) { index ->
        val pair = pairs[index]
        ExploreProviderCategoryRow(
            pair = pair,
            prominent = prominent,
            categoryArtwork = categoryArtwork,
            trailingZone = trailingZone.takeIf { index == pairs.lastIndex && pair.size == 1 },
            onOpenZone = onOpenZone,
            onOpenCategory = onOpenCategory,
            onRequestCategoryArtwork = onRequestCategoryArtwork
        )
    }
}

private fun exploreProviderSectionTitle(
    section: ExploreCategorySection,
    strings: DerbisStrings
): String = section.providerTitle.ifBlank {
    when (section.presentation) {
        ExploreCategoryPresentation.Atmospheric -> strings.exploreMoodSection
        ExploreCategoryPresentation.Structured -> strings.genres
        ExploreCategoryPresentation.Mixed -> strings.exploreMoods
    }
}

@Composable
private fun ExploreProviderCategoryRow(
    pair: List<ExploreCategory>,
    prominent: Boolean,
    categoryArtwork: Map<String, String>,
    trailingZone: ExploreZone?,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (prominent) 112.dp else 102.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pair.forEach { category ->
            LaunchedEffect(category.params) {
                onRequestCategoryArtwork(category.params, prominent)
            }
            ExploreDiscoveryCategoryCard(
                title = category.title,
                identity = category.params,
                artworkUrl = categoryArtwork[category.params].orEmpty(),
                prominent = prominent,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenCategory(category) }
            )
        }
        if (pair.size == 1) {
            if (trailingZone != null) {
                val artworkUrl = rememberExploreMoodArtworkUrl(trailingZone)
                ExploreDiscoveryCategoryCard(
                    title = trailingZone.label,
                    identity = trailingZone.id,
                    artworkUrl = artworkUrl,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onOpenZone(trailingZone) }
                )
            } else {
                ExploreDiscoveryCategoryPlaceholder(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

private fun LazyListScope.exploreFallbackGenreItems(
    fallbackGenres: List<ExploreZone>,
    onOpenZone: (ExploreZone) -> Unit
) {
    if (fallbackGenres.isEmpty()) return
    items(
        items = fallbackGenres.chunked(2),
        key = { pair -> "editorial-genres-${pair.joinToString("|") { it.id }}" }
    ) { pair ->
        ExploreFallbackGenreRow(pair = pair, onOpenZone = onOpenZone)
    }
}

@Composable
private fun ExploreFallbackGenreRow(
    pair: List<ExploreZone>,
    onOpenZone: (ExploreZone) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(102.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pair.forEach { zone ->
            val artworkUrl = rememberExploreMoodArtworkUrl(zone)
            ExploreDiscoveryCategoryCard(
                title = zone.label,
                identity = zone.id,
                artworkUrl = artworkUrl,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenZone(zone) }
            )
        }
        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}

private fun LazyListScope.exploreMoodEmptyItem(
    isLoading: Boolean,
    sections: List<ExploreCategorySection>,
    fallbackGenres: List<ExploreZone>,
    strings: DerbisStrings
) {
    if (isLoading || sections.isNotEmpty() || fallbackGenres.isNotEmpty()) return
    item(key = "moods-and-genres-empty") {
        Text(
            text = strings.exploreEmpty,
            color = DerbisMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 42.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExploreCategorySectionHeader(title: String) {
    DerbisSectionHeader(title = title)
}

private data class ExploreCategoryCardMetrics(
    val artworkSize: androidx.compose.ui.unit.Dp,
    val placeholderSize: androidx.compose.ui.unit.Dp,
    val titleSize: androidx.compose.ui.unit.TextUnit,
    val titleWidthFraction: Float
)

private fun exploreCategoryCardMetrics(prominent: Boolean): ExploreCategoryCardMetrics =
    if (prominent) {
        ExploreCategoryCardMetrics(
            artworkSize = 86.dp,
            placeholderSize = 38.dp,
            titleSize = 17.sp,
            titleWidthFraction = 0.74f
        )
    } else {
        ExploreCategoryCardMetrics(
            artworkSize = 78.dp,
            placeholderSize = 34.dp,
            titleSize = 16.sp,
            titleWidthFraction = 0.76f
        )
    }

@Composable
internal fun ExploreDiscoveryCategoryCard(
    title: String,
    identity: String,
    artworkUrl: String = "",
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    accentColors: Pair<Color, Color>? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    if (com.yasuda.derbis.ui.theme.DerbisIsYouTubeMusic) {
        YouTubeMusicMoodTile(title, (accentColors ?: exploreCategoryPalette(identity)).first, onClick, modifier, onLongClick)
        return
    }
    if (com.yasuda.derbis.ui.theme.DerbisIsAppleMusic) {
        AppleMusicCategoryTile(
            label = title, portrait = artworkUrl, active = false, onClick = onClick, onLongClick = onLongClick,
            longClickLabel = title, onPortraitError = {}, modifier = modifier, tint = accentColors?.first
        )
        return
    }
    if (com.yasuda.derbis.ui.theme.DerbisIsPrism) {
        SkinCategoryCard(title, artworkUrl, modifier, onLongClick, onClick)
        return
    }
    val fallbackUrl = remember(identity) { exploreMoodHardFallbackPortraitUrl(identity) }
    val effectiveArtwork = artworkUrl.ifBlank { fallbackUrl }
    val shape = RoundedCornerShape(16.dp)
    val metrics = exploreCategoryCardMetrics(prominent)
    val longTitle = title.length >= 14
    val titleSize = if (longTitle) metrics.titleSize * 0.92f else metrics.titleSize
    var artworkFailed by remember(effectiveArtwork) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Color(0xFF14141B))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
            .semantics { role = Role.Button }
            .derbisPressable(
                onClick = onClick,
                onLongClick = onLongClick,
                pressedScale = DerbisPressScale.Tile,
                role = Role.Button,
                onClickLabel = title
            )
    ) {
        // Real high-res artist photography background (no random colors)
        if (effectiveArtwork.isNotBlank() && !artworkFailed) {
            AsyncImage(
                model = effectiveArtwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                onError = { artworkFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient scrims for pristine text legibility over artist photo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.20f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f)
                    )
                )
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = titleSize,
            lineHeight = DerbisTypeRhythm.lineHeight(titleSize),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(0f, 2f), blurRadius = 8f)
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(metrics.titleWidthFraction)
                .padding(start = 14.dp, top = 14.dp, end = 6.dp)
        )
    }
}

@Composable
private fun BoxScope.ExploreDiscoveryCategoryArtwork(
    artworkUrl: String,
    metrics: ExploreCategoryCardMetrics
) {
    val artworkModifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(x = 14.dp, y = 12.dp)
        .size(metrics.artworkSize)
        .rotate(13f)
        .clip(RoundedCornerShape(9.dp))
    var artworkFailed by remember(artworkUrl) { mutableStateOf(false) }
    var artworkLoading by remember(artworkUrl) { mutableStateOf(artworkUrl.isNotBlank()) }
    Box(
        modifier = artworkModifier.background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUrl.isNotBlank() && !artworkFailed) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onLoading = { artworkLoading = true },
                onSuccess = { artworkLoading = false },
                onError = { artworkLoading = false; artworkFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (artworkLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(metrics.placeholderSize * 0.52f),
                color = Color.White.copy(alpha = 0.72f),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun ExploreDiscoveryCategoryPlaceholder(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DerbisPanelSoft,
                        DerbisPanel,
                        DerbisPanel.copy(alpha = 0.92f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), shape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 12.dp)
                .size(78.dp)
                .rotate(13f)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.06f))
        )
    }
}

@Composable
internal fun exploreCategoryPalette(identity: String): Pair<Color, Color> {
    val palette = listOf(
        Color(0xFF7C3AED) to Color(0xFF4C1D95),
        Color(0xFFD81B60) to Color(0xFF7A1538),
        Color(0xFF0F8A78) to Color(0xFF075E54),
        Color(0xFF246BCE) to Color(0xFF173F7A),
        Color(0xFFB96A16) to Color(0xFF70400F),
        Color(0xFF238636) to Color(0xFF145325),
        Color(0xFFB335B5) to Color(0xFF64236C),
        Color(0xFFD9571C) to Color(0xFF7B3215)
    )
    return palette[(identity.hashCode() and Int.MAX_VALUE) % palette.size]
}

@Composable
private fun ExploreDestinationSurface(
    title: String,
    subtitle: String?,
    strings: DerbisStrings,
    onBack: () -> Unit,
    headerColor: Color = DerbisBlack,
    titleVisible: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    bottomInset: Dp = ExploreDestinationBottomInset,
    content: @Composable (PaddingValues) -> Unit
) {
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        label = "explore-destination-title"
    )
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DerbisBlack)
            .zIndex(24f)
    ) {
        content(PaddingValues(top = statusBarTop + ExploreDestinationHeaderHeight, bottom = bottomInset))
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(1f)
                .background(headerColor.copy(alpha = headerColor.alpha * titleAlpha))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DerbisBackButton(onClick = onBack, contentDescription = strings.back)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = titleAlpha }
                    .then(if (titleVisible) Modifier else Modifier.clearAndSetSemantics { })
            ) {
                Text(
                    text = title,
                    color = DerbisText,
                    fontSize = 21.sp,
                    lineHeight = DerbisTypeRhythm.lineHeight(21.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf(String::isNotBlank)?.let { detail ->
                    Text(
                        text = detail,
                        color = DerbisMuted,
                        fontSize = 12.5.sp,
                        lineHeight = DerbisTypeRhythm.lineHeight(12.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun ExploreDestinationTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isCurrent) DerbisCyan.copy(alpha = 0.16f) else DerbisPanel.copy(alpha = 0.78f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(if (isCurrent) MaterialShapes.Cookie9Sided.toShape() else RoundedCornerShape(14.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = DerbisText,
                fontSize = 14.5.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(14.5.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = DerbisMuted,
                fontSize = 12.5.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier.size(40.dp).background(
                if (isCurrent) DerbisCyan else Color.White.copy(alpha = 0.08f),
                if (isCurrent) MaterialShapes.Cookie4Sided.toShape() else CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isCurrent) DerbisBlack else DerbisText,
                modifier = Modifier.size(if (isPlaying) 20.dp else 21.dp)
            )
        }
    }
}
