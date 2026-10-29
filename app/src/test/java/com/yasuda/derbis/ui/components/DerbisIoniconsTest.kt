package com.yasuda.derbis.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisIoniconsTest {
    @Test
    fun buildsEveryBundledIcon() {
        val icons = listOf(
            DerbisIonicons.Play,
            DerbisIonicons.Pause,
            DerbisIonicons.SkipPrevious,
            DerbisIonicons.SkipNext,
            DerbisIonicons.Shuffle,
            DerbisIonicons.Repeat,
            DerbisIonicons.RepeatOne,
            DerbisIonicons.ChevronDown,
            DerbisIonicons.MoreVertical,
            DerbisIonicons.AddCircle,
            DerbisIonicons.Heart,
            DerbisIonicons.HeartOutline,
            DerbisIonicons.Download,
            DerbisIonicons.Queue,
            DerbisIonicons.Lyrics,
            DerbisIonicons.Timer,
            DerbisIonicons.Equalizer,
            DerbisIonicons.Device,
            DerbisIonicons.Settings,
            DerbisIonicons.Share
        )

        assertEquals(icons.size, icons.map { it.name }.distinct().size)
        icons.forEach { icon ->
            assertEquals(512f, icon.viewportWidth, 0f)
            assertEquals(512f, icon.viewportHeight, 0f)
            assertEquals(24f, icon.defaultWidth.value, 0f)
            assertEquals(24f, icon.defaultHeight.value, 0f)
            assertTrue(icon.name.startsWith("Ionicons."))
        }
    }
}
