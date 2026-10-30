# Derbis release process

Derbis ships **one** artifact: the Android app. The Windows desktop client was removed from this
repository and may come back later; nothing in this process refers to it any more.

## Version history rules

1. Every shipped change gets an entry in [`CHANGELOG.md`](../CHANGELOG.md) under its version.
2. Tag Android releases `v<version>` (`v1.1.0`, `v1.2.0`, …). The tag name is what the in-app
   updater and the web build check against.
3. `versionCode` always increases, even when `versionName` stays the same for a test build.
4. Release notes use the same **What's Fixed** / **What's New** / **Changed** headings the changelog
   uses, so the release page and the in-app notes read the same.

## Stable release sequence

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Move the `[Unreleased]` section of `CHANGELOG.md` under the new version heading.
3. Run `./gradlew :app:assembleRelease` locally and install the APK once.
4. Dispatch **Build Derbis APK** from the Actions tab with:
   - `version`: the display version, for example `1.2.0`
   - `release_name`: `Derbis v<version>`
5. Publish the draft release. The workflow uploads `DERBIS-Android-<version>-universal.apk` and its
   `.sha256` file.

## Test releases

Test builds publish under the same workflow with a `-test` suffix and are marked as pre-releases.
They never move the `latest` pointer.

## Installer naming and verification

- `DERBIS-Android-<version>-universal.apk` — the universal APK.
- `DERBIS-Android-<version>-universal.apk.sha256` — checksum, always uploaded next to the APK.

Verify a download with `sha256sum -c DERBIS-Android-<version>-universal.apk.sha256` before installing.

## Windows

Not built from this repository. The previous `desktop-v*` releases stay published but receive no
updates. If the client returns, it comes back as a separate module with its own workflow and this
section is rewritten.
