package com.yasuda.derbis.desktop.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yasuda.derbis.desktop.app.state.DiscoverUiState
import com.yasuda.derbis.desktop.app.ui.components.TrackActions
import com.yasuda.derbis.desktop.core.model.Track
import com.yasuda.derbis.desktop.core.storage.LibraryData

@Composable
fun HomeScreen(
    library: LibraryData,
    discover: DiscoverUiState,
    actions: TrackActions,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onPlayMix: (List<Track>) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeScreen(
        library = library,
        discover = discover,
        currentTrack = null,
        actions = actions,
        onOpenSearch = onOpenSearch,
        onOpenDiscover = onOpenDiscover,
        onOpenNewReleases = onOpenNewReleases,
        onPlayMix = onPlayMix,
        onOpenPlaylist = onOpenPlaylist,
        onCreatePlaylist = onCreatePlaylist,
        onImportUrl = onImportUrl,
        onClearHistory = onClearHistory,
        modifier = modifier
    )
}
