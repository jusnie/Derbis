package com.yasuda.derbis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.yasuda.derbis.domain.DerbisDockActions
import com.yasuda.derbis.domain.DerbisDockIcons
import com.yasuda.derbis.domain.DerbisDockItem
import com.yasuda.derbis.domain.DerbisDockLabelMode
import com.yasuda.derbis.domain.DerbisTabBarConfig
import com.yasuda.derbis.ui.components.DerbisBackButton
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.TabBarText
import com.yasuda.derbis.ui.i18n.tabBarCopy
import com.yasuda.derbis.ui.i18n.speedDialCopy
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import kotlinx.coroutines.delay

private val TabBarCardShape = RoundedCornerShape(24.dp)
private val TabBarPillShape = RoundedCornerShape(percent = 50)

private typealias TabBarActionLabels = Map<String, String>

@Composable
private fun rememberDockActionLabels(): TabBarActionLabels {
    val strings = LocalDerbisStrings.current
    return remember(strings) {
        mapOf(
            DerbisDockActions.Home to strings.home,
            DerbisDockActions.Search to strings.search,
            DerbisDockActions.Explore to strings.explore,
            DerbisDockActions.Library to strings.library,
            DerbisDockActions.Playlists to strings.playlistsPlain,
            DerbisDockActions.Favorites to strings.favoritesPlain,
            DerbisDockActions.Downloads to strings.downloads,
            DerbisDockActions.NowPlaying to strings.nowPlaying,
            DerbisDockActions.Jam to strings.jamTitle.ifBlank { "Jam" },
            DerbisDockActions.Artists to strings.artists,
            DerbisDockActions.Recent to strings.recent,
            DerbisDockActions.Stats to strings.listeningInsights,
            DerbisDockActions.Local to strings.localOnDevice
        )
    }
}

/** Reduced-motion aware physics: when animations are disabled, everything snaps. */
@Composable
private fun <T> tabBarAnimSpec(
    dampingRatio: Float = 0.8f,
    stiffness: Float = Spring.StiffnessMedium
): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalAnimationsEnabled.current) {
        spring(dampingRatio = dampingRatio, stiffness = stiffness)
    } else {
        androidx.compose.animation.core.snap()
    }

private fun tabBarReorderStep(
    order: List<String>,
    id: String,
    offset: Float,
    heights: Map<String, Int>
): Pair<List<String>, Float>? {
    val position = order.indexOf(id)
    if (position < 0) return null
    val nextHeight = order.getOrNull(position + 1)?.let(heights::get)?.toFloat()
    val previousHeight = order.getOrNull(position - 1)?.let(heights::get)?.toFloat()
    return when {
        nextHeight != null && offset > nextHeight / 2f ->
            order.toMutableList().apply { add(position + 1, removeAt(position)) } to offset - nextHeight
        previousHeight != null && offset < -previousHeight / 2f ->
            order.toMutableList().apply { add(position - 1, removeAt(position)) } to offset + previousHeight
        else -> null
    }
}

@Composable
internal fun TabBarEditorOverlay(
    config: DerbisTabBarConfig,
    onUpdate: (DerbisTabBarConfig) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val copy = strings.tabBarCopy
    val haptics = LocalDerbisHaptics.current
    val latest by rememberUpdatedState(config)
    val actionLabels = rememberDockActionLabels()

    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var workingOrder by remember(config.items) { mutableStateOf(config.items.map { it.id }) }
    val rowHeights = remember { mutableStateMapOf<String, Int>() }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var editItemId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var previewKey by remember(config) { mutableStateOf<String?>(config.enabledItems().firstOrNull()?.id) }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2600L)
            notice = null
        }
    }

    fun commitOrder(order: List<String>) {
        val byId = latest.items.associateBy { it.id }
        val merged = order.mapNotNull(byId::get)
        if (merged.size == latest.items.size && merged.map { it.id } != latest.items.map { it.id }) {
            onUpdate(latest.copy(items = merged))
        }
    }

    fun toggle(id: String) {
        val item = latest.items.firstOrNull { it.id == id } ?: return
        if (item.enabled && latest.enabledCount() <= DerbisTabBarConfig.MIN_ENABLED) {
            haptics.perform(DerbisHapticAction.SeekSnap)
            notice = copy[TabBarText.LastTab]
            return
        }
        if (!item.enabled && latest.enabledCount() >= DerbisTabBarConfig.MAX_ENABLED) {
            haptics.perform(DerbisHapticAction.SeekSnap)
            notice = copy[TabBarText.MaxTabs]
            return
        }
        haptics.perform(DerbisHapticAction.SeekSnap)
        onUpdate(latest.withToggled(id))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DerbisInk)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DerbisBackButton(onClick = onClose, contentDescription = strings.back)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = copy[TabBarText.Title],
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = DerbisText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = copy[TabBarText.Subtitle],
                        style = MaterialTheme.typography.bodyMedium,
                        color = DerbisMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "tabbar-preview", contentType = "preview") {
                    TabBarPreviewCard(
                        config = config,
                        selectedKey = previewKey,
                        onSelect = { previewKey = it },
                        actionLabels = actionLabels
                    )
                }

                item(key = "tabbar-appearance-label", contentType = "label") {
                    Text(
                        text = copy[TabBarText.Appearance],
                        style = MaterialTheme.typography.labelLarge,
                        color = DerbisMuted
                    )
                }
                item(key = "tabbar-appearance", contentType = "appearance") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TabBarModeCard(
                            title = copy[TabBarText.IconsOnly],
                            selected = config.mode == DerbisDockLabelMode.IconsOnly,
                            iconsOnlySample = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptics.perform(DerbisHapticAction.Confirm)
                            onUpdate(config.withMode(DerbisDockLabelMode.IconsOnly))
                        }
                        TabBarModeCard(
                            title = copy[TabBarText.IconsAndNames],
                            selected = config.mode == DerbisDockLabelMode.IconsAndNames,
                            iconsOnlySample = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptics.perform(DerbisHapticAction.Confirm)
                            onUpdate(config.withMode(DerbisDockLabelMode.IconsAndNames))
                        }
                    }
                }

                item(key = "tabbar-tabs-label", contentType = "label") {
                    Column {
                        Text(
                            text = copy[TabBarText.Tabs],
                            style = MaterialTheme.typography.labelLarge,
                            color = DerbisMuted
                        )
                        Text(
                            text = copy[TabBarText.TabsHint],
                            style = MaterialTheme.typography.bodySmall,
                            color = DerbisMuted
                        )
                    }
                }

                items(workingOrder, key = { "tabbar-row-$it" }, contentType = { "tabbar-row" }) { id ->
                    val item = config.items.firstOrNull { it.id == id } ?: return@items
                    val index = workingOrder.indexOf(id)
                    val isDragging = dragging == id
                    val onMove: (Int) -> Unit = { delta ->
                        latest.movedBy(id, delta)?.let { moved ->
                            haptics.perform(DerbisHapticAction.Reorder)
                            onUpdate(moved)
                        }
                    }
                    TabBarItemRow(
                        item = item,
                        title = item.label?.takeIf { it.isNotBlank() }
                            ?: actionLabels[item.action].orEmpty(),
                        actionLabel = actionLabels[item.action].orEmpty(),
                        dragging = isDragging,
                        dragOffset = { dragOffset },
                        copy = copy,
                        moveActions = listOfNotNull(
                            CustomAccessibilityAction(strings.speedDialCopy().moveEarlier) { onMove(-1) }
                                .takeIf { index > 0 },
                            CustomAccessibilityAction(strings.speedDialCopy().moveLater) { onMove(1) }
                                .takeIf { index < workingOrder.lastIndex }
                        ),
                        onMeasured = { rowHeights[id] = it },
                        onDragStart = {
                            dragging = id
                            dragOffset = 0f
                            haptics.perform(DerbisHapticAction.Confirm)
                        },
                        onDragEnd = {
                            dragging = null
                            dragOffset = 0f
                            commitOrder(workingOrder)
                        },
                        onDrag = { delta ->
                            dragOffset += delta
                            tabBarReorderStep(workingOrder, id, dragOffset, rowHeights)?.let { (order, offset) ->
                                workingOrder = order
                                dragOffset = offset
                                haptics.perform(DerbisHapticAction.Reorder)
                            }
                        },
                        onToggle = { toggle(id) },
                        onEdit = { editItemId = id }
                    )
                }

                item(key = "tabbar-notice", contentType = "notice") {
                    AnimatedVisibility(
                        visible = notice != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = notice.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DerbisCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TabBarCardShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .semantics { contentDescription = notice.orEmpty() }
                        )
                    }
                }

                item(key = "tabbar-add", contentType = "add") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TabBarCardShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .derbisPressable(
                                onClick = { adding = true },
                                onClickLabel = copy[TabBarText.AddTab],
                                role = Role.Button
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DerbisCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.Add, contentDescription = null, tint = DerbisCyan)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = copy[TabBarText.AddTab],
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DerbisText
                            )
                            Text(
                                text = copy[TabBarText.AddTabSubtitle],
                                style = MaterialTheme.typography.bodySmall,
                                color = DerbisMuted,
                                maxLines = 2
                            )
                        }
                    }
                }

                item(key = "tabbar-reset", contentType = "actions") {
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copy[TabBarText.Reset], maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(copy[TabBarText.ResetTitle]) },
            text = { Text(copy[TabBarText.ResetBody]) },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(DerbisTabBarConfig.default())
                    haptics.perform(DerbisHapticAction.Confirm)
                    confirmReset = false
                }) { Text(copy[TabBarText.Reset]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(strings.cancel) }
            }
        )
    }

    if (adding) {
        TabEditSheet(
            initial = null,
            actionLabels = actionLabels,
            onDismiss = { adding = false },
            onSave = { action, label, icon ->
                val updated = latest.withCustomTab(action, label, icon)
                onUpdate(updated)
                haptics.perform(DerbisHapticAction.Confirm)
                adding = false
            },
            onDelete = null
        )
    }

    val editingId = editItemId
    if (editingId != null) {
        val item = latest.items.firstOrNull { it.id == editingId }
        if (item != null) {
            TabEditSheet(
                initial = item,
                actionLabels = actionLabels,
                onDismiss = { editItemId = null },
                onSave = { action, label, icon ->
                    val withAction = if (item.isCustom && item.action != action) {
                        latest.copy(items = latest.items.map { if (it.id == item.id) it.copy(action = action) else it })
                    } else {
                        latest
                    }
                    onUpdate(withAction.withOverride(item.id, label, icon))
                    haptics.perform(DerbisHapticAction.Confirm)
                    editItemId = null
                },
                onDelete = if (item.isCustom) {
                    {
                        onUpdate(latest.withRemoved(item.id))
                        haptics.perform(DerbisHapticAction.Confirm)
                        editItemId = null
                    }
                } else {
                    null
                }
            )
        } else {
            editItemId = null
        }
    }
}

@Composable
private fun TabBarPreviewCard(
    config: DerbisTabBarConfig,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    actionLabels: TabBarActionLabels
) {
    val entries = rememberDockEntries(config)
    val capsule = TabBarPillShape
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TabBarCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        // The floating pill, exactly like the one that renders over apps.
        Row(
            modifier = Modifier
                .clip(capsule)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2B2B31), Color(0xFF17171B))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), capsule)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { entry ->
                val active = entry.key == selectedKey || (selectedKey == null && entry == entries.firstOrNull())
                val tint by animateColorAsState(
                    targetValue = if (active) DerbisCyan else Color.White.copy(alpha = 0.66f),
                    animationSpec = tabBarAnimSpec(),
                    label = "tabbar-preview-tint"
                )
                Box(
                    modifier = Modifier
                        .clip(capsule)
                        .background(if (active) DerbisCyan.copy(alpha = 0.18f) else Color.Transparent)
                        .derbisPressable(onClick = { onSelect(entry.key) }, role = Role.Tab)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (config.mode == DerbisDockLabelMode.IconsOnly) {
                        androidx.compose.material3.Icon(
                            imageVector = entry.icon,
                            contentDescription = entry.label.ifEmpty { actionLabels[entry.action] },
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = entry.label,
                                color = tint,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBarModeCard(
    title: String,
    selected: Boolean,
    iconsOnlySample: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.55f),
        animationSpec = tabBarAnimSpec(),
        label = "tabbar-mode-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tabBarAnimSpec(),
        label = "tabbar-mode-card-border-width"
    )
    Column(
        modifier = modifier
            .clip(TabBarCardShape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .border(borderWidth, borderColor, TabBarCardShape)
            .derbisPressable(onClick = onClick, onClickLabel = title, role = Role.RadioButton)
            .semantics {
                this.selected = selected
            }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sample strip: three glyphs, or glyph + name pairs.
        Row(
            modifier = Modifier
                .clip(TabBarPillShape)
                .background(Color(0xFF17171B))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (iconsOnlySample) 16.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sampleIcons = listOf(Icons.Rounded.Home, Icons.Rounded.Search, Icons.Rounded.LibraryMusic)
            val sampleNames = listOf("Home", "Search", "Library")
            sampleIcons.forEachIndexed { index, vector ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        imageVector = vector,
                        contentDescription = null,
                        tint = if (index == 0) DerbisCyan else Color.White.copy(alpha = 0.62f),
                        modifier = Modifier.size(if (iconsOnlySample) 16.dp else 13.dp)
                    )
                    if (!iconsOnlySample) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = sampleNames[index],
                            color = if (index == 0) DerbisCyan else Color.White.copy(alpha = 0.62f),
                            fontSize = 7.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (selected) scheme.onSecondaryContainer else DerbisText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LazyItemScope.TabBarItemRow(
    item: DerbisDockItem,
    title: String,
    actionLabel: String,
    dragging: Boolean,
    dragOffset: () -> Float,
    copy: com.yasuda.derbis.ui.i18n.TabBarCopy,
    moveActions: List<CustomAccessibilityAction>,
    onMeasured: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val icon = remember(item.action, item.icon) {
        derbisDockIconVector(item.icon ?: DerbisDockIcons.defaultFor(item.action))
    }
    val placement = if (dragging) {
        Modifier
    } else {
        Modifier.animateItem(
            placementSpec = if (LocalAnimationsEnabled.current) spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) else androidx.compose.animation.core.snap()
        )
    }
    Row(
        modifier = Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                val lifted = if (dragging) 1.02f else 1f
                translationY = if (dragging) dragOffset() else 0f
                scaleX = lifted
                scaleY = lifted
            }
            .then(placement)
            .onSizeChanged(onMeasured)
            .semantics { customActions = moveActions }
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dragging) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enable/disable checkbox. Checked = the tab shows in the bar.
        Box(
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "${copy[TabBarText.Show]}: $title"
                    stateDescription = if (item.enabled) "on" else "off"
                }
                .derbisPressable(onClick = onToggle, role = Role.Checkbox, onClickLabel = "${copy[TabBarText.Show]}: $title"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (item.enabled) DerbisCyan else Color.Transparent)
                    .border(1.6.dp, if (item.enabled) DerbisCyan else DerbisMuted.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (item.enabled) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // Icon + name. Tapping the body edits name/icon/destination.
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .derbisPressable(onClick = onEdit, onClickLabel = title, role = Role.Button)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (item.enabled) DerbisCyan.copy(alpha = 0.14f) else scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.enabled) DerbisCyan else DerbisMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (item.enabled) DerbisText else DerbisMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.isCustom) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = DerbisMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Drag grip.
        Box(
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = copy[TabBarText.DragHandle] }
                .pointerInput(item.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = DerbisMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TabEditSheet(
    initial: DerbisDockItem?,
    actionLabels: TabBarActionLabels,
    onDismiss: () -> Unit,
    onSave: (action: String, label: String?, icon: String?) -> Unit,
    onDelete: (() -> Unit)?
) {
    val strings = LocalDerbisStrings.current
    val copy = strings.tabBarCopy
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isEdit = initial != null
    val defaultAction = initial?.action ?: DerbisDockActions.Playlists
    var action by remember { mutableStateOf(defaultAction) }
    var name by remember(initial?.id) { mutableStateOf(initial?.label.orEmpty()) }
    var iconOverride by remember(initial?.id) { mutableStateOf<String?>(initial?.icon) }
    val effectiveIcon = iconOverride ?: DerbisDockIcons.defaultFor(action)
    val defaultLabel = actionLabels[action].orEmpty()
    val nameIsCustom = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = DerbisText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEdit) name.ifBlank { defaultLabel } else copy[TabBarText.AddTab],
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = DerbisText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(copy[TabBarText.Name], style = MaterialTheme.typography.labelMedium, color = DerbisMuted)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    placeholder = { Text(defaultLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameIsCustom) {
                    TextButton(onClick = { name = "" }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(copy[TabBarText.UseDefaultName])
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(copy[TabBarText.Opens], style = MaterialTheme.typography.labelMedium, color = DerbisMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DerbisDockActions.CustomDestinations.forEach { destination ->
                        val selected = action == destination
                        val vector = remember(destination) {
                            derbisDockIconVector(DerbisDockIcons.defaultFor(destination))
                        }
                        Row(
                            modifier = Modifier
                                .clip(TabBarPillShape)
                                .background(if (selected) DerbisCyan.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    1.dp,
                                    if (selected) DerbisCyan.copy(alpha = 0.6f) else Color.Transparent,
                                    TabBarPillShape
                                )
                                .derbisPressable(onClick = { action = destination }, role = Role.RadioButton, onClickLabel = actionLabels[destination])
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Icon(vector, contentDescription = null, tint = if (selected) DerbisCyan else DerbisMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = actionLabels[destination].orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) DerbisCyan else DerbisText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(copy[TabBarText.Icon], style = MaterialTheme.typography.labelMedium, color = DerbisMuted)
                // Icon picker
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(DerbisDockIcons.Picker, key = { it }) { iconId ->
                        val picked = effectiveIcon == iconId
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (picked) DerbisCyan else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .derbisPressable(
                                    onClick = { iconOverride = if (DerbisDockIcons.defaultFor(action) == iconId) null else iconId },
                                    role = Role.RadioButton,
                                    onClickLabel = iconId
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = derbisDockIconVector(iconId),
                                contentDescription = iconId,
                                tint = if (picked) Color.White else DerbisText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                if (iconOverride != null) {
                    TextButton(onClick = { iconOverride = null }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(copy[TabBarText.UseDefaultIcon])
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        androidx.compose.material3.Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(copy[TabBarText.DeleteTab])
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    TextButton(
                        onClick = {
                            onSave(
                                action,
                                name.trim().ifBlank { null },
                                iconOverride
                            )
                        }
                    ) { Text(strings.done, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

