# Derbis Universal Web Sharing & Rich Social Previews

## Production Deployment Status: LIVE
- **Universal Track Link**: `https://derbis-web.vercel.app/t/<videoId>`
- **Alternate Alias Link**: `https://derbis-web.vercel.app/track/<videoId>`
- **Live Web Player**: `https://derbis-web.vercel.app/?play=<videoId>`
- **Direct Android APK**: `https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk`
- **Vercel Production Deployment**: `dpl_8qub7F4sbra35WoPXeA3YkJFhX6z` (Status: `READY`, HTTP 200 OK verified)

---

## 1. Overview & Architecture

Derbis universal track sharing bridges the native Android app, external social platforms (Instagram, WhatsApp, Twitter/X, Discord, iMessage), and the web client into a single fluid ecosystem.

```
       [ Derbis Android App ]
                 │
  (Tap Share in Now Playing or Stories)
                 │
                 ▼
  https://derbis-web.vercel.app/t/<videoId>
                 │
      ┌──────────┴──────────┐
      ▼                     ▼
[ Social Bot / Crawler ]    [ User Browser / Device ]
- OpenGraph (og:title, ...)  - Responsive Pure Black UI
- Twitter Card (large image) - Animated Radial Glow
- YouTube oEmbed Resolution  - Auto-open Android App Link (`derbis://play?id=...`)
- 1080p Cover Art            - In-browser Fallback to Web Player (`?play=...`)
```

---

## 2. Serverless Edge Unfurl (`website/api/track.js`)

When a shared link `https://derbis-web.vercel.app/t/<id>` is visited:
1. **Dynamic Metadata Extraction**:
   - Queries YouTube oEmbed endpoint (`https://www.youtube.com/oembed?url=...&format=json`) to resolve track title, artist name, and high-resolution thumbnail.
   - Falls back gracefully to `https://i.ytimg.com/vi/<id>/maxresdefault.jpg` for lossless cover art preview.
2. **OpenGraph & Twitter Card Tags**:
   - `og:site_name`: `Derbis Music`
   - `og:type`: `music.song`
   - `og:title`: `<Clean Title> — <Clean Artist>`
   - `og:description`: `Lossless Hi-Fi Audio • Derbis Music`
   - `og:image`: High-res cover image (1280x720)
   - `twitter:card`: `summary_large_image`
3. **App Links & Deep Linking Meta**:
   - `al:android:url`: `derbis://play?id=<id>`
   - `al:android:package`: `com.yasuda.derbis`
   - `al:android:app_name`: `Derbis`
   - `al:web:url`: `https://derbis-web.vercel.app/?play=<id>`
4. **Responsive Black & White Landing Page**:
   - Matches Derbis design language: pure OLED black (`#000000`), smooth blurred glowing radial backdrop (`animation: pulse 8s ease-in-out infinite alternate`), modern SVG icons, and typography.
   - Prominent actions: **Open in Derbis App** (`derbis://play?id=...`), **Play in Web Player** (`/?play=...`), and **Download APK**.
   - Automatic deep-link dispatch on Android devices.

---

## 3. Web Player Integration (`website/src/context/PlayerContext.jsx`)

- Automatically listens for query parameters (`?play=<videoId>` or `?t=<videoId>`) upon mounting.
- Instantly decodes the video identifier, fetches track details via YouTube oEmbed, and initiates playback.
- Automatically seeds the queue with continuous radio recommendations based on the shared song, upholding the endless listening guarantee.

---

## 4. Android Deep Linking & App Intents (`derbis/app/`)

1. **`AndroidManifest.xml` Intent-Filters**:
   - Registered `https://derbis-web.vercel.app/t/*` and `/track/*` with `android.intent.category.BROWSABLE` and `DEFAULT`.
   - Registered custom URI scheme `derbis://play?id=...` and `derbis://track/...`.
2. **`SharedMediaIntentParser.kt`**:
   - Enhanced to recognize `derbis-web.vercel.app` and custom scheme `derbis://`, extracting the 11-character video ID and mapping to `SharedMediaKind.Video`.
3. **`TrackStoryShareSheet.kt` & `DerbisNowPlaying.kt`**:
   - Updated link generators to emit the canonical universal share URL: `https://derbis-web.vercel.app/t/<videoId>`.

---

## 5. Live Endpoint Verification

```bash
$ curl -s -i "https://derbis-web.vercel.app/t/dQw4w9WgXcQ" | head -n 25
HTTP/2 200
content-type: text/html; charset=utf-8
server: Vercel
...
<meta property="og:site_name" content="Derbis Music" />
<meta property="og:type" content="music.song" />
<meta property="og:title" content="Never Gonna Give You Up (Official Video) (4K Remaster) — Rick Astley" />
<meta property="og:description" content="Lossless Hi-Fi Audio • Derbis Music" />
<meta property="og:image" content="https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg" />
<meta name="twitter:card" content="summary_large_image" />
<meta property="al:android:url" content="derbis://play?id=dQw4w9WgXcQ" />
```
