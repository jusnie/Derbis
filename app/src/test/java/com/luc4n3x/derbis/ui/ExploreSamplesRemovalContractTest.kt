package com.luc4n3x.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreSamplesRemovalContractTest {

    @Test
    fun `Explore no longer publishes or opens the Samples route`() {
        val app = source("ui/DerbisApp.kt")
        val layout = source("ui/ExploreLayout.kt")
        val screenViewModels = source("viewmodel/DerbisScreenViewModels.kt")
        val rootViewModel = source("viewmodel/DerbisViewModel.kt")

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
            rootViewModel.indexOf("fun ensureExplore(strings: DerbisStrings)"),
            rootViewModel.indexOf("fun selectExploreZone", rootViewModel.indexOf("fun ensureExplore(strings: DerbisStrings)"))
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
        Path.of("app/src/main/java/com/luc4n3x/derbis/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/derbis/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
