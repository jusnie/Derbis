import Foundation

actor InnerTubeService {
    static let shared = InnerTubeService()

    private let session: URLSession
    private var cachedVisitorData: String = ""
    private var cachedApiKey: String = ""
    private var lastContextFetch: Date = .distantPast

    private let webRemixVersion = "1.20260804.01.00"
    private let webUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 14
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)
    }

    func searchTracks(query: String) async throws -> [Track] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }

        let songsTask = Task { try? await performWebRemixSearch(query: trimmed, params: "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D") }
        let generalTask = Task { try? await performWebRemixSearch(query: trimmed, params: nil) }

        let songs = await songsTask.value ?? []
        let general = await generalTask.value ?? []

        var seen = Set<String>()
        var combined: [Track] = []
        for track in (songs + general) {
            if seen.insert(track.id).inserted {
                combined.append(track)
            }
        }
        return Array(combined.prefix(40))
    }

    func searchSuggestions(query: String) async -> [String] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2,
              let encoded = trimmed.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=\(encoded)") else {
            return []
        }
        do {
            let (data, _) = try await session.data(from: url)
            if let root = try JSONSerialization.jsonObject(with: data) as? [Any],
               root.count >= 2,
               let items = root[1] as? [String] {
                return Array(items.prefix(8))
            }
        } catch {
            return []
        }
        return []
    }

    func fetchHomeShelves(mood: String = "All") async -> [HomeShelf] {
        let queries: [(id: String, title: String, subtitle: String, search: String)] = {
            switch mood {
            case "Chill":
                return [
                    ("chill-1", "Chill rotation", "Warm downtempo & acoustic picks", "chill r&b indie songs"),
                    ("chill-2", "Soft focus", "Lo-fi & ambient instrumentals", "lofi chill beats study"),
                    ("chill-3", "Acoustic evenings", "Unplugged & stripped sessions", "acoustic chill pop songs")
                ]
            case "Late Night":
                return [
                    ("night-1", "After hours", "Midnight R&B and synthwave", "late night drive songs r&b"),
                    ("night-2", "Dark alt & dream pop", "Slow-burn atmospheric cuts", "dream pop shoegaze songs"),
                    ("night-3", "City lights", "Smooth electronic & soul", "midnight electronic chill songs")
                ]
            case "Focus":
                return [
                    ("focus-1", "Deep work", "Zero-distraction modern classical & ambient", "ambient focus instrumental music"),
                    ("focus-2", "Lo-fi study", "Steady tape beats", "lofi hip hop instrumental"),
                    ("focus-3", "Piano & strings", "Minimalist neoclassical", "modern piano instrumental music")
                ]
            case "Energy":
                return [
                    ("energy-1", "High tempo", "Upbeat pop, dance & hip-hop", "upbeat workout hits songs"),
                    ("energy-2", "Electronic pulse", "House & UK garage cuts", "house dance music hits"),
                    ("energy-3", "Alt rock drive", "Guitars & heavy drums", "modern rock anthems songs")
                ]
            case "OPM":
                return [
                    ("opm-1", "OPM essentials", "Top Filipino bands & soloists", "OPM top hits songs"),
                    ("opm-2", "OPM indie & chill", "Manila indie & acoustic", "OPM indie chill songs"),
                    ("opm-3", "OPM classics", "Timeless Pinoy favorites", "classic OPM hits")
                ]
            default:
                return [
                    ("top-1", "Trending right now", "Global chart movers", "top global hits songs 2026"),
                    ("top-2", "Late night rotation", "Smooth R&B, alt-pop & soul", "late night r&b pop songs"),
                    ("top-3", "OPM & Asian indie", "Regional favorites", "OPM hits indie songs"),
                    ("top-4", "Electronic & chill", "Clean production & deep bass", "electronic chill house songs")
                ]
            }
        }()

        return await withTaskGroup(of: (Int, HomeShelf?).self) { group -> [HomeShelf] in
            for (idx, item) in queries.enumerated() {
                group.addTask {
                    let tracks = (try? await self.searchTracks(query: item.search)) ?? []
                    guard !tracks.isEmpty else { return (idx, nil) }
                    return (
                        idx,
                        HomeShelf(
                            id: item.id,
                            title: item.title,
                            subtitle: item.subtitle,
                            tracks: Array(tracks.prefix(12))
                        )
                    )
                }
            }
            var indexed: [(Int, HomeShelf)] = []
            for await (idx, shelf) in group {
                if let shelf = shelf {
                    indexed.append((idx, shelf))
                }
            }
            return indexed.sorted(by: { $0.0 < $1.0 }).map(\.1)
        }
    }

    func fetchRadioQueue(seedVideoId: String) async -> [Track] {
        guard !seedVideoId.isEmpty,
              let url = URL(string: "https://music.youtube.com/youtubei/v1/next?prettyPrint=false") else {
            return []
        }

        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": "en",
                    "gl": "US"
                ]
            ],
            "videoId": seedVideoId,
            "playlistId": "RDAMVM\(seedVideoId)",
            "isAudioOnly": true
        ]

        do {
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
            request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
            request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
            request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
            request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)

            let (data, _) = try await session.data(for: request)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return []
            }
            var results: [Track] = []
            collectTracksFromJSON(json, into: &results)
            var seen = Set<String>([seedVideoId, "yt:\(seedVideoId)"])
            return results.filter { seen.insert($0.id).inserted }
        } catch {
            return []
        }
    }

    func resolvePlayableStream(videoId: String, preferHighQuality: Bool) async throws -> ResolvedStream {
        await refreshWatchContextIfNeeded(videoId: videoId)

        let strategies: [ClientProfile] = [
            ClientProfile(
                name: "IOS",
                version: "20.10.4",
                headerId: "5",
                userAgent: "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)",
                extraFields: [
                    "deviceMake": "Apple",
                    "deviceModel": "iPhone16,2",
                    "osName": "iPhone",
                    "osVersion": "18.3.0.22D63"
                ],
                endpoint: "player"
            ),
            ClientProfile(
                name: "VISIONOS",
                version: "1.04",
                headerId: "101",
                userAgent: "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; US)",
                extraFields: [
                    "deviceMake": "Apple",
                    "deviceModel": "RealityDevice17,1",
                    "osName": "visionOS",
                    "osVersion": "26.6.0.23O770",
                    "platform": "MOBILE",
                    "clientScreen": "WATCH"
                ],
                endpoint: "player"
            ),
            ClientProfile(
                name: "ANDROID",
                version: "21.03.36",
                headerId: "3",
                userAgent: "com.google.android.youtube/21.03.36 (Linux; U; Android 15) gzip",
                extraFields: [
                    "androidSdkVersion": 35,
                    "osName": "Android",
                    "osVersion": "15"
                ],
                endpoint: "reel"
            ),
            ClientProfile(
                name: "ANDROID_MUSIC",
                version: "8.10.52",
                headerId: "21",
                userAgent: "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15) gzip",
                extraFields: [
                    "androidSdkVersion": 34,
                    "osName": "Android",
                    "osVersion": "15"
                ],
                endpoint: "player"
            )
        ]

        for profile in strategies {
            if let stream = try? await requestInnerTubeStream(
                videoId: videoId,
                profile: profile,
                preferHighQuality: preferHighQuality
            ) {
                return stream
            }
        }

        if let fallback = try? await resolveViaPipedFallback(videoId: videoId, preferHighQuality: preferHighQuality) {
            return fallback
        }

        throw URLError(.cannotDecodeRawData)
    }

    private struct ClientProfile {
        let name: String
        let version: String
        let headerId: String
        let userAgent: String
        let extraFields: [String: Any]
        let endpoint: String
    }

    private func refreshWatchContextIfNeeded(videoId: String) async {
        if !cachedVisitorData.isEmpty && Date().timeIntervalSince(lastContextFetch) < 1800 {
            return
        }
        guard let url = URL(string: "https://www.youtube.com/watch?v=\(videoId)&bpctr=9999999999&has_verified=1") else {
            return
        }
        var request = URLRequest(url: url)
        request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("en-US,en;q=0.9", forHTTPHeaderField: "Accept-Language")

        guard let (data, _) = try? await session.data(for: request),
              let html = String(data: data, encoding: .utf8) else {
            return
        }

        if let visitor = firstRegexMatch(in: html, pattern: #""VISITOR_DATA"\s*:\s*"([^"]+)""#) {
            cachedVisitorData = visitor
        }
        if let key = firstRegexMatch(in: html, pattern: #""INNERTUBE_API_KEY"\s*:\s*"([^"]+)""#) {
            cachedApiKey = key
        }
        lastContextFetch = Date()
    }

    private func requestInnerTubeStream(
        videoId: String,
        profile: ClientProfile,
        preferHighQuality: Bool
    ) async throws -> ResolvedStream? {
        var clientDict: [String: Any] = [
            "clientName": profile.name,
            "clientVersion": profile.version,
            "hl": "en",
            "gl": "US",
            "utcOffsetMinutes": 0
        ]
        for (k, v) in profile.extraFields {
            clientDict[k] = v
        }
        if !cachedVisitorData.isEmpty {
            clientDict["visitorData"] = cachedVisitorData
        }

        let endpointURLString: String
        let body: [String: Any]

        if profile.endpoint == "reel" {
            let querySuffix = cachedApiKey.isEmpty ? "prettyPrint=false" : "key=\(cachedApiKey)&prettyPrint=false"
            endpointURLString = "https://www.youtube.com/youtubei/v1/reel/reel_item_watch?\(querySuffix)"
            body = [
                "context": ["client": clientDict],
                "playerRequest": [
                    "videoId": videoId,
                    "contentCheckOk": true,
                    "racyCheckOk": true
                ],
                "disablePlayerResponse": false
            ]
        } else {
            let querySuffix = cachedApiKey.isEmpty ? "prettyPrint=false" : "key=\(cachedApiKey)&prettyPrint=false"
            endpointURLString = "https://www.youtube.com/youtubei/v1/player?\(querySuffix)"
            body = [
                "context": ["client": clientDict],
                "videoId": videoId,
                "contentCheckOk": true,
                "racyCheckOk": true
            ]
        }

        guard let url = URL(string: endpointURLString) else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(profile.userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue(profile.headerId, forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(profile.version, forHTTPHeaderField: "X-YouTube-Client-Version")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        let playerRoot = (root["playerResponse"] as? [String: Any]) ?? root
        if let status = (playerRoot["playabilityStatus"] as? [String: Any])?["status"] as? String,
           status != "OK" {
            return nil
        }

        guard let streamingData = playerRoot["streamingData"] as? [String: Any] else {
            return nil
        }

        let headers = ["User-Agent": profile.userAgent]

        let adaptive = (streamingData["adaptiveFormats"] as? [[String: Any]]) ?? []
        let muxed = (streamingData["formats"] as? [[String: Any]]) ?? []

        var mp4AudioCandidates: [(url: URL, bitrate: Int, badge: String)] = []
        for format in adaptive {
            guard let urlStr = format["url"] as? String,
                  let streamURL = URL(string: urlStr),
                  let mime = (format["mimeType"] as? String)?.lowercased() else {
                continue
            }
            if mime.contains("audio/mp4") || mime.contains("audio/m4a") || mime.contains("mp4a") {
                if !mime.contains("webm") && !mime.contains("opus") {
                    let bps = (format["averageBitrate"] as? Int) ?? (format["bitrate"] as? Int) ?? 128_000
                    let kbps = max(64, bps / 1000)
                    let badge = kbps >= 200 ? "AAC \(kbps) kbps" : "AAC"
                    mp4AudioCandidates.append((streamURL, kbps, badge))
                }
            }
        }

        if !mp4AudioCandidates.isEmpty {
            let sorted = mp4AudioCandidates.sorted {
                preferHighQuality ? ($0.bitrate > $1.bitrate) : ($0.bitrate < $1.bitrate)
            }
            if let best = sorted.first {
                return ResolvedStream(
                    url: best.url,
                    headers: headers,
                    codecBadge: best.badge,
                    bitrateKbps: best.bitrate,
                    isDirectFile: true
                )
            }
        }

        if let hlsStr = streamingData["hlsManifestUrl"] as? String,
           let hlsURL = URL(string: hlsStr) {
            return ResolvedStream(
                url: hlsURL,
                headers: headers,
                codecBadge: "HLS AAC",
                bitrateKbps: preferHighQuality ? 256 : 128,
                isDirectFile: false
            )
        }

        for format in muxed {
            guard let urlStr = format["url"] as? String,
                  let streamURL = URL(string: urlStr),
                  let mime = (format["mimeType"] as? String)?.lowercased(),
                  mime.contains("mp4"),
                  !mime.contains("webm") else {
                continue
            }
            let bps = (format["averageBitrate"] as? Int) ?? (format["bitrate"] as? Int) ?? 128_000
            let kbps = max(96, bps / 1000)
            return ResolvedStream(
                url: streamURL,
                headers: headers,
                codecBadge: "MP4 AAC",
                bitrateKbps: kbps,
                isDirectFile: true
            )
        }

        return nil
    }

    private func resolveViaPipedFallback(videoId: String, preferHighQuality: Bool) async throws -> ResolvedStream? {
        let instances = [
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.moomoo.me"
        ]
        for base in instances {
            guard let url = URL(string: "\(base)/streams/\(videoId)") else { continue }
            var req = URLRequest(url: url)
            req.timeoutInterval = 8
            req.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
            guard let (data, resp) = try? await session.data(for: req),
                  let http = resp as? HTTPURLResponse,
                  http.statusCode == 200,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                continue
            }

            if let audioStreams = json["audioStreams"] as? [[String: Any]] {
                let m4aStreams = audioStreams.compactMap { item -> (URL, Int)? in
                    let mime = ((item["mimeType"] as? String) ?? "").lowercased()
                    let format = ((item["format"] as? String) ?? "").lowercased()
                    guard (mime.contains("mp4") || mime.contains("m4a") || format.contains("m4a")),
                          !mime.contains("webm"),
                          let uStr = item["url"] as? String,
                          let u = URL(string: uStr) else {
                        return nil
                    }
                    let bps = (item["bitrate"] as? Int) ?? 128_000
                    return (u, max(64, bps / 1000))
                }
                let sorted = m4aStreams.sorted { preferHighQuality ? ($0.1 > $1.1) : ($0.1 < $1.1) }
                if let pick = sorted.first {
                    return ResolvedStream(
                        url: pick.0,
                        headers: ["User-Agent": webUserAgent],
                        codecBadge: "AAC \(pick.1) kbps",
                        bitrateKbps: pick.1,
                        isDirectFile: true
                    )
                }
            }

            if let hlsStr = json["hls"] as? String, let hlsURL = URL(string: hlsStr) {
                return ResolvedStream(
                    url: hlsURL,
                    headers: ["User-Agent": webUserAgent],
                    codecBadge: "HLS AAC",
                    bitrateKbps: 192,
                    isDirectFile: false
                )
            }
        }
        return nil
    }

    private func performWebRemixSearch(query: String, params: String?) async throws -> [Track] {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/search?prettyPrint=false") else {
            return []
        }

        var body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": "en",
                    "gl": "US"
                ]
            ],
            "query": query
        ]
        if let params = params {
            body["params"] = params
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
        request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
        request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await session.data(for: request)
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }

        var collected: [Track] = []
        collectTracksFromJSON(json, into: &collected)
        return collected
    }

    private func collectTracksFromJSON(_ node: Any, into results: inout [Track]) {
        if let dict = node as? [String: Any] {
            if let renderer = dict["musicResponsiveListItemRenderer"] as? [String: Any],
               let track = parseResponsiveItem(renderer) {
                results.append(track)
                return
            }
            if let panel = dict["playlistPanelVideoRenderer"] as? [String: Any],
               let track = parsePanelVideoItem(panel) {
                results.append(track)
                return
            }
            for (_, value) in dict {
                collectTracksFromJSON(value, into: &results)
            }
        } else if let array = node as? [Any] {
            for element in array {
                collectTracksFromJSON(element, into: &results)
            }
        }
    }

    private func parseResponsiveItem(_ renderer: [String: Any]) -> Track? {
        var videoId: String? = (renderer["playlistItemData"] as? [String: Any])?["videoId"] as? String
        if videoId == nil {
            videoId = findFirstValue(forKey: "videoId", in: renderer) as? String
        }
        guard let vid = videoId, vid.count == 11 else { return nil }

        guard let flexColumns = renderer["flexColumns"] as? [[String: Any]], !flexColumns.isEmpty else {
            return nil
        }

        let titleRuns = extractRuns(from: flexColumns[0])
        let title = titleRuns.first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !title.isEmpty else { return nil }

        var subtitleTokens: [String] = []
        if flexColumns.count > 1 {
            for col in flexColumns.dropFirst() {
                subtitleTokens.append(contentsOf: extractRuns(from: col))
            }
        }

        let ignoredTokens: Set<String> = ["•", "·", "&", "song", "video"]
        var filtered: [String] = []
        for rawToken in subtitleTokens {
            let trimmedToken = rawToken.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmedToken.isEmpty && !ignoredTokens.contains(trimmedToken.lowercased()) {
                filtered.append(trimmedToken)
            }
        }

        var durationSec = 0
        var metaParts: [String] = []
        for token in filtered {
            if let parsedDur = parseDurationToken(token), parsedDur > 0 {
                durationSec = parsedDur
            } else {
                metaParts.append(token)
            }
        }

        if durationSec == 0, let fixedCols = renderer["fixedColumns"] as? [[String: Any]] {
            for col in fixedCols {
                for run in extractRuns(from: col) {
                    if let parsedDur = parseDurationToken(run) {
                        durationSec = parsedDur
                        break
                    }
                }
            }
        }

        let artist = metaParts.first ?? "Unknown Artist"
        let album = metaParts.count >= 2 ? metaParts[1] : ""

        let rawThumb = extractBestThumbnail(from: renderer)
        let smallThumb = rawThumb.isEmpty ? "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg" : rawThumb
        let largeThumb = upgradeArtworkURL(smallThumb, videoId: vid)

        return Track(
            id: "yt:\(vid)",
            title: title,
            artist: artist,
            album: album,
            durationSec: durationSec,
            thumbnailURL: smallThumb,
            largeThumbnailURL: largeThumb,
            source: .youtubeMusic,
            codecBadge: "AAC",
            bitrateKbps: 256
        )
    }

    private func parsePanelVideoItem(_ panel: [String: Any]) -> Track? {
        guard let vid = panel["videoId"] as? String, vid.count == 11 else { return nil }
        let title = extractText(from: panel["title"]).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else { return nil }
        let artist = extractText(from: panel["shortBylineText"]).trimmingCharacters(in: .whitespacesAndNewlines)
        let lengthStr = extractText(from: panel["lengthText"])
        let durationSec = parseDurationToken(lengthStr) ?? 0
        let rawThumb = extractBestThumbnail(from: panel)
        let smallThumb = rawThumb.isEmpty ? "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg" : rawThumb
        let largeThumb = upgradeArtworkURL(smallThumb, videoId: vid)

        return Track(
            id: "yt:\(vid)",
            title: title,
            artist: artist.isEmpty ? "Unknown Artist" : artist,
            album: "",
            durationSec: durationSec,
            thumbnailURL: smallThumb,
            largeThumbnailURL: largeThumb,
            source: .youtubeMusic,
            codecBadge: "AAC",
            bitrateKbps: 256
        )
    }

    private func extractRuns(from container: [String: Any]) -> [String] {
        var out: [String] = []
        if let textDict = (container["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any]
            ?? (container["musicResponsiveListItemFixedColumnRenderer"] as? [String: Any])?["text"] as? [String: Any] {
            if let runs = textDict["runs"] as? [[String: Any]] {
                for r in runs {
                    if let t = r["text"] as? String {
                        out.append(t)
                    }
                }
            }
        }
        return out
    }

    private func extractText(from value: Any?) -> String {
        guard let dict = value as? [String: Any] else { return "" }
        if let simple = dict["simpleText"] as? String { return simple }
        if let runs = dict["runs"] as? [[String: Any]] {
            return runs.compactMap { $0["text"] as? String }.joined()
        }
        return ""
    }

    private func extractBestThumbnail(from dict: [String: Any]) -> String {
        if let thumbs = findFirstValue(forKey: "thumbnails", in: dict) as? [[String: Any]],
           let last = thumbs.last,
           let url = last["url"] as? String {
            return url
        }
        return ""
    }

    private func upgradeArtworkURL(_ url: String, videoId: String) -> String {
        if url.contains("googleusercontent.com") {
            if let range = url.range(of: #"=w\d+-h\d+[^/]*$"#, options: .regularExpression) {
                return url.replacingCharacters(in: range, with: "=w600-h600-l90-rj")
            }
            return url
        }
        return "https://i.ytimg.com/vi/\(videoId)/hqdefault.jpg"
    }

    private func findFirstValue(forKey key: String, in node: Any) -> Any? {
        if let dict = node as? [String: Any] {
            if let hit = dict[key] { return hit }
            for (_, v) in dict {
                if let found = findFirstValue(forKey: key, in: v) { return found }
            }
        } else if let arr = node as? [Any] {
            for item in arr {
                if let found = findFirstValue(forKey: key, in: item) { return found }
            }
        }
        return nil
    }

    private func parseDurationToken(_ text: String) -> Int? {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let parts = clean.split(separator: ":")
        guard parts.count == 2 || parts.count == 3 else { return nil }
        var nums: [Int] = []
        for p in parts {
            guard let n = Int(p) else { return nil }
            nums.append(n)
        }
        if nums.count == 2 {
            return nums[0] * 60 + nums[1]
        }
        return nums[0] * 3600 + nums[1] * 60 + nums[2]
    }

    private func firstRegexMatch(in text: String, pattern: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        guard let match = regex.firstMatch(in: text, range: range),
              match.numberOfRanges >= 2,
              let captureRange = Range(match.range(at: 1), in: text) else {
            return nil
        }
        return String(text[captureRange])
    }
}
