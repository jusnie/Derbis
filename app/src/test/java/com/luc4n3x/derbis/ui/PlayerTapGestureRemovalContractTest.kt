package com.luc4n3x.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTapGestureRemovalContractTest {

    @Test
    fun `Player settings no longer expose tap long press or vertical swipe controls`() {
        val app = source("ui/DerbisApp.kt")

        assertFalse(app.contains("title = strings.advancedGestures"))
        assertFalse(app.contains("title = strings.playerGestureDoubleTapAction"))
        assertFalse(app.contains("title = strings.doubleTapSeek"))
        assertFalse(app.contains("title = strings.playerGestureLongPressAction"))
        assertFalse(app.contains("title = strings.longPress"))
        assertFalse(app.contains("title = strings.playerGestureVerticalSwipe"))
        assertTrue(app.contains("title = strings.playerGestureHorizontalSwipe"))
    }

    @Test
    fun `Full player input retains optional horizontal switching only`() {
        val app = source("ui/DerbisApp.kt")
        val layer = app.substring(
            app.indexOf("private fun PlayerGestureLayer("),
            app.indexOf("private fun PlayerTimeline(", app.indexOf("private fun PlayerGestureLayer("))
        )

        assertFalse(layer.contains("detectTapGestures"))
        assertFalse(layer.contains("playerDoubleTapCommand"))
        assertFalse(layer.contains("playerLongPressCommand"))
        assertFalse(layer.contains("verticalSwipeAction"))
        assertTrue(layer.contains("verticalEnabled = false"))
        assertTrue(layer.contains("swipeTrackChangeEnabled"))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/derbis/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
