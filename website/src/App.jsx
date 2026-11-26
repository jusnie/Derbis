import {
  memo,
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState
} from "react";
import { DownloadSection } from "./components/DownloadSection";
import { FeaturesSection } from "./components/FeaturesSection";
import {
  AndroidIcon,
  AppleIcon,
  ArrowRightIcon,
  ChevronToggleIcon,
  LaptopIcon,
  PixelMark,
  ShieldIcon,
  WindowsIcon
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
import { useActiveSection } from "./hooks/useScrollEffects";

const FONT_STORAGE_KEY = "derbis-site-font";

function detectDefaultPlatform() {
  if (typeof navigator === "undefined") return "android";
  const ua = navigator.userAgent || "";
  if (/iPhone|iPad|iPod/i.test(ua)) return "ios";
  if (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1) return "ios";
  return "android";
}

const HeroVersionBar = memo(function HeroVersionBar({
  apkUrl,
  ipaUrl,
  onSelectPlatform
}) {
  const [open, setOpen] = useState(false);
  const panelRef = useRef(null);
  const innerRef = useRef(null);

  useLayoutEffect(() => {
    const panel = panelRef.current;
    const inner = innerRef.current;
    if (!panel || !inner) return undefined;

    const syncHeight = () => {
      panel.style.setProperty("--panel-h", `${inner.offsetHeight}px`);
    };

    syncHeight();

    if (typeof ResizeObserver === "undefined") {
      window.addEventListener("resize", syncHeight, { passive: true });
      return () => window.removeEventListener("resize", syncHeight);
    }

    const observer = new ResizeObserver(syncHeight);
    observer.observe(inner);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!open) return undefined;

    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        setOpen(false);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div className="hero-cta-wrap">
      <div className={open ? "version-bar is-open" : "version-bar"}>
        <div className="version-bar-top">
          <a className="version-bar-main" href={apkUrl}>
            <AndroidIcon />
            <span>Download Derbis.apk</span>
          </a>

          <button
            type="button"
            className="version-bar-toggle"
            aria-expanded={open}
            aria-controls="hero-other-versions"
            aria-label={
              open
                ? "Collapse versions menu"
                : "Expand Mobile, Desktop, and KSign Guide options"
            }
            title={open ? "Collapse versions" : "All versions"}
            onClick={() => setOpen((prev) => !prev)}
          >
            <ChevronToggleIcon open={open} />
          </button>
        </div>

        <div
          id="hero-other-versions"
          ref={panelRef}
          className="version-bar-panel"
          aria-hidden={!open}
        >
          <div ref={innerRef} className="version-bar-panel-inner">
            <div className="version-group">
              <div className="version-group-label">Mobile</div>
              <div className="version-group-items">
                <a
                  className="version-row"
                  href={apkUrl}
                  tabIndex={open ? 0 : -1}
                >
                  <span className="version-row-lead">
                    <AndroidIcon />
                    <span>Derbis.apk</span>
                  </span>
                  <span className="platform-pill platform-pill-live">Official</span>
                </a>

                <a
                  className="version-row"
                  href={ipaUrl}
                  download="Derbis.ipa"
                  tabIndex={open ? 0 : -1}
                >
                  <span className="version-row-lead">
                    <AppleIcon />
                    <span>Derbis.ipa</span>
                  </span>
                  <span className="platform-pill platform-pill-live">Beta</span>
                </a>
              </div>
            </div>

            <div className="version-group">
              <div className="version-group-label">Desktop</div>
              <div className="version-group-items">
                <div className="version-row is-soon" aria-disabled="true">
                  <span className="version-row-lead">
                    <WindowsIcon />
                    <span>Windows</span>
                  </span>
                  <span className="platform-pill platform-pill-soon">
                    Coming soon
                  </span>
                </div>

                <div className="version-row is-soon" aria-disabled="true">
                  <span className="version-row-lead">
                    <LaptopIcon />
                    <span>macOS</span>
                  </span>
                  <span className="platform-pill platform-pill-soon">
                    Coming soon
                  </span>
                </div>
              </div>
            </div>

            <div className="version-group">
              <div className="version-group-label">iOS Guide</div>
              <div className="version-group-items">
                <a
                  className="version-row"
                  href="#ksign"
                  tabIndex={open ? 0 : -1}
                  onClick={() => {
                    onSelectPlatform("ios");
                    setOpen(false);
                  }}
                >
                  <span className="version-row-lead">
                    <ShieldIcon />
                    <span>KSign Guide</span>
                  </span>
                  <span className="version-row-arrow" aria-hidden="true">
                    <ArrowRightIcon />
                  </span>
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
});

export default function App() {
  const sectionIds = useMemo(() => NAV_ITEMS.map((item) => item.id), []);
  const activeSection = useActiveSection(sectionIds);

  const [fontMode, setFontMode] = useState(() => {
    try {
      const saved = window.localStorage.getItem(FONT_STORAGE_KEY);
      return saved === "sfpro" ? "sfpro" : "pixel";
    } catch {
      return "pixel";
    }
  });

  const [platform, setPlatform] = useState(detectDefaultPlatform);
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

  const handleSelectPlatform = useCallback((next) => {
    setPlatform(next);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.font = fontMode;
    try {
      window.localStorage.setItem(FONT_STORAGE_KEY, fontMode);
    } catch {}
  }, [fontMode]);

  useEffect(() => {
    if (window.location.hash === "#ksign" || window.location.hash === "#ios") {
      setPlatform("ios");
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadGitHubMetadata = async () => {
      try {
        const [repoRes, releasesRes] = await Promise.all([
          fetch(`https://api.github.com/repos/${GITHUB_REPO}`, {
            headers: { Accept: "application/vnd.github+json" }
          }),
          fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=15`, {
            headers: { Accept: "application/vnd.github+json" }
          })
        ]);

        if (cancelled) return;

        if (repoRes.ok) {
          const repoJson = await repoRes.json();
          if (typeof repoJson.stargazers_count === "number") {
            setStars(repoJson.stargazers_count);
          }
        }

        if (!releasesRes.ok) return;
        const relList = await releasesRes.json();
        if (!Array.isArray(relList) || relList.length === 0) return;

        const byPublishedDesc = (a, b) =>
          (Date.parse(b.published_at || b.created_at || "") || 0) -
          (Date.parse(a.published_at || a.created_at || "") || 0);

        const officialAndroid = relList
          .filter(
            (r) =>
              !r.draft &&
              !r.prerelease &&
              (r.assets || []).some(
                (a) => typeof a.name === "string" && a.name.toLowerCase().endsWith(".apk")
              )
          )
          .sort(byPublishedDesc);
        const primary = officialAndroid[0];
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

              <HeroVersionBar
                apkUrl={apkUrl}
                ipaUrl={ipaUrl}
                onSelectPlatform={handleSelectPlatform}
              />
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
                <dt>Windows</dt>
                <dd className="spec-soon">Coming soon</dd>
              </div>
              <div className="spec-cell">
                <dt>macOS</dt>
                <dd className="spec-soon">Coming soon</dd>
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
          onPlatformChange={handleSelectPlatform}
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
            <PixelMark size={20} />
            <span>Derbis · 2026 Yasuda · GPL-3.0-only</span>
          </div>

          <div className="footer-links">
            <a href={REPO_URL} target="_blank" rel="noopener noreferrer">
              GitHub
            </a>
            <a href={RELEASES_URL} target="_blank" rel="noopener noreferrer">
              Releases
            </a>
            <a href={LATEST_RELEASE_URL} target="_blank" rel="noopener noreferrer">
              {releaseTag}
            </a>
            <a href="#ksign" onClick={() => handleSelectPlatform("ios")}>
              KSign iOS Guide
            </a>
            <a href={apkUrl}>Derbis.apk</a>
            <a href={ipaUrl} download="Derbis.ipa">
              Derbis.ipa
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
