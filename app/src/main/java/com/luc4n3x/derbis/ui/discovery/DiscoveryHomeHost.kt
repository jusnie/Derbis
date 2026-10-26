package com.luc4n3x.derbis.ui.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.derbis.domain.HomeStyle
import com.luc4n3x.derbis.ui.theme.DerbisSkinManagesLayout
import com.luc4n3x.derbis.viewmodel.DiscoveryViewModel

@Composable
internal fun DiscoveryHomeHost(
    factory: ViewModelProvider.Factory,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    backEnabled: Boolean,
    style: HomeStyle,
    animationsEnabled: Boolean,
    bottomInset: Dp,
    content: @Composable () -> Unit
) {
    if (DerbisSkinManagesLayout) {
        content()
        return
    }
    val discovery: DiscoveryViewModel = viewModel(key = "derbis-discovery", factory = factory)
    val openChange by rememberUpdatedState(onOpenChange)
    val handle = remember(discovery) {
        DiscoveryHomeHandle(
            state = discovery.state,
            onWarm = discovery::warm,
            onOpen = { openChange(true) },
            onRetry = discovery::retry
        )
    }
    CompositionLocalProvider(LocalHomeDiscovery provides handle) { content() }
    if (open) {
        DiscoveryScreen(
            viewModel = discovery,
            style = style,
            animationsEnabled = animationsEnabled,
            bottomInset = bottomInset,
            backEnabled = backEnabled,
            onDismiss = { openChange(false) },
            onOpenSaved = {
                openChange(false)
                discovery.openSavedPlaylist()
            }
        )
    }
}
