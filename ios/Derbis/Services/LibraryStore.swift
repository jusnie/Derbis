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
    @Published var activeDownloads: [String: Double] = [:]
    @Published var cachedHomeShelves: [HomeShelf] = []

    private struct PersistedState: Codable {
        var likedTracks: [Track]
        var playlists: [UserPlaylist]
        var offlineTracks: [Track]
        var localTracks: [Track]
        var history: [ListeningRecord]
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
        if let idx = likedTracks.firstIndex(where: { $0.id == track.id }) {
            likedTracks.remove(at: idx)
        } else {
            likedTracks.insert(track, at: 0)
        }
        saveState()
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
            let resolved: ResolvedStream
            if track.source == .subsonic,
               let songId = track.subsonicSongId,
               let url = SubsonicService.shared.signedStreamURL(songId: songId) {
                resolved = ResolvedStream(
                    url: url,
                    headers: [:],
                    codecBadge: track.codecBadge,
                    bitrateKbps: track.bitrateKbps,
                    isDirectFile: true
                )
            } else if let vid = track.videoId {
                resolved = try await InnerTubeService.shared.resolvePlayableStream(
                    videoId: vid,
                    preferHighQuality: preferHighQuality
                )
            } else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            guard resolved.isDirectFile else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            activeDownloads[track.id] = 0.35

            var req = URLRequest(url: resolved.url)
            for (k, v) in resolved.headers {
                req.setValue(v, forHTTPHeaderField: k)
            }

            let (tempURL, response) = try await URLSession.shared.download(for: req)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                activeDownloads.removeValue(forKey: track.id)
                return
            }

            activeDownloads[track.id] = 0.85

            let ext: String = {
                let mime = (http.value(forHTTPHeaderField: "Content-Type") ?? "").lowercased()
                if mime.contains("flac") { return "flac" }
                if mime.contains("mpeg") || mime.contains("mp3") { return "mp3" }
                if mime.contains("wav") { return "wav" }
                return "m4a"
            }()

            let safeId = track.id.replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
            let fileName = "\(safeId).\(ext)"
            let destination = downloadsDirectory().appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: destination)
            try FileManager.default.moveItem(at: tempURL, to: destination)

            var savedTrack = track
            savedTrack.localFileName = fileName
            savedTrack.codecBadge = "Offline \(ext.uppercased())"
            savedTrack.bitrateKbps = resolved.bitrateKbps

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
            history: history
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
