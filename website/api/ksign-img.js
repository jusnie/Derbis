const ALLOWED_IMAGES = {
  hero: "https://ksignios.app/assets/hero-signing.webp?v=webp30",
  "dns-profile": "https://ksignios.app/assets/home-guide/setup-dns.webp",
  "direct-install": "https://ksignios.app/assets/home-guide/setup-install.webp",
  "trust-profile": "https://ksignios.app/assets/home-guide/setup-trust.webp?v=webp30",
  "cert-extract": "https://ksignios.app/assets/certificate-save-files-v2.webp?v=webp30",
  "cert-open": "https://ksignios.app/assets/certificate-open-certificates-v2.webp?v=webp30",
  "cert-pair": "https://ksignios.app/assets/certificate-import-pair-v2.webp?v=webp30",
  "cert-save": "https://ksignios.app/assets/certificate-save-identity-v3.webp?v=webp30",
  "ipa-import": "https://ksignios.app/assets/home-guide/ipa-import.webp",
  "ipa-library": "https://ksignios.app/assets/home-guide/ipa-library.webp",
  "ipa-sign": "https://ksignios.app/assets/home-guide/ipa-sign.webp"
};

export default async function handler(req, res) {
  if (req.method !== "GET" && req.method !== "HEAD") {
    res.setHeader("Allow", "GET, HEAD");
    return res.status(405).end("Method Not Allowed");
  }

  const urlObj = new URL(req.url, `https://${req.headers.host || "derbis-web.vercel.app"}`);
  const key = urlObj.searchParams.get("key") || "";
  const upstreamUrl = ALLOWED_IMAGES[key];

  if (!upstreamUrl) {
    return res.status(404).end("Not Found");
  }

  try {
    const upstream = await fetch(upstreamUrl, {
      method: req.method,
      redirect: "follow",
      headers: {
        Accept: "image/webp,image/avif,image/*,*/*;q=0.8",
        "User-Agent": "Derbis-Web-Guide-Proxy/1.8"
      }
    });

    if (!upstream.ok) {
      return res.redirect(302, upstreamUrl);
    }

    const contentType = upstream.headers.get("content-type") || "image/webp";
    res.setHeader("Content-Type", contentType);
    res.setHeader(
      "Cache-Control",
      "public, max-age=2592000, s-maxage=2592000, stale-while-revalidate=86400"
    );
    res.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

    if (req.method === "HEAD") {
      return res.status(200).end();
    }

    const buf = Buffer.from(await upstream.arrayBuffer());
    res.setHeader("Content-Length", String(buf.byteLength));
    return res.status(200).send(buf);
  } catch {
    return res.redirect(302, upstreamUrl);
  }
}
