const IOS_SHA256 = "75f59ac934b17295fc0210091f0061c53091800bcccda7998521327df58a50cc";

export default function handler(req, res) {
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.setHeader("Content-Disposition", 'attachment; filename="Derbis.ipa.sha256"');
  res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
  return res.status(200).send(`${IOS_SHA256}  Derbis.ipa\n`);
}
