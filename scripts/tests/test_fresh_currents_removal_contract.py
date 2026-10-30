import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/yasuda/derbis"


class FreshCurrentsRemovalContractTest(unittest.TestCase):
    def test_carousel_renderer_is_removed_not_hidden(self):
        self.assertFalse((MAIN / "ui/ExploreFreshCurrents.kt").exists())
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        for symbol in (
            "ExploreFreshSpotlight(", "ExploreFreshSectionHeader(",
            "ExploreFreshScopeRail(", "ExploreFreshMomentRail(",
        ):
            self.assertNotIn(symbol, app)

    def test_no_carousel_rows_or_anchor_remain(self):
        planner = (MAIN / "ui/ExploreLayout.kt").read_text()
        for symbol in (
            "FreshLoading", "FreshEmpty", "FreshScopes", "FreshSpotlight", "FreshMoment",
            "ExploreAnchor.Fresh", "isFreshLoading", "hasFreshTracks",
        ):
            self.assertNotIn(symbol, planner)
        self.assertIn("internal fun buildExploreRows(zones: List<ExploreZone>)", planner)

    def test_explore_no_longer_prepares_the_removed_carousel_or_its_actions(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        screen = app.split("fun ExploreScreen(", 1)[1].split(
            "private fun ExploreContentRow(", 1
        )[0]
        for symbol in (
            "exploreFreshTracks", "exploreWorldFreshTracks", "isFreshCurrentsLoading",
            "freshScope", "freshTrackActions", "onPlayFresh", "trackStoryShareTarget",
        ):
            self.assertNotIn(symbol, screen)
        self.assertIn("remember(zones) { buildExploreRows(zones) }", screen)

    def test_independent_new_releases_browser_remains_available(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        planner = (MAIN / "ui/ExploreLayout.kt").read_text()
        self.assertIn("NewReleases(null, ExploreCatalog.NEW_RELEASES_ZONE_ID)", planner)
        self.assertIn("ExploreShortcut.NewReleases -> exploreDestination = ExploreNewReleasesDestination", app)
        shortcuts = app.split("internal fun ExplorePrimaryShortcuts(", 1)[1].split(
            "private class ExploreShortcutEntry(", 1
        )[0]
        self.assertIn('ExploreShortcutEntry("releases"', shortcuts)
        self.assertNotIn("ExploreAnchor.Fresh", shortcuts)

    def test_other_explore_sections_and_destinations_are_kept(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        planner = (MAIN / "ui/ExploreLayout.kt").read_text()
        for symbol in (
            "ExploreRow.Shortcuts", "ExploreRow.MixTools", "ExploreRow.MoodRail",
            "ExploreAnchor.Moods",
        ):
            self.assertIn(symbol, planner)
            self.assertIn(symbol, app)
        for symbol in ('ExploreShortcutEntry("radio"', 'ExploreShortcutEntry("jam"'):
            self.assertIn(symbol, app)
        self.assertIn("YouTubeMusicExploreScreen(", app)
        self.assertIn("ExploreOverlays(", app)

    def test_shared_catalog_fetch_is_not_deleted_with_the_ui(self):
        owner = (MAIN / "viewmodel/DerbisViewModel.kt").read_text()
        self.assertIn("fun ensureExplore(strings: DerbisStrings)", owner)
        self.assertIn("private fun ensureFreshCurrentsLoaded(", owner)
        self.assertIn("zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID", owner)
        self.assertIn("ensureOfficialNewReleasesLoaded()", owner)


if __name__ == "__main__":
    unittest.main()
