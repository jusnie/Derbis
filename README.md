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

## 📥 Downloads

Download the latest version of Derbis directly from [GitHub Releases](https://github.com/jusnie/Derbis/releases):

- **Android (APK)**: [Derbis Latest APK](https://github.com/jusnie/Derbis/releases/latest)
- **Desktop**: [Derbis Desktop Releases](https://github.com/jusnie/Derbis/releases)

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

---

## 📄 License

Derbis is licensed under the [GNU General Public License v3.0](LICENSE).
Developed with care by **Yasuda**.
