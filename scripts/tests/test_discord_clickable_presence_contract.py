from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/yasuda/derbis"


class DiscordClickablePresenceContractTest(unittest.TestCase):
    def test_owner_application_id_matches_the_existing_build_default(self):
        build = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn('.ifBlank { "1563840846372012102" }', build)
        self.assertIn('if (isFdroidBuild) "" else envOrProperty("DISCORD_APPLICATION_ID"', build)

    def test_native_sdk_gets_two_clickable_buttons_without_gateway_login(self):
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        self.assertIn('add_presence_button(&activity, "Listen on Derbis", listen_url)', bridge)
        self.assertIn('add_presence_button(&activity, "View Artist", profile_url)', bridge)
        self.assertEqual(2, bridge.count('add_presence_button(&activity,'))
        self.assertIn('Discord_Activity_AddButton(activity, &button)', bridge)
        self.assertFalse((MAIN / "feature/discord/DiscordRpcClient.kt").exists())

    def test_song_artist_and_artwork_fields_have_clickable_urls(self):
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        for api in (
            "Discord_Activity_SetDetailsUrl", "Discord_Activity_SetStateUrl",
            "Discord_ActivityAssets_SetLargeUrl", "Discord_ActivityAssets_SetSmallUrl",
            "Discord_ActivityAssets_SetSmallImage",
        ):
            self.assertIn(api, bridge)
        self.assertIn("presence_link_allowed", bridge)

    def test_sdk_timestamps_have_explicit_millisecond_start_and_end(self):
        facade = (MAIN / "feature/discord/DerbisDiscordRichPresence.kt").read_text()
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        self.assertIn("payload.startedAtEpochMs, payload.endsAtEpochMs", facade)
        self.assertNotIn("startedAtEpochSeconds", facade)
        self.assertIn("Discord_ActivityTimestamps_SetEnd", bridge)
        self.assertIn("ends_at_epoch_ms > started_at_epoch_ms", bridge)

    def test_jni_strings_and_clickable_urls_have_boundaries(self):
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        self.assertIn('if (value == nullptr) return "";', bridge)
        self.assertIn("value.size() > 256", bridge)
        self.assertIn('https://derbis-web.vercel.app/t/', bridge)
        self.assertIn('https://music.youtube.com/channel/', bridge)


if __name__ == "__main__":
    unittest.main()
