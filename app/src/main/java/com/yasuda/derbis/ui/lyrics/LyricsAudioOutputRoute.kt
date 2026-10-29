package com.yasuda.derbis.ui.lyrics

import androidx.compose.runtime.Composable
import com.yasuda.derbis.feature.audio.DerbisAudioOutputRoute
import com.yasuda.derbis.feature.audio.rememberDerbisAudioOutputState
import com.yasuda.derbis.feature.audio.resolveDerbisPreTiramisuAudioOutputRoute
import com.yasuda.derbis.feature.audio.selectDerbisAudioOutputRoute
import com.yasuda.derbis.feature.audio.stableDerbisAudioRouteKey

typealias LyricsAudioOutputRoute = DerbisAudioOutputRoute

internal fun selectLyricsAudioOutputRoute(
    routes: List<LyricsAudioOutputRoute>,
    systemOrdered: Boolean
): LyricsAudioOutputRoute? = selectDerbisAudioOutputRoute(routes, systemOrdered)

internal fun stableLyricsAudioRouteKey(type: Int, address: String?): String? =
    stableDerbisAudioRouteKey(type, address)

@Composable
fun rememberLyricsAudioOutputRoute(): LyricsAudioOutputRoute? =
    rememberDerbisAudioOutputState().active

internal fun resolvePreTiramisuAudioOutputRoute(
    routes: List<LyricsAudioOutputRoute>,
    selectedRouteName: String?,
    selectedDeviceType: Int
): LyricsAudioOutputRoute? =
    resolveDerbisPreTiramisuAudioOutputRoute(routes, selectedRouteName, selectedDeviceType)
