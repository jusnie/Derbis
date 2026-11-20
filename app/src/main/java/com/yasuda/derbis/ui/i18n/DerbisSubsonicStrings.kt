package com.yasuda.derbis.ui.i18n

private val subsonicKeyOrder = listOf(
    "subsonicHomeServer",
    "subsonicTileSubtitle",
    "subsonicTileOff",
    "subsonicPanelBody",
    "subsonicServerAddress",
    "subsonicAddressHint",
    "subsonicUsername",
    "subsonicPassword",
    "subsonicPasswordSaved",
    "subsonicTest",
    "subsonicTesting",
    "subsonicSave",
    "subsonicRemove",
    "subsonicCancel",
    "subsonicContacting",
    "subsonicConnected",
    "subsonicFillAll",
    "subsonicRejected",
    "subsonicUnreachable"
)

internal val subsonicKeys: Set<String> = subsonicKeyOrder.toSet()

private fun server(vararg values: String): Map<String, String> {
    require(values.size == subsonicKeyOrder.size) { "Subsonic bundle has ${values.size} values" }
    return subsonicKeyOrder.zip(values).toMap()
}

/**
 * Only English and Italian so far.
 *
 * Every other language falls back to English rather than showing a key, because a half-translated
 * settings panel is worse than an untranslated one: the reader can tell the second is deliberate.
 */
private val subsonicBundles: Map<String, Map<String, String>> = mapOf(
    "en" to server(
        "Home server",
        "Stream your own library over the Subsonic API",
        "Turned off",
        "The address of a Subsonic-compatible server — Navidrome, Gonic, Airsonic, Ampache. " +
            "Its tracks appear next to the streamed ones in search, and play from the server itself.",
        "Server address",
        "https://music.example.com",
        "Username",
        "Password",
        "Password (saved)",
        "Test",
        "Testing…",
        "Save",
        "Remove",
        "Cancel",
        "Contacting the server…",
        "Connected",
        "Fill in the address, the user and the password",
        "The server refused those credentials",
        "Could not reach that server"
    ),
    "it" to server(
        "Server di casa",
        "Ascolta la tua libreria tramite l'API Subsonic",
        "Disattivato",
        "L'indirizzo di un server compatibile Subsonic — Navidrome, Gonic, Airsonic, Ampache. " +
            "I suoi brani compaiono accanto a quelli in streaming nella ricerca e suonano dal server stesso.",
        "Indirizzo del server",
        "https://musica.esempio.it",
        "Nome utente",
        "Password",
        "Password (salvata)",
        "Prova",
        "Verifica…",
        "Salva",
        "Rimuovi",
        "Annulla",
        "Contatto il server…",
        "Connesso",
        "Compila indirizzo, utente e password",
        "Il server ha rifiutato queste credenziali",
        "Impossibile raggiungere il server"
    )
)

internal fun subsonicLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(subsonicBundles, code)

internal fun subsonicLocalizationCodes(): Set<String> = subsonicBundles.keys
