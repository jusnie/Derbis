package com.yasuda.derbis.ui.lyrics

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Share
import com.yasuda.derbis.ui.icons.Download
import com.yasuda.derbis.ui.icons.Check
import com.yasuda.derbis.ui.icons.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_ASPECT = 4f / 5f
private val PREVIEW_MAX_HEIGHT = 420.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsShareSheet(
    snapshot: LyricsShareSnapshot,
    onDismiss: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val context = LocalContext.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val style = LyricsShareCardStyle.GRADIENT
    var selectedBackgroundIndex by rememberSaveable { mutableIntStateOf(0) }
    var mode by remember(snapshot) { mutableStateOf(LyricsShareTextMode.ORIGINAL) }
    val artwork = remember(snapshot) { LyricsShareResource<Bitmap>(Bitmap::recycle) }
    var accents by remember(snapshot) { mutableStateOf<Pair<Int, Int>?>(null) }
    var previewState by remember(snapshot) { mutableStateOf<LyricsSharePreviewState<Bitmap>?>(null) }
    var exportRequest by remember(snapshot) { mutableIntStateOf(0) }
    var saveRequest by remember(snapshot) { mutableIntStateOf(0) }
    var exporting by remember(snapshot) { mutableStateOf(false) }
    var saving by remember(snapshot) { mutableStateOf(false) }
    var saved by remember(snapshot) { mutableStateOf(false) }
    var exportFailed by remember(snapshot) { mutableStateOf(false) }
    val content = remember(snapshot, mode) { lyricsShareCardContent(snapshot, mode) }
    val selectedBackground = ReferenceShareBackgrounds[selectedBackgroundIndex.coerceIn(ReferenceShareBackgrounds.indices)]
    val cardAccents = accents?.let { referenceShareAccents(selectedBackground, it) }
    val previewKey = remember(content, style, cardAccents) {
        LyricsSharePreviewKey(content, style, cardAccents ?: (0 to 0))
    }
    val preview = previewState.readyFor(previewKey)?.takeUnless(Bitmap::isRecycled)
    val previewFailed = previewState.failedFor(previewKey)

    val latestPreviewState by rememberUpdatedState(previewState)
    DisposableEffect(artwork) {
        onDispose {
            artwork.close()
            val latest = latestPreviewState
            if (latest is LyricsSharePreviewState.Ready) latest.image.recycle()
        }
    }

    LaunchedEffect(artwork) {
        LyricsShareCard.loadArtwork(context, snapshot.track, artwork)
        accents = withContext(Dispatchers.Default) {
            artwork.use { cover -> LyricsShareCard.resolveAccents(snapshot.track, cover) }
        }
    }

    LaunchedEffect(previewKey, cardAccents) {
        previewState = null
        val resolvedAccents = cardAccents ?: return@LaunchedEffect
        var rendered: Bitmap? = null
        try {
            withContext(Dispatchers.Default) {
                rendered = artwork.use { cover ->
                    LyricsShareCard.render(
                        content = previewKey.content,
                        artwork = cover,
                        style = previewKey.style,
                        accents = resolvedAccents,
                        widthPx = LyricsShareCard.PREVIEW_WIDTH_PX
                    )
                }
            }
            val bitmap = rendered
            previewState = if (bitmap == null) {
                LyricsSharePreviewState.Failed(previewKey)
            } else {
                rendered = null
                LyricsSharePreviewState.Ready(previewKey, bitmap)
            }
        } finally {
            rendered?.recycle()
        }
    }

    LaunchedEffect(exportRequest) {
        if (exportRequest == 0) return@LaunchedEffect
        val resolvedAccents = cardAccents ?: return@LaunchedEffect
        exporting = true
        exportFailed = false
        try {
            val intent = LyricsShareCard.createShareIntent(
                context = context,
                content = content,
                artwork = artwork,
                style = style,
                accents = resolvedAccents
            )
            exportFailed = intent == null || !startChooser(context, intent, strings.shareVia)
        } finally {
            exporting = false
        }
    }

    LaunchedEffect(saveRequest) {
        if (saveRequest == 0) return@LaunchedEffect
        val resolvedAccents = cardAccents ?: return@LaunchedEffect
        saving = true
        saved = false
        try {
            saved = LyricsShareCard.saveToGallery(
                context = context,
                content = content,
                artwork = artwork,
                style = style,
                accents = resolvedAccents
            )
        } finally {
            saving = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF00313A),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = strings.close,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = strings.shareLyrics,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Box(Modifier.size(48.dp))
            }

            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SharePreview(
                    preview = preview,
                    failed = previewFailed,
                    description = strings.shareLyricsPreviewDescription,
                    failedText = strings.shareLyricsFailed,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Text(
                text = "Background",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReferenceShareBackgrounds.forEachIndexed { index, color ->
                    Surface(
                        color = Color(color),
                        shape = CircleShape,
                        border = if (index == selectedBackgroundIndex) {
                            BorderStroke(3.dp, Color.White)
                        } else {
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .selectable(
                                selected = index == selectedBackgroundIndex,
                                role = Role.RadioButton,
                                onClick = { selectedBackgroundIndex = index }
                            )
                    ) {}
                }
            }
            if (exportFailed) {
                Text(
                    text = strings.shareLyricsFailed,
                    color = Color(0xFFFFA6AE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { saveRequest += 1 },
                    enabled = cardAccents != null && !saving && content.lyrics.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.42f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(
                            imageVector = if (saved) Icons.Rounded.Check else Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (saved) strings.saved else strings.save,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { exportRequest += 1 },
                    enabled = cardAccents != null && !exporting && content.lyrics.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF102026),
                        disabledContainerColor = Color.White.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF102026).copy(alpha = 0.56f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp)
                ) {
                    if (exporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF102026))
                    } else {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = strings.share,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private val ReferenceShareBackgrounds = listOf(
    0xFF148A98.toInt(),
    0xFF202126.toInt(),
    0xFF427866.toInt(),
    0xFF8B434C.toInt(),
    0xFFBBB4A5.toInt()
)

private fun referenceShareAccents(background: Int, fallback: Pair<Int, Int>): Pair<Int, Int> {
    val start = LyricsShareCardColors.background(background, fallback.first)
    return start to start
}

@Composable
private fun SharePreview(
    preview: Bitmap?,
    failed: Boolean,
    description: String,
    failedText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(max = PREVIEW_MAX_HEIGHT)
                .aspectRatio(PREVIEW_ASPECT)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                preview != null -> SharePreviewImage(preview, description)
                failed -> Text(
                    text = failedText,
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = DerbisCyan
                )
            }
        }
    }
}

@Composable
private fun SharePreviewImage(bitmap: Bitmap, description: String) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    DisposableEffect(bitmap) {
        onDispose { bitmap.recycle() }
    }
    Image(
        bitmap = image,
        contentDescription = description,
        modifier = Modifier.fillMaxSize()
    )
}


private fun startChooser(context: android.content.Context, intent: Intent, title: String): Boolean = try {
    context.startActivity(Intent.createChooser(intent, title))
    true
} catch (_: ActivityNotFoundException) {
    false
}
