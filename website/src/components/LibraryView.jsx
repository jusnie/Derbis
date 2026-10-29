import React from "react";
import { Heart, Play, Music, Sparkles, Youtube, ListMusic } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS } from "../data/catalog";

export default function LibraryView() {
  const { playTrack, currentTrack, isPlaying } = usePlayer();

  const favoriteTracks = QUICK_PICKS_TRACKS.slice(0, 8);

  return (
    <div className="space-y-8 select-none py-2">
      {/* Header */}
      <div>
        <p className="text-[10px] font-black tracking-widest text-white/40 uppercase">
          YOUR COLLECTION
        </p>
        <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight mt-1">
          Library
        </h2>
      </div>

      {/* Google / YouTube Music Sync Banner */}
      <div className="p-6 rounded-3xl bg-gradient-to-r from-blue-900/30 via-purple-900/20 to-black/40 border border-white/10 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-500 shrink-0">
            <Youtube className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">
              Connect Google & YouTube Music
            </h3>
            <p className="text-xs text-white/50 mt-0.5">
              Sync your playlists, subscriptions, and listening history with Derbis.
            </p>
          </div>
        </div>

        <button
          onClick={() => alert("Google OAuth authorization is ready for authorized accounts.")}
          className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-lg shadow-blue-500/30 transition-all shrink-0"
        >
          Connect Account
        </button>
      </div>

      {/* Favorites shelf */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <Heart className="w-4 h-4 text-pink-500 fill-pink-500" />
          <h3 className="text-lg font-bold text-white">Favorite Tracks</h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {favoriteTracks.map((track) => {
            const isCurrent = currentTrack?.id === track.id;

            return (
              <div
                key={track.id}
                onClick={() => playTrack(track, favoriteTracks)}
                className={`group flex items-center gap-3 p-3 rounded-2xl border cursor-pointer transition-all ${
                  isCurrent
                    ? "bg-blue-500/15 border-blue-400/40"
                    : "bg-white/[0.03] hover:bg-white/[0.08] border-white/5 hover:border-white/15"
                }`}
              >
                <img
                  src={track.thumbnail}
                  alt={track.title}
                  className="w-11 h-11 rounded-xl object-cover shadow shrink-0"
                />
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold text-white truncate group-hover:text-blue-400 transition-colors">
                    {track.title}
                  </p>
                  <p className="text-[11px] text-white/50 truncate">
                    {track.artist}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
