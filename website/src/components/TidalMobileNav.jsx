import React from "react";
import { Music, Bell, ArrowUp, AlignJustify, Search } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalMobileNav({ onOpenAuth }) {
  const { activeTab, setActiveTab } = usePlayer();

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-30 h-14 bg-[#0a0a0c] border-t border-[#181820] flex items-center justify-around px-2 select-none">
      {/* 1. Music Note in Rounded Pill (Active indicator matching screenshot) */}
      <button
        onClick={() => setActiveTab("home")}
        className={`flex items-center justify-center w-14 h-9 rounded-full transition-all ${
          activeTab === "home" ? "bg-white/15 text-white" : "text-white/40 hover:text-white"
        }`}
        aria-label="Home Music"
      >
        <Music className="w-5 h-5" />
      </button>

      {/* 2. Notifications Bell (matching screenshot) */}
      <button
        onClick={onOpenAuth}
        className="flex items-center justify-center w-10 h-10 text-white/40 hover:text-white transition-colors"
        aria-label="Notifications"
      >
        <Bell className="w-5 h-5" />
      </button>

      {/* 3. Up Arrow (Uploads / Staff picks matching screenshot) */}
      <button
        onClick={() => setActiveTab("explore")}
        className={`flex items-center justify-center w-10 h-10 transition-colors ${
          activeTab === "explore" ? "text-white" : "text-white/40 hover:text-white"
        }`}
        aria-label="Explore & Uploads"
      >
        <ArrowUp className="w-5 h-5" />
      </button>

      {/* 4. Three Vertical Lines (Collection / Library matching screenshot) */}
      <button
        onClick={() => setActiveTab("collection")}
        className={`flex items-center justify-center w-10 h-10 transition-colors ${
          activeTab === "collection" ? "text-white" : "text-white/40 hover:text-white"
        }`}
        aria-label="My Collection"
      >
        <div className="flex gap-1 items-center justify-center h-4">
          <span className="w-[2px] h-4 bg-current rounded-full" />
          <span className="w-[2px] h-4 bg-current rounded-full" />
          <span className="w-[2px] h-4 bg-current rounded-full" />
        </div>
      </button>

      {/* 5. Search Magnifying Glass (matching screenshot) */}
      <button
        onClick={() => {
          setActiveTab("home");
          window.scrollTo({ top: 0, behavior: "smooth" });
          const searchInput = document.querySelector('input[type="search"]');
          if (searchInput) searchInput.focus();
        }}
        className="flex items-center justify-center w-10 h-10 text-white/40 hover:text-white transition-colors"
        aria-label="Search"
      >
        <Search className="w-5 h-5" />
      </button>
    </nav>
  );
}
