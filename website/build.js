import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const requiredSourceFiles = [
  "index.html",
  "vercel.json",
  "fonts/pixelify-sans-latin-400-normal.woff2",
  "fonts/pixelify-sans-latin-700-normal.woff2",
  "public/screens/apple-music-13.jpg",
  "public/screens/pixel-ui.jpg",
  "public/screens/derbis-default.jpg",
  "public/screens/synced-lyrics.jpg",
  "public/screens/late-night.jpg",
  "api/track.js",
  "api/jam.js"
];

for (const relPath of requiredSourceFiles) {
  const fullPath = path.resolve(__dirname, relPath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing required website file: ${relPath}`);
  }
}

const distDir = path.resolve(__dirname, "dist");
const requiredDistFiles = [
  "index.html",
  "install.html",
  "manifest.plist",
  "screens/apple-music-13.jpg",
  "screens/pixel-ui.jpg",
  "screens/derbis-default.jpg",
  "screens/synced-lyrics.jpg",
  "screens/late-night.jpg"
];

for (const relPath of requiredDistFiles) {
  const fullPath = path.resolve(distDir, relPath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing built dist file: ${relPath}`);
  }
}

console.log("Derbis React + Vite website verified in dist/ successfully.");
