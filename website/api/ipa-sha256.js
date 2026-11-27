const IOS_SHA256 = "0c717142e103895c8e9c7841dec96427bebf2ebd38126cdb9acfdee6c58c5417";

export default function handler(req, res) {
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa.sha256"');
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(`${IOS_SHA256}  Derbis.ipa\n`);
}
