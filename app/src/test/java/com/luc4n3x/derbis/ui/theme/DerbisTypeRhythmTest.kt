package com.luc4n3x.derbis.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisTypeRhythmTest {

    @Test
    fun `derbisLineHeightRatio is monotonically non-increasing as font size grows`() {
        val sizes = listOf(9f, 11f, 12f, 14f, 16f, 20f, 24f, 28f, 36f, 57f)
        val ratios = sizes.map { derbisLineHeightRatio(it) }
        for (index in 1 until ratios.size) {
            assertTrue(
                "ratio should not increase from ${sizes[index - 1]} to ${sizes[index]}: " +
                    "${ratios[index - 1]} -> ${ratios[index]}",
                ratios[index] <= ratios[index - 1]
            )
        }
    }

    @Test
    fun `every line height for sizes 9 to 24 meets the 1_28x readability floor`() {
        for (size in 9..24) {
            val fontSize = size.toFloat()
            val lineHeight = derbisLineHeightSp(fontSize)
            val floor = 1.28f * fontSize
            assertTrue(
                "size=$fontSize lineHeight=$lineHeight expected >= $floor",
                lineHeight >= floor
            )
        }
    }

    @Test
    fun `derbisLineHeightSp of zero is zero and never NaN or negative`() {
        assertTrue(derbisLineHeightSp(0f) == 0f)

        val samples = listOf(-5f, 0f, 1f, 9f, 16f, 24f, 34f, 57f, 200f)
        samples.forEach { size ->
            val value = derbisLineHeightSp(size)
            assertFalse(value.isNaN())
            assertTrue("size=$size produced negative line height $value", value >= 0f)
        }
    }
}
