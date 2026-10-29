import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from "react";
import { QUICK_PICKS_TRACKS, EXPLORE_CATEGORIES, GENRE_SEEDS } from "../data/catalog";

const PlayerContext = createContext(null);

export function PlayerProvider({ children }) {
  const [currentTrack, setCurrentTrack] = useState(QUICK_PICKS_TRACKS[0]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(236); // default 3:56
  const [volume, setVolume] = useState(80);
  const [isMuted, setIsMuted] = useState(false);
  
  // Audio quality badge ('MAX', 'HI-FI', 'HIGH')
  const [audioQuality, setAudioQuality] = useState("MAX");
  
  // 'song' (artwork & 3D visualizer) vs 'video' (YouTube embedded visual)
  const [playbackMode, setPlaybackMode] = useState("song");
  
  // Continuous radio / Endless listening
  const [endlessListening, setEndlessListening] = useState(true);
  
  // Up Next Queue
  const [queue, setQueue] = useState(QUICK_PICKS_TRACKS.slice(1, 12));
  const [history, setHistory] = useState([]);
  
  // UI states
  const [isQueueOpen, setIsQueueOpen] = useState(false);
  const [isLyricsOpen, setIsLyricsOpen] = useState(false);
  const [isMobilePlayerExpanded, setIsMobilePlayerExpanded] = useState(false);
  const [isShuffle, setIsShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState("off"); // 'off' | 'all' | 'one'
  const [visualizerMode, setVisualizerMode] = useState("aura"); // 'aura' | 'vinyl' | 'waves' | 'off'
  
  // Navigation / Search state
  const [activeTab, setActiveTab] = useState("home"); // 'home' | 'explore' | 'videos' | 'collection'
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedGenre, setSelectedGenre] = useState(null);

  // Audio frequency simulation for Three.js visualizer
  const [audioFrequencies, setAudioFrequencies] = useState(() => new Float32Array(64));

  // YouTube player references
  const ytPlayerRef = useRef(null);
  const isApiReadyRef = useRef(false);
  const timerRef = useRef(null);
  const animFrameRef = useRef(null);

  // Load YouTube IFrame API
  useEffect(() => {
    if (window.YT && window.YT.Player) {
      isApiReadyRef.current = true;
      initPlayer();
      return;
    }

    const tag = document.createElement("script");
    tag.src = "https://www.youtube.com/iframe_api";
    const firstScriptTag = document.getElementsByTagName("script")[0];
    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

    window.onYouTubeIframeAPIReady = () => {
      isApiReadyRef.current = true;
      initPlayer();
    };

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, []);

  const initPlayer = useCallback(() => {
    if (!window.YT || !window.YT.Player || ytPlayerRef.current) return;

    ytPlayerRef.current = new window.YT.Player("yt-audio-engine", {
      height: "100%",
      width: "100%",
      videoId: currentTrack?.videoId || "g5qU7p7yOY8",
      playerVars: {
        autoplay: 0,
        controls: 0,
        disablekb: 1,
        fs: 0,
        modestbranding: 1,
        rel: 0,
        playsinline: 1,
        origin: window.location.origin
      },
      events: {
        onReady: (e) => {
          e.target.setVolume(volume);
        },
        onStateChange: (e) => {
          if (e.data === window.YT.PlayerState.PLAYING) {
            setIsPlaying(true);
            const d = e.target.getDuration();
            if (d && !isNaN(d)) setDuration(d);
          } else if (e.data === window.YT.PlayerState.PAUSED) {
            setIsPlaying(false);
          } else if (e.data === window.YT.PlayerState.ENDED) {
            handleTrackEnded();
          }
        },
        onError: () => {
          handleTrackEnded();
        }
      }
    });
  }, [currentTrack, volume]);

  // Track progress ticker & audio frequencies
  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        if (ytPlayerRef.current && ytPlayerRef.current.getCurrentTime) {
          try {
            const time = ytPlayerRef.current.getCurrentTime();
            setCurrentTime(time);
            const d = ytPlayerRef.current.getDuration();
            if (d && !isNaN(d) && d > 0) setDuration(d);
          } catch (e) {}
        }
      }, 400);

      let phase = 0;
      const updateFrequencies = () => {
        phase += 0.08;
        const freqs = new Float32Array(64);
        for (let i = 0; i < 64; i++) {
          const bassBoost = i < 8 ? 1.6 : 1.0;
          const wave = Math.sin(phase * 1.5 + i * 0.25) * 0.4 + 0.5;
          const flutter = Math.sin(phase * 3.7 + i * 0.8) * 0.15;
          freqs[i] = Math.min(1.0, Math.max(0.1, (wave + flutter) * bassBoost));
        }
        setAudioFrequencies(freqs);
        animFrameRef.current = requestAnimationFrame(updateFrequencies);
      };
      animFrameRef.current = requestAnimationFrame(updateFrequencies);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
      setAudioFrequencies(new Float32Array(64).fill(0.15));
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [isPlaying]);

  // Volume
  useEffect(() => {
    if (ytPlayerRef.current && ytPlayerRef.current.setVolume) {
      try {
        if (isMuted) {
          ytPlayerRef.current.mute();
        } else {
          ytPlayerRef.current.unMute();
          ytPlayerRef.current.setVolume(volume);
        }
      } catch (e) {}
    }
  }, [volume, isMuted]);

  // Play track
  const playTrack = useCallback((track, newQueue = null) => {
    if (!track) return;
    if (currentTrack?.videoId) {
      setHistory(prev => [currentTrack, ...prev.slice(0, 19)]);
    }
    setCurrentTrack(track);
    setCurrentTime(0);
    setIsPlaying(true);

    if (newQueue && Array.isArray(newQueue)) {
      setQueue(newQueue.filter(t => t.id !== track.id));
    }

    if (ytPlayerRef.current && ytPlayerRef.current.loadVideoById) {
      try {
        ytPlayerRef.current.loadVideoById({
          videoId: track.videoId || track.id,
          startSeconds: 0
        });
      } catch (e) {
        console.error("Failed to load track:", e);
      }
    }
  }, [currentTrack]);

  // Toggle play/pause
  const togglePlayPause = useCallback(() => {
    if (!ytPlayerRef.current) return;
    try {
      if (isPlaying) {
        ytPlayerRef.current.pauseVideo();
        setIsPlaying(false);
      } else {
        ytPlayerRef.current.playVideo();
        setIsPlaying(true);
      }
    } catch (e) {
      setIsPlaying(!isPlaying);
    }
  }, [isPlaying]);

  // Seek
  const seekTo = useCallback((seconds) => {
    setCurrentTime(seconds);
    if (ytPlayerRef.current && ytPlayerRef.current.seekTo) {
      try {
        ytPlayerRef.current.seekTo(seconds, true);
      } catch (e) {}
    }
  }, []);

  // Track Ended / Continuous Radio
  const handleTrackEnded = useCallback(() => {
    if (repeatMode === "one") {
      seekTo(0);
      if (ytPlayerRef.current && ytPlayerRef.current.playVideo) {
        ytPlayerRef.current.playVideo();
      }
      return;
    }

    if (queue.length > 0) {
      const next = queue[0];
      const remaining = queue.slice(1);
      setQueue(remaining);
      playTrack(next);
      return;
    }

    if (endlessListening) {
      const genre = currentTrack?.genre || "pop";
      const candidateList = GENRE_SEEDS[genre] || QUICK_PICKS_TRACKS;
      const available = candidateList.filter(t => t.id !== currentTrack.id);
      const next = available[Math.floor(Math.random() * available.length)] || QUICK_PICKS_TRACKS[0];
      
      const moreRadioTracks = QUICK_PICKS_TRACKS.filter(t => t.id !== next.id && t.id !== currentTrack.id).slice(0, 5);
      setQueue(moreRadioTracks);
      playTrack(next);
    } else {
      setIsPlaying(false);
    }
  }, [repeatMode, queue, endlessListening, currentTrack, playTrack, seekTo]);

  // Next Track
  const nextTrack = useCallback(() => {
    if (queue.length > 0) {
      const next = queue[0];
      const remaining = queue.slice(1);
      setQueue(remaining);
      playTrack(next);
    } else if (endlessListening) {
      handleTrackEnded();
    }
  }, [queue, endlessListening, playTrack, handleTrackEnded]);

  // Previous Track
  const previousTrack = useCallback(() => {
    if (currentTime > 4) {
      seekTo(0);
      return;
    }
    if (history.length > 0) {
      const prev = history[0];
      setHistory(prevHistory => prevHistory.slice(1));
      setQueue(prevQueue => [currentTrack, ...prevQueue]);
      playTrack(prev);
    } else {
      seekTo(0);
    }
  }, [currentTime, history, currentTrack, playTrack, seekTo]);

  const addToQueue = useCallback((track) => {
    setQueue(prev => [...prev, track]);
  }, []);

  const removeFromQueue = useCallback((index) => {
    setQueue(prev => prev.filter((_, i) => i !== index));
  }, []);

  const reorderQueue = useCallback((fromIdx, toIdx) => {
    setQueue(prev => {
      const updated = [...prev];
      const [moved] = updated.splice(fromIdx, 1);
      updated.splice(toIdx, 0, moved);
      return updated;
    });
  }, []);

  const clearQueue = useCallback(() => {
    setQueue([]);
  }, []);

  const value = {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    volume,
    setVolume,
    isMuted,
    setIsMuted,
    audioQuality,
    setAudioQuality,
    playbackMode,
    setPlaybackMode,
    endlessListening,
    setEndlessListening,
    toggleEndlessListening: () => setEndlessListening(prev => !prev),
    queue,
    history,
    isShuffle,
    setIsShuffle,
    repeatMode,
    setRepeatMode,
    visualizerMode,
    setVisualizerMode,
    isQueueOpen,
    setIsQueueOpen,
    isLyricsOpen,
    setIsLyricsOpen,
    isMobilePlayerExpanded,
    setIsMobilePlayerExpanded,
    activeTab,
    setActiveTab,
    searchQuery,
    setSearchQuery,
    selectedGenre,
    setSelectedGenre,
    audioFrequencies,
    playTrack,
    togglePlayPause,
    seekTo,
    nextTrack,
    previousTrack,
    addToQueue,
    removeFromQueue,
    reorderQueue,
    clearQueue
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
    </PlayerContext.Provider>
  );
}

export function usePlayer() {
  const context = useContext(PlayerContext);
  if (!context) {
    throw new Error("usePlayer must be used within a PlayerProvider");
  }
  return context;
}
