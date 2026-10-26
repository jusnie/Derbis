package com.luc4n3x.derbis.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisAdaptiveLayoutTest {

    @Test
    fun `phones stay compact and tablets become expanded`() {
        assertEquals(DerbisLayoutMode.Compact, resolveDerbisLayoutMode(392f, 850f))
        assertEquals(DerbisLayoutMode.Medium, resolveDerbisLayoutMode(673f, 841f))
        assertEquals(DerbisLayoutMode.Expanded, resolveDerbisLayoutMode(1280f, 800f))
    }

    @Test
    fun `an unfolded inner display crosses into medium`() {
        assertEquals(DerbisLayoutMode.Compact, resolveDerbisLayoutMode(374f, 819f))
        assertEquals(DerbisLayoutMode.Medium, resolveDerbisLayoutMode(674f, 841f))
    }

    @Test
    fun `a short landscape phone is treated as medium`() {
        assertEquals(DerbisLayoutMode.Medium, resolveDerbisLayoutMode(800f, 392f))
        assertEquals(DerbisLayoutMode.Expanded, resolveDerbisLayoutMode(850f, 392f))
    }

    @Test
    fun `the player splits into two panes only when it fits`() {
        assertEquals(DerbisPlayerPane.Stacked, resolvePlayerPane(392f, 850f))
        assertEquals(DerbisPlayerPane.Stacked, resolvePlayerPane(673f, 841f))
        assertEquals(DerbisPlayerPane.SideBySide, resolvePlayerPane(1280f, 800f))
        assertEquals(DerbisPlayerPane.SideBySide, resolvePlayerPane(850f, 392f))
    }

    @Test
    fun `a very short window keeps the stacked player`() {
        assertEquals(DerbisPlayerPane.Stacked, resolvePlayerPane(900f, 300f))
    }

    @Test
    fun `only a short landscape window counts as compact landscape`() {
        assertTrue(isDerbisCompactLandscape(915f, 412f))
        assertTrue(isDerbisCompactLandscape(640f, 360f))
        assertFalse(isDerbisCompactLandscape(412f, 915f))
        assertFalse(isDerbisCompactLandscape(1280f, 800f))
        assertFalse(isDerbisCompactLandscape(841f, 673f))
        assertFalse(isDerbisCompactLandscape(400f, 400f))
        assertFalse(isDerbisCompactLandscape(915f, 0f))
    }

    @Test
    fun `video splits beside the controls only on a landscape phone`() {
        assertEquals(DerbisPlayerPane.Stacked, resolveNowPlayingPane(412f, 915f, isVideoMode = true))
        assertEquals(DerbisPlayerPane.SideBySide, resolveNowPlayingPane(915f, 412f, isVideoMode = true))
        assertEquals(DerbisPlayerPane.Stacked, resolveNowPlayingPane(1280f, 800f, isVideoMode = true))
        assertEquals(DerbisPlayerPane.Stacked, resolveNowPlayingPane(900f, 300f, isVideoMode = true))
    }

    @Test
    fun `audio keeps the existing pane resolution`() {
        listOf(392f to 850f, 673f to 841f, 1280f to 800f, 850f to 392f, 900f to 300f).forEach { (width, height) ->
            assertEquals(resolvePlayerPane(width, height), resolveNowPlayingPane(width, height, isVideoMode = false))
        }
    }

    @Test
    fun `landscape video earns a wider pane only when the controls keep room`() {
        assertEquals(1.5f, derbisCompactLandscapeVideoPaneWeight(915f), 0.0001f)
        assertEquals(1f, derbisCompactLandscapeVideoPaneWeight(640f), 0.0001f)
    }

    @Test
    fun `hero heights are capped by the viewport only in compact landscape`() {
        assertEquals(472f, derbisCompactLandscapeHeightDp(472f, 412f, 915f, 0.62f, 236f), 0.0001f)
        assertEquals(472f, derbisCompactLandscapeHeightDp(472f, 1280f, 800f, 0.62f, 236f), 0.0001f)
        assertEquals(412f * 0.62f, derbisCompactLandscapeHeightDp(472f, 915f, 412f, 0.62f, 236f), 0.0001f)
        assertEquals(236f, derbisCompactLandscapeHeightDp(472f, 600f, 300f, 0.62f, 236f), 0.0001f)
        assertEquals(200f, derbisCompactLandscapeHeightDp(200f, 915f, 412f, 0.62f, 236f), 0.0001f)
    }

    @Test
    fun `content widens with the window without becoming unbounded`() {
        assertEquals(560f, derbisContentMaxWidthDp(DerbisLayoutMode.Compact), 0.0001f)
        assertEquals(720f, derbisContentMaxWidthDp(DerbisLayoutMode.Medium), 0.0001f)
        assertEquals(1080f, derbisContentMaxWidthDp(DerbisLayoutMode.Expanded), 0.0001f)
    }

    @Test
    fun `the two pane artwork stays smaller than the stacked artwork`() {
        val sideBySide = derbisPlayerArtworkMaxWidthDp(DerbisPlayerPane.SideBySide, DerbisLayoutMode.Expanded)
        val stacked = derbisPlayerArtworkMaxWidthDp(DerbisPlayerPane.Stacked, DerbisLayoutMode.Expanded)
        assertTrue(sideBySide < stacked)
    }

    @Test
    fun `the mini player is unbounded on phones and capped on large screens`() {
        assertFalse(derbisMiniPlayerMaxWidthDp(DerbisLayoutMode.Compact).isFinite())
        assertEquals(640f, derbisMiniPlayerMaxWidthDp(DerbisLayoutMode.Medium), 0.0001f)
        assertEquals(760f, derbisMiniPlayerMaxWidthDp(DerbisLayoutMode.Expanded), 0.0001f)
    }

    @Test
    fun `gutters grow with the window`() {
        assertEquals(18f, derbisFoldAwareGutterDp(DerbisLayoutMode.Compact, compactPlayer = true), 0.0001f)
        assertEquals(22f, derbisFoldAwareGutterDp(DerbisLayoutMode.Compact, compactPlayer = false), 0.0001f)
        assertEquals(28f, derbisFoldAwareGutterDp(DerbisLayoutMode.Medium, compactPlayer = false), 0.0001f)
        assertEquals(34f, derbisFoldAwareGutterDp(DerbisLayoutMode.Expanded, compactPlayer = true), 0.0001f)
    }
}
