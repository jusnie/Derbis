package com.luc4n3x.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisMixToolRemovalContractTest {

    @Test
    fun `Library no longer exposes the two-song Derbis Mix tool`() {
        val library = source("ui/library/DerbisLibraryScreen.kt")
        val rootViewModel = source("viewmodel/DerbisViewModel.kt")
        val libraryViewModel = source("viewmodel/DerbisScreenViewModels.kt")
        val store = source("data/DerbisMixStudioStore.kt")

        assertFalse(Files.exists(primarySourcePath("ui/library/DerbisMixTool.kt")))
        assertFalse(library.contains("DerbisMixTool"))
        assertFalse(library.contains("showDerbisMixTool"))
        assertFalse(library.contains("derbisMixTracks"))
        assertFalse(rootViewModel.contains("playDerbisMixPair"))
        assertFalse(libraryViewModel.contains("playDerbisMixPair"))
        assertFalse(store.contains("saveToolTransition"))
    }

    @Test
    fun `high refresh switch stays available outside the Default-skin settings gate`() {
        val app = source("ui/DerbisApp.kt")
        val highRefresh = app.indexOf("title = \"Liquid Glass 90Hz / 120Hz\"")
        val designGate = app.lastIndexOf("skinSettingsItems(interfaceSettings.skin", highRefresh)
        val firstGateEnd = app.indexOf("                            })", designGate) + "                            })".length

        assertTrue(highRefresh > firstGateEnd)
    }

    private fun source(relativePath: String): String {
        val path = sourcePaths(relativePath).firstOrNull(Files::exists) ?: error("$relativePath not found")
        return Files.readString(path).replace("\r\n", "\n")
    }

    private fun primarySourcePath(relativePath: String): Path = sourcePaths(relativePath).first()

    private fun sourcePaths(relativePath: String): List<Path> = listOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/derbis/$relativePath")
    )
}
