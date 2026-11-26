import { memo, useEffect, useMemo, useState } from "react";
import {
  IOS_IPA_DOWNLOAD_URL,
  IOS_IPA_PUBLIC_URL,
  IOS_RELEASE_META,
  IOS_RELEASE_URL,
  KSIGN_GUIDE_PHASES,
  KSIGN_LINKS
} from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import {
  AppleIcon,
  ArrowLeftIcon,
  ArrowRightIcon,
  CloseIcon,
  DownloadIcon,
  ExpandIcon,
  ExternalIcon,
  ShieldIcon
} from "./Icons";

function resolveGuideImgPrimary(key) {
  return `/api/ksign-img?key=${encodeURIComponent(key)}`;
}

function GuideStepImage({ imgKey, fallbackSrc, alt, stepLabel, onInspect, onUnavailable }) {
  const [stage, setStage] = useState("proxy");
  const [loaded, setLoaded] = useState(false);

  if (stage === "failed") {
    return null;
  }

  const activeSrc = stage === "proxy" ? resolveGuideImgPrimary(imgKey) : fallbackSrc;

  return (
    <button
      type="button"
      className={loaded ? "ksign-img-btn is-loaded" : "ksign-img-btn"}
      onClick={onInspect}
      aria-label={`Zoom screenshot for step ${stepLabel}`}
    >
      <img
        src={activeSrc}
        alt={alt}
        loading="lazy"
        decoding="async"
        className="ksign-step-img"
        onLoad={(e) => {
          if (e.currentTarget.naturalWidth > 0) {
            setLoaded(true);
          }
        }}
        onError={() => {
          if (stage === "proxy" && fallbackSrc) {
            setStage("direct");
          } else {
            setStage("failed");
            if (onUnavailable) onUnavailable(imgKey);
          }
        }}
      />
      {loaded && (
        <span className="ksign-zoom-pill" aria-hidden="true">
          <ExpandIcon />
        </span>
      )}
    </button>
  );
}

export const KSignGuideSection = memo(function KSignGuideSection({
  ipaUrl,
  iosVersion,
  iosReleaseUrl,
  iosSizeLabel
}) {
  const { ref, visible } = useScrollReveal();
  const [activePhase, setActivePhase] = useState("all");
  const [heroStage, setHeroStage] = useState("proxy");
  const [heroLoaded, setHeroLoaded] = useState(false);
  const [unavailableKeys, setUnavailableKeys] = useState(() => new Set());
  const [lightboxIdx, setLightboxIdx] = useState(null);

  const resolvedIpaUrl = ipaUrl || IOS_IPA_DOWNLOAD_URL;
  const resolvedPublicIpaUrl =
    resolvedIpaUrl.startsWith("http") ? resolvedIpaUrl : IOS_IPA_PUBLIC_URL;
  const resolvedVersion = iosVersion || IOS_RELEASE_META.version;
  const resolvedReleaseUrl = iosReleaseUrl || IOS_RELEASE_URL;
  const resolvedSize = iosSizeLabel || IOS_RELEASE_META.sizeLabel;
  const encodedIpaUrl = encodeURIComponent(resolvedPublicIpaUrl);

  const markUnavailable = (key) => {
    setUnavailableKeys((prev) => {
      if (prev.has(key)) return prev;
      const next = new Set(prev);
      next.add(key);
      return next;
    });
  };

  const allLightboxItems = useMemo(() => {
    const items = [];
    if (heroStage !== "failed" && !unavailableKeys.has("hero")) {
      items.push({
        id: "hero",
        step: "KSign",
        title: "KSign iOS on-device IPA signer",
        detail: "Sign and install Derbis.ipa directly on iPhone or iPad with no computer or jailbreak required.",
        primaryImg: resolveGuideImgPrimary("hero"),
        fallbackImg: KSIGN_LINKS.heroImage,
        alt: "KSign iOS app preview showing on-device IPA signing interface"
      });
    }
    KSIGN_GUIDE_PHASES.forEach((phase) => {
      phase.steps.forEach((s) => {
        if (!unavailableKeys.has(s.id)) {
          items.push({
            id: s.id,
            step: `Step ${s.step}`,
            title: s.title,
            detail: s.detail,
            primaryImg: resolveGuideImgPrimary(s.id),
            fallbackImg: s.img,
            alt: s.alt
          });
        }
      });
    });
    return items;
  }, [heroStage, unavailableKeys]);

  const visiblePhases = useMemo(() => {
    if (activePhase === "all") return KSIGN_GUIDE_PHASES;
    return KSIGN_GUIDE_PHASES.filter((p) => p.id === activePhase);
  }, [activePhase]);

  useEffect(() => {
    if (lightboxIdx === null) return undefined;
    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        setLightboxIdx(null);
      } else if (e.key === "ArrowRight") {
        setLightboxIdx((prev) => (prev + 1) % allLightboxItems.length);
      } else if (e.key === "ArrowLeft") {
        setLightboxIdx((prev) => (prev - 1 + allLightboxItems.length) % allLightboxItems.length);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [lightboxIdx, allLightboxItems.length]);

  const openStepLightbox = (stepId) => {
    const idx = allLightboxItems.findIndex((item) => item.id === stepId);
    if (idx >= 0) setLightboxIdx(idx);
  };

  const inspectedItem = lightboxIdx !== null ? allLightboxItems[lightboxIdx] : null;

  return (
    <section
      id="ksign"
      ref={ref}
      className={visible ? "section-band reveal is-visible" : "section-band reveal"}
      aria-labelledby="ksign-heading"
    >
      <div className="wrap">
        <div className="section-head section-head-row">
          <div>
            <div className="eyebrow-row">
              <span className="eyebrow-badge">
                <AppleIcon />
                <span>Derbis for iOS · Beta</span>
              </span>
              <span className="eyebrow-meta">Derbis.ipa · {resolvedVersion} · {resolvedSize} · iOS / iPadOS 16.0+</span>
            </div>
            <h2 id="ksign-heading" className="section-title">
              Install Derbis.ipa on iPhone &amp; iPad with KSign
            </h2>
            <p className="section-sub">
              Visual step-by-step guide to installing KSign, importing an active signing certificate, and installing Derbis.ipa directly on your device with no PC
            </p>
          </div>

          <div className="phase-filter" role="tablist" aria-label="Filter KSign setup phases">
            <button
              type="button"
              role="tab"
              aria-selected={activePhase === "all"}
              className={activePhase === "all" ? "phase-btn is-active" : "phase-btn"}
              onClick={() => setActivePhase("all")}
            >
              All steps
            </button>
            {KSIGN_GUIDE_PHASES.map((p) => (
              <button
                key={p.id}
                type="button"
                role="tab"
                aria-selected={activePhase === p.id}
                className={activePhase === p.id ? "phase-btn is-active" : "phase-btn"}
                onClick={() => setActivePhase(p.id)}
              >
                {p.phase}
              </button>
            ))}
          </div>
        </div>

        <div
          className={
            heroStage === "failed"
              ? "ksign-overview-card ksign-overview-card-single"
              : "ksign-overview-card"
          }
        >
          <div className="ksign-overview-copy">
            <div className="bench-meta-row">
              <span className="version-badge version-badge-accent">Derbis.ipa · {resolvedVersion}</span>
              <span className="meta-item">{resolvedSize}</span>
              <span className="meta-item">Single-bundle arm64 SwiftUI</span>
              <span className="meta-item">No PC · No Jailbreak</span>
            </div>

            <h3 className="bench-title">Everything you need before starting</h3>
            <p className="bench-desc">
              Derbis for iOS ships as a single-bundle native arm64 IPA with zero restricted Apple entitlements so KSign v1.6 can sign and install Derbis.ipa directly on iOS or iPadOS 16.0 and newer. Grab Derbis.ipa below alongside the Anti-Revoke DNS profile and an active certificate pair from ksignios.app.
            </p>

            <div className="ksign-quick-actions">
              <a className="btn btn-primary" href={resolvedIpaUrl} download="Derbis.ipa">
                <DownloadIcon />
                <span>Download Derbis.ipa</span>
              </a>
              <a
                className="btn btn-secondary"
                href={KSIGN_LINKS.dnsProfileUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <ShieldIcon />
                <span>Anti-Revoke DNS</span>
              </a>
              <a
                className="btn btn-secondary"
                href={KSIGN_LINKS.siteUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span>KSign Direct Install</span>
                <ExternalIcon />
              </a>
              <a
                className="btn btn-secondary"
                href={KSIGN_LINKS.certUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span>KSign Certificates</span>
                <ExternalIcon />
              </a>
            </div>

            <div className="ksign-inline-links">
              <a href={resolvedReleaseUrl} target="_blank" rel="noopener noreferrer">
                View iOS {resolvedVersion} release notes on GitHub
              </a>
              <span aria-hidden="true">·</span>
              <a href={KSIGN_LINKS.ksignIpaUrl} target="_blank" rel="noopener noreferrer">
                Direct KSign v1.6 IPA (15.6 MB)
              </a>
            </div>
          </div>

          {heroStage !== "failed" && (
            <div className="ksign-overview-media">
              <button
                type="button"
                className={
                  heroLoaded ? "ksign-hero-preview is-loaded" : "ksign-hero-preview"
                }
                onClick={() => openStepLightbox("hero")}
                aria-label="Inspect KSign iOS preview image"
              >
                <img
                  src={
                    heroStage === "proxy"
                      ? resolveGuideImgPrimary("hero")
                      : KSIGN_LINKS.heroImage
                  }
                  alt="KSign iOS app preview showing on-device IPA signing workflow"
                  loading="lazy"
                  decoding="async"
                  onLoad={(e) => {
                    if (e.currentTarget.naturalWidth > 0) {
                      setHeroLoaded(true);
                    }
                  }}
                  onError={() => {
                    if (heroStage === "proxy") {
                      setHeroStage("direct");
                    } else {
                      setHeroStage("failed");
                      markUnavailable("hero");
                    }
                  }}
                />
                {heroLoaded && (
                  <span className="ksign-zoom-pill" aria-hidden="true">
                    <ExpandIcon />
                  </span>
                )}
              </button>
            </div>
          )}
        </div>

        <div className="ksign-phases-stack">
          {visiblePhases.map((phase) => (
            <div key={phase.id} className="ksign-phase-block">
              <div className="ksign-phase-head">
                <div>
                  <span className="ksign-phase-kicker">{phase.phase}</span>
                  <h3 className="ksign-phase-title">{phase.title}</h3>
                  <p className="ksign-phase-sub">{phase.subtitle}</p>
                </div>

                <div className="ksign-phase-actions">
                  {phase.useIpaAction ? (
                    <a className="btn btn-primary btn-compact" href={resolvedIpaUrl} download="Derbis.ipa">
                      <DownloadIcon />
                      <span>Derbis.ipa ({resolvedSize})</span>
                    </a>
                  ) : (
                    (phase.actions || []).map((act) => (
                      <a
                        key={act.label}
                        className={act.primary ? "btn btn-primary btn-compact" : "btn btn-secondary btn-compact"}
                        href={act.href}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <span>{act.label}</span>
                        {act.external && <ExternalIcon />}
                      </a>
                    ))
                  )}
                </div>
              </div>

              <div
                className={
                  phase.steps.length === 4
                    ? "ksign-steps-grid ksign-steps-grid-four"
                    : "ksign-steps-grid"
                }
              >
                {phase.steps.map((step) => (
                  <article key={step.id} className="ksign-step-card">
                    <div className="ksign-step-top">
                      <span className="ksign-step-num">Step {step.step}</span>
                      <span className="shot-tag">{step.badge}</span>
                    </div>

                    <GuideStepImage
                      imgKey={step.id}
                      fallbackSrc={step.img}
                      alt={step.alt}
                      stepLabel={step.step}
                      onInspect={() => openStepLightbox(step.id)}
                      onUnavailable={markUnavailable}
                    />

                    <div className="ksign-step-body">
                      <h4 className="ksign-step-title">{step.title}</h4>
                      <p className="ksign-step-detail">{step.detail}</p>
                      <code className="ksign-step-path">{step.pathHint}</code>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="ksign-tips-grid">
          <div className="ksign-tip-card">
            <h3 className="ksign-tip-title">Fixing &ldquo;Unable to Verify App&rdquo;</h3>
            <p className="ksign-tip-copy">
              Always install and enable the Anti-Revoke DNS profile before installing KSign. If Apple revokes a public enterprise mirror, delete the unverified app, install KSign from a different Direct Install mirror on{" "}
              <a href={KSIGN_LINKS.siteUrl} target="_blank" rel="noopener noreferrer">
                ksignios.app
              </a>
              , or import another certificate ZIP from{" "}
              <a href={KSIGN_LINKS.certUrl} target="_blank" rel="noopener noreferrer">
                ksignios.app/ksign-certificate
              </a>
              .
            </p>
          </div>

          <div className="ksign-tip-card">
            <h3 className="ksign-tip-title">Alternative iOS sideloaders</h3>
            <p className="ksign-tip-copy">
              Already use SideStore, AltStore, Sideloadly, Feather, or TrollStore? Derbis.ipa uses a clean single-target bundle identifier with no App Group or extension plug-in restrictions, so you can open it directly:
            </p>
            <div className="ksign-alt-buttons">
              <a className="copy-btn" href={`sidestore://install?url=${encodedIpaUrl}`}>
                <span>Open in SideStore</span>
              </a>
              <a className="copy-btn" href={`altstore://install?url=${encodedIpaUrl}`}>
                <span>Open in AltStore</span>
              </a>
              <a className="copy-btn" href={`apple-magnifier://install?url=${encodedIpaUrl}`}>
                <span>Open in TrollStore</span>
              </a>
            </div>
          </div>
        </div>
      </div>

      {inspectedItem && (
        <div
          className="lightbox-backdrop"
          role="dialog"
          aria-modal="true"
          aria-label={inspectedItem.title}
          onClick={() => setLightboxIdx(null)}
        >
          <div className="lightbox-panel" onClick={(e) => e.stopPropagation()}>
            <div className="lightbox-head">
              <div className="lightbox-title-row">
                <span className="shot-tag">{inspectedItem.step}</span>
                <h3 className="lightbox-title">{inspectedItem.title}</h3>
              </div>
              <div className="lightbox-actions">
                <button
                  type="button"
                  className="icon-btn"
                  aria-label="Previous step screenshot"
                  onClick={() =>
                    setLightboxIdx(
                      (lightboxIdx - 1 + allLightboxItems.length) % allLightboxItems.length
                    )
                  }
                >
                  <ArrowLeftIcon />
                </button>
                <button
                  type="button"
                  className="icon-btn"
                  aria-label="Next step screenshot"
                  onClick={() => setLightboxIdx((lightboxIdx + 1) % allLightboxItems.length)}
                >
                  <ArrowRightIcon />
                </button>
                <button
                  type="button"
                  className="icon-btn"
                  aria-label="Close preview"
                  onClick={() => setLightboxIdx(null)}
                >
                  <CloseIcon />
                </button>
              </div>
            </div>

            <div className="lightbox-stage">
              <img
                key={inspectedItem.id}
                src={inspectedItem.primaryImg}
                alt={inspectedItem.alt}
                className="lightbox-img"
                onError={(e) => {
                  if (
                    inspectedItem.fallbackImg &&
                    e.currentTarget.src !== inspectedItem.fallbackImg
                  ) {
                    e.currentTarget.src = inspectedItem.fallbackImg;
                  }
                }}
              />
            </div>

            <p className="lightbox-detail">{inspectedItem.detail}</p>
          </div>
        </div>
      )}
    </section>
  );
});
