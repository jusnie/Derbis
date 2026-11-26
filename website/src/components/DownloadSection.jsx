import { useState } from "react";
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
import { AppleIcon, CheckIcon, CopyIcon, DownloadIcon, ExternalIcon } from "./Icons";

export function DownloadSection({
  apkUrl,
  releaseTag,
  sizeLabel,
  totalDownloads,
  ipaUrl,
  iosReleaseTag,
  iosReleaseUrl,
  iosSizeLabel,
  iosSha256,
  platform,
  onPlatformChange
}) {
  const { ref, visible } = useScrollReveal();
  const [copied, setCopied] = useState(false);

  const isIos = platform === "ios";
  const activeSha = isIos
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
              Direct app packages
            </h2>
            <p className="section-sub">
              Official signed universal Android APK and unofficial KSign-ready native iOS IPA published on GitHub Releases
            </p>
          </div>

          <div className="platform-switch" role="tablist" aria-label="Choose download platform">
            <button
              type="button"
              role="tab"
              aria-selected={!isIos}
              className={!isIos ? "platform-tab is-active" : "platform-tab"}
              onClick={() => handleSelect("android")}
            >
              <span>Android APK</span>
              <span className="platform-pill">Official</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={isIos}
              className={isIos ? "platform-tab is-active" : "platform-tab"}
              onClick={() => handleSelect("ios")}
            >
              <AppleIcon />
              <span>iOS IPA</span>
              <span className="platform-pill platform-pill-accent">KSign</span>
            </button>
          </div>
        </div>

        {!isIos ? (
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
                  <DownloadIcon />
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
              <h3 className="steps-heading">Sideload on Android in three steps</h3>
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
        ) : (
          <div className="download-bench">
            <div className="bench-main">
              <div className="bench-meta-row">
                <span className="version-badge version-badge-accent">
                  {iosReleaseTag || IOS_RELEASE_META.tag}
                </span>
                <span className="meta-item">{iosSizeLabel || IOS_RELEASE_META.sizeLabel}</span>
                <span className="meta-item">{IOS_RELEASE_META.iosMin}</span>
                <span className="meta-item">{IOS_RELEASE_META.archLabel}</span>
                <span className="meta-item">KSign ready</span>
              </div>

              <h3 className="bench-title">
                Derbis-unofficial.ipa ({IOS_RELEASE_META.bundleId})
              </h3>
              <p className="bench-desc">
                100% native Swift + SwiftUI + AVPlayer client with 9 Derbis skins, Motion Canvas video, 5-band real-time EQ & spatial DSP, karaoke word lyrics with romanization/translation, Jam rooms, and local FLAC/ALAC support
              </p>

              <div className="bench-actions">
                <a className="btn btn-primary" href={ipaUrl || IOS_IPA_DOWNLOAD_URL}>
                  <DownloadIcon />
                  <span>Download Derbis-unofficial.ipa</span>
                </a>
                <a className="btn btn-secondary" href="#ksign">
                  <span>Visual KSign guide</span>
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
                  <span>Derbis-unofficial.ipa.sha256</span>
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
                    Published alongside {iosReleaseTag || IOS_RELEASE_META.tag} on GitHub Releases
                  </code>
                </div>
              )}
            </div>

            <div className="steps-card steps-card-flex">
              <div>
                <h3 className="steps-heading">Sideload on iPhone or iPad with KSign</h3>
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
                <span>Open step-by-step KSign guide with screenshots</span>
              </a>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
