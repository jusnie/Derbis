import React, { useRef } from "react";
import { Search, ChevronLeft, ChevronRight, Menu, Download, Settings, Sparkles } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTopbar({ onToggleMobileMenu, onOpenSettings }) {
  const { searchQuery, setSearchQuery, audioQuality } = usePlayer();
  const inputRef = useRef(null);

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-3 h-16 px-4 sm:px-8 bg-[#000000]/95 backdrop-blur-xl border-b border-[#141418]">
      {/* Left: Mobile hamburger or Desktop History arrows */}
      <div className="flex items-center gap-3 shrink-0">
        <button
          onClick={onToggleMobileMenu}
          className="p-1.5 rounded-lg text-white/70 hover:text-white md:hidden"
          aria-label="Open navigation"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Mobile Brand */}
        <div className="flex items-center gap-2 md:hidden">
          <div className="grid grid-cols-2 gap-0.5 w-4 h-4 rotate-45 transform">
            <div className="w-1.5 h-1.5 bg-[#00e5ff] rounded-[0.5px]" />
            <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
            <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
            <div className="w-1.5 h-1.5 bg-[#00e5ff] rounded-[0.5px]" />
          </div>
          <span className="text-xs font-black tracking-widest text-white">DERBIS</span>
        </div>

        {/* Desktop History chevrons */}
        <div className="hidden md:flex items-center gap-1 text-white/50">
          <button
            onClick={() => window.history.back()}
            className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center transition-colors"
            title="Go back"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            onClick={() => window.history.forward()}
            className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center transition-colors"
            title="Go forward"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Center: Tidal-style Clean Pill Search Bar */}
      <div className="flex-1 max-w-md mx-2">
        <label className="relative flex items-center w-full h-9 px-3 rounded-full bg-[#141418] hover:bg-[#1a1a20] focus-within:bg-[#202028] border border-transparent focus-within:border-white/20 transition-all cursor-text">
          <Search className="w-3.5 h-3.5 text-white/40 mr-2 shrink-0" />
          <input
            ref={inputRef}
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search songs, artists, or genres"
            className="w-full bg-transparent border-0 outline-none text-xs text-white placeholder-white/40"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="text-[10px] text-white/40 hover:text-white"
            >
              Clear
            </button>
          )}
        </label>
      </div>

      {/* Right: Tidal Audio Quality Badge, APK link & Settings */}
      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        {/* Tidal Audio Quality Badge */}
        <div
          title="Streaming in Hi-Res Lossless Audio (24-bit, 96 kHz)"
          className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-[#00e5ff]/10 border border-[#00e5ff]/30 text-[#00e5ff] text-[10px] font-black tracking-widest uppercase select-none"
        >
          <Sparkles className="w-3 h-3" />
          <span>{audioQuality}</span>
        </div>

        {/* Direct APK Link */}
        <a
          href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
          download
          className="hidden lg:inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 hover:bg-white/20 text-white text-xs font-bold border border-white/10 transition-all"
        >
          <Download className="w-3 h-3 text-[#00e5ff]" />
          <span>Get App</span>
        </a>

        {/* Settings */}
        <button
          onClick={onOpenSettings}
          className="p-1.5 rounded-lg text-white/60 hover:text-white hover:bg-white/10 transition-colors"
          title="Settings"
        >
          <Settings className="w-4 h-4" />
        </button>

        {/* User Avatar */}
        <button
          onClick={onOpenSettings}
          className="w-7 h-7 rounded-full bg-gradient-to-tr from-cyan-400 to-blue-600 flex items-center justify-center text-black font-black text-xs hover:scale-105 transition-transform"
        >
          Y
        </button>
      </div>
    </header>
  );
}
