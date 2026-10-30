# Discord Rich Presence Enhancements (OuterTune Pattern)

## Overview & Architecture

Derbis now integrates all 5 Discord Rich Presence features inspired by the [notK50BML/OuterTune](https://github.com/notK50BML/OuterTune) pattern:

1. **Improved Normal Login Authentication & Token Login**
2. **Built-in Connection Tester**
3. **Clickable Song and Artist Links**
4. **Dual Artwork Assets: Song Cover & Artist Icon**
5. **Working Scrubbing Progress Bar**

```
                       ┌───────────────────────────────┐
                       │ Derbis Playback Service       │
                       │ (Active Track + Playback Deck)│
                       └───────────────┬───────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │ derbisDiscordPresenceSnapshot │
                       │ • Track ID, Title, Artist     │
                       │ • Position & Duration         │
                       │ • Universal Song & Artist URL │
                       │ • High-Res Album Cover & Icon │
                       └───────────────┬───────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │ DiscordRpcClient (Pure Kotlin)│
                       │ Gateway v10 WebSocket & REST  │
                       └───────────────┬───────────────┘
                                       │
         ┌─────────────────────────────┴─────────────────────────────┐
         ▼                                                           ▼
┌──────────────────────────────┐                            ┌──────────────────────────────┐
│ Real-Time Discord Gateway    │                            │ Connection Diagnostics       │
│ • Live Progress Timeline     │                            │ • DNS & Network Check        │
│ • "Play on Derbis" Button    │                            │ • REST API User /@me Ping    │
│ • "Artist Profile" Button    │                            │ • Latency Measurement (ms)   │
│ • Song Cover + Artist Icon   │                            │ • User Profile & Avatar      │
└──────────────────────────────┘                            └──────────────────────────────┘
```

---

## The 5 Upgraded Features

### 1. Improved Normal Login & Direct Token Login
* **Normal Login**: An embedded web session client that automatically intercepts the user token from Discord's web client (`localStorage` and authorization headers), requiring no manual copy-pasting.
* **Token Login**: Direct user token input with clipboard paste button, masked visibility toggle, and instant connection testing.
* **Security**: Token and OAuth sessions are stored with AES-256 GCM authenticated encryption backed by the hardware **Android Keystore** (`DerbisDiscordAccountStore.kt`).

### 2. Built-in Connection Tester
* Integrated directly into Derbis Settings > Discord.
* **4-Phase Diagnostic Check**:
  1. **Network Reachability**: Verifies device internet and DNS resolution to Discord endpoints.
  2. **REST API Check**: Calls `https://discord.com/api/v10/users/@me` with latency measurement (e.g. `42 ms`).
  3. **Profile Verification**: Retrieves user tag (`@username`), global display name, user ID, and avatar hash.
  4. **Gateway Status**: Verifies WebSocket handshake and Presence update ACK.
* Displays a live diagnostic status badge and ping latency.

### 3. Clickable Song and Artist Buttons
Discord profile viewers can click two interactive buttons on your listening activity:
* **"Play on Derbis"**: Direct link to the track's universal web player (`https://derbis-web.vercel.app/t/<trackId>`), allowing friends to listen instantly in their browser or open it directly in Derbis via Android App Links.
* **"Artist Profile"**: Direct link to the artist's YouTube Music page (`https://music.youtube.com/channel/<artistId>`).

### 4. Dual Assets: Song Cover and Artist Icon
* **`large_image` & `large_text`**: High-resolution album artwork with track title on hover.
* **`small_image` & `small_text`**: Circular artist thumbnail badge anchored in the bottom-right corner of the album artwork, with the artist's name on hover.

### 5. Working Scrubbing Progress Bar
* Calculated from player state:
  $$\text{start} = \text{System.currentTimeMillis()} - \text{positionMs}$$
  $$\text{end} = \text{start} + \text{durationMs}$$
* Discord natively displays the animated playback scrubbing bar:
  `0:45 ━━━━━━━●────────── 3:18`
* When paused, timestamps are cleared to prevent the progress bar from continuing to advance.

### 6. Lyrics in Presence (opt-in)

The current synced lyric line can replace the first activity line, so friends see what is
being sung without opening anything.

* **Line 1 (`details`)**: the active lyric line, sanitized to Discord's 128-character cap.
* **Line 2 (`state`)**: `{artist} — {title}`, so the track stays identifiable.
* **`status_display_type: 2`**: puts that lyric line into the member-list status text
  ("Listening to *never gonna give you up*"), not just the expanded card.
* **Opt-in**: `DerbisDiscordRichPresence.isLyricsEnabled` / `setLyricsEnabled`, stored in the
  `derbis_discord_presence` preferences under `lyrics_in_presence`. Off by default because the
  exact sung line is more granular than the title and artist already shown.
* **Only synced lyrics**: the line comes from `LyricsEngine.currentLine(positionMs, lines)`;
  instrumental and metadata markers are skipped, and lyrics cached for one track never leak
  onto another.

#### Why the line does not update on every word

Discord accepts only **five presence updates per 20 seconds per session**
([docs](https://discord.com/developers/docs/events/gateway-events#update-presence)), so
per-syllable or per-word karaoke text is not possible over Rich Presence. Line-level updates
therefore go through `shouldFlushPresenceNow`, which spaces sends at least
`DISCORD_PRESENCE_MIN_INTERVAL_MS` (4 s) apart and coalesces bursts into a single trailing
update instead of dropping the newest line.

#### Native bridge note

The Android Discord Social SDK activity used by `DiscordRichPresenceBridge.cpp` has no lyric
field, so the lyric line is stripped before the native dedupe key is computed. Lyrics appear on
the Gateway path only; the native path keeps showing title and artist.

---

## Files Added & Modified

| File | Changes |
| :--- | :--- |
| `app/src/main/java/com/yasuda/derbis/feature/discord/DiscordRpcClient.kt` | Discord Gateway v10 WebSocket engine, Heartbeat loop, OP 3 presence updates with buttons, dual assets, progress bar timestamps, and 4-phase Connection Tester. |
| `app/src/main/java/com/yasuda/derbis/feature/discord/DiscordLoginSheet.kt` | Compose modal bottom sheet providing Normal Web Login and Token Login with clipboard paste and live validation. |
| `app/src/main/java/com/yasuda/derbis/feature/discord/DerbisDiscordAccountStore.kt` | Keystore AES-256 GCM storage supporting `userToken`, `username`, `globalName`, and `avatarUrl`. |
| `app/src/main/java/com/yasuda/derbis/feature/discord/DerbisDiscordRichPresence.kt` | Bridge coordinator integrating Gateway RPC and native SDK, snapshot creation with `durationMs`, `songUrl`, and `artistUrl`. |
| `app/src/main/java/com/yasuda/derbis/player/PlaybackService.kt` | Passes deck duration to `derbisDiscordPresenceSnapshot` to enable timeline progress bar. |
| `app/src/main/java/com/yasuda/derbis/ui/DerbisApp.kt` | Redesigned Discord Settings panel with user profile card, avatar, latency badge, Connection Tester, login sheet trigger, and the "Show lyrics in presence" toggle. |
| `app/src/test/java/com/yasuda/derbis/feature/discord/DiscordRichPresenceTest.kt` | Unit tests for snapshot creation, button URLs, progress bar timestamp math, token session validation, lyric sanitization, and the presence rate gate. |
