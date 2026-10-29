import React, { useEffect, useState, useRef } from "react";
import { Search, Menu, Bell, Settings, Sun, Moon, Plus } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function Topbar({ onToggleMobileMenu, onOpenSettings }) {
  const { searchQuery, setSearchQuery } = usePlayer();
  const [greeting, setGreeting] = useState("Good day");
  const searchInputRef = useRef(null);

  // Time-based dynamic greeting (morning, afternoon, evening)
  useEffect(() => {
    const hour = new Date().getHours();
    if (hour < 12) setGreeting("Good morning");
    else if (hour < 18) setGreeting("Good afternoon");
    else setGreeting("Good evening");
  }, []);

  // Keyboard shortcut: Command+K / Ctrl+K focus search
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        if (searchInputRef.current) {
          searchInputRef.current.focus();
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-4 h-20 px-6 sm:px-10 border-b border-white/10 bg-[#050506]/90 backdrop-blur-2xl">
      {/* Start: Greeting / Mobile Hamburger */}
      <div className="flex items-center gap-4 min-w-0">
        <button
          onClick={onToggleMobileMenu}
          className="p-2 rounded-xl border border-white/10 bg-white/5 text-white/80 hover:text-white md:hidden"
          aria-label="Open navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Mobile Brand */}
        <div className="flex items-center gap-2 md:hidden">
          <img src="/assets/derbis-logo.png" alt="Derbis" className="w-6 h-6 object-contain" />
          <span className="text-sm font-black tracking-wider text-white">DERBIS</span>
        </div>

        {/* Desktop Greeting */}
        <div className="hidden md:flex flex-col">
          <p className="text-xs font-semibold text-white/50">{greeting}</p>
          <h1 className="text-xl font-black text-white tracking-tight">Yasuda</h1>
        </div>
      </div>

      {/* Global Search Bar with ⌘K */}
      <div className="flex-1 max-w-md mx-2">
        <label className="relative flex items-center w-full h-11 px-3.5 rounded-2xl bg-white/[0.05] hover:bg-white/[0.08] focus-within:bg-blue-500/10 border border-white/10 focus-within:border-blue-400/60 transition-all cursor-text">
          <Search className="w-4 h-4 text-white/40 mr-2.5 shrink-0" />
          <input
            ref={searchInputRef}
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search songs, artists, and playlists"
            className="w-full bg-transparent border-0 outline-none text-xs sm:text-sm text-white placeholder-white/40"
          />
          <kbd className="hidden sm:inline-flex items-center px-2 py-0.5 text-[10px] font-mono font-bold text-white/40 bg-white/5 border border-white/10 rounded-md shrink-0 select-none">
            ⌘ K
          </kbd>
        </label>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        <button
          onClick={() => {}}
          className="hidden lg:inline-flex items-center gap-2 h-10 px-4 rounded-xl bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 text-white text-xs font-bold shadow-lg shadow-blue-500/20 transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Create</span>
        </button>

        <button
          onClick={() => {}}
          title="Notifications"
          className="relative p-2.5 rounded-xl border border-white/10 bg-white/[0.04] hover:bg-white/[0.08] text-white/70 hover:text-white transition-all"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-blue-500 ring-2 ring-[#050506]" />
        </button>

        <button
          onClick={onOpenSettings}
          title="Settings"
          className="p-2.5 rounded-xl border border-white/10 bg-white/[0.04] hover:bg-white/[0.08] text-white/70 hover:text-white transition-all"
        >
          <Settings className="w-4 h-4" />
        </button>

        <button
          onClick={onOpenSettings}
          className="w-9 h-9 rounded-full bg-gradient-to-tr from-pink-500 to-rose-700 flex items-center justify-center text-white text-xs font-black shadow ring-1 ring-white/20 hover:scale-105 transition-transform"
          aria-label="User profile"
        >
          Y
        </button>
      </div>
    </header>
  );
}
