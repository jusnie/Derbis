import React, { useState } from "react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalHomeHeader({ onOpenAuth }) {
  const [activeSubTab, setActiveSubTab] = useState("for_you");
  const { userProfile } = usePlayer();

  return (
    <div className="select-none pt-2 pb-4">
      {/* Mobile Top Bar */}
      <div className="flex md:hidden items-center justify-between py-2 mb-3">
        {/* Official Derbis PNG Logo on Left */}
        <div className="flex items-center gap-2">
          <img
            src="/assets/derbis-logo.png"
            alt="Derbis"
            className="w-7 h-7 object-contain"
          />
          <span className="text-sm font-black tracking-[0.15em] text-white">
            DERBIS
          </span>
        </div>

        {/* Log in / Profile pill on Right */}
        {userProfile?.connected ? (
          <button
            onClick={onOpenAuth}
            className="h-8 pl-1.5 pr-3 rounded-full bg-[#181820] border border-white/10 text-white text-xs font-bold flex items-center gap-1.5 shadow"
          >
            {userProfile.avatar && (
              <img src={userProfile.avatar} alt="Avatar" className="w-5 h-5 rounded-full object-cover" />
            )}
            <span className="truncate max-w-[80px]">{userProfile.name?.split(" ")[0]}</span>
          </button>
        ) : (
          <button
            onClick={onOpenAuth}
            className="h-8 px-4 rounded-full bg-white text-black text-xs font-bold tracking-tight hover:bg-white/90 active:scale-95 transition-all shadow"
          >
            Log in / Create a free account
          </button>
        )}
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
