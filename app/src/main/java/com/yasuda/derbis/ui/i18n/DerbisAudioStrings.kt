package com.yasuda.derbis.ui.i18n

private fun audioStrings(
    quality: String,
    equalizer: String,
    spatial: String,
    dynamics: String,
    playback: String,
    reset: String,
    custom: String,
    bands: String,
    medium: String = "Medium"
): Map<String, String> = mapOf(
    "audioSectionQuality" to quality,
    "audioSectionEqualizer" to equalizer,
    "audioSectionSpatial" to spatial,
    "audioSectionDynamics" to dynamics,
    "audioSectionPlayback" to playback,
    "audioResetEqualizer" to reset,
    "audioPresetCustom" to custom,
    "audioBands" to bands,
    "audioQualityMedium" to medium
)

private val audioBundles: Map<String, Map<String, String>> = mapOf(
    "en" to audioStrings("Streaming quality", "Equalizer", "Spatial sound", "Dynamics", "Playback", "Reset", "Custom", "Bands", "Medium"),
    "it" to audioStrings("Qualità streaming", "Equalizzatore", "Suono spaziale", "Dinamica", "Riproduzione", "Reimposta", "Personalizzato", "Bande", "Medio"),
    "es" to audioStrings("Calidad de streaming", "Ecualizador", "Sonido espacial", "Dinámica", "Reproducción", "Restablecer", "Personalizado", "Bandas", "Media"),
    "fr" to audioStrings("Qualité de streaming", "Égaliseur", "Son spatial", "Dynamique", "Lecture", "Réinitialiser", "Personnalisé", "Bandes", "Moyenne"),
    "de" to audioStrings("Streaming-Qualität", "Equalizer", "Raumklang", "Dynamik", "Wiedergabe", "Zurücksetzen", "Benutzerdefiniert", "Bänder", "Mittel"),
    "pt" to audioStrings("Qualidade de streaming", "Equalizador", "Som espacial", "Dinâmica", "Reprodução", "Repor", "Personalizado", "Bandas", "Média"),
    "nl" to audioStrings("Streamingkwaliteit", "Equalizer", "Ruimtelijk geluid", "Dynamiek", "Afspelen", "Herstellen", "Aangepast", "Banden", "Gemiddeld"),
    "pl" to audioStrings("Jakość streamingu", "Korektor", "Dźwięk przestrzenny", "Dynamika", "Odtwarzanie", "Resetuj", "Własny", "Pasma", "Średnia"),
    "ro" to audioStrings("Calitate streaming", "Egalizator", "Sunet spațial", "Dinamică", "Redare", "Resetează", "Personalizat", "Benzi", "Medie"),
    "el" to audioStrings("Ποιότητα streaming", "Ισοσταθμιστής", "Χωρικός ήχος", "Δυναμική", "Αναπαραγωγή", "Επαναφορά", "Προσαρμοσμένο", "Μπάντες", "Μεσαία"),
    "sv" to audioStrings("Streamingkvalitet", "Equalizer", "Rumsligt ljud", "Dynamik", "Uppspelning", "Återställ", "Anpassad", "Band", "Medium"),
    "da" to audioStrings("Streamingkvalitet", "Equalizer", "Rumlig lyd", "Dynamik", "Afspilning", "Nulstil", "Tilpasset", "Bånd", "Mellem"),
    "cs" to audioStrings("Kvalita streamování", "Ekvalizér", "Prostorový zvuk", "Dynamika", "Přehrávání", "Obnovit", "Vlastní", "Pásma", "Střední"),
    "uk" to audioStrings("Якість стримінгу", "Еквалайзер", "Просторовий звук", "Динаміка", "Відтворення", "Скинути", "Власний", "Смуги", "Середня"),
    "ru" to audioStrings("Качество стриминга", "Эквалайзер", "Пространственный звук", "Динамика", "Воспроизведение", "Сбросить", "Свой", "Полосы", "Среднее"),
    "tr" to audioStrings("Yayın kalitesi", "Ekolayzer", "Uzamsal ses", "Dinamik", "Oynatma", "Sıfırla", "Özel", "Bantlar", "Orta"),
    "ar" to audioStrings("جودة البث", "المعادل", "الصوت المكاني", "الديناميكية", "التشغيل", "إعادة الضبط", "مخصص", "النطاقات", "متوسطة"),
    "zh" to audioStrings("流媒体音质", "均衡器", "空间音效", "动态", "播放", "重置", "自定义", "频段", "中"),
    "ja" to audioStrings("ストリーミング音質", "イコライザー", "空間オーディオ", "ダイナミクス", "再生", "リセット", "カスタム", "バンド", "中"),
    "ko" to audioStrings("스트리밍 음질", "이퀄라이저", "공간 음향", "다이내믹", "재생", "초기화", "사용자 설정", "밴드", "보통"),
    "hi" to audioStrings("स्ट्रीमिंग गुणवत्ता", "इक्वलाइज़र", "स्थानिक ध्वनि", "डायनामिक्स", "प्लेबैक", "रीसेट", "कस्टम", "बैंड", "मध्यम"),
    "id" to audioStrings("Kualitas streaming", "Equalizer", "Suara spasial", "Dinamika", "Pemutaran", "Atur ulang", "Kustom", "Band", "Sedang"),
    "vi" to audioStrings("Chất lượng phát trực tuyến", "Bộ chỉnh âm", "Âm thanh không gian", "Dải động", "Phát nhạc", "Đặt lại", "Tùy chỉnh", "Dải tần", "Trung bình"),
    "th" to audioStrings("คุณภาพสตรีมมิง", "อีควอไลเซอร์", "เสียงรอบทิศทาง", "ไดนามิก", "การเล่น", "รีเซ็ต", "กำหนดเอง", "ย่านความถี่", "ปานกลาง"),
    "fil" to audioStrings("Kalidad ng streaming", "Equalizer", "Spatial na tunog", "Dynamics", "Pag-playback", "I-reset", "Custom", "Mga banda", "Katamtaman"),
    "he" to audioStrings("איכות סטרימינג", "אקולייזר", "צליל מרחבי", "דינמיקה", "השמעה", "איפוס", "מותאם אישית", "רצועות", "בינונית"),
    "fi" to audioStrings("Suoratoiston laatu", "Taajuuskorjain", "Tilaääni", "Dynamiikka", "Toisto", "Nollaa", "Mukautettu", "Kaistat", "Keskitaso"),
    "et" to audioStrings("Voogesituse kvaliteet", "Ekvalaiser", "Ruumiline heli", "Dünaamika", "Taasesitus", "Lähtesta", "Kohandatud", "Sagedusribad", "Keskmine")
)

internal fun audioLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(audioBundles, code)

internal fun audioLocalizationCodes(): Set<String> = supportedLocalizationCodes()
