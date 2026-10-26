package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisEqualizerBandsTest {
    @Test
    fun bandLabelsCoverEveryDspBand() {
        assertEquals(DerbisAudioPresets.bandCount, DerbisAudioPresets.bandFrequencyLabels.size)
        assertEquals("31", DerbisAudioPresets.bandFrequencyLabels.first())
        assertEquals("16k", DerbisAudioPresets.bandFrequencyLabels.last())
    }

    @Test
    fun bandLevelsMapToTheDspDecibelRange() {
        assertEquals(0f, DerbisAudioPresets.bandDb(0), 0.0001f)
        assertEquals(12f, DerbisAudioPresets.bandDb(100), 0.0001f)
        assertEquals(-12f, DerbisAudioPresets.bandDb(-100), 0.0001f)
        assertEquals(6f, DerbisAudioPresets.bandDb(50), 0.0001f)
        assertEquals(12f, DerbisAudioPresets.bandDb(400), 0.0001f)
    }

    @Test
    fun verticalDragMapsTopToBoostAndBottomToCut() {
        assertEquals(100, DerbisAudioPresets.bandLevelFromVerticalFraction(0f))
        assertEquals(0, DerbisAudioPresets.bandLevelFromVerticalFraction(0.5f))
        assertEquals(-100, DerbisAudioPresets.bandLevelFromVerticalFraction(1f))
    }

    @Test
    fun verticalDragOutsideTheCurveIsClamped() {
        assertEquals(100, DerbisAudioPresets.bandLevelFromVerticalFraction(-3f))
        assertEquals(-100, DerbisAudioPresets.bandLevelFromVerticalFraction(4.5f))
    }

    @Test
    fun equalizerResetLeavesTheSpatialValueAlone() {
        val tuned = DerbisAudioSettings(
            equalizerEnabled = true,
            presetId = DerbisAudioPresets.GYM,
            bandLevels = DerbisAudioPresets.levelsFor(DerbisAudioPresets.GYM),
            bassBoost = 80,
            virtualizer = 34,
            preampDb = -3.5f
        )

        val reset = tuned.withNeutralEqualizer()

        assertEquals(34, reset.virtualizer)
        assertEquals(DerbisAudioPresets.FLAT, reset.presetId)
        assertEquals(DerbisAudioPresets.flatLevels, reset.bandLevels)
        assertEquals(0, reset.bassBoost)
        assertEquals(0f, reset.preampDb, 0.0001f)
    }

    @Test
    fun equalizerResetKeepsEveryPlaybackSetting() {
        val tuned = DerbisAudioSettings(
            equalizerEnabled = true,
            crossfadeSeconds = 8,
            djSoftMode = true,
            replayGainEnabled = true,
            limiterEnabled = false,
            playbackSpeed = 1.25f,
            pitch = 0.9f,
            gaplessEnabled = false
        )

        val reset = tuned.withNeutralEqualizer()

        assertEquals(true, reset.equalizerEnabled)
        assertEquals(8, reset.crossfadeSeconds)
        assertEquals(true, reset.djSoftMode)
        assertEquals(true, reset.replayGainEnabled)
        assertEquals(false, reset.limiterEnabled)
        assertEquals(1.25f, reset.playbackSpeed, 0.0001f)
        assertEquals(0.9f, reset.pitch, 0.0001f)
        assertEquals(false, reset.gaplessEnabled)
    }

    @Test
    fun everyPresetKeepsTheDspBandContract() {
        DerbisAudioPresets.presets.forEach { preset ->
            assertEquals(preset.id, DerbisAudioPresets.bandCount, preset.levels.size)
            preset.levels.forEach { level ->
                assertEquals(preset.id, level.coerceIn(-100, 100), level)
            }
        }
    }
}
