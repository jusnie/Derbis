const IOS_SHA256 = "204648914c151dfadb7545362cad6b89ba6ddfe1dd2715e20338a6127c59fdbb";

export default function handler(req, res) {
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa.sha256"');
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(`${IOS_SHA256}  Derbis.ipa\n`);
}
