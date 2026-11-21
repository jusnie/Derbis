package com.yasuda.derbis.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAudioStrategyContractTest {
    @Test
    fun `reel audio strategy cannot consume the muxed fallback`() {
        val resolver = readResolverSource()
        val policyDispatch = resolver
            .substringAfter("private suspend fun resolveAudioByCompatibilityPolicy")
            .substringBefore("private suspend fun resolveVideoByCompatibilityPolicy")
        val reelAudio = resolver
            .substringAfter("private suspend fun resolveAudioWithAndroidReel")
            .substringBefore("private suspend fun resolveVideoWithAndroidReel")
        val reelAudioCandidates = resolver
            .substringAfter("private fun androidReelAudioCandidates")
            .substringBefore("private suspend fun firstPlayableAndroidReelAudio")

        assertTrue(policyDispatch.contains("PlaybackAudioStrategy.REEL_AUDIO"))
        assertTrue(policyDispatch.contains("resolveAudioWithAndroidReel(track, audioQuality)"))
        assertTrue(policyDispatch.contains("PlaybackAudioStrategy.REEL_MUXED"))
        assertTrue(policyDispatch.contains("resolveVideoWithAndroidReel(track)"))
        assertTrue(reelAudioCandidates.contains("mime.startsWith(\"audio/\", true)"))
        assertFalse(reelAudioCandidates.contains("mime.startsWith(\"video/\", true)"))
        assertTrue(reelAudio.contains("stream solo audio riproducibile"))
        assertTrue(reelAudio.contains("rejectedHttpStatusCode"))
        assertTrue(policyDispatch.contains("strategy == PlaybackAudioStrategy.PERSISTED && failureReason == null"))
    }

    @Test
    fun `reel muxed reuses fresh reel watch response and skips failed audio reprobe`() {
        val resolver = readResolverSource()
        val reelAudio = resolver
            .substringAfter("private suspend fun resolveAudioWithAndroidReel")
            .substringBefore("private suspend fun resolveVideoWithAndroidReel")
        val reelVideo = resolver
            .substringAfter("private suspend fun resolveVideoWithAndroidReel")
            .substringBefore("private suspend fun fetchOrReuseAndroidReelWatch")

        assertTrue(reelAudio.contains("fetchOrReuseAndroidReelWatch("))
        assertTrue(reelAudio.contains("markAndroidReelAudioUnavailable("))
        assertTrue(reelVideo.contains("fetchOrReuseAndroidReelWatch("))
        assertTrue(reelVideo.contains("if (reelWatch.verifiedNoPlayableAudio)"))
    }

    @Test
    fun `malformed persisted stream entries are removed during restore`() {
        val restore = readResolverSource()
            .substringAfter("private fun restoreCache()")
            .substringBefore("private fun store(")

        assertTrue(restore.contains("val json = parsePersistedCacheEntry(value)"))
        assertTrue(restore.contains("if (json == null)"))
        assertTrue(restore.contains("editor.remove(key)"))
    }

    private fun readResolverSource(): String = Files.readString(resolverSource()).replace("\r\n", "\n")

    private fun resolverSource(): Path = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/data/PlaybackResolver.kt"),
        Path.of("src/main/java/com/yasuda/derbis/data/PlaybackResolver.kt")
    ).firstOrNull(Files::exists) ?: error("PlaybackResolver source not found")
}
