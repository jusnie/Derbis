package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreSamplesRemovalContractTest {

    @Test
    fun `Explore no longer publishes or opens the Samples route`() {
        val app = source("ui/LevyraApp.kt")
        val layout = source("ui/ExploreLayout.kt")
        val screenViewModels = source("viewmodel/LevyraScreenViewModels.kt")
        val rootViewModel = source("viewmodel/LevyraViewModel.kt")

        assertFalse(app.contains("ExploreShortcut.Samples"))
        assertFalse(app.contains("ExploreSamplesScreen("))
        assertFalse(app.contains("samplesStartIndex"))
        assertFalse(app.contains("exploreSampleTracks("))
        assertFalse(layout.contains("Samples(null, null)"))
        assertFalse(layout.contains("exploreSampleTracks("))
        assertFalse(screenViewModels.contains("fun ensureSamples()"))
        assertFalse(screenViewModels.contains("fun playSample("))
        assertFalse(screenViewModels.contains("val exploreSamples:"))
        assertFalse(screenViewModels.contains("val isSamplesOpen:"))
        val ensureExplore = rootViewModel.substring(
            rootViewModel.indexOf("fun ensureExplore(strings: LevyraStrings)"),
            rootViewModel.indexOf("fun selectExploreZone", rootViewModel.indexOf("fun ensureExplore(strings: LevyraStrings)"))
        )
        assertFalse(ensureExplore.contains("ensureMusicVideosLoaded"))
    }

    @Test
    fun `Explore keeps its release and mood entry points`() {
        assertTrue(ExploreShortcut.entries.toList() == listOf(
            ExploreShortcut.NewReleases,
            ExploreShortcut.Moods
        ))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
