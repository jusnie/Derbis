package com.yasuda.derbis.ui.i18n

/**
 * Copy for the Derbis Notch, the in-app dynamic island. The product name stays English everywhere;
 * only the hint is translated and everything untranslated falls back to the English line.
 */
internal val derbisNotchKeys = setOf(
    "derbisNotch",
    "derbisNotchHint",
    "derbisNotchStyleDefault",
    "derbisNotchStyleSpicy"
)

private val derbisNotchHints = mapOf(
    "en" to "Dynamic island at the top with the artwork, the track and synced lyrics. Tap to open.",
    "ja" to "画面上部のダイナミックアイランド。アートワーク、曲名、同期した歌詞を表示します。タップで開きます。",
    "ko" to "화면 상단의 다이내믹 아일랜드. 앨범 아트, 곡 정보, 동기화된 가사를 보여 줍니다. 탭하여 엽니다.",
    "zh" to "屏幕顶部的灵动岛，显示封面、曲目和同步歌词。点按即可打开。",
    "zh-Hant" to "螢幕頂端的動態島，顯示封面、曲目與同步歌詞。輕點即可開啟。",
    "es" to "Isla dinámica en la parte superior con la portada, el tema y la letra sincronizada. Toca para abrir.",
    "fr" to "Île dynamique en haut avec la pochette, le titre et les paroles synchronisées. Touchez pour ouvrir.",
    "de" to "Dynamic Island oben mit Cover, Titel und synchronisierten Lyrics. Tippen zum Öffnen.",
    "it" to "Isola dinamica in alto con copertina, brano e testo sincronizzato. Tocca per aprire.",
    "pt" to "Ilha dinâmica no topo com a capa, a faixa e a letra sincronizada. Toque para abrir.",
    "nl" to "Dynamic island bovenaan met hoes, nummer en gesynchroniseerde songtekst. Tik om te openen.",
    "pl" to "Dynamiczna wyspa u góry z okładką, utworem i zsynchronizowanym tekstem. Dotknij, aby otworzyć.",
    "ru" to "Динамический остров сверху: обложка, трек и синхронный текст. Нажмите, чтобы открыть.",
    "uk" to "Динамічний острів угорі: обкладинка, трек і синхронізований текст. Торкніться, щоб відкрити.",
    "tr" to "Üstte kapak, parça ve senkron sözlerle dinamik ada. Açmak için dokunun.",
    "id" to "Dynamic island di bagian atas dengan sampul, lagu, dan lirik tersinkron. Ketuk untuk membuka.",
    "ms" to "Dynamic island di bahagian atas dengan kulit, lagu dan lirik segerak. Ketik untuk membuka.",
    "vi" to "Dynamic island ở trên cùng với bìa, bài hát và lời đồng bộ. Chạm để mở.",
    "th" to "ไดนามิกไอส์แลนด์ด้านบน พร้อมปกเพลง เพลง และเนื้อเพลงที่ซิงก์ แตะเพื่อเปิด",
    "hi" to "ऊपर डायनामिक आइलैंड — कवर, ट्रैक और सिंक किए गए बोल। खोलने के लिए टैप करें।",
    "bn" to "উপরে ডায়নামিক আইল্যান্ড — কভার, ট্র্যাক ও সিঙ্ক করা গানের কথা। খুলতে ট্যাপ করুন।",
    "ar" to "جزيرة ديناميكية في الأعلى مع الغلاف والمقطع وكلمات متزامنة. اضغط للفتح.",
    "fa" to "جزیره پویا در بالا با کاور، آهنگ و متن هم‌زمان. برای باز کردن ضربه بزنید.",
    "he" to "אי דינמי למעלה עם העטיפה, השיר והמילים המסונכרנות. הקש כדי לפתוח.",
    "fil" to "Dynamic island sa itaas na may cover, track, at naka-sync na lyrics. I-tap para buksan.",
    "sv" to "Dynamisk ö med omslag, låt och synkad text högst upp. Tryck för att öppna.",
    "da" to "Dynamisk ø med cover, nummer og synkroniseret tekst øverst. Tryk for at åbne.",
    "nb" to "Dynamisk ø med cover, spor og synkronisert tekst øverst. Trykk for å åpne.",
    "fi" to "Dynaaminen saareke ylhäällä: kansi, kappale ja synkattu teksti. Napauta avataksesi.",
    "cs" to "Dynamický ostrov nahoře s obalem, skladbou a synchronizovaným textem. Klepněte pro otevření.",
    "sk" to "Dynamický ostrov hore s obalom, skladbou a synchronizovaným textom. Ťuknite pre otvorenie.",
    "el" to "Δυναμικό νησί στην κορυφή με εξώφυλλο, κομμάτι και συγχρονισμένους στίχους. Πατήστε για άνοιγμα.",
    "ro" to "Insulă dinamică sus cu copertă, piesa și versuri sincronizate. Atingeți pentru a deschide.",
    "hu" to "Dinamikus sziget felül a borítóval, a dallal és a szinkronizált szöveggel. Koppintson a megnyitáshoz.",
    "bg" to "Динамичен остров отгоре с корица, песен и синхронизиран текст. Докоснете за отваряне.",
    "hr" to "Dinamički otok na vrhu s naslovnicom, pjesmom i sinkroniziranim tekstom. Dodirnite za otvaranje.",
    "sr" to "Динамичко острво на врху са насловницом, песмом и синхронизованим текстом. Додирните да отворите.",
    "sq" to "Ishull dinamik në krye me kopertinën, këngën dhe tekstin e sinkronizuar. Trokit për ta hapur.",
    "et" to "Dünaamiline saar ülaosas koos kaanega, loo ja sünkroonitud sõnadega. Puuduta avamiseks.",
    "ca" to "Illa dinàmica a dalt amb la portada, el tema i la lletra sincronitzada. Toca per obrir.",
    "kk" to "Жоғарғы жақтағы динамикалық арал: мұқаба, трек және синхрондалған мәтін. Ашу үшін түртіңіз."
)

internal fun derbisNotchLocalizationEntries(code: String): Map<String, String> = mapOf(
    "derbisNotch" to "Derbis Notch",
    "derbisNotchHint" to (derbisNotchHints[code] ?: derbisNotchHints.getValue("en")),
    "derbisNotchStyleDefault" to "Default",
    "derbisNotchStyleSpicy" to "Spicy"
)
