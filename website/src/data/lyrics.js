export const SYNCHRONIZED_LYRICS = {
  "kPa7bsKwL-c": [
    { time: 13, text: "I, I just woke up from a dream" },
    { time: 17, text: "Where you and I had to say goodbye" },
    { time: 22, text: "And I don't know what it all means" },
    { time: 26, text: "But since I survived, I realized" },
    { time: 31, text: "Wherever you go, that's where I'll follow" },
    { time: 36, text: "Nobody's promised tomorrow" },
    { time: 40, text: "So I'ma love you every night like it's the last night" },
    { time: 47, text: "Like it's the last night" },
    { time: 50, text: "If the world was ending, I'd wanna be next to you" },
    { time: 59, text: "If the party was over and our time on Earth was through" },
    { time: 69, text: "I'd wanna hold you just for a while" },
    { time: 74, text: "And die with a smile" },
    { time: 79, text: "If the world was ending, I'd wanna be next to you" }
  ],
  "eVli-tstM5E": [
    { time: 8, text: "Now he's thinkin' 'bout me every night, oh" },
    { time: 13, text: "Is it that sweet? I guess so" },
    { time: 17, text: "Say you can't sleep, baby, I know" },
    { time: 21, text: "That's that me espresso" },
    { time: 25, text: "Move it up, down, left, right, oh" },
    { time: 29, text: "Switch it up like Nintendo" },
    { time: 33, text: "Say you can't sleep, baby, I know" },
    { time: 37, text: "That's that me espresso" }
  ],
  "V9PVRfjEBTI": [
    { time: 4, text: "I want you to stay" },
    { time: 8, text: "'Til I'm in the grave" },
    { time: 11, text: "'Til I rot away, dead and buried" },
    { time: 15, text: "'Til I'm in the casket you carry" },
    { time: 19, text: "If you go, I'm goin' too, uh" },
    { time: 23, text: "'Cause it was always you, alright" },
    { time: 28, text: "And if I'm turnin' blue, please don't save me" },
    { time: 32, text: "Nothin' in this world to distrust" },
    { time: 36, text: "Birds of a feather, we should stick together, I know" }
  ],
  "DuKb1yF5Wh4": [
    { time: 8, text: "Tinititigan ang langit na kay ganda" },
    { time: 16, text: "Kulay kahel ang ulap sa tuwing kasama ka" },
    { time: 24, text: "Sa bawat dampi ng hangin, may dalang kaba" },
    { time: 32, text: "Baka panaginip lang ang lahat ng ito sinta" },
    { time: 40, text: "Kahel na langit, ikaw ang aking pahinga" },
    { time: 48, text: "Sa lilim ng takipsilim, tayo'y magkasama" }
  ],
  "g5qU7p7yOY8": [
    { time: 12, text: "Tell me something I need to know" },
    { time: 16, text: "Then take my breath and never let it go" },
    { time: 21, text: "If you just let me invade your space" },
    { time: 25, text: "I'll take the pleasure, take it with the pain" },
    { time: 30, text: "And if in the moment you mean what you say" },
    { time: 35, text: "The love me hard, love me, love me harder" },
    { time: 40, text: "'Cause if it makes me better, and make you real" },
    { time: 45, text: "Then love me, love me, love me harder" }
  ],
  "akap": [
    { time: 6, text: "Nanginig ang aking mga kamay" },
    { time: 14, text: "Nang ika'y masilayan sa dulo ng tulay" },
    { time: 22, text: "Ikaw lamang ang tangi kong inaasam" },
    { time: 31, text: "Makapiling ka kahit na sandali lang" },
    { time: 39, text: "Sa bawat saglit, ikaw ang hanap" },
    { time: 47, text: "Tunay na ligaya sa iyong yakap" },
    { time: 55, text: "Hahawiin ang ulap sa madilim na gabi" },
    { time: 63, text: "Basta't ika'y laging katabi" },
    { time: 70, text: "Ikaw lamang ang tangi kong inaasam" }
  ]
};

export function getTrackLyrics(track) {
  if (!track) return [];
  const id = track.id || track.videoId || "";
  return SYNCHRONIZED_LYRICS[id] || [];
}
