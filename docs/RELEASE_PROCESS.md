# Derbis release process

Derbis ships **one** artifact: the Android app. The Windows desktop client was removed from this
repository and may come back later; nothing in this process refers to it any more.

## Version history rules

1. Every shipped change gets an entry in [`CHANGELOG.md`](../CHANGELOG.md) under its version.
2. Tag Android releases `v<version>` (`v1.1.0`, `v1.5.0`, …). The tag name is what the in-app
   updater and the web build check against.
3. `versionCode` always increases, even when `versionName` stays the same for a test build. The
   current line is `3 000 2xx`: `3000211` was the 1.6.0 prebuild, `3000212` the 1.5.0 test build, so
   the official 1.5.0 ships as `3000213` and installs over either of them.
4. Release notes live in `docs/releases/v<version>.md` and use the same **What's New** /
   **What's Fixed** / **Changed** headings the changelog uses, so the release page and the in-app
   notes read the same. The workflow reads that file — a missing or empty one fails the release
   instead of publishing a placeholder body.

## Stable release sequence

1. Move the `[Unreleased]` section of `CHANGELOG.md` under the new version heading and date it.
2. Write `docs/releases/v<version>.md`: the page GitHub shows for the release.
3. Pick a `versionCode` above every published build (rule 3).
4. Dispatch **Build Derbis Release APK** from the Actions tab with:
   - `release_tag`: `v<version>`
   - `release_name`: `Derbis v<version>`
   - `release_version`: the display version, for example `1.5.0`
   - `release_version_code`: the code from step 3

   The workflow checks that the tag, the name and the version agree, builds
   `:app:assembleSidecarRelease`, verifies the APK's `aapt` badging against the requested metadata,
   writes `Derbis.apk.sha256`, and cuts the tag and release at the dispatched commit.
5. Installing over an older build works because `app/derbis-release.jks` signs every build and the
   code only ever goes up.

There is no local build step. `assembleSidecarRelease` in CI is the only thing that produces a
shipped APK; `assembleRelease` is not the release variant.

## Test releases

- **Derbis Prebuild APK** builds the same `com.derbis.app` package under a test version name,
  publishes it as a pre-release only when the `publish` input is true, and never moves the `latest`
  pointer.
- Test builds use the same keystore, so either line installs over the other as long as the version
  code goes up.
- The in-app updater is line-aware: a stable install only sees `v*` releases, a test install prefers
  `prebuild-*` and falls back to stable. A test release may be deleted once its line is superseded;
  the app then simply stops offering it.

## Installer naming and verification

- `Derbis.apk` — the release APK.
- `Derbis.apk.sha256` — checksum, always uploaded next to the APK.

Verify a download with `sha256sum -c Derbis.apk.sha256` before installing.

## Windows

Not built from this repository. The previous `desktop-v*` releases stay published but receive no
updates. If the client returns, it comes back as a separate module with its own workflow and this
section is rewritten.
