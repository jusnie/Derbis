import https from "node:https";
import zlib from "node:zlib";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return get(res.headers.location).then(resolve, reject);
      }
      let data = "";
      res.on("data", (chunk) => data += chunk);
      res.on("end", () => resolve(data));
      res.on("error", reject);
    });
  });
}

(async () => {
  const url = "https://dpaste.org/5EwJN/raw";
  console.log("Fetching Black & White Tidal bundle from " + url);
  const text = await get(url);
  console.log("Successfully fetched " + text.length + " chars from " + url);
  const buf = Buffer.from(text.trim(), "base64");
  const decomp = zlib.brotliDecompressSync(buf);
  const files = JSON.parse(decomp.toString("utf8"));
  for (const [relPath, content] of Object.entries(files)) {
    const fullPath = path.resolve(__dirname, relPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    if (relPath.endsWith(".png")) {
      fs.writeFileSync(fullPath, Buffer.from(content, "base64"));
    } else {
      fs.writeFileSync(fullPath, content, "utf8");
    }
    console.log(`Extracted ${relPath} (${content.length} bytes)`);
  }
  if (files["assets/derbis-logo.png"]) {
    fs.writeFileSync(path.resolve(__dirname, "assets/derbis-logo.png"), Buffer.from(files["assets/derbis-logo.png"], "base64"));
  }
  console.log("NEW BLACK & WHITE TIDAL WEB PLAYER DEPLOYED SUCCESSFULLY");
})();
