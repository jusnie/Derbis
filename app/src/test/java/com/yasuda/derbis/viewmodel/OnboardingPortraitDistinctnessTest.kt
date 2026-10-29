package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.DerbisContentLocales
import com.yasuda.derbis.domain.DerbisLanguageCatalog
import com.yasuda.derbis.domain.MoodEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OnboardingPortraitDistinctnessTest {
    @Test
    fun tasteSkipsArtistPortraitsAlreadyShownByAnotherTaste() {
        val shown = mapOf("hits" to listOf("artist-a", "artist-b"))

        assertEquals(
            listOf("artist-c"),
            distinctOnboardingPortraits(listOf("artist-a", "artist-c", "artist-b"), shown)
        )
    }

    @Test
    fun tasteKeepsItsPortraitsWhenNoUnusedAlternativeExists() {
        val shown = mapOf("hits" to listOf("artist-a"))

        assertEquals(listOf("artist-a"), distinctOnboardingPortraits(listOf("artist-a"), shown))
    }

    @Test
    fun hitsAndLocalTastesNeverShareTheSameSearchQuery() {
        val engine = MoodEngine()
        DerbisLanguageCatalog.languages.forEach { language ->
            val tastes = engine.tastesForLanguage(language.code).associateBy { it.id }
            assertNotEquals(
                "Hits and local tastes share a query for ${language.code}",
                tastes.getValue("hits").query,
                tastes.getValue("italiana").query
            )
            val moods = DerbisContentLocales.forLanguage(language.code).moodQueries
            if ("hits" in moods && "italia" in moods) {
                assertNotEquals("Hits and local moods share a query for ${language.code}", moods["hits"], moods["italia"])
            }
        }
    }
}
