# Derbis Canvas Catalog

A small **self-hosted** resolver for genuine animated album artwork in Derbis. It deliberately does not query Spotify, Apple Music, TIDAL, YouTube, or any third-party Canvas endpoint. Add only media that you own or are explicitly licensed to host and show.

Derbis treats this service as optional. Without a configured catalog URL **and** release key, or when a track has no verified match, the app remains on static album art.

## Security and ownership model

- The Android client sends a bounded lookup containing only `isrc`, `title`, `artist`, `album`, and `durationMs`—never listener login cookies, streaming-service tokens, or account identity.
- The service uses a required `X-Derbis-Canvas-Key` request header. Generate a unique, high-entropy secret (for example, `openssl rand -base64 32`); do not commit it.
- The Android client accepts only `https://` URLs on port 443, at the **same origin** as the configured resolver, under `/media/`, ending in `.mp4` or `.m3u8`. It also checks media MIME type and blocks private/local-network DNS destinations.
- Do not point a production build at a LAN URL, IP address, a redirector, or a shared third-party catalog.
- Key authentication identifies a trusted app build; it is not DRM. Serve only media that is safe to deliver to every holder of that release. Rotate the key and build a new release if it leaks.

## Catalog format

Edit [`catalog/catalog.json`](catalog/catalog.json). The checked-in file starts empty so it cannot accidentally claim ownership of any artwork.

```json
{
  "version": 1,
  "entries": [
    {
      "isrc": "USABC2400001",
      "title": "Track title",
      "artist": "Artist name",
      "album": "Album title",
      "durationMs": 213000,
      "file": "artist-name/track-title.mp4",
      "mimeType": "video/mp4",
      "width": 1080,
      "height": 1920
    }
  ]
}
```

Place that file at `media/artist-name/track-title.mp4`. `file` is a safe path relative to `media/`; only `.mp4` and `.m3u8` entries are accepted. HLS playlists may reference `.ts` or `.m4s` segments under the same `media/` tree. The service serves byte ranges for MP4 seeking/playback and conservative immutable cache headers; use a new filename when replacing a media asset.

Resolution is deterministic:

1. A valid ISRC match wins.
2. Otherwise, title + artist + album must match exactly after Unicode normalization, case folding, accent removal, and punctuation/whitespace folding. When both durations exist, they must be within two seconds.
3. A miss returns `{"status":"miss"}`. There is no fuzzy web search or external fallback.

The resolver reloads `catalog.json` after a save. It rejects malformed, duplicate, unsafe, or missing-media entries and keeps the last valid catalog in memory.

## Run locally

Node 20+ is the only dependency:

```bash
cd server/derbis-canvas-catalog
export DERBIS_CANVAS_CATALOG_KEY="$(openssl rand -base64 32)"
export PUBLIC_BASE_URL="https://canvas.example.com"
npm run check
npm start
```

Local HTTP is useful for exercising the service, but an Android release will intentionally reject `http://` media and resolver URLs. Publish it behind TLS at one stable origin instead.

Health check:

```bash
curl http://127.0.0.1:8789/healthz
```

Authenticated resolution request:

```bash
curl --fail-with-body http://127.0.0.1:8789/v1/motion-artwork/resolve \
  -H "Content-Type: application/json" \
  -H "X-Derbis-Canvas-Key: $DERBIS_CANVAS_CATALOG_KEY" \
  --data '{"isrc":"USABC2400001","title":"Track title","artist":"Artist name","album":"Album title","durationMs":213000}'
```

## Docker and HTTPS deployment

1. Copy the secret and public address to a local `.env` file (which is gitignored):

   ```dotenv
   DERBIS_CANVAS_CATALOG_KEY=replace-with-a-long-random-secret
   PUBLIC_BASE_URL=https://canvas.example.com
   ```

2. Put the rights-cleared media under `media/` and update `catalog/catalog.json`.
3. Start the private application port:

   ```bash
   docker compose up -d --build
   ```

4. Terminate TLS on the *same* `canvas.example.com` origin. [`Caddyfile.example`](Caddyfile.example) is the minimal Caddy v2 virtual host; it obtains HTTPS certificates and reverse-proxies to `127.0.0.1:8789`. Configure DNS and firewall access for TCP 443. Do not expose the container's HTTP port directly to the internet.
5. Confirm `https://canvas.example.com/healthz`, then use the exact `https://canvas.example.com/v1/motion-artwork/resolve` path in the Android build configuration below.

Keep the resolver and media at the same bare HTTPS origin. Redirects, different hosts, alternate ports, credentials embedded in URLs, and paths outside `/media/` are rejected by Derbis.

## Android release configuration

The URL and key are build-time fields, not a settings form. This avoids asking listeners to paste an arbitrary endpoint and makes an unconfigured/F-Droid build safely static-only.

For a local release build, place untracked values in `local.properties`:

```properties
derbisCanvasCatalogUrl=https://canvas.example.com/v1/motion-artwork/resolve
derbisCanvasCatalogKey=replace-with-the-same-long-random-secret
```

Or provide CI environment variables:

```text
DERBIS_CANVAS_CATALOG_URL=https://canvas.example.com/v1/motion-artwork/resolve
DERBIS_CANVAS_CATALOG_KEY=replace-with-the-same-long-random-secret
```

For GitHub Actions, create repository/environment secrets named `DERBIS_CANVAS_CATALOG_URL` and `DERBIS_CANVAS_CATALOG_KEY`, then pass them only to the signed Android release build environment. Never print either value in build logs. The key will be recoverable from a distributed APK, so it must not authorize anything beyond serving the catalog's public-to-app media.

The endpoint is strict: it must be HTTPS on port 443 with no URL credentials and end exactly in `/v1/motion-artwork/resolve`. An absent/invalid URL or key produces no animation request; static artwork remains the fallback.
