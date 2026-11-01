package com.yasuda.derbis.ui.player

/**
 * Controls shared by more than one player surface.
 *
 * These lived inside the immersive "Beat Sync" deck until it was retired. The output volume
 * slider is used by the Now Playing sheet, the Apple Music deck and the audio-route sheet, so it
 * moved here instead of going with that deck.
 */

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign

private val PlayerInk = Color.White
private val PlayerInkSoft = Color.White.copy(alpha = 0.64f)
private val PlayerBadge = Color.White.copy(alpha = 0.14f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerOutputVolume(
    contentColor: Color = PlayerInk,
    mutedColor: Color = PlayerInkSoft,
    activeColor: Color = PlayerInk,
    trackColor: Color = PlayerBadge,
    condensed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val label = LocalDerbisStrings.current.volume
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max) }
    DisposableEffect(audio) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                level = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val apply: (Float) -> Unit = { fraction ->
        level = fraction
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).toInt().coerceIn(0, max), 0)
    }
    val sliderColors = SliderDefaults.colors(
        thumbColor = activeColor,
        activeTrackColor = activeColor,
        inactiveTrackColor = trackColor
    )
    val sliderState = rememberSliderState(value = level.coerceIn(0f, 1f), trackRange = 0f..1f)
    LaunchedEffect(level) {
        sliderState.value = level.coerceIn(0f, 1f)
    }
    if (condensed) {
        Row(
            modifier = modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = mutedColor, modifier = Modifier.size(19.dp))
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    sliderState.value = value
                    apply(value)
                },
                colors = sliderColors,
                thumb = { Box(Modifier.size(8.dp).background(activeColor, CircleShape)) },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors,
                        modifier = Modifier.height(6.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 4.dp,
                        trackInsideCornerSize = 3.dp
                    )
                },
                modifier = Modifier.weight(1f).height(DerbisPlayerDesign.MinimumTouchTarget)
                    .semantics { contentDescription = label }
            )
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = mutedColor, modifier = Modifier.size(19.dp))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = mutedColor, modifier = Modifier.size(20.dp))
                Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${(level * 100).toInt()}%", color = mutedColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    sliderState.value = value
                    apply(value)
                },
                colors = sliderColors,
                thumb = {
                    Box(Modifier.size(width = 4.dp, height = 40.dp).background(activeColor, CircleShape))
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors,
                        modifier = Modifier.height(32.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 4.dp,
                        trackInsideCornerSize = 4.dp
                    )
                },
                modifier = Modifier.fillMaxWidth().height(DerbisPlayerDesign.MinimumTouchTarget)
                    .semantics { contentDescription = label }
            )
        }
    }
}
