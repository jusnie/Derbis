package com.yasuda.derbis.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Catalog of every destination or action a tab-bar slot can hold.
 *
 * Builtin tabs switch the app between the four root destinations. Shortcuts fire real app-wide
 * actions (library section jumps, the player, Jam) — every entry in the bar does something.
 */
object DerbisDockActions {
    const val Home = "home"
    const val Search = "search"
    const val Explore = "explore"
    const val Library = "library"
    const val Playlists = "playlists"
    const val Favorites = "favorites"
    const val Downloads = "downloads"
    const val NowPlaying = "now_playing"
    const val Jam = "jam"
    const val Artists = "artists"
    const val Recent = "recent"
    const val Stats = "stats"
    const val Local = "local"

    /** Builtin destinations, mapped 1:1 onto [DerbisTab] by name (lowercased). */
    val BuiltinTabs: List<String> = listOf(Home, Search, Explore, Library)

    /** Every action the editor offers, builtins first. */
    val All: List<String> = listOf(Home, Search, Explore, Library, Playlists, Favorites, Downloads, NowPlaying, Jam, Artists, Recent, Stats, Local)

    /** Actions offered when adding a brand-new custom tab. */
    val CustomDestinations: List<String> = All

    fun isBuiltinTab(action: String): Boolean = action in BuiltinTabs

    fun known(action: String): Boolean = action in All

    fun customId(): String = "custom:" + java.util.UUID.randomUUID().toString()

    fun isCustomId(id: String): Boolean = id.startsWith("custom:")
}

/**
 * Default glyph for an action. Custom tabs store their own pick; builtin slots stay null and
 * fall back to these so a reset is just "clear the overrides".
 */
object DerbisDockIcons {
    const val Home = "home"
    const val List = "list"
    const val Search = "search"
    const val Compass = "compass"
    const val Playlist = "playlist"
    const val Plus = "plus"
    const val Heart = "heart"
    const val Note = "note"
    const val Star = "star"
    const val Person = "person"
    const val Download = "download"
    const val Clock = "clock"
    const val Bolt = "bolt"
    const val Grid = "grid"
    const val Chart = "chart"
    const val Folder = "folder"
    const val People = "people"

    /** The 17 glyphs offered by the icon picker, in display order. */
    val Picker: List<String> = listOf(Home, List, Search, Compass, Playlist, Plus, Heart, Note, Star, Person, Download, Clock, Bolt, Grid, Chart, Folder, People)

    fun defaultFor(action: String): String = when (action) {
        DerbisDockActions.Home -> Home
        DerbisDockActions.Search -> Search
        DerbisDockActions.Explore -> Compass
        DerbisDockActions.Library -> List
        DerbisDockActions.Playlists -> Playlist
        DerbisDockActions.Favorites -> Heart
        DerbisDockActions.Downloads -> Download
        DerbisDockActions.NowPlaying -> Note
        DerbisDockActions.Jam -> Bolt
        DerbisDockActions.Artists -> People
        DerbisDockActions.Recent -> Clock
        DerbisDockActions.Stats -> Chart
        DerbisDockActions.Local -> Folder
        else -> Grid
    }
}

enum class DerbisDockLabelMode(val storageId: String) {
    IconsOnly("icons_only"),
    IconsAndNames("icons_and_names");

    companion object {
        fun from(value: String?): DerbisDockLabelMode =
            entries.firstOrNull { it.storageId == value } ?: IconsAndNames
    }
}

/**
 * One slot in the tab bar. [id] is stable and unique; builtin slots use their action id,
 * custom tabs get `custom:<uuid>`. [label]/[icon] are user overrides; null means "default".
 * Custom tabs bind to any catalog action, so they are never decorative dead buttons.
 */
data class DerbisDockItem(
    val id: String,
    val action: String,
    val label: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true
) {
    val isCustom: Boolean get() = DerbisDockActions.isCustomId(id)

    fun normalized(): DerbisDockItem = copy(
        id = id.ifBlank { DerbisDockActions.customId() },
        action = action.trim(),
        label = label?.trim()?.take(40)?.ifBlank { null },
        icon = icon?.takeIf { it in DerbisDockIcons.Picker }
    )

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("action", action)
        label?.let { put("label", it) }
        icon?.let { put("icon", it) }
        put("enabled", enabled)
    }

    companion object {
        internal fun fromJson(json: JSONObject): DerbisDockItem? {
            val action = json.optString("action").orEmpty()
            if (!DerbisDockActions.known(action)) return null
            val id = json.optString("id").orEmpty().ifBlank { if (DerbisDockActions.isBuiltinTab(action) || idLooksBuiltin(action)) action else DerbisDockActions.customId() }
            return DerbisDockItem(
                id = id,
                action = action,
                label = json.optString("label").orEmpty().ifBlank { null },
                icon = json.optString("icon").orEmpty().ifBlank { null },
                enabled = json.optBoolean("enabled", true)
            ).normalized()
        }

        private fun idLooksBuiltin(action: String): Boolean = DerbisDockActions.All.contains(action)
    }
}

/**
 * The persisted tab-bar configuration: slot order (enabled or not), per-slot overrides and the
 * label mode. Serialized as JSON under the `tab_bar_config` preference key.
 */
data class DerbisTabBarConfig(
    val mode: DerbisDockLabelMode = DerbisDockLabelMode.IconsAndNames,
    val items: List<DerbisDockItem> = defaultItems()
) {
    fun enabledCount(): Int = items.count { it.enabled }

    fun enabledItems(): List<DerbisDockItem> = items.filter { it.enabled }

    /** Toggle a slot on/off. Off is refused when it would empty the bar; on is refused past [MAX_ENABLED]. */
    fun withToggled(id: String): DerbisTabBarConfig {
        val item = items.firstOrNull { it.id == id } ?: return this
        return if (item.enabled) {
            if (enabledCount() <= 1) this else copy(items = items.map { if (it.id == id) it.copy(enabled = false) else it })
        } else {
            if (enabledCount() >= MAX_ENABLED) this else copy(items = items.map { if (it.id == id) it.copy(enabled = true) else it })
        }
    }

    fun withMode(mode: DerbisDockLabelMode): DerbisTabBarConfig = copy(mode = mode)

    fun moved(from: Int, to: Int): DerbisTabBarConfig {
        if (from !in items.indices || to !in items.indices || from == to) return this
        return copy(items = items.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Move the item with [id] by [delta] slots (positive = down). Returns null when the move is impossible. */
    fun movedBy(id: String, delta: Int): DerbisTabBarConfig? {
        val index = items.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in items.indices || delta == 0) return null
        return moved(index, target)
    }

    fun withOverride(id: String, label: String?, icon: String?): DerbisTabBarConfig =
        copy(items = items.map { if (it.id == id) it.copy(label = label, icon = icon).normalized() else it })

    /** Add a custom tab bound to [action]. Disabled when the bar is already full. */
    fun withCustomTab(action: String, label: String?, icon: String?): DerbisTabBarConfig {
        if (!DerbisDockActions.known(action)) return this
        val room = enabledCount() < MAX_ENABLED
        val item = DerbisDockItem(
            id = DerbisDockActions.customId(),
            action = action,
            label = label,
            icon = icon,
            enabled = room
        ).normalized()
        return copy(items = items + item)
    }

    fun withRemoved(id: String): DerbisTabBarConfig {
        val item = items.firstOrNull { it.id == id } ?: return this
        if (!item.isCustom) return this
        val remaining = items.filterNot { it.id == id }
        // Never leave the bar empty.
        return if (remaining.none { it.enabled }) {
            this
        } else {
            copy(items = remaining)
        }
    }

    fun normalized(): DerbisTabBarConfig {
        val seen = LinkedHashMap<String, DerbisDockItem>()
        items.forEach { raw ->
            val item = raw.normalized()
            // Custom slots keep their identity; builtin duplicates collapse onto the first.
            val key = if (item.isCustom) item.id else item.action
            if (seen.containsKey(key)) return@forEach
            seen[key] = item
        }
        val merged = seen.values.toList()
        // Every known catalog action the user has never seen (fresh installs on upgrade) is appended,
        // disabled, so the editor always shows the full catalog.
        val missing = DerbisDockActions.All.filter { action -> merged.none { it.action == action } }
            .map { action -> DerbisDockItem(id = action, action = action, enabled = false) }
        val all = merged + missing
        val enabled = all.count { it.enabled }
        val ensured = if (enabled == 0 && all.isNotEmpty()) {
            all.mapIndexed { index, item -> if (index == 0) item.copy(enabled = true) else item }
        } else {
            all
        }
        // Respect the cap for anyone who edited an older/hacked build.
        var budget = MAX_ENABLED
        val capped = ensured.map { item ->
            if (item.enabled) {
                budget -= 1
                if (budget >= 0) item else item.copy(enabled = false)
            } else {
                item
            }
        }
        // Guarantee at least one enabled slot survives capping.
        val finalItems = if (capped.none { it.enabled } && capped.isNotEmpty()) {
            capped.toMutableList().apply { this[0] = this[0].copy(enabled = true) }
        } else {
            capped
        }
        return copy(items = finalItems, mode = mode)
    }

    fun encode(): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("mode", mode.storageId)
        put("items", JSONArray(items.map { it.toJson() }))
    }.toString()

    companion object {
        const val STORAGE_VERSION = 1
        const val MAX_ENABLED = 5
        const val MIN_ENABLED = 1

        fun defaultItems(): List<DerbisDockItem> =
            DerbisDockActions.BuiltinTabs.map { DerbisDockItem(id = it, action = it, enabled = true) } +
                DerbisDockActions.All.filterNot { it in DerbisDockActions.BuiltinTabs }
                    .map { DerbisDockItem(id = it, action = it, enabled = false) }

        fun default(): DerbisTabBarConfig = DerbisTabBarConfig()

        fun decode(raw: String?): DerbisTabBarConfig {
            if (raw.isNullOrBlank()) return default()
            return runCatching {
                val json = JSONObject(raw)
                val itemsJson = json.optJSONArray("items")
                val parsed = buildList {
                    if (itemsJson != null) {
                        for (index in 0 until itemsJson.length()) {
                            val item = itemsJson.optJSONObject(index)?.let(DerbisDockItem::fromJson) ?: continue
                            add(item)
                        }
                    }
                }
                val mode = DerbisDockLabelMode.from(json.optString("mode"))
                if (parsed.isEmpty()) default() else DerbisTabBarConfig(mode = mode, items = parsed).normalized()
            }.getOrDefault(default())
        }
    }
}
