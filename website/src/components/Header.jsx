import React, { useState } from "react";
import { Search, Music, Disc, Sparkles, Download, ListMusic, Mic2, Compass, Radio, ExternalLink } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function Header() {
  const {
    activeTab,
    setActiveTab,
    searchQuery,
    setSearchQuery,
    visualizerMode,
    setVisualizerMode,
    isQueueOpen,
    setIsQueueOpen,
    isLyricsOpen,
    setIsLyricsOpen,
    queue
  } = usePlayer();

  const [searchFocused, setSearchFocused] = useState(false);

  return (
    <header className="sticky top-0 z-40 w-full px-4 sm:px-6 py-3 transition-all duration-300">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-3 sm:gap-6 px-4 py-2.5 rounded-2xl bg-black/40 backdrop-blur-2xl border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
        {/* Logo & Brand */}
        <div
          onClick={() => setActiveTab("home")}
          className="flex items-center gap-3 cursor-pointer group select-none shrink-0"
        >
          <div className="relative w-10 h-10 rounded-xl bg-gradient-to-tr from-accent to-pink-500 p-0.5 shadow-lg shadow-accent/30 group-hover:scale-105 transition-transform">
            <div className="w-full h-full bg-[#121218] rounded-[10px] flex items-center justify-center overflow-hidden">
              <span className="text-xl font-black bg-gradient-to-r from-white via-pink-200 to-accent bg-clip-text text-transparent">
                D
              </span>
            </div>
            {/* Live pulsating dot */}
            <span className="absolute -top-0.5 -right-0.5 flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-accent opacity-75" />
              <span className="relative inline-flex rounded-full h-3 w-3 bg-accent" />
            </span>
          </div>
          <div className="hidden md:flex flex-col">
            <div className="flex items-center gap-1.5">
              <span className="font-extrabold text-base tracking-tight text-white group-hover:text-accent transition-colors">
                Derbis
              </span>
              <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-md bg-white/10 text-white/80 border border-white/10 uppercase tracking-widest">
                v1.1.1
              </span>
            </div>
            <span className="text-[11px] text-white/40 tracking-wider">Hi-Fi Music Companion</span>
          </div>
        </div>

        {/* Global Search Bar */}
        <div className={`relative flex-1 max-w-md transition-all duration-300 ${searchFocused ? "max-w-lg" : ""}`}>
          <div className="relative flex items-center">
            <Search className={`absolute left-3.5 w-4 h-4 transition-colors ${searchFocused ? "text-accent" : "text-white/40"}`} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => setSearchFocused(true)}
              onBlur={() => setSearchFocused(false)}
              placeholder="Search 100M+ songs, artists, genres..."
              className="w-full pl-10 pr-4 py-2 text-xs sm:text-sm bg-white/[0.06] hover:bg-white/[0.09] focus:bg-white/[0.12] rounded-xl border border-white/10 focus:border-accent/60 outline-none text-white placeholder-white/40 transition-all shadow-inner"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-3 text-xs text-white/40 hover:text-white"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="hidden lg:flex items-center gap-1 bg-white/[0.04] p-1 rounded-xl border border-white/5">
          <button
            onClick={() => setActiveTab("home")}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              activeTab === "home"
                ? "bg-accent text-white shadow-md shadow-accent/40"
                : "text-white/70 hover:text-white hover:bg-white/5"
            }`}
          >
            Discover
          </button>
          <button
            onClick={() => setActiveTab("explore")}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeTab === "explore"
                ? "bg-accent text-white shadow-md shadow-accent/40"
                : "text-white/70 hover:text-white hover:bg-white/5"
            }`}
          >
            <Compass className="w-3.5 h-3.5" />
            16 Genres
          </button>
        </nav>

        {/* 3D Visualizer Mode Switcher */}
        <div className="hidden sm:flex items-center bg-white/[0.05] border border-white/10 p-0.5 rounded-xl">
          <button
            onClick={() => setVisualizerMode("aura")}
            title="3D Particle Aura"
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs transition-all ${
              visualizerMode === "aura"
                ? "bg-accent text-white shadow"
                : "text-white/60 hover:text-white"
            }`}
          >
            <Sparkles className="w-3 h-3" />
            <span className="hidden xl:inline">Aura</span>
          </button>
          <button
            onClick={() => setVisualizerMode("vinyl")}
            title="3D Tactile Vinyl Disc"
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs transition-all ${
              visualizerMode === "vinyl"
                ? "bg-accent text-white shadow"
                : "text-white/60 hover:text-white"
            }`}
          >
            <Disc className="w-3 h-3" />
            <span className="hidden xl:inline">Vinyl</span>
          </button>
          <button
            onClick={() => setVisualizerMode("waves")}
            title="3D Sound Waves"
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs transition-all ${
              visualizerMode === "waves"
                ? "bg-accent text-white shadow"
                : "text-white/60 hover:text-white"
            }`}
          >
            <Radio className="w-3 h-3" />
            <span className="hidden xl:inline">Waves</span>
          </button>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          {/* Lyrics toggle */}
          <button
            onClick={() => setIsLyricsOpen(!isLyricsOpen)}
            className={`p-2 rounded-xl transition-all border ${
              isLyricsOpen
                ? "bg-accent text-white border-accent shadow-md shadow-accent/30"
                : "bg-white/[0.05] text-white/70 hover:text-white border-white/10"
            }`}
            title="Lyrics view"
          >
            <Mic2 className="w-4 h-4" />
          </button>

          {/* Queue toggle */}
          <button
            onClick={() => setIsQueueOpen(!isQueueOpen)}
            className={`relative p-2 rounded-xl transition-all border ${
              isQueueOpen
                ? "bg-accent text-white border-accent shadow-md shadow-accent/30"
                : "bg-white/[0.05] text-white/70 hover:text-white border-white/10"
            }`}
            title="Up Next Queue"
          >
            <ListMusic className="w-4 h-4" />
            {queue.length > 0 && (
              <span className="absolute -top-1 -right-1 px-1.5 py-0.2 bg-accent text-[10px] font-bold text-white rounded-full ring-2 ring-[#0c0c12]">
                {queue.length}
              </span>
            )}
          </button>

          {/* Direct APK Download Button */}
          <a
            href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-accent to-pink-600 hover:from-pink-600 hover:to-accent text-white text-xs font-semibold shadow-lg shadow-accent/25 hover:shadow-accent/40 transition-all"
            download
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Get APK</span>
          </a>
        </div>
      </div>
    </header>
  );
}
