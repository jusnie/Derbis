package com.luc4n3x.levyra.ui.i18n

internal val downloadLocationKeys = setOf(
    "downloadLocation",
    "downloadLocationSubtitle",
    "downloadLocationDefault",
    "downloadLocationReset",
    "downloadLocationResetSubtitle",
    "downloadLocationUnavailable",
    "downloadLocationPermissionFailed"
)

private fun downloadLocation(
    location: String,
    subtitle: String,
    defaultLocation: String,
    reset: String,
    resetSubtitle: String,
    unavailable: String,
    permissionFailed: String
): Map<String, String> = mapOf(
    "downloadLocation" to location,
    "downloadLocationSubtitle" to subtitle,
    "downloadLocationDefault" to defaultLocation,
    "downloadLocationReset" to reset,
    "downloadLocationResetSubtitle" to resetSubtitle,
    "downloadLocationUnavailable" to unavailable,
    "downloadLocationPermissionFailed" to permissionFailed
)

private val downloadLocationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to downloadLocation("Download location", "Choose internal storage or an SD card folder", "Music/Derbis (default)", "Use default folder", "Save future downloads in Music/Derbis", "Selected folder unavailable — Music/Derbis will be used", "Derbis could not keep write access to that folder"),
    "it" to downloadLocation("Cartella dei download", "Scegli la memoria interna o una cartella sulla scheda SD", "Music/Derbis (predefinita)", "Usa cartella predefinita", "Salva i prossimi download in Music/Derbis", "Cartella selezionata non disponibile — verrà usata Music/Derbis", "Derbis non riesce a mantenere l'accesso in scrittura a questa cartella"),
    "es" to downloadLocation("Ubicación de descargas", "Elige el almacenamiento interno o una carpeta de la tarjeta SD", "Music/Derbis (predeterminada)", "Usar carpeta predeterminada", "Guardar las próximas descargas en Music/Derbis", "La carpeta seleccionada no está disponible — se usará Music/Derbis", "Derbis no pudo conservar el acceso de escritura a esa carpeta"),
    "fr" to downloadLocation("Emplacement des téléchargements", "Choisissez le stockage interne ou un dossier sur carte SD", "Music/Derbis (par défaut)", "Utiliser le dossier par défaut", "Enregistrer les prochains téléchargements dans Music/Derbis", "Dossier sélectionné indisponible — Music/Derbis sera utilisé", "Derbis n’a pas pu conserver l’accès en écriture à ce dossier"),
    "de" to downloadLocation("Download-Speicherort", "Internen Speicher oder einen Ordner auf der SD-Karte wählen", "Music/Derbis (Standard)", "Standardordner verwenden", "Künftige Downloads in Music/Derbis speichern", "Ausgewählter Ordner nicht verfügbar — Music/Derbis wird verwendet", "Derbis konnte den Schreibzugriff auf diesen Ordner nicht beibehalten"),
    "pt" to downloadLocation("Local dos downloads", "Escolha o armazenamento interno ou uma pasta no cartão SD", "Music/Derbis (padrão)", "Usar pasta padrão", "Salvar os próximos downloads em Music/Derbis", "Pasta selecionada indisponível — Music/Derbis será usada", "O Derbis não conseguiu manter acesso de escrita a essa pasta"),
    "nl" to downloadLocation("Downloadlocatie", "Kies interne opslag of een map op de SD-kaart", "Music/Derbis (standaard)", "Standaardmap gebruiken", "Sla toekomstige downloads op in Music/Derbis", "Geselecteerde map niet beschikbaar — Music/Derbis wordt gebruikt", "Derbis kon schrijftoegang tot deze map niet behouden"),
    "pl" to downloadLocation("Lokalizacja pobierania", "Wybierz pamięć wewnętrzną lub folder na karcie SD", "Music/Derbis (domyślnie)", "Użyj folderu domyślnego", "Zapisuj przyszłe pobrania w Music/Derbis", "Wybrany folder jest niedostępny — zostanie użyty Music/Derbis", "Derbis nie mogła zachować dostępu do zapisu w tym folderze"),
    "ro" to downloadLocation("Locația descărcărilor", "Alege stocarea internă sau un folder de pe cardul SD", "Music/Derbis (implicit)", "Folosește folderul implicit", "Salvează descărcările viitoare în Music/Derbis", "Folderul selectat nu este disponibil — se va folosi Music/Derbis", "Derbis nu a putut păstra accesul de scriere la acest folder"),
    "el" to downloadLocation("Τοποθεσία λήψεων", "Επιλέξτε εσωτερικό χώρο ή φάκελο στην κάρτα SD", "Music/Derbis (προεπιλογή)", "Χρήση προεπιλεγμένου φακέλου", "Αποθήκευση μελλοντικών λήψεων στο Music/Derbis", "Ο επιλεγμένος φάκελος δεν είναι διαθέσιμος — θα χρησιμοποιηθεί το Music/Derbis", "Το Derbis δεν μπόρεσε να διατηρήσει πρόσβαση εγγραφής σε αυτόν τον φάκελο"),
    "sv" to downloadLocation("Nedladdningsplats", "Välj intern lagring eller en mapp på SD-kortet", "Music/Derbis (standard)", "Använd standardmappen", "Spara framtida nedladdningar i Music/Derbis", "Vald mapp är inte tillgänglig — Music/Derbis används", "Derbis kunde inte behålla skrivåtkomst till den mappen"),
    "da" to downloadLocation("Downloadplacering", "Vælg intern lagerplads eller en mappe på SD-kortet", "Music/Derbis (standard)", "Brug standardmappen", "Gem fremtidige downloads i Music/Derbis", "Den valgte mappe er ikke tilgængelig — Music/Derbis bruges", "Derbis kunne ikke bevare skriveadgang til den mappe"),
    "cs" to downloadLocation("Umístění stažených souborů", "Vyberte interní úložiště nebo složku na SD kartě", "Music/Derbis (výchozí)", "Použít výchozí složku", "Budoucí stahování ukládat do Music/Derbis", "Vybraná složka není dostupná — použije se Music/Derbis", "Derbis nemohla zachovat oprávnění k zápisu do této složky"),
    "uk" to downloadLocation("Розташування завантажень", "Виберіть внутрішню пам’ять або папку на SD-карті", "Music/Derbis (типово)", "Використовувати типову папку", "Зберігати наступні завантаження в Music/Derbis", "Вибрана папка недоступна — буде використано Music/Derbis", "Derbis не вдалося зберегти доступ на запис до цієї папки"),
    "ru" to downloadLocation("Папка загрузок", "Выберите внутреннюю память или папку на SD-карте", "Music/Derbis (по умолчанию)", "Использовать папку по умолчанию", "Сохранять новые загрузки в Music/Derbis", "Выбранная папка недоступна — будет использована Music/Derbis", "Derbis не удалось сохранить доступ на запись к этой папке"),
    "tr" to downloadLocation("İndirme konumu", "Dahili depolamayı veya SD karttaki bir klasörü seçin", "Music/Derbis (varsayılan)", "Varsayılan klasörü kullan", "Yeni indirmeleri Music/Derbis içine kaydet", "Seçilen klasör kullanılamıyor — Music/Derbis kullanılacak", "Derbis bu klasöre yazma erişimini koruyamadı"),
    "ar" to downloadLocation("موقع التنزيل", "اختر التخزين الداخلي أو مجلدًا على بطاقة SD", "Music/Derbis (افتراضي)", "استخدام المجلد الافتراضي", "حفظ التنزيلات القادمة في Music/Derbis", "المجلد المحدد غير متاح — سيتم استخدام Music/Derbis", "تعذر على Derbis الاحتفاظ بإذن الكتابة إلى هذا المجلد"),
    "zh" to downloadLocation("下载位置", "选择内部存储或 SD 卡上的文件夹", "Music/Derbis（默认）", "使用默认文件夹", "将后续下载保存到 Music/Derbis", "所选文件夹不可用 — 将使用 Music/Derbis", "Derbis 无法保留对此文件夹的写入权限"),
    "ja" to downloadLocation("ダウンロード先", "内部ストレージまたはSDカードのフォルダーを選択", "Music/Derbis（デフォルト）", "デフォルトフォルダーを使用", "今後のダウンロードを Music/Derbis に保存", "選択したフォルダーは利用できません — Music/Derbis を使用します", "Derbis はこのフォルダーへの書き込み権限を保持できませんでした"),
    "ko" to downloadLocation("다운로드 위치", "내부 저장소 또는 SD 카드의 폴더를 선택하세요", "Music/Derbis (기본값)", "기본 폴더 사용", "앞으로의 다운로드를 Music/Derbis에 저장", "선택한 폴더를 사용할 수 없어 Music/Derbis를 사용합니다", "Derbis가 이 폴더의 쓰기 권한을 유지할 수 없습니다"),
    "hi" to downloadLocation("डाउनलोड स्थान", "आंतरिक स्टोरेज या SD कार्ड का फ़ोल्डर चुनें", "Music/Derbis (डिफ़ॉल्ट)", "डिफ़ॉल्ट फ़ोल्डर इस्तेमाल करें", "आगे के डाउनलोड Music/Derbis में सेव करें", "चुना गया फ़ोल्डर उपलब्ध नहीं है — Music/Derbis इस्तेमाल होगा", "Derbis इस फ़ोल्डर की लिखने की अनुमति बनाए नहीं रख सका"),
    "id" to downloadLocation("Lokasi unduhan", "Pilih penyimpanan internal atau folder di kartu SD", "Music/Derbis (default)", "Gunakan folder default", "Simpan unduhan berikutnya di Music/Derbis", "Folder yang dipilih tidak tersedia — Music/Derbis akan digunakan", "Derbis tidak dapat mempertahankan akses tulis ke folder tersebut"),
    "vi" to downloadLocation("Vị trí tải xuống", "Chọn bộ nhớ trong hoặc một thư mục trên thẻ SD", "Music/Derbis (mặc định)", "Dùng thư mục mặc định", "Lưu các bản tải xuống sau vào Music/Derbis", "Thư mục đã chọn không khả dụng — sẽ dùng Music/Derbis", "Derbis không thể duy trì quyền ghi vào thư mục này"),
    "th" to downloadLocation("ตำแหน่งดาวน์โหลด", "เลือกพื้นที่จัดเก็บภายในหรือโฟลเดอร์บนการ์ด SD", "Music/Derbis (ค่าเริ่มต้น)", "ใช้โฟลเดอร์เริ่มต้น", "บันทึกการดาวน์โหลดต่อไปใน Music/Derbis", "โฟลเดอร์ที่เลือกไม่พร้อมใช้งาน — จะใช้ Music/Derbis", "Derbis ไม่สามารถรักษาสิทธิ์เขียนในโฟลเดอร์นี้ได้"),
    "fil" to downloadLocation("Lokasyon ng download", "Pumili ng internal storage o folder sa SD card", "Music/Derbis (default)", "Gamitin ang default na folder", "I-save ang susunod na downloads sa Music/Derbis", "Hindi available ang napiling folder — Music/Derbis ang gagamitin", "Hindi mapanatili ng Derbis ang write access sa folder na iyon"),
    "he" to downloadLocation("מיקום הורדות", "בחרו אחסון פנימי או תיקייה בכרטיס SD", "Music/Derbis (ברירת מחדל)", "שימוש בתיקיית ברירת המחדל", "שמירת הורדות עתידיות ב-Music/Derbis", "התיקייה שנבחרה אינה זמינה — ייעשה שימוש ב-Music/Derbis", "Derbis לא הצליחה לשמור הרשאת כתיבה לתיקייה הזו"),
    "fi" to downloadLocation("Lataussijainti", "Valitse sisäinen tallennustila tai SD-kortin kansio", "Music/Derbis (oletus)", "Käytä oletuskansiota", "Tallenna tulevat lataukset kansioon Music/Derbis", "Valittu kansio ei ole saatavilla — käytetään kansiota Music/Derbis", "Derbis ei voinut säilyttää kirjoitusoikeutta tähän kansioon"),
    "et" to downloadLocation("Allalaadimise asukoht", "Vali sisemälu või SD-kaardi kaust", "Music/Derbis (vaikimisi)", "Kasuta vaikekausta", "Salvesta tulevased allalaadimised kausta Music/Derbis", "Valitud kaust pole saadaval — kasutatakse kausta Music/Derbis", "Derbisl ei õnnestunud säilitada sellesse kausta kirjutamise õigust")
)

internal fun downloadLocationLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(downloadLocationBundles, code)

internal fun downloadLocationLocalizationCodes(): Set<String> = supportedLocalizationCodes()
