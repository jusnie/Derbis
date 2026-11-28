package com.yasuda.derbis.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yasuda.derbis.domain.DerbisDockActions
import com.yasuda.derbis.domain.DerbisDockIcons
import com.yasuda.derbis.domain.DerbisDockItem
import com.yasuda.derbis.domain.DerbisDockLabelMode
import com.yasuda.derbis.domain.DerbisTab
import com.yasuda.derbis.domain.DerbisTabBarConfig
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
/**
 * One resolved slot of the tab bar: a stable key, the catalog action behind it, the builtin tab
 * it maps to (null for shortcuts/customs), the (possibly empty, e.g. icons-only mode) label and
 * the glyph to render. Built by [rememberDockEntries] from the persisted [DerbisTabBarConfig].
 */
data class DerbisDockEntry(
    val key: String,
    val action: String,
    val tab: DerbisTab?,
    val label: String,
    val icon: ImageVector,
    /** Non-blank accessibility label (icons-only mode blanks [label] but not this). */
    val spoken: String = label
)

/** The glyph for an icon-picker id. Falls back to the grid glyph for anything unknown. */
fun derbisDockIconVector(iconId: String): ImageVector = when (iconId) {
    DerbisDockIcons.Home -> Icons.Rounded.Home
    DerbisDockIcons.List -> Icons.Rounded.LibraryMusic
    DerbisDockIcons.Search -> Icons.Rounded.Search
    DerbisDockIcons.Compass -> Icons.Rounded.Explore
    DerbisDockIcons.Playlist -> Icons.AutoMirrored.Rounded.QueueMusic
    DerbisDockIcons.Plus -> Icons.Rounded.Add
    DerbisDockIcons.Heart -> Icons.Rounded.Favorite
    DerbisDockIcons.Note -> Icons.Rounded.MusicNote
    DerbisDockIcons.Star -> Icons.Rounded.Star
    DerbisDockIcons.Person -> Icons.Rounded.Person
    DerbisDockIcons.Download -> Icons.Rounded.Download
    DerbisDockIcons.Clock -> Icons.Rounded.AccessTime
    DerbisDockIcons.Bolt -> Icons.Rounded.Bolt
    DerbisDockIcons.Chart -> Icons.Rounded.BarChart
    DerbisDockIcons.Folder -> Icons.Rounded.Folder
    DerbisDockIcons.People -> Icons.Rounded.Groups
    DerbisDockIcons.Grid -> Icons.Rounded.GridView
    else -> Icons.Rounded.GridView
}

/** Localized default label for a catalog action. */
@Composable
fun dockActionLabel(action: String): String {
    val strings = LocalDerbisStrings.current
    return when (action) {
        DerbisDockActions.Home -> strings.home
        DerbisDockActions.Search -> strings.search
        DerbisDockActions.Explore -> strings.explore
        DerbisDockActions.Library -> strings.library
        DerbisDockActions.Playlists -> strings.playlistsPlain
        DerbisDockActions.Favorites -> strings.favoritesPlain
        DerbisDockActions.Downloads -> strings.downloads
        DerbisDockActions.NowPlaying -> strings.nowPlaying
        DerbisDockActions.Jam -> strings.jamTitle.ifBlank { "Jam" }
        DerbisDockActions.Artists -> strings.artists
        DerbisDockActions.Recent -> strings.recent
        DerbisDockActions.Stats -> strings.listeningInsights
        DerbisDockActions.Local -> strings.localOnDevice
        else -> action
    }
}

/** Resolve a dock item's glyph: user override, else the catalog default. */
fun derbisDockItemIcon(item: DerbisDockItem): ImageVector =
    derbisDockIconVector(item.icon ?: DerbisDockIcons.defaultFor(item.action))

/**
 * Build the ordered list of entries to render. In icons-only mode every label is blanked so the
 * renderer draws the glyph-only pill. The mode also rides on the config for non-dock call sites.
 */
@Composable
fun rememberDockEntries(config: DerbisTabBarConfig): List<DerbisDockEntry> {
    val strings = LocalDerbisStrings.current
    return remember(config, strings) {
        val labels = mapOf(
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
        config.enabledItems().map { item ->
            val defaultLabel = labels[item.action] ?: item.action
            DerbisDockEntry(
                key = item.id,
                action = item.action,
                tab = when (item.action) {
                    DerbisDockActions.Home -> DerbisTab.Home
                    DerbisDockActions.Search -> DerbisTab.Search
                    DerbisDockActions.Explore -> DerbisTab.Explore
                    DerbisDockActions.Library -> DerbisTab.Library
                    else -> null
                },
                label = if (config.mode == DerbisDockLabelMode.IconsOnly) {
                    ""
                } else {
                    item.label?.takeIf { it.isNotBlank() } ?: defaultLabel
                },
                icon = derbisDockItemIcon(item),
                spoken = item.label?.takeIf { it.isNotBlank() } ?: defaultLabel
            )
        }
    }
}

/** Convert legacy tab entries into dock entries (used when the tab-bar config is not supplied). */
internal fun dockEntriesFromTabEntries(entries: List<DerbisTabEntry>): List<DerbisDockEntry> =
    entries.map { tabEntry ->
        val action = when (tabEntry.tab) {
            DerbisTab.Home -> DerbisDockActions.Home
            DerbisTab.Search -> DerbisDockActions.Search
            DerbisTab.Explore -> DerbisDockActions.Explore
            DerbisTab.Library -> DerbisDockActions.Library
            DerbisTab.Player -> "player"
        }
        DerbisDockEntry(
            key = action,
            action = action,
            tab = tabEntry.tab,
            label = tabEntry.label,
            icon = tabEntry.selectedIcon
        )
    }

/** The selection key for a builtin tab; dock shortcuts have no persistent selection. */
fun derbisDockSelectionKey(tab: DerbisTab): String = when (tab) {
    DerbisTab.Home -> DerbisDockActions.Home
    DerbisTab.Search -> DerbisDockActions.Search
    DerbisTab.Explore -> DerbisDockActions.Explore
    DerbisTab.Library -> DerbisDockActions.Library
    DerbisTab.Player -> ""
}

/**
 * Selection test for dock entries. Builtin-bound destinations (including custom tabs bound to
 * one) light up when that builtin tab is active; pure shortcuts match nothing. Blank selection
 * keys (Player is "at no builtin tab") never match, so nothing appears pressed on the player.
 */
fun derbisDockEntrySelected(entry: DerbisDockEntry, selectedKey: String): Boolean =
    selectedKey.isNotEmpty() && entry.tab != null && derbisDockSelectionKey(entry.tab!!) == selectedKey

