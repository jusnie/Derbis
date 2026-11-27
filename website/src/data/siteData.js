export const GITHUB_REPO = "jusnie/Derbis";
export const REPO_URL = "https://github.com/jusnie/Derbis";
export const RELEASES_URL = "https://github.com/jusnie/Derbis/releases";
export const LATEST_RELEASE_URL = "https://github.com/jusnie/Derbis/releases/tag/v1.8.0";
export const APK_DOWNLOAD_URL = "https://github.com/jusnie/Derbis/releases/download/v1.8.0/Derbis.apk";
export const SHA256_DOWNLOAD_URL = "https://github.com/jusnie/Derbis/releases/download/v1.8.0/Derbis.apk.sha256";

export const IOS_RELEASE_URL = "https://github.com/jusnie/Derbis/releases/tag/ios-unofficial-13630b6";
export const IOS_IPA_DOWNLOAD_URL = "/Derbis.ipa";
export const IOS_IPA_PUBLIC_URL = "https://derbis-web.vercel.app/Derbis.ipa";
export const IOS_SHA256_DOWNLOAD_URL = "/Derbis.ipa.sha256";

export const RELEASE_META = {
  version: "v1.8.0",
  versionCode: "3000215",
  packageName: "com.derbis.app",
  sizeLabel: "71.7 MB",
  androidMin: "Android 8.0+",
  publishedLabel: "October 2026",
  sha256: "9267a5efbedcb84bbf083139506ca1b61d1e7ca84e5dac02861a89ea6e3e9e77"
};

export const IOS_RELEASE_META = {
  version: "v1.8.0",
  buildId: "13630b6",
  upstreamTag: "ios-unofficial-13630b6",
  bundleId: "com.derbis.ios",
  sizeLabel: "3.3 MB",
  iosMin: "iOS 16.0+",
  archLabel: "arm64 SwiftUI",
  publishedLabel: "October 2026",
  sha256: "204648914c151dfadb7545362cad6b89ba6ddfe1dd2715e20338a6127c59fdbb"
};

export const UPCOMING_PLATFORMS = {
  desktop: {
    id: "desktop",
    name: "Derbis for macOS & Desktop",
    packageLabel: "macOS, Windows & Linux",
    status: "Coming soon",
    target: "macOS 14+ (Apple Silicon & Intel) · Windows 10/11 · Linux",
    arch: "Universal .dmg · Installer · AppImage / Flatpak",
    summary:
      "Unified native desktop experience for macOS, Windows, and Linux featuring bit-accurate CoreAudio, WASAPI, and PipeWire output, bundled lossless FFmpeg decoding, menu bar & tray mini-players, Discord Rich Presence, and live Jam sync.",
    highlights: [
      {
        step: "01",
        title: "Native macOS, Windows & Linux audio output",
        detail: "Low-overhead CoreAudio on Apple Silicon/Intel plus WASAPI and PipeWire routing with bundled FFmpeg FLAC, ALAC, and Opus decoding"
      },
      {
        step: "02",
        title: "Menu bar & system tray mini-player",
        detail: "Global media keys, compact menu bar/tray transport controls, full-window karaoke lyrics, and all nine Derbis skins adapted for widescreen"
      },
      {
        step: "03",
        title: "Navidrome, Discord Presence & live Jam sync",
        detail: "Share the same self-hosted library, Last.fm scrobbling, Discord Rich Presence, and real-time Jam rooms with Android and iOS"
      }
    ]
  }
};

export const KSIGN_LINKS = {
  siteUrl: "https://ksignios.app/",
  certUrl: "https://ksignios.app/ksign-certificate/",
  dnsProfileUrl: "https://ksignios.app/downloads/ksign-dns.mobileconfig",
  ksignIpaUrl: "https://ksignios.app/downloads/Ksign-v1.6.ipa",
  heroImage: "https://ksignios.app/assets/hero-signing.webp?v=webp30"
};

export const NAV_ITEMS = [
  { id: "screens", label: "Screens" },
  { id: "features", label: "Features" },
  { id: "download", label: "Download" },
  { id: "ksign", label: "iOS & KSign" },
  { id: "versions", label: "Versions" },
  { id: "source", label: "Source" }
];

export const APP_SCREENS = [
  {
    id: "apple-music-13",
    index: "01",
    title: "Apple Music 13",
    category: "Player",
    caption: "Full-cover stage, inline lyric, and codec badge",
    detail: "Full-bleed album artwork with live inline lyrics, centered stream quality badge, condensed volume bar, and four-action bottom dock",
    src: "/screens/apple-music-13.jpg",
    width: 720,
    height: 1612,
    stageBg: "#0a0a0c",
    alt: "Derbis Apple Music 13 player screen showing full-bleed monochrome cover art, active lyric line, AAC badge, and transport controls"
  },
  {
    id: "pixel-ui",
    index: "02",
    title: "Pixel UI",
    category: "Skin",
    caption: "Graph-paper home, Pixelify type, and square dock",
    detail: "Warm blush graph-paper stage, square hard-shadow cards, Pixelify typography, mood chips, Your orbit shelf, and four-tab pixel navigation",
    src: "/screens/pixel-ui-full.jpg",
    width: 720,
    height: 1612,
    stageBg: "#ffedf1",
    alt: "Derbis Pixel UI home screen with blush graph-paper stage, My soundtrack card, Your orbit album shelf, and square pixel tab bar"
  },
  {
    id: "derbis-default",
    index: "03",
    title: "Derbis Default",
    category: "Skin",
    caption: "Personal radio, mood chips, and Your orbit grid",
    detail: "AMOLED dark home surface with personal radio hero, quick-filter genre chips, three-column Your orbit rotation grid, and translucent top controls",
    src: "/screens/derbis-default.jpg",
    width: 720,
    height: 1612,
    stageBg: "#070709",
    alt: "Derbis Default dark home screen showing personal radio hero, genre filter row, and three-by-three Your orbit album grid"
  },
  {
    id: "synced-lyrics",
    index: "04",
    title: "Synced lyrics",
    category: "Lyrics",
    caption: "Word-timed lines over blurred cover artwork",
    detail: "Full-screen Apple Music 13 lyrics surface with sharp active vocals, soft-focus upcoming lines, floating quick actions, and direct scrub bar",
    src: "/screens/synced-lyrics.jpg",
    width: 720,
    height: 1612,
    stageBg: "#121215",
    alt: "Derbis full-screen synced lyrics view with sharp active vocal line and soft-blurred upcoming lyrics over cover artwork"
  },
  {
    id: "late-night",
    index: "05",
    title: "Late Night",
    category: "Player",
    caption: "Songwriter credits, next-track peek, and lyric bar",
    detail: "Sleeve-led Now Playing deck displaying overlapping artist avatars, songwriter credit header, peeking next queue card, and bottom lyric preview",
    src: "/screens/late-night.jpg",
    width: 720,
    height: 1612,
    stageBg: "#0b0b0e",
    alt: "Derbis Late Night player deck showing artist avatars, Written by credits, square cover art with next-card peek, and bottom lyric strip"
  },
  {
    id: "pixel-ui-shelves",
    index: "06",
    title: "Pixel UI shelves",
    category: "Skin",
    caption: "Hard-shadow cards and Derbis Collections",
    detail: "Borderless Pixel UI view highlighting square album sleeves, rose play controls, and curated Derbis Collections cards",
    src: "/screens/pixel-ui.jpg",
    width: 720,
    height: 1511,
    stageBg: "#ffedf1",
    alt: "Derbis Pixel UI shelves showing Your orbit square covers and Charting now collection card"
  }
];

export const CAPABILITIES = [
  {
    id: "skins",
    index: "01",
    title: "Nine skins and seven player decks",
    summary: "Switch between Derbis Default, Pixel UI, Liquid Glass, Torque, Ivory, and service-inspired skins, or pick Apple Music 13, Late Night, Sleeve, Frost, Vinyl, and Flow"
  },
  {
    id: "library",
    index: "02",
    title: "YouTube Music plus Navidrome and Subsonic",
    summary: "Search and stream songs with no account required, or connect your self-hosted Navidrome, Subsonic, or Jellyfin server alongside local FLAC and ALAC folders"
  },
  {
    id: "audio",
    index: "03",
    title: "Lossless decoding and real-time audio DSP",
    summary: "Bundled C++ Media3 FFmpeg decoder on Android and native AVPlayer + MTAudioProcessingTap EQ/spatial DSP on iOS, paired with loudness normalization and live codec badges"
  },
  {
    id: "lyrics",
    index: "04",
    title: "Word-timed karaoke lyrics and canvas video",
    summary: "Follow syllable-synced lyrics inline on the player or full-screen over blurred cover art, with romanization, translation, and smooth looping motion canvas backdrops"
  },
  {
    id: "jam",
    index: "05",
    title: "Jam rooms and universal track links",
    summary: "Start a shared Jam room code so friends stay synced on play, pause, seek, and queue edits across Android, iOS, and web share links"
  },
  {
    id: "presence",
    index: "06",
    title: "Discord Rich Presence and Last.fm scrobbling",
    summary: "Publish live artwork, timestamps, and listening status to Discord and scrobble plays to Last.fm or ListenBrainz with offline retry queuing",
    brands: ["discord", "lastfm"]
  }
];

export const INSTALL_STEPS = [
  {
    step: "01",
    title: "Download Derbis.apk",
    detail: "Fetch the signed universal release package directly from GitHub Releases"
  },
  {
    step: "02",
    title: "Verify SHA-256 digest",
    detail: "Compare sha256sum Derbis.apk against the published checksum below before installing"
  },
  {
    step: "03",
    title: "Open and allow install",
    detail: "Android 8.0 and newer prompt once to allow installation from your browser or file manager"
  }
];

export const IOS_INSTALL_STEPS = [
  {
    step: "01",
    title: "Install KSign & Anti-Revoke DNS",
    detail: "Install the KSign DNS profile first, direct-install KSign on your iPhone, and trust the enterprise profile in Settings"
  },
  {
    step: "02",
    title: "Import KSign certificate pair",
    detail: "Extract an active .p12 and .mobileprovision package from ksignios.app/ksign-certificate/ into KSign → Settings → Certificates"
  },
  {
    step: "03",
    title: "Sign & install Derbis.ipa",
    detail: "Download Derbis.ipa, import it into KSign Library, tap Sign & Install with your certificate, and confirm the iOS prompt"
  }
];

export const KSIGN_GUIDE_PHASES = [
  {
    id: "install-ksign",
    phase: "Part 01",
    title: "Install KSign & Anti-Revoke DNS on iPhone or iPad",
    subtitle: "Set up KSign once on iOS / iPadOS 16.0+ with no computer and no jailbreak required",
    actions: [
      {
        label: "1. Anti-Revoke DNS (.mobileconfig)",
        href: "https://ksignios.app/downloads/ksign-dns.mobileconfig",
        primary: true
      },
      {
        label: "2. Open KSign Direct Install",
        href: "https://ksignios.app/",
        external: true
      },
      {
        label: "KSign v1.6.ipa",
        href: "https://ksignios.app/downloads/Ksign-v1.6.ipa"
      }
    ],
    steps: [
      {
        id: "dns-profile",
        step: "01",
        badge: "Required first",
        title: "Install Anti-Revoke DNS profile",
        detail: "Download ksign-dns.mobileconfig on Safari, then open Settings → General → VPN & Device Management, tap the downloaded profile, and install it before installing KSign so Apple certificate revokes are blocked.",
        pathHint: "Settings → General → VPN & Device Management → DNS Profile",
        img: "https://ksignios.app/assets/home-guide/setup-dns.webp",
        alt: "KSign iOS setup step 1 showing Anti-Revoke DNS profile installation inside iOS VPN and Device Management settings"
      },
      {
        id: "direct-install",
        step: "02",
        badge: "Direct OTA",
        title: "Tap a KSign Direct Install link",
        detail: "Open ksignios.app in Safari, tap one of the active Direct Install mirrors (Rubber 1/2 or Rakuten Securities 1/2), and tap Install on the system prompt. KSign will download onto your Home Screen.",
        pathHint: "Safari → ksignios.app → Direct Install → Install",
        img: "https://ksignios.app/assets/home-guide/setup-install.webp",
        alt: "KSign iOS setup step 2 showing Direct Install prompt in Safari on iPhone"
      },
      {
        id: "trust-profile",
        step: "03",
        badge: "Enterprise trust",
        title: "Trust the Enterprise Developer certificate",
        detail: "Before opening KSign for the first time, return to Settings → General → VPN & Device Management, select the Enterprise App developer profile, and tap Trust → Trust.",
        pathHint: "Settings → General → VPN & Device Management → Trust",
        img: "https://ksignios.app/assets/home-guide/setup-trust.webp?v=webp30",
        alt: "KSign iOS setup step 3 showing trusting the enterprise developer profile in iOS Settings"
      }
    ]
  },
  {
    id: "import-cert",
    phase: "Part 02",
    title: "Download & import a KSign certificate pair",
    subtitle: "Grab a matching .p12 signing certificate and .mobileprovision file so KSign can sign IPA packages on-device",
    actions: [
      {
        label: "Get Active Certificates (.p12 + .mobileprovision)",
        href: "https://ksignios.app/ksign-certificate/",
        primary: true,
        external: true
      }
    ],
    steps: [
      {
        id: "cert-extract",
        step: "04",
        badge: "Files app",
        title: "Download & extract certificate ZIP",
        detail: "Visit ksignios.app/ksign-certificate/, download an active certificate archive (such as Rakuten, HSBC, or Central Power), open the iOS Files app → Downloads, and tap the .zip once to extract the .p12, .mobileprovision, and password.",
        pathHint: "Files → Downloads → Tap .zip to extract folder",
        img: "https://ksignios.app/assets/certificate-save-files-v2.webp?v=webp30",
        alt: "KSign certificate guide step showing extracting the certificate ZIP archive inside iOS Files"
      },
      {
        id: "cert-open",
        step: "05",
        badge: "KSign settings",
        title: "Open KSign Certificates & tap +",
        detail: "Launch KSign, open the Settings tab, tap Certificates, and tap the + button in the top-right corner to add a signing identity.",
        pathHint: "KSign → Settings → Certificates → +",
        img: "https://ksignios.app/assets/certificate-open-certificates-v2.webp?v=webp30",
        alt: "KSign certificate guide step showing Settings to Certificates screen and the plus button"
      },
      {
        id: "cert-pair",
        step: "06",
        badge: "Matching pair",
        title: "Import matching .p12 & .mobileprovision",
        detail: "Tap Import Certificate File to choose the .p12 file and Import Provisioning File to choose the .mobileprovision file from the exact same extracted folder (never mix files from two different archives).",
        pathHint: "Import Certificate (.p12) + Import Provisioning (.mobileprovision)",
        img: "https://ksignios.app/assets/certificate-import-pair-v2.webp?v=webp30",
        alt: "KSign certificate guide step showing selecting matching p12 and mobileprovision files"
      },
      {
        id: "cert-save",
        step: "07",
        badge: "Activate signing",
        title: "Enter certificate password & Save",
        detail: "Type the password included on ksignios.app/ksign-certificate/ (or inside the extracted password text file) and tap Save. Your certificate will now show as active in KSign.",
        pathHint: "Enter Password → Save Identity",
        img: "https://ksignios.app/assets/certificate-save-identity-v3.webp?v=webp30",
        alt: "KSign certificate guide step showing entering certificate password and tapping Save"
      }
    ]
  },
  {
    id: "sign-derbis",
    phase: "Part 03",
    title: "Import, sign & install Derbis.ipa",
    subtitle: "Derbis for iOS is built as a clean single-bundle arm64 app with zero restricted entitlements so KSign signs Derbis.ipa cleanly on any certificate",
    useIpaAction: true,
    steps: [
      {
        id: "ipa-import",
        step: "08",
        badge: "Import IPA",
        title: "Import Derbis.ipa to KSign Library",
        detail: "Download Derbis.ipa to your iPhone, open KSign → Files tab, tap + to pick Derbis.ipa from Downloads, then tap the imported file and choose Import to Library.",
        pathHint: "KSign → Files → + → Tap Derbis.ipa → Import to Library",
        img: "https://ksignios.app/assets/home-guide/ipa-import.webp",
        alt: "KSign IPA guide step 1 showing importing an IPA file into KSign Library"
      },
      {
        id: "ipa-library",
        step: "09",
        badge: "Prepare app",
        title: "Open Library & choose Sign & Install",
        detail: "Switch to KSign's Library tab under Downloaded Apps, tap Derbis, and select Sign & Install to open the signing sheet.",
        pathHint: "KSign → Library → Downloaded Apps → Derbis → Sign & Install",
        img: "https://ksignios.app/assets/home-guide/ipa-library.webp",
        alt: "KSign IPA guide step 2 showing selecting the imported app in KSign Library and tapping Sign and Install"
      },
      {
        id: "ipa-sign",
        step: "10",
        badge: "Install Derbis",
        title: "Start Signing & confirm iOS Install",
        detail: "Verify your active certificate is selected under Signing, tap Start Signing, wait a few seconds for re-signing to finish, and tap Install on the iOS popup. Derbis is ready on your Home Screen.",
        pathHint: "Select Certificate → Start Signing → Tap Install",
        img: "https://ksignios.app/assets/home-guide/ipa-sign.webp",
        alt: "KSign IPA guide step 3 showing Start Signing button and confirming iOS installation"
      }
    ]
  }
];

export const INITIAL_RELEASES = [
  {
    tag: "v1.8.0",
    displayTag: "v1.8.0",
    name: "Derbis v1.8.0 (Android)",
    channel: "Official Android",
    date: "Oct 30, 2026",
    latest: true,
    prerelease: false,
    downloads: null,
    sizeLabel: "71.7 MB",
    url: "https://github.com/jusnie/Derbis/releases/tag/v1.8.0",
    apkUrl: "https://github.com/jusnie/Derbis/releases/download/v1.8.0/Derbis.apk",
    groups: [
      {
        heading: "Skins and player decks",
        items: [
          "Added the full Pixel UI skin with warm blush graph-paper surfaces, square geometry, and Pixelify typography",
          "Refined Apple Music 13 inline word-timed lyrics, condensed audio codec badge, and smooth motion canvas transitions",
          "Polished Late Night deck songwriter credit header and next-track queue card peek"
        ]
      },
      {
        heading: "Audio and integrations",
        items: [
          "Locked seek-row codec slot width so elapsed and remaining timestamps never shift during track transitions",
          "Updated Discord Rich Presence, Last.fm scrobbling queue, and universal /t/ and /jam/ deep-link handoff",
          "Signed universal APK release with published SHA-256 verification file"
        ]
      }
    ]
  },
  {
    tag: "ios-unofficial-13630b6",
    displayTag: "v1.8.0-beta · iOS",
    name: "Derbis v1.8.0 Beta for iOS",
    channel: "iOS Beta",
    date: "Oct 30, 2026",
    latest: false,
    prerelease: true,
    downloads: null,
    sizeLabel: "3.3 MB",
    url: "https://github.com/jusnie/Derbis/releases/tag/ios-unofficial-13630b6",
    ipaUrl: "/Derbis.ipa",
    groups: [
      {
        heading: "Derbis Studio UI overhaul",
        items: [
          "Linear Glass is now the default skin, joined by Derbis Default, Pixel, Torque, Ivory, Late Night, Liquid Glass, and Apple Music 13 (7 skins total)",
          "Six Now Playing decks — Flow, Vinyl, Sleeve, Frost, Pixel, and Classic 13 — with morphing squircle, pixel, and chamfered transport buttons plus a swipeable mini-player dock",
          "Studio Home with Endless Personal Radio hero, 2-column Speed Dial, 3x3 Orbit grid, and a horizontal visual carousel shelf"
        ]
      },
      {
        heading: "Endless Listening and catalogs",
        items: [
          "Persistent RDAMVM song-radio sessions with rolling refills, infinite queue scroll, Similar Songs, and Artist Radio",
          "Artist and Album studio pages with top tracks, releases, and related artists backed by InnerTube catalog lookups",
          "Listening Receipt cards (Receipt, Linear Story, Pixel Stamp) with in-app receipt rendering and sharing"
        ]
      },
      {
        heading: "Real-time DSP and lyrics",
        items: [
          "Lock-free MTAudioProcessingTap 5-band biquad EQ, Bass Boost, Spatial Widener, Loudness Normalization, and Crossfade engine",
          "120Hz karaoke word-by-word synced lyrics with multi-script romanization, translation toggle, and calibration offset",
          "100% native arm64 Swift + SwiftUI + AVPlayer build (iOS 16.0+) packaged as Derbis.ipa for direct KSign, SideStore, and AltStore installation"
        ]
      }
    ]
  },
  {
    tag: "v1.7.0",
    displayTag: "v1.7.0",
    name: "Derbis v1.7.0",
    channel: "Official Android",
    date: "Sep 2026",
    latest: false,
    prerelease: false,
    downloads: null,
    sizeLabel: "70.4 MB",
    url: "https://github.com/jusnie/Derbis/releases",
    apkUrl: null,
    groups: [
      {
        heading: "Playback and library",
        items: [
          "Expanded Navidrome and Subsonic album browsing, offline pinning, and local folder scanner",
          "Improved word-by-word lyric timing engine and calibration offset controls"
        ]
      }
    ]
  },
  {
    tag: "v1.6.0",
    displayTag: "v1.6.0",
    name: "Derbis v1.6.0",
    channel: "Official Android",
    date: "Aug 2026",
    latest: false,
    prerelease: false,
    downloads: null,
    sizeLabel: "68.9 MB",
    url: "https://github.com/jusnie/Derbis/releases",
    apkUrl: null,
    groups: [
      {
        heading: "Jam rooms and audio engine",
        items: [
          "Introduced live Jam session room codes with synchronized queue and seek state",
          "Added bundled Media3 FFmpeg native audio renderer for lossless codecs"
        ]
      }
    ]
  }
];

export const SOURCE_FACTS = [
  { label: "License", value: "GPL-3.0-only with REUSE-compliant SPDX headers" },
  { label: "Mobile", value: "Android 8.0+ (Derbis.apk · Official) · iOS / iPadOS 16.0+ (Derbis.ipa · Beta)" },
  { label: "Desktop", value: "macOS (Apple Silicon & Intel) · Windows & Linux — Coming soon" },
  { label: "Android stack", value: "Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer + C++ FFmpeg" },
  { label: "iOS stack", value: "Swift 5, SwiftUI, AVPlayer, lock-free MTAudioProcessingTap DSP" },
  { label: "Privacy", value: "Zero analytics SDKs, zero ad trackers, local encrypted backups" },
  { label: "Share routes", value: "derbis-web.vercel.app/t/:id and /jam/:code" }
];
