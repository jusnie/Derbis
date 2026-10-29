import React from "react";
import { Play, Pause, Disc, Video, Music2, Radio, Plus, Sparkles, Check } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import ThreeVisualizer from "./ThreeVisualizer";

export default function HeroSection() {
  const {
    currentTrack,
    isPlaying,
    togglePlayPause,
    playbackMode,
    setPlaybackMode,
    endlessListening,
    toggleEndlessListening,
    addToQueue,
    visualizerMode,
    setVisualizerMode
  } = usePlayer();

  return (
    <section className="relative w-full overflow-hidden pt-4 pb-8 px-4 sm:px-6">
      {/* Background radial atmosphere aura */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[500px] bg-gradient-to-tr from-accent/20 via-purple-600/15 to-blue-600/10 rounded-full blur-[120px] pointer-events-none -z-10" />

      <div className="max-w-7xl mx-auto rounded-3xl bg-gradient-to-b from-white/[0.08] to-white/[0.02] border border-white/10 p-6 sm:p-10 shadow-2xl backdrop-blur-3xl">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          
          {/* Left Column: Track Info & Controls */}
          <div className="lg:col-span-7 flex flex-col justify-center space-y-6">
            
            {/* Header badges */}
            <div className="flex flex-wrap items-center gap-2.5">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-accent/20 text-accent border border-accent/30 shadow-sm">
                <Sparkles className="w-3.5 h-3.5 animate-spin-slow" />
                NOW PLAYING SPOTLIGHT
              </span>
              <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-white/10 text-white/80 border border-white/5">
                Lossless 320kbps Web Audio
              </span>
              {endlessListening && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  <Radio className="w-3 h-3 animate-pulse" />
                  Continuous Radio
                </span>
              )}
            </div>

            {/* Song Title & Artist */}
            <div className="space-y-2">
              <h1 className="text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight text-white line-clamp-2 leading-tight">
                {currentTrack.title}
              </h1>
              <p className="text-lg sm:text-2xl font-medium text-white/70 flex items-center gap-2">
                <span>{currentTrack.artist}</span>
                {currentTrack.year && (
                  <span className="text-xs font-semibold text-white/40 px-2 py-0.5 rounded-md bg-white/5 border border-white/10">
                    {currentTrack.year}
                  </span>
                )}
              </p>
            </div>

            {/* Song vs Video Mode Switcher (Signature Derbis Feature) */}
            <div className="flex flex-wrap items-center gap-3 pt-2">
              <div className="inline-flex p-1 rounded-2xl bg-black/40 border border-white/10 backdrop-blur-md">
                <button
                  onClick={() => setPlaybackMode("song")}
                  className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all ${
                    playbackMode === "song"
                      ? "bg-accent text-white shadow-lg shadow-accent/40"
                      : "text-white/60 hover:text-white"
                  }`}
                >
                  <Music2 className="w-4 h-4" />
                  <span>Song (Artwork Only)</span>
                </button>
                <button
                  onClick={() => setPlaybackMode("video")}
                  className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all ${
                    playbackMode === "video"
                      ? "bg-accent text-white shadow-lg shadow-accent/40"
                      : "text-white/60 hover:text-white"
                  }`}
                >
                  <Video className="w-4 h-4" />
                  <span>Video View</span>
                </button>
              </div>

              {/* Endless Listening Toggle */}
              <button
                onClick={toggleEndlessListening}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-2xl border text-xs sm:text-sm font-semibold transition-all ${
                  endlessListening
                    ? "bg-emerald-500/15 border-emerald-500/40 text-emerald-300"
                    : "bg-black/30 border-white/10 text-white/60 hover:text-white"
                }`}
                title="Continuous Radio: When current queue ends, similar songs play automatically"
              >
                <Radio className={`w-4 h-4 ${endlessListening ? "text-emerald-400 animate-pulse" : ""}`} />
                <span>Endless Listening: {endlessListening ? "ON" : "OFF"}</span>
              </button>
            </div>

            {/* Tactile Action Buttons */}
            <div className="flex flex-wrap items-center gap-4 pt-3">
              <button
                onClick={togglePlayPause}
                className="flex items-center gap-3 px-8 py-3.5 rounded-2xl bg-gradient-to-r from-accent to-pink-600 hover:from-pink-600 hover:to-accent text-white font-bold text-base shadow-xl shadow-accent/30 hover:scale-[1.02] active:scale-[0.98] transition-all"
              >
                {isPlaying ? (
                  <>
                    <Pause className="w-5 h-5 fill-current" />
                    <span>Pause</span>
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5 fill-current" />
                    <span>Play Now</span>
                  </>
                )}
              </button>

              <button
                onClick={() => addToQueue(currentTrack)}
                className="flex items-center gap-2 px-5 py-3.5 rounded-2xl bg-white/10 hover:bg-white/15 text-white font-semibold text-sm border border-white/10 hover:border-white/20 transition-all"
              >
                <Plus className="w-4 h-4" />
                <span>Add to Queue</span>
              </button>
            </div>

          </div>

          {/* Right Column: Interactive 3D Three.js Visualizer Canvas */}
          <div className="lg:col-span-5 flex flex-col items-center justify-center">
            <div className="relative w-full max-w-[420px] aspect-square rounded-3xl bg-gradient-to-b from-black/60 to-black/90 border border-white/15 shadow-[0_20px_50px_rgba(0,0,0,0.8)] overflow-hidden flex items-center justify-center group">
              
              {/* Three.js visualizer mounted directly inside */}
              <ThreeVisualizer className="w-full h-full" />

              {/* Artwork overlay thumbnail badge */}
              <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between p-3 rounded-2xl bg-black/60 backdrop-blur-xl border border-white/15 shadow-lg pointer-events-auto">
                <div className="flex items-center gap-3 overflow-hidden">
                  <img
                    src={currentTrack.thumbnail}
                    alt={currentTrack.title}
                    className="w-11 h-11 rounded-xl object-cover shadow border border-white/10 shrink-0"
                  />
                  <div className="overflow-hidden">
                    <p className="text-xs font-bold text-white truncate">{currentTrack.title}</p>
                    <p className="text-[11px] text-white/60 truncate">{currentTrack.artist}</p>
                  </div>
                </div>

                {/* 3D Mode Switcher mini pills */}
                <div className="flex items-center gap-1 bg-white/10 p-1 rounded-xl shrink-0">
                  <button
                    onClick={() => setVisualizerMode("aura")}
                    className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                      visualizerMode === "aura" ? "bg-accent text-white" : "text-white/60 hover:text-white"
                    }`}
                  >
                    Aura
                  </button>
                  <button
                    onClick={() => setVisualizerMode("vinyl")}
                    className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                      visualizerMode === "vinyl" ? "bg-accent text-white" : "text-white/60 hover:text-white"
                    }`}
                  >
                    Vinyl
                  </button>
                  <button
                    onClick={() => setVisualizerMode("waves")}
                    className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                      visualizerMode === "waves" ? "bg-accent text-white" : "text-white/60 hover:text-white"
                    }`}
                  >
                    Waves
                  </button>
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>
    </section>
  );
}
