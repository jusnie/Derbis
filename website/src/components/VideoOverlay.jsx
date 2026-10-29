import React from "react";
import { X, Minimize2, Music2 } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function VideoOverlay() {
  const { playbackMode, setPlaybackMode, currentTrack } = usePlayer();

  if (playbackMode !== "video") return null;

  return (
    <div className="fixed top-20 right-4 sm:right-8 z-40 w-[320px] sm:w-[420px] aspect-video rounded-3xl overflow-hidden bg-black/90 border border-white/20 shadow-2xl backdrop-blur-3xl animate-scaleIn">
      {/* Header bar */}
      <div className="absolute top-0 inset-x-0 z-20 flex items-center justify-between p-3 bg-gradient-to-b from-black/80 to-transparent">
        <span className="text-xs font-bold text-white/90 truncate max-w-[200px]">
          {currentTrack?.title}
        </span>
        <button
          onClick={() => setPlaybackMode("song")}
          className="p-1 rounded-lg bg-black/60 text-white/70 hover:text-white border border-white/10"
          title="Switch to Song Mode (Artwork Only)"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Target for video player when in video mode */}
      <div id="video-theater-slot" className="w-full h-full relative" />
    </div>
  );
}
