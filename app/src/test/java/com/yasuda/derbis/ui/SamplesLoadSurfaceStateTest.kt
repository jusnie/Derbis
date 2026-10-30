package com.yasuda.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Test

class SamplesLoadSurfaceStateTest {
    @Test
    fun removedSamplesLoadingSurfaceIsNotRestored() {
        val app = sequenceOf(
            Path.of("app/src/main/java/com/yasuda/derbis/ui/DerbisApp.kt"),
            Path.of("src/main/java/com/yasuda/derbis/ui/DerbisApp.kt")
        ).first(Files::exists).let(Files::readString)
        assertFalse(app.contains("SamplesLoadSurfaceState"))
        assertFalse(app.contains("samplesLoadSurfaceState("))
        assertFalse(app.contains("ExploreSamplesScreen("))
        assertFalse(ExploreShortcut.entries.any { it.name == "Samples" })
    }
}
