package com.yasuda.derbis.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yasuda.derbis.domain.ChartsCatalog
import com.yasuda.derbis.domain.HighQualityAudioMode
import com.yasuda.derbis.domain.DerbisAudioPreset
import java.io.IOException
import com.yasuda.derbis.domain.DerbisAudioPresets
import com.yasuda.derbis.domain.DerbisAudioSettings
import com.yasuda.derbis.domain.PlaybackBufferMode
import com.yasuda.derbis.domain.PlaybackBufferSettings
import com.yasuda.derbis.domain.DerbisAutomationSettings
import com.yasuda.derbis.domain.DerbisLyricsStudioSettings
import com.yasuda.derbis.domain.LyricsAlignment
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import com.yasuda.derbis.domain.DerbisVisualPerformance
import com.yasuda.derbis.domain.PlayerDoubleTapAction
import com.yasuda.derbis.domain.PlayerLongPressAction
import com.yasuda.derbis.domain.PlayerVerticalSwipeAction
import com.yasuda.derbis.domain.PlayerVisualMode
import com.yasuda.derbis.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisPreferencesStoreTest {
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val persisted = MutableStateFlow(emptyPreferences())

        @Volatile
        var readGate: CompletableDeferred<Unit>? = null

        @Volatile
        var readFailuresRemaining: Int = 0

        @Volatile
        var writeFailuresRemaining: Int = 0

        override val data: Flow<Preferences> = flow {
            if (readFailuresRemaining > 0) {
                readFailuresRemaining -= 1
                throw IOException("Injected read failure")
            }
            readGate?.await()
            emitAll(persisted)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (writeFailuresRemaining > 0) {
                writeFailuresRemaining -= 1
                throw IOException("Injected write failure")
            }
            return mutex.withLock { transform(persisted.value).also { persisted.value = it } }
        }
    }

    private val scopes = mutableListOf<CoroutineScope>()
    private val disk = InMemoryPreferencesDataStore()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun open(): Pair<DerbisPreferencesStore, DerbisPreferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val store = DerbisPreferencesStore(disk, scope)
        return store to DerbisPreferences(store)
    }

    private fun reopen(): DerbisPreferences = open().second

    private fun flush(store: DerbisPreferencesStore) = runBlocking { store.commit { } }

    @Test
    fun masterArtworkTogglePersistsBothFlagsAcrossStoreRecreation() = runBlocking {
        val (_, preferences) = open()
        assertFalse(preferences.snapshot().motionArtworkEnabled)
        assertFalse(preferences.interfaceSettings().motionArtworkEnabled)

        preferences.setMotionArtworkEnabled(true)
        assertTrue(preferences.snapshot().motionArtworkEnabled)
        assertTrue(preferences.interfaceSettings().motionArtworkEnabled)
        assertTrue(reopen().snapshot().motionArtworkEnabled)
        assertTrue(reopen().interfaceSettings().motionArtworkEnabled)

        preferences.setMotionArtworkEnabled(false)
        assertFalse(reopen().snapshot().motionArtworkEnabled)
        assertFalse(reopen().interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun interfaceArtworkTogglePersistsTheMasterFlagToo() {
        val (store, preferences) = open()
        preferences.setInterfaceSettings(DerbisInterfaceSettings(motionArtworkEnabled = true))
        assertTrue(preferences.snapshot().motionArtworkEnabled)
        flush(store)
        assertTrue(reopen().snapshot().motionArtworkEnabled)
        assertTrue(reopen().interfaceSettings().motionArtworkEnabled)

        preferences.setInterfaceSettings(DerbisInterfaceSettings(motionArtworkEnabled = false))
        flush(store)
        assertFalse(reopen().snapshot().motionArtworkEnabled)
        assertFalse(reopen().interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun oldMasterOptInIsSharedBySnapshotAndInterface() = runBlocking {
        disk.updateData { current ->
            current.toMutablePreferences().also {
                it[booleanPreferencesKey("motion_artwork_enabled")] = true
                it[booleanPreferencesKey("ui_motion_artwork_enabled")] = false
            }
        }
        val preferences = reopen()
        assertTrue(preferences.snapshot().motionArtworkEnabled)
        assertTrue(preferences.interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun oldExplicitMasterOptOutRemainsAuthoritative() = runBlocking {
        disk.updateData { current ->
            current.toMutablePreferences().also {
                it[booleanPreferencesKey("motion_artwork_enabled")] = false
                it[booleanPreferencesKey("ui_motion_artwork_enabled")] = true
            }
        }
        val preferences = reopen()
        assertFalse(preferences.snapshot().motionArtworkEnabled)
        assertFalse(preferences.interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun interfaceOnlyArtworkPreferenceMigratesWithoutLosingOptIn() = runBlocking {
        disk.updateData { current ->
            current.toMutablePreferences().also {
                it[booleanPreferencesKey("ui_motion_artwork_enabled")] = true
            }
        }
        val preferences = reopen()
        assertTrue(preferences.snapshot().motionArtworkEnabled)
        assertTrue(preferences.interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun restoredArtworkFlagsAreNormalizedToTheMasterChoice() = runBlocking {
        val (_, preferences) = open()
        val enabled = preferences.snapshot().copy(
            motionArtworkEnabled = true,
            interfaceSettings = DerbisInterfaceSettings(motionArtworkEnabled = false)
        )
        preferences.restoreSnapshot(enabled)
        assertTrue(reopen().snapshot().motionArtworkEnabled)
        assertTrue(reopen().interfaceSettings().motionArtworkEnabled)

        preferences.restoreSnapshot(enabled.copy(
            motionArtworkEnabled = false,
            interfaceSettings = DerbisInterfaceSettings(motionArtworkEnabled = true)
        ))
        assertFalse(reopen().snapshot().motionArtworkEnabled)
        assertFalse(reopen().interfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun prismConfigurationSurvivesSkinSwitchAndStoreRecreation() {
        val (store, preferences) = open()
        val prism = com.yasuda.derbis.domain.PrismSettings(
            appearance = com.yasuda.derbis.domain.PrismAppearance.Light,
            playerLayout = com.yasuda.derbis.domain.PrismPlayerLayout.Focus,
            translucency = com.yasuda.derbis.domain.PrismTranslucency.Reduced
        )
        val saved = DerbisInterfaceSettings(
            skin = com.yasuda.derbis.domain.DerbisSkin.LiquidGlass,
            prism = prism, playerVisualMode = PlayerVisualMode.Flow, pureBlack = true
        )
        preferences.setInterfaceSettings(saved)
        flush(store)
        assertEquals(saved, reopen().interfaceSettings())
        preferences.setInterfaceSettings(saved.copy(skin = com.yasuda.derbis.domain.DerbisSkin.Default))
        flush(store)
        assertEquals(prism, reopen().interfaceSettings().prism)
        assertEquals(PlayerVisualMode.Flow, reopen().interfaceSettings().playerVisualMode)
        assertTrue(reopen().interfaceSettings().pureBlack)
    }

    @Test
    fun dataSaverDefaultsOffAndPersistsAcrossStoreRecreation() {
        val (store, preferences) = open()

        assertFalse(preferences.interfaceSettings().dataSaverEnabled)
        preferences.setInterfaceSettings(DerbisInterfaceSettings(dataSaverEnabled = true))
        flush(store)

        assertTrue(reopen().interfaceSettings().dataSaverEnabled)
    }

    @Test
    fun skinPersistsAcrossStoreRecreationWithoutReplacingDefaultCustomization() {
        val (store, preferences) = open()
        preferences.setThemePreset(com.yasuda.derbis.ui.theme.DerbisThemes.FROST)
        preferences.setThemeAccent(0xFF1256AB.toInt())
        preferences.setInterfaceSettings(DerbisInterfaceSettings(skin = com.yasuda.derbis.domain.DerbisSkin.Nocturne78))
        flush(store)
        val reopened = reopen()
        assertEquals(com.yasuda.derbis.domain.DerbisSkin.Nocturne78, reopened.interfaceSettings().skin)
        assertEquals(com.yasuda.derbis.ui.theme.DerbisThemes.FROST, reopened.themePreset())
        assertEquals(0xFF1256AB.toInt(), reopened.themeAccent())
    }

    @Test
    fun lyricsStudioWriteFailureReportsFailureAndKeepsPersistedSettings() = runBlocking {
        val (_, preferences) = open()
        val original = DerbisLyricsStudioSettings()
        val changed = original.copy(alignment = LyricsAlignment.CENTER)
        assertTrue(preferences.setLyricsStudioSettings(original))
        disk.writeFailuresRemaining = 1
        assertFalse(preferences.setLyricsStudioSettings(changed))
        assertEquals(original, preferences.lyricsStudioSettings())
        assertEquals(original, reopen().lyricsStudioSettings())
    }

    @Test
    fun lyricsStudioSuccessfulWriteSurvivesReopening() = runBlocking {
        val (_, preferences) = open()
        val settings = DerbisLyricsStudioSettings().copy(
            alignment = LyricsAlignment.END
        )
        assertTrue(preferences.setLyricsStudioSettings(settings))
        assertEquals(settings, reopen().lyricsStudioSettings())
    }

    @Test
    fun emptyStoreExposesExistingDefaults() {
        val (_, preferences) = open()
        val snapshot = preferences.snapshot()

        assertFalse(snapshot.onboarded)
        assertTrue(snapshot.animationsEnabled)
        assertTrue(snapshot.dynamicColor)
        assertTrue(snapshot.endlessListeningEnabled)
        assertEquals(DEFAULT_SPONSORBLOCK_ENABLED, snapshot.sponsorBlock)
        assertEquals("High", snapshot.audioQuality)
        assertEquals(DerbisAudioSettings().normalized(), preferences.audioSettings())
        assertEquals(PlayerVisualMode.Flow, preferences.interfaceSettings().playerVisualMode)
        assertEquals(DerbisVisualPerformance.Full, preferences.interfaceSettings().visualPerformance)
        assertNull(snapshot.lastTrack)
        assertEquals(DerbisAutomationSettings().normalized(), runBlocking { preferences.automationSettingsFlow.first() })
    }

    @Test
    fun endlessListeningDefaultsToEnabledAndSurvivesStoreRecreation() {
        val (store, preferences) = open()
        assertTrue(preferences.isEndlessListeningEnabled())
        assertTrue(preferences.snapshot().endlessListeningEnabled)

        preferences.setEndlessListeningEnabled(false)
        assertFalse(preferences.isEndlessListeningEnabled())
        assertFalse(preferences.snapshot().endlessListeningEnabled)

        flush(store)
        val reopened = reopen()
        assertFalse(reopened.isEndlessListeningEnabled())
        assertFalse(reopened.snapshot().endlessListeningEnabled)

        reopened.setEndlessListeningEnabled(true)
        assertTrue(reopened.isEndlessListeningEnabled())
    }

    @Test
    fun storedAudioQualityChoicesSurviveTheHighDefault() {
        mapOf("High" to "High", "Auto" to "Auto", "Low" to "Low", "" to "High", "Medium" to "High").forEach { (stored, expected) ->
            runBlocking { disk.edit { it[stringPreferencesKey("audio_quality")] = stored } }
            val (_, preferences) = open()

            assertEquals("stored=\"$stored\"", expected, preferences.audioQuality())
            assertEquals("stored=\"$stored\"", expected, preferences.snapshot().audioQuality)
        }
    }

    @Test
    fun restoringASnapshotKeepsAnExplicitAudioQualityChoice() {
        listOf("Auto", "High", "Low").forEach { choice ->
            val (store, preferences) = open()
            runBlocking { preferences.restoreSnapshot(preferences.snapshot().copy(audioQuality = choice)) }
            flush(store)

            assertEquals(choice, preferences.audioQuality())
            assertEquals(choice, reopen().audioQuality())
        }
    }

    @Test
    fun persistedValuesAndLegacyMappingsLoadIntoMemory() {
        runBlocking {
            disk.edit {
                it[booleanPreferencesKey("onboarded")] = true
                it[stringPreferencesKey("user_name")] = "Luca"
                it[intPreferencesKey("audio_crossfade_seconds")] = 6
                it[booleanPreferencesKey("motion_artwork_enabled")] = true
            }
        }
        val (_, preferences) = open()

        assertTrue(preferences.isOnboarded())
        assertEquals("Luca", preferences.userName())
        assertEquals(6, preferences.audioSettings().crossfadeSeconds)
        assertEquals(PlayerVisualMode.CanvasCard, preferences.interfaceSettings().playerVisualMode)
    }

    @Test
    fun writesAreVisibleImmediatelyAndSurviveRecreation() {
        val (store, preferences) = open()

        preferences.setUserName("Luca")
        preferences.setLanguageCode("it")
        preferences.setDynamicColor(false)
        preferences.setSkipSilence(true)
        preferences.setJamDisplayName("  DJ Luca  ")
        assertEquals("Luca", preferences.userName())
        assertEquals("it", preferences.languageCode())
        assertFalse(preferences.dynamicColor())
        assertTrue(preferences.skipSilence())
        assertEquals("DJ Luca", preferences.jamDisplayName())

        flush(store)
        val reopened = reopen()

        assertEquals("Luca", reopened.userName())
        assertEquals("it", reopened.languageCode())
        assertFalse(reopened.dynamicColor())
        assertTrue(reopened.skipSilence())
        assertEquals("DJ Luca", reopened.jamDisplayName())
    }

    @Test
    fun upgradedStoreWithoutVisualPerformanceKeyResolvesToFull() {
        runBlocking {
            disk.edit {
                it[booleanPreferencesKey("onboarded")] = true
                it[stringPreferencesKey("ui_canvas_quality")] = "High"
                it[booleanPreferencesKey("ui_player_gestures")] = false
            }
        }
        val (_, preferences) = open()

        assertEquals(DerbisVisualPerformance.Full, preferences.interfaceSettings().visualPerformance)
    }

    @Test
    fun unknownStoredVisualPerformanceFallsBackToFull() {
        runBlocking {
            disk.edit { it[stringPreferencesKey("ui_visual_performance")] = "Turbo" }
        }
        val (_, preferences) = open()

        assertEquals(DerbisVisualPerformance.Full, preferences.interfaceSettings().visualPerformance)
    }

    @Test
    fun chartMarketIsUnsetUntilChosenAndSurvivesRecreation() {
        val (store, preferences) = open()
        assertEquals("", preferences.chartRegionId())

        preferences.setChartRegionId("ru")
        assertEquals("ru", preferences.chartRegionId())
        flush(store)

        val reopened = reopen()
        assertEquals("ru", reopened.chartRegionId())
        assertEquals(
            "ru",
            ChartsCatalog.startupRegion(
                storedRegionId = reopened.chartRegionId(),
                deviceCountry = "IT",
                languageCode = reopened.languageCode()
            ).id
        )
    }

    @Test
    fun visualPerformanceChoiceSurvivesRecreation() {
        val (store, preferences) = open()
        preferences.setInterfaceSettings(DerbisInterfaceSettings(visualPerformance = DerbisVisualPerformance.Smooth))
        flush(store)

        assertEquals(DerbisVisualPerformance.Smooth, reopen().interfaceSettings().visualPerformance)
    }

    @Test
    fun playerGestureActionsSurviveRecreation() {
        val (store, preferences) = open()
        val configured = DerbisInterfaceSettings(
            releaseNotificationsEnabled = true,
            swipeTrackChangeEnabled = false,
            doubleTapAction = PlayerDoubleTapAction.PlayPause,
            longPressAction = PlayerLongPressAction.Queue,
            verticalSwipeAction = PlayerVerticalSwipeAction.Volume
        )

        preferences.setInterfaceSettings(configured)
        flush(store)

        assertEquals(configured, reopen().interfaceSettings())
    }

    @Test
    fun rapidWritesKeepLastIntent() {
        val (store, preferences) = open()

        repeat(200) { index ->
            preferences.setAudioSettings(DerbisAudioSettings(crossfadeSeconds = index % 12))
        }
        preferences.setAudioSettings(DerbisAudioSettings(crossfadeSeconds = 3, playbackSpeed = 1.25f, pitch = 0.9f))
        assertEquals(3, preferences.audioSettings().crossfadeSeconds)

        flush(store)
        val reopened = reopen().audioSettings()

        assertEquals(3, reopened.crossfadeSeconds)
        assertEquals(1.25f, reopened.playbackSpeed, 0f)
        assertEquals(0.9f, reopened.pitch, 0f)
    }

    @Test
    fun customPlaybackBufferIsVisibleImmediatelyAndSurvivesRecreation() {
        val (store, preferences) = open()
        val configured = PlaybackBufferSettings(
            mode = PlaybackBufferMode.CUSTOM,
            minBufferSeconds = 9f,
            maxBufferSeconds = 30f,
            playbackBufferSeconds = 0.8f,
            rebufferSeconds = 1.4f
        )

        preferences.setAudioSettings(DerbisAudioSettings(playbackBuffer = configured))
        assertEquals(configured, preferences.audioSettings().playbackBuffer)
        flush(store)

        assertEquals(configured, reopen().audioSettings().playbackBuffer)
    }

    @Test
    fun concurrentWritesToDifferentKeysAreAllKept() {
        val (store, preferences) = open()

        runBlocking {
            (0 until 50).map { index ->
                async(Dispatchers.Default) {
                    if (index % 2 == 0) preferences.setUserName("user-$index") else preferences.setAudioQuality("High")
                    preferences.setListeningLifetimeBackfillVersion(7)
                }
            }.awaitAll()
        }
        preferences.setUserName("final")
        flush(store)

        val reopened = reopen()
        assertEquals("final", reopened.userName())
        assertEquals("High", reopened.audioQuality())
        assertEquals(7, reopened.listeningLifetimeBackfillVersion())
    }

    @Test
    fun transientInitialReadFailureRecoversPersistedValues() {
        runBlocking {
            disk.edit {
                it[stringPreferencesKey("user_name")] = "Recovered"
                it[booleanPreferencesKey("onboarded")] = true
            }
        }
        disk.readFailuresRemaining = 1

        val (_, preferences) = open()

        assertEquals("Recovered", preferences.userName())
        assertTrue(preferences.isOnboarded())
    }

    @Test
    fun failedWriteRollsBackOptimisticValueAndKeepsNewerWrites() {
        runBlocking {
            disk.edit {
                it[stringPreferencesKey("user_name")] = "Persisted"
                it[stringPreferencesKey("audio_quality")] = "Auto"
            }
        }
        val (store, preferences) = open()
        assertEquals("Persisted", preferences.userName())
        disk.writeFailuresRemaining = 1

        preferences.setUserName("Failed")
        preferences.setAudioQuality("High")
        flush(store)

        assertEquals("Persisted", preferences.userName())
        assertEquals("High", preferences.audioQuality())

        val reopened = reopen()
        assertEquals("Persisted", reopened.userName())
        assertEquals("High", reopened.audioQuality())
    }

    @Test
    fun writesIssuedBeforeInitialLoadAreAppliedOverPersistedState() {
        runBlocking {
            disk.edit {
                it[stringPreferencesKey("user_name")] = "Persisted"
                it[booleanPreferencesKey("onboarded")] = true
            }
        }
        val gate = CompletableDeferred<Unit>()
        disk.readGate = gate
        val (store, preferences) = open()

        preferences.setUserName("Newer")
        gate.complete(Unit)

        assertEquals("Newer", preferences.userName())
        assertTrue(preferences.isOnboarded())
        flush(store)
        disk.readGate = null
        val reopened = reopen()
        assertEquals("Newer", reopened.userName())
        assertTrue(reopened.isOnboarded())
    }

    @Test
    fun restoreSnapshotIsVisibleImmediatelyAndDurable() {
        val (_, preferences) = open()
        val customPreset = DerbisAudioPreset(
            id = "${DerbisAudioPresets.CUSTOM_PRESET_PREFIX}night",
            fallbackLabel = "Night",
            levels = List(DerbisAudioPresets.bandCount) { it - 2 },
            bassBoost = 60,
            virtualizer = 40,
            preampDb = -2f
        )
        val restored = preferences.snapshot().copy(
            onboarded = true,
            userName = "Restored",
            languageCode = "de",
            highQualityAudioMode = HighQualityAudioMode.OFF,
            audioSettings = DerbisAudioSettings(
                equalizerEnabled = true,
                presetId = customPreset.id,
                bandLevels = customPreset.levels,
                customPresets = listOf(customPreset),
                crossfadeSeconds = 5
            ),
            interfaceSettings = DerbisInterfaceSettings(playerVisualMode = PlayerVisualMode.CanvasCard),
            automationSettings = DerbisAutomationSettings(pauseOnMute = true),
            jamDisplayName = "Jam"
        )
        preferences.setUserName("Before restore")

        runBlocking { preferences.restoreSnapshot(restored) }

        assertEquals("Restored", preferences.userName())
        assertEquals("de", preferences.languageCode())
        assertEquals(HighQualityAudioMode.OFF, preferences.highQualityAudioMode())
        assertEquals(listOf(customPreset), preferences.audioSettings().customPresets)
        assertTrue(runBlocking { preferences.automationSettingsFlow.first() }.pauseOnMute)

        val snapshot = reopen().snapshot()
        assertTrue(snapshot.onboarded)
        assertEquals("Restored", snapshot.userName)
        assertEquals("de", snapshot.languageCode)
        assertEquals(HighQualityAudioMode.OFF, snapshot.highQualityAudioMode)
        assertEquals(customPreset.id, snapshot.audioSettings.presetId)
        assertEquals(listOf(customPreset), snapshot.audioSettings.customPresets)
        assertEquals(5, snapshot.audioSettings.crossfadeSeconds)
        assertEquals(PlayerVisualMode.CanvasCard, snapshot.interfaceSettings.playerVisualMode)
        assertTrue(snapshot.automationSettings.pauseOnMute)
        assertEquals("Jam", snapshot.jamDisplayName)
    }

    @Test
    fun structuredValuesAreParsedOncePerStateAndRefreshAfterWrites() {
        val (store, preferences) = open()
        val track = Track(
            id = "abc",
            title = "Song",
            artist = "Artist",
            album = "",
            durationMs = 1000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "youtube",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
        preferences.saveRecentSearches(listOf(track))
        preferences.saveLastPlayback(track, 4200L)

        val first = preferences.snapshot()
        assertSame(first, preferences.snapshot())
        assertEquals(listOf("abc"), preferences.loadRecentSearches().map { it.id })
        assertEquals("abc", preferences.lastTrack()?.id)
        assertEquals(4200L, preferences.lastPositionMs())

        flush(store)
        val reopened = reopen()
        assertEquals("abc", reopened.lastTrack()?.id)
        assertEquals(4200L, reopened.lastPositionMs())

        preferences.saveLastPlayback(null, 0L)
        assertNull(preferences.lastTrack())
        assertEquals(0L, preferences.lastPositionMs())
    }

    @Test
    fun automationFlowFollowsInMemoryWrites() {
        val (_, preferences) = open()

        runBlocking { preferences.setAutomationSettings(DerbisAutomationSettings(resumeOnBluetoothReconnect = true)) }

        assertTrue(runBlocking { preferences.automationSettingsFlow.first() }.resumeOnBluetoothReconnect)
    }
}
