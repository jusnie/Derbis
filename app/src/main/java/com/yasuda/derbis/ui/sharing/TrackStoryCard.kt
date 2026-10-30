package com.yasuda.derbis.ui.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.yasuda.derbis.data.ArtworkPaletteCache
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.artwork.ArtworkMediaStore
import com.yasuda.derbis.ui.lyrics.isRtlText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance client-side image generator for Track Stories and Social Cards.
 * Renders high-resolution bitmaps directly using Android's native Canvas and FileProvider.
 *
 * Supports Instagram Stories (9:16), Social Feed Posts (4:5), and Square Cards (1:1).
 */
internal object TrackStoryCard {
    const val EXPORT_WIDTH_PX = 1080
    const val PREVIEW_WIDTH_PX = 540

    private const val MARGIN_STORY_X = 96f
    private const val MARGIN_CARD_X = 84f
    private const val BRAND_TEXT = "DERBIS"
    private const val ARTWORK_TARGET_SIZE = 900
    private const val ARTWORK_TIMEOUT_MS = 6_000L
    private const val MAX_CACHE_FILES = 8
    private const val MAX_CACHE_AGE_MS = 24L * 60L * 60L * 1000L
    private const val SHARE_DIRECTORY = "share/story"

    private const val FALLBACK_DOMINANT = 0xFF183B56.toInt()
    private const val FALLBACK_ACCENT = 0xFF26B2D6.toInt()

    fun heightFor(widthPx: Int, format: TrackStoryFormat): Int =
        (widthPx * format.heightPx / format.widthPx)

    /**
     * Resolves the highest quality cover artwork for the track.
     */
    suspend fun loadArtwork(context: Context, track: Track): Bitmap? = withContext(Dispatchers.IO) {
        ensureActive()
        // 1. Try local cached high-res / regular file first
        val localFile = DerbisArtworkCache.localFile(context, track, highRes = true)
            ?: DerbisArtworkCache.localFile(context, track, highRes = false)
        if (localFile != null && localFile.isFile) {
            val decoded = decodeCover(localFile)
            if (decoded != null) return@withContext decoded
        }

        // 2. Fall back to Coil loader to fetch remote candidate
        val candidates = DerbisArtworkCache.artworkUrlCandidates(track, highRes = true)
        val loader = SingletonImageLoader.get(context)
        for (url in candidates) {
            ensureActive()
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(ARTWORK_TARGET_SIZE, ARTWORK_TARGET_SIZE))
                .allowHardware(false)
                .build()
            val result = withTimeoutOrNull(ARTWORK_TIMEOUT_MS) { loader.execute(request) }
            if (result is SuccessResult) {
                val bmp = result.image.toBitmap()
                if (bmp.width > 0 && bmp.height > 0) return@withContext bmp
            }
        }
        null
    }

    /**
     * Extracts vibrant colors from the artwork to build the card's palette.
     */
    fun resolvePalette(track: Track, artwork: Bitmap?): TrackStoryPalette {
        val start = track.accentStart.takeIf { it != 0 } ?: FALLBACK_DOMINANT
        val end = track.accentEnd.takeIf { it != 0 } ?: FALLBACK_ACCENT
        val palette = artwork?.let {
            ArtworkPaletteCache.extract(bitmap = it, fallbackStart = start, fallbackEnd = end)
        }
        val dominant = palette?.start ?: start
        val accent = palette?.end ?: end
        return TrackStoryPalette(
            dominant = dominant,
            accent = accent,
            darkBase = 0xFF08080C.toInt(),
            lightAccent = 0xFFFFFFFF.toInt()
        )
    }

    /**
     * Creates an Intent to share via Android's universal system share sheet.
     */
    suspend fun createShareIntent(
        context: Context,
        snapshot: TrackStorySnapshot,
        artwork: Bitmap?,
        format: TrackStoryFormat,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette
    ): Intent? = withContext(Dispatchers.IO) {
        val file = exportToFile(context, snapshot, artwork, format, theme, palette) ?: return@withContext null
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
        } catch (e: Exception) {
            Timber.w(e, "Failed to get FileProvider URI for story card")
            file.delete()
            return@withContext null
        }

        val caption = buildString {
            append("Now playing on Derbis:\n")
            append(snapshot.track.title)
            if (snapshot.track.artist.isNotBlank()) append(" — ").append(snapshot.track.artist)
            if (snapshot.watchUrl.isNotBlank()) append("\n").append(snapshot.watchUrl)
        }

        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "${snapshot.track.title} — ${snapshot.track.artist}")
            clipData = ClipData.newUri(context.contentResolver, "story_card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Checks if the Instagram application is installed.
     */
    fun isInstagramInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.instagram.android", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates an Intent to post directly into Instagram Stories.
     */
    suspend fun createInstagramStoryIntent(
        context: Context,
        snapshot: TrackStorySnapshot,
        artwork: Bitmap?,
        format: TrackStoryFormat,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette
    ): Intent? = withContext(Dispatchers.IO) {
        val file = exportToFile(context, snapshot, artwork, format, theme, palette) ?: return@withContext null
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
        } catch (e: Exception) {
            Timber.w(e, "Failed to get FileProvider URI for Instagram Story")
            file.delete()
            return@withContext null
        }

        val topHex = "#" + Integer.toHexString(palette.dominant and 0x00FFFFFF).padStart(6, '0')
        val bottomHex = "#" + Integer.toHexString(palette.darkBase and 0x00FFFFFF).padStart(6, '0')

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            type = "image/png"
            putExtra("interactive_asset_uri", uri)
            putExtra("top_background_color", topHex)
            putExtra("bottom_background_color", bottomHex)
            if (snapshot.watchUrl.isNotBlank()) {
                putExtra("content_url", snapshot.watchUrl)
            }
            clipData = ClipData.newUri(context.contentResolver, "instagram_story", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent
    }

    /**
     * Saves the high-res card to device storage / Photos / Pictures/Derbis.
     */
    suspend fun saveToGallery(
        context: Context,
        snapshot: TrackStorySnapshot,
        artwork: Bitmap?,
        format: TrackStoryFormat,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = "derbis-${snapshot.track.title.take(18).filter { it.isLetterOrDigit() }}-${System.currentTimeMillis()}.png"
        ArtworkMediaStore.save(
            context = context,
            fileName = fileName,
            mimeType = "image/png"
        ) { output ->
            val bitmap = render(snapshot, artwork, format, theme, palette, EXPORT_WIDTH_PX) ?: return@save false
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } finally {
                bitmap.recycle()
            }
        }
    }

    private suspend fun exportToFile(
        context: Context,
        snapshot: TrackStorySnapshot,
        artwork: Bitmap?,
        format: TrackStoryFormat,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette
    ): File? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
        prune(directory)

        val file = File(directory, "derbis-story-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        try {
            ensureActive()
            bitmap = render(snapshot, artwork, format, theme, palette, EXPORT_WIDTH_PX)
            if (bitmap == null) return@withContext null

            val written = FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (written && file.length() > 0L) file else null
        } catch (cancelled: CancellationException) {
            file.delete()
            throw cancelled
        } catch (e: IOException) {
            Timber.w(e, "Story export failed")
            file.delete()
            null
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Renders the complete story card on an Android Canvas.
     */
    fun render(
        snapshot: TrackStorySnapshot,
        artwork: Bitmap?,
        format: TrackStoryFormat,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette,
        widthPx: Int
    ): Bitmap? {
        val designWidth = format.widthPx.toFloat()
        val designHeight = format.heightPx.toFloat()
        val heightPx = heightFor(widthPx, format)

        val bitmap = try {
            Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return null
        }
        var completed = false
        try {
            val canvas = Canvas(bitmap)
            val scale = widthPx.toFloat() / designWidth
            canvas.scale(scale, scale)

            // 1. Draw theme-specific living backdrop
            drawBackdrop(canvas, designWidth, designHeight, theme, palette, artwork)

            // 2. Draw Top Bar (Derbis Vector Mark + Brand Name + Quality Pill)
            drawTopHeader(canvas, designWidth, format, snapshot, palette)

            // 3. Draw Hero Cover / Vinyl Artwork Presentation
            val coverBounds = calculateCoverBounds(designWidth, designHeight, format, theme)
            if (theme == TrackStoryTheme.VINYL_STUDIO) {
                drawVinylDisc(canvas, coverBounds, artwork)
            }
            drawHeroCover(canvas, coverBounds, artwork, theme, palette)

            // 4. Draw Typography (Title, Artist, Audio Specs)
            val textTop = coverBounds.bottom + if (format == TrackStoryFormat.STORY_9_16) 72f else 48f
            drawMetadata(canvas, designWidth, textTop, snapshot, format)

            // 5. Draw Stylized Audio Waveform / Progress Bars
            val waveTop = if (format == TrackStoryFormat.STORY_9_16) {
                designHeight - 340f
            } else if (format == TrackStoryFormat.CARD_4_5) {
                designHeight - 240f
            } else {
                designHeight - 190f
            }
            drawWaveformAndProgress(canvas, designWidth, waveTop, snapshot, format, palette)

            // 6. Draw Bottom Stamp / Watermark
            drawBottomStamp(canvas, designWidth, designHeight, format, palette)

            completed = true
            return bitmap
        } finally {
            if (!completed) bitmap.recycle()
        }
    }

    private fun drawBackdrop(
        canvas: Canvas,
        width: Float,
        height: Float,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette,
        artwork: Bitmap?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (theme) {
            TrackStoryTheme.OLED_BLACK -> {
                // 100% OLED Pure Black
                canvas.drawColor(Color.BLACK)

                // Ultra subtle luminous ambient halo behind the artwork center
                paint.shader = RadialGradient(
                    width * 0.5f,
                    height * 0.42f,
                    width * 0.65f,
                    withAlpha(palette.dominant, 48),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }

            TrackStoryTheme.VINYL_STUDIO -> {
                // Sleek charcoal studio gradient
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height,
                    0xFF121218.toInt(),
                    0xFF060608.toInt(),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)

                // Soft dynamic light bloom on the left
                paint.shader = RadialGradient(
                    width * 0.35f,
                    height * 0.40f,
                    width * 0.8f,
                    withAlpha(palette.dominant, 65),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }

            TrackStoryTheme.AURA_GLOW -> {
                // Rich multi-orb living aura
                canvas.drawColor(palette.darkBase)

                // Top dominant light pool
                paint.shader = RadialGradient(
                    width * 0.80f,
                    height * 0.15f,
                    width * 0.85f,
                    withAlpha(palette.dominant, 130),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)

                // Center vibrant hero bloom
                paint.shader = RadialGradient(
                    width * 0.30f,
                    height * 0.46f,
                    width * 0.90f,
                    withAlpha(palette.accent, 150),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)

                // Lower counter-glow
                paint.shader = RadialGradient(
                    width * 0.65f,
                    height * 0.82f,
                    width * 0.70f,
                    withAlpha(palette.dominant, 90),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)

                // Deep perimeter vignette for readability
                paint.shader = RadialGradient(
                    width * 0.5f,
                    height * 0.5f,
                    height * 0.65f,
                    Color.TRANSPARENT,
                    Color.argb(135, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }
        }
    }

    private fun drawTopHeader(
        canvas: Canvas,
        width: Float,
        format: TrackStoryFormat,
        snapshot: TrackStorySnapshot,
        palette: TrackStoryPalette
    ) {
        val marginX = if (format == TrackStoryFormat.STORY_9_16) MARGIN_STORY_X else MARGIN_CARD_X
        val topY = if (format == TrackStoryFormat.STORY_9_16) 130f else 80f

        // Draw Derbis 3-diamond mark
        drawDerbisDiamondLogo(canvas, marginX, topY, 44f, Color.WHITE)

        // Draw Derbis Brand Wordmark
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.24f
        }
        canvas.drawText(BRAND_TEXT, marginX + 62f, topY + 33f, brandPaint)

        // Draw Audio Quality Badge on the right
        val badgeText = "LOSSLESS OPUS"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val textWidth = textPaint.measureText(badgeText)
        val badgePaddingX = 26f
        val badgeHeight = 44f
        val badgeWidth = textWidth + badgePaddingX * 2 + 36f
        val badgeRight = width - marginX
        val badgeLeft = badgeRight - badgeWidth
        val badgeTop = topY - 2f
        val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeTop + badgeHeight)

        // Pill backdrop
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, pillPaint)

        // Pill border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, borderPaint)

        // 7-bar audio wave icon inside the badge
        drawAudioWaveIcon(canvas, badgeLeft + 18f, badgeTop + badgeHeight / 2f, 22f, Color.WHITE)

        // Text inside badge
        canvas.drawText(badgeText, badgeLeft + 48f, badgeTop + 30f, textPaint)
    }

    private fun calculateCoverBounds(
        width: Float,
        height: Float,
        format: TrackStoryFormat,
        theme: TrackStoryTheme
    ): RectF {
        val marginX = if (format == TrackStoryFormat.STORY_9_16) MARGIN_STORY_X else MARGIN_CARD_X
        return when (format) {
            TrackStoryFormat.STORY_9_16 -> {
                val coverSize = if (theme == TrackStoryTheme.VINYL_STUDIO) 660f else 820f
                val left = if (theme == TrackStoryTheme.VINYL_STUDIO) marginX + 30f else (width - coverSize) / 2f
                val top = 310f
                RectF(left, top, left + coverSize, top + coverSize)
            }
            TrackStoryFormat.CARD_4_5 -> {
                val coverSize = if (theme == TrackStoryTheme.VINYL_STUDIO) 540f else 680f
                val left = if (theme == TrackStoryTheme.VINYL_STUDIO) marginX + 24f else (width - coverSize) / 2f
                val top = 190f
                RectF(left, top, left + coverSize, top + coverSize)
            }
            TrackStoryFormat.SQUARE_1_1 -> {
                val coverSize = if (theme == TrackStoryTheme.VINYL_STUDIO) 440f else 540f
                val left = if (theme == TrackStoryTheme.VINYL_STUDIO) marginX + 20f else (width - coverSize) / 2f
                val top = 160f
                RectF(left, top, left + coverSize, top + coverSize)
            }
        }
    }

    private fun drawVinylDisc(canvas: Canvas, coverBounds: RectF, artwork: Bitmap?) {
        val discRadius = coverBounds.width() * 0.46f
        val discCenterX = coverBounds.right + discRadius * 0.38f
        val discCenterY = coverBounds.centerY()

        // Outer vinyl disc body
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF141418.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(discCenterX, discCenterY, discRadius, discPaint)

        // Concentric audio grooves
        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(22, 255, 255, 255)
            strokeWidth = 1.6f
        }
        val grooveSteps = 7
        for (i in 1..grooveSteps) {
            val r = discRadius * (0.42f + (i.toFloat() / grooveSteps.toFloat()) * 0.52f)
            canvas.drawCircle(discCenterX, discCenterY, r, groovePaint)
        }

        // Center label ring
        val labelRadius = discRadius * 0.36f
        val labelRect = RectF(
            discCenterX - labelRadius,
            discCenterY - labelRadius,
            discCenterX + labelRadius,
            discCenterY + labelRadius
        )
        if (artwork != null) {
            val labelPath = Path().apply { addCircle(discCenterX, discCenterY, labelRadius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(labelPath)
            canvas.drawBitmap(artwork, null, labelRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A34.toInt() }
            canvas.drawCircle(discCenterX, discCenterY, labelRadius, labelBg)
        }

        // Center spindle hole
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF08080C.toInt() }
        canvas.drawCircle(discCenterX, discCenterY, discRadius * 0.08f, holePaint)
    }

    private fun drawHeroCover(
        canvas: Canvas,
        target: RectF,
        artwork: Bitmap?,
        theme: TrackStoryTheme,
        palette: TrackStoryPalette
    ) {
        val cornerRadius = 52f

        // Ambient shadow / backlight glow behind the album cover
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val glowColor = if (theme == TrackStoryTheme.OLED_BLACK) {
                withAlpha(palette.dominant, 70)
            } else {
                withAlpha(palette.accent, 120)
            }
            shader = RadialGradient(
                target.centerX(),
                target.centerY() + 30f,
                target.width() * 0.75f,
                glowColor,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        val glowRect = RectF(
            target.left - 60f,
            target.top - 40f,
            target.right + 60f,
            target.bottom + 80f
        )
        canvas.drawRect(glowRect, shadowPaint)

        // Clip and draw cover artwork
        val path = Path().apply { addRoundRect(target, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        if (artwork != null && !artwork.isRecycled) {
            canvas.drawBitmap(artwork, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            // Elegant placeholder
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    target.left, target.top, target.right, target.bottom,
                    palette.dominant, palette.accent, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(target, bgPaint)
            drawDerbisDiamondLogo(canvas, target.centerX() - 40f, target.centerY() - 40f, 80f, Color.WHITE)
        }
        canvas.restore()

        // High-contrast luxury border stroke
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(target, cornerRadius, cornerRadius, border)
    }

    private fun drawMetadata(
        canvas: Canvas,
        width: Float,
        topY: Float,
        snapshot: TrackStorySnapshot,
        format: TrackStoryFormat
    ) {
        val marginX = if (format == TrackStoryFormat.STORY_9_16) MARGIN_STORY_X else MARGIN_CARD_X
        val maxTextWidth = (width - marginX * 2f).toInt()

        // 1. Title
        val titleSize = if (format == TrackStoryFormat.STORY_9_16) 66f else if (format == TrackStoryFormat.CARD_4_5) 54f else 46f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = titleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.015f
        }
        val titleLayout = buildTextLayout(snapshot.track.title.ifBlank { "Untitled Track" }, titlePaint, maxTextWidth, 2)
        canvas.save()
        canvas.translate(marginX, topY)
        titleLayout.draw(canvas)
        canvas.restore()

        val afterTitleY = topY + titleLayout.height + 14f

        // 2. Artist
        val artistSize = if (format == TrackStoryFormat.STORY_9_16) 42f else if (format == TrackStoryFormat.CARD_4_5) 36f else 32f
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 255, 255, 255)
            textSize = artistSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val artistText = snapshot.track.artist.ifBlank { "Unknown Artist" }
        drawEllipsized(canvas, artistText, artistPaint, marginX, afterTitleY + artistSize, maxTextWidth.toFloat())

        // 3. Album & Year (if present)
        val albumText = snapshot.track.album.takeIf { it.isNotBlank() }
        if (albumText != null && format != TrackStoryFormat.SQUARE_1_1) {
            val albumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(140, 255, 255, 255)
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            drawEllipsized(canvas, albumText, albumPaint, marginX, afterTitleY + artistSize + 44f, maxTextWidth.toFloat())
        }
    }

    private fun drawWaveformAndProgress(
        canvas: Canvas,
        width: Float,
        startY: Float,
        snapshot: TrackStorySnapshot,
        format: TrackStoryFormat,
        palette: TrackStoryPalette
    ) {
        val marginX = if (format == TrackStoryFormat.STORY_9_16) MARGIN_STORY_X else MARGIN_CARD_X
        val availableWidth = width - marginX * 2f
        val progress = snapshot.progress

        // 1. Modern Stylized Audio Frequency Waveform (28 bars)
        val barCount = 28
        val gap = 6f
        val barWidth = (availableWidth - (barCount - 1) * gap) / barCount
        val maxBarHeight = 56f
        val waveBaselineY = startY + maxBarHeight

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.FILL
        }

        // Rhythmic wave height patterns
        val wavePattern = floatArrayOf(
            0.35f, 0.55f, 0.85f, 0.40f, 0.70f, 0.95f, 0.60f, 0.78f,
            0.45f, 0.90f, 1.00f, 0.75f, 0.50f, 0.65f, 0.82f, 0.94f,
            0.68f, 0.42f, 0.76f, 0.88f, 0.58f, 0.80f, 0.62f, 0.92f,
            0.74f, 0.50f, 0.68f, 0.38f
        )

        for (i in 0 until barCount) {
            val barX = marginX + i * (barWidth + gap)
            val barFraction = i.toFloat() / barCount.toFloat()
            val height = maxBarHeight * wavePattern[i % wavePattern.size]
            val barTop = waveBaselineY - height
            val rect = RectF(barX, barTop, barX + barWidth, waveBaselineY)
            val isPlayed = barFraction <= progress
            val paint = if (isPlayed) activePaint else inactivePaint
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint)
        }

        // 2. Timestamps Row
        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 255, 255, 255)
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            letterSpacing = 0.05f
        }
        val timeY = waveBaselineY + 42f
        canvas.drawText(snapshot.formattedPosition, marginX, timeY, timePaint)

        val durationWidth = timePaint.measureText(snapshot.formattedDuration)
        canvas.drawText(snapshot.formattedDuration, width - marginX - durationWidth, timeY, timePaint)
    }

    private fun drawBottomStamp(
        canvas: Canvas,
        width: Float,
        height: Float,
        format: TrackStoryFormat,
        palette: TrackStoryPalette
    ) {
        val marginX = if (format == TrackStoryFormat.STORY_9_16) MARGIN_STORY_X else MARGIN_CARD_X
        val bottomY = height - if (format == TrackStoryFormat.STORY_9_16) 110f else 64f

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, 255, 255, 255)
            strokeWidth = 1.8f
        }
        canvas.drawLine(marginX, bottomY - 32f, width - marginX, bottomY - 32f, dividerPaint)

        // Footer Derbis Diamond Vector
        drawDerbisDiamondLogo(canvas, marginX, bottomY - 14f, 28f, Color.WHITE)

        val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.16f
        }
        canvas.drawText("PLAY ON DERBIS", marginX + 42f, bottomY + 8f, stampPaint)

        val audioEngPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        val engText = "HI-RES AUDIO"
        val engWidth = audioEngPaint.measureText(engText)
        canvas.drawText(engText, width - marginX - engWidth, bottomY + 8f, audioEngPaint)
    }

    /**
     * Vectorially renders the Derbis 3-diamond mark with smooth anti-aliased geometry.
     */
    private fun drawDerbisDiamondLogo(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        color: Int
    ) {
        val squareSide = size * 0.1768f
        val radius = size * 0.022f

        val centers = listOf(
            Pair(x + size * 0.4134f, y + size * 0.3380f),
            Pair(x + size * 0.5833f, y + size * 0.4988f),
            Pair(x + size * 0.4134f, y + size * 0.6597f)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        for ((cx, cy) in centers) {
            canvas.save()
            canvas.rotate(45f, cx, cy)
            val rect = RectF(
                cx - squareSide / 2f,
                cy - squareSide / 2f,
                cx + squareSide / 2f,
                cy + squareSide / 2f
            )
            canvas.drawRoundRect(rect, radius, radius, paint)
            canvas.restore()
        }
    }

    /**
     * Draws the modern 7-bar audio wave icon.
     */
    private fun drawAudioWaveIcon(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        height: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2.4f
            strokeCap = Paint.Cap.ROUND
        }
        val barHeights = floatArrayOf(0.35f, 0.60f, 0.85f, 1.0f, 0.75f, 0.50f, 0.30f)
        val spacing = 3.6f
        val startX = centerX - (barHeights.size - 1) * spacing / 2f

        for (i in barHeights.indices) {
            val h = height * barHeights[i]
            val x = startX + i * spacing
            canvas.drawLine(x, centerY - h / 2f, x, centerY + h / 2f, paint)
        }
    }

    private fun buildTextLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(
                if (isRtlText(text)) TextDirectionHeuristics.FIRSTSTRONG_RTL
                else TextDirectionHeuristics.FIRSTSTRONG_LTR
            )
            .setIncludePad(false)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setEllipsizedWidth(width)
            .build()
    }

    private fun drawEllipsized(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        maxWidth: Float
    ) {
        val ellipsized = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipsized, x, y, paint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun decodeCover(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val width = bounds.outWidth.takeIf { it > 0 } ?: return null
        val height = bounds.outHeight.takeIf { it > 0 } ?: return null
        val sample = maxOf(1, minOf(width / ARTWORK_TARGET_SIZE, height / ARTWORK_TARGET_SIZE))
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching { BitmapFactory.decodeFile(file.path, options) }.getOrNull()
    }

    private fun prune(directory: File) {
        val files = directory.listFiles() ?: return
        val now = System.currentTimeMillis()
        val sorted = files.filter { it.isFile }.sortedBy { it.lastModified() }
        for (file in sorted) {
            val expired = now - file.lastModified() > MAX_CACHE_AGE_MS
            val overflow = sorted.size > MAX_CACHE_FILES
            if (expired || overflow) {
                file.delete()
            }
        }
    }
}
