package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisYtMusicPairingTest {

    @Test
    fun choosingYtMusicBarAlsoPicksItsMiniPlayer() {
        assertEquals(
            MiniPlayerStyle.YtMusic,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.YtMusic, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.YtMusic,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.Original, MiniPlayerStyle.YtMusic, NavStyle.Minimal)
        )
    }

    @Test
    fun leavingYtMusicRestoresTheOriginalPartner() {
        assertEquals(
            MiniPlayerStyle.Original,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.YtMusic, NavStyle.Minimal, MiniPlayerStyle.YtMusic)
        )
        assertEquals(
            NavStyle.Original,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.YtMusic, MiniPlayerStyle.Floating, NavStyle.YtMusic)
        )
    }

    @Test
    fun otherStyleChangesLeaveThePartnerAlone() {
        assertEquals(
            MiniPlayerStyle.Floating,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.Minimal, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.LiquidGlass,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.Original, MiniPlayerStyle.Floating, NavStyle.LiquidGlass)
        )
    }

    @Test
    fun connectedDockPairsWithItsMiniPlayerBothWays() {
        assertEquals(
            MiniPlayerStyle.Connected,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.FloatingDock, NavStyle.ConnectedDock, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.ConnectedDock,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.Floating, MiniPlayerStyle.Connected, NavStyle.SplitDock)
        )
        assertEquals(
            MiniPlayerStyle.Original,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.ConnectedDock, NavStyle.SplitDock, MiniPlayerStyle.Connected)
        )
        assertEquals(
            NavStyle.Original,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.Connected, MiniPlayerStyle.Floating, NavStyle.ConnectedDock)
        )
    }

    @Test
    fun switchingBetweenPairedStylesSwapsThePartnerDirectly() {
        assertEquals(
            MiniPlayerStyle.Connected,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.YtMusic, NavStyle.ConnectedDock, MiniPlayerStyle.YtMusic)
        )
        assertEquals(
            NavStyle.YtMusic,
            DerbisYtMusicPairing.navStyleFor(MiniPlayerStyle.Connected, MiniPlayerStyle.YtMusic, NavStyle.ConnectedDock)
        )
    }

    @Test
    fun splitDockLeavesTheMiniPlayerAlone() {
        assertEquals(
            MiniPlayerStyle.LiquidGlass,
            DerbisYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.SplitDock, MiniPlayerStyle.LiquidGlass)
        )
    }

    @Test
    fun newDockStylesRoundTripAndFloat() {
        listOf(NavStyle.SplitDock, NavStyle.ConnectedDock).forEach { style ->
            assertEquals(true, style.floating)
            val settings = DerbisNavigationStudioSettings()
                .withStyle(style)
                .updateTuning(style) { it.copy(cornerDp = 24, heightDp = 60) }
            assertEquals(settings, DerbisNavigationStudioSettings.decode(settings.encode()))
            assertEquals(24, settings.activeTuning().cornerDp)
        }
        val mini = DerbisMiniPlayerStudioSettings().withStyle(MiniPlayerStyle.Connected)
        assertEquals(mini, DerbisMiniPlayerStudioSettings.decode(mini.encode()))
        assertEquals(16, mini.activeTuning().insetDp)
        assertEquals(MiniProgressStyle.Line, mini.activeTuning().progress)
    }

    @Test
    fun ytMusicDefaultsAreFlatAndIndicatorFree() {
        val nav = DerbisNavigationStudioSettings.spec(NavStyle.YtMusic).defaults
        assertEquals(NavIndicator.None, nav.indicator)
        assertEquals(0, nav.cornerDp)
        assertEquals(NavLabelMode.Always, nav.labels)
        val mini = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.YtMusic).defaults
        assertEquals(0, mini.cornerDp)
        assertEquals(0, mini.insetDp)
        assertEquals(MiniControlSet.Full, mini.controls)
        assertEquals(MiniButtonStyle.Plain, mini.buttons)
        val settings = DerbisNavigationStudioSettings().withStyle(NavStyle.YtMusic)
        assertEquals(settings, DerbisNavigationStudioSettings.decode(settings.encode()))
        val miniSettings = DerbisMiniPlayerStudioSettings().withStyle(MiniPlayerStyle.YtMusic)
        assertEquals(miniSettings, DerbisMiniPlayerStudioSettings.decode(miniSettings.encode()))
    }
}
