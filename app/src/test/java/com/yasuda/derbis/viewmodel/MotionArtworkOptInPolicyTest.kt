package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.data.resolveMotionArtworkEnabled
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkOptInPolicyTest {
    @Test
    fun newInstallsRemainOptedOut() {
        assertFalse(resolveMotionArtworkEnabled(null, null))
    }

    @Test
    fun legacyMasterOptInIsNotLostToTheSeparateInterfaceDefault() {
        assertTrue(resolveMotionArtworkEnabled(true, null))
        assertTrue(resolveMotionArtworkEnabled(true, false))
        assertTrue(resolveMotionArtworkEnabled(true, true))
    }

    @Test
    fun explicitMasterOptOutIsNeverOverriddenDuringMigration() {
        assertFalse(resolveMotionArtworkEnabled(false, null))
        assertFalse(resolveMotionArtworkEnabled(false, false))
        assertFalse(resolveMotionArtworkEnabled(false, true))
    }

    @Test
    fun interfaceOnlyInstallKeepsItsExistingChoice() {
        assertTrue(resolveMotionArtworkEnabled(null, true))
        assertFalse(resolveMotionArtworkEnabled(null, false))
    }

    @Test
    fun enableAndDisableBothInvalidateArtworkOwnership() {
        val disabled = DerbisInterfaceSettings(motionArtworkEnabled = false)
        val enabled = disabled.copy(motionArtworkEnabled = true)

        assertTrue(shouldRefreshMotionArtworkOwnership(disabled, enabled))
        assertTrue(shouldRefreshMotionArtworkOwnership(enabled, disabled))
        assertFalse(shouldRefreshMotionArtworkOwnership(enabled, enabled))
    }

    @Test
    fun networkPreferenceChangesStillInvalidateOwnership() {
        val original = DerbisInterfaceSettings(motionArtworkEnabled = true)

        assertTrue(shouldRefreshMotionArtworkOwnership(original, original.copy(motionArtworkWifiOnly = true)))
        assertTrue(shouldRefreshMotionArtworkOwnership(original, original.copy(dataSaverEnabled = true)))
        assertFalse(shouldRefreshMotionArtworkOwnership(original, original.copy(compactHome = true)))
    }
}
