# Derbis changelog

All notable, user-visible changes are documented here. Each published Android and Windows package also has its own **What's Fixed**, **What's New**, and **Changed** notes on the [Derbis Releases page](https://github.com/jusnie/Derbis/releases).

## [Unreleased]

### What's New
- Liquid Glass lens now has real chromatic dispersion at the rim, a directional specular highlight and a thickness-shaped bezel instead of a single flat edge warp.
- The whole app carries the Liquid Glass material: every screen gets a page-level glass film, and the dock mini player now joins the same material as the navigation bar and player panels.
- Listening Wrapped can share the receipt card from any page, not just the finale: the story header now has a share button that renders the same card as a 1080px PNG.
- The Aura player deck (Beat Sync visual) shows the artwork as a circle inside a neon RGB ring that blooms outward on every detected beat.
- Mix Studio's Beat Sync is now readable: each handoff shows the detected OUT and IN BPM, the exact tempo correction with a ±10% rail, and whether that pair can actually lock.
- The Derbis Notch sits at the top of the app: a black capsule that opens into a card with the artwork, the track and the synced lyrics, with a Default or Spicy lyric style.
- `1eddaeb` Lyrics can go edge to edge from a fullscreen button; the progress bar only slides to the screen edge when fullscreen is on.
- The track story sheet shows the live card in a fitted stage with a light sweep while it renders, format tabs with their export ratio, theme tiles previewing each look, and one consistent button style for share, Instagram, save, copy link and Jam.

- The Derbis Notch has its own section in Settings with a plain on/off switch and the Default / Spicy lyric choice, instead of being buried between haptics and gestures. The island now hides while a sheet is open, its expanded card is clamped to the screen width so it no longer runs off the edge of a small phone, it collapses after opening the player and its waveform stops redrawing while playback is paused.
- Lossless output, ported from BitChord. Derbis now knows whether the connected route can carry a source's samples untouched — wired, USB, HDMI and dock always count; Bluetooth only on LDAC, LHDC, aptX Lossless or SSC UHQ — and the audio sink runs a 24-bit float path there instead of truncating everything to 16-bit. It stays 16-bit on the phone speaker, whenever skip-silence is on (that processor only takes PCM16) and on Samsung, whose vendor FLAC decoder is unsafe with float. The technical audio sheet reports the verdict for the current route.

### Changed
- The Aura player deck is built around the artwork now. A 96 px decode of the sleeve sits behind the whole deck at roughly a fifth of its opacity, under a four-stop scrim and blurred on Android 12+, and both that wash and the accent bloom behind the cover swell on the same beat clock as the neon ring, so the page breathes with the track instead of sitting on flat black. The seek control is one thick bar — gradient fill, a round head that grows while you drag — instead of the travelling waveform and its draggable stick, and the play/pause action is the only filled surface in the deck.
- Listening Wrapped grew three cards pulled from SimpMusic's reel: **the albums you lived in** (the three albums that took the most time, with play counts), **your biggest day** (the single local day with the most listening, compared against a typical active day and naming the track that carried it) and **your listening shape** (one archetype — the Regular, the Omnivore, the Devotee or the Deep Diver — chosen from four measured axes: consistency, variety, devotion and replay). A year with fewer than thirty listening days now says so instead of printing a fortnight as if it were twelve months.
- Dolby Atmos, ported from BitChord. Derbis asks the renderer itself — Media3's decoder list first, the platform codec list as a backstop — whether this phone has an E-AC-3 decoder, and keeps the answer for the session. An **Audio → Dolby Atmos** switch decides whether a source may hand over the E-AC-3 JOC mix: on by default where the decoder exists, greyed with the reason where it does not, because an Atmos rendition on a phone without one is not a worse stream, it is a track that ends in silence with no error to recover from. The resolver now skips Atmos candidates when they would not be wanted, the technical audio sheet reports the verdict for the route, and the player badge names **DOLBY ATMOS** instead of folding a 768 kbps JOC stream into Hi-Fi.
- Streaming quality has four steps: **Low, Medium, High, Lossless**. Lossless outranks every lossy stream on the source ladder and keeps the 320 kbps HQ upgrade on, so a FLAC or ALAC source is chosen whenever the provider or the local library has one. Auto is gone; an older saved Auto now reads as High, which is the branch it already took.
- The player badge reports what you are hearing instead of the container carrying it: a lossless source reads **LOSSLESS**, the same source at 24-bit or 88.2 kHz and up reads **HI-FI**, and a 320 kbps HQ upgrade reads **HI-FI** with its bitrate. AAC and Opus only keep their names below that, and the technical audio sheet still names the real codec (FLAC, ALAC, AAC) plus the file's bit depth.
- Liquid Glass has no tuning panel any more. The Theme Studio's glass section is gone — no Auto / Always on / Off, no blur, transparency, tint or refraction sliders — because the material is now the fixed Apple-matched recipe and the skin is the only switch that matters.
- The Liquid Glass skin renders real glass the way BitChord does it. The page is recorded into a backdrop layer and every bar, dock, mini player, sheet and player control samples it through the Kyant0/backdrop engine — vibrancy, 8 dp blur, rim highlight, soft shadow and, on Android 13+, lens refraction with chromatic aberration — under the same neutral 40 % surface film, so artwork and text warp under the surface instead of sitting behind tint. The old blurred RenderEffect frost is gone entirely: a surface is either that real glass or a flat tint with an edge-light rim, which is also the fallback on Android 11 and older. The skin's palette panels are a glass film too, so every card, row, sheet and chip in the app is translucent — not just the ones that go through the glass primitive. Content cards, rows and tiles keep the lightweight material on purpose: a card drawn inside the page cannot sample the page it is being drawn into, so it gets the same tint and the same light-catching rim instead of a second blur.
- Discord sign-in is one tap: the login sheet, its Normal/Token tabs and the embedded browser are gone, and **Connect Discord** now opens Discord's own authorization page in the Discord app, falling back to the browser. The account comes back through `derbis-discord://callback`, so no password or token is ever typed into Derbis.
- Discord Rich Presence reads **Listening in Derbis**: the activity keeps the clickable Play/Artist buttons, the live progress bar and the opt-in lyric line, and falls back to the Derbis logo when a track has no artwork.
- The "Like Derbis?" star prompt is monochrome now: one hairline ring and a flat star replace the gold sticker, the floating mini stars and the gold glow, and the call to action is white on black in dark theme (black on white in light theme).
- The notch's playback indicator is one straight hairline along the bottom edge of the card, square ended and gliding between player ticks instead of stepping.
- Beat Sync matches half- and double-time pairs, so 62 BPM locks to 128 BPM instead of being refused, and long overlaps now start on a bar line while short ones snap to the nearest beat.
- The handoff watcher checks the outgoing position every 20 ms instead of 50 ms, so the incoming deck starts on the beat the grid aligned to instead of up to a fifth of a beat late.
- The track story sheet is translated: share, preparing, save, saved, copy link, link copied and Jam labels follow the app language, and noisy toasts are replaced by in-place button states.
- Listening Wrapped copy sizes scale with the page height, pages scroll instead of clipping on short screens, and the progress bar reports page position to accessibility services.
- Liquid Glass cost scales with the device: low-RAM and battery-saver devices keep a coarser frost with no lens, while content rows and page surfaces sample a cheaper bucket than chrome.
- The full-page glass film no longer re-blurs the backdrop every frame, which removes the single most expensive glass surface in the app.

## [1.1.0] — 2026-10-27

### Removed (project)
- The Windows desktop client is retired: `desktop/`, its Gradle build, its release workflow and its docs are gone from the repository. Only the Android app ships; the web companion in `website/` is untouched. The client may return later.

### Changed (project)
- Derbis is now **GPL-3.0** instead of the custom end user agreement, so anyone can read, change, rebuild and share it, and contributions land under the same terms. New `CONTRIBUTING.md`, and `docs/RELICENSING.md` records why the bundled GPL components made the old licence untenable.

### Removed
- Videos are gone: Derbis is songs only. Search no longer requests or shows a Videos tab, artist pages drop their video shelf, Home drops the video rail and any "music video" feed section, editorial pages skip video shelves, and the player always resolves and presents audio with artwork instead of a video surface.

### What's Fixed
- `be712b3` Pausing or stopping a song keeps the player cover at its full presentation size.
- `4bd9a93` and `7ca4f79` keep Endless Listening populated for Quick Picks and allow its small foreground recommendation request on mobile data.
- `d64bbb7` Selecting a recently played track starts it from the beginning instead of resuming an old position.
- `84b2677` Crossfade ignores stale handoff completions so an older transition cannot replace the active deck.
- `9e9300c` Apple Music 13 lyrics use a dimmed artwork backdrop for readable white text.
- `a4faca1` Android 1.1.0 packages now retain their correct version metadata so the in-app updater can recognize them as newer.
- `3fb1c86` Spotify library loading reads the complete signed-in library response so eligible playlists are not mistaken for an empty library.
- `c541041` Album and discovery artwork no longer fall back to words, initials, genre stickers, or decorative symbols.

### What's New
- `663f5b2` Android Home adds create actions for local playlists and Derbis Jam — LAN.
- `8519df2`, `4d020b2`, and `50ab5ea` add the original Apple Music 13 player deck and its dedicated word-timed lyrics renderer.
- `a704592` and `c31a2d3` introduce Endless Listening for single-song sessions while keeping albums and playlists finite.
- `108e5cb` keeps Derbis Mix Studio's playlist-specific transition plans and custom timeline controls.
- `72ee0a9` adds the original Tidal Music player deck and local TIDAL Style palette.
- `a1426f9` adds the persisted Beat Sync control for verified Mix Studio alignment.
- `0f8dcd4` and `4f9640e` make Liquid Glass an app-wide material treatment across player, navigation, Search, Explore, Library, and Settings.
- `83d1d7e` and `e871551` add opt-in, rights-cleared Canvas artwork through the self-hosted Derbis Canvas Catalog; static covers remain the default.
- `5f26471` adds the AI Slop setting to block explicitly identified AI-generated music and video content.
- `ba79e80` makes Derbis Jam invite codes letters-only and at least six letters long.
- `7e0be74` lets Jam hosts choose LAN Jam for the same Wi-Fi network or Server Jam through the configured Derbis relay; guests paste the code and Derbis detects the room type automatically.
- `c9095f7` lets signed-in Spotify users list their Spotify library, inspect accessible tracks, and import them through Derbis matching.
- `e3d9cb3` adds Balanced Data Saver, which suppresses background network work and Canvas fetching, avoids mobile-data automatic downloads, and caps automatic video at 480p without blocking listener-started playback, search, or imports.

### Changed
- `deaaf7c` makes static player artwork the default presentation.
- `f6132c0` gives Apple Music 13 full-bleed artwork and Velvet a contained rounded-square cover.
- `fb07fc9` removes Explore Samples.
- `f5c6583` removes only the two-song Derbis Mix tool; Mix Studio and automatic mixing remain.
- `3439bf4` removes full-player tap, long-press, double-tap, double-tap-to-seek, and vertical-swipe gestures.
- `9953af2` completes the internal Levyra-to-Derbis rebrand.
- `4bea410` refreshes Home and onboarding with the transparent Derbis mark and a more resilient Discover for you flow.

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
