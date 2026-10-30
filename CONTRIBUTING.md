# Contributing to Derbis

Derbis is GPL-3.0-only. By opening a pull request you agree that your contribution is licensed
under the same terms and that you have the right to submit it.

## Ground rules

- One change per pull request. Keep diffs readable.
- Match the surrounding code: Kotlin, Jetpack Compose, no new dependencies without a reason.
- No copyrighted audio, artwork, API secrets or signing keys in a commit. `app/derbis-release.jks`
  and anything under `keystore/` must never be pushed.
- Run `./gradlew assembleDebug` locally before opening the PR. CI builds the release APK, the IPA and
  the Windows package.

## Third-party code

If you copy or adapt code, keep its licence header and add the component to `THIRD_PARTY_NOTICES.md`
and to the matching `REUSE.toml` annotation. Any GPL-3.0-only component is fine; anything that is
not GPL compatible is not.

## Reporting bugs

Include the app version, the device, the Android version and the steps to reproduce. Logs help:
`adb logcat -s Derbis`.
