import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/yasuda/derbis"
ENGINE = MAIN / "feature/search/DerbisSearchEngine.kt"


class LyricsSearchModeContractTest(unittest.TestCase):
    def test_empty_and_nonempty_query_transitions_keep_the_selected_lyrics_mode(self):
        engine = ENGINE.read_text()
        opening = engine.split("private fun openingSnapshot(", 1)[1].split(
            "private suspend fun runStages(", 1
        )[0]
        self.assertIn("previous: SearchSessionSnapshot", opening)
        self.assertIn("filter = retainedQueryFilter(previous.filter)", opening)
        self.assertNotIn("val previous = _state.value", opening)
        session = engine.split("private suspend fun runSession(", 1)[1].split(
            "private fun openingSnapshot(", 1
        )[0]
        self.assertEqual(2, session.count("_state.update { previous ->"))
        self.assertIn("filter = retainedQueryFilter(previous.filter)", session)

    def test_normal_filter_policy_does_not_become_globally_sticky(self):
        helper = ENGINE.read_text().split("private fun retainedQueryFilter(", 1)[1].split(
            "private fun loadSelectedLyrics(", 1
        )[0]
        self.assertIn("if (filter == SearchFilter.Lyrics) SearchFilter.Lyrics else SearchFilter.All", helper)

    def test_both_cached_and_fresh_sessions_request_the_selected_lyrics_section(self):
        stages = ENGINE.read_text().split("private suspend fun runStages(", 1)[1].split(
            "private fun retainedQueryFilter(", 1
        )[0]
        self.assertEqual(2, stages.count("loadSelectedLyrics(session)"))
        self.assertLess(stages.index("loadSelectedLyrics(session)"), stages.index("cacheHit = true"))
        self.assertLess(stages.index("awaitFullSearchTrigger("), stages.rindex("loadSelectedLyrics(session)"))
        self.assertLess(stages.rindex("loadSelectedLyrics(session)"), stages.index("runRemoteSearch(session)"))

    def test_selected_lyrics_load_checks_current_generation_and_explicit_mode(self):
        loader = ENGINE.read_text().split("private fun loadSelectedLyrics(", 1)[1].split(
            "private fun isSubmitted(", 1
        )[0]
        for token in (
            "activeSession === session", "snapshot.generation == session.generation",
            "snapshot.filter == SearchFilter.Lyrics", "loadSection(SearchFilter.Lyrics, initial = true)",
        ):
            self.assertIn(token, loader)

    def test_overview_and_suggestion_merges_preserve_lyrics_payloads(self):
        merge = (MAIN / "feature/search/SearchProgressiveMerge.kt").read_text()
        self.assertIn(
            "lyricsMatches = (existing.lyricsMatches + incoming.lyricsMatches).distinctBy { it.track.id }",
            merge,
        )

    def test_ui_keeps_one_authoritative_mode_and_an_explicit_off_action(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        self.assertIn("lyricsActive = filter == SearchFilter.Lyrics", app)
        self.assertIn(
            "viewModel.setSearchFilter(if (filter == SearchFilter.Lyrics) SearchFilter.All else SearchFilter.Lyrics)",
            app,
        )
        owner = (MAIN / "viewmodel/DerbisViewModel.kt").read_text()
        self.assertIn("searchFilter = snapshot.filter", owner)


if __name__ == "__main__":
    unittest.main()
