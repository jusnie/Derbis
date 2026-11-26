import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { APP_SCREENS } from "../data/siteData";
import { ArrowLeftIcon, ArrowRightIcon, CloseIcon } from "./Icons";

const SCREEN_COUNT = APP_SCREENS.length;
const AUTO_SWIPE_MS = 3200;
const RESUME_IDLE_MS = 3200;

export const ScreenGallery = memo(function ScreenGallery() {
  const loopScreens = useMemo(
    () => [
      ...APP_SCREENS.map((s) => ({ ...s, loopKey: `a-${s.id}` })),
      ...APP_SCREENS.map((s) => ({ ...s, loopKey: `b-${s.id}` })),
      ...APP_SCREENS.map((s) => ({ ...s, loopKey: `c-${s.id}` }))
    ],
    []
  );

  const railRef = useRef(null);
  const cardRefs = useRef([]);
  const metricsRef = useRef({ padStart: 24, setSpan: 0, cardStep: 276 });
  const interactingRef = useRef(false);
  const lastInteractionRef = useRef(0);
  const dragRef = useRef({ active: false, moved: false, startX: 0, startScrollLeft: 0 });

  const [activeRealIndex, setActiveRealIndex] = useState(0);
  const [inspectedIndex, setInspectedIndex] = useState(null);

  const measureMetrics = useCallback(() => {
    const first = cardRefs.current[0];
    const second = cardRefs.current[1];
    const middle = cardRefs.current[SCREEN_COUNT];
    if (!first || !middle) return;

    const padStart = first.offsetLeft;
    const setSpan = middle.offsetLeft - first.offsetLeft;
    const cardStep = second ? Math.max(1, second.offsetLeft - first.offsetLeft) : Math.max(1, setSpan / SCREEN_COUNT);
    metricsRef.current = { padStart, setSpan, cardStep };
  }, []);

  const findClosestLoopIndex = useCallback(() => {
    const rail = railRef.current;
    if (!rail) return SCREEN_COUNT;
    const { cardStep } = metricsRef.current;
    if (cardStep <= 0) return SCREEN_COUNT;
    const rawIdx = Math.round(rail.scrollLeft / cardStep);
    return Math.max(0, Math.min(loopScreens.length - 1, rawIdx));
  }, [loopScreens.length]);

  const normalizeLoopPosition = useCallback(() => {
    const rail = railRef.current;
    if (!rail) return;
    const { setSpan } = metricsRef.current;
    if (setSpan <= 0) return;

    if (rail.scrollLeft < setSpan * 0.5) {
      rail.scrollLeft += setSpan;
    } else if (rail.scrollLeft >= setSpan * 1.5) {
      rail.scrollLeft -= setSpan;
    }
  }, []);

  useEffect(() => {
    const rail = railRef.current;
    if (!rail) return undefined;

    const placeAtMiddleStart = () => {
      measureMetrics();
      const { setSpan } = metricsRef.current;
      if (setSpan > 0) {
        rail.scrollLeft = setSpan;
      }
    };

    placeAtMiddleStart();
    const raf = window.requestAnimationFrame(placeAtMiddleStart);

    const onResize = () => {
      measureMetrics();
    };
    window.addEventListener("resize", onResize, { passive: true });

    let settleTimer = 0;
    let scrollRaf = 0;
    const onScroll = () => {
      if (!scrollRaf) {
        scrollRaf = window.requestAnimationFrame(() => {
          scrollRaf = 0;
          const closest = findClosestLoopIndex();
          const nextReal = closest % SCREEN_COUNT;
          setActiveRealIndex((prev) => (prev === nextReal ? prev : nextReal));
        });
      }

      if (settleTimer) window.clearTimeout(settleTimer);
      settleTimer = window.setTimeout(() => {
        if (!interactingRef.current && !dragRef.current.active) {
          normalizeLoopPosition();
        }
      }, 200);
    };

    rail.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.cancelAnimationFrame(raf);
      if (scrollRaf) window.cancelAnimationFrame(scrollRaf);
      if (settleTimer) window.clearTimeout(settleTimer);
      window.removeEventListener("resize", onResize);
      rail.removeEventListener("scroll", onScroll);
    };
  }, [findClosestLoopIndex, measureMetrics, normalizeLoopPosition]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      if (inspectedIndex !== null) return;
      if (document.hidden) return;
      if (interactingRef.current || dragRef.current.active) return;
      if (Date.now() - lastInteractionRef.current < RESUME_IDLE_MS) return;

      const rail = railRef.current;
      if (!rail) return;

      normalizeLoopPosition();
      const currentIdx = findClosestLoopIndex();
      const safeIdx =
        currentIdx < SCREEN_COUNT || currentIdx >= SCREEN_COUNT * 2
          ? SCREEN_COUNT + (currentIdx % SCREEN_COUNT)
          : currentIdx;

      const { cardStep } = metricsRef.current;
      rail.scrollTo({
        left: Math.max(0, (safeIdx + 1) * cardStep),
        behavior: "smooth"
      });
    }, AUTO_SWIPE_MS);

    return () => window.clearInterval(interval);
  }, [findClosestLoopIndex, inspectedIndex, normalizeLoopPosition]);

  const markTouchStart = () => {
    interactingRef.current = true;
    lastInteractionRef.current = Date.now();
  };

  const markTouchEnd = () => {
    interactingRef.current = false;
    lastInteractionRef.current = Date.now();
  };

  const handlePointerDown = (event) => {
    dragRef.current.moved = false;
    lastInteractionRef.current = Date.now();
    if (event.pointerType === "touch") {
      interactingRef.current = true;
      return;
    }

    const rail = railRef.current;
    if (!rail) return;

    interactingRef.current = true;
    dragRef.current = {
      active: true,
      moved: false,
      startX: event.clientX,
      startScrollLeft: rail.scrollLeft
    };
    rail.classList.add("is-dragging");
  };

  const handlePointerMove = (event) => {
    const state = dragRef.current;
    const rail = railRef.current;
    if (!state.active || !rail) return;

    const delta = event.clientX - state.startX;
    if (Math.abs(delta) > 6) {
      state.moved = true;
    }
    rail.scrollLeft = state.startScrollLeft - delta;
    lastInteractionRef.current = Date.now();
  };

  const endPointerDrag = () => {
    const rail = railRef.current;
    if (rail) rail.classList.remove("is-dragging");
    dragRef.current.active = false;
    interactingRef.current = false;
    lastInteractionRef.current = Date.now();
  };

  useEffect(() => {
    if (inspectedIndex === null) return undefined;

    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        setInspectedIndex(null);
      } else if (event.key === "ArrowRight") {
        setInspectedIndex((prev) => (prev + 1) % SCREEN_COUNT);
      } else if (event.key === "ArrowLeft") {
        setInspectedIndex((prev) => (prev - 1 + SCREEN_COUNT) % SCREEN_COUNT);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [inspectedIndex]);

  const inspected = inspectedIndex !== null ? APP_SCREENS[inspectedIndex] : null;

  return (
    <section id="screens" className="screens-showcase" aria-label="Derbis Android app screens">
      <div className="rail-viewport">
        <div
          ref={railRef}
          className="screens-rail"
          role="region"
          aria-label="Derbis Android screenshots, auto-swiping and swipeable left or right"
          tabIndex={0}
          onTouchStart={markTouchStart}
          onTouchMove={() => {
            lastInteractionRef.current = Date.now();
          }}
          onTouchEnd={markTouchEnd}
          onTouchCancel={markTouchEnd}
          onWheel={() => {
            lastInteractionRef.current = Date.now();
          }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={endPointerDrag}
          onPointerCancel={endPointerDrag}
          onPointerLeave={() => {
            if (dragRef.current.active) endPointerDrag();
          }}
        >
          {loopScreens.map((screen, idx) => {
            const realIdx = idx % SCREEN_COUNT;
            const isCurrent = realIdx === activeRealIndex;
            return (
              <article
                key={screen.loopKey}
                ref={(el) => {
                  cardRefs.current[idx] = el;
                }}
                className={isCurrent ? "shot-card is-current" : "shot-card"}
              >
                <div className="shot-top">
                  <span className="shot-index">{screen.index}</span>
                  <span className="shot-tag">{screen.category}</span>
                </div>

                <div
                  className="shot-frame"
                  onClick={() => {
                    if (!dragRef.current.moved) {
                      setInspectedIndex(realIdx);
                    }
                  }}
                >
                  <img
                    src={screen.src}
                    alt={screen.alt}
                    width={screen.width}
                    height={screen.height}
                    loading="eager"
                    decoding="async"
                    draggable={false}
                    className="shot-img"
                  />
                </div>

                <div className="shot-meta">
                  <h2 className="shot-title">{screen.title}</h2>
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
                    setInspectedIndex((prev) => (prev - 1 + SCREEN_COUNT) % SCREEN_COUNT)
                  }
                  aria-label="Previous screenshot"
                >
                  <ArrowLeftIcon />
                </button>
                <button
                  type="button"
                  className="icon-btn"
                  onClick={() => setInspectedIndex((prev) => (prev + 1) % SCREEN_COUNT)}
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

            <div className="lightbox-stage">
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
});
