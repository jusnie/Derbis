(() => {
  "use strict";

  const config = window.DERBIS_WEB_CONFIG || {};
  const clientId = String(config.googleOAuthClientId || "").trim();
  const youtubeDataApiKey = String(config.youtubeDataApiKey || "").trim();
  const scopes = Array.isArray(config.googleOAuthScopes)
    ? config.googleOAuthScopes.join(" ")
    : "openid email profile https://www.googleapis.com/auth/youtube.readonly";
  const stateKey = "derbis-web-google-profile";
  const themeKey = "derbis-web-theme";
  const visualModeKey = "derbis-web-visual-mode";
  let currentVisualMode = (() => {
    try {
      return localStorage.getItem(visualModeKey) || "artwork";
    } catch {
      return "artwork";
    }
  })();
  let tokenClient = null;
  let localObjectUrl = null;
  let activeAccessToken = null;
  let pendingYouTubeSearch = null;
  let youtubeSearchAbort = null;
  let youTubePlayer = null;
  let youtubeApiPromise = null;
  let directYoutubeEmbed = false;
  let spotlightHeroTrack = null;
  let currentYouTubeTrack = {
    title: "Love Me Harder",
    artist: "Ariana Grande, The Weeknd",
    thumbnail: "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg",
    thumb: "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg",
    videoId: "g5qU7p7yOY8"
  };
  let toastTimer = 0;

  const $ = (selector) => document.querySelector(selector);
  const all = (selector) => [...document.querySelectorAll(selector)];
  const elements = {
    sidebar: $("#sidebar"),
    sidebarScrim: $("#sidebar-scrim"),
    menuToggle: $("#menu-toggle"),
    closeMenu: $("#close-menu"),
    main: $("#main-content"),
    toast: $("#toast"),
    accountDialog: $("#account-dialog"),
    dialogClose: $("#dialog-close"),
    profileTrigger: $("#profile-trigger"),
    topProfileTrigger: $("#top-profile-trigger"),
    themeToggle: $("#theme-toggle"),
    settingsDialog: $("#settings-dialog"),
    settingsTrigger: $("#settings-trigger"),
    sidebarSettingsBtn: $("#sidebar-settings-btn"),
    settingsClose: $("#settings-close"),
    modeArtworkBtn: $("#mode-artwork-btn"),
    modeVideoBtn: $("#mode-video-btn"),
    npSwitchArtwork: $("#np-switch-artwork"),
    npSwitchVideo: $("#np-switch-video"),
    dockQueueBtn: $("#dock-queue-btn"),
    dockQueueBadge: $("#dock-queue-badge"),
    sideQueueBtn: $("#side-queue-btn"),
    queueDialog: $("#queue-dialog"),
    queueClose: $("#queue-close"),
    queueNowTitle: $("#queue-now-title"),
    queueNowArtist: $("#queue-now-artist"),
    endlessToggleInput: $("#endless-toggle-input"),
    endlessStatusDesc: $("#endless-status-desc"),
    queueCountPill: $("#queue-count-pill"),
    queueCountNumber: $("#queue-count-number"),
    queueShuffleBtn: $("#queue-shuffle-btn"),
    queueClearBtn: $("#queue-clear-btn"),
    queueList: $("#queue-list"),
    queueEmptyBox: $("#queue-empty-box"),
    sideMediaShell: $("#side-media-shell"),
    sideArtworkCard: $("#side-artwork-card"),
    sideArtworkImg: $("#side-artwork-img"),
    sideVideoContainer: $("#side-video-container"),
    settingsThemeToggle: $("#settings-theme-toggle"),
    nowPlayingSidebar: $("#now-playing-sidebar"),
    youtubeClose: $("#youtube-close"),
    nowPlayingTitle: $("#youtube-dialog-title"),
    nowPlayingArtist: $("#now-playing-artist"),
    sideTrackTitle: $("#side-track-title"),
    sideArtistName: $("#side-artist-name"),
    sideArtistAvatar: $("#side-artist-avatar"),
    sideArtistBio: $("#side-artist-bio"),
    sideArtistExplore: $("#side-artist-explore-btn"),
    dockSideToggle: $("#dock-side-panel-toggle"),
    youtubeForm: $("#youtube-form"),
    youtubeUrl: $("#youtube-url"),
    youtubeFrame: $("#youtube-frame"),
    youtubeSearchForm: $("#youtube-search-form"),
    youtubeSearchInput: $("#youtube-search-input"),
    youtubeSearchStatus: $("#youtube-search-status"),
    youtubeSearchResults: $("#youtube-search-results"),
    profileName: $("#profile-name"),
    profileStatus: $("#profile-status"),
    profileAvatar: $("#profile-avatar"),
    topProfileAvatar: $("#top-profile-avatar"),
    dialogAvatar: $("#dialog-avatar"),
    dialogName: $("#account-dialog-title"),
    dialogDetail: $("#account-dialog-detail"),
    dialogConnect: $("#dialog-connect"),
    disconnect: $("#disconnect-button"),
    libraryConnectBtn: $("#library-connect-btn"),
    libraryDisconnectBtn: $("#library-disconnect-btn"),
    welcomeName: $("#welcome-name"),
    mobileWelcomeName: $("#mobile-welcome-name"),
    desktopDayGreeting: $("#desktop-day-greeting"),
    mobileDayGreeting: $("#mobile-day-greeting"),
    libraryAccountName: $("#library-account-name"),
    libraryAccountDetail: $("#library-account-detail"),
    audio: $("#audio-player"),
    file: $("#local-audio"),
    playerDock: $("#player-dock"),
    dockArt: $("#dock-art"),
    nowPlayingTrigger: $("#now-playing-trigger"),
    dockTitle: $("#dock-title"),
    dockArtist: $("#dock-artist"),
    dockPlay: $("#dock-play"),
    dockNext: $("#dock-next"),
    dockSeek: $("#dock-seek"),
    dockElapsed: $("#dock-elapsed"),
    dockDuration: $("#dock-duration"),
    dockMiniProgressFill: $("#dock-mini-progress-fill"),
    sideSeek: $("#side-seek"),
    sideElapsed: $("#side-elapsed"),
    sideDuration: $("#side-duration"),
    sidePlay: $("#side-play"),
    sidePrevBtn: $("#side-prev-btn"),
    sideNextBtn: $("#side-next-btn"),
    sheetHandle: $(".mobile-sheet-handle"),
    soundtrackCard: $("#soundtrack-card"),
    soundtrackArt: $("#soundtrack-art"),
    soundtrackPortraitImg: $("#soundtrack-portrait-img"),
    soundtrackGlow: $("#soundtrack-glow"),
    soundtrackTitle: $("#soundtrack-title"),
    soundtrackSubtitle: $("#soundtrack-subtitle"),
    soundtrackLead: $("#soundtrack-lead"),
    soundtrackCompanionStack: $("#soundtrack-companion-stack"),
    soundtrackPlay: $("#soundtrack-play"),
    quickShelf: $("#quick-shelf") || $("#quick-grid"),
    quickGrid: $("#quick-shelf") || $("#quick-grid"),
    quickPrevBtn: $("#quick-prev-btn"),
    quickNextBtn: $("#quick-next-btn"),
    quickPlayAll: $("#quick-play-all"),
    orbitRail: $("#orbit-rail"),
    search: $("#search-input")
  };

  function showToast(message) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => elements.toast.classList.remove("is-visible"), 4400);
  }

  function escapeHtml(value) {
    return String(value || "").replace(/[&<>'"]/g, (character) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[character]));
  }

  function storedProfile() {
    try {
      const value = sessionStorage.getItem(stateKey);
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  }

  function saveProfile(profile) {
    sessionStorage.setItem(stateKey, JSON.stringify(profile));
  }

  function setAvatar(element, profile, fallback) {
    if (!element) return;
    if (profile?.picture) {
      element.innerHTML = `<img src="${escapeHtml(profile.picture)}" alt="" referrerpolicy="no-referrer" />`;
      return;
    }
    element.textContent = fallback;
  }

  function greetingDayPart(hour = new Date().getHours()) {
    if (hour >= 5 && hour <= 11) return "Good morning";
    if (hour >= 12 && hour <= 17) return "Good afternoon";
    if (hour >= 18 && hour <= 22) return "Good evening";
    return "Good night";
  }

  function renderDayGreeting() {
    const greeting = greetingDayPart();
    elements.desktopDayGreeting.textContent = greeting;
    elements.mobileDayGreeting.textContent = greeting;
  }

  function scheduleGreetingRefresh() {
    const now = new Date();
    const nextHour = new Date(now);
    nextHour.setMinutes(0, 0, 0);
    nextHour.setHours(now.getHours() + 1);
    window.setTimeout(() => {
      renderDayGreeting();
      scheduleGreetingRefresh();
    }, Math.max(1000, nextHour.getTime() - now.getTime()));
  }

  function getStoredTheme() {
    try {
      const saved = localStorage.getItem(themeKey);
      if (saved === "light" || saved === "dark") return saved;
    } catch {
      // Theme persistence is optional when browser storage is unavailable.
    }
    return window.matchMedia?.("(prefers-color-scheme: light)").matches ? "light" : "dark";
  }

  function applyTheme(theme) {
    const resolvedTheme = theme === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = resolvedTheme;
    const switchingToLight = resolvedTheme === "dark";
    elements.themeToggle.setAttribute("aria-pressed", String(resolvedTheme === "light"));
    elements.themeToggle.setAttribute("aria-label", switchingToLight ? "Use light mode" : "Use dark mode");
    elements.themeToggle.innerHTML = `<svg><use href="#${switchingToLight ? "i-sun" : "i-moon"}"/></svg>`;
    try {
      localStorage.setItem(themeKey, resolvedTheme);
    } catch {
      // The chosen theme still applies for this page view.
    }
  }

  function toggleTheme() {
    applyTheme(document.documentElement.dataset.theme === "light" ? "dark" : "light");
  }

  function extractYouTubeVideoId(value) {
    const raw = String(value || "").trim();
    if (!raw) return null;
    if (/^[A-Za-z0-9_-]{11}$/.test(raw)) return raw;
    try {
      const url = new URL(/^https?:\/\//i.test(raw) ? raw : `https://${raw}`);
      const host = url.hostname.replace(/^www\./, "").toLowerCase();
      let id = null;
      if (host === "youtu.be") id = url.pathname.split("/").filter(Boolean)[0];
      if (host.endsWith("youtube.com")) {
        id = url.searchParams.get("v") || url.pathname.match(/^\/(?:embed|shorts|live)\/([^/?]+)/)?.[1];
      }
      return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null;
    } catch {
      return null;
    }
  }

  function playerPlaceholder(message = "Paste a video link to begin") {
    return `<div class="youtube-placeholder"><svg><use href="#i-youtube"/></svg><span>${escapeHtml(message)}</span></div>`;
  }

  function showPlayerLoading() {
    if (elements.youtubeFrame?.isConnected) {
      elements.youtubeFrame.innerHTML = playerPlaceholder("Loading the official YouTube player…");
    }
  }

  function renderDirectYouTubeEmbed(videoId) {
    // Reliable official fallback for slow/blocked IFrame API script loads.
    if (!elements.youtubeFrame?.isConnected) return;
    directYoutubeEmbed = true;
    elements.youtubeFrame.innerHTML = `<iframe title="YouTube video player" src="https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?autoplay=1&playsinline=1&rel=0&modestbranding=1" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>`;
    showToast("Use the YouTube player controls if your browser blocks autoplay.");
  }

  function loadYouTubeIframeApi() {
    if (window.YT?.Player) return Promise.resolve();
    if (youtubeApiPromise) return youtubeApiPromise;
    youtubeApiPromise = new Promise((resolve, reject) => {
      let settled = false;
      const finish = (callback, value) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timeout);
        callback(value);
      };
      const previousReady = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => {
        if (typeof previousReady === "function") previousReady();
        finish(resolve);
      };
      const script = document.createElement("script");
      script.src = "https://www.youtube.com/iframe_api";
      script.async = true;
      script.onerror = () => finish(reject, new Error("The YouTube player library could not load."));
      const timeout = window.setTimeout(() => finish(reject, new Error("The YouTube player library timed out.")), 7000);
      document.head.append(script);
    }).catch((error) => {
      youtubeApiPromise = null;
      throw error;
    });
    return youtubeApiPromise;
  }

  let youTubeProgressTimer = null;
  let isSeeking = false;

  function updateYouTubeProgress() {
    if (!youTubePlayer || typeof youTubePlayer.getCurrentTime !== "function") return;
    try {
      const current = youTubePlayer.getCurrentTime() || 0;
      const duration = youTubePlayer.getDuration() || 0;
      if (duration > 0) {
        const pct = (current / duration) * 100;
        const curFormatted = formatTime(current);
        const durFormatted = formatTime(duration);
        elements.dockSeek.disabled = false;
        elements.dockDuration.textContent = durFormatted;
        elements.dockElapsed.textContent = curFormatted;
        if (!isSeeking) {
          elements.dockSeek.value = String(pct);
          if (elements.sideSeek) elements.sideSeek.value = String(pct);
        }
        if (elements.dockMiniProgressFill) {
          elements.dockMiniProgressFill.style.width = pct + "%";
        }
        if (elements.sideDuration) elements.sideDuration.textContent = durFormatted;
        if (elements.sideElapsed) elements.sideElapsed.textContent = curFormatted;
      }
    } catch {
      // Player might not be ready yet
    }
  }

  function startYouTubeProgress() {
    stopYouTubeProgress();
    updateYouTubeProgress();
    youTubeProgressTimer = window.setInterval(updateYouTubeProgress, 250);
  }

  function stopYouTubeProgress() {
    if (youTubeProgressTimer) {
      window.clearInterval(youTubeProgressTimer);
      youTubeProgressTimer = null;
    }
  }

  function pauseYouTube() {
    stopYouTubeProgress();
    try {
      youTubePlayer?.pauseVideo?.();
    } catch {
      // The API player may not have finished initializing yet.
    }
    if (directYoutubeEmbed && elements.youtubeFrame?.isConnected) {
      elements.youtubeFrame.innerHTML = playerPlaceholder("Playback paused");
      directYoutubeEmbed = false;
    }
  }

  function openNowPlayingSidebar() {
    elements.nowPlayingSidebar?.classList.add("is-open");
    document.body.classList.add("has-now-playing-open");
    elements.dockSideToggle?.classList.add("is-active");
  }

  function closeNowPlayingSidebar() {
    elements.nowPlayingSidebar?.classList.remove("is-open");
    document.body.classList.remove("has-now-playing-open");
    elements.dockSideToggle?.classList.remove("is-active");
  }

  function toggleNowPlayingSidebar() {
    if (elements.nowPlayingSidebar?.classList.contains("is-open")) {
      closeNowPlayingSidebar();
    } else {
      openNowPlayingSidebar();
    }
  }

  function openYouTubeDialog() {
    openNowPlayingSidebar();
    if (!currentYouTubeTrack) window.setTimeout(() => elements.youtubeUrl?.focus(), 0);
  }

  function closeYouTubeDialog() {
    closeNowPlayingSidebar();
  }

  function setYouTubeNowPlaying(track) {
    const title = track?.title || "Public YouTube video";
    const artist = track?.artist || "YouTube";
    const thumbnail = String(track?.thumbnail || track?.thumb || "");
    const duration = String(track?.duration || "");
    const videoId = track?.videoId || "";
    currentYouTubeTrack = { title, artist, thumbnail, thumb: thumbnail, duration, videoId };
    elements.dockTitle.textContent = title;
    elements.dockArtist.textContent = artist;
    elements.nowPlayingTitle.textContent = title;
    elements.nowPlayingArtist.textContent = artist;
    if (elements.sideTrackTitle) elements.sideTrackTitle.textContent = title;
    if (elements.sideArtistName) elements.sideArtistName.textContent = artist;
    if (elements.sideArtistAvatar && thumbnail) elements.sideArtistAvatar.src = thumbnail;
    if (elements.sideArtworkImg && thumbnail) elements.sideArtworkImg.src = thumbnail;
    if (elements.sideArtistBio) elements.sideArtistBio.textContent = `Official track by ${artist}. Playing with YouTube’s official embedded player in Derbis.`;
    if (elements.queueNowTitle) elements.queueNowTitle.textContent = title;
    if (elements.queueNowArtist) elements.queueNowArtist.textContent = artist;
    if (duration) elements.dockDuration.textContent = duration;
    elements.dockElapsed.textContent = "0:00";
    elements.dockSeek.value = "0";
    elements.dockSeek.disabled = false;
    elements.dockArt.className = "dock-art has-thumb";
    if (thumbnail) {
      elements.dockArt.innerHTML = `<img id="dock-art-img" src="${escapeHtml(thumbnail)}" alt="${escapeHtml(title)}" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover;border-radius:inherit;" />`;
    } else {
      elements.dockArt.innerHTML = "<i></i><em>▶</em>";
    }
    all(".quick-track-row, .quick-track, .orbit-card").forEach((item) => {
      const match = item.dataset.title === title;
      item.classList.toggle("is-current", match);
      const bars = item.querySelector(".playing-bars");
      if (bars) bars.style.display = match ? "inline-flex" : "none";
    });
  }

  async function playYouTubeVideo(value, track = null) {
    const videoId = extractYouTubeVideoId(value);
    if (!videoId) {
      showToast("Paste a valid public YouTube video URL or 11-character video ID.");
      return;
    }
    setYouTubeNowPlaying(track || { title: "Public YouTube video", artist: "YouTube", videoId });
    if (window.innerWidth > 820) {
      openNowPlayingSidebar();
    }
    if (elements.audio.src) elements.audio.pause();
    showPlayerLoading();
    try {
      await loadYouTubeIframeApi();
      if (youTubePlayer) {
        directYoutubeEmbed = false;
        youTubePlayer.loadVideoById(videoId);
      } else {
        directYoutubeEmbed = false;
        youTubePlayer = new window.YT.Player("youtube-frame", {
          videoId,
          playerVars: { autoplay: 1, playsinline: 1, rel: 0, modestbranding: 1 },
          events: {
            onReady: (event) => {
              event.target.playVideo();
            },
            onStateChange: (event) => {
              const state = event.data;
              if (state === window.YT.PlayerState.PLAYING) {
                if (elements.audio.src) elements.audio.pause();
                setPlayIcon(true);
                startYouTubeProgress();
              } else if (state === window.YT.PlayerState.PAUSED) {
                setPlayIcon(false);
                stopYouTubeProgress();
              } else if (state === window.YT.PlayerState.ENDED) {
                setPlayIcon(false);
                stopYouTubeProgress();
                elements.dockSeek.value = "0";
                elements.dockElapsed.textContent = "0:00";
                playNextTrack();
              }
            },
            onAutoplayBlocked: () => showToast("Tap play in the player to start this video."),
            onError: () => {
              console.warn("YouTube iframe API error; trying fallback embed.");
              renderDirectYouTubeEmbed(videoId);
            }
          }
        });
      }
      showToast(`Playing ${track?.title || "song"} on Derbis`);
    } catch (error) {
      console.warn("YouTube IFrame API unavailable; using official embed fallback.", error);
      renderDirectYouTubeEmbed(videoId);
    }
  }

  function renderAccount(profile) {
    const connected = Boolean(profile);
    const name = profile?.name || "Yasuda";
    const initial = name.slice(0, 1).toUpperCase() || "Y";
    const accountLine = connected
      ? `${profile.email || "Google account"} · ${profile.channelTitle || "No public channel found"}`
      : "Connect Google";

    elements.profileName.textContent = name;
    elements.profileStatus.textContent = accountLine;
    elements.welcomeName.textContent = name;
    elements.mobileWelcomeName.textContent = name;
    elements.dialogName.textContent = name;
    elements.dialogDetail.textContent = connected
      ? `${profile.email || "Google account"} · ${profile.channelTitle || "No public YouTube channel found"}`
      : "Connect Google to add official YouTube account and channel details.";
    elements.libraryAccountName.textContent = connected ? `${name}'s connection` : "Connect your account";
    elements.libraryAccountDetail.textContent = connected
      ? `Connected as ${profile.email || "Google account"}. ${profile.channelTitle || "No public YouTube channel found"}.`
      : "Use official Google browser authorization for your YouTube profile and channel details.";
    elements.dialogConnect.hidden = connected;
    elements.disconnect.hidden = !connected;
    if (elements.libraryConnectBtn) elements.libraryConnectBtn.hidden = connected;
    if (elements.libraryDisconnectBtn) elements.libraryDisconnectBtn.hidden = !connected;
    [elements.profileAvatar, elements.topProfileAvatar, elements.dialogAvatar].forEach((avatar) => setAvatar(avatar, profile, initial));
  }

  function openAccountDialog() {
    if (!elements.accountDialog.open) elements.accountDialog.showModal();
  }

  function closeAccountDialog() {
    if (elements.accountDialog.open) elements.accountDialog.close();
  }

  function openSettingsDialog() {
    if (!elements.settingsDialog.open) elements.settingsDialog.showModal();
  }

  function closeSettingsDialog() {
    if (elements.settingsDialog.open) elements.settingsDialog.close();
  }

  function applyVisualMode(mode, notify = false) {
    currentVisualMode = mode === "video" ? "video" : "artwork";
    try {
      localStorage.setItem(visualModeKey, currentVisualMode);
    } catch {}

    if (elements.sideMediaShell) {
      elements.sideMediaShell.setAttribute("data-visual-mode", currentVisualMode);
    }
    if (elements.modeArtworkBtn) {
      const isArtwork = currentVisualMode === "artwork";
      elements.modeArtworkBtn.classList.toggle("is-active", isArtwork);
      elements.modeArtworkBtn.setAttribute("aria-checked", String(isArtwork));
    }
    if (elements.modeVideoBtn) {
      const isVideo = currentVisualMode === "video";
      elements.modeVideoBtn.classList.toggle("is-active", isVideo);
      elements.modeVideoBtn.setAttribute("aria-checked", String(isVideo));
    }
    if (elements.npSwitchArtwork) {
      elements.npSwitchArtwork.classList.toggle("is-active", currentVisualMode === "artwork");
    }
    if (elements.npSwitchVideo) {
      elements.npSwitchVideo.classList.toggle("is-active", currentVisualMode === "video");
    }

    if (notify) {
      showToast(currentVisualMode === "artwork" ? "Visual preference: Song Artwork" : "Visual preference: Video Player");
    }
  }

  function openMenu() {
    elements.sidebar.classList.add("is-open");
    elements.sidebarScrim.hidden = false;
    elements.menuToggle.setAttribute("aria-expanded", "true");
  }

  function closeMenu() {
    elements.sidebar.classList.remove("is-open");
    elements.sidebarScrim.hidden = true;
    elements.menuToggle.setAttribute("aria-expanded", "false");
  }

  function goTo(page) {
    const validPages = ["home", "explore", "library"];
    if (!validPages.includes(page)) return;
    all("[data-page-panel]").forEach((panel) => { panel.hidden = panel.dataset.pagePanel !== page; });
    all("[data-page]").forEach((button) => button.classList.toggle("is-active", button.dataset.page === page));
    window.location.hash = page === "home" ? "" : page;
    closeMenu();
    elements.main.focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function loadGoogleIdentity() {
    return new Promise((resolve, reject) => {
      if (window.google?.accounts?.oauth2) return resolve();
      const script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.async = true;
      script.defer = true;
      script.onload = resolve;
      script.onerror = () => reject(new Error("Google Identity Services could not load."));
      document.head.append(script);
    });
  }

  async function requestConnection() {
    if (!clientId) {
      showToast("Google OAuth needs a public Web Client ID before it can connect.");
      return;
    }
    try {
      await loadGoogleIdentity();
      tokenClient ||= window.google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: scopes,
        callback: async (tokenResponse) => {
          if (tokenResponse.error) {
            showToast("Google authorization was not completed.");
            return;
          }
          try {
            activeAccessToken = tokenResponse.access_token;
            const headers = { Authorization: `Bearer ${activeAccessToken}` };
            const [identityResponse, channelResponse] = await Promise.all([
              fetch("https://www.googleapis.com/oauth2/v3/userinfo", { headers }),
              fetch("https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true", { headers })
            ]);
            if (!identityResponse.ok) throw new Error("Google account information was unavailable.");
            const identity = await identityResponse.json();
            const channels = channelResponse.ok ? await channelResponse.json() : { items: [] };
            const profile = {
              name: identity.name,
              email: identity.email,
              picture: identity.picture,
              channelTitle: channels.items?.[0]?.snippet?.title || "No public YouTube channel found"
            };
            saveProfile(profile);
            renderAccount(profile);
            closeAccountDialog();
            showToast("Google / YouTube account connected for this browser session.");
            await syncYouTubeAccountData(activeAccessToken);
            if (pendingYouTubeSearch) {
              const query = pendingYouTubeSearch;
              pendingYouTubeSearch = null;
              searchYouTube(query);
            }
          } catch (error) {
            console.error(error);
            showToast("Authorization finished, but account data could not be loaded. Check the YouTube Data API setup.");
          }
        }
      });
      tokenClient.requestAccessToken({ prompt: "select_account" });
    } catch (error) {
      console.error(error);
      showToast("Google sign-in could not start. Check the Client ID and allowed website origin.");
    }
  }

  function disconnect() {
    sessionStorage.removeItem(stateKey);
    localStorage.removeItem("derbis-youtube-account-data");
    renderAccount(null);
    applyDerbisAlgorithm();
    showToast("Disconnected from this browser session.");
  }

  function formatTime(seconds) {
    if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
    const whole = Math.floor(seconds);
    return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, "0")}`;
  }

  function setPlayIcon(isPlaying) {
    const iconHtml = `<svg><use href="#${isPlaying ? "i-pause" : "i-play"}"/></svg>`;
    elements.dockPlay.innerHTML = iconHtml;
    elements.dockPlay.setAttribute("aria-label", isPlaying ? "Pause audio" : "Play audio");
    if (elements.sidePlay) {
      elements.sidePlay.innerHTML = iconHtml;
      elements.sidePlay.setAttribute("aria-label", isPlaying ? "Pause audio" : "Play audio");
    }
  }

  function resetPlayer() {
    elements.dockSeek.value = "0";
    elements.dockElapsed.textContent = "0:00";
    elements.dockDuration.textContent = formatTime(elements.audio.duration);
    if (elements.sideSeek) elements.sideSeek.value = "0";
    if (elements.sideElapsed) elements.sideElapsed.textContent = "0:00";
    if (elements.sideDuration) elements.sideDuration.textContent = formatTime(elements.audio.duration);
    if (elements.dockMiniProgressFill) elements.dockMiniProgressFill.style.width = "0%";
  }

  let playbackQueue = [];
  let endlessListeningEnabled = (localStorage.getItem("derbis_endless_listening") ?? "true") === "true";

  function setEndlessListening(enabled) {
    endlessListeningEnabled = Boolean(enabled);
    try {
      localStorage.setItem("derbis_endless_listening", String(endlessListeningEnabled));
    } catch {}
    if (elements.endlessToggleInput) elements.endlessToggleInput.checked = endlessListeningEnabled;
    if (elements.endlessStatusDesc) {
      elements.endlessStatusDesc.textContent = endlessListeningEnabled ? "Continuous radio on" : "Stops when queue ends";
    }
    const emptyNote = $("#queue-empty-note");
    if (emptyNote) {
      emptyNote.textContent = endlessListeningEnabled
        ? "Endless listening is active. More songs from this genre will autoplay automatically when this track finishes."
        : "Endless listening is off. Playback will stop when current track ends.";
    }
    showToast(endlessListeningEnabled ? "Endless listening enabled" : "Endless listening off");
  }

  function openQueueDialog() {
    renderQueue();
    if (!elements.queueDialog.open) elements.queueDialog.showModal();
  }

  function closeQueueDialog() {
    if (elements.queueDialog.open) elements.queueDialog.close();
  }

  function toggleQueueDialog() {
    if (elements.queueDialog.open) {
      closeQueueDialog();
    } else {
      openQueueDialog();
    }
  }

  function renderQueue() {
    if (!elements.queueList) return;
    const count = playbackQueue.length;
    if (elements.queueCountNumber) elements.queueCountNumber.textContent = count;
    if (elements.dockQueueBadge) {
      if (count > 0) {
        elements.dockQueueBadge.hidden = false;
        elements.dockQueueBadge.textContent = count > 99 ? "99+" : String(count);
      } else {
        elements.dockQueueBadge.hidden = true;
      }
    }
    if (currentYouTubeTrack) {
      if (elements.queueNowTitle) elements.queueNowTitle.textContent = currentYouTubeTrack.title;
      if (elements.queueNowArtist) elements.queueNowArtist.textContent = currentYouTubeTrack.artist;
    }
    if (elements.endlessToggleInput) {
      elements.endlessToggleInput.checked = endlessListeningEnabled;
    }
    if (elements.endlessStatusDesc) {
      elements.endlessStatusDesc.textContent = endlessListeningEnabled ? "Continuous radio on" : "Stops when queue ends";
    }

    if (count === 0) {
      elements.queueList.innerHTML = "";
      if (elements.queueEmptyBox) elements.queueEmptyBox.style.display = "flex";
      return;
    }

    if (elements.queueEmptyBox) elements.queueEmptyBox.style.display = "none";
    elements.queueList.innerHTML = playbackQueue.map((track, idx) => `
      <div class="queue-track-row" data-queue-idx="${idx}" role="button" tabindex="0" aria-label="Play ${escapeHtml(track.title)} by ${escapeHtml(track.artist)}">
        <span class="queue-track-idx">${idx + 1}</span>
        <img class="queue-track-art" src="${escapeHtml(track.thumbnail || track.thumb || 'https://i.ytimg.com/vi/' + track.videoId + '/hqdefault.jpg')}" alt="" loading="lazy" />
        <div class="queue-track-text">
          <strong>${escapeHtml(track.title)}</strong>
          <span>${escapeHtml(track.artist)}</span>
        </div>
        <span class="queue-track-dur">${escapeHtml(track.duration || '')}</span>
        <button class="queue-track-del" type="button" data-del-idx="${idx}" aria-label="Remove ${escapeHtml(track.title)} from queue" title="Remove track">
          <svg viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </button>
      </div>
    `).join("");
  }

  function removeFromQueue(index) {
    if (index >= 0 && index < playbackQueue.length) {
      const removed = playbackQueue.splice(index, 1)[0];
      renderQueue();
      showToast(`Removed “${removed.title}” from queue`);
    }
  }

  function clearQueue() {
    if (!playbackQueue.length) return;
    playbackQueue = [];
    renderQueue();
    showToast("Queue cleared");
  }

  function shuffleQueue() {
    if (playbackQueue.length <= 1) return;
    for (let i = playbackQueue.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [playbackQueue[i], playbackQueue[j]] = [playbackQueue[j], playbackQueue[i]];
    }
    renderQueue();
    showToast("Queue shuffled");
  }

  function addToQueue(track, playNext = false) {
    if (!track || !track.videoId) return;
    if (playNext) {
      playbackQueue.unshift(track);
    } else {
      playbackQueue.push(track);
    }
    renderQueue();
    showToast(`Added “${track.title}” to queue`);
  }

  function loadEndlessRecommendationsAndPlay() {
    const currentVid = currentYouTubeTrack?.videoId;
    let pool = [];

    if (currentYouTubeTrack) {
      const artistLower = (currentYouTubeTrack.artist || "").toLowerCase();
      for (const [genreKey, catData] of Object.entries(CURATED_GENRE_TRACKS)) {
        const matches = (catData.songs || []).some((s) => s.videoId === currentVid || s.artist.toLowerCase().includes(artistLower) || artistLower.includes(s.artist.toLowerCase()));
        if (matches) {
          pool = (catData.songs || []).filter((s) => s.videoId !== currentVid);
          break;
        }
      }
    }

    if (!pool.length) {
      pool = DERBIS_CATALOG_TRACKS.filter((s) => s.videoId !== currentVid);
    }

    const shuffled = [...pool].sort(() => Math.random() - 0.5);
    const nextTrack = shuffled[0];
    const rest = shuffled.slice(1, 6);

    if (nextTrack) {
      playbackQueue = rest;
      renderQueue();
      showToast(`Endless listening: ${nextTrack.title} · ${nextTrack.artist}`);
      playYouTubeVideo(nextTrack.videoId, nextTrack);
    } else {
      setPlayIcon(false);
      showToast("End of playback");
    }
  }

  function playNextTrack() {
    if (playbackQueue.length > 0) {
      const nextTrack = playbackQueue.shift();
      renderQueue();
      if (nextTrack.videoId) {
        showToast(`Now playing: ${nextTrack.title}`);
        playYouTubeVideo(nextTrack.videoId, nextTrack);
      }
      return;
    }

    if (endlessListeningEnabled) {
      loadEndlessRecommendationsAndPlay();
      return;
    }

    setPlayIcon(false);
    showToast("Queue finished · Endless listening is off");
  }

  function playPrevTrack() {
    if (youTubePlayer && typeof youTubePlayer.getCurrentTime === "function") {
      try {
        if (youTubePlayer.getCurrentTime() > 3) {
          youTubePlayer.seekTo(0, true);
          return;
        }
      } catch {}
    }
    const history = getPlaybackHistory();
    if (history && history.length > 1) {
      const prevTrack = history[1];
      if (prevTrack && prevTrack.videoId) {
        playYouTubeVideo(prevTrack.videoId, prevTrack);
        return;
      }
    }
    const tracks = all(".quick-track-row, .quick-track");
    if (!tracks.length) return;
    const currentIdx = tracks.findIndex((t) => t.classList.contains("is-current"));
    const prevIdx = (currentIdx - 1 + tracks.length) % tracks.length;
    selectTrack(tracks[prevIdx]);
  }

  function selectLocalAudio(file) {
    if (!file) return;
    if (localObjectUrl) URL.revokeObjectURL(localObjectUrl);
    localObjectUrl = URL.createObjectURL(file);
    elements.audio.src = localObjectUrl;
    elements.audio.load();
    const title = file.name.replace(/\.[^.]+$/, "") || "Local audio";
    setNowPlaying(title, "Local audio · browser playback", "art-blue");
    resetPlayer();
    elements.audio.play().catch(() => showToast("Your audio file is ready. Press play when you are ready."));
  }

  function togglePlayback() {
    if (currentYouTubeTrack || youTubePlayer) {
      if (!youTubePlayer && currentYouTubeTrack?.videoId) {
        playYouTubeVideo(currentYouTubeTrack.videoId, currentYouTubeTrack);
        return;
      }
      try {
        const state = youTubePlayer?.getPlayerState?.();
        if (state === window.YT?.PlayerState?.PLAYING || state === 1) {
          youTubePlayer?.pauseVideo?.();
          setPlayIcon(false);
          return;
        } else if (state === window.YT?.PlayerState?.PAUSED || state === 2 || state === -1 || state === 0 || state === 5) {
          youTubePlayer?.playVideo?.();
          setPlayIcon(true);
          return;
        }
      } catch {
        // Fall back to opening Now Playing
      }
      if (currentYouTubeTrack?.videoId) {
        playYouTubeVideo(currentYouTubeTrack.videoId, currentYouTubeTrack);
        return;
      }
      openYouTubeDialog();
      return;
    }
    if (elements.audio.src) {
      if (elements.audio.paused) {
        elements.audio.play().catch(() => showToast("This browser could not play that audio file."));
      } else {
        elements.audio.pause();
      }
      return;
    }
    // If no song is active, open Now Playing so the user can search or play
    openYouTubeDialog();
  }

  function setNowPlaying(title, artist, artClass = "art-noir", thumb = "", videoId = "") {
    currentYouTubeTrack = { title, artist, thumbnail: thumb, thumb, videoId };
    elements.dockTitle.textContent = title;
    elements.dockArtist.textContent = artist;
    elements.nowPlayingTitle.textContent = title;
    elements.nowPlayingArtist.textContent = artist;
    if (elements.sideTrackTitle) elements.sideTrackTitle.textContent = title;
    if (elements.sideArtistName) elements.sideArtistName.textContent = artist;
    if (elements.sideArtistAvatar && thumb) elements.sideArtistAvatar.src = thumb;
    if (elements.sideArtworkImg && thumb) elements.sideArtworkImg.src = thumb;

    elements.dockArt.className = `dock-art ${thumb ? "has-thumb" : artClass}`;
    if (thumb) {
      elements.dockArt.innerHTML = `<img id="dock-art-img" src="${escapeHtml(thumb)}" alt="${escapeHtml(title)}" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover;border-radius:inherit;" />`;
    } else {
      elements.dockArt.innerHTML = `<i></i><em>${escapeHtml(title.slice(0, 8).toUpperCase())}</em>`;
    }
    all(".quick-track-row, .quick-track, .orbit-card").forEach((item) => {
      const match = item.dataset.title === title;
      item.classList.toggle("is-current", match);
      const bars = item.querySelector(".playing-bars");
      if (bars) bars.style.display = match ? "inline-flex" : "none";
    });
  }

  /* ==========================================================================
     Derbis Personal Orbit & Quick Picks Algorithm Engine
     Mirrors DerbisPersonalOrbit.kt and DerbisScreenViewModels.kt (buildQuickPicks)
     ========================================================================== */

  const KNOWN_ARTIST_PROFILES = {
    "ariana grande": {
      name: "Ariana Grande",
      portrait: "https://yt3.ggpht.com/u2o5_V4WeYMDebsryzMFHN6OBoXZsMjfpOSguscVEAVS6MKYHowaXnPjfZmzaRSr1jqtjiCowg=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/u2o5_V4WeYMDebsryzMFHN6OBoXZsMjfpOSguscVEAVS6MKYHowaXnPjfZmzaRSr1jqtjiCowg=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(139, 187, 255, 0.35)",
      topTrack: {
        title: "Love Me Harder",
        artist: "Ariana Grande, The Weeknd",
        videoId: "g5qU7p7yOY8",
        thumbnail: "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg"
      }
    },
    "maki": {
      name: "Maki",
      portrait: "https://yt3.ggpht.com/FoD6VmzaE4nKTPEKR0HXYH9GAaoFhGRifUJW6T9tneMfHjRMMvr_ToomsP5TDDTFxPAWiWp_=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/FoD6VmzaE4nKTPEKR0HXYH9GAaoFhGRifUJW6T9tneMfHjRMMvr_ToomsP5TDDTFxPAWiWp_=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(244, 161, 99, 0.35)",
      topTrack: {
        title: "kahel na langit",
        artist: "Maki",
        videoId: "DuKb1yF5Wh4",
        thumbnail: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg"
      }
    },
    "lady gaga": {
      name: "Lady Gaga",
      portrait: "https://yt3.ggpht.com/lM8HHBd5BZtP_-Dh1i3n1Z4teVW8v2KTG-hsS83yXUiK0K6krkk1XNEFbho3uwH-CS-FQpnlHw=s800-c-k-c0x00ffffff-no-rj",
      avatar: "https://yt3.ggpht.com/lM8HHBd5BZtP_-Dh1i3n1Z4teVW8v2KTG-hsS83yXUiK0K6krkk1XNEFbho3uwH-CS-FQpnlHw=s800-c-k-c0x00ffffff-no-rj",
      glow: "rgba(220, 120, 240, 0.35)",
      topTrack: {
        title: "Die With A Smile",
        artist: "Lady Gaga & Bruno Mars",
        videoId: "kPa7bsKwL-c",
        thumbnail: "https://i.ytimg.com/vi/kPa7bsKwL-c/hqdefault.jpg"
      }
    },
    "billie eilish": {
      name: "Billie Eilish",
      portrait: "https://yt3.ggpht.com/cCf3P5ZRQ5f0D69oXzAg6LUI6gazIPLhIohervVyg0JdGrDfo5MUCzU_qujvGruLVWwtVtbf6Q=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/cCf3P5ZRQ5f0D69oXzAg6LUI6gazIPLhIohervVyg0JdGrDfo5MUCzU_qujvGruLVWwtVtbf6Q=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(110, 200, 160, 0.35)",
      topTrack: {
        title: "BIRDS OF A FEATHER",
        artist: "Billie Eilish",
        videoId: "V9PVRfjEBTI",
        thumbnail: "https://i.ytimg.com/vi/V9PVRfjEBTI/hqdefault.jpg"
      }
    },
    "sabrina carpenter": {
      name: "Sabrina Carpenter",
      portrait: "https://yt3.ggpht.com/KgFH3zHZEmjN6tPMRBTrc1kBAHPajOGgw5tQbh-rNJnLyoabg7CCxcGdLjBOhaBxGZtRz-Z5=s800-c-k-c0x00ffffff-no-rj",
      avatar: "https://yt3.ggpht.com/KgFH3zHZEmjN6tPMRBTrc1kBAHPajOGgw5tQbh-rNJnLyoabg7CCxcGdLjBOhaBxGZtRz-Z5=s800-c-k-c0x00ffffff-no-rj",
      glow: "rgba(255, 190, 120, 0.35)",
      topTrack: {
        title: "Espresso",
        artist: "Sabrina Carpenter",
        videoId: "eVli-tstM5E",
        thumbnail: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg"
      }
    },
    "the weeknd": {
      name: "The Weeknd",
      portrait: "https://yt3.ggpht.com/EuRWZeic2Q6xLGGxm-VihpDFHAS790YJXHwnnUnYBubP1alIO8rmsbUhaQT2Cz-zzZ28HEo7pg=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/EuRWZeic2Q6xLGGxm-VihpDFHAS790YJXHwnnUnYBubP1alIO8rmsbUhaQT2Cz-zzZ28HEo7pg=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(230, 80, 80, 0.35)",
      topTrack: {
        title: "Blinding Lights",
        artist: "The Weeknd",
        videoId: "4NRXx6U8ABQ",
        thumbnail: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"
      }
    },
    "bini": {
      name: "BINI",
      portrait: "https://yt3.ggpht.com/z7Z3zm2O1_pHcFBcZW0DBfME5xlVmgfiMoWlw9LqGYybjiIzHqnbZoAG-ApCHmcBQ12H1TFN-uU=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/z7Z3zm2O1_pHcFBcZW0DBfME5xlVmgfiMoWlw9LqGYybjiIzHqnbZoAG-ApCHmcBQ12H1TFN-uU=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(255, 140, 200, 0.35)",
      topTrack: {
        title: "Pantropiko",
        artist: "BINI",
        videoId: "Zx31bB2vMns",
        thumbnail: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg"
      }
    },
    "bruno mars": {
      name: "Bruno Mars",
      portrait: "https://yt3.ggpht.com/7v3fStxrcZgiFvAjAs-Mm9VtjSCmSyExaJ7X4MgXD24Evzm-_pAPZKAkT9bwTgdrq0Be5kltCw=s800-c-k-c0xffffffff-no-rj-mo",
      avatar: "https://yt3.ggpht.com/7v3fStxrcZgiFvAjAs-Mm9VtjSCmSyExaJ7X4MgXD24Evzm-_pAPZKAkT9bwTgdrq0Be5kltCw=s800-c-k-c0xffffffff-no-rj-mo",
      glow: "rgba(255, 180, 80, 0.35)",
      topTrack: {
        title: "24K Magic",
        artist: "Bruno Mars",
        videoId: "UqyT8IEBkvY",
        thumbnail: "https://i.ytimg.com/vi/UqyT8IEBkvY/hqdefault.jpg"
      }
    },
    "taylor swift": {
      name: "Taylor Swift",
      portrait: "https://yt3.ggpht.com/5cW4-NDteLLBaWpsjSirr7EmaOKtFGyX8PG2-6Xx7RELneLZe50Y5Bv9oEothnH9BOqxUpClfA=s800-c-k-c0x00ffffff-no-rj",
      avatar: "https://yt3.ggpht.com/5cW4-NDteLLBaWpsjSirr7EmaOKtFGyX8PG2-6Xx7RELneLZe50Y5Bv9oEothnH9BOqxUpClfA=s800-c-k-c0x00ffffff-no-rj",
      glow: "rgba(170, 200, 255, 0.35)",
      topTrack: {
        title: "Cruel Summer",
        artist: "Taylor Swift",
        videoId: "ic8j13piAhQ",
        thumbnail: "https://i.ytimg.com/vi/ic8j13piAhQ/hqdefault.jpg"
      }
    },
    "olivia rodrigo": {
      name: "Olivia Rodrigo",
      portrait: "https://yt3.ggpht.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=s800-c-k-c0x00ffffff-no-rj",
      avatar: "https://yt3.ggpht.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=s800-c-k-c0x00ffffff-no-rj",
      glow: "rgba(180, 120, 220, 0.35)",
      topTrack: {
        title: "vampire",
        artist: "Olivia Rodrigo",
        videoId: "ZmDBbnmKpqQ",
        thumbnail: "https://i.ytimg.com/vi/ZmDBbnmKpqQ/hqdefault.jpg"
      }
    }
  };

  const DERBIS_CATALOG_TRACKS = [
    { title: "Love Me Harder", artist: "Ariana Grande, The Weeknd", videoId: "g5qU7p7yOY8", thumbnail: "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg" },
    { title: "kahel na langit", artist: "Maki", videoId: "DuKb1yF5Wh4", thumbnail: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg" },
    { title: "Die With A Smile", artist: "Lady Gaga & Bruno Mars", videoId: "kPa7bsKwL-c", thumbnail: "https://i.ytimg.com/vi/kPa7bsKwL-c/hqdefault.jpg" },
    { title: "BIRDS OF A FEATHER", artist: "Billie Eilish", videoId: "V9PVRfjEBTI", thumbnail: "https://i.ytimg.com/vi/V9PVRfjEBTI/hqdefault.jpg" },
    { title: "Espresso", artist: "Sabrina Carpenter", videoId: "eVli-tstM5E", thumbnail: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg" },
    { title: "Dilaw", artist: "Maki", videoId: "L215z9C4Zd8", thumbnail: "https://i.ytimg.com/vi/L215z9C4Zd8/hqdefault.jpg" },
    { title: "we can't be friends (wait for your love)", artist: "Ariana Grande", videoId: "KNtJGQkC-WI", thumbnail: "https://i.ytimg.com/vi/KNtJGQkC-WI/hqdefault.jpg" },
    { title: "Taste", artist: "Sabrina Carpenter", videoId: "kJQP7kiw5Fk", thumbnail: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" },
    { title: "Pantropiko", artist: "BINI", videoId: "Zx31bB2vMns", thumbnail: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg" },
    { title: "Blinding Lights", artist: "The Weeknd", videoId: "4NRXx6U8ABQ", thumbnail: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
    { title: "Starboy", artist: "The Weeknd ft. Daft Punk", videoId: "34Na4j8AVgA", thumbnail: "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg" },
    { title: "Cruel Summer", artist: "Taylor Swift", videoId: "ic8j13piAhQ", thumbnail: "https://i.ytimg.com/vi/ic8j13piAhQ/hqdefault.jpg" },
    { title: "vampire", artist: "Olivia Rodrigo", videoId: "ZmDBbnmKpqQ", thumbnail: "https://i.ytimg.com/vi/ZmDBbnmKpqQ/hqdefault.jpg" },
    { title: "good 4 u", artist: "Olivia Rodrigo", videoId: "gNi_6U5Pm_o", thumbnail: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg" },
    { title: "7 rings", artist: "Ariana Grande", videoId: "QYh6mYIJG2Y", thumbnail: "https://i.ytimg.com/vi/QYh6mYIJG2Y/hqdefault.jpg" },
    { title: "Bad Romance", artist: "Lady Gaga", videoId: "bESGLojNYSo", thumbnail: "https://i.ytimg.com/vi/bESGLojNYSo/hqdefault.jpg" },
    { title: "24K Magic", artist: "Bruno Mars", videoId: "UqyT8IEBkvY", thumbnail: "https://i.ytimg.com/vi/UqyT8IEBkvY/hqdefault.jpg" },
    { title: "Levitating", artist: "Dua Lipa", videoId: "TUVcZfQe-Kw", thumbnail: "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg" },
    { title: "Bohemian Rhapsody", artist: "Queen", videoId: "fJ9rUzIMcZQ", thumbnail: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg" },
    { title: "Viva La Vida", artist: "Coldplay", videoId: "dvgZkm1xWPE", thumbnail: "https://i.ytimg.com/vi/dvgZkm1xWPE/hqdefault.jpg" }
  ];

  const bracketAnnotationPattern = /[([][^)]*(?:(?:official\s+)?(?:music\s+)?(?:video|audio)|lyrics?|visuali[sz]er|feat\.?|ft\.?|featuring|hd|4k|mv)[^)\]]*[)\]]/gi;
  const trailingFeaturePattern = /\b(?:feat\.?|ft\.?|featuring)\b.*$/i;
  const displayAnnotationPattern = /\b(?:(?:official\s+)?(?:music\s+)?(?:video|audio)|lyrics?|visuali[sz]er|4k|hd|mv)\b/gi;
  const nonMusicWordPattern = /[^\p{L}\p{M}\p{N}\s]/gu;
  const whitespacePattern = /\s+/g;
  const artistSeparatorPattern = /(?:\s(?:feat\.?|featuring|ft\.?|and|with|e|y|und|[,&;+])\s|[,;&+]\s)/gi;

  function cleanYouTubeTrackTitle(raw) {
    return String(raw || "")
      .replace(bracketAnnotationPattern, " ")
      .replace(displayAnnotationPattern, " ")
      .replace(whitespacePattern, " ")
      .trim();
  }

  function parseYouTubeVideoTitle(rawTitle, defaultChannel) {
    let clean = cleanYouTubeTrackTitle(rawTitle);
    if (clean.includes(" - ")) {
      const parts = clean.split(" - ");
      const artist = parts[0].trim();
      const title = parts.slice(1).join(" - ").trim();
      return { title: title || clean, artist: artist || defaultChannel || "Official Artist" };
    }
    return { title: clean, artist: defaultChannel || "Official Artist" };
  }

  function normalizeMusicTitle(title) {
    return String(title || "")
      .toLowerCase()
      .replace(bracketAnnotationPattern, " ")
      .replace(trailingFeaturePattern, " ")
      .replace(displayAnnotationPattern, " ")
      .replace(nonMusicWordPattern, " ")
      .replace(whitespacePattern, " ")
      .trim();
  }

  function normalizeArtists(artist) {
    return String(artist || "")
      .toLowerCase()
      .replace(artistSeparatorPattern, "|")
      .split("|")
      .map((a) => a.replace(nonMusicWordPattern, " ").replace(whitespacePattern, " ").trim())
      .filter(Boolean);
  }

  function getTrackIdentityKey(track) {
    if (!track) return "";
    const titleKey = normalizeMusicTitle(track.title);
    const artistKey = normalizeArtists(track.artist).sort().join("|");
    if (titleKey && artistKey) return `${artistKey}|${titleKey}`;
    return track.videoId || track.id || "";
  }

  function recordPlaybackHistory(track) {
    if (!track?.title || !track?.videoId) return;
    try {
      const historyKey = "derbis-playback-history";
      const raw = localStorage.getItem(historyKey);
      let list = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(list)) list = [];
      const identity = getTrackIdentityKey(track);
      list = list.filter((item) => getTrackIdentityKey(item) !== identity);
      list.unshift(track);
      if (list.length > 50) list.length = 50;
      localStorage.setItem(historyKey, JSON.stringify(list));
      applyDerbisAlgorithm({ preserveCurrentPlaying: true });
    } catch (e) {
      console.warn("Could not save playback history:", e);
    }
  }

  const HOME_QUICK_PICKS_LIMIT = 20;
  const HOME_QUICK_PICKS_ARTIST_LIMIT = 2;
  const HOME_ORBIT_LIMIT = 8;

  function buildPersonalOrbitAlgorithm({ accountTracks = [], historyTracks = [], catalogTracks = [] }) {
    const tasteSeeds = [...historyTracks, ...accountTracks];
    const topSeedArtists = new Set();
    tasteSeeds.forEach((t) => {
      normalizeArtists(t.artist).forEach((a) => topSeedArtists.add(a));
    });

    const candidates = [...catalogTracks];
    candidates.sort((a, b) => {
      const aArtists = normalizeArtists(a.artist);
      const bArtists = normalizeArtists(b.artist);
      const aMatch = aArtists.some((art) => topSeedArtists.has(art)) ? 1 : 0;
      const bMatch = bArtists.some((art) => topSeedArtists.has(art)) ? 1 : 0;
      return bMatch - aMatch;
    });

    const selected = [];
    const seen = new Set();
    for (const t of candidates) {
      if (selected.length >= HOME_ORBIT_LIMIT) break;
      const idKey = getTrackIdentityKey(t);
      if (!idKey || seen.has(idKey)) continue;
      seen.add(idKey);
      selected.push(t);
    }
    return selected;
  }

  function buildQuickPicksAlgorithm({ accountTracks = [], historyTracks = [], catalogTracks = [], orbitTracks = [] }) {
    const orbitKeys = new Set(orbitTracks.map(getTrackIdentityKey).filter(Boolean));
    const candidates = [
      ...accountTracks,
      ...historyTracks,
      ...catalogTracks
    ];

    const selected = [];
    const seen = new Set();
    const artistCounts = new Map();

    function addCandidate(track, enforceArtistLimit, allowOrbitOverlap) {
      if (selected.length >= HOME_QUICK_PICKS_LIMIT) return;
      if (!track?.videoId || !track?.title) return;
      const identity = getTrackIdentityKey(track);
      if (!identity) return;
      if (!allowOrbitOverlap && orbitKeys.has(identity)) return;
      if (seen.has(identity)) return;

      const primaryArtist = (normalizeArtists(track.artist)[0] || track.artist || "").toLowerCase().trim();
      const count = artistCounts.get(primaryArtist) || 0;
      if (enforceArtistLimit && primaryArtist && count >= HOME_QUICK_PICKS_ARTIST_LIMIT) {
        return;
      }

      seen.add(identity);
      selected.push(track);
      if (primaryArtist) {
        artistCounts.set(primaryArtist, count + 1);
      }
    }

    for (const track of candidates) {
      addCandidate(track, true, false);
    }
    if (selected.length < HOME_QUICK_PICKS_LIMIT) {
      for (const track of candidates) {
        addCandidate(track, false, false);
      }
    }
    if (selected.length < HOME_QUICK_PICKS_LIMIT) {
      for (const track of candidates) {
        addCandidate(track, false, true);
      }
    }

    return selected;
  }

  function buildStudioSpotlightHero({ quickTracks = [], orbitTracks = [], accountTracks = [], historyTracks = [], subscriptions = [] }) {
    const leadCandidates = [currentYouTubeTrack, ...historyTracks, ...accountTracks, ...orbitTracks, ...quickTracks].filter(Boolean);
    let heroCandidate = leadCandidates[0] || DERBIS_CATALOG_TRACKS[0];
    let primaryArtist = (normalizeArtists(heroCandidate.artist)[0] || heroCandidate.artist || "Ariana Grande").toLowerCase();

    let heroProfile = KNOWN_ARTIST_PROFILES[primaryArtist];
    if (!heroProfile) {
      const matchedKey = Object.keys(KNOWN_ARTIST_PROFILES).find((k) => primaryArtist.includes(k) || k.includes(primaryArtist));
      if (matchedKey) {
        heroProfile = KNOWN_ARTIST_PROFILES[matchedKey];
        primaryArtist = matchedKey;
      } else {
        heroProfile = KNOWN_ARTIST_PROFILES["ariana grande"];
      }
    }

    spotlightHeroTrack = heroCandidate;

    const companionArtists = [];
    const companionAvatars = [];
    const seenCompanion = new Set([primaryArtist]);

    subscriptions.forEach((sub) => {
      const subNorm = sub.name.toLowerCase().trim();
      if (!seenCompanion.has(subNorm) && sub.avatar && companionAvatars.length < 3) {
        seenCompanion.add(subNorm);
        companionArtists.push(sub.name);
        companionAvatars.push({ name: sub.name, avatar: sub.avatar });
      }
    });

    for (const track of [...quickTracks, ...orbitTracks, ...DERBIS_CATALOG_TRACKS]) {
      if (companionAvatars.length >= 3) break;
      const tArtists = normalizeArtists(track.artist);
      for (const art of tArtists) {
        if (companionAvatars.length >= 3) break;
        if (!seenCompanion.has(art)) {
          seenCompanion.add(art);
          const prof = KNOWN_ARTIST_PROFILES[art] || Object.values(KNOWN_ARTIST_PROFILES).find((p) => p.name.toLowerCase().includes(art) || art.includes(p.name.toLowerCase()));
          if (prof) {
            companionArtists.push(prof.name);
            companionAvatars.push({ name: prof.name, avatar: prof.avatar });
          }
        }
      }
    }

    if (companionAvatars.length < 3) {
      const defaults = [KNOWN_ARTIST_PROFILES["maki"], KNOWN_ARTIST_PROFILES["billie eilish"], KNOWN_ARTIST_PROFILES["lady gaga"]];
      defaults.forEach((def) => {
        if (companionAvatars.length < 3 && !companionArtists.includes(def.name) && def.name.toLowerCase() !== heroProfile.name.toLowerCase()) {
          companionArtists.push(def.name);
          companionAvatars.push({ name: def.name, avatar: def.avatar });
        }
      });
    }

    const leadNames = [heroProfile.name, ...companionArtists.slice(0, 2)];
    let leadText = `Starts with ${leadNames[0]}`;
    if (leadNames.length === 2) {
      leadText = `Starts with ${leadNames[0]} and ${leadNames[1]}`;
    } else if (leadNames.length >= 3) {
      leadText = `Starts with ${leadNames[0]}, ${leadNames[1]}, and ${leadNames[2]}`;
    }

    if (elements.soundtrackPortraitImg) {
      elements.soundtrackPortraitImg.src = heroProfile.portrait;
      elements.soundtrackPortraitImg.alt = heroProfile.name;
    }
    if (elements.soundtrackGlow && heroProfile.glow) {
      elements.soundtrackGlow.style.background = `radial-gradient(circle, ${heroProfile.glow} 0%, rgba(94,129,244,0.16) 45%, transparent 72%)`;
    }
    if (elements.soundtrackCompanionStack) {
      elements.soundtrackCompanionStack.innerHTML = companionAvatars
        .slice(0, 3)
        .map((c) => `<img class="companion-avatar" src="${escapeHtml(c.avatar)}" alt="${escapeHtml(c.name)}" loading="lazy" />`)
        .join("");
    }
    if (elements.soundtrackLead) {
      elements.soundtrackLead.textContent = leadText;
    }
  }

  function renderQuickPicksShelf(tracks) {
    const shelf = elements.quickShelf || elements.quickGrid || $("#quick-shelf") || $("#quick-grid");
    if (!shelf) return;
    const TRACK_COLUMN_ROWS = 4;
    const columns = [];
    for (let i = 0; i < tracks.length; i += TRACK_COLUMN_ROWS) {
      columns.push(tracks.slice(i, i + TRACK_COLUMN_ROWS));
    }

    shelf.innerHTML = columns
      .map((colTracks, colIdx) => `
        <div class="quick-column" data-column-index="${colIdx}">
          ${colTracks
            .map((t, rowIdx) => {
              const isCurrent = currentYouTubeTrack?.videoId === t.videoId || (!currentYouTubeTrack && colIdx === 0 && rowIdx === 0);
              return `
                <button class="quick-track-row ${isCurrent ? "is-current" : ""}" type="button" data-title="${escapeHtml(t.title)}" data-artist="${escapeHtml(t.artist)}" data-video-id="${escapeHtml(t.videoId)}" data-thumb="${escapeHtml(t.thumbnail)}">
                  <span class="quick-row-thumb">
                    <img src="${escapeHtml(t.thumbnail)}" alt="${escapeHtml(t.title)}" loading="lazy" />
                    <span class="playing-bars" aria-label="Playing" style="display:${isCurrent ? "inline-flex" : "none"};"><i></i><i></i><i></i></span>
                  </span>
                  <span class="quick-row-meta">
                    <strong class="quick-row-title">${escapeHtml(t.title)}</strong>
                    <small class="quick-row-artist">${escapeHtml(t.artist)}</small>
                  </span>
                  <button class="quick-row-more" type="button" aria-label="Song options" data-action="options" data-title="${escapeHtml(t.title)}" data-artist="${escapeHtml(t.artist)}">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><use href="#i-more"/></svg>
                  </button>
                </button>
              `;
            })
            .join("")}
        </div>
      `)
      .join("");
  }
  const renderQuickPicksGrid = renderQuickPicksShelf;

  function renderOrbitRail(tracks) {
    if (!elements.orbitRail) return;
    elements.orbitRail.innerHTML = tracks
      .map((t) => {
        return `
          <button class="orbit-card" type="button" data-title="${escapeHtml(t.title)}" data-artist="${escapeHtml(t.artist)}" data-video-id="${escapeHtml(t.videoId)}" data-thumb="${escapeHtml(t.thumbnail)}">
            <span class="cover has-thumb"><img src="${escapeHtml(t.thumbnail)}" alt="${escapeHtml(t.title)}" loading="lazy" /></span>
            <strong>${escapeHtml(t.title)}</strong>
            <small>${escapeHtml(t.artist)}</small>
          </button>
        `;
      })
      .join("");
  }

  function applyDerbisAlgorithm({ preserveCurrentPlaying = false } = {}) {
    let accountData = null;
    try {
      const raw = localStorage.getItem("derbis-youtube-account-data");
      if (raw) accountData = JSON.parse(raw);
    } catch {}

    let historyTracks = [];
    try {
      const raw = localStorage.getItem("derbis-playback-history");
      if (raw) historyTracks = JSON.parse(raw);
      if (!Array.isArray(historyTracks)) historyTracks = [];
    } catch {}

    const accountTracks = Array.isArray(accountData?.likedTracks) ? accountData.likedTracks : [];
    const subscriptions = Array.isArray(accountData?.subscriptions) ? accountData.subscriptions : [];

    const orbitTracks = buildPersonalOrbitAlgorithm({
      accountTracks,
      historyTracks,
      catalogTracks: DERBIS_CATALOG_TRACKS
    });

    const quickTracks = buildQuickPicksAlgorithm({
      accountTracks,
      historyTracks,
      catalogTracks: DERBIS_CATALOG_TRACKS,
      orbitTracks
    });

    renderOrbitRail(orbitTracks);
    renderQuickPicksGrid(quickTracks);

    buildStudioSpotlightHero({
      quickTracks,
      orbitTracks,
      accountTracks,
      historyTracks,
      subscriptions
    });
  }

  async function syncYouTubeAccountData(token) {
    if (!token) return;
    try {
      const headers = { Authorization: `Bearer ${token}` };
      const [likedRes, playlistsRes, subsRes] = await Promise.all([
        fetch("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=LL&maxResults=25", { headers }).catch(() => null),
        fetch("https://www.googleapis.com/youtube/v3/playlists?part=snippet&mine=true&maxResults=20", { headers }).catch(() => null),
        fetch("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&maxResults=25", { headers }).catch(() => null)
      ]);

      const likedTracks = [];
      if (likedRes && likedRes.ok) {
        const likedData = await likedRes.json();
        (likedData.items || []).forEach((item) => {
          const snip = item.snippet;
          const vid = snip?.resourceId?.videoId || item.contentDetails?.videoId;
          if (vid && snip?.title) {
            const rawTitle = snip.title;
            const channel = snip.videoOwnerChannelTitle || snip.channelTitle || "";
            const { title, artist } = parseYouTubeVideoTitle(rawTitle, channel);
            const thumb = snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`;
            likedTracks.push({ title, artist, videoId: vid, thumbnail: thumb, source: "liked" });
          }
        });
      }

      const subscriptions = [];
      if (subsRes && subsRes.ok) {
        const subsData = await subsRes.json();
        (subsData.items || []).forEach((item) => {
          const snip = item.snippet;
          if (snip?.title) {
            subscriptions.push({
              name: snip.title,
              avatar: snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || "",
              channelId: snip.resourceId?.channelId
            });
          }
        });
      }

      const accountData = {
        likedTracks,
        subscriptions,
        syncedAt: Date.now()
      };

      localStorage.setItem("derbis-youtube-account-data", JSON.stringify(accountData));
      applyDerbisAlgorithm();
      showToast("YouTube account data synced: Quick Picks & My Soundtrack updated.");
    } catch (err) {
      console.warn("YouTube account data sync failed:", err);
    }
  }

  function selectTrack(target) {
    if (target.closest(".quick-row-more, [data-action='options']")) return;
    const button = target.closest("[data-title]");
    if (!button) return;
    const title = button.dataset.title;
    const artist = button.dataset.artist;
    const thumb = button.dataset.thumb || button.querySelector("img")?.src || "";
    const videoId = button.dataset.videoId;
    const art = button.dataset.art || "art-noir";

    recordPlaybackHistory({ title, artist, thumbnail: thumb, thumb, videoId });
    setNowPlaying(title, artist, art, thumb, videoId);

    // Populate upcoming queue with the remaining tracks on the shelf
    const allRows = all(".quick-track-row, .quick-track");
    const currentIndex = allRows.indexOf(button);
    if (currentIndex !== -1 && allRows.length > 1) {
      const remainingRows = [
        ...allRows.slice(currentIndex + 1),
        ...allRows.slice(0, currentIndex)
      ];
      playbackQueue = remainingRows.map((r) => ({
        title: r.dataset.title,
        artist: r.dataset.artist,
        videoId: r.dataset.videoId,
        thumbnail: r.dataset.thumb || r.querySelector("img")?.src || "",
        duration: r.dataset.duration || r.querySelector(".track-duration")?.textContent?.trim() || ""
      })).filter((t) => t.title && t.videoId);
      renderQueue();
    }

    if (videoId) {
      playYouTubeVideo(videoId, { title, artist, thumbnail: thumb, thumb, videoId });
    } else {
      searchYouTube(`${title} ${artist}`);
    }
  }

  function setYouTubeSearchStatus(message, state = "") {
    elements.youtubeSearchStatus.textContent = message;
    elements.youtubeSearchStatus.classList.toggle("is-error", state === "error");
    elements.youtubeSearchStatus.classList.toggle("is-loading", state === "loading");
  }

  function decodeYouTubeText(value) {
    const decoder = document.createElement("textarea");
    decoder.innerHTML = String(value || "");
    return decoder.value;
  }

  function formatIsoDuration(iso) {
    if (!iso) return "";
    const match = String(iso).match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
    if (!match) return "";
    const hours = parseInt(match[1] || "0", 10);
    const minutes = parseInt(match[2] || "0", 10);
    const seconds = parseInt(match[3] || "0", 10);
    const secStr = String(seconds).padStart(2, "0");
    if (hours > 0) {
      return `${hours}:${String(minutes).padStart(2, "0")}:${secStr}`;
    }
    return `${minutes}:${secStr}`;
  }

  function cleanSongTrack(item) {
    const videoId = item?.id?.videoId;
    if (!videoId) return null;
    const snippet = item?.snippet || {};
    const rawTitle = decodeYouTubeText(snippet.title || "Untitled song");
    const rawChannel = decodeYouTubeText(snippet.channelTitle || "Unknown Artist");
    const thumbnail = snippet?.thumbnails?.high?.url || snippet?.thumbnails?.medium?.url || snippet?.thumbnails?.default?.url || "";

    const junkPattern = /\b(choreograph\w*|dance\b|tutorial|reaction|review|interview|behind\s+the\s+scenes|vlog|parody|karaoke|1\s*hour|10\s*hours|slowed|reverb|speed\s*up|sped\s*up|tiktok\s*edit)\b/i;
    if (junkPattern.test(rawTitle)) return null;

    const isTopic = rawChannel.endsWith(" - Topic");
    let artist = isTopic ? rawChannel.slice(0, -8).trim() : rawChannel;
    let title = rawTitle.replace(/[\(\[\{]?(?:official\s+)?(?:music\s+)?(?:audio|video|lyric\s+video|lyrics|hd|hq|visualizer|mv|4k)[\)\]\}]?/gi, "").trim();

    if (!isTopic && title.includes(" - ")) {
      const parts = title.split(" - ");
      if (parts.length >= 2 && parts[0].trim() && parts[1].trim()) {
        artist = parts[0].trim();
        title = parts.slice(1).join(" - ").trim();
      }
    }
    title = title.replace(/\s+/g, " ").replace(/^[-–—\s]+|[-–—\s]+$/g, "").trim();
    if (!title) title = rawTitle;

    let score = 0;
    if (isTopic) score += 20;
    if (/official\s+audio/i.test(rawTitle)) score += 15;
    if (/audio/i.test(rawTitle)) score += 8;
    if (/lyrics?/i.test(rawTitle)) score -= 10;

    return {
      videoId,
      title,
      artist,
      thumbnail,
      score,
      dedupKey: `${title.toLowerCase()}_${artist.toLowerCase()}`
    };
  }

  function processSearchResults(items) {
    const seen = new Set();
    const songs = [];
    for (const item of items) {
      const song = cleanSongTrack(item);
      if (!song) continue;
      if (seen.has(song.dedupKey)) continue;
      seen.add(song.dedupKey);
      songs.push(song);
    }
    songs.sort((a, b) => b.score - a.score);
    return songs.slice(0, 10);
  }

  function renderMultiSectionResults({ songs, artists, albums, playlists }) {
    if (!songs.length && !artists.length && !albums.length && !playlists.length) {
      elements.youtubeSearchResults.innerHTML = "";
      setYouTubeSearchStatus("No matching songs or artists found. Try a different search.");
      return;
    }

    let html = `
      <div class="search-tabs" id="search-tabs" role="tablist" aria-label="Search categories">
        <button class="search-tab is-active" type="button" data-filter="all">All</button>
        ${songs.length ? `<button class="search-tab" type="button" data-filter="songs">Songs</button>` : ""}
        ${artists.length ? `<button class="search-tab" type="button" data-filter="artists">Artists</button>` : ""}
        ${albums.length ? `<button class="search-tab" type="button" data-filter="albums">Albums</button>` : ""}
        ${playlists.length ? `<button class="search-tab" type="button" data-filter="playlists">Playlists</button>` : ""}
      </div>
      <div class="search-sections-wrap">
    `;

    // 1. Songs section (vertical track rows matching user screenshot)
    if (songs.length) {
      html += `
        <section class="search-section" data-section="songs">
          <h2 class="search-section-heading">Songs</h2>
          <div class="search-song-list">
            ${songs.map((song) => `
              <button class="search-track-row" type="button" data-youtube-video="${escapeHtml(song.videoId)}" data-youtube-title="${escapeHtml(song.title)}" data-youtube-artist="${escapeHtml(song.artist)}" data-youtube-thumbnail="${escapeHtml(song.thumbnail)}" data-youtube-duration="${escapeHtml(song.duration || "")}" aria-label="Play ${escapeHtml(song.title)} by ${escapeHtml(song.artist)}">
                <img class="search-track-art" src="${escapeHtml(song.thumbnail)}" alt="" loading="lazy" referrerpolicy="no-referrer" />
                <div class="search-track-info">
                  <strong class="search-track-title">${escapeHtml(song.title)}</strong>
                  <span class="search-track-artist">${escapeHtml(song.artist)}</span>
                </div>
                ${song.duration ? `<span class="search-track-duration">${escapeHtml(song.duration)}</span>` : ""}
              </button>
            `).join("")}
          </div>
        </section>
      `;
    }

    // 2. Artists section
    if (artists.length) {
      html += `
        <section class="search-section" data-section="artists">
          <h2 class="search-section-heading">Artists</h2>
          <div class="search-artist-grid">
            ${artists.map((artist) => `
              <button class="search-artist-card" type="button" data-genre-search="${escapeHtml(artist.title)} songs" aria-label="Explore songs by ${escapeHtml(artist.title)}">
                <img class="search-artist-img" src="${escapeHtml(artist.thumb)}" alt="" loading="lazy" referrerpolicy="no-referrer" />
                <strong>${escapeHtml(artist.title)}</strong>
                <span>Artist</span>
              </button>
            `).join("")}
          </div>
        </section>
      `;
    }

    // 3. Albums section
    if (albums.length) {
      html += `
        <section class="search-section" data-section="albums">
          <h2 class="search-section-heading">Albums</h2>
          <div class="search-album-grid">
            ${albums.map((album) => `
              <button class="search-album-card" type="button" data-genre-search="${escapeHtml(album.title)} ${escapeHtml(album.artist)}" aria-label="Explore album ${escapeHtml(album.title)}">
                <img class="search-album-img" src="${escapeHtml(album.thumb)}" alt="" loading="lazy" referrerpolicy="no-referrer" />
                <strong>${escapeHtml(album.title)}</strong>
                <span>${escapeHtml(album.artist)}</span>
              </button>
            `).join("")}
          </div>
        </section>
      `;
    }

    // 4. Playlists section
    if (playlists.length) {
      html += `
        <section class="search-section" data-section="playlists">
          <h2 class="search-section-heading">Playlists</h2>
          <div class="search-playlist-grid">
            ${playlists.map((playlist) => `
              <button class="search-playlist-card" type="button" data-genre-search="${escapeHtml(playlist.title)}" aria-label="Explore playlist ${escapeHtml(playlist.title)}">
                <img class="search-playlist-img" src="${escapeHtml(playlist.thumb)}" alt="" loading="lazy" referrerpolicy="no-referrer" />
                <strong>${escapeHtml(playlist.title)}</strong>
                <span>${escapeHtml(playlist.author || "Playlist")}</span>
              </button>
            `).join("")}
          </div>
        </section>
      `;
    }

    html += `</div>`;
    elements.youtubeSearchResults.innerHTML = html;
    setYouTubeSearchStatus("Select a song to play in Derbis.");

    // Wire up song playback
    all("[data-youtube-video]").forEach((button, bIdx, btnArr) => button.addEventListener("click", () => {
      elements.youtubeUrl.value = button.dataset.youtubeVideo;
      const clickedTrack = {
        title: button.dataset.youtubeTitle,
        artist: button.dataset.youtubeArtist,
        thumbnail: button.dataset.youtubeThumbnail,
        duration: button.dataset.youtubeDuration,
        videoId: button.dataset.youtubeVideo
      };
      // Queue subsequent search songs
      const subsequent = btnArr.slice(bIdx + 1).map((b) => ({
        title: b.dataset.youtubeTitle,
        artist: b.dataset.youtubeArtist,
        thumbnail: b.dataset.youtubeThumbnail,
        duration: b.dataset.youtubeDuration,
        videoId: b.dataset.youtubeVideo
      })).filter((t) => t.title && t.videoId);
      if (subsequent.length) {
        playbackQueue = subsequent;
        renderQueue();
      }
      playYouTubeVideo(button.dataset.youtubeVideo, clickedTrack);
    }));

    // Wire up artist/album/playlist searches
    all(".search-artist-card, .search-album-card, .search-playlist-card").forEach((button) => button.addEventListener("click", () => {
      if (button.dataset.genreSearch) searchYouTube(button.dataset.genreSearch);
    }));

    // Wire up tabs
    all(".search-tab").forEach((tab) => tab.addEventListener("click", () => {
      const filter = tab.dataset.filter;
      all(".search-tab").forEach((t) => t.classList.toggle("is-active", t === tab));
      all(".search-section").forEach((sec) => {
        sec.hidden = filter !== "all" && sec.dataset.section !== filter;
      });
    }));
  }

  const CURATED_GENRE_TRACKS = {
    pop: {
      songs: [
        { title: "Cruel Summer", artist: "Taylor Swift", videoId: "ic8j13piAhQ", duration: "2:58", thumbnail: "https://i.ytimg.com/vi/ic8j13piAhQ/hqdefault.jpg" },
        { title: "Love Me Harder", artist: "Ariana Grande ft. The Weeknd", videoId: "g5qU7p7yOY8", duration: "3:56", thumbnail: "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg" },
        { title: "Espresso", artist: "Sabrina Carpenter", videoId: "eVli-tstM5E", duration: "2:55", thumbnail: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg" },
        { title: "Levitating", artist: "Dua Lipa", videoId: "TUVcZfQe-Kw", duration: "3:23", thumbnail: "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg" },
        { title: "Taste", artist: "Sabrina Carpenter", videoId: "kJQP7kiw5Fk", duration: "2:37", thumbnail: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" }
      ],
      artists: [
        { title: "Taylor Swift", thumb: "https://yt3.googleusercontent.com/5cW4-NDteLLBaWpsjSirr7EmaOKtFGyX8PG2-6Xx7RELneLZe50Y5Bv9oEothnH9BOqxUpClfA=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Ariana Grande", thumb: "https://yt3.ggpht.com/u2o5_V4WeYMDebsryzMFHN6OBoXZsMjfpOSguscVEAVS6MKYHowaXnPjfZmzaRSr1jqtjiCowg=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Dua Lipa", thumb: "https://yt3.googleusercontent.com/c3upBFWLu55hnBvqncQS9ZEF_hkvHsNTQiB7m7ZYYavLFMzfyn9Bwo-1VF4HSPGo3G2EdwGtgWg=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    rock: {
      songs: [
        { title: "Bohemian Rhapsody", artist: "Queen", videoId: "fJ9rUzIMcZQ", duration: "5:55", thumbnail: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg" },
        { title: "Viva La Vida", artist: "Coldplay", videoId: "dvgZkm1xWPE", duration: "4:02", thumbnail: "https://i.ytimg.com/vi/dvgZkm1xWPE/hqdefault.jpg" },
        { title: "In the End", artist: "Linkin Park", videoId: "eVTXPUF4Oz4", duration: "3:36", thumbnail: "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg" },
        { title: "Smells Like Teen Spirit", artist: "Nirvana", videoId: "hTWKbfoikeg", duration: "4:38", thumbnail: "https://i.ytimg.com/vi/hTWKbfoikeg/hqdefault.jpg" }
      ],
      artists: [
        { title: "Queen", thumb: "https://yt3.googleusercontent.com/MiFTTCMl22bQ46F91rXFVhZ7PnBfLujsRWNxMik7NKVRDRBc-uBE7fba_3r9vTN39JvfRmh8nVU=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Coldplay", thumb: "https://yt3.googleusercontent.com/vWWn3HO1emNl2iGb08UuY4q_OtqXwUUWJMjMDr6bBPpuCBoWg-dE_Gu-yoMQNrBqb2gMzA3wLQ=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Linkin Park", thumb: "https://yt3.googleusercontent.com/OtUf050j6w6iRPUmGgmzblD2saluxUusxiVmo0-SMUnWiNKwR89JL_-xJ6JvIEram8UbPT_zDA=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    jam: {
      songs: [
        { title: "Gravity (Live)", artist: "John Mayer", videoId: "dBFW8OvciIU", duration: "5:48", thumbnail: "https://i.ytimg.com/vi/dBFW8OvciIU/hqdefault.jpg" },
        { title: "Touch of Grey", artist: "Grateful Dead", videoId: "mzvk0fWtCs0", duration: "5:47", thumbnail: "https://i.ytimg.com/vi/mzvk0fWtCs0/hqdefault.jpg" },
        { title: "Crash Into Me", artist: "Dave Matthews Band", videoId: "k7in-9E3ImQ", duration: "5:16", thumbnail: "https://i.ytimg.com/vi/k7in-9E3ImQ/hqdefault.jpg" },
        { title: "Ripple", artist: "Grateful Dead", videoId: "CFsbAuX9P4w", duration: "4:09", thumbnail: "https://i.ytimg.com/vi/CFsbAuX9P4w/hqdefault.jpg" }
      ],
      artists: [
        { title: "John Mayer", thumb: "https://yt3.googleusercontent.com/IrtssmU5WpTdgnAC3r-YNq_WDGujO8025uptIyQAj_qSL8I5w1NliNEGKvvtm9fWL3xVX6kmrQ=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Grateful Dead", thumb: "https://yt3.googleusercontent.com/kn1ugrZ4Nv2pGEKa7Oj1l01gcyR0c97Epx7cbsdiV7hwd2clnjocV_K9iyNIH8X5NJO2wFbuWw=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Dave Matthews Band", thumb: "https://yt3.googleusercontent.com/7Y5Hye_NzWoGQ_dmlWP33U70rG6JtwNeyi0-FIB9TECM2WFW1CJMP98mBrpO0uV0Ejjhteq_4A=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    hiphop: {
      songs: [
        { title: "HUMBLE.", artist: "Kendrick Lamar", videoId: "tvTRZJ-4EyI", duration: "2:57", thumbnail: "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg" },
        { title: "SICKO MODE", artist: "Travis Scott", videoId: "6ONRf7h3Mdk", duration: "5:12", thumbnail: "https://i.ytimg.com/vi/6ONRf7h3Mdk/hqdefault.jpg" },
        { title: "Lose Yourself", artist: "Eminem", videoId: "_Yhyp-_hX2s", duration: "5:26", thumbnail: "https://i.ytimg.com/vi/_Yhyp-_hX2s/hqdefault.jpg" },
        { title: "Without Me", artist: "Eminem", videoId: "YVkUvmDQ3HY", duration: "4:50", thumbnail: "https://i.ytimg.com/vi/YVkUvmDQ3HY/hqdefault.jpg" }
      ],
      artists: [
        { title: "Kendrick Lamar", thumb: "https://yt3.googleusercontent.com/ytc/AIdro_mJoVPMBjJ0WNhRgNoQeztGbf1fbHjWlrP72VWlTr0rBg=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Travis Scott", thumb: "https://yt3.googleusercontent.com/ytc/AIdro_lYT_V7ztsYEvILayV7Ey_fgzx2VYpeLJxFXf1TO0rjPH8=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Eminem", thumb: "https://yt3.googleusercontent.com/8yd6Ce-sUyDjr4SbcpSrFGHK7lgDIzT24_uAet_h5A7SkKIotSzeSyseAuwuHBsjKpdDR9RIm-s=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    rnb: {
      songs: [
        { title: "Kill Bill", artist: "SZA", videoId: "MSRcC626prw", duration: "2:33", thumbnail: "https://i.ytimg.com/vi/MSRcC626prw/hqdefault.jpg" },
        { title: "Blinding Lights", artist: "The Weeknd", videoId: "4NRXx6U8ABQ", duration: "3:20", thumbnail: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
        { title: "Starboy", artist: "The Weeknd ft. Daft Punk", videoId: "34Na4j8AVgA", duration: "3:50", thumbnail: "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg" },
        { title: "Die With A Smile", artist: "Lady Gaga & Bruno Mars", videoId: "kPa7bsKwL-c", duration: "4:11", thumbnail: "https://i.ytimg.com/vi/kPa7bsKwL-c/hqdefault.jpg" }
      ],
      artists: [
        { title: "SZA", thumb: "https://yt3.googleusercontent.com/yLBjfGExL_iEyNmOd5VjEVt6tQWg8Upr1mpafHQfsv-MU3875DnCI74VsslG0lZtiGjg0lf1wTk=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "The Weeknd", thumb: "https://yt3.ggpht.com/EuRWZeic2Q6xLGGxm-VihpDFHAS790YJXHwnnUnYBubP1alIO8rmsbUhaQT2Cz-zzZ28HEo7pg=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Frank Ocean", thumb: "https://yt3.ggpht.com/cEJPESZMHmLtSJblh9s7V5b_Xw7jv0LrHCTMyK-6L3S4noWl_RDySI1Ywkg5e3o0EkXFZjSsJQ=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    opm: {
      songs: [
        { title: "Pantropiko", artist: "BINI", videoId: "Zx31bB2vMns", duration: "3:45", thumbnail: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg" },
        { title: "kahel na langit", artist: "Maki", videoId: "DuKb1yF5Wh4", duration: "4:01", thumbnail: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg" },
        { title: "Dilaw", artist: "Maki", videoId: "L215z9C4Zd8", duration: "3:10", thumbnail: "https://i.ytimg.com/vi/L215z9C4Zd8/hqdefault.jpg" },
        { title: "GENTO", artist: "SB19", videoId: "VZZA_38RUBI", duration: "3:52", thumbnail: "https://i.ytimg.com/vi/VZZA_38RUBI/hqdefault.jpg" },
        { title: "Salamin, Salamin", artist: "BINI", videoId: "J1Ip2sC_lss", duration: "3:50", thumbnail: "https://i.ytimg.com/vi/J1Ip2sC_lss/hqdefault.jpg" }
      ],
      artists: [
        { title: "BINI", thumb: "https://yt3.ggpht.com/z7Z3zm2O1_pHcFBcZW0DBfME5xlVmgfiMoWlw9LqGYybjiIzHqnbZoAG-ApCHmcBQ12H1TFN-uU=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Maki", thumb: "https://yt3.ggpht.com/FoD6VmzaE4nKTPEKR0HXYH9GAaoFhGRifUJW6T9tneMfHjRMMvr_ToomsP5TDDTFxPAWiWp_=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "SB19", thumb: "https://yt3.ggpht.com/Lq6oaEj_0jHHYdZezF5_wQuWpmc49sWyL1X1c-_cSmdltb83ZW8oO4qa-t4BQT6VKiuTLdTaxAg=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    indie: {
      songs: [
        { title: "BIRDS OF A FEATHER", artist: "Billie Eilish", videoId: "V9PVRfjEBTI", duration: "3:13", thumbnail: "https://i.ytimg.com/vi/V9PVRfjEBTI/hqdefault.jpg" },
        { title: "vampire", artist: "Olivia Rodrigo", videoId: "ZmDBbnmKpqQ", duration: "3:39", thumbnail: "https://i.ytimg.com/vi/ZmDBbnmKpqQ/hqdefault.jpg" },
        { title: "good 4 u", artist: "Olivia Rodrigo", videoId: "gNi_6U5Pm_o", duration: "2:58", thumbnail: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg" },
        { title: "bad guy", artist: "Billie Eilish", videoId: "DyDfgMOUjCI", duration: "3:14", thumbnail: "https://i.ytimg.com/vi/DyDfgMOUjCI/hqdefault.jpg" }
      ],
      artists: [
        { title: "Billie Eilish", thumb: "https://yt3.ggpht.com/cCf3P5ZRQ5f0D69oXzAg6LUI6gazIPLhIohervVyg0JdGrDfo5MUCzU_qujvGruLVWwtVtbf6Q=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Olivia Rodrigo", thumb: "https://yt3.ggpht.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=s800-c-k-c0x00ffffff-no-rj" },
        { title: "keshi", thumb: "https://yt3.ggpht.com/Fms_QqC83TEej3mcHa8qy41s2rrSX3WeNtNCPDrbJ2EYWb_K7uBtgMRAhq5rQFVd_v19y6Qi=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    electro: {
      songs: [
        { title: "One Kiss", artist: "Calvin Harris, Dua Lipa", videoId: "DkeiKbqa02g", duration: "3:34", thumbnail: "https://i.ytimg.com/vi/DkeiKbqa02g/hqdefault.jpg" },
        { title: "Summer", artist: "Calvin Harris", videoId: "ebXbLfLACGM", duration: "3:42", thumbnail: "https://i.ytimg.com/vi/ebXbLfLACGM/hqdefault.jpg" },
        { title: "Starboy", artist: "The Weeknd ft. Daft Punk", videoId: "34Na4j8AVgA", duration: "3:50", thumbnail: "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg" }
      ],
      artists: [
        { title: "Calvin Harris", thumb: "https://yt3.ggpht.com/ouLbox-YbCuUOQCqTnjNpHifrrcWHb88ib44Kv5nYVSKoeQR4OflWM1sERnPNICGLROLcnlIJg=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Peggy Gou", thumb: "https://yt3.ggpht.com/ko1eSag1s_0E_C65UFduoTy0CbwcFvl6GcsewDR77aR3gTlJnlMNgnTqViV1UEaj0Bw64GsF3pU=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Fred again..", thumb: "https://yt3.ggpht.com/GE02xhztGfN-uFAgLcECHUg_Q4dsAx4wI9ceipru59gd2atY1Kom10seh2byGW2p0l3w5rRZ1A=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    chill: {
      songs: [
        { title: "Glimpse of Us", artist: "Joji", videoId: "FvOpPeKSf_4", duration: "3:53", thumbnail: "https://i.ytimg.com/vi/FvOpPeKSf_4/hqdefault.jpg" },
        { title: "LIMBO", artist: "keshi", videoId: "W8a4W8w9mR4", duration: "3:32", thumbnail: "https://i.ytimg.com/vi/W8a4W8w9mR4/hqdefault.jpg" },
        { title: "From the Start", artist: "Laufey", videoId: "lSD_L-x77Po", duration: "2:49", thumbnail: "https://i.ytimg.com/vi/lSD_L-x77Po/hqdefault.jpg" }
      ],
      artists: [
        { title: "Joji", thumb: "https://yt3.ggpht.com/4x9M18imJnFNUG71pOsVy8iL_Klz7_WCoIK3-3UJ_XCU1WjAkq8Oqts_cbHKeLDw0Awej-_rHQ=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "keshi", thumb: "https://yt3.ggpht.com/Fms_QqC83TEej3mcHa8qy41s2rrSX3WeNtNCPDrbJ2EYWb_K7uBtgMRAhq5rQFVd_v19y6Qi=s800-c-k-c0xffffffff-no-rj-mo" },
        { title: "Laufey", thumb: "https://yt3.ggpht.com/KoMKgKlz4DlQLPqWvGIvV6hsDR3CTbPoMLdTDAT1mExwHFzH1aasBdvz_9inH1lygCNEzCLQ=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    kpop: {
      songs: [
        { title: "Super Shy", artist: "NewJeans", videoId: "ArmDp-zijuc", duration: "2:34", thumbnail: "https://i.ytimg.com/vi/ArmDp-zijuc/hqdefault.jpg" },
        { title: "Hype Boy", artist: "NewJeans", videoId: "11cta61Wi0g", duration: "2:59", thumbnail: "https://i.ytimg.com/vi/11cta61Wi0g/hqdefault.jpg" },
        { title: "How You Like That", artist: "BLACKPINK", videoId: "ioNng23DkIM", duration: "3:01", thumbnail: "https://i.ytimg.com/vi/ioNng23DkIM/hqdefault.jpg" },
        { title: "Dynamite", artist: "BTS", videoId: "gdZLi9oWNZg", duration: "3:19", thumbnail: "https://i.ytimg.com/vi/gdZLi9oWNZg/hqdefault.jpg" }
      ],
      artists: [
        { title: "NewJeans", thumb: "https://yt3.googleusercontent.com/seIPyT29v_T3KcqQ1aHoAC8qHBVH4Pz4pPa1qpZL623ypce3of75-uA5US-TZyk5WwZLUO0n3A=s800-c-k-c0x00ffffff-no-rj" },
        { title: "BLACKPINK", thumb: "https://yt3.googleusercontent.com/U3VrCkKjzTpQ3VYv4SCPjNfDHeJV-swGNnhLYhr0nV4lZz_GVUNzK4EB-HFRfKv9S5VNh14uAg=s800-c-k-c0x00ffffff-no-rj" },
        { title: "BTS", thumb: "https://yt3.googleusercontent.com/NNG93LntJGDpMlGYIhavu2n4gREs7CNbNT8jjG-7dpotBa5uQDHhuOL8e_oU4ihzWhlwdCmsJw0=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    country: {
      songs: [
        { title: "Something in the Orange", artist: "Zach Bryan", videoId: "lA8F9sIhGdg", duration: "3:48", thumbnail: "https://i.ytimg.com/vi/lA8F9sIhGdg/hqdefault.jpg" },
        { title: "Last Night", artist: "Morgan Wallen", videoId: "b1kc4eC1w1k", duration: "2:43", thumbnail: "https://i.ytimg.com/vi/b1kc4eC1w1k/hqdefault.jpg" },
        { title: "Stick Season", artist: "Noah Kahan", videoId: "g_V76v8Lhbg", duration: "3:02", thumbnail: "https://i.ytimg.com/vi/g_V76v8Lhbg/hqdefault.jpg" }
      ],
      artists: [
        { title: "Zach Bryan", thumb: "https://yt3.googleusercontent.com/kZ-tPheXtCBcKzOxyGEpGM8SwcxYYChkjdYVq4pAnv2fOnhAAlrXbV9EJi5ml_Iv4uTT6dIZ3w=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Morgan Wallen", thumb: "https://yt3.googleusercontent.com/Z3VsV2Xn09OykpfcXtgNKaUfcA9mlGtDlokxSUkIBfemZciBvg4Q116ZdjDM6NFDHaUcEwUW6Q=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Noah Kahan", thumb: "https://yt3.googleusercontent.com/6XXnm3gVcQjrZQoMg9roYAQw9kqDY8ICH8Qw2PPCu5el_17MgZOLctoKcESs7yTS5Q1ykiqK=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    metal: {
      songs: [
        { title: "Enter Sandman", artist: "Metallica", videoId: "CD-E-LDc384", duration: "5:32", thumbnail: "https://i.ytimg.com/vi/CD-E-LDc384/hqdefault.jpg" },
        { title: "Master of Puppets", artist: "Metallica", videoId: "E0ozmU9cJDg", duration: "8:35", thumbnail: "https://i.ytimg.com/vi/E0ozmU9cJDg/hqdefault.jpg" },
        { title: "In the End", artist: "Linkin Park", videoId: "eVTXPUF4Oz4", duration: "3:36", thumbnail: "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg" }
      ],
      artists: [
        { title: "Metallica", thumb: "https://yt3.googleusercontent.com/fK2IIcXxpselcbHY6CfS5wadTo51L8kUkRTzvysvZDUGuFNBimIgnsiKu7gz6GhvESySpCAQCw=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Linkin Park", thumb: "https://yt3.googleusercontent.com/OtUf050j6w6iRPUmGgmzblD2saluxUusxiVmo0-SMUnWiNKwR89JL_-xJ6JvIEram8UbPT_zDA=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Queen", thumb: "https://yt3.googleusercontent.com/MiFTTCMl22bQ46F91rXFVhZ7PnBfLujsRWNxMik7NKVRDRBc-uBE7fba_3r9vTN39JvfRmh8nVU=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    jazz: {
      songs: [
        { title: "So What", artist: "Miles Davis", videoId: "ylXk1LBvIqU", duration: "9:22", thumbnail: "https://i.ytimg.com/vi/ylXk1LBvIqU/hqdefault.jpg" },
        { title: "Don't Know Why", artist: "Norah Jones", videoId: "tO4dxvguQDk", duration: "3:05", thumbnail: "https://i.ytimg.com/vi/tO4dxvguQDk/hqdefault.jpg" },
        { title: "Gravity (Live)", artist: "John Mayer", videoId: "dBFW8OvciIU", duration: "5:48", thumbnail: "https://i.ytimg.com/vi/dBFW8OvciIU/hqdefault.jpg" }
      ],
      artists: [
        { title: "Miles Davis", thumb: "https://yt3.googleusercontent.com/H4NIbxVIIA5bubjmg9LeCHefQuR-TMQikw4-CKcyIBJ4LZeyvRPn_LTlnNKHTwasbZOXE2mq=s800-c-k-c0x00ffffff-no-rj" },
        { title: "John Mayer", thumb: "https://yt3.googleusercontent.com/IrtssmU5WpTdgnAC3r-YNq_WDGujO8025uptIyQAj_qSL8I5w1NliNEGKvvtm9fWL3xVX6kmrQ=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    acoustic: {
      songs: [
        { title: "Thinking Out Loud", artist: "Ed Sheeran", videoId: "lp-EO5I60KA", duration: "4:41", thumbnail: "https://i.ytimg.com/vi/lp-EO5I60KA/hqdefault.jpg" },
        { title: "Say You Won't Let Go", artist: "James Arthur", videoId: "0yW7w8F2TVA", duration: "3:31", thumbnail: "https://i.ytimg.com/vi/0yW7w8F2TVA/hqdefault.jpg" },
        { title: "Riptide", artist: "Vance Joy", videoId: "uJ_1HMAGb4k", duration: "3:24", thumbnail: "https://i.ytimg.com/vi/uJ_1HMAGb4k/hqdefault.jpg" }
      ],
      artists: [
        { title: "Ed Sheeran", thumb: "https://yt3.googleusercontent.com/pZQ5JMD4EOI8TcNYAPTzMexe_fC0CKnb_hYlV4rPfIzmDidF239fH1XKmzkeT30XSg7fxNwc_w=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Olivia Rodrigo", thumb: "https://yt3.ggpht.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=s800-c-k-c0x00ffffff-no-rj" }
      ]
    },
    party: {
      songs: [
        { title: "24K Magic", artist: "Bruno Mars", videoId: "UqyT8IEBkvY", duration: "3:46", thumbnail: "https://i.ytimg.com/vi/UqyT8IEBkvY/hqdefault.jpg" },
        { title: "Uptown Funk", artist: "Mark Ronson ft. Bruno Mars", videoId: "OPf0YbXqDm0", duration: "4:30", thumbnail: "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg" },
        { title: "Timber", artist: "Pitbull ft. Ke$ha", videoId: "hHUbLv4ThOo", duration: "3:24", thumbnail: "https://i.ytimg.com/vi/hHUbLv4ThOo/hqdefault.jpg" }
      ],
      artists: [
        { title: "Bruno Mars", thumb: "https://yt3.googleusercontent.com/dxwzp4IYsNJKDcwNYgF0UGdw9vCWSMqkHaLtC08VVKg8eybNs69pbdTaGdoaKQYHVQjFyeybzA=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Dua Lipa", thumb: "https://yt3.googleusercontent.com/c3upBFWLu55hnBvqncQS9ZEF_hkvHsNTQiB7m7ZYYavLFMzfyn9Bwo-1VF4HSPGo3G2EdwGtgWg=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Calvin Harris", thumb: "https://yt3.ggpht.com/ouLbox-YbCuUOQCqTnjNpHifrrcWHb88ib44Kv5nYVSKoeQR4OflWM1sERnPNICGLROLcnlIJg=s800-c-k-c0xffffffff-no-rj-mo" }
      ]
    },
    workout: {
      songs: [
        { title: "Till I Collapse", artist: "Eminem", videoId: "gY9C0ItyO0k", duration: "4:57", thumbnail: "https://i.ytimg.com/vi/gY9C0ItyO0k/hqdefault.jpg" },
        { title: "HUMBLE.", artist: "Kendrick Lamar", videoId: "tvTRZJ-4EyI", duration: "2:57", thumbnail: "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg" },
        { title: "Titanium", artist: "David Guetta ft. Sia", videoId: "JRfuAukYTKg", duration: "4:05", thumbnail: "https://i.ytimg.com/vi/JRfuAukYTKg/hqdefault.jpg" }
      ],
      artists: [
        { title: "Eminem", thumb: "https://yt3.googleusercontent.com/8yd6Ce-sUyDjr4SbcpSrFGHK7lgDIzT24_uAet_h5A7SkKIotSzeSyseAuwuHBsjKpdDR9RIm-s=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Kendrick Lamar", thumb: "https://yt3.googleusercontent.com/ytc/AIdro_mJoVPMBjJ0WNhRgNoQeztGbf1fbHjWlrP72VWlTr0rBg=s800-c-k-c0x00ffffff-no-rj" },
        { title: "Dua Lipa", thumb: "https://yt3.googleusercontent.com/c3upBFWLu55hnBvqncQS9ZEF_hkvHsNTQiB7m7ZYYavLFMzfyn9Bwo-1VF4HSPGo3G2EdwGtgWg=s800-c-k-c0x00ffffff-no-rj" }
      ]
    }
  };

  async function searchYouTube(query, categoryKey = "") {
    const normalizedQuery = String(query || "").trim();
    if (!normalizedQuery) {
      setYouTubeSearchStatus("Enter a song, artist, or track to search.", "error");
      return;
    }
    goTo("explore");
    elements.youtubeSearchInput.value = normalizedQuery;

    if (!categoryKey) {
      const lower = normalizedQuery.toLowerCase();
      for (const k of Object.keys(CURATED_GENRE_TRACKS)) {
        if (lower.includes(k)) {
          categoryKey = k;
          break;
        }
      }
    }

    if (!youtubeDataApiKey && !activeAccessToken) {
      if (categoryKey && CURATED_GENRE_TRACKS[categoryKey]) {
        const catData = CURATED_GENRE_TRACKS[categoryKey];
        renderMultiSectionResults({
          songs: catData.songs || [],
          artists: catData.artists || [],
          albums: [],
          playlists: []
        });
        setYouTubeSearchStatus(`Top official songs for “${normalizedQuery}” · Connect Google or enter a YouTube API key in Settings to search all of YouTube.`);
        setTimeout(() => {
          elements.youtubeSearchResults?.scrollIntoView({ behavior: "smooth", block: "start" });
        }, 80);
        return;
      }
      pendingYouTubeSearch = normalizedQuery;
      setYouTubeSearchStatus("Connect Google to search songs.", "loading");
      requestConnection();
      return;
    }
    youtubeSearchAbort?.abort();
    youtubeSearchAbort = new AbortController();
    setYouTubeSearchStatus(`Searching songs for “${normalizedQuery}”…`, "loading");
    elements.youtubeSearchResults.innerHTML = "";

    try {
      const songParams = new URLSearchParams({
        part: "snippet",
        type: "video",
        videoCategoryId: "10",
        videoEmbeddable: "true",
        videoSyndicated: "true",
        maxResults: "12",
        q: normalizedQuery
      });
      const artistParams = new URLSearchParams({
        part: "snippet",
        type: "channel",
        maxResults: "4",
        q: normalizedQuery
      });
      const albumParams = new URLSearchParams({
        part: "snippet",
        type: "playlist",
        maxResults: "4",
        q: `${normalizedQuery} album`
      });
      const playlistParams = new URLSearchParams({
        part: "snippet",
        type: "playlist",
        maxResults: "4",
        q: `${normalizedQuery} playlist`
      });

      if (youtubeDataApiKey) {
        songParams.set("key", youtubeDataApiKey);
        artistParams.set("key", youtubeDataApiKey);
        albumParams.set("key", youtubeDataApiKey);
        playlistParams.set("key", youtubeDataApiKey);
      }

      const headers = youtubeDataApiKey ? {} : { Authorization: `Bearer ${activeAccessToken}` };

      const [songsRes, artistsRes, albumsRes, playlistsRes] = await Promise.allSettled([
        fetch(`https://www.googleapis.com/youtube/v3/search?${songParams}`, { headers, signal: youtubeSearchAbort.signal }),
        fetch(`https://www.googleapis.com/youtube/v3/search?${artistParams}`, { headers, signal: youtubeSearchAbort.signal }),
        fetch(`https://www.googleapis.com/youtube/v3/search?${albumParams}`, { headers, signal: youtubeSearchAbort.signal }),
        fetch(`https://www.googleapis.com/youtube/v3/search?${playlistParams}`, { headers, signal: youtubeSearchAbort.signal })
      ]);

      if (songsRes.status === "rejected" || !songsRes.value.ok) {
        throw new Error("Song search failed");
      }

      const songPayload = await songsRes.value.json();
      const rawSongs = processSearchResults(songPayload.items || []);

      // Batch-fetch durations for these songs
      const videoIds = rawSongs.map((s) => s.videoId).filter(Boolean);
      const durations = {};
      if (videoIds.length) {
        try {
          const detailParams = new URLSearchParams({
            part: "contentDetails",
            id: videoIds.join(",")
          });
          if (youtubeDataApiKey) detailParams.set("key", youtubeDataApiKey);
          const detailRes = await fetch(`https://www.googleapis.com/youtube/v3/videos?${detailParams}`, {
            headers,
            signal: youtubeSearchAbort.signal
          });
          if (detailRes.ok) {
            const detailPayload = await detailRes.json();
            (detailPayload.items || []).forEach((item) => {
              if (item?.id && item.contentDetails?.duration) {
                durations[item.id] = formatIsoDuration(item.contentDetails.duration);
              }
            });
          }
        } catch {
          // Durations are optional enrichment; proceed smoothly if unavailable
        }
      }

      rawSongs.forEach((song) => {
        song.duration = durations[song.videoId] || "";
      });

      // Parse artists
      let artists = [];
      if (artistsRes.status === "fulfilled" && artistsRes.value.ok) {
        try {
          const payload = await artistsRes.value.json();
          artists = (payload.items || []).map((it) => {
            const snip = it.snippet || {};
            const title = decodeYouTubeText(snip.title || "Artist").replace(/ - Topic$/, "");
            const thumb = snip.thumbnails?.medium?.url || snip.thumbnails?.default?.url || "";
            return { title, thumb };
          }).filter((a) => a.title);
        } catch {}
      }

      // Parse albums
      let albums = [];
      if (albumsRes.status === "fulfilled" && albumsRes.value.ok) {
        try {
          const payload = await albumsRes.value.json();
          albums = (payload.items || []).map((it) => {
            const snip = it.snippet || {};
            const title = decodeYouTubeText(snip.title || "Album");
            const artist = decodeYouTubeText(snip.channelTitle || "");
            const thumb = snip.thumbnails?.medium?.url || snip.thumbnails?.default?.url || "";
            return { title, artist, thumb };
          }).filter((a) => a.title);
        } catch {}
      }

      // Parse playlists
      let playlists = [];
      if (playlistsRes.status === "fulfilled" && playlistsRes.value.ok) {
        try {
          const payload = await playlistsRes.value.json();
          playlists = (payload.items || []).map((it) => {
            const snip = it.snippet || {};
            const title = decodeYouTubeText(snip.title || "Playlist");
            const author = decodeYouTubeText(snip.channelTitle || "");
            const thumb = snip.thumbnails?.medium?.url || snip.thumbnails?.default?.url || "";
            return { title, author, thumb };
          }).filter((p) => p.title);
        } catch {}
      }

      renderMultiSectionResults({
        songs: rawSongs,
        artists,
        albums,
        playlists
      });
    } catch (error) {
      if (error.name === "AbortError") return;
      console.error(error);
      setYouTubeSearchStatus("Song search is unavailable right now. Try again shortly.", "error");
    }
  }

  function handleSearch() {
    const query = elements.search.value.trim();
    if (!query) return;
    elements.search.blur();
    searchYouTube(query);
  }

  function bindEvents() {
    elements.menuToggle.addEventListener("click", openMenu);
    elements.closeMenu.addEventListener("click", closeMenu);
    elements.sidebarScrim.addEventListener("click", closeMenu);
    all("[data-page]").forEach((button) => button.addEventListener("click", () => goTo(button.dataset.page)));
    all("[data-toast]").forEach((button) => button.addEventListener("click", () => showToast(button.dataset.toast)));
    all(".mood-chip").forEach((chip) => chip.addEventListener("click", () => {
      all(".mood-chip").forEach((item) => item.classList.toggle("is-selected", item === chip));
      showToast(`${chip.textContent.trim()} is selected.`);
    }));
    all(".quick-track-row, .quick-track, .orbit-card").forEach((button) => button.addEventListener("click", (event) => selectTrack(event.currentTarget)));
    elements.quickPlayAll?.addEventListener("click", () => {
      const first = $(".quick-track-row, .quick-track");
      if (first) selectTrack(first);
    });
    elements.quickPrevBtn?.addEventListener("click", () => {
      const shelf = elements.quickShelf || $("#quick-shelf");
      if (shelf) shelf.scrollBy({ left: -320, behavior: "smooth" });
    });
    elements.quickNextBtn?.addEventListener("click", () => {
      const shelf = elements.quickShelf || $("#quick-shelf");
      if (shelf) shelf.scrollBy({ left: 320, behavior: "smooth" });
    });
    all("[data-genre-search]").forEach((button) => button.addEventListener("click", () => {
      searchYouTube(button.dataset.genreSearch, button.dataset.genreCategory || "");
    }));
    all("[data-genre-filter]").forEach((chip) => chip.addEventListener("click", () => {
      all("[data-genre-filter]").forEach((item) => item.classList.toggle("is-selected", item === chip));
      const filter = chip.dataset.genreFilter;
      all(".explore-tile").forEach((tile) => {
        if (filter === "all" || tile.dataset.genreCategory === filter) {
          tile.style.display = "";
        } else {
          tile.style.display = "none";
        }
      });
      showToast(filter === "all" ? "Showing all categories" : `${chip.textContent.trim()} category`);
    }));
    elements.profileTrigger.addEventListener("click", openAccountDialog);
    elements.topProfileTrigger.addEventListener("click", openAccountDialog);
    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.youtubeClose.addEventListener("click", closeNowPlayingSidebar);
    elements.dockSideToggle?.addEventListener("click", (event) => {
      event.stopPropagation();
      toggleNowPlayingSidebar();
    });
    elements.sideArtistExplore?.addEventListener("click", () => {
      if (currentYouTubeTrack?.artist) {
        searchYouTube(`${currentYouTubeTrack.artist} songs`);
      }
    });
    elements.youtubeForm.addEventListener("submit", (event) => {
      event.preventDefault();
      playYouTubeVideo(elements.youtubeUrl.value);
    });
    elements.youtubeSearchForm.addEventListener("submit", (event) => {
      event.preventDefault();
      searchYouTube(elements.youtubeSearchInput.value);
    });
    elements.dialogClose.addEventListener("click", closeAccountDialog);
    elements.accountDialog.addEventListener("click", (event) => {
      const rect = elements.accountDialog.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeAccountDialog();
    });
    elements.settingsTrigger?.addEventListener("click", openSettingsDialog);
    elements.sidebarSettingsBtn?.addEventListener("click", () => {
      closeMenu();
      openSettingsDialog();
    });
    elements.settingsClose?.addEventListener("click", closeSettingsDialog);
    elements.settingsDialog?.addEventListener("click", (event) => {
      const rect = elements.settingsDialog.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeSettingsDialog();
    });

    elements.modeArtworkBtn?.addEventListener("click", () => applyVisualMode("artwork", true));
    elements.modeVideoBtn?.addEventListener("click", () => applyVisualMode("video", true));
    elements.npSwitchArtwork?.addEventListener("click", () => applyVisualMode("artwork", true));
    elements.npSwitchVideo?.addEventListener("click", () => applyVisualMode("video", true));
    elements.sideVideoContainer?.addEventListener("click", (event) => {
      if (currentVisualMode === "artwork" && !event.target.closest("iframe")) {
        applyVisualMode("video", true);
      }
    });
    elements.settingsThemeToggle?.addEventListener("click", toggleTheme);

    all(".connect-trigger").forEach((button) => button.addEventListener("click", requestConnection));
    elements.disconnect.addEventListener("click", disconnect);
    elements.libraryDisconnectBtn?.addEventListener("click", disconnect);
    elements.file.addEventListener("change", () => selectLocalAudio(elements.file.files?.[0]));
    elements.playerDock.addEventListener("click", (event) => {
      if (event.target.closest("#dock-play, .dock-icon, .dock-queue, input")) return;
      toggleNowPlayingSidebar();
    });
    elements.dockArt.addEventListener("click", (event) => {
      event.stopPropagation();
      toggleNowPlayingSidebar();
    });
    elements.nowPlayingTrigger.addEventListener("click", (event) => {
      event.stopPropagation();
      toggleNowPlayingSidebar();
    });
    const shelfContainer = elements.quickShelf || elements.quickGrid || $("#quick-shelf") || $("#quick-grid");
    shelfContainer?.addEventListener("click", (event) => {
      const moreBtn = event.target.closest(".quick-row-more");
      if (moreBtn) {
        event.stopPropagation();
        const title = moreBtn.dataset.title || "Song";
        const artist = moreBtn.dataset.artist || "Artist";
        showToast(`${title} · ${artist} added to queue`);
        return;
      }
      const track = event.target.closest(".quick-track-row, .quick-track");
      if (track) selectTrack(track);
    });
    elements.orbitRail?.addEventListener("click", (event) => {
      const card = event.target.closest(".orbit-card");
      if (card) selectTrack(card);
    });
    elements.soundtrackPlay?.addEventListener("click", (event) => {
      event.stopPropagation();
      if (spotlightHeroTrack?.videoId) {
        setNowPlaying(spotlightHeroTrack.title, spotlightHeroTrack.artist, "art-pop", spotlightHeroTrack.thumbnail, spotlightHeroTrack.videoId);
        playYouTubeVideo(spotlightHeroTrack.videoId, spotlightHeroTrack);
      } else {
        const first = $(".quick-track-row, .quick-track");
        if (first) {
          selectTrack(first);
        } else {
          playYouTubeVideo("kPa7bsKwL-c", {
            title: "Die With A Smile",
            artist: "Lady Gaga & Bruno Mars",
            thumbnail: "https://i.ytimg.com/vi/kPa7bsKwL-c/hqdefault.jpg",
            videoId: "kPa7bsKwL-c"
          });
        }
      }
    });
    elements.dockPlay.addEventListener("click", togglePlayback);
    elements.audio.addEventListener("loadedmetadata", () => {
      resetPlayer();
      elements.dockSeek.disabled = false;
    });
    elements.audio.addEventListener("timeupdate", () => {
      if (!Number.isFinite(elements.audio.duration)) return;
      const pct = (elements.audio.currentTime / elements.audio.duration) * 100;
      const curFormatted = formatTime(elements.audio.currentTime);
      const durFormatted = formatTime(elements.audio.duration);
      elements.dockSeek.value = String(pct);
      elements.dockElapsed.textContent = curFormatted;
      elements.dockDuration.textContent = durFormatted;
      if (elements.sideSeek) elements.sideSeek.value = String(pct);
      if (elements.sideElapsed) elements.sideElapsed.textContent = curFormatted;
      if (elements.sideDuration) elements.sideDuration.textContent = durFormatted;
      if (elements.dockMiniProgressFill) elements.dockMiniProgressFill.style.width = pct + "%";
    });
    elements.audio.addEventListener("play", () => {
      pauseYouTube();
      setPlayIcon(true);
    });
    elements.audio.addEventListener("pause", () => setPlayIcon(false));
    elements.audio.addEventListener("ended", () => {
      elements.dockSeek.value = "0";
      setPlayIcon(false);
      playNextTrack();
    });
    elements.dockSeek.addEventListener("input", () => {
      isSeeking = true;
      const val = Number(elements.dockSeek.value);
      if (elements.sideSeek) elements.sideSeek.value = String(val);
      if (elements.dockMiniProgressFill) elements.dockMiniProgressFill.style.width = val + "%";
      if (youTubePlayer && typeof youTubePlayer.getDuration === "function" && youTubePlayer.getDuration() > 0) {
        const seekTime = (val / 100) * youTubePlayer.getDuration();
        elements.dockElapsed.textContent = formatTime(seekTime);
        if (elements.sideElapsed) elements.sideElapsed.textContent = formatTime(seekTime);
      } else if (Number.isFinite(elements.audio.duration) && elements.audio.duration > 0) {
        const seekTime = (val / 100) * elements.audio.duration;
        elements.dockElapsed.textContent = formatTime(seekTime);
        if (elements.sideElapsed) elements.sideElapsed.textContent = formatTime(seekTime);
      }
    });
    elements.dockSeek.addEventListener("change", () => {
      isSeeking = false;
      const val = Number(elements.dockSeek.value);
      if (elements.sideSeek) elements.sideSeek.value = String(val);
      if (youTubePlayer && typeof youTubePlayer.getDuration === "function" && youTubePlayer.getDuration() > 0) {
        const seekTime = (val / 100) * youTubePlayer.getDuration();
        try {
          youTubePlayer.seekTo(seekTime, true);
        } catch {}
        elements.dockElapsed.textContent = formatTime(seekTime);
        if (elements.sideElapsed) elements.sideElapsed.textContent = formatTime(seekTime);
      } else if (Number.isFinite(elements.audio.duration) && elements.audio.duration > 0) {
        elements.audio.currentTime = (val / 100) * elements.audio.duration;
      }
    });

    elements.sideSeek?.addEventListener("input", () => {
      isSeeking = true;
      const val = Number(elements.sideSeek.value);
      elements.dockSeek.value = String(val);
      if (elements.dockMiniProgressFill) elements.dockMiniProgressFill.style.width = val + "%";
      if (youTubePlayer && typeof youTubePlayer.getDuration === "function" && youTubePlayer.getDuration() > 0) {
        const seekTime = (val / 100) * youTubePlayer.getDuration();
        elements.dockElapsed.textContent = formatTime(seekTime);
        elements.sideElapsed.textContent = formatTime(seekTime);
      } else if (Number.isFinite(elements.audio.duration) && elements.audio.duration > 0) {
        const seekTime = (val / 100) * elements.audio.duration;
        elements.dockElapsed.textContent = formatTime(seekTime);
        elements.sideElapsed.textContent = formatTime(seekTime);
      }
    });
    elements.sideSeek?.addEventListener("change", () => {
      isSeeking = false;
      const val = Number(elements.sideSeek.value);
      elements.dockSeek.value = String(val);
      if (youTubePlayer && typeof youTubePlayer.getDuration === "function" && youTubePlayer.getDuration() > 0) {
        const seekTime = (val / 100) * youTubePlayer.getDuration();
        try {
          youTubePlayer.seekTo(seekTime, true);
        } catch {}
        elements.dockElapsed.textContent = formatTime(seekTime);
        elements.sideElapsed.textContent = formatTime(seekTime);
      } else if (Number.isFinite(elements.audio.duration) && elements.audio.duration > 0) {
        elements.audio.currentTime = (val / 100) * elements.audio.duration;
      }
    });

    elements.dockNext?.addEventListener("click", playNextTrack);
    elements.sideNextBtn?.addEventListener("click", playNextTrack);
    elements.sidePrevBtn?.addEventListener("click", playPrevTrack);
    elements.sidePlay?.addEventListener("click", togglePlayback);
    elements.dockQueueBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      toggleQueueDialog();
    });
    elements.sideQueueBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      toggleQueueDialog();
    });
    elements.queueClose?.addEventListener("click", closeQueueDialog);
    elements.queueDialog?.addEventListener("click", (event) => {
      const rect = elements.queueDialog.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeQueueDialog();
    });
    elements.endlessToggleInput?.addEventListener("change", (e) => {
      setEndlessListening(e.target.checked);
    });
    elements.queueShuffleBtn?.addEventListener("click", shuffleQueue);
    elements.queueClearBtn?.addEventListener("click", clearQueue);
    elements.queueList?.addEventListener("click", (event) => {
      const delBtn = event.target.closest(".queue-track-del");
      if (delBtn) {
        event.stopPropagation();
        const idx = parseInt(delBtn.dataset.delIdx, 10);
        if (!isNaN(idx)) {
          removeFromQueue(idx);
        }
        return;
      }
      const row = event.target.closest(".queue-track-row");
      if (row) {
        const idx = parseInt(row.dataset.queueIdx, 10);
        if (!isNaN(idx) && playbackQueue[idx]) {
          const track = playbackQueue.splice(idx, 1)[0];
          renderQueue();
          if (track.videoId) {
            playYouTubeVideo(track.videoId, track);
          }
        }
      }
    });
    elements.sheetHandle?.addEventListener("click", closeNowPlayingSidebar);
    elements.search.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        handleSearch();
      }
    });
    elements.search.addEventListener("search", () => {
      handleSearch();
    });
    $("#mobile-search")?.addEventListener("click", () => {
      goTo("explore");
      all(".mobile-nav-item").forEach((btn) => btn.classList.remove("is-active"));
      $("#mobile-search")?.classList.add("is-active");
      window.scrollTo({ top: 0, behavior: "smooth" });
      window.setTimeout(() => {
        elements.youtubeSearchInput?.focus();
        elements.youtubeSearchInput?.select();
      }, 120);
    });
    window.addEventListener("keydown", (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        elements.search.focus();
      }
    });
    window.addEventListener("hashchange", () => {
      const page = window.location.hash.replace("#", "");
      if (["home", "explore", "library"].includes(page)) goTo(page);
    });
  }

  bindEvents();
  applyTheme(getStoredTheme());
  applyVisualMode(currentVisualMode);
  const requestedPage = window.location.hash.replace("#", "");
  if (["explore", "library"].includes(requestedPage)) goTo(requestedPage);
  renderAccount(storedProfile());
  renderDayGreeting();
  scheduleGreetingRefresh();
  applyDerbisAlgorithm();
  setPlayIcon(false);
  if (!playbackQueue.length && DERBIS_CATALOG_TRACKS.length > 1) {
    playbackQueue = DERBIS_CATALOG_TRACKS.slice(1);
  }
  renderQueue();
})();
