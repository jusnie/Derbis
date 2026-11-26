const IOS_SHA256 = "334d50f8e26cf1a82ddd4a47d3f1fa0ee6eb5278bcf2e888b163ab242c52f69d";

export default function handler(req, res) {
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa.sha256"');
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(`${IOS_SHA256}  Derbis.ipa\n`);
}
