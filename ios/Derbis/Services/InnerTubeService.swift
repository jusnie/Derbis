import Foundation

actor InnerTubeService {
    static let shared = InnerTubeService()

    private let session: URLSession
    private let probeSession: URLSession

    private var cachedWebVisitorData: String = ""
    private var cachedReelVisitorData: String = ""
    private var cachedApiKey: String = ""
    private var lastWebContextFetch: Date = .distantPast
    private var lastReelVisitorFetch: Date = .distantPast

    private var resolvedCache: [String: [ResolvedStream]] = [:]
    private var quarantinedURLs: [String: Date] = [:]
    private var quarantinedClients: [String: Date] = [:]
    private var requestSequence: Int = 1

    private let webRemixVersion = "1.20260804.16.00"
    private let androidReelVersion = "21.03.36"
    private let webUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
    private let nonceAlphabet = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_")

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 12
        config.timeoutIntervalForResource = 24
        self.session = URLSession(configuration: config)

        let probeConfig = URLSessionConfiguration.ephemeral
        probeConfig.timeoutIntervalForRequest = 4.5
        probeConfig.timeoutIntervalForResource = 6.0
        self.probeSession = URLSession(configuration: probeConfig)
    }

    private var effectiveCountryCode: String {
        if let code = Locale.current.region?.identifier,
           code.count == 2 {
            return code.uppercased()
        }
        return "US"
    }

    private var effectiveLanguageCode: String {
        if let code = Locale.current.language.languageCode?.identifier,
           !code.isEmpty {
            return code.lowercased()
        }
        return "en"
    }

    func quarantineStream(url: URL, videoId: String?, clientLabel: String?) {
        let cleanKey = canonicalURLKey(url)
        quarantinedURLs[cleanKey] = Date().addingTimeInterval(600)
        if let vid = videoId {
            for key in resolvedCache.keys where key.hasPrefix("\(vid)|") {
                if let list = resolvedCache[key] {
                    let remaining = list.filter { canonicalURLKey($0.url) != cleanKey }
                    if remaining.isEmpty {
                        resolvedCache.removeValue(forKey: key)
                    } else {
                        resolvedCache[key] = remaining
                    }
                }
            }
        }
        if let client = clientLabel, !client.isEmpty, client != "Direct" {
            quarantinedClients[client] = Date().addingTimeInterval(180)
        }
    }

    func invalidateVideoCache(videoId: String) {
        for key in resolvedCache.keys where key.hasPrefix("\(videoId)|") {
            resolvedCache.removeValue(forKey: key)
        }
    }

    func searchTracks(query: String) async throws -> [Track] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }

        async let songsFetch = performWebRemixSearch(query: trimmed, params: "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        async let generalFetch = performWebRemixSearch(query: trimmed, params: nil)

        let songs = (try? await songsFetch) ?? []
        let general = (try? await generalFetch) ?? []

        var seen = Set<String>()
        var combined: [Track] = []
        for track in (songs + general) {
            if seen.insert(track.id).inserted {
                combined.append(track)
            }
        }
        if combined.isEmpty,
           let androidFallback = try? await performAndroidMusicSearchFallback(query: trimmed) {
            for track in androidFallback where seen.insert(track.id).inserted {
                combined.append(track)
            }
        }
        return Array(combined.prefix(40))
    }

    private func performAndroidMusicSearchFallback(query: String) async throws -> [Track] {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/search?prettyPrint=false") else {
            return []
        }
        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "ANDROID_MUSIC",
                    "clientVersion": "8.10.52",
                    "androidSdkVersion": 35,
                    "osName": "Android",
                    "osVersion": "15",
                    "hl": effectiveLanguageCode,
                    "gl": effectiveCountryCode
                ]
            ],
            "query": query
        ]
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15) gzip", forHTTPHeaderField: "User-Agent")
        request.setValue("21", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue("8.10.52", forHTTPHeaderField: "X-YouTube-Client-Version")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await session.data(for: request)
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }
        var collected: [Track] = []
        collectTracksFromJSON(json, into: &collected)
        return collected
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
        var personalShelves: [HomeShelf] = []
        if mood == "All",
           let authHeaders = GoogleAccountSyncService.currentAuthenticatedHeaders(requirePersonalizedHomeToggle: true) {
            personalShelves = await fetchPersonalizedBrowseHomeShelves(authHeaders: authHeaders)
        }

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
            let discovery = indexed.sorted(by: { $0.0 < $1.0 }).map(\.1)
            if !personalShelves.isEmpty {
                return personalShelves + discovery
            }
            return discovery
        }
    }

    private func fetchPersonalizedBrowseHomeShelves(authHeaders: [String: String]) async -> [HomeShelf] {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false") else {
            return []
        }
        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": effectiveLanguageCode,
                    "gl": effectiveCountryCode
                ],
                "user": ["lockedSafetyMode": false]
            ],
            "browseId": "FEmusic_home"
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
        for (k, v) in authHeaders {
            request.setValue(v, forHTTPHeaderField: k)
        }
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await session.data(for: request),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }

        var rawShelves: [[String: Any]] = []
        collectNamedDicts("musicCarouselShelfRenderer", in: json, into: &rawShelves)
        collectNamedDicts("musicShelfRenderer", in: json, into: &rawShelves)

        var built: [HomeShelf] = []
        for (idx, shelfDict) in rawShelves.enumerated() {
            var tracks: [Track] = []
            collectTracksFromJSON(shelfDict["contents"] ?? [:], into: &tracks)
            var seen = Set<String>()
            let unique = tracks.filter { seen.insert($0.id).inserted }
            guard unique.count >= 3 else { continue }

            let basicHeader = (shelfDict["header"] as? [String: Any])?["musicCarouselShelfBasicHeaderRenderer"] as? [String: Any]
            let rawTitle = extractText(from: basicHeader?["title"] ?? shelfDict["title"])
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let rawSub = extractText(from: basicHeader?["strapline"])
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let shelfTitle = rawTitle.isEmpty ? "For You" : rawTitle
            let shelfSub = rawSub.isEmpty ? "Personalized from your Google account" : rawSub

            built.append(
                HomeShelf(
                    id: "personal-\(idx)-\(shelfTitle.lowercased().replacingOccurrences(of: " ", with: "-"))",
                    title: shelfTitle,
                    subtitle: shelfSub,
                    tracks: Array(unique.prefix(14))
                )
            )
            if built.count >= 4 { break }
        }
        return built
    }

    private func collectNamedDicts(_ key: String, in node: Any, depth: Int = 0, into out: inout [[String: Any]]) {
        guard depth <= 40 else { return }
        if let dict = node as? [String: Any] {
            for (k, v) in dict {
                if k == key, let matched = v as? [String: Any] {
                    out.append(matched)
                }
                collectNamedDicts(key, in: v, depth: depth + 1, into: &out)
            }
        } else if let arr = node as? [Any] {
            for el in arr {
                collectNamedDicts(key, in: el, depth: depth + 1, into: &out)
            }
        }
    }

    func fetchRadioQueue(seedVideoId: String) async -> [Track] {
        let placeholder = Track(
            id: "yt:\(seedVideoId)",
            title: "",
            artist: "",
            album: "",
            durationSec: 0,
            thumbnailURL: "",
            source: .youtubeMusic
        )
        if let page = await fetchRadioPage(seed: placeholder), !page.tracks.isEmpty {
            return page.tracks
        }
        return []
    }

    func fetchRadioPage(seed: Track) async -> InnerTubeRadioPage? {
        let resolvedSeedVid: String? = await resolveRadioSeedVideoId(for: seed)
        guard let seedVideoId = resolvedSeedVid, !seedVideoId.isEmpty else {
            return nil
        }
        let defaultRadioId = "RDAMVM\(seedVideoId)"

        guard let initialJSON = await postWatchNextJSON(
            videoId: seedVideoId,
            playlistId: defaultRadioId,
            params: "wAEB",
            continuation: nil
        ) else {
            return nil
        }

        var workingJSON = initialJSON
        var resolvedRadioId = extractPlaylistId(from: initialJSON)
        if resolvedRadioId.isEmpty { resolvedRadioId = defaultRadioId }

        var initialTracks = extractRadioTracks(from: initialJSON, excludingVideoId: seedVideoId)
        if initialTracks.count <= 2, let automix = extractAutomixEndpoint(from: initialJSON) {
            let handoffPlaylist = automix.playlistId.isEmpty ? resolvedRadioId : automix.playlistId
            let handoffVid = automix.videoId.isEmpty ? seedVideoId : automix.videoId
            let handoffParams = automix.params.isEmpty ? "wAEB" : automix.params
            if let handoffJSON = await postWatchNextJSON(
                videoId: handoffVid,
                playlistId: handoffPlaylist,
                params: handoffParams,
                continuation: nil
            ) {
                let handoffTracks = extractRadioTracks(from: handoffJSON, excludingVideoId: seedVideoId)
                if !handoffTracks.isEmpty {
                    workingJSON = handoffJSON
                    initialTracks = handoffTracks
                    let nextPid = extractPlaylistId(from: handoffJSON)
                    resolvedRadioId = nextPid.isEmpty ? handoffPlaylist : nextPid
                }
            }
        }

        let continuation = extractWatchContinuation(from: workingJSON)
        guard !initialTracks.isEmpty || !continuation.isEmpty else {
            return nil
        }
        return InnerTubeRadioPage(
            radioId: resolvedRadioId,
            tracks: initialTracks,
            continuation: continuation
        )
    }

    func fetchRadioContinuationPage(
        seed: Track,
        radioId: String,
        continuation: String
    ) async -> InnerTubeRadioPage? {
        let cleanRadioId = radioId.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanToken = continuation.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanRadioId.isEmpty, !cleanToken.isEmpty else { return nil }

        guard let json = await postWatchNextJSON(
            videoId: seed.videoId,
            playlistId: cleanRadioId,
            params: "wAEB",
            continuation: cleanToken
        ) else {
            return nil
        }

        let tracks = extractRadioTracks(from: json, excludingVideoId: seed.videoId)
        let nextToken = extractWatchContinuation(from: json)
        let nextPid = extractPlaylistId(from: json)
        return InnerTubeRadioPage(
            radioId: nextPid.isEmpty ? cleanRadioId : nextPid,
            tracks: tracks,
            continuation: nextToken
        )
    }

    func fetchFallbackRadioTracks(
        seed: Track,
        excludeIds: Set<String>,
        limit: Int = 18
    ) async -> [Track] {
        let cleanArtist = seed.artist.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanTitle = seed.title.trimmingCharacters(in: .whitespacesAndNewlines)
        var queries: [String] = []
        if !cleanTitle.isEmpty && !cleanArtist.isEmpty && cleanArtist.lowercased() != "unknown artist" {
            queries.append("\(cleanTitle) \(cleanArtist) mix")
            queries.append("songs like \(cleanTitle) \(cleanArtist)")
        }
        if !cleanArtist.isEmpty && cleanArtist.lowercased() != "unknown artist" {
            queries.append("\(cleanArtist) best songs")
        }
        if queries.isEmpty {
            queries.append("trending chill hits")
        }

        return await withTaskGroup(of: [Track].self) { group -> [Track] in
            for q in queries {
                group.addTask {
                    (try? await self.searchTracks(query: q)) ?? []
                }
            }
            var combined: [Track] = []
            var seen = excludeIds
            seen.insert(seed.id)
            if let vid = seed.videoId {
                seen.insert(vid)
                seen.insert("yt:\(vid)")
            }
            for await batch in group {
                for item in batch {
                    if seen.insert(item.id).inserted {
                        combined.append(item)
                    }
                }
            }
            return Array(combined.prefix(max(limit, 8)))
        }
    }

    // MARK: - Artist browse (the artist page /browse endpoint, mirrors Android's ArtistRepository)

    /// Shared: walk any subtree and return the largest thumbnails-array URL found.
    private func largestThumbnailURL(in node: Any?) -> String {
        var best = ""
        var bestArea: Int64 = -1
        walkThumbnails(node, best: &best, bestArea: &bestArea)
        return best
    }

    private func walkThumbnails(_ node: Any?, best: inout String, bestArea: inout Int64) {
        if let dict = node as? [String: Any] {
            if let thumbnails = dict["thumbnails"] as? [[String: Any]] {
                for item in thumbnails {
                    guard let url = (item["url"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                          !url.isEmpty else { continue }
                    let width = (item["width"] as? NSNumber)?.int64Value ?? 0
                    let height = (item["height"] as? NSNumber)?.int64Value ?? 0
                    let area = width * height
                    if area >= bestArea {
                        bestArea = area
                        best = url
                    }
                }
            }
            for (_, value) in dict {
                walkThumbnails(value, best: &best, bestArea: &bestArea)
            }
        } else if let array = node as? [Any] {
            for element in array {
                walkThumbnails(element, best: &best, bestArea: &bestArea)
            }
        }
    }

    /// Shared: gather browseEndpoint dictionaries anywhere in a subtree.
    private func collectBrowseEndpoints(in node: Any, depth: Int = 0, into out: inout [[String: Any]]) {
        guard depth <= 32 else { return }
        if let dict = node as? [String: Any] {
            if let endpoint = dict["browseEndpoint"] as? [String: Any] {
                out.append(endpoint)
            }
            for (_, value) in dict {
                collectBrowseEndpoints(in: value, depth: depth + 1, into: &out)
            }
        } else if let array = node as? [Any] {
            for element in array {
                collectBrowseEndpoints(in: element, depth: depth + 1, into: &out)
            }
        }
    }

    /// Artist-only search (the same filter param Android uses) to resolve the browseId.
    private func resolveArtistBrowseId(artistName: String) async -> (browseId: String, name: String)? {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/search?prettyPrint=false") else {
            return nil
        }

        let hl = effectiveLanguageCode
        let gl = effectiveCountryCode
        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": hl,
                    "gl": gl
                ]
            ],
            "query": artistName,
            "params": "EgWKAQIgAWoMEAMQBBAJEAoQBRAV"
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
        request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
        request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, response) = try? await session.data(for: request),
              let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        var renderers: [[String: Any]] = []
        collectNamedDicts("musicResponsiveListItemRenderer", in: json, into: &renderers)

        struct ArtistCandidate {
            let name: String
            let browseId: String
        }

        var candidates: [ArtistCandidate] = []
        for renderer in renderers {
            guard let flexColumns = renderer["flexColumns"] as? [[String: Any]], !flexColumns.isEmpty else {
                continue
            }
            let name = extractRuns(from: flexColumns[0])
                .joined()
                .trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { continue }

            var endpoints: [[String: Any]] = []
            collectBrowseEndpoints(in: renderer, into: &endpoints)
            let preferred = endpoints.first { endpoint in
                let pageType = ((endpoint["browseEndpointContextSupportedConfigs"] as? [String: Any])?["browseEndpointContextMusicConfig"] as? [String: Any])?["pageType"] as? String
                return (pageType ?? "").localizedCaseInsensitiveContains("ARTIST")
            }
            let chosen = preferred ?? endpoints.first { endpoint in
                (endpoint["browseId"] as? String)?.hasPrefix("UC") == true
            }
            guard let browseId = (chosen?["browseId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !browseId.isEmpty else { continue }
            candidates.append(ArtistCandidate(name: name, browseId: browseId))
        }
        guard !candidates.isEmpty else { return nil }

        let queryNorm = ArtistLoreService.normalizeName(artistName)
        func score(_ candidate: ArtistCandidate) -> Int {
            let norm = ArtistLoreService.normalizeName(candidate.name)
            if norm == queryNorm { return 110 }
            let head = norm.components(separatedBy: " (").first ?? norm
            if head == queryNorm { return 100 }
            if norm.hasPrefix(queryNorm + " ") || norm.contains(queryNorm) || queryNorm.contains(norm) { return 70 }
            let queryTokens = Set(queryNorm.split(separator: " "))
            let candidateTokens = Set(norm.split(separator: " "))
            guard !queryTokens.isEmpty else { return 0 }
            let overlap = Double(queryTokens.intersection(candidateTokens).count)
            return Int((overlap / Double(queryTokens.count)) * 55)
        }
        let best = candidates.max { score($0) < score($1) }
        guard let winner = best, score(winner) >= 40 else { return nil }
        return (winner.browseId, winner.name)
    }

    private func postArtistBrowse(browseId: String) async -> [String: Any]? {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false") else {
            return nil
        }
        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": effectiveLanguageCode,
                    "gl": effectiveCountryCode
                ]
            ],
            "browseId": browseId
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
        request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
        request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, response) = try? await session.data(for: request),
              let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            return nil
        }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    private static let browseReleaseYearRegex = #"\b((?:19|20)\d{2})\b"#

    /// Release cards (musicTwoRowItemRenderer) with an MPRE browse id — the same filter
    /// Android uses. Release-type badges are intentionally dropped on iOS: the glass page
    /// shows "year · artist" only, and the search-grouped fallback carries no type either.
    private func browseReleases(_ root: [String: Any]) -> [AlbumSummaryCard] {
        var cards: [[String: Any]] = []
        collectNamedDicts("musicTwoRowItemRenderer", in: root, into: &cards)

        var releases: [AlbumSummaryCard] = []
        var seen = Set<String>()
        for card in cards {
            let title = extractText(from: card["title"]).trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty else { continue }
            let subtitle = extractText(from: card["subtitle"]).trimmingCharacters(in: .whitespacesAndNewlines)
            var browseId = ""
            if let endpoint = (card["navigationEndpoint"] as? [String: Any])?["browseEndpoint"] as? [String: Any] {
                browseId = (endpoint["browseId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            }
            guard browseId.hasPrefix("MPRE") else { continue }
            let key = browseId.lowercased()
            guard seen.insert(key).inserted else { continue }
            let year = firstRegexMatch(in: subtitle, pattern: Self.browseReleaseYearRegex) ?? ""
            let artwork = largestThumbnailURL(in: card)
            releases.append(
                AlbumSummaryCard(
                    id: browseId,
                    title: title,
                    artist: "",
                    year: year,
                    artworkURL: artwork,
                    seedTracks: []
                )
            )
        }
        return releases
    }

    /// Related artists: two-row cards whose browseEndpoint page type is ARTIST.
    private func browseRelatedArtists(_ root: [String: Any], selfName: String) -> [String] {
        var cards: [[String: Any]] = []
        collectNamedDicts("musicTwoRowItemRenderer", in: root, into: &cards)

        let selfNorm = ArtistLoreService.normalizeName(selfName)
        var names: [String] = []
        var seen = Set<String>()
        for card in cards {
            guard let endpoint = (card["navigationEndpoint"] as? [String: Any])?["browseEndpoint"] as? [String: Any] else {
                continue
            }
            let pageType = ((endpoint["browseEndpointContextSupportedConfigs"] as? [String: Any])?["browseEndpointContextMusicConfig"] as? [String: Any])?["pageType"] as? String ?? ""
            guard pageType.localizedCaseInsensitiveContains("ARTIST") else { continue }
            let name = extractText(from: card["title"]).trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { continue }
            let norm = ArtistLoreService.normalizeName(name)
            guard norm != selfNorm, seen.insert(norm).inserted else { continue }
            names.append(name)
            if names.count >= 8 { break }
        }
        return names
    }

    private func browseHeaderArtwork(_ header: [String: Any]) -> (portrait: String, banner: String) {
        let renderers: [[String: Any]] = [
            (header["musicImmersiveHeaderRenderer"] as? [String: Any]),
            (header["musicVisualHeaderRenderer"] as? [String: Any]),
            (header["musicDetailHeaderRenderer"] as? [String: Any]),
            (header["musicResponsiveHeaderRenderer"] as? [String: Any])
        ].compactMap { $0 }

        func portraitURL(_ renderer: [String: Any]) -> String {
            largestThumbnailURL(in: renderer["thumbnail"] ?? renderer["foregroundThumbnail"])
        }
        func bannerURL(_ renderer: [String: Any]) -> String {
            largestThumbnailURL(in: renderer["backgroundThumbnail"])
        }

        let portrait = renderers.map(portraitURL).first { !$0.isEmpty } ?? ""
        let banner = renderers.map(bannerURL).first { !$0.isEmpty } ?? ""
        return (portrait, banner)
    }

    private func browseHeaderTitle(_ header: [String: Any]) -> String {
        let renderers: [[String: Any]] = [
            (header["musicImmersiveHeaderRenderer"] as? [String: Any]),
            (header["musicVisualHeaderRenderer"] as? [String: Any]),
            (header["musicDetailHeaderRenderer"] as? [String: Any]),
            (header["musicResponsiveHeaderRenderer"] as? [String: Any])
        ].compactMap { $0 }
        for renderer in renderers {
            let title = extractText(from: renderer["title"]).trimmingCharacters(in: .whitespacesAndNewlines)
            if !title.isEmpty { return title }
        }
        return ""
    }

    private func browseHeaderSubscribers(_ header: [String: Any]) -> String {
        // Android reads subscriptionButton directly off the top-level header; some surfaces
        // nest it under the specific renderer instead, so check both.
        var nodes: [[String: Any]] = [header]
        for key in ["musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicDetailHeaderRenderer", "musicResponsiveHeaderRenderer", "musicHeaderRenderer"] {
            if let renderer = header[key] as? [String: Any] { nodes.append(renderer) }
        }
        for node in nodes {
            let text = extractText(
                from: ((node["subscriptionButton"] as? [String: Any])?["subscribeButtonRenderer"] as? [String: Any])?["subscriberCountText"]
            ).trimmingCharacters(in: .whitespacesAndNewlines)
            if !text.isEmpty { return text }
        }
        return ""
    }

    private func browseHeaderMonthlyListeners(_ header: [String: Any]) -> String {
        for key in ["musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicDetailHeaderRenderer", "musicResponsiveHeaderRenderer", "musicHeaderRenderer"] {
            guard let renderer = header[key] as? [String: Any] else { continue }
            let text = extractText(from: renderer["monthlyListenerCount"]).trimmingCharacters(in: .whitespacesAndNewlines)
            if !text.isEmpty { return text }
        }
        return ""
    }

    /// The browse-first catalog. Returns nil when the artist can't be resolved to a page
    /// or the page yields nothing useful — the search fallback then takes over.
    private func browseArtistCatalog(artistName: String) async -> ArtistCatalogPayload? {
        guard let (browseId, resolvedName) = await resolveArtistBrowseId(artistName: artistName) else {
            return nil
        }
        guard let root = await postArtistBrowse(browseId: browseId) else {
            return nil
        }
        let header = root["header"] as? [String: Any] ?? [:]
        let artwork = browseHeaderArtwork(header)
        let headerTitle = browseHeaderTitle(header)
        let name = headerTitle.isEmpty ? resolvedName : headerTitle
        let subscribers = browseHeaderSubscribers(header)
        let monthlyListeners = browseHeaderMonthlyListeners(header)

        var songs: [Track] = []
        collectTracksFromJSON(root, into: &songs)
        var seen = Set<String>()
        let uniqueSongs = songs.filter { seen.insert($0.id).inserted }
        let topSongs = Array(uniqueSongs.prefix(20))

        var releases = browseReleases(root)
        for index in releases.indices {
            if releases[index].artist.isEmpty {
                releases[index] = AlbumSummaryCard(
                    id: releases[index].id,
                    title: releases[index].title,
                    artist: name,
                    year: releases[index].year,
                    artworkURL: releases[index].artworkURL,
                    seedTracks: releases[index].seedTracks
                )
            }
        }

        let related = browseRelatedArtists(root, selfName: name)
        let hero = !artwork.banner.isEmpty ? artwork.banner
            : (!artwork.portrait.isEmpty ? artwork.portrait
                : (topSongs.first?.displayArtworkURL?.absoluteString ?? topSongs.first?.thumbnailURL))

        guard !topSongs.isEmpty || !releases.isEmpty else { return nil }

        return ArtistCatalogPayload(
            artistName: name,
            heroArtworkURL: hero,
            topTracks: topSongs,
            albums: releases,
            relatedArtists: related,
            bannerURL: artwork.banner,
            subscribers: subscribers,
            monthlyListeners: monthlyListeners
        )
    }

    func fetchArtistCatalog(artistName: String) async -> ArtistCatalogPayload {
        let trimmed = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return ArtistCatalogPayload(artistName: artistName, heroArtworkURL: nil, topTracks: [], albums: [], relatedArtists: [])
        }

        // Browse-first: same artist /browse endpoint Android uses, with header facts and
        // release years from the shelves. Falls back to search-grouping when it fails.
        if let browsed = await browseArtistCatalog(artistName: trimmed) {
            return browsed
        }
        return await searchArtistCatalogFallback(artistName: trimmed)
    }

    private func searchArtistCatalogFallback(artistName: String) async -> ArtistCatalogPayload {
        let trimmed = artistName
        async let topBatch = (try? searchTracks(query: "\(trimmed) top songs")) ?? []
        async let deepBatch = (try? searchTracks(query: "\(trimmed) albums singles")) ?? []
        async let mixBatch = (try? searchTracks(query: "artists similar to \(trimmed) songs")) ?? []

        let (topRaw, deepRaw, similarRaw) = await (topBatch, deepBatch, mixBatch)
        let targetKey = EndlessTrackTextNormalizer.canonicalArtist(trimmed)

        var seenIds = Set<String>()
        var primaryTracks: [Track] = []
        var secondaryTracks: [Track] = []

        for t in topRaw + deepRaw {
            guard seenIds.insert(t.id).inserted else { continue }
            let itemArtistKey = EndlessTrackTextNormalizer.canonicalArtist(t.artist)
            if itemArtistKey == targetKey || t.artist.localizedCaseInsensitiveContains(trimmed) {
                primaryTracks.append(t)
            } else {
                secondaryTracks.append(t)
            }
        }

        let orderedTop = Array((primaryTracks.isEmpty ? secondaryTracks : primaryTracks).prefix(20))

        var albumGroups: [String: [Track]] = [:]
        var albumOrder: [String] = []
        for track in primaryTracks where !track.album.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let key = track.album.trimmingCharacters(in: .whitespacesAndNewlines)
            if albumGroups[key] == nil {
                albumOrder.append(key)
                albumGroups[key] = []
            }
            albumGroups[key]?.append(track)
        }

        var albumCards: [AlbumSummaryCard] = albumOrder.prefix(12).compactMap { albumTitle in
            guard let list = albumGroups[albumTitle], let first = list.first else { return nil }
            return AlbumSummaryCard(
                id: "\(trimmed.lowercased())|\(albumTitle.lowercased())",
                title: albumTitle,
                artist: trimmed,
                year: "",
                artworkURL: first.displayArtworkURL?.absoluteString ?? first.thumbnailURL,
                seedTracks: list
            )
        }

        if albumCards.isEmpty {
            for track in orderedTop.prefix(6) {
                albumCards.append(
                    AlbumSummaryCard(
                        id: "single|\(track.id)",
                        title: track.album.isEmpty ? "\(track.title) — Single" : track.album,
                        artist: trimmed,
                        year: "Single",
                        artworkURL: track.displayArtworkURL?.absoluteString ?? track.thumbnailURL,
                        seedTracks: [track]
                    )
                )
            }
        }

        var relatedArtists: [String] = []
        var seenArtistKeys: Set<String> = [targetKey]
        for t in similarRaw + secondaryTracks {
            let candidate = t.artist
                .components(separatedBy: CharacterSet(charactersIn: ",&"))
                .first?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let key = EndlessTrackTextNormalizer.canonicalArtist(candidate)
            guard !candidate.isEmpty,
                  candidate.lowercased() != "unknown artist",
                  seenArtistKeys.insert(key).inserted else { continue }
            relatedArtists.append(candidate)
            if relatedArtists.count >= 8 { break }
        }

        let heroURL = orderedTop.first?.displayArtworkURL?.absoluteString ?? orderedTop.first?.thumbnailURL
        return ArtistCatalogPayload(
            artistName: trimmed,
            heroArtworkURL: heroURL,
            topTracks: orderedTop,
            albums: albumCards,
            relatedArtists: relatedArtists
        )
    }

    func fetchAlbumCatalog(
        albumTitle: String,
        artistName: String,
        seedTracks: [Track] = []
    ) async -> AlbumCatalogPayload {
        let cleanAlbum = albumTitle.replacingOccurrences(of: " — Single", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanArtist = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        let query = cleanArtist.isEmpty ? "\(cleanAlbum) album" : "\(cleanAlbum) \(cleanArtist) album"
        let fetched = (try? await searchTracks(query: query)) ?? []

        var seen = Set<String>()
        var matched: [Track] = []
        for s in seedTracks where seen.insert(s.id).inserted {
            matched.append(s)
        }

        let normAlbum = EndlessTrackTextNormalizer.normalizedTitle(cleanAlbum)
        let normArtist = EndlessTrackTextNormalizer.canonicalArtist(cleanArtist)

        for item in fetched where seen.insert(item.id).inserted {
            let itemAlbumNorm = EndlessTrackTextNormalizer.normalizedTitle(item.album)
            let itemArtistNorm = EndlessTrackTextNormalizer.canonicalArtist(item.artist)
            if (!normAlbum.isEmpty && itemAlbumNorm == normAlbum) || (!normArtist.isEmpty && itemArtistNorm == normArtist) {
                matched.append(item)
            }
        }

        if matched.isEmpty {
            matched = Array(fetched.prefix(14))
        }

        let artwork = matched.first?.displayArtworkURL?.absoluteString ?? matched.first?.thumbnailURL
        return AlbumCatalogPayload(
            title: cleanAlbum.isEmpty ? "Release" : cleanAlbum,
            artistName: cleanArtist.isEmpty ? (matched.first?.artist ?? "Artist") : cleanArtist,
            artworkURL: artwork,
            year: "",
            tracks: Array(matched.prefix(24))
        )
    }

    private func resolveRadioSeedVideoId(for seed: Track) async -> String? {
        if let vid = seed.videoId, !vid.isEmpty {
            return vid
        }
        let q = "\(seed.title) \(seed.artist)".trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return nil }
        let hits = (try? await searchTracks(query: q)) ?? []
        return hits.first?.videoId
    }

    private func postWatchNextJSON(
        videoId: String?,
        playlistId: String,
        params: String,
        continuation: String?
    ) async -> [String: Any]? {
        var urlString = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
        if let token = continuation, !token.isEmpty,
           let encoded = token.addingPercentEncoding(withAllowedCharacters: .alphanumerics.union(CharacterSet(charactersIn: "-_."))) {
            urlString += "&ctoken=\(encoded)&continuation=\(encoded)"
        }
        guard let url = URL(string: urlString) else { return nil }

        let hl = effectiveLanguageCode
        let gl = effectiveCountryCode
        var body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": hl,
                    "gl": gl,
                    "platform": "DESKTOP"
                ],
                "user": ["lockedSafetyMode": false]
            ],
            "enablePersistentPlaylistPanel": true,
            "isAudioOnly": true,
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "playlistId": playlistId,
            "params": params
        ]
        if let vid = videoId, !vid.isEmpty {
            body["videoId"] = vid
        }
        if let token = continuation, !token.isEmpty {
            body["continuation"] = token
        }

        do {
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
            request.setValue("https://music.youtube.com/", forHTTPHeaderField: "Referer")
            request.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
            request.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
            request.setValue(webRemixVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
            if let authHeaders = GoogleAccountSyncService.currentAuthenticatedHeaders() {
                for (k, v) in authHeaders {
                    request.setValue(v, forHTTPHeaderField: k)
                }
            }
            request.httpBody = try JSONSerialization.data(withJSONObject: body)

            let (data, resp) = try await session.data(for: request)
            if let http = resp as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
                return nil
            }
            return try JSONSerialization.jsonObject(with: data) as? [String: Any]
        } catch {
            return nil
        }
    }

    private func extractRadioTracks(from json: [String: Any], excludingVideoId: String?) -> [Track] {
        var results: [Track] = []
        collectTracksFromJSON(json, into: &results)
        var seen = Set<String>()
        if let vid = excludingVideoId, !vid.isEmpty {
            seen.insert(vid)
            seen.insert("yt:\(vid)")
        }
        return results.filter { seen.insert($0.id).inserted }
    }

    private func extractAutomixEndpoint(from json: [String: Any]) -> (videoId: String, playlistId: String, params: String)? {
        var previews: [[String: Any]] = []
        collectNamedDicts("automixPreviewVideoRenderer", in: json, into: &previews)
        for preview in previews {
            guard let content = preview["content"] as? [String: Any],
                  let renderer = content["automixPlaylistVideoRenderer"] as? [String: Any],
                  let nav = renderer["navigationEndpoint"] as? [String: Any] else {
                continue
            }
            let watch = (nav["watchPlaylistEndpoint"] as? [String: Any]) ?? (nav["watchEndpoint"] as? [String: Any])
            guard let watch else { continue }
            let vid = (watch["videoId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let pid = (watch["playlistId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let prm = (watch["params"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !pid.isEmpty || !vid.isEmpty {
                return (vid, pid, prm)
            }
        }
        return nil
    }

    private func extractPlaylistId(from json: [String: Any]) -> String {
        var endpoints: [[String: Any]] = []
        collectNamedDicts("watchEndpoint", in: json, into: &endpoints)
        for ep in endpoints {
            if let pid = ep["playlistId"] as? String, !pid.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return pid.trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        return ""
    }

    private func extractWatchContinuation(from json: [String: Any]) -> String {
        let keys = [
            "nextRadioContinuationData",
            "nextContinuationData",
            "reloadContinuationData",
            "continuationCommand"
        ]
        for key in keys {
            var objects: [[String: Any]] = []
            collectNamedDicts(key, in: json, into: &objects)
            for obj in objects {
                let c1 = (obj["continuation"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                if !c1.isEmpty { return c1 }
                let c2 = (obj["token"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                if !c2.isEmpty { return c2 }
            }
        }
        return ""
    }

    func resolvePlayableStream(
        for track: Track,
        preferHighQuality: Bool,
        requireDirectFile: Bool = false,
        forceRefresh: Bool = false
    ) async throws -> ResolvedStream {
        let ladder = try await resolveStreamCandidates(
            for: track,
            preferHighQuality: preferHighQuality,
            requireDirectFile: requireDirectFile,
            forceRefresh: forceRefresh
        )
        guard let first = ladder.first else {
            throw URLError(.cannotDecodeRawData)
        }
        return first
    }

    func resolveStreamCandidates(
        for track: Track,
        preferHighQuality: Bool,
        requireDirectFile: Bool = false,
        forceRefresh: Bool = false
    ) async throws -> [ResolvedStream] {
        pruneExpiredQuarantines()
        guard let primaryVideoId = track.videoId, !primaryVideoId.isEmpty else {
            throw URLError(.badURL)
        }

        let cacheKey = "\(primaryVideoId)|\(preferHighQuality ? "hq" : "std")|\(requireDirectFile ? "direct" : "any")"
        if !forceRefresh, let cached = resolvedCache[cacheKey] {
            let valid = cached.filter { $0.isFresh && !isURLQuarantined($0.url) }
            if !valid.isEmpty {
                resolvedCache[cacheKey] = valid
                return valid
            }
            resolvedCache.removeValue(forKey: cacheKey)
        }

        let primaryCandidates = await resolveCandidatesForVideoId(
            videoId: primaryVideoId,
            preferHighQuality: preferHighQuality,
            requireDirectFile: requireDirectFile
        )
        if !primaryCandidates.isEmpty {
            resolvedCache[cacheKey] = primaryCandidates
            return primaryCandidates
        }

        if let alternateTrack = await findAlternateAudioTrack(for: track, excludingVideoId: primaryVideoId),
           let altVideoId = alternateTrack.videoId {
            let altCandidates = await resolveCandidatesForVideoId(
                videoId: altVideoId,
                preferHighQuality: preferHighQuality,
                requireDirectFile: requireDirectFile
            )
            if !altCandidates.isEmpty {
                resolvedCache[cacheKey] = altCandidates
                return altCandidates
            }
        }

        throw URLError(.cannotDecodeRawData)
    }

    private struct ClientProfile {
        let name: String
        let label: String
        let version: String
        let headerId: String
        let userAgent: String
        let extraFields: [String: Any]
        let preferHLSFirst: Bool
    }

    private func resolveCandidatesForVideoId(
        videoId: String,
        preferHighQuality: Bool,
        requireDirectFile: Bool
    ) async -> [ResolvedStream] {
        await refreshWatchContextIfNeeded(videoId: videoId)
        let gl = effectiveCountryCode
        let hl = effectiveLanguageCode

        var verified: [ResolvedStream] = []
        var seenURLs = Set<String>()

        if !isClientQuarantined("ANDROID_REEL") {
            if let reelStreams = try? await requestAndroidReelCandidates(
                videoId: videoId,
                hl: hl,
                gl: gl,
                preferHighQuality: preferHighQuality,
                requireDirectFile: requireDirectFile
            ) {
                for candidate in reelStreams {
                    let key = canonicalURLKey(candidate.url)
                    guard !seenURLs.contains(key), !isURLQuarantined(candidate.url) else { continue }
                    if let checked = await verifyAndAdaptStream(candidate) {
                        seenURLs.insert(canonicalURLKey(checked.url))
                        verified.append(checked)
                        if verified.count >= 2 { break }
                    }
                }
            }
        }

        let playerProfiles: [ClientProfile] = [
            ClientProfile(
                name: "VISIONOS",
                label: "VISIONOS",
                version: "1.04",
                headerId: "101",
                userAgent: "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; \(gl))",
                extraFields: [
                    "deviceMake": "Apple",
                    "deviceModel": "RealityDevice17,1",
                    "osName": "visionOS",
                    "osVersion": "26.6.0.23O770",
                    "platform": "MOBILE",
                    "clientScreen": "WATCH"
                ],
                preferHLSFirst: false
            ),
            ClientProfile(
                name: "IOS",
                label: "IOS",
                version: "20.10.4",
                headerId: "5",
                userAgent: "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)",
                extraFields: [
                    "deviceMake": "Apple",
                    "deviceModel": "iPhone16,2",
                    "osName": "iPhone",
                    "osVersion": "18.3",
                    "platform": "MOBILE"
                ],
                preferHLSFirst: true
            ),
            ClientProfile(
                name: "ANDROID_VR",
                label: "ANDROID_VR",
                version: "1.65.10",
                headerId: "28",
                userAgent: "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip",
                extraFields: [
                    "androidSdkVersion": 32,
                    "osName": "Android",
                    "osVersion": "12L",
                    "platform": "MOBILE",
                    "deviceMake": "Oculus",
                    "deviceModel": "Quest 3"
                ],
                preferHLSFirst: false
            ),
            ClientProfile(
                name: "ANDROID_MUSIC",
                label: "ANDROID_MUSIC",
                version: "8.10.52",
                headerId: "21",
                userAgent: "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15) gzip",
                extraFields: [
                    "androidSdkVersion": 35,
                    "osName": "Android",
                    "osVersion": "15",
                    "platform": "MOBILE"
                ],
                preferHLSFirst: false
            )
        ]

        for profile in playerProfiles {
            if !verified.isEmpty { break }
            if isClientQuarantined(profile.label) { continue }

            guard let rawCandidates = try? await requestPlayerCandidates(
                videoId: videoId,
                profile: profile,
                hl: hl,
                gl: gl,
                preferHighQuality: preferHighQuality,
                requireDirectFile: requireDirectFile
            ) else {
                continue
            }

            for candidate in rawCandidates {
                let key = canonicalURLKey(candidate.url)
                guard !seenURLs.contains(key), !isURLQuarantined(candidate.url) else { continue }
                if let checked = await verifyAndAdaptStream(candidate) {
                    seenURLs.insert(canonicalURLKey(checked.url))
                    verified.append(checked)
                    if verified.count >= 2 { break }
                }
            }
        }

        if verified.isEmpty, !isClientQuarantined("AUTH_TVHTML5") {
            if let authCandidates = try? await requestAuthenticatedPlayerCandidates(
                videoId: videoId,
                hl: hl,
                gl: gl,
                preferHighQuality: preferHighQuality,
                requireDirectFile: requireDirectFile
            ) {
                for candidate in authCandidates {
                    let key = canonicalURLKey(candidate.url)
                    guard !seenURLs.contains(key), !isURLQuarantined(candidate.url) else { continue }
                    if let checked = await verifyAndAdaptStream(candidate) {
                        seenURLs.insert(canonicalURLKey(checked.url))
                        verified.append(checked)
                        if verified.count >= 2 { break }
                    }
                }
            }
        }

        if verified.isEmpty {
            if let piped = await resolveViaExternalMirrors(
                videoId: videoId,
                preferHighQuality: preferHighQuality,
                requireDirectFile: requireDirectFile
            ) {
                for candidate in piped {
                    if let checked = await verifyAndAdaptStream(candidate) {
                        verified.append(checked)
                        if verified.count >= 2 { break }
                    }
                }
            }
        }

        return verified
    }

    private func buildAndroidReelContext(hl: String, gl: String, visitorData: String) -> [String: Any] {
        var client: [String: Any] = [
            "clientName": "ANDROID",
            "clientVersion": androidReelVersion,
            "clientScreen": "WATCH",
            "platform": "MOBILE",
            "osName": "Android",
            "osVersion": "16",
            "androidSdkVersion": 36,
            "hl": hl,
            "gl": gl,
            "utcOffsetMinutes": 0
        ]
        if !visitorData.isEmpty {
            client["visitorData"] = visitorData
        }
        return [
            "client": client,
            "request": [
                "internalExperimentFlags": [Any](),
                "useSsl": true
            ],
            "user": [
                "lockedSafetyMode": false
            ]
        ]
    }

    private func resolveAndroidReelVisitorData(hl: String, gl: String, userAgent: String) async -> String {
        if !cachedReelVisitorData.isEmpty && Date().timeIntervalSince(lastReelVisitorFetch) < 1800 {
            return cachedReelVisitorData
        }
        guard let url = URL(string: "https://youtubei.googleapis.com/youtubei/v1/visitor_id?prettyPrint=false") else {
            return cachedWebVisitorData
        }
        let payload: [String: Any] = [
            "context": buildAndroidReelContext(hl: hl, gl: gl, visitorData: "")
        ]
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("2", forHTTPHeaderField: "X-Goog-Api-Format-Version")
        request.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        if let (data, resp) = try? await session.data(for: request),
           let http = resp as? HTTPURLResponse,
           (200..<300).contains(http.statusCode),
           let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let ctx = json["responseContext"] as? [String: Any],
           let visitor = ctx["visitorData"] as? String,
           !visitor.isEmpty {
            cachedReelVisitorData = visitor
            lastReelVisitorFetch = Date()
            return visitor
        }
        return cachedWebVisitorData
    }

    private func requestAndroidReelCandidates(
        videoId: String,
        hl: String,
        gl: String,
        preferHighQuality: Bool,
        requireDirectFile: Bool
    ) async throws -> [ResolvedStream] {
        let userAgent = "com.google.android.youtube/\(androidReelVersion) (Linux; U; Android 15; \(gl)) gzip"
        let visitorData = await resolveAndroidReelVisitorData(hl: hl, gl: gl, userAgent: userAgent)
        let cpn = generateNonce(length: 16)
        let tNonce = generateNonce(length: 12)

        let endpointStr = "https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch?prettyPrint=false&t=\(tNonce)&id=\(videoId)&$fields=playerResponse"
        guard let url = URL(string: endpointStr) else { return [] }

        let body: [String: Any] = [
            "context": buildAndroidReelContext(hl: hl, gl: gl, visitorData: visitorData),
            "playerRequest": [
                "videoId": videoId,
                "cpn": cpn,
                "contentCheckOk": true,
                "racyCheckOk": true
            ],
            "disablePlayerResponse": false
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("2", forHTTPHeaderField: "X-Goog-Api-Format-Version")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let playerResponse = root["playerResponse"] as? [String: Any] else {
            return []
        }

        return extractCandidatesFromPlayerResponse(
            playerResponse,
            userAgent: userAgent,
            clientLabel: "ANDROID_REEL",
            preferHighQuality: preferHighQuality,
            preferHLSFirst: false,
            requireDirectFile: requireDirectFile
        )
    }

    private func requestPlayerCandidates(
        videoId: String,
        profile: ClientProfile,
        hl: String,
        gl: String,
        preferHighQuality: Bool,
        requireDirectFile: Bool
    ) async throws -> [ResolvedStream] {
        var clientDict: [String: Any] = [
            "clientName": profile.name,
            "clientVersion": profile.version,
            "hl": hl,
            "gl": gl,
            "utcOffsetMinutes": 0,
            "timeZone": "UTC"
        ]
        for (k, v) in profile.extraFields {
            clientDict[k] = v
        }
        if !cachedWebVisitorData.isEmpty {
            clientDict["visitorData"] = cachedWebVisitorData
        }

        let querySuffix = cachedApiKey.isEmpty ? "prettyPrint=false" : "key=\(cachedApiKey)&prettyPrint=false"
        guard let url = URL(string: "https://youtubei.googleapis.com/youtubei/v1/player?\(querySuffix)") else {
            return []
        }

        let body: [String: Any] = [
            "context": ["client": clientDict],
            "videoId": videoId,
            "contentCheckOk": true,
            "racyCheckOk": true,
            "params": "CgIQBg",
            "playbackContext": [
                "contentPlaybackContext": [
                    "html5Preference": "HTML5_PREF_WANTS"
                ]
            ],
            "watchEndpointMusicSupportedConfigs": [
                "watchEndpointMusicConfig": [
                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                ]
            ]
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(profile.userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue(profile.headerId, forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue(profile.version, forHTTPHeaderField: "X-YouTube-Client-Version")
        if !cachedWebVisitorData.isEmpty {
            request.setValue(cachedWebVisitorData, forHTTPHeaderField: "X-Goog-Visitor-Id")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let playerResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }

        return extractCandidatesFromPlayerResponse(
            playerResponse,
            userAgent: profile.userAgent,
            clientLabel: profile.label,
            preferHighQuality: preferHighQuality,
            preferHLSFirst: profile.preferHLSFirst,
            requireDirectFile: requireDirectFile
        )
    }

    private func requestAuthenticatedPlayerCandidates(
        videoId: String,
        hl: String,
        gl: String,
        preferHighQuality: Bool,
        requireDirectFile: Bool
    ) async throws -> [ResolvedStream] {
        guard let authHeaders = GoogleAccountSyncService.currentAuthenticatedHeaders(customOrigin: "https://www.youtube.com") else {
            return []
        }

        let querySuffix = cachedApiKey.isEmpty ? "prettyPrint=false" : "key=\(cachedApiKey)&prettyPrint=false"
        guard let url = URL(string: "https://www.youtube.com/youtubei/v1/player?\(querySuffix)") else {
            return []
        }

        let tvUserAgent = "Mozilla/5.0 (PlayStation; PlayStation 5/8.20) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        let body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                    "clientVersion": "2.0",
                    "hl": hl,
                    "gl": gl
                ],
                "thirdParty": [
                    "embedUrl": "https://www.youtube.com/"
                ]
            ],
            "videoId": videoId,
            "contentCheckOk": true,
            "racyCheckOk": true
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(tvUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("85", forHTTPHeaderField: "X-YouTube-Client-Name")
        request.setValue("2.0", forHTTPHeaderField: "X-YouTube-Client-Version")
        for (k, v) in authHeaders {
            request.setValue(v, forHTTPHeaderField: k)
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let playerResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }

        return extractCandidatesFromPlayerResponse(
            playerResponse,
            userAgent: tvUserAgent,
            clientLabel: "AUTH_TVHTML5",
            preferHighQuality: preferHighQuality,
            preferHLSFirst: false,
            requireDirectFile: requireDirectFile
        )
    }

    private func extractCandidatesFromPlayerResponse(
        _ playerRoot: [String: Any],
        userAgent: String,
        clientLabel: String,
        preferHighQuality: Bool,
        preferHLSFirst: Bool,
        requireDirectFile: Bool
    ) -> [ResolvedStream] {
        if let status = (playerRoot["playabilityStatus"] as? [String: Any])?["status"] as? String,
           status != "OK" {
            return []
        }
        guard let streamingData = playerRoot["streamingData"] as? [String: Any] else {
            return []
        }

        let mediaHeaders: [String: String] = [
            "User-Agent": userAgent,
            "Accept": "*/*",
            "Accept-Encoding": "identity"
        ]

        let adaptive = (streamingData["adaptiveFormats"] as? [[String: Any]]) ?? []
        let muxed = (streamingData["formats"] as? [[String: Any]]) ?? []

        struct AudioCandidate {
            let url: URL
            let bitrateKbps: Int
            let tier: Int
            let badge: String
        }

        var adaptiveAudio: [AudioCandidate] = []
        for format in adaptive {
            guard let urlStr = format["url"] as? String,
                  let rawURL = URL(string: urlStr),
                  let mime = (format["mimeType"] as? String)?.lowercased() else {
                continue
            }
            let isM4A = mime.contains("audio/mp4") || mime.contains("audio/m4a") || mime.contains("mp4a")
            let isUnsupported = mime.contains("webm") || mime.contains("opus")
            guard isM4A && !isUnsupported else { continue }

            let trackTier = audioTrackTier(for: format)
            guard trackTier > 0 else { continue }

            let bps = (format["averageBitrate"] as? Int) ?? (format["bitrate"] as? Int) ?? 128_000
            let kbps = max(64, bps / 1000)
            let badge = kbps >= 192 ? "AAC \(kbps) kbps" : "AAC"
            let numberedURL = appendRequestNumberIfNeeded(rawURL)
            adaptiveAudio.append(AudioCandidate(url: numberedURL, bitrateKbps: kbps, tier: trackTier, badge: badge))
        }

        adaptiveAudio.sort { a, b in
            if a.tier != b.tier { return a.tier > b.tier }
            return preferHighQuality ? (a.bitrateKbps > b.bitrateKbps) : (a.bitrateKbps < b.bitrateKbps)
        }

        var muxedStreams: [ResolvedStream] = []
        for format in muxed {
            guard let urlStr = format["url"] as? String,
                  let rawURL = URL(string: urlStr),
                  let mime = (format["mimeType"] as? String)?.lowercased(),
                  mime.contains("mp4"),
                  !mime.contains("webm") else {
                continue
            }
            let bps = (format["averageBitrate"] as? Int) ?? (format["bitrate"] as? Int) ?? 128_000
            let kbps = max(96, bps / 1000)
            let numberedURL = appendRequestNumberIfNeeded(rawURL)
            muxedStreams.append(
                ResolvedStream(
                    url: numberedURL,
                    headers: mediaHeaders,
                    codecBadge: "MP4 AAC",
                    bitrateKbps: kbps,
                    isDirectFile: true,
                    clientLabel: clientLabel,
                    expiresAt: expirationDate(for: numberedURL)
                )
            )
        }

        var hlsCandidate: ResolvedStream?
        if !requireDirectFile,
           let hlsStr = streamingData["hlsManifestUrl"] as? String,
           let hlsURL = URL(string: hlsStr) {
            hlsCandidate = ResolvedStream(
                url: hlsURL,
                headers: ["User-Agent": userAgent],
                codecBadge: "HLS AAC",
                bitrateKbps: preferHighQuality ? 256 : 128,
                isDirectFile: false,
                clientLabel: "\(clientLabel)_HLS",
                expiresAt: expirationDate(for: hlsURL)
            )
        }

        let directAudioStreams: [ResolvedStream] = adaptiveAudio.map { item in
            ResolvedStream(
                url: item.url,
                headers: mediaHeaders,
                codecBadge: item.badge,
                bitrateKbps: item.bitrateKbps,
                isDirectFile: true,
                clientLabel: clientLabel,
                expiresAt: expirationDate(for: item.url)
            )
        }

        var ordered: [ResolvedStream] = []
        if preferHLSFirst {
            if let hls = hlsCandidate { ordered.append(hls) }
            ordered.append(contentsOf: directAudioStreams)
            ordered.append(contentsOf: muxedStreams)
        } else {
            ordered.append(contentsOf: directAudioStreams)
            if let hls = hlsCandidate { ordered.append(hls) }
            ordered.append(contentsOf: muxedStreams)
        }
        return ordered
    }

    private func verifyAndAdaptStream(_ stream: ResolvedStream) async -> ResolvedStream? {
        if !stream.isDirectFile {
            let ok = await probeHLSManifest(url: stream.url, headers: stream.headers)
            return ok ? stream : nil
        }

        let outcome = await probeDirectByteRange(url: stream.url, headers: stream.headers)
        switch outcome {
        case .playable:
            return stream
        case .endpointFailure:
            let alternates = alternateGoogleVideoURLs(from: stream.url)
            for altURL in alternates {
                let retryOutcome = await probeDirectByteRange(url: altURL, headers: stream.headers)
                if retryOutcome == .playable {
                    return ResolvedStream(
                        url: altURL,
                        headers: stream.headers,
                        codecBadge: stream.codecBadge,
                        bitrateKbps: stream.bitrateKbps,
                        isDirectFile: true,
                        clientLabel: stream.clientLabel,
                        expiresAt: stream.expiresAt
                    )
                }
            }
            return nil
        case .rejected:
            return nil
        }
    }

    private enum ProbeResult: Equatable {
        case playable
        case endpointFailure
        case rejected
    }

    private func probeDirectByteRange(url: URL, headers: [String: String]) async -> ProbeResult {
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        for (k, v) in headers {
            req.setValue(v, forHTTPHeaderField: k)
        }
        req.setValue("bytes=0-4095", forHTTPHeaderField: "Range")
        req.setValue("identity", forHTTPHeaderField: "Accept-Encoding")

        do {
            let (data, resp) = try await probeSession.data(for: req)
            guard let http = resp as? HTTPURLResponse else {
                return .endpointFailure
            }
            let code = http.statusCode
            if code == 404 || code == 500 || code == 502 || code == 503 || code == 504 {
                return .endpointFailure
            }
            if code == 401 || code == 403 || code == 410 || code == 416 || code == 429 {
                return .rejected
            }
            guard (200...299).contains(code) else {
                return .rejected
            }
            let ctype = (http.value(forHTTPHeaderField: "Content-Type") ?? "").lowercased()
            if ctype.contains("text/html") || ctype.contains("application/json") {
                return .rejected
            }
            return data.isEmpty ? .rejected : .playable
        } catch {
            return .endpointFailure
        }
    }

    private func probeHLSManifest(url: URL, headers: [String: String]) async -> Bool {
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        for (k, v) in headers {
            req.setValue(v, forHTTPHeaderField: k)
        }
        do {
            let (data, resp) = try await probeSession.data(for: req)
            guard let http = resp as? HTTPURLResponse,
                  (200...299).contains(http.statusCode),
                  let text = String(data: data.prefix(256), encoding: .utf8) else {
                return false
            }
            return text.contains("#EXTM3U")
        } catch {
            return false
        }
    }

    private func alternateGoogleVideoURLs(from url: URL) -> [URL] {
        guard var comps = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let host = comps.host?.lowercased(),
              host.hasSuffix(".googlevideo.com") else {
            return []
        }
        let label = String(host.dropLast(".googlevideo.com".count))
        guard let sepRange = label.range(of: "---") else { return [] }
        let prefix = String(label[..<sepRange.lowerBound])
        let currentMN = String(label[sepRange.upperBound...])
        guard !prefix.isEmpty, !currentMN.isEmpty else { return [] }

        guard let mnValue = comps.queryItems?.first(where: { $0.name == "mn" })?.value else {
            return []
        }
        let networks = mnValue
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
            .filter { !$0.isEmpty && $0 != currentMN }

        var out: [URL] = []
        for net in networks.prefix(3) {
            comps.host = "\(prefix)---\(net).googlevideo.com"
            if let candidate = comps.url {
                out.append(candidate)
            }
        }
        return out
    }

    private func findAlternateAudioTrack(for track: Track, excludingVideoId: String) async -> Track? {
        let cleanTitle = track.title
            .replacingOccurrences(of: #"\((?i:official[^)]*|music\s*video[^)]*|mv[^)]*|hd|4k)\)"#, with: "", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanArtist = track.artist
            .replacingOccurrences(of: " - Topic", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        let searchQuery = "\(cleanTitle) \(cleanArtist) audio"
        guard let candidates = try? await performWebRemixSearch(
            query: searchQuery,
            params: "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
        ), !candidates.isEmpty else {
            return nil
        }

        let filtered = candidates.filter { item in
            guard let vid = item.videoId else { return false }
            return vid != excludingVideoId
        }

        if track.durationSec > 0 {
            let sorted = filtered.sorted { a, b in
                let diffA = a.durationSec > 0 ? abs(a.durationSec - track.durationSec) : 999
                let diffB = b.durationSec > 0 ? abs(b.durationSec - track.durationSec) : 999
                return diffA < diffB
            }
            return sorted.first
        }
        return filtered.first
    }

    private func resolveViaExternalMirrors(
        videoId: String,
        preferHighQuality: Bool,
        requireDirectFile: Bool
    ) async -> [ResolvedStream]? {
        let pipedBases = [
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.drgns.space",
            "https://pipedapi.ducks.party"
        ]
        var collected: [ResolvedStream] = []

        for base in pipedBases {
            guard let url = URL(string: "\(base)/streams/\(videoId)") else { continue }
            var req = URLRequest(url: url)
            req.timeoutInterval = 5
            req.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
            guard let (data, resp) = try? await probeSession.data(for: req),
                  let http = resp as? HTTPURLResponse,
                  http.statusCode == 200,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                continue
            }

            if let audioStreams = json["audioStreams"] as? [[String: Any]] {
                var m4aItems: [(URL, Int)] = []
                for item in audioStreams {
                    let mime = ((item["mimeType"] as? String) ?? "").lowercased()
                    let format = ((item["format"] as? String) ?? "").lowercased()
                    guard (mime.contains("mp4") || mime.contains("m4a") || format.contains("m4a")),
                          !mime.contains("webm"),
                          let uStr = item["url"] as? String,
                          let u = URL(string: uStr) else {
                        continue
                    }
                    let bps = (item["bitrate"] as? Int) ?? 128_000
                    m4aItems.append((u, max(64, bps / 1000)))
                }
                m4aItems.sort { preferHighQuality ? ($0.1 > $1.1) : ($0.1 < $1.1) }
                for pick in m4aItems.prefix(2) {
                    collected.append(
                        ResolvedStream(
                            url: pick.0,
                            headers: ["User-Agent": webUserAgent, "Accept": "*/*", "Accept-Encoding": "identity"],
                            codecBadge: "AAC \(pick.1) kbps",
                            bitrateKbps: pick.1,
                            isDirectFile: true,
                            clientLabel: "PIPED_MIRROR",
                            expiresAt: expirationDate(for: pick.0)
                        )
                    )
                }
            }

            if !requireDirectFile,
               let hlsStr = json["hls"] as? String,
               let hlsURL = URL(string: hlsStr) {
                collected.append(
                    ResolvedStream(
                        url: hlsURL,
                        headers: ["User-Agent": webUserAgent],
                        codecBadge: "HLS AAC",
                        bitrateKbps: 192,
                        isDirectFile: false,
                        clientLabel: "PIPED_HLS",
                        expiresAt: expirationDate(for: hlsURL)
                    )
                )
            }

            if !collected.isEmpty {
                return collected
            }
        }

        let invidiousBases = [
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://yewtu.be"
        ]
        for base in invidiousBases {
            guard let url = URL(string: "\(base)/api/v1/videos/\(videoId)?fields=adaptiveFormats") else { continue }
            var req = URLRequest(url: url)
            req.timeoutInterval = 5
            req.setValue(webUserAgent, forHTTPHeaderField: "User-Agent")
            guard let (data, resp) = try? await probeSession.data(for: req),
                  let http = resp as? HTTPURLResponse,
                  http.statusCode == 200,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let formats = json["adaptiveFormats"] as? [[String: Any]] else {
                continue
            }

            var m4aItems: [(URL, Int)] = []
            for item in formats {
                let typeStr = ((item["type"] as? String) ?? "").lowercased()
                guard typeStr.contains("audio/mp4"),
                      let uStr = item["url"] as? String,
                      let u = URL(string: uStr) else {
                    continue
                }
                let bpsRaw = (item["bitrate"] as? String).flatMap(Int.init) ?? (item["bitrate"] as? Int) ?? 128_000
                m4aItems.append((u, max(64, bpsRaw / 1000)))
            }
            m4aItems.sort { preferHighQuality ? ($0.1 > $1.1) : ($0.1 < $1.1) }
            for pick in m4aItems.prefix(2) {
                collected.append(
                    ResolvedStream(
                        url: pick.0,
                        headers: ["User-Agent": webUserAgent, "Accept": "*/*", "Accept-Encoding": "identity"],
                        codecBadge: "AAC \(pick.1) kbps",
                        bitrateKbps: pick.1,
                        isDirectFile: true,
                        clientLabel: "INVIDIOUS_MIRROR",
                        expiresAt: expirationDate(for: pick.0)
                    )
                )
            }
            if !collected.isEmpty {
                return collected
            }
        }

        return nil
    }

    private func refreshWatchContextIfNeeded(videoId: String) async {
        if !cachedWebVisitorData.isEmpty && Date().timeIntervalSince(lastWebContextFetch) < 1800 {
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
            cachedWebVisitorData = visitor
        }
        if let key = firstRegexMatch(in: html, pattern: #""INNERTUBE_API_KEY"\s*:\s*"([^"]+)""#) {
            cachedApiKey = key
        }
        lastWebContextFetch = Date()
    }

    private func performWebRemixSearch(query: String, params: String?) async throws -> [Track] {
        guard let url = URL(string: "https://music.youtube.com/youtubei/v1/search?prettyPrint=false") else {
            return []
        }

        let hl = effectiveLanguageCode
        let gl = effectiveCountryCode
        var body: [String: Any] = [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": webRemixVersion,
                    "hl": hl,
                    "gl": gl
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

    private func audioTrackTier(for format: [String: Any]) -> Int {
        guard let audioTrack = format["audioTrack"] as? [String: Any] else {
            return 2
        }
        if (audioTrack["isAutoDubbed"] as? Bool) == true {
            return 0
        }
        if (audioTrack["audioIsDefault"] as? Bool) == true {
            return 3
        }
        return 1
    }

    private func appendRequestNumberIfNeeded(_ url: URL) -> URL {
        guard var comps = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let host = comps.host?.lowercased(),
              host.hasSuffix("googlevideo.com") else {
            return url
        }
        var items = comps.queryItems ?? []
        if items.contains(where: { $0.name == "rn" }) {
            return url
        }
        let nextNum = requestSequence
        requestSequence += 1
        items.append(URLQueryItem(name: "rn", value: String(nextNum)))
        comps.queryItems = items
        return comps.url ?? url
    }

    private func expirationDate(for url: URL) -> Date {
        if let comps = URLComponents(url: url, resolvingAgainstBaseURL: false),
           let expStr = comps.queryItems?.first(where: { $0.name == "expire" })?.value,
           let expEpoch = TimeInterval(expStr) {
            let safeDate = Date(timeIntervalSince1970: expEpoch - 240)
            if safeDate > Date() {
                return safeDate
            }
        }
        return Date().addingTimeInterval(5400)
    }

    private func canonicalURLKey(_ url: URL) -> String {
        guard var comps = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return url.absoluteString
        }
        if let items = comps.queryItems {
            comps.queryItems = items.filter { $0.name != "rn" }
        }
        return comps.string ?? url.absoluteString
    }

    private func isURLQuarantined(_ url: URL) -> Bool {
        guard let until = quarantinedURLs[canonicalURLKey(url)] else { return false }
        return until > Date()
    }

    private func isClientQuarantined(_ label: String) -> Bool {
        guard let until = quarantinedClients[label] else { return false }
        return until > Date()
    }

    private func pruneExpiredQuarantines() {
        let now = Date()
        quarantinedURLs = quarantinedURLs.filter { $0.value > now }
        quarantinedClients = quarantinedClients.filter { $0.value > now }

        if resolvedCache.count > 60 {
            for (k, list) in resolvedCache {
                let valid = list.filter { $0.isFresh && !isURLQuarantined($0.url) }
                if valid.isEmpty {
                    resolvedCache.removeValue(forKey: k)
                } else {
                    resolvedCache[k] = valid
                }
            }
            while resolvedCache.count > 120, let firstKey = resolvedCache.keys.first {
                resolvedCache.removeValue(forKey: firstKey)
            }
        }
    }

    private func generateNonce(length: Int) -> String {
        String((0..<length).compactMap { _ in nonceAlphabet.randomElement() })
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
        let longByline = extractText(from: panel["longBylineText"]).trimmingCharacters(in: .whitespacesAndNewlines)
        let bylineParts = longByline
            .components(separatedBy: CharacterSet(charactersIn: "•·"))
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let albumName: String = {
            guard bylineParts.count >= 2 else { return "" }
            let candidate = bylineParts[1]
            if Int(candidate) != nil { return "" }
            return candidate
        }()
        let lengthStr = extractText(from: panel["lengthText"])
        let durationSec = parseDurationToken(lengthStr) ?? 0
        let rawThumb = extractBestThumbnail(from: panel)
        let smallThumb = rawThumb.isEmpty ? "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg" : rawThumb
        let largeThumb = upgradeArtworkURL(smallThumb, videoId: vid)

        return Track(
            id: "yt:\(vid)",
            title: title,
            artist: artist.isEmpty ? (bylineParts.first ?? "Unknown Artist") : artist,
            album: albumName,
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
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return "https://i.ytimg.com/vi/\(videoId)/hqdefault.jpg"
        }
        return ArtworkURLResolver.resolve(trimmed, targetPixels: 1080)
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
