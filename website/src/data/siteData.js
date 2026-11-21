export const GITHUB_REPO = "jusnie/Derbis";
export const REPO_URL = "https://github.com/jusnie/Derbis";
export const RELEASES_URL = "https://github.com/jusnie/Derbis/releases";
export const LATEST_RELEASE_URL = "https://github.com/jusnie/Derbis/releases/tag/v1.8.0";
export const APK_DOWNLOAD_URL = "https://github.com/jusnie/Derbis/releases/download/v1.8.0/Derbis.apk";
export const SHA256_DOWNLOAD_URL = "https://github.com/jusnie/Derbis/releases/download/v1.8.0/Derbis.apk.sha256";

export const RELEASE_META = {
  version: "v1.8.0",
  versionCode: "3000215",
  packageName: "com.derbis.app",
  sizeLabel: "71.7 MB",
  androidMin: "Android 8.0+",
  publishedLabel: "October 2026",
  sha256: "9267a5efbedcb84bbf083139506ca1b61d1e7ca84e5dac02861a89ea6e3e9e77"
};

export const NAV_ITEMS = [
  { id: "screens", label: "Screens" },
  { id: "features", label: "Features" },
  { id: "download", label: "Download" },
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
    title: "Native FFmpeg decoding and audio route sheet",
    summary: "Bundled C++ Media3 FFmpeg decoder handles FLAC, ALAC, Opus, Vorbis, and EAC3 locally, paired with a 10-band EQ, loudness normalization, and live codec badge"
  },
  {
    id: "lyrics",
    index: "04",
    title: "Word-timed karaoke lyrics and canvas video",
    summary: "Follow syllable-synced lyrics inline on the player or full-screen over blurred cover art, with smooth looping artist motion backdrops when available"
  },
  {
    id: "jam",
    index: "05",
    title: "Jam rooms and universal track links",
    summary: "Start a shared Jam room code so friends stay synced on play, pause, seek, and queue edits across Android and web share links"
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

export const INITIAL_RELEASES = [
  {
    tag: "v1.8.0",
    name: "Derbis v1.8.0",
    date: "Oct 30, 2026",
    latest: true,
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
    tag: "v1.7.0",
    name: "Derbis v1.7.0",
    date: "Sep 2026",
    latest: false,
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
    name: "Derbis v1.6.0",
    date: "Aug 2026",
    latest: false,
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
  { label: "Platform", value: "Android 8.0+ (API 26 to Android 16), universal APK" },
  { label: "Client stack", value: "Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer" },
  { label: "Native audio", value: "Custom C++ JNI + FFmpeg decoder module" },
  { label: "Privacy", value: "Zero analytics SDKs, zero ad trackers, local encrypted backups" },
  { label: "Share routes", value: "derbis-web.vercel.app/t/:id and /jam/:code" }
];
