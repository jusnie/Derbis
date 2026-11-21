import { useCallback, useEffect, useRef, useState } from "react";
import { APP_SCREENS } from "../data/siteData";
import { useScrollReveal } from "../hooks/useScrollEffects";
import { ArrowLeftIcon, ArrowRightIcon, CloseIcon, ExpandIcon } from "./Icons";

export function ScreenGallery() {
  const { ref: sectionRef, visible } = useScrollReveal();
  const railRef = useRef(null);
  const cardRefs = useRef([]);
  const dragState = useRef({ active: false, moved: false, startX: 0, startScrollLeft: 0 });

  const [activeIndex, setActiveIndex] = useState(0);
  const [railProgress, setRailProgress] = useState(0.2);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(true);
  const [inspectedIndex, setInspectedIndex] = useState(null);

  const syncRailMetrics = useCallback(() => {
    const rail = railRef.current;
    if (!rail) return;

    const maxScroll = Math.max(0, rail.scrollWidth - rail.clientWidth);
    const current = rail.scrollLeft;

    setCanPrev(current > 8);
    setCanNext(current < maxScroll - 8);

    if (maxScroll <= 0) {
      setRailProgress(1);
    } else {
      const ratio = current / maxScroll;
      setRailProgress(Math.min(1, Math.max(0.2, 0.2 + ratio * 0.8)));
    }

    const railRect = rail.getBoundingClientRect();
    const targetLeft = railRect.left + 24;
    let closestIdx = 0;
    let closestDist = Number.POSITIVE_INFINITY;

    cardRefs.current.forEach((card, idx) => {
      if (!card) return;
      const dist = Math.abs(card.getBoundingClientRect().left - targetLeft);
      if (dist < closestDist) {
        closestDist = dist;
        closestIdx = idx;
      }
    });

    setActiveIndex(closestIdx);
  }, []);

  useEffect(() => {
    const rail = railRef.current;
    if (!rail) return undefined;

    let rafId = 0;
    const onScroll = () => {
      if (!rafId) {
        rafId = window.requestAnimationFrame(() => {
          rafId = 0;
          syncRailMetrics();
        });
      }
    };

    syncRailMetrics();
    rail.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll, { passive: true });

    return () => {
      rail.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      if (rafId) window.cancelAnimationFrame(rafId);
    };
  }, [syncRailMetrics]);

  const scrollToIndex = useCallback((nextIdx) => {
    const clamped = Math.max(0, Math.min(APP_SCREENS.length - 1, nextIdx));
    const rail = railRef.current;
    const card = cardRefs.current[clamped];
    if (!rail || !card) return;

    const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;
    const targetLeft = card.offsetLeft - rail.offsetLeft;
    rail.scrollTo({
      left: Math.max(0, targetLeft),
      behavior: reduced ? "auto" : "smooth"
    });
    setActiveIndex(clamped);
  }, []);

  const handlePointerDown = (event) => {
    dragState.current.moved = false;
    if (event.pointerType === "touch") return;
    if (event.target.closest("button")) return;
    const rail = railRef.current;
    if (!rail) return;

    dragState.current = {
      active: true,
      moved: false,
      startX: event.clientX,
      startScrollLeft: rail.scrollLeft
    };
    rail.classList.add("is-dragging");
  };

  const handlePointerMove = (event) => {
    const state = dragState.current;
    const rail = railRef.current;
    if (!state.active || !rail) return;

    const delta = event.clientX - state.startX;
    if (Math.abs(delta) > 6) {
      state.moved = true;
    }
    rail.scrollLeft = state.startScrollLeft - delta;
  };

  const endPointerDrag = () => {
    const rail = railRef.current;
    if (rail) rail.classList.remove("is-dragging");
    dragState.current.active = false;
  };

  useEffect(() => {
    if (inspectedIndex === null) return undefined;

    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        setInspectedIndex(null);
      } else if (event.key === "ArrowRight") {
        setInspectedIndex((prev) => (prev + 1) % APP_SCREENS.length);
      } else if (event.key === "ArrowLeft") {
        setInspectedIndex((prev) => (prev - 1 + APP_SCREENS.length) % APP_SCREENS.length);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [inspectedIndex]);

  const inspected = inspectedIndex !== null ? APP_SCREENS[inspectedIndex] : null;

  return (
    <section
      id="screens"
      ref={sectionRef}
      className={visible ? "section-band screens-band reveal is-visible" : "section-band screens-band reveal"}
      aria-labelledby="screens-heading"
    >
      <div className="wrap">
        <div className="rail-header">
          <div className="rail-heading-group">
            <h2 id="screens-heading" className="section-title">
              Inside the Android build
            </h2>
            <p className="section-sub">
              Scroll sideways across five live captures from Derbis v1.8.0
            </p>
          </div>

          <div className="rail-controls" aria-label="Screenshot gallery navigation">
            <span className="rail-counter">
              {String(activeIndex + 1).padStart(2, "0")} / {String(APP_SCREENS.length).padStart(2, "0")}
            </span>
            <button
              type="button"
              className="icon-btn"
              onClick={() => scrollToIndex(activeIndex - 1)}
              disabled={!canPrev && activeIndex === 0}
              aria-label="Scroll to previous screen"
            >
              <ArrowLeftIcon />
            </button>
            <button
              type="button"
              className="icon-btn"
              onClick={() => scrollToIndex(activeIndex + 1)}
              disabled={!canNext && activeIndex === APP_SCREENS.length - 1}
              aria-label="Scroll to next screen"
            >
              <ArrowRightIcon />
            </button>
          </div>
        </div>

        <div className="rail-tabs" role="tablist" aria-label="Jump to app screen">
          {APP_SCREENS.map((screen, idx) => {
            const isActive = idx === activeIndex;
            return (
              <button
                key={screen.id}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={isActive ? "rail-tab is-active" : "rail-tab"}
                onClick={() => scrollToIndex(idx)}
              >
                <span className="tab-num">{screen.index}</span>
                <span>{screen.title}</span>
              </button>
            );
          })}
        </div>

        <div className="rail-progress" aria-hidden="true">
          <div
            className="rail-progress-fill"
            style={{ transform: `scaleX(${railProgress.toFixed(4)})` }}
          />
        </div>
      </div>

      <div className="rail-viewport">
        <div
          ref={railRef}
          className="screens-rail"
          role="region"
          aria-label="Derbis Android screenshots, scrollable left and right"
          tabIndex={0}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={endPointerDrag}
          onPointerCancel={endPointerDrag}
          onPointerLeave={endPointerDrag}
          onKeyDown={(event) => {
            if (event.key === "ArrowRight") {
              event.preventDefault();
              scrollToIndex(activeIndex + 1);
            } else if (event.key === "ArrowLeft") {
              event.preventDefault();
              scrollToIndex(activeIndex - 1);
            }
          }}
        >
          {APP_SCREENS.map((screen, idx) => {
            const isCurrent = idx === activeIndex;
            return (
              <article
                key={screen.id}
                ref={(el) => {
                  cardRefs.current[idx] = el;
                }}
                className={isCurrent ? "shot-card is-current" : "shot-card"}
              >
                <div className="shot-top">
                  <div className="shot-index-group">
                    <span className="shot-index">{screen.index}</span>
                    <span className="shot-tag">{screen.category}</span>
                  </div>
                  <button
                    type="button"
                    className="shot-expand"
                    onClick={() => setInspectedIndex(idx)}
                    aria-label={`Inspect ${screen.title} full screen`}
                  >
                    <ExpandIcon />
                  </button>
                </div>

                <div
                  className="shot-frame"
                  style={{ backgroundColor: screen.stageBg }}
                  onClick={() => {
                    if (!dragState.current.moved) {
                      setInspectedIndex(idx);
                    }
                  }}
                >
                  <img
                    src={screen.src}
                    alt={screen.alt}
                    width={screen.width}
                    height={screen.height}
                    loading={idx < 2 ? "eager" : "lazy"}
                    decoding="async"
                    draggable={false}
                    className="shot-img"
                  />
                </div>

                <div className="shot-meta">
                  <h3 className="shot-title">{screen.title}</h3>
                  <p className="shot-caption">{screen.caption}</p>
                </div>
              </article>
            );
          })}
        </div>
      </div>

      {inspected && (
        <div
          className="lightbox-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="lightbox-title"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setInspectedIndex(null);
            }
          }}
        >
          <div className="lightbox-panel">
            <div className="lightbox-head">
              <div className="lightbox-title-row">
                <span className="shot-index">{inspected.index}</span>
                <h3 id="lightbox-title" className="lightbox-title">
                  {inspected.title}
                </h3>
                <span className="shot-tag">{inspected.category}</span>
              </div>
              <div className="lightbox-actions">
                <button
                  type="button"
                  className="icon-btn"
                  onClick={() =>
                    setInspectedIndex((prev) => (prev - 1 + APP_SCREENS.length) % APP_SCREENS.length)
                  }
                  aria-label="Previous screenshot"
                >
                  <ArrowLeftIcon />
                </button>
                <button
                  type="button"
                  className="icon-btn"
                  onClick={() => setInspectedIndex((prev) => (prev + 1) % APP_SCREENS.length)}
                  aria-label="Next screenshot"
                >
                  <ArrowRightIcon />
                </button>
                <button
                  type="button"
                  className="icon-btn"
                  onClick={() => setInspectedIndex(null)}
                  aria-label="Close screenshot viewer"
                >
                  <CloseIcon />
                </button>
              </div>
            </div>

            <div className="lightbox-stage" style={{ backgroundColor: inspected.stageBg }}>
              <img
                src={inspected.src}
                alt={inspected.alt}
                width={inspected.width}
                height={inspected.height}
                className="lightbox-img"
              />
            </div>

            <p className="lightbox-detail">{inspected.detail}</p>
          </div>
        </div>
      )}
    </section>
  );
}
