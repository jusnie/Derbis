import Foundation

actor LyricsService {
    static let shared = LyricsService()

    private let session: URLSession
    private var memoryCache: [String: LyricsPayload] = [:]

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 6.5
        config.timeoutIntervalForResource = 12
        self.session = URLSession(configuration: config)
    }

    func fetchLyrics(for track: Track, forceRefresh: Bool = false) async -> LyricsPayload? {
        if !forceRefresh {
            if let cached = memoryCache[track.id] {
                return cached
            }
            if let disk = loadFromDisk(trackId: track.id) {
                memoryCache[track.id] = disk
                return disk
            }
        }

        let cleanTitle = sanitizeQueryTitle(track.title)
        let cleanArtist = track.artist
            .replacingOccurrences(of: " - Topic", with: "")
            .replacingOccurrences(of: "VEVO", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if let unison = await queryUnisonTTML(
            trackId: track.id,
            title: cleanTitle,
            artist: cleanArtist,
            album: track.album,
            durationSec: track.durationSec
        ) {
            memoryCache[track.id] = unison
            saveToDisk(unison)
            return unison
        }

        if let bini = await queryBiniLyricsTTML(
            trackId: track.id,
            title: cleanTitle,
            artist: cleanArtist,
            album: track.album,
            durationSec: track.durationSec
        ) {
            memoryCache[track.id] = bini
            saveToDisk(bini)
            return bini
        }

        if let direct = await queryLRCLibDirect(
            trackId: track.id,
            title: cleanTitle,
            artist: cleanArtist,
            album: track.album,
            durationSec: track.durationSec
        ) {
            memoryCache[track.id] = direct
            saveToDisk(direct)
            return direct
        }

        if let searched = await queryLRCLibSearch(
            trackId: track.id,
            title: cleanTitle,
            artist: cleanArtist,
            durationSec: track.durationSec
        ) {
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

    private func queryUnisonTTML(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ) async -> LyricsPayload? {
        guard !title.isEmpty, !artist.isEmpty,
              var comps = URLComponents(string: "https://unison.boidu.dev/lyrics") else {
            return nil
        }
        var items = [
            URLQueryItem(name: "song", value: title),
            URLQueryItem(name: "artist", value: artist)
        ]
        if durationSec > 0 {
            items.append(URLQueryItem(name: "duration", value: String(durationSec)))
        }
        if !album.isEmpty {
            items.append(URLQueryItem(name: "album", value: album))
        }
        comps.queryItems = items
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.timeoutInterval = 5.0
        req.setValue("Derbis-iOS/1.8.0", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let payload = root["data"] as? [String: Any],
              (payload["format"] as? String) == "ttml",
              let ttml = payload["lyrics"] as? String else {
            return nil
        }

        let foundDuration = extractInteger(payload["duration"])
        if durationSec > 0 && foundDuration > 0 && abs(foundDuration - durationSec) > 5 {
            return nil
        }

        let richSync = ((payload["syncType"] as? String) == "richsync")
        return TTMLDocumentReader.parse(xmlString: ttml, trackId: trackId, provider: "Apple TTML", preferWordSync: richSync)
    }

    private func queryBiniLyricsTTML(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ) async -> LyricsPayload? {
        guard !title.isEmpty, !artist.isEmpty,
              var comps = URLComponents(string: "https://lyrics-api.binimum.org/") else {
            return nil
        }
        var items = [
            URLQueryItem(name: "track", value: title),
            URLQueryItem(name: "artist", value: artist)
        ]
        if durationSec > 0 {
            items.append(URLQueryItem(name: "duration", value: String(durationSec)))
        }
        if !album.isEmpty {
            items.append(URLQueryItem(name: "album", value: album))
        }
        comps.queryItems = items
        guard let url = comps.url else { return nil }

        var req = URLRequest(url: url)
        req.timeoutInterval = 5.5
        req.setValue("Derbis-iOS/1.8.0", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              http.statusCode == 200,
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let results = root["results"] as? [[String: Any]],
              !results.isEmpty else {
            return nil
        }

        var bestItem: [String: Any]?
        var bestWordTimed = false
        var bestDiff = Int.max

        for candidate in results {
            guard candidate["lyricsUrl"] is String else { continue }
            let len = extractInteger(candidate["duration"])
            let diff = (durationSec > 0 && len > 0) ? abs(len - durationSec) : 0
            if diff > 5 { continue }
            let isWord = ((candidate["timing_type"] as? String) == "word")
            if bestItem == nil || (isWord && !bestWordTimed) || (isWord == bestWordTimed && diff < bestDiff) {
                bestItem = candidate
                bestWordTimed = isWord
                bestDiff = diff
            }
        }

        guard let chosen = bestItem,
              let ttmlURLStr = chosen["lyricsUrl"] as? String,
              let ttmlURL = URL(string: ttmlURLStr) else {
            return nil
        }

        var ttmlReq = URLRequest(url: ttmlURL)
        ttmlReq.timeoutInterval = 5.5
        guard let (ttmlData, ttmlResp) = try? await session.data(for: ttmlReq),
              let ttmlHTTP = ttmlResp as? HTTPURLResponse,
              ttmlHTTP.statusCode == 200,
              let xmlStr = String(data: ttmlData, encoding: .utf8) else {
            return nil
        }

        return TTMLDocumentReader.parse(
            xmlString: xmlStr,
            trackId: trackId,
            provider: bestWordTimed ? "TTML Word-Sync" : "TTML",
            preferWordSync: bestWordTimed
        )
    }

    private func queryLRCLibDirect(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ) async -> LyricsPayload? {
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

    private func queryLRCLibSearch(trackId: String, title: String, artist: String, durationSec: Int) async -> LyricsPayload? {
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
                let aDur = extractInteger(a["duration"])
                let bDur = extractInteger(b["duration"])
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
                return LyricsPayload(trackId: trackId, synced: true, wordSynced: false, provider: "LRCLIB", lines: lines)
            }
        }
        if let plainText = json["plainLyrics"] as? String, !plainText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let rawLines = plainText
                .components(separatedBy: .newlines)
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
            let lines = rawLines.enumerated().map { idx, text in
                LyricLine(id: idx, startTimeSec: Double(idx) * 3.5, endTimeSec: Double(idx + 1) * 3.5, text: text)
            }
            if !lines.isEmpty {
                return LyricsPayload(trackId: trackId, synced: false, wordSynced: false, provider: "LRCLIB", lines: lines)
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
            let nextStart = (idx + 1 < sorted.count) ? sorted[idx + 1].0 : (pair.0 + 4.5)
            return LyricLine(
                id: idx,
                startTimeSec: pair.0,
                endTimeSec: max(pair.0 + 0.5, min(nextStart, pair.0 + 8.0)),
                text: pair.1
            )
        }
    }

    private func extractInteger(_ value: Any?) -> Int {
        if let n = value as? Int { return n }
        if let d = value as? Double { return Int(d.rounded()) }
        if let s = value as? String, let d = Double(s) { return Int(d.rounded()) }
        return 0
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
        let dir = caches.appendingPathComponent("DerbisLyricsV2", isDirectory: true)
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

private final class TTMLDocumentReader: NSObject, XMLParserDelegate {
    private enum HeadSection {
        case none
        case translation
        case transliteration
    }

    private final class WordAccumulator {
        var words: [LyricWord] = []
        var spaced: Bool = true
    }

    private struct RawLineRecord {
        var key: String?
        var startSec: Double
        var endSec: Double
        var voice: String?
        var mainWords: [LyricWord]
        var backingWords: [LyricWord]
        var plainText: String
    }

    private var rawLines: [RawLineRecord] = []
    private var translationsByLang: [String: [String: String]] = [:]
    private var translationLangOrder: [String] = []
    private var romanizationsByLang: [String: [String: String]] = [:]
    private var romanizationLangOrder: [String] = []

    private var stack: [WordAccumulator] = []
    private var backingAcc: WordAccumulator?
    private var currentHeadSection: HeadSection = .none
    private var activeHeadReading: HeadSection = .none
    private var activeLanguage: String = ""

    private var lineStartSec: Double = -1
    private var lineEndSec: Double = -1
    private var lineVoice: String?
    private var lineKey: String?
    private var plainBuffer: String = ""

    private var wordBuffer: String?
    private var wordStartSec: Double = 0
    private var wordEndSec: Double = 0
    private var spanDepth: Int = 0
    private var bgDepth: Int = 0
    private var wordDepth: Int = 0

    static func parse(xmlString: String, trackId: String, provider: String, preferWordSync: Bool) -> LyricsPayload? {
        guard let data = xmlString.data(using: .utf8), !data.isEmpty else { return nil }
        let delegate = TTMLDocumentReader()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.shouldProcessNamespaces = false
        guard parser.parse(), !delegate.rawLines.isEmpty else { return nil }
        return delegate.buildPayload(trackId: trackId, provider: provider, preferWordSync: preferWordSync)
    }

    private func resetLineContainers() {
        stack = [WordAccumulator()]
        backingAcc = nil
        wordBuffer = nil
        spanDepth = 0
        bgDepth = 0
        wordDepth = 0
        plainBuffer = ""
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        if elementName == "p" {
            resetLineContainers()
            activeHeadReading = .none
            lineStartSec = Self.parseClockSeconds(attributeDict["begin"])
            lineEndSec = Self.parseClockSeconds(attributeDict["end"])
            lineVoice = attributeDict["ttm:agent"] ?? attributeDict["agent"]
            lineKey = attributeDict["itunes:key"] ?? attributeDict["lrc:key"] ?? attributeDict["key"]
            return
        }

        if elementName == "translation" {
            currentHeadSection = .translation
            activeLanguage = attributeDict["xml:lang"] ?? attributeDict["lang"] ?? "default"
            return
        }

        if elementName == "transliteration" {
            currentHeadSection = .transliteration
            activeLanguage = attributeDict["xml:lang"] ?? attributeDict["lang"] ?? "default"
            return
        }

        if elementName == "text", currentHeadSection != .none, let targetKey = attributeDict["for"], !targetKey.isEmpty {
            resetLineContainers()
            activeHeadReading = currentHeadSection
            lineKey = targetKey
            return
        }

        guard elementName == "span", !stack.isEmpty else { return }
        spanDepth += 1
        let role = attributeDict["ttm:role"] ?? attributeDict["role"]
        if role == "x-bg", backingAcc == nil {
            let bg = WordAccumulator()
            backingAcc = bg
            stack.append(bg)
            bgDepth = spanDepth
            return
        }

        guard wordBuffer == nil else { return }
        let wStart = Self.parseClockSeconds(attributeDict["begin"])
        let wEnd = Self.parseClockSeconds(attributeDict["end"])
        guard wStart >= 0 else { return }
        wordBuffer = ""
        wordStartSec = wStart
        wordEndSec = max(wStart, wEnd)
        wordDepth = spanDepth
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard !stack.isEmpty else { return }
        plainBuffer.append(string)
        if wordBuffer != nil {
            wordBuffer?.append(string)
            return
        }
        if string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !string.isEmpty {
            stack.last?.spaced = true
        }
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        if elementName == "p" {
            finalizeLyricLine()
            stack.removeAll()
            return
        }

        if elementName == "text", activeHeadReading != .none {
            finalizeHeadText()
            stack.removeAll()
            activeHeadReading = .none
            return
        }

        if elementName == "translation" || elementName == "transliteration" {
            currentHeadSection = .none
            return
        }

        guard elementName == "span", !stack.isEmpty else { return }
        if let rawWord = wordBuffer, spanDepth == wordDepth {
            let trimmed = rawWord.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty, let top = stack.last {
                let word = LyricWord(
                    id: top.words.count,
                    text: trimmed,
                    startTimeSec: wordStartSec,
                    endTimeSec: max(wordStartSec + 0.04, wordEndSec),
                    joinedToPrevious: !top.words.isEmpty && !top.spaced
                )
                top.words.append(word)
                top.spaced = false
            }
            wordBuffer = nil
            wordDepth = 0
        }

        if bgDepth > 0 && spanDepth == bgDepth {
            _ = stack.popLast()
            bgDepth = 0
        }
        if spanDepth > 0 {
            spanDepth -= 1
        }
    }

    private func finalizeLyricLine() {
        guard let main = stack.first else { return }
        let mainWords = main.words
        let bgWords = backingAcc?.words ?? []

        let start: Double
        if lineStartSec >= 0 {
            start = lineStartSec
        } else if let firstW = mainWords.first {
            start = firstW.startTimeSec
        } else {
            return
        }

        let end: Double
        if lineEndSec > start {
            end = lineEndSec
        } else if let lastW = mainWords.last {
            end = max(start + 0.4, lastW.endTimeSec)
        } else {
            end = start + 3.5
        }

        let collapsedPlain = plainBuffer
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .joined(separator: " ")

        rawLines.append(
            RawLineRecord(
                key: lineKey,
                startSec: start,
                endSec: end,
                voice: lineVoice,
                mainWords: mainWords,
                backingWords: bgWords,
                plainText: collapsedPlain
            )
        )
    }

    private func finalizeHeadText() {
        guard let key = lineKey, !key.isEmpty else { return }
        let wordsText: String
        if let main = stack.first, !main.words.isEmpty {
            wordsText = Self.joinWords(main.words)
        } else {
            wordsText = plainBuffer
                .components(separatedBy: .whitespacesAndNewlines)
                .filter { !$0.isEmpty }
                .joined(separator: " ")
        }
        guard !wordsText.isEmpty else { return }

        switch activeHeadReading {
        case .translation:
            if translationsByLang[activeLanguage] == nil {
                translationsByLang[activeLanguage] = [:]
                translationLangOrder.append(activeLanguage)
            }
            translationsByLang[activeLanguage]?[key] = wordsText
        case .transliteration:
            if romanizationsByLang[activeLanguage] == nil {
                romanizationsByLang[activeLanguage] = [:]
                romanizationLangOrder.append(activeLanguage)
            }
            romanizationsByLang[activeLanguage]?[key] = wordsText
        case .none:
            break
        }
    }

    private func buildPayload(trackId: String, provider: String, preferWordSync: Bool) -> LyricsPayload? {
        let preferredUserLang = (Locale.current.language.languageCode?.identifier ?? "en").lowercased()
        let pickedTranslationLang = translationLangOrder.first(where: { $0.lowercased().hasPrefix(preferredUserLang) })
            ?? translationLangOrder.first(where: { $0.lowercased().hasPrefix("en") })
            ?? translationLangOrder.first
        let pickedRomanLang = romanizationLangOrder.first(where: { $0.lowercased().contains("latn") })
            ?? romanizationLangOrder.first

        let transMap = pickedTranslationLang.flatMap { translationsByLang[$0] } ?? [:]
        let romanMap = pickedRomanLang.flatMap { romanizationsByLang[$0] } ?? [:]

        var builtLines: [LyricLine] = []
        var anyWordSynced = false
        var anyRoman = false
        var anyTrans = false

        var primaryAgent: String?
        for rec in rawLines {
            if let v = rec.voice, !v.isEmpty {
                primaryAgent = v
                break
            }
        }

        for (idx, rec) in rawLines.enumerated() {
            let lineText = rec.mainWords.isEmpty ? rec.plainText : Self.joinWords(rec.mainWords)
            guard !lineText.isEmpty else { continue }

            let backingText = rec.backingWords.isEmpty ? nil : Self.joinWords(rec.backingWords)
            if rec.mainWords.count >= 2 {
                anyWordSynced = true
            }

            var romanText: String?
            var transText: String?
            if let k = rec.key {
                if let r = romanMap[k], Self.bareAlphanumeric(r) != Self.bareAlphanumeric(lineText) {
                    romanText = r
                    anyRoman = true
                }
                if let t = transMap[k], Self.bareAlphanumeric(t) != Self.bareAlphanumeric(lineText) {
                    transText = t
                    anyTrans = true
                }
            }

            let isSecondSinger: Bool = {
                guard let v = rec.voice, !v.isEmpty, let primary = primaryAgent else { return false }
                return v != primary
            }()

            builtLines.append(
                LyricLine(
                    id: idx,
                    startTimeSec: rec.startSec,
                    endTimeSec: rec.endSec,
                    text: lineText,
                    words: rec.mainWords,
                    backingText: backingText,
                    pronunciation: romanText,
                    translation: transText,
                    isSecondaryVoice: isSecondSinger
                )
            )
        }

        guard !builtLines.isEmpty else { return nil }
        return LyricsPayload(
            trackId: trackId,
            synced: true,
            wordSynced: anyWordSynced || preferWordSync,
            provider: provider,
            lines: builtLines,
            hasRomanization: anyRoman,
            hasTranslation: anyTrans
        )
    }

    private static func joinWords(_ words: [LyricWord]) -> String {
        var out = ""
        for (idx, w) in words.enumerated() {
            if idx > 0 && !w.joinedToPrevious {
                out.append(" ")
            }
            out.append(w.text)
        }
        return out
    }

    private static func bareAlphanumeric(_ text: String) -> String {
        text.lowercased().unicodeScalars.filter { CharacterSet.alphanumerics.contains($0) }.map(String.init).joined()
    }

    private static func parseClockSeconds(_ raw: String?) -> Double {
        guard var text = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else {
            return -1
        }
        var multiplier = 1.0
        if text.hasSuffix("ms") {
            multiplier = 0.001
            text.removeLast(2)
        } else if text.hasSuffix("s") {
            multiplier = 1.0
            text.removeLast(1)
        }
        let parts = text.split(separator: ":")
        guard !parts.isEmpty && parts.count <= 3 else { return -1 }
        var total = 0.0
        for part in parts {
            guard let val = Double(part) else { return -1 }
            total = total * 60.0 + val
        }
        return parts.count > 1 ? total : (total * multiplier)
    }
}
