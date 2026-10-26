package com.luc4n3x.derbis.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.luc4n3x.derbis.ui.components.DerbisPressScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisMotionTest {

    private val tokens = listOf(
        DerbisMotion.press,
        DerbisMotion.release,
        DerbisMotion.expand,
        DerbisMotion.collapse,
        DerbisMotion.playerExpand,
        DerbisMotion.playerCollapse,
        DerbisMotion.settle,
        DerbisMotion.snappy,
        DerbisMotion.expressive,
        DerbisMotion.reorder,
        DerbisMotion.spatial,
        DerbisMotion.gesture,
        DerbisMotion.feedback
    )

    @Test
    fun `spring tokens are deterministic and physically sane`() {
        tokens.forEach { token ->
            assertTrue("$token damping", token.dampingRatio in 0.5f..1f)
            assertTrue("$token stiffness", token.stiffness in 200f..1_500f)
            val first = token.spec<Float>()
            val second = token.spec<Float>()
            assertEquals(first, second)
            assertEquals(token.dampingRatio, first.dampingRatio, 0f)
            assertEquals(token.stiffness, first.stiffness, 0f)
        }
    }

    @Test
    fun `only feedback springs are allowed to bounce noticeably`() {
        val bouncy = tokens.filter { it.dampingRatio < 0.7f }
        assertEquals(setOf(DerbisMotion.release, DerbisMotion.expressive, DerbisMotion.feedback), bouncy.toSet())
        assertTrue(DerbisMotion.gesture.dampingRatio >= 0.9f)
        assertTrue(DerbisMotion.spatial.dampingRatio > DerbisMotion.expand.dampingRatio)
        assertTrue(DerbisMotion.settle.dampingRatio >= 0.9f)
        assertTrue(DerbisMotion.collapse.dampingRatio > DerbisMotion.expand.dampingRatio)
    }

    @Test
    fun `player morph springs are softer and calmer than generic surface springs`() {
        assertTrue(DerbisMotion.playerExpand.stiffness < DerbisMotion.expand.stiffness)
        assertTrue(DerbisMotion.playerCollapse.stiffness < DerbisMotion.collapse.stiffness)
        assertTrue(DerbisMotion.playerExpand.dampingRatio >= DerbisMotion.expand.dampingRatio)
        assertTrue(DerbisMotion.playerCollapse.dampingRatio >= DerbisMotion.playerExpand.dampingRatio)
        assertEquals(DerbisMotion.playerExpand.spec<Float>(), DerbisPlayerDesign.expandSpring<Float>())
        assertEquals(DerbisMotion.playerCollapse.spec<Float>(), DerbisPlayerDesign.collapseSpring<Float>())
    }

    @Test
    fun `durations are ordered and short enough for a music app`() {
        val durations = with(DerbisMotion.Durations) { listOf(Instant, Quick, Short, Medium, Long, Palette) }
        assertEquals(durations.sorted(), durations)
        assertTrue(durations.all { it in 1..700 })
    }

    @Test
    fun `reduced motion snaps every spec`() {
        assertTrue(DerbisMotion.spec(false, DerbisMotion.crossfade<Float>()) is SnapSpec)
        assertTrue(DerbisMotion.physics<Float>(false, DerbisMotion.press) is SnapSpec)
        assertTrue(DerbisMotion.physics<Float>(true, DerbisMotion.press) is SpringSpec)
        assertTrue(DerbisMotion.spec(true, DerbisMotion.crossfade<Float>()) is TweenSpec)
        assertSame(EnterTransition.None, DerbisMotion.overlayEnter(false))
        assertSame(ExitTransition.None, DerbisMotion.overlayExit(false))
        assertSame(EnterTransition.None, DerbisMotion.sheetEnter(false))
        assertSame(ExitTransition.None, DerbisMotion.sheetExit(false))
        assertNotEquals(EnterTransition.None, DerbisMotion.overlayEnter(true))
        val reduced = DerbisMotion.contentSwap(false)
        assertSame(EnterTransition.None, reduced.targetContentEnter)
        assertSame(ExitTransition.None, reduced.initialContentExit)
    }

    @Test
    fun `time based specs keep their duration contract`() {
        assertEquals(DerbisMotion.Durations.Medium, DerbisMotion.crossfade<Float>().durationMillis)
        assertEquals(DerbisMotion.Durations.Long, DerbisMotion.artwork<Float>().durationMillis)
        assertEquals(DerbisMotion.Durations.Palette, DerbisMotion.palette<Float>().durationMillis)
        assertEquals(DerbisMotion.Durations.Short, DerbisMotion.fade<Float>().durationMillis)
        assertEquals(0, DerbisMotion.fade<Float>().delay)
    }

    @Test
    fun `player design delegates to the shared motion language without changing values`() {
        assertEquals(0.64f, DerbisPlayerDesign.ExpressiveDamping, 0f)
        assertEquals(360f, DerbisPlayerDesign.ExpressiveStiffness, 0f)
        assertEquals(0.90f, DerbisPlayerDesign.SmoothDamping, 0f)
        assertEquals(540f, DerbisPlayerDesign.SmoothStiffness, 0f)
        assertEquals(0.58f, DerbisPlayerDesign.PressDamping, 0f)
        assertEquals(760f, DerbisPlayerDesign.PressStiffness, 0f)
        assertEquals(650, DerbisPlayerDesign.PaletteMillis)
        assertEquals(DerbisMotion.expressive.spec<Float>(), DerbisPlayerDesign.expressiveSpring<Float>())
        assertEquals(DerbisMotion.settle.spec<Float>(), DerbisPlayerDesign.smoothSpring<Float>())
        assertEquals(DerbisMotion.release.spec<Float>(), DerbisPlayerDesign.pressSpring<Float>())
        assertEquals(320, DerbisPlayerDesign.emphasizedTween<Float>().durationMillis)
        assertEquals(220, DerbisPlayerDesign.standardTween<Float>().durationMillis)
        assertTrue(DerbisPlayerDesign.motion(false, DerbisPlayerDesign.paletteTween<Float>()) is SnapSpec)
    }

    @Test
    fun `press scales come from one place`() {
        assertEquals(DerbisMotion.Scale.Row, DerbisPressScale.Row, 0f)
        assertEquals(DerbisMotion.Scale.Tile, DerbisPressScale.Tile, 0f)
        assertEquals(DerbisMotion.Scale.Surface, DerbisPressScale.Surface, 0f)
        assertEquals(DerbisMotion.Scale.Control, DerbisPressScale.Control, 0f)
        listOf(DerbisPressScale.Row, DerbisPressScale.Tile, DerbisPressScale.Surface, DerbisPressScale.Control).forEach {
            assertTrue(it in 0.9f..1f)
        }
    }
}
