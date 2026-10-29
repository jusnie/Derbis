package com.yasuda.derbis.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.domain.DerbisTab
import com.yasuda.derbis.domain.NavTuning
import com.yasuda.derbis.ui.theme.*
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.derbisGlass

@Composable
internal fun PrismBottomTabs(tuning: NavTuning, selected: DerbisTab, onSelect: (DerbisTab) -> Unit, applySystemInset: Boolean) {
    val all = rememberDerbisTabEntries()
    val entries = remember(all, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
    val haptics = LocalDerbisHaptics.current
    val measuredHeight = LocalNocturneNavigationHeight.current
    val density = LocalDensity.current
    val torque = DerbisIsTorque
    val skinModifier = Modifier.onSizeChanged { size -> measuredHeight?.value = with(density) { size.height.toDp() } }
    val skinSelect: (DerbisTabEntry, Boolean) -> Unit = { entry, active ->
        if (!active && tuning.haptics) haptics.perform(DerbisHapticAction.SeekSnap)
        onSelect(entry.tab)
    }
    if (SkinBottomTabs(entries, selected, applySystemInset, skinModifier, skinSelect)) return
    val dockShape = if (torque) RectangleShape else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val liquidGlass = DerbisIsLiquidGlass
    val dockModifier = if (liquidGlass) {
        Modifier.derbisGlass(
            shape = dockShape,
            baseTint = Color.White,
            fallbackColor = prismGlassColor(),
            fallbackBorderColor = Color.White.copy(alpha = if (DerbisActivePalette.isLight) 0.32f else 0.22f),
            intensity = DerbisGlassIntensity.Strong,
            artworkTint = DerbisActivePalette.cyan
        )
    } else {
        Modifier
    }
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    measuredHeight?.value = with(density) { size.height.toDp() }
                }
                .then(dockModifier),
            shape = dockShape,
            color = if (liquidGlass) Color.Transparent else prismGlassColor(),
            border = if (liquidGlass) null else if (DerbisActivePalette.isLight) BorderStroke(PrismDesign.hairline, PrismAccent.Hairline) else null
        ) {
            Column {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                entries.forEach { entry ->
                    val active = entry.tab == selected
                    PrismNavigationTab(entry, active) {
                        if (!active && tuning.haptics) haptics.perform(DerbisHapticAction.SeekSnap)
                        onSelect(entry.tab)
                    }
                }
            }
            if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SkinBottomTabs(
    entries: List<DerbisTabEntry>, selected: DerbisTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (DerbisTabEntry, Boolean) -> Unit
): Boolean {
    when {
        DerbisIsSpotify -> SpotifyBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        DerbisIsAppleMusic -> AppleMusicBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        DerbisIsYouTubeMusic -> YouTubeMusicBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        DerbisIsIvory -> IvoryBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        else -> return false
    }
    return true
}

@Composable
private fun RowScope.PrismNavigationTab(entry: DerbisTabEntry, active: Boolean, onClick: () -> Unit) {
    val torque = DerbisIsTorque
    val liquidGlass = DerbisIsLiquidGlass
    val tint = if (active) DerbisCyan else DerbisMuted
    val indicatorShape = RoundedCornerShape(if (torque) 2.dp else 15.dp)
    val indicatorMaterial = when {
        active && liquidGlass -> Modifier.derbisGlass(
            shape = indicatorShape,
            baseTint = DerbisActivePalette.cyan,
            fallbackColor = DerbisActivePalette.cyan.copy(alpha = 0.28f),
            fallbackBorderColor = Color.White.copy(alpha = if (DerbisActivePalette.isLight) 0.32f else 0.24f),
            // The active state belongs within an already-frosted dock, so it is a quiet material
            // fill rather than another blur layer.
            intensity = DerbisGlassIntensity.Subtle,
            artworkTint = DerbisActivePalette.cyan
        )
        active && !torque -> Modifier.background(PrismAccent.Tint, indicatorShape)
        else -> Modifier.background(Color.Transparent, indicatorShape)
    }
    Column(
        Modifier.weight(1f).fillMaxHeight().heightIn(min = 60.dp).clip(RoundedCornerShape(PrismDesign.controlCorner))
            .selectable(active, role = Role.Tab, onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.width(52.dp).height(30.dp).then(indicatorMaterial),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (active) entry.selectedIcon else entry.unselectedIcon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(entry.label, color = if (active) DerbisText else DerbisMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (torque) Box(Modifier.padding(top = 4.dp).width(20.dp).height(3.dp).background(if (active) DerbisActivePalette.blue else Color.Transparent))
    }
}

@Composable
private fun IvoryBottomTabs(
    entries: List<DerbisTabEntry>, selected: DerbisTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (DerbisTabEntry, Boolean) -> Unit
) {
    Column(modifier.fillMaxWidth().background(DerbisActivePalette.ink)) {
        Box(Modifier.fillMaxWidth().height(PrismDesign.hairline).background(DerbisActivePalette.outline))
        Row(Modifier.fillMaxWidth().height(66.dp).selectableGroup().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
            entries.forEach { entry ->
                val active = entry.tab == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (active) Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f).clip(IvoryNotchShape).background(IvoryDesign.ink))
                    Column(
                        Modifier.fillMaxSize().padding(top = 10.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (active) entry.selectedIcon else entry.unselectedIcon, null,
                            tint = if (active) IvoryDesign.onInk else DerbisText, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            entry.label, color = if (active) IvoryDesign.onInk else DerbisMuted, style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
