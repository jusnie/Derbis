export default async function handler(req, res) {
  const { id } = req.query;
  const videoId = typeof id === "string" ? id.trim() : "";

  // Validate YouTube / track ID format
  if (!videoId || !/^[A-Za-z0-9_-]{6,20}$/.test(videoId)) {
    return res.redirect(302, "/");
  }

  let title = "Derbis Music Track";
  let artist = "Derbis Hi-Fi Audio";
  let rawThumbnail = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
  let highResThumbnail = `https://i.ytimg.com/vi/${videoId}/maxresdefault.jpg`;

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 2800);

    const oembedUrl = `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${videoId}&format=json`;
    const response = await fetch(oembedUrl, { signal: controller.signal });
    clearTimeout(timeout);

    if (response.ok) {
      const data = await response.json();
      if (data.title) {
        title = data.title;
        // Clean common suffixes like (Official Music Video), [Audio], etc.
        const cleanTitle = title
          .replace(/\s*\((?:official|music|video|audio|lyrics|hd|4k)[^)]*\)/gi, "")
          .replace(/\s*\[(?:official|music|video|audio|lyrics|hd|4k)[^\]]*\]/gi, "")
          .trim();

        if (cleanTitle.includes(" - ")) {
          const parts = cleanTitle.split(" - ");
          artist = parts[0].trim();
          title = parts.slice(1).join(" - ").trim();
        } else if (data.author_name) {
          artist = data.author_name.replace(/ - Topic$/i, "").trim();
          title = cleanTitle;
        }
      }
      if (data.author_name && (!artist || artist === "Derbis Hi-Fi Audio")) {
        artist = data.author_name.replace(/ - Topic$/i, "").trim();
      }
    }
  } catch (err) {
    // Non-blocking fallback
  }

  const escapeHtml = (str) =>
    String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");

  const safeTitle = escapeHtml(title);
  const safeArtist = escapeHtml(artist);
  const pageUrl = `https://derbis-web.vercel.app/t/${videoId}`;
  const webPlayerUrl = `/?play=${videoId}`;
  const deepLinkApp = `derbis://play?id=${videoId}`;
  const apkDownloadUrl = `https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk`;

  const html = `<!DOCTYPE html>
<html lang="en" class="dark">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
  <title>${safeTitle} — ${safeArtist} | Derbis</title>
  <meta name="description" content="Listen to ${safeTitle} by ${safeArtist} in lossless studio quality on Derbis." />
  <meta name="theme-color" content="#000000" />

  <!-- OpenGraph Metadata for Discord, WhatsApp, Telegram, Facebook -->
  <meta property="og:site_name" content="Derbis Music" />
  <meta property="og:type" content="music.song" />
  <meta property="og:title" content="${safeTitle} — ${safeArtist}" />
  <meta property="og:description" content="Lossless Hi-Fi Audio • Derbis Music" />
  <meta property="og:image" content="${highResThumbnail}" />
  <meta property="og:image:width" content="1280" />
  <meta property="og:image:height" content="720" />
  <meta property="og:image:alt" content="${safeTitle}" />
  <meta property="og:url" content="${pageUrl}" />

  <!-- Twitter / X Card Metadata -->
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:site" content="@DerbisMusic" />
  <meta name="twitter:title" content="${safeTitle} — ${safeArtist}" />
  <meta name="twitter:description" content="Lossless Hi-Fi Audio • Derbis Music" />
  <meta name="twitter:image" content="${highResThumbnail}" />

  <!-- Native Android App Link Metadata -->
  <meta property="al:android:url" content="${deepLinkApp}" />
  <meta property="al:android:app_name" content="Derbis" />
  <meta property="al:android:package" content="com.yasuda.derbis" />

  <!-- Typography -->
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet" />

  <style>
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
    }
    body {
      background-color: #000000;
      color: #ffffff;
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px 16px;
      position: relative;
      overflow-x: hidden;
    }
    .ambient-glow-1 {
      position: fixed;
      top: 15%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 480px;
      height: 480px;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, rgba(38, 178, 214, 0.12) 40%, transparent 70%);
      filter: blur(60px);
      pointer-events: none;
      z-index: 0;
      animation: pulseAura 8s ease-in-out infinite alternate;
    }
    .ambient-glow-2 {
      position: fixed;
      bottom: 10%;
      left: 50%;
      transform: translate(-50%, 0);
      width: 520px;
      height: 380px;
      background: radial-gradient(circle, rgba(111, 76, 255, 0.10) 0%, rgba(255, 255, 255, 0.04) 50%, transparent 70%);
      filter: blur(80px);
      pointer-events: none;
      z-index: 0;
    }
    @keyframes pulseAura {
      0% { transform: translate(-50%, -50%) scale(0.95); opacity: 0.8; }
      100% { transform: translate(-50%, -50%) scale(1.08); opacity: 1.0; }
    }
    .card-shell {
      position: relative;
      z-index: 10;
      max-width: 420px;
      width: 100%;
      background: rgba(18, 18, 24, 0.82);
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 28px;
      padding: 28px 24px;
      box-shadow: 0 24px 64px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.06);
      backdrop-filter: blur(32px);
      -webkit-backdrop-filter: blur(32px);
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .brand-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 22px;
    }
    .brand-title {
      font-size: 15px;
      font-weight: 800;
      letter-spacing: 0.22em;
      color: #ffffff;
      text-transform: uppercase;
    }
    .badge-quality {
      display: flex;
      align-items: center;
      gap: 6px;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.16);
      border-radius: 9999px;
      padding: 4px 12px;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #ffffff;
      text-transform: uppercase;
    }
    .wave-bars {
      display: flex;
      align-items: center;
      gap: 2.5px;
      height: 12px;
    }
    .wave-bar {
      width: 2px;
      background: #ffffff;
      border-radius: 2px;
      animation: waveBounce 1.2s ease-in-out infinite alternate;
    }
    .wave-bar:nth-child(1) { height: 4px; animation-delay: 0.1s; }
    .wave-bar:nth-child(2) { height: 9px; animation-delay: 0.3s; }
    .wave-bar:nth-child(3) { height: 12px; animation-delay: 0.2s; }
    .wave-bar:nth-child(4) { height: 6px; animation-delay: 0.4s; }
    .wave-bar:nth-child(5) { height: 10px; animation-delay: 0.15s; }
    @keyframes waveBounce {
      0% { transform: scaleY(0.4); }
      100% { transform: scaleY(1.0); }
    }
    .cover-container {
      position: relative;
      width: 100%;
      max-width: 310px;
      aspect-ratio: 1 / 1;
      border-radius: 20px;
      overflow: hidden;
      margin-bottom: 22px;
      box-shadow: 0 16px 36px rgba(0, 0, 0, 0.7);
      border: 1px solid rgba(255, 255, 255, 0.16);
      background: #111116;
    }
    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .song-title {
      font-size: 20px;
      font-weight: 800;
      color: #ffffff;
      line-height: 1.3;
      margin-bottom: 6px;
      max-width: 100%;
      word-break: break-word;
    }
    .song-artist {
      font-size: 14px;
      font-weight: 500;
      color: #a2a4b2;
      margin-bottom: 24px;
    }
    .actions-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
    }
    .btn-primary {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      height: 48px;
      background: #ffffff;
      color: #08080c;
      border-radius: 24px;
      font-size: 14px;
      font-weight: 700;
      text-decoration: none;
      transition: transform 0.15s ease, background-color 0.15s ease;
      box-shadow: 0 6px 20px rgba(255, 255, 255, 0.18);
    }
    .btn-primary:hover {
      background: #f0f0f5;
      transform: translateY(-1px);
    }
    .btn-secondary {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      height: 44px;
      background: rgba(255, 255, 255, 0.07);
      color: #ffffff;
      border: 1px solid rgba(255, 255, 255, 0.14);
      border-radius: 22px;
      font-size: 13px;
      font-weight: 600;
      text-decoration: none;
      transition: background-color 0.15s ease;
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.12);
    }
    .footer-note {
      margin-top: 18px;
      font-size: 11px;
      color: rgba(255, 255, 255, 0.45);
      letter-spacing: 0.04em;
    }
  </style>
</head>
<body>
  <div class="ambient-glow-1"></div>
  <div class="ambient-glow-2"></div>

  <div class="card-shell">
    <!-- Brand Header -->
    <div class="brand-row">
      <svg width="22" height="22" viewBox="0 0 58 58" fill="none">
        <rect x="18.9" y="14.6" width="10.2" height="10.2" rx="1.3" transform="rotate(45 24 19.7)" fill="white"/>
        <rect x="28.8" y="23.9" width="10.2" height="10.2" rx="1.3" transform="rotate(45 33.9 29)" fill="white"/>
        <rect x="18.9" y="33.2" width="10.2" height="10.2" rx="1.3" transform="rotate(45 24 38.3)" fill="white"/>
      </svg>
      <span class="brand-title">Derbis</span>
      <div class="badge-quality">
        <div class="wave-bars">
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
        </div>
        Lossless
      </div>
    </div>

    <!-- Artwork Card -->
    <div class="cover-container">
      <img
        src="${highResThumbnail}"
        onerror="this.onerror=null;this.src='${rawThumbnail}';"
        alt="${safeTitle}"
        class="cover-img"
      />
    </div>

    <!-- Metadata -->
    <h1 class="song-title">${safeTitle}</h1>
    <p class="song-artist">${safeArtist}</p>

    <!-- Action Buttons -->
    <div class="actions-list">
      <a href="${deepLinkApp}" class="btn-primary" id="open-app-btn">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="5 3 19 12 5 21 5 3"></polygon>
        </svg>
        Open in Derbis App
      </a>

      <a href="${webPlayerUrl}" class="btn-secondary">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="2" y1="12" x2="22" y2="12"></line>
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
        </svg>
        Play in Web Player
      </a>

      <a href="${apkDownloadUrl}" class="btn-secondary">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Download Android APK
      </a>
    </div>

    <div class="footer-note">Experience Hi-Fi streaming with zero ads & synced lyrics</div>
  </div>

  <script>
    // If opened on an Android device, attempt to launch Derbis via intent
    if (/Android/i.test(navigator.userAgent)) {
      setTimeout(() => {
        window.location.href = "${deepLinkApp}";
      }, 300);
    }
  </script>
</body>
</html>`;

  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(html);
}
