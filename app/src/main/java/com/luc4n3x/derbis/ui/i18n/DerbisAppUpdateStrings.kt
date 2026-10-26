package com.luc4n3x.derbis.ui.i18n

internal val appUpdateKeys = setOf(
    "appUpToDate",
    "appUpdateAvailable",
    "appUpdateCheckFailed"
)

private fun appUpdate(upToDate: String, available: String, failed: String): Map<String, String> = mapOf(
    "appUpToDate" to upToDate,
    "appUpdateAvailable" to available,
    "appUpdateCheckFailed" to failed
)

private val appUpdateBundles: Map<String, Map<String, String>> = mapOf(
    "en" to appUpdate("DERBIS is up to date", "DERBIS {version} is available", "Couldn't check for updates"),
    "it" to appUpdate("DERBIS è già aggiornata", "DERBIS {version} è disponibile", "Controllo aggiornamenti non riuscito"),
    "es" to appUpdate("DERBIS está actualizada", "DERBIS {version} está disponible", "No se pudieron buscar actualizaciones"),
    "fr" to appUpdate("DERBIS est à jour", "DERBIS {version} est disponible", "Impossible de rechercher les mises à jour"),
    "de" to appUpdate("DERBIS ist auf dem neuesten Stand", "DERBIS {version} ist verfügbar", "Suche nach Updates fehlgeschlagen"),
    "pt" to appUpdate("O DERBIS está atualizado", "O DERBIS {version} está disponível", "Não foi possível verificar atualizações"),
    "nl" to appUpdate("DERBIS is up-to-date", "DERBIS {version} is beschikbaar", "Kan niet controleren op updates"),
    "pl" to appUpdate("DERBIS jest aktualna", "Dostępna jest DERBIS {version}", "Nie udało się sprawdzić aktualizacji"),
    "ro" to appUpdate("DERBIS este actualizată", "DERBIS {version} este disponibilă", "Verificarea actualizărilor a eșuat"),
    "el" to appUpdate("Το DERBIS είναι ενημερωμένο", "Το DERBIS {version} είναι διαθέσιμο", "Δεν ήταν δυνατός ο έλεγχος για ενημερώσεις"),
    "sv" to appUpdate("DERBIS är uppdaterad", "DERBIS {version} är tillgänglig", "Det gick inte att söka efter uppdateringar"),
    "da" to appUpdate("DERBIS er opdateret", "DERBIS {version} er tilgængelig", "Kunne ikke søge efter opdateringer"),
    "cs" to appUpdate("DERBIS je aktuální", "Je dostupná DERBIS {version}", "Kontrola aktualizací se nezdařila"),
    "sk" to appUpdate("DERBIS je aktuálna", "Je dostupná DERBIS {version}", "Kontrola aktualizácií zlyhala"),
    "hr" to appUpdate("DERBIS je ažurirana", "Dostupna je DERBIS {version}", "Provjera ažuriranja nije uspjela"),
    "sr" to appUpdate("DERBIS је ажурирана", "Доступна је DERBIS {version}", "Провера ажурирања није успела"),
    "sq" to appUpdate("DERBIS është e përditësuar", "DERBIS {version} është e disponueshme", "Kontrolli për përditësime dështoi"),
    "bg" to appUpdate("DERBIS е актуализирана", "Налична е DERBIS {version}", "Проверката за актуализации не бе успешна"),
    "hu" to appUpdate("A DERBIS naprakész", "Elérhető a DERBIS {version}", "Nem sikerült frissítéseket keresni"),
    "fi" to appUpdate("DERBIS on ajan tasalla", "DERBIS {version} on saatavilla", "Päivitysten tarkistus epäonnistui"),
    "et" to appUpdate("DERBIS on ajakohane", "DERBIS {version} on saadaval", "Värskenduste otsimine ebaõnnestus"),
    "nb" to appUpdate("DERBIS er oppdatert", "DERBIS {version} er tilgjengelig", "Kunne ikke se etter oppdateringer"),
    "ca" to appUpdate("DERBIS està actualitzada", "DERBIS {version} està disponible", "No s'han pogut cercar actualitzacions"),
    "uk" to appUpdate("DERBIS оновлено до останньої версії", "Доступна DERBIS {version}", "Не вдалося перевірити оновлення"),
    "ru" to appUpdate("DERBIS обновлена до последней версии", "Доступна DERBIS {version}", "Не удалось проверить обновления"),
    "kk" to appUpdate("DERBIS соңғы нұсқаға жаңартылған", "DERBIS {version} қолжетімді", "Жаңартуларды тексеру мүмкін болмады"),
    "tr" to appUpdate("DERBIS güncel", "DERBIS {version} kullanılabilir", "Güncellemeler denetlenemedi"),
    "ar" to appUpdate("DERBIS محدّث", "يتوفر DERBIS {version}", "تعذّر التحقق من التحديثات"),
    "fa" to appUpdate("DERBIS به‌روز است", "DERBIS {version} در دسترس است", "بررسی به‌روزرسانی‌ها ممکن نشد"),
    "zh" to appUpdate("DERBIS 已是最新版本", "DERBIS {version} 现已推出", "无法检查更新"),
    "zh-Hant" to appUpdate("DERBIS 已是最新版本", "DERBIS {version} 現已推出", "無法檢查更新"),
    "ja" to appUpdate("DERBIS は最新です", "DERBIS {version} が利用可能です", "アップデートを確認できませんでした"),
    "ko" to appUpdate("DERBIS가 최신 상태입니다", "DERBIS {version} 버전을 사용할 수 있습니다", "업데이트를 확인하지 못했습니다"),
    "hi" to appUpdate("DERBIS अप टू डेट है", "DERBIS {version} उपलब्ध है", "अपडेट की जाँच नहीं हो सकी"),
    "bn" to appUpdate("DERBIS হালনাগাদ আছে", "DERBIS {version} উপলব্ধ", "আপডেট পরীক্ষা করা যায়নি"),
    "id" to appUpdate("DERBIS sudah versi terbaru", "DERBIS {version} tersedia", "Gagal memeriksa pembaruan"),
    "ms" to appUpdate("DERBIS sudah dikemas kini", "DERBIS {version} tersedia", "Tidak dapat menyemak kemas kini"),
    "vi" to appUpdate("DERBIS đã được cập nhật", "Đã có DERBIS {version}", "Không thể kiểm tra bản cập nhật"),
    "th" to appUpdate("DERBIS เป็นเวอร์ชันล่าสุดแล้ว", "DERBIS {version} พร้อมใช้งานแล้ว", "ตรวจหาอัปเดตไม่สำเร็จ"),
    "fil" to appUpdate("Updated na ang DERBIS", "Available na ang DERBIS {version}", "Hindi ma-check ang mga update"),
    "he" to appUpdate("DERBIS מעודכנת", "DERBIS {version} זמינה", "לא ניתן היה לבדוק אם יש עדכונים")
)

internal fun appUpdateLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(appUpdateBundles, code)

internal fun appUpdateLocalizationCodes(): Set<String> = appUpdateBundles.keys
