/*
 * The Liquid Glass "Background" sheet from the glass-skin mock, shaped as an inline settings
 * section for the Design category: mode picker, intensity/blur/brightness sliders with per-mode
 * gating, background motion switch, accent touch pad with swatches and "match artwork" auto, and
 * the custom image picker/removal. Real settings persisted through DerbisInterfaceSettings.
 */
package com.yasuda.derbis.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisBackgroundMode
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntOffset
import java.io.File
import kotlin.math.roundToInt

private val SheetCard = Color.White.copy(alpha = 0.06f)
private val SheetCardStrong = Color.White.copy(alpha = 0.10f)
private val SheetLine = Color.White.copy(alpha = 0.14f)
private val SheetDim = Color(0xFFA1A1AA)
private val SheetInk = Color(0xFFF5F5F7)

private fun glassAccentFor(settings: DerbisInterfaceSettings): Color {
    val hue = settings.glassAccentHue
    return if (hue >= 0) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.72f, 0.60f)))
    } else {
        Color(0xFF8B5CF6)
    }
}

@Composable
internal fun GlassBackgroundSettingsSection(
    settings: DerbisInterfaceSettings,
    onSettings: (DerbisInterfaceSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importFailed by rememberSaveable { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val target = File(context.filesDir, "glass_background.jpg")
            val ok = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(target).use { output -> input.copyTo(output) }
                } ?: error("Cannot read the picked file")
            }.isSuccess
            if (ok) {
                importFailed = false
                onSettings(settings.copy(glassBackgroundMode = DerbisBackgroundMode.CustomImage, glassBackgroundImage = target.absolutePath))
            } else {
                importFailed = true
            }
        }
    }

    val mode = settings.glassBackgroundMode
    val showIntensity = mode == DerbisBackgroundMode.Glow || mode == DerbisBackgroundMode.Animated || mode == DerbisBackgroundMode.ArtworkBlur
    val showBlur = mode == DerbisBackgroundMode.ArtworkBlur || mode == DerbisBackgroundMode.CustomImage
    val showAccent = mode != DerbisBackgroundMode.CustomImage
    val showImageControls = mode == DerbisBackgroundMode.CustomImage

    val sectionCard: @Composable (content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) -> Unit = { content ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SheetCard)
                .border(1.dp, SheetLine, RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) { content() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Background",
            color = SheetInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose how the black backdrop looks.",
            color = SheetDim,
            fontSize = 12.5.sp
        )

        sectionCard {
            // Mode segmented control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(4.dp)
            ) {
                DerbisBackgroundMode.entries.forEach { option ->
                    val selected = option == mode
                    val accent = glassAccentFor(settings)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent else Color.Transparent)
                            .clickable {
                                onSettings(settings.copy(glassBackgroundMode = option))
                                if (option == DerbisBackgroundMode.CustomImage && settings.glassBackgroundImage.isBlank()) {
                                    pickImage.launch(arrayOf("image/*"))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            color = if (selected) Color.Black else SheetDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (showImageControls) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetButton(
                        text = if (settings.glassBackgroundImage.isNotBlank()) "Change image" else "Choose image",
                        primary = true
                    ) { pickImage.launch(arrayOf("image/*")) }
                    SheetButton(
                        text = "Remove",
                        primary = false,
                        enabled = settings.glassBackgroundImage.isNotBlank()
                    ) {
                        runCatching { File(settings.glassBackgroundImage).delete() }
                        onSettings(settings.copy(glassBackgroundImage = "", glassBackgroundMode = DerbisBackgroundMode.Glow))
                    }
                }
                if (importFailed) {
                    Spacer(Modifier.height(6.dp))
                    Text("Couldn't load that image. Try another one.", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }

            if (showIntensity) {
                SheetSlider(
                    label = "Intensity",
                    value = settings.glassBackgroundIntensity,
                    range = 20..100,
                    suffix = "%"
                ) { onSettings(settings.copy(glassBackgroundIntensity = it)) }
            }
            if (showBlur) {
                SheetSlider(
                    label = "Blur",
                    value = settings.glassBackgroundBlur,
                    range = 0..80,
                    suffix = "px"
                ) { onSettings(settings.copy(glassBackgroundBlur = it)) }
            }
            SheetSlider(
                label = "Brightness",
                value = settings.glassBackgroundBrightness,
                range = 15..100,
                suffix = "%"
            ) { onSettings(settings.copy(glassBackgroundBrightness = it)) }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Background motion",
                    color = SheetInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.glassBackgroundMotion,
                    onCheckedChange = { onSettings(settings.copy(glassBackgroundMotion = it)) }
                )
            }

            if (showAccent) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Accent color",
                    color = SheetInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                AccentTouchPad(settings, onSettings)
                Spacer(Modifier.height(10.dp))
                AccentSwatches(settings, onSettings)
            }
        }
    }
}

@Composable
private fun SheetButton(
    text: String,
    primary: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Color.White else SheetCardStrong)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) Color.Black else SheetInk,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SheetSlider(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    onValue: (Int) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SheetInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(78.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
            )
        )
        Text(
            text = "$value$suffix",
            color = SheetDim,
            fontSize = 12.sp,
            modifier = Modifier.width(38.dp)
        )
    }
}

/** 2D hue/lightness pad: horizontal axis hue 0..360, vertical axis 80..25 lightness. */
@Composable
private fun AccentTouchPad(
    settings: DerbisInterfaceSettings,
    onSettings: (DerbisInterfaceSettings) -> Unit
) {
    val hue = (settings.glassAccentHue.takeIf { it >= 0 } ?: 265).toFloat()
    val lightness = settings.glassAccentLightness.toFloat()

    fun padSet(x: Float, y: Float, padWidth: Float, padHeight: Float) {
        val h = ((x / padWidth).coerceIn(0f, 1f) * 360f).roundToInt()
        val l = (80f - (y / padHeight).coerceIn(0f, 1f) * 55f).roundToInt().coerceIn(25, 80)
        onSettings(settings.copy(glassAccentHue = h, glassAccentLightness = l))
    }

    var padSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val hueBrush = Brush.horizontalGradient(
        colors = listOf(0, 60, 120, 180, 240, 300, 360).map { degrees ->
            Color(android.graphics.Color.HSVToColor(floatArrayOf(degrees.toFloat(), 0.85f, 0.55f)))
        }
    )
    val lumaOverlay = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.78f), Color.Transparent, Color.Black.copy(alpha = 0.70f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(hueBrush)
            .background(lumaOverlay)
            .border(1.dp, SheetLine, RoundedCornerShape(18.dp))
            .pointerInput(settings.glassAccentHue, settings.glassAccentLightness) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                detectTapGestures { offset -> padSet(offset.x, offset.y, w, h) }
            }
            .pointerInput(settings.glassAccentHue, settings.glassAccentLightness) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                detectDragGestures { change: PointerInputChange, _ ->
                    change.consume()
                    padSet(change.position.x, change.position.y, w, h)
                }
            }
            .onGloballyPositioned { coordinates -> padSize = coordinates.size.toSize() }
    ) {
        if (padSize != androidx.compose.ui.geometry.Size.Zero) {
            val w = padSize.width
            val h = padSize.height
            val x = (hue / 360f) * w
            val y = ((80f - lightness) / 55f) * h
            Box(
                modifier = Modifier
                    .offset { IntOffset((x - 13f).roundToInt(), (y - 13f).roundToInt()) }
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, lightness / 100f)))
                    )
                    .border(3.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun AccentSwatches(
    settings: DerbisInterfaceSettings,
    onSettings: (DerbisInterfaceSettings) -> Unit
) {
    val swatches = listOf(
        0 to 55, 28 to 55, 50 to 52, 150 to 45, 190 to 50, 265 to 62, 330 to 58
    )
    val auto = settings.glassAccentHue < 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        // Auto — follow the artwork/theme accent
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFF97316), Color(0xFFFACC15), Color(0xFF34D399),
                            Color(0xFF22D3EE), Color(0xFF8B5CF6), Color(0xFFF43F5E), Color(0xFFF97316)
                        )
                    )
                )
                .border(if (auto) 3.dp else 1.5.dp, if (auto) Color.White else SheetLine, CircleShape)
                .clickable { onSettings(settings.copy(glassAccentHue = -1)) }
        )
        swatches.forEach { (swatchHue, swatchLightness) ->
            val selected = settings.glassAccentHue == swatchHue && settings.glassAccentLightness == swatchLightness
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(swatchHue.toFloat(), 0.85f, swatchLightness / 100f)))
                    )
                    .border(if (selected) 3.dp else 1.5.dp, if (selected) Color.White else SheetLine, CircleShape)
                    .clickable {
                        onSettings(
                            settings.copy(
                                glassAccentHue = swatchHue,
                                glassAccentLightness = swatchLightness
                            )
                        )
                    }
            )
        }
    }
}
