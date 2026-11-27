const IOS_SHA256 = "64282df0c7a30dd1072cd4be0e22a3b23e2ddcc007637060754d633d065f943b";

export default function handler(req, res) {
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa.sha256"');
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(`${IOS_SHA256}  Derbis.ipa\n`);
}
