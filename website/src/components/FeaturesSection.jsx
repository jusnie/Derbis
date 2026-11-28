import { memo } from "react";
import { CAPABILITIES } from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import { DiscordLogo, LastFmLogo } from "./Icons";

export const FeaturesSection = memo(function FeaturesSection() {
  const { ref, visible } = useScrollReveal();

  return (
    <section
      id="features"
      ref={ref}
      className={visible ? "section-band reveal is-visible" : "section-band reveal"}
      aria-labelledby="features-heading"
    >
      <div className="wrap features-split">
        <div className="features-lead">
          <h2 id="features-heading" className="section-title">
            Built for daily listening, not upsell banners
          </h2>
          <p className="section-sub">
            Every playback engine, lyric renderer, and library connector runs locally on your Android phone with zero accounts required to start playing
          </p>
        </div>

        <div className="capability-list">
          {CAPABILITIES.map((item) => (
            <article key={item.id} className="capability-row">
              <span className="capability-idx">{item.index}</span>
              <div className="capability-body">
                <div className="capability-head">
                  <h3 className="capability-title">{item.title}</h3>
                  {item.brands && (
                    <span className="brand-pair" aria-label="Discord and Last.fm">
                      <DiscordLogo />
                      <LastFmLogo />
                    </span>
                  )}
                </div>
                <p className="capability-summary">{item.summary}</p>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
});
