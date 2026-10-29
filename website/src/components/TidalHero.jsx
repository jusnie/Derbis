import React, { useState } from "react";
import { Play, Pause, Heart, Sparkles, Video, Music2, Radio } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import ThreeVisualizer from "./ThreeVisualizer";

export default function TidalHero() {
  const {
    currentTrack,
    isPlaying,
    playTrack,
    togglePlayPause,
    playbackMode,
    setPlaybackMode,
    endlessListening,
    toggleEndlessListening
  } = usePlayer();

  const [visualMode, setVisualMode] = useState("cover"); // "cover" | "3d"
  const [isLiked, setIsLiked] = useState(false);

  const handlePlayHero = () => {
    if (isPlaying) {
      togglePlayPause();
    } else {
      playTrack(QUICK_PICKS_TRACKS[0], QUICK_PICKS_TRACKS);
    }
  };

  return (
    <section className="relative overflow-hidden rounded-2xl bg-[#0c0c10] border border-[#1a1a22] p-5 sm:p-8 select-none">
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
        
        {/* Left: Metadata & Actions */}
        <div className="md:col-span-7 flex flex-col justify-center space-y-4">
          
          {/* Eyebrow */}
          <div className="flex items-center gap-2">
            <span className="px-2 py-0.5 rounded bg-[#00e5ff]/15 text-[#00e5ff] text-[10px] font-black tracking-widest uppercase border border-[#00e5ff]/30">
              HI-RES LOSSLESS
            </span>
            <span className="text-[11px] font-bold text-white/40 uppercase tracking-wider">
              TIDAL MASTER AUDIO • 24-BIT / 96 KHZ
            </span>
          </div>

          {/* Title & Artist */}
          <div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight">
              {currentTrack.title}
            </h1>
            <p className="text-base sm:text-lg font-semibold text-white/70 mt-1 flex items-center gap-2">
              <span>{currentTrack.artist}</span>
              {currentTrack.year && (
                <span className="text-xs text-white/40">• {currentTrack.year}</span>
              )}
            </p>
          </div>

          {/* Action Row */}
          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              onClick={handlePlayHero}
              className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-[#00e5ff] hover:bg-[#33ebff] text-black font-extrabold text-xs uppercase tracking-wider shadow-lg shadow-[#00e5ff]/20 hover:scale-105 active:scale-95 transition-all"
            >
              {isPlaying ? (
                <>
                  <Pause className="w-4 h-4 fill-current" />
                  <span>Pause</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current ml-0.5" />
                  <span>Play</span>
                </>
              )}
            </button>

            <button
              onClick={() => setIsLiked(!isLiked)}
              className={`p-2 rounded-full border transition-all ${
                isLiked
                  ? "border-pink-500 bg-pink-500/20 text-pink-500"
                  : "border-white/10 hover:border-white/30 text-white/70 hover:text-white"
              }`}
              title={isLiked ? "In Favorites" : "Add to Favorites"}
            >
              <Heart className={`w-4 h-4 ${isLiked ? "fill-current" : ""}`} />
            </button>

            {/* Song vs Video mode */}
            <button
              onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${
                playbackMode === "video"
                  ? "bg-purple-600/30 border-purple-500/50 text-purple-300"
                  : "bg-white/[0.04] border-white/10 text-white/70 hover:text-white"
              }`}
            >
              {playbackMode === "video" ? <Video className="w-3.5 h-3.5" /> : <Music2 className="w-3.5 h-3.5 text-[#00e5ff]" />}
              <span>{playbackMode === "video" ? "Video Mode" : "Audio Only"}</span>
            </button>

            {/* Endless listening */}
            <button
              onClick={toggleEndlessListening}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${
                endlessListening
                  ? "bg-emerald-500/15 border-emerald-500/40 text-emerald-400"
                  : "bg-white/[0.04] border-white/10 text-white/50"
              }`}
            >
              <Radio className={`w-3.5 h-3.5 ${endlessListening ? "animate-pulse" : ""}`} />
              <span>Track Radio: {endlessListening ? "ON" : "OFF"}</span>
            </button>
          </div>

        </div>

        {/* Right: Artwork or 3D Visualizer Canvas */}
        <div className="md:col-span-5 flex flex-col items-center justify-center">
          <div className="relative w-full max-w-[280px] sm:max-w-[320px] aspect-square rounded-2xl overflow-hidden bg-[#141418] border border-[#22222a] shadow-2xl">
            {visualMode === "cover" ? (
              <img
                src={currentTrack.thumbnail}
                alt={currentTrack.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full">
                <ThreeVisualizer className="w-full h-full" />
              </div>
            )}

            {/* 3D vs Cover toggle pill */}
            <div className="absolute top-3 right-3 flex items-center p-0.5 rounded-lg bg-black/70 backdrop-blur-md border border-white/15">
              <button
                onClick={() => setVisualMode("cover")}
                className={`px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                  visualMode === "cover" ? "bg-white text-black" : "text-white/60 hover:text-white"
                }`}
              >
                Cover
              </button>
              <button
                onClick={() => setVisualMode("3d")}
                className={`flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                  visualMode === "3d" ? "bg-[#00e5ff] text-black" : "text-white/60 hover:text-white"
                }`}
              >
                <Sparkles className="w-2.5 h-2.5" />
                <span>3D Aura</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </section>
  );
}
