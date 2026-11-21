import { useState } from "react";
import { NAV_ITEMS, REPO_URL } from "../data/siteData";
import { PixelMark, StarIcon } from "./Icons";

export function Navbar({ scrollProgress, activeSection, stars, fontMode, onFontChange }) {
  const [menuOpen, setMenuOpen] = useState(false);

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
          className="page-progress-fill"
          style={{ transform: `scaleX(${scrollProgress.toFixed(4)})` }}
        />
      </div>
    </header>
  );
}
