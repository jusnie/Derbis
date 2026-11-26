import Foundation
import SwiftUI

struct PlayLogEvent: Codable, Hashable, Sendable {
    let trackId: String
    let title: String
    let artist: String
    let album: String
    let thumbnailURL: String
    let playedMs: Int64
    let endedAtEpochSec: Int64

    var dedupeKey: String {
        "\(endedAtEpochSec)|\(title.lowercased())|\(artist.lowercased())"
    }
}

enum StatsPeriod: String, CaseIterable, Identifiable, Sendable {
    case week = "Past Week"
    case month = "Past Month"
    case year = "Past Year"
    case allTime = "All Time"

    var id: String { rawValue }

    var lookbackSeconds: Int64? {
        switch self {
        case .week: return 7 * 86_400
        case .month: return 30 * 86_400
        case .year: return 365 * 86_400
        case .allTime: return nil
        }
    }
}

struct RankedStatItem: Identifiable, Hashable, Sendable {
    let id: String
    let rank: Int
    let title: String
    let subtitle: String
    let thumbnailURL: String
    let plays: Int
    let playedMs: Int64
}

struct PeriodStatsSummary: Sendable {
    let period: StatsPeriod
    let totalPlayedMs: Int64
    let totalPlays: Int
    let topTracks: [RankedStatItem]
    let topArtists: [RankedStatItem]
    let topAlbums: [RankedStatItem]

    var formattedDuration: String {
        ListeningStatsStore.formatDuration(ms: totalPlayedMs)
    }
}

@MainActor
final class ListeningStatsStore: ObservableObject {
    static let shared = ListeningStatsStore()

    @Published var recordPlaysEnabled: Bool {
        didSet { UserDefaults.standard.set(recordPlaysEnabled, forKey: "derbis.stats.recordPlays") }
    }

    @Published private(set) var events: [PlayLogEvent] = [] {
        didSet {
            summaryCache.removeAll()
        }
    }
    @Published private(set) var isImporting: Bool = false

    private static let maxStoredEvents = 50_000
    private var summaryCache: [String: (timestamp: Date, summary: PeriodStatsSummary)] = [:]
    private var saveWorkTask: Task<Void, Never>?
    private let persistenceQueue = DispatchQueue(label: "com.derbis.stats.persistence", qos: .utility)

    private init() {
        if UserDefaults.standard.object(forKey: "derbis.stats.recordPlays") == nil {
            self.recordPlaysEnabled = true
        } else {
            self.recordPlaysEnabled = UserDefaults.standard.bool(forKey: "derbis.stats.recordPlays")
        }
        loadFromDisk()
    }

    func recordPlayIfQualified(track: Track, listenedSeconds: Double) {
        guard recordPlaysEnabled else { return }
        let threshold: Double = (track.durationSec > 0 && track.durationSec < 60)
            ? max(10.0, Double(track.durationSec) * 0.5)
            : 30.0
        guard listenedSeconds >= threshold else { return }

        let nowSec = Int64(Date().timeIntervalSince1970)
        if let last = events.last,
           last.trackId == track.id,
           abs(nowSec - last.endedAtEpochSec) < 12 {
            return
        }

        let thumb = track.largeThumbnailURL.isEmpty ? track.thumbnailURL : track.largeThumbnailURL
        let entry = PlayLogEvent(
            trackId: track.id,
            title: track.title,
            artist: track.artist,
            album: track.album,
            thumbnailURL: thumb,
            playedMs: Int64(listenedSeconds * 1000.0),
            endedAtEpochSec: nowSec
        )
        events.append(entry)
        if events.count > Self.maxStoredEvents {
            events.removeFirst(events.count - Self.maxStoredEvents)
        }
        saveToDisk()
    }

    func durationSummaryString(for period: StatsPeriod) -> String {
        let cutoff = cutoffEpoch(for: period)
        var totalMs: Int64 = 0
        for ev in events where ev.endedAtEpochSec >= cutoff {
            totalMs += ev.playedMs
        }
        return Self.formatDuration(ms: totalMs)
    }

    func summary(for period: StatsPeriod, limit: Int = 25) -> PeriodStatsSummary {
        let cacheKey = "\(period.rawValue)|\(limit)"
        if let cached = summaryCache[cacheKey], Date().timeIntervalSince(cached.timestamp) < 60 {
            return cached.summary
        }

        let cutoff = cutoffEpoch(for: period)
        let filtered = events.filter { $0.endedAtEpochSec >= cutoff }

        var totalMs: Int64 = 0
        struct Bucket {
            var title: String
            var subtitle: String
            var thumb: String
            var plays: Int = 0
            var ms: Int64 = 0
        }

        var trackMap: [String: Bucket] = [:]
        var artistMap: [String: Bucket] = [:]
        var albumMap: [String: Bucket] = [:]

        for ev in filtered {
            totalMs += ev.playedMs

            let tKey = "\(ev.title.lowercased())::\(ev.artist.lowercased())"
            var tb = trackMap[tKey] ?? Bucket(title: ev.title, subtitle: ev.artist, thumb: ev.thumbnailURL)
            tb.plays += 1
            tb.ms += ev.playedMs
            if tb.thumb.isEmpty && !ev.thumbnailURL.isEmpty { tb.thumb = ev.thumbnailURL }
            trackMap[tKey] = tb

            let cleanArtist = ev.artist.trimmingCharacters(in: .whitespacesAndNewlines)
            if !cleanArtist.isEmpty {
                let aKey = cleanArtist.lowercased()
                var ab = artistMap[aKey] ?? Bucket(title: cleanArtist, subtitle: "", thumb: ev.thumbnailURL)
                ab.plays += 1
                ab.ms += ev.playedMs
                if ab.thumb.isEmpty && !ev.thumbnailURL.isEmpty { ab.thumb = ev.thumbnailURL }
                artistMap[aKey] = ab
            }

            let cleanAlbum = ev.album.trimmingCharacters(in: .whitespacesAndNewlines)
            if !cleanAlbum.isEmpty {
                let alKey = "\(cleanAlbum.lowercased())::\(cleanArtist.lowercased())"
                var alb = albumMap[alKey] ?? Bucket(title: cleanAlbum, subtitle: cleanArtist, thumb: ev.thumbnailURL)
                alb.plays += 1
                alb.ms += ev.playedMs
                if alb.thumb.isEmpty && !ev.thumbnailURL.isEmpty { alb.thumb = ev.thumbnailURL }
                albumMap[alKey] = alb
            }
        }

        func rank(_ dict: [String: Bucket]) -> [RankedStatItem] {
            dict.sorted {
                if $0.value.plays != $1.value.plays { return $0.value.plays > $1.value.plays }
                return $0.value.ms > $1.value.ms
            }
            .prefix(limit)
            .enumerated()
            .map { idx, pair in
                let sub = pair.value.subtitle.isEmpty
                    ? Self.formatDuration(ms: pair.value.ms)
                    : "\(pair.value.subtitle) · \(Self.formatDuration(ms: pair.value.ms))"
                return RankedStatItem(
                    id: pair.key,
                    rank: idx + 1,
                    title: pair.value.title,
                    subtitle: sub,
                    thumbnailURL: pair.value.thumb,
                    plays: pair.value.plays,
                    playedMs: pair.value.ms
                )
            }
        }

        let computed = PeriodStatsSummary(
            period: period,
            totalPlayedMs: totalMs,
            totalPlays: filtered.count,
            topTracks: rank(trackMap),
            topArtists: rank(artistMap),
            topAlbums: rank(albumMap)
        )
        summaryCache[cacheKey] = (Date(), computed)
        return computed
    }

    func importSpotifyExportFiles(urls: [URL]) async -> (added: Int, skipped: Int) {
        guard !isImporting else { return (0, 0) }
        isImporting = true
        defer { isImporting = false }

        let parsedEvents: [PlayLogEvent] = await Task.detached(priority: .userInitiated) {
            var collected: [PlayLogEvent] = []
            for url in urls {
                let accessed = url.startAccessingSecurityScopedResource()
                defer { if accessed { url.stopAccessingSecurityScopedResource() } }
                guard let data = try? Data(contentsOf: url) else { continue }
                collected.append(contentsOf: ListeningStatsStore.parseSpotifyHistoryJSON(data))
            }
            return collected
        }.value

        guard !parsedEvents.isEmpty else { return (0, 0) }

        var existingKeys = Set(events.map(\.dedupeKey))
        var added = 0
        var skipped = 0

        for item in parsedEvents {
            if existingKeys.insert(item.dedupeKey).inserted {
                events.append(item)
                added += 1
            } else {
                skipped += 1
            }
        }

        events.sort { $0.endedAtEpochSec < $1.endedAtEpochSec }
        if events.count > Self.maxStoredEvents {
            events.removeFirst(events.count - Self.maxStoredEvents)
        }
        saveToDisk()
        return (added, skipped)
    }

    func eraseHistory() {
        events.removeAll()
        saveToDisk()
    }

    private func cutoffEpoch(for period: StatsPeriod) -> Int64 {
        guard let lookback = period.lookbackSeconds else { return 0 }
        return Int64(Date().timeIntervalSince1970) - lookback
    }

    nonisolated static func formatDuration(ms: Int64) -> String {
        let totalMinutes = max(0, ms / 60_000)
        if totalMinutes < 60 {
            return "\(totalMinutes) min"
        }
        let hours = totalMinutes / 60
        let remMin = totalMinutes % 60
        return remMin > 0 ? "\(hours) h \(remMin) min" : "\(hours) h"
    }

    nonisolated private static func parseSpotifyHistoryJSON(_ data: Data) -> [PlayLogEvent] {
        guard let array = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]] else {
            return []
        }

        let isoFormatter = ISO8601DateFormatter()
        let fallbackFormatter = DateFormatter()
        fallbackFormatter.locale = Locale(identifier: "en_US_POSIX")
        fallbackFormatter.timeZone = TimeZone(secondsFromGMT: 0)
        fallbackFormatter.dateFormat = "yyyy-MM-dd HH:mm"

        var out: [PlayLogEvent] = []
        out.reserveCapacity(array.count)

        for row in array {
            let ms = (row["msPlayed"] as? Int64)
                ?? (row["msPlayed"] as? Int).map(Int64.init)
                ?? (row["ms_played"] as? Int64)
                ?? (row["ms_played"] as? Int).map(Int64.init)
                ?? 0
            guard ms >= 30_000 else { continue }

            let title = ((row["trackName"] as? String) ?? (row["master_metadata_track_name"] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let artist = ((row["artistName"] as? String) ?? (row["master_metadata_album_artist_name"] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let album = ((row["albumName"] as? String) ?? (row["master_metadata_album_album_name"] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty, !artist.isEmpty else { continue }

            var epoch: Int64 = Int64(Date().timeIntervalSince1970)
            if let ts = row["ts"] as? String, let d = isoFormatter.date(from: ts) {
                epoch = Int64(d.timeIntervalSince1970)
            } else if let endStr = row["endTime"] as? String, let d = fallbackFormatter.date(from: endStr) {
                epoch = Int64(d.timeIntervalSince1970)
            }

            let uri = (row["spotify_track_uri"] as? String) ?? "import:\(title.lowercased()):\(artist.lowercased())"
            out.append(
                PlayLogEvent(
                    trackId: uri,
                    title: title,
                    artist: artist,
                    album: album,
                    thumbnailURL: "",
                    playedMs: ms,
                    endedAtEpochSec: epoch
                )
            )
        }
        return out
    }

    private func storageURL() -> URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        let folder = dir.appendingPathComponent("Derbis", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder.appendingPathComponent("listening_stats_log.json")
    }

    private func loadFromDisk() {
        if let url = storageURL(),
           let data = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode([PlayLogEvent].self, from: data) {
            self.events = decoded
            return
        }

        let seeded = LibraryStore.shared.history.map { rec in
            PlayLogEvent(
                trackId: rec.track.id,
                title: rec.track.title,
                artist: rec.track.artist,
                album: rec.track.album,
                thumbnailURL: rec.track.thumbnailURL,
                playedMs: Int64(max(30, rec.listenedSeconds)) * 1000,
                endedAtEpochSec: Int64(rec.lastPlayedAt.timeIntervalSince1970)
            )
        }
        self.events = seeded.sorted { $0.endedAtEpochSec < $1.endedAtEpochSec }
    }

    private func saveToDisk() {
        saveWorkTask?.cancel()
        let snapshot = events
        guard let url = storageURL() else { return }
        let queue = persistenceQueue
        saveWorkTask = Task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            guard !Task.isCancelled else { return }
            queue.async {
                if let data = try? JSONEncoder().encode(snapshot) {
                    try? data.write(to: url, options: .atomic)
                }
            }
        }
    }
}
