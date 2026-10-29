import React from "react";
import { Home, Compass, Search, Library } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalMobileNav() {
  const { activeTab, setActiveTab } = usePlayer();

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-30 h-14 bg-[#08080a]/95 backdrop-blur-2xl border-t border-[#181820] grid grid-cols-4 items-center select-none">
      <button
        onClick={() => setActiveTab("home")}
        className={`flex flex-col items-center justify-center gap-1 h-full transition-colors ${
          activeTab === "home" ? "text-[#00e5ff]" : "text-white/40"
        }`}
      >
        <Home className="w-4 h-4" />
        <span className="text-[10px] font-bold">Home</span>
      </button>

      <button
        onClick={() => setActiveTab("explore")}
        className={`flex flex-col items-center justify-center gap-1 h-full transition-colors ${
          activeTab === "explore" ? "text-[#00e5ff]" : "text-white/40"
        }`}
      >
        <Compass className="w-4 h-4" />
        <span className="text-[10px] font-bold">Explore</span>
      </button>

      <button
        onClick={() => {
          setActiveTab("home");
          window.scrollTo({ top: 0, behavior: "smooth" });
          const searchInput = document.querySelector('input[type="search"]');
          if (searchInput) searchInput.focus();
        }}
        className="flex flex-col items-center justify-center gap-1 h-full text-white/40 hover:text-white"
      >
        <Search className="w-4 h-4" />
        <span className="text-[10px] font-bold">Search</span>
      </button>

      <button
        onClick={() => setActiveTab("collection")}
        className={`flex flex-col items-center justify-center gap-1 h-full transition-colors ${
          activeTab === "collection" ? "text-[#00e5ff]" : "text-white/40"
        }`}
      >
        <Library className="w-4 h-4" />
        <span className="text-[10px] font-bold">Collection</span>
      </button>
    </nav>
  );
}
