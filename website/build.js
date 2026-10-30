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
  const url = "https://dpaste.org/fWuWO/raw";
  console.log("Fetching Derbis bundle from " + url);
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

  // Apply iOS & IPA platform adaptations to bundled JS
  const jsBundlePath = path.resolve(__dirname, "assets/index-rTq1BDYL.js");
  if (fs.existsSync(jsBundlePath)) {
    let js = fs.readFileSync(jsBundlePath, "utf8");
    const iosCheck = "(/iPad|iPhone|iPod/.test(navigator.userAgent)||(navigator.platform===\"MacIntel\"&&navigator.maxTouchPoints>1)||window.Capacitor?.getPlatform?.()===\"ios\")";

    const oldSidebarCard = "(0,T.jsx)(`div`,{className:`mt-6 px-1`,children:(0,T.jsxs)(`a`,{href:`https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk`,download:!0";
    const newSidebarCard = iosCheck + "?null:(0,T.jsx)(`div`,{className:`mt-6 px-1`,children:(0,T.jsxs)(`a`,{href:`https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk`,download:!0";
    if (js.includes(oldSidebarCard)) {
      js = js.replace(oldSidebarCard, newSidebarCard);
    }

    const oldSettingsCard = "(0,T.jsxs)(`div`,{className:`p-3.5 rounded-xl bg-[#0e0e12] border border-[#1e1e24] space-y-2`,children:[(0,T.jsxs)(`div`,{className:`flex items-center justify-between`,children:[(0,T.jsxs)(`div`,{className:`flex items-center gap-1.5`,children:[(0,T.jsx)(Ot,{className:`w-3.5 h-3.5 text-white/70`}),(0,T.jsx)(`strong`,{className:`text-xs font-bold text-white`,children:`Derbis Android App`})";
    const newSettingsCard = iosCheck + "?(0,T.jsxs)(`div`,{className:`p-3.5 rounded-xl bg-[#0e0e12] border border-cyan-500/20 space-y-1.5`,children:[(0,T.jsxs)(`div`,{className:`flex items-center justify-between`,children:[(0,T.jsx)(`strong`,{className:`text-xs font-bold text-white`,children:`Derbis iOS Native`}),(0,T.jsx)(`span`,{className:`text-[9px] font-mono text-cyan-400 font-bold`,children:`v1.1.1`})]}),(0,T.jsx)(`p`,{className:`text-[11px] text-white/60 leading-relaxed`,children:`Lock Screen Controls, AirPlay & Hi-Fi Lossless Audio Active.`})]}):" + oldSettingsCard;
    if (js.includes(oldSettingsCard)) {
      js = js.replace(oldSettingsCard, newSettingsCard);
    }

    fs.writeFileSync(jsBundlePath, js, "utf8");
    console.log("Applied iOS platform adaptations to bundle");
  }

  // Ensure vercel.json rewrites for /install, /ios, /t/:id universal track links, and manifest.plist
  const vercelConfig = {
    cleanUrls: true,
    rewrites: [
      { source: "/install", destination: "/install.html" },
      { source: "/ios", destination: "/install.html" },
      { source: "/t/:id", destination: "/api/track?id=:id" },
      { source: "/track/:id", destination: "/api/track?id=:id" },
      { source: "/jam/:code", destination: "/api/jam?code=:code" },
      { source: "/jam", destination: "/api/jam" }
    ],
    headers: [
      {
        source: "/manifest.plist",
        headers: [
          { key: "Content-Type", value: "application/xml" }
        ]
      },
      {
        source: "/(.*)",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" }
        ]
      }
    ]
  };
  fs.writeFileSync(path.resolve(__dirname, "vercel.json"), JSON.stringify(vercelConfig, null, 2), "utf8");

  console.log("DERBIS SEARCH SELECTION & ENDLESS RADIO BUNDLE DEPLOYED SUCCESSFULLY");
})();
