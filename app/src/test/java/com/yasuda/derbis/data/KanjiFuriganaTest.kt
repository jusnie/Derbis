package com.yasuda.derbis.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Japanese Kanji Furigana (振仮名 / Ruby Annotations)
 * following the SpMp / Kanon pattern.
 */
class KanjiFuriganaTest {

    @Test
    fun nonKanjiTextReturnsSingleSegmentWithoutRuby() {
        val english = KanjiFurigana.parse("Under the city lights tonight")
        assertEquals(1, english.size)
        assertEquals("Under the city lights tonight", english[0].baseText)
        assertEquals("", english[0].rubyText)
        assertFalse(english[0].hasRuby)

        val pureHiragana = KanjiFurigana.parse("ありがとう")
        assertEquals(1, pureHiragana.size)
        assertEquals("ありがとう", pureHiragana[0].baseText)
        assertFalse(pureHiragana[0].hasRuby)
    }

    @Test
    fun containsKanjiDetectionAccuratelyIdentifiesJapaneseKanji() {
        assertTrue(KanjiFurigana.containsKanji("君がいた夏"))
        assertTrue(KanjiFurigana.containsKanji("世界"))
        assertTrue(KanjiFurigana.containsKanji("愛してる"))

        assertFalse(KanjiFurigana.containsKanji("こんにちは"))
        assertFalse(KanjiFurigana.containsKanji("サヨナラ"))
        assertFalse(KanjiFurigana.containsKanji("Hello World"))
        assertFalse(KanjiFurigana.containsKanji("12345!"))
    }

    @Test
    fun explicitAozoraBunkoRubyIsParsedCorrectly() {
        val parsed = KanjiFurigana.parse("｜私《わたし》の心に響くメロディ")
        val rubySegment = parsed.firstOrNull { it.baseText == "私" }

        assertTrue(rubySegment != null)
        assertEquals("わたし", rubySegment?.rubyText)
        assertTrue(rubySegment?.hasRuby == true)
    }

    @Test
    fun explicitParenthesesRubyIsParsedCorrectly() {
        val parsed = KanjiFurigana.parse("運命(さだめ)を信じて")
        val rubySegment = parsed.firstOrNull { it.baseText == "運命" }

        assertTrue(rubySegment != null)
        assertEquals("さだめ", rubySegment?.rubyText)
    }

    @Test
    fun explicitHtmlRubyTagsAreParsedCorrectly() {
        val parsed = KanjiFurigana.parse("<ruby>未来<rt>みらい</rt></ruby>へ")
        val rubySegment = parsed.firstOrNull { it.baseText == "未来" }

        assertTrue(rubySegment != null)
        assertEquals("みらい", rubySegment?.rubyText)
    }

    @Test
    fun jpopCompoundKanjiDictionaryTokenizesCommonLyricsWords() {
        val parsed = KanjiFurigana.parse("世界が終わる前に約束しよう")

        val sekai = parsed.firstOrNull { it.baseText == "世界" }
        assertEquals("せかい", sekai?.rubyText)

        val yakusoku = parsed.firstOrNull { it.baseText == "約束" }
        assertEquals("やくそく", yakusoku?.rubyText)
    }

    @Test
    fun commonJpopLyricsSentenceExtractsKanjiFuriganaWithKanaGlue() {
        val parsed = KanjiFurigana.parse("君がいた夏の夜 笑顔の記憶")

        val kimi = parsed.firstOrNull { it.baseText == "君" }
        assertEquals("きみ", kimi?.rubyText)

        val natsu = parsed.firstOrNull { it.baseText == "夏" }
        assertEquals("なつ", natsu?.rubyText)

        val yoru = parsed.firstOrNull { it.baseText == "夜" }
        assertEquals("よる", yoru?.rubyText)

        val egao = parsed.firstOrNull { it.baseText == "笑顔" }
        assertEquals("えがお", egao?.rubyText)

        val kioku = parsed.firstOrNull { it.baseText == "記憶" }
        assertEquals("きおく", kioku?.rubyText)
    }
}
