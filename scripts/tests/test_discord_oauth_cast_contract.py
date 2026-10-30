from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/yasuda/derbis"
DISCORD = MAIN / "feature/discord"


class DiscordOauthCastContractTest(unittest.TestCase):
    def test_embedded_login_and_raw_gateway_are_removed(self):
        self.assertFalse((DISCORD / "DiscordLoginSheet.kt").exists())
        self.assertFalse((DISCORD / "DiscordRpcClient.kt").exists())
        facade = (DISCORD / "DerbisDiscordRichPresence.kt").read_text()
        self.assertNotIn("loginWithToken", facade)
        self.assertNotIn("DiscordRpcClient", facade)
        self.assertNotIn("WebView", facade)

    def test_connect_button_starts_official_authorization_not_a_login_sheet(self):
        app = (MAIN / "ui/DerbisApp.kt").read_text()
        panel = app.split("private fun DiscordRichPresenceSettingsPanel()", 1)[1].split(
            "private fun SpotifyConnectionSettingsPanel(", 1
        )[0]
        self.assertIn("discord.beginSignIn(context)", panel)
        self.assertNotIn("showLoginSheet", panel)
        self.assertNotIn("testConnection", panel)
        self.assertNotIn("Token Login", panel)

    def test_sdk_and_application_id_are_required(self):
        facade = (DISCORD / "DerbisDiscordRichPresence.kt").read_text()
        self.assertIn("sdkAvailable && applicationId > 0L", facade)
        self.assertIn("discordAuthorizationAvailable(BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE, applicationId)", facade)
        self.assertNotIn("fun isAvailable(): Boolean = true", facade)

    def test_engine_activity_is_attached_before_native_client_initialization(self):
        facade = (DISCORD / "DerbisDiscordRichPresence.kt").read_text()
        self.assertIn('getMethod("setEngineActivity", Activity::class.java)', facade)
        initialize = facade.split("fun initialize(context: Context)", 1)[1].split("fun beginSignIn(", 1)[0]
        self.assertLess(initialize.index("if (!activityAttached)"), initialize.index("nativeInitialize(applicationId)"))

    def test_native_authorization_keeps_pkce_and_does_not_fail_during_app_handoff(self):
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        for token in (
            "Discord_Client_CreateAuthorizationCodeVerifier", "Discord_AuthorizationArgs_SetCodeChallenge",
            "Discord_Client_Authorize", "Discord_Client_GetToken",
        ):
            self.assertIn(token, bridge)
        self.assertIn("g_connection_state != kStateDisconnected && g_connection_state != kStateSigningIn", bridge)

    def test_only_sdk_ready_is_reported_as_connected(self):
        bridge = (ROOT / "app/src/main/cpp/DiscordRichPresenceBridge.cpp").read_text()
        ready = bridge.split("case Discord_Client_Status_Ready:", 1)[1].split("case Discord_Client_Status_Connected:", 1)[0]
        connecting = bridge.split("case Discord_Client_Status_Connected:", 1)[1].split("case Discord_Client_Status_Disconnected:", 1)[0]
        self.assertIn("g_connection_state = kStateConnected", ready)
        self.assertNotIn("g_connection_state = kStateConnected", connecting)
        self.assertIn("g_connection_state != kStateSigningIn", connecting)

    def test_discord_package_visibility_and_sdk_callback_are_registered(self):
        root = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        self.assertIn("com.discord", [p.get(ns + "name") for p in root.findall("queries/package")])
        activity = next(a for a in root.findall("application/activity") if a.get(ns + "name") == "com.discord.socialsdk.AuthenticationActivity")
        self.assertEqual("${discordSocialSdkAvailable}", activity.get(ns + "enabled"))
        self.assertTrue(any(d.get(ns + "scheme") == "discord-${discordApplicationId}" for d in activity.findall("intent-filter/data")))

    def test_raw_token_sessions_are_rejected_and_only_oauth_is_saved(self):
        store = (DISCORD / "DerbisDiscordAccountStore.kt").read_text()
        self.assertIn('json.optString("user_token").isNotBlank()', store)
        self.assertNotIn('put("user_token"', store)
        self.assertNotIn("val userToken:", store)
        self.assertIn("accessToken.isNotBlank() && refreshToken.isNotBlank()", store)
        self.assertIn('"AES/GCM/NoPadding"', store)
        self.assertIn("clear(context)", store)

    def test_saved_oauth_does_not_mark_connected_before_the_sdk_is_ready(self):
        facade = (DISCORD / "DerbisDiscordRichPresence.kt").read_text()
        initialize = facade.split("fun initialize(context: Context)", 1)[1].split("fun beginSignIn(", 1)[0]
        self.assertIn("nativeRestoreSession(session.refreshToken)", initialize)
        self.assertNotIn("DerbisDiscordConnectionStatus.Connected", initialize)
        self.assertIn("while (isActive)", facade)

    def test_cast_icon_has_no_glass_circle_but_keeps_a_touch_target(self):
        cast = (ROOT / "app/src/upstream/java/com/yasuda/derbis/feature/cast/CastRouteButton.kt").read_text()
        for token in ("playerGlass", "CircleShape", ".background(", ".border("):
            self.assertNotIn(token, cast)
        self.assertIn("minimumInteractiveComponentSize()", cast)
        self.assertIn("Media3RouteButton(", cast)
        self.assertIn("LocalContentColor provides", cast)


if __name__ == "__main__":
    unittest.main()
