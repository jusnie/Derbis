package com.luc4n3x.derbis.ui.i18n

import com.luc4n3x.derbis.domain.DerbisLanguageCatalog

internal data class SkinCopy(val title: String, val defaultBody: String, val nocturneBody: String)

internal object DerbisSkinStrings {
    private val copies = mapOf(
        "en" to SkinCopy("Skin", "Derbis's original interface, with your personal settings.", "AMOLED black, graphite and amber."),
        "it" to SkinCopy("Skin", "L'interfaccia originale di Derbis, con le tue personalizzazioni.", "Nero AMOLED, grafite e ambra."),
        "es" to SkinCopy("Skin", "La interfaz original de Derbis, con tus ajustes personales.", "Negro AMOLED, grafito y ámbar."),
        "fr" to SkinCopy("Habillage", "L'interface originale de Derbis, avec vos réglages personnels.", "Noir AMOLED, graphite et ambre."),
        "de" to SkinCopy("Skin", "Die ursprüngliche Derbis-Oberfläche mit deinen persönlichen Einstellungen.", "AMOLED-Schwarz, Graphit und Bernstein."),
        "pt" to SkinCopy("Skin", "A interface original do Derbis, com as tuas personalizações.", "Preto AMOLED, grafite e âmbar."),
        "nl" to SkinCopy("Skin", "De originele Derbis-interface met je persoonlijke instellingen.", "AMOLED-zwart, grafiet en amber."),
        "pl" to SkinCopy("Skórka", "Oryginalny interfejs Derbis z Twoimi ustawieniami.", "Czerń AMOLED, grafit i bursztyn."),
        "ru" to SkinCopy("Обложка", "Оригинальный интерфейс Derbis с вашими настройками.", "Чёрный AMOLED, графит и янтарь."),
        "uk" to SkinCopy("Оболонка", "Оригінальний інтерфейс Derbis з вашими налаштуваннями.", "Чорний AMOLED, графіт і бурштин."),
        "ja" to SkinCopy("スキン", "個人設定を反映した Derbis 本来のインターフェース。", "AMOLED の黒、グラファイト、アンバー。"),
        "ko" to SkinCopy("스킨", "개인 설정이 적용된 Derbis의 기본 인터페이스.", "AMOLED 블랙, 그래파이트, 앰버."),
        "zh" to SkinCopy("界面风格", "保留个人设置的 Derbis 原始界面。", "AMOLED 黑色、石墨灰和琥珀色。"),
        "zh-Hant" to SkinCopy("介面風格", "保留個人設定的 Derbis 原始介面。", "AMOLED 黑色、石墨灰和琥珀色。"),
        "ar" to SkinCopy("المظهر", "واجهة Derbis الأصلية مع إعداداتك الشخصية.", "أسود AMOLED وغرافيت وكهرماني."),
        "ro" to SkinCopy("Aspect", "Interfața originală Derbis, cu setările tale.", "Negru AMOLED, grafit și chihlimbar."),
        "sq" to SkinCopy("Pamja", "Ndërfaqja origjinale e Derbis me cilësimet e tua.", "E zezë AMOLED, grafit dhe qelibar."),
        "bg" to SkinCopy("Облик", "Оригиналният интерфейс на Derbis с личните ти настройки.", "AMOLED черно, графит и кехлибар."),
        "el" to SkinCopy("Εμφάνιση", "Η αρχική διεπαφή του Derbis, με τις ρυθμίσεις σου.", "Μαύρο AMOLED, γραφίτης και κεχριμπάρι."),
        "sv" to SkinCopy("Utseende", "Derbiss ursprungliga gränssnitt med dina inställningar.", "AMOLED-svart, grafit och bärnsten."),
        "da" to SkinCopy("Udseende", "Derbiss oprindelige grænseflade med dine indstillinger.", "AMOLED-sort, grafit og rav."),
        "cs" to SkinCopy("Vzhled", "Původní rozhraní Derbis s tvým nastavením.", "Černá AMOLED, grafit a jantar."),
        "sk" to SkinCopy("Vzhľad", "Pôvodné rozhranie Derbis s tvojimi nastaveniami.", "Čierna AMOLED, grafit a jantár."),
        "bn" to SkinCopy("স্কিন", "আপনার ব্যক্তিগত সেটিংসসহ Derbis-এর মূল ইন্টারফেস।", "AMOLED কালো, গ্রাফাইট ও অ্যাম্বার।"),
        "kk" to SkinCopy("Безендіру", "Жеке параметрлеріңіз сақталған Derbis-ның бастапқы интерфейсі.", "AMOLED қара, графит және кәріптас."),
        "sr" to SkinCopy("Изглед", "Оригинални Derbis интерфејс са твојим подешавањима.", "AMOLED црна, графит и ћилибар."),
        "hr" to SkinCopy("Izgled", "Izvorno sučelje Derbis s tvojim postavkama.", "AMOLED crna, grafit i jantar."),
        "hu" to SkinCopy("Megjelenés", "A Derbis eredeti felülete a személyes beállításaiddal.", "AMOLED-fekete, grafit és borostyán."),
        "fi" to SkinCopy("Ulkoasu", "Derbisn alkuperäinen käyttöliittymä omilla asetuksillasi.", "AMOLED-musta, grafiitti ja meripihka."),
        "et" to SkinCopy("Välimus", "Derbis algne liides sinu isiklike seadetega.", "AMOLED-must, grafiit ja merevaik."),
        "nb" to SkinCopy("Utseende", "Derbiss opprinnelige grensesnitt med dine innstillinger.", "AMOLED-svart, grafitt og rav."),
        "ca" to SkinCopy("Aparença", "La interfície original de Derbis, amb els teus ajustos.", "Negre AMOLED, grafit i ambre."),
        "tr" to SkinCopy("Görünüm", "Kişisel ayarlarınla Derbis'nın özgün arayüzü.", "AMOLED siyah, grafit ve kehribar."),
        "fa" to SkinCopy("پوسته", "رابط اصلی Derbis با تنظیمات شخصی شما.", "مشکی AMOLED، گرافیت و کهربایی."),
        "hi" to SkinCopy("स्किन", "आपकी निजी सेटिंग्स के साथ Derbis का मूल इंटरफ़ेस।", "AMOLED काला, ग्रेफाइट और एम्बर।"),
        "id" to SkinCopy("Tampilan", "Antarmuka asli Derbis dengan pengaturan pribadimu.", "Hitam AMOLED, grafit, dan amber."),
        "ms" to SkinCopy("Rupa", "Antara muka asal Derbis dengan tetapan peribadi anda.", "Hitam AMOLED, grafit dan ambar."),
        "vi" to SkinCopy("Giao diện", "Giao diện gốc của Derbis với cài đặt cá nhân của bạn.", "Đen AMOLED, xám than và hổ phách."),
        "th" to SkinCopy("สกิน", "อินเทอร์เฟซดั้งเดิมของ Derbis พร้อมการตั้งค่าส่วนตัวของคุณ", "ดำ AMOLED กราไฟต์ และอำพัน"),
        "fil" to SkinCopy("Hitsura", "Ang orihinal na interface ng Derbis kasama ang personal mong mga setting.", "AMOLED na itim, graphite at amber."),
        "he" to SkinCopy("עיצוב", "הממשק המקורי של Derbis עם ההגדרות האישיות שלך.", "שחור AMOLED, גרפיט וענבר.")
    )

    fun forCode(code: String): SkinCopy = copies[DerbisLanguageCatalog.normalize(code)] ?: copies.getValue("en")

    fun managedForCode(code: String, skin: String = "Nocturne 78"): String =
        (managed[DerbisLanguageCatalog.normalize(code)] ?: managed.getValue("en")).replace("Nocturne 78", skin)

    private val managed = mapOf(
        "en" to "Nocturne 78 manages the appearance. Switch to Derbis Default to change it. Your previous choices are saved.",
        "it" to "L'aspetto è gestito da Nocturne 78. Per modificarlo, passa a Derbis Default. Le tue scelte precedenti sono conservate.",
        "es" to "Nocturne 78 controla la apariencia. Cambia a Derbis Default para modificarla. Tus ajustes anteriores se conservan.",
        "fr" to "Nocturne 78 gère l'apparence. Passez à Derbis Default pour la modifier. Vos choix précédents sont conservés.",
        "de" to "Nocturne 78 bestimmt das Aussehen. Wechsle zu Derbis Default, um es anzupassen. Deine bisherigen Einstellungen bleiben gespeichert.",
        "pt" to "Nocturne 78 controla o aspeto. Muda para Derbis Default para o personalizares. As escolhas anteriores ficam guardadas.",
        "nl" to "Nocturne 78 bepaalt het uiterlijk. Schakel naar Derbis Default om dit te wijzigen. Je eerdere keuzes blijven bewaard.",
        "pl" to "Nocturne 78 ustala wygląd. Przełącz na Derbis Default, aby go zmienić. Poprzednie ustawienia są zachowane.",
        "ro" to "Nocturne 78 controlează aspectul. Treci la Derbis Default pentru a-l schimba. Alegerile anterioare sunt păstrate.",
        "el" to "Το Nocturne 78 ορίζει την εμφάνιση. Επιλέξτε Derbis Default για αλλαγές. Οι προηγούμενες επιλογές σας διατηρούνται.",
        "sv" to "Nocturne 78 styr utseendet. Byt till Derbis Default för att ändra det. Dina tidigare val sparas.",
        "da" to "Nocturne 78 styrer udseendet. Skift til Derbis Default for at ændre det. Dine tidligere valg er gemt.",
        "cs" to "Vzhled řídí Nocturne 78. Pro změny přepni na Derbis Default. Předchozí volby zůstávají uložené.",
        "sk" to "Vzhľad riadi Nocturne 78. Pre zmeny prejdi na Derbis Default. Predchádzajúce voľby zostávajú uložené.",
        "hr" to "Nocturne 78 određuje izgled. Prijeđi na Derbis Default za promjene. Prethodni odabiri ostaju spremljeni.",
        "sr" to "Nocturne 78 одређује изглед. Пређи на Derbis Default за измене. Претходни избори остају сачувани.",
        "sq" to "Nocturne 78 përcakton pamjen. Kalo te Derbis Default për ta ndryshuar. Zgjedhjet e mëparshme ruhen.",
        "bg" to "Nocturne 78 определя облика. Премини към Derbis Default за промени. Предишните избори се запазват.",
        "hu" to "A megjelenést a Nocturne 78 kezeli. Válts Derbis Default módra a módosításhoz. A korábbi beállításaid megmaradnak.",
        "fi" to "Nocturne 78 määrittää ulkoasun. Vaihda Derbis Defaultiin muokataksesi sitä. Aiemmat valintasi säilyvät.",
        "et" to "Nocturne 78 määrab välimuse. Muutmiseks vali Derbis Default. Varasemad valikud säilivad.",
        "nb" to "Nocturne 78 styrer utseendet. Bytt til Derbis Default for å endre det. Dine tidligere valg beholdes.",
        "ca" to "Nocturne 78 controla l'aparença. Canvia a Derbis Default per modificar-la. Les tries anteriors es conserven.",
        "uk" to "Nocturne 78 визначає вигляд. Перейдіть на Derbis Default для змін. Попередні налаштування збережено.",
        "ru" to "Nocturne 78 определяет оформление. Переключитесь на Derbis Default для изменений. Прежние настройки сохранены.",
        "kk" to "Көріністі Nocturne 78 басқарады. Өзгерту үшін Derbis Default таңдаңыз. Бұрынғы таңдаулар сақталады.",
        "tr" to "Görünümü Nocturne 78 yönetir. Değiştirmek için Derbis Default'a geç. Önceki seçimlerin saklanır.",
        "ar" to "يتحكم Nocturne 78 بالمظهر. انتقل إلى Derbis Default لتغييره. تبقى اختياراتك السابقة محفوظة.",
        "fa" to "ظاهر توسط Nocturne 78 مدیریت می‌شود. برای تغییر، Derbis Default را انتخاب کنید. انتخاب‌های قبلی شما حفظ می‌شوند.",
        "zh" to "外观由 Nocturne 78 管理。切换至 Derbis Default 即可修改。之前的设置会保留。",
        "zh-Hant" to "外觀由 Nocturne 78 管理。切換至 Derbis Default 即可修改。先前的設定會保留。",
        "ja" to "外観は Nocturne 78 が管理します。変更するには Derbis Default に切り替えてください。以前の設定は保持されます。",
        "ko" to "Nocturne 78이 외관을 관리합니다. 변경하려면 Derbis Default로 전환하세요. 이전 선택은 보존됩니다.",
        "hi" to "रूप Nocturne 78 द्वारा नियंत्रित है। बदलने के लिए Derbis Default चुनें। आपकी पिछली सेटिंग्स सुरक्षित हैं।",
        "bn" to "দেখতে কেমন হবে তা Nocturne 78 নির্ধারণ করে। পরিবর্তনের জন্য Derbis Default বেছে নিন। আগের পছন্দগুলো সংরক্ষিত আছে।",
        "id" to "Tampilan diatur oleh Nocturne 78. Beralih ke Derbis Default untuk mengubahnya. Pilihan sebelumnya tetap tersimpan.",
        "ms" to "Rupa diuruskan oleh Nocturne 78. Tukar kepada Derbis Default untuk mengubahnya. Pilihan terdahulu disimpan.",
        "vi" to "Nocturne 78 quản lý giao diện. Chuyển sang Derbis Default để thay đổi. Các lựa chọn trước đó được giữ lại.",
        "th" to "Nocturne 78 จัดการรูปลักษณ์ สลับเป็น Derbis Default เพื่อเปลี่ยนแปลง การตั้งค่าเดิมของคุณจะถูกเก็บไว้",
        "fil" to "Nocturne 78 ang namamahala sa hitsura. Lumipat sa Derbis Default para baguhin ito. Naka-save ang mga dati mong pinili.",
        "he" to "המראה מנוהל על ידי Nocturne 78. לשינוי, עבור אל Derbis Default. הבחירות הקודמות שלך נשמרות."
    )
}
