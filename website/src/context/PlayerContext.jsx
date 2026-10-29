import React, { createContext, useContext, useState, useEffect, useRef, useCallback, useMemo } from "react";
import { CATEGORIES_WITH_ARTIST_BACKGROUND, DERBIS_THE_HITS } from "../data/catalog";
import {
  fetchYouTubeUserData,
  fetchPlaylistTracks,
  extractPlaylistId,
  searchYouTubeLive,
  fetchRadioTracks
} from "../services/youtubeAuth";

const PlayerContext = createContext(null);

export function PlayerProvider({ children }) {
  const [currentTrack, setCurrentTrack] = useState({
    id: "gNi_6U5Pm_o",
    videoId: "gNi_6U5Pm_o",
    title: "Love Me Harder",
    artist: "Ariana Grande, The Weeknd",
    thumbnail: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg",
    duration: "3:56"
  });
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(236);
  const [volume, setVolume] = useState(80);
  const [isMuted, setIsMuted] = useState(false);
  
  // Audio quality badge
  const [audioQuality, setAudioQuality] = useState("MAX");
  
  // 'song' vs 'video' mode
  const [playbackMode, setPlaybackMode] = useState("song");
  
  // Continuous radio (Endless listening)
  const [endlessListening, setEndlessListening] = useState(true);
  
  // Up Next Queue & Radio Recommendations
  const [queue, setQueue] = useState([]);
  const [history, setHistory] = useState([]);
  const [radioRecommendations, setRadioRecommendations] = useState([]);
  const [isRadioLoading, setIsRadioLoading] = useState(false);
  const [radioSeedTrack, setRadioSeedTrack] = useState(null);
  
  // UI states
  const [isQueueOpen, setIsQueueOpen] = useState(false);
  const [isLyricsOpen, setIsLyricsOpen] = useState(false);
  const [isMobilePlayerExpanded, setIsMobilePlayerExpanded] = useState(false);
  const [isShuffle, setIsShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState("off");
  const [visualizerMode, setVisualizerMode] = useState("off");
  
  // Navigation / Search state
  const [activeTab, setActiveTab] = useState("home"); // 'home' | 'search' | 'explore' | 'videos' | 'collection'
  const [activeSubTab, setActiveSubTab] = useState("for_you"); // 'for_you' | 'staff_picks' | 'uploads'
  const [activeCategoryFilter, setActiveCategoryFilter] = useState(null);
  const [categoryFilterTitle, setCategoryFilterTitle] = useState(null);
  const [categoryTracks, setCategoryTracks] = useState([]);
  const [isCategoryLoading, setIsCategoryLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [liveSearchResults, setLiveSearchResults] = useState([]);
  const [isSearchingLive, setIsSearchingLive] = useState(false);

  // Audio frequency simulation
  const [audioFrequencies, setAudioFrequencies] = useState(() => new Float32Array(64));

  // YouTube / Google OAuth state
  const [userProfile, setUserProfile] = useState(() => {
    try {
      const saved = localStorage.getItem("derbis_user_profile");
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [youtubeData, setYoutubeData] = useState(() => {
    try {
      const saved = localStorage.getItem("derbis_youtube_data");
      return saved ? JSON.parse(saved) : { likedTracks: [], playlists: [], subscriptions: [] };
    } catch {
      return { likedTracks: [], playlists: [], subscriptions: [] };
    }
  });

  const [isSyncingYouTube, setIsSyncingYouTube] = useState(false);
  const [syncError, setSyncError] = useState(null);
  const [activeToken, setActiveToken] = useState(null);

  // NO FAKE SONGS: displayed tracks come strictly from real sync or live category search
  const displayedQuickPicks = useMemo(() => {
    // 1. If user clicked a category/hit card
    if (activeCategoryFilter && categoryTracks.length > 0) {
      return categoryTracks;
    }
    // 2. If user logged in and has real YouTube Liked Songs
    if (userProfile?.connected && youtubeData.likedTracks.length > 0) {
      return youtubeData.likedTracks;
    }
    // 3. Fallback to empty (no fake songs!)
    return [];
  }, [activeCategoryFilter, categoryTracks, userProfile, youtubeData.likedTracks]);

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
      videoId: currentTrack?.videoId || "gNi_6U5Pm_o",
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

  // Progress ticker
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
      setQueue(newQueue.filter(t => (t.videoId || t.id) !== (track.videoId || track.id)));
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

  // Fetch Endless Listening Radio tail for a seed track
  // (Matches Derbis App ensureRadioTail and SimilarSongsSelector)
  const ensureRadioTail = useCallback(async (seedTrack) => {
    if (!seedTrack) return [];
    setIsRadioLoading(true);
    setRadioSeedTrack(seedTrack);
    try {
      const config = window.DERBIS_WEB_CONFIG || {};
      const radioTracks = await fetchRadioTracks(seedTrack, config.youtubeDataApiKey, activeToken);
      setRadioRecommendations(radioTracks);
      return radioTracks;
    } catch (e) {
      console.error("ensureRadioTail error:", e);
      return [];
    } finally {
      setIsRadioLoading(false);
    }
  }, [activeToken]);

  // Play song selected from Search:
  // Starts an Endless Listening session with just that song,
  // and fetches different, similar songs for the queue (NOT duplicate search results)
  const playSearchTrack = useCallback(async (track) => {
    if (!track) return;
    // 1. Play track immediately with empty queue
    playTrack(track, []);
    setEndlessListening(true);

    // 2. Fetch Endless Radio tail (different songs by artist & related artists)
    const radioTracks = await ensureRadioTail(track);
    if (radioTracks && radioTracks.length > 0) {
      setQueue(radioTracks);
    }
  }, [playTrack, ensureRadioTail]);

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

  // Track Ended
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

      // Pre-fetch more radio tracks if tail is running low
      if (endlessListening && remaining.length <= 2) {
        ensureRadioTail(next).then((moreTracks) => {
          if (moreTracks && moreTracks.length > 0) {
            setQueue(prev => [...prev, ...moreTracks.slice(0, 8)]);
          }
        });
      }
      return;
    }

    // If queue is empty and endless radio is active, fetch tail for current track
    if (endlessListening && currentTrack) {
      ensureRadioTail(currentTrack).then((radioTracks) => {
        if (radioTracks && radioTracks.length > 0) {
          const next = radioTracks[0];
          setQueue(radioTracks.slice(1));
          playTrack(next);
        } else if (displayedQuickPicks.length > 0) {
          const available = displayedQuickPicks.filter(t => (t.videoId || t.id) !== (currentTrack?.videoId || currentTrack?.id));
          const fallback = available[0] || displayedQuickPicks[0];
          if (fallback) playTrack(fallback);
        } else {
          setIsPlaying(false);
        }
      });
    } else {
      setIsPlaying(false);
    }
  }, [repeatMode, queue, endlessListening, currentTrack, playTrack, seekTo, ensureRadioTail, displayedQuickPicks]);

  // Next Track
  const nextTrack = useCallback(() => {
    if (queue.length > 0) {
      const next = queue[0];
      const remaining = queue.slice(1);
      setQueue(remaining);
      playTrack(next);

      if (endlessListening && remaining.length <= 2) {
        ensureRadioTail(next).then((moreTracks) => {
          if (moreTracks && moreTracks.length > 0) {
            setQueue(prev => [...prev, ...moreTracks.slice(0, 8)]);
          }
        });
      }
    } else if (endlessListening) {
      handleTrackEnded();
    }
  }, [queue, endlessListening, playTrack, handleTrackEnded, ensureRadioTail]);

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

  // Category Filter Selection ("make it changing based what you clicked or select")
  const selectCategoryFilter = useCallback(async (hit) => {
    if (!hit) {
      setActiveCategoryFilter(null);
      setCategoryFilterTitle(null);
      setCategoryTracks([]);
      return;
    }

    setActiveCategoryFilter(hit.id);
    setCategoryFilterTitle(hit.title);
    setIsCategoryLoading(true);

    try {
      const config = window.DERBIS_WEB_CONFIG || {};
      const query = hit.searchQuery || `${hit.title} songs`;
      const tracks = await searchYouTubeLive(query, config.youtubeDataApiKey, activeToken);

      setCategoryTracks(tracks);
      if (tracks.length > 0) {
        playTrack(tracks[0], tracks);
      }
    } catch (err) {
      console.error("Failed to load category tracks:", err);
    } finally {
      setIsCategoryLoading(false);
    }
  }, [playTrack, activeToken]);

  const clearCategoryFilter = useCallback(() => {
    setActiveCategoryFilter(null);
    setCategoryFilterTitle(null);
    setCategoryTracks([]);
  }, []);

  // Real live search
  const performSearch = useCallback(async (query) => {
    if (!query || !query.trim()) {
      setLiveSearchResults([]);
      return;
    }
    setIsSearchingLive(true);
    try {
      const config = window.DERBIS_WEB_CONFIG || {};
      const tracks = await searchYouTubeLive(query, config.youtubeDataApiKey, activeToken);
      setLiveSearchResults(tracks);
    } catch (err) {
      console.error("Live search error:", err);
    } finally {
      setIsSearchingLive(false);
    }
  }, [activeToken]);

  // Connect Google / YouTube OAuth
  const connectGoogleYouTube = useCallback(() => {
    const config = window.DERBIS_WEB_CONFIG || {};
    const clientId = config.googleOAuthClientId || "584141503005-h3lamjrir2ehbpjl3pruojg52u0hvu2j.apps.googleusercontent.com";

    if (!window.google?.accounts?.oauth2) {
      setSyncError("Google Sign-In is still loading. Please check your internet connection and try again.");
      return;
    }

    try {
      setIsSyncingYouTube(true);
      setSyncError(null);

      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: "openid email profile https://www.googleapis.com/auth/youtube.readonly",
        callback: async (tokenResponse) => {
          if (tokenResponse.error) {
            setIsSyncingYouTube(false);
            setSyncError("Google sign-in was canceled or not authorized.");
            return;
          }

          try {
            const token = tokenResponse.access_token;
            setActiveToken(token);
            const data = await fetchYouTubeUserData(token);

            setUserProfile(data.profile);
            setYoutubeData({
              likedTracks: data.likedTracks,
              playlists: data.playlists,
              subscriptions: data.subscriptions
            });

            localStorage.setItem("derbis_user_profile", JSON.stringify(data.profile));
            localStorage.setItem("derbis_youtube_data", JSON.stringify({
              likedTracks: data.likedTracks,
              playlists: data.playlists,
              subscriptions: data.subscriptions
            }));

            // Automatically switch to "For you" (Listen again) on Home
            setActiveSubTab("for_you");
            setActiveCategoryFilter(null);
            setCategoryFilterTitle(null);
            setIsSyncingYouTube(false);

            // If user has liked tracks, begin playback of first real liked track
            if (data.likedTracks.length > 0) {
              playTrack(data.likedTracks[0], data.likedTracks);
            }
          } catch (err) {
            console.error("YouTube sync error:", err);
            setIsSyncingYouTube(false);
            setSyncError("Connected, but could not load YouTube data. Check Google Cloud permissions.");
          }
        }
      });

      client.requestAccessToken({ prompt: "select_account" });
    } catch (err) {
      console.error("Failed to start Google OAuth:", err);
      setIsSyncingYouTube(false);
      setSyncError("Google Sign-In initialization failed.");
    }
  }, [playTrack]);

  // Disconnect Google / YouTube
  const disconnectGoogleYouTube = useCallback(() => {
    setUserProfile(null);
    setYoutubeData({ likedTracks: [], playlists: [], subscriptions: [] });
    setActiveToken(null);
    localStorage.removeItem("derbis_user_profile");
    localStorage.removeItem("derbis_youtube_data");
  }, []);

  // Manual import public YouTube playlist
  const importYouTubePlaylist = useCallback(async (urlOrId) => {
    const playlistId = extractPlaylistId(urlOrId);
    if (!playlistId) throw new Error("Invalid playlist URL or ID. Look for a link containing 'list=PL...'");

    setIsSyncingYouTube(true);
    setSyncError(null);
    try {
      const config = window.DERBIS_WEB_CONFIG || {};
      const tracks = await fetchPlaylistTracks(playlistId, config.youtubeDataApiKey, activeToken);
      
      const newPlaylist = {
        id: playlistId,
        title: `Imported Playlist (${tracks.length} songs)`,
        itemCount: tracks.length,
        thumbnail: tracks[0]?.thumbnail || "",
        tracks
      };

      setYoutubeData(prev => {
        const updated = {
          ...prev,
          playlists: [newPlaylist, ...prev.playlists.filter(p => p.id !== playlistId)]
        };
        localStorage.setItem("derbis_youtube_data", JSON.stringify(updated));
        return updated;
      });

      setIsSyncingYouTube(false);
      return newPlaylist;
    } catch (err) {
      setIsSyncingYouTube(false);
      setSyncError(err.message || "Could not load playlist");
      throw err;
    }
  }, [activeToken]);

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
    radioRecommendations,
    isRadioLoading,
    radioSeedTrack,
    ensureRadioTail,
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
    activeSubTab,
    setActiveSubTab,
    activeCategoryFilter,
    categoryFilterTitle,
    isCategoryLoading,
    selectCategoryFilter,
    clearCategoryFilter,
    displayedQuickPicks,
    searchQuery,
    setSearchQuery,
    liveSearchResults,
    isSearchingLive,
    performSearch,
    audioFrequencies,
    playTrack,
    playSearchTrack,
    togglePlayPause,
    seekTo,
    nextTrack,
    previousTrack,
    addToQueue,
    removeFromQueue,
    reorderQueue,
    clearQueue,

    // YouTube Sync & Auth
    userProfile,
    youtubeData,
    isSyncingYouTube,
    syncError,
    connectGoogleYouTube,
    disconnectGoogleYouTube,
    importYouTubePlaylist
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
