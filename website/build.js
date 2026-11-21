import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const requiredFiles = [
  "index.html",
  "vercel.json",
  "fonts/pixelify.css",
  "fonts/pixelify-sans-latin-400-normal.woff2",
  "fonts/pixelify-sans-latin-700-normal.woff2",
  "assets/derbis-logo.png",
  "api/track.js",
  "api/jam.js"
];

for (const relPath of requiredFiles) {
  const fullPath = path.resolve(__dirname, relPath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing required website file: ${relPath}`);
  }
}

// Populate dist/ as well so both "." and "dist" output directory configs work seamlessly
const distDir = path.resolve(__dirname, "dist");
fs.mkdirSync(distDir, { recursive: true });
for (const file of ["index.html", "install.html", "manifest.plist", "config.js"]) {
  const src = path.resolve(__dirname, file);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.resolve(distDir, file));
  }
}
for (const dir of ["fonts", "assets"]) {
  const srcDir = path.resolve(__dirname, dir);
  if (fs.existsSync(srcDir)) {
    fs.cpSync(srcDir, path.resolve(distDir, dir), { recursive: true });
  }
}
const publicDir = path.resolve(__dirname, "public");
if (fs.existsSync(publicDir)) {
  fs.cpSync(publicDir, distDir, { recursive: true });
}

console.log("Derbis download website verified and staged successfully.");
