import React from "react";
import { ListMusic, Play, Users } from "lucide-react";
import { usePlayer } from "../context/PlayerContext";

export default function HomeYouTubeShelves() {
  const { userProfile, youtubeData, playTrack } = usePlayer();

  if (!userProfile?.connected) return null;

  return (
    <div className="space-y-8 mt-8 select-none">
      
      {/* 1. User's YouTube Playlists right on Home */}
      {youtubeData.playlists && youtubeData.playlists.length > 0 && (
        <section>
          <div className="flex items-center justify-between mb-3 px-0.5">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
                Your YouTube Playlists
              </h2>
              <p className="text-xs text-white/50 mt-0.5">
                Imported directly from your YouTube Music account
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
            {youtubeData.playlists.slice(0, 6).map((pl) => (
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
                      loading="lazy"
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
                  <h4 className="text-xs sm:text-sm font-bold text-white truncate group-hover:underline">
                    {pl.title}
                  </h4>
                  <p className="text-[11px] text-white/50 truncate mt-0.5">
                    {pl.itemCount} tracks
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 2. User's Subscribed Artists right on Home */}
      {youtubeData.subscriptions && youtubeData.subscriptions.length > 0 && (
        <section>
          <div className="flex items-center justify-between mb-3 px-0.5">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
                Your Subscribed Artists
              </h2>
              <p className="text-xs text-white/50 mt-0.5">
                Artists from your YouTube subscriptions
              </p>
            </div>
          </div>

          <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-thin">
            {youtubeData.subscriptions.slice(0, 12).map((sub) => (
              <div
                key={sub.id}
                className="flex flex-col items-center text-center shrink-0 w-20 group cursor-pointer"
              >
                <div className="w-16 h-16 rounded-full overflow-hidden bg-[#181820] border border-white/10 group-hover:border-white/40 transition-colors shadow">
                  <img
                    src={sub.avatar}
                    alt={sub.name}
                    loading="lazy"
                    className="w-full h-full object-cover"
                  />
                </div>
                <span className="text-[11px] font-bold text-white truncate w-full mt-1.5 group-hover:underline">
                  {sub.name}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

    </div>
  );
}
