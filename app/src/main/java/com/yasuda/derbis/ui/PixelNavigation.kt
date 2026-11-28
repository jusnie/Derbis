package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisTab
import com.yasuda.derbis.domain.NavTuning
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelIcons
import com.yasuda.derbis.ui.theme.PixelPalette
import com.yasuda.derbis.ui.theme.PixelifyFont
import java.util.Locale

internal val LocalPixelNavigationHeight = compositionLocalOf<MutableState<Dp>?> { null }

@Composable
internal fun PixelBottomTabs(tuning: NavTuning, selected: DerbisTab, onSelect: (DerbisTab) -> Unit, applySystemInset: Boolean) {
    val measuredHeight = LocalPixelNavigationHeight.current
    val density = LocalDensity.current
    val haptics = LocalDerbisHaptics.current
    val all = rememberDerbisTabEntries()
    val entries = remember(all, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
    val p = PixelPalette
    Column(Modifier.fillMaxWidth().background(p.panel)) {
        HorizontalDivider(color = p.outline, thickness = PixelDesign.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onSizeChanged { size -> measuredHeight?.value = with(density) { size.height.toDp() } }
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { entry ->
                val active = entry.tab == selected
                val tint = if (active) p.text else p.muted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 62.dp)
                        .background(if (active) p.ink else Color.Transparent)
                        .selectable(
                            selected = active,
                            onClick = {
                                if (!active && tuning.haptics) haptics.perform(DerbisHapticAction.SeekSnap)
                                onSelect(entry.tab)
                            },
                            role = Role.Tab
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (active) p.cyan else Color.Transparent)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = PixelIcons.resolve(entry.unselectedIcon),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.label.uppercase(Locale.getDefault()),
                            color = tint,
                            fontFamily = PixelifyFont,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
