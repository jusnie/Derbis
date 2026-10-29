import React, { useEffect, useRef } from "react";
import { X, Mic2, Music } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { SYNCHRONIZED_LYRICS } from "../data/lyrics";

export default function LyricsModal() {
  const { isLyricsOpen, setIsLyricsOpen, currentTrack, currentTime, seekTo } = usePlayer();
  const activeLineRef = useRef(null);

  const lyrics = SYNCHRONIZED_LYRICS[currentTrack?.id] || [
    { time: 0, text: `♪ Listening to ${currentTrack?.title} ♪` },
    { time: 5, text: `Performed by ${currentTrack?.artist}` },
    { time: 12, text: "Lyrics synced live with web player engine" },
    { time: 20, text: "Feel the rhythm in hi-fi lossless quality" },
    { time: 35, text: "Tap on any line to seek instantly" },
    { time: 60, text: `♪ ${currentTrack?.title} - ${currentTrack?.artist} ♪` }
  ];

  // Find active line index based on currentTime
  let activeIndex = -1;
  for (let i = 0; i < lyrics.length; i++) {
    if (currentTime >= lyrics[i].time) {
      activeIndex = i;
    }
  }

  // Auto-scroll to active line
  useEffect(() => {
    if (activeLineRef.current) {
      activeLineRef.current.scrollIntoView({
        behavior: "smooth",
        block: "center"
      });
    }
  }, [activeIndex]);

  if (!isLyricsOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex items-center justify-center p-4 sm:p-6">
      {/* Dimmed glass backdrop */}
      <div
        onClick={() => setIsLyricsOpen(false)}
        className="absolute inset-0 bg-black/80 backdrop-blur-2xl transition-opacity"
      />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-2xl h-[80vh] rounded-3xl bg-gradient-to-b from-[#181824]/95 to-[#0c0c14]/95 border border-white/15 shadow-[0_25px_60px_rgba(0,0,0,0.9)] flex flex-col overflow-hidden z-10">
        
        {/* Header */}
        <div className="p-5 border-b border-white/10 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-accent/20 text-accent">
              <Mic2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white truncate max-w-xs sm:max-w-md">
                {currentTrack.title}
              </h3>
              <p className="text-xs text-white/50">{currentTrack.artist}</p>
            </div>
          </div>
          <button
            onClick={() => setIsLyricsOpen(false)}
            className="p-2 rounded-xl text-white/50 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Synchronized Lyrics Container */}
        <div className="flex-1 overflow-y-auto p-6 sm:p-10 space-y-6 text-center select-none no-scrollbar">
          {lyrics.map((line, idx) => {
            const isActive = idx === activeIndex;
            const isPast = idx < activeIndex;

            return (
              <p
                key={idx}
                ref={isActive ? activeLineRef : null}
                onClick={() => seekTo(line.time)}
                className={`cursor-pointer transition-all duration-300 font-extrabold ${
                  isActive
                    ? "text-2xl sm:text-3xl text-white scale-105 drop-shadow-[0_4px_24px_rgba(255,45,85,0.6)]"
                    : isPast
                    ? "text-lg sm:text-xl text-white/40 hover:text-white/70"
                    : "text-lg sm:text-xl text-white/25 hover:text-white/60"
                }`}
              >
                {line.text}
              </p>
            );
          })}
        </div>

        {/* Footer info */}
        <div className="p-3 border-t border-white/10 text-center bg-black/40">
          <span className="text-[11px] text-white/40 tracking-wider">
            TAP ANY LINE TO JUMP AUDIO • TIME-SYNCED LYRICS
          </span>
        </div>

      </div>
    </div>
  );
}
