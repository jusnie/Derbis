import React from "react";
import { Play } from "lucide-react";
import { EXPLORE_CATEGORIES, GENRE_SEEDS, QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function TidalExploreGrid() {
  const { playTrack, selectedGenre, setSelectedGenre } = usePlayer();

  const handleSelectGenre = (category) => {
    setSelectedGenre(category);
    const tracks = GENRE_SEEDS[category.id] || QUICK_PICKS_TRACKS.filter(t => t.genre === category.id);
    if (tracks && tracks.length > 0) {
      playTrack(tracks[0], tracks);
    }
  };

  return (
    <section className="mt-10 select-none">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
            Genres & Moods
          </h2>
          <p className="text-xs text-white/50 mt-0.5">
            Curated Hi-Fi soundscapes
          </p>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
        {EXPLORE_CATEGORIES.map((category) => {
          const isSelected = selectedGenre?.id === category.id;

          return (
            <div
              key={category.id}
              onClick={() => handleSelectGenre(category)}
              className={`group relative overflow-hidden rounded-xl p-3.5 sm:p-4 cursor-pointer transition-all duration-200 border aspect-[4/3] flex flex-col justify-between ${
                isSelected
                  ? "ring-2 ring-[#00e5ff] border-[#00e5ff]/50"
                  : "bg-gradient-to-br " + category.gradient + " border-white/10 hover:border-white/20 hover:scale-[1.02]"
              }`}
            >
              <div className="flex items-start justify-between">
                <span className="text-[9px] font-black tracking-widest text-white/80 uppercase">
                  {category.kicker}
                </span>
                <div className="w-6 h-6 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <Play className="w-3 h-3 text-white fill-white ml-0.5" />
                </div>
              </div>

              <div>
                <h3 className="text-lg sm:text-xl font-black text-white tracking-tight">
                  {category.label}
                </h3>
                <p className="text-[10px] text-white/70 line-clamp-1 mt-0.5">
                  {category.description}
                </p>
              </div>

              {/* Real artist avatars */}
              <div className="flex items-center justify-between pt-2 border-t border-white/10">
                <div className="flex -space-x-1.5 overflow-hidden">
                  {category.artists.map((artist, idx) => (
                    <img
                      key={idx}
                      src={artist.avatar}
                      alt={artist.name}
                      className="w-5 h-5 rounded-full border border-black/50 object-cover"
                    />
                  ))}
                </div>
                <span className="text-[9px] font-bold text-white/60 truncate max-w-[100px]">
                  {category.artists[0]?.name}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
