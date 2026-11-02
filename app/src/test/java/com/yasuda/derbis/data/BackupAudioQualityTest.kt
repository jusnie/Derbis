package com.yasuda.derbis.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupAudioQualityTest {
    @Test
    fun backupWithoutAudioQualityGetsTheHighDefault() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject()))
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", JSONObject.NULL)))
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "")))
    }

    @Test
    fun explicitBackupChoicesAreRestoredUnchanged() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "High")))
        assertEquals("Medium", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Medium")))
        assertEquals("Low", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Low")))
        assertEquals("Lossless", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Lossless")))
    }

    @Test
    fun legacyAutoBackupsLandOnTheHighBranchTheyAlreadyTook() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Auto")))
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "auto")))
    }

    @Test
    fun corruptBackupValueFallsBackToTheHighDefault() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Ultra")))
    }
}
