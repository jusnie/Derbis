package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.feature.motion.DerbisCanvasCatalogProvider
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.KaraokeWordTimedText
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.AppleMusicShapes
import com.luc4n3x.levyra.ui.theme.AppleMusicTypography
import com.luc4n3x.levyra.ui.theme.AppleMusicVibrant
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraPalette
import com.luc4n3x.levyra.ui.theme.appleMusicColorScheme
import com.luc4n3x.levyra.ui.theme.appleMusicPalette

private const val PausedArtworkScale = 0.82f
private const val ImmersiveCanvasMinAspect = 1.4f

internal val LocalAppleMusicPlayerPalette = staticCompositionLocalOf<LevyraPalette?> { null }

internal val PlayerAppleMusicStyle: Boolean
    @Composable @ReadOnlyComposable get() = LevyraIsAppleMusic || LocalAppleMusicPlayerPalette.current != null

internal val PlayerPrismStyle: Boolean
    @Composable @ReadOnlyComposable get() = LevyraIsPrism || LocalAppleMusicPlayerPalette.current != null

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
        MaterialTheme(colorScheme = colorScheme, typography = AppleMusicTypography, shapes = AppleMusicShapes, content = content)
    }
}

@Immutable
internal class AppleMusicCanvas(
    val artwork: MotionArtwork?,
    val enabled: Boolean,
    val quality: LevyraCanvasQuality,
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
        val immersive = appleMusicCanvasImmersive(canvas, maxWidth, maxHeight, video)
        val canvasAlpha by animateFloatAsState(if (immersive) 1f else 0f, label = "apple-player-canvas-alpha")
        if (canvas != null && canvasAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = canvasAlpha }) {
                AppleMusicCanvasBackdrop(track, canvas, playing)
            }
        }
        val artwork: @Composable (Dp) -> Unit = { size ->
            if (video) AppleMusicPlayerVideo(track, slots, size) else AppleMusicPlayerArtwork(track, slots, size, playing, { canvasAlpha }, showStage = !immersive)
        }
        if (landscape) {
            AppleMusicLandscapeDeck(track, slots, minOf(maxHeight - 34.dp, artworkCap), artwork, metadata, transport)
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
                lyricPositionMs = lyricPositionMs
            )
        }
    }
}

@Composable
private fun AppleMusicPlayerArtwork(
    track: Track, slots: PlayerDeckSlots, size: Dp, playing: Boolean, canvasAlpha: () -> Float, showStage: Boolean
) {
    val animations = LocalAnimationsEnabled.current
    val artScale by animateFloatAsState(
        if (playing) 1f else PausedArtworkScale,
        if (animations) spring(dampingRatio = 0.72f, stiffness = 380f) else spring(stiffness = 100_000f),
        label = "apple-player-artwork-scale"
    )
    val artElevation by animateDpAsState(if (playing) 15.dp else 8.dp, label = "apple-player-artwork-elevation")
    Box(
        Modifier.size(size)
            .graphicsLayer {
                scaleX = artScale
                scaleY = artScale
                alpha = 1f - canvasAlpha()
            }
            .shadow(artElevation, RoundedCornerShape(AppleMusicDesign.cornerL), clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
    ) {
        if (showStage) slots.stage(track, Modifier.fillMaxSize(), AppleMusicDesign.cornerL)
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
    lyricPositionMs: Long = 0L
) {
    if (video) {
        AppleMusicVideoPortraitDeck(track, slots, artworkCap, immersive, artwork, metadata, transport, activeLyric, lyricPositionMs)
    } else {
        AppleMusic13ReferencePortraitDeck(track, slots, artworkCap, immersive, artwork, metadata, transport, activeLyric, lyricPositionMs)
    }
}

/**
 * Portrait Apple Music 13 follows the supplied reference: a cropped cover band on top, the
 * artwork's blurred adaptive field behind it, then airy metadata, slim progress, transport,
 * volume and a compact output/action strip. This is intentionally separate from Velvet/Aura.
 */
@Composable
private fun AppleMusic13ReferencePortraitDeck(
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
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val coverHeight = (maxWidth / 1.62f).coerceIn(154.dp, 272.dp)
        val topBreathingRoom = (maxHeight * 0.055f).coerceIn(20.dp, 46.dp)
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(topBreathingRoom))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                SeamlessArtworkImage(
                    url = preferredPlayerArtworkUrl(track),
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = null,
                    fallback = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            artwork(minOf(coverHeight, artworkCap))
                        }
                    }
                )
                slots.gestures(track, Modifier.matchParentSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.08f),
                            1f to Color.Black.copy(alpha = 0.30f)
                        )
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clipToBounds()) { metadata() }
                AppleMusic13PageDots()
                AppleMusic13InlineLyrics(
                    line = activeLyric,
                    positionMs = lyricPositionMs,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Box(Modifier.padding(top = 2.dp)) { slots.progress(PlayerProgressStyle.Classic) }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(Modifier.padding(horizontal = 8.dp)) { transport() }
                }
                PlayerOutputVolume(
                    contentColor = Color.White,
                    mutedColor = Color.White.copy(alpha = 0.72f),
                    activeColor = Color.White,
                    trackColor = Color.White.copy(alpha = 0.26f),
                    condensed = true,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                AppleMusic13ActionStrip(slots.dockActions(track))
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun AppleMusic13PageDots() {
    Row(
        modifier = Modifier.padding(top = 3.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(if (index == 0) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (index == 0) 0.92f else 0.42f))
            )
        }
    }
}

@Composable
private fun AppleMusic13ActionStrip(actions: List<PlayerDockAction>) {
    val shown = actions.filter { it.key == "lyrics" || it.key == "queue" || it.key == "visual-mode" }.take(3)
    if (shown.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 48.dp)
                    .clip(if (index == 1) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp))
                    .background(if (index == 1 || action.active) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(enabled = action.enabled && !action.busy, role = Role.Button, onClick = action.onClick)
                    .semantics {
                        contentDescription = action.label
                        action.stateDescription?.let { stateDescription = it }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action.busy) {
                    LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (action.enabled) 0.94f else 0.40f),
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

@Composable
private fun AppleMusic13InlineLyrics(line: LyricLine?, positionMs: Long, modifier: Modifier = Modifier) {
    val activeLine = line?.takeIf { it.text.isNotBlank() } ?: return
    if (appleMusic13UsesWordTiming(activeLine)) {
        Box(modifier = modifier.fillMaxWidth()) {
            KaraokeWordTimedText(
                lineText = activeLine.text,
                words = activeLine.words,
                positionProvider = { positionMs },
                isActive = true,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
                inactiveColor = Color.White.copy(alpha = 0.42f),
                completedColor = Color.White.copy(alpha = 0.96f),
                activeColor = Color.White,
                pendingColor = Color.White.copy(alpha = 0.42f),
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Text(
            text = activeLine.text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.2).sp
            ),
            color = Color.White.copy(alpha = 0.95f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppleMusicLandscapeDeck(
    track: Track, slots: PlayerDeckSlots, artworkSize: Dp, artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit
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
                if (resolving) LevyraLoadingIndicator(color = AppleMusicVibrant.primary, modifier = Modifier.size(30.dp))
                else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = AppleMusicVibrant.primary, modifier = Modifier.size(AppleMusicDesign.playerPlayIcon + 10.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon + AppleMusicDesign.playerControlPadding * 2)) {
                Icon(Icons.Rounded.FastForward, labels.next, tint = AppleMusicVibrant.primary, modifier = Modifier.size(AppleMusicDesign.playerSkipIcon))
            }
        }
        AppleMusicModeToggle(
            if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off,
            when (repeat) {
                RepeatMode.All -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_all)
                RepeatMode.One -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_one)
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
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val barHeight by animateDpAsState(if (dragging != null) 11.dp else 7.dp, label = "apple-seekbar-height")
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).semantics { contentDescription = seekLabel },
            thumb = { Spacer(Modifier.size(1.dp)) },
            track = { state ->
                val shape = RoundedCornerShape(barHeight / 2)
                Box(Modifier.fillMaxWidth().height(barHeight).clip(shape).background(AppleMusicVibrant.tertiary)) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(AppleMusicVibrant.quaternary))
                    Box(
                        Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight()
                            .background(if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary)
                    )
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val timeStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp)
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = timeStyle, color = if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary)
            AppleMusicAudioBadge(Modifier.weight(1f))
            Text(
                if (durationMs > 0L) "-" + formatSeekbarMillis((durationMs - shown).coerceAtLeast(0L)) else formatSeekbarMillis(0L),
                style = timeStyle, color = if (dragging != null) AppleMusicVibrant.primary else AppleMusicVibrant.secondary
            )
        }
    }
}

@Composable
private fun AppleMusicAudioBadge(modifier: Modifier = Modifier) {
    val spec by rememberPlayerAudioSpec()
    val label = remember(spec) { appleMusicAudioBadgeLabel(spec) }
    Box(modifier, contentAlignment = Alignment.Center) {
        val displayLabel = if (label.isNotEmpty()) label else "73 kbps · 48 kHz"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Rounded.Headphones,
                contentDescription = null,
                tint = AppleMusicVibrant.secondary,
                modifier = Modifier.size(11.dp)
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

internal fun appleMusicAudioBadgeLabel(spec: PlayerAudioSpec): String {
    if (spec.isEmpty) return ""
    val mime = spec.mimeType.lowercase(java.util.Locale.ROOT)
    val codec = spec.codecString.lowercase(java.util.Locale.ROOT)
    val lossless = spec.codec == "FLAC" || spec.codec == "ALAC" || spec.codec == "PCM"
    return when {
        mime == "audio/eac3-joc" || codec.startsWith("ec+3") -> "Dolby Atmos"
        lossless && (spec.sampleRateHz ?: 0) > 48_000 -> "Hi-Res Lossless"
        lossless -> "Lossless"
        else -> listOfNotNull(spec.codec.ifEmpty { null }, spec.bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" }).joinToString(" · ")
    }
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
                    if (action.busy) LevyraLoadingIndicator(color = AppleMusicVibrant.secondary, modifier = Modifier.size(20.dp))
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
                    if (action.busy) LevyraLoadingIndicator(color = AppleMusicVibrant.secondary, modifier = Modifier.size(20.dp))
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
