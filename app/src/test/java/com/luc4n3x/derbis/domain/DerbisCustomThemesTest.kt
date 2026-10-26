package com.luc4n3x.derbis.domain

import com.luc4n3x.derbis.ui.i18n.themeLibraryCompleteLocalizationCodes
import com.luc4n3x.derbis.ui.i18n.themeLibraryLocalizationCodes
import com.luc4n3x.derbis.ui.theme.DerbisThemes
import com.luc4n3x.derbis.ui.theme.parseThemeHex
import com.luc4n3x.derbis.ui.theme.toPalette
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisCustomThemesTest {

    private val sample = DerbisCustomTheme(
        id = "custom:sample",
        name = "  Night drive  ",
        isLight = false,
        background = 0x05070F,
        surface = 0xFF131A2B.toInt(),
        primary = 0xFF4C7DFF.toInt(),
        secondary = 0xFFE38BD8.toInt(),
        accent = 0xFFFFC978.toInt(),
        cornerScale = 500
    )

    @After
    fun clearRegistry() {
        DerbisThemes.setCustomThemes(emptyList())
    }

    @Test
    fun defaultPresetStaysDerbisAura() {
        assertEquals(DerbisThemes.DERBIS_AURA, DerbisThemes.defaultPresetId)
        assertEquals(DerbisThemes.derbisAura, DerbisThemes.byId(""))
        assertEquals(DerbisThemes.DERBIS_AURA, DerbisThemes.normalize("unknown"))
    }

    @Test
    fun existingPresetsKeepTheirOrderAndNewOnesAreAppended() {
        val ids = DerbisThemes.presets.map { it.id }
        assertEquals(
            listOf("derbis_aura", "cover_flow", "amoled", "frost", "ocean", "ember", "cosmic", "neon_cyan", "purple_glass", "minimal_white", "mood_flow"),
            ids.take(11)
        )
        assertEquals(listOf("midnight", "aurora", "pure_material", "velvet", "editorial"), ids.drop(11))
    }

    @Test
    fun normalizationCleansValues() {
        val normalized = sample.normalized()
        assertEquals("Night drive", normalized.name)
        assertEquals(0xFF05070F.toInt(), normalized.background)
        assertEquals(DerbisCustomTheme.CORNER_SCALE_RANGE.last, normalized.cornerScale)
    }

    @Test
    fun storageRoundTripsAndRejectsGarbage() {
        val themes = listOf(sample.normalized())
        assertEquals(themes, DerbisCustomThemes.decode(DerbisCustomThemes.encode(themes)))
        assertTrue(DerbisCustomThemes.decode("{broken").isEmpty())
        assertTrue(DerbisCustomThemes.decode("""{"themes":[{"id":"preset","background":1,"surface":1,"primary":1,"secondary":1,"accent":1}]}""").isEmpty())
        assertTrue(DerbisCustomThemes.decode("""{"themes":[{"id":"custom:x","name":"no colors"}]}""").isEmpty())
    }

    @Test
    fun libraryOperationsKeepIdsUnique() {
        val saved = DerbisCustomThemes.upsert(emptyList(), sample)
        val duplicated = DerbisCustomThemes.duplicate(saved, sample.id, "copy")
        assertEquals(2, duplicated.size)
        assertEquals("Night drive copy", duplicated[1].name)
        assertTrue(duplicated[0].id != duplicated[1].id)
        val renamed = DerbisCustomThemes.upsert(duplicated, duplicated[0].copy(name = "Renamed"))
        assertEquals("Renamed", renamed[0].name)
        assertEquals(1, DerbisCustomThemes.remove(renamed, sample.id).size)
    }

    @Test
    fun duplicateKeepsSuffixForLongNames() {
        val long = sample.copy(name = "A".repeat(DerbisCustomTheme.MAX_NAME_LENGTH)).normalized()
        val duplicated = DerbisCustomThemes.duplicate(listOf(long), long.id, "copy")
        assertTrue(duplicated[1].name.endsWith(" copy"))
        assertTrue(duplicated[1].name.length <= DerbisCustomTheme.MAX_NAME_LENGTH)
    }

    @Test
    fun customIdsSurviveNormalizationAndResolveThroughRegistry() {
        assertEquals(sample.id, DerbisThemes.normalize(sample.id))
        assertEquals(DerbisThemes.derbisAura, DerbisThemes.byId(sample.id))
        DerbisThemes.setCustomThemes(listOf(sample.normalized()))
        val palette = DerbisThemes.byId(sample.id)
        assertEquals(sample.id, palette.id)
        assertEquals("Night drive", palette.label)
    }

    @Test
    fun customPaletteKeepsAccentsReadable() {
        val dim = sample.copy(primary = 0xFF0A0A20.toInt()).normalized().toPalette()
        val lum = { c: androidx.compose.ui.graphics.Color -> 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue }
        assertTrue(lum(dim.cyan) > lum(dim.black))
    }

    @Test
    fun hexParsingIsStrict() {
        assertEquals(0xFF4C7DFF.toInt(), parseThemeHex("#4c7dff"))
        assertNull(parseThemeHex("4C7DF"))
    }

    @Test
    fun themeLibraryCopyCoversEveryLanguage() {
        val expected = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, themeLibraryLocalizationCodes())
        assertEquals(expected, themeLibraryCompleteLocalizationCodes())
    }
}
