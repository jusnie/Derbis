import React, { useState } from "react";
import {
  Heart,
  Play,
  ListMusic,
  Users,
  ExternalLink,
  Plus,
  RefreshCw,
  LogOut,
  AlertCircle
} from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS } from "../data/catalog";

export default function LibraryView() {
  const {
    playTrack,
    currentTrack,
    userProfile,
    youtubeData,
    isSyncingYouTube,
    syncError,
    connectGoogleYouTube,
    disconnectGoogleYouTube,
    importYouTubePlaylist
  } = usePlayer();

  const [playlistInput, setPlaylistInput] = useState("");
  const [importLoading, setImportLoading] = useState(false);
  const [importStatus, setImportStatus] = useState(null);

  const handleImportPlaylist = async (e) => {
    e.preventDefault();
    if (!playlistInput.trim()) return;
    setImportLoading(true);
    setImportStatus(null);
    try {
      const pl = await importYouTubePlaylist(playlistInput);
      setPlaylistInput("");
      setImportStatus(`Successfully imported "${pl.title}"!`);
      if (pl.tracks && pl.tracks.length > 0) {
        playTrack(pl.tracks[0], pl.tracks);
      }
    } catch (err) {
      setImportStatus(`Error: ${err.message || "Failed to import"}`);
    } finally {
      setImportLoading(false);
    }
  };

  const tracksToDisplay = youtubeData.likedTracks.length > 0
    ? youtubeData.likedTracks
    : QUICK_PICKS_TRACKS;

  return (
    <div className="space-y-8 select-none py-2 max-w-6xl">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <p className="text-[10px] font-black tracking-widest text-white/40 uppercase">
            YOUR COLLECTION
          </p>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight mt-1">
            Library
          </h2>
        </div>

        {userProfile?.connected && (
          <button
            onClick={connectGoogleYouTube}
            disabled={isSyncingYouTube}
            className="inline-flex items-center gap-2 h-9 px-4 rounded-full bg-[#181820] hover:bg-[#22222a] border border-white/10 text-xs font-bold text-white transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isSyncingYouTube ? "animate-spin" : ""}`} />
            <span>{isSyncingYouTube ? "Syncing..." : "Sync YouTube"}</span>
          </button>
        )}
      </div>

      {/* Sync Banner: Connected vs Disconnected */}
      {userProfile?.connected ? (
        <div className="p-5 rounded-2xl bg-[#111116] border border-[#222228] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            {userProfile.avatar ? (
              <img
                src={userProfile.avatar}
                alt={userProfile.name}
                className="w-12 h-12 rounded-full object-cover border-2 border-white/20 shrink-0"
              />
            ) : (
              <div className="w-12 h-12 rounded-full bg-white text-black flex items-center justify-center font-black text-base shrink-0">
                {userProfile.name?.charAt(0) || "U"}
              </div>
            )}
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-white leading-tight">
                  {userProfile.name}
                </h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/10 text-white border border-white/10">
                  YouTube Synced
                </span>
              </div>
              <p className="text-xs text-white/50 mt-0.5">
                {youtubeData.likedTracks.length} liked tracks • {youtubeData.playlists.length} playlists • {youtubeData.subscriptions.length} artists
              </p>
            </div>
          </div>

          <button
            onClick={disconnectGoogleYouTube}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-white/60 hover:text-white hover:bg-white/10 border border-white/10 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Disconnect</span>
          </button>
        </div>
      ) : (
        <div className="p-6 rounded-2xl bg-[#121217] border border-[#22222a] flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-1.5 max-w-xl">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-white animate-pulse" />
              <h3 className="text-base font-black text-white">
                Sync with your YouTube Account
              </h3>
            </div>
            <p className="text-xs text-white/60 leading-relaxed">
              Sign in with your Google account to automatically import your YouTube Music liked songs, custom playlists, and subscribed artists right here in Derbis.
            </p>
            {syncError && (
              <div className="flex items-center gap-1.5 text-xs text-red-400 mt-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{syncError}</span>
              </div>
            )}
          </div>

          <button
            onClick={connectGoogleYouTube}
            disabled={isSyncingYouTube}
            className="h-10 px-5 rounded-full bg-white hover:bg-white/90 active:scale-95 text-black text-xs font-black tracking-tight transition-all shadow-lg shrink-0 flex items-center gap-2"
          >
            {isSyncingYouTube ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Connecting...</span>
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
        </div>
      )}

      {/* Manual Public YouTube Playlist Import */}
      <div className="p-4 rounded-xl bg-[#0e0e12] border border-[#1d1d24]">
        <form onSubmit={handleImportPlaylist} className="flex flex-col sm:flex-row gap-2">
          <input
            type="text"
            value={playlistInput}
            onChange={(e) => setPlaylistInput(e.target.value)}
            placeholder="Paste public YouTube playlist link (e.g. youtube.com/playlist?list=...)"
            className="flex-1 h-9 px-3 rounded-lg bg-[#16161c] border border-white/10 text-xs text-white placeholder-white/40 outline-none focus:border-white/30"
          />
          <button
            type="submit"
            disabled={importLoading}
            className="h-9 px-4 rounded-lg bg-white/10 hover:bg-white/20 text-xs font-bold text-white transition-colors flex items-center justify-center gap-1.5 shrink-0"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>{importLoading ? "Importing..." : "Import Playlist"}</span>
          </button>
        </form>
        {importStatus && (
          <p className="text-[11px] text-white/70 mt-2">{importStatus}</p>
        )}
      </div>

      {/* Section 1: Liked Songs (from YouTube) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Heart className="w-4 h-4 text-white fill-current" />
            <h3 className="text-base sm:text-lg font-bold text-white">
              {youtubeData.likedTracks.length > 0 ? "YouTube Liked Songs" : "Favorite Tracks"}
            </h3>
            <span className="text-xs font-mono text-white/40">
              ({tracksToDisplay.length})
            </span>
          </div>

          <button
            onClick={() => tracksToDisplay.length > 0 && playTrack(tracksToDisplay[0], tracksToDisplay)}
            className="text-xs font-bold text-white/50 hover:text-white transition-colors flex items-center gap-1"
          >
            <Play className="w-3 h-3 fill-current" />
            <span>Play All</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {tracksToDisplay.map((track, idx) => {
            const isCurrent = (currentTrack?.videoId || currentTrack?.id) === (track.videoId || track.id);

            return (
              <div
                key={track.id || idx}
                onClick={() => playTrack(track, tracksToDisplay)}
                className={`group flex items-center gap-3 p-2.5 rounded-xl border cursor-pointer transition-all ${
                  isCurrent
                    ? "bg-[#181820] border-white/30"
                    : "bg-[#101014] hover:bg-[#16161c] border-[#1e1e24] hover:border-white/20"
                }`}
              >
                <div className="relative w-11 h-11 rounded-lg overflow-hidden bg-[#181820] shrink-0">
                  <img
                    src={track.thumbnail}
                    alt={track.title}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                    <Play className="w-4 h-4 fill-white text-white ml-0.5" />
                  </div>
                </div>

                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold text-white truncate leading-tight group-hover:underline">
                    {track.title}
                  </p>
                  <p className="text-[11px] text-white/50 truncate mt-0.5">
                    {track.artist}
                  </p>
                </div>

                <span className="text-[10px] font-mono text-white/40 shrink-0">
                  {track.duration || "3:30"}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Section 2: YouTube Playlists */}
      {youtubeData.playlists.length > 0 && (
        <div className="space-y-3 pt-4">
          <div className="flex items-center gap-2">
            <ListMusic className="w-4 h-4 text-white" />
            <h3 className="text-base sm:text-lg font-bold text-white">
              Your Playlists
            </h3>
            <span className="text-xs font-mono text-white/40">
              ({youtubeData.playlists.length})
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {youtubeData.playlists.map((pl) => (
              <div
                key={pl.id}
                onClick={() => {
                  if (pl.tracks && pl.tracks.length > 0) {
                    playTrack(pl.tracks[0], pl.tracks);
                  }
                }}
                className="group cursor-pointer flex flex-col"
              >
                <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#121216] border border-[#222228] shadow group-hover:border-white/30 transition-all">
                  {pl.thumbnail ? (
                    <img
                      src={pl.thumbnail}
                      alt={pl.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center bg-[#181820] text-white/40">
                      <ListMusic className="w-8 h-8" />
                    </div>
                  )}
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                    <div className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center shadow">
                      <Play className="w-4 h-4 fill-current ml-0.5" />
                    </div>
                  </div>
                </div>

                <div className="mt-2 px-0.5">
                  <h4 className="text-xs font-bold text-white truncate group-hover:underline">
                    {pl.title}
                  </h4>
                  <p className="text-[11px] text-white/50 truncate mt-0.5">
                    {pl.itemCount} tracks
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Section 3: Subscriptions / Artists */}
      {youtubeData.subscriptions.length > 0 && (
        <div className="space-y-3 pt-4">
          <div className="flex items-center gap-2">
            <Users className="w-4 h-4 text-white" />
            <h3 className="text-base sm:text-lg font-bold text-white">
              Subscribed Artists
            </h3>
            <span className="text-xs font-mono text-white/40">
              ({youtubeData.subscriptions.length})
            </span>
          </div>

          <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-thin">
            {youtubeData.subscriptions.map((sub) => (
              <div
                key={sub.id}
                className="flex flex-col items-center text-center shrink-0 w-20 group cursor-pointer"
              >
                <div className="w-16 h-16 rounded-full overflow-hidden bg-[#181820] border border-white/10 group-hover:border-white/40 transition-colors shadow">
                  <img
                    src={sub.avatar}
                    alt={sub.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <span className="text-[11px] font-bold text-white truncate w-full mt-1.5 group-hover:underline">
                  {sub.name}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
}
