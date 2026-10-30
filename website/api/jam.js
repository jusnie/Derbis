function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export default async function handler(req, res) {
  const urlObj = new URL(req.url, `https://${req.headers.host || "derbis-web.vercel.app"}`);
  let jamCode = urlObj.searchParams.get("code");

  if (!jamCode) {
    const parts = urlObj.pathname.split("/").filter(Boolean);
    if (parts[0] === "jam" && parts[1]) {
      jamCode = parts[1];
    }
  }

  const cleanCode = escapeHtml(jamCode ? jamCode.trim().toUpperCase() : "");
  const canonicalUrl = `https://derbis-web.vercel.app/jam/${cleanCode}`;
  const appIntentUrl = cleanCode ? `derbis://jam/${cleanCode}` : "derbis://jam";
  const apkDownloadUrl = "https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk";

  const html = `<!DOCTYPE html>
<html lang="en" class="dark">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
  <title>Listen Together — Derbis Jam | Real-Time Music Party</title>
  <meta name="description" content="Join a live Listen Together party on Derbis Jam. Synchronized real-time playback, shared queue, and studio audio." />
  <meta name="theme-color" content="#000000" />

  <meta property="og:site_name" content="Derbis Music" />
  <meta property="og:type" content="music.song" />
  <meta property="og:title" content="Join Listen Together on Derbis Jam" />
  <meta property="og:description" content="Live synchronized listening session • Real-time shared queue" />
  <meta property="og:url" content="${canonicalUrl}" />

  <meta name="twitter:card" content="summary" />
  <meta name="twitter:title" content="Join Listen Together on Derbis Jam" />
  <meta name="twitter:description" content="Stream synchronously in real time on Derbis Music." />

  <meta property="al:android:url" content="${appIntentUrl}" />
  <meta property="al:android:package" content="com.yasuda.derbis" />
  <meta property="al:android:app_name" content="Derbis" />
  <meta property="al:web:url" content="https://derbis-web.vercel.app" />

  <style>
    :root {
      --bg: #000000;
      --card-bg: rgba(255, 255, 255, 0.04);
      --card-border: rgba(255, 255, 255, 0.1);
      --text-pri: #ffffff;
      --text-sec: rgba(255, 255, 255, 0.64);
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text-pri);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
      position: relative;
      overflow-x: hidden;
      -webkit-font-smoothing: antialiased;
    }
    .blur-glow {
      position: fixed;
      width: 460px;
      height: 460px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(255,255,255,0.16) 0%, rgba(255,255,255,0.03) 45%, transparent 75%);
      filter: blur(80px);
      z-index: 0;
      pointer-events: none;
      animation: pulse 8s ease-in-out infinite alternate;
    }
    @keyframes pulse {
      0% { transform: scale(0.9) translate(-10px, -15px); opacity: 0.65; }
      100% { transform: scale(1.15) translate(15px, 20px); opacity: 0.95; }
    }
    .card {
      position: relative;
      z-index: 1;
      width: 100%;
      max-width: 440px;
      background: var(--card-bg);
      backdrop-filter: blur(32px);
      -webkit-backdrop-filter: blur(32px);
      border: 1px solid var(--card-border);
      border-radius: 28px;
      padding: 36px 28px;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      box-shadow: 0 30px 60px rgba(0, 0, 0, 0.7);
    }
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.12);
      padding: 6px 14px;
      border-radius: 9999px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: rgba(255, 255, 255, 0.85);
      margin-bottom: 24px;
    }
    .icon-wrap {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.14);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
    }
    .title {
      font-size: 26px;
      font-weight: 800;
      line-height: 1.2;
      letter-spacing: -0.02em;
      margin-bottom: 8px;
    }
    .subtitle {
      font-size: 14px;
      line-height: 1.45;
      color: var(--text-sec);
      margin-bottom: 26px;
    }
    .code-box {
      width: 100%;
      background: rgba(255, 255, 255, 0.05);
      border: 1px dashed rgba(255, 255, 255, 0.2);
      border-radius: 18px;
      padding: 16px 20px;
      margin-bottom: 26px;
    }
    .code-label {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: rgba(255, 255, 255, 0.5);
      margin-bottom: 6px;
    }
    .code-value {
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 0.12em;
      color: #ffffff;
      word-break: break-all;
    }
    .actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
      width: 100%;
    }
    .btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      padding: 14px 20px;
      border-radius: 16px;
      font-size: 14px;
      font-weight: 600;
      text-decoration: none;
      transition: all 0.2s ease;
      cursor: pointer;
    }
    .btn-primary {
      background: #ffffff;
      color: #000000;
    }
    .btn-primary:hover {
      background: rgba(255, 255, 255, 0.9);
      transform: translateY(-1px);
    }
    .btn-secondary {
      background: rgba(255, 255, 255, 0.08);
      color: #ffffff;
      border: 1px solid rgba(255, 255, 255, 0.12);
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.14);
      transform: translateY(-1px);
    }
    .footer {
      margin-top: 24px;
      font-size: 11px;
      color: rgba(255, 255, 255, 0.4);
    }
  </style>
</head>
<body>
  <div class="blur-glow"></div>
  <main class="card">
    <div class="badge">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
      Listen Together
    </div>

    <div class="icon-wrap">
      <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
        <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
      </svg>
    </div>

    <h1 class="title">Derbis Jam</h1>
    <p class="subtitle">Join this synchronized Listen Together session to hear songs and queue tracks with friends in real time.</p>

    ${cleanCode ? `
    <div class="code-box">
      <div class="code-label">Session Code</div>
      <div class="code-value">${cleanCode}</div>
    </div>
    ` : ''}

    <div class="actions">
      <a href="${appIntentUrl}" class="btn btn-primary" id="openAppBtn">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5v14l11-7z"/>
        </svg>
        Join in Derbis App
      </a>
      <a href="${apkDownloadUrl}" class="btn btn-secondary">
        Download Derbis for Android
      </a>
      <a href="https://derbis-web.vercel.app" class="btn btn-secondary" style="font-size: 12px; opacity: 0.7;">
        Open Web Player
      </a>
    </div>

    <div class="footer">
      <span>High-Fidelity Audio • Yasuda</span>
    </div>
  </main>

  <script>
    if (/android/i.test(navigator.userAgent) && "${cleanCode}") {
      var appBtn = document.getElementById("openAppBtn");
      if (appBtn) {
        setTimeout(function() {
          window.location.href = "${appIntentUrl}";
        }, 120);
      }
    }
  </script>
</body>
</html>`;

  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(html);
}
