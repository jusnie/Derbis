import Foundation
import CryptoKit
import Security
import Combine
import os

enum GoogleAccountSyncError: LocalizedError {
    case sessionExpired
    case invalidCookie
    case networkFailure(Int)
    case malformedResponse

    var errorDescription: String? {
        switch self {
        case .sessionExpired:
            return "Google session expired. Please sign in again."
        case .invalidCookie:
            return "Sign-in did not include Google session cookies."
        case .networkFailure(let code):
            return "Google sync returned HTTP \(code)."
        case .malformedResponse:
            return "Unexpected response format from Google Music."
        }
    }
}

extension Notification.Name {
    static let derbisGoogleAccountDidChange = Notification.Name("derbis.googleAccountDidChange")
}

private struct PersistedGoogleSessionsBundle: Codable {
    var sessions: [GoogleMusicSession]
    var activeAccountId: String?
}

private struct NonisolatedAuthHeaderSnapshot {
    var cookie: String = ""
    var authUser: Int = 0
    var pageId: String?
    var personalizedHomeEnabled: Bool = true
}

@MainActor
final class GoogleAccountSyncService: ObservableObject {
    static let shared = GoogleAccountSyncService()

    nonisolated static let origin = "https://music.youtube.com"
    nonisolated static let loginURL = URL(
        string: "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%26feature%3D__FEATURE__&hl=en"
    )!
    nonisolated static let mobileSafariUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
    nonisolated private static let webClientVersion = "1.20260804.16.00"
    nonisolated private static let desktopUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
    nonisolated private static let keychainService = "com.derbis.ios.googleaccount"
    nonisolated private static let keychainAccount = "sessions_v1"

    nonisolated(unsafe) private static let snapshotLock: UnsafeMutablePointer<os_unfair_lock_s> = {
        let ptr = UnsafeMutablePointer<os_unfair_lock_s>.allocate(capacity: 1)
        ptr.initialize(to: os_unfair_lock_s())
        return ptr
    }()

    nonisolated(unsafe) private static let authSnapshotPtr: UnsafeMutablePointer<NonisolatedAuthHeaderSnapshot> = {
        let ptr = UnsafeMutablePointer<NonisolatedAuthHeaderSnapshot>.allocate(capacity: 1)
        ptr.initialize(to: NonisolatedAuthHeaderSnapshot())
        return ptr
    }()

    @Published private(set) var sessions: [GoogleMusicSession] = []
    @Published private(set) var activeSession: GoogleMusicSession?
    @Published private(set) var isSyncing: Bool = false
    @Published private(set) var isVerifyingLogin: Bool = false
    @Published private(set) var lastSyncedAt: Date?
    @Published var statusMessage: String?
    @Published var errorMessage: String?

    @Published var twoWayLikesSync: Bool {
        didSet { UserDefaults.standard.set(twoWayLikesSync, forKey: "derbis.google.twoWayLikesSync") }
    }

    @Published var autoImportPlaylists: Bool {
        didSet { UserDefaults.standard.set(autoImportPlaylists, forKey: "derbis.google.autoImportPlaylists") }
    }

    @Published var personalizedHomeEnabled: Bool {
        didSet {
            UserDefaults.standard.set(personalizedHomeEnabled, forKey: "derbis.google.personalizedHomeEnabled")
            publishNonisolatedSnapshot()
            NotificationCenter.default.post(name: .derbisGoogleAccountDidChange, object: nil)
        }
    }

    @Published var syncListeningHistory: Bool {
        didSet { UserDefaults.standard.set(syncListeningHistory, forKey: "derbis.google.syncListeningHistory") }
    }

    private let urlSession: URLSession

    private init() {
        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 14
        cfg.timeoutIntervalForResource = 28
        cfg.httpShouldSetCookies = false
        self.urlSession = URLSession(configuration: cfg)

        if UserDefaults.standard.object(forKey: "derbis.google.twoWayLikesSync") == nil {
            self.twoWayLikesSync = true
        } else {
            self.twoWayLikesSync = UserDefaults.standard.bool(forKey: "derbis.google.twoWayLikesSync")
        }

        if UserDefaults.standard.object(forKey: "derbis.google.autoImportPlaylists") == nil {
            self.autoImportPlaylists = true
        } else {
            self.autoImportPlaylists = UserDefaults.standard.bool(forKey: "derbis.google.autoImportPlaylists")
        }

        if UserDefaults.standard.object(forKey: "derbis.google.personalizedHomeEnabled") == nil {
            self.personalizedHomeEnabled = true
        } else {
            self.personalizedHomeEnabled = UserDefaults.standard.bool(forKey: "derbis.google.personalizedHomeEnabled")
        }

        if UserDefaults.standard.object(forKey: "derbis.google.syncListeningHistory") == nil {
            self.syncListeningHistory = true
        } else {
            self.syncListeningHistory = UserDefaults.standard.bool(forKey: "derbis.google.syncListeningHistory")
        }

        if let epoch = UserDefaults.standard.object(forKey: "derbis.google.lastSyncedAt") as? Double, epoch > 0 {
            self.lastSyncedAt = Date(timeIntervalSince1970: epoch)
        }

        restoreSessionsFromKeychain()
        publishNonisolatedSnapshot()

        if activeSession != nil {
            Task { @MainActor [weak self] in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                await self?.syncAccountDataToLibrary(silent: true)
            }
        }
    }

    var isSignedIn: Bool {
        activeSession != nil
    }

    var activeAccount: GoogleMusicAccount? {
        activeSession?.account
    }

    private var effectiveLanguage: String {
        Locale.current.language.languageCode?.identifier ?? "en"
    }

    private var effectiveCountry: String {
        if let reg = Locale.current.region?.identifier, reg.count == 2 {
            return reg.uppercased()
        }
        return "US"
    }

    nonisolated static func parseCookieMap(_ rawCookie: String) -> [String: String] {
        var map: [String: String] = [:]
        for part in rawCookie.split(separator: ";") {
            let trimmed = part.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let eqIdx = trimmed.firstIndex(of: "=") else { continue }
            let key = String(trimmed[..<eqIdx]).trimmingCharacters(in: .whitespaces)
            let val = String(trimmed[trimmed.index(after: eqIdx)...]).trimmingCharacters(in: .whitespaces)
            if !key.isEmpty && !val.isEmpty {
                map[key] = val
            }
        }
        return map
    }

    nonisolated static func hasValidSAPISID(_ rawCookie: String) -> Bool {
        let map = parseCookieMap(rawCookie)
        let sap = map["SAPISID"] ?? map["__Secure-3PAPISID"] ?? ""
        return !sap.isEmpty
    }

    nonisolated static func authorizationHeader(for rawCookie: String, epochSeconds: Int64 = Int64(Date().timeIntervalSince1970)) -> String? {
        let map = parseCookieMap(rawCookie)
        guard let sapisid = map["SAPISID"] ?? map["__Secure-3PAPISID"], !sapisid.isEmpty else {
            return nil
        }
        let input = "\(epochSeconds) \(sapisid) \(origin)"
        let digest = Insecure.SHA1.hash(data: Data(input.utf8))
        let hex = digest.map { String(format: "%02x", $0) }.joined()
        return "SAPISIDHASH \(epochSeconds)_\(hex)"
    }

    nonisolated static func currentAuthenticatedHeaders(requirePersonalizedHomeToggle: Bool = false) -> [String: String]? {
        os_unfair_lock_lock(snapshotLock)
        let snap = authSnapshotPtr.pointee
        os_unfair_lock_unlock(snapshotLock)

        guard !snap.cookie.isEmpty else { return nil }
        if requirePersonalizedHomeToggle && !snap.personalizedHomeEnabled {
            return nil
        }
        guard let auth = authorizationHeader(for: snap.cookie) else { return nil }
        var headers: [String: String] = [
            "Cookie": snap.cookie,
            "Authorization": auth,
            "Origin": origin,
            "x-origin": origin,
            "Referer": "\(origin)/",
            "X-Goog-Authuser": String(snap.authUser),
            "X-Goog-Api-Format-Version": "1"
        ]
        if let pageId = snap.pageId, !pageId.isEmpty {
            headers["X-Goog-Pageid"] = pageId
        }
        return headers
    }

    private func publishNonisolatedSnapshot() {
        var next = NonisolatedAuthHeaderSnapshot()
        if let session = activeSession {
            next.cookie = session.cookie
            next.authUser = session.account.authUser
            next.pageId = session.account.pageId
        }
        next.personalizedHomeEnabled = personalizedHomeEnabled

        os_unfair_lock_lock(Self.snapshotLock)
        Self.authSnapshotPtr.pointee = next
        os_unfair_lock_unlock(Self.snapshotLock)
    }

    func completeLogin(withCookieHeader rawCookie: String) async -> Bool {
        let cleaned = rawCookie
            .replacingOccurrences(of: "\r", with: "")
            .replacingOccurrences(of: "\n", with: "; ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard Self.hasValidSAPISID(cleaned) else {
            errorMessage = GoogleAccountSyncError.invalidCookie.localizedDescription
            return false
        }

        isVerifyingLogin = true
        errorMessage = nil
        statusMessage = "Verifying Google account…"

        do {
            var discovered = (try? await fetchSwitcherAccounts(cookie: cleaned)) ?? []
            if discovered.isEmpty {
                let fallbackSeed = GoogleMusicAccount(
                    id: Self.computeAccountId(handle: "primary", pageId: nil, authUser: 0),
                    name: "Google Account",
                    handle: "",
                    avatarURL: "",
                    pageId: nil,
                    authUser: 0,
                    isSelected: true
                )
                let seedSession = GoogleMusicSession(account: fallbackSeed, cookie: cleaned, updatedAt: Date())
                let verified = try await fetchActiveAccountMenu(session: seedSession)
                discovered = [verified]
            }

            let now = Date()
            var mergedSessions = sessions
            var chosenId: String?

            for acc in discovered {
                let newSession = GoogleMusicSession(account: acc, cookie: cleaned, updatedAt: now)
                if let existingIdx = mergedSessions.firstIndex(where: { $0.id == acc.id }) {
                    mergedSessions[existingIdx] = newSession
                } else {
                    mergedSessions.append(newSession)
                }
                if acc.isSelected || chosenId == nil {
                    chosenId = acc.id
                }
            }

            sessions = mergedSessions
            if let target = chosenId {
                activeSession = mergedSessions.first(where: { $0.id == target }) ?? mergedSessions.first
            } else {
                activeSession = mergedSessions.first
            }

            saveSessionsToKeychain()
            publishNonisolatedSnapshot()
            isVerifyingLogin = false
            NotificationCenter.default.post(name: .derbisGoogleAccountDidChange, object: nil)

            await syncAccountDataToLibrary(silent: false)
            return true
        } catch {
            isVerifyingLogin = false
            errorMessage = error.localizedDescription
            statusMessage = nil
            return false
        }
    }

    func selectAccount(id: String) {
        guard let found = sessions.first(where: { $0.id == id }) else { return }
        activeSession = found
        saveSessionsToKeychain()
        publishNonisolatedSnapshot()
        NotificationCenter.default.post(name: .derbisGoogleAccountDidChange, object: nil)
        Task { @MainActor in
            await syncAccountDataToLibrary(silent: false)
        }
    }

    func signOut(accountId: String? = nil) {
        if let targetId = accountId {
            sessions.removeAll { $0.id == targetId }
            if activeSession?.id == targetId {
                activeSession = sessions.first
            }
        } else {
            sessions.removeAll()
            activeSession = nil
        }
        saveSessionsToKeychain()
        publishNonisolatedSnapshot()
        statusMessage = activeSession == nil ? "Signed out of Google account" : "Switched active account"
        errorMessage = nil
        NotificationCenter.default.post(name: .derbisGoogleAccountDidChange, object: nil)
    }

    func syncAccountDataToLibrary(silent: Bool = false) async {
        guard let session = activeSession, !isSyncing else { return }
        isSyncing = true
        if !silent {
            errorMessage = nil
            statusMessage = "Syncing your Google library…"
        }

        do {
            if let refreshedAcc = try? await fetchActiveAccountMenu(session: session) {
                var updatedSession = session
                updatedSession.account = refreshedAcc
                updatedSession.updatedAt = Date()
                activeSession = updatedSession
                if let idx = sessions.firstIndex(where: { $0.id == session.id }) {
                    sessions[idx] = updatedSession
                }
                saveSessionsToKeychain()
                publishNonisolatedSnapshot()
            }

            guard let currentSession = activeSession else {
                isSyncing = false
                return
            }

            let likedTracks = try await fetchTracks(
                session: currentSession,
                browseId: GoogleCloudCollectionKind.likes.browseId,
                maxPages: 4
            )
            let mergedLikesCount = LibraryStore.shared.mergeCloudLikedTracks(likedTracks)

            var syncedPlaylistsCount = 0
            if autoImportPlaylists {
                let entries = (try? await fetchCollectionEntries(session: currentSession, collection: .playlists)) ?? []
                let userPlaylists = entries
                    .filter { $0.browseId != "VLLM" && $0.browseId != "VLSE" }
                    .sorted { ($0.isOwned ? 1 : 0) > ($1.isOwned ? 1 : 0) }
                    .prefix(10)

                for pl in userPlaylists {
                    if let plTracks = try? await fetchTracks(
                        session: currentSession,
                        browseId: pl.browseId,
                        maxPages: 2,
                        fallbackArtist: pl.subtitle,
                        fallbackThumbnail: pl.thumbnailURL
                    ), !plTracks.isEmpty {
                        _ = LibraryStore.shared.upsertCloudPlaylist(
                            cloudId: "\(currentSession.account.id):\(pl.browseId)",
                            title: pl.title,
                            tracks: plTracks
                        )
                        syncedPlaylistsCount += 1
                    }
                }
            }

            if syncListeningHistory {
                if let historyTracks = try? await fetchTracks(
                    session: currentSession,
                    browseId: GoogleCloudCollectionKind.history.browseId,
                    maxPages: 1
                ), !historyTracks.isEmpty {
                    _ = LibraryStore.shared.mergeCloudHistoryTracks(historyTracks)
                }
            }

            let now = Date()
            lastSyncedAt = now
            UserDefaults.standard.set(now.timeIntervalSince1970, forKey: "derbis.google.lastSyncedAt")

            if autoImportPlaylists && syncedPlaylistsCount > 0 {
                statusMessage = "Synced \(likedTracks.count) liked songs (\(mergedLikesCount) new) & \(syncedPlaylistsCount) playlists"
            } else {
                statusMessage = "Synced \(likedTracks.count) liked songs (\(mergedLikesCount) new)"
            }
            errorMessage = nil
            NotificationCenter.default.post(name: .derbisGoogleAccountDidChange, object: nil)
        } catch let syncErr as GoogleAccountSyncError {
            if case .sessionExpired = syncErr {
                errorMessage = syncErr.localizedDescription
            } else if !silent {
                errorMessage = syncErr.localizedDescription
            }
        } catch {
            if !silent {
                errorMessage = "Sync failed. Check your connection and try again."
            }
        }

        isSyncing = false
    }

    func fetchCloudCollection(_ collection: GoogleCloudCollectionKind) async throws -> (entries: [GoogleCloudLibraryEntry], tracks: [Track]) {
        guard let session = activeSession else {
            throw GoogleAccountSyncError.sessionExpired
        }
        switch collection {
        case .likes, .history:
            let tracks = try await fetchTracks(session: session, browseId: collection.browseId, maxPages: 3)
            return ([], tracks)
        case .playlists, .albums, .artists:
            let entries = try await fetchCollectionEntries(session: session, collection: collection)
            return (entries, [])
        }
    }

    func fetchEntryDetailTracks(_ entry: GoogleCloudLibraryEntry) async throws -> [Track] {
        guard let session = activeSession else {
            throw GoogleAccountSyncError.sessionExpired
        }
        return try await fetchTracks(
            session: session,
            browseId: entry.browseId,
            maxPages: 4,
            fallbackArtist: entry.subtitle,
            fallbackAlbum: entry.collection == .albums ? entry.title : "",
            fallbackThumbnail: entry.thumbnailURL
        )
    }

    func pushLikeStateIfEnabled(track: Track, isLiked: Bool) {
        guard twoWayLikesSync,
              let session = activeSession,
              track.source == .youtubeMusic,
              let videoId = track.videoId,
              !videoId.isEmpty else {
            return
        }
        let endpoint = isLiked ? "youtubei/v1/like/like" : "youtubei/v1/like/removelike"
        Task {
            var body = baseContextPayload()
            body["target"] = ["videoId": videoId]
            _ = try? await authorizedRequest(session: session, path: endpoint, body: body)
        }
    }

    func reportPlaybackIfEnabled(track: Track) {
        guard syncListeningHistory,
              let session = activeSession,
              track.source == .youtubeMusic,
              let videoId = track.videoId,
              !videoId.isEmpty else {
            return
        }
        Task {
            var body = baseContextPayload()
            body["videoId"] = videoId
            guard let playerRoot = try? await authorizedRequest(session: session, path: "youtubei/v1/player", body: body),
                  let tracking = playerRoot["playbackTracking"] as? [String: Any],
                  let videostats = tracking["videostatsPlaybackUrl"] as? [String: Any],
                  let baseUrlStr = videostats["baseUrl"] as? String,
                  var comps = URLComponents(string: baseUrlStr),
                  comps.scheme == "https",
                  let host = comps.host?.lowercased(),
                  (host == "youtube.com" || host.hasSuffix(".youtube.com")) else {
                return
            }

            let alphabet = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_")
            let cpn = String((0..<16).compactMap { _ in alphabet.randomElement() })

            var items = comps.queryItems ?? []
            items.removeAll { $0.name == "ver" || $0.name == "c" || $0.name == "cpn" }
            items.append(URLQueryItem(name: "ver", value: "2"))
            items.append(URLQueryItem(name: "c", value: "WEB_REMIX"))
            items.append(URLQueryItem(name: "cpn", value: cpn))
            comps.queryItems = items
            guard let pingURL = comps.url,
                  let req = buildAuthorizedURLRequest(session: session, url: pingURL, body: nil) else {
                return
            }
            _ = try? await urlSession.data(for: req)
        }
    }

    private func fetchSwitcherAccounts(cookie: String) async throws -> [GoogleMusicAccount] {
        let dummy = GoogleMusicAccount(
            id: "seed",
            name: "",
            handle: "",
            avatarURL: "",
            pageId: nil,
            authUser: 0,
            isSelected: false
        )
        let seedSession = GoogleMusicSession(account: dummy, cookie: cookie, updatedAt: Date())
        let root = try await authorizedRequest(session: seedSession, path: "getAccountSwitcherEndpoint", body: nil)

        var itemNodes: [[String: Any]] = []
        Self.collectDictionaries(named: "accountItem", in: root, into: &itemNodes)

        var results: [GoogleMusicAccount] = []
        var seenIds = Set<String>()

        for item in itemNodes {
            if (item["isDisabled"] as? Bool) == true { continue }
            let name = Self.extractText(item["accountName"])
            guard !name.isEmpty else { continue }

            var tokenDicts: [[String: Any]] = []
            if let serviceEndpoint = item["serviceEndpoint"] {
                Self.collectDictionaries(named: "accountSigninToken", in: serviceEndpoint, into: &tokenDicts)
            }
            let signinURL = tokenDicts.first?["signinUrl"] as? String ?? ""
            let authUser = Self.extractAuthUserIndex(from: signinURL)

            var pageId: String? = (item["onBehalfOfParameter"] as? String).flatMap {
                ($0.isEmpty || $0 == "null") ? nil : $0
            }
            if pageId == nil, let serviceEndpoint = item["serviceEndpoint"] {
                var pageTokens: [[String: Any]] = []
                Self.collectDictionaries(named: "pageIdToken", in: serviceEndpoint, into: &pageTokens)
                if let pid = pageTokens.first?["pageId"] as? String, !pid.isEmpty {
                    pageId = pid
                }
            }

            let rawHandle = Self.extractText(item["channelHandle"])
            let handle = rawHandle.isEmpty ? Self.extractText(item["accountByline"]) : rawHandle
            let avatar = Self.extractBestThumbnail(item["accountPhoto"])
            let selected = (item["isSelected"] as? Bool) ?? false

            let identitySeed = handle.isEmpty ? (signinURL.isEmpty ? name : signinURL) : handle
            let accId = Self.computeAccountId(handle: identitySeed, pageId: pageId, authUser: authUser)
            if seenIds.insert(accId).inserted {
                results.append(
                    GoogleMusicAccount(
                        id: accId,
                        name: name,
                        handle: handle,
                        avatarURL: avatar,
                        pageId: pageId,
                        authUser: authUser,
                        isSelected: selected
                    )
                )
            }
        }
        return results
    }

    private func fetchActiveAccountMenu(session: GoogleMusicSession) async throws -> GoogleMusicAccount {
        let root = try await authorizedRequest(
            session: session,
            path: "youtubei/v1/account/account_menu",
            body: baseContextPayload()
        )
        var headers: [[String: Any]] = []
        Self.collectDictionaries(named: "activeAccountHeaderRenderer", in: root, into: &headers)
        guard let header = headers.first else {
            throw GoogleAccountSyncError.sessionExpired
        }
        let name = Self.extractText(header["accountName"])
        guard !name.isEmpty else {
            throw GoogleAccountSyncError.malformedResponse
        }
        let handle = Self.extractText(header["channelHandle"])
        let avatar = Self.extractBestThumbnail(header["accountPhoto"])

        var updated = session.account
        updated.name = name
        if !handle.isEmpty { updated.handle = handle }
        if !avatar.isEmpty { updated.avatarURL = avatar }
        if updated.id == "seed" || updated.id.isEmpty {
            updated = GoogleMusicAccount(
                id: Self.computeAccountId(handle: handle.isEmpty ? name : handle, pageId: updated.pageId, authUser: updated.authUser),
                name: name,
                handle: handle,
                avatarURL: avatar,
                pageId: updated.pageId,
                authUser: updated.authUser,
                isSelected: true
            )
        }
        return updated
    }

    private func fetchCollectionEntries(
        session: GoogleMusicSession,
        collection: GoogleCloudCollectionKind
    ) async throws -> [GoogleCloudLibraryEntry] {
        let root = try await authorizedRequest(
            session: session,
            path: "youtubei/v1/browse",
            body: browsePayload(browseId: collection.browseId)
        )

        var twoRowItems: [[String: Any]] = []
        Self.collectDictionaries(named: "musicTwoRowItemRenderer", in: root, into: &twoRowItems)
        var responsiveItems: [[String: Any]] = []
        Self.collectDictionaries(named: "musicResponsiveListItemRenderer", in: root, into: &responsiveItems)

        var entries: [GoogleCloudLibraryEntry] = []
        var seenBrowseIds = Set<String>()

        for renderer in (twoRowItems + responsiveItems) {
            guard let browseId = Self.extractBrowseId(from: renderer),
                  !browseId.isEmpty,
                  seenBrowseIds.insert(browseId).inserted else {
                continue
            }

            if collection == .playlists && !browseId.hasPrefix("VL") && !browseId.hasPrefix("PL") {
                continue
            }
            if collection == .albums && !browseId.hasPrefix("MPRE") && !browseId.hasPrefix("OLAK") {
                continue
            }
            if collection == .artists && !browseId.hasPrefix("UC") && !browseId.hasPrefix("MPLA") {
                continue
            }

            let title: String
            let subtitle: String
            if renderer["title"] != nil {
                title = Self.extractText(renderer["title"])
                subtitle = Self.extractText(renderer["subtitle"])
            } else if let cols = renderer["flexColumns"] as? [[String: Any]], !cols.isEmpty {
                title = Self.extractFirstRun(cols[0])
                subtitle = cols.count > 1 ? Self.extractJoinedRuns(cols[1]) : ""
            } else {
                continue
            }

            guard !title.isEmpty else { continue }
            let thumb = Self.extractBestThumbnail(renderer["thumbnailRenderer"] ?? renderer["thumbnail"])
            let owned = Self.detectOwnedPlaylist(renderer: renderer)

            let normalizedBrowseId = (collection == .playlists && !browseId.hasPrefix("VL"))
                ? "VL\(browseId)"
                : browseId

            entries.append(
                GoogleCloudLibraryEntry(
                    browseId: normalizedBrowseId,
                    title: title,
                    subtitle: subtitle,
                    thumbnailURL: thumb,
                    collection: collection,
                    isOwned: owned
                )
            )
        }

        return entries
    }

    private func fetchTracks(
        session: GoogleMusicSession,
        browseId: String,
        maxPages: Int,
        fallbackArtist: String = "",
        fallbackAlbum: String = "",
        fallbackThumbnail: String = ""
    ) async throws -> [Track] {
        var collected: [Track] = []
        var seenTrackIds = Set<String>()
        var continuationToken = ""

        for page in 0..<max(1, maxPages) {
            let payload: [String: Any]
            if page == 0 {
                payload = browsePayload(browseId: browseId)
            } else {
                guard !continuationToken.isEmpty else { break }
                payload = browsePayload(continuation: continuationToken)
            }

            let root = try await authorizedRequest(
                session: session,
                path: "youtubei/v1/browse",
                body: payload
            )

            let headerThumb = fallbackThumbnail.isEmpty ? Self.extractHeaderThumbnail(from: root) : fallbackThumbnail
            var rowRenderers: [[String: Any]] = []
            Self.collectDictionaries(named: "musicResponsiveListItemRenderer", in: root, into: &rowRenderers)

            for row in rowRenderers {
                if let track = Self.parseTrackFromResponsiveRow(
                    row,
                    fallbackArtist: fallbackArtist,
                    fallbackAlbum: fallbackAlbum,
                    fallbackThumbnail: headerThumb
                ), seenTrackIds.insert(track.id).inserted {
                    collected.append(track)
                }
            }

            continuationToken = Self.extractContinuationToken(from: root)
            if continuationToken.isEmpty {
                break
            }
        }

        return collected
    }

    private func baseContextPayload() -> [String: Any] {
        [
            "context": [
                "client": [
                    "clientName": "WEB_REMIX",
                    "clientVersion": Self.webClientVersion,
                    "hl": effectiveLanguage,
                    "gl": effectiveCountry
                ],
                "user": [
                    "lockedSafetyMode": false
                ]
            ]
        ]
    }

    private func browsePayload(browseId: String = "", continuation: String = "") -> [String: Any] {
        var payload = baseContextPayload()
        if !continuation.isEmpty {
            payload["continuation"] = continuation
        } else if !browseId.isEmpty {
            payload["browseId"] = browseId
        }
        return payload
    }

    private func buildAuthorizedURLRequest(
        session: GoogleMusicSession,
        url: URL,
        body: [String: Any]?
    ) -> URLRequest? {
        guard let authHeader = Self.authorizationHeader(for: session.cookie) else {
            return nil
        }
        var req = URLRequest(url: url)
        req.httpMethod = body == nil ? "GET" : "POST"
        req.setValue(session.cookie, forHTTPHeaderField: "Cookie")
        req.setValue(authHeader, forHTTPHeaderField: "Authorization")
        req.setValue(Self.origin, forHTTPHeaderField: "Origin")
        req.setValue(Self.origin, forHTTPHeaderField: "x-origin")
        req.setValue("\(Self.origin)/", forHTTPHeaderField: "Referer")
        req.setValue(String(session.account.authUser), forHTTPHeaderField: "X-Goog-Authuser")
        req.setValue("67", forHTTPHeaderField: "X-YouTube-Client-Name")
        req.setValue(Self.webClientVersion, forHTTPHeaderField: "X-YouTube-Client-Version")
        req.setValue("1", forHTTPHeaderField: "X-Goog-Api-Format-Version")
        req.setValue(Self.desktopUserAgent, forHTTPHeaderField: "User-Agent")
        if let pid = session.account.pageId, !pid.isEmpty {
            req.setValue(pid, forHTTPHeaderField: "X-Goog-Pageid")
        }
        if let body = body {
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            req.httpBody = try? JSONSerialization.data(withJSONObject: body)
        }
        return req
    }

    private func authorizedRequest(
        session: GoogleMusicSession,
        path: String,
        body: [String: Any]?
    ) async throws -> [String: Any] {
        guard let url = URL(string: "\(Self.origin)/\(path)?prettyPrint=false"),
              let req = buildAuthorizedURLRequest(session: session, url: url, body: body) else {
            throw GoogleAccountSyncError.invalidCookie
        }

        let (data, resp) = try await urlSession.data(for: req)
        guard let http = resp as? HTTPURLResponse else {
            throw GoogleAccountSyncError.malformedResponse
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw GoogleAccountSyncError.sessionExpired
        }
        guard (200...299).contains(http.statusCode) else {
            throw GoogleAccountSyncError.networkFailure(http.statusCode)
        }

        var rawString = String(data: data, encoding: .utf8) ?? ""
        if rawString.hasPrefix(")]}'\n") {
            rawString.removeFirst(5)
        } else if rawString.hasPrefix(")]}'") {
            rawString.removeFirst(4)
        }

        guard let cleanData = rawString.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: cleanData) as? [String: Any] else {
            throw GoogleAccountSyncError.malformedResponse
        }

        if let ctx = json["responseContext"] as? [String: Any],
           (ctx["loggedOut"] as? Bool) == true {
            throw GoogleAccountSyncError.sessionExpired
        }

        return json
    }

    private func saveSessionsToKeychain() {
        let bundle = PersistedGoogleSessionsBundle(
            sessions: sessions,
            activeAccountId: activeSession?.id
        )
        guard let encoded = try? JSONEncoder().encode(bundle) else { return }

        let baseQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.keychainService,
            kSecAttrAccount as String: Self.keychainAccount
        ]
        SecItemDelete(baseQuery as CFDictionary)

        guard !sessions.isEmpty else { return }
        var insert = baseQuery
        insert[kSecValueData as String] = encoded
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(insert as CFDictionary, nil)
    }

    private func restoreSessionsFromKeychain() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.keychainService,
            kSecAttrAccount as String: Self.keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess,
              let data = item as? Data,
              let decoded = try? JSONDecoder().decode(PersistedGoogleSessionsBundle.self, from: data) else {
            return
        }
        self.sessions = decoded.sessions
        if let activeId = decoded.activeAccountId {
            self.activeSession = decoded.sessions.first(where: { $0.id == activeId }) ?? decoded.sessions.first
        } else {
            self.activeSession = decoded.sessions.first
        }
    }

    private static func computeAccountId(handle: String, pageId: String?, authUser: Int) -> String {
        let raw = "\(handle)|\(pageId ?? "")|\(authUser)"
        let digest = SHA256.hash(data: Data(raw.utf8))
        return digest.prefix(12).map { String(format: "%02x", $0) }.joined()
    }

    private static func extractAuthUserIndex(from urlString: String) -> Int {
        guard let regex = try? NSRegularExpression(pattern: #"[?&]authuser=(\d+)"#) else { return 0 }
        let range = NSRange(urlString.startIndex..<urlString.endIndex, in: urlString)
        guard let match = regex.firstMatch(in: urlString, range: range),
              let digitRange = Range(match.range(at: 1), in: urlString) else {
            return 0
        }
        return Int(urlString[digitRange]) ?? 0
    }

    private static func collectDictionaries(named targetKey: String, in node: Any, depth: Int = 0, into output: inout [[String: Any]]) {
        guard depth <= 56 else { return }
        if let dict = node as? [String: Any] {
            for (k, v) in dict {
                if k == targetKey, let matched = v as? [String: Any] {
                    output.append(matched)
                }
                collectDictionaries(named: targetKey, in: v, depth: depth + 1, into: &output)
            }
        } else if let arr = node as? [Any] {
            for el in arr {
                collectDictionaries(named: targetKey, in: el, depth: depth + 1, into: &output)
            }
        }
    }

    private static func extractText(_ node: Any?) -> String {
        guard let dict = node as? [String: Any] else { return "" }
        if let simple = dict["simpleText"] as? String, !simple.isEmpty {
            return simple.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if let runs = dict["runs"] as? [[String: Any]] {
            return runs.compactMap { $0["text"] as? String }.joined().trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return ""
    }

    private static func extractFirstRun(_ node: Any?) -> String {
        var runsList: [[[String: Any]]] = []
        collectRunsArrays(in: node, into: &runsList)
        for runs in runsList {
            if let first = runs.first?["text"] as? String, !first.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return first.trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        return ""
    }

    private static func extractJoinedRuns(_ node: Any?) -> String {
        var runsList: [[[String: Any]]] = []
        collectRunsArrays(in: node, into: &runsList)
        let texts = runsList.flatMap { runs in runs.compactMap { $0["text"] as? String } }
        return texts.joined().trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func collectRunsArrays(in node: Any?, depth: Int = 0, into out: inout [[[String: Any]]]) {
        guard depth <= 20, let node = node else { return }
        if let dict = node as? [String: Any] {
            if let runs = dict["runs"] as? [[String: Any]] {
                out.append(runs)
            }
            for (_, v) in dict {
                collectRunsArrays(in: v, depth: depth + 1, into: &out)
            }
        } else if let arr = node as? [Any] {
            for el in arr {
                collectRunsArrays(in: el, depth: depth + 1, into: &out)
            }
        }
    }

    private static func extractBestThumbnail(_ node: Any?) -> String {
        guard let node = node else { return "" }
        var bestURL = ""
        var bestWidth = -1

        func walk(_ current: Any, depth: Int) {
            guard depth <= 24 else { return }
            if let dict = current as? [String: Any] {
                if let thumbs = dict["thumbnails"] as? [[String: Any]] {
                    for t in thumbs {
                        if let u = t["url"] as? String, !u.isEmpty {
                            let w = (t["width"] as? Int) ?? 0
                            if w >= bestWidth {
                                bestWidth = w
                                bestURL = u
                            }
                        }
                    }
                }
                for (_, v) in dict { walk(v, depth: depth + 1) }
            } else if let arr = current as? [Any] {
                for el in arr { walk(el, depth: depth + 1) }
            }
        }

        walk(node, depth: 0)
        if bestURL.hasPrefix("//") {
            return "https:\(bestURL)"
        }
        return bestURL
    }

    private static func extractHeaderThumbnail(from root: [String: Any]) -> String {
        for key in ["musicDetailHeaderRenderer", "musicResponsiveHeaderRenderer", "musicEditablePlaylistDetailHeaderRenderer"] {
            var headers: [[String: Any]] = []
            collectDictionaries(named: key, in: root, into: &headers)
            if let first = headers.first {
                let thumb = extractBestThumbnail(first)
                if !thumb.isEmpty { return thumb }
            }
        }
        return ""
    }

    private static func extractBrowseId(from renderer: [String: Any]) -> String? {
        if let nav = renderer["navigationEndpoint"] as? [String: Any],
           let browse = nav["browseEndpoint"] as? [String: Any],
           let id = browse["browseId"] as? String, !id.isEmpty {
            return id
        }
        var endpoints: [[String: Any]] = []
        collectDictionaries(named: "browseEndpoint", in: renderer, into: &endpoints)
        for ep in endpoints {
            if let id = ep["browseId"] as? String, !id.isEmpty {
                return id
            }
        }
        return nil
    }

    private static func detectOwnedPlaylist(renderer: [String: Any]) -> Bool {
        guard let menu = renderer["menu"] else { return false }
        var icons: [[String: Any]] = []
        collectDictionaries(named: "icon", in: menu, into: &icons)
        collectDictionaries(named: "defaultIcon", in: menu, into: &icons)
        let types = icons.compactMap { $0["iconType"] as? String }
        return types.contains("EDIT") || types.contains("DELETE")
    }

    private static func extractContinuationToken(from root: [String: Any]) -> String {
        var nextNodes: [[String: Any]] = []
        collectDictionaries(named: "nextContinuationData", in: root, into: &nextNodes)
        for node in nextNodes {
            if let token = node["continuation"] as? String, !token.isEmpty {
                return token
            }
        }
        var cmdNodes: [[String: Any]] = []
        collectDictionaries(named: "continuationCommand", in: root, into: &cmdNodes)
        for node in cmdNodes {
            if let token = node["token"] as? String, !token.isEmpty {
                return token
            }
        }
        return ""
    }

    static func parseTrackFromResponsiveRow(
        _ renderer: [String: Any],
        fallbackArtist: String = "",
        fallbackAlbum: String = "",
        fallbackThumbnail: String = ""
    ) -> Track? {
        var videoId = (renderer["playlistItemData"] as? [String: Any])?["videoId"] as? String
        if videoId == nil {
            var watchNodes: [[String: Any]] = []
            collectDictionaries(named: "watchEndpoint", in: renderer, into: &watchNodes)
            videoId = watchNodes.first?["videoId"] as? String
        }
        guard let vid = videoId, vid.count == 11 else { return nil }

        guard let flexCols = renderer["flexColumns"] as? [[String: Any]], !flexCols.isEmpty else {
            return nil
        }

        let title = extractFirstRun(flexCols[0])
        guard !title.isEmpty else { return nil }

        var rawTokens: [String] = []
        if flexCols.count > 1 {
            for col in flexCols.dropFirst() {
                var runsArrays: [[[String: Any]]] = []
                collectRunsArrays(in: col, into: &runsArrays)
                for runs in runsArrays {
                    for run in runs {
                        if let t = run["text"] as? String {
                            rawTokens.append(t)
                        }
                    }
                }
            }
        }

        let ignored: Set<String> = ["•", "·", "&", "song", "video"]
        var meta: [String] = []
        var durationSec = 0

        for raw in rawTokens {
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, !ignored.contains(trimmed.lowercased()) else { continue }
            if let dur = parseClockDuration(trimmed), dur > 0 {
                durationSec = dur
            } else {
                meta.append(trimmed)
            }
        }

        if durationSec == 0, let fixedCols = renderer["fixedColumns"] as? [[String: Any]] {
            for col in fixedCols {
                let text = extractJoinedRuns(col)
                if let dur = parseClockDuration(text), dur > 0 {
                    durationSec = dur
                    break
                }
            }
        }

        let artist = meta.first ?? (fallbackArtist.isEmpty ? "Unknown Artist" : fallbackArtist)
        let album = meta.count > 1 ? meta[1] : fallbackAlbum

        let rowThumb = extractBestThumbnail(renderer["thumbnail"])
        let baseThumb = rowThumb.isEmpty ? fallbackThumbnail : rowThumb
        let finalThumb = baseThumb.isEmpty ? "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg" : baseThumb
        let hiResThumb = finalThumb.replacingOccurrences(of: "w120-h120", with: "w544-h544")
            .replacingOccurrences(of: "w60-h60", with: "w544-h544")

        return Track(
            id: "yt:\(vid)",
            title: title,
            artist: artist,
            album: album,
            durationSec: durationSec,
            thumbnailURL: finalThumb,
            largeThumbnailURL: hiResThumb,
            source: .youtubeMusic,
            codecBadge: "AAC 256k",
            bitrateKbps: 256
        )
    }

    private static func parseClockDuration(_ text: String) -> Int? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let parts = trimmed.split(separator: ":")
        guard parts.count == 2 || parts.count == 3 else { return nil }
        var total = 0
        for p in parts {
            guard let n = Int(p) else { return nil }
            total = total * 60 + n
        }
        return total
    }
}
