package com.yasuda.derbis.ui.sharing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.data.PlaybackSourceIdentity
import com.yasuda.derbis.data.youtubeWatchUrl
import com.yasuda.derbis.ui.components.DerbisVectorLogo
import com.yasuda.derbis.ui.components.ModernChevronRightVector
import com.yasuda.derbis.ui.components.ModernCopyLinkVector
import com.yasuda.derbis.ui.components.ModernInstagramStoryVector
import com.yasuda.derbis.ui.components.ModernListenTogetherVector
import com.yasuda.derbis.ui.components.ModernSaveGalleryVector
import com.yasuda.derbis.ui.components.ModernShareSparkleVector
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SheetBackground = Color(0xFF0C0D12)
private val CardSurface = Color(0xFF161720)
private val MutedText = Color(0xFFA2A4B2)

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DerbisVectorLogo(size = 28.dp, tint = Color.White)
                    Column {
                        Text(
                            text = "Share Story & Card",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "High-Res Studio Export",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E202B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 14.sp)
                }
            }

            // Live Interactive Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ambient backlight glow matching artwork colors
                val glowColor = palette?.dominant?.let { Color(it).copy(alpha = 0.35f) } ?: Color.White.copy(alpha = 0.1f)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .blur(50.dp)
                        .background(glowColor, CircleShape)
                )

                // The rendered preview container
                Box(
                    modifier = Modifier
                        .height(340.dp)
                        .aspectRatio(selectedFormat.aspectRatio)
                        .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.8f))
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val preview = previewBitmap
                    if (preview != null && !preview.isRecycled) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Track Story Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isRenderingPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }

            // Format Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrackStoryFormat.entries.forEach { format ->
                    val isSelected = selectedFormat == format
                    val bg = if (isSelected) Color.White else Color.Transparent
                    val textColor = if (isSelected) Color(0xFF08080C) else MutedText
                    val iconColor = if (isSelected) Color(0xFF08080C) else MutedText

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable { selectedFormat = format }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = format.icon(),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${format.label} (${format.ratioLabel})",
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Theme Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackStoryTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    val borderColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f)
                    val bgColor = if (isSelected) Color(0xFF1E202B) else CardSurface

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .clickable { selectedTheme = theme }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Visual Dot Swatch
                        val dotColor = when (theme) {
                            TrackStoryTheme.AURA_GLOW -> palette?.dominant?.let { Color(it) } ?: Color(0xFF26B2D6)
                            TrackStoryTheme.OLED_BLACK -> Color.White
                            TrackStoryTheme.VINYL_STUDIO -> Color(0xFF888896)
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = theme.label,
                            color = if (isSelected) Color.White else MutedText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Action Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Share Story / Universal Image
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .clickable(enabled = !isSharing) {
                            val currentPalette = palette ?: return@clickable
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
                                    try {
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share Track Story")
                                        )
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch share", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                    color = Color.White,
                    shape = RoundedCornerShape(27.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Preparing Story Card...",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = ModernShareSparkleVector,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Share ${selectedFormat.label} Card",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = ModernChevronRightVector,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Instagram Stories direct option (if Instagram is installed)
                if (isInstagramInstalled) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .border(1.2.dp, Color(0xFFE1306C).copy(alpha = 0.5f), RoundedCornerShape(25.dp))
                            .clickable(enabled = !isSharing) {
                                val currentPalette = palette ?: return@clickable
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
                                        try {
                                            context.startActivity(intent)
                                            onDismiss()
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, "Instagram not found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                        color = Color(0xFF161722),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ModernInstagramStoryVector,
                                contentDescription = null,
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Share to Instagram Stories",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Secondary Actions Row: Save to Gallery & Copy Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save to Gallery Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                            .clickable(enabled = !isSaving) {
                                val currentPalette = palette ?: return@clickable
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
                                    if (success) {
                                        savedSuccess = true
                                        Toast.makeText(context, "Saved to Photos & Gallery!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        color = CardSurface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = ModernSaveGalleryVector,
                                    contentDescription = null,
                                    tint = if (savedSuccess) Color(0xFF4BB543) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (savedSuccess) "Saved!" else "Save Image",
                                    color = if (savedSuccess) Color(0xFF4BB543) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Copy Link Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                            .clickable {
                                if (snapshot.watchUrl.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(snapshot.watchUrl))
                                    linkCopied = true
                                    Toast.makeText(context, "Song link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        color = CardSurface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ModernCopyLinkVector,
                                contentDescription = null,
                                tint = if (linkCopied) Color(0xFF26B2D6) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (linkCopied) "Link Copied!" else "Copy Link",
                                color = if (linkCopied) Color(0xFF26B2D6) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Listen Together in Jam Option
                if (onOpenJam != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                            .clickable {
                                onDismiss()
                                onOpenJam()
                            },
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ModernListenTogetherVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listen Together in Jam",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
