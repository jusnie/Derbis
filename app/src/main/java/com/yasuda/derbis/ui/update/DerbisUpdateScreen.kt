package com.yasuda.derbis.ui.update

import com.yasuda.derbis.ui.components.DerbisBackButton
import com.yasuda.derbis.ui.pixelPaperGrid
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.yasuda.derbis.ui.derbisSafeTopPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.AppUpdateInfo
import com.yasuda.derbis.ui.DerbisLogoMark
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.components.derbisGroupedListShape
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.DerbisUpdateReleaseCopy
import com.yasuda.derbis.ui.i18n.updateReleaseCopy
import com.yasuda.derbis.ui.i18n.updateReleaseHighlights
import com.yasuda.derbis.ui.theme.DerbisBlack
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisGlassBorder
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisOnAccent
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm
import com.yasuda.derbis.ui.theme.DerbisViolet
import com.yasuda.derbis.update.formatUpdateBytes
import com.yasuda.derbis.update.updateUnitsFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val UpdateBadgeSpinMillis = 24_000

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DerbisUpdateScreen(
    update: AppUpdateInfo,
    strings: DerbisStrings,
    languageCode: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = strings.updateReleaseCopy()
    val noteSections = remember(strings.code, update.latestVersionName, update.releaseNotes) {
        derbisUpdateNoteSections(update.releaseNotes, update.latestVersionName).ifEmpty {
            listOf(
                DerbisUpdateNoteSection(
                    title = "",
                    entries = strings.updateReleaseHighlights(update.latestVersionName).take(5)
                )
            )
        }
    }
    val meta = remember(update.publishedAtEpochMs, update.assetSizeBytes, languageCode) {
        updateMetaLine(update.publishedAtEpochMs, update.assetSizeBytes, languageCode)
    }

    val pixel = com.yasuda.derbis.ui.theme.DerbisIsPixel
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DerbisBlack)
            .then(
                if (pixel) {
                    Modifier.pixelPaperGrid()
                } else {
                    Modifier.drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(DerbisCyan.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.04f),
                                radius = size.maxDimension * 0.6f
                            )
                        )
                    }
                }
            )
            .derbisSafeTopPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DerbisBackButton(onClick = onLater, contentDescription = strings.later)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            UpdateBadge()
            Spacer(Modifier.height(28.dp))
            Text(
                text = copy.releaseReady,
                color = DerbisCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = update.latestVersionName,
                color = DerbisText,
                fontSize = 64.sp,
                lineHeight = DerbisTypeRhythm.lineHeight(64.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (meta.isNotBlank()) {
                Text(meta, color = DerbisMuted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(22.dp))
            VersionRunway(
                current = update.currentVersionName,
                latest = update.latestVersionName,
                copy = copy
            )
            if (noteSections.any { it.entries.isNotEmpty() }) {
                Spacer(Modifier.height(30.dp))
                UpdateHighlights(title = copy.releaseHighlights, sections = noteSections)
            }
            Spacer(Modifier.height(22.dp))
            UpdateProtectionCard(title = copy.releaseProtection, detail = copy.releaseProtectionDetail)
            Spacer(Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onUpdate,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = DerbisOnAccent),
                contentPadding = PaddingValues(horizontal = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(strings.update, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            TextButton(
                onClick = onLater,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(strings.later, color = DerbisMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateBadge() {
    if (com.yasuda.derbis.ui.theme.DerbisIsPixel) {
        val p = com.yasuda.derbis.ui.theme.PixelPalette
        val rect = androidx.compose.ui.graphics.RectangleShape
        Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(p.ink, rect)
                    .border(com.yasuda.derbis.ui.theme.PixelDesign.border, p.outline, rect),
                contentAlignment = Alignment.Center
            ) {
                DerbisLogoMark(size = 64.dp, contentDescription = null)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(38.dp)
                    .background(p.cyan, rect)
                    .border(com.yasuda.derbis.ui.theme.PixelDesign.border, p.text, rect),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = p.ink, modifier = Modifier.size(18.dp))
            }
        }
        return
    }
    val animated = LocalAnimationsEnabled.current
    val bloom = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) bloom.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 160f)) else bloom.snapTo(1f)
    }
    val spin = if (animated) {
        rememberInfiniteTransition(label = "update-badge").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(UpdateBadgeSpinMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "update-badge-spin"
        )
    } else {
        null
    }
    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    val value = bloom.value
                    scaleX = 0.6f + 0.4f * value
                    scaleY = 0.6f + 0.4f * value
                    alpha = value.coerceIn(0f, 1f)
                    rotationZ = -(spin?.value ?: 0f) - 50f * (1f - value)
                }
                .border(1.5.dp, DerbisCyan.copy(alpha = 0.45f), MaterialShapes.SoftBurst.toShape())
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    val value = bloom.value
                    scaleX = 0.7f + 0.3f * value
                    scaleY = 0.7f + 0.3f * value
                    rotationZ = (spin?.value ?: 0f) * 1.4f + 70f * (1f - value)
                }
                .background(
                    Brush.linearGradient(listOf(DerbisCyan.copy(alpha = 0.22f), DerbisViolet.copy(alpha = 0.22f))),
                    MaterialShapes.Cookie12Sided.toShape()
                )
        )
        Box(
            modifier = Modifier.graphicsLayer {
                val value = bloom.value
                scaleX = 0.75f + 0.25f * value
                scaleY = 0.75f + 0.25f * value
                alpha = value.coerceIn(0f, 1f)
            }
        ) {
            DerbisLogoMark(size = 72.dp, contentDescription = null)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .graphicsLayer {
                    val value = ((bloom.value - 0.4f) / 0.6f).coerceIn(0f, 1.2f)
                    scaleX = value
                    scaleY = value
                }
                .background(DerbisBlack, MaterialShapes.Cookie9Sided.toShape())
                .padding(3.dp)
                .background(DerbisCyan, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = DerbisOnAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionRunway(current: String, latest: String, copy: DerbisUpdateReleaseCopy) {
    val pixel = com.yasuda.derbis.ui.theme.DerbisIsPixel
    val rect = androidx.compose.ui.graphics.RectangleShape
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (pixel) 8.dp else 4.dp)
    ) {
        VersionCell(
            label = copy.releaseFrom,
            version = current,
            container = if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.ink else DerbisPanel,
            labelColor = DerbisMuted,
            shape = if (pixel) rect else RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier
                .weight(1f)
                .then(
                    if (pixel) Modifier.border(com.yasuda.derbis.ui.theme.PixelDesign.border, com.yasuda.derbis.ui.theme.PixelPalette.outline, rect)
                    else Modifier
                )
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.ink else DerbisPanel,
                    if (pixel) rect else MaterialShapes.Cookie4Sided.toShape()
                )
                .then(
                    if (pixel) Modifier.border(com.yasuda.derbis.ui.theme.PixelDesign.border, com.yasuda.derbis.ui.theme.PixelPalette.outline, rect)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = DerbisCyan, modifier = Modifier.size(18.dp))
        }
        VersionCell(
            label = copy.releaseTo,
            version = latest,
            container = if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.panelSoft else DerbisCyan.copy(alpha = 0.16f),
            labelColor = DerbisCyan,
            shape = if (pixel) rect else RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 26.dp, bottomEnd = 26.dp),
            modifier = Modifier
                .weight(1f)
                .then(
                    if (pixel) Modifier.border(com.yasuda.derbis.ui.theme.PixelDesign.border, com.yasuda.derbis.ui.theme.PixelPalette.cyan, rect)
                    else Modifier
                )
        )
    }
}

@Composable
private fun VersionCell(
    label: String,
    version: String,
    container: Color,
    labelColor: Color,
    shape: Shape,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(container, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(version, color = DerbisText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val CommitPrefix = Regex("^([0-9a-fA-F]{7,40})\\s*(?:[—:-]\\s*)?(.+)$")

private fun splitCommitPrefix(note: String): Pair<String?, String> {
    val match = CommitPrefix.matchEntire(note.trim()) ?: return null to note
    return match.groupValues[1] to match.groupValues[2].trim()
}

@Composable
private fun UpdateHighlights(title: String, sections: List<DerbisUpdateNoteSection>) {
    Column {
        Text(
            text = title,
            color = DerbisText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() }
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            sections.forEach { section ->
                if (section.title.isNotBlank()) {
                    Text(
                        text = section.title,
                        color = DerbisCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(if (com.yasuda.derbis.ui.theme.DerbisIsPixel) 6.dp else 3.dp)) {
                    section.entries.forEachIndexed { index, note ->
                        val pixel = com.yasuda.derbis.ui.theme.DerbisIsPixel
                        val rect = androidx.compose.ui.graphics.RectangleShape
                        val (commit, body) = splitCommitPrefix(note)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.ink else DerbisPanel,
                                    derbisGroupedListShape(index, section.entries.size)
                                )
                                .then(
                                    if (pixel) Modifier.border(com.yasuda.derbis.ui.theme.PixelDesign.border, com.yasuda.derbis.ui.theme.PixelPalette.outline, rect)
                                    else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (commit == null) 34.dp else 68.dp)
                                    .height(34.dp)
                                    .background(
                                        if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.panelSoft else DerbisCyan.copy(alpha = 0.16f),
                                        if (pixel) rect else RoundedCornerShape(9.dp)
                                    )
                                    .then(
                                        if (pixel) Modifier.border(1.5.dp, com.yasuda.derbis.ui.theme.PixelPalette.cyan, rect)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = commit ?: (index + 1).toString(),
                                    color = DerbisCyan,
                                    fontSize = if (commit == null) 14.sp else 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            Text(
                                text = body,
                                color = DerbisText.copy(alpha = 0.86f),
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateProtectionCard(title: String, detail: String) {
    val pixel = com.yasuda.derbis.ui.theme.DerbisIsPixel
    val cardShape = if (pixel) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (pixel) com.yasuda.derbis.ui.theme.PixelPalette.ink else DerbisCyan.copy(alpha = 0.08f),
                cardShape
            )
            .border(
                if (pixel) com.yasuda.derbis.ui.theme.PixelDesign.border else 1.dp,
                DerbisGlassBorder,
                cardShape
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    DerbisCyan,
                    if (pixel) androidx.compose.ui.graphics.RectangleShape else MaterialShapes.Clover4Leaf.toShape()
                )
                .then(
                    if (pixel) Modifier.border(com.yasuda.derbis.ui.theme.PixelDesign.border, com.yasuda.derbis.ui.theme.PixelPalette.text, androidx.compose.ui.graphics.RectangleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = DerbisOnAccent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DerbisMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

internal fun updateMetaLine(publishedAtEpochMs: Long, assetSizeBytes: Long, languageCode: String): String {
    val parts = mutableListOf<String>()
    formatUpdateReleaseDate(publishedAtEpochMs, languageCode)?.let(parts::add)
    if (assetSizeBytes > 0L) parts += formatUpdateBytes(assetSizeBytes, updateUnitsFor(languageCode))
    return parts.joinToString(separator = " · ")
}

internal fun formatUpdateReleaseDate(publishedAtEpochMs: Long, languageCode: String): String? {
    if (publishedAtEpochMs <= 0L) return null
    return runCatching {
        val locale = Locale.forLanguageTag(languageCode.ifBlank { "en" })
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(publishedAtEpochMs))
    }.getOrNull()
}
