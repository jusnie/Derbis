package com.luc4n3x.derbis.ui.i18n

internal val offlineHomeKeys = setOf(
    "offlineHomeTitle",
    "offlineHomeMessage",
    "offlineHomeRetry",
    "offlineHomeDownloads",
    "offlineHomePlaylists",
    "offlineHomeFavorites",
    "offlineHomeRecent",
    "homeRemoteUnavailable",
    "homeRemoteEmpty"
)

private fun offlineHome(
    title: String,
    message: String,
    retry: String,
    downloads: String,
    playlists: String,
    favorites: String,
    recent: String,
    remoteUnavailable: String,
    remoteEmpty: String
): Map<String, String> = mapOf(
    "offlineHomeTitle" to title,
    "offlineHomeMessage" to message,
    "offlineHomeRetry" to retry,
    "offlineHomeDownloads" to downloads,
    "offlineHomePlaylists" to playlists,
    "offlineHomeFavorites" to favorites,
    "offlineHomeRecent" to recent,
    "homeRemoteUnavailable" to remoteUnavailable,
    "homeRemoteEmpty" to remoteEmpty
)

private val offlineHomeBundles: Map<String, Map<String, String>> = mapOf(
    "en" to offlineHome("You're offline", "Derbis is showing the music saved on this device.", "Retry", "Downloads", "Offline playlists", "Favorites", "Recently played", "Home is unavailable right now", "Remote Home is empty — try a search"),
    "it" to offlineHome("Sei offline", "Derbis mostra la musica salvata su questo dispositivo.", "Riprova", "Download", "Playlist offline", "Preferiti", "Ascoltati di recente", "Home non disponibile", "Home remota vuota: prova una ricerca"),
    "es" to offlineHome("Estás sin conexión", "Derbis muestra la música guardada en este dispositivo.", "Reintentar", "Descargas", "Listas sin conexión", "Favoritos", "Reproducidas hace poco", "La Home no está disponible ahora", "La Home remota está vacía: prueba una búsqueda"),
    "fr" to offlineHome("Vous êtes hors ligne", "Derbis affiche la musique enregistrée sur cet appareil.", "Réessayer", "Téléchargements", "Playlists hors ligne", "Favoris", "Écoutés récemment", "L'accueil est indisponible pour le moment", "L'accueil distant est vide — essayez une recherche"),
    "de" to offlineHome("Du bist offline", "Derbis zeigt die auf diesem Gerät gespeicherte Musik.", "Erneut versuchen", "Downloads", "Offline-Playlists", "Favoriten", "Zuletzt gehört", "Start ist gerade nicht verfügbar", "Der entfernte Start ist leer — versuche eine Suche"),
    "pt" to offlineHome("Você está offline", "O Derbis mostra a música salva neste dispositivo.", "Tentar de novo", "Downloads", "Playlists offline", "Favoritos", "Ouvidas recentemente", "A Home não está disponível agora", "A Home remota está vazia — tente uma busca"),
    "nl" to offlineHome("Je bent offline", "Derbis toont de muziek die op dit apparaat is opgeslagen.", "Opnieuw proberen", "Downloads", "Offline afspeellijsten", "Favorieten", "Onlangs afgespeeld", "Home is nu niet beschikbaar", "De externe Home is leeg — probeer een zoekopdracht"),
    "pl" to offlineHome("Jesteś offline", "Derbis pokazuje muzykę zapisaną na tym urządzeniu.", "Spróbuj ponownie", "Pobrane", "Playlisty offline", "Ulubione", "Ostatnio odtwarzane", "Strona główna jest teraz niedostępna", "Zdalna strona główna jest pusta — spróbuj wyszukać"),
    "ro" to offlineHome("Ești offline", "Derbis afișează muzica salvată pe acest dispozitiv.", "Reîncearcă", "Descărcări", "Playlisturi offline", "Favorite", "Ascultate recent", "Pagina principală nu este disponibilă acum", "Pagina principală de la distanță este goală — încearcă o căutare"),
    "el" to offlineHome("Είσαι εκτός σύνδεσης", "Το Derbis δείχνει τη μουσική που είναι αποθηκευμένη σε αυτή τη συσκευή.", "Δοκίμασε ξανά", "Λήψεις", "Λίστες εκτός σύνδεσης", "Αγαπημένα", "Πρόσφατες ακροάσεις", "Η αρχική δεν είναι διαθέσιμη τώρα", "Η απομακρυσμένη αρχική είναι άδεια — δοκίμασε μια αναζήτηση"),
    "sv" to offlineHome("Du är offline", "Derbis visar musiken som är sparad på den här enheten.", "Försök igen", "Nedladdningar", "Offlinespellistor", "Favoriter", "Nyligen spelade", "Startsidan är inte tillgänglig just nu", "Fjärrstartsidan är tom — prova en sökning"),
    "da" to offlineHome("Du er offline", "Derbis viser musikken, der er gemt på denne enhed.", "Prøv igen", "Downloads", "Offlineplaylister", "Favoritter", "Senest afspillet", "Hjem er ikke tilgængelig lige nu", "Fjern-hjem er tom — prøv en søgning"),
    "cs" to offlineHome("Jsi offline", "Derbis zobrazuje hudbu uloženou v tomto zařízení.", "Zkusit znovu", "Stažené", "Offline playlisty", "Oblíbené", "Nedávno přehrané", "Domů právě není dostupné", "Vzdálená domovská stránka je prázdná — zkus vyhledávání"),
    "uk" to offlineHome("Ви офлайн", "Derbis показує музику, збережену на цьому пристрої.", "Повторити", "Завантаження", "Офлайн-плейлисти", "Улюблені", "Нещодавно прослухані", "Головна зараз недоступна", "Віддалена головна порожня — спробуйте пошук"),
    "ru" to offlineHome("Вы офлайн", "Derbis показывает музыку, сохранённую на этом устройстве.", "Повторить", "Загрузки", "Офлайн-плейлисты", "Избранное", "Недавно прослушанные", "Главная сейчас недоступна", "Удалённая главная пуста — попробуйте поиск"),
    "tr" to offlineHome("Çevrimdışısın", "Derbis bu cihazda kayıtlı müzikleri gösteriyor.", "Yeniden dene", "İndirilenler", "Çevrimdışı çalma listeleri", "Favoriler", "Son çalınanlar", "Ana sayfa şu anda kullanılamıyor", "Uzak ana sayfa boş — bir arama dene"),
    "ar" to offlineHome("أنت غير متصل", "يعرض Derbis الموسيقى المحفوظة على هذا الجهاز.", "إعادة المحاولة", "التنزيلات", "قوائم التشغيل دون اتصال", "المفضلة", "المُشغَّلة مؤخرًا", "الصفحة الرئيسية غير متاحة الآن", "الصفحة الرئيسية البعيدة فارغة — جرّب البحث"),
    "zh" to offlineHome("你处于离线状态", "Derbis 正在显示保存在此设备上的音乐。", "重试", "下载内容", "离线播放列表", "收藏", "最近播放", "首页当前不可用", "远程首页为空，试试搜索"),
    "ja" to offlineHome("オフラインです", "Derbis はこの端末に保存された音楽を表示しています。", "再試行", "ダウンロード", "オフラインのプレイリスト", "お気に入り", "最近再生した曲", "ホームは現在利用できません", "リモートのホームが空です。検索してみてください"),
    "ko" to offlineHome("오프라인 상태입니다", "Derbis가 이 기기에 저장된 음악을 표시하고 있습니다.", "다시 시도", "다운로드", "오프라인 재생목록", "즐겨찾기", "최근 재생", "홈을 지금 사용할 수 없습니다", "원격 홈이 비어 있습니다 — 검색해 보세요"),
    "hi" to offlineHome("आप ऑफ़लाइन हैं", "Derbis इस डिवाइस पर सहेजा गया संगीत दिखा रहा है।", "फिर कोशिश करें", "डाउनलोड", "ऑफ़लाइन प्लेलिस्ट", "पसंदीदा", "हाल में सुने गए", "होम अभी उपलब्ध नहीं है", "रिमोट होम खाली है — खोज आज़माएँ"),
    "id" to offlineHome("Kamu sedang offline", "Derbis menampilkan musik yang tersimpan di perangkat ini.", "Coba lagi", "Unduhan", "Playlist offline", "Favorit", "Baru diputar", "Beranda tidak tersedia saat ini", "Beranda jarak jauh kosong — coba cari"),
    "vi" to offlineHome("Bạn đang ngoại tuyến", "Derbis đang hiển thị nhạc đã lưu trên thiết bị này.", "Thử lại", "Bản tải xuống", "Danh sách phát ngoại tuyến", "Yêu thích", "Nghe gần đây", "Trang chủ hiện không khả dụng", "Trang chủ từ xa trống — hãy thử tìm kiếm"),
    "th" to offlineHome("คุณออฟไลน์อยู่", "Derbis กำลังแสดงเพลงที่บันทึกไว้ในอุปกรณ์นี้", "ลองใหม่", "ดาวน์โหลด", "เพลย์ลิสต์ออฟไลน์", "รายการโปรด", "เล่นล่าสุด", "หน้าแรกใช้งานไม่ได้ในขณะนี้", "หน้าแรกระยะไกลว่างเปล่า — ลองค้นหา"),
    "fil" to offlineHome("Offline ka ngayon", "Ipinapakita ng Derbis ang musikang naka-save sa device na ito.", "Subukan ulit", "Mga download", "Mga offline na playlist", "Mga paborito", "Kamakailang pinatugtog", "Hindi available ang Home ngayon", "Walang laman ang remote Home — sumubok maghanap"),
    "he" to offlineHome("אתה במצב לא מקוון", "Derbis מציגה את המוזיקה השמורה במכשיר הזה.", "נסה שוב", "הורדות", "פלייליסטים במצב לא מקוון", "מועדפים", "הושמעו לאחרונה", "הבית אינו זמין כרגע", "הבית המרוחק ריק — נסה חיפוש"),
    "fi" to offlineHome("Olet offline-tilassa", "Derbis näyttää tähän laitteeseen tallennetun musiikin.", "Yritä uudelleen", "Lataukset", "Offline-soittolistat", "Suosikit", "Äskettäin toistetut", "Kotivalikko ei ole juuri nyt saatavilla", "Etäkotivalikko on tyhjä — kokeile hakua"),
    "et" to offlineHome("Oled võrguühenduseta", "Derbis kuvab sellesse seadmesse salvestatud muusikat.", "Proovi uuesti", "Allalaadimised", "Võrguühenduseta esitusloendid", "Lemmikud", "Hiljuti mängitud", "Avaleht pole praegu saadaval", "Kaugavaleht on tühi — proovi otsingut")
)

internal fun offlineHomeLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(offlineHomeBundles, code)

internal fun offlineHomeLocalizationCodes(): Set<String> = supportedLocalizationCodes()
