package com.yasuda.derbis.data

/**
 * Japanese Kanji Furigana (振仮名 / Ruby Annotations) tokenizer & dictionary.
 * Adapted from SpMp / Kanon architecture for J-Pop, Anime, and Vocaloid lyrics.
 *
 * Supports:
 * 1. Explicit Ruby syntax parsing (Aozora ｜漢字《かんじ》, brackets 漢字(かんじ), <ruby> tags).
 * 2. Morphological dictionary mapping for common Japanese song Kanji words, compounds, and verbs.
 */
data class FuriganaSegment(
    val baseText: String,
    val rubyText: String = ""
) {
    val hasRuby: Boolean get() = rubyText.isNotBlank()
}

object KanjiFurigana {
    private val KANJI_RANGE = Regex("[\\u4E00-\\u9FFF\\u3400-\\u4DBF]")
    private val KANA_RANGE = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF]")

    /** Matches explicit ruby like ｜漢字《かんじ》 or 漢字《かんじ》 */
    private val AOZORA_RUBY_REGEX = Regex("(?:｜|\\|)?([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)《([\\u3040-\\u309F\\u30A0-\\u30FF]+)》")

    /** Matches parenthetical ruby like 漢字(かんじ) or 漢字（かんじ） */
    private val PAREN_RUBY_REGEX = Regex("([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)[(（]([\\u3040-\\u309F\\u30A0-\\u30FF]+)[)）]")

    /** Matches HTML-style ruby tags <ruby>漢字<rt>かんじ</rt></ruby> */
    private val HTML_RUBY_REGEX = Regex("<ruby>([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)(?:<rp>.*?</rp>)?<rt>([\\u3040-\\u309F\\u30A0-\\u30FF]+)</rt>(?:<rp>.*?</rp>)?</ruby>")

    fun containsKanji(text: String): Boolean = KANJI_RANGE.containsMatchIn(text)

    fun containsJapanese(text: String): Boolean = KANJI_RANGE.containsMatchIn(text) || KANA_RANGE.containsMatchIn(text)

    /**
     * Parses a lyric line into a list of [FuriganaSegment] items with base text and ruby readings.
     */
    fun parse(lineText: String): List<FuriganaSegment> {
        val trimmed = lineText.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. If explicit ruby tags exist, parse them with highest priority
        if (hasExplicitRuby(trimmed)) {
            val parsedExplicit = parseExplicitRuby(trimmed)
            if (parsedExplicit.isNotEmpty()) return parsedExplicit
        }

        // 2. If line contains no Kanji, return as single segment without ruby
        if (!containsKanji(trimmed)) {
            return listOf(FuriganaSegment(trimmed))
        }

        // 3. Morphological dictionary tokenizer (longest-match first)
        return tokenizeWithJPopDictionary(trimmed)
    }

    private fun hasExplicitRuby(text: String): Boolean =
        AOZORA_RUBY_REGEX.containsMatchIn(text) ||
            HTML_RUBY_REGEX.containsMatchIn(text) ||
            PAREN_RUBY_REGEX.containsMatchIn(text)

    private fun parseExplicitRuby(text: String): List<FuriganaSegment> {
        val result = mutableListOf<FuriganaSegment>()
        var cursor = 0

        // Combined pattern for explicit ruby
        val combinedRegex = Regex("(?:(?:｜|\\|)?([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)《([\\u3040-\\u309F\\u30A0-\\u30FF]+)》|<ruby>([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)(?:<rp>.*?</rp>)?<rt>([\\u3040-\\u309F\\u30A0-\\u30FF]+)</rt>(?:<rp>.*?</rp>)?</ruby>|([\\u4E00-\\u9FFF\\u3400-\\u4DBF]+)[(（]([\\u3040-\\u309F\\u30A0-\\u30FF]+)[)）])")

        for (match in combinedRegex.findAll(text)) {
            if (match.range.first > cursor) {
                val nonRubyChunk = text.substring(cursor, match.range.first)
                result.addAll(tokenizeWithJPopDictionary(nonRubyChunk))
            }
            val kanji = match.groupValues[1].ifBlank {
                match.groupValues[3].ifBlank { match.groupValues[5] }
            }
            val reading = match.groupValues[2].ifBlank {
                match.groupValues[4].ifBlank { match.groupValues[6] }
            }
            result.add(FuriganaSegment(kanji, reading))
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            val remaining = text.substring(cursor)
            result.addAll(tokenizeWithJPopDictionary(remaining))
        }

        return result
    }

    private fun tokenizeWithJPopDictionary(text: String): List<FuriganaSegment> {
        val segments = mutableListOf<FuriganaSegment>()
        var i = 0
        val nonKanjiBuffer = StringBuilder()

        while (i < text.length) {
            var matched = false

            // Try compound words (longest match from 5 chars down to 2 chars)
            val maxLen = minOf(6, text.length - i)
            for (len in maxLen downTo 2) {
                val sub = text.substring(i, i + len)
                val reading = JPOP_COMPOUND_DICTIONARY[sub]
                if (reading != null) {
                    if (nonKanjiBuffer.isNotEmpty()) {
                        segments.add(FuriganaSegment(nonKanjiBuffer.toString()))
                        nonKanjiBuffer.clear()
                    }
                    segments.add(FuriganaSegment(sub, reading))
                    i += len
                    matched = true
                    break
                }
            }

            if (matched) continue

            // Single character check
            val charStr = text.substring(i, i + 1)
            val singleReading = JPOP_SINGLE_DICTIONARY[charStr]
            if (singleReading != null) {
                if (nonKanjiBuffer.isNotEmpty()) {
                    segments.add(FuriganaSegment(nonKanjiBuffer.toString()))
                    nonKanjiBuffer.clear()
                }
                segments.add(FuriganaSegment(charStr, singleReading))
                i += 1
            } else {
                nonKanjiBuffer.append(text[i])
                i += 1
            }
        }

        if (nonKanjiBuffer.isNotEmpty()) {
            segments.add(FuriganaSegment(nonKanjiBuffer.toString()))
        }

        return segments
    }

    // High-precision J-Pop, Anime, and Vocaloid lyric dictionary
    private val JPOP_COMPOUND_DICTIONARY: Map<String, String> = mapOf(
        // Emotions & Soul
        "世界" to "せかい",
        "未来" to "みらい",
        "過去" to "かこ",
        "永遠" to "えいえん",
        "約束" to "やくそく",
        "記憶" to "きおく",
        "運命" to "うんめい",
        "奇跡" to "きせき",
        "真実" to "しんじつ",
        "笑顔" to "えがお",
        "優しさ" to "やさしさ",
        "強さ" to "つよさ",
        "悲しみ" to "かなしみ",
        "切なさ" to "せつなさ",
        "孤独" to "こどく",
        "自由" to "じゆう",
        "希望" to "きぼう",
        "絶望" to "ぜつぼう",
        "時間" to "じかん",
        "瞬間" to "しゅんかん",
        "言葉" to "ことば",
        "本当" to "ほんとう",
        "今日" to "きょう",
        "明日" to "あした",
        "昨日" to "きのう",
        "今夜" to "こんや",
        "何故" to "なぜ",
        "自分" to "じぶん",
        "誰か" to "だれか",
        "一人" to "ひとり",
        "二人" to "ふたり",
        "彼女" to "かのじょ",
        "彼氏" to "かれし",
        "友達" to "ともだち",
        "仲間" to "なかま",
        "恋人" to "こいびと",
        "生命" to "いのち",
        "景色" to "けしき",
        "季節" to "きせつ",
        "理由" to "りゆう",
        "秘密" to "ひみつ",
        "宇宙" to "うちゅう",
        "地球" to "ちきゅう",
        "太陽" to "たいよう",
        "満月" to "まんげつ",
        "三日月" to "みかづき",
        "青空" to "あおぞら",
        "夜空" to "よぞら",
        "星空" to "ほしぞら",
        "夕暮れ" to "ゆうぐれ",
        "夕焼け" to "ゆうやけ",
        "朝焼け" to "あさやけ",
        "帰り道" to "かえりみち",
        "旅人" to "たびびと",
        "交差点" to "こうさてん",
        "足音" to "あしおと",
        "鼓動" to "こどう",
        "呼吸" to "こきゅう",
        "温度" to "おんど",
        "温もり" to "ぬくもり",
        "手帳" to "てちょう",
        "写真" to "しゃしん",
        "手紙" to "てがみ",
        "涙色" to "なみだいろ",

        // Okurigana Verbs & Adjectives (Common lyrics stems)
        "想い" to "おもい",
        "思い" to "おもい",
        "思い出" to "おもいで",
        "願い" to "ねがい",
        "祈り" to "いのり",
        "叫び" to "さけび",
        "迷い" to "まよい",
        "生きる" to "いきる",
        "生きて" to "いきて",
        "愛しい" to "いとしい",
        "切ない" to "せつない",
        "美しい" to "うつくしい",
        "優しい" to "やさしい",
        "眩しい" to "まぶしい",
        "悲しい" to "かなしい",
        "寂しい" to "さびしい",
        "嬉しい" to "うれしい",
        "儚い" to "はかない",
        "温かい" to "あたたかい",
        "歩き出す" to "あるきだす",
        "走り出す" to "はしりだす",
        "動き出す" to "うごきだす",
        "見つめる" to "みつめる",
        "見つめて" to "みつめて",
        "抱きしめる" to "だきしめる",
        "抱きしめて" to "だきしめて",
        "抱きしめた" to "だきしめた",
        "忘れない" to "わすれない",
        "忘れて" to "わすれて",
        "溢れる" to "あふれる",
        "溢れ" to "あふれ",
        "揺れる" to "ゆれる",
        "流れる" to "ながれる",
        "輝く" to "かがやく",
        "輝いて" to "かがやいて",
        "響く" to "ひびく",
        "響いて" to "ひびいて",
        "繋ぐ" to "つなぐ",
        "繋いで" to "つないで",
        "届く" to "とどく",
        "届いて" to "とどいて",
        "届かない" to "とどかない",
        "感じる" to "かんじる",
        "感じて" to "かんじて",
        "信じる" to "しんじる",
        "信じて" to "しんじて",
        "眠る" to "ねむる",
        "目覚める" to "めざめる",
        "消える" to "きえる",
        "消えて" to "きえて",
        "始まる" to "はじまる",
        "終わる" to "おわる",
        "変わる" to "かわる",
        "変わらない" to "かわらない",
        "旅立つ" to "たびだつ",
        "出会い" to "であい",
        "さよなら" to "さよなら"
    )

    private val JPOP_SINGLE_DICTIONARY: Map<String, String> = mapOf(
        "私" to "わたし",
        "僕" to "ぼく",
        "君" to "きみ",
        "俺" to "おれ",
        "愛" to "あい",
        "恋" to "こい",
        "心" to "こころ",
        "夢" to "ゆめ",
        "時" to "とき",
        "空" to "そら",
        "海" to "うみ",
        "風" to "かぜ",
        "雨" to "あめ",
        "雪" to "ゆき",
        "星" to "ほし",
        "月" to "つき",
        "夜" to "よる",
        "朝" to "あさ",
        "光" to "ひかり",
        "影" to "かげ",
        "闇" to "やみ",
        "花" to "はな",
        "桜" to "さくら",
        "声" to "こえ",
        "音" to "おと",
        "歌" to "うた",
        "詩" to "うた",
        "涙" to "なみだ",
        "道" to "みち",
        "街" to "まち",
        "旅" to "たび",
        "手" to "て",
        "目" to "め",
        "胸" to "むね",
        "瞳" to "ひとみ",
        "今" to "いま",
        "春" to "はる",
        "夏" to "なつ",
        "秋" to "あき",
        "冬" to "ふゆ",
        "誰" to "だれ",
        "人" to "ひと",
        "何" to "なに",
        "色" to "いろ",
        "白" to "しろ",
        "黒" to "くろ",
        "赤" to "あか",
        "青" to "あお",
        "嘘" to "うそ",
        "虹" to "にじ",
        "波" to "なみ",
        "扉" to "とびら",
        "鍵" to "かぎ",
        "針" to "はり",
        "炎" to "ほのお",
        "鳥" to "とり",
        "羽" to "はね",
        "翼" to "つばさ"
    )
}
