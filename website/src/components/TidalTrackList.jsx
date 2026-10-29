import React from "react";
import { Play, Pause, Plus, Heart, MoreHorizontal, Sparkles } from "lucide-react";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTrackList() {
  const { currentTrack, isPlaying, playTrack, togglePlayPause, addToQueue } = usePlayer();

  return (
    <section className="mt-8 select-none">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
            Top Tracks & Quick Picks
          </h2>
          <p className="text-xs text-white/50 mt-0.5">
            Streamed in High-Fidelity Master Audio
          </p>
        </div>

        <button
          onClick={() => playTrack(QUICK_PICKS_TRACKS[0], QUICK_PICKS_TRACKS)}
          className="text-xs font-bold text-[#00e5ff] hover:underline"
        >
          Play All
        </button>
      </div>

      {/* Table Header */}
      <div className="hidden sm:grid grid-cols-12 gap-4 px-3 py-2 border-b border-[#1a1a20] text-[11px] font-bold tracking-wider text-white/40 uppercase">
        <div className="col-span-1 text-center">#</div>
        <div className="col-span-6">Title</div>
        <div className="col-span-3">Artist</div>
        <div className="col-span-2 text-right">Time</div>
      </div>

      {/* Track Rows */}
      <div className="divide-y divide-white/[0.04] mt-1">
        {QUICK_PICKS_TRACKS.map((track, idx) => {
          const isCurrent = currentTrack?.id === track.id;
          const isCurrentlyPlaying = isCurrent && isPlaying;

          return (
            <div
              key={track.id}
              onClick={() => {
                if (isCurrent) togglePlayPause();
                else playTrack(track, QUICK_PICKS_TRACKS);
              }}
              className={`group flex sm:grid sm:grid-cols-12 items-center gap-3 sm:gap-4 px-3 py-2.5 rounded-xl cursor-pointer transition-colors ${
                isCurrent
                  ? "bg-[#00e5ff]/10 text-white"
                  : "hover:bg-white/[0.04] text-white/80 hover:text-white"
              }`}
            >
              {/* Number or Equalizer */}
              <div className="col-span-1 hidden sm:flex items-center justify-center text-xs font-mono">
                {isCurrentlyPlaying ? (
                  <div className="flex items-center gap-0.5">
                    <span className="w-0.5 h-3 bg-[#00e5ff] rounded-full animate-bounce" />
                    <span className="w-0.5 h-4 bg-[#00e5ff] rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="w-0.5 h-2 bg-[#00e5ff] rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
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
                <div className="relative w-10 h-10 rounded-lg overflow-hidden bg-[#16161c] shrink-0">
                  <img
                    src={track.thumbnail}
                    alt={track.title}
                    loading="lazy"
                    className="w-full h-full object-cover"
                  />
                  {isCurrentlyPlaying && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center sm:hidden">
                      <span className="w-1 h-3 bg-[#00e5ff] rounded-full animate-bounce" />
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs sm:text-sm font-bold truncate ${
                        isCurrent ? "text-[#00e5ff]" : "text-white"
                      }`}
                    >
                      {track.title}
                    </span>
                    <span className="px-1 py-0.2 rounded text-[9px] font-black text-[#00e5ff] bg-[#00e5ff]/15 border border-[#00e5ff]/30 uppercase shrink-0">
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
                  {track.duration}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
