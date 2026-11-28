import { memo, useEffect, useRef, useState } from "react";
import { NAV_ITEMS, REPO_URL } from "../data/siteData";
import { PixelMark, StarIcon } from "./Icons";

export const Navbar = memo(function Navbar({ activeSection, stars, fontMode, onFontChange }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const progressFillRef = useRef(null);

  useEffect(() => {
    let rafId = 0;

    const syncProgress = () => {
      rafId = 0;
      const el = progressFillRef.current;
      if (!el) return;
      const doc = document.documentElement;
      const maxScroll = Math.max(1, doc.scrollHeight - window.innerHeight);
      const ratio = Math.min(1, Math.max(0, window.scrollY / maxScroll));
      el.style.transform = `scaleX(${ratio.toFixed(4)})`;
    };

    const onScroll = () => {
      if (!rafId) {
        rafId = window.requestAnimationFrame(syncProgress);
      }
    };

    syncProgress();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      if (rafId) window.cancelAnimationFrame(rafId);
    };
  }, []);

  return (
    <header className="site-nav">
      <div className="wrap nav-inner">
        <a className="brand-link" href="#top">
          <PixelMark size={18} />
          <span>Derbis</span>
        </a>

        <nav className="nav-links" aria-label="Primary">
          {NAV_ITEMS.map((item) => (
            <a
              key={item.id}
              href={`#${item.id}`}
              className={activeSection === item.id ? "nav-link is-active" : "nav-link"}
              aria-current={activeSection === item.id ? "true" : undefined}
            >
              {item.label}
            </a>
          ))}
        </nav>

        <div className="nav-actions">
          <div className="font-switch" role="group" aria-label="Choose website font">
            <button
              type="button"
              className={fontMode === "pixel" ? "font-opt font-opt-pixel is-active" : "font-opt font-opt-pixel"}
              aria-pressed={fontMode === "pixel"}
              onClick={() => onFontChange("pixel")}
            >
              Pixel
            </button>
            <button
              type="button"
              className={fontMode === "sfpro" ? "font-opt font-opt-sf is-active" : "font-opt font-opt-sf"}
              aria-pressed={fontMode === "sfpro"}
              onClick={() => onFontChange("sfpro")}
            >
              SF Pro
            </button>
          </div>

          <a
            className="star-btn"
            href={REPO_URL}
            target="_blank"
            rel="noopener noreferrer"
          >
            <StarIcon />
            <span className="star-word">Star</span>
            <span className="star-count">{stars}</span>
          </a>

          <button
            type="button"
            className="menu-toggle"
            aria-expanded={menuOpen}
            aria-controls="mobile-nav-drawer"
            onClick={() => setMenuOpen((prev) => !prev)}
          >
            {menuOpen ? "Close" : "Menu"}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav id="mobile-nav-drawer" className="mobile-drawer" aria-label="Mobile">
          {NAV_ITEMS.map((item) => (
            <a
              key={item.id}
              href={`#${item.id}`}
              className={activeSection === item.id ? "drawer-link is-active" : "drawer-link"}
              onClick={() => setMenuOpen(false)}
            >
              {item.label}
            </a>
          ))}
          <a
            href={REPO_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="drawer-link"
            onClick={() => setMenuOpen(false)}
          >
            GitHub repository
          </a>
        </nav>
      )}

      <div className="page-progress-track" aria-hidden="true">
        <div
          ref={progressFillRef}
          className="page-progress-fill"
          style={{ transform: "scaleX(0)" }}
        />
      </div>
    </header>
  );
});
