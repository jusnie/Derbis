package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.i18n.DerbisStrings

internal val DerbisStrings.preloadNextTrack: String
    get() = if (code == "it") "Precarica il brano successivo" else "Preload next track"

internal val DerbisStrings.preloadNextTrackSubtitle: String
    get() = if (code == "it") {
        "Prepara in anticipo il brano successivo per passaggi più rapidi. " +
            "Disattiva per ridurre l'attività di rete in background."
    } else {
        "Prepare the next track in advance for faster transitions. " +
            "Disable to reduce background network activity."
    }
