import React, { useState } from "react";
import {
  ChevronDown,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Heart,
  Mic2,
  ListMusic,
  Radio,
  Video,
  Music2,
  Sparkles
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import ThreeVisualizer from "./ThreeVisualizer";

function formatTime(seconds) {
  if (isNaN(seconds) || seconds < 0) return "0:00";
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
}

export default function TidalFullScreenPlayer() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayPause,
    seekTo,
    nextTrack,
    previousTrack,
    isShuffle,
    setIsShuffle,
    repeatMode,
    setRepeatMode,
    audioQuality,
    playbackMode,
    setPlaybackMode,
    endlessListening,
    toggleEndlessListening,
    isMobilePlayerExpanded,
    setIsMobilePlayerExpanded,
    setIsLyricsOpen,
    setIsQueueOpen
  } = usePlayer();

  const [isLiked, setIsLiked] = useState(false);
  const [visualMode, setVisualMode] = useState("cover"); // "cover" | "3d"

  if (!isMobilePlayerExpanded) return null;

  const handleSeek = (e) => {
    const val = parseFloat(e.target.value);
    seekTo(val);
  };

  const toggleRepeat = () => {
    if (repeatMode === "off") setRepeatMode("all");
    else if (repeatMode === "all") setRepeatMode("one");
    else setRepeatMode("off");
  };

  return (
    <div className="md:hidden fixed inset-0 z-50 bg-[#08080a] flex flex-col justify-between p-6 select-none animate-slideUp overflow-y-auto">
      
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => setIsMobilePlayerExpanded(false)}
          className="p-2 -ml-2 text-white/70 hover:text-white"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] font-black tracking-widest text-[#00e5ff] uppercase">
            PLAYING FROM RADIO
          </span>
          <span className="text-xs font-bold text-white/70 truncate max-w-[200px]">
            {currentTrack.genre ? `${currentTrack.genre.toUpperCase()} MIX` : "DERBIS HI-FI"}
          </span>
        </div>

        {/* 3D vs Cover mode toggle */}
        <button
          onClick={() => setVisualMode(visualMode === "cover" ? "3d" : "cover")}
          className={`flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-bold border ${
            visualMode === "3d"
              ? "bg-[#00e5ff] text-black border-[#00e5ff]"
              : "bg-white/10 text-white/70 border-white/10"
          }`}
        >
          <Sparkles className="w-3 h-3" />
          <span>{visualMode === "3d" ? "3D ON" : "3D"}</span>
        </button>
      </div>

      {/* Artwork or 3D Canvas */}
      <div className="flex items-center justify-center my-6">
        <div className="relative w-full max-w-[320px] aspect-square rounded-2xl overflow-hidden bg-[#121216] border border-[#22222a] shadow-2xl">
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
        </div>
      </div>

      {/* Metadata & Favorite */}
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div className="min-w-0 flex-1">
            <h2 className="text-xl font-black text-white truncate">
              {currentTrack.title}
            </h2>
            <p className="text-sm font-semibold text-white/60 truncate mt-0.5">
              {currentTrack.artist}
            </p>
          </div>

          <button
            onClick={() => setIsLiked(!isLiked)}
            className={`p-2 rounded-full border transition-colors ${
              isLiked ? "text-pink-500 border-pink-500/40 bg-pink-500/10" : "text-white/40 border-white/10"
            }`}
          >
            <Heart className={`w-5 h-5 ${isLiked ? "fill-current" : ""}`} />
          </button>
        </div>

        {/* Timeline Scrubber */}
        <div className="space-y-1">
          <div className="relative py-1">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1.5 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#00e5ff]"
            />
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-white/40">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Big Playback Controls */}
        <div className="flex items-center justify-between py-2">
          <button
            onClick={() => setIsShuffle(!isShuffle)}
            className={`p-2 ${isShuffle ? "text-[#00e5ff]" : "text-white/40"}`}
          >
            <Shuffle className="w-5 h-5" />
          </button>

          <button onClick={previousTrack} className="p-2 text-white">
            <SkipBack className="w-6 h-6 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center shadow-xl active:scale-95 transition-transform"
          >
            {isPlaying ? (
              <Pause className="w-7 h-7 fill-current" />
            ) : (
              <Play className="w-7 h-7 fill-current ml-1" />
            )}
          </button>

          <button onClick={nextTrack} className="p-2 text-white">
            <SkipForward className="w-6 h-6 fill-current" />
          </button>

          <button
            onClick={toggleRepeat}
            className={`p-2 ${repeatMode !== "off" ? "text-[#00e5ff]" : "text-white/40"}`}
          >
            {repeatMode === "one" ? <Repeat1 className="w-5 h-5" /> : <Repeat className="w-5 h-5" />}
          </button>
        </div>

        {/* Bottom Utility Row */}
        <div className="flex items-center justify-between pt-2 border-t border-white/10">
          {/* Tidal Audio Quality Badge */}
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#00e5ff]/15 border border-[#00e5ff]/40 text-[#00e5ff] text-[10px] font-black uppercase tracking-wider">
            <Sparkles className="w-3 h-3" />
            <span>{audioQuality} • 24-BIT / 96 KHZ</span>
          </div>

          <div className="flex items-center gap-3">
            {/* Song vs Video mode */}
            <button
              onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
              className={`p-2 rounded-lg border ${
                playbackMode === "video" ? "bg-purple-600/30 border-purple-500/50 text-purple-300" : "text-white/50 border-white/10"
              }`}
              title="Song vs Video mode"
            >
              {playbackMode === "video" ? <Video className="w-4 h-4" /> : <Music2 className="w-4 h-4" />}
            </button>

            {/* Continuous Radio */}
            <button
              onClick={toggleEndlessListening}
              className={`p-2 rounded-lg border ${
                endlessListening ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-300" : "text-white/50 border-white/10"
              }`}
              title="Continuous radio"
            >
              <Radio className="w-4 h-4" />
            </button>

            {/* Lyrics */}
            <button
              onClick={() => {
                setIsMobilePlayerExpanded(false);
                setIsLyricsOpen(true);
              }}
              className="p-2 rounded-lg text-white/50 border border-white/10"
              title="Lyrics"
            >
              <Mic2 className="w-4 h-4" />
            </button>

            {/* Queue */}
            <button
              onClick={() => {
                setIsMobilePlayerExpanded(false);
                setIsQueueOpen(true);
              }}
              className="p-2 rounded-lg text-white/50 border border-white/10"
              title="Queue"
            >
              <ListMusic className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

    </div>
  );
}
