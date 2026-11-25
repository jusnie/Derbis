import Foundation
import Security
import CryptoKit
import WebKit

struct SpotifySessionCredentials: Sendable {
    let accessToken: String
    let clientId: String
    let clientToken: String?
    let clientVersion: String
    let expiresAt: Date
}

@MainActor
final class SpotifySessionStore: ObservableObject {
    static let shared = SpotifySessionStore()

    @Published private(set) var hasSession: Bool = false
    @Published private(set) var maskedPreview: String = ""
    @Published var statusMessage: String?

    private let keychainService = "com.derbis.ios.spotify"
    private let keychainAccount = "sp_dc"

    private init() {
        let existing = readKeychainCookie()
        hasSession = !existing.isEmpty
        maskedPreview = Self.mask(existing)
    }

    func currentCookie() -> String {
        readKeychainCookie()
    }

    @discardableResult
    func saveCookie(_ rawInput: String) -> Bool {
        guard let cleaned = Self.extractCleanCookie(from: rawInput) else {
            statusMessage = "Invalid sp_dc cookie value"
            return false
        }
        writeKeychainCookie(cleaned)
        hasSession = true
        maskedPreview = Self.mask(cleaned)
        statusMessage = "Spotify Canvas session connected"
        SpotifyTokenHarvester.shared.invalidate()
        Task {
            await MotionCanvasService.shared.invalidateAll()
            _ = await SpotifyTokenHarvester.shared.credentials(forceRefresh: true)
        }
        return true
    }

    func disconnect() {
        deleteKeychainCookie()
        hasSession = false
        maskedPreview = ""
        statusMessage = "Spotify session removed"
        SpotifyTokenHarvester.shared.invalidate()
        Task {
            await MotionCanvasService.shared.invalidateAll()
        }
    }

    static func extractCleanCookie(from raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        var candidate = trimmed
        if let range = trimmed.range(of: #"sp_dc=([^;\s"']+)"#, options: .regularExpression) {
            let sub = String(trimmed[range])
            candidate = String(sub.dropFirst("sp_dc=".count))
        }
        candidate = candidate.trimmingCharacters(in: CharacterSet(charactersIn: "\"'; "))
        guard candidate.count >= 28,
              candidate.range(of: #"^[A-Za-z0-9._\-]+$"#, options: .regularExpression) != nil else {
            return nil
        }
        return candidate
    }

    private static func mask(_ cookie: String) -> String {
        guard cookie.count > 12 else { return "" }
        return "\(cookie.prefix(6))••••\(cookie.suffix(4))"
    }

    private func readKeychainCookie() -> String {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess,
              let data = result as? Data,
              let str = String(data: data, encoding: .utf8) else {
            return ""
        }
        return str
    }

    private func writeKeychainCookie(_ value: String) {
        deleteKeychainCookie()
        guard let data = value.data(using: .utf8) else { return }
        let attrs: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        SecItemAdd(attrs as CFDictionary, nil)
    }

    private func deleteKeychainCookie() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount
        ]
        SecItemDelete(query as CFDictionary)
    }
}

@MainActor
final class SpotifyTokenHarvester: NSObject {
    static let shared = SpotifyTokenHarvester()

    static let browserUserAgent =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private let defaultClientId = "d8a5ed958d274c2e8ee717e6a4b0971d"
    private let fallbackClientVersion = "1.2.60.564.g8cd2f22c"

    private var cachedCredentials: SpotifySessionCredentials?
    private var cachedForCookie: String = ""
    private var pathfinderHashes: [String: String] = [:]
    private var inFlightTask: Task<SpotifySessionCredentials?, Never>?
    private(set) var lastSawAnonymousOnly: Bool = false

    func invalidate() {
        cachedCredentials = nil
        cachedForCookie = ""
        lastSawAnonymousOnly = false
        inFlightTask?.cancel()
        inFlightTask = nil
    }

    func pathfinderHash(for operation: String) -> String? {
        pathfinderHashes[operation]
    }

    func recordHash(operation: String, sha256: String) {
        if sha256.count == 64 && sha256.range(of: #"^[0-9a-fA-F]{64}$"#, options: .regularExpression) != nil {
            pathfinderHashes[operation] = sha256.lowercased()
        }
    }

    func credentials(forceRefresh: Bool = false) async -> SpotifySessionCredentials? {
        let cookie = SpotifySessionStore.shared.currentCookie()
        guard !cookie.isEmpty else { return nil }

        if forceRefresh {
            invalidate()
        }

        if let existing = cachedCredentials,
           cachedForCookie == cookie,
           Date().addingTimeInterval(30) < existing.expiresAt {
            return existing
        }

        if let active = inFlightTask {
            return await active.value
        }

        let task = Task<SpotifySessionCredentials?, Never> { @MainActor in
            defer { self.inFlightTask = nil }
            let harvested = await self.performWebHarvest(cookie: cookie)
            if let creds = harvested {
                self.cachedCredentials = creds
                self.cachedForCookie = cookie
            }
            return harvested
        }
        inFlightTask = task
        return await task.value
    }

    private func performWebHarvest(cookie: String) async -> SpotifySessionCredentials? {
        let store = WKWebsiteDataStore.default().httpCookieStore
        let domains = [".spotify.com", "open.spotify.com", "accounts.spotify.com"]
        for domain in domains {
            if let httpCookie = HTTPCookie(properties: [
                .domain: domain,
                .path: "/",
                .name: "sp_dc",
                .value: cookie,
                .secure: "TRUE",
                .expires: Date().addingTimeInterval(86_400 * 30)
            ]) {
                await store.setCookie(httpCookie)
            }
        }

        let collector = HarvestBridgeCollector()
        let contentController = WKUserContentController()
        contentController.add(collector, name: "DerbisSpotifyTokenBridge")
        contentController.addUserScript(
            WKUserScript(source: Self.hookScript, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        )
        contentController.addUserScript(
            WKUserScript(source: Self.domScanScript, injectionTime: .atDocumentEnd, forMainFrameOnly: true)
        )

        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()
        config.userContentController = contentController

        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: 1, height: 1), configuration: config)
        webView.customUserAgent = Self.browserUserAgent

        if let url = URL(string: "https://open.spotify.com/") {
            webView.load(URLRequest(url: url))
        }

        let deadline = Date().addingTimeInterval(15.0)
        while Date() < deadline {
            if Task.isCancelled { break }
            if let token = collector.pendingToken, !token.isEmpty {
                if collector.pendingClientToken != nil {
                    break
                }
                try? await Task.sleep(nanoseconds: 550_000_000)
                if collector.pendingToken != nil {
                    break
                }
            }
            try? await Task.sleep(nanoseconds: 250_000_000)
        }

        for (op, hash) in collector.observedHashes {
            recordHash(operation: op, sha256: hash)
        }

        contentController.removeScriptMessageHandler(forName: "DerbisSpotifyTokenBridge")
        webView.stopLoading()

        guard let token = collector.pendingToken, !token.isEmpty else {
            lastSawAnonymousOnly = collector.sawAnonymousToken
            return nil
        }

        lastSawAnonymousOnly = false
        let clientId = collector.pendingClientId ?? defaultClientId
        let version = collector.pendingClientVersion ?? fallbackClientVersion
        let expiry = collector.pendingExpiresAt ?? Date().addingTimeInterval(3500)

        var resolvedClientToken = collector.pendingClientToken
        if resolvedClientToken == nil || resolvedClientToken?.isEmpty == true {
            resolvedClientToken = await mintClientToken(clientId: clientId, clientVersion: version)
        }

        return SpotifySessionCredentials(
            accessToken: token,
            clientId: clientId,
            clientToken: resolvedClientToken,
            clientVersion: version,
            expiresAt: expiry
        )
    }

    private func mintClientToken(clientId: String, clientVersion: String) async -> String? {
        guard let url = URL(string: "https://clienttoken.spotify.com/v1/clienttoken") else {
            return nil
        }
        let payload: [String: Any] = [
            "client_data": [
                "client_version": clientVersion,
                "client_id": clientId,
                "js_sdk_data": [
                    "device_brand": "unknown",
                    "device_model": "unknown",
                    "os": "macos",
                    "os_version": "10_15_7",
                    "device_id": UUID().uuidString.lowercased(),
                    "device_type": "computer"
                ]
            ]
        ]

        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("https://open.spotify.com", forHTTPHeaderField: "Origin")
        req.setValue("https://open.spotify.com/", forHTTPHeaderField: "Referer")
        req.setValue(Self.browserUserAgent, forHTTPHeaderField: "User-Agent")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              (json["response_type"] as? String) == "RESPONSE_GRANTED_TOKEN_RESPONSE",
              let granted = json["granted_token"] as? [String: Any],
              let token = granted["token"] as? String,
              !token.isEmpty else {
            return nil
        }
        return token
    }

    private static let hookScript: String = """
    (function () {
      if (window.__derbisHookInstalled) return;
      window.__derbisHookInstalled = true;
      function send(msg) {
        try { window.webkit.messageHandlers.DerbisSpotifyTokenBridge.postMessage(msg); } catch (e) {}
      }
      function isTokenUrl(u) {
        var s = String(u || '');
        return s.indexOf('/api/token') !== -1 || s.indexOf('/get_access_token') !== -1;
      }
      function isClientTokenUrl(u) {
        return String(u || '').indexOf('clienttoken.spotify.com') !== -1;
      }
      function readHdr(headers, name) {
        if (!headers) return '';
        try {
          if (typeof headers.get === 'function') return headers.get(name) || headers.get(name.toLowerCase()) || '';
          if (Array.isArray(headers)) {
            for (var i = 0; i < headers.length; i++) {
              if (headers[i] && String(headers[i][0]).toLowerCase() === name.toLowerCase()) return headers[i][1] || '';
            }
          } else if (typeof headers === 'object') {
            for (var k in headers) {
              if (Object.prototype.hasOwnProperty.call(headers, k) && k.toLowerCase() === name.toLowerCase()) return headers[k] || '';
            }
          }
        } catch (e) {}
        return '';
      }
      function inspectBody(body) {
        try {
          if (typeof body !== 'string' || body.indexOf('persistedQuery') === -1) return;
          var j = JSON.parse(body);
          var op = j && j.operationName;
          var h = j && j.extensions && j.extensions.persistedQuery && j.extensions.persistedQuery.sha256Hash;
          if (op && h) send({ kind: 'hash', op: String(op), hash: String(h) });
        } catch (e) {}
      }
      var origFetch = window.fetch;
      if (origFetch) {
        window.fetch = function (input, init) {
          var url = (input && input.url) ? input.url : input;
          try {
            var hdrs = (init && init.headers) || (input && input.headers);
            var auth = readHdr(hdrs, 'authorization');
            var ct = readHdr(hdrs, 'client-token');
            var ver = readHdr(hdrs, 'spotify-app-version');
            if (auth || ct || ver) send({ kind: 'headers', auth: auth, ct: ct, ver: ver });
            if (init && init.body) inspectBody(init.body);
          } catch (e) {}
          var p = origFetch.apply(this, arguments);
          if (isTokenUrl(url)) {
            p.then(function (r) { r.clone().text().then(function (t) { send({ kind: 'token', body: t }); }); }).catch(function () {});
          } else if (isClientTokenUrl(url)) {
            p.then(function (r) { r.clone().text().then(function (t) { send({ kind: 'clientToken', body: t }); }); }).catch(function () {});
          }
          return p;
        };
      }
    })();
    """

    private static let domScanScript: String = """
    (function () {
      function send(msg) {
        try { window.webkit.messageHandlers.DerbisSpotifyTokenBridge.postMessage(msg); } catch (e) {}
      }
      try {
        var sessionEl = document.getElementById('session') || document.querySelector('script[data-testid="session"]');
        if (sessionEl && sessionEl.textContent) {
          send({ kind: 'token', body: sessionEl.textContent });
        }
        var cfgEl = document.getElementById('appServerConfig');
        if (cfgEl && cfgEl.textContent) {
          var raw = cfgEl.textContent.trim();
          var decoded = raw.charAt(0) === '{' ? raw : atob(raw);
          var cfg = JSON.parse(decoded);
          if (cfg && cfg.clientVersion) {
            send({ kind: 'headers', auth: '', ct: '', ver: String(cfg.clientVersion) });
          }
        }
      } catch (e) {}
    })();
    """
}

@MainActor
private final class HarvestBridgeCollector: NSObject, WKScriptMessageHandler {
    var pendingToken: String?
    var pendingExpiresAt: Date?
    var pendingClientId: String?
    var pendingClientToken: String?
    var pendingClientVersion: String?
    var sawAnonymousToken: Bool = false
    var observedHashes: [String: String] = [:]

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let dict = message.body as? [String: Any],
              let kind = dict["kind"] as? String else {
            return
        }

        switch kind {
        case "token":
            guard let body = dict["body"] as? String,
                  let data = body.data(using: .utf8),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let token = (json["accessToken"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !token.isEmpty else {
                return
            }
            if (json["isAnonymous"] as? Bool) == true {
                sawAnonymousToken = true
                return
            }
            pendingToken = token
            if let expMs = json["accessTokenExpirationTimestampMs"] as? Double, expMs > 0 {
                pendingExpiresAt = Date(timeIntervalSince1970: expMs / 1000.0)
            }
            if let cid = (json["clientId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !cid.isEmpty {
                pendingClientId = cid
            }

        case "clientToken":
            guard let body = dict["body"] as? String,
                  let data = body.data(using: .utf8),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let granted = json["granted_token"] as? [String: Any],
                  let ct = (granted["token"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !ct.isEmpty else {
                return
            }
            pendingClientToken = ct

        case "headers":
            if let ct = (dict["ct"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !ct.isEmpty {
                pendingClientToken = ct
            }
            if let ver = (dict["ver"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !ver.isEmpty {
                pendingClientVersion = ver
            }
            if pendingToken == nil,
               let auth = (dict["auth"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
               auth.lowercased().hasPrefix("bearer ") {
                let raw = String(auth.dropFirst(7)).trimmingCharacters(in: .whitespacesAndNewlines)
                if raw.count > 40 && !sawAnonymousToken {
                    pendingToken = raw
                }
            }

        case "hash":
            if let op = dict["op"] as? String, let hash = dict["hash"] as? String {
                observedHashes[op] = hash
            }

        default:
            break
        }
    }
}

actor MotionCanvasService {
    static let shared = MotionCanvasService()

    private struct CacheEntry {
        let asset: MotionCanvasAsset?
        let expiresAt: Date
    }

    private let session: URLSession
    private var memoryCache: [String: CacheEntry] = [:]
    private var spclientHosts: [String] = []
    private var spclientExpiresAt: Date = .distantPast

    private let knownCanvasHashes = [
        "575138ab27cd5c1b3e54da54d0a7cc8d85485402de26340c2145f0f6bb5e7a9f",
        "1b1e1915481c99f4349af88268c6b49a2b601cf0db7b8749b5dd750880ab6fc5"
    ]
    private let knownSearchHashes = [
        "fcad5a3e0d5af727fb76966f06971c19cfa2275e6ff7671196753e008611873c",
        "75bbf6bfcfdf85fa8fc828417bf0c4b75d0679a1fb1ab02cc73d13b61817b60d"
    ]
    private let defaultSpclientHosts = [
        "gae2-spclient.spotify.com",
        "gew4-spclient.spotify.com",
        "guc3-spclient.spotify.com",
        "spclient.wg.spotify.com"
    ]

    init() {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 8
        cfg.timeoutIntervalForResource = 16
        self.session = URLSession(configuration: cfg)
    }

    func invalidateAll() {
        memoryCache.removeAll()
    }

    func clearCachedVideos() {
        memoryCache.removeAll()
        try? FileManager.default.removeItem(at: canvasCacheDirectory())
    }

    func resolveMotionCanvas(for track: Track, preferTall: Bool) async -> MotionCanvasAsset? {
        let key = "\(track.id)|\(preferTall ? "tall" : "sq")"
        if let hit = memoryCache[key], hit.expiresAt > Date() {
            if let asset = hit.asset {
                if !asset.playbackURL.isFileURL || FileManager.default.fileExists(atPath: asset.playbackURL.path) {
                    return asset
                }
            } else {
                return nil
            }
        }

        let cleanTitle = cleanTrackTitle(track.title)
        let cleanArtist = cleanArtistName(track.artist)

        let spotifyIds = await resolveSpotifyTrackIds(
            track: track,
            cleanTitle: cleanTitle,
            cleanArtist: cleanArtist
        )

        let hasSpotifySession = await MainActor.run { SpotifySessionStore.shared.hasSession }
        for spotifyId in spotifyIds.prefix(3) {
            if let canvasMP4 = await resolveSpotifyCanvasMP4(
                spotifyId: spotifyId,
                useAuthenticatedTiers: hasSpotifySession
            ) {
                let localOrRemote = await cacheMP4LocallyIfPossible(remoteURL: canvasMP4) ?? canvasMP4
                let asset = MotionCanvasAsset(
                    trackId: track.id,
                    remoteURL: canvasMP4,
                    playbackURL: localOrRemote,
                    provider: .spotifyCanvas,
                    isHLS: false,
                    isTall: true
                )
                memoryCache[key] = CacheEntry(asset: asset, expiresAt: Date().addingTimeInterval(1800))
                return asset
            }
        }

        if let appleAsset = await resolveAppleMusicAnimatedArtwork(
            track: track,
            cleanTitle: cleanTitle,
            cleanArtist: cleanArtist,
            preferTall: preferTall
        ) {
            memoryCache[key] = CacheEntry(asset: appleAsset, expiresAt: Date().addingTimeInterval(1800))
            return appleAsset
        }

        memoryCache[key] = CacheEntry(asset: nil, expiresAt: Date().addingTimeInterval(420))
        return nil
    }

    func testConnectionProbe() async -> String {
        let hasSession = await MainActor.run { SpotifySessionStore.shared.hasSession }
        if hasSession {
            guard let creds = await SpotifyTokenHarvester.shared.credentials(forceRefresh: true) else {
                let expired = await MainActor.run { SpotifyTokenHarvester.shared.lastSawAnonymousOnly }
                return expired
                    ? "Spotify cookie expired — sign in again to refresh"
                    : "Could not mint a Spotify Web Player token right now"
            }
            let probeIds = ["0VjIjW4GlUZAMYd2vXMi3b", "39LLxExYz6ewLAcYrzQQyP"]
            for pid in probeIds {
                if let url = await queryAuthenticatedSpotifyCanvas(spotifyId: pid, creds: creds) {
                    return "Verified official Spotify Canvas (\(url.host ?? "scdn.co"))"
                }
            }
            return "Spotify token active — ready for Canvas lookups"
        }

        if let mirror = await queryPublicCanvasStore(spotifyId: "0VjIjW4GlUZAMYd2vXMi3b") {
            return "Public Canvas & Apple Motion active (\(mirror.host ?? "ready"))"
        }
        return "Motion lookup online — connect Spotify for full catalog coverage"
    }

    private func resolveSpotifyTrackIds(
        track: Track,
        cleanTitle: String,
        cleanArtist: String
    ) async -> [String] {
        var collected: [String] = []

        if let vid = track.videoId, let odesliId = await lookupSpotifyIdViaOdesli(videoId: vid) {
            collected.append(odesliId)
        }

        let hasSession = await MainActor.run { SpotifySessionStore.shared.hasSession }
        if hasSession, let creds = await SpotifyTokenHarvester.shared.credentials() {
            let searched = await searchSpotifyCatalog(
                title: cleanTitle,
                artist: cleanArtist,
                durationSec: track.durationSec,
                creds: creds
            )
            for sid in searched where !collected.contains(sid) {
                collected.append(sid)
            }
        }

        return collected
    }

    private func lookupSpotifyIdViaOdesli(videoId: String) async -> String? {
        let ytURL = "https://www.youtube.com/watch?v=\(videoId)"
        guard let encoded = ytURL.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://api.song.link/v1-alpha.1/links?url=\(encoded)") else {
            return nil
        }
        var req = URLRequest(url: url)
        req.timeoutInterval = 5.5
        req.setValue("application/json", forHTTPHeaderField: "Accept")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let platforms = json["linksByPlatform"] as? [String: Any],
              let spotifyObj = platforms["spotify"] as? [String: Any] else {
            return nil
        }

        if let entityUid = spotifyObj["entityUniqueId"] as? String,
           let idPart = entityUid.components(separatedBy: "::").last,
           let valid = normalizeSpotifyId(idPart) {
            return valid
        }
        if let spotifyLink = spotifyObj["url"] as? String,
           let valid = normalizeSpotifyId(spotifyLink) {
            return valid
        }
        return nil
    }

    private func searchSpotifyCatalog(
        title: String,
        artist: String,
        durationSec: Int,
        creds: SpotifySessionCredentials
    ) async -> [String] {
        let query = "\(title) \(artist)".trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return [] }

        var candidates: [(id: String, score: Int)] = []

        if let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
           let url = URL(string: "https://api.spotify.com/v1/search?type=track&limit=6&q=\(encoded)") {
            var req = URLRequest(url: url)
            applySpotifyWebHeaders(&req, creds: creds)
            if let (data, resp) = try? await session.data(for: req),
               let http = resp as? HTTPURLResponse {
                if http.statusCode == 401 {
                    await MainActor.run { SpotifyTokenHarvester.shared.invalidate() }
                } else if (200..<300).contains(http.statusCode),
                          let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                          let tracksObj = root["tracks"] as? [String: Any],
                          let items = tracksObj["items"] as? [[String: Any]] {
                    for item in items {
                        guard let rawId = item["id"] as? String,
                              let sid = normalizeSpotifyId(rawId),
                              let hitTitle = item["name"] as? String else {
                            continue
                        }
                        let artistArr = (item["artists"] as? [[String: Any]]) ?? []
                        let hitArtist = artistArr.compactMap { $0["name"] as? String }.joined(separator: " ")
                        let durMs = (item["duration_ms"] as? Int) ?? 0
                        let s = matchScore(
                            wantedTitle: title,
                            wantedArtist: artist,
                            wantedDurationSec: durationSec,
                            candidateTitle: hitTitle,
                            candidateArtist: hitArtist,
                            candidateDurationSec: durMs / 1000
                        )
                        if s >= 65 {
                            candidates.append((sid, s))
                        }
                    }
                }
            }
        }

        if candidates.isEmpty {
            let liveHash = await MainActor.run { SpotifyTokenHarvester.shared.pathfinderHash(for: "searchDesktop") }
            var hashes: [String] = []
            if let liveHash = liveHash { hashes.append(liveHash) }
            for h in knownSearchHashes where !hashes.contains(h) { hashes.append(h) }

            for hash in hashes {
                let body: [String: Any] = [
                    "variables": [
                        "searchTerm": query,
                        "offset": 0,
                        "limit": 6,
                        "numberOfTopResults": 3,
                        "includeAudiobooks": false,
                        "includeArtistHasConcertsField": false,
                        "includePreReleases": false
                    ],
                    "operationName": "searchDesktop",
                    "extensions": [
                        "persistedQuery": [
                            "version": 1,
                            "sha256Hash": hash
                        ]
                    ]
                ]
                guard let url = URL(string: "https://api-partner.spotify.com/pathfinder/v2/query") else { continue }
                var req = URLRequest(url: url)
                req.httpMethod = "POST"
                applySpotifyWebHeaders(&req, creds: creds)
                req.setValue("application/json", forHTTPHeaderField: "Content-Type")
                req.httpBody = try? JSONSerialization.data(withJSONObject: body)

                guard let (data, resp) = try? await session.data(for: req),
                      let http = resp as? HTTPURLResponse,
                      (200..<300).contains(http.statusCode),
                      let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let dataObj = root["data"] as? [String: Any],
                      let searchV2 = dataObj["searchV2"] as? [String: Any],
                      let tracksV2 = searchV2["tracksV2"] as? [String: Any],
                      let items = tracksV2["items"] as? [[String: Any]] else {
                    continue
                }

                for wrapper in items {
                    guard let itemData = (wrapper["item"] as? [String: Any])?["data"] as? [String: Any],
                          let rawId = (itemData["id"] as? String) ?? (itemData["uri"] as? String),
                          let sid = normalizeSpotifyId(rawId),
                          let hitTitle = itemData["name"] as? String else {
                        continue
                    }
                    let artistItems = ((itemData["artists"] as? [String: Any])?["items"] as? [[String: Any]]) ?? []
                    let hitArtist = artistItems.compactMap { ($0["profile"] as? [String: Any])?["name"] as? String }.joined(separator: " ")
                    let durMs = ((itemData["duration"] as? [String: Any])?["totalMilliseconds"] as? Int) ?? 0
                    let s = matchScore(
                        wantedTitle: title,
                        wantedArtist: artist,
                        wantedDurationSec: durationSec,
                        candidateTitle: hitTitle,
                        candidateArtist: hitArtist,
                        candidateDurationSec: durMs / 1000
                    )
                    if s >= 65 {
                        candidates.append((sid, s))
                    }
                }
                if !candidates.isEmpty { break }
            }
        }

        return candidates.sorted(by: { $0.score > $1.score }).map(\.id)
    }

    private func resolveSpotifyCanvasMP4(spotifyId: String, useAuthenticatedTiers: Bool) async -> URL? {
        if useAuthenticatedTiers {
            for attempt in 0..<2 {
                if let creds = await SpotifyTokenHarvester.shared.credentials(forceRefresh: attempt > 0) {
                    if let official = await queryAuthenticatedSpotifyCanvas(spotifyId: spotifyId, creds: creds) {
                        return official
                    }
                }
            }
        }
        return await queryPublicCanvasStore(spotifyId: spotifyId)
    }

    private func queryAuthenticatedSpotifyCanvas(spotifyId: String, creds: SpotifySessionCredentials) async -> URL? {
        let trackUri = "spotify:track:\(spotifyId)"

        if let pfURL = await queryPathfinderCanvas(trackUri: trackUri, creds: creds) {
            return pfURL
        }
        if let spURL = await querySpclientCanvazProtobuf(trackUri: trackUri, creds: creds) {
            return spURL
        }
        return nil
    }

    private func queryPathfinderCanvas(trackUri: String, creds: SpotifySessionCredentials) async -> URL? {
        let liveHash = await MainActor.run { SpotifyTokenHarvester.shared.pathfinderHash(for: "canvas") }
        var hashes: [String] = []
        if let liveHash = liveHash { hashes.append(liveHash) }
        for h in knownCanvasHashes where !hashes.contains(h) { hashes.append(h) }

        let endpoints = [
            "https://api-partner.spotify.com/pathfinder/v1/query",
            "https://api-partner.spotify.com/pathfinder/v2/query"
        ]

        for hash in hashes {
            let variableSets: [[String: Any]] = [
                ["trackUri": trackUri],
                ["uri": trackUri]
            ]
            for vars in variableSets {
                let payload: [String: Any] = [
                    "variables": vars,
                    "operationName": "canvas",
                    "extensions": [
                        "persistedQuery": [
                            "version": 1,
                            "sha256Hash": hash
                        ]
                    ]
                ]
                guard let bodyData = try? JSONSerialization.data(withJSONObject: payload) else { continue }

                for endpoint in endpoints {
                    guard let url = URL(string: endpoint) else { continue }
                    var req = URLRequest(url: url)
                    req.httpMethod = "POST"
                    applySpotifyWebHeaders(&req, creds: creds)
                    req.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    req.httpBody = bodyData

                    guard let (data, resp) = try? await session.data(for: req),
                          let http = resp as? HTTPURLResponse else {
                        continue
                    }
                    if http.statusCode == 401 {
                        await MainActor.run { SpotifyTokenHarvester.shared.invalidate() }
                        return nil
                    }
                    guard (200..<300).contains(http.statusCode),
                          let rawText = String(data: data, encoding: .utf8) else {
                        continue
                    }
                    if let parsed = extractCanvasURLFromJSON(data: data, rawText: rawText) {
                        return parsed
                    }
                }
            }
        }
        return nil
    }

    private func querySpclientCanvazProtobuf(trackUri: String, creds: SpotifySessionCredentials) async -> URL? {
        let hosts = await resolveSpclientHosts()
        let protoBody = encodeCanvasProtobufRequest(trackUri: trackUri)
        let browserUA = await MainActor.run { SpotifyTokenHarvester.browserUserAgent }
        let userAgents: [(ua: String, webHeaders: Bool)] = [
            ("Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)", false),
            (browserUA, true),
            ("Spotify/9.0.34.593 Android/35 (Pixel 9)", false)
        ]

        for host in hosts.prefix(3) {
            guard let url = URL(string: "https://\(host)/canvaz-cache/v0/canvases") else { continue }
            for profile in userAgents {
                var req = URLRequest(url: url)
                req.httpMethod = "POST"
                req.setValue("Bearer \(creds.accessToken)", forHTTPHeaderField: "Authorization")
                if let ct = creds.clientToken, !ct.isEmpty {
                    req.setValue(ct, forHTTPHeaderField: "client-token")
                }
                if profile.webHeaders {
                    req.setValue("WebPlayer", forHTTPHeaderField: "app-platform")
                    req.setValue(creds.clientVersion, forHTTPHeaderField: "spotify-app-version")
                    req.setValue("https://open.spotify.com", forHTTPHeaderField: "Origin")
                    req.setValue("https://open.spotify.com/", forHTTPHeaderField: "Referer")
                }
                req.setValue("application/protobuf", forHTTPHeaderField: "Content-Type")
                req.setValue("application/protobuf", forHTTPHeaderField: "Accept")
                req.setValue("en", forHTTPHeaderField: "Accept-Language")
                req.setValue(profile.ua, forHTTPHeaderField: "User-Agent")
                req.httpBody = protoBody

                guard let (data, resp) = try? await session.data(for: req),
                      let http = resp as? HTTPURLResponse else {
                    continue
                }
                if http.statusCode == 401 {
                    await MainActor.run { SpotifyTokenHarvester.shared.invalidate() }
                    return nil
                }
                if (200..<300).contains(http.statusCode) && !data.isEmpty {
                    if let decoded = decodeCanvasProtobufResponse(data: data, wantedUri: trackUri) {
                        return decoded
                    }
                }
            }
        }
        return nil
    }

    private func resolveSpclientHosts() async -> [String] {
        if !spclientHosts.isEmpty && Date() < spclientExpiresAt {
            return spclientHosts
        }
        var found: [String] = []
        if let url = URL(string: "https://apresolve.spotify.com/?type=spclient"),
           let (data, resp) = try? await session.data(from: url),
           let http = resp as? HTTPURLResponse,
           (200..<300).contains(http.statusCode),
           let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let arr = json["spclient"] as? [String] {
            for entry in arr {
                let host = entry.components(separatedBy: ":").first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                if host.hasSuffix(".spotify.com") && !found.contains(host) {
                    found.append(host)
                }
            }
        }
        for fallback in defaultSpclientHosts where !found.contains(fallback) {
            found.append(fallback)
        }
        spclientHosts = found
        spclientExpiresAt = Date().addingTimeInterval(1800)
        return found
    }

    private func queryPublicCanvasStore(spotifyId: String) async -> URL? {
        guard let url = URL(string: "https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist/\(spotifyId)") else {
            return nil
        }
        var req = URLRequest(url: url)
        req.timeoutInterval = 6
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("Derbis/1.8 (iOS; SpotifyCanvasProvider)", forHTTPHeaderField: "User-Agent")

        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let dataObj = json["data"] as? [String: Any],
              let trackObj = dataObj[spotifyId] as? [String: Any],
              let canvasStr = (trackObj["url"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
              canvasStr.hasPrefix("https://"),
              let canvasURL = URL(string: canvasStr) else {
            return nil
        }
        return canvasURL
    }

    private func resolveAppleMusicAnimatedArtwork(
        track: Track,
        cleanTitle: String,
        cleanArtist: String,
        preferTall: Bool
    ) async -> MotionCanvasAsset? {
        var albumCandidates: [String] = []
        let trimmedTrackAlbum = track.album.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedTrackAlbum.isEmpty {
            albumCandidates.append(trimmedTrackAlbum)
        }

        if let itunesAlbum = await lookupCanonicalAlbumViaITunes(title: cleanTitle, artist: cleanArtist),
           !albumCandidates.contains(itunesAlbum) {
            albumCandidates.append(itunesAlbum)
        }

        if !cleanTitle.isEmpty && !albumCandidates.contains(cleanTitle) {
            albumCandidates.append(cleanTitle)
        }

        let endpoints = [
            "https://artwork.m8tec.top",
            "https://amaa.tromme.app"
        ]

        for album in albumCandidates.prefix(2) {
            guard !cleanArtist.isEmpty, !album.isEmpty,
                  let encArtist = cleanArtist.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
                  let encAlbum = album.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
                continue
            }

            for base in endpoints {
                guard let url = URL(string: "\(base)/api/v1/artwork/search?artist=\(encArtist)&album=\(encAlbum)") else {
                    continue
                }
                var req = URLRequest(url: url)
                req.timeoutInterval = 6
                req.setValue("application/json", forHTTPHeaderField: "Accept")
                req.setValue("Derbis/1.8 (iOS; AppleMusicAnimatedArtwork)", forHTTPHeaderField: "User-Agent")

                guard let (data, resp) = try? await session.data(for: req),
                      let http = resp as? HTTPURLResponse,
                      (200..<300).contains(http.statusCode),
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                    continue
                }

                let tallStr = ((json["url_tall"] as? String) ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let squareStr = ((json["url"] as? String) ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let pickedStr = preferTall
                    ? (tallStr.isEmpty ? squareStr : tallStr)
                    : (squareStr.isEmpty ? tallStr : squareStr)

                guard pickedStr.hasPrefix("https://"), let streamURL = URL(string: pickedStr) else {
                    continue
                }

                let isHLS = pickedStr.lowercased().contains(".m3u8")
                if isHLS {
                    guard await verifyHLSStream(streamURL) else { continue }
                }

                return MotionCanvasAsset(
                    trackId: track.id,
                    remoteURL: streamURL,
                    playbackURL: streamURL,
                    provider: .appleMusicMotion,
                    isHLS: isHLS,
                    isTall: !tallStr.isEmpty && pickedStr == tallStr
                )
            }
        }
        return nil
    }

    private func lookupCanonicalAlbumViaITunes(title: String, artist: String) async -> String? {
        let term = "\(title) \(artist)".trimmingCharacters(in: .whitespacesAndNewlines)
        guard !term.isEmpty,
              let encoded = term.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://itunes.apple.com/search?media=music&entity=song&limit=5&term=\(encoded)") else {
            return nil
        }
        var req = URLRequest(url: url)
        req.timeoutInterval = 4.5
        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let results = json["results"] as? [[String: Any]] else {
            return nil
        }

        for item in results {
            guard let trackName = item["trackName"] as? String,
                  let artistName = item["artistName"] as? String,
                  let collectionName = item["collectionName"] as? String else {
                continue
            }
            let score = matchScore(
                wantedTitle: title,
                wantedArtist: artist,
                wantedDurationSec: 0,
                candidateTitle: trackName,
                candidateArtist: artistName,
                candidateDurationSec: 0
            )
            if score >= 65 {
                let cleanedAlbum = collectionName
                    .replacingOccurrences(of: " - Single", with: "")
                    .replacingOccurrences(of: " - EP", with: "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                if !cleanedAlbum.isEmpty {
                    return cleanedAlbum
                }
            }
        }
        return nil
    }

    private func verifyHLSStream(_ url: URL) async -> Bool {
        var req = URLRequest(url: url)
        req.timeoutInterval = 4.5
        guard let (data, resp) = try? await session.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let prefix = String(data: data.prefix(200), encoding: .utf8) else {
            return false
        }
        return prefix.contains("#EXTM3U")
    }

    private func cacheMP4LocallyIfPossible(remoteURL: URL) async -> URL? {
        let dir = canvasCacheDirectory()
        let digest = SHA256.hash(data: Data(remoteURL.absoluteString.utf8))
            .compactMap { String(format: "%02x", $0) }
            .joined()
        let fileURL = dir.appendingPathComponent("\(digest).mp4")

        if FileManager.default.fileExists(atPath: fileURL.path) {
            try? FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: fileURL.path)
            return fileURL
        }

        var req = URLRequest(url: remoteURL)
        req.timeoutInterval = 12
        req.setValue("*/*", forHTTPHeaderField: "Accept")

        guard let (tempURL, resp) = try? await session.download(for: req),
              let http = resp as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            return nil
        }

        let size = (try? tempURL.resourceValues(forKeys: [.fileSizeKey]))?.fileSize ?? 0
        let ctype = (http.value(forHTTPHeaderField: "Content-Type") ?? "").lowercased()
        guard size >= 16_384, !ctype.contains("text/html"), !ctype.contains("application/json") else {
            try? FileManager.default.removeItem(at: tempURL)
            return nil
        }

        try? FileManager.default.removeItem(at: fileURL)
        do {
            try FileManager.default.moveItem(at: tempURL, to: fileURL)
            pruneCanvasDiskCache(maxBytes: 72 * 1024 * 1024)
            return fileURL
        } catch {
            return nil
        }
    }

    private func canvasCacheDirectory() -> URL {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        let dir = caches.appendingPathComponent("DerbisCanvas", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    private func pruneCanvasDiskCache(maxBytes: Int) {
        let dir = canvasCacheDirectory()
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: dir,
            includingPropertiesForKeys: [.fileSizeKey, .contentModificationDateKey],
            options: [.skipsHiddenFiles]
        ) else {
            return
        }

        var entries: [(url: URL, size: Int, date: Date)] = []
        var total = 0
        for file in files {
            let vals = try? file.resourceValues(forKeys: [.fileSizeKey, .contentModificationDateKey])
            let sz = vals?.fileSize ?? 0
            let dt = vals?.contentModificationDate ?? .distantPast
            total += sz
            entries.append((file, sz, dt))
        }

        guard total > maxBytes else { return }
        entries.sort { $0.date < $1.date }
        for item in entries {
            try? FileManager.default.removeItem(at: item.url)
            total -= item.size
            if total <= maxBytes * 3 / 4 {
                break
            }
        }
    }

    private func applySpotifyWebHeaders(_ req: inout URLRequest, creds: SpotifySessionCredentials) {
        req.setValue("Bearer \(creds.accessToken)", forHTTPHeaderField: "Authorization")
        if let ct = creds.clientToken, !ct.isEmpty {
            req.setValue(ct, forHTTPHeaderField: "client-token")
        }
        req.setValue(creds.clientVersion, forHTTPHeaderField: "spotify-app-version")
        req.setValue("WebPlayer", forHTTPHeaderField: "app-platform")
        req.setValue("https://open.spotify.com", forHTTPHeaderField: "Origin")
        req.setValue("https://open.spotify.com/", forHTTPHeaderField: "Referer")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            forHTTPHeaderField: "User-Agent"
        )
    }

    private func extractCanvasURLFromJSON(data: Data, rawText: String) -> URL? {
        if let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let dataObj = root["data"] as? [String: Any] {
            if let trackUnion = dataObj["trackUnion"] as? [String: Any],
               let canvasObj = trackUnion["canvas"] as? [String: Any] {
                let candidate = ((canvasObj["url"] as? String) ?? (canvasObj["canvasUrl"] as? String) ?? "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                if candidate.hasPrefix("https://"), let u = URL(string: candidate) {
                    return u
                }
            }
            if let canvasRoot = dataObj["canvas"] as? [String: Any],
               let list = canvasRoot["canvasesList"] as? [[String: Any]],
               let first = list.first {
                let candidate = ((first["canvasUrl"] as? String) ?? (first["url"] as? String) ?? "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                if candidate.hasPrefix("https://"), let u = URL(string: candidate) {
                    return u
                }
            }
        }

        let unescaped = rawText.replacingOccurrences(of: "\\/", with: "/")
        let pattern = #"https://[A-Za-z0-9.\-]*(?:scdn\.co|spotifycdn\.com)/[^"'\s\\]+\.mp4(?:\?[^"'\s\\]*)?"#
        if let regex = try? NSRegularExpression(pattern: pattern),
           let match = regex.firstMatch(in: unescaped, range: NSRange(unescaped.startIndex..<unescaped.endIndex, in: unescaped)),
           let r = Range(match.range, in: unescaped) {
            return URL(string: String(unescaped[r]))
        }
        return nil
    }

    private func encodeCanvasProtobufRequest(trackUri: String) -> Data {
        let uriBytes = Array(trackUri.utf8)
        var trackMsg = Data()
        appendProtoBytes(to: &trackMsg, field: 1, bytes: uriBytes)

        var reqMsg = Data()
        appendProtoBytes(to: &reqMsg, field: 1, bytes: Array(trackMsg))
        return reqMsg
    }

    private func decodeCanvasProtobufResponse(data: Data, wantedUri: String) -> URL? {
        let bytes = Array(data)
        var entries: [(url: String, trackUri: String?)] = []
        var idx = 0

        while idx < bytes.count {
            guard let header = readVarint(bytes: bytes, start: idx) else { break }
            idx = header.next
            let field = Int(header.value >> 3)
            let wire = Int(header.value & 7)
            if field == 1 && wire == 2 {
                guard let len = readVarint(bytes: bytes, start: idx) else { break }
                let start = len.next
                let end = start + Int(len.value)
                guard end <= bytes.count else { break }
                if let entry = decodeSingleCanvasEntry(bytes: Array(bytes[start..<end])) {
                    entries.append(entry)
                }
                idx = end
            } else {
                guard let skipped = skipProtoField(bytes: bytes, index: idx, wire: wire) else { break }
                idx = skipped
            }
        }

        if let exact = entries.first(where: { $0.trackUri == wantedUri }),
           exact.url.hasPrefix("https://"),
           let url = URL(string: exact.url) {
            return url
        }
        if let first = entries.first,
           first.url.hasPrefix("https://"),
           let url = URL(string: first.url) {
            return url
        }
        if let latin = String(data: data, encoding: .isoLatin1) {
            return extractCanvasURLFromJSON(data: Data(), rawText: latin)
        }
        return nil
    }

    private func decodeSingleCanvasEntry(bytes: [UInt8]) -> (url: String, trackUri: String?)? {
        var foundURL: String?
        var foundURI: String?
        var idx = 0

        while idx < bytes.count {
            guard let header = readVarint(bytes: bytes, start: idx) else { break }
            idx = header.next
            let field = Int(header.value >> 3)
            let wire = Int(header.value & 7)
            if wire == 2 && (field == 1 || field == 2 || field == 5) {
                guard let len = readVarint(bytes: bytes, start: idx) else { break }
                let start = len.next
                let end = start + Int(len.value)
                guard end <= bytes.count else { break }
                if let text = String(bytes: bytes[start..<end], encoding: .utf8) {
                    if field == 2 { foundURL = text }
                    if field == 5 { foundURI = text }
                }
                idx = end
            } else {
                guard let skipped = skipProtoField(bytes: bytes, index: idx, wire: wire) else { break }
                idx = skipped
            }
        }

        guard let u = foundURL else { return nil }
        return (u, foundURI)
    }

    private func readVarint(bytes: [UInt8], start: Int) -> (value: UInt64, next: Int)? {
        var value: UInt64 = 0
        var shift: UInt64 = 0
        var idx = start
        while idx < bytes.count && shift <= 63 {
            let b = bytes[idx]
            value |= UInt64(b & 0x7F) << shift
            idx += 1
            if (b & 0x80) == 0 {
                return (value, idx)
            }
            shift += 7
        }
        return nil
    }

    private func skipProtoField(bytes: [UInt8], index: Int, wire: Int) -> Int? {
        switch wire {
        case 0:
            return readVarint(bytes: bytes, start: index)?.next
        case 1:
            let end = index + 8
            return end <= bytes.count ? end : nil
        case 2:
            guard let len = readVarint(bytes: bytes, start: index) else { return nil }
            let end = len.next + Int(len.value)
            return end <= bytes.count ? end : nil
        case 5:
            let end = index + 4
            return end <= bytes.count ? end : nil
        default:
            return nil
        }
    }

    private func appendVarint(to data: inout Data, value raw: UInt64) {
        var v = raw
        while v >= 0x80 {
            data.append(UInt8((v & 0x7F) | 0x80))
            v >>= 7
        }
        data.append(UInt8(v & 0x7F))
    }

    private func appendProtoBytes(to data: inout Data, field: Int, bytes: [UInt8]) {
        appendVarint(to: &data, value: UInt64((field << 3) | 2))
        appendVarint(to: &data, value: UInt64(bytes.count))
        data.append(contentsOf: bytes)
    }

    private func normalizeSpotifyId(_ raw: String) -> String? {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.hasPrefix("spotify:track:") {
            s = String(s.dropFirst("spotify:track:".count))
        } else if let r = s.range(of: "/track/") {
            s = String(s[r.upperBound...])
        }
        if let q = s.firstIndex(of: "?") {
            s = String(s[..<q])
        }
        if let sl = s.firstIndex(of: "/") {
            s = String(s[..<sl])
        }
        guard s.count == 22,
              s.range(of: #"^[A-Za-z0-9]{22}$"#, options: .regularExpression) != nil else {
            return nil
        }
        return s
    }

    private func cleanTrackTitle(_ title: String) -> String {
        title
            .replacingOccurrences(
                of: #"[\(\[](?i:official[^)\]]*|music\s*video[^)\]]*|lyric[^)\]]*|audio[^)\]]*|visualizer[^)\]]*|hd|4k|mv)[\)\]]"#,
                with: "",
                options: .regularExpression
            )
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func cleanArtistName(_ artist: String) -> String {
        artist
            .replacingOccurrences(of: " - Topic", with: "")
            .replacingOccurrences(of: "VEVO", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func normalizeComparisonTokens(_ text: String) -> Set<String> {
        let lowered = text.lowercased()
            .replacingOccurrences(of: "[^a-z0-9\\s]", with: " ", options: .regularExpression)
        let parts = lowered.split(separator: " ").map(String.init).filter { $0.count >= 2 }
        return Set(parts)
    }

    private func matchScore(
        wantedTitle: String,
        wantedArtist: String,
        wantedDurationSec: Int,
        candidateTitle: String,
        candidateArtist: String,
        candidateDurationSec: Int
    ) -> Int {
        let wt = normalizeComparisonTokens(wantedTitle)
        let ct = normalizeComparisonTokens(candidateTitle)
        guard !wt.isEmpty && !ct.isEmpty else { return 0 }

        let titleOverlap = Double(wt.intersection(ct).count) / Double(max(1, wt.count))
        var score = Int(titleOverlap * 62.0)

        let wa = normalizeComparisonTokens(wantedArtist)
        let ca = normalizeComparisonTokens(candidateArtist)
        if !wa.isEmpty && !ca.isEmpty {
            let artistOverlap = Double(wa.intersection(ca).count) / Double(max(1, min(wa.count, 3)))
            score += Int(min(1.0, artistOverlap) * 28.0)
        }

        if wantedDurationSec > 0 && candidateDurationSec > 0 {
            let diff = abs(wantedDurationSec - candidateDurationSec)
            if diff <= 4 {
                score += 12
            } else if diff <= 12 {
                score += 6
            } else if diff > 35 {
                score -= 20
            }
        } else {
            score += 6
        }

        return max(0, min(100, score))
    }
}
