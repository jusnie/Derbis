package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.theme.DerbisIsNocturne
import com.yasuda.derbis.ui.theme.DerbisIsPrism

import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.IconButtonDefaults
import com.yasuda.derbis.ui.components.DerbisExpressiveIconButton
import com.yasuda.derbis.ui.components.derbisExpressiveCorner
import com.yasuda.derbis.ui.components.derbisExpressiveToggleCorner
import com.yasuda.derbis.ui.components.derbisGroupedListShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.role
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yasuda.derbis.domain.AutoEqCatalog
import com.yasuda.derbis.domain.AutoEqCatalogEntry
import com.yasuda.derbis.domain.AutoEqImporter
import com.yasuda.derbis.domain.DerbisAudioQuality
import com.yasuda.derbis.domain.HighQualityAudioMode
import com.yasuda.derbis.domain.Derbis360Experience
import com.yasuda.derbis.domain.DerbisAudioPresets
import com.yasuda.derbis.domain.DerbisAudioSettings
import com.yasuda.derbis.domain.PlaybackBufferMode
import com.yasuda.derbis.domain.PlaybackBufferSettings
import com.yasuda.derbis.domain.ParametricEqualizer
import com.yasuda.derbis.domain.ParametricEqProfile
import com.yasuda.derbis.domain.AudioOffloadPreference
import com.yasuda.derbis.domain.ReplayGainMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.audio.isDerbis360HeadphoneOutput
import com.yasuda.derbis.feature.audio.rememberDerbisAudioOutputState
import com.yasuda.derbis.player.equalPowerCrossfade
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.CrossfadeLabCopy
import com.yasuda.derbis.ui.i18n.crossfadeLabCopy
import com.yasuda.derbis.ui.i18n.localizedAudioPresetLabel
import com.yasuda.derbis.ui.i18n.parametricEqCopy
import com.yasuda.derbis.ui.i18n.parametricProfileCopy
import com.yasuda.derbis.ui.i18n.PlaybackBufferCopy
import com.yasuda.derbis.ui.i18n.playbackBufferCopy
import com.yasuda.derbis.ui.i18n.replayGainCopy
import com.yasuda.derbis.ui.theme.DerbisBlack
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisOrange
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.viewmodel.AutoEqCatalogStatus
import com.yasuda.derbis.viewmodel.AutoEqCatalogUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.text.NumberFormat
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import kotlin.math.abs
import kotlin.math.roundToInt

private val PanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val CardShape = RoundedCornerShape(28.dp)
private val ChipShape = RoundedCornerShape(50)
private val AudioSegmentInnerCorner = 8.dp
internal val AudioPanelSurface: Color get() = if (DerbisIsLight) DerbisPanel else Color(0xFF111214)
internal val AudioCardSurface: Color get() = if (DerbisIsLight) DerbisAdaptiveCard else Color(0xFF1C1D21)
internal val AudioTonalSurface: Color get() = if (DerbisIsLight) DerbisCyan.copy(alpha = 0.14f) else lerp(Color(0xFF1C1D21), DerbisCyan, 0.30f)
internal val AudioSelected: Color get() = if (DerbisIsLight) DerbisCyan else lerp(DerbisCyan, Color.White, 0.42f)
private val AudioSegmentGap = 3.dp
private const val DISABLED_ALPHA = 0.42f
private const val CROSSFADE_CURVE_SAMPLE_COUNT = 49
private val EqualizerHandleRadius = 5.dp

@Composable
internal fun AudioSettingsPanel(
    selected: String,
    currentTrack: Track?,
    audioSettings: DerbisAudioSettings,
    onSelect: (String) -> Unit,
    highQualityAudioMode: HighQualityAudioMode,
    onHighQualityAudioMode: (HighQualityAudioMode) -> Unit,
    onEqualizerEnabled: (Boolean) -> Unit,
    onPreset: (String) -> Unit,
    onBandLevel: (Int, Int) -> Unit,
    onBassBoost: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onDerbis360Enabled: (Boolean) -> Unit,
    onDerbis360Experience: (Derbis360Experience) -> Unit,
    onPreamp: (Float) -> Unit,
    onLimiter: (Boolean) -> Unit,
    onEnhancedAudio: (Boolean) -> Unit = {},
    onCrossfade: (Int) -> Unit,
    onDjSoft: (Boolean) -> Unit,
    onBeatSync: (Boolean) -> Unit,
    onReplayGain: (Boolean) -> Unit,
    onTempo: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onGapless: (Boolean) -> Unit,
    onPreloadNextTrack: (Boolean) -> Unit,
    aaudioOutputAvailable: Boolean,
    onAaudioOutput: (Boolean) -> Unit,
    onAudioOffloadPreference: (AudioOffloadPreference) -> Unit = {},
    onPlaybackBuffer: (PlaybackBufferSettings) -> Unit,
    onResetEqualizer: () -> Unit,
    onApplyAutoEq: (AutoEqImporter.ImportedProfile) -> Unit,
    onSaveAutoEqPreset: (String, AutoEqImporter.ImportedProfile) -> Boolean,
    parametricActions: ParametricProfileActions,
    onApplyParametricAutoEq: (ParametricEqProfile) -> Unit,
    onSaveParametricProfile: (String, ParametricEqProfile) -> Boolean,
    autoEqCatalog: AutoEqCatalogUiState,
    onOpenAutoEqCatalog: () -> Unit,
    onAutoEqCatalogQuery: (String) -> Unit,
    onSelectAutoEqCatalogEntry: (AutoEqCatalogEntry) -> Unit,
    onDismissAutoEqCatalogProfile: () -> Unit,
    onCloseAutoEqCatalog: () -> Unit,
    onReplayGainMode: (ReplayGainMode) -> Unit,
    onReplayGainPreamp: (Float) -> Unit,
    onReplayGainPreventClipping: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val haptics = LocalDerbisHaptics.current
    val replayGainCopy = strings.replayGainCopy()
    val parametricCopy = strings.parametricEqCopy()
    val parametricProfileCopy = strings.parametricProfileCopy()
    val outputState = rememberDerbisAudioOutputState()
    val derbis360OutputAvailable = isDerbis360HeadphoneOutput(outputState.active)
    val bufferCopy = playbackBufferCopy(strings.code)
    val blocker = remember { MutableInteractionSource() }
    val equalizerEnabled = audioSettings.equalizerEnabled
    var showAutoEqImport by remember { mutableStateOf(false) }
    var equalizerMode by rememberSaveable {
        mutableStateOf(
            if (audioSettings.parametricEqualizerEnabled) EqualizerEditorMode.PARAMETRIC else EqualizerEditorMode.GRAPHIC
        )
    }
    LaunchedEffect(audioSettings.equalizerEnabled, audioSettings.parametricEqualizerEnabled) {
        when {
            audioSettings.parametricEqualizerEnabled -> equalizerMode = EqualizerEditorMode.PARAMETRIC
            audioSettings.equalizerEnabled -> equalizerMode = EqualizerEditorMode.GRAPHIC
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DerbisBlack.copy(alpha = 0.62f))
            .clickable(interactionSource = blocker, indication = null) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AudioPanelSurface,
            shape = PanelShape,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
                .clickable(interactionSource = blocker, indication = null) {}
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AudioPanelHandle() }
                item {
                    AudioPanelHeader(
                        title = strings.audioEngine,
                        subtitle = strings.audioEngineSubtitle,
                        volumeLabel = "${outputState.volumePercent}%",
                        closeLabel = strings.close,
                        onClose = onClose
                    )
                }
                item {
                    DerbisOutputHub(
                        output = outputState,
                        track = currentTrack,
                        audioSettings = audioSettings
                    )
                }

                item { AudioSectionLabel(strings.audioSectionQuality) }
                item {
                    AudioQualityRow(
                        // A stale "Auto" from an old backup can never leave the row with no segment lit.
                        selected = DerbisAudioQuality.normalize(selected),
                        labels = listOf(
                            strings.audioQualityLow to DerbisAudioQuality.LOW,
                            strings.audioQualityMedium to DerbisAudioQuality.MEDIUM,
                            strings.audioQualityHigh to DerbisAudioQuality.HIGH,
                            strings.audioQualityLossless to DerbisAudioQuality.LOSSLESS
                        ),
                        onSelect = onSelect
                    )
                }
                item {
                    val qualitySubtitle = when (DerbisAudioQuality.normalize(selected).lowercase()) {
                        "low" -> "Low · 48 kbps data saver (AAC/Opus)"
                        "medium" -> "Medium · 98 – 128 kbps balanced audio"
                        "high" -> "High · 300+ kbps (320 kbps HQ upgrade with highest fallback)"
                        else -> "Lossless · FLAC / ALAC when the source has it; the player badge shows Lossless or Hi-Fi instead of the codec"
                    }
                    Text(
                        text = qualitySubtitle,
                        color = DerbisMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                item {
                    AlternativeAudioCard(
                        title = strings.alternativeAudioTitle,
                        description = strings.alternativeAudioSubtitle,
                        selected = highQualityAudioMode,
                        options = listOf(
                            HighQualityAudioMode.OFF to strings.alternativeAudioOff,
                            HighQualityAudioMode.AUTOMATIC to strings.alternativeAudioAutomatic,
                            HighQualityAudioMode.PREFER_320 to strings.alternativeAudioPrefer320
                        ),
                        onSelect = onHighQualityAudioMode
                    )
                }
                item { AudioSectionLabel(strings.audioSectionEqualizer) }
                item {
                    EqualizerModeSelector(
                        selected = equalizerMode,
                        copy = parametricCopy,
                        onSelect = { equalizerMode = it }
                    )
                }
                if (equalizerMode == EqualizerEditorMode.GRAPHIC) {
                item {
                    val selectedCustomPreset = audioSettings.customPresets
                        .firstOrNull { it.id == audioSettings.presetId }
                    val customCurve = selectedCustomPreset == null &&
                        audioSettings.presetId == DerbisAudioPresets.FLAT &&
                        audioSettings.bandLevels != DerbisAudioPresets.flatLevels
                    AudioCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AudioEqualizerSwitchRow(
                                label = strings.equalizer,
                                subtitle = strings.equalizerSubtitle,
                                checked = equalizerEnabled,
                                onCheckedChange = onEqualizerEnabled
                            )
                            AudioCardHeader(
                                title = strings.preset,
                                trailing = when {
                                    selectedCustomPreset != null -> selectedCustomPreset.fallbackLabel
                                    customCurve -> strings.audioPresetCustom
                                    else -> strings.localizedAudioPresetLabel(
                                        audioSettings.presetId,
                                        DerbisAudioPresets.labelFor(audioSettings.presetId)
                                    )
                                }
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(audioSettings.customPresets, key = { "custom:${it.id}" }) { preset ->
                                    AudioPresetChip(
                                        label = preset.fallbackLabel,
                                        selected = audioSettings.presetId == preset.id,
                                        enabled = true,
                                        onClick = { onPreset(preset.id) }
                                    )
                                }
                                items(DerbisAudioPresets.presets, key = { it.id }) { preset ->
                                    AudioPresetChip(
                                        label = strings.localizedAudioPresetLabel(preset.id, preset.fallbackLabel),
                                        selected = selectedCustomPreset == null &&
                                            !customCurve &&
                                            audioSettings.presetId == preset.id,
                                        enabled = equalizerEnabled,
                                        onClick = { onPreset(preset.id) }
                                    )
                                }
                            }
                            EqualizerCurve(
                                levels = audioSettings.bandLevels,
                                enabled = equalizerEnabled,
                                bandsLabel = strings.audioBands,
                                resetLabel = strings.audioResetEqualizer,
                                onBandLevel = onBandLevel,
                                onReset = onResetEqualizer
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AudioActionButton(
                                    label = strings.autoEqCatalog,
                                    icon = Icons.Rounded.Headphones,
                                    primary = true,
                                    enabled = equalizerEnabled,
                                    onClick = onOpenAutoEqCatalog
                                )
                                AudioActionButton(
                                    label = strings.autoEqImport,
                                    icon = Icons.Rounded.FileOpen,
                                    primary = false,
                                    enabled = equalizerEnabled,
                                    onClick = { showAutoEqImport = true }
                                )
                            }
                        }
                    }
                }
                item {
                    AudioSliderRow(
                        title = strings.preamp,
                        valueLabel = decibels(audioSettings.preampDb),
                        value = audioSettings.preampDb,
                        range = -12f..3f,
                        defaultValue = 0f,
                        onReset = { onPreamp(0f) },
                        onValue = { onPreamp((it * 2f).roundToInt() / 2f) }
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.bassBoost,
                        valueLabel = "${audioSettings.bassBoost}%",
                        value = audioSettings.bassBoost.toFloat(),
                        range = 0f..100f,
                        defaultValue = 0f,
                        onReset = { onBassBoost(0) },
                        onValue = { onBassBoost(it.roundToInt()) }
                    )
                }
                } else {
                    item {
                        ParametricEqualizerCard(
                            enabled = audioSettings.parametricEqualizerEnabled,
                            activeProfile = audioSettings.activeParametricProfile,
                            customProfiles = audioSettings.customParametricProfiles,
                            copy = parametricCopy,
                            profileCopy = parametricProfileCopy,
                            actions = parametricActions
                        )
                    }
                    item {
                        AudioCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AudioActionButton(
                                    label = strings.autoEqCatalog,
                                    icon = Icons.Rounded.Headphones,
                                    primary = true,
                                    enabled = true,
                                    onClick = onOpenAutoEqCatalog
                                )
                                AudioActionButton(
                                    label = strings.autoEqImport,
                                    icon = Icons.Rounded.FileOpen,
                                    primary = false,
                                    enabled = true,
                                    onClick = { showAutoEqImport = true }
                                )
                            }
                        }
                    }
                }

                item { AudioSectionLabel("Derbis Spatial Audio") }
                item {
                    AudioToggleRow(
                        title = "Derbis Spatial Audio",
                        subtitle = "Optional virtual surround processing for headphones and stereo speakers.",
                        checked = audioSettings.virtualizer > 0,
                        onCheckedChange = { enabled -> onVirtualizer(if (enabled) 55 else 0) }
                    )
                }
                item {
                    AudioToggleRow(
                        title = "Derbis 360",
                        subtitle = if (derbis360OutputAvailable) {
                            strings.derbis360AvailableSubtitle
                        } else {
                            strings.derbis360UnavailableSubtitle
                        },
                        checked = audioSettings.eightDAudioEnabled && derbis360OutputAvailable,
                        enabled = derbis360OutputAvailable,
                        onCheckedChange = onDerbis360Enabled
                    )
                }
                if (audioSettings.eightDAudioEnabled && derbis360OutputAvailable) {
                    item {
                        AudioCard {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(strings.derbis360ExperienceTitle, color = DerbisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        strings.derbis360ExperienceSubtitle,
                                        color = DerbisMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(AudioSegmentGap)
                                ) {
                                    AudioSegment(
                                        label = "8D",
                                        selected = audioSettings.derbis360Experience == Derbis360Experience.EIGHT_D,
                                        first = true,
                                        last = false,
                                        onClick = { onDerbis360Experience(Derbis360Experience.EIGHT_D) }
                                    )
                                    AudioSegment(
                                        label = "16D",
                                        selected = audioSettings.derbis360Experience == Derbis360Experience.SIXTEEN_D,
                                        first = false,
                                        last = true,
                                        onClick = { onDerbis360Experience(Derbis360Experience.SIXTEEN_D) }
                                    )
                                }
                            }
                        }
                    }
                }
                if (audioSettings.virtualizer > 0) {
                    item {
                        AudioSliderRow(
                            title = "Spatial intensity",
                            valueLabel = "${audioSettings.virtualizer}%",
                            value = audioSettings.virtualizer.toFloat(),
                            range = 1f..100f,
                            icon = true,
                            onValue = { onVirtualizer(it.roundToInt()) }
                        )
                    }
                }

                item { AudioSectionLabel(strings.audioSectionEnhancement) }
                item {
                    AudioToggleRow(
                        title = strings.enhancedAudioTitle,
                        subtitle = strings.enhancedAudioSubtitle,
                        checked = audioSettings.enhancedAudioEnabled,
                        onCheckedChange = onEnhancedAudio
                    )
                }

                item { AudioSectionLabel(strings.audioSectionDynamics) }
                item {
                    AudioToggleRow(
                        title = strings.truePeakLimiter,
                        subtitle = "−1.0 dBTP",
                        checked = audioSettings.limiterEnabled,
                        onCheckedChange = onLimiter
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.replayGain,
                        subtitle = replayGainCopy.modeLabel(audioSettings.effectiveReplayGainMode),
                        checked = audioSettings.replayGainActive,
                        onCheckedChange = onReplayGain
                    )
                }
                if (audioSettings.replayGainActive) {
                    item {
                        AudioQualityRow(
                            selected = audioSettings.effectiveReplayGainMode.storageValue,
                            labels = listOf(
                                replayGainCopy.track to ReplayGainMode.TRACK.storageValue,
                                replayGainCopy.album to ReplayGainMode.ALBUM.storageValue,
                                replayGainCopy.smart to ReplayGainMode.SMART.storageValue
                            ),
                            onSelect = { value ->
                                val mode = ReplayGainMode.fromStorage(value, legacyEnabled = true)
                                onReplayGainMode(mode)
                            }
                        )
                    }
                    item {
                        AudioSliderRow(
                            title = "${strings.replayGain} · ${strings.preamp}",
                            valueLabel = decibels(audioSettings.replayGainPreampDb),
                            value = audioSettings.replayGainPreampDb,
                            range = -12f..12f,
                            defaultValue = 0f,
                            onReset = { onReplayGainPreamp(0f) },
                            onValue = { onReplayGainPreamp((it * 2f).roundToInt() / 2f) }
                        )
                    }
                    item {
                        AudioToggleRow(
                            title = "${strings.replayGain} · ${replayGainCopy.clippingProtection}",
                            subtitle = replayGainCopy.peakAware,
                            checked = audioSettings.replayGainPreventClipping,
                            onCheckedChange = onReplayGainPreventClipping
                        )
                    }
                }

                item { AudioSectionLabel(strings.audioSectionPlayback) }
                item {
                    AudioSliderRow(
                        title = strings.crossfade,
                        valueLabel = "${audioSettings.crossfadeSeconds}s",
                        value = audioSettings.crossfadeSeconds.toFloat(),
                        range = 0f..30f,
                        defaultValue = 0f,
                        onReset = { onCrossfade(0) },
                        onValue = { onCrossfade(it.roundToInt()) }
                    )
                }
                item {
                    CrossfadeCurveLab(
                        seconds = audioSettings.crossfadeSeconds,
                        autoMixEnabled = audioSettings.djSoftMode,
                        gaplessEnabled = audioSettings.gaplessEnabled,
                        copy = crossfadeLabCopy(strings.code)
                    )
                }
                item {
                    AudioToggleRow(
                        title = "Beat Sync",
                        subtitle = if (audioSettings.djSoftMode) {
                            "Unavailable while DJ Crossfade is on. Turn DJ Crossfade off to use verified Mix Studio beat alignment."
                        } else {
                            "Enables verified BPM and beat-grid alignment for transitions selected in Mix Studio."
                        },
                        checked = audioSettings.beatSyncEnabled,
                        enabled = !audioSettings.djSoftMode,
                        onCheckedChange = onBeatSync
                    )
                }
                item {
                    AudioToggleRow(
                        title = "DJ Crossfade",
                        subtitle = if (audioSettings.beatSyncEnabled) {
                            "Unavailable while Beat Sync is on. Turn Beat Sync off to restore energy-adaptive crossfades."
                        } else {
                            "${strings.crossfade} ${audioSettings.crossfadeSeconds}s"
                        },
                        checked = audioSettings.djSoftMode,
                        enabled = !audioSettings.beatSyncEnabled,
                        onCheckedChange = onDjSoft
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.gapless,
                        subtitle = "",
                        checked = audioSettings.gaplessEnabled,
                        onCheckedChange = onGapless
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.preloadNextTrack,
                        subtitle = strings.preloadNextTrackSubtitle,
                        checked = audioSettings.preloadNextTrack,
                        onCheckedChange = onPreloadNextTrack
                    )
                }
                item { AudioSectionLabel(bufferCopy.title) }
                item {
                    PlaybackBufferModeCard(
                        settings = audioSettings.playbackBuffer,
                        copy = bufferCopy,
                        onSettings = onPlaybackBuffer
                    )
                }
                if (audioSettings.playbackBuffer.mode == PlaybackBufferMode.CUSTOM) {
                    item {
                        PlaybackBufferPresetCard(
                            settings = audioSettings.playbackBuffer,
                            copy = bufferCopy,
                            onSettings = onPlaybackBuffer
                        )
                    }
                    item {
                        PlaybackBufferSlider(
                            title = bufferCopy.minimum,
                            value = audioSettings.playbackBuffer.minBufferSeconds,
                            range = PlaybackBufferSettings.MIN_BUFFER_SECONDS..PlaybackBufferSettings.MAX_MIN_BUFFER_SECONDS,
                            languageCode = strings.code,
                            onValue = { seconds ->
                                onPlaybackBuffer(audioSettings.playbackBuffer.copy(minBufferSeconds = seconds))
                            }
                        )
                    }
                    item {
                        PlaybackBufferSlider(
                            title = bufferCopy.maximum,
                            value = audioSettings.playbackBuffer.maxBufferSeconds,
                            range = audioSettings.playbackBuffer.minBufferSeconds..PlaybackBufferSettings.MAX_BUFFER_SECONDS,
                            languageCode = strings.code,
                            onValue = { seconds ->
                                onPlaybackBuffer(audioSettings.playbackBuffer.copy(maxBufferSeconds = seconds))
                            }
                        )
                    }
                    item {
                        PlaybackBufferSlider(
                            title = bufferCopy.start,
                            value = audioSettings.playbackBuffer.playbackBufferSeconds,
                            range = PlaybackBufferSettings.MIN_THRESHOLD_SECONDS..
                                audioSettings.playbackBuffer.minBufferSeconds.coerceAtMost(PlaybackBufferSettings.MAX_THRESHOLD_SECONDS),
                            languageCode = strings.code,
                            onValue = { seconds ->
                                onPlaybackBuffer(audioSettings.playbackBuffer.copy(playbackBufferSeconds = seconds))
                            }
                        )
                    }
                    item {
                        PlaybackBufferSlider(
                            title = bufferCopy.afterInterruption,
                            value = audioSettings.playbackBuffer.rebufferSeconds,
                            range = PlaybackBufferSettings.MIN_THRESHOLD_SECONDS..
                                audioSettings.playbackBuffer.minBufferSeconds.coerceAtMost(PlaybackBufferSettings.MAX_THRESHOLD_SECONDS),
                            languageCode = strings.code,
                            onValue = { seconds ->
                                onPlaybackBuffer(audioSettings.playbackBuffer.copy(rebufferSeconds = seconds))
                            }
                        )
                    }
                    item {
                        AudioActionButton(
                            label = bufferCopy.restoreAutomatic,
                            icon = Icons.Rounded.RestartAlt,
                            primary = false,
                            enabled = true,
                            onClick = {
                                onPlaybackBuffer(audioSettings.playbackBuffer.copy(mode = PlaybackBufferMode.AUTOMATIC))
                            }
                        )
                    }
                }
                if (aaudioOutputAvailable) {
                    item {
                        AudioToggleRow(
                            title = strings.audioOutputAaudio,
                            subtitle = strings.audioOutputAaudioSubtitle,
                            checked = audioSettings.aaudioOutputEnabled,
                            onCheckedChange = onAaudioOutput
                        )
                    }
                }
                item {
                    AudioEfficiencyRow(
                        preference = audioSettings.audioOffloadPreference,
                        onPreference = onAudioOffloadPreference
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.tempo,
                        valueLabel = "${trimAudioSpeed(audioSettings.playbackSpeed)}x",
                        value = audioSettings.playbackSpeed,
                        range = 0.5f..2.0f,
                        defaultValue = 1.0f,
                        onReset = { onTempo(1.0f) },
                        onValue = { onTempo((it * 100f).roundToInt() / 100f) }
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.pitch,
                        valueLabel = "${trimAudioSpeed(audioSettings.pitch)}x",
                        value = audioSettings.pitch,
                        range = 0.5f..2.0f,
                        defaultValue = 1.0f,
                        onReset = { onPitch(1.0f) },
                        onValue = { onPitch((it * 100f).roundToInt() / 100f) }
                    )
                }
                item {
                    val hasSpeedOrPitchModifications =
                        abs(audioSettings.playbackSpeed - 1.0f) > 0.005f ||
                        abs(audioSettings.pitch - 1.0f) > 0.005f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (hasSpeedOrPitchModifications) DerbisAdaptiveChip else DerbisAdaptiveChip.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .clickable(enabled = hasSpeedOrPitchModifications) {
                                    haptics.perform(DerbisHapticAction.Confirm)
                                    onTempo(1.0f)
                                    onPitch(1.0f)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.RestartAlt,
                                    contentDescription = strings.audioResetEqualizer,
                                    tint = if (hasSpeedOrPitchModifications) DerbisCyan else DerbisMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    strings.audioResetEqualizer,
                                    color = if (hasSpeedOrPitchModifications) DerbisText else DerbisMuted.copy(alpha = 0.5f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = DerbisCyan,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(136.dp)
                                .heightIn(min = 52.dp)
                                .clickable(onClick = onClose)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(strings.done, color = DerbisBlack, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAutoEqImport) {
        AutoEqImportDialog(
            onDismiss = { showAutoEqImport = false },
            onApply = onApplyAutoEq,
            onSavePreset = onSaveAutoEqPreset,
            onApplyParametric = onApplyParametricAutoEq,
            onSaveParametric = onSaveParametricProfile
        )
    }

    val catalogSelection = autoEqCatalog.selection
    if (catalogSelection != null) {
        AutoEqImportDialog(
            initialText = catalogSelection.profileText,
            initialPresetName = catalogSelection.name,
            catalogDetail = catalogSelection.detail,
            onDismiss = onDismissAutoEqCatalogProfile,
            onApply = onApplyAutoEq,
            onSavePreset = onSaveAutoEqPreset,
            onApplyParametric = onApplyParametricAutoEq,
            onSaveParametric = onSaveParametricProfile
        )
    } else if (autoEqCatalog.visible) {
        AutoEqCatalogDialog(
            state = autoEqCatalog,
            onQuery = onAutoEqCatalogQuery,
            onSelect = onSelectAutoEqCatalogEntry,
            onRetry = onOpenAutoEqCatalog,
            onDismiss = onCloseAutoEqCatalog
        )
    }
}

@Composable
private fun AutoEqCatalogDialog(
    state: AutoEqCatalogUiState,
    onQuery: (String) -> Unit,
    onSelect: (AutoEqCatalogEntry) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    var query by rememberSaveable { mutableStateOf(state.query) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = DerbisPanel,
            shape = CardShape,
            border = BorderStroke(1.dp, DerbisAdaptiveHairline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    strings.autoEqCatalog,
                    color = DerbisText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    strings.autoEqCatalogHint,
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { candidate ->
                        query = candidate.take(AutoEqCatalog.MAX_QUERY_CHARS)
                        onQuery(query)
                    },
                    enabled = state.status == AutoEqCatalogStatus.READY,
                    singleLine = true,
                    placeholder = { Text(strings.autoEqCatalogSearch, color = DerbisMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = DerbisMuted) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DerbisText,
                        unfocusedTextColor = DerbisText,
                        disabledTextColor = DerbisMuted,
                        focusedBorderColor = DerbisCyan.copy(alpha = 0.7f),
                        unfocusedBorderColor = DerbisAdaptiveHairline,
                        disabledBorderColor = DerbisAdaptiveHairline,
                        cursorColor = DerbisCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    state.status == AutoEqCatalogStatus.LOADING -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DerbisCyan)
                        Text(strings.autoEqCatalogLoading, color = DerbisMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    state.status == AutoEqCatalogStatus.UNAVAILABLE -> {
                        Text(strings.autoEqCatalogUnavailable, color = DerbisOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        AudioTextAction(label = strings.autoEqCatalogRetry, enabled = true, onClick = onRetry)
                    }
                    state.results.isNotEmpty() -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.results, key = { it.key }) { entry ->
                            AutoEqCatalogRow(
                                entry = entry,
                                loading = state.loadingKey == entry.key,
                                failed = state.failedKey == entry.key,
                                enabled = state.loadingKey == null,
                                onClick = { onSelect(entry) }
                            )
                        }
                    }
                    query.isNotBlank() && state.resultsQuery == state.query -> Text(
                        strings.autoEqCatalogEmpty,
                        color = DerbisMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    strings.autoEqCatalogAttribution,
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                AutoEqDialogButton(
                    label = strings.cancel,
                    primary = false,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun AutoEqCatalogRow(
    entry: AutoEqCatalogEntry,
    loading: Boolean,
    failed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    Surface(
        color = AudioCardSurface,
        shape = ChipShape,
        border = BorderStroke(1.dp, DerbisAdaptiveHairline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.name,
                    color = DerbisText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (failed) strings.autoEqCatalogProfileFailed else "${entry.source} · ${entry.variant}",
                    color = if (failed) DerbisOrange else DerbisMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DerbisCyan)
            }
        }
    }
}

@Composable
private fun AutoEqImportDialog(
    onDismiss: () -> Unit,
    onApply: (AutoEqImporter.ImportedProfile) -> Unit,
    onSavePreset: (String, AutoEqImporter.ImportedProfile) -> Boolean,
    onApplyParametric: (ParametricEqProfile) -> Unit,
    onSaveParametric: (String, ParametricEqProfile) -> Boolean,
    initialText: String = "",
    initialPresetName: String = "",
    catalogDetail: String? = null
) {
    val fromCatalog = catalogDetail != null
    val strings = LocalDerbisStrings.current
    val parametricCopy = strings.parametricEqCopy()
    val context = LocalContext.current
    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var presetName by remember(initialPresetName) { mutableStateOf(initialPresetName.take(ParametricEqualizer.MAX_NAME_CHARS)) }
    var presetNameDirty by remember(initialPresetName) { mutableStateOf(initialPresetName.isNotBlank()) }
    var readError by remember { mutableStateOf<String?>(null) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fileRequest by remember { mutableIntStateOf(0) }
    var readingFile by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<ParsedAutoEqImport>(ParsedAutoEqImport.Empty) }
    var parsedInput by remember { mutableStateOf<String?>(null) }
    val parsing = parsedInput != rawText
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.size > DerbisAudioPresets.MAX_CUSTOM_PRESETS) {
            readError = strings.parametricProfileCopy().limitReached
        } else if (uris.isNotEmpty()) {
            readError = null
            saveError = null
            rawText = ""
            readingFile = true
            pendingUris = uris.distinct()
            fileRequest += 1
        }
    }

    fun finishProfile() {
        if (pendingUris.size > 1) {
            rawText = ""
            readError = null
            saveError = null
            readingFile = true
            pendingUris = pendingUris.drop(1)
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(pendingUris.firstOrNull(), fileRequest) {
        val uri = pendingUris.firstOrNull() ?: return@LaunchedEffect
        val outcome = withContext(Dispatchers.IO) { readBoundedAutoEqText(context, uri) }
        readingFile = false
        when (outcome) {
            is AutoEqFileRead.Success -> {
                rawText = outcome.text
                presetName = outcome.name.take(ParametricEqualizer.MAX_NAME_CHARS)
                presetNameDirty = presetName.isNotBlank()
                readError = null
            }
            AutoEqFileRead.TooLarge -> readError = strings.autoEqInputTooLarge
            AutoEqFileRead.Unreadable -> readError = strings.autoEqInvalidProfile
        }
    }

    LaunchedEffect(rawText, initialPresetName, parametricCopy.parametricEq) {
        val input = rawText
        parsed = withContext(Dispatchers.Default) {
            parseAutoEqImport(input, initialPresetName.ifBlank { parametricCopy.parametricEq })
        }
        parsedInput = input
    }
    val profile = parsed.takeIf {
        !readingFile && !parsing && readError == null && (it is ParsedAutoEqImport.Graphic || it is ParsedAutoEqImport.Parametric)
    }
    LaunchedEffect(profile?.name) {
        if (!presetNameDirty) presetName = profile?.name.orEmpty()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = DerbisPanel,
            shape = CardShape,
            border = BorderStroke(1.dp, DerbisAdaptiveHairline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (fromCatalog) initialPresetName else strings.autoEqImport,
                    color = DerbisText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    catalogDetail ?: parametricCopy.importHint,
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!fromCatalog) OutlinedTextField(
                    value = rawText,
                    onValueChange = { candidate ->
                        if (candidate.length > AutoEqImporter.MAX_INPUT_CHARS) {
                            readError = strings.autoEqInputTooLarge
                        } else {
                            readError = null
                            rawText = candidate
                        }
                    },
                    placeholder = { Text("GraphicEQ: 20 4.5; 25 4.4; ...", color = DerbisMuted, fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DerbisText,
                        unfocusedTextColor = DerbisText,
                        focusedBorderColor = DerbisCyan.copy(alpha = 0.7f),
                        unfocusedBorderColor = DerbisAdaptiveHairline,
                        cursorColor = DerbisCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!fromCatalog) {
                    AudioTextAction(
                        label = strings.autoEqPickFile,
                        enabled = true,
                        onClick = { picker.launch(AutoEqDocumentMimeTypes) }
                    )
                }

                val errorMessage = saveError ?: readError ?: if (!parsing && parsed is ParsedAutoEqImport.Error) {
                    if ((parsed as ParsedAutoEqImport.Error).tooLarge) {
                        strings.autoEqInputTooLarge
                    } else {
                        strings.autoEqInvalidProfile
                    }
                } else {
                    null
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = DerbisOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (profile != null) {
                    when (profile) {
                        is ParsedAutoEqImport.Graphic -> AutoEqProfilePreview(
                            profile = profile.profile,
                            title = if (fromCatalog) strings.audioBands else presetName.ifBlank {
                                profile.profile.name ?: strings.audioPresetCustom
                            }
                        )
                        is ParsedAutoEqImport.Parametric -> ParametricAutoEqProfilePreview(
                            profile = profile.profile,
                            title = if (fromCatalog) parametricCopy.bands else presetName.ifBlank { profile.profile.name },
                            copy = parametricCopy
                        )
                        is ParsedAutoEqImport.Error, ParsedAutoEqImport.Empty -> Unit
                    }
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = {
                            presetNameDirty = true
                            saveError = null
                            presetName = it.take(ParametricEqualizer.MAX_NAME_CHARS)
                        },
                        singleLine = true,
                        label = { Text(strings.autoEqPresetName, color = DerbisMuted, fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DerbisText,
                            unfocusedTextColor = DerbisText,
                            focusedBorderColor = DerbisCyan.copy(alpha = 0.7f),
                            unfocusedBorderColor = DerbisAdaptiveHairline,
                            cursorColor = DerbisCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AutoEqDialogButton(
                        label = strings.autoEqSavePreset,
                        primary = false,
                        enabled = profile != null && presetName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val saved = when (val value = profile) {
                                is ParsedAutoEqImport.Graphic -> onSavePreset(presetName.trim(), value.profile)
                                is ParsedAutoEqImport.Parametric -> onSaveParametric(presetName.trim(), value.profile)
                                is ParsedAutoEqImport.Error, ParsedAutoEqImport.Empty, null -> false
                            }
                            if (saved) finishProfile() else saveError = strings.parametricProfileCopy().limitReached
                        }
                    )
                    AutoEqDialogButton(
                        label = strings.autoEqApply,
                        primary = true,
                        enabled = profile != null,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (val value = profile) {
                                is ParsedAutoEqImport.Graphic -> onApply(value.profile)
                                is ParsedAutoEqImport.Parametric -> onApplyParametric(value.profile)
                                is ParsedAutoEqImport.Error, ParsedAutoEqImport.Empty, null -> Unit
                            }
                            finishProfile()
                        }
                    )
                }
                if (pendingUris.size > 1) {
                    AudioTextAction(
                        label = "${strings.next} · ${pendingUris.size - 1}",
                        enabled = !readingFile,
                        onClick = ::finishProfile
                    )
                }
                AutoEqDialogButton(
                    label = strings.cancel,
                    primary = false,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }
}

private sealed interface ParsedAutoEqImport {
    val name: String?

    data object Empty : ParsedAutoEqImport {
        override val name: String? = null
    }

    data class Error(val tooLarge: Boolean) : ParsedAutoEqImport {
        override val name: String? = null
    }

    data class Graphic(val profile: AutoEqImporter.ImportedProfile) : ParsedAutoEqImport {
        override val name: String? get() = profile.name
    }

    data class Parametric(val profile: ParametricEqProfile) : ParsedAutoEqImport {
        override val name: String get() = profile.name
    }
}

private fun parseAutoEqImport(text: String, fallbackName: String): ParsedAutoEqImport {
    if (text.isBlank()) return ParsedAutoEqImport.Empty
    val hasParametricFilters = text.lineSequence().any {
        it.trimStart().removePrefix("\uFEFF").startsWith("Filter", ignoreCase = true)
    }
    return if (hasParametricFilters) {
        when (val result = AutoEqImporter.parseParametric(text, fallbackName)) {
            is AutoEqImporter.ParametricParseResult.Success -> ParsedAutoEqImport.Parametric(result.profile)
            is AutoEqImporter.ParametricParseResult.Error -> ParsedAutoEqImport.Error(
                tooLarge = result.error == AutoEqImporter.ParametricParseError.TOO_LARGE
            )
        }
    } else {
        when (val result = AutoEqImporter.parse(text)) {
            is AutoEqImporter.ParseResult.Success -> ParsedAutoEqImport.Graphic(result.profile)
            is AutoEqImporter.ParseResult.Error -> ParsedAutoEqImport.Error(
                tooLarge = result.error == AutoEqImporter.ParseError.TOO_LARGE
            )
        }
    }
}

@Composable
private fun ParametricAutoEqProfilePreview(
    profile: ParametricEqProfile,
    title: String,
    copy: com.yasuda.derbis.ui.i18n.ParametricEqCopy
) {
    Surface(
        color = AudioCardSurface,
        shape = CardShape,
        border = BorderStroke(1.dp, DerbisAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AudioCardHeader(title = title, trailing = "${copy.bands} · ${profile.bands.size}")
            Text(
                text = "${LocalDerbisStrings.current.preamp} ${decibels(profile.preampDb)}",
                color = DerbisMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            profile.bands.take(5).forEachIndexed { index, band ->
                Text(
                    text = "${index + 1} · ${band.filterType.autoEqCode} · ${band.frequencyHz.roundToInt()} Hz · " +
                        "${decibels(band.gainDb)} · Q ${String.format(Locale.US, "%.2f", band.q)}",
                    color = if (band.enabled) DerbisText else DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (profile.bands.size > 5) {
                Text(
                    text = "+${profile.bands.size - 5}",
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AutoEqProfilePreview(profile: AutoEqImporter.ImportedProfile, title: String) {
    val strings = LocalDerbisStrings.current
    val bandSummary = remember(profile) {
        DerbisAudioPresets.bandFrequencyLabels.mapIndexed { index, label ->
            "$label Hz ${decibels(profile.bandGainDb.getOrElse(index) { 0f })}"
        }.joinToString(", ")
    }
    Surface(
        color = AudioCardSurface,
        shape = CardShape,
        border = BorderStroke(1.dp, DerbisAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AudioCardHeader(
                title = title,
                trailing = "${strings.preamp} ${decibels(profile.preampDb)}"
            )
            EqualizerCurvePreview(levels = profile.bandLevels, description = bandSummary)
            if (profile.clamped || profile.interpolated || profile.skippedPoints > 0) {
                Text(
                    strings.autoEqAdjustedNotice,
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AutoEqDialogButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (primary) DerbisCyan else DerbisAdaptiveChip,
        shape = ChipShape,
        border = if (primary) null else BorderStroke(1.dp, DerbisAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(
                label,
                color = if (primary) DerbisBlack else DerbisText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private sealed interface AutoEqFileRead {
    data class Success(val text: String, val name: String) : AutoEqFileRead
    data object TooLarge : AutoEqFileRead
    data object Unreadable : AutoEqFileRead
}

private val AutoEqDocumentMimeTypes = arrayOf("text/plain", "application/octet-stream", "*/*")

private fun readBoundedAutoEqText(context: Context, uri: Uri): AutoEqFileRead {
    val limit = AutoEqImporter.MAX_INPUT_CHARS
    return runCatching {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty().substringBeforeLast('.') else ""
            }.orEmpty()
        }.getOrDefault("")
        context.contentResolver.openInputStream(uri)?.buffered()?.use { stream ->
            stream.mark(3)
            val first = stream.read()
            val second = stream.read()
            stream.reset()
            val charset = when {
                first == 0xFF && second == 0xFE -> Charsets.UTF_16LE
                first == 0xFE && second == 0xFF -> Charsets.UTF_16BE
                else -> Charsets.UTF_8
            }
            if (charset != Charsets.UTF_8) stream.skip(2)
            val reader = stream.reader(charset)
            val buffer = CharArray(limit + 1)
            var read = 0
            while (read <= limit) {
                val count = reader.read(buffer, read, buffer.size - read)
                if (count <= 0) break
                read += count
            }
            if (read > limit) AutoEqFileRead.TooLarge else AutoEqFileRead.Success(String(buffer, 0, read), name)
        } ?: AutoEqFileRead.Unreadable
    }.getOrElse { AutoEqFileRead.Unreadable }
}

@Composable
private fun AudioPanelHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(DerbisMuted.copy(alpha = 0.5f))
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioPanelHeader(
    title: String,
    subtitle: String,
    volumeLabel: String,
    closeLabel: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(DerbisCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Equalizer, null, tint = DerbisCyan, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = DerbisText, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(DerbisCyan.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = DerbisCyan, modifier = Modifier.size(14.dp))
                    Text(volumeLabel, color = DerbisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    subtitle,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DerbisExpressiveIconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = AudioCardSurface,
                contentColor = DerbisText
            )
        ) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel)
        }
    }
}

@Composable
private fun AudioSectionLabel(text: String) {
    val strings = LocalDerbisStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code.replace('_', '-')) }
    val title = remember(text, locale) {
        text.trim().lowercase(locale).replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(locale) else character.toString()
        }
    }
    Text(
        title,
        color = DerbisCyan,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
        modifier = Modifier.padding(top = 14.dp, start = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun AudioCard(content: @Composable () -> Unit) {
    Surface(
        color = AudioCardSurface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            content()
        }
    }
}

@Composable
private fun AudioExpressiveSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = DerbisBlack,
            checkedIconColor = DerbisCyan,
            checkedTrackColor = DerbisCyan,
            uncheckedThumbColor = DerbisMuted,
            uncheckedTrackColor = DerbisAdaptiveTrack,
            uncheckedBorderColor = DerbisMuted.copy(alpha = 0.6f)
        ),
        modifier = modifier
    )
}

@Composable
private fun PlaybackBufferModeCard(
    settings: PlaybackBufferSettings,
    copy: PlaybackBufferCopy,
    onSettings: (PlaybackBufferSettings) -> Unit
) {
    AudioCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                copy.description,
                color = DerbisMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
            AudioQualityRow(
                selected = settings.mode.storageValue,
                labels = listOf(
                    copy.automatic to PlaybackBufferMode.AUTOMATIC.storageValue,
                    copy.custom to PlaybackBufferMode.CUSTOM.storageValue
                ),
                onSelect = { selected ->
                    onSettings(settings.copy(mode = PlaybackBufferMode.fromStorage(selected)))
                }
            )
            Text(
                if (settings.mode == PlaybackBufferMode.AUTOMATIC) copy.automaticDetail else copy.customDetail,
                color = DerbisMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PlaybackBufferPresetCard(
    settings: PlaybackBufferSettings,
    copy: PlaybackBufferCopy,
    onSettings: (PlaybackBufferSettings) -> Unit
) {
    AudioCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioCardHeader(copy.presets, "")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    copy.reduced to PlaybackBufferSettings.Reduced,
                    copy.balanced to PlaybackBufferSettings.Balanced,
                    copy.high to PlaybackBufferSettings.High
                )
                items(presets, key = { it.first }) { (label, preset) ->
                    AudioPresetChip(
                        label = label,
                        selected = settings.normalized() == preset,
                        enabled = true,
                        onClick = { onSettings(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackBufferSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    languageCode: String,
    onValue: (Float) -> Unit
) {
    AudioSliderRow(
        title = title,
        valueLabel = "${formatBufferSeconds(value, languageCode)} s",
        value = value,
        range = range,
        onValue = { onValue((it * 10f).roundToInt() / 10f) }
    )
}

@Composable
private fun CrossfadeCurveLab(
    seconds: Int,
    autoMixEnabled: Boolean,
    gaplessEnabled: Boolean,
    copy: CrossfadeLabCopy
) {
    val outgoingColor = DerbisOrange
    val incomingColor = DerbisCyan
    val enabled = gaplessEnabled && seconds > 0
    val samples = remember {
        val outgoing = FloatArray(CROSSFADE_CURVE_SAMPLE_COUNT)
        val incoming = FloatArray(CROSSFADE_CURVE_SAMPLE_COUNT)
        for (index in 0 until CROSSFADE_CURVE_SAMPLE_COUNT) {
            val gains = equalPowerCrossfade(index.toFloat() / (CROSSFADE_CURVE_SAMPLE_COUNT - 1).toFloat())
            outgoing[index] = gains.outgoing
            incoming[index] = gains.incoming
        }
        outgoing to incoming
    }
    val status = when {
        !gaplessEnabled -> copy.gaplessRequired
        seconds <= 0 -> copy.off
        autoMixEnabled -> copy.adaptiveDuration
        else -> copy.fixedDuration
    }

    AudioCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(copy.title, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        "$seconds s · $status",
                        color = DerbisMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = DerbisCyan.copy(alpha = 0.13f),
                    shape = ChipShape
                ) {
                    Text(
                        copy.equalPower,
                        color = DerbisCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CrossfadeLegend(copy.outgoing, outgoingColor)
                CrossfadeLegend(copy.incoming, incomingColor)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .semantics {
                        contentDescription = "${copy.title}: ${copy.equalPower}, $seconds s. $status"
                    }
            ) {
                val horizontalInset = 5.dp.toPx()
                val verticalInset = 7.dp.toPx()
                val graphWidth = (size.width - horizontalInset * 2f).coerceAtLeast(1f)
                val graphHeight = (size.height - verticalInset * 2f).coerceAtLeast(1f)
                val gridColor = DerbisMuted.copy(alpha = if (enabled) 0.18f else 0.10f)
                for (step in 0..4) {
                    val fraction = step / 4f
                    val x = horizontalInset + graphWidth * fraction
                    val y = verticalInset + graphHeight * fraction
                    drawLine(gridColor, Offset(x, verticalInset), Offset(x, size.height - verticalInset), 1f)
                    drawLine(gridColor, Offset(horizontalInset, y), Offset(size.width - horizontalInset, y), 1f)
                }

                val outgoingPath = Path()
                val incomingPath = Path()
                samples.first.indices.forEach { index ->
                    val fraction = index.toFloat() / (samples.first.size - 1).toFloat()
                    val x = horizontalInset + graphWidth * fraction
                    val outgoingY = verticalInset + (1f - samples.first[index]) * graphHeight
                    val incomingY = verticalInset + (1f - samples.second[index]) * graphHeight
                    if (index == 0) {
                        outgoingPath.moveTo(x, outgoingY)
                        incomingPath.moveTo(x, incomingY)
                    } else {
                        outgoingPath.lineTo(x, outgoingY)
                        incomingPath.lineTo(x, incomingY)
                    }
                }
                val alpha = if (enabled) 1f else DISABLED_ALPHA
                drawPath(
                    outgoingPath,
                    outgoingColor.copy(alpha = alpha),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    incomingPath,
                    incomingColor.copy(alpha = alpha),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (gaplessEnabled) {
                Text(
                    copy.sameReleasePolicy,
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CrossfadeLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(7.dp).background(color, RoundedCornerShape(50)))
        Text(label, color = DerbisMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AudioEqualizerSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = DerbisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                color = DerbisMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AudioExpressiveSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun AudioCardHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = DerbisText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing.isNotBlank()) {
            Text(
                trailing,
                color = DerbisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f, fill = false)
                    .clip(ChipShape)
                    .background(DerbisCyan.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AudioEfficiencyRow(
    preference: AudioOffloadPreference,
    onPreference: (AudioOffloadPreference) -> Unit
) {
    val strings = LocalDerbisStrings.current
    val automatic = preference == AudioOffloadPreference.AUTOMATIC
    val mode = if (automatic) strings.audioEfficiencyAutomatic else strings.audioEfficiencyOff
    AudioToggleRow(
        title = strings.audioEfficiencyTitle,
        subtitle = "$mode · ${strings.audioEfficiencySubtitle}",
        checked = automatic,
        onCheckedChange = { enabled ->
            onPreference(if (enabled) AudioOffloadPreference.AUTOMATIC else AudioOffloadPreference.OFF)
        }
    )
}

@Composable
private fun AudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        color = AudioCardSurface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = DerbisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = DerbisMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AudioExpressiveSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
private fun AudioSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    icon: Boolean = false,
    defaultValue: Float? = null,
    onReset: (() -> Unit)? = null,
    onValue: (Float) -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val isModified = defaultValue != null && abs(value - defaultValue) > 0.005f

    Surface(
        color = AudioCardSurface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon) {
                        Icon(Icons.Rounded.SurroundSound, null, tint = DerbisCyan, modifier = Modifier.size(18.dp))
                    }
                    Text(title, color = DerbisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isModified && onReset != null) {
                        IconButton(
                            onClick = {
                                haptics.perform(DerbisHapticAction.Confirm)
                                onReset()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = "Reset $title",
                                tint = DerbisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        valueLabel,
                        color = if (isModified) DerbisCyan else DerbisText.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(ChipShape)
                            .background(if (isModified) DerbisCyan.copy(alpha = 0.16f) else DerbisAdaptiveChip)
                            .clickable(enabled = isModified && onReset != null) {
                                haptics.perform(DerbisHapticAction.Confirm)
                                onReset?.invoke()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            AudioLevelSlider(
                value = value,
                range = range,
                label = title,
                valueLabel = valueLabel,
                defaultValue = defaultValue,
                onReset = onReset,
                onValue = onValue
            )
        }
    }
}

@Composable
private fun AudioPresetChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val corner = derbisExpressiveToggleCorner(selected, unchecked = 14.dp, checkedCorner = 24.dp, label = "audio-preset-corner")
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        color = if (selected) AudioSelected else DerbisAdaptiveChip,
        contentColor = if (selected) DerbisBlack else DerbisText,
        shape = RoundedCornerShape(corner),
        modifier = Modifier
            .widthIn(max = 240.dp)
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(visible = selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun RowScope.AudioSegment(
    label: String,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val outer = 26.dp
    val inner = derbisExpressiveCorner(
        interactionSource = interaction,
        rest = if (selected) outer else AudioSegmentInnerCorner,
        pressed = outer,
        label = "audio-segment-inner"
    )
    Surface(
        selected = selected,
        onClick = onClick,
        interactionSource = interaction,
        color = if (selected) AudioSelected else DerbisAdaptiveChip,
        contentColor = if (selected) DerbisBlack else DerbisText,
        shape = RoundedCornerShape(
            topStart = if (first) outer else inner,
            bottomStart = if (first) outer else inner,
            topEnd = if (last) outer else inner,
            bottomEnd = if (last) outer else inner
        ),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 52.dp)
            .semantics { role = Role.RadioButton }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            AnimatedVisibility(visible = selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AudioQualityRow(
    selected: String,
    labels: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AudioSegmentGap)
    ) {
        labels.forEachIndexed { index, (label, quality) ->
            AudioSegment(
                label = label,
                selected = selected.equals(quality, ignoreCase = true),
                first = index == 0,
                last = index == labels.lastIndex,
                onClick = { onSelect(quality) }
            )
        }
    }
}

@Composable
private fun AlternativeAudioCard(
    title: String,
    description: String,
    selected: HighQualityAudioMode,
    options: List<Pair<HighQualityAudioMode, String>>,
    onSelect: (HighQualityAudioMode) -> Unit
) {
    AudioCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = DerbisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AudioSegmentGap)
            ) {
                options.forEachIndexed { index, (mode, label) ->
                    val isSelected = mode == selected
                    Surface(
                        color = if (isSelected) DerbisCyan.copy(alpha = 0.2f) else DerbisAdaptiveChip,
                        shape = derbisGroupedListShape(index, options.size),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) DerbisCyan else DerbisText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) DerbisCyan else Color.Transparent)
                                    .border(2.dp, if (isSelected) DerbisCyan else DerbisMuted.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = DerbisBlack, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTextAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = Color.Transparent,
            shape = ChipShape,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(label, color = DerbisCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun EqualizerCurve(
    levels: List<Int>,
    enabled: Boolean,
    bandsLabel: String,
    resetLabel: String,
    onBandLevel: (Int, Int) -> Unit,
    onReset: () -> Unit
) {
    val bandCount = DerbisAudioPresets.bandCount
    val safeLevels = remember(levels) {
        if (levels.size == bandCount) levels else DerbisAudioPresets.flatLevels
    }
    var activeBand by remember { mutableIntStateOf(-1) }
    val handleRadiusPx = with(LocalDensity.current) { EqualizerHandleRadius.toPx() }
    val curveColor = DerbisCyan
    val gridColor = DerbisMuted.copy(alpha = 0.28f)
    val handleFill = DerbisPanel
    val density = LocalDensity.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                AudioCardHeader(
                    title = bandsLabel,
                    trailing = if (activeBand in 0 until bandCount) {
                        "${DerbisAudioPresets.bandFrequencyLabels[activeBand]} Hz · " +
                            decibels(DerbisAudioPresets.bandDb(safeLevels[activeBand]))
                    } else {
                        "±${DerbisAudioPresets.maxBandDb.roundToInt()} dB"
                    }
                )
            }
            IconButton(onClick = onReset, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = resetLabel,
                    tint = if (enabled) DerbisMuted else DerbisMuted.copy(alpha = DISABLED_ALPHA),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (DerbisIsLight) DerbisBlack.copy(alpha = 0.05f) else DerbisBlack.copy(alpha = 0.35f))
                .border(1.dp, DerbisAdaptiveHairline, RoundedCornerShape(16.dp))
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .pointerInput(enabled, bandCount) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val band = bandAt(offset.x, size.width, bandCount)
                            activeBand = band
                            onBandLevel(band, bandLevelAt(offset.y, size.height.toFloat(), handleRadiusPx))
                            tryAwaitRelease()
                            activeBand = -1
                        }
                    )
                }
                .pointerInput(enabled, bandCount) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset -> activeBand = bandAt(offset.x, size.width, bandCount) },
                        onDragEnd = { activeBand = -1 },
                        onDragCancel = { activeBand = -1 }
                    ) { change, _ ->
                        val band = bandAt(change.position.x, size.width, bandCount)
                        activeBand = band
                        onBandLevel(band, bandLevelAt(change.position.y, size.height.toFloat(), handleRadiusPx))
                        change.consume()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawEqualizerCurve(
                    levels = safeLevels,
                    curveColor = curveColor,
                    gridColor = gridColor,
                    handleFill = handleFill,
                    activeBand = activeBand,
                    handleRadius = handleRadiusPx,
                    strokeWidth = with(density) { 2.dp.toPx() }
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.fillMaxSize()) {
                    safeLevels.forEachIndexed { index, level ->
                    val frequency = DerbisAudioPresets.bandFrequencyLabels[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .progressSemantics(level.toFloat(), -100f..100f, 0)
                                .semantics {
                                    contentDescription = "$frequency Hz"
                                    stateDescription = decibels(DerbisAudioPresets.bandDb(level))
                                    if (enabled) {
                                        setProgress { target ->
                                            onBandLevel(index, target.roundToInt())
                                            true
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DerbisAudioPresets.bandFrequencyLabels.forEach { frequency ->
                    Text(
                        frequency,
                        color = DerbisMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerCurvePreview(levels: List<Int>, description: String) {
    val handleRadiusPx = with(LocalDensity.current) { 4.dp.toPx() }
    val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }
    val curveColor = DerbisCyan
    val gridColor = DerbisMuted.copy(alpha = 0.28f)
    val handleFill = AudioCardSurface
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            drawEqualizerCurve(
                levels = levels,
                curveColor = curveColor,
                gridColor = gridColor,
                handleFill = handleFill,
                activeBand = -1,
                handleRadius = handleRadiusPx,
                strokeWidth = strokeWidthPx
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DerbisAudioPresets.bandFrequencyLabels.forEach { frequency ->
                    Text(
                        frequency,
                        color = DerbisMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioLevelSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    valueLabel: String,
    defaultValue: Float? = null,
    onReset: (() -> Unit)? = null,
    onValue: (Float) -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val bounded = value.coerceIn(range.start, range.endInclusive)
    val origin = 0f.coerceIn(range.start, range.endInclusive)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val thumbRadius = with(density) { 9.dp.toPx() }
    val nocturne = DerbisIsNocturne
    val trackHeight = with(density) { (if (nocturne || DerbisIsPrism) 3.dp else 14.dp).toPx() }
    val handleWidth = with(density) { 4.dp.toPx() }
    val handleHeight = with(density) { 36.dp.toPx() }
    val handleGap = with(density) { 5.dp.toPx() }
    val touchCatchRadius = with(density) { 44.dp.toPx() }
    val activeColor = DerbisCyan
    val inactiveColor = DerbisAdaptiveTrack
    val ringColor = AudioCardSurface

    fun xOf(level: Float, width: Int): Float {
        val usable = width - thumbRadius * 2f
        val fraction = ((level - range.start) / span).coerceIn(0f, 1f)
        return thumbRadius + usable * if (rtl) 1f - fraction else fraction
    }

    fun valueAt(x: Float, width: Int): Float {
        val usable = (width - thumbRadius * 2f).coerceAtLeast(1f)
        val raw = ((x - thumbRadius) / usable).coerceIn(0f, 1f)
        val fraction = if (rtl) 1f - raw else raw
        val rawValue = range.start + fraction * span
        if (defaultValue != null) {
            val magneticTolerance = 0.045f * span
            if (abs(rawValue - defaultValue) <= magneticTolerance) {
                return defaultValue
            }
        }
        return rawValue
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .progressSemantics(bounded, range, 0)
            .semantics {
                contentDescription = label
                stateDescription = valueLabel
                setProgress { target ->
                    onValue(target.coerceIn(range.start, range.endInclusive))
                    true
                }
            }
            .pointerInput(range, rtl, defaultValue) {
                detectTapGestures(
                    onDoubleTap = {
                        if (onReset != null) {
                            haptics.perform(DerbisHapticAction.Confirm)
                            onReset()
                        }
                    }
                    // Deliberately no single-tap: eliminates misclicks & accidental jumping when scrolling
                )
            }
            .pointerInput(range, rtl, defaultValue, bounded) {
                var isInteracting = false
                var wasSnapped = defaultValue != null && abs(bounded - defaultValue) < 0.005f

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val currentHandleX = xOf(bounded, size.width)
                        // Reduce sensitivity: ONLY engage drag when gesture starts near the thumb handle
                        if (abs(offset.x - currentHandleX) <= touchCatchRadius) {
                            isInteracting = true
                        } else {
                            isInteracting = false
                        }
                    },
                    onDragEnd = { isInteracting = false },
                    onDragCancel = { isInteracting = false },
                    onHorizontalDrag = { change, _ ->
                        if (isInteracting) {
                            val computed = valueAt(change.position.x, size.width)
                            val isSnappedNow = defaultValue != null && abs(computed - defaultValue) < 0.005f
                            if (isSnappedNow && !wasSnapped) {
                                haptics.perform(DerbisHapticAction.SeekSnap)
                            }
                            wasSnapped = isSnappedNow
                            onValue(computed)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val usable = size.width - thumbRadius * 2f
            fun xOfCanvas(level: Float): Float {
                val fraction = (level - range.start) / span
                return thumbRadius + usable * if (rtl) 1f - fraction else fraction
            }
            val centerY = size.height / 2f
            val handleX = xOfCanvas(bounded)
            val originX = xOfCanvas(origin)
            val trackLeft = thumbRadius - trackHeight / 2f
            val trackRight = size.width - thumbRadius + trackHeight / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            fun segment(left: Float, right: Float, color: Color) {
                if (right - left <= 0.5f) return
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, centerY - trackHeight / 2f),
                    size = Size(right - left, trackHeight),
                    cornerRadius = corner
                )
            }
            segment(trackLeft, handleX - handleGap, inactiveColor)
            segment(handleX + handleGap, trackRight, inactiveColor)
            val originEdge = when {
                origin > range.start -> originX
                rtl -> trackRight
                else -> trackLeft
            }
            if (handleX > originEdge) {
                segment(originEdge, handleX - handleGap, activeColor)
            } else if (handleX < originEdge) {
                segment(handleX + handleGap, originEdge, activeColor)
            }
            if (origin > range.start) {
                drawCircle(ringColor, trackHeight / 5f, Offset(originX, centerY))
            }
            val defaultMarker = defaultValue?.takeIf { it in range && it > range.start }
            if (defaultMarker != null) {
                val defaultX = xOfCanvas(defaultMarker)
                drawCircle(activeColor.copy(alpha = 0.5f), trackHeight / 4.5f, Offset(defaultX, centerY))
            }
            val stopX = if (rtl) trackLeft + trackHeight / 2f else trackRight - trackHeight / 2f
            drawCircle(activeColor, trackHeight / 7f, Offset(stopX, centerY))
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(handleX - handleWidth / 2f, centerY - handleHeight / 2f),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleWidth / 2f, handleWidth / 2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioActionButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (primary) DerbisCyan else DerbisText
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (primary) DerbisCyan.copy(alpha = 0.14f) else DerbisAdaptiveChip,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Cookie4Sided.toShape())
                    .background(contentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Text(
                label,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun bandLevelAt(y: Float, height: Float, handleRadius: Float): Int {
    val half = height / 2f
    val usable = (half - handleRadius * 2f).coerceAtLeast(1f)
    val fraction = 0.5f - ((half - y) / usable) / 2f
    return DerbisAudioPresets.bandLevelFromVerticalFraction(fraction)
}

private fun bandAt(x: Float, width: Int, bandCount: Int): Int {
    if (width <= 0) return 0
    val slot = width.toFloat() / bandCount
    return (x / slot).toInt().coerceIn(0, bandCount - 1)
}

private fun DrawScope.drawEqualizerCurve(
    levels: List<Int>,
    curveColor: Color,
    gridColor: Color,
    handleFill: Color,
    activeBand: Int,
    handleRadius: Float,
    strokeWidth: Float
) {
    val slot = size.width / levels.size
    val zeroY = size.height / 2f
    val amplitude = size.height / 2f - handleRadius * 2f
    val guideColor = gridColor.copy(alpha = gridColor.alpha * 0.45f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 2f, strokeWidth * 3f))
    EQUALIZER_GRID_FRACTIONS.forEach { fraction ->
        val y = zeroY - fraction * amplitude
        drawLine(guideColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeWidth / 2f, pathEffect = dash)
    }
    repeat(levels.size) { index ->
        val x = slot * index + slot / 2f
        drawLine(guideColor, Offset(x, handleRadius), Offset(x, size.height - handleRadius), strokeWidth = strokeWidth / 2f)
    }
    drawLine(gridColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = strokeWidth / 2f)

    val points = levels.mapIndexed { index, level ->
        val x = slot * index + slot / 2f
        val y = zeroY - level / 100f * amplitude
        Offset(x, y)
    }
    val curve = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (index in 0 until points.size - 1) {
            val current = points[index]
            val next = points[index + 1]
            val midX = (current.x + next.x) / 2f
            cubicTo(midX, current.y, midX, next.y, next.x, next.y)
        }
    }
    val fill = Path().apply {
        addPath(curve)
        lineTo(points.last().x, zeroY)
        lineTo(points.first().x, zeroY)
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            0f to curveColor.copy(alpha = 0.30f),
            0.5f to curveColor.copy(alpha = 0.05f),
            1f to curveColor.copy(alpha = 0.30f)
        )
    )
    drawPath(path = curve, color = curveColor.copy(alpha = 0.22f), style = Stroke(width = strokeWidth * 4f, cap = StrokeCap.Round))
    drawPath(path = curve, color = curveColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    points.forEachIndexed { index, point ->
        if (index == activeBand) drawCircle(curveColor.copy(alpha = 0.22f), handleRadius * 2.6f, point)
        drawCircle(handleFill, handleRadius + strokeWidth, point)
        drawCircle(curveColor, if (index == activeBand) handleRadius * 1.25f else handleRadius, point)
    }
}

private val EQUALIZER_GRID_FRACTIONS = floatArrayOf(-1f, -0.5f, 0.5f, 1f)

private fun decibels(value: Float): String = String.format(Locale.US, "%+.1f dB", value)

private fun trimAudioSpeed(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (abs(rounded - rounded.roundToInt()) < 0.005f) {
        rounded.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}

private fun formatBufferSeconds(value: Float, languageCode: String): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag(languageCode)).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }.format(value.toDouble())
