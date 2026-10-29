import React from "react";
import { X, Trash2, Radio, Play, ChevronUp, ChevronDown, Sparkles, Music } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalQueueDrawer() {
  const {
    isQueueOpen,
    setIsQueueOpen,
    queue,
    currentTrack,
    isPlaying,
    playTrack,
    removeFromQueue,
    reorderQueue,
    clearQueue,
    endlessListening,
    toggleEndlessListening,
    ensureRadioTail,
    isRadioLoading
  } = usePlayer();

  if (!isQueueOpen) return null;

  const handleAddRecommendations = () => {
    if (currentTrack) {
      ensureRadioTail(currentTrack);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden select-none animate-fadeIn">
      {/* Backdrop */}
      <div
        onClick={() => setIsQueueOpen(false)}
        className="absolute inset-0 bg-black/80 backdrop-blur-md transition-opacity"
      />

      {/* Drawer Container: Full width on mobile, max-w-md on desktop */}
      <div className="absolute inset-y-0 right-0 w-full sm:max-w-md flex pointer-events-auto">
        <div className="w-full bg-[#09090c] border-l border-[#1a1a24] shadow-2xl flex flex-col h-full">
          
          {/* Header */}
          <div className="p-4 sm:p-5 border-b border-[#181820] flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <h3 className="text-base font-black text-white tracking-tight">Play Queue</h3>
              <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-white/10 border border-white/15 text-white">
                {queue.length}
              </span>
            </div>
            <button
              onClick={() => setIsQueueOpen(false)}
              className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10 transition-colors"
              aria-label="Close queue"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Endless Listening / Track Radio Banner (Monochrome Derbis Style) */}
          <div className="p-4 bg-[#111116] border-b border-[#181820] flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-white/10 border border-white/10 text-white">
                <Radio className={`w-4 h-4 ${endlessListening ? "animate-pulse" : ""}`} />
              </div>
              <div>
                <p className="text-xs font-bold text-white">Track Radio (Endless)</p>
                <p className="text-[11px] text-white/50">Auto-play similar Hi-Fi songs</p>
              </div>
            </div>

            <button
              type="button"
              onClick={toggleEndlessListening}
              className={`relative inline-flex h-5 w-10 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none ${
                endlessListening ? "bg-white" : "bg-white/20"
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-black shadow transition duration-200 ${
                  endlessListening ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Queue Content (Scrollable) */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
            
            {/* Now Playing */}
            {currentTrack && (
              <div>
                <span className="text-[10px] font-black tracking-widest text-white/40 uppercase">
                  NOW PLAYING
                </span>
                <div className="mt-2 flex items-center gap-3 p-3 rounded-xl bg-[#14141a] border border-white/15 shadow-md">
                  <img
                    src={currentTrack.thumbnail}
                    alt={currentTrack.title}
                    className="w-11 h-11 rounded-lg object-cover shrink-0 border border-white/10"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs sm:text-sm font-bold text-white truncate">{currentTrack.title}</p>
                    <p className="text-[11px] text-white/50 truncate">{currentTrack.artist}</p>
                  </div>
                  {isPlaying && (
                    <div className="flex items-center gap-0.5 shrink-0 px-2">
                      <span className="w-1 h-3 bg-white rounded-full animate-bounce" />
                      <span className="w-1 h-5 bg-white rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                      <span className="w-1 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Upcoming List */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-black tracking-widest text-white/40 uppercase">
                  Next In Queue ({queue.length})
                </span>
                {queue.length > 0 && (
                  <button
                    onClick={clearQueue}
                    className="text-xs text-white/50 hover:text-red-400 flex items-center gap-1 transition-colors"
                  >
                    <Trash2 className="w-3 h-3" />
                    <span>Clear</span>
                  </button>
                )}
              </div>

              {queue.length === 0 ? (
                <div className="text-center py-10 px-4 rounded-xl bg-white/[0.02] border border-dashed border-white/10 space-y-3">
                  <Music className="w-8 h-8 text-white/30 mx-auto" />
                  <p className="text-sm font-bold text-white/70">Queue is empty</p>
                  <p className="text-xs text-white/40">
                    {endlessListening
                      ? "Track Radio will queue similar songs automatically."
                      : "Add songs to build your listening session."}
                  </p>
                  <button
                    onClick={handleAddRecommendations}
                    disabled={isRadioLoading}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs font-bold border border-white/20 transition-colors"
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>{isRadioLoading ? "Loading Radio..." : "Add Recommendations"}</span>
                  </button>
                </div>
              ) : (
                <div className="space-y-1.5">
                  {queue.map((track, idx) => (
                    <div
                      key={`${track.videoId || track.id}-${idx}`}
                      className="group flex items-center justify-between p-2 rounded-xl bg-white/[0.02] hover:bg-white/[0.06] border border-transparent hover:border-white/10 transition-all"
                    >
                      <div
                        onClick={() => {
                          const remaining = queue.filter((_, i) => i !== idx);
                          playTrack(track, remaining);
                        }}
                        className="flex items-center gap-2.5 min-w-0 flex-1 cursor-pointer"
                      >
                        <div className="relative w-9 h-9 rounded-lg overflow-hidden bg-black/40 shrink-0 border border-white/5">
                          <img
                            src={track.thumbnail}
                            alt={track.title}
                            className="w-full h-full object-cover"
                          />
                          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center">
                            <Play className="w-3.5 h-3.5 text-white fill-white ml-0.5" />
                          </div>
                        </div>

                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-bold text-white truncate group-hover:underline">
                            {track.title}
                          </p>
                          <p className="text-[10px] text-white/50 truncate">
                            {track.artist}
                          </p>
                        </div>
                      </div>

                      {/* Reorder and Delete controls */}
                      <div className="flex items-center gap-0.5 shrink-0 ml-2">
                        {idx > 0 && (
                          <button
                            onClick={() => reorderQueue(idx, idx - 1)}
                            className="p-1 rounded text-white/30 hover:text-white"
                            title="Move Up"
                          >
                            <ChevronUp className="w-3.5 h-3.5" />
                          </button>
                        )}
                        {idx < queue.length - 1 && (
                          <button
                            onClick={() => reorderQueue(idx, idx + 1)}
                            className="p-1 rounded text-white/30 hover:text-white"
                            title="Move Down"
                          >
                            <ChevronDown className="w-3.5 h-3.5" />
                          </button>
                        )}
                        <button
                          onClick={() => removeFromQueue(idx)}
                          className="p-1 rounded text-white/30 hover:text-red-400"
                          title="Remove"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>

        </div>
      </div>
    </div>
  );
}
