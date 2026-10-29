import React, { createContext, useContext, useState, useEffect, useRef, useCallback, useMemo } from "react";
import { QUICK_PICKS_TRACKS, HITS_TRACK_SETS, GENRE_SEEDS, DERBIS_THE_HITS } from "../data/catalog";
import { fetchYouTubeUserData, fetchPlaylistTracks, extractPlaylistId } from "../services/youtubeAuth";

const PlayerContext = createContext(null);

export function PlayerProvider({ children }) {
  const [currentTrack, setCurrentTrack] = useState(QUICK_PICKS_TRACKS[0]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(194);
  const [volume, setVolume] = useState(80);
  const [isMuted, setIsMuted] = useState(false);
  
  // Audio quality badge ('MAX', 'HI-FI', 'HIGH')
  const [audioQuality, setAudioQuality] = useState("MAX");
  
  // 'song' (artwork) vs 'video' (YouTube embedded visual)
  const [playbackMode, setPlaybackMode] = useState("song");
  
  // Continuous radio / Endless listening
  const [endlessListening, setEndlessListening] = useState(true);
  
  // Up Next Queue
  const [queue, setQueue] = useState(QUICK_PICKS_TRACKS.slice(1));
  const [history, setHistory] = useState([]);
  
  // UI states
  const [isQueueOpen, setIsQueueOpen] = useState(false);
  const [isLyricsOpen, setIsLyricsOpen] = useState(false);
  const [isMobilePlayerExpanded, setIsMobilePlayerExpanded] = useState(false);
  const [isShuffle, setIsShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState("off"); // 'off' | 'all' | 'one'
  const [visualizerMode, setVisualizerMode] = useState("off");
  
  // Navigation / Search state
  const [activeTab, setActiveTab] = useState("home"); // 'home' | 'explore' | 'videos' | 'collection' | 'search'
  const [activeSubTab, setActiveSubTab] = useState("for_you"); // 'for_you' | 'staff_picks' | 'uploads'
  const [activeCategoryFilter, setActiveCategoryFilter] = useState(null);
  const [categoryFilterTitle, setCategoryFilterTitle] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");

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

  // Dynamic Quick Picks calculation based on user selection or YouTube data
  const displayedQuickPicks = useMemo(() => {
    // 1. If user clicked a specific category/hit card
    if (activeCategoryFilter && HITS_TRACK_SETS[activeCategoryFilter]) {
      return HITS_TRACK_SETS[activeCategoryFilter];
    }
    // 2. If user selected subtab "Staff Picks"
    if (activeSubTab === "staff_picks") {
      return HITS_TRACK_SETS.staff_picks;
    }
    // 3. If user selected subtab "Uploads"
    if (activeSubTab === "uploads") {
      return HITS_TRACK_SETS.uploads;
    }
    // 4. If user logged in and has YouTube Music data ("For you" / "Listen again")
    if (userProfile?.connected && youtubeData.likedTracks.length > 0) {
      return youtubeData.likedTracks;
    }
    // 5. Default curated tracks
    return QUICK_PICKS_TRACKS;
  }, [activeCategoryFilter, activeSubTab, userProfile, youtubeData.likedTracks]);

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
      videoId: currentTrack?.videoId || "JFcgOboQZ08",
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

  // Track progress ticker
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
      const candidateList = displayedQuickPicks.length > 0 ? displayedQuickPicks : QUICK_PICKS_TRACKS;
      const available = candidateList.filter(t => (t.videoId || t.id) !== (currentTrack.videoId || currentTrack.id));
      const next = available[Math.floor(Math.random() * available.length)] || candidateList[0];
      
      const moreRadioTracks = candidateList.filter(t => (t.videoId || t.id) !== (next.videoId || next.id)).slice(0, 5);
      setQueue(moreRadioTracks);
      playTrack(next);
    } else {
      setIsPlaying(false);
    }
  }, [repeatMode, queue, endlessListening, currentTrack, playTrack, seekTo, displayedQuickPicks]);

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

  // Category Filter Selection ("make it changing based what you clicked or select")
  const selectCategoryFilter = useCallback((hit) => {
    if (!hit) {
      setActiveCategoryFilter(null);
      setCategoryFilterTitle(null);
      return;
    }
    setActiveCategoryFilter(hit.id);
    setCategoryFilterTitle(hit.title);

    const trackSet = HITS_TRACK_SETS[hit.id] || QUICK_PICKS_TRACKS;
    if (trackSet.length > 0) {
      playTrack(trackSet[0], trackSet);
    }
  }, [playTrack]);

  const clearCategoryFilter = useCallback(() => {
    setActiveCategoryFilter(null);
    setCategoryFilterTitle(null);
  }, []);

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

            // Once logged in, automatically switch subtab to "for_you" so Home displays their YouTube data!
            setActiveSubTab("for_you");
            setActiveCategoryFilter(null);
            setCategoryFilterTitle(null);
            setIsSyncingYouTube(false);

            // If user has liked tracks, begin playback of first liked track
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
    selectCategoryFilter,
    clearCategoryFilter,
    displayedQuickPicks,
    searchQuery,
    setSearchQuery,
    audioFrequencies,
    playTrack,
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
