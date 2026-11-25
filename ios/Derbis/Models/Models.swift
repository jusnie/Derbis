import Foundation
import SwiftUI

enum TrackSource: String, Codable, Hashable, CaseIterable, Sendable {
    case youtubeMusic = "YouTube Music"
    case subsonic = "Server"
    case localFile = "Local"
}

struct Track: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var title: String
    var artist: String
    var album: String
    var durationSec: Int
    var thumbnailURL: String
    var largeThumbnailURL: String
    var source: TrackSource
    var directStreamURL: String?
    var codecBadge: String
    var bitrateKbps: Int
    var localFileName: String?
    var subsonicSongId: String?

    init(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        durationSec: Int = 0,
        thumbnailURL: String = "",
        largeThumbnailURL: String = "",
        source: TrackSource = .youtubeMusic,
        directStreamURL: String? = nil,
        codecBadge: String = "AAC",
        bitrateKbps: Int = 256,
        localFileName: String? = nil,
        subsonicSongId: String? = nil
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.durationSec = max(0, durationSec)
        self.thumbnailURL = thumbnailURL
        self.largeThumbnailURL = largeThumbnailURL.isEmpty ? thumbnailURL : largeThumbnailURL
        self.source = source
        self.directStreamURL = directStreamURL
        self.codecBadge = codecBadge
        self.bitrateKbps = bitrateKbps
        self.localFileName = localFileName
        self.subsonicSongId = subsonicSongId
    }

    var videoId: String? {
        guard source == .youtubeMusic else { return nil }
        return id.hasPrefix("yt:") ? String(id.dropFirst(3)) : id
    }

    var formattedDuration: String {
        TimeFormatting.clockString(seconds: Double(durationSec))
    }

    var displayArtworkURL: URL? {
        let candidate = largeThumbnailURL.isEmpty ? thumbnailURL : largeThumbnailURL
        return URL(string: candidate)
    }
}

struct HomeShelf: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var title: String
    var subtitle: String
    var tracks: [Track]
}

struct UserPlaylist: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var name: String
    var createdAt: Date
    var tracks: [Track]

    init(id: String = UUID().uuidString, name: String, createdAt: Date = Date(), tracks: [Track] = []) {
        self.id = id
        self.name = name
        self.createdAt = createdAt
        self.tracks = tracks
    }
}

struct LyricLine: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let startTimeSec: Double
    let text: String
}

struct LyricsPayload: Codable, Hashable, Sendable {
    let trackId: String
    let synced: Bool
    let provider: String
    let lines: [LyricLine]
}

enum RepeatMode: String, Codable, CaseIterable {
    case off = "Off"
    case all = "All"
    case one = "One"

    var systemImage: String {
        switch self {
        case .off, .all: return "repeat"
        case .one: return "repeat.1"
        }
    }

    func next() -> RepeatMode {
        switch self {
        case .off: return .all
        case .all: return .one
        case .one: return .off
        }
    }
}

enum SleepTimerOption: Int, CaseIterable, Identifiable {
    case off = 0
    case fifteen = 15
    case thirty = 30
    case fortyFive = 45
    case sixty = 60

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .off: return "Off"
        case .fifteen: return "15 minutes"
        case .thirty: return "30 minutes"
        case .fortyFive: return "45 minutes"
        case .sixty: return "60 minutes"
        }
    }
}

enum DerbisSkin: String, Codable, CaseIterable, Identifiable {
    case appleMusic13 = "apple_music_13"
    case pixelUI = "pixel_ui"
    case derbisDefault = "derbis_default"
    case lateNight = "late_night"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .appleMusic13: return "Apple Music 13"
        case .pixelUI: return "Pixel UI"
        case .derbisDefault: return "Derbis Default"
        case .lateNight: return "Late Night"
        }
    }

    var caption: String {
        switch self {
        case .appleMusic13: return "Ambient cover glow, inline lyrics, SF Pro"
        case .pixelUI: return "Blush graph paper, hard shadows, Pixelify"
        case .derbisDefault: return "AMOLED dark surface with crimson accents"
        case .lateNight: return "Pure OLED black with warm low-glare amber"
        }
    }

    var isLight: Bool {
        self == .pixelUI
    }
}

enum DerbisFontChoice: String, Codable, CaseIterable, Identifiable {
    case skinDefault = "skin"
    case sfPro = "sf_pro"
    case pixelify = "pixelify"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .skinDefault: return "Match skin"
        case .sfPro: return "SF Pro"
        case .pixelify: return "Pixelify Sans"
        }
    }
}

struct SubsonicConfig: Codable, Equatable {
    var serverURL: String
    var username: String
    var preferRawLossless: Bool

    static let empty = SubsonicConfig(serverURL: "", username: "", preferRawLossless: true)
}

struct SubsonicPlaylistSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let songCount: Int
    let durationSec: Int
}

struct ListeningRecord: Codable, Hashable {
    var track: Track
    var playCount: Int
    var listenedSeconds: Int
    var lastPlayedAt: Date
}

struct DerbisBackupDocument: Codable {
    let schemaVersion: Int
    let exportedAt: Date
    let likedTracks: [Track]
    let playlists: [UserPlaylist]
    let history: [ListeningRecord]
}

struct ResolvedStream: Hashable, Sendable {
    let url: URL
    let headers: [String: String]
    let codecBadge: String
    let bitrateKbps: Int
    let isDirectFile: Bool
    var clientLabel: String = "Direct"
    var expiresAt: Date = Date().addingTimeInterval(5400)

    var isFresh: Bool {
        Date() < expiresAt
    }
}

enum TimeFormatting {
    static func clockString(seconds: Double) -> String {
        guard seconds.isFinite && seconds > 0 else { return "0:00" }
        let total = Int(seconds.rounded(.down))
        let hrs = total / 3600
        let mins = (total % 3600) / 60
        let secs = total % 60
        if hrs > 0 {
            return String(format: "%d:%02d:%02d", hrs, mins, secs)
        }
        return String(format: "%d:%02d", mins, secs)
    }
}
