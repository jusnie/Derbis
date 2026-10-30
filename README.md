# Derbis

<p align="center">
  <img src="assets/derbis-banner.png" alt="Derbis Android app previews" width="100%">
</p>

**Sleek, lightweight, AMOLED-optimized music player for Android and Desktop.**

<p align="center">
  <a href="https://github.com/jusnie/Derbis/releases/latest"><img src="https://img.shields.io/github/v/release/jusnie/Derbis?style=for-the-badge&color=white&labelColor=000000" alt="Latest Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-EULA-white?style=for-the-badge&labelColor=000000" alt="License"></a>
  <a href="https://github.com/jusnie/Derbis"><img src="https://img.shields.io/badge/Developer-Yasuda-white?style=for-the-badge&labelColor=000000" alt="Developer"></a>
</p>

<h2 align="center">Screenshots</h2>

<p align="center">
  <img src="assets/derbis-screenshots.png" alt="Derbis music library, home, and now-playing screens" width="100%">
</p>

<p align="center">
  <img src="assets/derbis-screenshot-01.png" alt="Derbis Home and Your Orbit preview" width="44%">
  <img src="assets/derbis-screenshot-02.png" alt="Derbis Discover for You preview" width="44%">
</p>

<p align="center">
  <img src="assets/derbis-screenshot-03.png" alt="Derbis Player Deck and Reactive Aura preview" width="44%">
  <img src="assets/derbis-screenshot-04.png" alt="Derbis queue preview" width="44%">
</p>

<p align="center">
  <img src="assets/derbis-screenshot-05.png" alt="Derbis synchronized lyrics preview" width="44%">
  <img src="assets/derbis-screenshot-06.png" alt="Derbis now-playing preview" width="44%">
</p>

---

## ✨ Features

- 🖤 **Pure AMOLED Black & White Design**: Minimalist pitch-black interface (#000000) with crisp white accents, optimized for OLED displays and battery efficiency.
- ⚡ **Lightweight & High Performance**: Clean R8-optimized release build with zero runtime diagnostic overhead, snappy animations, and 120Hz smooth scrolling.
- 🌅 **Dynamic Greetings**: Welcomes you based on the time of day ("Good morning", "Good afternoon", "Good evening", "Good night") across both Android and Desktop.
- 🎵 **Unified Music Experience**: Stream high-quality tracks and play your local audio library seamlessly.
- 📜 **Synced Lyrics**: Follow along with synchronized timed lyrics and word-by-word playback.
- 🔒 **Privacy Focused**: No accounts, no data collection, no telemetry.
- 💻 **Cross-Platform**: Built for Android (`com.derbis.app`) and Desktop (Windows / Linux / macOS).

---

## 📥 Downloads and version history

Every release has a version-specific **What's Fixed**, **What's New**, and **Changed** record. Browse the full [GitHub Releases history](https://github.com/jusnie/Derbis/releases) or read the concise [CHANGELOG](CHANGELOG.md).

- **Android (APK):** [Derbis v1.1.0](https://github.com/jusnie/Derbis/releases/tag/v1.1.0)
- **Windows (recommended MSI):** [Derbis for Windows v1.0.3](https://github.com/jusnie/Derbis/releases/tag/desktop-v1.0.3)
- **Windows (alternative EXE):** [Derbis for Windows v1.0.3](https://github.com/jusnie/Derbis/releases/tag/desktop-v1.0.3)

For the Windows installers, download the matching `.sha256` asset to verify the file integrity. See the [release process](docs/RELEASE_PROCESS.md) for package naming, test-release, and publication rules.

---

## 🛠️ Building from Source

### Android Build
```bash
./gradlew :app:assembleRelease
```
The output APK will be located at `app/build/outputs/apk/release/app-release.apk`.

### Desktop Build
```bash
cd desktop
./gradlew packageDistributionForCurrentOS
```

### Website
The dependency-free responsive web companion lives in [`website/`](website/). It uses browser-native HTML5 audio, so it does **not** need VLC or a desktop helper. Its optional Google/YouTube connection is limited to official Google authorization and YouTube Data API access; setup instructions are in [`website/README.md`](website/README.md).

---

<h2 align="center">Support Derbis</h2>

<p align="center">
  If Derbis makes your listening better, you can support its continued development.
</p>

<p align="center">
  <a href="https://buymeacoffee.com/yasudaig"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=000000" alt="Support Derbis on Buy Me a Coffee"></a>
  <a href="https://www.paypal.me/YasudaPlayz"><img src="https://img.shields.io/badge/PayPal-support-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="Support Derbis via PayPal"></a>
</p>

<p align="center">
  <img src="assets/derbis-gcash-qr.jpg" alt="GCash or InstaPay QR code for supporting Derbis" width="220">
  <br>
  <sub>Scan with GCash or a supported InstaPay app.</sub>
</p>

---

## 📄 License

Derbis is proprietary software licensed under the [Derbis End User License Agreement (EULA)](LICENSE).  
Copyright © 2026 **Yasuda**. All rights reserved.  
Unauthorized copying, cloning, redistribution, public mirroring, or commercial exploitation is strictly prohibited.
