import { useEffect, useMemo, useState } from "react";
import { DownloadSection } from "./components/DownloadSection";
import { FeaturesSection } from "./components/FeaturesSection";
import {
  AndroidIcon,
  AppleIcon,
  ChevronToggleIcon,
  LaptopIcon,
  PixelMark
} from "./components/Icons";
import { KSignGuideSection } from "./components/KSignGuideSection";
import { Navbar } from "./components/Navbar";
import { ScreenGallery } from "./components/ScreenGallery";
import { SourceSection } from "./components/SourceSection";
import { VersionsSection } from "./components/VersionsSection";
import {
  APK_DOWNLOAD_URL,
  GITHUB_REPO,
  INITIAL_RELEASES,
  IOS_IPA_DOWNLOAD_URL,
  IOS_RELEASE_META,
  IOS_RELEASE_URL,
  LATEST_RELEASE_URL,
  NAV_ITEMS,
  RELEASE_META,
  RELEASES_URL,
  REPO_URL
} from "./data/siteData";
import { usePageScroll } from "./hooks/useScrollEffects";

const FONT_STORAGE_KEY = "derbis-site-font";

function detectDefaultPlatform() {
  if (typeof navigator === "undefined") return "android";
  const ua = navigator.userAgent || "";
  if (/iPhone|iPad|iPod/i.test(ua)) return "ios";
  if (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1) return "ios";
  return "android";
}

export default function App() {
  const sectionIds = useMemo(() => NAV_ITEMS.map((item) => item.id), []);
  const { progress, activeSection } = usePageScroll(sectionIds);

  const [fontMode, setFontMode] = useState(() => {
    try {
      const saved = window.localStorage.getItem(FONT_STORAGE_KEY);
      return saved === "sfpro" ? "sfpro" : "pixel";
    } catch {
      return "pixel";
    }
  });

  const [platform, setPlatform] = useState(detectDefaultPlatform);
  const [heroVersionsOpen, setHeroVersionsOpen] = useState(false);
  const [stars, setStars] = useState(7);
  const [apkUrl, setApkUrl] = useState(APK_DOWNLOAD_URL);
  const [releaseTag, setReleaseTag] = useState(RELEASE_META.version);
  const [sizeLabel, setSizeLabel] = useState(RELEASE_META.sizeLabel);

  const [ipaUrl, setIpaUrl] = useState(IOS_IPA_DOWNLOAD_URL);
  const [iosVersion, setIosVersion] = useState(IOS_RELEASE_META.version);
  const [iosReleaseUrl, setIosReleaseUrl] = useState(IOS_RELEASE_URL);
  const [iosSizeLabel, setIosSizeLabel] = useState(IOS_RELEASE_META.sizeLabel);
  const [iosSha256, setIosSha256] = useState(IOS_RELEASE_META.sha256);

  const [totalDownloads, setTotalDownloads] = useState(null);
  const [releases, setReleases] = useState(INITIAL_RELEASES);

  useEffect(() => {
    document.documentElement.dataset.font = fontMode;
    try {
      window.localStorage.setItem(FONT_STORAGE_KEY, fontMode);
    } catch {}
  }, [fontMode]);

  useEffect(() => {
    let cancelled = false;

    const loadGitHubMetadata = async () => {
      try {
        const repoRes = await fetch(`https://api.github.com/repos/${GITHUB_REPO}`, {
          headers: { Accept: "application/vnd.github+json" }
        });
        if (repoRes.ok) {
          const repoJson = await repoRes.json();
          if (!cancelled && typeof repoJson.stargazers_count === "number") {
            setStars(repoJson.stargazers_count);
          }
        }
      } catch {}

      try {
        const relRes = await fetch(
          `https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=30`,
          { headers: { Accept: "application/vnd.github+json" } }
        );
        if (!relRes.ok) return;
        const relList = await relRes.json();
        if (cancelled || !Array.isArray(relList) || !relList.length) return;

        const byPublishedDesc = (a, b) =>
          (Date.parse(b.published_at || b.created_at || "") || 0) -
          (Date.parse(a.published_at || a.created_at || "") || 0);

        const official = relList
          .filter((r) => !r.draft && !r.prerelease)
          .sort(byPublishedDesc);
        const primary = official[0];
        if (primary) {
          const apkAsset = (primary.assets || []).find(
            (a) => typeof a.name === "string" && a.name.toLowerCase().endsWith(".apk")
          );
          if (primary.tag_name) setReleaseTag(primary.tag_name);
          if (apkAsset?.browser_download_url) setApkUrl(apkAsset.browser_download_url);
          if (typeof apkAsset?.size === "number") {
            setSizeLabel(`${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB`);
          }
        }

        const iosReleases = relList
          .filter(
            (r) =>
              !r.draft &&
              typeof r.tag_name === "string" &&
              r.tag_name.startsWith("ios-") &&
              (r.assets || []).some(
                (a) => typeof a.name === "string" && a.name.toLowerCase().endsWith(".ipa")
              )
          )
          .sort(byPublishedDesc);
        const iosRel = iosReleases[0];
        if (iosRel) {
          const ipaAsset = (iosRel.assets || []).find(
            (a) => typeof a.name === "string" && a.name.toLowerCase().endsWith(".ipa")
          );
          if (iosRel.tag_name && iosRel.tag_name !== IOS_RELEASE_META.upstreamTag) {
            const semverMatch = iosRel.tag_name.match(/v\d+\.\d+\.\d+/i);
            if (semverMatch) setIosVersion(semverMatch[0]);
            setIosSha256("");
          }
          if (iosRel.html_url) setIosReleaseUrl(iosRel.html_url);
          if (
            ipaAsset?.browser_download_url &&
            typeof ipaAsset.name === "string" &&
            ipaAsset.name.toLowerCase() === "derbis.ipa"
          ) {
            setIpaUrl(ipaAsset.browser_download_url);
          }
          if (typeof ipaAsset?.size === "number") {
            setIosSizeLabel(`${(ipaAsset.size / (1024 * 1024)).toFixed(1)} MB`);
          }
        }

        let downloadSum = 0;
        const byTag = new Map();
        relList.forEach((r) => {
          const count = (r.assets || []).reduce(
            (acc, asset) => acc + (typeof asset.download_count === "number" ? asset.download_count : 0),
            0
          );
          downloadSum += count;
          if (r.tag_name) {
            byTag.set(r.tag_name, count);
          }
        });

        setTotalDownloads(downloadSum);
        setReleases((prev) =>
          prev.map((entry) =>
            byTag.has(entry.tag) ? { ...entry, downloads: byTag.get(entry.tag) } : entry
          )
        );
      } catch {}
    };

    loadGitHubMetadata();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div id="top" className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <Navbar
        scrollProgress={progress}
        activeSection={activeSection}
        stars={stars}
        fontMode={fontMode}
        onFontChange={setFontMode}
      />

      <main id="main-content">
        <section className="hero-section" aria-labelledby="hero-heading">
          <div className="wrap hero-inner">
            <div className="hero-copy">
              <h1 id="hero-heading" className="hero-title">
                YouTube Music and your own library, on Android and iOS
              </h1>
              <p className="hero-lede">
                Stream songs with no account, connect Navidrome or Subsonic for your FLAC collection, and switch between Apple Music 13, Pixel UI, Derbis Default, Synced lyrics, and Late Night
              </p>
              <div className="hero-cta-wrap">
                <div
                  className={
                    heroVersionsOpen ? "version-bar is-open" : "version-bar"
                  }
                >
                  <a className="version-bar-main" href={apkUrl}>
                    <AndroidIcon />
                    <span>Download Derbis.apk</span>
                  </a>

                  <div
                    id="hero-other-versions"
                    className="version-bar-drawer"
                    aria-hidden={!heroVersionsOpen}
                  >
                    <div className="version-bar-drawer-inner">
                      <a
                        className="version-chip"
                        href={ipaUrl}
                        download="Derbis.ipa"
                        tabIndex={heroVersionsOpen ? 0 : -1}
                      >
                        <AppleIcon />
                        <span>Derbis.ipa</span>
                        <span className="platform-pill platform-pill-live">Beta</span>
                      </a>

                      <a
                        className="version-chip version-chip-soon"
                        href="#download"
                        tabIndex={heroVersionsOpen ? 0 : -1}
                        onClick={() => setPlatform("desktop")}
                      >
                        <LaptopIcon />
                        <span>macOS &amp; Desktop</span>
                        <span className="platform-pill platform-pill-soon">
                          Coming soon
                        </span>
                      </a>
                    </div>
                  </div>

                  <button
                    type="button"
                    className="version-bar-toggle"
                    aria-expanded={heroVersionsOpen}
                    aria-controls="hero-other-versions"
                    aria-label={
                      heroVersionsOpen
                        ? "Hide other versions"
                        : "Show iOS, macOS, and Desktop versions"
                    }
                    title={
                      heroVersionsOpen
                        ? "Hide other versions"
                        : "Show other versions"
                    }
                    onClick={() => setHeroVersionsOpen((prev) => !prev)}
                  >
                    <ChevronToggleIcon open={heroVersionsOpen} />
                  </button>
                </div>

                <div
                  className={
                    heroVersionsOpen
                      ? "hero-sub-hint is-open"
                      : "hero-sub-hint"
                  }
                >
                  <div className="hero-sub-hint-inner">
                    <a
                      href="#ksign"
                      tabIndex={heroVersionsOpen ? 0 : -1}
                      onClick={() => setPlatform("ios")}
                    >
                      Installing Derbis.ipa on iPhone or iPad? Open the KSign visual guide &rarr;
                    </a>
                  </div>
                </div>
              </div>
            </div>

            <dl className="hero-specs" aria-label="Platform release summary">
              <div className="spec-cell">
                <dt>Android · Official</dt>
                <dd>{releaseTag} · {sizeLabel}</dd>
              </div>
              <div className="spec-cell">
                <dt>iOS · Beta</dt>
                <dd>Derbis.ipa · {iosSizeLabel}</dd>
              </div>
              <div className="spec-cell">
                <dt>macOS &amp; Desktop</dt>
                <dd className="spec-soon">Coming soon</dd>
              </div>
              <div className="spec-cell">
                <dt>License</dt>
                <dd>GPL-3.0-only</dd>
              </div>
            </dl>
          </div>

          <ScreenGallery />
        </section>

        <FeaturesSection />
        <DownloadSection
          apkUrl={apkUrl}
          releaseTag={releaseTag}
          sizeLabel={sizeLabel}
          totalDownloads={totalDownloads}
          ipaUrl={ipaUrl}
          iosVersion={iosVersion}
          iosReleaseUrl={iosReleaseUrl}
          iosSizeLabel={iosSizeLabel}
          iosSha256={iosSha256}
          platform={platform}
          onPlatformChange={setPlatform}
        />
        <KSignGuideSection
          ipaUrl={ipaUrl}
          iosVersion={iosVersion}
          iosReleaseUrl={iosReleaseUrl}
          iosSizeLabel={iosSizeLabel}
        />
        <VersionsSection releases={releases} />
        <SourceSection stars={stars} />
      </main>

      <footer className="site-footer">
        <div className="wrap footer-inner">
          <div className="footer-brand">
            <PixelMark size={16} />
            <span>Derbis {releaseTag} · GPL-3.0-only</span>
          </div>

          <div className="footer-links">
            <a href={LATEST_RELEASE_URL} target="_blank" rel="noopener noreferrer">
              Derbis.apk
            </a>
            <a href={ipaUrl} download="Derbis.ipa">
              Derbis.ipa
            </a>
            <a href="#ksign">
              KSign Guide
            </a>
            <a href={RELEASES_URL} target="_blank" rel="noopener noreferrer">
              Releases
            </a>
            <a href={REPO_URL} target="_blank" rel="noopener noreferrer">
              GitHub
            </a>
            <a href={`${REPO_URL}/issues`} target="_blank" rel="noopener noreferrer">
              Issues
            </a>
          </div>
        </div>
        <div className="wrap">
          <p className="footer-note">
            Derbis is an independent open-source project and is not affiliated with Google, YouTube, Apple, Spotify, Discord, Last.fm, or KSign
          </p>
        </div>
      </footer>
    </div>
  );
}
