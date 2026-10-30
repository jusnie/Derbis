# Building Derbis iOS IPA Packages (Sideloadly, AltStore & Free Sideloading)

## Overview

Derbis now includes a dedicated cloud build pipeline in GitHub Actions (`.github/workflows/build-ios-ipa.yml`) that compiles the Derbis iOS package and produces ready-to-sideload `.ipa` files without requiring a personal Mac or a paid ($99/year) Apple Developer account.

---

## How It Works in GitHub Actions

```
[ Push to main or Manual Workflow Dispatch ]
                    │
                    ▼
       ┌─────────────────────────┐
       │ macOS-14 GitHub Runner  │
       │ (Xcode 15/16 Preloaded) │
       └────────────┬────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
       │ Capacitor iOS Build     │
       │ • Audio background mode │
       │ • Lock screen controls  │
       │ • Beat Sync visualizer  │
       └────────────┬────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
       │ xcodebuild (Ad-Hoc)     │
       │ CODE_SIGNING_ALLOWED=NO │
       │ CODE_SIGNING_REQUIRED=NO│
       └────────────┬────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
       │ Package Payload (.ipa)  │
       │ • Derbis.ipa            │
       │ • Derbis-unsigned.ipa   │
       └────────────┬────────────┘
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
┌──────────────────┐  ┌──────────────────┐
│ Sideloadly /     │  │ Optional Signing │
│ AltStore /       │  │ (zsign + .p12)   │
│ SideStore        │  │ Derbis-signed.ipa│
└──────────────────┘  └──────────────────┘
```

---

## Supported Devices: iPhone 12 and Newer (iOS 16.0+)

The package is strictly optimized for **iPhone 12 and above** (iPhone 12 mini, iPhone 12, 12 Pro, 12 Pro Max, iPhone 13, iPhone 14, iPhone 15, and iPhone 16 series):
- **Architecture:** 64-bit ARM64 (A14 Bionic and newer Apple Silicon)
- **Minimum OS:** iOS 16.0+
- **Display Engine:** Super Retina XDR OLED full-bleed layout (`UIRequiresFullScreen: true`)
- **ProMotion 120Hz:** `CADisableMinimumFrameDurationOnPhone: true` (unlocks smooth 120fps on Pro models)
- **Hardware Acceleration:** Metal 3 GPU shaders for J.A.R.V.I.S. 3D and liquid visualizers
- **Background Audio:** Apple `AVAudioSessionCategoryPlayback` with Lock Screen & Dynamic Island controls

---

## 3 Ways to Sideload Derbis on iPhone

### Option 1: Sideloadly (Recommended — 100% Free on PC & Mac)
1. Download Sideloadly on Windows or Mac from [sideloadly.io](https://sideloadly.io/).
2. Download `Derbis.ipa` directly from Google Drive:
   👉 **[Download Derbis.ipa (iPhone 12+)](https://drive.google.com/file/d/1hucL-P0hiR2mkccxsT9hzB_Ik9kT4w6K/view?usp=sharing)**
3. Plug your iPhone into your computer via USB (first time only).
4. Drag and drop `Derbis.ipa` into Sideloadly.
5. Enter your personal (free) Apple ID and click **Start**.
6. On iPhone: Go to **Settings → General → VPN & Device Management**, tap your Apple ID, and tap **Trust**.
7. **Done!** Sideloadly can automatically refresh the 7-day certificate in the background over home Wi-Fi.

### Option 2: SideStore / AltStore (Direct On-Device Refresh)
* Import `Derbis.ipa` into SideStore or AltStore.
* The app signs the package using your free Apple ID and refreshes it without ever connecting to a PC again.

### Option 3: TrollStore (iOS 14.0 – 17.0)
* Open `Derbis.ipa` directly in TrollStore on compatible devices for **permanent installation with zero certificate expiration**.

---

## Optional: 1-Year Signed IPA via Community Certificate

If you have a `.p12` certificate and provisioning profile (from services like MapleSign, Kravasign, Signulous, or an Apple Developer account):

1. Go to your GitHub Repository **Settings → Secrets and variables → Actions**.
2. Add the following secrets:
   - `IOS_P12_BASE64`: Base64 encoded `.p12` file (`base64 -i cert.p12`).
   - `IOS_P12_PASSWORD`: The password for your `.p12`.
   - `IOS_MOBILEPROVISION_BASE64`: Base64 encoded `.mobileprovision` file.
3. The GitHub Actions workflow will automatically use [`zhlynn/zsign`](https://github.com/zhlynn/zsign) to output a **`Derbis-signed.ipa`** ready to install in one click without 7-day limits.

---

## File Deliverables

* **GitHub Actions Workflow**: `.github/workflows/build-ios-ipa.yml`
* **Capacitor Configuration**: `website/capacitor.config.json`
* **Download Banner Buttons**: `website/src/components/ApkDownloadBanner.jsx` (with Android APK and iOS Sideloadly buttons)
