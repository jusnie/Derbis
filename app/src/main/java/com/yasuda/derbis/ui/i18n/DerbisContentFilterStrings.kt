package com.yasuda.derbis.ui.i18n

/**
 * Copy for the explicit-content filter and the status-bar lyric line.
 *
 * Both live in one file because they are the two settings that change what the listener
 * *sees* rather than how the audio is processed, and because a translator should read them
 * together: the filter's subtitle is a promise about accuracy, and the lyric line's is a
 * promise about silence, so neither can be translated as decoration.
 *
 * Translation falls back to English for every locale without an entry here, which is why the
 * five that ship are the ones this project actually reviews. `localizedBundleOrEnglish` merges
 * the English map underneath, so a missing key can never surface as a raw id.
 */
internal val contentFilterKeys = setOf(
    "explicitFilterTitle",
    "explicitFilterSubtitle",
    "explicitFilterShowAll",
    "explicitFilterHide",
    "explicitFilterOnly",
    "statusBarLyricsTitle",
    "statusBarLyricsSubtitle"
)

private fun contentFilter(
    explicitTitle: String,
    explicitSubtitle: String,
    showAll: String,
    hide: String,
    only: String,
    statusTitle: String,
    statusSubtitle: String
): Map<String, String> = mapOf(
    "explicitFilterTitle" to explicitTitle,
    "explicitFilterSubtitle" to explicitSubtitle,
    "explicitFilterShowAll" to showAll,
    "explicitFilterHide" to hide,
    "explicitFilterOnly" to only,
    "statusBarLyricsTitle" to statusTitle,
    "statusBarLyricsSubtitle" to statusSubtitle
)

private val contentFilterBundles: Map<String, Map<String, String>> = mapOf(
    "en" to contentFilter(
        "Explicit content",
        "Uses the provider's own flag. Nothing is guessed from a title, so nothing is hidden by mistake.",
        "Show all",
        "Hide explicit",
        "Explicit only",
        "Status bar lyrics",
        "The line you are hearing appears in a silent notification, without opening Derbis."
    ),
    "it" to contentFilter(
        "Contenuti espliciti",
        "Usa il flag del provider. Nessuna stima dal titolo, quindi nessun risultato nascosto per errore.",
        "Mostra tutto",
        "Nascondi espliciti",
        "Solo espliciti",
        "Testo nella barra di stato",
        "La riga del testo che stai ascoltando compare in una notifica silenziosa, senza aprire Derbis."
    ),
    "es" to contentFilter(
        "Contenido explícito",
        "Usa la marca del proveedor. Sin adivinar por el título, así que no se oculta nada por error.",
        "Mostrar todo",
        "Ocultar explícitos",
        "Solo explícitos",
        "Letra en la barra de estado",
        "La línea que suena aparece en una notificación silenciosa, sin abrir Derbis."
    ),
    "fr" to contentFilter(
        "Contenu explicite",
        "Utilise la mention du fournisseur. Aucune déduction depuis le titre, donc rien n'est masqué par erreur.",
        "Tout afficher",
        "Masquer explicites",
        "Explicites seulement",
        "Paroles dans la barre d'état",
        "La ligne en cours apparaît dans une notification silencieuse, sans ouvrir Derbis."
    ),
    "de" to contentFilter(
        "Explizite Inhalte",
        "Nutzt die Kennzeichnung des Anbieters. Keine Rückschlüsse aus dem Titel, es wird also nichts versehentlich versteckt.",
        "Alles zeigen",
        "Explizites ausblenden",
        "Nur Explizites",
        "Songtext in der Statusleiste",
        "Die aktuelle Zeile erscheint in einer stillen Benachrichtigung, ohne Derbis zu öffnen."
    )
)

internal fun contentFilterLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(contentFilterBundles, code)

internal fun contentFilterLocalizationCodes(): Set<String> = contentFilterBundles.keys
