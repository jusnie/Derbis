# Next-Song Stream Preloading (ArchiveTune Pattern) in Derbis

## Overview & Architecture
To eliminate track transition latency, dropped audio frames, and stream stall glitches—especially when streaming through YouTube InnerTube or performing crossfading transitions—Derbis integrates the **ArchiveTune Sliding-Window Stream Preloading Architecture**.

Previously, the app only resolved track URLs and primed a minimal 384 KB chunk when a song started. In practice, long songs or pause periods could cause playback tokens or temporary URLs to become stale, and standard gapless playback had no proactive background pre-caching mechanism before the track boundary.

With this implementation, Derbis continuously tracks playback progress and executes deep stream pre-caching directly into Media3's LRU cache.

---

## Technical Specifications

### 1. Sliding-Window Preload Trigger
- **Lead Window Threshold:** `30,000 ms` (30 seconds before track completion).
- **Short-Track Progress Ratio:** `70%` elapsed playback time for short songs (e.g., interludes, skits under 30s).
- **Evaluation Loop:** Monitored on every 250ms tick of `queueTransitionMonitorJob` in `PlaybackService.kt`.
- **Preload Guard:** `shouldTriggerStreamPreload` prevents duplicate downloads once cached and honors user settings (`currentAudioSettings.preloadNextTrack`).

### 2. Deep Stream Buffer Sizing
- **Buffer Size:** `1,536 KB` (1.5 MB) via `PlaybackWarmup.STREAM_PRELOAD_BYTES`.
- **Audio Capacity:** Corresponds to ~15–20 seconds of continuous Opus (160 kbps) or AAC (256 kbps) audio.
- **Maximum Prime Upper Bound:** Expanded from 1.5 MB to `2,048 KB` (2 MB) in `PlaybackWarmup.MAX_PRIME_BYTES` to ensure complete header and audio sample coverage.

### 3. URL Freshness & Anti-Expiry Guard
- **Precache Max Age:** Governed by `PREPARED_QUEUE_MAX_AGE_MS` (15 minutes).
- **Freshness Check:** If the current track was paused for longer than the validity window, the stale stream URL is automatically discarded and re-resolved fresh from YouTube InnerTube with verified signature tokens before preloading.

### 4. Seamless Integration with Crossfade & AutoMix
- When **Crossfade** is enabled (`crossfadeSeconds > 0`):
  - Secondary ExoPlayer initialization occurs when remaining time matches the AutoMix lead window (`plan.preloadLeadMs`).
  - Because the 1.5 MB stream chunk was already cached during the 30-second window, the secondary deck prepares instantly from local disk cache, achieving 0ms latency without network stalls or stutter.
- When **Gapless Playback** is active (`crossfadeSeconds == 0`):
  - Media3 advances to the next item seamlessly as soon as the current track completes. The preloaded bytes ensure immediate audio output without waiting for an HTTP handshake.

---

## Modified & Added Components
1. **`app/src/main/java/com/yasuda/derbis/player/QueuePrecachePolicy.kt`**
   - Added `DEFAULT_STREAM_PRELOAD_TRIGGER_REMAINING_MS` (30,000 ms).
   - Added `SHORT_TRACK_STREAM_PRELOAD_THRESHOLD_RATIO` (0.70).
   - Added pure functional predicates `shouldTriggerStreamPreload` and `isStreamPreloadValid`.
2. **`app/src/main/java/com/yasuda/derbis/player/PlaybackWarmup.kt`**
   - Added `STREAM_PRELOAD_BYTES` (1.5 MB) and `MAX_PRIME_BYTES` (2 MB).
   - Added `streamPreload(track, bytes)` and `isStreamPreloaded(track, bytes)`.
3. **`app/src/main/java/com/yasuda/derbis/player/PlaybackService.kt`**
   - Updated `PreparedQueueNext` to track `streamPreloaded` and `preloadedBytes`.
   - Updated `prepareQueueNextInternal` to support `deepStreamPreload` with background coroutine streaming.
   - Updated `maybePrepareQueueTransition` to run the sliding-window preload trigger during playback.
4. **`app/src/main/java/com/yasuda/derbis/ui/DerbisPreloadStrings.kt`**
   - Updated English and Italian localized descriptions to clearly reflect ArchiveTune-style 1.5 MB stream preloading.
5. **Unit Tests:**
   - `app/src/test/java/com/yasuda/derbis/player/NextSongStreamPreloadTest.kt` (trigger window, short-track 70% ratio, setting suppression, precache freshness, and buffer constant validation).
   - `app/src/test/java/com/yasuda/derbis/player/QueuePrecachePolicyTest.kt` (extended with preload trigger and validity tests).
