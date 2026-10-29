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
  Maximize2
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

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-2 sm:p-4 pointer-events-none">
      <div className="max-w-7xl mx-auto rounded-3xl bg-black/75 backdrop-blur-3xl border border-white/15 p-3 sm:px-6 sm:py-3.5 shadow-[0_12px_48px_rgba(0,0,0,0.8)] pointer-events-auto flex flex-col gap-2">
        
        {/* Progress Bar (Integrated at top of dock on mobile or within dock) */}
        <div className="w-full flex items-center gap-3">
          <span className="text-[11px] font-mono text-white/50 w-9 text-right select-none">
            {formatTime(currentTime)}
          </span>
          <div className="relative flex-1 group flex items-center py-1 cursor-pointer">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1.5 bg-white/15 rounded-lg appearance-none cursor-pointer accent-accent focus:outline-none group-hover:h-2 transition-all"
            />
          </div>
          <span className="text-[11px] font-mono text-white/50 w-9 text-left select-none">
            {formatTime(duration)}
          </span>
        </div>

        {/* Main Dock Controls Bar */}
        <div className="flex items-center justify-between gap-4">
          
          {/* Left: Current Track Thumbnail & Metadata */}
          <div className="flex items-center gap-3 min-w-0 flex-1 max-w-[280px] sm:max-w-[320px]">
            <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-black/50 shrink-0 border border-white/10 shadow-md">
              <img
                src={currentTrack.thumbnail}
                alt={currentTrack.title}
                className={`w-full h-full object-cover ${isPlaying ? "scale-105" : ""} transition-transform duration-700`}
              />
              {isPlaying && (
                <div className="absolute inset-0 bg-accent/10 pointer-events-none ring-1 ring-inset ring-accent/30" />
              )}
            </div>

            <div className="min-w-0 flex-1">
              <h4 className="text-xs sm:text-sm font-bold text-white truncate hover:underline cursor-pointer">
                {currentTrack.title}
              </h4>
              <p className="text-[11px] sm:text-xs text-white/50 truncate">
                {currentTrack.artist}
              </p>
            </div>

            <button
              onClick={() => setIsLiked(!isLiked)}
              className={`p-1.5 rounded-lg transition-colors hidden sm:inline-flex ${
                isLiked ? "text-accent fill-accent" : "text-white/40 hover:text-white"
              }`}
              title={isLiked ? "Unlike" : "Like song"}
            >
              <Heart className={`w-4 h-4 ${isLiked ? "fill-current" : ""}`} />
            </button>
          </div>

          {/* Center: Playback Controls */}
          <div className="flex items-center gap-1.5 sm:gap-3 shrink-0">
            {/* Shuffle */}
            <button
              onClick={() => setIsShuffle(!isShuffle)}
              className={`p-2 rounded-xl transition-all hidden md:inline-flex ${
                isShuffle ? "text-accent bg-accent/15" : "text-white/40 hover:text-white"
              }`}
              title="Shuffle"
            >
              <Shuffle className="w-4 h-4" />
            </button>

            {/* Previous */}
            <button
              onClick={previousTrack}
              className="p-2 rounded-xl text-white/70 hover:text-white hover:bg-white/10 transition-all"
              title="Previous song"
            >
              <SkipBack className="w-5 h-5 fill-current" />
            </button>

            {/* Play / Pause button */}
            <button
              onClick={togglePlayPause}
              className="w-11 h-11 sm:w-12 sm:h-12 rounded-full bg-gradient-to-tr from-accent to-pink-500 hover:scale-105 active:scale-95 text-white flex items-center justify-center shadow-lg shadow-accent/40 transition-all"
              title={isPlaying ? "Pause" : "Play"}
            >
              {isPlaying ? (
                <Pause className="w-5 h-5 fill-current" />
              ) : (
                <Play className="w-5 h-5 fill-current ml-0.5" />
              )}
            </button>

            {/* Next */}
            <button
              onClick={nextTrack}
              className="p-2 rounded-xl text-white/70 hover:text-white hover:bg-white/10 transition-all"
              title="Next song"
            >
              <SkipForward className="w-5 h-5 fill-current" />
            </button>

            {/* Repeat */}
            <button
              onClick={toggleRepeat}
              className={`p-2 rounded-xl transition-all hidden md:inline-flex ${
                repeatMode !== "off" ? "text-accent bg-accent/15" : "text-white/40 hover:text-white"
              }`}
              title={`Repeat: ${repeatMode}`}
            >
              {repeatMode === "one" ? <Repeat1 className="w-4 h-4" /> : <Repeat className="w-4 h-4" />}
            </button>
          </div>

          {/* Right: Modern Mode Switches, Continuous Radio, Volume & Drawers */}
          <div className="flex items-center gap-1.5 sm:gap-2.5 shrink-0">
            {/* Song vs Video mode pill */}
            <button
              onClick={() => setPlaybackMode(playbackMode === "song" ? "video" : "song")}
              className={`hidden lg:flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
                playbackMode === "video"
                  ? "bg-purple-600/30 border-purple-500/50 text-purple-300"
                  : "bg-white/5 border-white/10 text-white/70 hover:text-white"
              }`}
              title={playbackMode === "song" ? "Switch to Video View" : "Switch to Song (Artwork Only)"}
            >
              {playbackMode === "video" ? (
                <>
                  <Video className="w-3.5 h-3.5 text-purple-400" />
                  <span>Video</span>
                </>
              ) : (
                <>
                  <Music2 className="w-3.5 h-3.5 text-accent" />
                  <span>Song</span>
                </>
              )}
            </button>

            {/* Endless Listening Continuous Radio Switch */}
            <button
              onClick={toggleEndlessListening}
              className={`hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
                endlessListening
                  ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-300"
                  : "bg-white/5 border-white/10 text-white/40 hover:text-white"
              }`}
              title="Endless Listening (Continuous Radio): automatically queues songs when the playlist ends"
            >
              <Radio className={`w-3.5 h-3.5 ${endlessListening ? "text-emerald-400 animate-pulse" : ""}`} />
              <span className="hidden xl:inline">Radio</span>
            </button>

            {/* Lyrics view toggle */}
            <button
              onClick={() => setIsLyricsOpen(!isLyricsOpen)}
              className={`p-2 rounded-xl transition-all border ${
                isLyricsOpen
                  ? "bg-accent text-white border-accent shadow-md shadow-accent/30"
                  : "bg-white/5 text-white/60 hover:text-white border-white/10"
              }`}
              title="Synchronized Lyrics"
            >
              <Mic2 className="w-4 h-4" />
            </button>

            {/* Queue drawer toggle */}
            <button
              onClick={() => setIsQueueOpen(!isQueueOpen)}
              className={`relative p-2 rounded-xl transition-all border ${
                isQueueOpen
                  ? "bg-accent text-white border-accent shadow-md shadow-accent/30"
                  : "bg-white/5 text-white/60 hover:text-white border-white/10"
              }`}
              title="Up Next Queue"
            >
              <ListMusic className="w-4 h-4" />
              {queue.length > 0 && (
                <span className="absolute -top-1 -right-1 px-1.5 py-0.2 bg-accent text-[9px] font-bold text-white rounded-full ring-2 ring-black">
                  {queue.length}
                </span>
              )}
            </button>

            {/* Volume Control */}
            <div className="hidden lg:flex items-center gap-2 pl-2 border-l border-white/10">
              <button
                onClick={() => setIsMuted(!isMuted)}
                className="text-white/50 hover:text-white transition-colors"
                title={isMuted ? "Unmute" : "Mute"}
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-accent" />
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
                className="w-16 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-accent"
              />
            </div>

          </div>

        </div>

      </div>
    </div>
  );
}
