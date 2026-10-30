package com.yasuda.derbis.ui.sharing

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.data.PlaybackSourceIdentity
import com.yasuda.derbis.ui.components.DerbisVectorLogo
import com.yasuda.derbis.ui.components.ModernChevronRightVector
import com.yasuda.derbis.ui.components.ModernCopyLinkVector
import com.yasuda.derbis.ui.components.ModernInstagramStoryVector
import com.yasuda.derbis.ui.components.ModernListenTogetherVector
import com.yasuda.derbis.ui.components.ModernSaveGalleryVector
import com.yasuda.derbis.ui.components.ModernShareSparkleVector
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SheetBackground = Color(0xFF0C0D12)
private val CardSurface = Color(0xFF161720)
private val MutedText = Color(0xFFA2A4B2)
private val InstagramPink = Color(0xFFE1306C)
private val SavedGreen = Color(0xFF4BB543)
private val CopyCyan = Color(0xFF26B2D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackStoryShareSheet(
    track: Track,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    onOpenJam: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = LocalDerbisStrings.current

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    var selectedFormat by remember { mutableStateOf(TrackStoryFormat.STORY_9_16) }
    var selectedTheme by remember { mutableStateOf(TrackStoryTheme.AURA_GLOW) }

    var artworkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var palette by remember { mutableStateOf<TrackStoryPalette?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(true) }

    var isSharing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }
    var linkCopied by remember { mutableStateOf(false) }

    val watchUrl = remember(track) {
        val videoId = PlaybackSourceIdentity.sourceVideoId(track)
        if (videoId.isNotBlank()) "https://derbis-web.vercel.app/t/$videoId"
        else if (track.videoUrl.isNotBlank()) track.videoUrl
        else track.streamUrl
    }

    val snapshot = remember(track, currentPositionMs, durationMs, watchUrl) {
        TrackStorySnapshot(
            track = track,
            positionMs = currentPositionMs,
            durationMs = durationMs,
            qualityTier = "LOSSLESS OPUS • 48 kHz",
            engineLabel = "DERBIS AUDIO ENGINE",
            watchUrl = watchUrl
        )
    }

    val isInstagramInstalled = remember { TrackStoryCard.isInstagramInstalled(context) }

    // 1. Load cover artwork and compute palette
    LaunchedEffect(track) {
        withContext(Dispatchers.IO) {
            val loaded = TrackStoryCard.loadArtwork(context, track)
            val resolvedPalette = TrackStoryCard.resolvePalette(track, loaded)
            withContext(Dispatchers.Main) {
                artworkBitmap = loaded
                palette = resolvedPalette
            }
        }
    }

    // 2. Render live preview whenever format, theme, or artwork updates
    LaunchedEffect(selectedFormat, selectedTheme, artworkBitmap, palette) {
        val currentPalette = palette ?: return@LaunchedEffect
        isRenderingPreview = true
        withContext(Dispatchers.Default) {
            val preview = TrackStoryCard.render(
                snapshot = snapshot,
                artwork = artworkBitmap,
                format = selectedFormat,
                theme = selectedTheme,
                palette = currentPalette,
                widthPx = TrackStoryCard.PREVIEW_WIDTH_PX
            )
            withContext(Dispatchers.Main) {
                val old = previewBitmap
                previewBitmap = preview
                if (old != null && old !== preview) {
                    old.recycle()
                }
                isRenderingPreview = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            artworkBitmap?.recycle()
            previewBitmap?.recycle()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = DerbisPlayerDesign.DockTrayCorner,
            topEnd = DerbisPlayerDesign.DockTrayCorner
        ),
        containerColor = SheetBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StorySheetGrabber()

            StorySheetHeader(
                title = strings.share,
                meta = "${selectedFormat.widthPx} × ${selectedFormat.heightPx}",
                closeLabel = strings.close,
                onClose = onDismiss
            )

            StoryPreviewCard(
                bitmap = previewBitmap,
                ratio = selectedFormat.aspectRatio,
                busy = isRenderingPreview,
                glow = palette?.dominant?.let { Color(it) },
                description = "${track.title} · ${selectedFormat.label}"
            )

            StoryFormatSelector(
                selected = selectedFormat,
                onSelect = { selectedFormat = it }
            )

            StoryThemeSelector(
                selected = selectedTheme,
                palette = palette,
                onSelect = { selectedTheme = it }
            )

            StoryPrimaryButton(
                label = "${strings.share} ${selectedFormat.label}",
                busyLabel = strings.shareLyricsPreparing,
                busy = isSharing,
                enabled = palette != null,
                onClick = {
                    val currentPalette = palette
                    if (currentPalette != null && !isSharing) {
                        isSharing = true
                        scope.launch {
                            val intent = TrackStoryCard.createShareIntent(
                                context = context,
                                snapshot = snapshot,
                                artwork = artworkBitmap,
                                format = selectedFormat,
                                theme = selectedTheme,
                                palette = currentPalette
                            )
                            isSharing = false
                            if (intent != null) {
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(intent, strings.share)
                                    )
                                }
                                onDismiss()
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            if (isInstagramInstalled) {
                StorySecondaryButton(
                    label = "Instagram Stories",
                    icon = ModernInstagramStoryVector,
                    tint = InstagramPink,
                    container = Color(0xFF161722),
                    borderColor = InstagramPink.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = palette != null && !isSharing,
                    onClick = {
                        val currentPalette = palette
                        if (currentPalette != null && !isSharing) {
                            isSharing = true
                            scope.launch {
                                val intent = TrackStoryCard.createInstagramStoryIntent(
                                    context = context,
                                    snapshot = snapshot,
                                    artwork = artworkBitmap,
                                    format = selectedFormat,
                                    theme = selectedTheme,
                                    palette = currentPalette
                                )
                                isSharing = false
                                if (intent != null) {
                                    runCatching { context.startActivity(intent) }
                                    onDismiss()
                                }
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StorySecondaryButton(
                    label = if (savedSuccess) strings.saved else strings.save,
                    icon = ModernSaveGalleryVector,
                    tint = if (savedSuccess) SavedGreen else Color.White,
                    modifier = Modifier.weight(1f),
                    busy = isSaving,
                    enabled = palette != null,
                    onClick = {
                        val currentPalette = palette
                        if (currentPalette != null && !isSaving) {
                            isSaving = true
                            scope.launch {
                                val success = TrackStoryCard.saveToGallery(
                                    context = context,
                                    snapshot = snapshot,
                                    artwork = artworkBitmap,
                                    format = selectedFormat,
                                    theme = selectedTheme,
                                    palette = currentPalette
                                )
                                isSaving = false
                                savedSuccess = success
                            }
                        }
                    }
                )

                StorySecondaryButton(
                    label = if (linkCopied) strings.storyLinkCopied else strings.storyCopyLink,
                    icon = ModernCopyLinkVector,
                    tint = if (linkCopied) CopyCyan else Color.White,
                    modifier = Modifier.weight(1f),
                    enabled = snapshot.watchUrl.isNotBlank(),
                    onClick = {
                        if (snapshot.watchUrl.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(snapshot.watchUrl))
                            linkCopied = true
                        }
                    }
                )
            }

            if (onOpenJam != null) {
                Spacer(Modifier.height(10.dp))
                StorySecondaryButton(
                    label = strings.jamListenTogether,
                    icon = ModernListenTogetherVector,
                    container = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismiss()
                        onOpenJam()
                    }
                )
            }
        }
    }
}

/** Small pill that replaces the default sheet handle so the sheet still reads as draggable. */
@Composable
private fun StorySheetGrabber() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 14.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
    )
}

@Composable
private fun StorySheetHeader(
    title: String,
    meta: String,
    closeLabel: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DerbisVectorLogo(size = 26.dp, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = closeLabel,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Preview stage. The card is fitted into a fixed box instead of a fixed height, so switching
 * between 9:16, 4:5 and 1:1 changes the frame a little instead of snapping the whole sheet.
 */
@Composable
private fun StoryPreviewCard(
    bitmap: Bitmap?,
    ratio: Float,
    busy: Boolean,
    glow: Color?,
    description: String
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        val stageHeight = 336.dp
        val stageWidth = 296.dp
        val cardHeight = minOf(stageHeight, maxWidth / ratio, stageWidth / ratio)
        val cardWidth = cardHeight * ratio
        val glowColor = (glow ?: Color.White).copy(alpha = 0.30f)

        Box(
            modifier = Modifier
                .size(width = cardWidth * 1.3f, height = cardHeight * 0.9f)
                .blur(48.dp)
                .background(glowColor, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(width = cardWidth, height = cardHeight)
                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.8f))
                .clip(RoundedCornerShape(22.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val preview = bitmap
            if (preview != null && !preview.isRecycled) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (busy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    StoryShimmer(modifier = Modifier.fillMaxSize())
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.4.dp
                    )
                }
            }
        }
    }
}

/** Diagonal light sweep shown while a new frame renders — calmer than a flat scrim. */
@Composable
private fun StoryShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "story-shimmer")
    val sweep by transition.animateFloat(
        initialValue = -0.9f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "story-shimmer-sweep"
    )
    Canvas(modifier = modifier) {
        val x = size.width * sweep
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                start = Offset(x - size.width * 0.4f, 0f),
                end = Offset(x, size.height)
            )
        )
    }
}

@Composable
private fun StoryFormatSelector(
    selected: TrackStoryFormat,
    onSelect: (TrackStoryFormat) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TrackStoryFormat.entries.forEach { format ->
            val isSelected = format == selected
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF08080C) else MutedText,
                animationSpec = tween(180),
                label = "format-content"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(format) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = format.icon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = format.label,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = format.ratioLabel,
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StoryThemeSelector(
    selected: TrackStoryTheme,
    palette: TrackStoryPalette?,
    onSelect: (TrackStoryTheme) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrackStoryTheme.entries.forEach { theme ->
            val isSelected = theme == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF1E202B) else CardSurface,
                animationSpec = tween(180),
                label = "theme-background"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
                animationSpec = tween(180),
                label = "theme-border"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
                    .background(background)
                    .clickable { onSelect(theme) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // A tiny version of the theme itself reads faster than a colour dot.
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(themeSwatch(theme, palette))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = theme.label,
                    color = if (isSelected) Color.White else MutedText,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun themeSwatch(theme: TrackStoryTheme, palette: TrackStoryPalette?): Brush = when (theme) {
    TrackStoryTheme.AURA_GLOW -> Brush.linearGradient(
        listOf(
            palette?.dominant?.let { Color(it) } ?: Color(0xFF26B2D6),
            palette?.accent?.let { Color(it) } ?: Color(0xFF7A5CFF)
        )
    )
    TrackStoryTheme.OLED_BLACK -> Brush.linearGradient(
        listOf(Color(0xFF050507), Color(0xFF2A2A3A))
    )
    TrackStoryTheme.VINYL_STUDIO -> Brush.linearGradient(
        listOf(Color(0xFF2C2C35), Color(0xFF8A8A99))
    )
}

@Composable
private fun StoryPrimaryButton(
    label: String,
    busyLabel: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .clickable(enabled = enabled && !busy, onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(27.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = busyLabel,
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Icon(
                        imageVector = ModernShareSparkleVector,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = ModernChevronRightVector,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorySecondaryButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    container: Color = CardSurface,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    busy: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled && !busy, onClick = onClick),
        color = container,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = tint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
