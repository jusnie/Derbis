package com.yasuda.derbis.ui.theme

import androidx.compose.ui.graphics.Color
import com.yasuda.derbis.domain.DerbisGlassEffects
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import com.yasuda.derbis.ui.icons.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import com.yasuda.derbis.ui.icons.Check
import com.yasuda.derbis.ui.icons.Close
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.DerbisGlassStyle
import com.yasuda.derbis.ui.i18n.GlassStudioCopy
import kotlin.math.roundToInt

private val GlassTileGap = 3.dp
private val GlassTileOuterCorner = 28.dp
private val GlassTileInnerCorner = 8.dp
private val GlassTileFirstShape = RoundedCornerShape(
    topStart = GlassTileOuterCorner,
    topEnd = GlassTileOuterCorner,
    bottomStart = GlassTileInnerCorner,
    bottomEnd = GlassTileInnerCorner
)
private val GlassTileMiddleShape = RoundedCornerShape(GlassTileInnerCorner)
private val GlassTileLastShape = RoundedCornerShape(
    topStart = GlassTileInnerCorner,
    topEnd = GlassTileInnerCorner,
    bottomStart = GlassTileOuterCorner,
    bottomEnd = GlassTileOuterCorner
)
private val GlassSliderTrackHeight = 16.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ThemeStudioGlassSection(
    copy: GlassStudioCopy,
    enabled: Boolean,
    style: DerbisGlassStyle,
    onStyleChange: (DerbisGlassStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceMd)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = DerbisPlayerDesign.SpaceXs)
            ) {
                Text(
                    copy.title,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    copy.subtitle,
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    lineHeight = DerbisTypeRhythm.lineHeight(13f),
                    fontWeight = FontWeight.Medium
                )
            }
            FilledTonalButton(
                onClick = { onStyleChange(DerbisGlassStyle()) },
                enabled = enabled && style != DerbisGlassStyle(),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(copy.reset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(GlassTileGap)) {
            GlassLevelTile(
                label = copy.blur,
                icon = Icons.Rounded.BlurOn,
                value = style.blur,
                enabled = enabled,
                shape = GlassTileFirstShape
            ) { onStyleChange(style.copy(blur = it)) }
            GlassLevelTile(
                label = copy.transparency,
                icon = Icons.Rounded.Opacity,
                value = style.transparency,
                enabled = enabled,
                shape = GlassTileMiddleShape
            ) { onStyleChange(style.copy(transparency = it)) }
            GlassLevelTile(
                label = copy.tint,
                icon = Icons.Rounded.InvertColors,
                value = style.tint,
                enabled = enabled,
                shape = GlassTileMiddleShape
            ) { onStyleChange(style.copy(tint = it)) }
            GlassRefractionTile(
                title = copy.refraction,
                subtitle = copy.refractionSubtitle,
                checked = style.refraction,
                enabled = enabled,
                onCheckedChange = { onStyleChange(style.copy(refraction = it)) }
            )
            // Plain-language budget for the optical glass. Deliberately not labelled with the
            // internal tiers: "Auto" is the only one a non-technical user has to reason about.
            GlassEffectsTile(
                title = copy.effects,
                subtitle = copy.effectsSubtitle,
                selected = style.effects,
                enabled = enabled,
                copy = copy,
                onSelect = { onStyleChange(style.copy(effects = it)) }
            )
        }
        Text(
            if (enabled) copy.performanceNote else copy.disabledNote,
            color = DerbisMuted,
            fontSize = 12.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(12f),
            modifier = Modifier.padding(horizontal = DerbisPlayerDesign.SpaceXs)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun GlassTileIcon(icon: ImageVector, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(if (enabled) scheme.secondaryContainer else scheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassLevelTile(
    label: String,
    icon: ImageVector,
    value: Int,
    enabled: Boolean,
    shape: Shape,
    onCommit: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val percent = "${draft.roundToInt()}%"
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassTileIcon(icon = icon, enabled = enabled)
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = if (enabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
                    contentColor = if (enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                ) {
                    Text(
                        percent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            val sliderState = remember { SliderState(draft, 0, 0f..100f) }
            sliderState.value = draft
            Slider(
                state = sliderState,
                onValueChange = { draft = it },
                onValueChangeFinished = {
                    val committed = draft.roundToInt().coerceIn(0, 100)
                    if (committed != value) onCommit(committed)
                },
                enabled = enabled,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(interactionSource = interactionSource, enabled = enabled)
                },
                track = { sliderState: SliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        enabled = enabled,
                        modifier = Modifier.height(GlassSliderTrackHeight)
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = percent
                }
            )
        }
    }
}

@Composable
private fun GlassRefractionTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassTileLastShape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassTileIcon(icon = Icons.Rounded.AutoAwesome, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

/**
 * Three-option chooser for [DerbisGlassEffects]. Each row carries a plain hint so the choice
 * reads as "how it looks / how fast it feels", never as a rendering quality tier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassEffectsTile(
    title: String,
    subtitle: String,
    selected: DerbisGlassEffects,
    enabled: Boolean,
    copy: GlassStudioCopy,
    onSelect: (DerbisGlassEffects) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val options = listOf(
        Triple(DerbisGlassEffects.Automatic, copy.effectsAuto, copy.effectsAutoHint),
        Triple(DerbisGlassEffects.AlwaysOn, copy.effectsAlwaysOn, copy.effectsAlwaysOnHint),
        Triple(DerbisGlassEffects.Off, copy.effectsOff, copy.effectsOffHint)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassTileLastShape)
            .background(if (enabled) scheme.surfaceContainerHigh else scheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassTileIcon(icon = Icons.Rounded.AutoAwesome, enabled = enabled)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = if (enabled) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = DerbisTypeRhythm.lineHeight(13f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
            options.forEach { (value, label, hint) ->
                val active = selected == value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) scheme.secondaryContainer
                            else Color.Transparent
                        )
                        .then(
                            if (enabled) {
                                Modifier.toggleable(
                                    value = active,
                                    role = Role.RadioButton,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onValueChange = { onSelect(value) }
                                ).semantics { stateDescription = if (active) label else "" }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) scheme.primary
                                else scheme.surfaceContainerHighest
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            hint,
                            color = scheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
