import Foundation

actor LyricsService {
    static let shared = LyricsService()

    private let session: URLSession
    private var memoryCache: [String: LyricsPayload] = [:]

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 8
        self.session = URLSession(configuration: config)
    }

    func fetchLyrics(for track: Track) async -> LyricsPayload? {
        if let cached = memoryCache[track.id] {
            return cached
        }
        if let disk = loadFromDisk(trackId: track.id) {
            memoryCache[track.id] = disk
            return disk
        }

        let cleanTitle = sanitizeQueryTitle(track.title)
        let cleanArtist = track.artist
            .replacingOccurrences(of: " - Topic", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if let direct = await queryDirect(trackId: track.id, title: cleanTitle, artist: cleanArtist, album: track.album, durationSec: track.durationSec) {
            memoryCache[track.id] = direct
            saveToDisk(direct)
            return direct
        }

        if let searched = await querySearch(trackId: track.id, title: cleanTitle, artist: cleanArtist, durationSec: track.durationSec) {
            memoryCache[track.id] = searched
            saveToDisk(searched)
            return searched
        }

        return nil
    }

    func clearCache() {
        memoryCache.removeAll()
        if let dir = cacheDirectory() {
            try? FileManager.default.removeItem(at: dir)
        }
    }

    private func queryDirect(trackId: String, title: String, artist: String, album: String, durationSec: Int) async -> LyricsPayload? {
        guard var comps = URLComponents(string: "https://lrclib.net/api/get") else { return nil }
        var items = [
            URLQueryItem(name: "track_name", value: title),
            URLQueryItem(name: "artist_name", value: artist)
        ]
        if !album.isEmpty {
            items.append(URLQueryItem(name: "album_name", value: album))
        }
        if durationSec > 0 {
            items.append(URLQueryItem(name: "duration", value: String(durationSec)))
        }
        comps.queryItems = items
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.setValue("Derbis-iOS/1.8.0 (https://github.com/jusnie/Derbis)", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return parseLRCLibObject(json, trackId: trackId)
    }

    private func querySearch(trackId: String, title: String, artist: String, durationSec: Int) async -> LyricsPayload? {
        guard var comps = URLComponents(string: "https://lrclib.net/api/search") else { return nil }
        comps.queryItems = [
            URLQueryItem(name: "q", value: "\(title) \(artist)")
        ]
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.setValue("Derbis-iOS/1.8.0 (https://github.com/jusnie/Derbis)", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]],
              !array.isEmpty else {
            return nil
        }

        let sorted = array.sorted { a, b in
            let aHasSync = ((a["syncedLyrics"] as? String) ?? "").isEmpty == false
            let bHasSync = ((b["syncedLyrics"] as? String) ?? "").isEmpty == false
            if aHasSync != bHasSync { return aHasSync }
            if durationSec > 0 {
                let aDur = Int((a["duration"] as? Double) ?? Double((a["duration"] as? Int) ?? 0))
                let bDur = Int((b["duration"] as? Double) ?? Double((b["duration"] as? Int) ?? 0))
                return abs(aDur - durationSec) < abs(bDur - durationSec)
            }
            return true
        }

        for candidate in sorted {
            if let parsed = parseLRCLibObject(candidate, trackId: trackId) {
                return parsed
            }
        }
        return nil
    }

    private func parseLRCLibObject(_ json: [String: Any], trackId: String) -> LyricsPayload? {
        if let syncedText = json["syncedLyrics"] as? String, !syncedText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let lines = parseLRC(syncedText)
            if !lines.isEmpty {
                return LyricsPayload(trackId: trackId, synced: true, provider: "LRCLIB", lines: lines)
            }
        }
        if let plainText = json["plainLyrics"] as? String, !plainText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let rawLines = plainText
                .components(separatedBy: .newlines)
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
            let lines = rawLines.enumerated().map { idx, text in
                LyricLine(id: idx, startTimeSec: Double(idx) * 3.5, text: text)
            }
            if !lines.isEmpty {
                return LyricsPayload(trackId: trackId, synced: false, provider: "LRCLIB", lines: lines)
            }
        }
        return nil
    }

    private func parseLRC(_ text: String) -> [LyricLine] {
        guard let regex = try? NSRegularExpression(pattern: #"\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\](.*)"#) else {
            return []
        }
        var rawItems: [(Double, String)] = []
        for rawLine in text.components(separatedBy: .newlines) {
            let nsRange = NSRange(rawLine.startIndex..<rawLine.endIndex, in: rawLine)
            guard let match = regex.firstMatch(in: rawLine, range: nsRange),
                  let minR = Range(match.range(at: 1), in: rawLine),
                  let secR = Range(match.range(at: 2), in: rawLine),
                  let textR = Range(match.range(at: 4), in: rawLine) else {
                continue
            }
            let mins = Double(rawLine[minR]) ?? 0
            let secs = Double(rawLine[secR]) ?? 0
            var frac = 0.0
            if match.range(at: 3).location != NSNotFound, let fracR = Range(match.range(at: 3), in: rawLine) {
                let fracStr = String(rawLine[fracR])
                let divisor = pow(10.0, Double(fracStr.count))
                frac = (Double(fracStr) ?? 0) / divisor
            }
            let content = rawLine[textR].trimmingCharacters(in: .whitespacesAndNewlines)
            if !content.isEmpty {
                rawItems.append((mins * 60.0 + secs + frac, content))
            }
        }
        let sorted = rawItems.sorted { $0.0 < $1.0 }
        return sorted.enumerated().map { idx, pair in
            LyricLine(id: idx, startTimeSec: pair.0, text: pair.1)
        }
    }

    private func sanitizeQueryTitle(_ title: String) -> String {
        var out = title
        let patterns = [
            #"\((?i:official[^)]*|lyric[^)]*|audio[^)]*|video[^)]*|visualizer[^)]*)\)"#,
            #"\[(?i:official[^\]]*|lyric[^\]]*|audio[^\]]*|video[^\]]*|hd|4k)\]"#
        ]
        for pattern in patterns {
            if let regex = try? NSRegularExpression(pattern: pattern) {
                let range = NSRange(out.startIndex..<out.endIndex, in: out)
                out = regex.stringByReplacingMatches(in: out, range: range, withTemplate: "")
            }
        }
        return out.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func cacheDirectory() -> URL? {
        guard let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            return nil
        }
        let dir = caches.appendingPathComponent("DerbisLyrics", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private func fileURL(for trackId: String) -> URL? {
        let safe = trackId.replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
        return cacheDirectory()?.appendingPathComponent("\(safe).json")
    }

    private func saveToDisk(_ payload: LyricsPayload) {
        guard let url = fileURL(for: payload.trackId),
              let data = try? JSONEncoder().encode(payload) else {
            return
        }
        try? data.write(to: url, options: .atomic)
    }

    private func loadFromDisk(trackId: String) -> LyricsPayload? {
        guard let url = fileURL(for: trackId),
              let data = try? Data(contentsOf: url) else {
            return nil
        }
        return try? JSONDecoder().decode(LyricsPayload.self, from: data)
    }
}
