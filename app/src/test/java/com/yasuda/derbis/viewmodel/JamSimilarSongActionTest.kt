package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.feature.jam.JamGuestPermission
import com.yasuda.derbis.feature.jam.JamRole
import com.yasuda.derbis.feature.jam.JamUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JamSimilarSongActionTest {

    @Test
    fun collaborativeGuestSelectsATrackTheSharedQueueAlreadyHolds() {
        assertEquals(
            JamSimilarSongAction.SelectExisting,
            jamSimilarSongAction(guest(JamGuestPermission.Collaborative), existingIndex = 4)
        )
    }

    @Test
    fun collaborativeGuestAddsThenSelectsATrackThatIsMissing() {
        assertEquals(
            JamSimilarSongAction.AddThenSelect,
            jamSimilarSongAction(guest(JamGuestPermission.Collaborative), existingIndex = -1)
        )
    }

    @Test
    fun addSongsGuestOnlyProposesTheTrack() {
        assertEquals(
            JamSimilarSongAction.AddOnly,
            jamSimilarSongAction(guest(JamGuestPermission.AddSongs), existingIndex = -1)
        )
        assertEquals(
            JamSimilarSongAction.AddOnly,
            jamSimilarSongAction(guest(JamGuestPermission.AddSongs), existingIndex = 2)
        )
    }

    @Test
    fun hostOnlyGuestIsRejected() {
        assertEquals(
            JamSimilarSongAction.Reject,
            jamSimilarSongAction(guest(JamGuestPermission.HostOnly), existingIndex = -1)
        )
    }

    @Test
    fun similarSongCardOffersOnlyWhatTheSessionPermissionAllows() {
        val collaborative = playerProjection(DerbisUiState(jam = guest(JamGuestPermission.Collaborative)))
        assertTrue(collaborative.canPlaySimilarSongNow)
        assertTrue(collaborative.canQueueSimilarSong)

        val addSongs = playerProjection(DerbisUiState(jam = guest(JamGuestPermission.AddSongs)))
        assertFalse(addSongs.canPlaySimilarSongNow)
        assertTrue(addSongs.canQueueSimilarSong)

        val hostOnly = playerProjection(DerbisUiState(jam = guest(JamGuestPermission.HostOnly)))
        assertFalse(hostOnly.canPlaySimilarSongNow)
        assertFalse(hostOnly.canQueueSimilarSong)
    }

    @Test
    fun similarSongCardStaysFullyEnabledOutsideAJam() {
        val solo = playerProjection(DerbisUiState())

        assertTrue(solo.canPlaySimilarSongNow)
        assertTrue(solo.canQueueSimilarSong)
    }

    private fun guest(permission: JamGuestPermission) = JamUiState(
        role = JamRole.Guest,
        permission = permission
    )
}
