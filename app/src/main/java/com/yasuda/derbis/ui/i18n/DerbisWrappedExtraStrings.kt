package com.yasuda.derbis.ui.i18n

/**
 * Copy for the Wrapped cards added on top of the original six: albums, the biggest day and the
 * listening archetype.
 *
 * Kept in its own bundle rather than appended to [listeningReceiptKeys] because that bundle is
 * positional — every locale passes its lines in order and a `require` guards the count — so one
 * new key there would mean retyping twenty bundles to add a line each. Here a locale simply
 * overrides the keys it has and inherits the English line for the rest.
 */
internal val wrappedExtraKeys = setOf(
    "wrappedAlbumsTitle",
    "wrappedAlbumPlays",
    "wrappedBiggestDayTitle",
    "wrappedBiggestDayMultiple",
    "wrappedBiggestDayOnRepeat",
    "wrappedPersonaTitle",
    "wrappedPersonaRegular",
    "wrappedPersonaRegularBody",
    "wrappedPersonaOmnivore",
    "wrappedPersonaOmnivoreBody",
    "wrappedPersonaDevotee",
    "wrappedPersonaDevoteeBody",
    "wrappedPersonaDeepDiver",
    "wrappedPersonaDeepDiverBody",
    "wrappedAxisConsistency",
    "wrappedAxisDiversity",
    "wrappedAxisDevotion",
    "wrappedAxisReplay",
    "wrappedNotEnoughTitle",
    "wrappedNotEnoughBody",
    "wrappedPlaysUnit"
)

private fun wrappedExtra(vararg values: String): Map<String, String> {
    require(values.size == wrappedExtraKeys.size) { "wrapped extra bundle is positional" }
    return wrappedExtraKeys.zip(values).toMap()
}

private val wrappedExtraBundles: Map<String, Map<String, String>> = mapOf(
    "en" to wrappedExtra(
        "The albums you lived in",
        "%s plays",
        "Your biggest day",
        "%s× a normal day",
        "On repeat that day",
        "Your listening shape",
        "The Regular",
        "Music nearly every day, spread evenly across the year.",
        "The Omnivore",
        "Wide and restless: many artists, none of them owning you.",
        "The Devotee",
        "A handful of artists took most of your year.",
        "The Deep Diver",
        "The same songs, again — and deeper every time.",
        "Consistency",
        "Variety",
        "Devotion",
        "Replay",
        "Not enough of %s yet",
        "Listen on %s more days and your story writes itself.",
        "plays"
    ),
    "it" to wrappedExtra(
        "Gli album in cui hai vissuto",
        "%s ascolti",
        "Il tuo giorno più grande",
        "%s× un giorno normale",
        "In ripetizione quel giorno",
        "La forma del tuo ascolto",
        "Il Costante",
        "Musica quasi ogni giorno, distribuita lungo tutto l'anno.",
        "L'Onnivoro",
        "Ampio e irrequieto: molti artisti, nessuno che ti possieda.",
        "Il Devoto",
        "Pochi artisti si sono presi quasi tutto il tuo anno.",
        "Il Subacqueo",
        "Le stesse canzoni, ancora — e ogni volta più a fondo.",
        "Costanza",
        "Varietà",
        "Devozione",
        "Ripetizione",
        "Non c'è ancora abbastanza del %s",
        "Ascolta per altri %s giorni e la tua storia si scrive da sola.",
        "ascolti"
    ),
    "es" to wrappedExtra(
        "Los álbumes en los que viviste",
        "%s reproducciones",
        "Tu día más grande",
        "%s× un día normal",
        "En repetición ese día",
        "La forma de tu escucha",
        "El Constante",
        "Música casi todos los días, repartida por todo el año.",
        "El Omnívoro",
        "Amplio e inquieto: muchos artistas, ninguno dueño de ti.",
        "El Devoto",
        "Unos pocos artistas se llevaron casi todo tu año.",
        "El Buceador",
        "Las mismas canciones, otra vez, y cada vez más profundo.",
        "Constancia",
        "Variedad",
        "Devoción",
        "Repetición",
        "Todavía no hay suficiente de %s",
        "Escucha %s días más y tu historia se escribe sola.",
        "reproducciones"
    ),
    "fr" to wrappedExtra(
        "Les albums où tu as vécu",
        "%s écoutes",
        "Ton plus grand jour",
        "%s× un jour normal",
        "En boucle ce jour-là",
        "La forme de ton écoute",
        "Le Régulier",
        "De la musique presque chaque jour, répartie sur toute l'année.",
        "L'Omnivore",
        "Large et agité : beaucoup d'artistes, aucun qui te possède.",
        "Le Dévoué",
        "Une poignée d'artistes a pris presque toute ton année.",
        "Le Plongeur",
        "Les mêmes chansons, encore — et toujours plus profond.",
        "Régularité",
        "Variété",
        "Dévotion",
        "Replay",
        "Pas encore assez de %s",
        "Écoute %s jours de plus et ton histoire s'écrit toute seule.",
        "écoutes"
    ),
    "de" to wrappedExtra(
        "Die Alben, in denen du gelebt hast",
        "%s Wiedergaben",
        "Dein größter Tag",
        "%s× ein normaler Tag",
        "An dem Tag in Schleife",
        "Die Form deines Hörens",
        "Der Beständige",
        "Fast jeden Tag Musik, gleichmäßig über das Jahr verteilt.",
        "Der Allesfresser",
        "Weit und rastlos: viele Künstler, keiner besitzt dich.",
        "Der Hingebungsvolle",
        "Eine Handvoll Künstler hat fast dein ganzes Jahr gefüllt.",
        "Der Taucher",
        "Dieselben Lieder, wieder — und jedes Mal tiefer.",
        "Beständigkeit",
        "Vielfalt",
        "Hingabe",
        "Wiederholung",
        "Noch nicht genug von %s",
        "Hör %s Tage länger zu, dann schreibt sich deine Geschichte von selbst.",
        "Wiedergaben"
    ),
    "pt" to wrappedExtra(
        "Os álbuns em que você viveu",
        "%s reproduções",
        "Seu maior dia",
        "%s× um dia normal",
        "Em repetição naquele dia",
        "A forma da sua escuta",
        "O Constante",
        "Música quase todos os dias, espalhada pelo ano inteiro.",
        "O Onívoro",
        "Amplo e inquieto: muitos artistas, nenhum dono de você.",
        "O Devoto",
        "Poucos artistas levaram quase todo o seu ano.",
        "O Mergulhador",
        "As mesmas canções, de novo — e mais fundo a cada vez.",
        "Constância",
        "Variedade",
        "Devoção",
        "Repetição",
        "Ainda não há %s suficiente",
        "Ouça por mais %s dias e sua história se escreve sozinha.",
        "reproduções"
    )
)

internal fun wrappedExtraLocalizationEntries(code: String): Map<String, String> {
    val english = wrappedExtraBundles["en"].orEmpty()
    val localized = wrappedExtraBundles[code] ?: return english
    return if (code == "en") localized else english + localized
}
