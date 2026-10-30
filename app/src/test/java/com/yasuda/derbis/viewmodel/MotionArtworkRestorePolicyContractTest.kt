package com.yasuda.derbis.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkRestorePolicyContractTest {
    @Test
    fun `restore applies restored wifi only policy before motion artwork restarts`() {
        val restore = readSource("viewmodel/DerbisViewModel.kt")
            .substringAfter("private suspend fun refreshAfterRestore()")
            .substringBefore("fun setLanguage(")
            .filterNot(Char::isWhitespace)
        val policyUpdate = restore.indexOf(
            "MotionArtworkNetworkPolicy.updateWifiOnly(snapshot.interfaceSettings.motionArtworkWifiOnly||snapshot.interfaceSettings.dataSaverEnabled)"
        )
        val enableUpdate = restore.indexOf(
            "MotionArtworkNetworkPolicy.updateEnabled(snapshot.motionArtworkEnabled)"
        )
        val stateRestore = restore.indexOf("interfaceSettings=snapshot.interfaceSettings")
        val motionRestart = restore.indexOf("refreshMotionArtworkAround")

        assertTrue(policyUpdate >= 0)
        assertTrue(enableUpdate >= 0)
        assertTrue(stateRestore > policyUpdate)
        assertTrue(stateRestore > enableUpdate)
        assertTrue(motionRestart > policyUpdate)
        assertTrue(motionRestart > enableUpdate)
    }

    @Test
    fun `resolved playback start realigns motion artwork with the playing identity`() {
        val start = readSource("viewmodel/DerbisViewModel.kt")
            .substringAfter("private fun startPlayback(playable: Track, request: PlaybackResolveRequest)")
            .substringBefore("private fun handlePlaybackStartSideEffects(")
            .filterNot(Char::isWhitespace)
        val stateUpdate = start.indexOf("currentTrack=resolved")
        val motionRefresh = start.indexOf("refreshMotionArtworkAround(resolved)")

        assertTrue(stateUpdate >= 0)
        assertTrue(motionRefresh > stateUpdate)
    }

    @Test
    fun `spotify artwork wins over other covers and is never replaced`() {
        val apply = readSource("viewmodel/DerbisViewModel.kt")
            .substringAfter("private suspend fun applyOfficialOrbitArtworkBatch(")
            .substringBefore("fun normalizedMetadataText(")
            .filterNot(Char::isWhitespace)
        val lookup = apply.indexOf("valenriched=enrichedByKey[DerbisPersonalOrbit.identityKey(item)]?:returnitem")

        assertTrue(lookup >= 0)
        assertTrue(apply.contains("valkeepsArtwork=DerbisPersonalOrbit.hasSquareAlbumArtwork(item)&&(isSpotifyArtworkUrl(item.thumbnailUrl)||!isSpotifyArtworkUrl(enriched.thumbnailUrl))"))
        assertTrue(apply.contains("thumbnailUrl=if(keepsArtwork)item.thumbnailUrlelse"))
        assertFalse(apply.contains("scoreMatch(item,enriched)"))
        assertFalse(apply.contains("sameSpotifyAlbum(item.album,enriched.album)"))
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/$relativePath"),
        Path.of("src/main/java/com/yasuda/derbis/$relativePath")
    ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
}
