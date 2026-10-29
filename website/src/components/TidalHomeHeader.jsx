import React from "react";
import { usePlayer } from "../context/PlayerContext";
import { RefreshCw, CheckCircle2 } from "lucide-react";

export default function TidalHomeHeader({ onOpenAuth }) {
  const {
    activeSubTab,
    setActiveSubTab,
    userProfile,
    isSyncingYouTube,
    clearCategoryFilter,
    activeCategoryFilter
  } = usePlayer();

  const handleSubTabClick = (tab) => {
    setActiveSubTab(tab);
    clearCategoryFilter();
  };

  return (
    <div className="select-none pt-2 pb-2">
      {/* Mobile Top Bar */}
      <div className="flex md:hidden items-center justify-between py-2 mb-3">
        {/* Official Derbis PNG Logo on Left */}
        <div className="flex items-center gap-2">
          <img
            src="/assets/derbis-logo.png"
            alt="Derbis"
            className="w-7 h-7 object-contain drop-shadow"
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

      {/* Large Bold "Home" Title or Welcome Back message */}
      <div className="flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
        <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
          {userProfile?.connected ? `Hi, ${userProfile.name?.split(" ")[0]}` : "Home"}
        </h1>

        {userProfile?.connected && (
          <div className="flex items-center gap-1.5 text-xs text-white/50 font-medium">
            <CheckCircle2 className="w-3.5 h-3.5 text-white" />
            <span>YouTube Music Synced</span>
          </div>
        )}
      </div>

      {/* Loading Bar Animation when Syncing YouTube Data */}
      {isSyncingYouTube && (
        <div className="mt-3 p-3 rounded-xl bg-[#121218] border border-white/15 animate-pulse space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-white">
            <span className="flex items-center gap-2">
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
              <span>Syncing your YouTube Music library & playlists...</span>
            </span>
            <span className="text-white/50 font-mono text-[10px]">APPLYING TO HOME</span>
          </div>
          {/* Animated Loading Bar */}
          <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
            <div className="h-full bg-white rounded-full animate-indeterminate" />
          </div>
        </div>
      )}

      {/* Tabs: For you, Staff Picks, Uploads */}
      <div className="flex items-center gap-6 mt-4 border-b border-[#1a1a22] text-xs font-bold">
        <button
          onClick={() => handleSubTabClick("for_you")}
          className={`pb-2.5 transition-colors relative ${
            activeSubTab === "for_you" && !activeCategoryFilter ? "text-white" : "text-white/50 hover:text-white"
          }`}
        >
          <span>{userProfile?.connected ? "Listen again (For you)" : "For you"}</span>
          {activeSubTab === "for_you" && !activeCategoryFilter && (
            <span className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-white rounded-t-full" />
          )}
        </button>

        <button
          onClick={() => handleSubTabClick("staff_picks")}
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
          onClick={() => handleSubTabClick("uploads")}
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
