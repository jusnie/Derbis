import React from "react";
import {
  Home,
  Compass,
  Library,
  Plus,
  Heart,
  ListMusic,
  Settings,
  ExternalLink,
  Download,
  X,
  MoreHorizontal
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { isIos } from "../utils/platform";

export default function Sidebar({ isMobileOpen, setIsMobileOpen, onOpenSettings }) {
  const { activeTab, setActiveTab } = usePlayer();

  const handleNavClick = (tab) => {
    setActiveTab(tab);
    if (setIsMobileOpen) setIsMobileOpen(false);
  };

  return (
    <>
      {/* Mobile Scrim Backdrop */}
      {isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm md:hidden transition-opacity"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-40 flex flex-col w-64 bg-[#0a0a0c] border-r border-white/10 px-4 py-6 transition-transform duration-300 md:translate-x-0 ${
          isMobileOpen ? "translate-x-0 shadow-2xl" : "-translate-x-full md:translate-x-0"
        }`}
      >
        {/* Brand Header */}
        <div className="flex items-center justify-between px-2 mb-8">
          <div
            onClick={() => handleNavClick("home")}
            className="flex items-center gap-2.5 cursor-pointer group"
          >
            <img
              src="/assets/derbis-logo.png"
              alt="Derbis"
              className="w-7 h-7 object-contain group-hover:scale-105 transition-transform"
            />
            <span className="text-lg font-black tracking-widest text-white">
              DERBIS
            </span>
          </div>

          {/* Close button for mobile */}
          <button
            onClick={() => setIsMobileOpen(false)}
            className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10 md:hidden"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Primary Navigation */}
        <nav className="flex flex-col gap-1">
          <button
            onClick={() => handleNavClick("home")}
            className={`relative flex items-center gap-3.5 w-full h-12 px-3 rounded-2xl text-sm font-bold text-left transition-all ${
              activeTab === "home"
                ? "bg-gradient-to-r from-blue-500/20 to-white/5 text-white border border-blue-500/30"
                : "text-white/60 hover:text-white hover:bg-white/5 border border-transparent"
            }`}
          >
            {activeTab === "home" && (
              <span className="absolute -left-4 top-1/2 -translate-y-1/2 w-1.5 h-6 bg-[#5d9cff] rounded-r-md shadow-[0_0_12px_#5d9cff]" />
            )}
            <Home className="w-5 h-5 shrink-0" />
            <span>Home</span>
          </button>

          <button
            onClick={() => handleNavClick("explore")}
            className={`relative flex items-center gap-3.5 w-full h-12 px-3 rounded-2xl text-sm font-bold text-left transition-all ${
              activeTab === "explore"
                ? "bg-gradient-to-r from-blue-500/20 to-white/5 text-white border border-blue-500/30"
                : "text-white/60 hover:text-white hover:bg-white/5 border border-transparent"
            }`}
          >
            {activeTab === "explore" && (
              <span className="absolute -left-4 top-1/2 -translate-y-1/2 w-1.5 h-6 bg-[#5d9cff] rounded-r-md shadow-[0_0_12px_#5d9cff]" />
            )}
            <Compass className="w-5 h-5 shrink-0" />
            <span>Explore</span>
          </button>

          <button
            onClick={() => handleNavClick("library")}
            className={`relative flex items-center gap-3.5 w-full h-12 px-3 rounded-2xl text-sm font-bold text-left transition-all ${
              activeTab === "library"
                ? "bg-gradient-to-r from-blue-500/20 to-white/5 text-white border border-blue-500/30"
                : "text-white/60 hover:text-white hover:bg-white/5 border border-transparent"
            }`}
          >
            {activeTab === "library" && (
              <span className="absolute -left-4 top-1/2 -translate-y-1/2 w-1.5 h-6 bg-[#5d9cff] rounded-r-md shadow-[0_0_12px_#5d9cff]" />
            )}
            <Library className="w-5 h-5 shrink-0" />
            <span>Library</span>
          </button>
        </nav>

        {/* Divider */}
        <div className="h-px bg-white/10 my-6 mx-2" />

        {/* Your Space Section */}
        <div className="flex flex-col gap-1">
          <p className="px-3 text-[10px] font-extrabold tracking-widest text-white/40 uppercase mb-2">
            YOUR SPACE
          </p>

          <button
            onClick={() => handleNavClick("library")}
            className="flex items-center gap-3.5 w-full h-10 px-3 rounded-xl text-xs font-semibold text-white/60 hover:text-white hover:bg-white/5 transition-all text-left"
          >
            <Heart className="w-4 h-4 text-pink-500 shrink-0" />
            <span>Favorites</span>
          </button>

          <button
            onClick={() => handleNavClick("library")}
            className="flex items-center gap-3.5 w-full h-10 px-3 rounded-xl text-xs font-semibold text-white/60 hover:text-white hover:bg-white/5 transition-all text-left"
          >
            <ListMusic className="w-4 h-4 text-blue-400 shrink-0" />
            <span>Playlists</span>
          </button>

          <button
            onClick={onOpenSettings}
            className="flex items-center gap-3.5 w-full h-10 px-3 rounded-xl text-xs font-semibold text-white/60 hover:text-white hover:bg-white/5 transition-all text-left"
          >
            <Settings className="w-4 h-4 shrink-0" />
            <span>Settings</span>
          </button>
        </div>

        {/* APK Download Button (Hidden on iOS) */}
        {!isIos && (
          <div className="mt-4 px-2">
            <a
              href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
              download
              className="flex items-center justify-between p-3 rounded-2xl bg-gradient-to-r from-blue-600/20 to-purple-600/20 hover:from-blue-600/30 hover:to-purple-600/30 border border-blue-500/30 text-white transition-all group"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-blue-500/20 flex items-center justify-center text-blue-400 group-hover:scale-110 transition-transform">
                  <Download className="w-4 h-4" />
                </div>
                <div className="flex flex-col">
                  <span className="text-xs font-bold leading-tight">Derbis Android</span>
                  <span className="text-[10px] text-white/50">v1.1.1 APK</span>
                </div>
              </div>
              <span className="text-[10px] font-bold text-blue-400 group-hover:translate-x-0.5 transition-transform">
                Get
              </span>
            </a>
          </div>
        )}

        {/* Bottom Sidebar: Profile Card & GitHub */}
        <div className="mt-auto pt-4 flex flex-col gap-3">
          <button
            onClick={onOpenSettings}
            className="flex items-center gap-3 w-full p-2.5 rounded-2xl bg-[#111115] hover:bg-[#16161c] border border-white/10 hover:border-white/20 transition-all text-left group"
          >
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-pink-500 to-rose-700 flex items-center justify-center text-white text-xs font-black shrink-0">
              Y
            </div>
            <div className="min-w-0 flex-1">
              <strong className="block text-xs font-bold text-white truncate group-hover:text-blue-400 transition-colors">
                Yasuda
              </strong>
              <small className="block text-[11px] text-white/50 truncate">
                Connect Google
              </small>
            </div>
            <MoreHorizontal className="w-4 h-4 text-white/40 shrink-0" />
          </button>

          <a
            href="https://github.com/jusnie/Derbis"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 px-3 text-[11px] font-semibold text-white/40 hover:text-white transition-colors"
          >
            <span>GitHub</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </aside>
    </>
  );
}
