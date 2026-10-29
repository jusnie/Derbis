// Curated Derbis Tracks (YouTube playable IDs)
export const QUICK_PICKS_TRACKS = [
  {
    id: "JFcgOboQZ08",
    title: "BIRDS OF A FEATHER",
    artist: "Billie Eilish",
    album: "HIT ME HARD AND SOFT",
    year: "2024",
    genre: "pop",
    videoId: "JFcgOboQZ08",
    duration: "3:14",
    thumbnail: "https://i.ytimg.com/vi/JFcgOboQZ08/hqdefault.jpg"
  },
  {
    id: "eVli-tstM5E",
    title: "Taste",
    artist: "Sabrina Carpenter",
    album: "Short n' Sweet",
    year: "2024",
    genre: "pop",
    videoId: "eVli-tstM5E",
    duration: "2:37",
    thumbnail: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg"
  },
  {
    id: "4NRXx6U8ABQ",
    title: "Blinding Lights",
    artist: "The Weeknd",
    album: "After Hours",
    year: "2020",
    genre: "rnb",
    videoId: "4NRXx6U8ABQ",
    duration: "3:20",
    thumbnail: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"
  },
  {
    id: "gNi_6U5Pm_o",
    title: "Love Me Harder",
    artist: "Ariana Grande, The Weeknd",
    album: "My Everything",
    year: "2014",
    genre: "pop",
    videoId: "gNi_6U5Pm_o",
    duration: "3:56",
    thumbnail: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg"
  },
  {
    id: "Zx31bB2vMns",
    title: "Pantropiko",
    artist: "BINI",
    album: "Talaarawan",
    year: "2024",
    genre: "opm",
    videoId: "Zx31bB2vMns",
    duration: "3:45",
    thumbnail: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg"
  },
  {
    id: "DuKb1yF5Wh4",
    title: "kahel na langit",
    artist: "Maki",
    album: "Tanaw",
    year: "2024",
    genre: "opm",
    videoId: "DuKb1yF5Wh4",
    duration: "4:01",
    thumbnail: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg"
  },
  {
    id: "fJ9rUzIMcZQ",
    title: "Bohemian Rhapsody",
    artist: "Queen",
    album: "A Night at the Opera",
    year: "1975",
    genre: "rock",
    videoId: "fJ9rUzIMcZQ",
    duration: "5:55",
    thumbnail: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg"
  },
  {
    id: "kJQP7kiw5Fk",
    title: "Despacito",
    artist: "Luis Fonsi, Daddy Yankee",
    album: "Vida",
    year: "2017",
    genre: "latin",
    videoId: "kJQP7kiw5Fk",
    duration: "3:48",
    thumbnail: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"
  }
];

// Curated tracks per Category / Hit selection
export const HITS_TRACK_SETS = {
  derbis_top_hits: [
    QUICK_PICKS_TRACKS[0], // BIRDS OF A FEATHER
    QUICK_PICKS_TRACKS[1], // Taste
    QUICK_PICKS_TRACKS[2], // Blinding Lights
    QUICK_PICKS_TRACKS[3], // Love Me Harder
    QUICK_PICKS_TRACKS[4], // Pantropiko
    QUICK_PICKS_TRACKS[5]  // kahel na langit
  ],
  pop_hits: [
    QUICK_PICKS_TRACKS[1], // Taste
    QUICK_PICKS_TRACKS[0], // BIRDS OF A FEATHER
    QUICK_PICKS_TRACKS[3], // Love Me Harder
    { id: "hT_nvWreIhg", title: "Counting Stars", artist: "OneRepublic", album: "Native", year: "2013", genre: "pop", videoId: "hT_nvWreIhg", duration: "4:17", thumbnail: "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg" },
    { id: "09R8_2nJtjg", title: "Sugar", artist: "Maroon 5", album: "V", year: "2014", genre: "pop", videoId: "09R8_2nJtjg", duration: "3:55", thumbnail: "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg" }
  ],
  rap_hits: [
    { id: "tvTRZJ-4EyI", title: "HUMBLE.", artist: "Kendrick Lamar", album: "DAMN.", year: "2017", genre: "rap", videoId: "tvTRZJ-4EyI", duration: "2:57", thumbnail: "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg" },
    { id: "dqt8Z1k0oKQ", title: "SICKO MODE", artist: "Travis Scott, Drake", album: "ASTROWORLD", year: "2018", genre: "rap", videoId: "dqt8Z1k0oKQ", duration: "5:12", thumbnail: "https://i.ytimg.com/vi/dqt8Z1k0oKQ/hqdefault.jpg" },
    { id: "uxpDa-c-4Mc", title: "God's Plan", artist: "Drake", album: "Scorpion", year: "2018", genre: "rap", videoId: "uxpDa-c-4Mc", duration: "3:18", thumbnail: "https://i.ytimg.com/vi/uxpDa-c-4Mc/hqdefault.jpg" },
    { id: "YVkUvmDQ3HY", title: "Without Me", artist: "Eminem", album: "The Eminem Show", year: "2002", genre: "rap", videoId: "YVkUvmDQ3HY", duration: "4:50", thumbnail: "https://i.ytimg.com/vi/YVkUvmDQ3HY/hqdefault.jpg" }
  ],
  dj_hits: [
    { id: "JRfuAukYTKg", title: "Titanium", artist: "David Guetta, Sia", album: "Nothing but the Beat", year: "2011", genre: "dance", videoId: "JRfuAukYTKg", duration: "4:05", thumbnail: "https://i.ytimg.com/vi/JRfuAukYTKg/hqdefault.jpg" },
    { id: "ebXbLfLAC3S", title: "Summer", artist: "Calvin Harris", album: "Motion", year: "2014", genre: "dance", videoId: "ebXbLfLAC3S", duration: "3:42", thumbnail: "https://i.ytimg.com/vi/ebXbLfLAC3S/hqdefault.jpg" },
    { id: "I52gK1pB6uU", title: "The Business", artist: "Tiësto", album: "Drive", year: "2020", genre: "dance", videoId: "I52gK1pB6uU", duration: "2:44", thumbnail: "https://i.ytimg.com/vi/I52gK1pB6uU/hqdefault.jpg" },
    { id: "IcrbM1l_BoI", title: "Wake Me Up", artist: "Avicii", album: "True", year: "2013", genre: "dance", videoId: "IcrbM1l_BoI", duration: "4:09", thumbnail: "https://i.ytimg.com/vi/IcrbM1l_BoI/hqdefault.jpg" }
  ],
  opm_hits: [
    QUICK_PICKS_TRACKS[4], // Pantropiko
    QUICK_PICKS_TRACKS[5], // kahel na langit
    { id: "JdV5_Wj4T74", title: "Salamin, Salamin", artist: "BINI", album: "Talaarawan", year: "2024", genre: "opm", videoId: "JdV5_Wj4T74", duration: "3:50", thumbnail: "https://i.ytimg.com/vi/JdV5_Wj4T74/hqdefault.jpg" },
    { id: "9bZkp7q19f0", title: "Dilaw", artist: "Maki", album: "Tanaw", year: "2024", genre: "opm", videoId: "9bZkp7q19f0", duration: "3:40", thumbnail: "https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg" }
  ],
  rnb_hits: [
    QUICK_PICKS_TRACKS[2], // Blinding Lights
    QUICK_PICKS_TRACKS[3], // Love Me Harder
    { id: "LDY_Xyx_F3E", title: "Snooze", artist: "SZA", album: "SOS", year: "2022", genre: "rnb", videoId: "LDY_Xyx_F3E", duration: "3:21", thumbnail: "https://i.ytimg.com/vi/LDY_Xyx_F3E/hqdefault.jpg" },
    { id: "sM0XvASEhYI", title: "Chanel", artist: "Frank Ocean", album: "Chanel", year: "2017", genre: "rnb", videoId: "sM0XvASEhYI", duration: "3:30", thumbnail: "https://i.ytimg.com/vi/sM0XvASEhYI/hqdefault.jpg" }
  ],
  staff_picks: [
    QUICK_PICKS_TRACKS[6], // Bohemian Rhapsody
    { id: "dvgZkm1xWPE", title: "Viva La Vida", artist: "Coldplay", album: "Viva la Vida", year: "2008", genre: "rock", videoId: "dvgZkm1xWPE", duration: "4:02", thumbnail: "https://i.ytimg.com/vi/dvgZkm1xWPE/hqdefault.jpg" },
    QUICK_PICKS_TRACKS[0], // BIRDS OF A FEATHER
    { id: "tvTRZJ-4EyI", title: "HUMBLE.", artist: "Kendrick Lamar", album: "DAMN.", year: "2017", genre: "rap", videoId: "tvTRZJ-4EyI", duration: "2:57", thumbnail: "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg" }
  ],
  uploads: [
    QUICK_PICKS_TRACKS[5], // kahel na langit (Maki)
    QUICK_PICKS_TRACKS[4], // Pantropiko (BINI)
    QUICK_PICKS_TRACKS[1]  // Taste (Sabrina Carpenter)
  ]
};

// Exact "The Hits" Editorial Cards matching Screenshot with Artist Backgrounds + 3 Circular Overlays
export const DERBIS_THE_HITS = [
  {
    id: "derbis_top_hits",
    title: "DERBIS TOP'S HITS",
    subtitle: "DERBIS",
    tagText: "DERBIS TOP'S HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/JFcgOboQZ08/maxresdefault.jpg",
    artists: [
      { name: "Billie Eilish", avatar: "https://i.ytimg.com/vi/JFcgOboQZ08/hqdefault.jpg" },
      { name: "The Weeknd", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
      { name: "Ariana Grande", avatar: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg" }
    ],
    seedTrackId: "JFcgOboQZ08"
  },
  {
    id: "pop_hits",
    title: "Pop Hits",
    subtitle: "DERBIS",
    tagText: "POP HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/eVli-tstM5E/maxresdefault.jpg",
    artists: [
      { name: "Sabrina Carpenter", avatar: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg" },
      { name: "Ariana Grande", avatar: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg" },
      { name: "Billie Eilish", avatar: "https://i.ytimg.com/vi/JFcgOboQZ08/hqdefault.jpg" }
    ],
    seedTrackId: "eVli-tstM5E"
  },
  {
    id: "rap_hits",
    title: "Rap Hits",
    subtitle: "DERBIS",
    tagText: "RAP HITS",
    tagOrientation: "vertical_right",
    bgArtistPhoto: "https://i.ytimg.com/vi/tvTRZJ-4EyI/maxresdefault.jpg",
    artists: [
      { name: "Kendrick Lamar", avatar: "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg" },
      { name: "Drake", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
      { name: "Travis Scott", avatar: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" }
    ],
    seedTrackId: "tvTRZJ-4EyI"
  },
  {
    id: "dj_hits",
    title: "DJ Hits",
    subtitle: "DERBIS",
    tagText: "DJ HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/kJQP7kiw5Fk/maxresdefault.jpg",
    artists: [
      { name: "David Guetta", avatar: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" },
      { name: "Calvin Harris", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
      { name: "Tiësto", avatar: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg" }
    ],
    seedTrackId: "JRfuAukYTKg"
  },
  {
    id: "opm_hits",
    title: "OPM Wave Hits",
    subtitle: "DERBIS",
    tagText: "OPM HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/Zx31bB2vMns/maxresdefault.jpg",
    artists: [
      { name: "BINI", avatar: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg" },
      { name: "Maki", avatar: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg" },
      { name: "SB19", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" }
    ],
    seedTrackId: "Zx31bB2vMns"
  },
  {
    id: "rnb_hits",
    title: "R&B Hits",
    subtitle: "DERBIS",
    tagText: "R&B HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/4NRXx6U8ABQ/maxresdefault.jpg",
    artists: [
      { name: "The Weeknd", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
      { name: "SZA", avatar: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg" },
      { name: "Frank Ocean", avatar: "https://i.ytimg.com/vi/JFcgOboQZ08/hqdefault.jpg" }
    ],
    seedTrackId: "4NRXx6U8ABQ"
  }
];

// All Discovery Categories for the Search View (every card has an artist background + 3 circular avatars!)
export const SEARCH_BROWSE_CATEGORIES = [
  ...DERBIS_THE_HITS,
  {
    id: "rock_hits",
    title: "Rock & Alternative",
    subtitle: "DERBIS",
    tagText: "ROCK HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/maxresdefault.jpg",
    artists: [
      { name: "Queen", avatar: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg" },
      { name: "Coldplay", avatar: "https://i.ytimg.com/vi/dvgZkm1xWPE/hqdefault.jpg" },
      { name: "Linkin Park", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" }
    ],
    seedTrackId: "fJ9rUzIMcZQ"
  },
  {
    id: "latin_hits",
    title: "Latin & Reggaeton",
    subtitle: "DERBIS",
    tagText: "LATIN HITS",
    tagOrientation: "top",
    bgArtistPhoto: "https://i.ytimg.com/vi/kJQP7kiw5Fk/maxresdefault.jpg",
    artists: [
      { name: "Luis Fonsi", avatar: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" },
      { name: "Daddy Yankee", avatar: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" },
      { name: "Bad Bunny", avatar: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg" }
    ],
    seedTrackId: "kJQP7kiw5Fk"
  }
];

export const TIDAL_THE_HITS = DERBIS_THE_HITS;
export const EXPLORE_CATEGORIES = SEARCH_BROWSE_CATEGORIES;

// Section 2: Popular Albums matching screenshot shelf
export const POPULAR_ALBUMS = [
  {
    id: "album_hit_me_hard",
    title: "HIT ME HARD AND SOFT",
    artist: "Billie Eilish",
    cover: "https://i.ytimg.com/vi/JFcgOboQZ08/hqdefault.jpg",
    year: "2024",
    seedTrackId: "JFcgOboQZ08"
  },
  {
    id: "album_short_n_sweet",
    title: "Short n' Sweet",
    artist: "Sabrina Carpenter",
    cover: "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg",
    year: "2024",
    seedTrackId: "eVli-tstM5E"
  },
  {
    id: "album_eternal_sunshine",
    title: "eternal sunshine",
    artist: "Ariana Grande",
    cover: "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg",
    year: "2024",
    seedTrackId: "gNi_6U5Pm_o"
  },
  {
    id: "album_tanaw",
    title: "Tanaw",
    artist: "Maki",
    cover: "https://i.ytimg.com/vi/DuKb1yF5Wh4/hqdefault.jpg",
    year: "2024",
    seedTrackId: "DuKb1yF5Wh4"
  },
  {
    id: "album_die_with_a_smile",
    title: "Die With A Smile",
    artist: "Lady Gaga, Bruno Mars",
    cover: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
    year: "2024",
    seedTrackId: "kJQP7kiw5Fk"
  },
  {
    id: "album_pantropiko",
    title: "Talaarawan",
    artist: "BINI",
    cover: "https://i.ytimg.com/vi/Zx31bB2vMns/hqdefault.jpg",
    year: "2024",
    seedTrackId: "Zx31bB2vMns"
  }
];

export const GENRE_SEEDS = {
  pop: QUICK_PICKS_TRACKS.filter(t => t.genre === "pop"),
  rock: HITS_TRACK_SETS.staff_picks,
  hiphop: HITS_TRACK_SETS.rap_hits,
  opm: HITS_TRACK_SETS.opm_hits
};
