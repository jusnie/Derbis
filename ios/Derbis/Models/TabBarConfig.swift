import Foundation

/// Persisted shape of the user's tab bar configuration for the iOS app.
///
/// Mirrors the Android model (`DerbisTabBar.kt`): the persisted list is the full catalog
/// (enabled or not, in the user's order), stored as JSON under the `derbis.tabBarConfig`
/// defaults key. Builtin slots use their action id as the stable slot id; custom tabs get
/// `custom:<uuid>`.

enum TabBarLabelMode: String, Codable, CaseIterable, Sendable {
    case iconsOnly = "icons_only"
    case iconsAndNames = "icons_and_names"
}

/// One slot in the tab bar.
struct TabBarTabItem: Codable, Equatable, Identifiable, Sendable {
    /// Stable unique slot id. Builtins use their action id; custom tabs use `custom:<uuid>`.
    var id: String
    /// Catalog action behind the slot (see `DerbisDockCatalog`).
    var action: String
    /// User override for the display name. `nil` means "the action's default label".
    var label: String?
    /// User override for the glyph (icon id from `DerbisDockIcons`). `nil` means "default".
    var icon: String?
    var enabled: Bool

    var isCustom: Bool { id.hasPrefix("custom:") }

    func normalized() -> TabBarTabItem {
        var item = self
        if id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            item.id = DerbisDockCatalog.customID()
        }
        item.action = action.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmed = label?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        item.label = trimmed.isEmpty ? nil : String(trimmed.prefix(40))
        if let icon = item.icon, !DerbisDockIcons.picker.contains(icon) {
            item.icon = nil
        }
        return item
    }
}

/// Catalog of every destination or action a tab bar slot can hold.
///
/// Builtin tabs switch the app between the root destinations. Shortcuts fire real app-wide
/// actions (library section jumps, the player, Jam). Every entry in the bar does something.
enum DerbisDockCatalog {
    static let home = "home"
    static let explore = "explore"
    static let library = "library"
    static let settings = "settings"
    static let playlists = "playlists"
    static let favorites = "favorites"
    static let downloads = "downloads"
    static let nowPlaying = "now_playing"
    static let stats = "stats"
    static let local = "local"
    static let jam = "jam"

    /// Builtin destinations, mapped 1:1 onto the app's root tabs.
    static let builtinTabs: [String] = [home, explore, library, settings]

    /// Every action the editor offers, builtins first. iOS has no dedicated Search tab;
    /// Explore *is* the search surface, so the catalog is honest about what exists.
    static let all: [String] = [
        home, explore, library, settings,
        playlists, favorites, downloads, nowPlaying, stats, local, jam
    ]

    /// Actions offered when adding a brand-new tab.
    static let customDestinations = all

    static func isBuiltinTab(_ action: String) -> Bool { builtinTabs.contains(action) }
    static func isKnown(_ action: String) -> Bool { all.contains(action) }
    static func customID() -> String { "custom:" + UUID().uuidString }

    static func defaultLabel(for action: String) -> String {
        switch action {
        case home: return "Home"
        case explore: return "Explore"
        case library: return "Library"
        case settings: return "Studio"
        case playlists: return "Playlists"
        case favorites: return "Favorites"
        case downloads: return "Downloads"
        case nowPlaying: return "Now Playing"
        case stats: return "Stats"
        case local: return "Local Files"
        case jam: return "Jam"
        default: return action
        }
    }

    /// Icon id used when the user has not picked one.
    static func defaultIcon(for action: String) -> String {
        switch action {
        case home: return "home"
        case explore: return "compass"
        case library: return "list"
        case settings: return "slider"
        case playlists: return "playlist"
        case favorites: return "heart"
        case downloads: return "download"
        case nowPlaying: return "note"
        case stats: return "chart"
        case local: return "folder"
        case jam: return "people"
        default: return "grid"
        }
    }
}

/// The SF Symbol for an icon-picker id.
enum DerbisDockIcons {
    static let home = "home"
    static let list = "list"
    static let search = "search"
    static let compass = "compass"
    static let playlist = "playlist"
    static let plus = "plus"
    static let heart = "heart"
    static let note = "note"
    static let star = "star"
    static let person = "person"
    static let download = "download"
    static let clock = "clock"
    static let bolt = "bolt"
    static let grid = "grid"
    static let slider = "slider"
    static let chart = "chart"
    static let folder = "folder"
    static let people = "people"

    static let symbols: [String: String] = [
        home: "sparkles.rectangle.stack.fill",
        list: "square.stack.3d.up.fill",
        search: "magnifyingglass",
        compass: "safari.fill",
        playlist: "music.note.list",
        plus: "plus.circle.fill",
        heart: "heart.fill",
        note: "music.note",
        star: "star.fill",
        person: "person.fill",
        download: "arrow.down.circle.fill",
        clock: "clock.fill",
        bolt: "bolt.fill",
        grid: "square.grid.2x2.fill",
        slider: "slider.horizontal.3",
        chart: "chart.bar.fill",
        folder: "folder.fill",
        people: "person.3.fill"
    ]

    static let picker: [String] = [
        home, list, search, compass, playlist, plus, heart, note, star,
        person, download, clock, bolt, grid, slider, chart, folder, people
    ]

    static func symbol(for iconID: String) -> String {
        symbols[iconID] ?? symbols[grid]!
    }
}

/// The persisted tab bar configuration: slot order (enabled or not), per-slot overrides and
/// the label mode.
struct TabBarConfig: Codable, Equatable, Sendable {
    static let maxEnabled = 5
    static let minEnabled = 1
    static let storageVersion = 1

    var mode: TabBarLabelMode
    var items: [TabBarTabItem]

    static func defaultItems() -> [TabBarTabItem] {
        DerbisDockCatalog.builtinTabs.map { TabBarTabItem(id: $0, action: $0, label: nil, icon: nil, enabled: true) }
            + DerbisDockCatalog.all.filter { !DerbisDockCatalog.builtinTabs.contains($0) }
                .map { TabBarTabItem(id: $0, action: $0, label: nil, icon: nil, enabled: false) }
    }

    static func `default`() -> TabBarConfig {
        TabBarConfig(mode: .iconsAndNames, items: defaultItems())
    }

    func encode() -> String {
        TabBarConfigCodec.encode(self)
            ?? (String(data: (try? JSONEncoder().encode(self)) ?? Data(), encoding: .utf8) ?? "")
    }

    static func decode(from raw: String?) -> TabBarConfig {
        guard let raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return .default()
        }
        if let decoded = TabBarConfigCodec.decode(raw) {
            return decoded.normalized()
        }
        if let data = raw.data(using: .utf8),
           let decoded = try? JSONDecoder().decode(TabBarConfig.self, from: data) {
            return decoded.normalized()
        }
        return .default()
    }

    func normalized() -> TabBarConfig {
        var seen = Set<String>()
        var merged: [TabBarTabItem] = []
        for rawItem in items {
            let item = rawItem.normalized()
            guard DerbisDockCatalog.isKnown(item.action) else { continue }
            let key = item.isCustom ? item.id : item.action
            if seen.contains(key) { continue }
            seen.insert(key)
            merged.append(item)
        }
        // Fresh installs on upgrade: append any never-seen catalog action, disabled.
        for action in DerbisDockCatalog.all where !merged.contains(where: { $0.action == action }) {
            merged.append(TabBarTabItem(id: action, action: action, label: nil, icon: nil, enabled: false))
        }
        if !merged.isEmpty, !merged.contains(where: { $0.enabled }) {
            merged[0].enabled = true
        }
        // Respect the cap, in display order.
        var budget = Self.maxEnabled
        var capped: [TabBarTabItem] = []
        for var item in merged {
            if item.enabled {
                budget -= 1
                if budget < 0 { item.enabled = false }
            }
            capped.append(item)
        }
        if !capped.contains(where: { $0.enabled }), !capped.isEmpty {
            capped[0].enabled = true
        }
        return TabBarConfig(mode: mode, items: capped)
    }

    func enabledCount() -> Int { items.count { $0.enabled } }
    func enabledItems() -> [TabBarTabItem] { items.filter(\.enabled) }

    /// Toggle a slot on/off. Off is refused when it would empty the bar; on is refused past the cap.
    func withToggled(_ id: String) -> TabBarConfig {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return self }
        if items[index].enabled {
            if enabledCount() <= Self.minEnabled { return self }
            var copy = items
            copy[index].enabled = false
            return TabBarConfig(mode: mode, items: copy)
        } else {
            if enabledCount() >= Self.maxEnabled { return self }
            var copy = items
            copy[index].enabled = true
            return TabBarConfig(mode: mode, items: copy)
        }
    }

    func withMode(_ newMode: TabBarLabelMode) -> TabBarConfig {
        TabBarConfig(mode: newMode, items: items)
    }

    func moved(from source: Int, to destination: Int) -> TabBarConfig {
        guard items.indices.contains(source), items.indices.contains(destination), source != destination else { return self }
        var copy = items
        let element = copy.remove(at: source)
        copy.insert(element, at: destination)
        return TabBarConfig(mode: mode, items: copy)
    }

    func withOverride(id: String, label: String?, icon: String?) -> TabBarConfig {
        TabBarConfig(
            mode: mode,
            items: items.map {
                $0.id == id ? TabBarTabItem(id: $0.id, action: $0.action, label: label, icon: icon, enabled: $0.enabled).normalized() : $0
            }
        )
    }

    /// Add a custom tab bound to [action]. Starts disabled when the bar is already full.
    func withCustomTab(action: String, label: String?, icon: String?) -> TabBarConfig {
        guard DerbisDockCatalog.isKnown(action) else { return self }
        let item = TabBarTabItem(
            id: DerbisDockCatalog.customID(),
            action: action,
            label: label,
            icon: icon,
            enabled: enabledCount() < Self.maxEnabled
        ).normalized()
        return TabBarConfig(mode: mode, items: items + [item])
    }

    func withRemoved(id: String) -> TabBarConfig {
        guard let item = items.first(where: { $0.id == id }), item.isCustom else { return self }
        let remaining = items.filter { $0.id != id }
        if !remaining.contains(where: { $0.enabled }) { return self }
        return TabBarConfig(mode: mode, items: remaining)
    }
}

/// Hand-rolled JSON codec for the persisted config. The top-level shape mirrors Android:
/// `{"version":1,"mode":"icons_and_names","items":[...]}`.
enum TabBarConfigCodec {
    struct StoredItem: Codable {
        var id: String
        var action: String
        var label: String?
        var icon: String?
        var enabled: Bool
    }

    struct StoredConfig: Codable {
        var version: Int?
        var mode: String?
        var items: [StoredItem]?
    }

    static func encode(_ config: TabBarConfig) -> String? {
        let stored = StoredConfig(
            version: TabBarConfig.storageVersion,
            mode: config.mode.rawValue,
            items: config.items.map {
                StoredItem(id: $0.id, action: $0.action, label: $0.label, icon: $0.icon, enabled: $0.enabled)
            }
        )
        let encoder = JSONEncoder()
        guard let data = try? encoder.encode(stored) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func decode(_ raw: String) -> TabBarConfig? {
        guard let data = raw.data(using: .utf8),
              let stored = try? JSONDecoder().decode(StoredConfig.self, from: data)
        else { return nil }
        let items: [TabBarTabItem] = (stored.items ?? []).compactMap { item in
            guard DerbisDockCatalog.isKnown(item.action) else { return nil }
            var tab = TabBarTabItem(id: item.id, action: item.action, label: item.label, icon: item.icon, enabled: item.enabled)
            if tab.id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                tab.id = DerbisDockCatalog.isBuiltinTab(tab.action) ? tab.action : DerbisDockCatalog.customID()
            }
            return tab.normalized()
        }
        guard !items.isEmpty else { return nil }
        let mode = stored.mode.flatMap(TabBarLabelMode.init(rawValue:)) ?? .iconsAndNames
        return TabBarConfig(mode: mode, items: items)
    }
}
