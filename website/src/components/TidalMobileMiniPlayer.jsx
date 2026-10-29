import React from "react";
import { Play, Pause, SkipForward, Sparkles } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalMobileMiniPlayer() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayPause,
    nextTrack,
    setIsMobilePlayerExpanded,
    audioQuality
  } = usePlayer();

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="md:hidden fixed bottom-[60px] left-2 right-2 z-30 select-none">
      <div
        onClick={() => setIsMobilePlayerExpanded(true)}
        className="relative overflow-hidden flex items-center justify-between p-2 rounded-xl bg-[#121217]/95 border border-[#24242e] shadow-[0_8px_24px_rgba(0,0,0,0.8)] backdrop-blur-xl cursor-pointer"
      >
        {/* Track Info */}
        <div className="flex items-center gap-2.5 min-w-0 flex-1 pr-2">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-10 h-10 rounded-lg object-cover shrink-0 shadow"
          />
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-bold text-white truncate">
                {currentTrack.title}
              </span>
              <span className="px-1 rounded text-[8px] font-black text-[#00e5ff] bg-[#00e5ff]/15 shrink-0">
                {audioQuality}
              </span>
            </div>
            <span className="text-[11px] text-white/50 truncate block">
              {currentTrack.artist}
            </span>
          </div>
        </div>

        {/* Quick Controls */}
        <div className="flex items-center gap-1 shrink-0" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={togglePlayPause}
            className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center shadow"
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

        {/* Progress bar line */}
        <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-white/10">
          <div
            className="h-full bg-[#00e5ff] transition-all"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </div>
    </div>
  );
}
