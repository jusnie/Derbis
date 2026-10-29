import React from "react";
import { Play, Pause, Plus, X, Sparkles, CheckCircle2 } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTrackList() {
  const {
    currentTrack,
    isPlaying,
    playTrack,
    togglePlayPause,
    addToQueue,
    displayedQuickPicks,
    categoryFilterTitle,
    clearCategoryFilter,
    activeSubTab,
    userProfile
  } = usePlayer();

  // Dynamic heading based on selection or YouTube sync
  let sectionTitle = "Quick Picks";
  let sectionSubtitle = "Recommended tracks for you";

  if (categoryFilterTitle) {
    sectionTitle = categoryFilterTitle;
    sectionSubtitle = "Filtered category tracks";
  } else if (userProfile?.connected && activeSubTab === "for_you") {
    sectionTitle = "Listen again";
    sectionSubtitle = "Based on your YouTube Music liked songs & history";
  } else if (activeSubTab === "staff_picks") {
    sectionTitle = "Staff Picks";
    sectionSubtitle = "Curated editorial selections by Derbis editors";
  } else if (activeSubTab === "uploads") {
    sectionTitle = "Trending Uploads";
    sectionSubtitle = "Top independent releases and uploads";
  }

  return (
    <section className="mt-8 select-none">
      {/* Header with Active Filter Pill */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4 px-0.5">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
              {sectionTitle}
            </h2>
            {categoryFilterTitle && (
              <button
                onClick={clearCategoryFilter}
                className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white/10 hover:bg-white/20 border border-white/20 text-[11px] font-bold text-white transition-colors"
                title="Clear category filter"
              >
                <span>Filtered</span>
                <X className="w-3 h-3" />
              </button>
            )}
          </div>
          <p className="text-xs text-white/50 mt-0.5">
            {sectionSubtitle}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => displayedQuickPicks.length > 0 && playTrack(displayedQuickPicks[0], displayedQuickPicks)}
            className="text-xs font-bold text-white/50 hover:text-white transition-colors"
          >
            Play all
          </button>
        </div>
      </div>

      {/* Table Header (Desktop) */}
      <div className="hidden sm:grid grid-cols-12 gap-4 px-3 py-2 border-b border-[#181820] text-[11px] font-bold tracking-wider text-white/40 uppercase">
        <div className="col-span-1 text-center">#</div>
        <div className="col-span-6">Title</div>
        <div className="col-span-3">Artist</div>
        <div className="col-span-2 text-right">Time</div>
      </div>

      {/* Track Rows */}
      <div className="divide-y divide-white/[0.04] mt-1">
        {displayedQuickPicks.map((track, idx) => {
          const isCurrent = (currentTrack?.videoId || currentTrack?.id) === (track.videoId || track.id);
          const isCurrentlyPlaying = isCurrent && isPlaying;

          return (
            <div
              key={track.id || track.videoId || idx}
              onClick={() => {
                if (isCurrent) togglePlayPause();
                else playTrack(track, displayedQuickPicks);
              }}
              className={`group flex sm:grid sm:grid-cols-12 items-center gap-3 sm:gap-4 px-3 py-2.5 rounded-xl cursor-pointer transition-colors ${
                isCurrent
                  ? "bg-white/10 text-white"
                  : "hover:bg-white/[0.04] text-white/80 hover:text-white"
              }`}
            >
              {/* Number or Equalizer */}
              <div className="col-span-1 hidden sm:flex items-center justify-center text-xs font-mono">
                {isCurrentlyPlaying ? (
                  <div className="flex items-center gap-0.5">
                    <span className="w-0.5 h-3 bg-white rounded-full animate-bounce" />
                    <span className="w-0.5 h-4 bg-white rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="w-0.5 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                  </div>
                ) : (
                  <>
                    <span className="group-hover:hidden text-white/40">{idx + 1}</span>
                    <Play className="w-3.5 h-3.5 hidden group-hover:block fill-current" />
                  </>
                )}
              </div>

              {/* Title + Thumbnail + MAX badge */}
              <div className="col-span-6 flex items-center gap-3 min-w-0 flex-1">
                <div className="relative w-10 h-10 rounded-lg overflow-hidden bg-[#16161c] shrink-0 border border-white/10">
                  <img
                    src={track.thumbnail}
                    alt={track.title}
                    loading="lazy"
                    className="w-full h-full object-cover"
                  />
                  {isCurrentlyPlaying && (
                    <div className="absolute inset-0 bg-black/60 flex items-center justify-center sm:hidden">
                      <div className="flex items-center gap-0.5">
                        <span className="w-0.5 h-2.5 bg-white rounded-full animate-bounce" />
                        <span className="w-0.5 h-3.5 bg-white rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                        <span className="w-0.5 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                      </div>
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs sm:text-sm font-bold truncate text-white">
                      {track.title}
                    </span>
                    <span className="px-1.5 py-0.2 rounded text-[9px] font-black text-white/80 bg-white/10 border border-white/15 uppercase shrink-0">
                      MAX
                    </span>
                  </div>
                  <span className="text-[11px] text-white/50 truncate block sm:hidden">
                    {track.artist}
                  </span>
                </div>
              </div>

              {/* Artist (Desktop) */}
              <div className="col-span-3 hidden sm:block text-xs text-white/60 truncate">
                {track.artist}
              </div>

              {/* Duration & Actions */}
              <div className="col-span-2 flex items-center justify-end gap-2 shrink-0">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    addToQueue(track);
                  }}
                  title="Add to queue"
                  className="p-1 rounded text-white/30 hover:text-white hover:bg-white/10 transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
                <span className="text-xs font-mono text-white/40">
                  {track.duration || "3:30"}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
