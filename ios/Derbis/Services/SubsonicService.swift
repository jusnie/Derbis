import Foundation
import CryptoKit
import Security

@MainActor
final class SubsonicService: ObservableObject {
    static let shared = SubsonicService()

    @Published var config: SubsonicConfig
    @Published var isConnected: Bool = false
    @Published var serverStatus: String = "Not connected"
    @Published var recentTracks: [Track] = []
    @Published var playlists: [SubsonicPlaylistSummary] = []

    private let configKey = "derbis.subsonic.config"
    private let keychainAccount = "derbis.subsonic.password"
    private let apiVersion = "1.16.1"
    private let clientName = "Derbis"

    init() {
        if let data = UserDefaults.standard.data(forKey: configKey),
           let decoded = try? JSONDecoder().decode(SubsonicConfig.self, from: data) {
            self.config = decoded
        } else {
            self.config = .empty
        }
        let hasPassword = !(loadPassword().isEmpty)
        self.isConnected = !config.serverURL.isEmpty && !config.username.isEmpty && hasPassword
        if self.isConnected {
            self.serverStatus = "Configured (\(config.username))"
        }
    }

    func saveCredentials(serverURL: String, username: String, password: String, preferRawLossless: Bool) async -> Bool {
        guard let normalized = Self.normalizeBaseURL(serverURL) else {
            serverStatus = "Invalid server URL"
            isConnected = false
            return false
        }
        let updated = SubsonicConfig(
            serverURL: normalized,
            username: username.trimmingCharacters(in: .whitespacesAndNewlines),
            preferRawLossless: preferRawLossless
        )
        config = updated
        if let encoded = try? JSONEncoder().encode(updated) {
            UserDefaults.standard.set(encoded, forKey: configKey)
        }
        storePassword(password)

        let ok = await testConnection()
        if ok {
            await refreshLibrary()
        }
        return ok
    }

    func disconnect() {
        config = .empty
        UserDefaults.standard.removeObject(forKey: configKey)
        deletePassword()
        isConnected = false
        serverStatus = "Not connected"
        recentTracks = []
        playlists = []
    }

    func testConnection() async -> Bool {
        let password = loadPassword()
        guard !config.serverURL.isEmpty, !config.username.isEmpty, !password.isEmpty else {
            serverStatus = "Missing credentials"
            isConnected = false
            return false
        }
        guard let url = buildEndpointURL(method: "ping", extra: []) else {
            serverStatus = "Invalid server URL"
            isConnected = false
            return false
        }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let resp = root["subsonic-response"] as? [String: Any],
                  let status = resp["status"] as? String else {
                serverStatus = "Unrecognized server response"
                isConnected = false
                return false
            }
            if status == "ok" {
                let serverType = (resp["type"] as? String) ?? "Subsonic"
                let ver = (resp["version"] as? String) ?? apiVersion
                serverStatus = "Connected · \(serverType) \(ver)"
                isConnected = true
                return true
            } else {
                let errMsg = ((resp["error"] as? [String: Any])?["message"] as? String) ?? "Authentication failed"
                serverStatus = errMsg
                isConnected = false
                return false
            }
        } catch {
            serverStatus = "Server unreachable"
            isConnected = false
            return false
        }
    }

    func refreshLibrary() async {
        guard isConnected else { return }
        async let songsTask = fetchRecentSongs()
        async let playlistsTask = fetchPlaylists()
        let (songs, list) = await (songsTask, playlistsTask)
        self.recentTracks = songs
        self.playlists = list
    }

    func search(query: String) async -> [Track] {
        guard isConnected, !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let url = buildEndpointURL(method: "search3", extra: [
                  URLQueryItem(name: "query", value: query),
                  URLQueryItem(name: "songCount", value: "30"),
                  URLQueryItem(name: "albumCount", value: "0"),
                  URLQueryItem(name: "artistCount", value: "0")
              ]) else {
            return []
        }
        guard let (data, _) = try? await URLSession.shared.data(from: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let resp = root["subsonic-response"] as? [String: Any],
              let result = (resp["searchResult3"] as? [String: Any]) ?? (resp["searchResult2"] as? [String: Any]),
              let songs = result["song"] as? [[String: Any]] else {
            return []
        }
        return songs.compactMap { parseSong($0) }
    }

    func fetchPlaylistTracks(playlistId: String) async -> [Track] {
        guard isConnected,
              let url = buildEndpointURL(method: "getPlaylist", extra: [
                  URLQueryItem(name: "id", value: playlistId)
              ]) else {
            return []
        }
        guard let (data, _) = try? await URLSession.shared.data(from: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let resp = root["subsonic-response"] as? [String: Any],
              let playlist = resp["playlist"] as? [String: Any],
              let entries = playlist["entry"] as? [[String: Any]] else {
            return []
        }
        return entries.compactMap { parseSong($0) }
    }

    func signedStreamURL(songId: String) -> URL? {
        var extra = [URLQueryItem(name: "id", value: songId)]
        if config.preferRawLossless {
            extra.append(URLQueryItem(name: "format", value: "raw"))
        } else {
            extra.append(URLQueryItem(name: "format", value: "mp3"))
            extra.append(URLQueryItem(name: "maxBitRate", value: "320"))
        }
        return buildEndpointURL(method: "stream", extra: extra)
    }

    private func fetchRecentSongs() async -> [Track] {
        guard let url = buildEndpointURL(method: "getRandomSongs", extra: [
            URLQueryItem(name: "size", value: "25")
        ]) else { return [] }

        guard let (data, _) = try? await URLSession.shared.data(from: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let resp = root["subsonic-response"] as? [String: Any],
              let container = resp["randomSongs"] as? [String: Any],
              let songs = container["song"] as? [[String: Any]] else {
            return []
        }
        return songs.compactMap { parseSong($0) }
    }

    private func fetchPlaylists() async -> [SubsonicPlaylistSummary] {
        guard let url = buildEndpointURL(method: "getPlaylists", extra: []) else { return [] }
        guard let (data, _) = try? await URLSession.shared.data(from: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let resp = root["subsonic-response"] as? [String: Any],
              let container = resp["playlists"] as? [String: Any],
              let items = container["playlist"] as? [[String: Any]] else {
            return []
        }
        return items.compactMap { dict in
            guard let id = Self.stringId(dict["id"]),
                  let name = dict["name"] as? String else { return nil }
            let count = (dict["songCount"] as? Int) ?? 0
            let dur = (dict["duration"] as? Int) ?? 0
            return SubsonicPlaylistSummary(id: id, name: name, songCount: count, durationSec: dur)
        }
    }

    private func parseSong(_ dict: [String: Any]) -> Track? {
        guard let rawId = Self.stringId(dict["id"]) else { return nil }
        let title = (dict["title"] as? String) ?? "Untitled"
        let artist = (dict["artist"] as? String) ?? "Unknown Artist"
        let album = (dict["album"] as? String) ?? ""
        let duration = (dict["duration"] as? Int) ?? 0
        let suffix = ((dict["suffix"] as? String) ?? "FLAC").uppercased()
        let bitRate = (dict["bitRate"] as? Int) ?? 320

        let coverId = Self.stringId(dict["coverArt"]) ?? rawId
        let coverURL = buildEndpointURL(method: "getCoverArt", extra: [
            URLQueryItem(name: "id", value: coverId),
            URLQueryItem(name: "size", value: "600")
        ])?.absoluteString ?? ""

        let badge = config.preferRawLossless ? suffix : "MP3 320"

        return Track(
            id: "subsonic:\(rawId)",
            title: title,
            artist: artist,
            album: album,
            durationSec: duration,
            thumbnailURL: coverURL,
            largeThumbnailURL: coverURL,
            source: .subsonic,
            codecBadge: badge,
            bitrateKbps: bitRate,
            subsonicSongId: rawId
        )
    }

    private func buildEndpointURL(method: String, extra: [URLQueryItem]) -> URL? {
        let password = loadPassword()
        guard !config.serverURL.isEmpty, !config.username.isEmpty, !password.isEmpty else {
            return nil
        }
        guard var comps = URLComponents(string: "\(config.serverURL)/rest/\(method).view") else {
            return nil
        }
        let salt = Self.randomSalt()
        let token = Self.md5Hex(password + salt)
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "u", value: config.username),
            URLQueryItem(name: "t", value: token),
            URLQueryItem(name: "s", value: salt),
            URLQueryItem(name: "v", value: apiVersion),
            URLQueryItem(name: "c", value: clientName),
            URLQueryItem(name: "f", value: "json")
        ]
        queryItems.append(contentsOf: extra)
        comps.queryItems = queryItems
        return comps.url
    }

    private static func normalizeBaseURL(_ raw: String) -> String? {
        var trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if !trimmed.lowercased().hasPrefix("http://") && !trimmed.lowercased().hasPrefix("https://") {
            trimmed = "https://\(trimmed)"
        }
        while trimmed.hasSuffix("/") {
            trimmed.removeLast()
        }
        if trimmed.lowercased().hasSuffix("/rest") {
            trimmed = String(trimmed.dropLast(5))
        }
        guard URL(string: trimmed)?.host != nil else { return nil }
        return trimmed
    }

    private static func stringId(_ any: Any?) -> String? {
        if let s = any as? String, !s.isEmpty { return s }
        if let n = any as? Int { return String(n) }
        return nil
    }

    private static func randomSalt() -> String {
        let chars = Array("abcdefghijklmnopqrstuvwxyz0123456789")
        return String((0..<12).compactMap { _ in chars.randomElement() })
    }

    private static func md5Hex(_ input: String) -> String {
        let digest = Insecure.MD5.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    func loadPassword() -> String {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess, let data = item as? Data, let str = String(data: data, encoding: .utf8) else {
            return ""
        }
        return str
    }

    private func storePassword(_ password: String) {
        deletePassword()
        guard let data = password.data(using: .utf8) else { return }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keychainAccount,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        SecItemAdd(query as CFDictionary, nil)
    }

    private func deletePassword() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keychainAccount
        ]
        SecItemDelete(query as CFDictionary)
    }
}
