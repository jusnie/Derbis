package com.yasuda.derbis.ui.player

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Derbis One — the scalloped sleeve.
 *
 * One idea: the sleeve is the page. The cover is blown up and blurred into the whole backdrop, a
 * scalloped mat holds the artwork, and the progress ring is that same scalloped outline pushed
 * out just past the mat, with the playhead riding it. Everything else is light ink on a dark
 * record.
 *
 * Geometry is expressed as a fraction of the stage box so the same shape reads identically on a
 * 5" phone and a tablet: the mat spans 62% of the box, the ring sits 10% outside its lobes.
 */

/** The record's dark ground, under the blurred sleeve. */
private val OneGround = Color(0xFF0C0C0F)

/** Ink is light now: the page is a dark record, not a sheet of paper. */
private val OneInk = Color(0xFFF4F1EA)

/** Ink printed inside the filled play button, which is the one light shape on the deck. */
private val OneFillInk = OneGround

private const val OneBlobLobes = 13
private const val OneBlobAmplitude = 0.055f

/**
 * Mean radius of the scalloped mat as a fraction of the stage box.
 *
 * It used to be 0.369, which put a 37 %-of-the-box stamp in the middle of a tall stage: the cover
 * is the one thing a deck should not shrink away from. At 0.62 the mat spans 62 % of the box, and
 * the ring below is sized to sit just outside the lobes rather than on a circle of its own.
 */
private const val OneBlobRadiusRatio = 0.62f

/** The ring orbits the scalloped edge: this is how far outside it sits. */
private const val OneRingScale = 1.10f

private const val OneOrbitWidthDp = 1f
private const val OneArcWidthDp = 1.4f
private const val OneDotRadiusDp = 4.5f

/** Ink alphas, taken from the study sheet. */
private const val OneInkFull = 1f
private const val OneInkStrong = 0.66f
private const val OneInkSoft = 0.44f
private const val OneInkMuted = 0.4f
private const val OneInkQuiet = 0.32f
private const val OneInkIdle = 0.5f
private const val OneHairlineSoft = 0.09f
private const val OneOrbitAlpha = 0.16f
private const val OneArcAlpha = 0.46f

/** How much of the blurred sleeve survives the scrim. */
private const val OneMistAlpha = 0.55f

private val OneStagePadding = 4.dp
private val OnePlaySize = 62.dp
private val OnePlaySizeCompact = 54.dp
private val OneControlSize = 46.dp
private val OneGlyphSize = 22.dp
private val OneModeGlyphSize = 19.dp
private val OneStripGap = 22.dp

/**
 * The scalloped mat: r(t) = mean * (1 + amplitude * cos(lobes * t)), smoothed with the same
 * Catmull-Rom to cubic conversion the study sheet uses so the bumps stay round at any size.
 */
/**
 * The scalloped outline as a path, so the mat and the ring are guaranteed to be the same shape.
 *
 * [startAngle] rotates where sample zero sits without rotating the lobes, which is what lets the
 * progress ring start at the top of the shape the way a clock hand does.
 */
private fun oneScallopPath(
    center: Offset,
    mean: Float,
    amplitude: Float,
    lobes: Int,
    startAngle: Float = 0f
): Path {
    val samples = 240
    val twoPi = 2f * PI.toFloat()
    val xs = FloatArray(samples)
    val ys = FloatArray(samples)
    for (index in 0 until samples) {
        val t = startAngle + index.toFloat() / samples.toFloat() * twoPi
        val r = mean * (1f + amplitude * cos(lobes * (t - startAngle)))
        xs[index] = center.x + r * cos(t)
        ys[index] = center.y + r * sin(t)
    }
    val path = Path()
    path.moveTo(xs[0], ys[0])
    for (index in 0 until samples) {
        val previous = (index - 1 + samples) % samples
        val next = (index + 1) % samples
        val afterNext = (index + 2) % samples
        // Catmull-Rom to cubic, so the bumps stay round at any size instead of going polygonal.
        path.cubicTo(
            xs[index] + (xs[next] - xs[previous]) / 6f,
            ys[index] + (ys[next] - ys[previous]) / 6f,
            xs[next] - (xs[afterNext] - xs[index]) / 6f,
            ys[next] - (ys[afterNext] - ys[index]) / 6f,
            xs[next],
            ys[next]
        )
    }
    path.close()
    return path
}

@Immutable
internal class PlayerOneBlobShape(
    private val lobes: Int = OneBlobLobes,
    private val amplitude: Float = OneBlobAmplitude,
    private val radiusRatio: Float = OneBlobRadiusRatio
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val mean = minOf(size.width, size.height) / 2f * radiusRatio
        val center = Offset(size.width / 2f, size.height / 2f)
        return Outline.Generic(oneScallopPath(center, mean, amplitude, lobes))
    }

    override fun equals(other: Any?): Boolean =
        other is PlayerOneBlobShape &&
            other.lobes == lobes &&
            other.amplitude == amplitude &&
            other.radiusRatio == radiusRatio

    override fun hashCode(): Int {
        var result = lobes
        result = 31 * result + amplitude.hashCode()
        result = 31 * result + radiusRatio.hashCode()
        return result
    }

    private companion object {
        val TwoPi: Float = 2f * PI.toFloat()
    }
}

private val OneBlobShape = PlayerOneBlobShape()

@Composable
internal fun PlayerDerbisOneDeck(
    track: Track,
    slots: PlayerDeckSlots,
    playback: PlayerDeckPlayback,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    onArtistClick: (Int) -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // The sleeve is the whole page now, blurred past recognition behind the mat, so the deck is
        // dark and the ink on it is light. A flat paper ground made every cover look pasted onto a
        // sheet; this is the record the track came from.
        OneBackdrop(track = track)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    start = gutter,
                    end = gutter,
                    top = DerbisPlayerDesign.SpaceXs,
                    bottom = if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceXl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            slots.header()
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceSm else 15.dp))
            OneClock(playback = playback)
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.height(OneScrollStage) else Modifier.weight(1f))
                    .padding(vertical = OneStagePadding),
                contentAlignment = Alignment.Center
            ) {
                val side = minOf(maxWidth, maxHeight, artworkCap)
                if (side > 0.dp) {
                    Box(
                        modifier = Modifier.size(side),
                        contentAlignment = Alignment.Center
                    ) {
                        slots.stage(
                            track,
                            Modifier
                                .size(side)
                                .graphicsLayer {
                                    shape = OneBlobShape
                                    clip = true
                                },
                            0.dp
                        )
                        OneOrbit(
                            trackId = track.id,
                            playback = playback,
                            modifier = Modifier.matchParentSize()
                        )
                        slots.gestures(track, Modifier.matchParentSize())
                    }
                }
            }
            OneHeadline(
                track = track,
                animated = animated,
                compact = compact,
                onArtistClick = onArtistClick,
                modifier = headlineModifier
            )
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg))
            OneTransport(
                isPlaying = isPlaying,
                isResolving = isResolving,
                shuffleOn = shuffleOn,
                repeatMode = repeatMode,
                labels = labels,
                compact = compact,
                onShuffle = onShuffle,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onRepeat = onRepeat
            )
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else OneStripGap))
            OneLyricsStrip(
                track = track,
                playback = playback,
                dockActions = slots.dockActions(track),
                compact = compact
            )
        }
    }
}

private val OneScrollStage = 300.dp

/**
 * The cover, blown up and blurred to a colour field, under a scrim.
 *
 * Two things keep it from being the wall of light a bright sleeve would otherwise be: the image is
 * decoded small and drawn at [OneMistAlpha], and the scrim pulls the top and bottom back down far
 * enough that the clock, the headline and the transport keep their contrast.
 */
@Composable
private fun OneBackdrop(track: Track) {
    val url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    val context = LocalContext.current
    val decodePx = remember { mistDecodePxForApi(Build.VERSION.SDK_INT) }
    Box(modifier = Modifier.fillMaxSize().background(OneGround)) {
        if (url.isNotBlank()) {
            val request = remember(context, url, decodePx) {
                ImageRequest.Builder(context)
                    .data(DerbisArtworkCache.large(url))
                    .size(decodePx, decodePx)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Overscan, so the blur never shows a soft edge at the window bounds.
                        scaleX = 1.28f
                        scaleY = 1.28f
                        alpha = OneMistAlpha
                    }
                    .blur(64.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to OneGround.copy(alpha = 0.74f),
                        0.40f to OneGround.copy(alpha = 0.30f),
                        0.72f to OneGround.copy(alpha = 0.52f),
                        1f to OneGround.copy(alpha = 0.88f)
                    )
                )
        )
    }
}

@Composable
private fun OneClock(playback: PlayerDeckPlayback, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatOneTime(playback.positionMs),
            color = OneInk,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            style = OneTabular,
            maxLines = 1
        )
        Text(
            text = "  /  ",
            color = OneInk.copy(alpha = OneInkMuted),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            maxLines = 1
        )
        Text(
            text = if (playback.hasDuration) formatOneTime(playback.durationMs) else "--:--",
            color = OneInk.copy(alpha = OneInkMuted),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            style = OneTabular,
            maxLines = 1
        )
    }
}

@Composable
private fun OneOrbit(
    trackId: String,
    playback: PlayerDeckPlayback,
    modifier: Modifier = Modifier
) {
    var scrub by remember(trackId) { mutableStateOf<Float?>(null) }
    val liveFraction by rememberUpdatedState(playback.fraction())
    val shown = scrub ?: liveFraction
    Box(
        modifier = modifier.pointerInput(trackId, playback.hasDuration) {
            if (!playback.hasDuration) return@pointerInput
            detectDragGestures(
                onDragStart = { scrub = liveFraction },
                onDragEnd = {
                    scrub?.let(playback.onSeek)
                    scrub = null
                },
                onDragCancel = { scrub = null }
            ) { change, dragAmount ->
                change.consume()
                val base = scrub ?: liveFraction
                scrub = (base + dragAmount.x / size.width.toFloat().coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
            }
        }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // The ring is the mat's own outline, pushed out by OneRingScale: the progress line
            // follows the scallops instead of ignoring them, and the dot rides that outline.
            val mean = size.minDimension / 2f * OneBlobRadiusRatio * OneRingScale
            val ring = oneScallopPath(
                center = center,
                mean = mean,
                amplitude = OneBlobAmplitude,
                lobes = OneBlobLobes,
                startAngle = -PI.toFloat() / 2f
            )
            val stroke = OneOrbitWidthDp.dp.toPx()
            val arcStroke = OneArcWidthDp.dp.toPx()
            drawPath(
                path = ring,
                color = OneInk.copy(alpha = OneOrbitAlpha),
                style = Stroke(width = stroke)
            )
            val measure = PathMeasure()
            measure.setPath(ring, false)
            val length = measure.length
            if (length <= 1f) return@Canvas
            val head = length * shown.coerceIn(0f, 1f)
            if (head > 0.5f) {
                val played = Path()
                measure.getSegment(0f, head, played, true)
                drawPath(
                    path = played,
                    color = OneInk.copy(alpha = OneArcAlpha),
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                )
            }
            val dot = measure.getPosition(head.coerceAtMost(length))
            drawCircle(
                color = OneInk,
                radius = OneDotRadiusDp.dp.toPx(),
                center = dot
            )
        }
    }
}

@Composable
private fun OneHeadline(
    track: Track,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { DerbisMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-one-headline"
    ) { shown ->
    Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val titleSize = if (compact) 21.sp else 24.sp
            Text(
                text = shown.title.uppercase(),
                color = OneInk,
                fontSize = titleSize,
                lineHeight = titleSize * 1.15f,
                fontWeight = FontWeight.Medium,
                letterSpacing = (titleSize.value * 0.055f).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )
            Spacer(Modifier.height(9.dp))
            val kicker = oneKicker(shown)
            if (kicker.isNotEmpty()) {
                Text(
                    text = kicker,
                    color = OneInk.copy(alpha = OneInkSoft),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .derbisPressable(
                            onClick = { onArtistClick(0) },
                            enabled = shown.artist.isNotBlank(),
                            pressedScale = DerbisPressScale.Row,
                            role = Role.Button
                        )
                        .padding(horizontal = DerbisPlayerDesign.SpaceSm, vertical = 2.dp)
                )
            }
        }
    }
}

/** "ALBUM by ARTIST", the caption line from the study sheet. */
internal fun oneKicker(track: Track): String {
    val album = track.album.trim()
    val artist = track.artist.trim()
    return when {
        artist.isBlank() -> album.uppercase()
        album.isBlank() || album.equals(track.title.trim(), ignoreCase = true) -> artist.uppercase()
        else -> "${album.uppercase()} by ${artist.uppercase()}"
    }
}

@Composable
private fun OneTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    compact: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneModeButton(
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            active = repeatMode != RepeatMode.Off,
            onClick = onRepeat
        )
        OneSkipButton(
            icon = Icons.Rounded.SkipPrevious,
            label = labels.previous,
            compact = compact,
            onClick = onPrevious
        )
        OnePlayButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            compact = compact,
            onClick = onTogglePlay
        )
        OneSkipButton(
            icon = Icons.Rounded.SkipNext,
            label = labels.next,
            compact = compact,
            onClick = onNext
        )
        OneModeButton(
            icon = Icons.Rounded.Shuffle,
            label = labels.shuffle,
            active = shuffleOn,
            onClick = onShuffle
        )
    }
}

@Composable
private fun OneModeButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(OneControlSize)
            .clip(CircleShape)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(
            icon = icon,
            tint = OneInk.copy(alpha = if (active) OneInkStrong else OneInkIdle),
            modifier = Modifier.size(OneModeGlyphSize)
        )
    }
}

@Composable
private fun OneSkipButton(
    icon: ImageVector,
    label: String,
    compact: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(if (compact) OneControlSize - 4.dp else OneControlSize)
            .clip(CircleShape)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(
            icon = icon,
            tint = OneInk.copy(alpha = OneInkFull * 0.92f),
            modifier = Modifier.size(OneGlyphSize)
        )
    }
}

@Composable
private fun OnePlayButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    compact: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val label = if (isPlaying) labels.pause else labels.play
    val size = if (compact) OnePlaySizeCompact else OnePlaySize
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(OneInk)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            DerbisLoadingIndicator(color = OneFillInk, modifier = Modifier.size(size * 0.36f))
        } else {
            DerbisPlayPauseGlyph(
                playing = isPlaying,
                color = OneFillInk,
                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
            )
        }
    }
}

@Composable
private fun OneLyricsStrip(
    track: Track,
    playback: PlayerDeckPlayback,
    dockActions: List<PlayerDockAction>,
    compact: Boolean
) {
    val line = playerInlineLyricsText(playback.activeLyric, playback.lyricsLabel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compact) DerbisPlayerDesign.SpaceMd else 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OneInk.copy(alpha = OneHairlineSoft))
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playback.lyricsLabel.uppercase(),
                color = OneInk.copy(alpha = OneInkSoft),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.9.sp,
                maxLines = 1
            )
            OneDockRow(actions = dockActions)
        }
        if (line.isNotEmpty()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = line,
                color = OneInk.copy(alpha = OneInkQuiet),
                fontSize = 11.5.sp,
                lineHeight = 11.5.sp * 1.85f,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .derbisPressable(
                        onClick = playback.onOpenLyrics,
                        pressedScale = DerbisPressScale.Row,
                        role = Role.Button
                    )
                    .semantics { contentDescription = "${playback.lyricsLabel}: $line" }
            )
        } else if (track.title.isNotBlank()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = playback.lyricsLabel,
                color = OneInk.copy(alpha = OneInkQuiet),
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .derbisPressable(
                        onClick = playback.onOpenLyrics,
                        pressedScale = DerbisPressScale.Row,
                        role = Role.Button
                    )
                    .semantics { contentDescription = playback.lyricsLabel }
            )
        }
    }
}

@Composable
private fun OneDockRow(actions: List<PlayerDockAction>) {
    if (actions.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .derbisPressable(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        pressedScale = DerbisPressScale.Control,
                        role = Role.Button
                    )
                    .semantics { contentDescription = action.label },
                contentAlignment = Alignment.Center
            ) {
                if (action.busy) {
                    DerbisLoadingIndicator(
                        color = OneInk.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    PlayerIcon(
                        icon = action.icon,
                        tint = OneInk.copy(alpha = if (action.active) 0.8f else OneInkSoft),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private val OneTabular = TextStyle(fontFeatureSettings = "tnum")

internal fun formatOneTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}
