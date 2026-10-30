import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/yasuda/derbis"


def source(path):
    return (SOURCE / path).read_text(encoding="utf-8")


def compact(text):
    return re.sub(r"\s+", "", text)


class MotionArtworkOptInContractTest(unittest.TestCase):
    def test_public_motion_policy_has_no_spotify_account_gate(self):
        policy = source("feature/motion/MotionArtworkNetworkPolicy.kt")
        self.assertNotIn("DerbisSpotifyConnection", policy)
        self.assertIn("funmotionArtworkAllowed():Boolean=enabled", compact(policy))
        self.assertIn("!network.dataSaverActive", policy)
        self.assertIn("network.localAllowed", policy)

    def test_player_uses_master_setting_without_a_spotify_gate(self):
        player = source("ui/player/DerbisNowPlaying.kt")
        self.assertIn("val motionArtworkAllowed = state.motionArtworkEnabled", player)
        self.assertNotIn("DerbisSpotifyConnection", player)
        self.assertIn("!state.interfaceSettings.liteMode", player)
        self.assertIn("!state.isVideoMode", player)

    def test_integration_switch_is_not_locked_by_account_status(self):
        panel = source("ui/DerbisApp.kt").split(
            "private fun SpotifyConnectionSettingsPanel(", 1
        )[1].split("private fun IntegrationSettingsPanel(", 1)[0]
        self.assertNotIn("LOGIN REQUIRED", panel)
        self.assertNotIn("checked = isConnected && motionArtworkEnabled", panel)
        self.assertIn("subtitle = strings.motionArtworkEnabledSubtitle", panel)
        self.assertIn("checked = motionArtworkEnabled", panel)
        self.assertIn("DerbisSpotifyConnection.begin(context)", panel)

    def test_master_toggle_delegates_to_interface_owner(self):
        setter = source("viewmodel/DerbisViewModel.kt").split(
            "fun setMotionArtworkEnabled(value: Boolean)", 1
        )[1].split("fun setDynamicColor(", 1)[0]
        self.assertIn("setInterfaceSettings(", setter)
        self.assertIn("interfaceSettings.copy(motionArtworkEnabled = value)", setter)
        self.assertNotIn("preferences.setMotionArtworkEnabled", setter)

    def test_interface_owner_updates_both_live_flags(self):
        setter = source("viewmodel/DerbisViewModel.kt").split(
            "fun setInterfaceSettings(value: DerbisInterfaceSettings)", 1
        )[1].split("localLibrarySortFlow.value", 1)[0]
        self.assertIn("interfaceSettings = normalized", setter)
        self.assertIn("motionArtworkEnabled = normalized.motionArtworkEnabled", setter)
        self.assertIn("MotionArtworkNetworkPolicy.updateEnabled(", setter)

    def test_enabling_and_disabling_refreshes_motion_ownership(self):
        helper = source("viewmodel/DerbisViewModel.kt").split(
            "internal fun shouldRefreshMotionArtworkOwnership(", 1
        )[1].split("internal fun resumeStartPositionMs(", 1)[0]
        self.assertIn(
            "previous.motionArtworkEnabled != next.motionArtworkEnabled", helper
        )
        self.assertIn("previous.motionArtworkWifiOnly != next.motionArtworkWifiOnly", helper)
        self.assertIn("previous.dataSaverEnabled != next.dataSaverEnabled", helper)

    def test_preference_reads_share_the_same_migration_rule(self):
        preferences = compact(source("data/DerbisPreferences.kt"))
        read = (
            "motionArtworkEnabled=resolveMotionArtworkEnabled("
            "preferences[KEY_MOTION_ARTWORK],preferences[KEY_UI_MOTION_ARTWORK_ENABLED])"
        )
        self.assertEqual(2, preferences.count(read))
        self.assertIn("Boolean=masterEnabled?:interfaceEnabled?:false", preferences)

    def test_both_preference_write_paths_keep_the_keys_together(self):
        preferences = source("data/DerbisPreferences.kt")
        master = preferences.split("suspend fun setMotionArtworkEnabled(", 1)[1].split(
            "fun themePreset(", 1
        )[0]
        interface = preferences.split(
            "fun setInterfaceSettings(value: DerbisInterfaceSettings)", 1
        )[1].split("fun ambientSettings(", 1)[0]
        for block in (master, interface):
            self.assertIn("it[KEY_MOTION_ARTWORK] =", block)
            self.assertIn("it[KEY_UI_MOTION_ARTWORK_ENABLED] =", block)

    def test_restore_normalizes_interface_to_master_opt_in(self):
        restore = source("data/DerbisPreferences.kt").split(
            "suspend fun restoreSnapshot(", 1
        )[1].split("val normalizedAmbient", 1)[0]
        self.assertIn("motionArtworkEnabled = snapshot.motionArtworkEnabled", restore)

    def test_auto_order_does_not_discard_registered_public_providers(self):
        helper = source("feature/motion/MotionArtworkEngine.kt").split(
            "internal fun motionArtworkProviderOrder(", 1
        )[1].split("internal data class MotionArtworkRankedCandidate", 1)[0]
        for provider in (
            "AppleMusicAnimatedArtworkProvider.PROVIDER_ID",
            "SpotifyCanvasProvider.PROVIDER_ID",
            "DerbisCanvasCatalogProvider.PROVIDER_ID",
        ):
            self.assertIn(provider, helper)
        self.assertIn("distinct()", helper)

    def test_canvas_search_is_wired_and_cancellable(self):
        provider = source("feature/motion/SpotifyCanvasProvider.kt")
        self.assertIn("catalogSearch = catalog::searchSpotifyMetadata", provider)
        self.assertIn("search.search(query, limit)", provider)
        self.assertNotIn(".cachedSearch(", provider)
        self.assertIn("awaitMotionArtworkResponse(client.newCall(request))", provider)
        self.assertIn("identity = resolvedIdentity", provider)
        self.assertIn("catch (error: CancellationException)", provider)


if __name__ == "__main__":
    unittest.main()
