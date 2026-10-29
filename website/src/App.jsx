import React, { useState } from "react";
import { PlayerProvider, usePlayer } from "./context/PlayerContext";
import TidalSidebar from "./components/TidalSidebar";
import TidalTopbar from "./components/TidalTopbar";
import TidalHero from "./components/TidalHero";
import TidalTrackList from "./components/TidalTrackList";
import TidalExploreGrid from "./components/TidalExploreGrid";
import TidalVideos from "./components/TidalVideos";
import LibraryView from "./components/LibraryView";
import TidalPlayerBar from "./components/TidalPlayerBar";
import TidalMobileMiniPlayer from "./components/TidalMobileMiniPlayer";
import TidalMobileNav from "./components/TidalMobileNav";
import TidalFullScreenPlayer from "./components/TidalFullScreenPlayer";
import TidalQueueDrawer from "./components/TidalQueueDrawer";
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
    <div className="relative min-h-screen bg-[#000000] text-white flex flex-col font-sans selection:bg-[#00e5ff] selection:text-black">
      
      {/* Background Ambience */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        <div className="absolute -top-32 right-1/4 w-[500px] h-[500px] bg-[#00e5ff]/5 rounded-full blur-[140px]" />
        <div className="absolute top-1/2 left-1/3 w-[500px] h-[500px] bg-blue-900/5 rounded-full blur-[140px]" />
      </div>

      {/* Persistent Audio Engine Container (Song mode = offscreen, Video mode = floating theater PiP) */}
      <div
        className={`transition-all duration-300 ${
          playbackMode === "video"
            ? "fixed top-20 right-6 z-40 w-[320px] sm:w-[440px] aspect-video rounded-2xl overflow-hidden bg-black/95 border border-[#22222a] shadow-2xl backdrop-blur-2xl"
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

      {/* Desktop Left Sidebar (240px) */}
      <TidalSidebar
        isMobileOpen={isMobileMenuOpen}
        setIsMobileOpen={setIsMobileMenuOpen}
        onOpenSettings={() => setIsSettingsOpen(true)}
      />

      {/* Main Content Area (offset by 240px / md:ml-60 on desktop) */}
      <div className="flex-1 flex flex-col md:ml-60 min-h-screen pb-32 md:pb-24">
        
        {/* Topbar */}
        <TidalTopbar
          onToggleMobileMenu={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          onOpenSettings={() => setIsSettingsOpen(true)}
        />

        {/* Dynamic Page Views */}
        <main className="flex-1 px-4 sm:px-8 py-5 max-w-[1500px] w-full mx-auto">
          {searchQuery.trim() ? (
            <SearchResults />
          ) : (
            <>
              {activeTab === "home" && (
                <>
                  <TidalHero />
                  <TidalTrackList />
                  <TidalExploreGrid />
                </>
              )}

              {activeTab === "explore" && (
                <div className="py-2">
                  <TidalExploreGrid />
                </div>
              )}

              {activeTab === "videos" && (
                <TidalVideos />
              )}

              {activeTab === "collection" && (
                <LibraryView />
              )}
            </>
          )}
        </main>
      </div>

      {/* Desktop Player Bar (Fixed across bottom on desktop) */}
      <TidalPlayerBar />

      {/* Mobile Floating Mini Player (Fixed above bottom nav on mobile) */}
      <TidalMobileMiniPlayer />

      {/* Mobile Bottom Navigation Bar (4 tabs on mobile) */}
      <TidalMobileNav />

      {/* Mobile Full-Screen Now Playing View (Tidal style bottom sheet) */}
      <TidalFullScreenPlayer />

      {/* Up Next Play Queue Drawer */}
      <TidalQueueDrawer />

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
