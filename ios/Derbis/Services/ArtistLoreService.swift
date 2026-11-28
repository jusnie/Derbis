import Foundation
import CryptoKit

/// iOS port of Android's ArtistLoreRepository: biographies come from Wikipedia only —
/// nothing is generated or extrapolated. Results are cached on disk for 30 days
/// (negatives for 24 hours). When a language has no acceptable article, falls back to
/// cross-language Wikipedia before giving up. Sections that need a bio simply hide when
/// there is nothing real to show.
struct ArtistCatalogBiography: Hashable, Sendable {
    let text: String
    let sourceLabel: String
    let sourceURL: String
    let languageCode: String
    let confidence: Int
}

actor ArtistLoreService {
    static let shared = ArtistLoreService()

    private let session: URLSession
    private var memory: [String: CachedLore] = [:]

    private let positiveTTL: TimeInterval = 30 * 24 * 60 * 60
    private let negativeTTL: TimeInterval = 24 * 60 * 60
    private let acceptedScore = 60
    private let maxExtractCharacters = 1800
    private let minExtractCharacters = 24

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 8
        config.timeoutIntervalForResource = 12
        config.waitsForConnectivity = false
        self.session = URLSession(configuration: config)
    }

    // MARK: - Public API

    /// Resolves an artist biography from Wikipedia. Returns nil when nothing acceptable
    /// exists — callers hide the bio section rather than show invented text.
    func resolve(
        artistName: String,
        browseId: String,
        languageCode: String
    ) async -> ArtistCatalogBiography? {
        let cleanName = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanName.count >= 2 else { return nil }
        let normalizedKey = Self.normalizeName(cleanName)
        guard !normalizedKey.isEmpty else { return nil }

        let preferred = Self.languagePriority(for: languageCode)
        for language in preferred {
            if let cached = cachedEntry(artistKey: normalizedKey, browseId: browseId, language: language) {
                if cached.negative {
                    continue
                }
                if let bio = cached.makeBiography() {
                    return bio
                }
                continue
            }
            let outcome = await fetchFromWikipedia(artistName: cleanName, language: language)
            switch outcome {
            case .found(let bio):
                persist(artistKey: normalizedKey, browseId: browseId, language: language, biography: bio, negative: false)
                return bio
            case .definitiveMiss:
                persist(artistKey: normalizedKey, browseId: browseId, language: language, biography: nil, negative: true)
            case .transientFailure:
                continue
            }
        }

        // Cross-language pass: device language gave nothing, try other big Wikipedias.
        for language in Self.crossLanguagePriority(excluding: preferred) {
            if let cached = cachedEntry(artistKey: normalizedKey, browseId: browseId, language: language) {
                if cached.negative {
                    continue
                }
                if let bio = cached.makeBiography() {
                    return bio
                }
                continue
            }
            let outcome = await fetchFromWikipedia(artistName: cleanName, language: language)
            switch outcome {
            case .found(let bio):
                persist(artistKey: normalizedKey, browseId: browseId, language: language, biography: bio, negative: false)
                return bio
            case .definitiveMiss:
                persist(artistKey: normalizedKey, browseId: browseId, language: language, biography: nil, negative: true)
            case .transientFailure:
                continue
            }
        }
        return nil
    }

    // MARK: - Fetch

    private enum FetchOutcome {
        case found(ArtistCatalogBiography)
        case definitiveMiss
        case transientFailure
    }

    private func fetchFromWikipedia(artistName: String, language: String) async -> FetchOutcome {
        do {
            let candidates = try await querySearchCandidates(artistName: artistName, language: language)
            guard let winner = candidates
                .map({ ($0, Self.score(candidate: $0, artistName: artistName)) })
                .filter({ $0.1 >= acceptedScore })
                .max(by: { $0.1 < $1.1 }) else {
                return .definitiveMiss
            }
            let (title, score) = winner
            guard let extract = try await fetchExtract(title: title, language: language),
                  extract.count >= minExtractCharacters else {
                return .definitiveMiss
            }
            let confidence = score >= 100 ? 96 : (score >= 80 ? 82 : 60)
            let encodedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
                .replacingOccurrences(of: " ", with: "_")
                .addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? title
            return .found(
                ArtistCatalogBiography(
                    text: extract,
                    sourceLabel: "Wikipedia",
                    sourceURL: "https://\(language).wikipedia.org/wiki/\(encodedTitle)",
                    languageCode: language,
                    confidence: confidence
                )
            )
        } catch is CancellationError {
            return .transientFailure
        } catch {
            return .transientFailure
        }
    }

    private func querySearchCandidates(artistName: String, language: String) async throws -> [String] {
        guard var components = URLComponents(string: "https://\(language).wikipedia.org/w/api.php") else {
            return []
        }
        components.queryItems = [
            URLQueryItem(name: "action", value: "query"),
            URLQueryItem(name: "list", value: "search"),
            URLQueryItem(name: "srsearch", value: artistName),
            URLQueryItem(name: "srlimit", value: "5"),
            URLQueryItem(name: "format", value: "json"),
        ]
        guard let url = components.url else { return [] }
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw URLError(.badServerResponse)
        }
        let search = (root["query"] as? [String: Any])?["search"] as? [[String: Any]] ?? []
        return search.compactMap { $0["title"] as? String }
    }

    private func fetchExtract(title: String, language: String) async throws -> String? {
        guard var components = URLComponents(string: "https://\(language).wikipedia.org/w/api.php") else {
            return nil
        }
        components.queryItems = [
            URLQueryItem(name: "action", value: "query"),
            URLQueryItem(name: "prop", value: "extracts"),
            URLQueryItem(name: "explaintext", value: "1"),
            URLQueryItem(name: "redirects", value: "1"),
            URLQueryItem(name: "titles", value: title),
            URLQueryItem(name: "format", value: "json"),
        ]
        guard let url = components.url else { return nil }
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let pages = (root["query"] as? [String: Any])?["pages"] as? [String: Any] else {
            throw URLError(.badServerResponse)
        }
        for (_, value) in pages {
            guard let page = value as? [String: Any],
                  let extract = page["extract"] as? String else { continue }
            return Self.cleanedExtract(extract)
        }
        return nil
    }

    // MARK: - Scoring & cleanup

    /// Name-match score. Mirrors Android's ACCEPTED_SCORE semantics loosely: an exact
    /// (normalized) title match clears the bar, strong containment clears it too.
    private static func score(candidate: String, artistName: String) -> Int {
        let candidateNorm = normalizeName(candidate)
        let queryNorm = normalizeName(artistName)
        guard !candidateNorm.isEmpty, !queryNorm.isEmpty else { return 0 }
        if candidateNorm == queryNorm { return 110 }
        // Drop disambiguation suffixes, e.g. "Coldplay (band)" → "coldplay".
        let candidateHead = candidateNorm.components(separatedBy: " (").first ?? candidateNorm
        if candidateHead == queryNorm { return 105 }
        if candidateNorm.hasPrefix(queryNorm + " ") || candidateHead.hasPrefix(queryNorm + " ") { return 82 }
        if candidateNorm.contains(queryNorm) || queryNorm.contains(candidateNorm) { return 70 }
        // Token overlap for multi-word names.
        let candidateTokens = Set(candidateNorm.split(separator: " ").map(String.init))
        let queryTokens = Set(queryNorm.split(separator: " ").map(String.init))
        guard !candidateTokens.isEmpty, !queryTokens.isEmpty else { return 0 }
        let overlap = candidateTokens.intersection(queryTokens).count
        let ratio = Double(overlap) / Double(queryTokens.count)
        return Int(ratio * 62)
    }

    static func normalizeName(_ raw: String) -> String {
        let folded = (raw as NSString).folding(options: .diacriticInsensitive, locale: nil)
        return folded
            .lowercased()
            .replacingOccurrences(of: "_", with: " ")
            .split(whereSeparator: { $0.isWhitespace || $0 == "," })
            .joined(separator: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func cleanedExtract(_ raw: String) -> String {
        var text = raw.replacingOccurrences(of: "\n\n", with: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
        // Paranoia: drop trailing license/attribution lines if an extract ever carries them.
        let markers = ["creative commons", "cc-by-sa", "wikipedia, the free encyclopedia", "wikipedia, l'enciclopedia libera"]
        let lowered = text.lowercased()
        if let cut = markers.compactMap({ lowered.range(of: $0)?.lowerBound }).min() {
            text = String(text[..<cut]).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        guard text.count > maxExtractCharacters else { return text }
        let cap = String(text.prefix(maxExtractCharacters))
        if let lastPeriod = cap.lastIndex(of: ".") {
            return String(cap[...lastPeriod]).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return cap.trimmingCharacters(in: .whitespacesAndNewlines) + "…"
    }

    private static func languagePriority(for code: String) -> [String] {
        let base = code.lowercased().split(separator: "-").first.map(String.init) ?? "en"
        var out: [String] = []
        for item in [base, "en"] {
            if !item.isEmpty, !out.contains(item) {
                out.append(item)
            }
        }
        return out
    }

    private static func crossLanguagePriority(excluding: [String]) -> [String] {
        let excluded = Set(excluding)
        return ["en", "fr", "es", "de", "id", "pt", "ms", "nl", "tl"].filter { !excluded.contains($0) }
    }

    // MARK: - Cache

    private struct CachedLore: Codable {
        let artistKey: String
        let browseId: String
        let languageCode: String
        let text: String
        let sourceLabel: String
        let sourceURL: String
        let confidence: Int
        let fetchedAt: Date
        let negative: Bool

        func makeBiography() -> ArtistCatalogBiography {
            ArtistCatalogBiography(
                text: text,
                sourceLabel: sourceLabel,
                sourceURL: sourceURL,
                languageCode: languageCode,
                confidence: confidence
            )
        }
    }

    private func cacheDirectory() -> URL? {
        guard let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            return nil
        }
        let dir = caches.appendingPathComponent("derbis-artist-lore", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private func cacheFileURL(artistKey: String, browseId: String, language: String) -> URL? {
        guard let dir = cacheDirectory() else { return nil }
        let seed = "\(artistKey)|\(browseId.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())|\(language)"
        let digest = SHA256.hash(data: Data(seed.utf8)).map { String(format: "%02x", $0) }.joined()
        return dir.appendingPathComponent("lore-\(digest.prefix(24)).json")
    }

    private func cachedEntry(artistKey: String, browseId: String, language: String) -> CachedLore? {
        let now = Date()
        if let memoryEntry = memory[cacheFileKey(artistKey: artistKey, browseId: browseId, language: language)] {
            if isFresh(memoryEntry, now: now) {
                return memoryEntry
            }
            memory.removeValue(forKey: cacheFileKey(artistKey: artistKey, browseId: browseId, language: language))
        }
        guard let file = cacheFileURL(artistKey: artistKey, browseId: browseId, language: language),
              let data = try? Data(contentsOf: file),
              let entry = try? JSONDecoder().decode(CachedLore.self, from: data) else {
            return nil
        }
        guard isFresh(entry, now: now) else {
            try? FileManager.default.removeItem(at: file)
            return nil
        }
        memory[cacheFileKey(artistKey: artistKey, browseId: browseId, language: language)] = entry
        return entry
    }

    private func cacheFileKey(artistKey: String, browseId: String, language: String) -> String {
        "\(artistKey)|\(browseId.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())|\(language)"
    }

    private func isFresh(_ entry: CachedLore, now: Date) -> Bool {
        let ttl = entry.negative ? negativeTTL : positiveTTL
        return now.timeIntervalSince(entry.fetchedAt) < ttl
    }

    private func persist(
        artistKey: String,
        browseId: String,
        language: String,
        biography: ArtistCatalogBiography?,
        negative: Bool
    ) {
        let entry = CachedLore(
            artistKey: artistKey,
            browseId: browseId.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            languageCode: language,
            text: biography?.text ?? "",
            sourceLabel: biography?.sourceLabel ?? "",
            sourceURL: biography?.sourceURL ?? "",
            confidence: biography?.confidence ?? 0,
            fetchedAt: Date(),
            negative: negative
        )
        memory[cacheFileKey(artistKey: artistKey, browseId: browseId, language: language)] = entry
        guard let file = cacheFileURL(artistKey: artistKey, browseId: browseId, language: language),
              let data = try? JSONEncoder().encode(entry) else {
            return
        }
        try? data.write(to: file, options: .atomic)
    }
}
