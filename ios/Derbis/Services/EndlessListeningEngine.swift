import Foundation

struct InnerTubeRadioPage: Sendable {
    let radioId: String
    let tracks: [Track]
    let continuation: String
}

struct AlbumSummaryCard: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let artist: String
    let year: String
    let artworkURL: String
    let seedTracks: [Track]
}

struct ArtistCatalogPayload: Sendable {
    let artistName: String
    let heroArtworkURL: String?
    let topTracks: [Track]
    let albums: [AlbumSummaryCard]
    let relatedArtists: [String]
}

struct AlbumCatalogPayload: Sendable {
    let title: String
    let artistName: String
    let artworkURL: String?
    let year: String
    let tracks: [Track]
}

struct RadioSession: Equatable, Sendable {
    static let maxAddedIds = 120
    static let scrollAppendBatch = 12
    static let targetUpcomingBuffer = 8
    static let minimumSkipAdditions = 5

    let sessionId: UUID
    var seed: Track
    var radioId: String
    var continuation: String
    var addedIds: [String]

    init(
        sessionId: UUID = UUID(),
        seed: Track,
        radioId: String = "",
        continuation: String = "",
        addedIds: [String] = []
    ) {
        self.sessionId = sessionId
        self.seed = seed
        self.radioId = radioId.trimmingCharacters(in: .whitespacesAndNewlines)
        self.continuation = continuation.trimmingCharacters(in: .whitespacesAndNewlines)
        self.addedIds = Self.boundedUnique(addedIds)
    }

    func updatingCurrentAnchor(_ current: Track?) -> RadioSession {
        guard let current, current.id != seed.id else { return self }
        var copy = self
        if copy.radioId.isEmpty {
            copy.seed = current
        }
        return copy
    }

    func recordingPage(radioId nextRadioId: String, continuation nextContinuation: String, added: [String]) -> RadioSession {
        var copy = self
        let cleanRadio = nextRadioId.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleanRadio.isEmpty {
            copy.radioId = cleanRadio
        }
        copy.continuation = nextContinuation.trimmingCharacters(in: .whitespacesAndNewlines)
        copy.addedIds = Self.boundedUnique(copy.addedIds + added)
        return copy
    }

    func recordingAdded(_ added: [String]) -> RadioSession {
        guard !added.isEmpty else { return self }
        var copy = self
        copy.addedIds = Self.boundedUnique(copy.addedIds + added)
        return copy
    }

    private static func boundedUnique(_ items: [String]) -> [String] {
        var seen = Set<String>()
        var ordered: [String] = []
        for id in items where !id.isEmpty {
            if seen.contains(id) {
                ordered.removeAll { $0 == id }
            } else {
                seen.insert(id)
            }
            ordered.append(id)
        }
        if ordered.count > maxAddedIds {
            return Array(ordered.suffix(maxAddedIds))
        }
        return ordered
    }
}

enum EndlessTrackTextNormalizer {
    private static let noiseMarkers = [
        "official video", "official music video", "official audio", "official lyric video",
        "lyric video", "lyrics", "visualizer", "remastered", "hd", "hq", "mv", "m/v"
    ]

    static func normalizedTitle(_ raw: String) -> String {
        var lowered = raw.lowercased()
        for noise in noiseMarkers {
            lowered = lowered.replacingOccurrences(of: "(\(noise))", with: " ")
            lowered = lowered.replacingOccurrences(of: "[\(noise)]", with: " ")
        }
        if let featRange = lowered.range(of: " feat.") ?? lowered.range(of: " ft.") ?? lowered.range(of: "(feat.") {
            lowered = String(lowered[..<featRange.lowerBound])
        }
        let cleaned = lowered.unicodeScalars.map { scalar -> Character in
            if CharacterSet.alphanumerics.contains(scalar) {
                return Character(scalar)
            }
            return " "
        }
        return String(cleaned)
            .split(whereSeparator: \.isWhitespace)
            .joined(separator: " ")
    }

    static func canonicalArtist(_ raw: String) -> String {
        let primary = raw
            .components(separatedBy: CharacterSet(charactersIn: ",&/×x"))
            .first ?? raw
        let cleaned = primary.lowercased().unicodeScalars.map { scalar -> Character in
            if CharacterSet.alphanumerics.contains(scalar) {
                return Character(scalar)
            }
            return " "
        }
        return String(cleaned)
            .split(whereSeparator: \.isWhitespace)
            .joined(separator: " ")
    }

    static func isSameOrDerivative(_ lhs: Track, _ rhs: Track) -> Bool {
        if lhs.id == rhs.id { return true }
        if let lv = lhs.videoId, let rv = rhs.videoId, lv == rv { return true }
        let lt = normalizedTitle(lhs.title)
        let rt = normalizedTitle(rhs.title)
        guard !lt.isEmpty, lt == rt else { return false }
        let la = canonicalArtist(lhs.artist)
        let ra = canonicalArtist(rhs.artist)
        if la == ra { return true }
        if lt.count >= 10 && (lhs.title.lowercased().contains("live") || rhs.title.lowercased().contains("live") || lhs.title.lowercased().contains("cover") || rhs.title.lowercased().contains("cover")) {
            return true
        }
        return false
    }
}

enum SimilarSongsSelector {
    static func select(seed: Track, candidates: [Track], blockedCheck: (String) -> Bool, limit: Int = 12) -> [Track] {
        guard limit > 0 else { return [] }
        var picked: [Track] = []
        var seenIds = Set<String>([seed.id])
        if let vid = seed.videoId {
            seenIds.insert(vid)
            seenIds.insert("yt:\(vid)")
        }

        for candidate in candidates {
            guard !seenIds.contains(candidate.id) else { continue }
            if let cv = candidate.videoId, seenIds.contains(cv) { continue }
            guard !blockedCheck(candidate.artist) else { continue }
            guard !EndlessTrackTextNormalizer.isSameOrDerivative(seed, candidate) else { continue }
            guard !picked.contains(where: { EndlessTrackTextNormalizer.isSameOrDerivative($0, candidate) }) else { continue }

            seenIds.insert(candidate.id)
            if let cv = candidate.videoId { seenIds.insert(cv) }
            picked.append(candidate)
            if picked.count >= limit { break }
        }
        return picked
    }
}

enum RadioMixer {
    static func mix(
        candidates: [Track],
        seed: Track,
        queued: [Track],
        sessionAddedIds: [String],
        blockedCheck: (String) -> Bool,
        limit: Int
    ) -> [Track] {
        guard limit > 0, !candidates.isEmpty else { return [] }

        var excludedIds = Set(queued.map(\.id))
        excludedIds.formUnion(sessionAddedIds)
        excludedIds.insert(seed.id)
        for q in queued {
            if let vid = q.videoId {
                excludedIds.insert(vid)
                excludedIds.insert("yt:\(vid)")
            }
        }

        let recentWindow = Array(queued.suffix(18))
        var filtered: [Track] = []

        for item in candidates {
            guard !excludedIds.contains(item.id) else { continue }
            if let vid = item.videoId, excludedIds.contains(vid) { continue }
            guard !blockedCheck(item.artist) else { continue }
            guard !EndlessTrackTextNormalizer.isSameOrDerivative(seed, item) else { continue }
            guard !recentWindow.contains(where: { EndlessTrackTextNormalizer.isSameOrDerivative($0, item) }) else { continue }
            guard !filtered.contains(where: { EndlessTrackTextNormalizer.isSameOrDerivative($0, item) }) else { continue }

            excludedIds.insert(item.id)
            if let vid = item.videoId { excludedIds.insert(vid) }
            filtered.append(item)
        }

        var diversified: [Track] = []
        var pool = filtered
        var lastArtist = queued.last.map { EndlessTrackTextNormalizer.canonicalArtist($0.artist) } ?? EndlessTrackTextNormalizer.canonicalArtist(seed.artist)

        while !pool.isEmpty && diversified.count < limit {
            if let diffIdx = pool.firstIndex(where: { EndlessTrackTextNormalizer.canonicalArtist($0.artist) != lastArtist }) {
                let chosen = pool.remove(at: diffIdx)
                lastArtist = EndlessTrackTextNormalizer.canonicalArtist(chosen.artist)
                diversified.append(chosen)
            } else {
                let chosen = pool.removeFirst()
                lastArtist = EndlessTrackTextNormalizer.canonicalArtist(chosen.artist)
                diversified.append(chosen)
            }
        }

        return diversified
    }
}
