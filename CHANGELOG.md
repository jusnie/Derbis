# Derbis changelog

All notable, user-visible changes are documented here. Each published Android and Windows package also has its own **New**, **Fixed**, and **Known limitations** notes on the [Derbis Releases page](https://github.com/jusnie/Derbis/releases).

## Unreleased — phased library and Home upgrade

### New
- Android Home now offers **Create** actions for a local playlist and **Derbis Jam — LAN**.
- Player Deck replaces the **Bloom** choice with **Apple Music 13**: an original Derbis layout with uncropped artwork, an artwork-colour field, active lyric preview, and available stream details. Existing Bloom selections migrate to Apple Music 13.
- Lyrics options and Lyrics Studio now include a persisted **Apple Music 13 Lyrics** renderer. When enabled, its centered, blurred-artwork, word-timed presentation exclusively replaces the Derbis lyric renderer until it is switched off.
- Now Playing places the genuine crossfade handoff card at the bottom of the player, showing the outgoing and incoming track. Crossfade handoff now re-aligns the replacement deck before it becomes audible to reduce transition jumps.
- **Endless listening** replaces the legacy finite, shuffled autoplay path. Starting a single song now begins a true listening session instead of turning nearby results into a queue; a small hidden rolling buffer follows the song currently playing and retries after a temporary empty response. Albums and playlists remain their exact chosen finite lists. Turning Endless listening off removes its recommendation buffer without interrupting the current track.
- Android's Add to Playlist picker now filters local playlist destinations by name before adding a track.
- Windows' expanded library sidebar now lets listeners search and sort their local playlists, open them directly, and collapse the library browser when they want a simpler navigation rail.

### Known limitations
- This is the first safe phase of the SimpMusic-inspired rollout. Cached-playback collection refinements, additional Home/player polish, and compliant authenticated YouTube social features remain separate follow-up work; YouTube actions will not be enabled without compliant authenticated access.

## [1.0.3] — 2026-10-25

### Android

#### New
- **Mix Studio** adds saved, playlist-specific transition plans, Auto Mix, editable handoff modes, durations, and a mixed-playback toggle.
- **Beat Align (beta)** performs local BPM and beat-grid candidate analysis. It aligns only where both tracks have confident local grids and a safe tempo correction.
- Imported playlists are visibly labelled **Imported from Spotify**.
- Remote Jam protocol groundwork supports a user-selected, authorized Metroserver endpoint. Derbis does not silently use public relays.

#### Fixed
- Corrected Derbis Android theme resource references so release APK packaging succeeds.
- Corrected queue row handling during release Kotlin compilation.
- Removed decorative rotating artwork and cleared stale active lyric lines across track changes.
- Preserved playlist-entry identities through playback resolution so Mix Studio transitions remain attached to the intended tracks.
- Retained crossfade protections against doubled playback voices.

#### Known limitations
- Discord Rich Presence requires an authorized Discord Social SDK build configuration.
- Without an owned remote relay, Jam remains **Derbis Jam — LAN**.
- Beat Align is a beta feature and remains off whenever local analysis is not confident.

### Windows

#### New
- First native Windows release with MSI and EXE installers.
- Derbis icon and branding, Start-menu shortcut, per-user installation, and `derbis://` link handling.
- Local library, search, queue, playback, lyrics, downloads, media-key support, and update groundwork.

#### Fixed
- Corrected the shared desktop source boundary for radio-library text.
- Corrected the packaged Derbis icon resource and Settings typography import.
- Updated desktop version metadata and the app-data folder name to Derbis.

#### Known limitations
- The Windows installers are not code-signed yet. SmartScreen may ask for confirmation; only install files downloaded from the official release page.

## [3.0.2] — legacy Android release

### Added
- Derbis logo refresh, DJ crossfade controls, optional animated-artwork providers, Apple Music-style player presentation, Lite Mode, high-refresh support, endless queue, Wrapped/Receipt-style cards, and Liquid Glass design controls.

> This is a legacy tag retained in the public history. Current public release numbering continues with `1.0.x`.

## [1.0.1] — 2026-10-24

### Fixed
- Removed Chucker HTTP monitoring and reduced background work to address lag and ANR reports.
- Enabled R8 minification and resource shrinking.
- Updated Derbis branding, developer attribution, GitHub sources, AMOLED theme, and time-of-day greeting.

## [1.0.0] — initial release

### Added
- Initial Derbis Android release with application ID `com.derbis.app`, AMOLED Black and White design, custom logo, and dynamic greeting.
