import React from "react";
import { X, Trash2, Radio, Play, ChevronUp, ChevronDown, Sparkles, Music } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS } from "../data/catalog";

export default function QueueDrawer() {
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
    addToQueue
  } = usePlayer();

  if (!isQueueOpen) return null;

  const addRecommendations = () => {
    const unqueued = QUICK_PICKS_TRACKS.filter(t => t.id !== currentTrack.id && !queue.find(q => q.id === t.id)).slice(0, 5);
    unqueued.forEach(t => addToQueue(t));
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden pointer-events-none">
      {/* Backdrop */}
      <div
        onClick={() => setIsQueueOpen(false)}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm pointer-events-auto transition-opacity"
      />

      {/* Drawer Panel */}
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10 pointer-events-auto">
        <div className="w-screen max-w-md bg-[#101016]/95 backdrop-blur-3xl border-l border-white/10 shadow-2xl flex flex-col">
          
          {/* Drawer Header */}
          <div className="p-5 border-b border-white/10 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-bold text-white">Up Next</h3>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-accent/20 text-accent">
                {queue.length}
              </span>
            </div>
            <button
              onClick={() => setIsQueueOpen(false)}
              className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Endless Listening Continuous Radio Switch Header (Matches DerbisApp QueueReferenceHeading) */}
          <div className="p-4 bg-white/[0.03] border-b border-white/10 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                <Radio className={`w-4 h-4 ${endlessListening ? "animate-pulse" : ""}`} />
              </div>
              <div>
                <p className="text-xs font-bold text-white">Endless listening</p>
                <p className="text-[11px] text-white/50">Continuous radio when queue ends</p>
              </div>
            </div>

            {/* Toggle Switch */}
            <button
              type="button"
              onClick={toggleEndlessListening}
              className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                endlessListening ? "bg-emerald-500" : "bg-white/20"
              }`}
              role="switch"
              aria-checked={endlessListening}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                  endlessListening ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Scrollable Queue Content */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
            
            {/* Now Playing Item */}
            <div>
              <span className="text-[11px] font-bold tracking-wider text-accent uppercase">Now Playing</span>
              <div className="mt-2 flex items-center gap-3 p-3 rounded-2xl bg-accent/15 border border-accent/30 shadow-md">
                <img
                  src={currentTrack.thumbnail}
                  alt={currentTrack.title}
                  className="w-12 h-12 rounded-xl object-cover shrink-0 shadow"
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-white truncate">{currentTrack.title}</p>
                  <p className="text-xs text-white/60 truncate">{currentTrack.artist}</p>
                </div>
                {isPlaying && (
                  <div className="flex items-center gap-0.5 shrink-0 px-2">
                    <span className="w-1 h-3 bg-accent rounded-full animate-bounce" />
                    <span className="w-1 h-5 bg-accent rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="w-1 h-2 bg-accent rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                  </div>
                )}
              </div>
            </div>

            {/* Queue List */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[11px] font-bold tracking-wider text-white/50 uppercase">
                  Upcoming ({queue.length})
                </span>
                {queue.length > 0 && (
                  <button
                    onClick={clearQueue}
                    className="text-xs text-red-400 hover:text-red-300 flex items-center gap-1 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Clear</span>
                  </button>
                )}
              </div>

              {queue.length === 0 ? (
                <div className="text-center py-10 px-4 rounded-2xl bg-white/[0.02] border border-dashed border-white/10 space-y-3">
                  <Music className="w-8 h-8 text-white/30 mx-auto" />
                  <p className="text-sm font-medium text-white/70">Your queue is empty</p>
                  <p className="text-xs text-white/40 max-w-xs mx-auto">
                    {endlessListening
                      ? "Endless listening will automatically play next recommendations!"
                      : "Add songs from Quick Picks or Explore to build your session."}
                  </p>
                  <button
                    onClick={addRecommendations}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-accent/20 hover:bg-accent/30 text-accent text-xs font-semibold border border-accent/40 transition-all"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>Add Recommendations</span>
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  {queue.map((track, idx) => (
                    <div
                      key={`${track.id}-${idx}`}
                      className="group flex items-center justify-between p-2.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.08] border border-white/5 hover:border-white/15 transition-all"
                    >
                      {/* Play on click */}
                      <div
                        onClick={() => {
                          const remaining = queue.filter((_, i) => i !== idx);
                          playTrack(track, remaining);
                        }}
                        className="flex items-center gap-3 min-w-0 flex-1 cursor-pointer"
                      >
                        <div className="relative w-10 h-10 rounded-xl overflow-hidden bg-black/40 shrink-0">
                          <img
                            src={track.thumbnail}
                            alt={track.title}
                            className="w-full h-full object-cover"
                          />
                          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                            <Play className="w-4 h-4 text-white fill-white ml-0.5" />
                          </div>
                        </div>

                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-bold text-white truncate group-hover:text-accent transition-colors">
                            {track.title}
                          </p>
                          <p className="text-[11px] text-white/50 truncate">
                            {track.artist}
                          </p>
                        </div>
                      </div>

                      {/* Reorder and Delete controls */}
                      <div className="flex items-center gap-1 shrink-0 ml-2">
                        {idx > 0 && (
                          <button
                            onClick={() => reorderQueue(idx, idx - 1)}
                            title="Move Up"
                            className="p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/10"
                          >
                            <ChevronUp className="w-3.5 h-3.5" />
                          </button>
                        )}
                        {idx < queue.length - 1 && (
                          <button
                            onClick={() => reorderQueue(idx, idx + 1)}
                            title="Move Down"
                            className="p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/10"
                          >
                            <ChevronDown className="w-3.5 h-3.5" />
                          </button>
                        )}
                        <button
                          onClick={() => removeFromQueue(idx)}
                          title="Remove from queue"
                          className="p-1 rounded-lg text-white/40 hover:text-red-400 hover:bg-white/10 ml-1"
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
