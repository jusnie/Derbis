package com.yasuda.derbis.ui.player

import com.yasuda.derbis.ui.theme.*
import com.yasuda.derbis.ui.PlayerAccentPair
import com.yasuda.derbis.ui.theme.Nocturne78Palette
import com.yasuda.derbis.ui.theme.DerbisIsNocturne
import com.yasuda.derbis.domain.LyricsEngine
import com.yasuda.derbis.ui.lyrics.lyricsOffsetPosition
import com.yasuda.derbis.ui.lyrics.rememberLyricsAudioOutputRoute
import com.yasuda.derbis.ui.lyrics.rememberLyricsPlaybackClock
import com.yasuda.derbis.ui.sharing.TrackStoryShareSheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.yasuda.derbis.data.ArtworkPalette
import com.yasuda.derbis.data.ArtworkPaletteCache
import com.yasuda.derbis.data.PlaybackSourceIdentity
import com.yasuda.derbis.data.youtubeWatchUrl
import com.yasuda.derbis.domain.ArtistHit
import com.yasuda.derbis.domain.PlayerVisualMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.cast.CastRouteButton
import com.yasuda.derbis.feature.radio.isLiveRadio
import com.yasuda.derbis.feature.radio.LiveRadioViewModel
import com.yasuda.derbis.feature.radio.RadioStation
import com.yasuda.derbis.ui.DerbisLayoutMode
import com.yasuda.derbis.ui.DerbisPlayerPane
import com.yasuda.derbis.ui.MotionBackdropPalette
import com.yasuda.derbis.ui.artwork.ArtworkPreviewOverlay
import com.yasuda.derbis.ui.artwork.livingArtworkColors
import com.yasuda.derbis.ui.artwork.rememberArtworkPalette
import com.yasuda.derbis.ui.components.ModernListenTogetherVector
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.PlayerGlassIconButton
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.harmonizePlayerAccents
import com.yasuda.derbis.ui.i18n.DerbisLiveRadioCatalog
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.systemPlayerCopy
import com.yasuda.derbis.ui.i18n.technicalAudioInfoCopy
import com.yasuda.derbis.ui.i18n.trackCreditsCopy
import com.yasuda.derbis.ui.isDerbisCompactLandscape
import com.yasuda.derbis.ui.derbisCompactLandscapeVideoPaneWeight
import com.yasuda.derbis.ui.derbisContentMaxWidthDp
import com.yasuda.derbis.ui.derbisFoldAwareGutterDp
import com.yasuda.derbis.ui.derbisPlayerArtworkMaxWidthDp
import com.yasuda.derbis.ui.playerAmbienceOf
import com.yasuda.derbis.ui.rememberPlayerFixedAccents
import com.yasuda.derbis.ui.preferredPlayerArtworkUrl
import com.yasuda.derbis.ui.resolveDerbisLayoutMode
import com.yasuda.derbis.ui.resolveNowPlayingPane
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.DerbisPlayerShapes
import com.yasuda.derbis.ui.theme.DerbisSegment
import com.yasuda.derbis.ui.theme.DerbisViolet
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import com.yasuda.derbis.viewmodel.DerbisUiState
import com.yasuda.derbis.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val CreditsPrefetchDelayMs = 1_200L
private const val CreditsCaptionRevealMs = 360
private val MinimumFittedPlayerHeight = 600.dp
private val ScrollingArtworkMax = 260.dp
private val HeaderSideReserve = 108.dp
private val HeaderButtonMinimumWidth = 40.dp
private val HeaderCenterMinimumWidth = 112.dp
private const val DefaultBackdropFocus = 0.34f
// UI identity only: stream provenance remains available in technical diagnostics.
internal const val PlayerMetadataSourceLabel = "DERBIS MUSIC METADATA"

/** Adds the active video resolution only when the player has a concrete selected quality. */
internal fun playerMetadataSourceLabel(activeQualityLabel: String?): String {
    val quality = activeQualityLabel?.trim().orEmpty()
    // The compact source line uses only the resolution number: “360p” becomes “360”.
    val resolution = quality.substringBefore('p').trim()
    return if (resolution.isBlank()) PlayerMetadataSourceLabel else "$PlayerMetadataSourceLabel • $resolution"
}

@Composable
fun DerbisNowPlaying(
    viewModel: PlayerViewModel,
    state: DerbisUiState,
    morphAnchors: PlayerMorphAnchors,
    morphActive: () -> Boolean,
    collapseActions: PlayerCollapseActions,
    modifier: Modifier = Modifier,
    motionSuspended: Boolean = false,
    videoSurfaceContent: (@Composable (Track, Modifier) -> Unit)? = null,
    gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, androidx.compose.runtime.MutableState<PlayerVideoTransform>?, Modifier) -> Unit)? = null,
    engagementContent: (@Composable (Track) -> Unit)? = null,
    similarSongsContent: (@Composable (Track) -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null,
    playlistDialogContent: (@Composable () -> Unit)? = null,
    onOpenPlaylistDialog: ((Track) -> Unit)? = null
) {
    // Apple Music 13 is its own deck. Velvet remains the Canvas/Aura deck and must not
    // enter this scope, otherwise the two choices resolve to the same Apple Music scene.
    val appleMusic13 = !DerbisSkinManagesLayout && state.interfaceSettings.playerVisualMode.usesAppleMusicPlayerStyle()
    AppleMusicPlayerStyleScope(appleMusic13) {
        DerbisNowPlayingContent(
            viewModel, state, morphAnchors, morphActive, collapseActions, modifier, motionSuspended,
            videoSurfaceContent, gestureLayerContent, engagementContent,
            similarSongsContent, errorContent, playlistDialogContent, onOpenPlaylistDialog
        )
    }
}

@Composable
private fun DerbisNowPlayingContent(
    viewModel: PlayerViewModel,
    state: DerbisUiState,
    morphAnchors: PlayerMorphAnchors,
    morphActive: () -> Boolean,
    collapseActions: PlayerCollapseActions,
    modifier: Modifier,
    motionSuspended: Boolean,
    videoSurfaceContent: (@Composable (Track, Modifier) -> Unit)?,
    gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, androidx.compose.runtime.MutableState<PlayerVideoTransform>?, Modifier) -> Unit)?,
    engagementContent: (@Composable (Track) -> Unit)?,
    similarSongsContent: (@Composable (Track) -> Unit)?,
    errorContent: (@Composable () -> Unit)?,
    playlistDialogContent: (@Composable () -> Unit)?,
    onOpenPlaylistDialog: ((Track) -> Unit)?
) {
    val strings = LocalDerbisStrings.current
    val track = state.currentTrack
    val liveRadio = track?.isLiveRadio() == true
    val liveRadioStrings = DerbisLiveRadioCatalog.forCode(strings.code)
    val playerContext = LocalContext.current
    val playerActivity = playerContext as? Activity
    val audioManager = remember(playerContext) { playerContext.getSystemService(AudioManager::class.java) }
    val hapticFeedback = LocalDerbisHaptics.current
    val density = LocalDensity.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty()
    val nocturne = DerbisIsNocturne
    // TIDAL Style supplies the palette, while the original Tidal Music deck owns its layout.
    val selectedVisualMode = if (DerbisIsPrism && !DerbisIsTidal) PlayerVisualMode.Artwork else state.interfaceSettings.playerVisualMode
    val appleMusicPalette = LocalAppleMusicPlayerPalette.current
    val appleMusicDeck = !DerbisSkinManagesLayout && selectedVisualMode.usesAppleMusicPlayerStyle()
    val appleMusicPlayer = DerbisIsAppleMusic || appleMusicDeck
    val visualMode = selectedVisualMode
    val animated = state.animationsEnabled
    val lyricsAudioRoute = rememberLyricsAudioOutputRoute()
    val lyricsEffectiveOffsetMs = remember(track?.id, lyricsAudioRoute, state.lyricsLatencyProfiles) {
        state.lyricsLatencyProfiles.effectiveOffsetMs(track?.id, lyricsAudioRoute?.stableKey, lyricsAudioRoute?.bluetooth == true)
    }
    // Keeps the compact Apple Music 13 lyric highlight smooth between player progress updates with latency calibration.
    val appleMusicLyricsClock = rememberLyricsPlaybackClock(
        trackKey = track?.id.orEmpty(),
        reportedPositionMs = lyricsOffsetPosition(state.positionMs, lyricsEffectiveOffsetMs),
        isPlaying = state.isPlaying,
        speed = state.playbackSpeed,
        smoothingEnabled = animated && appleMusicDeck && (state.activeLyric != null || state.lyricsSynced)
    )
    val lyricsEngine = remember { LyricsEngine() }

    val fallbackPalette = remember(track?.accentStart, track?.accentEnd) {
        ArtworkPalette(track?.accentStart ?: DerbisCyan.toArgb(), track?.accentEnd ?: DerbisViolet.toArgb())
    }
    val paletteKey = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        if (track != null) {
            ArtworkPaletteCache.key(
                trackId = track.id,
                thumbnailUrl = track.thumbnailUrl,
                largeThumbnailUrl = track.largeThumbnailUrl
            )
        } else ""
    }
    val paletteArtworkUrl = track?.let { current ->
        artworkUrl.ifBlank { current.largeThumbnailUrl.ifBlank { current.thumbnailUrl } }
    }.orEmpty()
    val fixedAccents = if (appleMusicPalette != null) {
        PlayerAccentPair(appleMusicPalette.cyan, appleMusicPalette.violet)
    } else if (DerbisIsPrism) PlayerAccentPair(DerbisCyan, DerbisViolet) else rememberPlayerFixedAccents(state.interfaceSettings)
    val activePalette by rememberArtworkPalette(
        paletteKey = if (fixedAccents == null) paletteKey else "",
        artworkUrl = paletteArtworkUrl,
        fallback = fallbackPalette
    )
    // Animated artwork and Canvas only need the user toggle. The Spotify canvas provider simply
    // returns nothing when it has no match, so a Spotify session is not a requirement here.
    val motionArtworkAllowed = state.interfaceSettings.motionArtworkEnabled
    // Lite Mode stops expensive Canvas decoding and animated artwork before it reaches the player.
    val motionEnabled = animated && !state.interfaceSettings.liteMode && !state.isVideoMode && !motionSuspended && motionArtworkAllowed
    var canvasBackdropPalette by remember(track?.id, state.isVideoMode) {
        mutableStateOf<MotionBackdropPalette?>(null)
    }
    val rawPrimaryTarget = Color(activePalette.start)
    val rawSecondaryTarget = Color(activePalette.end)
    val artworkTargets = remember(rawPrimaryTarget, rawSecondaryTarget) {
        harmonizePlayerAccents(rawPrimaryTarget, rawSecondaryTarget)
    }
    val harmonizedTargets = fixedAccents ?: artworkTargets
    val primaryTarget = harmonizedTargets.primary
    val secondaryTarget = harmonizedTargets.secondary
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-primary-color"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-secondary-color"
    )
    // Pure black is the OLED switch, not a background style: the deck keeps its artwork backdrop
    // and only the control surfaces go black.
    val amoled = state.interfaceSettings.pureBlack && !nocturne
    val surfaces = if (appleMusicPalette != null) prismPlayerSurfaces(appleMusicPalette) else if (DerbisIsPrism) prismPlayerSurfaces(DerbisActivePalette) else remember(primaryTarget, amoled, nocturne) { playerSurfaceTokens(primaryTarget, amoled, nocturne) }
    val heroTone by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-hero-tone"
    )
    val livingArtwork = remember(primaryTarget, secondaryTarget) {
        livingArtworkColors(primaryTarget, secondaryTarget)
    }
    val playerControlLabels = remember(strings) {
        PlayerControlLabels(
            shuffle = strings.shuffle,
            previous = strings.previous,
            play = strings.play,
            pause = strings.pause,
            next = strings.next,
            repeat = strings.repeat
        )
    }
    var showActions by remember { mutableStateOf(false) }
    var showTrackStoryShare by remember(track?.id) { mutableStateOf(false) }
    var showTechnicalAudioInfo by remember(track?.id) { mutableStateOf(false) }
    var showTrackCredits by remember(track?.id) { mutableStateOf(false) }
    var creditWriters by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    LaunchedEffect(track?.id, track?.composer, liveRadio) {
        val current = track
        if (current == null || liveRadio) return@LaunchedEffect
        delay(CreditsPrefetchDelayMs)
        val writers = viewModel.trackCredits(current, false).writers
        creditWriters = current.id to writers
    }
    var showAudioRouteCenter by rememberSaveable { mutableStateOf(false) }
    var showDeck by rememberSaveable { mutableStateOf(false) }
    var downloadRemovalTrackId by rememberSaveable { mutableStateOf<String?>(null) }
    var showArtistPicker by remember(track?.id) { mutableStateOf(false) }
    var openArtistPickerAfterActions by remember(track?.id) { mutableStateOf(false) }
    var playerArtistHits by remember(track?.id, track?.artist, track?.artistBrowseIds) {
        mutableStateOf(emptyList<ArtistHit>())
    }
    LaunchedEffect(track?.id, track?.artist, track?.artistBrowseIds) {
        val activeTrack = track
        playerArtistHits = emptyList()
        if (activeTrack == null || activeTrack.isLiveRadio()) {
            showArtistPicker = false
            return@LaunchedEffect
        }
        playerArtistHits = viewModel.playerArtistHits(activeTrack, resolveArtwork = false)
        playerArtistHits = viewModel.playerArtistHits(activeTrack, resolveArtwork = true)
        if (playerArtistHits.size < 2) showArtistPicker = false
    }
    LaunchedEffect(state.isVideoMode, track == null) {
        if (state.isVideoMode || track == null) {
            showDeck = false
            showAudioRouteCenter = false
        }
    }
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    var gestureFeedback by remember(track?.id) { mutableStateOf("") }
    var gestureFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    val swipe = rememberPlayerSwipeMotion(animated)
    val swipeOffset: () -> Float = { swipe.offsetFor(track?.id) }
    val stepDirection = rememberTrackStepDirection(
        trackId = track?.id,
        queue = state.queue,
        queueIndex = state.queueCurrentIndex
    )
    val lyricsFlip = rememberPlayerLyricsFlipState()
    val lyricsFlipDepth = LocalDerbisVisualCapabilities.current.depthTransitions
    val latestMorphActive by rememberUpdatedState(morphActive)

    LaunchedEffect(mediaSeekFeedbackEvent) {
        if (mediaSeekFeedbackEvent > 0) {
            delay(650L)
            mediaSeekFeedbackMs = 0L
        }
    }
    LaunchedEffect(gestureFeedbackEvent) {
        if (gestureFeedbackEvent > 0) {
            delay(700L)
            gestureFeedback = ""
        }
    }

    // Pausing must never shrink or drop the album artwork. Playback state changes only the
    // transport glyph and animated media layers; the player artwork retains its full geometry.
    val artRestScale = 1f
    val artRestOffset = 0.dp
    SideEffect {
        morphAnchors.updateFullRest(artRestScale, with(density) { artRestOffset.toPx() })
    }
    val artScale by animateFloatAsState(
        targetValue = artRestScale,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.expressiveSpring()),
        label = "artwork-scale"
    )
    val artOffset by animateDpAsState(
        targetValue = artRestOffset,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.expressiveSpring()),
        label = "artwork-offset"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val layoutMode = resolveDerbisLayoutMode(maxWidth.value, maxHeight.value)
        val compactLandscape = isDerbisCompactLandscape(maxWidth.value, maxHeight.value)
        val playerPane = resolveNowPlayingPane(maxWidth.value, maxHeight.value, state.isVideoMode)
        val appleMusicCanvas = AppleMusicCanvas(state.motionArtwork, motionEnabled && !lyricsFlip.lyricsSettled, state.interfaceSettings.canvasQuality, livingArtwork)
        val appleMusicImmersive = appleMusicPlayer && appleMusicCanvasImmersive(appleMusicCanvas, maxWidth, maxHeight, state.isVideoMode)
        val showTopArtistCluster = playerArtistHits.size > 1 &&
            !appleMusicImmersive &&
            !liveRadio &&
            !state.isVideoMode &&
            playerPane != DerbisPlayerPane.SideBySide
        val deckLayout = if (DerbisSkinManagesLayout) PlayerDeckLayout.Derbis else resolvePlayerDeckLayout(
            mode = visualMode,
            isVideoMode = state.isVideoMode,
            isLiveRadio = liveRadio,
            pane = playerPane,
            hasTrack = track != null
        )
        val deckMode = if (liveRadio) PlayerVisualMode.Artwork else resolvePlayerDeckVisualMode(visualMode, deckLayout)
        val deckOwnsStageGestures = deckLayout != PlayerDeckLayout.Derbis
        val lyricsFlipAvailable = track != null && !liveRadio && !state.isVideoMode && deckLayout.showsLyricsFace()
        LaunchedEffect(lyricsFlipAvailable) {
            snapshotFlow { latestMorphActive() }.collect { active ->
                if (!lyricsFlipAvailable || active) lyricsFlip.snapTo(PlayerLyricsFace.Player)
            }
        }
        val lyricsFlipSwipeModifier = Modifier.playerLyricsFlipDrag(
            state = lyricsFlip,
            enabled = lyricsFlipAvailable,
            rightToLeft = rightToLeft,
            depth = lyricsFlipDepth
        )
        SideEffect {
            morphAnchors.updateLanding(
                when (deckLayout) {
                    PlayerDeckLayout.Folio -> PlayerMorphLanding.Cookie
                    PlayerDeckLayout.Flow -> if (state.isPlaying) PlayerMorphLanding.FlowPlaying else PlayerMorphLanding.FlowPaused
                    else -> PlayerMorphLanding.Rounded
                }
            )
        }
        val motionBackdropVisible = !liveRadio
        LaunchedEffect(
            motionEnabled,
            motionBackdropVisible,
            state.motionArtwork?.identityKey
        ) {
            val activeIdentity = state.motionArtwork?.identityKey
            if (
                !canvasBackdropPaletteMatches(
                    palette = canvasBackdropPalette,
                    motionEnabled = motionEnabled,
                    motionBackdropVisible = motionBackdropVisible,
                    activeIdentity = activeIdentity
                )
            ) {
                canvasBackdropPalette = null
            }
        }
        val canvasBackdropTargets = canvasBackdropPalette
            ?.takeIf { fixedAccents == null }
            ?.takeIf { palette ->
                canvasBackdropPaletteMatches(
                    palette = palette,
                    motionEnabled = motionEnabled,
                    motionBackdropVisible = motionBackdropVisible,
                    activeIdentity = state.motionArtwork?.identityKey
                )
            }
            ?.let { palette -> harmonizePlayerAccents(palette.primary, palette.secondary) }
        val backdropPrimaryTarget = canvasBackdropTargets?.primary ?: primaryTarget
        val backdropSecondaryTarget = canvasBackdropTargets?.secondary ?: secondaryTarget
        val backdropAmbience = remember(backdropPrimaryTarget, backdropSecondaryTarget) {
            playerAmbienceOf(backdropPrimaryTarget, backdropSecondaryTarget)
        }
        val acceptDynamicBackdropPalette: (MotionBackdropPalette?) -> Unit = { palette ->
            val activeIdentity = state.motionArtwork?.identityKey
            if (palette == null || (activeIdentity != null && palette.identityKey == activeIdentity)) {
                canvasBackdropPalette = palette
            }
        }
        val compactPlayer = layoutMode == DerbisLayoutMode.Compact && (maxWidth < 380.dp || maxHeight < 720.dp)
        val fitsViewport = maxHeight >= MinimumFittedPlayerHeight || playerPane == DerbisPlayerPane.SideBySide
        val gutter = if (state.isVideoMode) {
            DerbisPlayerDesign.SpaceMd
        } else {
            derbisFoldAwareGutterDp(layoutMode, compactPlayer).dp
        }
        val detailMaxWidth = derbisContentMaxWidthDp(layoutMode).dp
        val artworkCap = derbisPlayerArtworkMaxWidthDp(playerPane, layoutMode).dp
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val scrollingArtworkHeight = min(maxWidth - gutter * 2, ScrollingArtworkMax)

        val artworkPreviewAvailable = !liveRadio && !state.isVideoMode && artworkUrl.isNotBlank() && deckMode == PlayerVisualMode.Artwork
        var showArtworkPreview by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        var videoFullscreen by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        val videoTransform = remember(track?.id, state.isVideoMode) {
            mutableStateOf(PlayerVideoTransform.None)
        }
        BackHandler(enabled = videoFullscreen) {
            videoFullscreen = false
        }

        var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var stageTopPx by remember { mutableIntStateOf(-1) }
        var stageBottomPx by remember { mutableIntStateOf(-1) }
        val backdropFocus = if (stageBottomPx > 0 && containerHeightPx > 0f) {
            ((stageTopPx + stageBottomPx) / 2f / containerHeightPx).coerceIn(0.1f, 0.9f)
        } else {
            DefaultBackdropFocus
        }

        PlayerVisualHost(
            artworkUrl = artworkUrl,
            ambience = backdropAmbience,
            animationsEnabled = animated,
            isPlaying = state.isPlaying,
            fixedColors = fixedAccents != null,
            backdropFocus = backdropFocus,
            modifier = Modifier.fillMaxSize()
        )

        val headerButtonFill = surfaces.controlQuiet
        val headerButtonBorder = if (surfaces.amoled) surfaces.outline else Color.Transparent
        val headerButtonSize = if (compactPlayer) DerbisPlayerDesign.HeaderButtonCompact else DerbisPlayerDesign.HeaderButton
        val headerSlotWidth = maxOf(headerButtonSize, HeaderButtonMinimumWidth)
        val headerPolicy = playerHeaderPolicy(
            isVideoMode = state.isVideoMode,
            isLiveRadio = liveRadio,
            hasTrack = track != null,
            hasSubtitles = track?.videoSubtitleTracks?.isNotEmpty() == true,
            videoQualityAvailable = state.videoQuality.available,
            slotWidthDp = headerSlotWidth.value,
            spacingDp = DerbisPlayerDesign.SpaceXs.value,
            sideReserveDp = HeaderSideReserve.value
        )
        val headerCentered = headerPolicy.centerLocked

        val headerLeading: @Composable () -> Unit = {
            PlayerGlassIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.collapsePlayer,
                size = headerButtonSize,
                iconSize = 24.dp,
                fill = headerButtonFill,
                borderTop = headerButtonBorder,
                borderBottom = headerButtonBorder,
                onClick = collapseActions.collapse
            )
        }
        val headerCenter: @Composable () -> Unit = {
            // Music-only playback: metadata for an optional video counterpart must never replace
            // the normal player identity with a song/video switch.
            PlayerSourceEyebrow(
                label = strings.playingFrom,
                source = playerMetadataSourceLabel(
                    activeQualityLabel = state.videoQuality.activeLabel.takeIf { state.isVideoMode }
                ),
                surfaces = surfaces
            )
        }
        val headerTrailing: @Composable () -> Unit = {
            if (!state.isVideoMode && !liveRadio) {
                CastRouteButton(modifier = Modifier.size(headerButtonSize))
            }

            if (track != null) {
                PlayerGlassIconButton(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = strings.more,
                    size = headerButtonSize,
                    iconSize = 22.dp,
                    fill = headerButtonFill,
                    borderTop = headerButtonBorder,
                    borderBottom = headerButtonBorder,
                    onClick = { showActions = true }
                )
            }
        }

        val headerRow: @Composable (Boolean) -> Unit = { centered ->
            if (centered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DerbisPlayerDesign.MinimumTouchTarget)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs)
                    ) { headerLeading() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = HeaderSideReserve),
                        contentAlignment = Alignment.Center
                    ) { headerCenter() }
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs)
                    ) { headerTrailing() }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DerbisPlayerDesign.MinimumTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs)
                ) {
                    headerLeading()
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) { headerCenter() }
                    headerTrailing()
                }
            }
        }
        val headerBlock: @Composable () -> Unit = { headerRow(headerCentered) }

        val stageGestureBlock: @Composable (Track, Modifier) -> Unit = { activeTrack, gestureModifier ->
            val videoGesturesEnabled = state.isVideoMode && activeTrack.videoUrl.isNotBlank()
            val gesturesAllowed = !liveRadio &&
                (state.interfaceSettings.swipeTrackChangeEnabled || videoGesturesEnabled) &&
                !videoFullscreen &&
                lyricsFlip.playerSettled
            Box(modifier = gestureModifier) {
                if (gesturesAllowed && gestureLayerContent != null) {
                    gestureLayerContent(
                        activeTrack,
                        PlayerGestureConfig(
                            trackId = activeTrack.id,
                            settings = state.interfaceSettings,
                            playbackSpeed = state.playbackSpeed,
                            environment = PlayerGestureEnvironment(
                                activity = playerActivity,
                                audioManager = audioManager,
                                brightnessLabel = strings.brightness,
                                volumeLabel = strings.volume,
                                rightToLeft = rightToLeft
                            )
                        ),
                        PlayerGestureMediaActions(
                            seekBy = { delta ->
                                viewModel.seekBy(delta)
                                hapticFeedback.perform(DerbisHapticAction.TrackSwipe)
                                mediaSeekFeedbackMs = delta
                                mediaSeekFeedbackEvent += 1
                            },
                            togglePlay = viewModel::togglePlay,
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { offset -> swipe.follow(activeTrack.id, offset) },
                            swipeSettled = { committed -> swipe.release(committed, carry = false) },
                            temporarySpeed = viewModel::setTemporaryPlaybackSpeed
                        ),
                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(DerbisHapticAction.TrackSwipe)
                            },
                            toggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                            openQueue = viewModel::openQueue,
                            openLyrics = viewModel::openLyrics,
                            collapse = collapseActions,
                            artworkPreview = if (artworkPreviewAvailable) {
                                { showArtworkPreview = true }
                            } else {
                                null
                            }
                        ),
                        videoTransform.takeIf { videoGesturesEnabled },
                        Modifier.matchParentSize()
                    )
                }
                PlayerGestureFeedbackPill(
                    text = gestureFeedback,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        val mediaHeroBlock: @Composable (Track, Dp, Dp?) -> Unit = { activeTrack, heroSize, cornerOverride ->
            val artworkCorner = cornerOverride ?: DerbisPlayerShapes.artworkCorner(heroSize)
            Box(
                modifier = if (state.isVideoMode) {
                    Modifier.aspectRatio(16f / 9f)
                } else {
                    Modifier.size(heroSize)
                },
                contentAlignment = Alignment.Center
            ) {
                if (state.isVideoMode && activeTrack.videoUrl.isNotBlank() && videoSurfaceContent != null) {
                    val zoom = videoTransform.value
                    if (!videoFullscreen) {
                        val videoShape = RoundedCornerShape(cornerOverride ?: DerbisPlayerDesign.CornerMd)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = artScale
                                    scaleY = artScale
                                    translationY = artOffset.toPx()
                                    shadowElevation = 24.dp.toPx()
                                    shape = videoShape
                                    clip = true
                                }
                        ) {
                            videoSurfaceContent(
                                activeTrack,
                                Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = zoom.scale
                                        scaleY = zoom.scale
                                        translationX = zoom.offsetX
                                        translationY = zoom.offsetY
                                    }
                            )
                        }
                    }
                } else {
                    PlayerArtworkHero(
                        track = activeTrack,
                        artworkUrl = artworkUrl,
                        motionArtwork = state.motionArtwork,
                        livingArtwork = livingArtwork,
                        animationsEnabled = animated && !state.isVideoMode,
                        motionEnabled = motionEnabled && !lyricsFlip.lyricsSettled,
                        isPlaying = state.isPlaying,
                        cornerRadius = artworkCorner,
                        canvasQuality = state.interfaceSettings.canvasQuality,
                        morphAnchors = morphAnchors,
                        morphActive = morphActive,
                        swipeOffset = swipeOffset,
                        artScale = artScale,
                        artOffset = artOffset,
                        modifier = Modifier
                            .fillMaxSize()
                            .playerLyricsFlipFace(
                                state = lyricsFlip,
                                back = false,
                                depth = lyricsFlipDepth,
                                rightToLeft = rightToLeft
                            ),
                        // The Canvas palette feeds the wet wash behind the deck in every mode now,
                        // so the hero only stops publishing it when Canvas itself is off.
                        dynamicBackdropEnabled = motionBackdropVisible,
                        onDynamicBackdropPalette = acceptDynamicBackdropPalette
                    )
                    if (lyricsFlipAvailable && lyricsFlip.lyricsComposed) {
                        PlayerLyricsCard(
                            trackId = activeTrack.id,
                            lines = state.lyrics,
                            synced = state.lyricsSynced,
                            loading = state.lyricsLoading,
                            positionMs = state.positionMs,
                            isPlaying = state.isPlaying,
                            playbackSpeed = state.playbackSpeed,
                            latencyProfiles = state.lyricsLatencyProfiles,
                            interactive = lyricsFlip.lyricsSettled,
                            animated = animated,
                            cornerRadius = artworkCorner,
                            surfaces = surfaces,
                            accent = primaryTarget,
                            onSeekToMs = { positionMs ->
                                if (state.durationMs > 0L) {
                                    viewModel.seekTo((positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f))
                                }
                            },
                            onShowArtwork = { lyricsFlip.show(PlayerLyricsFace.Player, lyricsFlipDepth) },
                            onOpenFullLyrics = viewModel::openLyrics,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(19f)
                                .then(lyricsFlipSwipeModifier)
                                .playerLyricsFlipFace(
                                    state = lyricsFlip,
                                    back = true,
                                    depth = lyricsFlipDepth,
                                    rightToLeft = rightToLeft,
                                    frontVisible = true
                                )
                        )
                    }
                }

                val videoGesturesEnabled = state.isVideoMode && activeTrack.videoUrl.isNotBlank()
                if (!deckOwnsStageGestures) {
                    stageGestureBlock(
                        activeTrack,
                        Modifier
                            .matchParentSize()
                            .zIndex(if (videoGesturesEnabled) 21f else 20f)
                    )
                }

                if (videoGesturesEnabled && !videoFullscreen) {
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = strings.enterImmersive,
                        size = DerbisPlayerDesign.HeaderButton,
                        iconSize = 21.dp,
                        fill = Color.Black.copy(alpha = 0.42f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = DerbisPlayerDesign.SpaceSm, bottom = DerbisPlayerDesign.SpaceSm)
                            .zIndex(23f),
                        onClick = { videoFullscreen = true }
                    )
                }

                if (showArtworkPreview && artworkPreviewAvailable) {
                    ArtworkPreviewOverlay(
                        artworkUrl = artworkUrl,
                        title = activeTrack.title,
                        previewLabel = strings.artworkPreview,
                        closeLabel = strings.close,
                        saveLabel = strings.saveArtwork,
                        savedMessage = strings.artworkSaved,
                        saveFailedMessage = strings.artworkSaveFailed,
                        onFeedback = { message ->
                            gestureFeedback = message
                            gestureFeedbackEvent += 1
                        },
                        onDismiss = { showArtworkPreview = false }
                    )
                }

            }
        }

        val stageBlock: @Composable (Track, Modifier, Dp?) -> Unit = { activeTrack, stageModifier, stageCorner ->
            BoxWithConstraints(
                modifier = stageModifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val root = rootCoordinates ?: return@onGloballyPositioned
                        if (!root.isAttached || !coordinates.isAttached) return@onGloballyPositioned
                        val top = root.localPositionOf(coordinates, Offset.Zero).y.roundToInt()
                        val bottom = top + coordinates.size.height
                        if (top != stageTopPx) stageTopPx = top
                        if (bottom != stageBottomPx) stageBottomPx = bottom
                    },
                contentAlignment = Alignment.Center
            ) {
                val heroSize = minOf(maxWidth, maxHeight, artworkCap)
                if (heroSize > 0.dp) {
                    mediaHeroBlock(activeTrack, heroSize, stageCorner)
                }
            }
        }

        val metadataBlock: @Composable (Track) -> Unit = { activeTrack ->
            if (activeTrack.isLiveRadio()) {
                PlayerLiveRadioMetadata(
                    track = activeTrack,
                    station = state.liveRadioStation,
                    animated = animated,
                    nowPlaying = when {
                        state.isResolving && state.liveRadioReconnectAttempt > 0 -> DerbisLiveRadioCatalog.reconnecting(strings.code)
                        else -> state.playerError ?: state.liveRadioNowPlaying
                    },
                    liveLabel = liveRadioStrings.live,
                    details = state.liveRadioStation?.let { station ->
                        listOf(station.country, station.language, station.qualityLabel)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                    }.orEmpty(),
                    surfaces = surfaces,
                    compact = compactPlayer
                )
            } else {
                PlayerTrackMetadata(
                    track = activeTrack,
                    isFavorite = activeTrack.id in state.favoriteIds,
                    surfaces = surfaces,
                    animationsEnabled = animated,
                    stepDirection = stepDirection,
                    compact = compactPlayer,
                    openArtistLabel = strings.openArtist,
                    favoritesLabel = strings.favoritesPlain,
                    // Same timeline as the lyrics page: latency, device and per-track offsets
                    // are already folded into the clock, so this preview cannot drift from it.
                    activeLyric = if (state.lyricsSynced && state.lyrics.isNotEmpty()) {
                        lyricsEngine.currentLine(appleMusicLyricsClock.positionMs, state.lyrics)
                    } else {
                        state.activeLyric
                    },
                    lyricsLoading = state.lyricsLoading,
                    // Apple Music 13 retains its own word-timed line directly beneath metadata;
                    // other decks show their active preview here while all retain the Lyrics link.
                    showLyricPreview = !appleMusicPlayer,
                    lyricsLabel = strings.lyrics,
                    onOpenLyrics = viewModel::openLyrics,
                    onArtistClick = { artistIndex ->
                        playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                            ?: viewModel.openArtist(activeTrack, artistIndex)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                    modifier = lyricsFlipSwipeModifier
                )
            }
        }

        val progressBlock: @Composable (PlayerProgressStyle) -> Unit = { style ->
            if (liveRadio) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DerbisPlayerDesign.SpaceMd)
                        .height(DerbisPlayerDesign.TrackHeight)
                        .background(
                            heroTone.copy(alpha = if (state.isResolving) 0.36f else 0.9f),
                            CircleShape
                        )
                )
            } else if (appleMusicPlayer) {
                AppleMusicSeekbar(
                    positionMs = state.positionMs,
                    bufferedMs = state.bufferedPositionMs,
                    durationMs = state.durationMs,
                    onSeek = viewModel::seekTo
                )
                errorContent?.invoke()
            } else {
                PlayerProgress(
                    positionMs = state.positionMs,
                    bufferedPositionMs = state.bufferedPositionMs,
                    durationMs = state.durationMs,
                    activeColor = if (nocturne) primary else heroTone,
                    secondaryColor = secondary,
                    surfaces = surfaces,
                    isPlaying = state.isPlaying,
                    playbackSpeed = state.playbackSpeed.coerceIn(0.5f, 2f),
                    animationsEnabled = animated,
                    motionActive = !motionSuspended,
                    compact = compactPlayer,
                    crossfadeSeconds = state.audioSettings.crossfadeSeconds,
                    onSeek = viewModel::seekTo,
                    style = style
                )
            }
        }

        val transportBlock: @Composable () -> Unit = {
            if (liveRadio) {
                PlayerLiveRadioTransport(
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    surfaces = surfaces,
                    compact = compactPlayer,
                    animated = animated,
                    strings = strings,
                    onStop = viewModel::closePlayer,
                    onToggle = viewModel::togglePlay
                )
            } else if (appleMusicPlayer) {
                AppleMusicTransport(
                    playing = state.isPlaying,
                    resolving = state.isResolving,
                    shuffle = state.shuffleEnabled,
                    repeat = state.repeatMode,
                    labels = playerControlLabels,
                    onShuffle = viewModel::toggleShuffle,
                    onPrevious = viewModel::previous,
                    onToggle = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onRepeat = viewModel::toggleRepeat
                )
            } else {
                PlayerTransportBar(
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    shuffleOn = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    surfaces = surfaces,
                    compact = compactPlayer,
                    animated = animated,
                    useAuraPlayPause = !nocturne && visualMode.usesAuraPlayPause(),
                    labels = playerControlLabels,
                    onShuffle = viewModel::toggleShuffle,
                    onPrevious = viewModel::previous,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onRepeat = viewModel::toggleRepeat
                )
            }
        }

        val dockActionsFor: @Composable (Track) -> List<PlayerDockAction> = { activeTrack ->
            val isDownloaded = activeTrack.id in state.downloadedTrackIds
            remember(
                activeTrack.id,
                state.showLyrics,
                state.isVideoMode,
                state.isOfflineExporting,
                isDownloaded,
                selectedVisualMode,
                visualMode,
                nocturne,
                DerbisSkinManagesLayout,
                DerbisSkinLabel,
                strings,
                viewModel,
                hapticFeedback
            ) {
                playerDockActions(
                    strings = strings,
                    visualMode = selectedVisualMode,
                    presentedMode = visualMode,
                    showLyrics = state.showLyrics,
                    videoMode = state.isVideoMode,
                    isDownloaded = isDownloaded,
                    isExporting = state.isOfflineExporting,
                    onLyrics = viewModel::openLyrics,
                    onQueue = viewModel::openQueue,
                    onOpenDeck = { showDeck = true },
                    onDownload = if (isDownloaded) {
                        { downloadRemovalTrackId = activeTrack.id }
                    } else {
                        viewModel::exportCurrentTrack
                    }
                )
            }
        }

        val dockBlock: @Composable (Track) -> Unit = { activeTrack ->
            PlayerActionDock(
                actions = dockActionsFor(activeTrack),
                surfaces = surfaces,
                compact = compactPlayer,
                animated = animated
            )
        }

        val controlsTailBlock: @Composable ColumnScope.(Track) -> Unit = { activeTrack ->
            if (!activeTrack.isLiveRadio()) {
                engagementContent?.invoke(activeTrack)
            }
            Spacer(modifier = Modifier.height(if (compactPlayer) DerbisPlayerDesign.SpaceXs else DerbisPlayerDesign.SpaceSm))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                progressBlock(PlayerProgressStyle.Classic)
            }
            errorContent?.invoke()
            Spacer(modifier = Modifier.height(if (compactPlayer) DerbisPlayerDesign.SpaceXs else DerbisPlayerDesign.SpaceMd))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                transportBlock()
            }
            if (!activeTrack.isLiveRadio()) {
                Spacer(modifier = Modifier.height(if (compactPlayer) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg + DerbisPlayerDesign.SpaceXs))
                dockBlock(activeTrack)
            }
        }

        val controlsBlock: @Composable ColumnScope.(Track) -> Unit = { activeTrack ->
            metadataBlock(activeTrack)
            controlsTailBlock(activeTrack)
        }

        val rootModifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }

        val deckSlots = PlayerDeckSlots(
            header = headerBlock,
            stage = stageBlock,
            gestures = stageGestureBlock,
            controls = controlsBlock,
            controlsWithoutMetadata = controlsTailBlock,
            engagement = { activeTrack ->
                if (!activeTrack.isLiveRadio() && engagementContent != null) {
                    engagementContent(activeTrack)
                    Spacer(modifier = Modifier.height(if (compactPlayer) DerbisPlayerDesign.SpaceXs else DerbisPlayerDesign.SpaceSm))
                }
            },
            progress = { style ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    progressBlock(style)
                }
                errorContent?.invoke()
            },
            dock = dockBlock,
            dockActions = dockActionsFor,
            creditsEyebrow = creditWriters?.takeIf { it.second.isNotEmpty() }?.let { (id, writers) ->
                PlayerCreditsEyebrow(
                    trackId = id,
                    heading = strings.trackCreditsCopy().writtenBy,
                    names = writers,
                    contentDescription = strings.trackCreditsCopy().title,
                    onClick = { showTrackCredits = true }
                )
            }
        )
        val deckModifier = rootModifier
            .widthIn(max = detailMaxWidth)
            .align(Alignment.TopCenter)

        if (((DerbisIsPrism && !DerbisIsTidal) || appleMusicDeck) && track != null && (!state.isVideoMode || appleMusicPlayer) && !liveRadio && (playerPane != DerbisPlayerPane.SideBySide || appleMusicPlayer)) {
            if (DerbisIsYouTubeMusic) {
                YouTubeMusicPlayerScene(
                    track, deckSlots, artworkCap, state.isPlaying,
                    metadata = { metadataBlock(track) }, transport = transportBlock, modifier = deckModifier,
                    canvas = AppleMusicCanvas(state.motionArtwork, motionEnabled && !lyricsFlip.lyricsSettled, state.interfaceSettings.canvasQuality, livingArtwork)
                )
            } else if (appleMusicPlayer) {
                val compensatedActiveLyric = remember(appleMusicLyricsClock.positionMs, state.lyrics) {
                    if (state.lyricsSynced && state.lyrics.isNotEmpty()) {
                        lyricsEngine.currentLine(appleMusicLyricsClock.positionMs, state.lyrics)
                    } else null
                } ?: state.activeLyric
                AppleMusicPlayerScene(
                    track, deckSlots, artworkCap, state.isPlaying,
                    metadata = { metadataBlock(track) }, transport = transportBlock, modifier = deckModifier,
                    canvas = appleMusicCanvas,
                    video = state.isVideoMode,
                    activeLyric = compensatedActiveLyric,
                    lyricPositionMs = appleMusicLyricsClock.positionMs
                )
            } else {
                val compensatedActiveLyric = remember(appleMusicLyricsClock.positionMs, state.lyrics) {
                    if (state.lyricsSynced && state.lyrics.isNotEmpty()) {
                        lyricsEngine.currentLine(appleMusicLyricsClock.positionMs, state.lyrics)
                    } else null
                } ?: state.activeLyric
                PrismPlayerScene(
                    track, deckSlots, artworkCap, metadata = { metadataBlock(track) }, transport = transportBlock, modifier = deckModifier, playing = state.isPlaying,
                    activeLyric = compensatedActiveLyric, lyricPositionMs = appleMusicLyricsClock.positionMs
                )
            }
        } else if (deckLayout == PlayerDeckLayout.TidalMusic && track != null) {
            TidalMusicPlayerScene(
                track = track,
                slots = deckSlots,
                artworkCap = artworkCap,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Aura && track != null) {
            PlayerAuraDeck(
                track = track,
                slots = deckSlots,
                artworkUrl = artworkUrl,
                motionArtwork = state.motionArtwork,
                motionEnabled = motionEnabled,
                canvasQuality = state.interfaceSettings.canvasQuality,
                isFavorite = track.id in state.favoriteIds,
                isDownloaded = track.id in state.downloadedTrackIds,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                labels = playerControlLabels,
                animated = animated,
                onCollapse = collapseActions.collapse,
                onMore = { showActions = true },
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onDownload = {
                    if (track.id in state.downloadedTrackIds) downloadRemovalTrackId = track.id else viewModel.exportCurrentTrack()
                },
                onSeek = viewModel::seekTo,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = rootModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Halo && track != null) {
            PlayerHaloDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                queuePosition = playerDeckQueuePosition(state.queue, track.id),
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                animated = animated,
                scrollable = !fitsViewport,
                gutter = gutter,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Folio && track != null) {
            PlayerFolioDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                artworkCap = artworkCap,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Sleeve && track != null) {
            PlayerSleeveDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                queuePosition = playerDeckQueuePosition(state.queue, track.id),
                isPlaying = state.isPlaying,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                artworkCap = artworkCap,
                stepDirection = stepDirection,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Flow && track != null) {
            PlayerFlowDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                artworkCap = artworkCap,
                stepDirection = stepDirection,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Expressive && track != null) {
            PlayerExpressiveDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Poster && track != null) {
            PlayerPosterDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                isFavorite = track.id in state.favoriteIds,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Pulse && track != null) {
            PlayerPulseDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                accent = heroTone,
                isFavorite = track.id in state.favoriteIds,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                labels = playerControlLabels,
                queuePosition = playerDeckQueuePosition(state.queue, track.id),
                seekStepMs = state.interfaceSettings.doubleTapSeekSeconds * 1_000L,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                artworkCap = artworkCap,
                stepDirection = stepDirection,
                onArtistClick = { artistIndex ->
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat,
                onSeekBy = viewModel::seekBy,
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (playerPane == DerbisPlayerPane.SideBySide && track != null && compactLandscape && !state.isVideoMode) {
            Row(
                modifier = rootModifier
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = gutter, vertical = DerbisPlayerDesign.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stageBlock(
                    track,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = DerbisPlayerDesign.SpaceSm),
                    null
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        headerRow(headerCentered && maxWidth - HeaderSideReserve * 2 >= HeaderCenterMinimumWidth)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        controlsBlock(track)
                    }
                }
            }
        } else if (playerPane == DerbisPlayerPane.SideBySide && track != null) {
            val stageWeight = if (compactLandscape) derbisCompactLandscapeVideoPaneWeight(maxWidth.value) else 1f
            Column(
                modifier = rootModifier
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(
                        horizontal = gutter,
                        vertical = if (compactLandscape) DerbisPlayerDesign.SpaceSm else DerbisPlayerDesign.SpaceMd
                    )
            ) {
                headerBlock()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stageBlock(
                        track,
                        Modifier
                            .weight(stageWeight)
                            .fillMaxHeight()
                            .padding(vertical = if (compactLandscape) DerbisPlayerDesign.SpaceSm else DerbisPlayerDesign.SpaceMd),
                        null
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        controlsBlock(track)
                    }
                }
            }
        } else {
            Column(
                modifier = rootModifier
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .then(if (fitsViewport) Modifier else Modifier.verticalScroll(rememberScrollState()))
                    .padding(
                        start = gutter,
                        end = gutter,
                        top = DerbisPlayerDesign.SpaceXs,
                        bottom = if (compactPlayer) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceXl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                headerBlock()
                if (track == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyPlayer,
                            color = surfaces.contentMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val stageSpacing = if (compactPlayer) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceXl
                    stageBlock(
                        track,
                        if (fitsViewport) {
                            Modifier
                                .weight(1f)
                                .padding(top = stageSpacing - DerbisPlayerDesign.SpaceXs, bottom = stageSpacing)
                        } else {
                            Modifier
                                .height(scrollingArtworkHeight)
                                .padding(vertical = DerbisPlayerDesign.SpaceMd)
                        },
                        null
                    )
                    controlsBlock(track)
                }
            }
        }

        if (videoFullscreen &&
            state.isVideoMode &&
            track != null &&
            track.videoUrl.isNotBlank() &&
            videoSurfaceContent != null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(100f)
            ) {
                val zoom = videoTransform.value
                videoSurfaceContent(
                    track,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom.scale
                            scaleY = zoom.scale
                            translationX = zoom.offsetX
                            translationY = zoom.offsetY
                        }
                )
                if (gestureLayerContent != null) {
                    gestureLayerContent(
                        track,
                        PlayerGestureConfig(
                            trackId = track.id,
                            settings = state.interfaceSettings,
                            playbackSpeed = state.playbackSpeed,
                            environment = PlayerGestureEnvironment(
                                activity = playerActivity,
                                audioManager = audioManager,
                                brightnessLabel = strings.brightness,
                                volumeLabel = strings.volume,
                                rightToLeft = rightToLeft
                            )
                        ),
                        PlayerGestureMediaActions(
                            seekBy = { delta ->
                                viewModel.seekBy(delta)
                                hapticFeedback.perform(DerbisHapticAction.TrackSwipe)
                                mediaSeekFeedbackMs = delta
                                mediaSeekFeedbackEvent += 1
                            },
                            togglePlay = viewModel::togglePlay,
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { offset -> swipe.follow(track.id, offset) },
                            swipeSettled = { committed -> swipe.release(committed, carry = false) },
                            temporarySpeed = viewModel::setTemporaryPlaybackSpeed
                        ),
                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(DerbisHapticAction.TrackSwipe)
                            },
                            toggleFavorite = { viewModel.toggleFavorite(track) },
                            openQueue = viewModel::openQueue,
                            openLyrics = viewModel::openLyrics,
                            collapse = collapseActions
                        ),
                        videoTransform,
                        Modifier
                            .matchParentSize()
                            .zIndex(101f)
                    )
                }
                PlayerGlassIconButton(
                    icon = Icons.Rounded.CloseFullscreen,
                    contentDescription = strings.exitImmersive,
                    size = DerbisPlayerDesign.MinimumTouchTarget,
                    iconSize = 22.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = DerbisPlayerDesign.SpaceSm, end = DerbisPlayerDesign.SpaceSm)
                        .zIndex(102f),
                    onClick = { videoFullscreen = false }
                )
            }
        }

        val captionWriters = creditWriters?.takeIf { it.first == track?.id }?.second.orEmpty()
        AnimatedVisibility(
            // Apple Music 13 keeps the upper artwork area clean; credits remain available from
            // the player actions instead of floating as a written-by overlay.
            visible = captionWriters.isNotEmpty() &&
                !liveRadio &&
                !appleMusicPlayer &&
                !appleMusicImmersive &&
                !state.isVideoMode &&
                playerPane != DerbisPlayerPane.SideBySide &&
                deckLayout != PlayerDeckLayout.Aura &&
                deckLayout != PlayerDeckLayout.Pulse,
            enter = fadeIn(tween(if (animated) CreditsCaptionRevealMs else 0)),
            exit = fadeOut(tween(if (animated) CreditsCaptionRevealMs / 2 else 0)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(
                    end = gutter,
                    top = DerbisPlayerDesign.MinimumTouchTarget + DerbisPlayerDesign.SpaceLg
                )
                .zIndex(24f)
        ) {
            PlayerCreditsCaption(
                heading = strings.trackCreditsCopy().writtenBy,
                names = captionWriters,
                contentDescription = strings.trackCreditsCopy().title,
                surfaces = surfaces,
                onClick = { showTrackCredits = true }
            )
        }

        if (showTopArtistCluster) {
            PlayerTopArtistCluster(
                artists = playerArtistHits,
                surfaces = surfaces,
                contentDescription = strings.openArtist,
                onClick = { showArtistPicker = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(
                        start = gutter,
                        top = DerbisPlayerDesign.MinimumTouchTarget + DerbisPlayerDesign.SpaceLg
                    )
                    .zIndex(24f)
            )
        }

        if (showActions && track != null) {
            val sheetActions = playerSheetActions(
                track = track,
                state = state,
                strings = strings,
                onAddToPlaylist = { onOpenPlaylistDialog?.invoke(track) },
                onStartRadio = {
                    viewModel.startSongRadio()
                    viewModel.openQueue()
                    hapticFeedback.perform(DerbisHapticAction.Confirm)
                },
                onShare = {
                    showActions = false
                    showTrackStoryShare = true
                },
                onOpenJam = {
                    showActions = false
                    viewModel.openJam()
                },
                onSleepTimer = viewModel::openSleepTimer,
                onSpeed = viewModel::cycleSpeed,
                onNormalization = viewModel::toggleAudioNormalization,
                onAudioOutput = {
                    showActions = false
                    showAudioRouteCenter = true
                },
                onAudioSettings = viewModel::openAudioQualityPanel,
                onTechnicalAudioInfo = {
                    showActions = false
                    showTechnicalAudioInfo = true
                },
                onTrackCredits = {
                    showActions = false
                    showTrackCredits = true
                },
                onRefreshMotionArtwork = {
                    showActions = false
                    viewModel.refreshCurrentMotionArtwork()
                    hapticFeedback.perform(DerbisHapticAction.Confirm)
                },
                onAmbient = viewModel::openAmbient,
                onOpenArtist = {
                    if (playerArtistHits.size > 1) {
                        openArtistPickerAfterActions = true
                    } else {
                        viewModel.openArtist(track)
                    }
                }
            )
            PlayerActionsSheet(
                track = track,
                artworkUrl = artworkUrl,
                surfaces = surfaces,
                animated = animated,
                actions = sheetActions,
                onDismiss = {
                    showActions = false
                    if (openArtistPickerAfterActions) {
                        openArtistPickerAfterActions = false
                        showArtistPicker = true
                    }
                },
                engagementContent = if (engagementContent != null && !liveRadio) {
                    { engagementContent(track) }
                } else {
                    null
                },
                discoverContent = if (similarSongsContent != null && !liveRadio) {
                    { similarSongsContent(track) }
                } else {
                    null
                }
            )
        }

        if (showArtistPicker && track != null) {
            PlayerArtistPickerSheet(
                track = track,
                artists = playerArtistHits,
                title = strings.openArtist,
                surfaces = surfaces,
                animated = animated,
                onArtistClick = { artistIndex ->
                    showArtistPicker = false
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onDismiss = { showArtistPicker = false }
            )
        }

        if (showTechnicalAudioInfo && track != null) {
            TechnicalAudioInfoSheet(
                track = track,
                audioSettings = state.audioSettings,
                audioNormalization = state.audioNormalization,
                onDismiss = { showTechnicalAudioInfo = false }
            )
        }

        if (showTrackCredits && track != null && !track.isLiveRadio()) {
            TrackCreditsSheet(
                track = track,
                artworkUrl = artworkUrl,
                artists = playerArtistHits,
                animated = animated,
                loadCredits = viewModel::trackCredits,
                onOpenArtist = { artistIndex ->
                    showTrackCredits = false
                    playerArtistHits.getOrNull(artistIndex)?.let(viewModel::openArtist)
                        ?: viewModel.openArtist(track, artistIndex)
                },
                onDismiss = { showTrackCredits = false }
            )
        }

        if (showAudioRouteCenter && track != null && !state.isVideoMode) {
            AudioRouteCenterSheet(
                surfaces = surfaces,
                animated = animated,
                onDismiss = { showAudioRouteCenter = false }
            )
        }

        if (showTrackStoryShare && track != null) {
            TrackStoryShareSheet(
                track = track,
                currentPositionMs = state.positionMs,
                durationMs = state.durationMs,
                onOpenJam = {
                    showTrackStoryShare = false
                    viewModel.openJam()
                },
                onDismiss = { showTrackStoryShare = false }
            )
        }

        val removalTrackId = downloadRemovalTrackId
        if (removalTrackId != null && (track?.id != removalTrackId || removalTrackId !in state.downloadedTrackIds)) {
            SideEffect { downloadRemovalTrackId = null }
        }
        if (removalTrackId != null && track?.id == removalTrackId && removalTrackId in state.downloadedTrackIds) {
            PlayerRemoveDownloadDialog(
                title = strings.removeDownloadPrompt,
                cancelLabel = strings.cancel,
                removeLabel = strings.remove,
                onDismiss = { downloadRemovalTrackId = null },
                onConfirm = {
                    downloadRemovalTrackId = null
                    viewModel.deleteTrackDownload(removalTrackId)
                }
            )
        }

        if (showDeck && track != null && !state.isVideoMode) {
            PlayerDeckSheet(
                track = track,
                artworkUrl = artworkUrl,
                selected = selectedVisualMode,
                surfaces = surfaces,
                accent = heroTone,
                animated = animated,
                onSelect = { mode ->
                    if (mode != selectedVisualMode) {
                        viewModel.setPlayerVisualMode(mode)
                        hapticFeedback.perform(DerbisHapticAction.Confirm)
                    }
                },
                onDismiss = { showDeck = false }
            )
        }

        playlistDialogContent?.invoke()
    }
}

@Composable
private fun PlayerSourceEyebrow(
    label: String,
    source: String,
    surfaces: PlayerSurfaceTokens
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = surfaces.contentFaint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = source,
            color = surfaces.contentMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerLiveRadioMetadata(
    track: Track,
    station: RadioStation?,
    animated: Boolean,
    nowPlaying: String,
    liveLabel: String,
    details: String,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean
) {
    val context = LocalContext.current
    val factory = remember(context.applicationContext) { LiveRadioViewModel.factory(context.applicationContext) }
    val radioViewModel: LiveRadioViewModel = composeViewModel(key = "derbis-live-radio", factory = factory)
    val radioState by radioViewModel.state.collectAsStateWithLifecycle()
    val strings = LocalDerbisStrings.current
    LaunchedEffect(station?.uuid) { radioViewModel.refreshFavorites() }
    val titleSize = if (compact) 22.sp else 26.sp
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = track.title,
                color = surfaces.content,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (station != null) {
                val favorite = radioState.favorites.any { it.uuid.equals(station.uuid, ignoreCase = true) }
                PlayerFavoriteButton(
                    trackId = track.id,
                    isFavorite = favorite,
                    surfaces = surfaces,
                    label = if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                    animated = animated,
                    onToggle = { radioViewModel.toggleFavorite(station) }
                )
            }
        }
        Text(
            text = nowPlaying.ifBlank { liveLabel },
            color = if (nowPlaying.isBlank()) surfaces.activeContent else surfaces.contentMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (details.isNotBlank()) {
            Text(
                text = details,
                color = surfaces.contentFaint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerLiveRadioTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    strings: DerbisStrings,
    onStop: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) DerbisPlayerDesign.TransportHeightCompact else DerbisPlayerDesign.TransportHeight),
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.TransportGap)
    ) {
        PlayerSegmentButton(
            position = DerbisSegment.Leading,
            weight = 1f,
            container = surfaces.control,
            innerCorner = DerbisPlayerDesign.TransportInnerCorner,
            contentDescription = strings.close,
            animated = animated,
            outline = if (surfaces.amoled) surfaces.outline else Color.Transparent,
            onClick = onStop
        ) {
            PlayerIcon(Icons.Rounded.Stop, surfaces.content, Modifier.size(DerbisPlayerDesign.TransportGlyph))
        }
        PlayerSegmentButton(
            position = DerbisSegment.Trailing,
            weight = 2f,
            container = surfaces.hero,
            innerCorner = DerbisPlayerDesign.TransportInnerCorner,
            contentDescription = if (isPlaying) strings.pause else strings.play,
            animated = animated,
            enabled = !isResolving,
            haptic = DerbisHapticAction.Transport,
            onClick = onToggle
        ) {
            PlayerIcon(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                tint = surfaces.heroContent,
                modifier = Modifier.size(DerbisPlayerDesign.TransportPlayGlyph)
            )
        }
    }
}

private fun playerDockActions(
    strings: DerbisStrings,
    visualMode: PlayerVisualMode,
    presentedMode: PlayerVisualMode,
    showLyrics: Boolean,
    videoMode: Boolean,
    isDownloaded: Boolean,
    isExporting: Boolean,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onOpenDeck: () -> Unit,
    onDownload: () -> Unit
): List<PlayerDockAction> {
    return buildList {
        add(
            PlayerDockAction(
                key = "lyrics",
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                active = showLyrics,
                onClick = onLyrics
            )
        )
        add(
            PlayerDockAction(
                key = "queue",
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                onClick = onQueue
            )
        )
        if (!videoMode) {
            add(
                PlayerDockAction(
                    key = "visual-mode",
                    icon = visualModeIcon(presentedMode),
                    label = if (DerbisSkinManagesLayout) DerbisSkinLabel else visualModeLabel(strings),
                    stateDescription = if (DerbisSkinManagesLayout) DerbisSkinLabel else visualModeStateDescription(visualMode, strings),
                    active = presentedMode != PlayerVisualMode.Artwork,
                    toggle = false,
                    onClick = onOpenDeck
                )
            )
        }
        add(
            PlayerDockAction(
                key = "download",
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = when {
                    isExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                },
                active = isDownloaded || isExporting,
                busy = isExporting,
                onClick = onDownload
            )
        )
    }
}

@Composable
private fun PlayerRemoveDownloadDialog(
    title: String,
    cancelLabel: String,
    removeLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text(title, textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(removeLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}

private fun canvasBackdropPaletteMatches(
    palette: MotionBackdropPalette?,
    motionEnabled: Boolean,
    motionBackdropVisible: Boolean,
    activeIdentity: String?
): Boolean {
    val presentationActive = motionEnabled && motionBackdropVisible
    val identityMatches = activeIdentity != null && palette?.identityKey == activeIdentity
    return presentationActive && identityMatches
}

private fun Track.isLocalMotionArtworkTrack(): Boolean {
    if (source.equals("Offline", ignoreCase = true)) return true
    val cleanStreamUrl = streamUrl.trim()
    return cleanStreamUrl.startsWith("content://", ignoreCase = true) ||
        cleanStreamUrl.startsWith("file://", ignoreCase = true)
}

private fun playerSheetActions(
    track: Track,
    state: DerbisUiState,
    strings: DerbisStrings,
    onAddToPlaylist: () -> Unit,
    onStartRadio: () -> Unit,
    onShare: () -> Unit,
    onOpenJam: () -> Unit,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onNormalization: () -> Unit,
    onAudioOutput: () -> Unit,
    onAudioSettings: () -> Unit,
    onTechnicalAudioInfo: () -> Unit,
    onTrackCredits: () -> Unit,
    onRefreshMotionArtwork: () -> Unit,
    onAmbient: () -> Unit,
    onOpenArtist: () -> Unit
): List<PlayerSheetAction> {
    val sleepActive = state.sleepTimerMinutes > 0 || state.sleepTimerEndOfTrack
    val sleepAction = PlayerSheetAction(
        key = "sleep",
        icon = Icons.Rounded.Bedtime,
        label = strings.sleepTimer,
        value = when {
            state.sleepTimerEndOfTrack -> strings.sleepTimerEndOfTrack
            state.sleepTimerMinutes > 0 -> strings.formatSleepTimerMinutes(state.sleepTimerMinutes)
            else -> null
        },
        active = sleepActive,
        onClick = onSleepTimer
    )
    val audioOutputAction = if (
        playerAudioRoutePlacement(
            isVideoMode = state.isVideoMode,
            hasTrack = true
        ) == PlayerAudioRoutePlacement.Overflow
    ) {
        PlayerSheetAction(
            key = "audio-output",
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = strings.systemPlayerCopy().outputTitle,
            onClick = onAudioOutput
        )
    } else {
        null
    }
    val audioAction = PlayerSheetAction(
        key = "audio",
        icon = Icons.Rounded.Tune,
        label = strings.audioQuality,
        onClick = onAudioSettings
    )
    val technicalAudioAction = PlayerSheetAction(
        key = "technical-audio-info",
        icon = Icons.Rounded.Info,
        label = strings.technicalAudioInfoCopy().title,
        onClick = onTechnicalAudioInfo
    )
    val ambientAction = PlayerSheetAction(
        key = "ambient",
        icon = Icons.Rounded.Nightlight,
        label = strings.ambientMode,
        onClick = onAmbient
    )
    val refreshMotionArtworkAction = if (
        state.animationsEnabled &&
        state.motionArtworkEnabled &&
        !state.isVideoMode &&
        !track.isLocalMotionArtworkTrack()
    ) {
        PlayerSheetAction(
            key = "refresh-motion-artwork",
            icon = Icons.Rounded.Refresh,
            label = strings.motionArtwork,
            value = strings.exploreSamplesRetry,
            onClick = onRefreshMotionArtwork
        )
    } else {
        null
    }
    if (track.isLiveRadio()) {
        return listOfNotNull(
            sleepAction,
            audioOutputAction,
            audioAction,
            technicalAudioAction,
            ambientAction
        )
    }

    val canStartRadio = !state.jam.isActive || state.jam.isHost
    return listOfNotNull(
        PlayerSheetAction(
            key = "playlist",
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            label = strings.addToPlaylist,
            onClick = onAddToPlaylist
        ),
        PlayerSheetAction(
            key = "radio",
            icon = Icons.Rounded.Radio,
            label = strings.startRadio,
            enabled = canStartRadio,
            onClick = onStartRadio
        ),
        PlayerSheetAction(
            key = "share",
            icon = Icons.Rounded.Share,
            label = strings.share,
            onClick = onShare
        ),
        PlayerSheetAction(
            key = "listen_together",
            icon = ModernListenTogetherVector,
            label = strings.jamListenTogether,
            onClick = onOpenJam
        ),
        sleepAction,
        PlayerSheetAction(
            key = "speed",
            icon = Icons.Rounded.Speed,
            label = strings.tempo,
            value = "${formatPlaybackSpeed(state.playbackSpeed)}x",
            active = state.playbackSpeed != 1f,
            keepsSheetOpen = true,
            onClick = onSpeed
        ),
        PlayerSheetAction(
            key = "normalization",
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = strings.normalizationShort,
            active = state.audioNormalization,
            toggle = true,
            keepsSheetOpen = true,
            onClick = onNormalization
        ),
        audioOutputAction,
        audioAction,
        PlayerSheetAction(
            key = "track-credits",
            icon = Icons.Rounded.Groups,
            label = strings.trackCreditsCopy().title,
            onClick = onTrackCredits
        ),
        technicalAudioAction,
        refreshMotionArtworkAction,
        ambientAction,
        PlayerSheetAction(
            key = "artist",
            icon = Icons.Rounded.Person,
            label = strings.openArtist,
            onClick = onOpenArtist
        )
    )
}

internal fun formatPlaybackSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')

private fun shareTrack(context: Context, track: Track, strings: DerbisStrings) {
    val videoId = PlaybackSourceIdentity.sourceVideoId(track)
    val text = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(" — ").append(track.artist)
        if (videoId.isNotBlank()) {
            append("\nhttps://derbis-web.vercel.app/t/").append(videoId)
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, strings.shareSong))
}

internal fun visualModeLabel(strings: DerbisStrings): String =
    strings.playerVisualMode

internal fun visualModeStateDescription(
    mode: PlayerVisualMode,
    strings: DerbisStrings
): String = when (mode) {
    PlayerVisualMode.Artwork -> strings.playerVisualModeArtwork
    PlayerVisualMode.CanvasCard -> strings.playerVisualModeCanvasCard
    PlayerVisualMode.CanvasImmersive -> strings.playerVisualModeCanvasImmersive
    PlayerVisualMode.AppleMusic13 -> strings.playerDeckAppleMusic13
    PlayerVisualMode.TidalMusic -> strings.playerDeckTidalMusic
    PlayerVisualMode.Pulse -> strings.playerDeckPulse
    PlayerVisualMode.Halo -> strings.playerDeckHalo
    PlayerVisualMode.Folio -> strings.playerDeckFolio
    PlayerVisualMode.Flow -> strings.playerDeckFlow
}

internal fun visualModeIcon(mode: PlayerVisualMode): ImageVector = when (mode) {
    PlayerVisualMode.Artwork -> Icons.Rounded.Image
    PlayerVisualMode.CanvasCard -> Icons.Rounded.AutoAwesome
    PlayerVisualMode.CanvasImmersive -> Icons.Rounded.BlurOn
    PlayerVisualMode.AppleMusic13 -> Icons.Rounded.Album
    PlayerVisualMode.TidalMusic -> Icons.Rounded.GraphicEq
    PlayerVisualMode.Pulse -> Icons.Rounded.GraphicEq
    PlayerVisualMode.Halo -> Icons.Rounded.Album
    PlayerVisualMode.Folio -> Icons.Rounded.Newspaper
    PlayerVisualMode.Flow -> Icons.Rounded.Waves
}

@Composable
private fun PlayerGestureFeedbackPill(text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = text.isNotBlank(),
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(110)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.74f),
            border = BorderStroke(DerbisPlayerDesign.Hairline, Color.White.copy(alpha = 0.16f)),
            shape = CircleShape
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}
