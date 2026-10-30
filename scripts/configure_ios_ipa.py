import argparse
import plistlib
import re
from pathlib import Path


VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
BUILD_NUMBER = re.compile(r"[1-9][0-9]*")


def configure_ios_plist(path: Path, version: str, build_number: str) -> None:
    if not VERSION.fullmatch(version):
        raise ValueError("iOS version must be major.minor.patch")
    if not BUILD_NUMBER.fullmatch(build_number):
        raise ValueError("iOS build number must be a positive integer")
    with path.open("rb") as stream:
        info = plistlib.load(stream)
    info.update({
        "CFBundleDisplayName": "Derbis",
        "CFBundleShortVersionString": version,
        "CFBundleVersion": build_number,
        "UIBackgroundModes": ["audio"],
        "MinimumOSVersion": "16.0",
        "ITSAppUsesNonExemptEncryption": False,
        "CADisableMinimumFrameDurationOnPhone": True,
        "UIRequiresFullScreen": True,
        "UIStatusBarStyle": "UIStatusBarStyleLightContent",
        "UIViewControllerBasedStatusBarAppearance": False,
        "UIRequiredDeviceCapabilities": ["arm64", "metal"],
    })
    with path.open("wb") as stream:
        plistlib.dump(info, stream, sort_keys=False)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--plist", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--build-number", required=True)
    args = parser.parse_args()
    configure_ios_plist(args.plist, args.version, args.build_number)


if __name__ == "__main__":
    main()
