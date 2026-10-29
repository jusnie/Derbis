import React from "react";
import { Play } from "lucide-react";
import { EXPLORE_CATEGORIES, GENRE_SEEDS, QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function ExploreBoard() {
  const { playTrack, selectedGenre, setSelectedGenre } = usePlayer();

  const handleSelectGenre = (category) => {
    setSelectedGenre(category);
    const tracks = GENRE_SEEDS[category.id] || QUICK_PICKS_TRACKS.filter(t => t.genre === category.id);
    if (tracks && tracks.length > 0) {
      playTrack(tracks[0], tracks);
    }
  };

  return (
    <section className="mt-12 select-none">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6">
        <div>
          <p className="text-[10px] font-black tracking-widest text-white/40 uppercase">
            DISCOVERY
          </p>
          <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight mt-0.5">
            Explore Soundscapes
          </h2>
          <p className="text-xs text-white/50 mt-1 max-w-lg">
            16 curated atmospheres featuring real artists, signature visuals, and immediate continuous radio playback.
          </p>
        </div>
      </div>

      {/* 16 Category Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {EXPLORE_CATEGORIES.map((category) => {
          const isSelected = selectedGenre?.id === category.id;

          return (
            <div
              key={category.id}
              onClick={() => handleSelectGenre(category)}
              className={`group relative overflow-hidden rounded-2xl p-4 cursor-pointer transition-all duration-300 border hover:-translate-y-1 shadow-lg ${
                isSelected
                  ? "ring-2 ring-blue-400 border-blue-400/50 shadow-blue-500/20"
                  : "bg-gradient-to-br " + category.gradient + " border-white/10 hover:border-white/20"
              }`}
            >
              {/* Radial glow background */}
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl pointer-events-none group-hover:scale-125 transition-transform" />

              {/* Header: Kicker and Play button */}
              <div className="relative z-10 flex items-start justify-between">
                <span className="text-[9px] font-black tracking-widest text-white/80 uppercase">
                  {category.kicker}
                </span>
                <div className="w-7 h-7 rounded-full bg-black/40 backdrop-blur-md border border-white/20 flex items-center justify-center group-hover:bg-blue-500 group-hover:border-blue-500 transition-all shadow-md">
                  <Play className="w-3.5 h-3.5 text-white fill-white ml-0.5 group-hover:scale-110 transition-transform" />
                </div>
              </div>

              {/* Title & Description */}
              <div className="relative z-10 mt-2 mb-4">
                <h3 className="text-xl font-black text-white tracking-tight">
                  {category.label}
                </h3>
                <p className="text-[11px] text-white/70 line-clamp-2 mt-0.5 leading-relaxed">
                  {category.description}
                </p>
              </div>

              {/* Visual Showcase: Hero Album Art + 3 Real Artist Avatars */}
              <div className="relative z-10 flex items-end justify-between mt-auto pt-3 border-t border-white/10">
                {/* 3 Real Artist Avatar Overlays */}
                <div className="flex -space-x-2 overflow-hidden">
                  {category.artists.map((artist, aIdx) => (
                    <div
                      key={aIdx}
                      className="inline-block relative w-8 h-8 rounded-full ring-2 ring-black/40 overflow-hidden bg-black/60 shadow"
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

                {/* Hero Album Art with 3D tilt */}
                <div className="relative w-14 h-14 rounded-xl overflow-hidden shadow-xl border border-white/20 transform rotate-6 group-hover:rotate-0 transition-transform duration-300">
                  <img
                    src={category.heroArt}
                    alt={category.label}
                    loading="lazy"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
                </div>
              </div>

              {/* Artist Names Caption */}
              <div className="relative z-10 mt-2.5 text-[10px] font-semibold text-white/70 truncate">
                {category.artists.map(a => a.name).join(" • ")}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
