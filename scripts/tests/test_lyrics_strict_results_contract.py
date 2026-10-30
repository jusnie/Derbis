import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/yasuda/derbis"


class LyricsStrictResultsContractTest(unittest.TestCase):
    def test_both_search_views_have_no_regular_song_fallback(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        youtube = (MAIN / "ui/YouTubeMusicSearch.kt").read_text()
        self.assertNotIn('LyricsSearchMatch(it, "")', app)
        self.assertNotIn('LyricsSearchMatch(it, "")', youtube)
        self.assertNotIn("lyricsMatches.ifEmpty", app)
        self.assertNotIn("lyricsMatches.ifEmpty", youtube)
        self.assertIn("SearchFilter.Lyrics -> lyricsSearchMatches(data)", youtube)

    def test_lyrics_has_a_dedicated_view_before_either_generic_song_view(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        routing = app.split("filter == SearchFilter.Lyrics -> {", 1)[1]
        self.assertIn("SearchSectionHeader(title = strings.lyrics", routing)
        self.assertIn("when (lyricsContent)", routing)
        self.assertLess(routing.index("when (lyricsContent)"), routing.index("!data.isEmpty && DerbisIsYouTubeMusic"))
        self.assertIn("itemsIndexed(visibleLyricsMatches", routing)

    def test_clearing_a_lyrics_query_does_not_show_normal_discovery_songs(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        self.assertIn("if (queryClean.isEmpty() && filter != SearchFilter.Lyrics)", app)
        self.assertIn("LyricsSearchContent.Prompt -> Unit", app)

    def test_the_current_lyrics_page_drives_loading_empty_and_error_states(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        self.assertIn("state.searchSectionContinuations.containsKey(SearchFilter.Lyrics)", app)
        self.assertIn("lyricsSearchMatches(data, loaded = lyricsLoaded)", app)
        for token in (
            "LyricsSearchContent.Loading", "LyricsSearchContent.Failed", "LyricsSearchContent.Empty",
            "SearchLoadingSkeleton()", "GlassMessage(strings.searchFailed", "strings.formatNoSearchResults(queryClean)",
        ):
            self.assertIn(token, app)

    def test_multi_selection_and_pagination_cannot_reintroduce_catalog_songs(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        self.assertIn("if (filter == SearchFilter.Lyrics) addAll(visibleLyricsMatches.map { it.track })", app)
        self.assertIn("autoLoadKey = visibleLyricsMatches.size", app)
        self.assertIn("SearchFilter.Lyrics -> lyricsMatches.size", app)
        self.assertNotIn("lyricsMatches.ifEmpty { songs }", app)

    def test_the_presentation_policy_uses_only_lyrics_matches(self):
        policy = (MAIN / "feature/search/LyricsSearchPresentation.kt").read_text()
        self.assertIn("results.lyricsMatches.distinctBy { it.track.id }", policy)
        self.assertNotIn("results.songs", policy)
        self.assertIn("loaded && matchCount > 0", policy)
        self.assertIn("!loaded && pending", policy)

    def test_previous_sticky_mode_and_real_lyrics_requests_are_preserved(self):
        engine = (MAIN / "feature/search/DerbisSearchEngine.kt").read_text()
        self.assertIn("filter = retainedQueryFilter(previous.filter)", engine)
        self.assertIn("loadSection(SearchFilter.Lyrics, initial = true)", engine)


if __name__ == "__main__":
    unittest.main()
