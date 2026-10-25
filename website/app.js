(() => {
  "use strict";

  const config = window.DERBIS_WEB_CONFIG || {};
  const clientId = String(config.googleOAuthClientId || "").trim();
  const scopes = Array.isArray(config.googleOAuthScopes) ? config.googleOAuthScopes.join(" ") : "openid email profile https://www.googleapis.com/auth/youtube.readonly";
  const stateKey = "derbis-web-google-profile";
  let tokenClient = null;
  let localObjectUrl = null;
  let toastTimer = 0;

  const $ = (selector) => document.querySelector(selector);
  const all = (selector) => [...document.querySelectorAll(selector)];
  const elements = {
    sidebar: $("#sidebar"),
    menu: $("#menu-button"),
    scrim: $("#scrim"),
    connect: $("#connect-button"),
    heroConnect: $("#hero-connect"),
    cardConnect: $("#card-connect"),
    disconnect: $("#disconnect-button"),
    setupDialog: $("#setup-dialog"),
    closeDialog: $("#dialog-close"),
    toast: $("#toast"),
    connectionDot: $("#connection-dot"),
    connectionLabel: $("#connection-label"),
    connectionBadge: $("#connection-badge"),
    accountSummary: $("#account-summary"),
    audio: $("#audio-player"),
    file: $("#local-audio"),
    play: $("#play-button"),
    mute: $("#mute-button"),
    seek: $("#seek"),
    elapsed: $("#elapsed"),
    duration: $("#duration"),
    title: $("#track-title"),
    artist: $("#track-artist")
  };

  function showToast(message) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => elements.toast.classList.remove("is-visible"), 4500);
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

  function escapeHtml(value) {
    return String(value || "").replace(/[&<>'"]/g, (character) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[character]));
  }

  function renderAccount(profile) {
    const connected = Boolean(profile);
    elements.connectionDot.classList.toggle("connected", connected);
    elements.connectionLabel.textContent = connected ? "Connected" : "Not connected";
    elements.connectionBadge.textContent = connected ? "Connected" : "Not connected";
    elements.connectionBadge.classList.toggle("connected", connected);
    elements.connect.textContent = connected ? "Account" : "Connect Google";
    elements.cardConnect.hidden = connected;
    elements.disconnect.hidden = !connected;

    if (!connected) {
      elements.accountSummary.innerHTML = `
        <div class="avatar placeholder-avatar" aria-hidden="true">D</div>
        <div>
          <strong>Connect only when you’re ready</strong>
          <p>Derbis uses Google’s official browser authorization. It can read the account/channel data permitted by the YouTube Data API. It does not copy cookies or impersonate YouTube Music playback.</p>
        </div>`;
      return;
    }

    const label = escapeHtml(profile.name || profile.email || "Google account");
    const email = escapeHtml(profile.email || "Official Google authorization active");
    const channel = escapeHtml(profile.channelTitle || "No public YouTube channel found");
    const initial = label.slice(0, 1).toUpperCase() || "G";
    const avatar = profile.picture
      ? `<img src="${escapeHtml(profile.picture)}" alt="" referrerpolicy="no-referrer" />`
      : initial;
    elements.accountSummary.innerHTML = `
      <div class="avatar">${avatar}</div>
      <div>
        <strong>${label}</strong>
        <p>${email} · ${channel}<br>Official YouTube Data access is active for this browser session.</p>
      </div>`;
  }

  function openSetup() {
    if (!elements.setupDialog.open) elements.setupDialog.showModal();
  }

  function closeMenu() {
    elements.sidebar.classList.remove("is-open");
    elements.scrim.hidden = true;
    elements.menu.setAttribute("aria-expanded", "false");
  }

  function toggleMenu() {
    const open = !elements.sidebar.classList.contains("is-open");
    elements.sidebar.classList.toggle("is-open", open);
    elements.scrim.hidden = !open;
    elements.menu.setAttribute("aria-expanded", String(open));
  }

  function goTo(page) {
    all("[data-page-panel]").forEach((panel) => { panel.hidden = panel.dataset.pagePanel !== page; });
    all("[data-page]").forEach((button) => button.classList.toggle("is-active", button.dataset.page === page));
    closeMenu();
    window.location.hash = page === "home" ? "" : page;
    document.querySelector("#main-content").focus({ preventScroll: true });
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
      openSetup();
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
            const headers = { Authorization: `Bearer ${tokenResponse.access_token}` };
            const [identityResponse, channelResponse] = await Promise.all([
              fetch("https://www.googleapis.com/oauth2/v3/userinfo", { headers }),
              fetch("https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true", { headers })
            ]);
            if (!identityResponse.ok) throw new Error("Google account information was unavailable.");
            const identity = await identityResponse.json();
            const channels = channelResponse.ok ? await channelResponse.json() : { items: [] };
            const channelTitle = channels.items?.[0]?.snippet?.title || "No public YouTube channel found";
            const profile = { name: identity.name, email: identity.email, picture: identity.picture, channelTitle };
            saveProfile(profile);
            renderAccount(profile);
            showToast("Google / YouTube account connected for this browser session.");
          } catch (error) {
            console.error(error);
            showToast("Authorization succeeded, but official account data could not be loaded. Check the YouTube Data API setup.");
          }
        }
      });
      tokenClient.requestAccessToken({ prompt: "select_account" });
    } catch (error) {
      console.error(error);
      showToast("Google sign-in could not start. Check your Client ID and allowed site origin.");
    }
  }

  function disconnect() {
    sessionStorage.removeItem(stateKey);
    renderAccount(null);
    showToast("Disconnected from this browser session.");
  }

  function formatTime(seconds) {
    if (!Number.isFinite(seconds) || seconds <= 0) return "0:00";
    const whole = Math.floor(seconds);
    return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, "0")}`;
  }

  function resetPlayer() {
    elements.seek.value = "0";
    elements.elapsed.textContent = "0:00";
    elements.duration.textContent = formatTime(elements.audio.duration);
  }

  function selectLocalAudio(file) {
    if (!file) return;
    if (localObjectUrl) URL.revokeObjectURL(localObjectUrl);
    localObjectUrl = URL.createObjectURL(file);
    elements.audio.src = localObjectUrl;
    elements.audio.load();
    elements.title.textContent = file.name.replace(/\.[^.]+$/, "") || "Local audio";
    elements.artist.textContent = "Local file · browser-native playback";
    elements.play.disabled = false;
    elements.mute.disabled = false;
    elements.play.textContent = "▶";
    elements.play.setAttribute("aria-label", "Play local audio");
    resetPlayer();
  }

  function togglePlayback() {
    if (!elements.audio.src) return;
    if (elements.audio.paused) {
      elements.audio.play().catch(() => showToast("This browser could not play that local audio file."));
    } else {
      elements.audio.pause();
    }
  }

  function bindEvents() {
    elements.menu.addEventListener("click", toggleMenu);
    elements.scrim.addEventListener("click", closeMenu);
    all(".nav-link").forEach((button) => button.addEventListener("click", () => goTo(button.dataset.page)));
    all(".connect-trigger").forEach((button) => button.addEventListener("click", requestConnection));
    [elements.connect, elements.heroConnect, elements.cardConnect].forEach((button) => button.addEventListener("click", requestConnection));
    elements.disconnect.addEventListener("click", disconnect);
    elements.closeDialog.addEventListener("click", () => elements.setupDialog.close());
    elements.setupDialog.addEventListener("click", (event) => {
      const rect = elements.setupDialog.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) elements.setupDialog.close();
    });
    elements.file.addEventListener("change", () => selectLocalAudio(elements.file.files?.[0]));
    elements.play.addEventListener("click", togglePlayback);
    elements.mute.addEventListener("click", () => {
      elements.audio.muted = !elements.audio.muted;
      elements.mute.textContent = elements.audio.muted ? "×" : "♩";
      elements.mute.setAttribute("aria-label", elements.audio.muted ? "Unmute audio" : "Mute audio");
    });
    elements.audio.addEventListener("loadedmetadata", resetPlayer);
    elements.audio.addEventListener("timeupdate", () => {
      if (!Number.isFinite(elements.audio.duration)) return;
      elements.seek.disabled = false;
      elements.seek.value = String((elements.audio.currentTime / elements.audio.duration) * 100);
      elements.elapsed.textContent = formatTime(elements.audio.currentTime);
      elements.duration.textContent = formatTime(elements.audio.duration);
    });
    elements.audio.addEventListener("play", () => {
      elements.play.textContent = "Ⅱ";
      elements.play.setAttribute("aria-label", "Pause local audio");
    });
    elements.audio.addEventListener("pause", () => {
      elements.play.textContent = "▶";
      elements.play.setAttribute("aria-label", "Play local audio");
    });
    elements.audio.addEventListener("ended", () => { elements.seek.value = "0"; });
    elements.seek.addEventListener("input", () => {
      if (!Number.isFinite(elements.audio.duration)) return;
      elements.audio.currentTime = (Number(elements.seek.value) / 100) * elements.audio.duration;
    });
    window.addEventListener("hashchange", () => {
      const page = window.location.hash.replace("#", "");
      if (["home", "library", "downloads"].includes(page)) goTo(page);
    });
  }

  bindEvents();
  const requestedPage = window.location.hash.replace("#", "");
  if (["library", "downloads"].includes(requestedPage)) goTo(requestedPage);
  renderAccount(storedProfile());
})();
