from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
STUDIO = ROOT / "app/src/main/java/com/yasuda/derbis/ui/lyrics/studio/LyricsStudioScreen.kt"


class LyricsStudioSpacingContractTest(unittest.TestCase):
    def setUp(self):
        self.source = STUDIO.read_text()
        self.preview = self.source.split("private fun LyricsStudioLivePreview(", 1)[1].split(
            "private fun AppleMusic13StudioLivePreview(", 1
        )[0]
        self.header = self.source.split("private fun StudioPreviewHeader(", 1)[1].split(
            "private fun StudioPreviewLyricsLine(", 1
        )[0]

    def test_preview_title_shares_width_with_the_compare_control(self):
        call = self.preview.split("StudioPreviewHeader(", 1)[1].split("\n            )", 1)[0]
        self.assertIn("modifier = Modifier.weight(1f)", call)
        self.assertIn("horizontalArrangement = Arrangement.spacedBy(8.dp)", self.preview)

    def test_header_accepts_the_parent_width_allocation(self):
        self.assertIn("modifier: Modifier = Modifier", self.header)
        self.assertIn("modifier = modifier", self.header)
        self.assertNotIn("modifier = Modifier\n            .fillMaxWidth()", self.header)

    def test_compare_text_cannot_wrap_into_a_tall_offscreen_column(self):
        text = self.preview.split("text = if (compareMode)", 1)[1].split("\n                )", 1)[0]
        self.assertIn("maxLines = 1", text)
        self.assertIn("overflow = TextOverflow.Ellipsis", text)
        self.assertIn(".widthIn(max = 164.dp)", self.preview)

    def test_preview_label_is_a_single_line_at_narrow_widths(self):
        text = self.header.split("text = strings.lyricsStudioPreview", 1)[1].split("\n        )", 1)[0]
        self.assertIn("maxLines = 1", text)
        self.assertIn("overflow = TextOverflow.Ellipsis", text)

    def test_compact_header_keeps_an_accessible_compare_touch_target(self):
        self.assertIn(".heightIn(min = DerbisPlayerDesign.MinimumTouchTarget)", self.preview)
        self.assertIn(".clickable { compareMode = !compareMode }", self.preview)

    def test_spacing_fix_does_not_shrink_or_stretch_the_actual_preview(self):
        self.assertIn("Spacer(modifier = Modifier.height(8.dp))", self.preview)
        card = self.source.split("private fun AppleMusic13StudioLivePreview(", 1)[1].split(
            "private fun AppleMusic13StudioPreviewLine(", 1
        )[0]
        self.assertIn(".height(230.dp)", card)
        self.assertNotIn(".fillMaxHeight(", self.preview)
        self.assertNotIn("Arrangement.SpaceEvenly", self.preview)
        self.assertNotIn("Arrangement.SpaceAround", self.preview)


if __name__ == "__main__":
    unittest.main()
