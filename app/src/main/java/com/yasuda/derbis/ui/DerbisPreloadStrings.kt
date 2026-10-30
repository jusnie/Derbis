package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.i18n.DerbisStrings

internal val DerbisStrings.preloadNextTrack: String
    get() = if (code == "it") "Precaricamento flusso brano successivo (ArchiveTune)" else "Next-Song Stream Preloading"

internal val DerbisStrings.preloadNextTrackSubtitle: String
    get() = if (code == "it") {
        "Pre-carica in anticipo 1.5 MB di flusso audio prima del termine del brano per transizioni gapless istantanee e crossfade senza interruzioni."
    } else {
        "Pre-buffers audio stream before current song ends for instant gapless transitions and stutter-free crossfading."
    }
