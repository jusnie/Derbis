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
  Heart,
  Sparkles
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

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
    togglePlayPause,
    seekTo,
    nextTrack,
    previousTrack,
    volume,
    setVolume,
    isMuted,
    setIsMuted,
    audioQuality,
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
    <footer className="hidden md:flex fixed bottom-0 left-0 right-0 z-40 h-[84px] bg-[#0c0c0f] border-t border-[#1a1a22] px-4 sm:px-6 items-center justify-between gap-4 select-none">
      
      {/* Left: Track Info */}
      <div className="flex items-center gap-3 min-w-0 w-1/4 max-w-[280px]">
        <div className="relative w-12 h-12 rounded-lg overflow-hidden bg-[#16161c] shrink-0 border border-white/5">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-full h-full object-cover"
          />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5">
            <h4 className="text-xs sm:text-sm font-bold text-white truncate leading-tight hover:underline cursor-pointer">
              {currentTrack.title}
            </h4>
            <span className="px-1 py-0.2 rounded text-[8px] font-black text-[#00e5ff] bg-[#00e5ff]/15 border border-[#00e5ff]/30 uppercase shrink-0">
              {audioQuality}
            </span>
          </div>
          <p className="text-[11px] text-white/50 truncate mt-0.5">
            {currentTrack.artist}
          </p>
        </div>

        <button
          onClick={() => setIsLiked(!isLiked)}
          className={`p-1.5 rounded-lg transition-colors ${
            isLiked ? "text-pink-500 fill-pink-500" : "text-white/40 hover:text-white"
          }`}
          title={isLiked ? "Unlike" : "Like song"}
        >
          <Heart className={`w-4 h-4 ${isLiked ? "fill-current" : ""}`} />
        </button>
      </div>

      {/* Center: Playback Controls & Progress Scrubber */}
      <div className="flex flex-col items-center justify-center gap-1 flex-1 max-w-xl">
        {/* Buttons */}
        <div className="flex items-center gap-4">
          <button
            onClick={() => setIsShuffle(!isShuffle)}
            className={`p-1 transition-colors ${
              isShuffle ? "text-[#00e5ff]" : "text-white/40 hover:text-white"
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
            className="w-9 h-9 rounded-full bg-white hover:bg-[#00e5ff] text-black flex items-center justify-center transition-all hover:scale-105 active:scale-95 shadow"
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
              repeatMode !== "off" ? "text-[#00e5ff]" : "text-white/40 hover:text-white"
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
              className="w-full h-1 bg-white/15 rounded-lg appearance-none cursor-pointer accent-[#00e5ff] focus:outline-none group-hover:h-1.5 transition-all"
            />
          </div>
          <span className="text-[10px] font-mono text-white/40 w-8 text-left">
            {formatTime(duration)}
          </span>
        </div>
      </div>

      {/* Right: Audio Quality Badge, Mode Switches, Lyrics, Queue, Volume */}
      <div className="flex items-center justify-end gap-2.5 w-1/4 max-w-[280px]">
        
        {/* Tidal Audio Quality Badge */}
        <div
          title="TIDAL MAX: FLAC 24-bit / 96 kHz Audio"
          className="flex items-center gap-1 px-2 py-0.5 rounded bg-[#00e5ff]/15 border border-[#00e5ff]/40 text-[#00e5ff] text-[10px] font-black uppercase tracking-wider select-none cursor-default"
        >
          <Sparkles className="w-2.5 h-2.5" />
          <span>{audioQuality}</span>
        </div>

        {/* Song vs Video mode */}
        <button
          onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
          className={`flex items-center gap-1 px-2 py-1 rounded text-[11px] font-bold border transition-all ${
            playbackMode === "video"
              ? "bg-purple-600/30 border-purple-500/40 text-purple-300"
              : "bg-white/[0.04] border-white/10 text-white/70 hover:text-white"
          }`}
          title={playbackMode === "song" ? "Switch to Video" : "Audio Only"}
        >
          {playbackMode === "video" ? <Video className="w-3 h-3 text-purple-400" /> : <Music2 className="w-3 h-3 text-[#00e5ff]" />}
          <span className="hidden xl:inline">{playbackMode === "video" ? "Video" : "Audio"}</span>
        </button>

        {/* Continuous Radio switch */}
        <button
          onClick={toggleEndlessListening}
          className={`p-1.5 rounded-lg transition-colors ${
            endlessListening ? "text-[#00e5ff]" : "text-white/40 hover:text-white"
          }`}
          title="Endless listening (Continuous Radio)"
        >
          <Radio className={`w-4 h-4 ${endlessListening ? "animate-pulse" : ""}`} />
        </button>

        {/* Lyrics */}
        <button
          onClick={() => setIsLyricsOpen(!isLyricsOpen)}
          className={`p-1.5 rounded-lg transition-colors ${
            isLyricsOpen ? "text-[#00e5ff]" : "text-white/50 hover:text-white"
          }`}
          title="Lyrics"
        >
          <Mic2 className="w-4 h-4" />
        </button>

        {/* Queue */}
        <button
          onClick={() => setIsQueueOpen(!isQueueOpen)}
          className={`relative p-1.5 rounded-lg transition-colors ${
            isQueueOpen ? "text-[#00e5ff]" : "text-white/50 hover:text-white"
          }`}
          title="Queue"
        >
          <ListMusic className="w-4 h-4" />
          {queue.length > 0 && (
            <span className="absolute -top-0.5 -right-0.5 px-1 py-0.2 bg-[#00e5ff] text-[8px] font-black text-black rounded-full">
              {queue.length}
            </span>
          )}
        </button>

        {/* Volume */}
        <div className="flex items-center gap-1.5 pl-1.5 border-l border-white/10">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className="text-white/50 hover:text-white transition-colors"
            title={isMuted ? "Unmute" : "Mute"}
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-3.5 h-3.5 text-red-400" />
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
            className="w-14 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#00e5ff]"
          />
        </div>

      </div>

    </footer>
  );
}
