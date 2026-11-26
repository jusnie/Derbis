import CoreFoundation
import Foundation

struct GeniusAnnotationEntry: Identifiable, Hashable, Sendable {
    let id: Int
    let fragment: String
    let explanation: String
    let authorName: String
    let isVerifiedArtist: Bool
    let isEditor: Bool
    let votesTotal: Int

    var badgeLabel: String {
        if isVerifiedArtist { return "Verified Artist" }
        if isEditor { return "Genius Editor" }
        return "Genius Community"
    }

    func passes(filter: GeniusMeaningFilter) -> Bool {
        switch filter {
        case .off:
            return false
        case .artistOnly:
            return isVerifiedArtist
        case .artistAndEditors:
            return isVerifiedArtist || isEditor
        case .everyone:
            return true
        }
    }
}

actor LyricsIntelligenceService {
    static let shared = LyricsIntelligenceService()

    private let session: URLSession
    private var geniusSongCache: [String: [GeniusAnnotationEntry]] = [:]
    private var translationCache: [String: [Int: String]] = [:]

    init() {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 8.0
        cfg.timeoutIntervalForResource = 15.0
        self.session = URLSession(configuration: cfg)
    }

    static func enrichWithOnDeviceRomanization(_ payload: LyricsPayload) -> LyricsPayload {
        if payload.hasRomanization { return payload }
        var changed = false
        var updatedLines = payload.lines

        for idx in updatedLines.indices {
            let text = updatedLines[idx].text.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty, containsNonLatinScript(text) else { continue }
            if let existing = updatedLines[idx].pronunciation, !existing.isEmpty { continue }
            if let roman = romanize(text), !roman.isEmpty,
               roman.caseInsensitiveCompare(text) != .orderedSame {
                updatedLines[idx].pronunciation = roman
                changed = true
            }
        }

        guard changed else { return payload }
        return LyricsPayload(
            trackId: payload.trackId,
            synced: payload.synced,
            wordSynced: payload.wordSynced,
            provider: payload.provider,
            lines: updatedLines,
            hasRomanization: true,
            hasTranslation: payload.hasTranslation
        )
    }

    private static func containsNonLatinScript(_ text: String) -> Bool {
        for scalar in text.unicodeScalars {
            let v = scalar.value
            if v >= 0x2000 && v <= 0x206F { continue }
            if v >= 0x2600 && v <= 0x27BF { continue }
            if v > 0x02AF && CharacterSet.letters.contains(scalar) {
                return true
            }
        }
        return false
    }

    private static func romanize(_ text: String) -> String? {
        let mutable = NSMutableString(string: text) as CFMutableString
        guard CFStringTransform(mutable, nil, kCFStringTransformToLatin, false) else {
            return nil
        }
        _ = CFStringTransform(mutable, nil, kCFStringTransformStripCombiningMarks, false)
        let result = (mutable as String)
            .replacingOccurrences(of: "  ", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return result.isEmpty ? nil : result
    }

    func fetchLineMeanings(
        for track: Track,
        lines: [LyricLine],
        filter: GeniusMeaningFilter
    ) async -> [Int: GeniusAnnotationEntry] {
        guard filter != .off, !lines.isEmpty else { return [:] }

        let allEntries = await loadGeniusAnnotations(title: track.title, artist: track.artist)
        let eligible = allEntries.filter { $0.passes(filter: filter) }
        guard !eligible.isEmpty else { return [:] }

        var result: [Int: GeniusAnnotationEntry] = [:]
        for line in lines {
            let foldedLine = Self.foldText(line.text)
            guard !foldedLine.isEmpty else { continue }

            for entry in eligible {
                let fragLines = Self.fragmentCandidateLines(entry.fragment)
                if fragLines.contains(where: { Self.linesMatch(foldedLine, $0) }) {
                    if let current = result[line.id] {
                        if entry.isVerifiedArtist && !current.isVerifiedArtist {
                            result[line.id] = entry
                        }
                    } else {
                        result[line.id] = entry
                    }
                }
            }
        }
        return result
    }

    private func loadGeniusAnnotations(title: String, artist: String) async -> [GeniusAnnotationEntry] {
        let cleanTitle = Self.bareSongTitle(title)
        let cleanArtist = artist
            .replacingOccurrences(of: " - Topic", with: "")
            .replacingOccurrences(of: "VEVO", with: "")
            .components(separatedBy: ",").first?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? artist

        let cacheKey = "\(cleanTitle.lowercased())::\(cleanArtist.lowercased())"
        if let cached = geniusSongCache[cacheKey] {
            return cached
        }

        guard let songId = await searchGeniusSongId(title: cleanTitle, artist: cleanArtist) else {
            geniusSongCache[cacheKey] = []
            return []
        }

        var collected: [GeniusAnnotationEntry] = []
        for page in 1...3 {
            guard var comps = URLComponents(string: "https://genius.com/api/referents") else { break }
            comps.queryItems = [
                URLQueryItem(name: "song_id", value: String(songId)),
                URLQueryItem(name: "text_format", value: "plain"),
                URLQueryItem(name: "per_page", value: "50"),
                URLQueryItem(name: "page", value: String(page))
            ]
            guard let url = comps.url,
                  let responseDict = await fetchGeniusResponseDict(url: url),
                  let referents = responseDict["referents"] as? [[String: Any]],
                  !referents.isEmpty else {
                break
            }

            for ref in referents {
                let fragment = (ref["fragment"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                guard !fragment.isEmpty,
                      let annotations = ref["annotations"] as? [[String: Any]],
                      let firstAnn = annotations.first else {
                    continue
                }
                let annId = (firstAnn["id"] as? Int) ?? ((ref["id"] as? Int) ?? collected.count)
                let bodyPlain = ((firstAnn["body"] as? [String: Any])?["plain"] as? String)?
                    .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                guard bodyPlain.count >= 12 else { continue }

                let verified = (firstAnn["verified"] as? Bool) == true
                    || ((firstAnn["cosigned_by"] as? [Any])?.isEmpty == false)
                var isEditor = false
                var authorName = "Genius Contributor"
                if let authors = firstAnn["authors"] as? [[String: Any]], let topAuthor = authors.first,
                   let user = topAuthor["user"] as? [String: Any] {
                    if let name = user["name"] as? String, !name.isEmpty {
                        authorName = name
                    }
                    let role = ((user["human_readable_role_for_display"] as? String) ?? "").lowercased()
                    if role.contains("editor") || role.contains("staff") || role.contains("moderator") {
                        isEditor = true
                    }
                    if role.contains("verified artist") {
                        authorName = "\(authorName)"
                    }
                }
                let votes = (firstAnn["votes_total"] as? Int) ?? 0

                collected.append(
                    GeniusAnnotationEntry(
                        id: annId,
                        fragment: fragment,
                        explanation: bodyPlain,
                        authorName: authorName,
                        isVerifiedArtist: verified,
                        isEditor: isEditor,
                        votesTotal: votes
                    )
                )
            }

            if referents.count < 50 { break }
        }

        if geniusSongCache.count >= 60, let firstKey = geniusSongCache.keys.first {
            geniusSongCache.removeValue(forKey: firstKey)
        }
        geniusSongCache[cacheKey] = collected
        return collected
    }

    private func searchGeniusSongId(title: String, artist: String) async -> Int? {
        guard !title.isEmpty, var comps = URLComponents(string: "https://genius.com/api/search/song") else {
            return nil
        }
        comps.queryItems = [URLQueryItem(name: "q", value: "\(title) \(artist)")]
        guard let url = comps.url,
              let response = await fetchGeniusResponseDict(url: url),
              let sections = response["sections"] as? [[String: Any]] else {
            return nil
        }

        let wantTitle = Self.foldText(title)
        let wantArtist = Self.foldText(artist)

        for section in sections {
            guard let hits = section["hits"] as? [[String: Any]] else { continue }
            for hit in hits {
                guard let result = hit["result"] as? [String: Any],
                      let id = result["id"] as? Int else { continue }
                let hitTitle = Self.foldText(Self.bareSongTitle((result["title"] as? String) ?? ""))
                let hitArtists = Self.foldText((result["artist_names"] as? String) ?? "")
                let titleMatch = !wantTitle.isEmpty && (hitTitle == wantTitle || hitTitle.contains(wantTitle) || wantTitle.contains(hitTitle))
                let artistMatch = wantArtist.isEmpty || hitArtists.contains(wantArtist) || wantArtist.contains(hitArtists)
                if titleMatch && artistMatch {
                    return id
                }
            }
        }
        return nil
    }

    private func fetchGeniusResponseDict(url: URL) async -> [String: Any]? {
        var req = URLRequest(url: url)
        req.timeoutInterval = 8.0
        req.setValue(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
            forHTTPHeaderField: "User-Agent"
        )
        req.setValue("application/json", forHTTPHeaderField: "Accept")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<400).contains(http.statusCode),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let inner = root["response"] as? [String: Any] else {
            return nil
        }
        return inner
    }

    func translateLyrics(
        payload: LyricsPayload,
        targetLanguage: LyricsTranslationLanguage
    ) async -> LyricsPayload {
        let key = "\(payload.trackId)::\(targetLanguage.rawValue)"
        if let cachedMap = translationCache[key], !cachedMap.isEmpty {
            return Self.applyTranslationMap(cachedMap, to: payload)
        }

        let nonEmptyIndices = payload.lines.indices.filter {
            !payload.lines[$0].text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
        guard !nonEmptyIndices.isEmpty else { return payload }

        var map: [Int: String] = [:]
        var batchIndices: [Int] = []
        var batchStrings: [String] = []
        var currentChars = 0

        for idx in nonEmptyIndices {
            let clean = payload.lines[idx].text
                .replacingOccurrences(of: "\n", with: " ")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if currentChars + clean.count > 1_800 && !batchStrings.isEmpty {
                let translated = await translateBatch(lines: batchStrings, target: targetLanguage.rawValue)
                for (offset, lineIdx) in batchIndices.enumerated() where offset < translated.count {
                    let candidate = translated[offset].trimmingCharacters(in: .whitespacesAndNewlines)
                    if !candidate.isEmpty && candidate.caseInsensitiveCompare(payload.lines[lineIdx].text) != .orderedSame {
                        map[payload.lines[lineIdx].id] = candidate
                    }
                }
                batchIndices.removeAll()
                batchStrings.removeAll()
                currentChars = 0
            }
            batchIndices.append(idx)
            batchStrings.append(clean)
            currentChars += clean.count + 1
        }

        if !batchStrings.isEmpty {
            let translated = await translateBatch(lines: batchStrings, target: targetLanguage.rawValue)
            for (offset, lineIdx) in batchIndices.enumerated() where offset < translated.count {
                let candidate = translated[offset].trimmingCharacters(in: .whitespacesAndNewlines)
                if !candidate.isEmpty && candidate.caseInsensitiveCompare(payload.lines[lineIdx].text) != .orderedSame {
                    map[payload.lines[lineIdx].id] = candidate
                }
            }
        }

        if !map.isEmpty {
            if translationCache.count >= 60, let firstKey = translationCache.keys.first {
                translationCache.removeValue(forKey: firstKey)
            }
            translationCache[key] = map
        }
        return Self.applyTranslationMap(map, to: payload)
    }

    private func translateBatch(lines: [String], target: String) async -> [String] {
        let joined = lines.joined(separator: "\n")

        if let primary = await queryGoogleTranslateSingle(joined: joined, target: target), !primary.isEmpty {
            return primary
        }
        if let secondary = await queryGoogleClients5Fallback(joined: joined, target: target), !secondary.isEmpty {
            return secondary
        }
        if let lingva = await queryLingvaFallback(joined: joined, target: target), !lingva.isEmpty {
            return lingva
        }
        return []
    }

    private func queryGoogleTranslateSingle(joined: String, target: String) async -> [String]? {
        guard var comps = URLComponents(string: "https://translate.googleapis.com/translate_a/single") else {
            return nil
        }
        comps.queryItems = [
            URLQueryItem(name: "client", value: "gtx"),
            URLQueryItem(name: "sl", value: "auto"),
            URLQueryItem(name: "tl", value: target),
            URLQueryItem(name: "dt", value: "t"),
            URLQueryItem(name: "q", value: joined)
        ]
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.timeoutInterval = 7.0
        req.setValue("Mozilla/5.0", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let root = try? JSONSerialization.jsonObject(with: data) as? [Any],
              let sentences = root.first as? [Any] else {
            return nil
        }

        var fullTranslated = ""
        for item in sentences {
            if let pair = item as? [Any], let chunk = pair.first as? String {
                fullTranslated += chunk
            }
        }
        let split = fullTranslated.components(separatedBy: "\n")
        return split.isEmpty ? nil : split
    }

    private func queryGoogleClients5Fallback(joined: String, target: String) async -> [String]? {
        guard var comps = URLComponents(string: "https://clients5.google.com/translate_a/t") else {
            return nil
        }
        comps.queryItems = [
            URLQueryItem(name: "client", value: "dict-chrome-ex"),
            URLQueryItem(name: "sl", value: "auto"),
            URLQueryItem(name: "tl", value: target),
            URLQueryItem(name: "q", value: joined)
        ]
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.timeoutInterval = 7.0
        req.setValue("Mozilla/5.0", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let root = try? JSONSerialization.jsonObject(with: data) as? [Any] else {
            return nil
        }

        if let firstArr = root.first as? [Any], let text = firstArr.first as? String {
            return text.components(separatedBy: "\n")
        }
        if let firstStr = root.first as? String {
            return firstStr.components(separatedBy: "\n")
        }
        return nil
    }

    private func queryLingvaFallback(joined: String, target: String) async -> [String]? {
        var allowed = CharacterSet.urlPathAllowed
        allowed.remove(charactersIn: "/?#")
        guard let encoded = joined.addingPercentEncoding(withAllowedCharacters: allowed),
              let url = URL(string: "https://lingva.ml/api/v1/auto/\(target)/\(encoded)") else {
            return nil
        }
        var req = URLRequest(url: url)
        req.timeoutInterval = 7.0
        req.setValue("Mozilla/5.0", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let translation = root["translation"] as? String,
              !translation.isEmpty else {
            return nil
        }
        return translation.components(separatedBy: "\n")
    }

    private static func applyTranslationMap(_ map: [Int: String], to payload: LyricsPayload) -> LyricsPayload {
        guard !map.isEmpty else { return payload }
        var updated = payload.lines
        var anyAdded = payload.hasTranslation
        for i in updated.indices {
            if let trans = map[updated[i].id], !trans.isEmpty {
                updated[i].translation = trans
                anyAdded = true
            }
        }
        return LyricsPayload(
            trackId: payload.trackId,
            synced: payload.synced,
            wordSynced: payload.wordSynced,
            provider: payload.provider,
            lines: updated,
            hasRomanization: payload.hasRomanization,
            hasTranslation: anyAdded
        )
    }

    private static func bareSongTitle(_ title: String) -> String {
        var bare = title
        if let dashRange = bare.range(of: " - ") {
            bare = String(bare[..<dashRange.lowerBound])
        }
        if let regex = try? NSRegularExpression(pattern: "\\s*[\\(\\[][^\\)\\]]*[\\)\\]]") {
            let nsRange = NSRange(bare.startIndex..<bare.endIndex, in: bare)
            bare = regex.stringByReplacingMatches(in: bare, range: nsRange, withTemplate: "")
        }
        return bare.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func foldText(_ text: String) -> String {
        let folded = text.folding(options: [.caseInsensitive, .diacriticInsensitive, .widthInsensitive], locale: nil)
        var out = ""
        var pendingSpace = false
        for scalar in folded.unicodeScalars {
            if scalar == "'" || scalar == "\u{2019}" || scalar == "\u{2018}" || scalar == "`" {
                continue
            }
            if CharacterSet.alphanumerics.contains(scalar) {
                if pendingSpace && !out.isEmpty {
                    out.append(" ")
                }
                pendingSpace = false
                out.unicodeScalars.append(scalar)
            } else {
                pendingSpace = true
            }
        }
        return out
    }

    private static func fragmentCandidateLines(_ fragment: String) -> [String] {
        var list: [String] = []
        for row in fragment.components(separatedBy: .newlines) {
            let trimmed = row.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.hasPrefix("[") && trimmed.hasSuffix("]") { continue }
            let folded = foldText(trimmed)
            if !folded.isEmpty {
                list.append(folded)
            }
        }
        return list
    }

    private static func linesMatch(_ a: String, _ b: String) -> Bool {
        guard !a.isEmpty, !b.isEmpty else { return false }
        if a == b { return true }
        if min(a.count, b.count) >= 10 && (a.contains(b) || b.contains(a)) {
            return true
        }
        let wordsA = a.split(separator: " ")
        let wordsB = b.split(separator: " ")
        guard min(wordsA.count, wordsB.count) >= 4 else { return false }
        var pool = wordsA
        var shared = 0
        for w in wordsB {
            if let idx = pool.firstIndex(of: w) {
                pool.remove(at: idx)
                shared += 1
            }
        }
        return Double(shared) / Double(max(wordsA.count, wordsB.count)) >= 0.75
    }
}
