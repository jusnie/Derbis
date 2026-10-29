import React, { useState } from "react";
import { PlayerProvider, usePlayer } from "./context/PlayerContext";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import SoundtrackHero from "./components/SoundtrackHero";
import MoodScroller from "./components/MoodScroller";
import QuickPicksShelf from "./components/QuickPicksShelf";
import ExploreBoard from "./components/ExploreBoard";
import LibraryView from "./components/LibraryView";
import ApkDownloadBanner from "./components/ApkDownloadBanner";
import PlayerDock from "./components/PlayerDock";
import QueueDrawer from "./components/QueueDrawer";
import LyricsModal from "./components/LyricsModal";
import SearchResults from "./components/SearchResults";
import SettingsModal from "./components/SettingsModal";
import { X } from "lucide-react";

function MainShell() {
  const {
    activeTab,
    searchQuery,
    playbackMode,
    setPlaybackMode,
    currentTrack
  } = usePlayer();

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  return (
    <div className="relative min-h-screen bg-[#050506] text-white flex flex-col font-sans selection:bg-[#5d9cff] selection:text-black">
      
      {/* Background Nocturnal Atmospheric Glows */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        <div className="absolute top-0 right-1/4 w-[600px] h-[500px] bg-blue-600/10 rounded-full blur-[160px]" />
        <div className="absolute top-1/2 left-1/3 w-[500px] h-[500px] bg-purple-700/8 rounded-full blur-[160px]" />
      </div>

      {/* Persistent Audio Engine (Song mode = offscreen, Video mode = floating theater PiP) */}
      <div
        className={`transition-all duration-300 ${
          playbackMode === "video"
            ? "fixed top-24 right-6 z-40 w-[320px] sm:w-[420px] aspect-video rounded-3xl overflow-hidden bg-black/90 border border-white/20 shadow-2xl backdrop-blur-3xl animate-scaleIn"
            : "fixed -left-[9999px] -top-[9999px] w-[320px] h-[240px] opacity-0 pointer-events-none"
        }`}
      >
        {playbackMode === "video" && (
          <div className="absolute top-0 inset-x-0 z-20 flex items-center justify-between p-3 bg-gradient-to-b from-black/80 to-transparent">
            <span className="text-xs font-bold text-white/90 truncate max-w-[200px]">
              {currentTrack?.title}
            </span>
            <button
              onClick={() => setPlaybackMode("song")}
              className="p-1 rounded-lg bg-black/60 text-white/70 hover:text-white border border-white/10"
              title="Close Video View"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
        <div id="yt-audio-engine" className="w-full h-full" />
      </div>

      {/* Left Sidebar (256px wide on desktop) */}
      <Sidebar
        isMobileOpen={isMobileMenuOpen}
        setIsMobileOpen={setIsMobileMenuOpen}
        onOpenSettings={() => setIsSettingsOpen(true)}
      />

      {/* Main Content Area (offset by 256px / ml-64 on desktop) */}
      <div className="flex-1 flex flex-col md:ml-64 min-h-screen pb-28">
        
        {/* Topbar */}
        <Topbar
          onToggleMobileMenu={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          onOpenSettings={() => setIsSettingsOpen(true)}
        />

        {/* Dynamic Page Views */}
        <main className="flex-1 px-4 sm:px-8 lg:px-12 py-6 max-w-[1500px] w-full mx-auto">
          {searchQuery.trim() ? (
            <SearchResults />
          ) : (
            <>
              {activeTab === "home" && (
                <>
                  <SoundtrackHero />
                  <MoodScroller />
                  <QuickPicksShelf />
                  <ExploreBoard />
                  <ApkDownloadBanner />
                </>
              )}

              {activeTab === "explore" && (
                <div className="py-2">
                  <ExploreBoard />
                </div>
              )}

              {activeTab === "library" && (
                <LibraryView />
              )}
            </>
          )}
        </main>
      </div>

      {/* Bottom Player Dock */}
      <PlayerDock />

      {/* Up Next Slide-over Queue Drawer */}
      <QueueDrawer />

      {/* Synchronized Lyrics Modal */}
      <LyricsModal />

      {/* Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
      />

    </div>
  );
}

export default function App() {
  return (
    <PlayerProvider>
      <MainShell />
    </PlayerProvider>
  );
}
