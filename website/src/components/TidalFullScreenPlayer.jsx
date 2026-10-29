import React, { useState, useMemo } from "react";
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
import { getTrackLyrics } from "../data/lyrics";

function formatTime(seconds) {
  if (isNaN(seconds) || seconds < 0) return "0:00";
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
}

export function SoundWaveIcon({ className = "w-3.5 h-3.5 text-current" }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" className={className}>
      <line x1="4.2" y1="10.2" x2="4.2" y2="13.8" />
      <line x1="6.8" y1="9.05" x2="6.8" y2="14.95" />
      <line x1="9.4" y1="7.25" x2="9.4" y2="16.75" />
      <line x1="12.0" y1="4.8" x2="12.0" y2="19.2" />
      <line x1="14.6" y1="7.25" x2="14.6" y2="16.75" />
      <line x1="17.2" y1="9.05" x2="17.2" y2="14.95" />
      <line x1="19.8" y1="10.2" x2="19.8" y2="13.8" />
    </svg>
  );
}

export default function TidalFullScreenPlayer() {
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

  // Synchronized lyric calculation for inline player karaoke
  const trackLyrics = useMemo(() => getTrackLyrics(currentTrack), [currentTrack]);
  const activeLyricIndex = useMemo(() => {
    let index = -1;
    for (let i = 0; i < trackLyrics.length; i++) {
      if (currentTime >= trackLyrics[i].time) {
        index = i;
      }
    }
    return index;
  }, [trackLyrics, currentTime]);

  const activeLyric = activeLyricIndex >= 0 ? trackLyrics[activeLyricIndex] : null;
  const nextLyricTime = activeLyricIndex >= 0 && activeLyricIndex + 1 < trackLyrics.length
    ? trackLyrics[activeLyricIndex + 1].time
    : (activeLyric ? activeLyric.time + 4.5 : 0);

  const renderKaraokeWords = () => {
    if (!activeLyric || !activeLyric.text) return null;
    const words = activeLyric.text.trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return null;

    const lineStart = activeLyric.time;
    const lineDuration = Math.max(1.8, nextLyricTime - lineStart);
    const progress = Math.max(0, Math.min(1, (currentTime - lineStart) / lineDuration));
    const activeWordIdx = Math.floor(progress * words.length);

    return words.map((word, idx) => {
      const isCompleted = idx < activeWordIdx;
      const isActive = idx === activeWordIdx;
      return (
        <span
          key={idx}
          className={`transition-all duration-150 inline-block mr-1.5 ${
            isActive
              ? "text-white font-black scale-[1.03] drop-shadow-[0_2px_8px_rgba(255,255,255,0.45)]"
              : isCompleted
              ? "text-white font-extrabold"
              : "text-white/45 font-semibold"
          }`}
        >
          {word}
        </span>
      );
    });
  };

  const currentQuality = (streamingQuality || audioQuality || "high").toLowerCase();
  const qualityLevel = currentQuality === "low" ? "Low" : currentQuality === "medium" ? "Medium" : "High";
  const codecBadgeLabel = `OPUS ${qualityLevel}`;

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
      <div className="space-y-3">
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

        {/* Animated Karaoke Lyric line directly on top of Seekbar (matches screenshot layout) */}
        <div className="min-h-[30px] flex items-end overflow-hidden pt-1 pb-1">
          {activeLyric ? (
            <div
              key={`${activeLyric.time}-${activeLyric.text}`}
              className="animate-slideUpFade w-full"
            >
              <p className="text-[17px] sm:text-lg font-black tracking-tight leading-snug text-left select-none drop-shadow-[0_2px_8px_rgba(0,0,0,0.65)]">
                {renderKaraokeWords()}
              </p>
            </div>
          ) : (
            <div className="h-6" />
          )}
        </div>

        {/* Timeline Scrubber */}
        <div className="space-y-1">
          <div className="relative py-1 flex items-center">
            {isCrossfading && (
              <div className="absolute inset-x-0 h-4 rounded-full blur-md bg-gradient-to-r from-pink-500 via-amber-400 via-emerald-400 via-cyan-400 to-purple-500 opacity-70 animate-rgbShift pointer-events-none" />
            )}
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={handleSeek}
              className={`relative z-10 w-full h-1.5 rounded-lg appearance-none cursor-pointer transition-all ${
                isCrossfading
                  ? "bg-gradient-to-r from-pink-500 via-amber-400 via-emerald-400 via-cyan-400 to-purple-500 animate-rgbShift shadow-[0_0_16px_rgba(236,72,153,0.8)]"
                  : "bg-white/20 accent-white"
              }`}
            />
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-white/40">
            <span>{formatTime(currentTime)}</span>
            {isCrossfading ? (
              <span className="text-[11px] font-bold text-white tracking-wider animate-pulse">
                Crossfading
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md bg-white/10 text-white/85 text-[11px] font-bold tracking-tight">
                <SoundWaveIcon className="w-3.5 h-3.5 text-white" />
                {codecBadgeLabel}
              </span>
            )}
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
          <span className="inline-flex items-center gap-1.5 text-[11px] font-mono font-bold text-white/70 uppercase tracking-wider">
            <SoundWaveIcon className="w-3.5 h-3.5 text-white/90" />
            {codecBadgeLabel}
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
