import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PLAYER = ROOT / "app/src/main/java/com/yasuda/derbis/ui/player"


def read(name):
    return (PLAYER / name).read_text(encoding="utf-8")


class BeatSyncDeckContractTest(unittest.TestCase):
    def test_only_beat_sync_maps_to_the_aura_deck(self):
        mapping = read("PlayerDeck.kt").split(
            "internal fun PlayerVisualMode.deckLayout()", 1
        )[1].split("internal fun PlayerVisualMode.usesAuraPlayPause", 1)[0]
        self.assertIn("PlayerVisualMode.CanvasImmersive -> PlayerDeckLayout.Aura", mapping)
        self.assertEqual(1, mapping.count("PlayerDeckLayout.Aura"))

    def test_live_circle_and_progress_have_one_beat_sync_owner(self):
        aura = read("PlayerDeckAura.kt")
        self.assertEqual(1, aura.count("rememberBeatSyncVisualState("))
        self.assertIn("AuraBeatSyncArtwork(", aura)
        self.assertIn("BeatSyncProgressBar(", aura)
        self.assertEqual(2, aura.count("visuals = beatSyncVisuals"))
        for name in (
            "AppleMusicPlayer.kt", "PlayerDeckPulse.kt", "PlayerDeck.kt",
        ):
            self.assertNotIn("BeatSyncArtworkFrame(", read(name))
            self.assertNotIn("BeatSyncProgressBar(", read(name))

    def test_reactivity_is_scoped_to_the_aura_branch_and_resource_policy(self):
        now = read("DerbisNowPlaying.kt")
        aura = now.split("else if (deckLayout == PlayerDeckLayout.Aura", 1)[1].split(
            "else if (deckLayout == PlayerDeckLayout.Halo", 1
        )[0]
        guard = "visualsEnabled = animated && !state.interfaceSettings.liteMode && !motionSuspended"
        self.assertIn(guard, aura)
        self.assertEqual(1, now.count(guard))

    def test_real_player_uses_pcm_not_the_demo_preview(self):
        live = read("BeatSyncVisuals.kt").split(
            "internal fun rememberBeatSyncVisualState(", 1
        )[1].split("internal fun rememberBeatSyncPreviewState()", 1)[0]
        self.assertIn("VisualizerAudioProcessor.waveformState.value", live)
        self.assertIn("sampleAge < BeatSyncFreshSeconds", live)
        self.assertIn("state.reset()", live)
        self.assertNotIn("sin(", live)
        self.assertNotIn("rememberBeatSyncPreviewState(", read("PlayerDeckAura.kt"))

    def test_visual_loop_stops_for_hidden_paused_or_reduced_motion_states(self):
        visuals = read("BeatSyncVisuals.kt")
        self.assertIn("lifecycle.isAtLeast(Lifecycle.State.STARTED)", visuals)
        self.assertIn("enabled && decorative", visuals)
        self.assertIn("if (!active || !isPlaying)", visuals)
        self.assertIn("BeatSyncFrameNanos = 33_333_333L", visuals)
        self.assertIn("delay(80L)", visuals)

    def test_only_the_beat_sync_preview_gets_the_circle(self):
        sheet = read("PlayerDeckSheet.kt")
        routing = sheet.split("private fun PlayerDeckPreview(", 1)[1].split(
            "private fun DeckArtwork(", 1
        )[0]
        self.assertIn("PlayerVisualMode.CanvasImmersive -> BeatSyncPreview(", routing)
        self.assertEqual(1, sheet.count("BeatSyncArtworkFrame("))
        preview = sheet.split("private fun BeatSyncPreview(", 1)[1].split(
            "private fun TidalMusicPreview(", 1
        )[0]
        self.assertIn("rememberBeatSyncPreviewState()", preview)
        self.assertNotIn("drawRoundRect(", preview)

    def test_no_stick_or_thumb_is_drawn_in_beat_sync(self):
        visuals = read("BeatSyncVisuals.kt")
        self.assertNotIn("drawRoundRect(", visuals)
        self.assertNotIn("stickHeight", visuals)
        self.assertNotIn("thumb", visuals.lower())
        self.assertNotIn("private fun AuraLine(", read("PlayerDeckAura.kt"))

    def test_seeking_keeps_drag_tap_accessibility_and_touch_target(self):
        visuals = read("BeatSyncVisuals.kt")
        for token in (
            "detectTapGestures", "detectHorizontalDragGestures", "setProgress",
            "ProgressBarRangeInfo", "requiredHeight(48.dp)", "LayoutDirection.Rtl",
            "rememberUpdatedState(onCommit)", "onDragCancel",
        ):
            self.assertIn(token, visuals)

    def test_circle_keeps_the_existing_verified_canvas_layer_and_cover(self):
        aura = read("PlayerDeckAura.kt").split("private fun AuraBeatSyncArtwork(", 1)[1].split(
            "private fun AuraGrabber(", 1
        )[0]
        self.assertIn("MotionArtworkLayer(", aura)
        self.assertIn("enabled = motionEnabled", aura)
        self.assertIn("SeamlessArtworkImage(", aura)
        self.assertIn(".clip(CircleShape)", read("BeatSyncVisuals.kt"))


if __name__ == "__main__":
    unittest.main()
