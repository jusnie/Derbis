import { useState } from "react";
import { RELEASES_URL } from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import { ChevronDownIcon, ExternalIcon } from "./Icons";

export function VersionsSection({ releases }) {
  const { ref, visible } = useScrollReveal();
  const [openTag, setOpenTag] = useState(releases[0]?.tag || "v1.8.0");

  return (
    <section
      id="versions"
      ref={ref}
      className={visible ? "section-band reveal is-visible" : "section-band reveal"}
      aria-labelledby="versions-heading"
    >
      <div className="wrap">
        <div className="section-head section-head-row">
          <div>
            <h2 id="versions-heading" className="section-title">
              Release history
            </h2>
            <p className="section-sub">
              Published tags and changelog notes from GitHub Releases
            </p>
          </div>

          <a
            className="btn btn-secondary"
            href={RELEASES_URL}
            target="_blank"
            rel="noopener noreferrer"
          >
            <span>All releases</span>
            <ExternalIcon />
          </a>
        </div>

        <div className="releases-stack">
          {releases.map((rel) => {
            const isOpen = openTag === rel.tag;
            return (
              <article key={rel.tag} className="release-card">
                <button
                  type="button"
                  className="release-summary"
                  aria-expanded={isOpen}
                  onClick={() => setOpenTag(isOpen ? "" : rel.tag)}
                >
                  <span className="release-tag">
                    {rel.latest && <span className="latest-dot" aria-hidden="true" />}
                    <span>{rel.tag}</span>
                  </span>
                  <span className="release-date">{rel.date}</span>
                  {rel.sizeLabel && <span className="release-meta">{rel.sizeLabel}</span>}
                  {typeof rel.downloads === "number" && rel.downloads > 0 && (
                    <span className="release-downloads">
                      {rel.downloads.toLocaleString("en-US")} downloads
                    </span>
                  )}
                  <ChevronDownIcon rotated={isOpen} />
                </button>

                {isOpen && (
                  <div className="release-body">
                    <div className="release-groups">
                      {rel.groups.map((grp) => (
                        <div key={grp.heading} className="release-group">
                          <h3 className="group-heading">{grp.heading}</h3>
                          <ul className="group-list">
                            {grp.items.map((line, idx) => (
                              <li key={idx}>{line}</li>
                            ))}
                          </ul>
                        </div>
                      ))}
                    </div>

                    <div className="release-foot">
                      <a href={rel.url} target="_blank" rel="noopener noreferrer">
                        View {rel.tag} on GitHub
                      </a>
                      {rel.apkUrl && <a href={rel.apkUrl}>Download {rel.tag} APK</a>}
                    </div>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
