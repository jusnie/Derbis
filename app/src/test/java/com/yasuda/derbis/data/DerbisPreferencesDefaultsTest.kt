package com.yasuda.derbis.data

import com.yasuda.derbis.domain.DerbisAudioSettings
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.DerbisCanvasSource
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import com.yasuda.derbis.domain.DerbisVisualPerformance
import com.yasuda.derbis.domain.PlayerBackgroundMode
import com.yasuda.derbis.domain.PlayerColorMode
import com.yasuda.derbis.domain.PlayerDoubleTapAction
import com.yasuda.derbis.domain.PlayerLongPressAction
import com.yasuda.derbis.domain.PlayerVerticalSwipeAction
import com.yasuda.derbis.domain.PlayerVisualMode
import com.yasuda.derbis.domain.ReactiveAuraPreset
import com.yasuda.derbis.viewmodel.DerbisUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisPreferencesDefaultsTest {
    @Test
    fun prismBackupPreservesVariantsAndOldBackupsUseSafeDefaults() {
        val prism = com.yasuda.derbis.domain.PrismSettings(
            appearance = com.yasuda.derbis.domain.PrismAppearance.Automatic,
            playerLayout = com.yasuda.derbis.domain.PrismPlayerLayout.Focus,
            translucency = com.yasuda.derbis.domain.PrismTranslucency.Reduced
        )
        val original = DerbisInterfaceSettings(skin = com.yasuda.derbis.domain.DerbisSkin.Torque, prism = prism, torque = com.yasuda.derbis.domain.TorqueSettings(com.yasuda.derbis.domain.TorqueAccent.Red), ivory = com.yasuda.derbis.domain.IvorySettings(com.yasuda.derbis.domain.IvoryAppearance.Dark))
        assertEquals(original, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original)))
        val spotify = DerbisInterfaceSettings(skin = com.yasuda.derbis.domain.DerbisSkin.SpotifyStyle)
        assertEquals(spotify, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(spotify)))
        assertEquals(com.yasuda.derbis.domain.PrismSettings(), backupInterfaceSettingsFromJson(JSONObject()).prism)
        assertEquals(com.yasuda.derbis.domain.PrismSettings(), backupInterfaceSettingsFromJson(JSONObject().put("prismAppearance", "unknown")).prism)
    }

    @Test
    fun skinBackupIsBackwardCompatibleAndPreservesPersonalSettings() {
        val original = DerbisInterfaceSettings(
            skin = com.yasuda.derbis.domain.DerbisSkin.Nocturne78,
            pureBlack = true,
            playerColorMode = PlayerColorMode.Custom,
            playerColorAccent = 0xFF1256AB.toInt()
        )
        assertEquals(original, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original)))
        assertEquals(com.yasuda.derbis.domain.DerbisSkin.Default, backupInterfaceSettingsFromJson(JSONObject()).skin)
        assertEquals(com.yasuda.derbis.domain.DerbisSkin.Default, DerbisInterfaceSettings().skin)
    }

    @Test
    fun sponsorBlockIsEnabledByDefault() {
        assertTrue(DEFAULT_SPONSORBLOCK_ENABLED)
        assertTrue(DerbisUiState().sponsorBlockEnabled)
    }

    @Test
    fun liquidGlassDefaultsToEnabledEverywhere() {
        assertTrue(DerbisInterfaceSettings().liquidGlassEnabled)
        assertTrue(DerbisUiState().interfaceSettings.liquidGlassEnabled)
        assertTrue(backupInterfaceSettingsFromJson(JSONObject()).liquidGlassEnabled)
        assertTrue(backupInterfaceSettingsFromJson(null).liquidGlassEnabled)
    }

    @Test
    fun backupRoundTripPreservesLiquidGlassChoice() {
        listOf(true, false).forEach { enabled ->
            assertEquals(
                enabled,
                backupInterfaceSettingsFromJson(
                    backupInterfaceSettingsToJson(DerbisInterfaceSettings(liquidGlassEnabled = enabled))
                ).liquidGlassEnabled
            )
        }
    }

    @Test
    fun expressiveGlassStyleDefaultsAndBackupRoundTrip() {
        val defaults = backupInterfaceSettingsFromJson(JSONObject())
        assertEquals(DerbisInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassBlur)
        assertEquals(DerbisInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassTransparency)
        assertEquals(DerbisInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassTint)
        assertTrue(defaults.glassRefraction)

        val custom = DerbisInterfaceSettings(
            glassBlur = 80,
            glassTransparency = 20,
            glassTint = 65,
            glassRefraction = false
        )
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(custom))
        assertEquals(80, restored.glassBlur)
        assertEquals(20, restored.glassTransparency)
        assertEquals(65, restored.glassTint)
        assertFalse(restored.glassRefraction)
        assertEquals(100, DerbisInterfaceSettings(glassBlur = 140).normalized().glassBlur)
    }

    @Test
    fun backupRoundTripPreservesPreampAndLimiterSettings() {
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(
                DerbisAudioSettings(preampDb = -4.5f, limiterEnabled = false)
            )
        )

        assertEquals(-4.5f, restored.preampDb, 0f)
        assertFalse(restored.limiterEnabled)
    }

    @Test
    fun legacyBackupKeepsPreampAndLimiterDefaults() {
        val restored = backupAudioSettingsFromJson(JSONObject())

        assertEquals(0f, restored.preampDb, 0f)
        assertTrue(restored.limiterEnabled)
    }

    @Test
    fun visualPerformanceDefaultsToFullEverywhere() {
        assertEquals(DerbisVisualPerformance.Full, DerbisInterfaceSettings().visualPerformance)
        assertEquals(DerbisVisualPerformance.Full, DerbisUiState().interfaceSettings.visualPerformance)
        assertEquals(DerbisVisualPerformance.Full, DerbisVisualPerformance.from(""))
        assertEquals(DerbisVisualPerformance.Full, DerbisVisualPerformance.from("unknown"))
        assertEquals(DerbisVisualPerformance.Full, backupInterfaceSettingsFromJson(JSONObject()).visualPerformance)
        assertEquals(DerbisVisualPerformance.Full, backupInterfaceSettingsFromJson(null).visualPerformance)
    }

    @Test
    fun backupRoundTripPreservesVisualPerformance() {
        DerbisVisualPerformance.entries.forEach { mode ->
            val restored = backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(DerbisInterfaceSettings(visualPerformance = mode))
            )
            assertEquals(mode, restored.visualPerformance)
        }
    }

    @Test
    fun legacyBackupPreservesCanvasPreferenceWhenVisualModeMissing() {
        val restoredWithCanvas = backupInterfaceSettingsFromJson(
            JSONObject(),
            legacyVisualMode = PlayerVisualMode.CanvasImmersive
        )
        assertEquals(PlayerVisualMode.CanvasImmersive, restoredWithCanvas.playerVisualMode)

        val restoredWithoutCanvas = backupInterfaceSettingsFromJson(
            JSONObject(),
            legacyVisualMode = PlayerVisualMode.Artwork
        )
        assertEquals(PlayerVisualMode.Artwork, restoredWithoutCanvas.playerVisualMode)
    }

    @Test
    fun backupRoundTripPreservesPlayerVisualAndCanvasSettings() {
        val original = DerbisInterfaceSettings(
            releaseNotificationsEnabled = true,
            canvasQuality = DerbisCanvasQuality.High,
            canvasSource = DerbisCanvasSource.Auto,
            motionArtworkWifiOnly = true,
            enhanceVideoMetadata = true,
            playerVisualMode = PlayerVisualMode.CanvasCard,
            playerBackground = PlayerBackgroundMode.Blur
        )
        val restored = backupInterfaceSettingsFromJson(
            backupInterfaceSettingsToJson(original),
            legacyVisualMode = PlayerVisualMode.Artwork
        )

        assertEquals(DerbisCanvasQuality.High, restored.canvasQuality)
        assertEquals(DerbisCanvasSource.Auto, restored.canvasSource)
        assertTrue(restored.motionArtworkWifiOnly)
        assertTrue(restored.enhanceVideoMetadata)
        assertTrue(restored.releaseNotificationsEnabled)
        assertEquals(PlayerVisualMode.CanvasCard, restored.playerVisualMode)
        assertEquals(PlayerBackgroundMode.Blur, restored.playerBackground)
    }

    @Test
    fun backupRoundTripPreservesPlayerGestureActions() {
        val original = DerbisInterfaceSettings(
            swipeTrackChangeEnabled = false,
            doubleTapAction = PlayerDoubleTapAction.Favorite,
            longPressAction = PlayerLongPressAction.Lyrics,
            verticalSwipeAction = PlayerVerticalSwipeAction.Volume
        )

        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))

        assertFalse(restored.swipeTrackChangeEnabled)
        assertEquals(PlayerDoubleTapAction.Favorite, restored.doubleTapAction)
        assertEquals(PlayerLongPressAction.Lyrics, restored.longPressAction)
        assertEquals(PlayerVerticalSwipeAction.Volume, restored.verticalSwipeAction)
    }

    @Test
    fun legacyBackupKeepsProfessionalGestureDefaults() {
        val restored = backupInterfaceSettingsFromJson(JSONObject())

        assertTrue(restored.swipeTrackChangeEnabled)
        assertEquals(PlayerDoubleTapAction.Seek, restored.doubleTapAction)
        assertEquals(PlayerLongPressAction.Speed, restored.longPressAction)
        assertEquals(PlayerVerticalSwipeAction.BrightnessAndVolume, restored.verticalSwipeAction)
    }

    @Test
    fun legacyPureBlackMapsToPureBlackBackgroundWhenUnset() {
        val restored = backupInterfaceSettingsFromJson(JSONObject().put("pureBlack", true))
        assertEquals(PlayerBackgroundMode.PureBlack, restored.playerBackground)
    }

    @Test
    fun backupRoundTripPreservesReactiveAuraSettings() {
        val original = DerbisInterfaceSettings(
            playerBackground = PlayerBackgroundMode.ReactiveAura,
            reactiveAuraPreset = ReactiveAuraPreset.Pulse,
            reactiveAuraIntensity = 75
        )

        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))

        assertEquals(PlayerBackgroundMode.ReactiveAura, restored.playerBackground)
        assertEquals(ReactiveAuraPreset.Pulse, restored.reactiveAuraPreset)
        assertEquals(75, restored.reactiveAuraIntensity)
    }

    @Test
    fun defaultReactiveAuraSettingsAreFlowAnd50() {
        val defaults = backupInterfaceSettingsFromJson(JSONObject())
        assertEquals(ReactiveAuraPreset.Flow, defaults.reactiveAuraPreset)
        assertEquals(50, defaults.reactiveAuraIntensity)
    }

    @Test
    fun defaultPlayerVisualModeIsFlow() {
        assertEquals(PlayerVisualMode.Flow, DerbisInterfaceSettings().playerVisualMode)
        assertFalse(DerbisInterfaceSettings().motionArtworkWifiOnly)
        assertFalse(DerbisInterfaceSettings().motionArtworkEnabled)
    }

    @Test
    fun dataSaverDefaultsOffAndRoundTripsThroughBackup() {
        assertFalse(DerbisInterfaceSettings().dataSaverEnabled)
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).dataSaverEnabled)
        assertTrue(
            backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(DerbisInterfaceSettings(dataSaverEnabled = true))
            ).dataSaverEnabled
        )
    }

    @Test
    fun legacyBackupWithoutMotionArtworkWifiOnlyDefaultsToFalse() {
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).motionArtworkWifiOnly)
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).motionArtworkEnabled)
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).releaseNotificationsEnabled)
        assertTrue(
            backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(DerbisInterfaceSettings(motionArtworkEnabled = true))
            ).motionArtworkEnabled
        )
    }

    @Test
    fun legacyBackupWithoutVisualModeDefaultsToCanvasCard() {
        assertEquals(PlayerVisualMode.CanvasCard, backupInterfaceSettingsFromJson(JSONObject()).playerVisualMode)
    }

    @Test
    fun playerColorsDefaultToDynamicAndRoundTripThroughBackup() {
        assertEquals(PlayerColorMode.Dynamic, DerbisInterfaceSettings().playerColorMode)
        assertEquals(PlayerColorMode.Dynamic, backupInterfaceSettingsFromJson(JSONObject()).playerColorMode)
        assertEquals(PlayerColorMode.Dynamic, PlayerColorMode.from("unknown"))
        val original = DerbisInterfaceSettings(playerColorMode = PlayerColorMode.Custom, playerColorAccent = 0x0033AAFF)
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))
        assertEquals(PlayerColorMode.Custom, restored.playerColorMode)
        assertEquals(0xFF33AAFF.toInt(), restored.playerColorAccent)
        val theme = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(DerbisInterfaceSettings(playerColorMode = PlayerColorMode.Theme)))
        assertEquals(PlayerColorMode.Theme, theme.playerColorMode)
    }
}
