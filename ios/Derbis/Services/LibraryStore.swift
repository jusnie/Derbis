import Foundation
import AVFoundation
import UIKit

@MainActor
final class LibraryStore: ObservableObject {
    static let shared = LibraryStore()

    @Published var likedTracks: [Track] = []
    @Published var playlists: [UserPlaylist] = []
    @Published var offlineTracks: [Track] = []
    @Published var localTracks: [Track] = []
    @Published var history: [ListeningRecord] = []
    @Published var blockedArtists: [String] = []
    @Published var activeDownloads: [String: Double] = [:]
    @Published var cachedHomeShelves: [HomeShelf] = []

    private struct PersistedState: Codable {
        var likedTracks: [Track]
        var playlists: [UserPlaylist]
        var offlineTracks: [Track]
        var localTracks: [Track]
        var history: [ListeningRecord]
        var blockedArtists: [String]?
    }

    init() {
        createDirectoriesIfNeeded()
        loadState()
        loadCachedHomeShelves()
    }

    func isLiked(_ track: Track) -> Bool {
        likedTracks.contains { $0.id == track.id }
    }

    func toggleLike(_ track: Track) {
        let nowLiked: Bool
        if let idx = likedTracks.firstIndex(where: { $0.id == track.id }) {
            likedTracks.remove(at: idx)
            nowLiked = false
        } else {
            likedTracks.insert(track, at: 0)
            nowLiked = true
        }
        saveState()
        GoogleAccountSyncService.shared.pushLikeStateIfEnabled(track: track, isLiked: nowLiked)
    }

    func mergeCloudLikedTracks(_ cloudTracks: [Track]) -> Int {
        guard !cloudTracks.isEmpty else { return 0 }
        var existingIds = Set(likedTracks.map(\.id))
        var added = 0
        for track in cloudTracks where existingIds.insert(track.id).inserted {
            likedTracks.append(track)
            added += 1
        }
        if added > 0 {
            saveState()
        }
        return added
    }

    func upsertCloudPlaylist(cloudId: String, title: String, tracks: [Track]) -> Int {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty, !tracks.isEmpty else { return 0 }

        var seen = Set<String>()
        let uniqueTracks = tracks.filter { seen.insert($0.id).inserted }
        guard !uniqueTracks.isEmpty else { return 0 }

        let safeKey = cloudId.replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
        let deterministicId = "ytm-\(safeKey)"

        if let idx = playlists.firstIndex(where: { $0.id == deterministicId }) {
            let known = Set(playlists[idx].tracks.map(\.id))
            var added = 0
            for t in uniqueTracks where !known.contains(t.id) {
                playlists[idx].tracks.append(t)
                added += 1
            }
            playlists[idx].name = trimmedTitle
            saveState()
            return added
        } else {
            let created = UserPlaylist(id: deterministicId, name: trimmedTitle, tracks: uniqueTracks)
            playlists.insert(created, at: 0)
            saveState()
            return uniqueTracks.count
        }
    }

    func mergeCloudHistoryTracks(_ cloudTracks: [Track]) -> Int {
        guard !cloudTracks.isEmpty else { return 0 }
        var existingIds = Set(history.map { $0.track.id })
        var added = 0
        let now = Date()
        for (idx, track) in cloudTracks.prefix(60).enumerated() {
            guard existingIds.insert(track.id).inserted else { continue }
            let stamped = now.addingTimeInterval(-Double(idx * 180))
            history.append(
                ListeningRecord(
                    track: track,
                    playCount: 1,
                    listenedSeconds: max(60, track.durationSec > 0 ? track.durationSec : 180),
                    lastPlayedAt: stamped
                )
            )
            added += 1
        }
        if history.count > 250 {
            history = Array(history.prefix(250))
        }
        if added > 0 {
            saveState()
        }
        return added
    }

    func normalizedArtistTokens(from rawArtist: String) -> [String] {
        let cleaned = rawArtist
            .replacingOccurrences(of: " feat. ", with: ",", options: .caseInsensitive)
            .replacingOccurrences(of: " ft. ", with: ",", options: .caseInsensitive)
            .replacingOccurrences(of: " & ", with: ",")
        let parts = cleaned
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? [rawArtist.trimmingCharacters(in: .whitespacesAndNewlines)] : parts
    }

    func isArtistBlocked(_ artistName: String) -> Bool {
        guard !blockedArtists.isEmpty else { return false }
        let blockedLower = Set(blockedArtists.map { $0.lowercased() })
        let fullLower = artistName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if blockedLower.contains(fullLower) { return true }
        for token in normalizedArtistTokens(from: artistName) {
            if blockedLower.contains(token.lowercased()) {
                return true
            }
        }
        return false
    }

    func toggleBlockedArtist(_ artistName: String) {
        let primary = normalizedArtistTokens(from: artistName).first ?? artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !primary.isEmpty else { return }
        if let idx = blockedArtists.firstIndex(where: { $0.caseInsensitiveCompare(primary) == .orderedSame }) {
            blockedArtists.remove(at: idx)
        } else {
            blockedArtists.insert(primary, at: 0)
        }
        saveState()
    }

    func unblockArtist(_ artistName: String) {
        blockedArtists.removeAll { $0.caseInsensitiveCompare(artistName) == .orderedSame }
        saveState()
    }

    var topArtistsStats: [(artist: String, plays: Int, minutes: Int)] {
        var bucket: [String: (display: String, plays: Int, seconds: Int)] = [:]
        for record in history {
            let name = normalizedArtistTokens(from: record.track.artist).first ?? record.track.artist
            let key = name.lowercased()
            guard !key.isEmpty else { continue }
            var current = bucket[key] ?? (display: name, plays: 0, seconds: 0)
            current.plays += max(1, record.playCount)
            current.seconds += record.listenedSeconds
            bucket[key] = current
        }
        return bucket.values
            .sorted { a, b in
                if a.plays != b.plays { return a.plays > b.plays }
                return a.seconds > b.seconds
            }
            .prefix(8)
            .map { (artist: $0.display, plays: $0.plays, minutes: max(1, $0.seconds / 60)) }
    }

    var topTracksStats: [(track: Track, plays: Int)] {
        var counts: [String: (track: Track, plays: Int)] = [:]
        for record in history {
            var current = counts[record.track.id] ?? (track: record.track, plays: 0)
            current.plays += max(1, record.playCount)
            counts[record.track.id] = current
        }
        return counts.values
            .sorted { $0.plays > $1.plays }
            .prefix(8)
            .map { ($0.track, $0.plays) }
    }

    func isDownloaded(_ track: Track) -> Bool {
        guard let item = offlineTracks.first(where: { $0.id == track.id }),
              let fileName = item.localFileName else {
            return false
        }
        let url = downloadsDirectory().appendingPathComponent(fileName)
        return FileManager.default.fileExists(atPath: url.path)
    }

    func localPlaybackURL(for track: Track) -> URL? {
        if track.source == .localFile, let fileName = track.localFileName {
            let url = localAudioDirectory().appendingPathComponent(fileName)
            if FileManager.default.fileExists(atPath: url.path) {
                return url
            }
        }
        if let offline = offlineTracks.first(where: { $0.id == track.id }),
           let fileName = offline.localFileName {
            let url = downloadsDirectory().appendingPathComponent(fileName)
            if FileManager.default.fileExists(atPath: url.path) {
                return url
            }
        }
        return nil
    }

    func createPlaylist(name: String, initialTrack: Track? = nil) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        var tracks: [Track] = []
        if let initialTrack = initialTrack {
            tracks.append(initialTrack)
        }
        let playlist = UserPlaylist(name: trimmed, tracks: tracks)
        playlists.insert(playlist, at: 0)
        saveState()
    }

    func deletePlaylist(id: String) {
        playlists.removeAll { $0.id == id }
        saveState()
    }

    func addTrack(_ track: Track, toPlaylistId playlistId: String) {
        guard let idx = playlists.firstIndex(where: { $0.id == playlistId }) else { return }
        if !playlists[idx].tracks.contains(where: { $0.id == track.id }) {
            playlists[idx].tracks.append(track)
            saveState()
        }
    }

    func removeTrack(trackId: String, fromPlaylistId playlistId: String) {
        guard let idx = playlists.firstIndex(where: { $0.id == playlistId }) else { return }
        playlists[idx].tracks.removeAll { $0.id == trackId }
        saveState()
    }

    func recordPlayback(track: Track, listenedSeconds: Int = 30) {
        if let idx = history.firstIndex(where: { $0.track.id == track.id }) {
            var existing = history.remove(at: idx)
            existing.playCount += 1
            existing.listenedSeconds += max(5, listenedSeconds)
            existing.lastPlayedAt = Date()
            existing.track = track
            history.insert(existing, at: 0)
        } else {
            let entry = ListeningRecord(
                track: track,
                playCount: 1,
                listenedSeconds: max(15, listenedSeconds),
                lastPlayedAt: Date()
            )
            history.insert(entry, at: 0)
        }
        if history.count > 200 {
            history = Array(history.prefix(200))
        }
        saveState()
    }

    var totalMinutesListened: Int {
        let secs = history.reduce(0) { $0 + $1.listenedSeconds }
        return max(0, secs / 60)
    }

    var totalPlaysCount: Int {
        history.reduce(0) { $0 + $1.playCount }
    }

    var orbitTracks: [Track] {
        var seen = Set<String>()
        var list: [Track] = []
        for item in history.sorted(by: { $0.playCount > $1.playCount }) {
            if seen.insert(item.track.id).inserted {
                list.append(item.track)
            }
            if list.count >= 9 { return list }
        }
        for liked in likedTracks {
            if seen.insert(liked.id).inserted {
                list.append(liked)
            }
            if list.count >= 9 { return list }
        }
        for shelf in cachedHomeShelves {
            for t in shelf.tracks {
                if seen.insert(t.id).inserted {
                    list.append(t)
                }
                if list.count >= 9 { return list }
            }
        }
        return list
    }

    func downloadTrackForOffline(_ track: Track, preferHighQuality: Bool) async {
        guard !isDownloaded(track), activeDownloads[track.id] == nil else { return }
        activeDownloads[track.id] = 0.08

        do {
            let candidates: [ResolvedStream]
            if track.source == .subsonic,
               let songId = track.subsonicSongId,
               let url = SubsonicService.shared.signedStreamURL(songId: songId) {
                candidates = [
                    ResolvedStream(
                        url: url,
                        headers: [:],
                        codecBadge: track.codecBadge,
                        bitrateKbps: track.bitrateKbps,
                        isDirectFile: true,
                        clientLabel: "SUBSONIC"
                    )
                ]
            } else if track.source == .youtubeMusic {
                candidates = try await InnerTubeService.shared.resolveStreamCandidates(
                    for: track,
                    preferHighQuality: preferHighQuality,
                    requireDirectFile: true
                )
            } else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            let directCandidates = candidates.filter(\.isDirectFile)
            guard !directCandidates.isEmpty else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            activeDownloads[track.id] = 0.35

            var verifiedDownload: (tempURL: URL, response: HTTPURLResponse, stream: ResolvedStream)?
            for candidate in directCandidates {
                var req = URLRequest(url: candidate.url)
                for (k, v) in candidate.headers {
                    req.setValue(v, forHTTPHeaderField: k)
                }

                guard let (tempURL, response) = try? await URLSession.shared.download(for: req),
                      let http = response as? HTTPURLResponse,
                      (200..<300).contains(http.statusCode) else {
                    await InnerTubeService.shared.quarantineStream(
                        url: candidate.url,
                        videoId: track.videoId,
                        clientLabel: candidate.clientLabel
                    )
                    continue
                }

                let ctype = (http.value(forHTTPHeaderField: "Content-Type") ?? "").lowercased()
                let downloadedSize = (try? tempURL.resourceValues(forKeys: [.fileSizeKey]))?.fileSize ?? 0
                if ctype.contains("text/html") || ctype.contains("application/json") || downloadedSize < 32_768 {
                    try? FileManager.default.removeItem(at: tempURL)
                    await InnerTubeService.shared.quarantineStream(
                        url: candidate.url,
                        videoId: track.videoId,
                        clientLabel: candidate.clientLabel
                    )
                    continue
                }

                verifiedDownload = (tempURL, http, candidate)
                break
            }

            guard let winner = verifiedDownload else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            activeDownloads[track.id] = 0.85

            let ext: String = {
                let mime = (winner.response.value(forHTTPHeaderField: "Content-Type") ?? "").lowercased()
                if mime.contains("flac") { return "flac" }
                if mime.contains("mpeg") || mime.contains("mp3") { return "mp3" }
                if mime.contains("wav") { return "wav" }
                return "m4a"
            }()

            let safeId = track.id.replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
            let fileName = "\(safeId).\(ext)"
            let destination = downloadsDirectory().appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: destination)
            try FileManager.default.moveItem(at: winner.tempURL, to: destination)

            var savedTrack = track
            savedTrack.localFileName = fileName
            savedTrack.codecBadge = "Offline \(ext.uppercased())"
            savedTrack.bitrateKbps = winner.stream.bitrateKbps

            _ = await LyricsService.shared.fetchLyrics(for: track)

            offlineTracks.removeAll { $0.id == track.id }
            offlineTracks.insert(savedTrack, at: 0)
            activeDownloads.removeValue(forKey: track.id)
            saveState()
        } catch {
            activeDownloads.removeValue(forKey: track.id)
        }
    }

    func removeOfflineDownload(_ track: Track) {
        if let existing = offlineTracks.first(where: { $0.id == track.id }),
           let fileName = existing.localFileName {
            let url = downloadsDirectory().appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: url)
        }
        offlineTracks.removeAll { $0.id == track.id }
        saveState()
    }

    func importLocalAudioFiles(urls: [URL]) async {
        for sourceURL in urls {
            let scoped = sourceURL.startAccessingSecurityScopedResource()
            defer {
                if scoped { sourceURL.stopAccessingSecurityScopedResource() }
            }

            let ext = sourceURL.pathExtension.isEmpty ? "m4a" : sourceURL.pathExtension.lowercased()
            let uniqueName = "\(UUID().uuidString.prefix(8))_\(sourceURL.lastPathComponent)"
            let destURL = localAudioDirectory().appendingPathComponent(uniqueName)

            do {
                try? FileManager.default.removeItem(at: destURL)
                try FileManager.default.copyItem(at: sourceURL, to: destURL)

                let asset = AVURLAsset(url: destURL)
                let durationCM = try? await asset.load(.duration)
                let durationSec = Int(CMTimeGetSeconds(durationCM ?? .zero).isFinite ? CMTimeGetSeconds(durationCM ?? .zero) : 0)

                var title = sourceURL.deletingPathExtension().lastPathComponent
                var artist = "Local File"
                var album = ""
                var artworkPath = ""

                if let metadata = try? await asset.load(.commonMetadata) {
                    for item in metadata {
                        guard let key = item.commonKey else { continue }
                        if key == .commonKeyTitle, let val = try? await item.load(.stringValue), !val.isEmpty {
                            title = val
                        } else if key == .commonKeyArtist, let val = try? await item.load(.stringValue), !val.isEmpty {
                            artist = val
                        } else if key == .commonKeyAlbumName, let val = try? await item.load(.stringValue), !val.isEmpty {
                            album = val
                        } else if key == .commonKeyArtwork, let data = try? await item.load(.dataValue), !data.isEmpty {
                            let artFile = localArtworkDirectory().appendingPathComponent("\(uniqueName).jpg")
                            try? data.write(to: artFile)
                            artworkPath = artFile.absoluteString
                        }
                    }
                }

                let badge: String = {
                    switch ext {
                    case "flac": return "FLAC Lossless"
                    case "alac": return "ALAC Lossless"
                    case "wav", "aiff": return "\(ext.uppercased()) PCM"
                    case "mp3": return "MP3 Local"
                    default: return "\(ext.uppercased()) Local"
                    }
                }()

                let track = Track(
                    id: "local:\(uniqueName)",
                    title: title,
                    artist: artist,
                    album: album,
                    durationSec: max(0, durationSec),
                    thumbnailURL: artworkPath,
                    largeThumbnailURL: artworkPath,
                    source: .localFile,
                    codecBadge: badge,
                    bitrateKbps: ext == "flac" || ext == "wav" ? 1411 : 320,
                    localFileName: uniqueName
                )
                localTracks.insert(track, at: 0)
            } catch {
                continue
            }
        }
        saveState()
    }

    func removeLocalTrack(_ track: Track) {
        if let fileName = track.localFileName {
            let url = localAudioDirectory().appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: url)
        }
        localTracks.removeAll { $0.id == track.id }
        saveState()
    }

    func offlineStorageSizeString() -> String {
        let dirs = [downloadsDirectory(), localAudioDirectory()]
        var bytes: Int64 = 0
        for dir in dirs {
            if let files = try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.fileSizeKey]) {
                for f in files {
                    if let sz = (try? f.resourceValues(forKeys: [.fileSizeKey]))?.fileSize {
                        bytes += Int64(sz)
                    }
                }
            }
        }
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useMB, .useGB, .useKB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }

    func saveCachedHomeShelves(_ shelves: [HomeShelf]) {
        cachedHomeShelves = shelves
        let url = appSupportDirectory().appendingPathComponent("home_cache.json")
        if let data = try? JSONEncoder().encode(shelves) {
            try? data.write(to: url, options: .atomic)
        }
    }

    func exportBackupURL() -> URL? {
        let doc = DerbisBackupDocument(
            schemaVersion: 1,
            exportedAt: Date(),
            likedTracks: likedTracks,
            playlists: playlists,
            history: history
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(doc) else { return nil }
        let temp = FileManager.default.temporaryDirectory.appendingPathComponent("derbis-ios-backup.json")
        try? data.write(to: temp, options: .atomic)
        return temp
    }

    func importBackup(from url: URL) -> Bool {
        let scoped = url.startAccessingSecurityScopedResource()
        defer {
            if scoped { url.stopAccessingSecurityScopedResource() }
        }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        guard let data = try? Data(contentsOf: url),
              let doc = try? decoder.decode(DerbisBackupDocument.self, from: data) else {
            return false
        }
        for track in doc.likedTracks where !isLiked(track) {
            likedTracks.append(track)
        }
        for pl in doc.playlists where !playlists.contains(where: { $0.id == pl.id }) {
            playlists.append(pl)
        }
        for rec in doc.history where !history.contains(where: { $0.track.id == rec.track.id }) {
            history.append(rec)
        }
        saveState()
        return true
    }

    private func loadCachedHomeShelves() {
        let url = appSupportDirectory().appendingPathComponent("home_cache.json")
        if let data = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode([HomeShelf].self, from: data) {
            cachedHomeShelves = decoded
        }
    }

    private func saveState() {
        let state = PersistedState(
            likedTracks: likedTracks,
            playlists: playlists,
            offlineTracks: offlineTracks,
            localTracks: localTracks,
            history: history,
            blockedArtists: blockedArtists
        )
        let url = appSupportDirectory().appendingPathComponent("library.json")
        if let data = try? JSONEncoder().encode(state) {
            try? data.write(to: url, options: .atomic)
        }
    }

    private func loadState() {
        let url = appSupportDirectory().appendingPathComponent("library.json")
        guard let data = try? Data(contentsOf: url),
              let state = try? JSONDecoder().decode(PersistedState.self, from: data) else {
            return
        }
        likedTracks = state.likedTracks
        playlists = state.playlists
        offlineTracks = state.offlineTracks
        localTracks = state.localTracks
        history = state.history
        blockedArtists = state.blockedArtists ?? []
    }

    private func createDirectoriesIfNeeded() {
        for dir in [appSupportDirectory(), downloadsDirectory(), localAudioDirectory(), localArtworkDirectory()] {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
    }

    private func appSupportDirectory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        return base.appendingPathComponent("Derbis", isDirectory: true)
    }

    private func downloadsDirectory() -> URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("Downloads", isDirectory: true)
    }

    private func localAudioDirectory() -> URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("LocalAudio", isDirectory: true)
    }

    private func localArtworkDirectory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        return base.appendingPathComponent("Derbis/LocalArtwork", isDirectory: true)
    }
}
