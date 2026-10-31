package com.yasuda.derbis.ui.insights

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.LayoutDirection
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import com.yasuda.derbis.ui.icons.Close
import com.yasuda.derbis.ui.icons.KeyboardArrowDown
import com.yasuda.derbis.ui.icons.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yasuda.derbis.R
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.domain.ListeningInsightsArtist
import com.yasuda.derbis.domain.ListeningInsightsRanges
import com.yasuda.derbis.domain.ListeningInsightsSnapshot
import com.yasuda.derbis.domain.ListeningInsightsTrack
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisViolet
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

private const val WRAPPED_PAGE_MS = 6_500
private val WrappedInk = Color.White
private val WrappedMuted = Color.White.copy(alpha = 0.72f)
private val WrappedBackground = Color(0xFF040508)
private val WrappedOnAccent = Color(0xFF07080C)

/**
 * Hero type in this story is sized for a tall phone. On a short viewport (landscape, split
 * screen, small phones) a 120sp year or an 84sp counter clips, so every page multiplies its
 * display sizes by this factor. 1f is the reference height; the floor keeps the words legible
 * instead of shrinking them into noise.
 */
internal fun wrappedTypeScale(availableHeightDp: Float): Float =
    (availableHeightDp / WRAPPED_REFERENCE_HEIGHT_DP).coerceIn(WRAPPED_MIN_TYPE_SCALE, 1f)

private const val WRAPPED_REFERENCE_HEIGHT_DP = 720f
private const val WRAPPED_MIN_TYPE_SCALE = 0.74f

private val LocalWrappedTypeScale = staticCompositionLocalOf { 1f }

/**
 * Height of the page viewport. Pages use it as a minimum height so short content still centres
 * while tall content scrolls instead of clipping — `Arrangement.Center` alone cannot do that
 * inside a scroll container, because scrolling measures children with an unbounded height.
 */
private val LocalWrappedViewportHeight = staticCompositionLocalOf { 0.dp }

/** Scales one display size by the current page budget. */
@Composable
private fun wrappedSp(base: Float): TextUnit = (base * LocalWrappedTypeScale.current).sp

@Composable
private fun wrappedDp(base: Dp): Dp = (base.value * LocalWrappedTypeScale.current).dp

/**
 * Shared eyebrow tag ("TOP ARTIST", "TOTAL LISTENING", …). One implementation keeps the pill
 * radius, hairline and letter-spacing identical across pages instead of drifting per copy.
 */
@Composable
private fun WrappedTag(
    text: String,
    accent: Color,
    icon: ImageVector,
    tint: Color = accent
) = WrappedTagContent(text = text) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
}

@Composable
private fun WrappedTag(
    text: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.painter.Painter,
    tint: Color = accent
) = WrappedTagContent(text = text) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
}

@Composable
private fun WrappedTagContent(text: String, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            color = WrappedInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            maxLines = 1
        )
    }
}

private enum class WrappedPage { Intro, Minutes, Artist, Tracks, Rhythm, Finale }

private fun wrappedPages(snapshot: ListeningInsightsSnapshot): List<WrappedPage> = buildList {
    add(WrappedPage.Intro)
    add(WrappedPage.Minutes)
    if (snapshot.topArtists.any { it.name.isNotBlank() }) add(WrappedPage.Artist)
    if (snapshot.topTracks.isNotEmpty()) add(WrappedPage.Tracks)
    if (snapshot.hourBuckets.any { it > 0L }) add(WrappedPage.Rhythm)
    add(WrappedPage.Finale)
}

@Composable
internal fun ListeningWrappedStory(
    state: ListeningWrappedState,
    accent: Color,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val animationsEnabled = LocalAnimationsEnabled.current
    val snapshot = state.snapshot?.takeIf { it.hasSignal }
    val pages = remember(snapshot) { snapshot?.let(::wrappedPages).orEmpty() }
    var pageIndex by rememberSaveable(state.year) { mutableIntStateOf(0) }
    val lastIndex = (pages.size - 1).coerceAtLeast(0)
    val page = pages.getOrNull(pageIndex.coerceIn(0, lastIndex))

    var isHolding by remember { mutableStateOf(false) }
    var currentProgress by remember(pageIndex) { mutableFloatStateOf(0f) }

    // The share card is the same listening receipt the Finale renders, built once here so the
    // share action works from any page of the story instead of only at the end.
    val receiptContent = remember(snapshot, strings, locale, state.year) {
        val current = snapshot ?: return@remember null
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        ListeningReceiptModel.content(
            snapshot = current,
            labels = receiptLabels(strings, strings.wrappedReceiptTitle, state.year.toString(), locale),
            locale = locale,
            nowMs = now,
            zone = zone,
            range = ListeningInsightsRanges.year(state.year, now, zone)
        )
    }
    // The card follows whatever receipt style the finale is set to, so sharing never
    // produces a different look than the one on screen.
    var receiptStyle by rememberSaveable {
        mutableStateOf(ListeningReceiptStyle.Aurora)
    }
    val shareCard = rememberWrappedCardSharer(
        content = receiptContent,
        caption = strings.receiptCaption,
        failedLabel = strings.shareLyricsFailed,
        styleOf = { receiptStyle }
    )

    val covers = remember(snapshot) {
        snapshot?.topTracks?.map { it.artworkUrl }?.filter(String::isNotBlank)?.distinct().orEmpty()
    }

    BackHandler {
        if (pageIndex > 0) pageIndex-- else onClose()
    }

    LaunchedEffect(pageIndex, animationsEnabled, pages.size) {
        if (!animationsEnabled || pages.isEmpty() || pageIndex >= lastIndex) {
            currentProgress = 1f
            return@LaunchedEffect
        }
        val durationMs = WRAPPED_PAGE_MS.toFloat()
        var lastTimeNanos = withFrameNanos { it }
        while (currentProgress < 1f) {
            val frameNanos = withFrameNanos { it }
            val dtMs = (frameNanos - lastTimeNanos) / 1_000_000f
            lastTimeNanos = frameNanos
            if (!isHolding) {
                currentProgress = (currentProgress + dtMs / durationMs).coerceAtMost(1f)
            }
        }
        if (!isHolding && currentProgress >= 1f) {
            pageIndex = (pageIndex + 1).coerceAtMost(lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WrappedBackground)
    ) {
        WrappedBackdrop(covers = covers, accent = accent, intro = page == WrappedPage.Intro)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            WrappedProgress(
                count = pages.size,
                current = pageIndex,
                progress = { currentProgress },
                accent = accent
            )
            WrappedTopBar(
                year = state.year,
                closeLabel = strings.close,
                accent = accent,
                share = shareCard,
                shareLabel = strings.share,
                onClose = onClose
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrappedHoldAndTapNavigation(
                        enabled = page != null && page != WrappedPage.Finale,
                        onHoldChanged = { isHolding = it },
                        onPrevious = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        onNext = { pageIndex = (pageIndex + 1).coerceAtMost(lastIndex) }
                    )
            ) {
                WrappedBody(
                    state = state,
                    snapshot = snapshot,
                    page = page,
                    strings = strings,
                    locale = locale,
                    accent = accent,
                    animationsEnabled = animationsEnabled,
                    onReceiptStyleChange = { receiptStyle = it }
                )
            }
        }
    }
}

private fun Modifier.wrappedHoldAndTapNavigation(
    enabled: Boolean,
    onHoldChanged: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier = if (!enabled) {
    this
} else {
    composed {
        // In RTL the "back" third is on the right, so the edge test has to mirror; otherwise an
        // Arabic or Hebrew reader taps the left edge expecting "next" and goes back.
        val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
        pointerInput(rightToLeft) {
            detectTapGestures(
                onPress = {
                    onHoldChanged(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHoldChanged(false)
                    }
                },
                onTap = { offset ->
                    val onTrailingEdge = offset.x >= size.width * 0.7f
                    val onLeadingEdge = offset.x < size.width * 0.3f
                    when {
                        onLeadingEdge -> if (rightToLeft) onNext() else onPrevious()
                        onTrailingEdge -> if (rightToLeft) onPrevious() else onNext()
                        else -> onNext()
                    }
                }
            )
        }
    }
}

@Composable
private fun WrappedTopBar(
    year: Int,
    closeLabel: String,
    accent: Color,
    share: WrappedCardSharer,
    shareLabel: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sound_wave),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "REPLAY '$year",
                color = WrappedInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp
            )
        }
        Spacer(Modifier.weight(1f))
        // Share lives next to Close so the card is one tap away from any page.
        IconButton(
            onClick = share::share,
            enabled = share.canShare && !share.busy,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
        ) {
            if (share.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 1.8.dp,
                    color = WrappedInk
                )
            } else {
                Icon(
                    Icons.Rounded.IosShare,
                    contentDescription = shareLabel,
                    tint = WrappedInk,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = closeLabel,
                tint = WrappedInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    share.error?.let { message ->
        Text(
            text = message,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Owns the share-card coroutine and its progress state for the story's top bar.
 *
 * The receipt render is expensive (covers, artist art, a 1080px bitmap), so it runs on demand and
 * every bitmap is closed again: flipping pages never keeps receipt artwork in memory.
 */
@Stable
internal class WrappedCardSharer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val caption: String,
    private val failedLabel: String,
    private val styleOf: () -> ListeningReceiptStyle,
    private val content: () -> ListeningReceiptContent?
) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canShare: Boolean get() = content() != null

    fun share() {
        val current = content() ?: return
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val launched = runCatching {
                withContext(Dispatchers.IO) {
                    val assets = listeningReceiptCovers()
                    try {
                        ListeningReceiptCard.loadAssets(
                            context = context,
                            urls = current.artworkUrls,
                            artistUrl = "",
                            into = assets
                        )
                        val intent = assets.use { loaded ->
                            ListeningReceiptCard.createShareIntent(
                                context = context,
                                content = current,
                                assets = assets,
                                style = styleOf(),
                                palette = ListeningReceiptCard.resolvePalette(loaded),
                                caption = caption
                            )
                        } ?: return@withContext false
                        withContext(Dispatchers.Main) {
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(intent, caption).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }.isSuccess
                        }
                    } finally {
                        assets.close()
                    }
                }
            }.getOrDefault(false)
            busy = false
            if (!launched) error = failedLabel
        }
    }
}

@Composable
private fun rememberWrappedCardSharer(
    content: ListeningReceiptContent?,
    caption: String,
    failedLabel: String,
    styleOf: () -> ListeningReceiptStyle
): WrappedCardSharer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(content)
    val latestStyle by rememberUpdatedState(styleOf)
    return remember(context, scope, caption, failedLabel) {
        WrappedCardSharer(
            context = context,
            scope = scope,
            caption = caption,
            failedLabel = failedLabel,
            styleOf = { latestStyle() },
            content = { latest }
        )
    }
}

@Composable
private fun BoxScope.WrappedBody(
    state: ListeningWrappedState,
    snapshot: ListeningInsightsSnapshot?,
    page: WrappedPage?,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean,
    onReceiptStyleChange: (ListeningReceiptStyle) -> Unit = {}
) {
    // Measure the page area rather than the window: the progress bar and top bar already took
    // their share, and that is what actually has to fit.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val typeScale = remember(maxHeight) { wrappedTypeScale(maxHeight.value) }
        CompositionLocalProvider(
            LocalWrappedTypeScale provides typeScale,
            LocalWrappedViewportHeight provides maxHeight
        ) {
            when {
                state.loading -> WrappedStatusCard(
                    title = strings.wrappedYearInMusic,
                    body = "",
                    accent = accent,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                snapshot == null || page == null -> WrappedStatusCard(
                    title = strings.wrappedYearInMusic,
                    body = if (state.failed) strings.insightsError else strings.wrappedEmpty.replace("%s", state.year.toString()),
                    accent = accent,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> AnimatedContent(
                    targetState = page,
                    transitionSpec = { wrappedPageTransition(animationsEnabled) },
                    label = "wrapped-page"
                ) { target ->
                    WrappedPageContent(
                        page = target,
                        year = state.year,
                        snapshot = snapshot,
                        strings = strings,
                        locale = locale,
                        accent = accent,
                        animationsEnabled = animationsEnabled,
                        onReceiptStyleChange = onReceiptStyleChange
                    )
                }
            }
        }
    }
}

/**
 * Loading and empty states used to be a bare spinner or a lonely sentence floating in the dark.
 * A framed card keeps the story from reading as broken while it resolves.
 */
@Composable
private fun WrappedStatusCard(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (content != null) {
            content()
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_sound_wave),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title.uppercase(),
            color = WrappedInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (body.isNotBlank()) {
            Text(
                text = body,
                color = WrappedMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun wrappedPageTransition(animationsEnabled: Boolean): ContentTransform = if (animationsEnabled) {
    val enter = fadeIn(tween(320)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f)
    enter togetherWith fadeOut(tween(180))
} else {
    fadeIn(snap()) togetherWith fadeOut(snap())
}

@Composable
private fun WrappedProgress(count: Int, current: Int, progress: () -> Float, accent: Color) {
    if (count <= 0) return
    val pageProgress = progress().coerceIn(0f, 1f)
    val completed = (current + pageProgress) / count
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(3.dp)
            // The bar is the only progress cue in the story, so expose it instead of leaving
            // screen-reader users with nothing but "next page" taps.
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(completed, 0f..1f, count)
            }
    ) {
        val gap = 4.dp.toPx()
        val width = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        repeat(count) { index ->
            val left = index * (width + gap)
            drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(left, 0f), Size(width, size.height), radius)
            val fill = when {
                index < current -> 1f
                index == current -> progress().coerceIn(0f, 1f)
                else -> 0f
            }
            if (fill > 0f) {
                // Finished segments stay neutral; the live one carries the accent so the eye
                // lands on where the story currently is.
                val brush = if (index == current) {
                    Brush.horizontalGradient(listOf(accent, Color.White))
                } else {
                    Brush.horizontalGradient(listOf(Color.White, Color.White))
                }
                drawRoundRect(brush, Offset(left, 0f), Size(width * fill, size.height), radius)
            }
        }
    }
}

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = (r1 + m).coerceIn(0f, 1f), green = (g1 + m).coerceIn(0f, 1f), blue = (b1 + m).coerceIn(0f, 1f))
}

@Composable
private fun WrappedBackdrop(covers: List<String>, accent: Color, intro: Boolean) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "wrapped-ambient-blur")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient-time"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient-pulse"
    )

    val rad = Math.toRadians(time.toDouble())
    val sinT = Math.sin(rad).toFloat()
    val cosT = Math.cos(rad).toFloat()
    val sin2T = Math.sin(rad * 2.0).toFloat()
    val cos2T = Math.cos(rad * 2.0).toFloat()

    // Apple Music Replay continuously shifting vibrant hues (HSV color wheel rotation)
    val color1 = remember(time) { hsvColor((time * 0.85f + 330f) % 360f, 0.88f, 0.95f) } // Neon magenta / electric rose
    val color2 = remember(time) { hsvColor((time * 0.70f + 195f) % 360f, 0.85f, 0.92f) } // Bright cyan / electric blue
    val color3 = remember(time) { hsvColor((time * 0.90f + 40f) % 360f, 0.92f, 0.98f) }  // Vibrant sunset amber / coral
    val color4 = remember(time) { hsvColor((time * 0.75f + 265f) % 360f, 0.86f, 0.90f) } // Deep royal violet / indigo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040508))
    ) {
        // Living dynamic ambient blur canvas with multi-orb fluid mesh gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base gradient wash
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color4.copy(alpha = 0.35f),
                        Color(0xFF040508),
                        color1.copy(alpha = 0.28f)
                    )
                )
            )

            // Orb 1 (Top-Left drifting glowing sphere)
            val orb1 = Offset(w * (0.28f + 0.18f * cosT), h * (0.22f + 0.14f * sinT))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1.copy(alpha = 0.65f), color1.copy(alpha = 0.25f), Color.Transparent),
                    center = orb1,
                    radius = (w * 0.82f) * pulse
                ),
                center = orb1,
                radius = (w * 0.82f) * pulse
            )

            // Orb 2 (Right-Center drifting glowing sphere)
            val orb2 = Offset(w * (0.78f - 0.20f * sinT), h * (0.46f + 0.16f * cosT))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2.copy(alpha = 0.60f), color2.copy(alpha = 0.20f), Color.Transparent),
                    center = orb2,
                    radius = (w * 0.88f) * (2f - pulse)
                ),
                center = orb2,
                radius = (w * 0.88f) * (2f - pulse)
            )

            // Orb 3 (Bottom-Left drifting glowing sphere)
            val orb3 = Offset(w * (0.34f + 0.20f * sin2T), h * (0.78f - 0.14f * cos2T))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3.copy(alpha = 0.55f), color3.copy(alpha = 0.18f), Color.Transparent),
                    center = orb3,
                    radius = (w * 0.76f) * pulse
                ),
                center = orb3,
                radius = (w * 0.76f) * pulse
            )

            // Orb 4 (Center Accent floating orb)
            val orb4 = Offset(w * (0.50f - 0.16f * cos2T), h * (0.38f + 0.18f * sin2T))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color4.copy(alpha = 0.52f), color4.copy(alpha = 0.15f), Color.Transparent),
                    center = orb4,
                    radius = (w * 0.70f)
                ),
                center = orb4,
                radius = (w * 0.70f)
            )
        }

        // Subdued album mosaic overlay for personalized music texture
        if (covers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (intro) 0.12f else 0.06f }
            ) {
                repeat(4) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(3) { column ->
                            val url = covers[(row * 3 + column + row) % covers.size]
                            val request = remember(context, url) {
                                ImageRequest.Builder(context).data(DerbisArtworkCache.large(url)).crossfade(true).build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }

        // Apple Music dark frosted glass overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF040508).copy(alpha = 0.40f),
                        0.45f to Color(0xFF040508).copy(alpha = 0.58f),
                        1f to Color(0xFF040508).copy(alpha = 0.88f)
                    )
                )
        )
    }
}

@Composable
private fun WrappedPageContent(
    page: WrappedPage,
    year: Int,
    snapshot: ListeningInsightsSnapshot,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean,
    onReceiptStyleChange: (ListeningReceiptStyle) -> Unit = {}
) {
    when (page) {
        WrappedPage.Intro -> WrappedIntro(year, strings, accent)
        WrappedPage.Minutes -> WrappedMinutes(snapshot, strings, locale, accent, animationsEnabled)
        WrappedPage.Artist -> WrappedArtist(snapshot.topArtists, strings, locale, accent)
        WrappedPage.Tracks -> WrappedTracks(snapshot.topTracks, strings, accent)
        WrappedPage.Rhythm -> WrappedRhythm(snapshot, strings, locale, accent)
        WrappedPage.Finale -> WrappedFinale(
            year = year,
            snapshot = snapshot,
            strings = strings,
            locale = locale,
            accent = accent,
            onReceiptStyleChange = onReceiptStyleChange
        )
    }
}

@Composable
private fun WrappedIntro(year: Int, strings: DerbisStrings, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        WrappedTag(
            text = "REPLAY",
            accent = accent,
            icon = painterResource(R.drawable.ic_sound_wave)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = strings.wrappedYearInMusic.uppercase(),
            color = WrappedMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.2.sp
        )
        Text(
            text = year.toString(),
            color = WrappedInk,
            fontSize = wrappedSp(120f),
            lineHeight = wrappedSp(118f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-4.5).sp,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = strings.wrappedEntrySubtitle,
            color = WrappedMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(wrappedDp(36.dp)))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.wrappedTapToBegin,
                color = WrappedInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = WrappedInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WrappedMinutes(
    snapshot: ListeningInsightsSnapshot,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val decimal = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val minutes = snapshot.metrics.listenedMs.coerceAtLeast(0L) / 60_000L
    val hours = snapshot.metrics.listenedMs.coerceAtLeast(0L) / 3_600_000.0
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Centred when it fits, scrollable when it does not, so short viewports never
            // clip the trend chip or the hero number.
            .verticalScroll(rememberScrollState())
            .heightIn(min = LocalWrappedViewportHeight.current)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        WrappedTag(
            text = "TOTAL LISTENING",
            accent = accent,
            icon = painterResource(R.drawable.ic_sound_wave)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = strings.wrappedMinutesTitle,
            color = WrappedInk,
            fontSize = wrappedSp(28f),
            lineHeight = wrappedSp(32f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountUpText(
                    target = minutes,
                    format = number,
                    animate = animationsEnabled
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.wrappedMinutesUnit.uppercase(locale),
                    color = accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = strings.wrappedHours.replace("%s", decimal.format(hours)),
            color = WrappedMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        snapshot.metrics.trendPercent?.let { trend ->
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (trend >= 0) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    val template = if (trend >= 0) strings.wrappedTrendUp else strings.wrappedTrendDown
                    Text(
                        text = template.replace("%s", number.format(abs(trend))),
                        color = WrappedInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CountUpText(target: Long, format: NumberFormat, animate: Boolean, modifier: Modifier = Modifier) {
    val value = remember(target) { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) value.animateTo(target.toFloat(), tween(1_400, easing = FastOutSlowInEasing)) else value.snapTo(target.toFloat())
    }
    Text(
        text = format.format(value.value.toLong()),
        color = WrappedInk,
        fontSize = wrappedSp(84f),
        lineHeight = wrappedSp(88f),
        fontWeight = FontWeight.Black,
        letterSpacing = (-3).sp,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

@Composable
private fun WrappedArtist(
    artists: List<ListeningInsightsArtist>,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color
) {
    val context = LocalContext.current
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val named = artists.filter { it.name.isNotBlank() }
    val top = named.firstOrNull() ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .heightIn(min = LocalWrappedViewportHeight.current)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        WrappedTag(
            text = "TOP ARTIST",
            accent = accent,
            icon = Icons.Rounded.AutoAwesome,
            tint = Color(0xFFFFD700)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = strings.wrappedArtistTitle,
            color = WrappedInk,
            fontSize = wrappedSp(28f),
            lineHeight = wrappedSp(32f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(wrappedDp(210.dp))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, DerbisViolet)))
                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ListeningReceiptModel.initial(top.name),
                color = WrappedOnAccent,
                fontSize = wrappedSp(80f),
                fontWeight = FontWeight.Black
            )
            if (top.artworkUrl.isNotBlank()) {
                val request = remember(context, top.artworkUrl) {
                    ImageRequest.Builder(context).data(DerbisArtworkCache.large(top.artworkUrl)).crossfade(true).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = top.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = top.name,
            color = WrappedInk,
            fontSize = wrappedSp(38f),
            lineHeight = wrappedSp(42f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (top.plays > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${number.format(top.plays)} ${strings.statPlays}",
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(20.dp))
        named.drop(1).take(2).forEachIndexed { index, artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${index + 2}",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    text = artist.name,
                    color = WrappedInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WrappedTracks(tracks: List<ListeningInsightsTrack>, strings: DerbisStrings, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .heightIn(min = LocalWrappedViewportHeight.current)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        WrappedTag(
            text = "TOP SONGS",
            accent = accent,
            icon = painterResource(R.drawable.ic_sound_wave)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.wrappedTracksTitle,
            color = WrappedInk,
            fontSize = wrappedSp(28f),
            lineHeight = wrappedSp(32f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(18.dp))
        tracks.take(ListeningReceiptModel.MAX_TRACKS).forEachIndexed { index, track ->
            WrappedTrackRow(rank = index + 1, track = track, accent = accent)
        }
    }
}

@Composable
private fun WrappedTrackRow(rank: Int, track: ListeningInsightsTrack, accent: Color) {
    val first = rank == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(if (first) 22.dp else 16.dp))
            .background(if (first) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (first) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(if (first) 22.dp else 16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = if (first) accent else WrappedMuted,
            fontSize = if (first) 22.sp else 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp)
        )
        WrappedTrackCover(track = track, accent = accent, size = if (first) 56.dp else 46.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = track.title,
                color = WrappedInk,
                fontSize = if (first) 17.sp else 15.sp,
                fontWeight = if (first) FontWeight.Black else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    color = WrappedMuted,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (first) {
            Icon(
                painter = painterResource(R.drawable.ic_sound_wave),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp).padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun WrappedTrackCover(track: ListeningInsightsTrack, accent: Color, size: Dp) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(accent, DerbisViolet))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ListeningReceiptModel.initial(track.title),
            color = WrappedOnAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        if (track.artworkUrl.isNotBlank()) {
            val request = remember(context, track.artworkUrl) {
                ImageRequest.Builder(context).data(DerbisArtworkCache.large(track.artworkUrl)).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WrappedRhythm(
    snapshot: ListeningInsightsSnapshot,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val buckets = snapshot.hourBuckets
    val peak = snapshot.metrics.peakHour.takeIf { it in 0..23 } ?: buckets.indices.maxByOrNull { buckets[it] } ?: -1
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .heightIn(min = LocalWrappedViewportHeight.current)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        WrappedTag(
            text = "LISTENING RHYTHM",
            accent = accent,
            icon = painterResource(R.drawable.ic_sound_wave)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.wrappedRhythmTitle,
            color = WrappedInk,
            fontSize = wrappedSp(28f),
            lineHeight = wrappedSp(32f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        if (peak >= 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.wrappedPeakHour.replace("%s", String.format(locale, "%02d:00", peak)),
                color = WrappedMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(28.dp))
        WrappedHourChart(buckets = buckets, peak = peak, accent = accent)
        Spacer(Modifier.height(28.dp))
        WrappedStatTiles(
            stats = listOf(
                number.format(snapshot.metrics.countedPlays) to strings.statPlays,
                number.format(snapshot.metrics.distinctTracks) to strings.statTracks,
                number.format(snapshot.metrics.distinctArtists) to strings.statArtists
            )
        )
    }
}

@Composable
private fun WrappedHourChart(buckets: List<Long>, peak: Int, accent: Color) {
    val max = (buckets.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val count = buckets.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        buckets.forEachIndexed { hour, countVal ->
            val ratio = (countVal / max).coerceIn(0.08f, 1f)
            val barHeight = size.height * ratio
            val left = hour * (barWidth + gap)
            val top = size.height - barHeight
            val isPeak = hour == peak
            val brush = if (isPeak) {
                Brush.verticalGradient(listOf(accent, Color.White), top, size.height)
            } else {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.15f)), top, size.height)
            }
            drawRoundRect(brush, Offset(left, top), Size(barWidth, barHeight), radius)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("00", "06", "12", "18", "23").forEach {
            Text(text = it, color = WrappedMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WrappedStatTiles(stats: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (value, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text(text = value, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(text = label, color = WrappedMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WrappedFinale(
    year: Int,
    snapshot: ListeningInsightsSnapshot,
    strings: DerbisStrings,
    locale: Locale,
    accent: Color,
    onReceiptStyleChange: (ListeningReceiptStyle) -> Unit = {}
) {
    val content = remember(snapshot, strings, locale, year) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        ListeningReceiptModel.content(
            snapshot = snapshot,
            labels = receiptLabels(strings, strings.wrappedReceiptTitle, year.toString(), locale),
            locale = locale,
            nowMs = now,
            zone = zone,
            range = ListeningInsightsRanges.year(year, now, zone)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.wrappedFinaleTitle,
            color = WrappedInk,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        ListeningReceiptComposer(
            content = content,
            loading = false,
            accent = accent,
            textColor = WrappedInk,
            mutedColor = WrappedMuted,
            previewMaxHeight = 460.dp,
            onStyleChange = onReceiptStyleChange
        )
    }
}
