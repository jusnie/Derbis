/*
 * Liquid Glass artist page — default shell for the Liquid Glass (Prism) skin,
 * following the attached "Artist Glass Skin" mock (parallax hero, 3D album
 * carousel, accent-from-artwork, action pills, album tracklist, Artistpedia,
 * 3-dot options sheet). Wired for the Liquid Glass skin only; other skins keep
 * the existing artist overlay untouched.
 *
 * Real data only: feeds on ArtistProfile + a per-selection album catalog fetch.
 * Sections that have no backing data (bio, Subscribers, monthly listeners,
 * release years) hide themselves rather than invent values.
 */
package com.yasuda.derbis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yasuda.derbis.data.ArtworkPalette
import com.yasuda.derbis.data.ArtworkPaletteCache
import com.yasuda.derbis.data.areAllFavoriteTracks
import com.yasuda.derbis.domain.ArtistProfile
import com.yasuda.derbis.domain.ArtistRelease
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.DerbisPersonalOrbit
import com.yasuda.derbis.domain.DerbisTab
import com.yasuda.derbis.feature.motion.MotionArtwork
import com.yasuda.derbis.domain.ReleaseType
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.artwork.rememberArtworkPalette
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPlayingIndicator
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.viewmodel.DerbisUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val GlassScreenBackground = 0xFF0B0B0DL
private const val GlassInk = 0xFFF5F5F7L
private const val GlassInkDim = 0xFFA1A1AAL
private const val GlassLine = 0x24FFFFFFL

private val GlassHeroHeight = 430.dp
private val GlassCarouselSize = 84.dp
private val GlassCarouselSpacing = 6.dp

internal data class LiquidGlassReleaseEntry(
    val release: ArtistRelease,
    val typeLabel: String
)

internal fun buildLiquidGlassReleases(profile: ArtistProfile, albumLabel: String, singleLabel: String): List<LiquidGlassReleaseEntry> {
    fun ReleaseType.label(): String = when (this) {
        ReleaseType.Album, ReleaseType.Compilation -> albumLabel
        ReleaseType.Ep -> "EP"
        ReleaseType.Single -> singleLabel
        ReleaseType.Unknown -> albumLabel
    }
    val primary = profile.albums.map { it to it.releaseType.label() } +
        profile.compilations.map { it to albumLabel }
    val fallback = profile.singles.map { it to it.releaseType.label() }
    val source = if (primary.isNotEmpty()) primary else fallback
    val seen = HashSet<String>()
    return source
        .filter { (release, _) ->
            val key = release.browseId.ifBlank { release.title }.trim().lowercase()
            seen.add(key)
        }
        .sortedBy { (release, _) -> artistReleaseYear(release) ?: Int.MAX_VALUE }
        .map { (release, label) -> LiquidGlassReleaseEntry(release = release, typeLabel = label) }
}

private val ArtistYearPattern = Regex("""\b(19|20)\d{2}\b""")

internal fun artistReleaseYear(raw: String): Int? =
    ArtistYearPattern.find(raw)?.value?.toIntOrNull()

internal fun artistReleaseYear(release: ArtistRelease): Int? =
    artistReleaseYear(release.year)

/** Minimal glass shell used by the loading/error states. */
@Composable
private fun LiquidGlassArtistFrame(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(GlassScreenBackground))
    ) {
        LiquidGlassArtistBar(
            title = title,
            collapse = { 1f },
            onClose = onClose,
            onOpenOptions = null,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        content()
    }
}

/**
 * Transparent-on-top chrome that fades into glass once the hero is mostly gone, mirroring the
 * mock's `.appbar` (`--bar` 0 → 1 across the scroll range).
 */
@Composable
private fun LiquidGlassArtistBar(
    title: String,
    collapse: () -> Float,
    onClose: () -> Unit,
    onOpenOptions: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val titleYPx = with(density) { 6.dp.toPx() }
    val swipeClose by rememberUpdatedState(onClose)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged > 72f) swipeClose() },
                    onDragCancel = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        dragged += amount
                        change.consume()
                    }
                )
            }
            .drawBehind {
                val shown = ((collapse() - 0.55f) / 0.40f).coerceIn(0f, 1f)
                if (shown > 0f) {
                    drawRect(Color(0xFF141419), alpha = shown * 0.62f)
                    drawRect(
                        color = Color(0x14FFFFFF),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                        alpha = shown
                    )
                }
            }
            .derbisSafeTopPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCircleButton(onClick = onClose, contentDescription = LocalDerbisStrings.current.back) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = Color(GlassInk),
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = title,
            color = Color(GlassInk),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val shown = ((collapse() - 0.55f) / 0.40f).coerceIn(0f, 1f)
                    alpha = shown
                    translationY = (1f - shown) * titleYPx
                }
            )
        if (onOpenOptions != null) {
            GlassCircleButton(onClick = onOpenOptions, contentDescription = LocalDerbisStrings.current.options) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = Color(GlassInk),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(38.dp))
        }
    }
}

/** Frosted circle used for the top bar's two actions (mock's `.icon-btn.g`). */
@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Parallax artwork hero: Ken Burns scale on the artwork, slow upward drift plus fade as the feed
 * scrolls, and the artist name over the fold in the mock's stacked display caps.
 */
@Composable
private fun LiquidGlassArtistHero(
    artistName: String,
    fittedHero: String,
    accentDeep: Color,
    heroScroll: () -> Float,
    animationsEnabled: Boolean,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    canvasQuality: DerbisCanvasQuality
) {
    val density = LocalDensity.current
    val heroHeight = GlassHeroHeight
    val heroHeightPx = with(density) { heroHeight.toPx() }
    val anim = rememberInfiniteTransition(label = "glass-ken-burns")
    val kenBurns by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .graphicsLayer {
                    translationY = heroScroll() * 0.45f
                    alpha = (1f - heroScroll() / (heroHeightPx * 0.85f)).coerceIn(0f, 1f)
                }
        ) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = motionEnabled,
                pageMode = true,
                cornerRadius = 0.dp,
                presentation = MotionArtworkPresentation.Cinematic,
                quality = canvasQuality,
                staticArtworkMotionEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                if (fittedHero.isNotBlank()) {
                    AsyncImage(
                        model = fittedHero,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (animationsEnabled) {
                                    Modifier.graphicsLayer {
                                        scaleX = kenBurns
                                        scaleY = kenBurns
                                    }
                                } else Modifier
                            )
                    )
                } else {
                    // no artwork anywhere: fall back to the accent-deep gradient so the hero still reads
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(accentDeep, Color(GlassScreenBackground))))
                    )
                }
            }
            // transparent → black gradient at the hero's foot (mock's `.hero::after`)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x66000000),
                            0.28f to Color.Transparent,
                            0.62f to Color(GlassScreenBackground).copy(alpha = 0f),
                            1f to Color(GlassScreenBackground)
                        )
                    )
            )
        }
        Text(
            text = artistName,
            color = Color(GlassInk),
            fontSize = 46.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp, start = 20.dp, end = 20.dp)
                .graphicsLayer {
                    translationY = heroScroll() * 0.25f
                    alpha = (1f - heroScroll() / 220f).coerceIn(0f, 1f)
                }
                .semantics { heading() }
        )
    }
}

/**
 * Full-page artist destination for the Liquid Glass skin. Self-contained chrome (top bar with a
 * close action and an options menu) so it can own its visual language end to end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiquidGlassArtistOverlay(
    state: DerbisUiState,
    onPlayFrom: (List<Track>, Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleFavorites: (List<Track>) -> Unit,
    onToggleFollow: () -> Unit,
    onAddTracksToQueue: (List<Track>) -> Unit,
    onOpenPlayer: () -> Unit,
    onStartRadio: (Track) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onShareProfile: (String) -> Unit,
    onDownloadAlbum: (ArtistRelease) -> Unit,
    onLoadReleaseTracks: suspend (ArtistRelease, String) -> List<Track>,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val density = LocalDensity.current
    val animationsEnabled = state.animationsEnabled
    var toast by remember { mutableStateOf<GlassToast?>(null) }
    val showToast: (String) -> Unit = { text ->
        val nextId = (toast?.id ?: 0) + 1
        toast = GlassToast(nextId, text)
    }
    val profile = state.artistProfile
    val artistName = profile?.name.orEmpty()

    // ---- loading state (glass chrome, page-level back button) ----------------------------
    if (profile == null) {
        LiquidGlassArtistFrame(title = artistName.ifBlank { strings.artistLabel }, onClose = onClose) {
            Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                if (state.artistError != null) {
                    Text(
                        text = strings.artistProfileUnavailable,
                        color = Color(GlassInkDim),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }
        }
        return
    }
    val artist = profile

    // ---- releases + carousel state (selection is the single source of truth) -------------
    val releases = remember(artist.browseId, artist.albums, artist.compilations, artist.singles, strings.code) {
        buildLiquidGlassReleases(artist, strings.albumPlain, strings.singlePlain)
    }
    val lastReleaseIndex = releases.lastIndex.coerceAtLeast(0)
    val carouselPosition = remember(releases) { Animatable(releases.lastIndex.toFloat().coerceAtLeast(0f)) }
    val selectedIndex by remember {
        derivedStateOf { carouselPosition.value.roundToInt().coerceIn(0, lastReleaseIndex) }
    }
    val selectedEntry = releases.getOrNull(selectedIndex)
    val selectedRelease = selectedEntry?.release
    val selectedKey = selectedRelease?.let { it.browseId.ifBlank { it.title + "|" + it.year } }.orEmpty()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val heroHeightForScroll = with(density) { GlassHeroHeight.toPx() }
    val heroScroll: () -> Float = {
        if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, heroHeightForScroll)
        } else {
            heroHeightForScroll
        }
    }
    val heroHeightPx = heroHeightForScroll
    val topBarPx = with(density) { (derbisSafeTopInset() + 64.dp).toPx() }
    val collapse: () -> Float = {
        (heroScroll() / (heroHeightPx - topBarPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    val fallbackPalette = remember(artist.accentStart, artist.accentEnd, selectedKey) {
        ArtworkPalette(artist.accentStart, artist.accentEnd)
    }
    val paletteKey = remember(selectedKey) {
        if (selectedKey.isBlank() || selectedRelease == null) ""
        else ArtworkPaletteCache.key(
            trackId = "artist-glass:$selectedKey",
            thumbnailUrl = selectedRelease.thumbnailUrl,
            largeThumbnailUrl = selectedRelease.thumbnailUrl
        )
    }
    val releasePalette by rememberArtworkPalette(
        paletteKey = paletteKey,
        artworkUrl = selectedRelease?.thumbnailUrl.orEmpty(),
        fallback = fallbackPalette
    )
    val accent = Color(releasePalette.start)
    val accentDeep = Color(releasePalette.end)

    // ---- per-album track list (per-selection fetch, top-songs fallback) -------------------
    var albumTracks by remember(selectedKey) { mutableStateOf<List<Track>?>(null) }
    var albumLoading by remember(selectedKey) { mutableStateOf(false) }
    LaunchedEffect(selectedKey) {
        val release = selectedRelease ?: return@LaunchedEffect
        albumTracks = null
        albumLoading = true
        val loaded = runCatching { onLoadReleaseTracks(release, artist.name) }.getOrDefault(emptyList())
        albumTracks = loaded
        albumLoading = false
    }
    val displayTracks = albumTracks?.takeIf { it.isNotEmpty() } ?: artist.topSongs
    val tracksLoading = albumLoading && albumTracks == null

    val currentTrackId = state.currentTrack?.id
    val albumActive = currentTrackId != null && displayTracks.any { it.id == currentTrackId }
    val currentIndexInAlbum = if (albumActive) displayTracks.indexOfFirst { it.id == currentTrackId } else -1
    val isPlaying = state.isPlaying && albumActive
    val albumSaved = remember(displayTracks, state.favorites) { areAllFavoriteTracks(state.favorites, displayTracks) }
    val totalMinutes = remember(displayTracks) { (displayTracks.sumOf { it.durationMs } / 60000L).coerceAtLeast(1L) }
    val shareText = remember(artist.name, artist.browseId) {
        buildString {
            append(artist.name)
            if (artist.browseId.isNotBlank()) append("\nhttps://music.youtube.com/browse/").append(artist.browseId)
        }
    }
    val artistBio = artist.bio.ifBlank { artist.biography?.description.orEmpty() }
    val followed = (artist.browseId.isNotBlank() && artist.browseId in state.followedArtistKeys) ||
        artist.name.trim().lowercase() in state.followedArtistKeys
    val heroArtwork = artist.bannerUrl.ifBlank { artist.thumbnailUrl }
    val fittedHero = remember(heroArtwork, artist.browseId) {
        DerbisPersonalOrbit.croppedArtworkUrl(heroArtwork, 1080, 430)
    }
    val heroNameLines = remember(artist.name) { splitDisplayName(artist.name) }
    val onToggleSave: () -> Unit = {
        if (displayTracks.isNotEmpty()) {
            onToggleFavorites(displayTracks)
            showToast(if (albumSaved) strings.artistRemovedFromFavorites else strings.artistSavedToFavorites)
        }
    }

    var optionsOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(GlassScreenBackground))
    ) {
        // accent-tinted radial glows behind everything (mock's screen background)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(GlassScreenBackground))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.58f),
                    radius = w * 0.95f
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.58f),
                radius = w * 0.95f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w, h * 0.85f),
                    radius = w * 0.75f
                ),
                center = androidx.compose.ui.geometry.Offset(w, h * 0.85f),
                radius = w * 0.75f
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = derbisTabContentBottomPadding(state.currentTrack != null)),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "hero", contentType = "glass-hero") {
                LiquidGlassArtistHero(
                    artistName = heroNameLines,
                    fittedHero = fittedHero,
                    accentDeep = accentDeep,
                    heroScroll = heroScroll,
                    animationsEnabled = animationsEnabled,
                    motionArtwork = if (state.selectedTab != DerbisTab.Player) state.artistMotionArtwork else null,
                    motionEnabled = state.animationsEnabled && state.motionArtworkEnabled && state.selectedTab != DerbisTab.Player,
                    canvasQuality = state.interfaceSettings.canvasQuality
                )
            }
            item(key = "carousel", contentType = "glass-carousel") {
                LiquidGlassReleaseCarousel(
                    releases = releases,
                    position = carouselPosition,
                    animationsEnabled = animationsEnabled
                )
            }
            item(key = "meta", contentType = "glass-meta") {
                LiquidGlassMetaRow(entry = selectedEntry, accent = accent)
            }
            item(key = "actions", contentType = "glass-actions") {
                LiquidGlassActionsRow(
                    accent = accent,
                    albumActive = albumActive,
                    isPlaying = isPlaying,
                    saved = albumSaved,
                    strings = strings,
                    onSongs = { coroutineScope.launch { listState.animateScrollToItem(5) } },
                    onShuffle = { if (displayTracks.isNotEmpty()) onPlayAll(displayTracks.shuffled()) },
                    onPlay = {
                        if (albumActive) onTogglePlayback()
                        else if (displayTracks.isNotEmpty()) onPlayAll(displayTracks)
                    },
                    onToggleSave = onToggleSave
                )
            }
            item(key = "progress", contentType = "glass-progress") {
                LiquidGlassProgressLine(
                    albumActive = albumActive,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    accent = accent,
                    onOpenPlayer = onOpenPlayer
                )
            }
            item(key = "tracks", contentType = "glass-tracks") {
                LiquidGlassTrackList(
                    sectionTitle = strings.songs,
                    meta = strings.formatArtistTracksMeta(displayTracks.size, totalMinutes),
                    tracks = displayTracks,
                    artworkUrl = selectedRelease?.thumbnailUrl.orEmpty(),
                    accent = accent,
                    currentIndex = currentIndexInAlbum,
                    isPlaying = state.isPlaying,
                    loading = tracksLoading,
                    onTrackClick = { track -> onPlayFrom(displayTracks, track) }
                )
            }
            item(key = "pedia", contentType = "glass-pedia") {
                LiquidGlassArtistpedia(
                    artist = artist,
                    releases = releases,
                    accent = accent,
                    bio = artistBio,
                    onJumpToRelease = { index ->
                        coroutineScope.launch {
                            carouselPosition.animateTo(
                                index.coerceIn(0, lastReleaseIndex).toFloat(),
                                spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                            )
                            listState.animateScrollToItem(0)
                        }
                    }
                )
            }
            item(key = "pedia-spacer", contentType = "glass-spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }

        LiquidGlassArtistBar(
            title = listOfNotNull(
                artist.name.takeIf(String::isNotBlank),
                selectedRelease?.title?.takeIf(String::isNotBlank)
            ).joinToString(" · "),
            collapse = collapse,
            onClose = {
                val atRest = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 48
                if (atRest) {
                    onClose()
                } else {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                }
            },
            onOpenOptions = { optionsOpen = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp)
        ) {
            GlassToastHost(toast = toast)
        }

        if (optionsOpen) {
            ModalBottomSheet(
                onDismissRequest = { optionsOpen = false },
                sheetState = rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
                ),
                containerColor = Color(0xFF1C1C22),
                tonalElevation = 0.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(38.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x4DFFFFFF))
                    )
                }
            ) {
                GlassArtistOptionsSheet(
                    artist = artist,
                    selectedRelease = selectedRelease,
                    selectedReleaseType = selectedEntry?.typeLabel.orEmpty(),
                    albumTracks = displayTracks,
                    followed = followed,
                    accent = accent,
                    sleepMinutes = state.sleepTimerMinutes,
                    onDismiss = { optionsOpen = false },
                    onToggleFollow = onToggleFollow,
                    onAddAlbumToQueue = {
                        if (displayTracks.isNotEmpty()) {
                            onAddTracksToQueue(displayTracks)
                            showToast(strings.formatArtistAddedToQueue(displayTracks.size))
                        }
                    },
                    onDownloadAlbum = { selectedRelease?.let(onDownloadAlbum) },
                    onStartRadio = {
                        (displayTracks.firstOrNull() ?: artist.topSongs.firstOrNull())?.let(onStartRadio)
                    },
                    onShareProfile = { onShareProfile(shareText) },
                    onOpenArtistpedia = {
                        optionsOpen = false
                        coroutineScope.launch { listState.animateScrollToItem(6) }
                    },
                    onCycleSleepTimer = {
                        val order = listOf(0, 15, 30, 60)
                        val current = state.sleepTimerMinutes
                        val next = order.firstOrNull { it > current } ?: 0
                        onSetSleepTimer(next)
                        showToast(if (next == 0) strings.artistSleepTimerOff else strings.formatArtistSleepInMinutes(next))
                    }
                )
            }
        }
    }
}

/**
 * Letter-by-letter reveal toast, local to the glass artist page (mock's `.toast`). Positioned
 * above the bottom chrome; self-dismisses after [TOAST_HOLD_MS].
 */
private data class GlassToast(
    val id: Int,
    val text: String
)

private const val TOAST_HOLD_MS = 2100L
private const val TOAST_CHAR_DELAY_MS = 14L

@Composable
private fun GlassToastHost(
    toast: GlassToast?,
    modifier: Modifier = Modifier
) {
    if (toast == null) return
    var shown by remember(toast.id) { mutableStateOf(0) }
    var dismissed by remember(toast.id) { mutableStateOf(false) }
    LaunchedEffect(toast.id) {
        shown = 0
        dismissed = false
        val steps = toast.text.length
        for (index in 1..steps) {
            shown = index
            delay(TOAST_CHAR_DELAY_MS)
        }
        delay(TOAST_HOLD_MS)
        dismissed = true
    }
    val visible = !dismissed
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + expandVertically(tween(200)),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xE6141419))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = toast.text.take(shown),
                color = Color(GlassInk),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private fun splitDisplayName(name: String): String {
    val parts = name.trim().split(Regex("""\s+""")).filter(String::isNotBlank)
    if (parts.size <= 1) return name.uppercase()
    val half = (parts.size + 1) / 2
    return parts.take(half).joinToString(" ").uppercase() + "\n" + parts.drop(half).joinToString(" ").uppercase()
}

private fun formatPlaybackMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

/**
 * 3D momentum carousel for releases. Items fan out along a perspective arc; dragging applies a
 * rubber-band resistance past either end and fling projects velocity into the settle target.
 */
@Composable
private fun LiquidGlassReleaseCarousel(
    releases: List<LiquidGlassReleaseEntry>,
    position: Animatable<Float, AnimationVector1D>,
    animationsEnabled: Boolean
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val lastIndex = releases.lastIndex.coerceAtLeast(0)
    val maxPos = lastIndex.toFloat()
    val stepPx = with(density) { (GlassCarouselSize + GlassCarouselSpacing).toPx() }
    val strings = LocalDerbisStrings.current

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(releases, stepPx) {
                    var velocityX = 0f
                    var lastUptime = 0L
                    detectDragGestures(
                        onDragStart = {
                            velocityX = 0f
                            lastUptime = 0L
                            scope.launch { position.stop() }
                        },
                        onDragEnd = {
                            val projected = position.value - (velocityX / stepPx) * 0.16f
                            val target = projected.roundToInt().coerceIn(0, lastIndex).toFloat()
                            scope.launch {
                                position.animateTo(
                                    target,
                                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onDragCancel = {},
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val raw = position.value - dragAmount.x / stepPx
                            val banded = when {
                                raw < 0f -> raw * 0.32f
                                raw > maxPos -> maxPos + (raw - maxPos) * 0.32f
                                else -> raw
                            }
                            scope.launch { position.snapTo(banded) }
                            if (lastUptime != 0L) {
                                val dt = (change.uptimeMillis - lastUptime).coerceAtLeast(1L)
                                val inst = dragAmount.x / dt * 1000f
                                velocityX = 0.75f * velocityX + 0.25f * inst
                            }
                            lastUptime = change.uptimeMillis
                        }
                    )
                }
                .semantics {
                    contentDescription = strings.artistCarouselLabel
                }
        ) {
            if (releases.isEmpty()) {
                Text(
                    text = LocalDerbisStrings.current.artistNoReleases,
                    color = Color(GlassInkDim),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val ordered = remember(releases, position.value) {
                    releases.indices.sortedByDescending { i -> abs(i - position.value) }
                }
                ordered.forEach { index ->
                    LiquidGlassCarouselItem(
                        entry = releases[index],
                        index = index,
                        position = position,
                        stepPx = stepPx,
                        animationsEnabled = animationsEnabled,
                        onSelect = {
                            val target = index.toFloat()
                            scope.launch {
                                position.animateTo(
                                    target,
                                    spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    )
                }
            }
        }
        ReleaseDots(count = releases.size, position = position, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.LiquidGlassCarouselItem(
    entry: LiquidGlassReleaseEntry,
    index: Int,
    position: Animatable<Float, AnimationVector1D>,
    stepPx: Float,
    animationsEnabled: Boolean,
    onSelect: () -> Unit
) {
    val density = LocalDensity.current
    val cameraDistancePx = 14f * density.density
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(GlassCarouselSize)
            .zIndex(MAX_Z - abs(index - position.value))
            .graphicsLayer {
                val offset = (index - position.value).coerceIn(-4.5f, 4.5f)
                val absOffset = abs(offset)
                translationX = offset * stepPx
                translationY = absOffset * absOffset * 9f
                val scale = (1f - absOffset * 0.17f).coerceAtLeast(0.5f)
                scaleX = scale
                scaleY = scale
                rotationY = (-offset * 16f).coerceIn(-46f, 46f)
                cameraDistance = cameraDistancePx
                alpha = (1f - absOffset * 0.30f).coerceIn(0.15f, 1f)
                shadowElevation = if (absOffset < 0.4f) 26.dp.toPx() else 6.dp.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .semantics { selected = index == position.value.roundToInt() }
            .clickable(role = Role.Button, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                onSelect()
            }
    ) {
        val release = entry.release
        if (release.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = release.thumbnailUrl,
                contentDescription = release.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2A2A33), Color(0xFF141419))
                        )
                    )
            )
        }
        // glass specular wash + inner stroke (mock's `.c-item::after`)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x59FFFFFF),
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to Color(0x4D000000)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(16.dp))
        )
    }
}

private const val MAX_Z = 100f

@Composable
private fun ReleaseDots(
    count: Int,
    position: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return
    val strings = LocalDerbisStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.formatArtistCarouselPosition(
                    position.value.roundToInt(),
                    count
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val distance = abs(index - position.value)
            val active = (1f - distance).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (active > 0.5f) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.16f + active * 0.62f))
            )
        }
    }
}

/** Floating caption under the carousel: selected release title, year, and track count. */
@Composable
private fun LiquidGlassMetaRow(
    entry: LiquidGlassReleaseEntry?,
    accent: Color
) {
    val release = entry?.release
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (release == null) {
            Text(
                text = LocalDerbisStrings.current.artistNoReleases,
                color = Color(GlassInkDim),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            return@Column
        }
        Text(
            text = release.title,
            color = Color(GlassInk),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = listOfNotNull(entry.typeLabel.uppercase(), release.year.takeIf { it.isNotBlank() })
                        .joinToString(" · "),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/** Glass pill actions: jump to the track list, play/pause the whole release, toggle Save. */
@Composable
private fun LiquidGlassActionsRow(
    accent: Color,
    albumActive: Boolean,
    isPlaying: Boolean,
    saved: Boolean,
    strings: DerbisStrings,
    onSongs: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onToggleSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassPillButton(
            onClick = onSongs,
            contentDescription = strings.songs,
            accent = null
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = Color(GlassInk),
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = strings.songs.uppercase(),
                color = Color(GlassInk),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF))
                .border(1.dp, Color(0x24FFFFFF), CircleShape)
                .clickable(role = Role.Button, onClick = onShuffle)
                .semantics {
                    this.contentDescription = strings.shuffle
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null,
                tint = Color(GlassInk),
                modifier = Modifier.size(19.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(elevation = 30.dp, shape = CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f))))
                .clickable(role = Role.Button, onClick = onPlay)
                .semantics {
                    this.contentDescription = if (isPlaying) strings.pause else strings.play
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            DerbisPlayPauseGlyph(
                playing = isPlaying,
                color = Color(0xFF1A1000),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        GlassPillButton(
            onClick = onToggleSave,
            contentDescription = if (saved) strings.saved else strings.save,
            accent = if (saved) accent else null
        ) {
            Icon(
                imageVector = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (saved) Color(0xFF1A1000) else Color(GlassInk),
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = if (saved) strings.saved.uppercase() else strings.save.uppercase(),
                color = if (saved) Color(0xFF1A1000) else Color(GlassInk),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
        }
    }
}

@Composable
private fun GlassPillButton(
    onClick: () -> Unit,
    contentDescription: String,
    accent: Color?,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(30.dp))
            .then(
                if (accent != null) {
                    Modifier
                        .background(accent)
                        .border(1.dp, accent, RoundedCornerShape(30.dp))
                } else {
                    Modifier
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(30.dp))
                }
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 22.dp),
        content = content
    )
}

/** Now-playing progress hairline, only when the album owns the current track. */
@Composable
private fun LiquidGlassProgressLine(
    albumActive: Boolean,
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onOpenPlayer: () -> Unit
) {
    AnimatedVisibility(
        visible = albumActive && durationMs > 0L,
        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenPlayer)
                .padding(horizontal = 26.dp, vertical = 2.dp)
        ) {
            LinearProgressIndicator(
                progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = Color(0x1FFFFFFF)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlaybackMs(positionMs),
                    color = Color(GlassInkDim),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatPlaybackMs(durationMs),
                    color = Color(GlassInkDim),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/** Section list for the selected release, with per-row EQ and a now-playing progress hairline. */
@Composable
private fun LiquidGlassTrackList(
    sectionTitle: String,
    meta: String,
    tracks: List<Track>,
    artworkUrl: String,
    accent: Color,
    currentIndex: Int,
    isPlaying: Boolean,
    loading: Boolean,
    onTrackClick: (Track) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sectionTitle.uppercase(),
                color = Color(GlassInk),
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = meta,
                color = Color(GlassInkDim),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (loading) {
            repeat(5) { index ->
                SkeletonTrackRow(delayMillis = index * 90)
            }
            return@Column
        }
        if (tracks.isEmpty()) {
            Text(
                text = strings.albumTracksUnavailable,
                color = Color(GlassInkDim),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
            )
            return@Column
        }
        tracks.forEachIndexed { index, track ->
            LiquidGlassTrackRow(
                index = index,
                track = track,
                fallbackArtwork = artworkUrl,
                accent = accent,
                active = index == currentIndex,
                isPlaying = isPlaying,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

@Composable
private fun LiquidGlassTrackRow(
    index: Int,
    track: Track,
    fallbackArtwork: String,
    accent: Color,
    active: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val artwork = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl.ifBlank { fallbackArtwork } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) accent.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 18.dp, top = 7.dp, bottom = 7.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                if (active) {
                    DerbisPlayingIndicator(
                        playing = isPlaying,
                        color = accent,
                        size = 17.dp,
                        width = 17.dp,
                        height = 15.dp,
                        barWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = Color(GlassInkDim),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E26))
            ) {
                if (artwork.isNotBlank()) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (active) accent else Color(GlassInk),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.artist.isNotBlank()) {
                    Text(
                        text = track.artist,
                        color = Color(GlassInkDim),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (track.durationMs > 0L) {
                Text(
                    text = formatPlaybackMs(track.durationMs),
                    color = Color(GlassInkDim),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SkeletonTrackRow(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "glass-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(850 + delayMillis, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(width = 22.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = pulse)))
        Spacer(modifier = Modifier.width(14.dp))
        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = pulse)))
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(width = 148.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = pulse)))
            Box(modifier = Modifier.size(width = 92.dp, height = 9.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = pulse * 0.8f)))
        }
    }
}

/** Hairline section label used inside Artistpedia. */
@Composable
private fun ArtistpediaLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(GlassInkDim),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.8.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

/**
 * Artistpedia — real data only. Sections hide themselves when their source data is missing;
 * nothing here is invented or extrapolated beyond the profile and the release list.
 */
@Composable
private fun LiquidGlassArtistpedia(
    artist: ArtistProfile,
    releases: List<LiquidGlassReleaseEntry>,
    accent: Color,
    bio: String,
    onJumpToRelease: (Int) -> Unit
) {
    val strings = LocalDerbisStrings.current
    var bioExpanded by remember(artist.browseId) { mutableStateOf(false) }
    val bioPreviewLimit = 260
    val needsClamp = bio.length > bioPreviewLimit
    val shownBio = if (bioExpanded || !needsClamp) bio else bio.take(bioPreviewLimit).trimEnd() + "…"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 26.dp)
    ) {
        Text(
            text = strings.artistpediaTitle.uppercase(),
            color = accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.4.sp
        )
        Text(
            text = strings.artistpediaTitle,
            color = Color(GlassInk),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = strings.artistpediaSubtitle,
            color = Color(GlassInkDim),
            fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 3.dp)
        )

        if (bio.isNotBlank()) {
            ArtistpediaLabel(strings.artistpediaBioTitle)
            Text(
                text = shownBio,
                color = Color(0xE0F5F5F7),
                fontSize = 13.5.sp,
                lineHeight = 20.5.sp
            )
            artist.biography?.sourceLabel?.takeIf { it.isNotBlank() }?.let { source ->
                Text(
                    text = "· $source",
                    color = Color(GlassInkDim),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (needsClamp) {
                Text(
                    text = if (bioExpanded) strings.showLess else strings.showMore,
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(role = Role.Button) { bioExpanded = !bioExpanded }
                )
            }
        }

        val knownYears = releases.withIndex().mapNotNull { (i, entry) ->
            artistReleaseYear(entry.release.year)?.let { year -> i to year }
        }
        val debut = knownYears.firstOrNull()?.second
        val latest = knownYears.lastOrNull()?.second
        val facts = buildList {
            if (artist.monthlyListeners.isNotBlank()) add(strings.artistpediaListeners to artist.monthlyListeners)
            if (artist.subscribers.isNotBlank()) add(strings.artistpediaSubscribers to artist.subscribers)
            debut?.let { add(strings.artistpediaDebut to it.toString()) }
            latest?.let { add(strings.artistpediaLatestRelease to it.toString()) }
            if (releases.isNotEmpty()) add(strings.artistpediaReleases to releases.size.toString())
        }
        if (facts.isNotEmpty()) {
            ArtistpediaLabel(strings.artistpediaFactsTitle)
            facts.chunked(2).forEach { rowFacts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowFacts.forEach { (label, value) ->
                        ArtistpediaFactCard(label = label, value = value, modifier = Modifier.weight(1f))
                    }
                    if (rowFacts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (knownYears.size >= 2) {
            val timeline = mutableListOf<Pair<Int, Int>>()
            timeline += knownYears.first()
            val middle = knownYears.drop(1).dropLast(1)
            val step = if (middle.size <= 2) 1 else middle.size / 2
            middle.filterIndexed { index, _ -> index % step == 0 }.take(2).forEach { timeline += it }
            timeline += knownYears.last()
            ArtistpediaLabel(strings.artistpediaTimelineTitle)
            timeline.forEach { (releaseIndex, year) ->
                ArtistpediaTimelineRow(
                    year = year,
                    entry = releases[releaseIndex],
                    isDebut = releaseIndex == knownYears.first().first,
                    accent = accent,
                    onClick = { onJumpToRelease(releaseIndex) }
                )
            }
        }
    }
}

@Composable
private fun ArtistpediaFactCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x0FFFFFFF))
            .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color(GlassInkDim),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = Color(GlassInk),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistpediaTimelineRow(
    year: Int,
    entry: LiquidGlassReleaseEntry,
    isDebut: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 7.dp)
    ) {
        Text(
            text = year.toString(),
            color = accent,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(74.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isDebut) strings.artistTimelineDebut else strings.artistTimelineEra,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
            }
            Text(
                text = entry.release.title,
                color = Color(GlassInk),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(GlassInkDim),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 3-dot bottom sheet: follow, queue, download, radio, share, Artistpedia, sleep-timer cycle. */
@Composable
private fun GlassArtistOptionsSheet(
    artist: ArtistProfile,
    selectedRelease: ArtistRelease?,
    selectedReleaseType: String,
    albumTracks: List<Track>,
    followed: Boolean,
    accent: Color,
    sleepMinutes: Int,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onAddAlbumToQueue: () -> Unit,
    onDownloadAlbum: () -> Unit,
    onStartRadio: () -> Unit,
    onShareProfile: () -> Unit,
    onOpenArtistpedia: () -> Unit,
    onCycleSleepTimer: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val releaseArtwork = selectedRelease?.thumbnailUrl.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(14.dp))
            ) {
                if (releaseArtwork.isNotBlank()) {
                    AsyncImage(
                        model = releaseArtwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedRelease?.title.orEmpty().ifBlank { artist.name },
                    color = Color(GlassInk),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        selectedReleaseType.takeIf(String::isNotBlank),
                        selectedRelease?.year?.takeIf(String::isNotBlank),
                        artist.name
                    ).joinToString(" · "),
                    color = Color(GlassInkDim),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null, tint = tint) },
            title = if (followed) strings.artistOptionsFollowing else strings.artistOptionsFollow,
            subtitle = artist.name,
            trailing = if (followed) {
                { Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)) }
            } else null,
            onClick = {
                onToggleFollow()
                onDismiss()
            }
        )
        ArtistOptionsDivider()
        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.PlaylistAdd, contentDescription = null, tint = tint) },
            title = strings.artistOptionsAddToQueue,
            subtitle = strings.formatArtistTracksMeta(albumTracks.size, (albumTracks.sumOf { it.durationMs } / 60000L).coerceAtLeast(1L)),
            onClick = {
                onAddAlbumToQueue()
                onDismiss()
            }
        )
        ArtistOptionsDivider()
        if (selectedRelease != null) {
            ArtistOptionsRow(
                icon = { tint -> Icon(imageVector = Icons.Rounded.Download, contentDescription = null, tint = tint) },
                title = strings.download,
                subtitle = selectedRelease.title,
                    onClick = {
                    onDownloadAlbum()
                    onDismiss()
                }
            )
            ArtistOptionsDivider()
        }
        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.Radio, contentDescription = null, tint = tint) },
            title = strings.artistOptionsRadio,
            subtitle = artist.name,
            onClick = {
                onStartRadio()
                onDismiss()
            }
        )
        ArtistOptionsDivider()
        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = tint) },
            title = strings.artistOptionsArtistpedia,
            subtitle = strings.artistpediaSubtitle,
            onClick = onOpenArtistpedia
        )
        ArtistOptionsDivider()
        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.Share, contentDescription = null, tint = tint) },
            title = strings.share,
            subtitle = artist.name,
            onClick = {
                onShareProfile()
                onDismiss()
            }
        )
        ArtistOptionsDivider()
        ArtistOptionsRow(
            icon = { tint -> Icon(imageVector = Icons.Rounded.Bedtime, contentDescription = null, tint = tint) },
            title = strings.sleepTimer,
            subtitle = strings.artistOptionsSleepTimerHint,
            trailing = {
                Text(
                    text = if (sleepMinutes > 0) "${sleepMinutes}m" else "—",
                    color = if (sleepMinutes > 0) accent else Color(GlassInkDim),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            },
            onClick = onCycleSleepTimer
        )
    }
}

@Composable
private fun ArtistOptionsRow(
    icon: @Composable (Color) -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            icon(Color(GlassInk))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(GlassInk),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null && subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color(GlassInkDim),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun ArtistOptionsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 20.dp)
            .height(1.dp)
            .background(Color(GlassLine))
    )
}
