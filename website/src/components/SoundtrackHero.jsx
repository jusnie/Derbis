import React, { useState } from "react";
import { Play, Pause, Sparkles, Music2, Video, Radio, Disc } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import ThreeVisualizer from "./ThreeVisualizer";

export default function SoundtrackHero() {
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

  const [heroVisualMode, setHeroVisualMode] = useState("portrait"); // "portrait" | "3d"

  const handleStartSoundtrack = () => {
    if (isPlaying) {
      togglePlayPause();
    } else {
      // Start radio from first track with full quick picks queue
      playTrack(QUICK_PICKS_TRACKS[0], QUICK_PICKS_TRACKS);
    }
  };

  return (
    <article className="relative overflow-hidden rounded-[30px] border border-white/10 bg-gradient-to-r from-[#171f2a] via-[#101317] to-[#060607] shadow-2xl min-h-[260px] md:min-h-[290px] flex flex-col justify-end p-6 sm:p-10 select-none isolate">
      {/* Background Orbit Rings */}
      <div className="absolute -top-36 right-16 w-80 h-80 rounded-full border border-blue-400/15 pointer-events-none" />
      <div className="absolute -bottom-48 -right-20 w-96 h-96 rounded-full border border-white/5 pointer-events-none" />
      <div className="absolute top-1/4 right-1/3 w-64 h-64 rounded-full bg-blue-500/10 blur-3xl pointer-events-none" />

      {/* Visual Canvas on Right: Either Artist Portrait or Three.js 3D Visualizer */}
      <div className="absolute top-0 right-0 bottom-0 w-full sm:w-1/2 md:w-[48%] overflow-hidden pointer-events-auto z-0">
        {heroVisualMode === "portrait" ? (
          <div className="relative w-full h-full">
            <img
              src="https://yt3.ggpht.com/u2o5_V4WeYMDebsryzMFHN6OBoXZsMjfpOSguscVEAVS6MKYHowaXnPjfZmzaRSr1jqtjiCowg=s800-c-k-c0xffffffff-no-rj-mo"
              alt="Featured Artist"
              className="w-full h-full object-cover object-center opacity-85 hover:scale-105 transition-transform duration-700"
              style={{
                maskImage: "linear-gradient(to right, transparent 0%, rgba(0,0,0,0.5) 20%, black 65%)",
                WebkitMaskImage: "linear-gradient(to right, transparent 0%, rgba(0,0,0,0.5) 20%, black 65%)"
              }}
            />
          </div>
        ) : (
          <div className="w-full h-full">
            <ThreeVisualizer className="w-full h-full" />
          </div>
        )}

        {/* 3D vs Portrait Switcher Pill */}
        <div className="absolute top-4 right-4 z-10 flex items-center gap-1 p-1 rounded-xl bg-black/60 backdrop-blur-md border border-white/15">
          <button
            onClick={() => setHeroVisualMode("portrait")}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              heroVisualMode === "portrait"
                ? "bg-blue-600 text-white shadow"
                : "text-white/60 hover:text-white"
            }`}
          >
            Artist
          </button>
          <button
            onClick={() => setHeroVisualMode("3d")}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              heroVisualMode === "3d"
                ? "bg-blue-600 text-white shadow"
                : "text-white/60 hover:text-white"
            }`}
          >
            <Sparkles className="w-3 h-3" />
            <span>3D Wave</span>
          </button>
        </div>
      </div>

      {/* Content on Left */}
      <div className="relative z-10 max-w-lg space-y-4">
        {/* Eyebrow */}
        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1.5 text-[11px] font-black tracking-widest text-[#9ebfff] uppercase">
            <span className="w-2 h-2 rounded-full bg-[#5d9cff] shadow-[0_0_8px_#5d9cff]" />
            RADIO
          </span>
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/10 text-white/70 border border-white/5">
            Lossless Hi-Fi
          </span>
        </div>

        {/* Title & Subtitle */}
        <div>
          <h2 className="text-3xl sm:text-5xl font-black text-white tracking-tight leading-none">
            My soundtrack
          </h2>
          <p className="text-sm sm:text-base text-white/60 mt-1 font-medium">
            Made for your next moment
          </p>
        </div>

        {/* Companion Artist Stack */}
        <div className="flex items-center gap-3 pt-1">
          <div className="flex -space-x-2">
            <img
              src="https://yt3.ggpht.com/u2o5_V4WeYMDebsryzMFHN6OBoXZsMjfpOSguscVEAVS6MKYHowaXnPjfZmzaRSr1jqtjiCowg=s800-c-k-c0xffffffff-no-rj-mo"
              alt="Ariana Grande"
              className="w-7 h-7 rounded-full border-2 border-[#14171b] object-cover"
            />
            <img
              src="https://yt3.ggpht.com/FoD6VmzaE4nKTPEKR0HXYH9GAaoFhGRifUJW6T9tneMfHjRMMvr_ToomsP5TDDTFxPAWiWp_=s800-c-k-c0xffffffff-no-rj-mo"
              alt="Maki"
              className="w-7 h-7 rounded-full border-2 border-[#14171b] object-cover"
            />
            <img
              src="https://yt3.ggpht.com/cCf3P5ZRQ5f0D69oXzAg6LUI6gazIPLhIohervVyg0JdGrDfo5MUCzU_qujvGruLVWwtVtbf6Q=s800-c-k-c0xffffffff-no-rj-mo"
              alt="Billie Eilish"
              className="w-7 h-7 rounded-full border-2 border-[#14171b] object-cover"
            />
          </div>
          <span className="text-xs font-semibold text-white/70 truncate max-w-[280px]">
            Starts with Ariana Grande, Maki, and Billie Eilish
          </span>
        </div>

        {/* Controls Pill: Song vs Video Mode + Endless Listening */}
        <div className="flex flex-wrap items-center gap-2 pt-2">
          <button
            onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
              playbackMode === "video"
                ? "bg-purple-600/30 border-purple-500/50 text-purple-300"
                : "bg-white/10 border-white/10 text-white/80 hover:text-white"
            }`}
          >
            {playbackMode === "video" ? <Video className="w-3.5 h-3.5" /> : <Music2 className="w-3.5 h-3.5 text-blue-400" />}
            <span>{playbackMode === "video" ? "Video View" : "Song (Artwork only)"}</span>
          </button>

          <button
            onClick={toggleEndlessListening}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
              endlessListening
                ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-300"
                : "bg-white/5 border-white/10 text-white/50"
            }`}
          >
            <Radio className={`w-3.5 h-3.5 ${endlessListening ? "text-emerald-400 animate-pulse" : ""}`} />
            <span>Endless Listening: {endlessListening ? "ON" : "OFF"}</span>
          </button>
        </div>
      </div>

      {/* Large Circular Play Button */}
      <button
        onClick={handleStartSoundtrack}
        aria-label="Play My Soundtrack"
        className="absolute bottom-6 right-6 sm:bottom-10 sm:right-10 z-20 w-14 h-14 sm:w-16 sm:h-16 rounded-2xl sm:rounded-[22px] bg-gradient-to-tr from-[#377fe9] to-[#67a5ff] hover:from-[#4c90f5] hover:to-[#7ab0ff] text-[#07172f] flex items-center justify-center shadow-[0_16px_34px_rgba(72,139,255,0.4)] hover:scale-105 active:scale-95 transition-all"
      >
        {isPlaying ? (
          <Pause className="w-7 h-7 fill-current" />
        ) : (
          <Play className="w-7 h-7 fill-current ml-1" />
        )}
      </button>
    </article>
  );
}
