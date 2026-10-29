import React, { useRef } from "react";
import { Play, Pause, ChevronLeft, ChevronRight, Plus, MoreHorizontal } from "lucide-react";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function QuickPicksShelf() {
  const { currentTrack, isPlaying, playTrack, togglePlayPause, addToQueue } = usePlayer();
  const shelfRef = useRef(null);

  // Group 20 tracks into 5 columns of 4 tracks each
  const rowsPerColumn = 4;
  const columns = [];
  for (let i = 0; i < QUICK_PICKS_TRACKS.length; i += rowsPerColumn) {
    columns.push(QUICK_PICKS_TRACKS.slice(i, i + rowsPerColumn));
  }

  const scrollShelf = (direction) => {
    if (shelfRef.current) {
      const scrollAmount = direction === "left" ? -380 : 380;
      shelfRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  const handlePlayAll = () => {
    playTrack(QUICK_PICKS_TRACKS[0], QUICK_PICKS_TRACKS);
  };

  return (
    <section className="mt-8 select-none">
      {/* Section Heading & Shelf Controls */}
      <div className="flex items-end justify-between gap-4 mb-4">
        <div>
          <p className="text-[10px] font-black tracking-widest text-white/40 uppercase">
            START RADIO FROM A SONG
          </p>
          <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight mt-0.5">
            Quick Picks
          </h2>
        </div>

        <div className="flex items-center gap-2">
          {/* Nav arrows */}
          <button
            onClick={() => scrollShelf("left")}
            aria-label="Previous Quick Picks"
            className="w-9 h-9 rounded-full border border-white/10 hover:border-white/25 bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/70 hover:text-white transition-all"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          <button
            onClick={() => scrollShelf("right")}
            aria-label="Next Quick Picks"
            className="w-9 h-9 rounded-full border border-white/10 hover:border-white/25 bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/70 hover:text-white transition-all"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          {/* Play All button */}
          <button
            onClick={handlePlayAll}
            className="flex items-center gap-2 h-9 px-3 rounded-full text-xs font-bold text-blue-400 hover:text-white hover:bg-blue-500/10 transition-colors"
          >
            <span className="w-6 h-6 rounded-full bg-blue-500 text-[#06142b] flex items-center justify-center">
              <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
            </span>
            <span>Play all</span>
          </button>
        </div>
      </div>

      {/* 4-Row Column Shelf */}
      <div
        ref={shelfRef}
        className="flex gap-4 overflow-x-auto pb-4 no-scrollbar scroll-smooth snap-x snap-mandatory"
      >
        {columns.map((column, colIdx) => (
          <div
            key={colIdx}
            className="min-w-[280px] sm:min-w-[320px] md:min-w-[340px] flex flex-col gap-1.5 snap-start shrink-0"
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
                  className={`group relative flex items-center justify-between p-2 rounded-xl border transition-all cursor-pointer ${
                    isCurrent
                      ? "bg-[#3dd9eb]/10 border-[#3dd9eb]/30 shadow-sm"
                      : "bg-transparent hover:bg-white/[0.05] border-transparent hover:border-white/5"
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1 pr-2">
                    {/* Thumbnail with playing bars overlay */}
                    <div className="relative w-12 h-12 rounded-lg overflow-hidden bg-[#1c1d22] shrink-0 shadow-md">
                      <img
                        src={track.thumbnail}
                        alt={track.title}
                        loading="lazy"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      {isCurrentlyPlaying && (
                        <div className="absolute inset-0 bg-black/60 flex items-center justify-center gap-0.5">
                          <span className="w-1 h-3.5 bg-[#3dd9eb] rounded-full animate-bounce" />
                          <span className="w-1 h-4 bg-[#3dd9eb] rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                          <span className="w-1 h-2 bg-[#3dd9eb] rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                        </div>
                      )}
                    </div>

                    {/* Metadata */}
                    <div className="min-w-0 flex-1">
                      <strong
                        className={`block text-xs sm:text-sm font-bold truncate transition-colors ${
                          isCurrent ? "text-[#3dd9eb]" : "text-white group-hover:text-white"
                        }`}
                      >
                        {track.title}
                      </strong>
                      <small className="block text-[11px] text-white/50 truncate">
                        {track.artist}
                      </small>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        addToQueue(track);
                      }}
                      title="Add to queue"
                      className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/10 transition-colors"
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
