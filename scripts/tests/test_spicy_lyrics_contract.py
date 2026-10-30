from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/yasuda/derbis"
SPICY = SOURCE / "ui/lyrics/spicy"


class SpicyLyricsContractTest(unittest.TestCase):
    def test_spicy_owns_a_distinct_stage_and_playback_dock(self):
        stage = (SPICY / "SpicyFullscreenLyrics.kt").read_text()
        verse = (SPICY / "SpicyVerseCard.kt").read_text()
        design = (SPICY / "SpicyLyricsDesign.kt").read_text()
        self.assertIn("SpicyPlaybackDock(", stage)
        self.assertIn("SpicyVerseCard(", stage)
        self.assertIn("lineNumber = index + 1", stage)
        self.assertIn("fontFamily = OutfitFont", verse)
        self.assertIn("SpicyLyricsIcons", design)
        self.assertNotIn("AppleMusic13", stage + verse + design)
        self.assertNotIn(".blur(", stage + verse + design)
        self.assertNotIn("scaleX =", verse)
        self.assertNotIn("scaleY =", verse)

    def test_spicy_does_not_fall_back_to_default_in_landscape(self):
        host = (SOURCE / "ui/DerbisApp.kt").read_text()
        self.assertIn("if (isSpicyLyrics) {\n        SpicyFullscreenLyrics(", host)
        self.assertNotIn("if (isSpicyLyrics && !landscapeLayout)", host)
        branch = host.split("SpicyFullscreenLyrics(", 1)[1].split("AppleMusic13FullscreenLyrics(", 1)[0]
        self.assertIn("onClose = onClose", branch)

    def test_word_timing_and_audio_progress_use_separate_clocks(self):
        stage = (SPICY / "SpicyFullscreenLyrics.kt").read_text()
        self.assertIn("lyricsOffsetPosition(clock.positionMs, latestLyricsOffsetMs)", stage)
        self.assertIn("positionProvider = lyricPositionProvider", stage)
        self.assertIn("positionProvider = audioPositionProvider", stage)
        self.assertIn("onSeekToMs((line.startMs + lyricsOffsetMs).coerceAtLeast(0L))", stage)
        verse = (SPICY / "SpicyVerseCard.kt").read_text()
        self.assertIn("wordHighlightTimings(line)", verse)
        self.assertIn("showFurigana && hasKanji", verse)
        self.assertIn("showTranslation && line.translated.isNotBlank()", verse)
        self.assertIn("showRomanization && line.romanized.isNotBlank()", verse)

    def test_share_selection_is_static_and_scrolling_can_return_to_live(self):
        stage = (SPICY / "SpicyFullscreenLyrics.kt").read_text()
        self.assertIn("followingLyrics && !selectionMode", stage)
        self.assertIn("isActive = !selectionMode && index == activeIndex", stage)
        self.assertIn("toggleLyricsShareSelection(shareRange, index, lyrics)", stage)
        self.assertIn("NestedScrollSource.UserInput", stage)
        self.assertIn("strings.spicyLyricsBackToLive", stage)

    def test_both_renderer_previews_share_the_same_sample_and_position(self):
        studio = (SOURCE / "ui/lyrics/studio/LyricsStudioScreen.kt").read_text()
        preview = studio.split("private fun LyricsStudioLivePreview(", 1)[1].split("private fun AppleMusic13StudioLivePreview(", 1)[0]
        for renderer in ("AppleMusic13StudioLivePreview", "SpicyStudioLivePreview"):
            calls = re.findall(rf"{renderer}\((.*?)\n\s*\)", preview, re.S)
            self.assertTrue(calls, renderer)
            for call in calls:
                self.assertIn("previewLines = previewLines", call)
                self.assertIn("playbackPositionMs = activePos", call)
        self.assertIn("var compareMode by remember { mutableStateOf(true) }", preview)
        spicy_preview = (SPICY / "SpicyStudioLivePreview.kt").read_text()
        self.assertIn("SpicyVerseCard(", spicy_preview)
        self.assertIn("spicyPreviewIndices(previewLines.size, activeIndex)", spicy_preview)
        self.assertNotIn("DemoLyrics", spicy_preview)

    def test_default_is_still_the_default_and_there_are_only_two_renderers(self):
        settings = (SOURCE / "domain/DerbisLyricsStudioSettings.kt").read_text()
        self.assertIn("val rendererMode: LyricsRendererMode = LyricsRendererMode.DEFAULT", settings)
        modes = settings.split("enum class LyricsRendererMode {", 1)[1].split(";", 1)[0]
        self.assertEqual(["DEFAULT", "SPICY"], re.findall(r"\b[A-Z_]+\b", modes))
        spicy = settings.split("LyricsRendererMode.SPICY -> copy(", 1)[1].split("\n        )", 1)[0]
        self.assertIn("inactiveScale = 1f", spicy)
        self.assertIn("glowEffect = LyricsGlowEffect.OFF", spicy)
        self.assertIn("blurEnabled = false", spicy)

    def test_new_ui_copy_is_registered_and_has_fallbacks(self):
        bundles = (SOURCE / "ui/i18n/DerbisAppleMusic13LyricsStrings.kt").read_text()
        accessors = (SOURCE / "ui/i18n/DerbisStrings.kt").read_text()
        for key in ("spicyLyricsBackToLive", "lyricsRendererCompare", "lyricsRendererSelectedStyle"):
            self.assertIn(f'"{key}" to ', bundles)
            self.assertIn(f'value("{key}")', accessors)
        keys = set(re.findall(r'"([a-zA-Z0-9]+)"', bundles.split("setOf(", 1)[1].split("\n)", 1)[0]))
        entries = set(re.findall(r'"([a-zA-Z0-9]+)" to ', bundles)) - {"en"}
        self.assertEqual(keys, entries)

    def test_custom_timeline_keeps_native_accessibility_and_touch_targets(self):
        dock = (SPICY / "SpicyPlaybackDock.kt").read_text()
        self.assertIn("Slider(", dock)
        self.assertIn("SpicyTickTimeline(", dock)
        self.assertIn(".height(48.dp)", dock)
        self.assertIn("contentDescription = strings.playback", dock)
        self.assertIn("LocalLayoutDirection provides LayoutDirection.Ltr", dock)
        self.assertIn("spicySeekPosition(it, durationMs)", dock)
        self.assertNotIn("steps =", dock)


if __name__ == "__main__":
    unittest.main()
