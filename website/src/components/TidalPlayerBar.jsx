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
import { SoundWaveIcon } from "./TidalFullScreenPlayer";

function formatTime(seconds) {
  if (isNaN(seconds) || seconds < 0) return "0:00";
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
}

export default function TidalPlayerBar() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    isCrossfading,
    audioQuality,
    streamingQuality,
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

  const currentQuality = (streamingQuality || audioQuality || "high").toLowerCase();
  const qualityLevel = currentQuality === "low" ? "Low" : currentQuality === "medium" ? "Medium" : "High";
  const codecBadgeLabel = `OPUS ${qualityLevel}`;

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
    <footer className="hidden md:flex fixed bottom-0 left-0 right-0 z-40 h-[80px] bg-[#000000] border-t border-[#181820] px-4 sm:px-6 items-center justify-between gap-4 select-none">
      
      {/* Left: Track Info */}
      <div className="flex items-center gap-3 min-w-0 w-1/4 max-w-[280px]">
        <div className="relative w-12 h-12 rounded-lg overflow-hidden bg-[#16161c] shrink-0 border border-white/10 shadow">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-full h-full object-cover"
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
          className={`p-1.5 rounded-lg transition-colors ${
            isLiked ? "text-white" : "text-white/40 hover:text-white"
          }`}
          title={isLiked ? "Unlike" : "Like song"}
        >
          <Heart className={`w-4 h-4 ${isLiked ? "fill-current text-white" : ""}`} />
        </button>
      </div>

      {/* Center: Controls & Scrubber */}
      <div className="flex flex-col items-center justify-center gap-1 flex-1 max-w-xl">
        <div className="flex items-center gap-4">
          <button
            onClick={() => setIsShuffle(!isShuffle)}
            className={`p-1 transition-colors ${
              isShuffle ? "text-white" : "text-white/30 hover:text-white"
            }`}
            title="Shuffle"
          >
            <Shuffle className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={previousTrack}
            className="p-1 text-white/70 hover:text-white transition-colors"
            title="Previous"
          >
            <SkipBack className="w-4 h-4 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-9 h-9 rounded-full bg-white hover:bg-white/90 text-black flex items-center justify-center transition-all hover:scale-105 active:scale-95 shadow"
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
            className="p-1 text-white/70 hover:text-white transition-colors"
            title="Next"
          >
            <SkipForward className="w-4 h-4 fill-current" />
          </button>

          <button
            onClick={toggleRepeat}
            className={`p-1 transition-colors ${
              repeatMode !== "off" ? "text-white" : "text-white/30 hover:text-white"
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
            {isCrossfading && (
              <div className="absolute inset-x-0 h-3 rounded-full blur-sm bg-gradient-to-r from-pink-500 via-amber-400 via-emerald-400 via-cyan-400 to-purple-500 opacity-70 animate-rgbShift pointer-events-none" />
            )}
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={handleSeek}
              className={`relative z-10 w-full h-1 rounded-lg appearance-none cursor-pointer focus:outline-none group-hover:h-1.5 transition-all ${
                isCrossfading
                  ? "bg-gradient-to-r from-pink-500 via-amber-400 via-emerald-400 via-cyan-400 to-purple-500 animate-rgbShift shadow-[0_0_12px_rgba(236,72,153,0.7)]"
                  : "bg-white/15 accent-white"
              }`}
            />
          </div>
          <span className="text-[10px] font-mono text-white/40 w-8 text-left">
            {formatTime(duration)}
          </span>
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-white/5 border border-white/10 text-[10px] font-bold text-white/70 shrink-0">
            <SoundWaveIcon className="w-3 h-3 text-white/90" />
            {codecBadgeLabel}
          </span>
        </div>
      </div>

      {/* Right: Mode switches, Radio, Lyrics, Queue, Volume */}
      <div className="flex items-center justify-end gap-2.5 w-1/4 max-w-[280px]">
        
        {/* Song vs Video mode */}
        <button
          onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
          className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border transition-colors ${
            playbackMode === "video"
              ? "bg-white text-black border-white"
              : "bg-white/[0.04] border-white/10 text-white/70 hover:text-white"
          }`}
          title={playbackMode === "song" ? "Switch to Video" : "Audio Only"}
        >
          {playbackMode === "video" ? <Video className="w-3.5 h-3.5" /> : <Music2 className="w-3.5 h-3.5" />}
          <span className="hidden xl:inline">{playbackMode === "video" ? "Video" : "Audio"}</span>
        </button>

        {/* Continuous Radio switch */}
        <button
          onClick={toggleEndlessListening}
          className={`p-1.5 rounded-lg transition-colors ${
            endlessListening ? "text-white" : "text-white/30 hover:text-white"
          }`}
          title="Track Radio (Continuous playback)"
        >
          <Radio className="w-4 h-4" />
        </button>

        {/* Lyrics */}
        <button
          onClick={() => setIsLyricsOpen(!isLyricsOpen)}
          className={`p-1.5 rounded-lg transition-colors ${
            isLyricsOpen ? "text-white" : "text-white/40 hover:text-white"
          }`}
          title="Lyrics"
        >
          <Mic2 className="w-4 h-4" />
        </button>

        {/* Queue */}
        <button
          onClick={() => setIsQueueOpen(!isQueueOpen)}
          className={`relative p-1.5 rounded-lg transition-colors ${
            isQueueOpen ? "text-white" : "text-white/40 hover:text-white"
          }`}
          title="Queue"
        >
          <ListMusic className="w-4 h-4" />
          {queue.length > 0 && (
            <span className="absolute -top-0.5 -right-0.5 px-1 py-0.2 bg-white text-[8px] font-black text-black rounded-full">
              {queue.length}
            </span>
          )}
        </button>

        {/* Volume */}
        <div className="flex items-center gap-1.5 pl-1.5 border-l border-white/10">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className="text-white/40 hover:text-white transition-colors"
            title={isMuted ? "Unmute" : "Mute"}
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-3.5 h-3.5 text-white/50" />
            ) : (
              <Volume2 className="w-3.5 h-3.5" />
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
            className="w-14 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-white"
          />
        </div>

      </div>

    </footer>
  );
}
