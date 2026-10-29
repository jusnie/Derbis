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
  Music2
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

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
    <div className="md:hidden fixed inset-0 z-50 bg-[#000000] flex flex-col justify-between p-6 select-none animate-slideUp overflow-y-auto">
      
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => setIsMobilePlayerExpanded(false)}
          className="p-2 -ml-2 text-white/70 hover:text-white"
          aria-label="Minimize player"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] font-black tracking-widest text-white/40 uppercase">
            PLAYING FROM
          </span>
          <span className="text-xs font-bold text-white truncate max-w-[200px]">
            {currentTrack.genre ? `${currentTrack.genre.toUpperCase()} TRACK RADIO` : "DERBIS TRACK RADIO"}
          </span>
        </div>

        <div className="w-6" />
      </div>

      {/* Large Square Album Artwork */}
      <div className="flex items-center justify-center my-6">
        <div className="relative w-full max-w-[320px] aspect-square rounded-2xl overflow-hidden bg-[#121216] border border-[#222228] shadow-2xl">
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-full h-full object-cover"
          />
        </div>
      </div>

      {/* Metadata & Favorite */}
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div className="min-w-0 flex-1">
            <h2 className="text-xl sm:text-2xl font-black text-white truncate leading-tight">
              {currentTrack.title}
            </h2>
            <p className="text-sm font-semibold text-white/50 truncate mt-0.5">
              {currentTrack.artist}
            </p>
          </div>

          <button
            onClick={() => setIsLiked(!isLiked)}
            className={`p-2 rounded-full border transition-colors ${
              isLiked ? "text-white border-white bg-white/10" : "text-white/40 border-white/15"
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
              className="w-full h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-white"
            />
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-white/40">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Big Tactile Playback Controls */}
        <div className="flex items-center justify-between py-2">
          <button
            onClick={() => setIsShuffle(!isShuffle)}
            className={`p-2 transition-colors ${isShuffle ? "text-white" : "text-white/30"}`}
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
            className={`p-2 transition-colors ${repeatMode !== "off" ? "text-white" : "text-white/30"}`}
          >
            {repeatMode === "one" ? <Repeat1 className="w-5 h-5" /> : <Repeat className="w-5 h-5" />}
          </button>
        </div>

        {/* Bottom Utility Row */}
        <div className="flex items-center justify-between pt-3 border-t border-white/10">
          <span className="text-[10px] font-mono font-bold text-white/50 uppercase tracking-widest">
            HI-RES LOSSLESS
          </span>

          <div className="flex items-center gap-3">
            {/* Song vs Video mode */}
            <button
              onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
              className={`p-2 rounded-lg border transition-colors ${
                playbackMode === "video" ? "bg-white text-black border-white" : "text-white/50 border-white/10"
              }`}
              title="Song vs Video mode"
            >
              {playbackMode === "video" ? <Video className="w-4 h-4" /> : <Music2 className="w-4 h-4" />}
            </button>

            {/* Continuous Radio */}
            <button
              onClick={toggleEndlessListening}
              className={`p-2 rounded-lg border transition-colors ${
                endlessListening ? "bg-white text-black border-white" : "text-white/50 border-white/10"
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
              className="p-2 rounded-lg text-white/50 hover:text-white border border-white/10"
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
              className="p-2 rounded-lg text-white/50 hover:text-white border border-white/10"
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
