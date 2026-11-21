import { useEffect, useMemo, useState } from "react";
import { DownloadSection } from "./components/DownloadSection";
import { FeaturesSection } from "./components/FeaturesSection";
import { DownloadIcon, PixelMark } from "./components/Icons";
import { Navbar } from "./components/Navbar";
import { ScreenGallery } from "./components/ScreenGallery";
import { SourceSection } from "./components/SourceSection";
import { VersionsSection } from "./components/VersionsSection";
import {
  APK_DOWNLOAD_URL,
  GITHUB_REPO,
  INITIAL_RELEASES,
  LATEST_RELEASE_URL,
  NAV_ITEMS,
  RELEASE_META,
  RELEASES_URL,
  REPO_URL
} from "./data/siteData";
import { usePageScroll } from "./hooks/useScrollEffects";

const FONT_STORAGE_KEY = "derbis-site-font";

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

  const [stars, setStars] = useState(7);
  const [apkUrl, setApkUrl] = useState(APK_DOWNLOAD_URL);
  const [releaseTag, setReleaseTag] = useState(RELEASE_META.version);
  const [sizeLabel, setSizeLabel] = useState(RELEASE_META.sizeLabel);
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
          `https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=10`,
          { headers: { Accept: "application/vnd.github+json" } }
        );
        if (!relRes.ok) return;
        const relList = await relRes.json();
        if (cancelled || !Array.isArray(relList) || !relList.length) return;

        const official = relList.filter((r) => !r.draft && !r.prerelease);
        const primary = official[0] || relList[0];
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
                YouTube Music and your own library, in one Android player
              </h1>
              <p className="hero-lede">
                Stream songs with no account, connect Navidrome or Subsonic for your FLAC collection, and switch between Apple Music 13, Pixel UI, Derbis Default, Synced lyrics, and Late Night
              </p>
              <div className="hero-ctas">
                <a className="btn btn-primary" href={apkUrl}>
                  <DownloadIcon />
                  <span>Download Derbis.apk</span>
                </a>
                <a
                  className="btn btn-secondary"
                  href={REPO_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span>View on GitHub</span>
                </a>
              </div>
            </div>

            <dl className="hero-specs" aria-label="Current release summary">
              <div className="spec-cell">
                <dt>Release</dt>
                <dd>{releaseTag}</dd>
              </div>
              <div className="spec-cell">
                <dt>Package</dt>
                <dd>{sizeLabel}</dd>
              </div>
              <div className="spec-cell">
                <dt>Target</dt>
                <dd>{RELEASE_META.androidMin}</dd>
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
              Latest APK
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
            Derbis is an independent open-source project and is not affiliated with Google, YouTube, Apple, Spotify, Discord, or Last.fm
          </p>
        </div>
      </footer>
    </div>
  );
}
