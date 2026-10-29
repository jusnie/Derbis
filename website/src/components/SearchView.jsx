import React, { useRef, useEffect } from "react";
import { Search, Play, Plus, X, Sparkles } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { SEARCH_BROWSE_CATEGORIES, QUICK_PICKS_TRACKS, HITS_TRACK_SETS } from "../data/catalog";

export default function SearchView() {
  const {
    searchQuery,
    setSearchQuery,
    playTrack,
    addToQueue,
    currentTrack,
    isPlaying,
    selectCategoryFilter,
    setActiveTab
  } = usePlayer();

  const inputRef = useRef(null);

  useEffect(() => {
    // Focus search input when opening search view
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  const query = searchQuery.toLowerCase().trim();

  // Search Results
  const matchedTracks = query
    ? QUICK_PICKS_TRACKS.filter(
        t =>
          t.title.toLowerCase().includes(query) ||
          t.artist.toLowerCase().includes(query) ||
          (t.album && t.album.toLowerCase().includes(query)) ||
          (t.genre && t.genre.toLowerCase().includes(query))
      )
    : [];

  const matchedCategories = query
    ? SEARCH_BROWSE_CATEGORIES.filter(
        c =>
          c.title.toLowerCase().includes(query) ||
          c.tagText.toLowerCase().includes(query) ||
          c.artists.some(a => a.name.toLowerCase().includes(query))
      )
    : [];

  const handleCategoryClick = (cat) => {
    selectCategoryFilter(cat);
    setActiveTab("home");
  };

  return (
    <div className="space-y-6 select-none py-2 max-w-6xl animate-fadeIn">
      
      {/* Sticky Clean Search Bar */}
      <div className="relative">
        <label className="relative flex items-center w-full h-12 px-4 rounded-2xl bg-[#121217] hover:bg-[#16161c] focus-within:bg-[#1a1a22] border border-[#22222a] focus-within:border-white/30 transition-all cursor-text shadow-lg">
          <Search className="w-5 h-5 text-white/40 mr-3 shrink-0" />
          <input
            ref={inputRef}
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="What do you want to listen to? (Songs, artists, genres)"
            className="w-full bg-transparent border-0 outline-none text-sm text-white placeholder-white/40 font-medium"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="p-1 rounded-full text-white/40 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </label>
      </div>

      {/* When user has typed a query: Show Instant Search Results */}
      {query ? (
        <div className="space-y-6">
          <div className="flex items-center justify-between border-b border-white/10 pb-3">
            <h3 className="text-sm font-bold text-white/70">
              Results for <span className="text-white">"{searchQuery}"</span>
            </h3>
            <span className="text-xs text-white/40">
              {matchedTracks.length} tracks • {matchedCategories.length} categories
            </span>
          </div>

          {matchedTracks.length === 0 && matchedCategories.length === 0 ? (
            <div className="text-center py-16 space-y-2">
              <Search className="w-10 h-10 text-white/20 mx-auto" />
              <p className="text-base text-white/70 font-semibold">No direct matches found</p>
              <p className="text-xs text-white/40">
                Try searching for "Billie", "Sabrina", "Ariana", "BINI", "Maki", or "Queen".
              </p>
            </div>
          ) : (
            <>
              {/* Matching Tracks */}
              {matchedTracks.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-bold text-white/40 tracking-wider uppercase">
                    Songs
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
                    {matchedTracks.map((track) => {
                      const isCurrent = (currentTrack?.videoId || currentTrack?.id) === (track.videoId || track.id);

                      return (
                        <div
                          key={track.id}
                          onClick={() => playTrack(track, matchedTracks)}
                          className={`group flex items-center justify-between p-2.5 rounded-xl border cursor-pointer transition-all ${
                            isCurrent
                              ? "bg-white/10 border-white/30 text-white"
                              : "bg-[#101014] hover:bg-[#16161c] border-[#1e1e24] hover:border-white/20 text-white/80 hover:text-white"
                          }`}
                        >
                          <div className="flex items-center gap-3 min-w-0 pr-2">
                            <img
                              src={track.thumbnail}
                              alt={track.title}
                              className="w-11 h-11 rounded-lg object-cover shrink-0 shadow"
                            />
                            <div className="min-w-0 flex-1">
                              <p className="text-xs font-bold text-white truncate group-hover:underline">
                                {track.title}
                              </p>
                              <p className="text-[11px] text-white/50 truncate mt-0.5">
                                {track.artist}
                              </p>
                            </div>
                          </div>

                          <div className="flex items-center gap-1.5 shrink-0">
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                addToQueue(track);
                              }}
                              className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/10"
                              title="Add to queue"
                            >
                              <Plus className="w-3.5 h-3.5" />
                            </button>
                            <span className="text-[10px] font-mono text-white/40 w-8 text-right">
                              {track.duration}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Matching Categories */}
              {matchedCategories.length > 0 && (
                <div className="space-y-3 pt-4">
                  <h4 className="text-xs font-bold text-white/40 tracking-wider uppercase">
                    Categories
                  </h4>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {matchedCategories.map((cat) => (
                      <div
                        key={cat.id}
                        onClick={() => handleCategoryClick(cat)}
                        className="group cursor-pointer flex flex-col"
                      >
                        <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#121216] border border-[#222228] shadow group-hover:border-white/30 transition-all">
                          <img
                            src={cat.bgArtistPhoto}
                            alt={cat.title}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          />
                          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/30 to-black/40" />
                          <div className="absolute top-2.5 right-2.5 z-10 w-4 h-4">
                            <img src="/assets/derbis-logo.png" alt="Derbis" className="w-full h-full object-contain" />
                          </div>
                          <span className="absolute top-2.5 left-2.5 z-10 text-xs font-black text-white uppercase">
                            {cat.tagText}
                          </span>
                        </div>
                        <h4 className="mt-2 text-xs font-bold text-white truncate">{cat.title}</h4>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      ) : (
        /* When search query is empty: BROWSE ALL CATEGORIES WITH ARTIST BACKGROUNDS (NO RANDOM COLORS) */
        <div className="space-y-4 pt-2">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
                Browse all
              </h2>
              <p className="text-xs text-white/50 mt-0.5">
                All categories feature real artist photography & curated hit stations
              </p>
            </div>
          </div>

          {/* Grid: 2 columns on mobile, 4 columns on desktop */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
            {SEARCH_BROWSE_CATEGORIES.map((cat) => (
              <div
                key={cat.id}
                onClick={() => handleCategoryClick(cat)}
                className="group cursor-pointer flex flex-col"
              >
                {/* Square Card with Artist Photo Background */}
                <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#121216] border border-[#222228] shadow-lg group-hover:border-white/30 transition-all">
                  
                  {/* Real Artist Photography Background */}
                  <img
                    src={cat.bgArtistPhoto}
                    alt={cat.title}
                    loading="lazy"
                    className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
                  />

                  {/* Dark Vignette Overlay for Crisp Typography */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/35 to-black/50" />

                  {/* Official Derbis PNG Logo on Top Right */}
                  <div className="absolute top-2.5 right-2.5 z-10 w-5 h-5 flex items-center justify-center">
                    <img
                      src="/assets/derbis-logo.png"
                      alt="Derbis"
                      className="w-full h-full object-contain drop-shadow"
                    />
                  </div>

                  {/* Editorial Title Banner */}
                  <div className="absolute top-2.5 left-2.5 z-10 max-w-[80%]">
                    <span className="block text-xs sm:text-sm font-black tracking-tighter text-white uppercase leading-none drop-shadow-md">
                      {cat.tagText}
                    </span>
                  </div>

                  {/* The 3 Circular Artist Avatars on the Card */}
                  <div className="absolute bottom-2.5 left-2.5 z-10 flex items-center justify-between w-[calc(100%-20px)]">
                    <div className="flex -space-x-2 overflow-hidden">
                      {cat.artists.map((artist, aIdx) => (
                        <div
                          key={aIdx}
                          className="w-6 h-6 sm:w-7 sm:h-7 rounded-full border-2 border-black overflow-hidden bg-black/60 shadow-md"
                          title={artist.name}
                        >
                          <img
                            src={artist.avatar}
                            alt={artist.name}
                            loading="lazy"
                            className="w-full h-full object-cover"
                          />
                        </div>
                      ))}
                    </div>

                    {/* Hover Play Button */}
                    <div className="w-7 h-7 rounded-full bg-white text-black flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-lg">
                      <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                    </div>
                  </div>

                </div>

                {/* Title & Subtitle Below the Card */}
                <div className="mt-2 px-0.5">
                  <h3 className="text-xs sm:text-sm font-bold text-white truncate group-hover:underline">
                    {cat.title}
                  </h3>
                  <p className="text-[11px] text-white/50 font-medium truncate mt-0.5">
                    {cat.subtitle}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
}
