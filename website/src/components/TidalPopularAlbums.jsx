import React from "react";
import { Play } from "lucide-react";
import { POPULAR_ALBUMS, QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function TidalPopularAlbums() {
  const { playTrack } = usePlayer();

  const handleAlbumClick = (album) => {
    const track = QUICK_PICKS_TRACKS.find(t => t.id === album.seedTrackId) || QUICK_PICKS_TRACKS[0];
    playTrack(track, QUICK_PICKS_TRACKS);
  };

  return (
    <section className="mt-8 select-none">
      {/* Section Header */}
      <div className="flex items-center justify-between mb-3 px-0.5">
        <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
          Popular albums
        </h2>
        <button
          onClick={() => {}}
          className="text-xs font-bold text-white/50 hover:text-white transition-colors"
        >
          View all
        </button>
      </div>

      {/* Grid: 2 on mobile, 6 on desktop */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
        {POPULAR_ALBUMS.map((album) => (
          <div
            key={album.id}
            onClick={() => handleAlbumClick(album)}
            className="group cursor-pointer flex flex-col"
          >
            {/* Square Album Cover */}
            <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#121216] border border-[#222228] shadow-md group-hover:border-white/30 transition-all">
              <img
                src={album.cover}
                alt={album.title}
                loading="lazy"
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />

              {/* Hover Play Button */}
              <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                <div className="w-10 h-10 rounded-full bg-white text-black flex items-center justify-center shadow-lg">
                  <Play className="w-4 h-4 fill-current ml-0.5" />
                </div>
              </div>
            </div>

            {/* Title & Artist */}
            <div className="mt-2 px-0.5">
              <h3 className="text-xs sm:text-sm font-bold text-white truncate group-hover:text-white">
                {album.title}
              </h3>
              <p className="text-[11px] text-white/50 font-medium truncate mt-0.5">
                {album.artist}
              </p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
