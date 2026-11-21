package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.DerbisExplicitFilter
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplicitContentUiFilterTest {
    @Test
    fun `each setting answers for itself`() {
        assertTrue(DerbisExplicitFilter.Off.allows(explicit = true))
        assertTrue(DerbisExplicitFilter.Off.allows(explicit = false))

        assertFalse(DerbisExplicitFilter.HideExplicit.allows(explicit = true))
        assertTrue(DerbisExplicitFilter.HideExplicit.allows(explicit = false))

        assertTrue(DerbisExplicitFilter.OnlyExplicit.allows(explicit = true))
        assertFalse(DerbisExplicitFilter.OnlyExplicit.allows(explicit = false))
    }

    @Test
    fun `stored values survive a round trip and junk falls back to off`() {
        DerbisExplicitFilter.entries.forEach { filter ->
            assertEquals(filter, DerbisExplicitFilter.fromStorage(filter.storageValue))
        }
        assertEquals(DerbisExplicitFilter.HideExplicit, DerbisExplicitFilter.fromStorage(" hide "))
        assertEquals(DerbisExplicitFilter.Off, DerbisExplicitFilter.fromStorage(null))
        assertEquals(DerbisExplicitFilter.Off, DerbisExplicitFilter.fromStorage("nonsense"))
    }

    @Test
    fun `turning the filter off returns the same state instance`() {
        // The cost of the setting existing must be zero when it is off, so the filter is not even
        // allowed to allocate a copy.
        val state = DerbisUiState(interfaceSettings = DerbisInterfaceSettings())
        assertSame(state, state.withExplicitFiltered())
    }
}
