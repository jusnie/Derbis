package com.yasuda.derbis.ui.i18n

import java.util.Locale

internal data class DiscordAuthorizationCopy(
    val connect: String = "Connect Discord",
    val authorize: String = "Authorize in Discord. If the app is not installed, continue in your browser.",
    val unavailable: String = "Discord authorization is not configured for this build.",
    val connecting: String = "Connecting Discord",
    val continueAuthorization: String = "Complete authorization in Discord or your browser, then return here.",
    val reconnect: String = "Reconnect Discord",
    val failed: String = "Discord authorization failed. Try again.",
    val presence: String = "Discord Rich Presence",
    val presenceHint: String = "Share the music you are currently playing.",
    val disconnect: String = "Disconnect Discord",
    val disconnectHint: String = "Remove this app's linked Discord session."
)

internal fun discordAuthorizationCopy(code: String): DiscordAuthorizationCopy = when (code.substringBefore('-').lowercase(Locale.ROOT)) {
    "it" -> DiscordAuthorizationCopy(
        "Collega Discord", "Autorizza in Discord. Se l'app non è installata, continua nel browser.",
        "L'autorizzazione Discord non è configurata per questa build.", "Connessione a Discord",
        "Completa l'autorizzazione in Discord o nel browser, poi torna qui.", "Ricollega Discord",
        "Autorizzazione Discord non riuscita. Riprova.", "Discord Rich Presence",
        "Condividi la musica che stai ascoltando.", "Scollega Discord", "Rimuovi la sessione Discord collegata a questa app."
    )
    "fil", "tl" -> DiscordAuthorizationCopy(
        "Ikonekta ang Discord", "Mag-apruba sa Discord. Kung walang Discord app, magpatuloy sa browser.",
        "Hindi pa naka-configure ang Discord authorization sa build na ito.", "Kumokonekta sa Discord",
        "Tapusin ang pag-apruba sa Discord o browser, at bumalik dito.", "Ikonekta muli ang Discord",
        "Hindi nagtagumpay ang pag-apruba sa Discord. Subukan muli.", "Discord Rich Presence",
        "Ibahagi ang musikang kasalukuyan mong pinapakinggan.", "Idiskonekta ang Discord", "Alisin ang Discord session na nakakonekta sa app na ito."
    )
    else -> DiscordAuthorizationCopy()
}
