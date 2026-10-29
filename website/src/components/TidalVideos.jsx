import React from "react";
import { Play, Video } from "lucide-react";
import { QUICK_PICKS_TRACKS } from "../data/catalog";
import { usePlayer } from "../context/PlayerContext";

export default function TidalVideos() {
  const { playTrack, setPlaybackMode } = usePlayer();

  const handlePlayVideo = (track) => {
    setPlaybackMode("video");
    playTrack(track);
  };

  return (
    <div className="space-y-6 select-none py-2">
      <div>
        <div className="flex items-center gap-2">
          <span className="px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 text-[10px] font-black uppercase tracking-wider">
            OFFICIAL VIDEOS
          </span>
          <span className="text-xs text-white/40">1080p Full HD</span>
        </div>
        <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight mt-1">
          Music Videos
        </h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {QUICK_PICKS_TRACKS.slice(0, 9).map((track) => (
          <div
            key={track.id}
            onClick={() => handlePlayVideo(track)}
            className="group relative overflow-hidden rounded-xl bg-[#101015] border border-white/10 hover:border-white/25 cursor-pointer transition-all hover:-translate-y-1"
          >
            <div className="relative aspect-video w-full overflow-hidden bg-black/50">
              <img
                src={track.thumbnail}
                alt={track.title}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
              <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                <div className="w-12 h-12 rounded-full bg-[#00e5ff] text-black flex items-center justify-center shadow-lg">
                  <Play className="w-5 h-5 fill-current ml-0.5" />
                </div>
              </div>
              <span className="absolute bottom-2 right-2 px-1.5 py-0.5 rounded bg-black/80 text-[10px] font-mono text-white/80">
                {track.duration}
              </span>
            </div>

            <div className="p-3">
              <h4 className="text-xs sm:text-sm font-bold text-white truncate group-hover:text-[#00e5ff] transition-colors">
                {track.title}
              </h4>
              <p className="text-[11px] text-white/50 truncate mt-0.5">
                {track.artist}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
