// Derbis YouTube & Google OAuth Service
// Uses Google Identity Services (GIS) Token Model and YouTube Data API v3

export function parseYouTubeTitle(rawTitle, channelTitle = "") {
  if (!rawTitle) return { title: "Unknown Track", artist: channelTitle || "Unknown Artist" };
  
  // Strip common YouTube fluff
  let cleaned = rawTitle
    .replace(/\s*[\(\[](official\s*(music\s*)?(video|audio|lyric\s*video|visualizer|video)?|4k|hd|remastered|lyrics?|audio)[\)\]]/gi, "")
    .replace(/\s*[\(\[]ft\.?.*[\)\]]/gi, "")
    .trim();

  if (cleaned.includes(" - ")) {
    const parts = cleaned.split(" - ");
    return {
      artist: parts[0].trim(),
      title: parts.slice(1).join(" - ").trim()
    };
  }

  return {
    artist: channelTitle || "YouTube Artist",
    title: cleaned
  };
}

export function extractPlaylistId(input) {
  if (!input) return null;
  const str = input.trim();
  const match = str.match(/[&?]list=([a-zA-Z0-9_-]+)/);
  if (match) return match[1];
  if (/^PL[a-zA-Z0-9_-]+$/.test(str)) return str;
  return null;
}

export async function fetchYouTubeUserData(token) {
  if (!token) throw new Error("No OAuth access token provided");

  const headers = { Authorization: `Bearer ${token}` };

  // 1. User Identity
  const identityRes = await fetch("https://www.googleapis.com/oauth2/v3/userinfo", { headers });
  if (!identityRes.ok) throw new Error("Failed to load Google account identity.");
  const identity = await identityRes.json();

  // 2. Parallel fetch for Liked Videos (LL), My Playlists, and Subscriptions
  const [likedRes, playlistsRes, subsRes] = await Promise.allSettled([
    fetch("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=LL&maxResults=50", { headers }),
    fetch("https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&mine=true&maxResults=50", { headers }),
    fetch("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&maxResults=50", { headers })
  ]);

  // Parse Liked Tracks (Real user songs)
  const likedTracks = [];
  if (likedRes.status === "fulfilled" && likedRes.value.ok) {
    const data = await likedRes.value.json();
    (data.items || []).forEach((item) => {
      const snip = item.snippet;
      const vid = snip?.resourceId?.videoId || item.contentDetails?.videoId;
      if (vid && snip?.title && snip.title !== "Private video" && snip.title !== "Deleted video") {
        const channel = snip.videoOwnerChannelTitle || snip.channelTitle || "";
        const { title, artist } = parseYouTubeTitle(snip.title, channel);
        const thumb = snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`;
        likedTracks.push({
          id: vid,
          videoId: vid,
          title,
          artist,
          thumbnail: thumb,
          duration: "3:30",
          genre: "liked",
          source: "liked"
        });
      }
    });
  }

  // Parse Playlists
  const playlists = [];
  if (playlistsRes.status === "fulfilled" && playlistsRes.value.ok) {
    const data = await playlistsRes.value.json();
    (data.items || []).forEach((item) => {
      const snip = item.snippet;
      const count = item.contentDetails?.itemCount || 0;
      if (snip?.title) {
        playlists.push({
          id: item.id,
          title: snip.title,
          description: snip.description || "",
          thumbnail: snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || "",
          itemCount: count
        });
      }
    });
  }

  // Parse Subscribed Artists / Channels
  const subscriptions = [];
  if (subsRes.status === "fulfilled" && subsRes.value.ok) {
    const data = await subsRes.value.json();
    (data.items || []).forEach((item) => {
      const snip = item.snippet;
      if (snip?.title) {
        subscriptions.push({
          id: item.id,
          name: snip.title,
          avatar: snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || "",
          channelId: snip.resourceId?.channelId
        });
      }
    });
  }

  return {
    profile: {
      name: identity.name || "Google User",
      email: identity.email || "",
      avatar: identity.picture || "",
      connected: true
    },
    likedTracks,
    playlists,
    subscriptions
  };
}

export async function fetchPlaylistTracks(playlistId, apiKey = "", token = "") {
  if (!playlistId) return [];

  let url = `https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=${playlistId}&maxResults=50`;
  const headers = {};

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else if (apiKey) {
    url += `&key=${apiKey}`;
  }

  const res = await fetch(url, { headers });
  if (!res.ok) throw new Error("Could not load playlist items");

  const data = await res.json();
  const tracks = [];
  (data.items || []).forEach((item) => {
    const snip = item.snippet;
    const vid = snip?.resourceId?.videoId || item.contentDetails?.videoId;
    if (vid && snip?.title && snip.title !== "Private video" && snip.title !== "Deleted video") {
      const channel = snip.videoOwnerChannelTitle || snip.channelTitle || "";
      const { title, artist } = parseYouTubeTitle(snip.title, channel);
      const thumb = snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`;
      tracks.push({
        id: vid,
        videoId: vid,
        title,
        artist,
        thumbnail: thumb,
        duration: "3:30",
        genre: "playlist"
      });
    }
  });

  return tracks;
}

// Real Live YouTube Music Search (No fake tracks)
export async function searchYouTubeLive(query, apiKey = "", token = "") {
  if (!query || !query.trim()) return [];

  let url = `https://www.googleapis.com/youtube/v3/search?part=snippet&q=${encodeURIComponent(query)}&type=video&videoCategoryId=10&maxResults=25`;
  const headers = {};

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else if (apiKey) {
    url += `&key=${apiKey}`;
  }

  const res = await fetch(url, { headers });
  if (!res.ok) throw new Error("YouTube search request failed");

  const data = await res.json();
  const tracks = [];
  (data.items || []).forEach((item) => {
    const vid = item.id?.videoId;
    const snip = item.snippet;
    if (vid && snip?.title) {
      const channel = snip.channelTitle || "";
      const { title, artist } = parseYouTubeTitle(snip.title, channel);
      const thumb = snip.thumbnails?.high?.url || snip.thumbnails?.medium?.url || `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`;
      tracks.push({
        id: vid,
        videoId: vid,
        title,
        artist,
        thumbnail: thumb,
        duration: "3:30",
        genre: "search"
      });
    }
  });

  return tracks;
}
