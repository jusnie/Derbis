import Foundation
import SwiftUI

enum TrackSource: String, Codable, Hashable, CaseIterable, Sendable {
    case youtubeMusic = "DERBIS"
    case subsonic = "Server"
    case localFile = "Local"

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let raw = (try? container.decode(String.self)) ?? "DERBIS"
        switch raw.lowercased() {
        case "derbis", "youtube music", "youtube":
            self = .youtubeMusic
        case "server", "subsonic":
            self = .subsonic
        case "local", "localfile":
            self = .localFile
        default:
            self = .youtubeMusic
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
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

struct LyricWord: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let text: String
    let startTimeSec: Double
    let endTimeSec: Double
    let joinedToPrevious: Bool
}

struct LyricLine: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let startTimeSec: Double
    var endTimeSec: Double
    let text: String
    var words: [LyricWord]
    var backingText: String?
    var pronunciation: String?
    var translation: String?
    var isSecondaryVoice: Bool

    init(
        id: Int,
        startTimeSec: Double,
        endTimeSec: Double? = nil,
        text: String,
        words: [LyricWord] = [],
        backingText: String? = nil,
        pronunciation: String? = nil,
        translation: String? = nil,
        isSecondaryVoice: Bool = false
    ) {
        self.id = id
        self.startTimeSec = startTimeSec
        self.endTimeSec = endTimeSec ?? (startTimeSec + 4.0)
        self.text = text
        self.words = words
        self.backingText = backingText
        self.pronunciation = pronunciation
        self.translation = translation
        self.isSecondaryVoice = isSecondaryVoice
    }

    enum CodingKeys: String, CodingKey {
        case id, startTimeSec, endTimeSec, text, words, backingText, pronunciation, translation, isSecondaryVoice
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let idVal = try c.decode(Int.self, forKey: .id)
        let startVal = try c.decode(Double.self, forKey: .startTimeSec)
        let textVal = try c.decode(String.self, forKey: .text)
        self.id = idVal
        self.startTimeSec = startVal
        self.endTimeSec = (try? c.decodeIfPresent(Double.self, forKey: .endTimeSec)) ?? (startVal + 4.0)
        self.text = textVal
        self.words = (try? c.decodeIfPresent([LyricWord].self, forKey: .words)) ?? []
        self.backingText = try? c.decodeIfPresent(String.self, forKey: .backingText)
        self.pronunciation = try? c.decodeIfPresent(String.self, forKey: .pronunciation)
        self.translation = try? c.decodeIfPresent(String.self, forKey: .translation)
        self.isSecondaryVoice = (try? c.decodeIfPresent(Bool.self, forKey: .isSecondaryVoice)) ?? false
    }
}

struct LyricsPayload: Codable, Hashable, Sendable {
    let trackId: String
    let synced: Bool
    var wordSynced: Bool
    let provider: String
    let lines: [LyricLine]
    var hasRomanization: Bool
    var hasTranslation: Bool

    init(
        trackId: String,
        synced: Bool,
        wordSynced: Bool = false,
        provider: String,
        lines: [LyricLine],
        hasRomanization: Bool = false,
        hasTranslation: Bool = false
    ) {
        self.trackId = trackId
        self.synced = synced
        self.wordSynced = wordSynced
        self.provider = provider
        self.lines = lines
        self.hasRomanization = hasRomanization
        self.hasTranslation = hasTranslation
    }

    enum CodingKeys: String, CodingKey {
        case trackId, synced, wordSynced, provider, lines, hasRomanization, hasTranslation
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.trackId = try c.decode(String.self, forKey: .trackId)
        self.synced = try c.decode(Bool.self, forKey: .synced)
        self.wordSynced = (try? c.decodeIfPresent(Bool.self, forKey: .wordSynced)) ?? false
        self.provider = try c.decode(String.self, forKey: .provider)
        let decodedLines = try c.decode([LyricLine].self, forKey: .lines)
        self.lines = decodedLines
        self.hasRomanization = (try? c.decodeIfPresent(Bool.self, forKey: .hasRomanization))
            ?? decodedLines.contains(where: { ($0.pronunciation ?? "").isEmpty == false })
        self.hasTranslation = (try? c.decodeIfPresent(Bool.self, forKey: .hasTranslation))
            ?? decodedLines.contains(where: { ($0.translation ?? "").isEmpty == false })
    }
}

enum RepeatMode: String, Codable, CaseIterable, Sendable {
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

enum SleepTimerOption: Int, CaseIterable, Identifiable, Sendable {
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

enum DerbisSkin: String, Codable, CaseIterable, Identifiable, Sendable {
    case liquidGlass = "liquid_glass"
    case appleMusic13 = "apple_music_13"
    case pixelUI = "pixel_ui"
    case derbisDefault = "derbis_default"
    case lateNight = "late_night"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .liquidGlass: return "Liquid Glass"
        case .appleMusic13: return "Apple Music 13"
        case .pixelUI: return "Pixel UI"
        case .derbisDefault: return "Derbis Default"
        case .lateNight: return "Late Night"
        }
    }

    var caption: String {
        switch self {
        case .liquidGlass: return "Taurus optical glass bars, AMOLED field, ambient aura"
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

enum DerbisFontChoice: String, Codable, CaseIterable, Identifiable, Sendable {
    case skinDefault = "skin"
    case sfPro = "sf_pro"
    case sfRounded = "sf_rounded"
    case pixelify = "pixelify"
    case customFile = "custom_file"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .skinDefault: return "Match skin"
        case .sfPro: return "SF Pro"
        case .sfRounded: return "SF Rounded"
        case .pixelify: return "Pixelify Sans"
        case .customFile: return "Imported Font (.ttf/.otf)"
        }
    }
}

enum MotionCanvasStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case immersive = "immersive"
    case framed = "framed"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .immersive: return "Full-bleed 9:16"
        case .framed: return "Artwork frame"
        }
    }
}

enum PlayerBackdropStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case animated = "Motion"
    case fill = "Fill"
    case fluid = "Fluid"
    case visualiser = "Visualiser"
    case ambient = "Colors"
    case still = "Still"

    var id: String { rawValue }

    var subtitle: String {
        switch self {
        case .animated: return "Motion Canvas with progressive Fill fallback"
        case .fill: return "Taurus full-bleed cover dissolving into sampled dark tint"
        case .fluid: return "Drifting multi-color aura sampled from cover art"
        case .visualiser: return "Audio-reactive spectrum waves & beat-pulsing glass aura"
        case .ambient: return "Soft blurred cover wash over dark glass"
        case .still: return "Clean AMOLED surface with zero backdrop motion"
        }
    }
}

enum LyricsSourceKind: String, Codable, CaseIterable, Identifiable, Sendable {
    case appleTTML = "apple_ttml"
    case netEaseYRC = "netease_yrc"
    case lrclib = "lrclib"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .appleTTML: return "Apple Music TTML (Unison & BiniLyrics)"
        case .netEaseYRC: return "NetEase YRC (Word-Synced + Romaji/Trans)"
        case .lrclib: return "LRCLIB (Open Synced & Plain)"
        }
    }

    var shortBadge: String {
        switch self {
        case .appleTTML: return "Apple TTML"
        case .netEaseYRC: return "NetEase YRC"
        case .lrclib: return "LRCLIB"
        }
    }
}

enum GeniusMeaningFilter: String, Codable, CaseIterable, Identifiable, Sendable {
    case off = "Off"
    case artistOnly = "From the Artist"
    case artistAndEditors = "Artist & Editors"
    case everyone = "Everyone"

    var id: String { rawValue }
}

enum LyricsTranslationLanguage: String, Codable, CaseIterable, Identifiable, Sendable {
    case english = "en"
    case filipino = "tl"
    case spanish = "es"
    case japanese = "ja"
    case korean = "ko"
    case chineseSimplified = "zh-CN"
    case indonesian = "id"
    case french = "fr"
    case german = "de"
    case portuguese = "pt"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .english: return "English"
        case .filipino: return "Filipino / Tagalog"
        case .spanish: return "Spanish"
        case .japanese: return "Japanese"
        case .korean: return "Korean"
        case .chineseSimplified: return "Chinese (Simplified)"
        case .indonesian: return "Indonesian"
        case .french: return "French"
        case .german: return "German"
        case .portuguese: return "Portuguese"
        }
    }
}

enum ReverbRoomPreset: String, Codable, CaseIterable, Identifiable, Sendable {
    case off = "Off"
    case studio = "Small Studio"
    case club = "Intimate Club"
    case concertHall = "Concert Hall"
    case cathedral = "Cathedral"

    var id: String { rawValue }

    var feedback: Float {
        switch self {
        case .off: return 0
        case .studio: return 0.58
        case .club: return 0.70
        case .concertHall: return 0.81
        case .cathedral: return 0.88
        }
    }

    var damping: Float {
        switch self {
        case .off: return 0.5
        case .studio: return 0.42
        case .club: return 0.34
        case .concertHall: return 0.26
        case .cathedral: return 0.18
        }
    }
}

enum AutoMixStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case smartBlend = "Smart Blend"
    case crossfade = "Crossfade"

    var id: String { rawValue }

    var subtitle: String {
        switch self {
        case .smartBlend: return "Adapts transition length to song energy & tempo"
        case .crossfade: return "Always blends using your exact crossfade duration"
        }
    }
}

enum MusicHapticsMode: String, Codable, CaseIterable, Identifiable, Sendable {
    case off = "Off"
    case subtle = "Subtle"
    case balanced = "Balanced"
    case dynamic = "Dynamic"
    case bassOnly = "Bass"

    var id: String { rawValue }
}

enum HeadNodAction: String, Codable, CaseIterable, Identifiable, Sendable {
    case none = "Disabled"
    case playPause = "Play / Pause"
    case nextTrack = "Next Track"
    case previousTrack = "Previous Track"
    case toggleLike = "Like / Unlike"

    var id: String { rawValue }
}

enum EqualizerPreset: String, Codable, CaseIterable, Identifiable, Sendable {
    case flat = "Flat"
    case harman = "Harman"
    case bassBoost = "Bass Boost"
    case vocalClarity = "Vocal"
    case lateNightWarm = "Late Night"
    case vShape = "V-Shape"
    case acoustic = "Acoustic"
    case electronic = "Electronic"
    case custom = "Custom"

    var id: String { rawValue }

    static let bandFrequencies: [Double] = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000]
    static let bandLabels: [String] = ["32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"]

    var gains: [Double] {
        switch self {
        case .flat, .custom:
            return [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        case .harman:
            return [4.2, 3.6, 1.4, -0.6, -0.4, 0.8, 2.4, 3.0, 1.8, 0.8]
        case .bassBoost:
            return [6.5, 5.5, 3.4, 1.2, 0.0, 0.0, 0.5, 1.0, 1.2, 0.8]
        case .vocalClarity:
            return [-1.5, -1.0, -0.5, 1.2, 2.8, 3.8, 3.2, 1.8, 0.6, 0.0]
        case .lateNightWarm:
            return [3.2, 2.8, 1.8, 0.8, 0.0, -0.8, -1.6, -2.2, -2.6, -3.0]
        case .vShape:
            return [4.8, 3.8, 1.5, -1.2, -2.0, -1.0, 1.4, 3.2, 4.2, 3.8]
        case .acoustic:
            return [2.2, 2.0, 1.0, 0.4, 1.2, 1.8, 2.6, 2.4, 2.0, 1.4]
        case .electronic:
            return [5.2, 4.4, 1.6, -0.8, -1.2, 0.4, 2.0, 3.4, 3.8, 3.2]
        }
    }
}

enum MotionCanvasProviderKind: String, Codable, Hashable, Sendable {
    case spotifyCanvas = "Spotify Canvas"
    case appleMusicMotion = "Apple Motion"
}

struct MotionCanvasAsset: Hashable, Sendable {
    let trackId: String
    let remoteURL: URL
    let playbackURL: URL
    let provider: MotionCanvasProviderKind
    let isHLS: Bool
    let isTall: Bool
}

struct SubsonicConfig: Codable, Equatable, Sendable {
    var serverURL: String
    var username: String
    var preferRawLossless: Bool

    static let empty = SubsonicConfig(serverURL: "", username: "", preferRawLossless: true)
}

struct SubsonicPlaylistSummary: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let songCount: Int
    let durationSec: Int
}

struct ListeningRecord: Codable, Hashable, Sendable {
    var track: Track
    var playCount: Int
    var listenedSeconds: Int
    var lastPlayedAt: Date
}

struct DerbisBackupDocument: Codable, Sendable {
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

enum GoogleCloudCollectionKind: String, Codable, CaseIterable, Identifiable, Sendable {
    case likes = "Liked Songs"
    case playlists = "Playlists"
    case albums = "Albums"
    case artists = "Artists"
    case history = "History"

    var id: String { rawValue }

    var browseId: String {
        switch self {
        case .likes: return "VLLM"
        case .playlists: return "FEmusic_liked_playlists"
        case .albums: return "FEmusic_liked_albums"
        case .artists: return "FEmusic_library_corpus_artists"
        case .history: return "FEmusic_history"
        }
    }

    var systemImage: String {
        switch self {
        case .likes: return "heart.fill"
        case .playlists: return "music.note.list"
        case .albums: return "square.stack.fill"
        case .artists: return "person.2.fill"
        case .history: return "clock.arrow.circlepath"
        }
    }
}

struct GoogleMusicAccount: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var name: String
    var handle: String
    var avatarURL: String
    var pageId: String?
    var authUser: Int
    var isSelected: Bool

    var initials: String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let first = trimmed.first else { return "G" }
        return String(first).uppercased()
    }
}

struct GoogleMusicSession: Identifiable, Codable, Hashable, Sendable {
    var id: String { account.id }
    var account: GoogleMusicAccount
    var cookie: String
    var updatedAt: Date
}

struct GoogleCloudLibraryEntry: Identifiable, Hashable, Sendable {
    var id: String { browseId }
    let browseId: String
    let title: String
    let subtitle: String
    let thumbnailURL: String
    let collection: GoogleCloudCollectionKind
    let isOwned: Bool
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
