package com.luc4n3x.derbis.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDesignTest {
    @Test
    fun `header keeps a premium touch friendly scale`() {
        assertTrue(DerbisHomeDesign.SettingsControlHeight >= DerbisPlayerDesign.MinimumTouchTarget)
        assertTrue(DerbisHomeDesign.MoodChipHeight >= DerbisPlayerDesign.MinimumTouchTarget)
        assertTrue(DerbisHomeDesign.HeaderCorner < DerbisHomeDesign.SettingsControlHeight)
        assertTrue(DerbisHomeDesign.HeaderPadding.value > 0f)
    }

    @Test
    fun `home hierarchy keeps a clear editorial scale`() {
        assertTrue(DerbisHomeDesign.HeaderCorner <= DerbisPlayerDesign.CornerLg)
        assertTrue(DerbisHomeDesign.HeroCorner > DerbisHomeDesign.ShelfCorner)
        assertTrue(DerbisHomeDesign.HeroHeight.value >= 240f)
        assertTrue(DerbisHomeDesign.SectionGap > DerbisHomeDesign.SectionGapCompact)
        assertEquals(16f, DerbisHomeDesign.HorizontalInset.value, 0f)
    }

    @Test
    fun `sections separate more than a header separates from its content`() {
        assertTrue(DerbisHomeDesign.SectionStride > DerbisHomeDesign.SectionStrideCompact)
        assertTrue(DerbisHomeDesign.SectionStride > DerbisHomeDesign.SectionGap * 2f)
        assertTrue(DerbisHomeDesign.SectionStrideCompact > DerbisHomeDesign.SectionGapCompact * 2f)
        assertTrue(DerbisHomeDesign.sectionLead(compact = false) > DerbisHomeDesign.sectionLead(compact = true))
        assertEquals(
            DerbisHomeDesign.SectionStride.value,
            (DerbisHomeDesign.sectionLead(compact = false) + DerbisHomeDesign.sectionGap(compact = false)).value,
            0.001f
        )
        assertEquals(
            DerbisHomeDesign.SectionStrideCompact.value,
            (DerbisHomeDesign.sectionLead(compact = true) + DerbisHomeDesign.sectionGap(compact = true)).value,
            0.001f
        )
    }

    @Test
    fun `artwork cards keep one radius family and two widths`() {
        assertTrue(DerbisHomeDesign.ArtworkCorner > DerbisHomeDesign.ShelfCorner)
        assertTrue(DerbisHomeDesign.ArtworkCorner < DerbisHomeDesign.HeroCorner)
        assertTrue(DerbisHomeDesign.ArtworkCardWidth > DerbisHomeDesign.ArtworkGridCardWidth)
        assertTrue(DerbisHomeDesign.ShelfItemGap.value > 0f)
    }
}
