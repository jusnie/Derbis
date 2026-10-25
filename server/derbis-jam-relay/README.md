# Derbis Jam Relay

A compact WebSocket relay for **Derbis Jam**. It relays encrypted-in-app session protocol messages between a host and up to seven guests; it does not stream or rebroadcast audio.

## Deploy

Run it on any host that supports long-lived WebSockets (Cloud Run, Fly.io, Render, Railway, a VPS, etc.). **Do not deploy it on Vercel serverless functions**: persistent WebSocket rooms require a stateful, long-running process.

```bash
npm install
PORT=8787 npm start
```

Health check: `GET /health`.

For Docker:

```bash
docker build -t derbis-jam-relay .
docker run --rm -p 8787:8787 derbis-jam-relay
```

Terminate TLS at the platform/load balancer and configure the Android app with the public `wss://your-domain/jam` URL as `DERBIS_JAM_RELAY_URL` / `derbisJamRelayUrl`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8787` | Listening port |
| `JAM_ALLOWED_ORIGINS` | unset | Optional comma-separated browser origin allowlist. Native Android/Desktop clients do not send an Origin header. |

Rooms are in memory and last only while the host is connected. Run a single instance for this release, or add sticky routing / shared room coordination before horizontally scaling.

## Security model

The share code contains a high-entropy session secret. The clients verify the host/guest challenge-response protocol themselves; the relay only forwards bounded protocol messages. Use TLS in production, avoid logging request content, and rotate/deploy infrastructure independently from the app release.
