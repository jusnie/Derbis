# Japanese Kanji Furigana (振仮名) Implementation in Derbis (SpMp / Kanon Pattern)

## Overview
Following the architectural pattern from **SpMp** and **Kanon** (`sayaka-sh/spmp` & `sayaka-sh/kanon`), Derbis now features native **Japanese Kanji Furigana (Ruby Annotations)** support across both lyric presentation modes (**Apple Music 13** and **Spicy Lyrics**).

While Derbis previously supported multi-script Latin Romanization (Romaji below the line), it now supports native Japanese **Ruby text**: rendering small Hiragana/Katakana readings directly above each Kanji character ($\overset{\text{わたし}}{\text{私}}$, $\overset{\text{せかい}}{\text{世界}}$, $\overset{\text{きみ}}{\text{君}}$) while preserving unified horizontal text baselines and synchronized word-level karaoke lighting.

---

## Technical Architecture

### 1. Kanji Detection & Morphological Tokenizer (`KanjiFurigana.kt`)
* **Kanji Recognition:** Detects CJK ideographs in Unicode blocks `\u4E00..\u9FFF` and `\u3400..\u4DBF`.
* **Explicit Ruby Notation Parser:**
  * **Aozora Bunko Syntax:** `｜漢字《かんじ》` or `漢字《かんじ》` (common in J-Pop digital lyric databases).
  * **Bracketed Reading:** `漢字(かんじ)` or `漢字（かんじ）`.
  * **HTML Ruby Markup:** `<ruby>漢字<rt>かんじ</rt></ruby>`.
* **Built-in J-Pop / Anime Morphological Dictionary:**
  * Contains over 350 of the most frequent Japanese lyrical compound words, okurigana verb/adjective stems, and single Kanji readings (e.g. `世界` $\rightarrow$ `せかい`, `未来` $\rightarrow$ `みらい`, `約束` $\rightarrow$ `やくそく`, `記憶` $\rightarrow$ `きおく`, `運命` $\rightarrow$ `うんめい`, `笑顔` $\rightarrow$ `えがお`, `想い` $\rightarrow$ `おもい`, `輝く` $\rightarrow$ `かがやく`, `抱きしめる` $\rightarrow$ `だきしめる`).
  * Longest-prefix matching ensures multi-kanji compound nouns match first before individual kanji fallback.

### 2. Compose Ruby Layout (`KanjiFuriganaView.kt`)
* **Unified Baseline Alignment:** Each token is composed into an inline vertical column inside an adaptive `FlowRow`. For tokens without ruby text (Kana, spaces, Latin letters), a spacer with height equal to `rubyFontSize` is placed above, guaranteeing that all base characters share the exact same baseline across the line.
* **Synchronous Word-by-Word Karaoke Lighting:** Base Kanji and its Ruby annotations highlight, scale (1.028x), and transition in unison as the playback clock advances.

### 3. Integrated Surfaces
1. **Apple Music 13 Lyrics (`AppleMusicPlayer.kt` & `DerbisApp.kt`)**:
   * Inline lyrics on the player deck dynamically detect Japanese Kanji and render ruby text above the active line.
   * Full-screen lyrics page renders clean Apple-style Furigana with active line magnification.
2. **Spicy Lyrics (`SpicyFullscreenLyrics.kt`)**:
   * Renders bold Kanji with vibrant fiery ambient ruby annotations and glowing drop shadows.
3. **Now Playing Quick Controls Chip**:
   * When Japanese lyrics are detected, a dedicated **"Furigana (振仮名)"** chip appears in the lyrics controls header, allowing listeners to toggle ruby annotations on or off on the fly.

---

## Unit Testing (`KanjiFuriganaTest.kt`)
* Non-Kanji and English lyrics handling.
* CJK Kanji detection accuracy.
* Aozora Bunko, bracketed, and HTML ruby tag parsing.
* J-Pop compound dictionary tokenization on authentic song lines (`君がいた夏の夜 笑顔の記憶`, `世界が終わる前に約束しよう`).
