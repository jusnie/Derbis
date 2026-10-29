import React from "react";
import { Sparkles, Play, Music, Radio } from "lucide-react";
import { EXPLORE_CATEGORIES, GENRE_SEEDS, QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function ExploreGrid() {
  const { playTrack, selectedGenre, setSelectedGenre } = usePlayer();

  const handleSelectGenre = (category) => {
    setSelectedGenre(category);
    // Find track seeds for this genre
    const tracks = GENRE_SEEDS[category.id] || QUICK_PICKS_TRACKS.filter(t => t.genre === category.id);
    if (tracks && tracks.length > 0) {
      playTrack(tracks[0], tracks);
    }
  };

  return (
    <section id="explore-section" className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-10">
      {/* Section Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold tracking-wider text-accent uppercase">
              16 DISCOVERY GENRES
            </span>
            <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-accent/20 text-accent border border-accent/30">
              DerbisApp Native Design
            </span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mt-1">
            Explore Soundscapes
          </h2>
          <p className="text-sm text-white/50 max-w-xl mt-1">
            Browse through curated atmospheres featuring real artists, signature visuals, and immediate continuous radio playback.
          </p>
        </div>

        {/* Quick Filter chips */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
          {["All", "Pop", "OPM", "Rock", "Hip-Hop", "R&B", "Chill"].map((genre) => (
            <button
              key={genre}
              onClick={() => {
                if (genre === "All") {
                  setSelectedGenre(null);
                } else {
                  const found = EXPLORE_CATEGORIES.find(c => c.label.toLowerCase().includes(genre.toLowerCase()));
                  if (found) handleSelectGenre(found);
                }
              }}
              className="px-3 py-1 rounded-xl text-xs font-medium bg-white/5 hover:bg-white/10 text-white/70 hover:text-white border border-white/5 transition-all whitespace-nowrap"
            >
              {genre}
            </button>
          ))}
        </div>
      </div>

      {/* 16 Category Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {EXPLORE_CATEGORIES.map((category) => {
          const isSelected = selectedGenre?.id === category.id;

          return (
            <div
              key={category.id}
              onClick={() => handleSelectGenre(category)}
              className={`group relative overflow-hidden rounded-3xl p-5 cursor-pointer transition-all duration-300 border hover:-translate-y-1.5 shadow-xl ${
                isSelected
                  ? "ring-2 ring-accent border-accent/60 shadow-accent/25"
                  : "bg-gradient-to-br " + category.gradient + " border-white/10 hover:border-white/25 hover:shadow-2xl"
              }`}
            >
              {/* Radial glow background */}
              <div className="absolute top-0 right-0 w-36 h-36 bg-white/10 rounded-full blur-2xl pointer-events-none group-hover:scale-150 transition-transform duration-500" />

              {/* Header: Kicker and Play button */}
              <div className="relative z-10 flex items-start justify-between">
                <span className="text-[10px] font-black tracking-widest text-white/80 uppercase">
                  {category.kicker}
                </span>
                <div className="w-8 h-8 rounded-full bg-black/40 backdrop-blur-md border border-white/20 flex items-center justify-center group-hover:bg-accent group-hover:border-accent transition-all duration-300 shadow-md">
                  <Play className="w-4 h-4 text-white fill-white ml-0.5 group-hover:scale-110 transition-transform" />
                </div>
              </div>

              {/* Title & Description */}
              <div className="relative z-10 mt-3 mb-6">
                <h3 className="text-2xl font-black text-white tracking-tight">
                  {category.label}
                </h3>
                <p className="text-xs text-white/75 line-clamp-2 mt-1 leading-relaxed">
                  {category.description}
                </p>
              </div>

              {/* Visual Showcase: Hero Album Art + 3 Real Artist Avatars */}
              <div className="relative z-10 flex items-end justify-between mt-auto pt-4 border-t border-white/10">
                {/* 3 Real Artist Avatar Overlays */}
                <div className="flex -space-x-2.5 overflow-hidden">
                  {category.artists.map((artist, aIdx) => (
                    <div
                      key={aIdx}
                      className="inline-block relative w-9 h-9 rounded-full ring-2 ring-black/40 overflow-hidden bg-black/60 shadow-lg group-hover:scale-110 transition-transform"
                      style={{ transitionDelay: `${aIdx * 50}ms` }}
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

                {/* Hero Album Art with 3D isometric tilt */}
                <div className="relative w-16 h-16 rounded-xl overflow-hidden shadow-2xl border border-white/20 transform rotate-6 group-hover:rotate-0 group-hover:scale-110 transition-all duration-300">
                  <img
                    src={category.heroArt}
                    alt={category.label}
                    loading="lazy"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                </div>
              </div>

              {/* Artist Names Caption */}
              <div className="relative z-10 mt-3 flex items-center gap-1 text-[11px] font-medium text-white/80 truncate">
                <Radio className="w-3 h-3 text-white/60 shrink-0" />
                <span className="truncate">
                  {category.artists.map(a => a.name).join(" • ")}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
