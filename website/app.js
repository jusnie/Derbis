(() => {
  "use strict";

  const config = window.DERBIS_WEB_CONFIG || {};
  const clientId = String(config.googleOAuthClientId || "").trim();
  const scopes = Array.isArray(config.googleOAuthScopes)
    ? config.googleOAuthScopes.join(" ")
    : "openid email profile https://www.googleapis.com/auth/youtube.readonly";
  const stateKey = "derbis-web-google-profile";
  const themeKey = "derbis-web-theme";
  let tokenClient = null;
  let localObjectUrl = null;
  let activeAccessToken = null;
  let pendingYouTubeSearch = null;
  let youtubeSearchAbort = null;
  let youTubePlayer = null;
  let youtubeApiPromise = null;
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
    youtubeDialog: $("#youtube-dialog"),
    youtubeClose: $("#youtube-close"),
    youtubeForm: $("#youtube-form"),
    youtubeUrl: $("#youtube-url"),
    youtubeTrigger: $("#youtube-trigger"),
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
    welcomeName: $("#welcome-name"),
    mobileWelcomeName: $("#mobile-welcome-name"),
    desktopDayGreeting: $("#desktop-day-greeting"),
    mobileDayGreeting: $("#mobile-day-greeting"),
    libraryAccountName: $("#library-account-name"),
    libraryAccountDetail: $("#library-account-detail"),
    audio: $("#audio-player"),
    file: $("#local-audio"),
    dockArt: $("#dock-art"),
    dockTitle: $("#dock-title"),
    dockArtist: $("#dock-artist"),
    dockPlay: $("#dock-play"),
    dockSeek: $("#dock-seek"),
    dockElapsed: $("#dock-elapsed"),
    dockDuration: $("#dock-duration"),
    soundtrackPlay: $("#soundtrack-play"),
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

  function loadYouTubeIframeApi() {
    if (window.YT?.Player) return Promise.resolve();
    if (youtubeApiPromise) return youtubeApiPromise;
    youtubeApiPromise = new Promise((resolve, reject) => {
      const previousReady = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => {
        if (typeof previousReady === "function") previousReady();
        resolve();
      };
      const script = document.createElement("script");
      script.src = "https://www.youtube.com/iframe_api";
      script.async = true;
      script.onerror = () => reject(new Error("The YouTube player library could not load."));
      document.head.append(script);
    });
    return youtubeApiPromise;
  }

  function pauseYouTube() {
    try {
      youTubePlayer?.pauseVideo?.();
    } catch {
      // The embedded player may not have finished initializing yet.
    }
  }

  function openYouTubeDialog() {
    if (!elements.youtubeDialog.open) elements.youtubeDialog.showModal();
    window.setTimeout(() => elements.youtubeUrl.focus(), 0);
  }

  function closeYouTubeDialog() {
    pauseYouTube();
    if (elements.youtubeDialog.open) elements.youtubeDialog.close();
  }

  async function playYouTubeVideo(value) {
    const videoId = extractYouTubeVideoId(value);
    if (!videoId) {
      showToast("Paste a valid public YouTube video URL or 11-character video ID.");
      return;
    }
    try {
      await loadYouTubeIframeApi();
      if (!elements.youtubeDialog.open) elements.youtubeDialog.showModal();
      if (elements.audio.src) elements.audio.pause();
      if (youTubePlayer) {
        youTubePlayer.loadVideoById(videoId);
      } else {
        youTubePlayer = new window.YT.Player("youtube-frame", {
          videoId,
          playerVars: { autoplay: 1, playsinline: 1, rel: 0, modestbranding: 1 },
          events: {
            onReady: (event) => event.target.playVideo(),
            onStateChange: (event) => {
              if (event.data === window.YT.PlayerState.PLAYING && elements.audio.src) elements.audio.pause();
            },
            onError: () => showToast("This YouTube video cannot be embedded. Try another public video.")
          }
        });
      }
      showToast("Playing with YouTube’s official embedded player.");
    } catch (error) {
      console.error(error);
      showToast("The YouTube player could not start. Check your connection and try again.");
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
    elements.dialogConnect.textContent = connected ? "Reconnect Google / YouTube" : "Connect Google / YouTube";
    elements.disconnect.hidden = !connected;
    [elements.profileAvatar, elements.topProfileAvatar, elements.dialogAvatar].forEach((avatar) => setAvatar(avatar, profile, initial));
  }

  function openAccountDialog() {
    if (!elements.accountDialog.open) elements.accountDialog.showModal();
  }

  function closeAccountDialog() {
    if (elements.accountDialog.open) elements.accountDialog.close();
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
    renderAccount(null);
    showToast("Disconnected from this browser session.");
  }

  function formatTime(seconds) {
    if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
    const whole = Math.floor(seconds);
    return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, "0")}`;
  }

  function setPlayIcon(isPlaying) {
    elements.dockPlay.innerHTML = `<svg><use href="#${isPlaying ? "i-pause" : "i-play"}"/></svg>`;
    elements.dockPlay.setAttribute("aria-label", isPlaying ? "Pause local audio" : "Play local audio");
  }

  function resetPlayer() {
    elements.dockSeek.value = "0";
    elements.dockElapsed.textContent = "0:00";
    elements.dockDuration.textContent = formatTime(elements.audio.duration);
  }

  function chooseLocalAudio() {
    elements.file.click();
  }

  function selectLocalAudio(file) {
    if (!file) return;
    if (localObjectUrl) URL.revokeObjectURL(localObjectUrl);
    localObjectUrl = URL.createObjectURL(file);
    elements.audio.src = localObjectUrl;
    elements.audio.load();
    const title = file.name.replace(/\.[^.]+$/, "") || "Local audio";
    setNowPlaying(title, "Local audio · browser playback", "art-blue", false);
    resetPlayer();
    elements.audio.play().catch(() => showToast("Your audio file is ready. Press play when you are ready."));
  }

  function togglePlayback() {
    if (!elements.audio.src) {
      showToast("Choose a local audio file to use the browser player.");
      chooseLocalAudio();
      return;
    }
    if (elements.audio.paused) {
      elements.audio.play().catch(() => showToast("This browser could not play that audio file."));
    } else {
      elements.audio.pause();
    }
  }

  function setNowPlaying(title, artist, artClass, showMessage = true) {
    elements.dockTitle.textContent = title;
    elements.dockArtist.textContent = artist;
    elements.dockArt.className = `dock-art ${artClass}`;
    elements.dockArt.innerHTML = `<i></i><em>${escapeHtml(title.slice(0, 8).toUpperCase())}</em>`;
    all(".quick-track").forEach((item) => item.classList.toggle("is-current", item.dataset.title === title));
    if (showMessage && !elements.audio.src) showToast(`${title} is selected. Choose local audio to use the browser player.`);
  }

  function selectTrack(target) {
    const button = target.closest("[data-title]");
    if (!button) return;
    setNowPlaying(button.dataset.title, button.dataset.artist, button.dataset.art || "art-blue");
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

  function renderYouTubeResults(items) {
    if (!items.length) {
      elements.youtubeSearchResults.innerHTML = "";
      setYouTubeSearchStatus("No embeddable public videos found. Try a different search.");
      return;
    }
    elements.youtubeSearchResults.innerHTML = items.map((item) => {
      const videoId = item?.id?.videoId;
      const snippet = item?.snippet || {};
      const thumbnail = snippet?.thumbnails?.medium?.url || snippet?.thumbnails?.high?.url || snippet?.thumbnails?.default?.url || "";
      const title = decodeYouTubeText(snippet.title || "Untitled video");
      const channel = decodeYouTubeText(snippet.channelTitle || "YouTube");
      if (!videoId) return "";
      return `<button class="youtube-result" type="button" data-youtube-video="${escapeHtml(videoId)}" aria-label="Play ${escapeHtml(title)} on YouTube">
        <span class="youtube-result-art"><img src="${escapeHtml(thumbnail)}" alt="" referrerpolicy="no-referrer" /><i class="youtube-result-play"><svg><use href="#i-play"/></svg></i></span>
        <span class="youtube-result-copy"><strong>${escapeHtml(title)}</strong><small>${escapeHtml(channel)}</small></span>
      </button>`;
    }).join("");
    all("[data-youtube-video]").forEach((button) => button.addEventListener("click", () => {
      elements.youtubeUrl.value = button.dataset.youtubeVideo;
      playYouTubeVideo(button.dataset.youtubeVideo);
    }));
  }

  async function searchYouTube(query) {
    const normalizedQuery = String(query || "").trim();
    if (!normalizedQuery) {
      setYouTubeSearchStatus("Enter a song, artist, or video title to search.", "error");
      return;
    }
    goTo("explore");
    elements.youtubeSearchInput.value = normalizedQuery;
    if (!activeAccessToken) {
      pendingYouTubeSearch = normalizedQuery;
      setYouTubeSearchStatus("Connect Google to search public YouTube videos.", "loading");
      requestConnection();
      return;
    }
    youtubeSearchAbort?.abort();
    youtubeSearchAbort = new AbortController();
    setYouTubeSearchStatus(`Searching YouTube for “${normalizedQuery}”…`, "loading");
    elements.youtubeSearchResults.innerHTML = "";
    try {
      const params = new URLSearchParams({
        part: "snippet",
        type: "video",
        videoEmbeddable: "true",
        videoSyndicated: "true",
        maxResults: "12",
        q: normalizedQuery
      });
      const response = await fetch(`https://www.googleapis.com/youtube/v3/search?${params}`, {
        headers: { Authorization: `Bearer ${activeAccessToken}` },
        signal: youtubeSearchAbort.signal
      });
      if (response.status === 401) {
        activeAccessToken = null;
        pendingYouTubeSearch = normalizedQuery;
        setYouTubeSearchStatus("Your Google session expired. Reconnecting…", "loading");
        requestConnection();
        return;
      }
      if (!response.ok) throw new Error(`YouTube search returned ${response.status}`);
      const payload = await response.json();
      renderYouTubeResults((payload.items || []).filter((item) => item?.id?.videoId));
      if (payload.items?.length) setYouTubeSearchStatus("Select a result to play it in Derbis.");
    } catch (error) {
      if (error.name === "AbortError") return;
      console.error(error);
      setYouTubeSearchStatus("YouTube search is unavailable. Check the YouTube Data API and try again.", "error");
    }
  }

  function handleSearch() {
    const query = elements.search.value.trim();
    if (!query) return;
    elements.search.value = "";
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
    all(".quick-track, .orbit-card").forEach((button) => button.addEventListener("click", (event) => selectTrack(event.currentTarget)));
    $("#quick-play-all").addEventListener("click", () => {
      const first = $(".quick-track");
      selectTrack(first);
      showToast("Quick Picks are selected. Choose local audio to use the browser player.");
    });
    elements.profileTrigger.addEventListener("click", openAccountDialog);
    elements.topProfileTrigger.addEventListener("click", openAccountDialog);
    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.youtubeTrigger.addEventListener("click", openYouTubeDialog);
    elements.youtubeClose.addEventListener("click", closeYouTubeDialog);
    elements.youtubeDialog.addEventListener("click", (event) => {
      const rect = elements.youtubeDialog.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeYouTubeDialog();
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
    all(".connect-trigger").forEach((button) => button.addEventListener("click", requestConnection));
    elements.disconnect.addEventListener("click", disconnect);
    elements.file.addEventListener("change", () => selectLocalAudio(elements.file.files?.[0]));
    elements.dockArt.addEventListener("click", chooseLocalAudio);
    elements.soundtrackPlay.addEventListener("click", openYouTubeDialog);
    elements.dockPlay.addEventListener("click", togglePlayback);
    elements.audio.addEventListener("loadedmetadata", () => {
      resetPlayer();
      elements.dockSeek.disabled = false;
    });
    elements.audio.addEventListener("timeupdate", () => {
      if (!Number.isFinite(elements.audio.duration)) return;
      elements.dockSeek.value = String((elements.audio.currentTime / elements.audio.duration) * 100);
      elements.dockElapsed.textContent = formatTime(elements.audio.currentTime);
      elements.dockDuration.textContent = formatTime(elements.audio.duration);
    });
    elements.audio.addEventListener("play", () => {
      pauseYouTube();
      setPlayIcon(true);
    });
    elements.audio.addEventListener("pause", () => setPlayIcon(false));
    elements.audio.addEventListener("ended", () => {
      elements.dockSeek.value = "0";
      setPlayIcon(false);
    });
    elements.dockSeek.addEventListener("input", () => {
      if (!Number.isFinite(elements.audio.duration)) return;
      elements.audio.currentTime = (Number(elements.dockSeek.value) / 100) * elements.audio.duration;
    });
    elements.search.addEventListener("keydown", (event) => {
      if (event.key === "Enter") handleSearch();
    });
    $("#mobile-search").addEventListener("click", () => {
      window.scrollTo({ top: 0, behavior: "smooth" });
      showToast("Search is available in the desktop bar. Web search is coming next.");
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
  const requestedPage = window.location.hash.replace("#", "");
  if (["explore", "library"].includes(requestedPage)) goTo(requestedPage);
  renderAccount(storedProfile());
  renderDayGreeting();
  scheduleGreetingRefresh();
  setPlayIcon(false);
})();
