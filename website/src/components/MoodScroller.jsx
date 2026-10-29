import React, { useState } from "react";
import { usePlayer } from "../context/PlayerContext";
import { QUICK_PICKS_TRACKS, GENRE_SEEDS } from "../data/catalog";

const MOODS = [
  { id: "for_you", label: "For you" },
  { id: "new_releases", label: "New releases" },
  { id: "local_wave", label: "Local Wave" },
  { id: "rap_drill", label: "Rap & Drill" },
  { id: "electronic", label: "Electronic" },
  { id: "chill", label: "Chill" },
  { id: "pop", label: "Pop" },
  { id: "opm", label: "OPM" }
];

export default function MoodScroller() {
  const [selectedMood, setSelectedMood] = useState("for_you");
  const { playTrack } = usePlayer();

  const handleMoodClick = (mood) => {
    setSelectedMood(mood.id);
    if (mood.id === "opm" || mood.id === "local_wave") {
      playTrack(GENRE_SEEDS.opm[0], GENRE_SEEDS.opm);
    } else if (mood.id === "rap_drill") {
      playTrack(GENRE_SEEDS.hiphop[0], GENRE_SEEDS.hiphop);
    } else if (mood.id === "electronic") {
      playTrack(QUICK_PICKS_TRACKS[10], QUICK_PICKS_TRACKS);
    } else if (mood.id === "pop") {
      playTrack(GENRE_SEEDS.pop[0], GENRE_SEEDS.pop);
    }
  };

  return (
    <div className="flex items-center gap-2 overflow-x-auto py-2 no-scrollbar my-4">
      {MOODS.map((mood) => {
        const isSelected = selectedMood === mood.id;
        return (
          <button
            key={mood.id}
            onClick={() => handleMoodClick(mood)}
            className={`h-10 px-4 rounded-full text-xs font-bold whitespace-nowrap transition-all border ${
              isSelected
                ? "bg-blue-500/20 text-[#dceaff] border-blue-400/40 shadow-sm shadow-blue-500/20"
                : "bg-white/[0.04] text-white/60 hover:text-white hover:bg-white/[0.08] border-white/10"
            }`}
          >
            {mood.label}
          </button>
        );
      })}
    </div>
  );
}
