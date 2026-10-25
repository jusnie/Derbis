# Derbis release process

Derbis publishes separate Android and Windows releases so users can download the right installer and read version-specific notes.

## Version history rules

1. Add user-visible **New**, **Fixed**, and **Known limitations** entries to [`CHANGELOG.md`](../CHANGELOG.md).
2. Keep the Android release tag as `v<version>` (for example, `v1.0.4`).
3. Keep the Windows release tag as `desktop-v<version>` (for example, `desktop-v1.0.4`).
4. Use the same display version on Android and Windows, while Android's internal `versionCode` always increases.
5. Do not overwrite or delete an older release just to ship a new version. Superseded packages remain visible in GitHub Releases for rollback and change history.

## Stable release sequence

1. Update version metadata, changelog, release notes, and test coverage.
2. Dispatch **Build Derbis Windows Test** with:
   - `release_tag`: `desktop-v<version>`
   - `release_name`: `Derbis for Windows v<version>`
   - `release_version`: `<version>`
   - `prerelease`: `false`
3. Verify the MSI and EXE, and confirm their SHA-256 files are published.
4. Dispatch **Build Derbis Release APK** with:
   - `release_tag`: `v<version>`
   - `release_name`: `Derbis v<version>`
   - `prerelease`: `false`
5. Verify the APK, its release notes, and the GitHub Releases list.

Publishing the Android release after the Windows release keeps legacy Android builds that still use GitHub's repository-wide “latest release” endpoint pointed to the APK. Newer Derbis builds select Android releases from the release list directly, so they are not affected by Windows release ordering.

## Test releases

Use prerelease tags such as `v1.0.4-test.1` and `desktop-v1.0.4-test.1`, with `prerelease: true`. Test builds stay visible in the history but are ignored by normal in-app update checks.

## Installer naming and verification

The Windows workflow publishes versioned files such as:

- `DERBIS-Windows-1.0.4-x64.msi`
- `DERBIS-Windows-1.0.4-x64.exe`
- matching `.sha256` checksum files

Users can verify a downloaded file in PowerShell:

```powershell
Get-FileHash .\DERBIS-Windows-1.0.4-x64.msi -Algorithm SHA256
```

Compare that hash with the text in the matching `.sha256` asset from the same release.
