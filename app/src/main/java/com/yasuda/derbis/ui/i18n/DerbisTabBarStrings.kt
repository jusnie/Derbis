package com.yasuda.derbis.ui.i18n

enum class TabBarText {
    Title, Subtitle, Appearance, IconsOnly, IconsAndNames,
    Tabs, TabsHint, Show, DragHandle, AddTab, AddTabSubtitle,
    Name, NamePlaceholder, Opens, Icon, UseDefaultName, UseDefaultIcon,
    DeleteTab, Reset, ResetTitle, ResetBody, MaxTabs, LastTab
}

class TabBarCopy internal constructor(private val texts: Map<TabBarText, String>) {
    operator fun get(key: TabBarText): String = texts[key] ?: englishTabBar.getValue(key)
}

private fun tabBar(vararg value: String): Map<TabBarText, String> =
    if (value.size == TabBarText.entries.size) TabBarText.entries.zip(value).toMap() else emptyMap()

private val englishTabBar: Map<TabBarText, String> = tabBar(
    "Tab bar", "Pick the tabs you want, reorder them or add your own", "Appearance", "Icons only", "Icons and names",
    "Tabs", "Drag to reorder. Up to 5 tabs can be shown.", "Show tab", "Drag to reorder", "Add a tab", "A shortcut with your own name and icon",
    "Name", "Tab name", "Opens", "Icon", "Use default name", "Use default icon",
    "Delete tab", "Use Derbis tabs", "Reset the tab bar?", "The bar returns to Home, Search, Explore and Library with default icons and names. Custom tabs are removed.", "You can show up to 5 tabs.", "Keep at least one tab shown."
)

private val tabBarCopies: Map<String, Map<TabBarText, String>> = mapOf(
    "en" to englishTabBar,
    "it" to tabBar(
        "Barra delle schede", "Scegli le schede, riordinale o aggiungine una tua", "Aspetto", "Solo icone", "Icone e nomi",
        "Schede", "Trascina per riordinare. Puoi mostrare fino a 5 schede.", "Mostra scheda", "Trascina per riordinare", "Aggiungi una scheda", "Un collegamento con nome e icona tuoi",
        "Nome", "Nome della scheda", "Apre", "Icona", "Usa nome predefinito", "Usa icona predefinita",
        "Elimina scheda", "Usa le schede Derbis", "Ripristinare la barra delle schede?", "La barra torna a Home, Cerca, Esplora e Libreria con icone e nomi predefiniti. Le schede personalizzate vengono rimosse.", "Puoi mostrare fino a 5 schede.", "Mantieni almeno una scheda visibile."
    ),
    "es" to tabBar(
        "Barra de pestañas", "Elige las pestañas, reordénalas o añade la tuya", "Apariencia", "Solo iconos", "Iconos y nombres",
        "Pestañas", "Arrastra para reordenar. Puedes mostrar hasta 5 pestañas.", "Mostrar pestaña", "Arrastra para reordenar", "Añadir pestaña", "Un acceso directo con tu propio nombre e icono",
        "Nombre", "Nombre de la pestaña", "Abre", "Icono", "Usar nombre predeterminado", "Usar icono predeterminado",
        "Eliminar pestaña", "Usar las pestañas de Derbis", "¿Restablecer la barra de pestañas?", "La barra vuelve a Inicio, Buscar, Explorar y Biblioteca con iconos y nombres predeterminados. Se eliminan las pestañas personalizadas.", "Puedes mostrar hasta 5 pestañas.", "Mantén al menos una pestaña visible."
    ),
    "fr" to tabBar(
        "Barre d'onglets", "Choisissez vos onglets, réorganisez-les ou ajoutez le vôtre", "Apparence", "Icônes uniquement", "Icônes et noms",
        "Onglets", "Faites glisser pour réorganiser. Jusqu'à 5 onglets peuvent être affichés.", "Afficher l'onglet", "Faites glisser pour réorganiser", "Ajouter un onglet", "Un raccourci avec votre propre nom et votre icône",
        "Nom", "Nom de l'onglet", "Ouvre", "Icône", "Utiliser le nom par défaut", "Utiliser l'icône par défaut",
        "Supprimer l'onglet", "Utiliser les onglets Derbis", "Réinitialiser la barre d'onglets ?", "La barre revient à Accueil, Rechercher, Explorer et Bibliothèque avec les icônes et noms par défaut. Les onglets personnalisés sont supprimés.", "Vous pouvez afficher jusqu'à 5 onglets.", "Conservez au moins un onglet affiché."
    ),
    "de" to tabBar(
        "Tab-Leiste", "Wähle Tabs aus, sortiere sie um oder füge eigene hinzu", "Darstellung", "Nur Symbole", "Symbole und Namen",
        "Tabs", "Zum Sortieren ziehen. Es können bis zu 5 Tabs angezeigt werden.", "Tab anzeigen", "Zum Sortieren ziehen", "Tab hinzufügen", "Eine Verknüpfung mit eigenem Namen und Symbol",
        "Name", "Tab-Name", "Öffnet", "Symbol", "Standardnamen verwenden", "Standardsymbol verwenden",
        "Tab löschen", "Derbis-Tabs verwenden", "Tab-Leiste zurücksetzen?", "Die Leiste kehrt zu Start, Suche, Entdecken und Mediathek mit Standardsymbolen und -namen zurück. Eigene Tabs werden entfernt.", "Es können bis zu 5 Tabs angezeigt werden.", "Lass mindestens einen Tab sichtbar."
    ),
    "pt" to tabBar(
        "Barra de separadores", "Escolhe os separadores, reordena-os ou adiciona um teu", "Aparência", "Só ícones", "Ícones e nomes",
        "Separadores", "Arrasta para reordenar. Podes mostrar até 5 separadores.", "Mostrar separador", "Arrasta para reordenar", "Adicionar separador", "Um atalho com o teu nome e ícone",
        "Nome", "Nome do separador", "Abre", "Ícone", "Usar nome predefinido", "Usar ícone predefinido",
        "Eliminar separador", "Usar separadores Derbis", "Repor a barra de separadores?", "A barra volta a Início, Procurar, Explorar e Biblioteca com ícones e nomes predefinidos. Os separadores personalizados são removidos.", "Podes mostrar até 5 separadores.", "Mantém pelo menos um separador visível."
    ),
    "nl" to tabBar(
        "Tabbladbalk", "Kies je tabbladen, herschik ze of voeg een eigen toe", "Weergave", "Alleen pictogrammen", "Pictogrammen en namen",
        "Tabbladen", "Sleep om te herschikken. Er kunnen maximaal 5 tabbladen worden weergegeven.", "Tabblad tonen", "Sleep om te herschikken", "Tabblad toevoegen", "Een snelkoppeling met je eigen naam en pictogram",
        "Naam", "Naam van tabblad", "Opent", "Pictogram", "Standaardnaam gebruiken", "Standaardpictogram gebruiken",
        "Tabblad verwijderen", "Derbis-tabbladen gebruiken", "Tabbladbalk herstellen?", "De balk keert terug naar Home, Zoeken, Verkennen en Bibliotheek met standaardpictogrammen en -namen. Eigen tabbladen worden verwijderd.", "Je kunt maximaal 5 tabbladen tonen.", "Houd minstens één tabblad zichtbaar."
    ),
    "pl" to tabBar(
        "Pasek kart", "Wybierz karty, zmień ich kolejność lub dodaj własną", "Wygląd", "Tylko ikony", "Ikony i nazwy",
        "Karty", "Przeciągnij, aby zmienić kolejność. Można pokazać do 5 kart.", "Pokaż kartę", "Przeciągnij, aby zmienić kolejność", "Dodaj kartę", "Skrót z własną nazwą i ikoną",
        "Nazwa", "Nazwa karty", "Otwiera", "Ikona", "Użyj domyślnej nazwy", "Użyj domyślnej ikony",
        "Usuń kartę", "Użyj kart Derbis", "Przywrócić pasek kart?", "Pasek wróci do kart Start, Szukaj, Odkryj i Biblioteka z domyślnymi ikonami i nazwami. Niestandardowe karty zostaną usunięte.", "Można pokazać do 5 kart.", "Zachowaj co najmniej jedną kartę."
    ),
    "ru" to tabBar(
        "Панель вкладок", "Выберите вкладки, измените порядок или добавьте свою", "Оформление", "Только значки", "Значки и названия",
        "Вкладки", "Перетащите, чтобы изменить порядок. Можно показать до 5 вкладок.", "Показать вкладку", "Перетащите для изменения порядка", "Добавить вкладку", "Ярлык со своим названием и значком",
        "Название", "Название вкладки", "Открывает", "Значок", "Использовать название по умолчанию", "Использовать значок по умолчанию",
        "Удалить вкладку", "Вкладки Derbis", "Сбросить панель вкладок?", "Панель вернётся к вкладкам «Главная», «Поиск», «Обзор» и «Медиатека» со значками и названиями по умолчанию. Пользовательские вкладки будут удалены.", "Можно показать до 5 вкладок.", "Оставьте хотя бы одну вкладку."
    ),
    "tr" to tabBar(
        "Sekme çubuğu", "Sekmelerini seç, yeniden sırala ya da kendin ekle", "Görünüm", "Yalnızca simgeler", "Simgeler ve adlar",
        "Sekmeler", "Yeniden sıralamak için sürükleyin. En fazla 5 sekme gösterilebilir.", "Sekmeyi göster", "Yeniden sıralamak için sürükleyin", "Sekme ekle", "Kendi adın ve simgenle bir kısayol",
        "Ad", "Sekme adı", "Açar", "Simge", "Varsayılan adı kullan", "Varsayılan simgeyi kullan",
        "Sekmeyi sil", "Derbis sekmelerini kullan", "Sekme çubuğu sıfırlansın mı?", "Çubuk; Ana sayfa, Ara, Keşfet ve Kitaplık sekmelerine varsayılan simge ve adlarla döner. Özel sekmeleriniz kaldırılır.", "En fazla 5 sekme gösterebilirsiniz.", "En az bir sekme görünür kalsın."
    ),
    "ja" to tabBar(
        "タブバー", "表示するタブを選んで並べ替えたり、自分のタブを追加できます", "外観", "アイコンのみ", "アイコンと名前",
        "タブ", "ドラッグで並べ替え。最大5つまで表示できます。", "タブを表示", "ドラッグして並べ替え", "タブを追加", "自分の名前とアイコンのショートカット",
        "名前", "タブ名", "開く", "アイコン", "デフォルト名を使う", "デフォルトのアイコンを使う",
        "タブを削除", "Derbisのタブを使う", "タブバーをリセットしますか？", "ホーム・検索・見つける・ライブラリのデフォルト表示に戻ります。カスタムタブは削除されます。", "最大5つまで表示できます。", "少なくとも1つのタブを表示してください。"
    ),
    "id" to tabBar(
        "Bilah tab", "Pilih tab yang muncul, urutkan ulang, atau tambahkan milikmu", "Tampilan", "Hanya ikon", "Ikon dan nama",
        "Tab", "Seret untuk mengurutkan. Hingga 5 tab dapat ditampilkan.", "Tampilkan tab", "Seret untuk mengurutkan", "Tambah tab", "Pintasan dengan nama dan ikon buatanmu",
        "Nama", "Nama tab", "Membuka", "Ikon", "Gunakan nama default", "Gunakan ikon default",
        "Hapus tab", "Gunakan tab Derbis", "Atur ulang bilah tab?", "Bilah kembali ke Home, Cari, Jelajah dan Pustaka dengan ikon dan nama default. Tab kustom akan dihapus.", "Kamu dapat menampilkan hingga 5 tab.", "Biarkan setidaknya satu tab tetap tampil."
    )
)

internal val DerbisStrings.tabBarCopy: TabBarCopy
    get() = TabBarCopy(tabBarCopies[code] ?: englishTabBar)
