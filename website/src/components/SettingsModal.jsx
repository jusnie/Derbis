import React from "react";
import { X, Smartphone, ExternalLink, Download, LogOut, RefreshCw, Music2 } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { isIos } from "../utils/platform";
import { APP_VERSION } from "../version";

export default function SettingsModal({ isOpen, onClose, onOpenIosInstaller }) {
  const {
    userProfile,
    youtubeData,
    isSyncingYouTube,
    syncError,
    connectGoogleYouTube,
    disconnectGoogleYouTube
  } = usePlayer();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex items-center justify-center p-4 select-none">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="absolute inset-0 bg-black/80 backdrop-blur-md transition-opacity"
      />

      {/* Dialog */}
      <div className="relative w-full max-w-md rounded-2xl bg-[#0f0f12] border border-[#222228] p-6 shadow-2xl z-10 space-y-5 text-white">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2.5">
            <img src="/assets/derbis-logo.png" alt="Derbis" className="w-6 h-6 object-contain" />
            <h3 className="text-sm font-black tracking-wider uppercase text-white">
              Account & Sync
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-white/50 hover:text-white hover:bg-white/10"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Google / YouTube Account Info */}
        {userProfile?.connected ? (
          <div className="p-4 rounded-xl bg-[#15151c] border border-white/10 space-y-3">
            <div className="flex items-center gap-3">
              {userProfile.avatar ? (
                <img
                  src={userProfile.avatar}
                  alt={userProfile.name}
                  className="w-12 h-12 rounded-full object-cover border border-white/20 shrink-0"
                />
              ) : (
                <div className="w-12 h-12 rounded-full bg-white text-black flex items-center justify-center font-black text-sm shrink-0">
                  {userProfile.name?.charAt(0) || "U"}
                </div>
              )}
              <div className="min-w-0 flex-1">
                <strong className="block text-sm font-bold text-white truncate">
                  {userProfile.name}
                </strong>
                <small className="block text-xs text-white/50 truncate">
                  {userProfile.email}
                </small>
              </div>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs">
              <span className="text-white/60">
                {youtubeData.likedTracks?.length || 0} Liked • {youtubeData.playlists?.length || 0} Playlists
              </span>
              <button
                onClick={disconnectGoogleYouTube}
                className="flex items-center gap-1 text-white/50 hover:text-white transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-[#14141a] border border-[#22222a] space-y-3">
            <div>
              <h4 className="text-xs font-bold text-white">
                Connect YouTube Music
              </h4>
              <p className="text-[11px] text-white/50 mt-0.5 leading-relaxed">
                Sign in with Google to automatically view your YouTube liked tracks and playlists on Derbis.
              </p>
            </div>

            <button
              onClick={connectGoogleYouTube}
              disabled={isSyncingYouTube}
              className="w-full h-10 rounded-full bg-white text-black text-xs font-bold flex items-center justify-center gap-2 hover:bg-white/90 active:scale-98 transition-all shadow"
            >
              {isSyncingYouTube ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Signing In...</span>
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="currentColor"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  <span>Sign in with Google</span>
                </>
              )}
            </button>

            {syncError && (
              <p className="text-[11px] text-red-400 mt-1">{syncError}</p>
            )}
          </div>
        )}

        {/* Native iOS App Info or Android App Promotion */}
        {isIos ? (
          <div className="p-3.5 rounded-xl bg-[#0e0e12] border border-cyan-500/20 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5">
                <Music2 className="w-3.5 h-3.5 text-cyan-400" />
                <strong className="text-xs font-bold text-white">Derbis iOS</strong>
              </div>
              <span className="text-[9px] font-mono text-cyan-400 font-bold">{`v${APP_VERSION}`}</span>
            </div>
            <p className="text-[11px] text-white/60 leading-relaxed">
              iOS web client. Playback quality and system controls depend on the selected source.
            </p>
            {onOpenIosInstaller && (
              <button
                onClick={() => {
                  onClose();
                  onOpenIosInstaller();
                }}
                className="w-full flex items-center justify-between px-3 py-2 rounded-lg bg-cyan-500/15 hover:bg-cyan-500/25 border border-cyan-500/30 text-cyan-300 text-xs font-bold transition-all"
              >
                <span>Launch iOS Web Installer (OTA)</span>
                <Download className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        ) : (
          <div className="p-3.5 rounded-xl bg-[#0e0e12] border border-[#1e1e24] space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5">
                <Smartphone className="w-3.5 h-3.5 text-white/70" />
                <strong className="text-xs font-bold text-white">Derbis Android App</strong>
              </div>
              <span className="text-[9px] font-mono text-white/40">{`v${APP_VERSION}`}</span>
            </div>
            <p className="text-[11px] text-white/60 leading-relaxed">
              Download the native APK for background playback with screen locked and offline downloads.
            </p>
            <a
              href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
              download
              className="flex items-center justify-between px-3 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs font-bold transition-all"
            >
              <span>Download APK (~71 MB)</span>
              <Download className="w-3.5 h-3.5" />
            </a>
          </div>
        )}

        {/* Footer */}
        <div className="flex items-center justify-between pt-2 text-[11px] text-white/40 border-t border-white/10">
          <span>Derbis Hi-Fi Web</span>
          <a
            href="https://github.com/jusnie/Derbis"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1 hover:text-white"
          >
            <span>GitHub</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </div>
    </div>
  );
}
