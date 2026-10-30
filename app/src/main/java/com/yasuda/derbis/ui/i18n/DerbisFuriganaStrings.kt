package com.yasuda.derbis.ui.i18n

internal val DerbisStrings.lyricsFurigana: String
    get() = if (code == "ja") "振仮名" else if (code == "zh") "假名注音" else "Furigana"

internal val DerbisStrings.lyricsFuriganaSubtitle: String
    get() = if (code == "ja") {
        "漢字の上にひらがなの読み（ルビ）を表示します。"
    } else if (code == "it") {
        "Mostra le annotazioni di lettura Hiragana sopra i Kanji giapponesi."
    } else {
        "Show Hiragana reading annotations (ruby) above Japanese Kanji."
    }
