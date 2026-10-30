package com.yasuda.derbis.ui.player

import com.yasuda.derbis.domain.PlayerBackgroundMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaymarchedLiquidShaderTest {

    @Test
    fun testPlayerBackgroundModeLiquidFerrofluidParsing() {
        assertEquals(PlayerBackgroundMode.LiquidFerrofluid, PlayerBackgroundMode.from("LiquidFerrofluid"))
        assertEquals(PlayerBackgroundMode.LiquidFerrofluid, PlayerBackgroundMode.from("liquid_ferrofluid"))
        assertEquals(PlayerBackgroundMode.LiquidFerrofluid, PlayerBackgroundMode.from("ferrofluid"))
        assertEquals(PlayerBackgroundMode.LiquidFerrofluid, PlayerBackgroundMode.from("liquid"))
        assertEquals(PlayerBackgroundMode.LiquidFerrofluid, PlayerBackgroundMode.from("LIQUIDFERROFLUID"))
    }

    @Test
    fun testExtractLiquidAudioSignalEmpty() {
        val signal = extractLiquidAudioSignal(FloatArray(0))
        assertEquals(0f, signal.bass, 0.001f)
        assertEquals(0f, signal.mid, 0.001f)
        assertEquals(0f, signal.treble, 0.001f)
        assertEquals(0f, signal.energy, 0.001f)
    }

    @Test
    fun testExtractLiquidAudioSignalBands() {
        // 64-sample synthetic frequency spectrum with high bass energy
        val waveform = FloatArray(64) { i ->
            when {
                i < 12 -> 0.8f // Bass heavy
                i < 36 -> 0.2f // Mid
                else -> 0.05f   // Treble
            }
        }
        val signal = extractLiquidAudioSignal(waveform)

        assertTrue("Bass should be dominant", signal.bass > signal.mid)
        assertTrue("Mid should be higher than treble", signal.mid > signal.treble)
        assertTrue("Signal energy should be positive", signal.energy > 0f)
        assertTrue("Bass should not exceed clamp ceiling 2.0", signal.bass <= 2.0f)
    }

    @Test
    fun testAudioTransientAttackDecaySmoothing() {
        var currentBass = 0f
        val attack = 0.35f
        val decay = 0.12f

        // Rapid transient hit (kick drum)
        val incomingTransient = 1.0f
        currentBass += (incomingTransient - currentBass) * attack
        assertTrue("Attack phase must ramp up immediately", currentBass > 0.3f)

        // Sustained hit
        currentBass += (incomingTransient - currentBass) * attack
        assertTrue("Attack phase continues ramping up", currentBass > 0.5f)

        // Drop to silence (decay phase)
        val prevBass = currentBass
        val silence = 0f
        currentBass += (silence - currentBass) * decay
        assertTrue("Decay phase must diminish gradually without instant drop", currentBass < prevBass && currentBass > 0.3f)
    }

    @Test
    fun testShaderCodeIntegrity() {
        assertTrue("AGSL must define uResolution uniform", RAYMARCHED_LIQUID_AGSL.contains("uniform float2 uResolution;"))
        assertTrue("AGSL must define uTime uniform", RAYMARCHED_LIQUID_AGSL.contains("uniform float uTime;"))
        assertTrue("AGSL must define audio reactivity uniforms", RAYMARCHED_LIQUID_AGSL.contains("uniform float uBass;") && RAYMARCHED_LIQUID_AGSL.contains("uniform float uTreble;"))
        assertTrue("AGSL must define smooth min smin", RAYMARCHED_LIQUID_AGSL.contains("float smin("))
        assertTrue("AGSL must contain main function", RAYMARCHED_LIQUID_AGSL.contains("half4 main(float2 fragCoord)"))
    }
}
