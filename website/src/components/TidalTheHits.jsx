import React from "react";
import { Play } from "lucide-react";
import { TIDAL_THE_HITS, QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function TidalTheHits() {
  const { playTrack } = usePlayer();

  const handleCardClick = (hit) => {
    const track = QUICK_PICKS_TRACKS.find(t => t.id === hit.seedTrackId) || QUICK_PICKS_TRACKS[0];
    playTrack(track, QUICK_PICKS_TRACKS);
  };

  return (
    <section className="mt-6 select-none">
      {/* Section Header */}
      <div className="flex items-center justify-between mb-3 px-0.5">
        <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
          The Hits
        </h2>
        <button
          onClick={() => {}}
          className="text-xs font-bold text-white/50 hover:text-white transition-colors"
        >
          View all
        </button>
      </div>

      {/* 2-Column on Mobile, 4-Column on Desktop (Matches Screenshot) */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
        {TIDAL_THE_HITS.map((hit) => (
          <div
            key={hit.id}
            onClick={() => handleCardClick(hit)}
            className="group cursor-pointer flex flex-col"
          >
            {/* Square Card with Artist Photo Background */}
            <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#121216] border border-[#222228] shadow-lg group-hover:border-white/30 transition-all">
              
              {/* Artist Background Photo */}
              <img
                src={hit.bgArtistPhoto}
                alt={hit.title}
                loading="lazy"
                className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
              />

              {/* Dark Vignette Overlay for Crisp Typography */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/35 to-black/50" />

              {/* 4-Diamond Logo on Top Right (Exact Tidal mark from screenshot) */}
              <div className="absolute top-2.5 right-2.5 z-10 grid grid-cols-2 gap-0.5 w-3.5 h-3.5 rotate-45 transform">
                <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
                <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
                <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
                <div className="w-1.5 h-1.5 bg-white rounded-[0.5px]" />
              </div>

              {/* Editorial Title Banner (Top or Vertical Right like Screenshot) */}
              {hit.tagOrientation === "top" ? (
                <div className="absolute top-2.5 left-2.5 z-10 max-w-[85%]">
                  <span className="block text-sm sm:text-base font-black tracking-tighter text-white uppercase leading-none drop-shadow-md">
                    {hit.tagText}
                  </span>
                </div>
              ) : (
                <div className="absolute top-2.5 left-2.5 z-10">
                  <span className="text-xs sm:text-sm font-black tracking-tight text-white uppercase drop-shadow-md">
                    {hit.tagText}
                  </span>
                </div>
              )}

              {/* The 3 Circular Artist Avatars on the Card (Explicit User Requirement) */}
              <div className="absolute bottom-2.5 left-2.5 z-10 flex items-center justify-between w-[calc(100%-20px)]">
                <div className="flex -space-x-2 overflow-hidden">
                  {hit.artists.map((artist, aIdx) => (
                    <div
                      key={aIdx}
                      className="w-6 h-6 sm:w-7 sm:h-7 rounded-full border-2 border-black overflow-hidden bg-black/60 shadow-md"
                      title={artist.name}
                    >
                      <img
                        src={artist.avatar}
                        alt={artist.name}
                        loading="lazy"
                        className="w-full h-full object-cover"
                      />
                    </div>
                  ))}
                </div>

                {/* Hover Play Button */}
                <div className="w-7 h-7 rounded-full bg-white text-black flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-lg">
                  <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                </div>
              </div>

            </div>

            {/* Title & Subtitle Below the Card (Exact match to Screenshot) */}
            <div className="mt-2 px-0.5">
              <h3 className="text-xs sm:text-sm font-bold text-white truncate group-hover:text-white">
                {hit.title}
              </h3>
              <p className="text-[11px] text-white/50 font-medium truncate mt-0.5">
                {hit.subtitle}
              </p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
