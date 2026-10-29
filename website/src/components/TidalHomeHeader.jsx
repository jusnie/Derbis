import React, { useState } from "react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalHomeHeader({ onOpenAuth }) {
  const [activeSubTab, setActiveSubTab] = useState("for_you");

  return (
    <div className="select-none pt-2 pb-4">
      {/* Mobile Top Bar matching screenshot */}
      <div className="flex md:hidden items-center justify-between py-2 mb-3">
        {/* 4-Diamond Logo on Left */}
        <div className="grid grid-cols-2 gap-0.5 w-5 h-5 rotate-45 transform">
          <div className="w-2 h-2 bg-white rounded-[0.5px]" />
          <div className="w-2 h-2 bg-white rounded-[0.5px]" />
          <div className="w-2 h-2 bg-white rounded-[0.5px]" />
          <div className="w-2 h-2 bg-white rounded-[0.5px]" />
        </div>

        {/* Log in / Create a free account pill on Right */}
        <button
          onClick={onOpenAuth}
          className="h-8 px-4 rounded-full bg-white text-black text-xs font-bold tracking-tight hover:bg-white/90 active:scale-95 transition-all shadow"
        >
          Log in / Create a free account
        </button>
      </div>

      {/* Large Bold "Home" Title */}
      <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
        Home
      </h1>

      {/* Tabs: For you, Staff Picks, Uploads */}
      <div className="flex items-center gap-6 mt-4 border-b border-[#1a1a22] text-xs font-bold">
        <button
          onClick={() => setActiveSubTab("for_you")}
          className={`pb-2.5 transition-colors relative ${
            activeSubTab === "for_you" ? "text-white" : "text-white/50 hover:text-white"
          }`}
        >
          <span>For you</span>
          {activeSubTab === "for_you" && (
            <span className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-white rounded-t-full" />
          )}
        </button>

        <button
          onClick={() => setActiveSubTab("staff_picks")}
          className={`pb-2.5 transition-colors relative ${
            activeSubTab === "staff_picks" ? "text-white" : "text-white/50 hover:text-white"
          }`}
        >
          <span>Staff Picks</span>
          {activeSubTab === "staff_picks" && (
            <span className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-white rounded-t-full" />
          )}
        </button>

        <button
          onClick={() => setActiveSubTab("uploads")}
          className={`pb-2.5 transition-colors relative ${
            activeSubTab === "uploads" ? "text-white" : "text-white/50 hover:text-white"
          }`}
        >
          <span>Uploads</span>
          {activeSubTab === "uploads" && (
            <span className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-white rounded-t-full" />
          )}
        </button>
      </div>
    </div>
  );
}
