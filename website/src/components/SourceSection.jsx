import { memo } from "react";
import { REPO_URL, SOURCE_FACTS } from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import { ExternalIcon, PixelMark, StarIcon } from "./Icons";

export const SourceSection = memo(function SourceSection({ stars }) {
  const { ref, visible } = useScrollReveal();

  return (
    <section
      id="source"
      ref={ref}
      className={visible ? "section-band surface-band reveal is-visible" : "section-band surface-band reveal"}
      aria-labelledby="source-heading"
    >
      <div className="wrap">
        <div className="section-head">
          <h2 id="source-heading" className="section-title">
            Open source from audio core to UI skin
          </h2>
          <p className="section-sub">
            Inspect every Kotlin screen, native C++ FFmpeg bridge, SwiftUI iOS view, and build workflow in jusnie/Derbis
          </p>
        </div>

        <div className="source-grid">
          <div className="facts-table">
            {SOURCE_FACTS.map((fact) => (
              <div key={fact.label} className="fact-row">
                <div className="fact-label">{fact.label}</div>
                <div className="fact-value">{fact.value}</div>
              </div>
            ))}
          </div>

          <aside className="star-card" aria-label="GitHub repository">
            <div className="star-card-top">
              <PixelMark size={20} />
              <span>jusnie/Derbis</span>
            </div>
            <h3 className="star-card-title">Follow development on GitHub</h3>
            <p className="star-card-copy">
              Browse source code, file bug reports, verify GitHub Actions APK builds, or star the repository to bookmark upcoming releases
            </p>
            <div className="star-card-actions">
              <a
                className="btn btn-primary"
                href={REPO_URL}
                target="_blank"
                rel="noopener noreferrer"
              >
                <StarIcon />
                <span>Star on GitHub ({stars})</span>
              </a>
              <a
                className="btn btn-secondary"
                href={`${REPO_URL}/issues`}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span>Open issues</span>
                <ExternalIcon />
              </a>
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
});
