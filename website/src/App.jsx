import React from "react";
import { PlayerProvider, usePlayer } from "./context/PlayerContext";
import Header from "./components/Header";
import HeroSection from "./components/HeroSection";
import QuickPicksCarousel from "./components/QuickPicksCarousel";
import ExploreGrid from "./components/ExploreGrid";
import ApkDownloadBanner from "./components/ApkDownloadBanner";
import PlayerDock from "./components/PlayerDock";
import QueueDrawer from "./components/QueueDrawer";
import LyricsModal from "./components/LyricsModal";
import SearchResults from "./components/SearchResults";
import { X, Video, Music2 } from "lucide-react";

function MainContent() {
  const {
    activeTab,
    searchQuery,
    playbackMode,
    setPlaybackMode,
    currentTrack
  } = usePlayer();

  return (
    <div className="relative min-h-screen bg-[#07070a] text-white flex flex-col selection:bg-accent selection:text-white">
      {/* Dynamic Background Lighting Blobs */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        <div className="absolute -top-40 -left-40 w-[600px] h-[600px] bg-accent/15 rounded-full blur-[140px]" />
        <div className="absolute top-1/3 -right-40 w-[500px] h-[500px] bg-purple-700/10 rounded-full blur-[140px]" />
        <div className="absolute bottom-10 left-1/3 w-[600px] h-[600px] bg-blue-700/10 rounded-full blur-[140px]" />
      </div>

      {/* Persistent Audio Engine Container (Song mode = hidden offscreen; Video mode = theater PiP window) */}
      <div
        className={`transition-all duration-300 ${
          playbackMode === "video"
            ? "fixed top-20 right-4 sm:right-8 z-40 w-[320px] sm:w-[440px] aspect-video rounded-3xl overflow-hidden bg-black/90 border border-white/20 shadow-2xl backdrop-blur-3xl animate-scaleIn"
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
              title="Switch to Song Mode (Artwork Only)"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
        <div id="yt-audio-engine" className="w-full h-full" />
      </div>

      {/* Top Floating Glass Navigation */}
      <Header />

      {/* Main Viewport */}
      <main className="flex-1 pb-36">
        {searchQuery.trim() ? (
          <SearchResults />
        ) : (
          <>
            {activeTab === "home" && (
              <>
                <HeroSection />
                <QuickPicksCarousel />
                <ExploreGrid />
                <ApkDownloadBanner />
              </>
            )}

            {activeTab === "explore" && (
              <div className="pt-4">
                <ExploreGrid />
              </div>
            )}
          </>
        )}
      </main>

      {/* Bottom Floating Glass Dock */}
      <PlayerDock />

      {/* Up Next Slide-over Queue Drawer */}
      <QueueDrawer />

      {/* Synchronized Lyrics Modal */}
      <LyricsModal />
    </div>
  );
}

export default function App() {
  return (
    <PlayerProvider>
      <MainContent />
    </PlayerProvider>
  );
}
