import json
import plistlib
import subprocess
import tempfile
import unittest
from pathlib import Path

from scripts.configure_ios_ipa import configure_ios_plist


ROOT = Path(__file__).resolve().parents[2]


class Build150ContractTest(unittest.TestCase):
    def test_ipa_metadata_is_versioned_and_preserves_bundle_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "Info.plist"
            path.write_bytes(plistlib.dumps({"CFBundleIdentifier": "com.yasuda.derbis", "CFBundleExecutable": "App"}))
            configure_ios_plist(path, "1.5.0", "3000210")
            data = plistlib.loads(path.read_bytes())
            self.assertEqual("1.5.0", data["CFBundleShortVersionString"])
            self.assertEqual("3000210", data["CFBundleVersion"])
            self.assertEqual("com.yasuda.derbis", data["CFBundleIdentifier"])
            self.assertEqual("16.0", data["MinimumOSVersion"])
            self.assertEqual(["arm64", "metal"], data["UIRequiredDeviceCapabilities"])
            self.assertTrue(data["CADisableMinimumFrameDurationOnPhone"])
            self.assertEqual(["audio"], data["UIBackgroundModes"])

    def test_invalid_version_and_build_inputs_are_rejected(self):
        for version, build in [("1.5.0;exit", "3000210"), ("1.5.0", "0"), ("1.5.0", "not-a-build")]:
            with self.assertRaises(ValueError):
                configure_ios_plist(Path("unused.plist"), version, build)

    def test_ipa_build_uses_checked_in_source_not_an_external_opaque_bundle(self):
        workflow = (ROOT / ".github/workflows/build-ios-ipa.yml").read_text()
        builder = (ROOT / "website/build.js").read_text()
        self.assertIn("npm ci", workflow)
        self.assertIn("npm run build", workflow)
        self.assertNotIn("node build.js", workflow)
        self.assertNotIn("npm install || true", workflow)
        self.assertNotIn("dpaste", builder)
        self.assertNotIn("brotliDecompress", builder)
        self.assertIn('import { build } from "vite"', builder)

    def test_native_web_entry_and_version_are_current(self):
        package = json.loads((ROOT / "website/package.json").read_text())
        lock = json.loads((ROOT / "website/package-lock.json").read_text())
        self.assertEqual("1.5.0", package["version"])
        self.assertEqual(package["version"], lock["version"])
        self.assertEqual(package["version"], lock["packages"][""]["version"])
        entry = (ROOT / "website/index.html").read_text()
        self.assertIn('src="/src/main.jsx"', entry)
        self.assertNotIn('src="/assets/index-', entry)
        self.assertIn('base: "./"', (ROOT / "website/vite.config.js").read_text())

    def test_android_ci_is_an_artifact_only_debug_test_build(self):
        workflow = (ROOT / ".github/workflows/test-apk.yml").read_text()
        self.assertIn("agent/validation-1.5.0", workflow)
        self.assertIn('DERBIS_VERSION_NAME: "1.5.0"', workflow)
        self.assertIn("./gradlew :app:assembleDebug", workflow)
        self.assertIn("./gradlew :app:testDebugUnitTest", workflow)
        self.assertLess(workflow.index("name: Upload test APK"), workflow.index("name: Run all Android unit tests"))
        self.assertIn("com.derbis.app.debug", workflow)
        self.assertIn("contents: read", workflow)
        self.assertNotIn("action-gh-release", workflow)
        self.assertNotIn("assembleRelease", workflow)

    def test_unsigned_ipa_is_not_mislabeled_as_signed(self):
        workflow = (ROOT / ".github/workflows/build-ios-ipa.yml").read_text()
        self.assertIn("CODE_SIGNING_ALLOWED=NO", workflow)
        self.assertIn("unsigned.ipa", workflow)
        self.assertIn("-derivedDataPath", workflow)
        self.assertNotIn("git clone", workflow)
        self.assertNotIn("IOS_P12_PASSWORD", workflow)

    def test_unknown_web_tracks_do_not_receive_fabricated_lyrics(self):
        result = subprocess.run(
            ["node", "--input-type=module", "-e", "import { getTrackLyrics } from './website/src/data/lyrics.js'; console.log(JSON.stringify(getTrackLyrics({id:'unknown-track',title:'Unrelated title',artist:'Artist'})));"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        )
        self.assertEqual([], json.loads(result.stdout))
        modal = (ROOT / "website/src/components/LyricsModal.jsx").read_text()
        self.assertIn("getTrackLyrics(currentTrack)", modal)
        self.assertNotIn("Lyrics synced live with web player engine", modal)


if __name__ == "__main__":
    unittest.main()
