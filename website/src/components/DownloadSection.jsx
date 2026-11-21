import { useState } from "react";
import {
  APK_DOWNLOAD_URL,
  INSTALL_STEPS,
  LATEST_RELEASE_URL,
  RELEASE_META,
  SHA256_DOWNLOAD_URL
} from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import { CheckIcon, CopyIcon, DownloadIcon, ExternalIcon } from "./Icons";

export function DownloadSection({ apkUrl, releaseTag, sizeLabel, totalDownloads }) {
  const { ref, visible } = useScrollReveal();
  const [copied, setCopied] = useState(false);

  const handleCopySha = async () => {
    try {
      await navigator.clipboard.writeText(RELEASE_META.sha256);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  return (
    <section
      id="download"
      ref={ref}
      className={visible ? "section-band surface-band reveal is-visible" : "section-band surface-band reveal"}
      aria-labelledby="download-heading"
    >
      <div className="wrap">
        <div className="section-head">
          <h2 id="download-heading" className="section-title">
            Direct Android package
          </h2>
          <p className="section-sub">
            Signed universal APK published on GitHub Releases with verifiable SHA-256 digest
          </p>
        </div>

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
              <code className="sha-value">{RELEASE_META.sha256}</code>
            </div>
          </div>

          <div className="steps-card">
            <h3 className="steps-heading">Sideload in three steps</h3>
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
      </div>
    </section>
  );
}
