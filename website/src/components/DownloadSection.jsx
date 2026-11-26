import { memo, useState } from "react";
import {
  APK_DOWNLOAD_URL,
  INSTALL_STEPS,
  IOS_INSTALL_STEPS,
  IOS_IPA_DOWNLOAD_URL,
  IOS_RELEASE_META,
  IOS_RELEASE_URL,
  IOS_SHA256_DOWNLOAD_URL,
  LATEST_RELEASE_URL,
  RELEASE_META,
  SHA256_DOWNLOAD_URL
} from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import {
  AndroidIcon,
  AppleIcon,
  CheckIcon,
  CopyIcon,
  ExternalIcon,
  LaptopIcon,
  WindowsIcon
} from "./Icons";

export const DownloadSection = memo(function DownloadSection({
  apkUrl,
  releaseTag,
  sizeLabel,
  totalDownloads,
  ipaUrl,
  iosVersion,
  iosReleaseUrl,
  iosSizeLabel,
  iosSha256,
  platform,
  onPlatformChange
}) {
  const { ref, visible } = useScrollReveal();
  const [copied, setCopied] = useState(false);

  const activeMobilePlatform = platform === "ios" ? "ios" : "android";
  const activeSha =
    activeMobilePlatform === "ios"
      ? iosSha256 || IOS_RELEASE_META.sha256
      : RELEASE_META.sha256;

  const handleCopySha = async () => {
    try {
      await navigator.clipboard.writeText(activeSha);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const handleSelect = (next) => {
    setCopied(false);
    onPlatformChange(next);
  };

  return (
    <section
      id="download"
      ref={ref}
      className={visible ? "section-band surface-band reveal is-visible" : "section-band surface-band reveal"}
      aria-labelledby="download-heading"
    >
      <div className="wrap">
        <div className="section-head section-head-row">
          <div>
            <h2 id="download-heading" className="section-title">
              Official app packages
            </h2>
            <p className="section-sub">
              Direct Android APK and native iOS IPA available now, with Windows and macOS builds coming soon
            </p>
          </div>

          <div className="platform-switch" role="tablist" aria-label="Choose mobile download platform">
            <button
              type="button"
              role="tab"
              aria-selected={activeMobilePlatform === "android"}
              className={
                activeMobilePlatform === "android"
                  ? "platform-tab is-active"
                  : "platform-tab"
              }
              onClick={() => handleSelect("android")}
            >
              <AndroidIcon />
              <span>Derbis.apk</span>
              <span className="platform-pill platform-pill-live">Official</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeMobilePlatform === "ios"}
              className={
                activeMobilePlatform === "ios"
                  ? "platform-tab is-active"
                  : "platform-tab"
              }
              onClick={() => handleSelect("ios")}
            >
              <AppleIcon />
              <span>Derbis.ipa</span>
              <span className="platform-pill platform-pill-live">Beta</span>
            </button>
          </div>
        </div>

        {activeMobilePlatform === "android" && (
          <div className="download-bench">
            <div className="bench-main">
              <div className="bench-meta-row">
                <span className="version-badge">{releaseTag || RELEASE_META.version}</span>
                <span className="meta-item">{sizeLabel || RELEASE_META.sizeLabel}</span>
                <span className="meta-item">{RELEASE_META.androidMin}</span>
                <span className="meta-item">build {RELEASE_META.versionCode}</span>
                {typeof totalDownloads === "number" && totalDownloads > 0 && (
                  <span className="meta-item">{totalDownloads.toLocaleString("en-US")} downloads</span>
                )}
              </div>

              <h3 className="bench-title">Derbis.apk ({RELEASE_META.packageName})</h3>
              <p className="bench-desc">
                Includes arm64-v8a and armeabi-v7a native FFmpeg audio decoders, all nine UI skins, and offline library sync in a single install
              </p>

              <div className="bench-actions">
                <a className="btn btn-primary" href={apkUrl || APK_DOWNLOAD_URL}>
                  <AndroidIcon />
                  <span>Download Derbis.apk</span>
                </a>
                <a
                  className="btn btn-secondary"
                  href={LATEST_RELEASE_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span>Release notes</span>
                  <ExternalIcon />
                </a>
                <a
                  className="btn btn-secondary"
                  href={SHA256_DOWNLOAD_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span>Derbis.apk.sha256</span>
                </a>
              </div>

              <div className="sha-box">
                <div className="sha-head">
                  <span className="sha-label">SHA-256 checksum</span>
                  <button type="button" className="copy-btn" onClick={handleCopySha}>
                    {copied ? <CheckIcon /> : <CopyIcon />}
                    <span>{copied ? "Copied" : "Copy hash"}</span>
                  </button>
                </div>
                <code className="sha-value">{activeSha}</code>
              </div>
            </div>

            <div className="steps-card">
              <h3 className="steps-heading">Install on Android in three steps</h3>
              <ol className="steps-list">
                {INSTALL_STEPS.map((s) => (
                  <li key={s.step} className="step-item">
                    <span className="step-num">{s.step}</span>
                    <div>
                      <div className="step-title">{s.title}</div>
                      <p className="step-detail">{s.detail}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        )}

        {activeMobilePlatform === "ios" && (
          <div className="download-bench">
            <div className="bench-main">
              <div className="bench-meta-row">
                <span className="version-badge version-badge-accent">
                  {iosVersion || IOS_RELEASE_META.version}
                </span>
                <span className="meta-item">{iosSizeLabel || IOS_RELEASE_META.sizeLabel}</span>
                <span className="meta-item">{IOS_RELEASE_META.iosMin}</span>
                <span className="meta-item">{IOS_RELEASE_META.archLabel}</span>
                <span className="meta-item">Beta · KSign ready</span>
              </div>

              <h3 className="bench-title">Derbis.ipa ({IOS_RELEASE_META.bundleId})</h3>
              <p className="bench-desc">
                100% native Swift + SwiftUI + AVPlayer client with 9 Derbis skins, Motion Canvas video, 5-band real-time EQ &amp; spatial DSP, karaoke word lyrics with romanization/translation, Jam rooms, and local FLAC/ALAC support
              </p>

              <div className="bench-actions">
                <a className="btn btn-primary" href={ipaUrl || IOS_IPA_DOWNLOAD_URL} download="Derbis.ipa">
                  <AppleIcon />
                  <span>Download Derbis.ipa</span>
                </a>
                <a className="btn btn-secondary" href="#ksign">
                  <span>KSign Guide</span>
                </a>
                <a
                  className="btn btn-secondary"
                  href={iosReleaseUrl || IOS_RELEASE_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span>Release notes</span>
                  <ExternalIcon />
                </a>
                <a
                  className="btn btn-secondary"
                  href={IOS_SHA256_DOWNLOAD_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span>Derbis.ipa.sha256</span>
                </a>
              </div>

              {activeSha ? (
                <div className="sha-box">
                  <div className="sha-head">
                    <span className="sha-label">SHA-256 checksum</span>
                    <button type="button" className="copy-btn" onClick={handleCopySha}>
                      {copied ? <CheckIcon /> : <CopyIcon />}
                      <span>{copied ? "Copied" : "Copy hash"}</span>
                    </button>
                  </div>
                  <code className="sha-value">{activeSha}</code>
                </div>
              ) : (
                <div className="sha-box">
                  <div className="sha-head">
                    <span className="sha-label">SHA-256 verification</span>
                  </div>
                  <code className="sha-value">
                    Published alongside Derbis.ipa on GitHub Releases
                  </code>
                </div>
              )}
            </div>

            <div className="steps-card steps-card-flex">
              <div>
                <h3 className="steps-heading">Install on iPhone or iPad with KSign</h3>
                <ol className="steps-list">
                  {IOS_INSTALL_STEPS.map((s) => (
                    <li key={s.step} className="step-item">
                      <span className="step-num">{s.step}</span>
                      <div>
                        <div className="step-title">{s.title}</div>
                        <p className="step-detail">{s.detail}</p>
                      </div>
                    </li>
                  ))}
                </ol>
              </div>
              <a className="btn btn-secondary steps-jump-btn" href="#ksign">
                <span>Open KSign Guide</span>
              </a>
            </div>
          </div>
        )}

        <div className="platform-matrix" aria-label="Platform availability">
          <button
            type="button"
            className={
              activeMobilePlatform === "android"
                ? "matrix-card is-active"
                : "matrix-card"
            }
            onClick={() => handleSelect("android")}
          >
            <div className="matrix-top">
              <span className="matrix-name">
                <AndroidIcon />
                <span>Derbis.apk</span>
              </span>
              <span className="platform-pill platform-pill-live">Official</span>
            </div>
            <span className="matrix-meta">Android · {releaseTag || RELEASE_META.version} · {sizeLabel || RELEASE_META.sizeLabel}</span>
          </button>

          <button
            type="button"
            className={
              activeMobilePlatform === "ios"
                ? "matrix-card is-active"
                : "matrix-card"
            }
            onClick={() => handleSelect("ios")}
          >
            <div className="matrix-top">
              <span className="matrix-name">
                <AppleIcon />
                <span>Derbis.ipa</span>
              </span>
              <span className="platform-pill platform-pill-live">Beta</span>
            </div>
            <span className="matrix-meta">iOS &amp; iPadOS · {iosVersion || IOS_RELEASE_META.version} · {iosSizeLabel || IOS_RELEASE_META.sizeLabel}</span>
          </button>

          <div className="matrix-card matrix-card-static" aria-disabled="true">
            <div className="matrix-top">
              <span className="matrix-name">
                <WindowsIcon />
                <span>Windows</span>
              </span>
              <span className="platform-pill platform-pill-soon">Coming soon</span>
            </div>
            <span className="matrix-meta">Desktop · Windows 10/11 (x64 &amp; ARM64)</span>
          </div>

          <div className="matrix-card matrix-card-static" aria-disabled="true">
            <div className="matrix-top">
              <span className="matrix-name">
                <LaptopIcon />
                <span>macOS</span>
              </span>
              <span className="platform-pill platform-pill-soon">Coming soon</span>
            </div>
            <span className="matrix-meta">Desktop · Apple Silicon &amp; Intel</span>
          </div>
        </div>
      </div>
    </section>
  );
});
