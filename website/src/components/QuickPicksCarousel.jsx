import React, { useRef } from "react";
import { Play, Pause, Plus, ChevronLeft, ChevronRight, BarChart2 } from "lucide-react";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function QuickPicksCarousel() {
  const { currentTrack, isPlaying, playTrack, togglePlayPause, addToQueue } = usePlayer();
  const scrollContainerRef = useRef(null);

  // Group 20 tracks into chunks of 4 rows per column (5 columns total)
  const rowsCount = 4;
  const columns = [];
  for (let i = 0; i < QUICK_PICKS_TRACKS.length; i += rowsCount) {
    columns.push(QUICK_PICKS_TRACKS.slice(i, i + rowsCount));
  }

  const scroll = (direction) => {
    if (scrollContainerRef.current) {
      const scrollAmount = direction === "left" ? -400 : 400;
      scrollContainerRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  return (
    <section className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-6">
      {/* Shelf Header */}
      <div className="flex items-end justify-between mb-4">
        <div>
          <span className="text-xs font-bold tracking-wider text-accent uppercase">START RADIO FROM A SONG</span>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Quick Picks</h2>
        </div>

        {/* Carousel controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => scroll("left")}
            aria-label="Scroll left"
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white/80 hover:text-white transition-all"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={() => scroll("right")}
            aria-label="Scroll right"
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white/80 hover:text-white transition-all"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 4-Row Column Scroller */}
      <div
        ref={scrollContainerRef}
        className="flex gap-4 overflow-x-auto pb-4 pt-1 no-scrollbar scroll-smooth snap-x snap-mandatory"
      >
        {columns.map((column, colIdx) => (
          <div
            key={colIdx}
            className="min-w-[300px] sm:min-w-[340px] md:min-w-[360px] flex flex-col gap-2.5 snap-start shrink-0"
          >
            {column.map((track) => {
              const isCurrent = currentTrack?.id === track.id;
              const isCurrentlyPlaying = isCurrent && isPlaying;

              return (
                <div
                  key={track.id}
                  onClick={() => {
                    if (isCurrent) {
                      togglePlayPause();
                    } else {
                      playTrack(track, QUICK_PICKS_TRACKS);
                    }
                  }}
                  className={`group relative flex items-center justify-between p-2.5 rounded-2xl transition-all duration-200 cursor-pointer border ${
                    isCurrent
                      ? "bg-accent/15 border-accent/40 shadow-lg shadow-accent/10"
                      : "bg-white/[0.03] hover:bg-white/[0.08] border-white/5 hover:border-white/15"
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0 pr-2">
                    {/* Thumbnail with overlay icon */}
                    <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-black/40 shrink-0 shadow-md">
                      <img
                        src={track.thumbnail}
                        alt={track.title}
                        loading="lazy"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      <div
                        className={`absolute inset-0 flex items-center justify-center transition-opacity ${
                          isCurrentlyPlaying
                            ? "bg-black/50 opacity-100"
                            : "bg-black/40 opacity-0 group-hover:opacity-100"
                        }`}
                      >
                        {isCurrentlyPlaying ? (
                          <div className="flex items-center gap-0.5">
                            <span className="w-1 h-3.5 bg-accent rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                            <span className="w-1 h-4 bg-accent rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                            <span className="w-1 h-2 bg-accent rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                          </div>
                        ) : (
                          <Play className="w-5 h-5 text-white fill-white ml-0.5" />
                        )}
                      </div>
                    </div>

                    {/* Metadata */}
                    <div className="min-w-0 flex-1">
                      <p
                        className={`text-sm font-semibold truncate transition-colors ${
                          isCurrent ? "text-accent font-bold" : "text-white group-hover:text-white"
                        }`}
                      >
                        {track.title}
                      </p>
                      <p className="text-xs text-white/50 truncate group-hover:text-white/70">
                        {track.artist}
                      </p>
                    </div>
                  </div>

                  {/* Actions & Duration */}
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="text-xs text-white/40 font-mono hidden sm:inline">
                      {track.duration}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        addToQueue(track);
                      }}
                      title="Add to Up Next queue"
                      className="p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/10 transition-colors"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </section>
  );
}
