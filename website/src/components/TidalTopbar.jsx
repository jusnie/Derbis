import React, { useRef } from "react";
import { Search, ChevronLeft, ChevronRight, Menu, Download, Settings } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTopbar({ onToggleMobileMenu, onOpenSettings, onOpenAuth }) {
  const { searchQuery, setSearchQuery } = usePlayer();
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

        {/* Desktop History chevrons */}
        <div className="hidden md:flex items-center gap-1 text-white/40">
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
        <label className="relative flex items-center w-full h-9 px-3.5 rounded-full bg-[#121217] hover:bg-[#181820] focus-within:bg-[#1e1e26] border border-transparent focus-within:border-white/20 transition-all cursor-text">
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

      {/* Right: "Log in / Create a free account" pill matching Screenshot */}
      <div className="flex items-center gap-3 shrink-0">
        <button
          onClick={onOpenAuth}
          className="hidden sm:inline-flex items-center h-8 px-4 rounded-full bg-white text-black text-xs font-bold hover:bg-white/90 active:scale-95 transition-all shadow"
        >
          Log in / Create a free account
        </button>

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
          className="w-7 h-7 rounded-full bg-white text-black flex items-center justify-center font-black text-xs hover:scale-105 transition-transform"
        >
          Y
        </button>
      </div>
    </header>
  );
}
