package com.luc4n3x.derbis.feature.systemintegration

import com.luc4n3x.derbis.data.LyricsLatencyProfiles
import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import com.luc4n3x.derbis.domain.LyricLine
import com.luc4n3x.derbis.feature.audio.DerbisAudioOutputState
import com.luc4n3x.derbis.ui.i18n.deviceIntegrationCopy
import com.luc4n3x.derbis.ui.i18n.deviceIntegrationLocalizationCodes
import com.luc4n3x.derbis.ui.settings.DeviceIntegrationStatus
import com.luc4n3x.derbis.ui.settings.deviceIntegrationRows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisDeviceIntegrationTest {
    @Test
    fun hyperOsThreeWithFocusProtocolEnablesSuperIsland() {
        listOf("Xiaomi" to "Xiaomi", "Xiaomi" to "POCO", "Xiaomi" to "Redmi").forEach { (manufacturer, brand) ->
            val profile = resolveDerbisDeviceProfile(
                signals(manufacturer, brand, hyperOsVersionCode = "3", focusProtocol = 3)
            )
            assertEquals(DerbisDeviceFamily.XIAOMI, profile.family)
            assertEquals("HyperOS 3", profile.systemName)
            assertTrue(profile.supports(DerbisDeviceFeature.SUPER_ISLAND))
        }
    }

    @Test
    fun olderHyperOsFocusProtocolsFallBackToStandardNotifications() {
        listOf(0, 1, 2).forEach { protocol ->
            val profile = resolveDerbisDeviceProfile(
                signals("Xiaomi", "POCO", hyperOsVersionName = "OS2.0.5.0", focusProtocol = protocol)
            )
            assertEquals(DerbisDeviceFamily.XIAOMI, profile.family)
            assertEquals("HyperOS 2", profile.systemName)
            assertFalse(profile.supports(DerbisDeviceFeature.SUPER_ISLAND))
        }
    }

    @Test
    fun manufacturerAloneNeverEnablesIslandProtocols() {
        assertTrue(resolveDerbisDeviceProfile(signals("Xiaomi", "Xiaomi")).features.isEmpty())
        assertTrue(resolveDerbisDeviceProfile(signals("vivo", "vivo")).features.isEmpty())
        assertTrue(resolveDerbisDeviceProfile(signals("samsung", "samsung")).features.isEmpty())
    }

    @Test
    fun originOsEnablesAtomicIslandButFuntouchDoesNot() {
        val origin = resolveDerbisDeviceProfile(
            signals("vivo", "vivo", sdkInt = 35, vivoOsName = "vos", vivoOsVersion = "5.0")
        )
        assertEquals(DerbisDeviceFamily.VIVO, origin.family)
        assertEquals("OriginOS 5", origin.systemName)
        assertTrue(origin.supports(DerbisDeviceFeature.ATOMIC_ISLAND))

        val funtouch = resolveDerbisDeviceProfile(
            signals("vivo", "vivo", sdkInt = 35, vivoOsName = "Funtouch", vivoOsVersion = "15.0")
        )
        assertEquals("Funtouch OS", funtouch.systemName)
        assertFalse(funtouch.supports(DerbisDeviceFeature.ATOMIC_ISLAND))

        val oldOrigin = resolveDerbisDeviceProfile(signals("vivo", "iQOO", sdkInt = 33, vivoOsName = "OriginOS"))
        assertFalse(oldOrigin.supports(DerbisDeviceFeature.ATOMIC_ISLAND))
    }

    @Test
    fun oplusFamiliesKeepTimedLyricsAndExposeRomNames() {
        assertEquals("ColorOS 15", resolveDerbisDeviceProfile(signals("OPPO", "OPPO", oplusRomVersion = "V15.0.0")).systemName)
        assertEquals("OxygenOS 15", resolveDerbisDeviceProfile(signals("OnePlus", "OnePlus", oplusRomVersion = "V15.0.0")).systemName)
        assertEquals("realme UI", resolveDerbisDeviceProfile(signals("realme", "realme", oplusRomVersion = "V15.0.0")).systemName)
        listOf("OPPO", "OnePlus", "realme").forEach { brand ->
            val profile = resolveDerbisDeviceProfile(signals(brand, brand))
            assertEquals(DerbisDeviceFamily.OPLUS, profile.family)
            assertTrue(profile.supports(DerbisDeviceFeature.OPLUS_TIMED_LYRICS))
        }
        val rebadged = resolveDerbisDeviceProfile(signals("Unknown", "Unknown", oplusRomVersion = "V14.1.0"))
        assertEquals(DerbisDeviceFamily.OPLUS, rebadged.family)
    }

    @Test
    fun existingOplusDetectionAcceptsRomPropertyWithoutRegressingBrands() {
        assertTrue(detectDerbisRomMediaCapabilities("OPPO", "OPPO", "").timedLyricsMetadata)
        assertTrue(detectDerbisRomMediaCapabilities("Unknown", "Unknown", "V15.0.0").timedLyricsMetadata)
        assertFalse(detectDerbisRomMediaCapabilities("Google", "google", "").timedLyricsMetadata)
    }

    @Test
    fun samsungNowBarRequiresOneUiSeven() {
        val oneUi7 = resolveDerbisDeviceProfile(signals("samsung", "samsung", oneUiVersion = "70000"))
        assertEquals("One UI 7.0", oneUi7.systemName)
        assertTrue(oneUi7.supports(DerbisDeviceFeature.NOW_BAR))
        val oneUi61 = resolveDerbisDeviceProfile(signals("samsung", "samsung", oneUiVersion = "60100"))
        assertEquals("One UI 6.1", oneUi61.systemName)
        assertFalse(oneUi61.supports(DerbisDeviceFeature.NOW_BAR))
    }

    @Test
    fun honorPixelAndGenericDevicesStayOnStandardAndroid() {
        val honor = resolveDerbisDeviceProfile(signals("HONOR", "HONOR", magicOsVersion = "MagicOS_8.0"))
        assertEquals(DerbisDeviceFamily.HONOR, honor.family)
        assertEquals("MagicOS 8.0", honor.systemName)
        assertTrue(honor.features.isEmpty())
        val pixel = resolveDerbisDeviceProfile(signals("Google", "google"))
        assertEquals(DerbisDeviceFamily.GOOGLE, pixel.family)
        assertEquals("Android 16", pixel.systemName)
        val generic = resolveDerbisDeviceProfile(signals("Fairphone", "Fairphone"))
        assertEquals(DerbisDeviceFamily.GENERIC, generic.family)
        assertTrue(generic.features.isEmpty())
    }

    @Test
    fun islandLyricsUseOnlySyncedDisplayLines() {
        assertNull(derbisIslandLyrics("id", "id", lyricLines(), synced = false))
        val prepared = derbisIslandLyrics(
            "id",
            "id",
            listOf(LyricLine(0L, 500L, "[ar:Artist]", isMetadata = true)) + lyricLines(),
            synced = true
        )
        assertNotNull(prepared)
        assertEquals(listOf("First", "Second", "Third"), prepared!!.lines.map(LyricLine::text))
    }

    @Test
    fun lyricFrameTracksCurrentNextTranslationAndBoundary() {
        val lines = derbisIslandLyrics("id", "id", lyricLines(), synced = true)!!.lines
        val before = derbisIslandLyricFrame(lines, 500L)!!
        assertEquals("", before.current)
        assertEquals("First", before.next)
        assertEquals(1_000L, before.nextBoundaryMs)

        val first = derbisIslandLyricFrame(lines, 1_500L)!!
        assertEquals("First", first.current)
        assertEquals("Second", first.next)
        assertEquals("Primo", first.secondary)
        assertEquals(3_000L, first.nextBoundaryMs)

        val last = derbisIslandLyricFrame(lines, 6_000L)!!
        assertEquals("Third", last.current)
        assertEquals("Sanbanme", last.secondary)
        assertNull(last.nextBoundaryMs)
    }

    @Test
    fun tickDelayFollowsPlaybackSpeedWithoutPolling() {
        assertEquals(1_024L, derbisIslandTickDelayMs(2_000L, 3_000L, 1f))
        assertEquals(524L, derbisIslandTickDelayMs(2_000L, 3_000L, 2f))
        assertEquals(1_024L, derbisIslandTickDelayMs(2_000L, 3_000L, 0f))
        assertEquals(80L, derbisIslandTickDelayMs(2_999L, 3_000L, 1f))
        assertNull(derbisIslandTickDelayMs(3_000L, 3_000L, 1f))
        assertNull(derbisIslandTickDelayMs(3_000L, null, 1f))
    }

    @Test
    fun progressTickAdvancesInFivePercentStepsWithoutLyrics() {
        assertEquals(6_024L, derbisIslandProgressTickDelayMs(0L, 120_000L, 1f))
        assertEquals(1_024L, derbisIslandProgressTickDelayMs(5_000L, 120_000L, 1f))
        assertEquals(3_024L, derbisIslandProgressTickDelayMs(0L, 120_000L, 2f))
        assertEquals(1_000L, derbisIslandProgressTickDelayMs(5_990L, 120_000L, 1f))
        assertNull(derbisIslandProgressTickDelayMs(120_000L, 120_000L, 1f))
        assertNull(derbisIslandProgressTickDelayMs(1_000L, 0L, 1f))
    }

    @Test
    fun centerKeepsRefreshingProgressWhenTrackHasNoLyrics() = runBlocking {
        val adapter = RecordingAdapter()
        val startedAt = System.nanoTime()
        val center = center(this, listOf(adapter)) {
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
            playerState(positionMs = 5_700L + elapsedMs).copy(durationMs = 120_000L)
        }
        center.refresh()
        val firstCount = adapter.published.size
        delay(1_500L)
        assertTrue(adapter.published.size > firstCount)
        assertNull(adapter.published.last().lyric)
        center.release()
    }

    @Test
    fun focusPayloadCarriesLyricsSongAndProgress() {
        val lines = derbisIslandLyrics("id", "id", lyricLines(), synced = true)!!.lines
        val snapshot = snapshot(lyric = derbisIslandLyricFrame(lines, 1_500L), positionMs = 30_000L)
        val json = JSONObject(
            hyperOsSuperIslandFocusParam(hyperOsSuperIslandContent(snapshot), snapshot.progressPercent, "com.luc4n3x.derbis")
        )
        val param = json.getJSONObject("param_v2")
        assertEquals("lyric_display", param.getString("business"))
        assertEquals("First", param.getString("ticker"))
        assertEquals("Primo", param.getJSONObject("chatInfo").getString("content"))
        assertEquals(25, param.getJSONObject("progressInfo").getInt("progress"))
        val island = param.getJSONObject("param_island")
        val big = island.getJSONObject("bigIslandArea")
        assertEquals("First", big.getJSONObject("imageTextInfoLeft").getJSONObject("textInfo").getString("title"))
        assertEquals("Second", big.getJSONObject("textInfo").getString("title"))
        assertEquals(
            25,
            island.getJSONObject("smallIslandArea").getJSONObject("combinePicInfo")
                .getJSONObject("progressInfo").getInt("progress")
        )
    }

    @Test
    fun superIslandFallsBackToTitleAndArtistWithoutCurrentLine() {
        val noLyrics = hyperOsSuperIslandContent(snapshot(lyric = null))
        assertEquals("Song", noLyrics.primary)
        assertEquals("Artist", noLyrics.upcoming)
        assertEquals("Artist", noLyrics.detail)
        val lines = derbisIslandLyrics("id", "id", lyricLines(), synced = true)!!.lines
        val intro = hyperOsSuperIslandContent(snapshot(lyric = derbisIslandLyricFrame(lines, 200L)))
        assertEquals("Song", intro.primary)
        assertEquals("First", intro.upcoming)
        val blank = hyperOsSuperIslandContent(snapshot(lyric = null, title = "", artist = ""))
        assertEquals("♪", blank.primary)
    }

    @Test
    fun superIslandKeyIgnoresSmallProgressChangesButTracksPauseAndLines() {
        val lines = derbisIslandLyrics("id", "id", lyricLines(), synced = true)!!.lines
        val frame = derbisIslandLyricFrame(lines, 1_500L)
        fun key(lyric: DerbisIslandLyricFrame?, positionMs: Long = 30_000L, active: Boolean = true): String {
            val value = snapshot(lyric = lyric, positionMs = positionMs, active = active)
            return hyperOsSuperIslandKey(value, hyperOsSuperIslandContent(value))
        }
        assertEquals(key(frame), key(frame, positionMs = 31_000L))
        assertFalse(key(frame) == key(frame, active = false))
        assertFalse(key(frame) == key(derbisIslandLyricFrame(lines, 3_500L)))
    }

    @Test
    fun vivoContentAddsOnceThenUpdatesSameTrack() {
        val first = vivoAtomicIslandContent(snapshot(), activeMediaId = null)
        assertEquals(VivoAtomicIslandOperation.ADD, first.operation)
        assertEquals("Song", first.title)
        assertEquals("Artist", first.content)
        assertEquals(VivoAtomicIslandOperation.UPDATE, vivoAtomicIslandContent(snapshot(), "track").operation)
        assertEquals(VivoAtomicIslandOperation.ADD, vivoAtomicIslandContent(snapshot(), "other").operation)
        val blank = vivoAtomicIslandContent(snapshot(title = " ", artist = ""), null)
        assertEquals("♪", blank.title)
        assertEquals("♪", blank.content)
        val playing = snapshot(positionMs = 30_000L)
        val oneSecondLater = snapshot(positionMs = 31_000L)
        assertEquals(vivoAtomicIslandKey(playing, 100_000L), vivoAtomicIslandKey(oneSecondLater, 101_000L))
        val afterSeek = snapshot(positionMs = 180_000L)
        assertFalse(vivoAtomicIslandKey(playing, 100_000L) == vivoAtomicIslandKey(afterSeek, 101_000L))
        assertTrue(vivoAtomicIslandContent(playing, null).chronometer)
        assertFalse(vivoAtomicIslandContent(playing.copy(speed = 1.5f), null).chronometer)
        assertFalse(vivoAtomicIslandContent(playing.copy(active = false), null).chronometer)
    }

    @Test
    fun lateLyricsForPreviousTrackDoNotReplaceCurrentTrackLyrics() = runBlocking {
        val adapter = RecordingAdapter()
        val center = center(this, listOf(adapter)) { playerState(positionMs = 1_500L) }
        center.onLyrics("track", "track", lyricLines(), synced = true)
        center.onLyrics("previous", "previous", listOf(LyricLine(0L, 9_000L, "Old")), synced = true)
        center.refresh()
        assertEquals("First", adapter.published.last().lyric?.current)
        center.release()
    }

    @Test
    fun pausedPlaybackKeepsIslandUntilIdleTimeout() = runBlocking {
        val adapter = RecordingAdapter()
        val center = center(this, listOf(adapter)) { playerState(positionMs = 1_500L, playing = false, active = false) }
        center.refresh()
        assertEquals(0, adapter.clears)
        assertFalse(adapter.published.last().active)
        center.release()
        assertEquals(1, adapter.clears)
    }

    @Test
    fun centerRoutesPlayPauseSeekNextAndStop() = runBlocking {
        val adapter = RecordingAdapter()
        var state: DerbisIslandPlayerState? = playerState(positionMs = 1_500L)
        val center = center(this, listOf(adapter)) { state }
        center.onLyrics("track", "track", lyricLines(), synced = true)
        assertEquals("First", adapter.published.last().lyric?.current)

        state = playerState(positionMs = 1_500L, playing = false, active = false)
        center.refresh()
        assertFalse(adapter.published.last().active)

        state = playerState(positionMs = 5_200L)
        center.refresh()
        assertEquals("Third", adapter.published.last().lyric?.current)

        state = playerState(mediaId = "next", positionMs = 0L)
        center.refresh()
        assertEquals("next", adapter.published.last().mediaId)
        assertNull(adapter.published.last().lyric)

        state = null
        center.refresh()
        assertEquals(1, adapter.clears)
        center.release()
    }

    @Test
    fun centerIgnoresLyricsForAnotherTrackAndAppliesOffset() = runBlocking {
        val adapter = RecordingAdapter()
        val center = center(
            this,
            listOf(adapter),
            profiles = LyricsLatencyProfiles(globalOffsetMs = 1_000L)
        ) { playerState(positionMs = 3_500L) }
        center.onLyrics("other", "other", lyricLines(), synced = true)
        assertNull(adapter.published.last().lyric)
        center.onLyrics("track", "track", lyricLines(), synced = true)
        delay(10L)
        assertEquals("First", adapter.published.last().lyric?.current)
        center.release()
    }

    @Test
    fun centerSchedulesNextLineTickWhilePlaying() = runBlocking {
        val adapter = RecordingAdapter()
        val startedAt = System.nanoTime()
        val center = center(this, listOf(adapter)) {
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
            playerState(positionMs = 2_850L + elapsedMs)
        }
        center.onLyrics("track", "track", lyricLines(), synced = true)
        assertEquals("First", adapter.published.last().lyric?.current)
        delay(600L)
        assertEquals("Second", adapter.published.last().lyric?.current)
        center.release()
    }

    @Test
    fun failingAdapterIsIsolatedAndReleaseClearsTheRest() = runBlocking {
        val healthy = RecordingAdapter()
        val failing = object : DerbisIslandAdapter {
            override val feature = DerbisDeviceFeature.ATOMIC_ISLAND
            override fun publish(snapshot: DerbisIslandSnapshot) = throw IllegalStateException("vendor")
            override fun clear() = Unit
        }
        val center = center(this, listOf(failing, healthy)) { playerState(positionMs = 1_500L) }
        center.onLyrics("track", "track", lyricLines(), synced = true)
        center.refresh()
        assertEquals(2, healthy.published.size)
        assertEquals(listOf(DerbisDeviceFeature.ATOMIC_ISLAND), failures)
        center.release()
        assertEquals(1, healthy.clears)
        center.refresh()
        assertEquals(2, healthy.published.size)
    }

    @Test
    fun settingsRowsReflectCapabilitiesAndNotificationPermission() {
        val copy = deviceIntegrationCopy("en")
        val xiaomi = resolveDerbisDeviceProfile(signals("Xiaomi", "POCO", hyperOsVersionCode = "3", focusProtocol = 3))
        assertEquals(DeviceIntegrationStatus.ACTIVE, deviceIntegrationRows(xiaomi, true, copy).last().status)
        assertEquals(DeviceIntegrationStatus.NOTIFICATIONS_OFF, deviceIntegrationRows(xiaomi, false, copy).last().status)
        val oldXiaomi = resolveDerbisDeviceProfile(signals("Xiaomi", "Redmi", hyperOsVersionCode = "2", focusProtocol = 2))
        assertEquals(DeviceIntegrationStatus.UNAVAILABLE, deviceIntegrationRows(oldXiaomi, true, copy).last().status)
        val samsung = resolveDerbisDeviceProfile(signals("samsung", "samsung", oneUiVersion = "80000"))
        assertEquals(DeviceIntegrationStatus.AUTOMATIC, deviceIntegrationRows(samsung, true, copy).last().status)
        val pixel = resolveDerbisDeviceProfile(signals("Google", "google"))
        assertEquals(1, deviceIntegrationRows(pixel, true, copy).size)
    }

    @Test
    fun deviceIntegrationCopyCoversEveryCatalogLanguage() {
        val catalog = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalog, deviceIntegrationLocalizationCodes())
        catalog.forEach { code ->
            val copy = deviceIntegrationCopy(code)
            listOf(
                copy.title, copy.subtitle, copy.active, copy.automatic, copy.unavailable, copy.notificationsOff,
                copy.openNotifications, copy.backgroundTitle, copy.backgroundDetail, copy.standardDetail,
                copy.superIslandDetail, copy.atomicIslandDetail, copy.oplusLyricsDetail, copy.nowBarDetail
            ).forEach { assertTrue("$code has blank copy", it.isNotBlank()) }
        }
    }

    private val failures = mutableListOf<DerbisDeviceFeature>()

    private class RecordingAdapter : DerbisIslandAdapter {
        override val feature = DerbisDeviceFeature.SUPER_ISLAND
        val published = mutableListOf<DerbisIslandSnapshot>()
        var clears = 0
        override fun publish(snapshot: DerbisIslandSnapshot) {
            published += snapshot
        }
        override fun clear() {
            clears++
        }
    }

    private fun center(
        scope: kotlinx.coroutines.CoroutineScope,
        adapters: List<DerbisIslandAdapter>,
        profiles: LyricsLatencyProfiles = LyricsLatencyProfiles(),
        state: () -> DerbisIslandPlayerState?
    ) = DerbisDeviceIntegrationCenter(
        scope = scope,
        adapters = adapters,
        latencyProfiles = flowOf(profiles),
        audioOutput = MutableStateFlow(DerbisAudioOutputState()),
        playerState = state,
        onAdapterFailure = { feature, _ -> failures += feature }
    )

    private fun playerState(
        mediaId: String = "track",
        positionMs: Long,
        playing: Boolean = true,
        active: Boolean = true
    ) = DerbisIslandPlayerState(
        mediaId = mediaId,
        title = "Song",
        artist = "Artist",
        active = active,
        playing = playing,
        positionMs = positionMs,
        durationMs = 120_000L,
        speed = 1f
    )

    private fun snapshot(
        lyric: DerbisIslandLyricFrame? = null,
        positionMs: Long = 0L,
        active: Boolean = true,
        title: String = "Song",
        artist: String = "Artist"
    ) = DerbisIslandSnapshot(
        mediaId = "track",
        title = title,
        artist = artist,
        active = active,
        positionMs = positionMs,
        durationMs = 120_000L,
        speed = 1f,
        lyric = lyric
    )

    private fun lyricLines() = listOf(
        LyricLine(1_000L, 2_900L, "First", translated = "Primo"),
        LyricLine(3_000L, 4_900L, "Second"),
        LyricLine(5_000L, 7_000L, "Third", romanized = "Sanbanme")
    )

    private fun signals(
        manufacturer: String,
        brand: String,
        sdkInt: Int = 36,
        hyperOsVersionCode: String = "",
        hyperOsVersionName: String = "",
        focusProtocol: Int = 0,
        vivoOsName: String = "",
        vivoOsVersion: String = "",
        oneUiVersion: String = "",
        oplusRomVersion: String = "",
        magicOsVersion: String = ""
    ) = DerbisDeviceSignals(
        manufacturer = manufacturer,
        brand = brand,
        sdkInt = sdkInt,
        androidRelease = "16",
        hyperOsVersionCode = hyperOsVersionCode,
        hyperOsVersionName = hyperOsVersionName,
        focusProtocol = focusProtocol,
        vivoOsName = vivoOsName,
        vivoOsVersion = vivoOsVersion,
        oneUiVersion = oneUiVersion,
        oplusRomVersion = oplusRomVersion,
        magicOsVersion = magicOsVersion
    )
}
