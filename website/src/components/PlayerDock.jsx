import React, { useState } from "react";
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Volume2,
  VolumeX,
  ListMusic,
  Mic2,
  Radio,
  Video,
  Music2,
  Heart
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

function formatTime(seconds) {
  if (isNaN(seconds) || seconds < 0) return "0:00";
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
}

export default function PlayerDock() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayPause,
    seekTo,
    nextTrack,
    previousTrack,
    volume,
    setVolume,
    isMuted,
    setIsMuted,
    playbackMode,
    setPlaybackMode,
    endlessListening,
    toggleEndlessListening,
    isShuffle,
    setIsShuffle,
    repeatMode,
    setRepeatMode,
    isQueueOpen,
    setIsQueueOpen,
    isLyricsOpen,
    setIsLyricsOpen,
    queue
  } = usePlayer();

  const [isLiked, setIsLiked] = useState(false);

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
    <footer className="fixed bottom-0 left-0 right-0 z-40 h-[88px] bg-[#0c0d11]/95 backdrop-blur-2xl border-t border-white/10 px-4 sm:px-6 flex items-center justify-between gap-4 select-none shadow-[0_-12px_40px_rgba(0,0,0,0.6)]">
      
      {/* Left: Track Artwork & Info */}
      <div className="flex items-center gap-3 min-w-0 w-1/4 max-w-[280px]">
        <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-[#1c1d22] shrink-0 border border-white/10 shadow">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className={`w-full h-full object-cover ${isPlaying ? "scale-105" : ""} transition-transform duration-500`}
          />
        </div>

        <div className="min-w-0 flex-1">
          <h4 className="text-xs sm:text-sm font-bold text-white truncate leading-tight hover:underline cursor-pointer">
            {currentTrack.title}
          </h4>
          <p className="text-[11px] text-white/50 truncate mt-0.5">
            {currentTrack.artist}
          </p>
        </div>

        <button
          onClick={() => setIsLiked(!isLiked)}
          className={`p-1.5 rounded-lg transition-colors hidden sm:inline-flex ${
            isLiked ? "text-pink-500 fill-pink-500" : "text-white/40 hover:text-white"
          }`}
          title={isLiked ? "Unlike" : "Like song"}
        >
          <Heart className={`w-4 h-4 ${isLiked ? "fill-current" : ""}`} />
        </button>
      </div>

      {/* Center: Controls & Scrubber */}
      <div className="flex flex-col items-center justify-center gap-1.5 flex-1 max-w-xl">
        {/* Buttons */}
        <div className="flex items-center gap-2 sm:gap-4">
          <button
            onClick={() => setIsShuffle(!isShuffle)}
            className={`p-1.5 rounded-lg transition-all hidden md:inline-flex ${
              isShuffle ? "text-blue-400" : "text-white/40 hover:text-white"
            }`}
            title="Shuffle"
          >
            <Shuffle className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={previousTrack}
            className="p-1.5 rounded-lg text-white/70 hover:text-white hover:bg-white/10 transition-all"
            title="Previous"
          >
            <SkipBack className="w-4 h-4 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-10 h-10 rounded-full bg-white hover:scale-105 active:scale-95 text-black flex items-center justify-center shadow-lg transition-all"
            title={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? (
              <Pause className="w-4 h-4 fill-current" />
            ) : (
              <Play className="w-4 h-4 fill-current ml-0.5" />
            )}
          </button>

          <button
            onClick={nextTrack}
            className="p-1.5 rounded-lg text-white/70 hover:text-white hover:bg-white/10 transition-all"
            title="Next"
          >
            <SkipForward className="w-4 h-4 fill-current" />
          </button>

          <button
            onClick={toggleRepeat}
            className={`p-1.5 rounded-lg transition-all hidden md:inline-flex ${
              repeatMode !== "off" ? "text-blue-400" : "text-white/40 hover:text-white"
            }`}
            title={`Repeat: ${repeatMode}`}
          >
            {repeatMode === "one" ? <Repeat1 className="w-3.5 h-3.5" /> : <Repeat className="w-3.5 h-3.5" />}
          </button>
        </div>

        {/* Timeline Scrubber */}
        <div className="w-full flex items-center gap-2.5">
          <span className="text-[10px] font-mono text-white/40 w-8 text-right">
            {formatTime(currentTime)}
          </span>
          <div className="relative flex-1 group flex items-center py-1">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1 bg-white/15 rounded-lg appearance-none cursor-pointer accent-blue-500 focus:outline-none group-hover:h-1.5 transition-all"
            />
          </div>
          <span className="text-[10px] font-mono text-white/40 w-8 text-left">
            {formatTime(duration)}
          </span>
        </div>
      </div>

      {/* Right: Mode Switchers, Continuous Radio & Drawers */}
      <div className="flex items-center justify-end gap-2 w-1/4 max-w-[280px]">
        {/* Song vs Video mode pill */}
        <button
          onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
          className={`hidden xl:flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold border transition-all ${
            playbackMode === "video"
              ? "bg-purple-600/30 border-purple-500/40 text-purple-300"
              : "bg-white/5 border-white/10 text-white/70 hover:text-white"
          }`}
          title={playbackMode === "song" ? "Switch to Video View" : "Switch to Song (Artwork Only)"}
        >
          {playbackMode === "video" ? <Video className="w-3 h-3 text-purple-400" /> : <Music2 className="w-3 h-3 text-blue-400" />}
          <span>{playbackMode === "video" ? "Video" : "Song"}</span>
        </button>

        {/* Endless Listening Radio Switch */}
        <button
          onClick={toggleEndlessListening}
          className={`hidden sm:flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] font-bold border transition-all ${
            endlessListening
              ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-300"
              : "bg-white/5 border-white/10 text-white/40 hover:text-white"
          }`}
          title="Endless listening (Continuous Radio)"
        >
          <Radio className={`w-3 h-3 ${endlessListening ? "text-emerald-400 animate-pulse" : ""}`} />
          <span className="hidden 2xl:inline">Radio</span>
        </button>

        {/* Lyrics */}
        <button
          onClick={() => setIsLyricsOpen(!isLyricsOpen)}
          className={`p-2 rounded-xl transition-all border ${
            isLyricsOpen
              ? "bg-blue-600 text-white border-blue-500 shadow"
              : "bg-white/5 text-white/60 hover:text-white border-white/10"
          }`}
          title="Lyrics"
        >
          <Mic2 className="w-4 h-4" />
        </button>

        {/* Queue */}
        <button
          onClick={() => setIsQueueOpen(!isQueueOpen)}
          className={`relative p-2 rounded-xl transition-all border ${
            isQueueOpen
              ? "bg-blue-600 text-white border-blue-500 shadow"
              : "bg-white/5 text-white/60 hover:text-white border-white/10"
          }`}
          title="Up Next Queue"
        >
          <ListMusic className="w-4 h-4" />
          {queue.length > 0 && (
            <span className="absolute -top-1 -right-1 px-1.5 py-0.2 bg-blue-500 text-[9px] font-bold text-white rounded-full ring-2 ring-[#0c0d11]">
              {queue.length}
            </span>
          )}
        </button>

        {/* Volume Slider */}
        <div className="hidden lg:flex items-center gap-2 pl-1 border-l border-white/10">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className="text-white/50 hover:text-white transition-colors"
            title={isMuted ? "Unmute" : "Mute"}
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-4 h-4 text-red-400" />
            ) : (
              <Volume2 className="w-4 h-4" />
            )}
          </button>
          <input
            type="range"
            min="0"
            max="100"
            value={isMuted ? 0 : volume}
            onChange={(e) => {
              setVolume(parseInt(e.target.value));
              if (isMuted) setIsMuted(false);
            }}
            className="w-14 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-blue-500"
          />
        </div>
      </div>

    </footer>
  );
}
