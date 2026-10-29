import React from "react";
import { Play, Pause, SkipForward } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalMobileMiniPlayer() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayPause,
    nextTrack,
    setIsMobilePlayerExpanded
  } = usePlayer();

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="md:hidden fixed bottom-[60px] left-2 right-2 z-30 select-none">
      <div
        onClick={() => setIsMobilePlayerExpanded(true)}
        className="relative overflow-hidden flex items-center justify-between p-2 rounded-xl bg-[#141418] border border-[#22222a] shadow-[0_8px_24px_rgba(0,0,0,0.9)] backdrop-blur-xl cursor-pointer"
      >
        {/* Track Info */}
        <div className="flex items-center gap-2.5 min-w-0 flex-1 pr-2">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-10 h-10 rounded-lg object-cover shrink-0 shadow"
          />
          <div className="min-w-0 flex-1">
            <h4 className="text-xs font-bold text-white truncate leading-tight">
              {currentTrack.title}
            </h4>
            <span className="text-[11px] text-white/50 truncate block mt-0.5">
              {currentTrack.artist}
            </span>
          </div>
        </div>

        {/* Quick Controls */}
        <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={togglePlayPause}
            className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center shadow hover:scale-105 active:scale-95 transition-transform"
            title={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? (
              <Pause className="w-3.5 h-3.5 fill-current" />
            ) : (
              <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
            )}
          </button>

          <button
            onClick={nextTrack}
            className="p-2 text-white/70 hover:text-white"
            title="Next"
          >
            <SkipForward className="w-4 h-4 fill-current" />
          </button>
        </div>

        {/* Clean white progress bar line */}
        <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-white/10">
          <div
            className="h-full bg-white transition-all"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </div>
    </div>
  );
}
