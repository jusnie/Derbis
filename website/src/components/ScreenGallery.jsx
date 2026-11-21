import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { APP_SCREENS } from "../data/siteData";
import { ArrowLeftIcon, ArrowRightIcon, CloseIcon } from "./Icons";

const SCREEN_COUNT = APP_SCREENS.length;
const AUTO_SWIPE_MS = 2800;
const RESUME_IDLE_MS = 3000;

export function ScreenGallery() {
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
  const interactingRef = useRef(false);
  const lastInteractionRef = useRef(0);
  const dragRef = useRef({ active: false, moved: false, startX: 0, startScrollLeft: 0 });

  const [activeRealIndex, setActiveRealIndex] = useState(0);
  const [inspectedIndex, setInspectedIndex] = useState(null);

  const getPadStart = useCallback((rail) => {
    const first = cardRefs.current[0];
    return first ? first.offsetLeft : 24;
  }, []);

  const getSetSpan = useCallback(() => {
    const first = cardRefs.current[0];
    const middle = cardRefs.current[SCREEN_COUNT];
    if (!first || !middle) return 0;
    return middle.offsetLeft - first.offsetLeft;
  }, []);

  const findClosestLoopIndex = useCallback(() => {
    const rail = railRef.current;
    if (!rail) return SCREEN_COUNT;
    const padStart = getPadStart(rail);
    const current = rail.scrollLeft;

    let bestIdx = SCREEN_COUNT;
    let bestDist = Number.POSITIVE_INFINITY;

    cardRefs.current.forEach((card, idx) => {
      if (!card) return;
      const targetLeft = card.offsetLeft - padStart;
      const dist = Math.abs(targetLeft - current);
      if (dist < bestDist) {
        bestDist = dist;
        bestIdx = idx;
      }
    });

    return bestIdx;
  }, [getPadStart]);

  const normalizeLoopPosition = useCallback(() => {
    const rail = railRef.current;
    if (!rail) return;
    const span = getSetSpan();
    if (span <= 0) return;

    if (rail.scrollLeft < span * 0.5) {
      rail.scrollLeft += span;
    } else if (rail.scrollLeft >= span * 1.5) {
      rail.scrollLeft -= span;
    }
  }, [getSetSpan]);

  useEffect(() => {
    const rail = railRef.current;
    if (!rail) return undefined;

    const placeAtMiddleStart = () => {
      const span = getSetSpan();
      if (span > 0) {
        rail.scrollLeft = span;
      }
    };

    placeAtMiddleStart();
    const raf = window.requestAnimationFrame(placeAtMiddleStart);

    let settleTimer = 0;
    const onScroll = () => {
      const closest = findClosestLoopIndex();
      setActiveRealIndex(closest % SCREEN_COUNT);

      if (settleTimer) window.clearTimeout(settleTimer);
      settleTimer = window.setTimeout(() => {
        if (!interactingRef.current && !dragRef.current.active) {
          normalizeLoopPosition();
        }
      }, 180);
    };

    rail.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.cancelAnimationFrame(raf);
      if (settleTimer) window.clearTimeout(settleTimer);
      rail.removeEventListener("scroll", onScroll);
    };
  }, [findClosestLoopIndex, getSetSpan, normalizeLoopPosition]);

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

      const nextCard = cardRefs.current[safeIdx + 1];
      if (!nextCard) return;

      const padStart = getPadStart(rail);
      rail.scrollTo({
        left: Math.max(0, nextCard.offsetLeft - padStart),
        behavior: "smooth"
      });
    }, AUTO_SWIPE_MS);

    return () => window.clearInterval(interval);
  }, [findClosestLoopIndex, getPadStart, inspectedIndex, normalizeLoopPosition]);

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
                  style={{ backgroundColor: screen.stageBg }}
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
                    loading={idx >= SCREEN_COUNT && idx < SCREEN_COUNT * 2 ? "eager" : "lazy"}
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
