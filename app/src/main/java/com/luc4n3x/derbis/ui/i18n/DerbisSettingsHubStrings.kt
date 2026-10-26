package com.luc4n3x.derbis.ui.i18n

enum class SettingsHubText { Studios, StudiosHint, Appearance, Listening, Library, Accounts, System }

class SettingsHubCopy internal constructor(private val texts: Map<SettingsHubText, String>) {
    operator fun get(key: SettingsHubText): String = texts[key] ?: englishSettingsHub.getValue(key)

    internal val complete: Boolean
        get() = texts.size == SettingsHubText.entries.size
}

private fun settingsHub(vararg value: String): Map<SettingsHubText, String> =
    if (value.size == SettingsHubText.entries.size) SettingsHubText.entries.zip(value).toMap() else emptyMap()

private val settingsHubTexts: Map<String, Map<SettingsHubText, String>> = mapOf(
    "en" to settingsHub("Studios", "Shape how Derbis looks", "Look and feel", "Listening", "Library and downloads", "Accounts and services", "System"),
    "it" to settingsHub("Studio", "Dai forma all'aspetto di Derbis", "Aspetto", "Ascolto", "Libreria e download", "Account e servizi", "Sistema"),
    "es" to settingsHub("Estudios", "Da forma al aspecto de Derbis", "Apariencia", "Escucha", "Biblioteca y descargas", "Cuentas y servicios", "Sistema"),
    "fr" to settingsHub("Studios", "Façonnez l'apparence de Derbis", "Apparence", "Écoute", "Bibliothèque et téléchargements", "Comptes et services", "Système"),
    "de" to settingsHub("Studios", "Gestalte das Aussehen von Derbis", "Aussehen", "Hören", "Mediathek und Downloads", "Konten und Dienste", "System"),
    "pt" to settingsHub("Estúdios", "Molde a aparência do Derbis", "Aparência", "Audição", "Biblioteca e downloads", "Contas e serviços", "Sistema"),
    "nl" to settingsHub("Studio's", "Bepaal hoe Derbis eruitziet", "Uiterlijk", "Luisteren", "Bibliotheek en downloads", "Accounts en diensten", "Systeem"),
    "pl" to settingsHub("Studia", "Nadaj Derbis własny wygląd", "Wygląd", "Słuchanie", "Biblioteka i pobrane", "Konta i usługi", "System"),
    "ro" to settingsHub("Studiouri", "Modelează aspectul Derbis", "Aspect", "Ascultare", "Bibliotecă și descărcări", "Conturi și servicii", "Sistem"),
    "el" to settingsHub("Studio", "Διαμορφώστε την εμφάνιση του Derbis", "Εμφάνιση", "Ακρόαση", "Βιβλιοθήκη και λήψεις", "Λογαριασμοί και υπηρεσίες", "Σύστημα"),
    "sv" to settingsHub("Studior", "Forma hur Derbis ser ut", "Utseende", "Lyssning", "Bibliotek och nedladdningar", "Konton och tjänster", "System"),
    "da" to settingsHub("Studier", "Form Derbiss udseende", "Udseende", "Lytning", "Bibliotek og downloads", "Konti og tjenester", "System"),
    "cs" to settingsHub("Studia", "Upravte vzhled Derbis", "Vzhled", "Poslech", "Knihovna a stahování", "Účty a služby", "Systém"),
    "sk" to settingsHub("Štúdiá", "Upravte vzhľad Derbis", "Vzhľad", "Počúvanie", "Knižnica a sťahovanie", "Účty a služby", "Systém"),
    "bn" to settingsHub("স্টুডিও", "Derbis-র চেহারা সাজান", "চেহারা", "শোনা", "লাইব্রেরি ও ডাউনলোড", "অ্যাকাউন্ট ও পরিষেবা", "সিস্টেম"),
    "kk" to settingsHub("Студиялар", "Derbis көрінісін өзгертіңіз", "Көрініс", "Тыңдау", "Кітапхана және жүктеулер", "Аккаунттар мен қызметтер", "Жүйе"),
    "sr" to settingsHub("Студији", "Обликујте изглед апликације Derbis", "Изглед", "Слушање", "Библиотека и преузимања", "Налози и услуге", "Систем"),
    "sq" to settingsHub("Studiot", "Jepi formë pamjes së Derbis", "Pamja", "Dëgjimi", "Biblioteka dhe shkarkimet", "Llogaritë dhe shërbimet", "Sistemi"),
    "hr" to settingsHub("Studiji", "Oblikujte izgled aplikacije Derbis", "Izgled", "Slušanje", "Knjižnica i preuzimanja", "Računi i usluge", "Sustav"),
    "bg" to settingsHub("Студия", "Оформете вида на Derbis", "Външен вид", "Слушане", "Библиотека и изтегляния", "Акаунти и услуги", "Система"),
    "hu" to settingsHub("Stúdiók", "Formáld a Derbis megjelenését", "Megjelenés", "Hallgatás", "Könyvtár és letöltések", "Fiókok és szolgáltatások", "Rendszer"),
    "fi" to settingsHub("Studiot", "Muokkaa Derbisn ulkoasua", "Ulkoasu", "Kuuntelu", "Kirjasto ja lataukset", "Tilit ja palvelut", "Järjestelmä"),
    "et" to settingsHub("Stuudiod", "Kujunda Derbis välimus", "Välimus", "Kuulamine", "Kogu ja allalaadimised", "Kontod ja teenused", "Süsteem"),
    "nb" to settingsHub("Studioer", "Form hvordan Derbis ser ut", "Utseende", "Lytting", "Bibliotek og nedlastinger", "Kontoer og tjenester", "System"),
    "ca" to settingsHub("Estudis", "Dona forma a l'aspecte de Derbis", "Aparença", "Escolta", "Biblioteca i baixades", "Comptes i serveis", "Sistema"),
    "uk" to settingsHub("Студії", "Налаштуйте вигляд Derbis", "Вигляд", "Прослуховування", "Бібліотека й завантаження", "Облікові записи й сервіси", "Система"),
    "ru" to settingsHub("Студии", "Настройте внешний вид Derbis", "Внешний вид", "Прослушивание", "Библиотека и загрузки", "Аккаунты и сервисы", "Система"),
    "tr" to settingsHub("Stüdyolar", "Derbis'nın görünümünü şekillendirin", "Görünüm", "Dinleme", "Kitaplık ve indirmeler", "Hesaplar ve hizmetler", "Sistem"),
    "ar" to settingsHub("الاستوديوهات", "شكّل مظهر Derbis", "المظهر", "الاستماع", "المكتبة والتنزيلات", "الحسابات والخدمات", "النظام"),
    "fa" to settingsHub("استودیوها", "ظاهر Derbis را شکل دهید", "ظاهر", "شنیدن", "کتابخانه و دانلودها", "حساب‌ها و سرویس‌ها", "سیستم"),
    "zh" to settingsHub("工作室", "塑造 Derbis 的外观", "外观", "收听", "音乐库与下载", "账号与服务", "系统"),
    "zh-Hant" to settingsHub("工作室", "打造 Derbis 的外觀", "外觀", "聆聽", "音樂庫與下載", "帳戶與服務", "系統"),
    "ja" to settingsHub("スタジオ", "Derbisの見た目を整える", "外観", "リスニング", "ライブラリとダウンロード", "アカウントとサービス", "システム"),
    "ko" to settingsHub("스튜디오", "Derbis의 모습을 꾸며 보세요", "모양", "감상", "보관함 및 다운로드", "계정 및 서비스", "시스템"),
    "hi" to settingsHub("स्टूडियो", "Derbis का लुक बनाएँ", "रूप", "सुनना", "लाइब्रेरी और डाउनलोड", "खाते और सेवाएँ", "सिस्टम"),
    "id" to settingsHub("Studio", "Atur tampilan Derbis", "Tampilan", "Mendengarkan", "Koleksi dan unduhan", "Akun dan layanan", "Sistem"),
    "ms" to settingsHub("Studio", "Bentuk rupa Derbis", "Rupa", "Mendengar", "Pustaka dan muat turun", "Akaun dan perkhidmatan", "Sistem"),
    "vi" to settingsHub("Studio", "Tùy biến giao diện Derbis", "Giao diện", "Nghe nhạc", "Thư viện và tải xuống", "Tài khoản và dịch vụ", "Hệ thống"),
    "th" to settingsHub("สตูดิโอ", "ปรับแต่งหน้าตาของ Derbis", "รูปลักษณ์", "การฟัง", "คลังเพลงและดาวน์โหลด", "บัญชีและบริการ", "ระบบ"),
    "fil" to settingsHub("Mga Studio", "Hubugin ang itsura ng Derbis", "Itsura", "Pakikinig", "Library at mga download", "Mga account at serbisyo", "System"),
    "he" to settingsHub("סטודיו", "עצבו את המראה של Derbis", "מראה", "האזנה", "ספרייה והורדות", "חשבונות ושירותים", "מערכת")
)

private val englishSettingsHub: Map<SettingsHubText, String> = settingsHubTexts.getValue("en")

private val settingsHubCopies: Map<String, SettingsHubCopy> by lazy {
    settingsHubTexts.mapValues { (_, texts) -> SettingsHubCopy(texts) }
}

internal fun settingsHubLocalizationCodes(): Set<String> = settingsHubCopies.keys

internal fun settingsHubIncompleteLocalizationCodes(): Set<String> =
    settingsHubCopies.filterValues { !it.complete }.keys

internal val DerbisStrings.settingsHubCopy: SettingsHubCopy
    get() = settingsHubCopies[code] ?: settingsHubCopies.getValue("en")
