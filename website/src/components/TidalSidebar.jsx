import React from "react";
import {
  Home,
  Compass,
  Video,
  Radio,
  ListMusic,
  Heart,
  Users,
  Download,
  ExternalLink,
  X
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function TidalSidebar({ isMobileOpen, setIsMobileOpen, onOpenSettings }) {
  const { activeTab, setActiveTab } = usePlayer();

  const handleNav = (tab) => {
    setActiveTab(tab);
    if (setIsMobileOpen) setIsMobileOpen(false);
  };

  return (
    <>
      {/* Mobile Scrim */}
      {isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          className="fixed inset-0 z-40 bg-black/80 backdrop-blur-sm md:hidden"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-40 flex flex-col w-60 bg-[#000000] border-r border-[#181820] px-3.5 py-5 transition-transform duration-300 md:translate-x-0 ${
          isMobileOpen ? "translate-x-0 shadow-2xl" : "-translate-x-full md:translate-x-0"
        }`}
      >
        {/* Brand Header with 4-Diamond Logo (Pure White) */}
        <div className="flex items-center justify-between px-2 mb-7">
          <div
            onClick={() => handleNav("home")}
            className="flex items-center gap-3 cursor-pointer group"
          >
            <div className="grid grid-cols-2 gap-0.5 w-6 h-6 rotate-45 transform group-hover:scale-105 transition-transform">
              <div className="w-2.5 h-2.5 bg-white rounded-[0.5px]" />
              <div className="w-2.5 h-2.5 bg-white rounded-[0.5px]" />
              <div className="w-2.5 h-2.5 bg-white rounded-[0.5px]" />
              <div className="w-2.5 h-2.5 bg-white rounded-[0.5px]" />
            </div>

            <div className="flex flex-col">
              <span className="text-base font-black tracking-[0.2em] text-white">
                DERBIS
              </span>
              <span className="text-[9px] font-bold tracking-widest text-white/40 uppercase -mt-0.5">
                HI-FI AUDIO
              </span>
            </div>
          </div>

          <button
            onClick={() => setIsMobileOpen(false)}
            className="p-1 rounded-lg text-white/50 hover:text-white md:hidden"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Primary Navigation */}
        <nav className="flex flex-col gap-0.5">
          <button
            onClick={() => handleNav("home")}
            className={`flex items-center gap-3 w-full h-10 px-3 rounded-lg text-xs font-bold transition-colors ${
              activeTab === "home"
                ? "bg-[#181820] text-white"
                : "text-white/50 hover:text-white hover:bg-white/[0.04]"
            }`}
          >
            <Home className="w-4 h-4" />
            <span>Home</span>
          </button>

          <button
            onClick={() => handleNav("explore")}
            className={`flex items-center gap-3 w-full h-10 px-3 rounded-lg text-xs font-bold transition-colors ${
              activeTab === "explore"
                ? "bg-[#181820] text-white"
                : "text-white/50 hover:text-white hover:bg-white/[0.04]"
            }`}
          >
            <Compass className="w-4 h-4" />
            <span>Explore</span>
          </button>

          <button
            onClick={() => handleNav("videos")}
            className={`flex items-center gap-3 w-full h-10 px-3 rounded-lg text-xs font-bold transition-colors ${
              activeTab === "videos"
                ? "bg-[#181820] text-white"
                : "text-white/50 hover:text-white hover:bg-white/[0.04]"
            }`}
          >
            <Video className="w-4 h-4" />
            <span>Videos</span>
          </button>
        </nav>

        {/* Section: MY COLLECTION */}
        <div className="mt-7 flex flex-col gap-0.5">
          <p className="px-3 text-[10px] font-black tracking-wider text-white/40 uppercase mb-2">
            MY COLLECTION
          </p>

          <button
            onClick={() => handleNav("collection")}
            className={`flex items-center gap-3 w-full h-9 px-3 rounded-lg text-xs font-semibold transition-colors ${
              activeTab === "collection"
                ? "bg-[#181820] text-white"
                : "text-white/50 hover:text-white hover:bg-white/[0.04]"
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Mixes & Radio</span>
          </button>

          <button
            onClick={() => handleNav("collection")}
            className="flex items-center gap-3 w-full h-9 px-3 rounded-lg text-xs font-semibold text-white/50 hover:text-white hover:bg-white/[0.04] transition-colors"
          >
            <ListMusic className="w-3.5 h-3.5" />
            <span>Playlists</span>
          </button>

          <button
            onClick={() => handleNav("collection")}
            className="flex items-center gap-3 w-full h-9 px-3 rounded-lg text-xs font-semibold text-white/50 hover:text-white hover:bg-white/[0.04] transition-colors"
          >
            <Heart className="w-3.5 h-3.5" />
            <span>Tracks</span>
          </button>

          <button
            onClick={() => handleNav("collection")}
            className="flex items-center gap-3 w-full h-9 px-3 rounded-lg text-xs font-semibold text-white/50 hover:text-white hover:bg-white/[0.04] transition-colors"
          >
            <Users className="w-3.5 h-3.5" />
            <span>Artists</span>
          </button>
        </div>

        {/* Android App Promotion Card */}
        <div className="mt-6 px-1">
          <a
            href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
            download
            className="block p-3 rounded-xl bg-[#0e0e12] hover:bg-[#141418] border border-[#1f1f26] transition-all group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-[10px] font-black tracking-wider text-white uppercase">
                ANDROID APP
              </span>
              <span className="text-[9px] font-bold text-white/40">v1.1.1</span>
            </div>
            <p className="text-xs font-bold text-white leading-tight">
              Derbis Companion
            </p>
            <p className="text-[10px] text-white/40 mt-0.5">
              Offline & background playback
            </p>
            <div className="mt-2.5 flex items-center justify-between text-[11px] font-bold text-white/80 group-hover:text-white transition-colors">
              <span>Get APK (~71 MB)</span>
              <Download className="w-3.5 h-3.5" />
            </div>
          </a>
        </div>

        {/* Profile Card at Bottom */}
        <div className="mt-auto pt-4 border-t border-[#181820]">
          <button
            onClick={onOpenSettings}
            className="flex items-center gap-3 w-full p-2 rounded-xl hover:bg-[#121216] transition-colors text-left"
          >
            <div className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center font-black text-xs">
              Y
            </div>
            <div className="min-w-0 flex-1">
              <strong className="block text-xs font-bold text-white truncate">
                Yasuda
              </strong>
              <small className="block text-[10px] text-white/50 font-semibold truncate">
                Hi-Fi Account
              </small>
            </div>
          </button>
        </div>
      </aside>
    </>
  );
}
