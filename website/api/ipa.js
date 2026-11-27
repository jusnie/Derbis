const UPSTREAM_IPA_URL =
  "https://github.com/jusnie/Derbis/releases/download/ios-unofficial-13630b6/Derbis-unofficial.ipa";

export default async function handler(req, res) {
  if (req.method !== "GET" && req.method !== "HEAD") {
    res.setHeader("Allow", "GET, HEAD");
    return res.status(405).end("Method Not Allowed");
  }

  try {
    const upstream = await fetch(UPSTREAM_IPA_URL, {
      method: req.method,
      redirect: "follow",
      headers: {
        "User-Agent": "Derbis-Web-IPA-Proxy/1.8.0"
      }
    });

    if (!upstream.ok) {
      return res.redirect(302, UPSTREAM_IPA_URL);
    }

    res.setHeader("Content-Type", "application/octet-stream");
    res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa"');
    res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");

    const contentLength = upstream.headers.get("content-length");
    if (contentLength) {
      res.setHeader("Content-Length", contentLength);
    }

    if (req.method === "HEAD") {
      return res.status(200).end();
    }

    const arrayBuffer = await upstream.arrayBuffer();
    return res.status(200).send(Buffer.from(arrayBuffer));
  } catch {
    return res.redirect(302, UPSTREAM_IPA_URL);
  }
}
