package com.yasuda.derbis.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassBackdropPolicyTest {
    @Test
    fun `quality buckets follow the glass blur families`() {
        assertEquals(GlassBackdropQuality.Native, resolveGlassBackdropQuality(18f))
        assertEquals(GlassBackdropQuality.Balanced, resolveGlassBackdropQuality(24f))
        assertEquals(GlassBackdropQuality.Efficient, resolveGlassBackdropQuality(30f))
    }

    @Test
    fun `scaled layer rounds outward and never becomes empty`() {
        assertEquals(76, glassBackdropLayerDimension(101f, 0.75f))
        assertEquals(1, glassBackdropLayerDimension(0f, 0.5f))
    }

    @Test
    fun `lens tier degrades on constrained devices`() {
        assertEquals(GlassLensTier.Full, resolveGlassLensTier(heavyBlur = true, lowRamDevice = false, powerSaveMode = false))
        assertEquals(GlassLensTier.Efficient, resolveGlassLensTier(heavyBlur = true, lowRamDevice = true, powerSaveMode = false))
        assertEquals(GlassLensTier.Efficient, resolveGlassLensTier(heavyBlur = true, lowRamDevice = false, powerSaveMode = true))
        assertEquals(GlassLensTier.Efficient, resolveGlassLensTier(heavyBlur = false, lowRamDevice = false, powerSaveMode = false))
    }

    @Test
    fun `tier ceilings only ever coarsen`() {
        assertEquals(GlassBackdropQuality.Native, GlassLensTier.Full.ceilingQuality)
        assertEquals(GlassBackdropQuality.Balanced, GlassLensTier.Balanced.ceilingQuality)
        assertEquals(GlassBackdropQuality.Efficient, GlassLensTier.Efficient.ceilingQuality)
    }

    @Test
    fun `coarsest quality wins so no surface samples finer than its budget`() {
        assertEquals(
            GlassBackdropQuality.Efficient,
            coarsestGlassBackdropQuality(GlassBackdropQuality.Native, GlassBackdropQuality.Efficient)
        )
        assertEquals(
            GlassBackdropQuality.Balanced,
            coarsestGlassBackdropQuality(GlassBackdropQuality.Balanced, GlassBackdropQuality.Native)
        )
        assertEquals(
            GlassBackdropQuality.Native,
            coarsestGlassBackdropQuality(GlassBackdropQuality.Native, GlassBackdropQuality.Native)
        )
    }

    @Test
    fun `content roles sample coarser than chrome`() {
        val subtle = com.yasuda.derbis.ui.components.DerbisGlassDefaults.Subtle
        val standard = com.yasuda.derbis.ui.components.DerbisGlassDefaults.Standard
        val strong = com.yasuda.derbis.ui.components.DerbisGlassDefaults.Strong

        // Content rows and page surfaces are the cheapest, chrome keeps the sharp bucket.
        assertEquals(GlassBackdropQuality.Efficient, subtle.samplingQuality)
        assertEquals(GlassBackdropQuality.Balanced, standard.samplingQuality)
        assertEquals(GlassBackdropQuality.Native, strong.samplingQuality)
        // Only the small chrome controls are allowed to pay for the lens.
        assertEquals(false, subtle.allowsRefraction)
        assertEquals(false, standard.allowsRefraction)
        assertEquals(true, strong.allowsRefraction)
    }
}
