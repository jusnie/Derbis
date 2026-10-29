import React from "react";
import { Play, Plus, Music, X } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS, EXPLORE_CATEGORIES } from "../data/catalog";

export default function SearchResults() {
  const { searchQuery, setSearchQuery, playTrack, addToQueue, currentTrack, isPlaying } = usePlayer();

  if (!searchQuery.trim()) return null;

  const query = searchQuery.toLowerCase().trim();

  // Filter songs
  const matchedTracks = QUICK_PICKS_TRACKS.filter(
    t => t.title.toLowerCase().includes(query) || t.artist.toLowerCase().includes(query) || (t.genre && t.genre.toLowerCase().includes(query))
  );

  // Filter genres
  const matchedCategories = EXPLORE_CATEGORIES.filter(
    c => c.label.toLowerCase().includes(query) || c.kicker.toLowerCase().includes(query) || c.artists.some(a => a.name.toLowerCase().includes(query))
  );

  return (
    <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-6 animate-fadeIn">
      <div className="p-6 rounded-3xl bg-black/60 border border-white/10 backdrop-blur-3xl shadow-2xl space-y-6">
        
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2">
            <h3 className="text-xl font-black text-white">
              Search Results for <span className="text-accent">"{searchQuery}"</span>
            </h3>
            <span className="text-xs text-white/40">
              ({matchedTracks.length} songs, {matchedCategories.length} categories)
            </span>
          </div>
          <button
            onClick={() => setSearchQuery("")}
            className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {matchedTracks.length === 0 && matchedCategories.length === 0 ? (
          <div className="text-center py-12 space-y-2">
            <Music className="w-10 h-10 text-white/30 mx-auto" />
            <p className="text-base text-white/70 font-semibold">No direct matches found</p>
            <p className="text-xs text-white/40">
              Try searching for popular artists like "Ariana", "Maki", "Sabrina", "Taylor", or "Queen".
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            
            {/* Tracks */}
            {matchedTracks.length > 0 && (
              <div>
                <h4 className="text-xs font-bold text-accent tracking-wider uppercase mb-3">Matching Songs</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  {matchedTracks.map((track) => {
                    const isCurrent = currentTrack?.id === track.id;
                    const isCurrentlyPlaying = isCurrent && isPlaying;

                    return (
                      <div
                        key={track.id}
                        onClick={() => playTrack(track, matchedTracks)}
                        className="group flex items-center justify-between p-3 rounded-2xl bg-white/[0.04] hover:bg-white/[0.09] border border-white/5 hover:border-white/15 cursor-pointer transition-all"
                      >
                        <div className="flex items-center gap-3 min-w-0 pr-2">
                          <img
                            src={track.thumbnail}
                            alt={track.title}
                            className="w-12 h-12 rounded-xl object-cover shrink-0 shadow"
                          />
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-bold text-white truncate group-hover:text-accent transition-colors">
                              {track.title}
                            </p>
                            <p className="text-xs text-white/50 truncate">{track.artist}</p>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              addToQueue(track);
                            }}
                            className="p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/10"
                            title="Add to queue"
                          >
                            <Plus className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Categories */}
            {matchedCategories.length > 0 && (
              <div>
                <h4 className="text-xs font-bold text-purple-400 tracking-wider uppercase mb-3">Matching Discovery Categories</h4>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {matchedCategories.map((c) => (
                    <div
                      key={c.id}
                      className="p-4 rounded-2xl bg-white/[0.04] border border-white/10 hover:border-white/20 transition-all cursor-pointer"
                    >
                      <p className="text-sm font-bold text-white">{c.label}</p>
                      <p className="text-xs text-white/50 mt-1">{c.kicker}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

          </div>
        )}

      </div>
    </div>
  );
}
