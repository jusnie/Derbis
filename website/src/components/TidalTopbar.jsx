import React, { useRef } from "react";
import { Search, ChevronLeft, ChevronRight, Menu, Settings } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTopbar({ onToggleMobileMenu, onOpenSettings, onOpenAuth }) {
  const { searchQuery, setSearchQuery, userProfile, setActiveTab, activeTab } = usePlayer();
  const inputRef = useRef(null);

  const handleSearchFocus = () => {
    if (activeTab !== "search") {
      setActiveTab("search");
    }
  };

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

      {/* Center: Derbis Clean Pill Search Bar - Clicking immediately switches to Search View */}
      <div className="flex-1 max-w-md mx-2">
        <label className="relative flex items-center w-full h-9 px-3.5 rounded-full bg-[#121217] hover:bg-[#181820] focus-within:bg-[#1e1e26] border border-transparent focus-within:border-white/20 transition-all cursor-text">
          <Search className="w-3.5 h-3.5 text-white/40 mr-2 shrink-0" />
          <input
            ref={inputRef}
            type="search"
            value={searchQuery}
            onFocus={handleSearchFocus}
            onClick={handleSearchFocus}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              handleSearchFocus();
            }}
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

      {/* Right: Auth / Profile Button */}
      <div className="flex items-center gap-2.5 shrink-0">
        {userProfile?.connected ? (
          <button
            onClick={onOpenAuth}
            className="hidden sm:inline-flex items-center gap-2 h-8 pl-1.5 pr-3 rounded-full bg-[#181820] border border-white/10 text-white text-xs font-bold hover:bg-[#202028] transition-all"
          >
            {userProfile.avatar ? (
              <img
                src={userProfile.avatar}
                alt={userProfile.name}
                className="w-5 h-5 rounded-full object-cover"
              />
            ) : (
              <div className="w-5 h-5 rounded-full bg-white text-black flex items-center justify-center text-[10px] font-black">
                {userProfile.name?.charAt(0) || "U"}
              </div>
            )}
            <span className="truncate max-w-[100px]">{userProfile.name?.split(" ")[0]}</span>
          </button>
        ) : (
          <button
            onClick={onOpenAuth}
            className="hidden sm:inline-flex items-center h-8 px-4 rounded-full bg-white text-black text-xs font-bold hover:bg-white/90 active:scale-95 transition-all shadow"
          >
            Log in / Create a free account
          </button>
        )}

        {/* Settings */}
        <button
          onClick={onOpenSettings}
          className="p-1.5 rounded-lg text-white/60 hover:text-white hover:bg-white/10 transition-colors"
          title="Settings"
        >
          <Settings className="w-4 h-4" />
        </button>

        {/* User Avatar Circle */}
        <button
          onClick={onOpenSettings}
          className="w-7 h-7 rounded-full overflow-hidden bg-white text-black flex items-center justify-center font-black text-xs hover:scale-105 transition-transform"
        >
          {userProfile?.avatar ? (
            <img src={userProfile.avatar} alt="User" className="w-full h-full object-cover" />
          ) : (
            <span>{userProfile?.name?.charAt(0) || "Y"}</span>
          )}
        </button>
      </div>
    </header>
  );
}
