# Derbis

**Sleek, lightweight, AMOLED-optimized music player for Android and Desktop.**

<p align="center">
  <a href="https://github.com/jusnie/Derbis/releases/latest"><img src="https://img.shields.io/github/v/release/jusnie/Derbis?style=for-the-badge&color=white&labelColor=000000" alt="Latest Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-white?style=for-the-badge&labelColor=000000" alt="License"></a>
  <a href="https://github.com/jusnie/Derbis"><img src="https://img.shields.io/badge/Developer-Yasuda-white?style=for-the-badge&labelColor=000000" alt="Developer"></a>
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

Every release has a version-specific **New**, **Fixed**, and **Known limitations** record. Browse the full [GitHub Releases history](https://github.com/jusnie/Derbis/releases) or read the concise [CHANGELOG](CHANGELOG.md).

- **Android (APK):** [Derbis v1.0.3](https://github.com/jusnie/Derbis/releases/tag/v1.0.3)
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

## 📄 License

Derbis is licensed under the [GNU General Public License v3.0](LICENSE).
Developed with care by **Yasuda**.
