package com.luc4n3x.derbis.viewmodel

import com.luc4n3x.derbis.data.effectiveVideoQualityTarget
import com.luc4n3x.derbis.domain.DerbisInterfaceSettings
import com.luc4n3x.derbis.domain.VideoQualityTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSaverPolicyTest {

    @Test
    fun dataSaverBlocksBackgroundWorkButNotTheUserInitiatedFeatureModel() {
        assertFalse(dataSaverBlocksBackgroundNetwork(DerbisInterfaceSettings()))
        assertTrue(dataSaverBlocksBackgroundNetwork(DerbisInterfaceSettings(dataSaverEnabled = true)))
    }

    @Test
    fun changingDataSaverResetsMotionArtworkOwnership() {
        val normal = DerbisInterfaceSettings(dataSaverEnabled = false)
        val saving = normal.copy(dataSaverEnabled = true)

        assertTrue(shouldRefreshMotionArtworkOwnership(normal, saving))
        assertTrue(shouldRefreshMotionArtworkOwnership(saving, normal))
    }

    @Test
    fun dataSaverCapsOnlyAutomaticVideoQuality() {
        assertEquals(
            VideoQualityTarget.P480,
            effectiveVideoQualityTarget(VideoQualityTarget.AUTO, dataSaverEnabled = true)
        )
        assertEquals(
            VideoQualityTarget.P1080,
            effectiveVideoQualityTarget(VideoQualityTarget.P1080, dataSaverEnabled = true)
        )
        assertEquals(
            VideoQualityTarget.AUTO,
            effectiveVideoQualityTarget(VideoQualityTarget.AUTO, dataSaverEnabled = false)
        )
    }
}
