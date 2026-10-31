package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasuda.derbis.data.KanjiFurigana
import com.yasuda.derbis.ui.lyrics.KaraokeFuriganaWordTimedText
import com.yasuda.derbis.player.PlaybackService
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.LyricWord
import com.yasuda.derbis.feature.motion.DerbisCanvasCatalogProvider
import com.yasuda.derbis.feature.motion.MotionArtwork
import com.yasuda.derbis.ui.artwork.LivingArtworkColors
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.KaraokeWordTimedText
import com.yasuda.derbis.ui.InstantArtworkPlaceholder
import com.yasuda.derbis.ui.MotionArtworkLayer
import com.yasuda.derbis.ui.MotionArtworkPresentation
import com.yasuda.derbis.ui.preferredPlayerArtworkUrl
import com.yasuda.derbis.ui.artwork.SeamlessArtworkImage
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.AppleMusicDesign
import com.yasuda.derbis.ui.theme.AppleMusicShapes
import com.yasuda.derbis.ui.theme.AppleMusic13SfProFallbackFont
import com.yasuda.derbis.ui.theme.AppleMusic13Typography
import com.yasuda.derbis.ui.theme.AppleMusicVibrant
import com.yasuda.derbis.ui.theme.DerbisIsAppleMusic
import com.yasuda.derbis.ui.theme.DerbisIsPrism
import com.yasuda.derbis.ui.theme.DerbisPalette
import com.yasuda.derbis.ui.theme.appleMusicColorScheme
import com.yasuda.derbis.ui.theme.appleMusicPalette

private const val ImmersiveCanvasMinAspect = 1.4f
private val AppleMusic13ArtworkCorner = 24.dp

internal val LocalAppleMusicPlayerPalette = staticCompositionLocalOf<DerbisPalette?> { null }

internal val PlayerAppleMusicStyle: Boolean
    @Composable @ReadOnlyComposable get() = DerbisIsAppleMusic || LocalAppleMusicPlayerPalette.current != null

internal val PlayerPrismStyle: Boolean
    @Composable @ReadOnlyComposable get() = DerbisIsPrism || LocalAppleMusicPlayerPalette.current != null

@Composable
internal fun AppleMusicPlayerStyleScope(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = remember(systemDark) { appleMusicPalette(systemDark) }
    val colorScheme = remember(palette) { appleMusicColorScheme(palette) }
    CompositionLocalProvider(LocalAppleMusicPlayerPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = AppleMusic13Typography, shapes = AppleMusicShapes, content = content)
    }
}

@Immutable
internal class AppleMusicCanvas(
    val artwork: MotionArtwork?,
    val enabled: Boolean,
    val quality: DerbisCanvasQuality,
    val livingArtwork: LivingArtworkColors?
) {
    val immersive: Boolean
        get() {
            val current = artwork ?: return false
            val width = current.width
            val height = current.height
            if (width == null || height == null) return enabled && current.provider == DerbisCanvasCatalogProvider.PROVIDER_ID
            return enabled && height > width
        }
}

internal fun appleMusicCanvasImmersive(canvas: AppleMusicCanvas?, width: Dp, height: Dp, video: Boolean): Boolean =
    height > width * ImmersiveCanvasMinAspect && !video && canvas?.immersive == true

@Composable
internal fun AppleMusicPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp, playing: Boolean,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier,
    canvas: AppleMusicCanvas? = null, video: Boolean = false,
    activeLyric: LyricLine? = null,
    lyricPositionMs: Long = 0L
) {
    // The Apple Music 13 deck deliberately lets the adaptive artwork ambience show through,
    // then protects controls with a neutral, translucent lower scrim.
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.36f to Color.Transparent,
                    0.68f to Color.Black.copy(alpha = 0.22f),
                    1f to Color.Black.copy(alpha = 0.48f)
                )
            )
    ) {
        val landscape = maxWidth > maxHeight
        // The portrait deck owns a full-bleed cover treatment below. A verified provider loop
        // appears there only when the listener has explicitly enabled Animated artwork.
        val immersive = false
        val artwork: @Composable (Dp) -> Unit = { size ->
            if (video) AppleMusicPlayerVideo(track, slots, size) else AppleMusicPlayerArtwork(track, slots, size, { 0f }, showStage = true)
        }
        if (landscape) {
            AppleMusicLandscapeDeck(
                track = track,
                slots = slots,
                artworkSize = minOf(maxHeight - 34.dp, artworkCap),
                artwork = artwork,
                metadata = metadata,
                transport = transport,
                activeLyric = activeLyric,
                lyricPositionMs = lyricPositionMs
            )
        } else {
            AppleMusicPortraitDeck(
                track = track,
                slots = slots,
                artworkCap = if (video) maxWidth else artworkCap,
                immersive = immersive,
                video = video,
                artwork = artwork,
                metadata = metadata,
                transport = transport,
                activeLyric = activeLyric,
                lyricPositionMs = lyricPositionMs,
                canvas = canvas,
                playing = playing
            )
        }
    }
}

@Composable
private fun AppleMusicPlayerArtwork(
    track: Track, slots: PlayerDeckSlots, size: Dp, canvasAlpha: () -> Float, showStage: Boolean
) {
    // Artwork is a stable hero in this deck.  Toggling play/pause changes media state, not
    // its geometry, so paused tracks retain exactly the same card size and position.
    val artElevation by animateDpAsState(
        targetValue = 18.dp,
        animationSpec = if (LocalAnimationsEnabled.current) spring(dampingRatio = 0.78f, stiffness = 420f) else spring(stiffness = 100_000f),
        label = "apple-player-artwork-elevation"
    )
    Box(
        Modifier.size(size)
            .graphicsLayer { alpha = 1f - canvasAlpha() }
            .shadow(artElevation, RoundedCornerShape(AppleMusic13ArtworkCorner), clip = false,
                ambientColor = Color.Black.copy(alpha = 0.38f), spotColor = Color.Black.copy(alpha = 0.52f))
    ) {
        if (showStage) slots.stage(track, Modifier.fillMaxSize(), AppleMusic13ArtworkCorner)
    }
}

@Composable
private fun AppleMusicPlayerVideo(track: Track, slots: PlayerDeckSlots, width: Dp) {
    Box(
        Modifier.width(width).aspectRatio(16f / 9f)
            .shadow(15.dp, RoundedCornerShape(AppleMusicDesign.cornerL), clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
    ) {
        slots.stage(track, Modifier.fillMaxSize(), AppleMusicDesign.cornerL)
    }
}

@Composable
private fun AppleMusicGrabber() {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(AppleMusicVibrant.secondary))
    }
}

@Composable
private fun AppleMusicPortraitDeck(
    track: Track,
    slots: PlayerDeckSlots,
    artworkCap: Dp,
    immersive: Boolean,
    video: Boolean,
    artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    activeLyric: LyricLine? = null,
    lyricPositionMs: Long = 0L,
    canvas: AppleMusicCanvas? = null,
    playing: Boolean = false
) {
    if (video) {
        AppleMusicVideoPortraitDeck(track, slots, artworkCap, immersive, artwork, metadata, transport, activeLyric, lyricPositionMs)
    } else {
        AppleMusic13ReferencePortraitDeck(track, slots, metadata, transport, activeLyric, lyricPositionMs, canvas, playing)
    }
}

/**
 * Derbis' Apple Music 13-inspired portrait player. Its cover is a full-bleed scene:
 * the image fills the upper stage without distortion and fades into the deck's own controls.
 * This is an original Derbis treatment, not a reference-app layout.
 */
@Composable
private fun AppleMusic13ReferencePortraitDeck(
    track: Track,
    slots: PlayerDeckSlots,
    metadata: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    activeLyric: LyricLine?,
    lyricPositionMs: Long,
    canvas: AppleMusicCanvas?,
    playing: Boolean
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Swap: Apple Music 13 now owns the stretched, full-bleed cover presentation.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(AppleMusic13StretchedArtworkHeight)
        ) {
            AppleMusic13StretchedArtwork(track, canvas, playing)
        }

        // Preserve artwork gestures without putting an invisible surface over the controls.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(AppleMusic13GestureStageHeight)
                .statusBarsPadding()
        ) {
            slots.gestures(track, Modifier.matchParentSize())
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
        ) {
            slots.header()
        }

        val inset = if (maxWidth < 390.dp) 20.dp else 28.dp
        // Keep the information and transport in one substantial lower composition while the
        // artwork recedes behind it. The old bounded sleeve is now Velvet's presentation.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = inset)
        ) {
            Box(Modifier.fillMaxWidth().clipToBounds()) { metadata() }
            Box(Modifier.padding(top = 4.dp)) { slots.engagement(track) }
            AppleMusic13InlineLyrics(
                line = activeLyric,
                positionMs = lyricPositionMs,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
            )
            Box(Modifier.padding(horizontal = 2.dp)) {
                slots.progress(PlayerProgressStyle.Classic)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.padding(horizontal = 4.dp)) { transport() }
            }
            PlayerOutputVolume(
                contentColor = Color.White,
                mutedColor = Color.White.copy(alpha = 0.64f),
                activeColor = Color.White,
                trackColor = Color.White.copy(alpha = 0.22f),
                condensed = true,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            AppleMusic13ActionDock(slots.dockActions(track))
            Spacer(Modifier.height(AppleMusicDesign.playerActionBottom + 4.dp))
        }
    }
}

private const val AppleMusic13StretchedArtworkHeight = 0.74f
private const val AppleMusic13GestureStageHeight = 0.58f
private val AppleMusic13ArtworkFade = Brush.verticalGradient(
    0.00f to Color.Transparent,
    0.48f to Color.Transparent,
    0.76f to Color.Black.copy(alpha = 0.36f),
    1.00f to Color.Black
)

@Composable
private fun AppleMusic13StretchedArtwork(
    track: Track,
    canvas: AppleMusicCanvas?,
    playing: Boolean
) {
    val artworkUrl = preferredPlayerArtworkUrl(track)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Only a user-enabled, provider-supplied loop can replace this static cover. There is no
        // generated colour treatment or ink overlay when Canvas is on or when it falls back.
        MotionArtworkLayer(
            artwork = canvas?.artwork,
            enabled = canvas?.enabled == true,
            isPlaying = playing,
            cornerRadius = 0.dp,
            presentation = MotionArtworkPresentation.Immersive,
            quality = canvas?.quality ?: DerbisCanvasQuality.Auto,
            staticArtworkMotionEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) {
            SeamlessArtworkImage(
                url = artworkUrl,
                modifier = Modifier.fillMaxSize(),
                fallback = { InstantArtworkPlaceholder(modifier = Modifier.fillMaxSize()) }
            )
        }
        Box(Modifier.fillMaxSize().background(AppleMusic13ArtworkFade))
    }
}

/** A balanced Derbis utility row with independent, generously sized touch targets. */
@Composable
private fun AppleMusic13ActionDock(actions: List<PlayerDockAction>) {
    val visibleActions = actions.take(4)
    if (visibleActions.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleActions.forEach { action ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (action.active) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(enabled = action.enabled && !action.busy, role = Role.Button, onClick = action.onClick)
                    .semantics {
                        contentDescription = action.label
                        action.stateDescription?.let { stateDescription = it }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action.busy) {
                    DerbisLoadingIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = when {
                            !action.enabled -> Color.White.copy(alpha = 0.36f)
                            action.active -> Color.White
                            else -> Color.White.copy(alpha = 0.88f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleMusicVideoPortraitDeck(
    track: Track,
    slots: PlayerDeckSlots,
    artworkCap: Dp,
    immersive: Boolean,
    artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    activeLyric: LyricLine?,
    lyricPositionMs: Long
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppleMusicGrabber()
        Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = AppleMusicDesign.playerArtworkInset, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) { artwork(minOf(maxWidth, maxHeight, artworkCap)) }
        Box(Modifier.padding(horizontal = AppleMusicDesign.playerMetadataInset - 16.dp).clipToBounds()) { metadata() }
        if (!immersive) Box(Modifier.padding(horizontal = AppleMusicDesign.playerMetadataInset - 16.dp)) { slots.engagement(track) }
        AppleMusic13InlineLyrics(
            line = activeLyric,
            positionMs = lyricPositionMs,
            modifier = Modifier.padding(horizontal = AppleMusicDesign.playerSeekbarInset, vertical = 4.dp)
        )
        Box(Modifier.padding(horizontal = AppleMusicDesign.playerSeekbarInset)) { slots.progress(PlayerProgressStyle.Classic) }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.padding(horizontal = 16.dp)) { transport() }
        }
        Box(Modifier.fillMaxWidth().padding(bottom = AppleMusicDesign.playerActionBottom)) { AppleMusicDockRow(slots.dockActions(track)) }
    }
}

internal fun appleMusic13UsesWordTiming(line: LyricLine?): Boolean =
    line?.words?.any { word -> word.text.isNotBlank() && word.endMs > word.startMs } == true

internal fun resolveOrSynthesizeLineWords(line: LyricLine): List<LyricWord> {
    if (appleMusic13UsesWordTiming(line)) return line.words
    val rawWords = line.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawWords.isEmpty()) return emptyList()
    val lineDuration = (line.endMs - line.startMs).takeIf { it > 400L } ?: 3_500L
    val totalChars = rawWords.sumOf { it.length.coerceAtLeast(1) }
    var currentStart = line.startMs
    return rawWords.map { wordText ->
        val weight = wordText.length.toFloat() / totalChars.toFloat()
        val duration = (lineDuration * weight).toLong().coerceAtLeast(120L)
        val wordEnd = (currentStart + duration).coerceAtMost(line.startMs + lineDuration)
        val word = LyricWord(startMs = currentStart, endMs = wordEnd, text = wordText)
        currentStart = wordEnd
        word
    }
}

@Composable
private fun AppleMusic13InlineLyrics(line: LyricLine?, positionMs: Long, modifier: Modifier = Modifier) {
    val activeLine = line?.takeIf { it.text.isNotBlank() }
    AnimatedContent(
        targetState = activeLine,
        transitionSpec = {
            // Swiping up transition between lines:
            // Outgoing line slides up and fades out
            // Incoming line slides up from below and fades in
            (slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { fullHeight -> (fullHeight * 0.85f).toInt() }
            ) + fadeIn(animationSpec = tween(320)))
            .togetherWith(
                slideOutVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    targetOffsetY = { fullHeight -> -(fullHeight * 0.85f).toInt() }
                ) + fadeOut(animationSpec = tween(240))
            )
        },
        modifier = modifier.fillMaxWidth(),
        label = "apple-music-inline-lyrics-swipe-up"
    ) { currentLine ->
        if (currentLine != null) {
            val words = remember(currentLine) { resolveOrSynthesizeLineWords(currentLine) }
            val hasKanji = remember(currentLine.text) { KanjiFurigana.containsKanji(currentLine.text) }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (hasKanji) {
                    KaraokeFuriganaWordTimedText(
                        lineText = currentLine.text,
                        words = words,
                        positionProvider = { positionMs },
                        isActive = true,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Start,
                        inactiveColor = Color.White.copy(alpha = 0.45f),
                        completedColor = Color.White,
                        activeColor = Color.White,
                        pendingColor = Color.White.copy(alpha = 0.45f),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = AppleMusic13SfProFallbackFont,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.28f),
                            offset = Offset(0f, 1f),
                            blurRadius = 6f
                        )
                    )
                } else {
                    KaraokeWordTimedText(
                        lineText = currentLine.text,
                        words = words,
                        positionProvider = { positionMs },
                        isActive = true,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Start,
                        inactiveColor = Color.White.copy(alpha = 0.45f),
                        completedColor = Color.White,
                        activeColor = Color.White,
                        pendingColor = Color.White.copy(alpha = 0.45f),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = AppleMusic13SfProFallbackFont,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.28f),
                            offset = Offset(0f, 1f),
                            blurRadius = 6f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleMusicLandscapeDeck(
    track: Track, slots: PlayerDeckSlots, artworkSize: Dp, artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit,
    activeLyric: LyricLine? = null, lyricPositionMs: Long = 0L
) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().displayCutoutPadding()) {
        AppleMusicGrabber()
        Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
                artwork(artworkSize)
            }
            Spacer(Modifier.width(24.dp))
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
                metadata()
                AppleMusic13InlineLyrics(
                    line = activeLyric,
                    positionMs = lyricPositionMs,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                slots.progress(PlayerProgressStyle.Classic)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                AppleMusicDockRow(slots.dockActions(track))
            }
        }
    }
}


@Composable
internal fun AppleMusicTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
        AppleMusicModeToggle(Icons.Rounded.Shuffle, labels.shuffle, shuffle, null, onShuffle)
        val compactWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 390
        val gap = if (compactWidth) AppleMusicDesign.playerTransportGap / 2 else AppleMusicDesign.playerTransportGap
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon + AppleMusicDesign.playerControlPadding * 2)) {
                Icon(Icons.Rounded.FastRewind, labels.previous, tint = AppleMusicVibrant.primary, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon))
            }
            Box(
                Modifier.size(AppleMusicDesign.playerPlayIcon + AppleMusicDesign.playerControlPadding * 2).clip(CircleShape).clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = if (playing) labels.pause else labels.play },
                contentAlignment = Alignment.Center
            ) {
                if (resolving) DerbisLoadingIndicator(color = AppleMusicVibrant.primary, modifier = Modifier.size(30.dp))
                else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = AppleMusicVibrant.primary, modifier = Modifier.size(AppleMusicDesign.playerPlayIcon + 10.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon + AppleMusicDesign.playerControlPadding * 2)) {
                Icon(Icons.Rounded.FastForward, labels.next, tint = AppleMusicVibrant.primary, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon))
            }
        }
        AppleMusicModeToggle(
            if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off,
            when (repeat) {
                RepeatMode.All -> androidx.compose.ui.res.stringResource(com.yasuda.derbis.R.string.notification_repeat_all)
                RepeatMode.One -> androidx.compose.ui.res.stringResource(com.yasuda.derbis.R.string.notification_repeat_one)
                else -> null
            },
            onRepeat
        )
    }
}

@Composable
private fun AppleMusicModeToggle(icon: ImageVector, label: String, active: Boolean, stateLabel: String?, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(AppleMusicDesign.cornerM))
            .toggleable(value = active, role = Role.Switch, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                if (stateLabel != null) stateDescription = stateLabel
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(AppleMusicDesign.cornerM))
                .background(if (active) AppleMusicVibrant.tertiary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) AppleMusicVibrant.primary else AppleMusicVibrant.secondary, modifier = Modifier.size(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleMusicSeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val crossfade by PlaybackService.crossfadeProgressFlow.collectAsStateWithLifecycle()
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalDerbisStrings.current.gestureActionSeek
    val barHeight by animateDpAsState(if (dragging != null) 11.dp else 7.dp, label = "apple-seekbar-height")
    SideEffect { sliderState.value = dragging ?: fraction }

    val crossfadeAlpha by animateFloatAsState(
        targetValue = if (crossfade.active) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "apple-crossfade-alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "apple-crossfade-rgb")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "apple-crossfade-hue"
    )

    val rgbBrush = remember(hue) {
        val c1 = Color.hsv(hue % 360f, 0.92f, 1f)
        val c2 = Color.hsv((hue + 72f) % 360f, 0.92f, 1f)
        val c3 = Color.hsv((hue + 144f) % 360f, 0.92f, 1f)
        val c4 = Color.hsv((hue + 216f) % 360f, 0.92f, 1f)
        val c5 = Color.hsv((hue + 288f) % 360f, 0.92f, 1f)
        Brush.horizontalGradient(listOf(c1, c2, c3, c4, c5))
    }

    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).semantics { contentDescription = seekLabel },
            thumb = { Spacer(Modifier.size(1.dp)) },
            track = { state ->
                val shape = RoundedCornerShape(barHeight / 2)
                val activeFraction = state.value.coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(barHeight)) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    // Inactive track with buffered background
                    Box(Modifier.fillMaxWidth().fillMaxHeight().clip(shape).background(AppleMusicVibrant.tertiary)) {
                        Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(AppleMusicVibrant.quaternary))
                    }

                    // Active progress bar: RGB applied directly and only to the progress bar track itself
                    if (activeFraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth(activeFraction)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary)
                        )
                        // RGB progress overlay applied strictly within the progress bar bounds
                        if (crossfadeAlpha > 0.01f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(activeFraction)
                                    .fillMaxHeight()
                                    .clip(shape)
                                    .graphicsLayer { alpha = crossfadeAlpha }
                                    .background(rgbBrush)
                            )
                        }
                    }
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val timeStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp)
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = timeStyle, color = if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary)
            AppleMusicAudioBadge(Modifier.weight(1f), isCrossfading = crossfade.active)
            Text(
                if (durationMs > 0L) "-" + formatSeekbarMillis((durationMs - shown).coerceAtLeast(0L)) else formatSeekbarMillis(0L),
                style = timeStyle, color = if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary
            )
        }
    }
}

@Composable
private fun AppleMusicAudioBadge(modifier: Modifier = Modifier, isCrossfading: Boolean = false) {
    val spec by rememberPlayerAudioSpec()
    val label = remember(spec) { appleMusicAudioBadgeLabel(spec) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val displayLabel = if (label.isNotEmpty()) label else "73 kbps · 48 kHz"
        Crossfade(
            targetState = isCrossfading,
            animationSpec = tween(durationMillis = 400),
            label = "apple-music-badge-crossfade-text"
        ) { crossfading ->
            if (crossfading) {
                Text(
                    text = "Crossfading",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = AppleMusicVibrant.secondary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    SoundWaveIcon(
                        tint = AppleMusicVibrant.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        displayLabel,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                        color = AppleMusicVibrant.secondary
                    )
                }
            }
        }
    }
}

@Composable
internal fun SoundWaveIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppleMusicVibrant.secondary
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = size.width * (1.6f / 24f)
        val bars = listOf(
            0.175f to (0.425f to 0.575f),
            0.283f to (0.377f to 0.623f),
            0.392f to (0.302f to 0.698f),
            0.500f to (0.200f to 0.800f),
            0.608f to (0.302f to 0.698f),
            0.717f to (0.377f to 0.623f),
            0.825f to (0.425f to 0.575f)
        )
        for ((xf, yPair) in bars) {
            drawLine(
                color = tint,
                start = Offset(xf * size.width, yPair.first * size.height),
                end = Offset(xf * size.width, yPair.second * size.height),
                strokeWidth = strokeW,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * The single label the Apple Music skin puts on the now-playing badge.
 *
 * It reads the same [PlayerAudioSpec.quality] the other skins badge, so both agree: a lossless
 * source reads Lossless, a hi-res one or a 320 kbps upgrade reads Hi-Fi, and only the lossy
 * streams below that name their codec with the bitrate that backs it up.
 */
internal fun appleMusicAudioBadgeLabel(spec: PlayerAudioSpec): String {
    val mime = spec.mimeType.lowercase(java.util.Locale.ROOT)
    val codecs = spec.codecString.lowercase(java.util.Locale.ROOT)
    if (mime == "audio/eac3-joc" || codecs.startsWith("ec+3")) return "Dolby Atmos"
    val lossless = isLosslessAudioStream(mime, codecs)
    val quality = when {
        spec.quality == "HI-FI" -> "Hi-Fi"
        spec.quality == "LOSSLESS" -> "Lossless"
        // Specs built by hand (tests, older call sites) still read correctly off the mime type.
        lossless && (spec.sampleRateHz ?: 0) >= 88_200 -> "Hi-Fi"
        lossless -> "Lossless"
        else -> null
    }
    if (quality != null) {
        // A lossless bitrate is just the compression ratio, so only the lossy upgrade gets a number.
        val kbps = spec.bitrateKbps?.takeIf { it > 0 && !lossless }
        return if (kbps != null) "$quality · $kbps kbps" else quality
    }
    return playerAudioSpecLabels(spec).joinToString(" · ")
}

@Composable
private fun AppleMusicDockRow(actions: List<PlayerDockAction>) {
    if (actions.isEmpty()) return
    Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        actions.forEach { action ->
            Box(
                Modifier.size(AppleMusicDesign.playerActionIcon + AppleMusicDesign.playerActionPadding * 2)
                    .clip(RoundedCornerShape(AppleMusicDesign.cornerM))
                    .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
                    .semantics {
                        contentDescription = action.label
                        action.stateDescription?.let { stateDescription = it }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(AppleMusicDesign.playerActionIcon + 8.dp).clip(RoundedCornerShape(AppleMusicDesign.cornerM))
                        .background(if (action.active) AppleMusicVibrant.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (action.busy) DerbisLoadingIndicator(color = AppleMusicVibrant.secondary, modifier = Modifier.size(20.dp))
                    else Icon(
                        action.icon, null,
                        tint = if (action.active) Color.Black.copy(alpha = 0.78f) else if (action.enabled) AppleMusicVibrant.secondary else AppleMusicVibrant.tertiary,
                        modifier = Modifier.size(AppleMusicDesign.playerActionIcon - 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppleMusicPlayerMenu(actions: List<PlayerSheetAction>, onPerform: (PlayerSheetAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(AppleMusicDesign.cornerXl)).background(AppleMusicVibrant.quaternary)
    ) {
        actions.forEachIndexed { index, action ->
            key(action.key) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(enabled = action.enabled && !action.busy, role = Role.Button) { onPerform(action) }
                        .semantics {
                            contentDescription = listOfNotNull(action.label, action.value).joinToString(", ")
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            action.label, style = MaterialTheme.typography.bodyLarge,
                            color = if (action.enabled) AppleMusicVibrant.primary else AppleMusicVibrant.tertiary,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        action.value?.let { value ->
                            Text(value, style = MaterialTheme.typography.bodySmall, color = AppleMusicVibrant.secondary, maxLines = 1)
                        }
                    }
                    if (action.busy) DerbisLoadingIndicator(color = AppleMusicVibrant.secondary, modifier = Modifier.size(20.dp))
                    else Icon(
                        action.icon, null,
                        tint = if (action.active) AppleMusicDesign.accent else AppleMusicVibrant.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (index < actions.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = AppleMusicVibrant.tertiary)
                }
            }
        }
    }
}
