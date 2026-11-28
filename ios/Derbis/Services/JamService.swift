import CryptoKit
import Darwin
import Foundation
import Network
import SwiftUI
import UIKit

enum JamRole: String, Sendable {
    case host
    case guest
}

enum JamTransportMode: String, CaseIterable, Identifiable, Sendable {
    case lan = "lan"
    case server = "server"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .lan: return "Local Wi-Fi / Hotspot"
        case .server: return "Remote Internet Relay"
        }
    }

    var shortBadge: String {
        switch self {
        case .lan: return "LAN Wi-Fi"
        case .server: return "Remote Relay"
        }
    }

    static func fromId(_ raw: String?) -> JamTransportMode {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() else {
            return .lan
        }
        return JamTransportMode(rawValue: trimmed) ?? .lan
    }
}

enum JamGuestPermission: String, CaseIterable, Identifiable, Sendable {
    case hostOnly = "host_only"
    case addSongs = "add_songs"
    case collaborative = "collaborative"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .hostOnly: return "Host Only"
        case .addSongs: return "Add Songs"
        case .collaborative: return "Collaborative"
        }
    }

    var subtitle: String {
        switch self {
        case .hostOnly: return "Guests listen in sync; host controls queue & playback"
        case .addSongs: return "Guests can add tracks to the shared Jam queue"
        case .collaborative: return "Everyone can add tracks, play/pause, skip & seek"
        }
    }

    var canAddTracks: Bool { self != .hostOnly }
    var canControlPlayback: Bool { self == .collaborative }

    static func fromId(_ raw: String?) -> JamGuestPermission {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() else {
            return .hostOnly
        }
        return JamGuestPermission(rawValue: trimmed) ?? .hostOnly
    }
}

enum JamConnectionState: String, Sendable {
    case idle
    case connecting
    case awaitingApproval
    case connected
    case disconnected
}

enum JamFailure: String, Sendable {
    case InvalidCode
    case ConnectionFailed
    case NotAuthorized
    case HostEnded
    case ProtocolError
    case Rejected
    case Banned
    case SessionLocked
    case SessionFull
    case Removed

    var userMessage: String {
        switch self {
        case .InvalidCode:
            return "Invalid Listen Together session code. Paste a 38-letter Derbis Jam code or link."
        case .ConnectionFailed:
            return "Could not reach the Jam room. Check Wi-Fi / Hotspot or relay connection and try again."
        case .NotAuthorized:
            return "That action or room authentication wasn't permitted by the host."
        case .HostEnded:
            return "The host ended this Listen Together session."
        case .ProtocolError:
            return "Jam protocol version mismatch. Make sure both devices are up to date."
        case .Rejected:
            return "Your join request was declined or timed out."
        case .Banned:
            return "You have been blocked from rejoining this Jam room."
        case .SessionLocked:
            return "This Jam room is currently locked by the host."
        case .SessionFull:
            return "This Jam room is full (up to 8 listeners)."
        case .Removed:
            return "The host removed you from this Jam room."
        }
    }
}

struct JamTrack: Identifiable, Equatable, Hashable, Sendable {
    let id: String
    var title: String
    var artist: String
    var durationMs: Int64
    var thumbnailUrl: String
    var addedBy: String = ""

    static func fromDerbisTrack(_ track: Track, addedBy: String = "") -> JamTrack {
        let wireId: String
        if let vid = track.videoId, !vid.isEmpty {
            wireId = vid
        } else {
            wireId = track.id
        }
        let thumbCandidate = track.largeThumbnailURL.isEmpty ? track.thumbnailURL : track.largeThumbnailURL
        let cleanThumb = thumbCandidate.lowercased().hasPrefix("https://") ? thumbCandidate : ""
        return JamTrack(
            id: wireId,
            title: String(track.title.prefix(JamSessionState.maxTextLength)),
            artist: String(track.artist.prefix(JamSessionState.maxTextLength)),
            durationMs: Int64(max(0, track.durationSec)) * 1000,
            thumbnailUrl: String(cleanThumb.prefix(JamSessionState.maxTextLength)),
            addedBy: String(addedBy.prefix(JamSessionState.maxTextLength))
        )
    }

    func toDerbisTrack() -> Track {
        let normalizedId: String
        if id.hasPrefix("yt:") || id.hasPrefix("sub:") || id.hasPrefix("local:") {
            normalizedId = id
        } else {
            normalizedId = "yt:\(id)"
        }
        let fallbackThumb = thumbnailUrl.isEmpty && !id.contains(":")
            ? "https://i.ytimg.com/vi/\(id)/hqdefault.jpg"
            : thumbnailUrl
        return Track(
            id: normalizedId,
            title: title.isEmpty ? "Jam Track" : title,
            artist: artist.isEmpty ? "Derbis Jam" : artist,
            album: "",
            durationSec: Int(max(0, durationMs / 1000)),
            thumbnailURL: fallbackThumb,
            largeThumbnailURL: fallbackThumb,
            source: .youtubeMusic
        )
    }
}

struct JamParticipant: Identifiable, Equatable, Sendable {
    let id: String
    let name: String
    let isHost: Bool
}

struct JamPendingParticipant: Identifiable, Equatable, Sendable {
    var id: String { participantId }
    let participantId: String
    let guestId: String
    let name: String
    let requestedAtElapsedMs: Int64
}

struct NearbyJamRoom: Identifiable, Equatable, Sendable {
    var id: String { code }
    let serviceName: String
    let hostName: String
    let currentTrackTitle: String
    let code: String
}

struct JamSessionState: Equatable, Sendable {
    static let maxQueueSize = 200
    static let maxParticipants = 8
    static let maxPending = 8
    static let maxBanned = 32
    static let maxNameLength = 32
    static let maxTextLength = 200
    static let batchAddCapability = "batch_add_tracks_v1"
    static let moderationCapability = "moderation_v1"

    var sessionId: String
    var hostId: String
    var revision: Int64
    var createdAt: Int64
    var participants: [JamParticipant]
    var queue: [JamTrack]
    var currentIndex: Int
    var currentMediaId: String
    var positionMs: Int64
    var playWhenReady: Bool
    var shuffle: Bool
    var repeatMode: Int
    var permission: JamGuestPermission
    var updatedAtElapsedMs: Int64
    var capabilities: Set<String>
    var locked: Bool
    var requireApproval: Bool
}

enum JamAction: Equatable, Sendable {
    case addTrack(JamTrack)
    case addTracks([JamTrack])
    case playNextTracks([JamTrack])
    case removeTrack(String)
    case selectIndex(Int)
    case setPlayWhenReady(Bool)
    case seek(Int64)
    case next
    case previous

    var isPlaybackControl: Bool {
        switch self {
        case .addTrack, .addTracks, .playNextTracks:
            return false
        case .removeTrack, .selectIndex, .setPlayWhenReady, .seek, .next, .previous:
            return true
        }
    }

    func isAllowed(for permission: JamGuestPermission) -> Bool {
        switch self {
        case .addTrack, .addTracks:
            return permission.canAddTracks
        case .playNextTracks:
            return false
        default:
            return isPlaybackControl && permission.canControlPlayback
        }
    }
}

struct JamSessionCode: Equatable, Sendable {
    static let secretBytes = 16
    static let addressBytes = 4
    static let portBytes = 2
    static let totalBytes = addressBytes + portBytes + secretBytes
    static let encodedLength = 38
    static let deepLinkPrefix = "derbis://jam/"
    static let webSharePrefix = "https://derbis-web.vercel.app/jam/"
    static let minPort = 1024
    static let maxPort = 65_535
    static let remoteMarkerAddress = "10.254.254.254"
    static let remoteMarkerPort = 54_443

    private static let alphabet = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZ")

    let hostAddress: String
    let port: Int
    let secret: String

    var isRemoteRelayCode: Bool {
        hostAddress == Self.remoteMarkerAddress && port == Self.remoteMarkerPort
    }

    func encoded() -> String? {
        guard Self.isPrivateIpv4(hostAddress),
              (Self.minPort...Self.maxPort).contains(port),
              Self.isValidSecret(secret),
              let secretRaw = Self.decodeHex(secret),
              secretRaw.count == Self.secretBytes else {
            return nil
        }
        let octets = hostAddress.split(separator: ".").compactMap { UInt8($0) }
        guard octets.count == Self.addressBytes else { return nil }

        var payload = [UInt8](repeating: 0, count: Self.totalBytes)
        for i in 0..<Self.addressBytes {
            payload[i] = octets[i]
        }
        payload[Self.addressBytes] = UInt8((port >> 8) & 0xFF)
        payload[Self.addressBytes + 1] = UInt8(port & 0xFF)
        for i in 0..<Self.secretBytes {
            payload[Self.addressBytes + Self.portBytes + i] = secretRaw[i]
        }
        return Self.encodeBase26(payload)
    }

    var deepLink: String {
        guard let code = encoded() else { return "derbis://jam" }
        return "\(Self.deepLinkPrefix)\(code)"
    }

    var webShareURL: String {
        guard let code = encoded() else { return "https://derbis-web.vercel.app" }
        return "\(Self.webSharePrefix)\(code)"
    }

    static func newSecret() -> String {
        var bytes = [UInt8](repeating: 0, count: secretBytes)
        _ = SecRandomCopyBytes(kSecRandomDefault, secretBytes, &bytes)
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    static func newGuestIdentity() -> String {
        newSecret()
    }

    static func isValidSecret(_ value: String) -> Bool {
        guard value.count == secretBytes * 2 else { return false }
        return value.allSatisfy { ch in
            ("0"..."9").contains(ch) || ("a"..."f").contains(ch) || ("A"..."F").contains(ch)
        }
    }

    static func sanitizeGuestIdentity(_ value: String) -> String {
        let lower = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard lower.count == 32,
              lower.allSatisfy({ ("0"..."9").contains($0) || ("a"..."f").contains($0) }) else {
            return ""
        }
        return lower
    }

    static func isPrivateIpv4(_ address: String) -> Bool {
        let parts = address.split(separator: ".", omittingEmptySubsequences: false)
        guard parts.count == 4 else { return false }
        var octets: [Int] = []
        octets.reserveCapacity(4)
        for p in parts {
            guard let v = Int(p), (0...255).contains(v) else { return false }
            octets.append(v)
        }
        if octets[0] == 10 { return true }
        if octets[0] == 172 && (16...31).contains(octets[1]) { return true }
        if octets[0] == 192 && octets[1] == 168 { return true }
        if octets[0] == 169 && octets[1] == 254 { return true }
        return false
    }

    static func extractCandidateLetters(from raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if let url = URL(string: trimmed),
           let comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            if let qCode = comps.queryItems?.first(where: { $0.name.lowercased() == "code" })?.value,
               !qCode.isEmpty {
                return qCode.uppercased().filter { ("A"..."Z").contains($0) }
            }
            if url.scheme?.lowercased() == "derbis", url.host?.lowercased() == "jam" {
                let pathCode = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                if !pathCode.isEmpty {
                    return pathCode.uppercased().filter { ("A"..."Z").contains($0) }
                }
            }
            let segments = url.pathComponents.filter { $0 != "/" }
            if let jamIdx = segments.firstIndex(where: { $0.lowercased() == "jam" }),
               jamIdx + 1 < segments.count {
                return segments[jamIdx + 1].uppercased().filter { ("A"..."Z").contains($0) }
            }
        }

        if trimmed.lowercased().hasPrefix(deepLinkPrefix) {
            let suffix = String(trimmed.dropFirst(deepLinkPrefix.count))
            return suffix.uppercased().filter { ("A"..."Z").contains($0) }
        }

        return trimmed.uppercased().filter { ("A"..."Z").contains($0) }
    }

    static func parse(_ raw: String) -> JamSessionCode? {
        let normalized = extractCandidateLetters(from: raw)
        guard normalized.count == encodedLength else { return nil }
        guard let bytes = decodeBase26(normalized), bytes.count == totalBytes else {
            return nil
        }
        let address = (0..<addressBytes).map { String(bytes[$0]) }.joined(separator: ".")
        let port = (Int(bytes[addressBytes]) << 8) | Int(bytes[addressBytes + 1])
        guard (minPort...maxPort).contains(port), isPrivateIpv4(address) else {
            return nil
        }
        let secret = bytes[(addressBytes + portBytes)..<totalBytes]
            .map { String(format: "%02x", $0) }
            .joined()
        return JamSessionCode(hostAddress: address, port: port, secret: secret)
    }

    private static func encodeBase26(_ bytes: [UInt8]) -> String? {
        var working = bytes
        var chars = Array(repeating: Character("A"), count: encodedLength)
        for outIdx in stride(from: encodedLength - 1, through: 0, by: -1) {
            var remainder = 0
            for i in 0..<working.count {
                let acc = remainder * 256 + Int(working[i])
                working[i] = UInt8(acc / 26)
                remainder = acc % 26
            }
            chars[outIdx] = alphabet[remainder]
        }
        guard working.allSatisfy({ $0 == 0 }) else { return nil }
        return String(chars)
    }

    private static func decodeBase26(_ text: String) -> [UInt8]? {
        var acc = [UInt8](repeating: 0, count: totalBytes + 1)
        for ch in text {
            guard let ascii = ch.asciiValue, ascii >= 65, ascii <= 90 else { return nil }
            var carry = Int(ascii - 65)
            for i in stride(from: totalBytes, through: 0, by: -1) {
                let prod = Int(acc[i]) * 26 + carry
                acc[i] = UInt8(prod & 0xFF)
                carry = prod >> 8
            }
            if carry != 0 || acc[0] != 0 {
                return nil
            }
        }
        return Array(acc[1...totalBytes])
    }

    private static func decodeHex(_ hex: String) -> [UInt8]? {
        let chars = Array(hex)
        guard chars.count == secretBytes * 2 else { return nil }
        var out: [UInt8] = []
        out.reserveCapacity(secretBytes)
        for i in stride(from: 0, to: chars.count, by: 2) {
            let pair = String([chars[i], chars[i + 1]])
            guard let byte = UInt8(pair, radix: 16) else { return nil }
            out.append(byte)
        }
        return out
    }
}

enum JamAuth {
    private static let guestAuthContext = "derbis-jam-guest-auth-v1"
    private static let hostWelcomeContext = "derbis-jam-host-welcome-v1"

    static func generateNonce() -> String {
        JamSessionCode.newSecret()
    }

    static func computeGuestProof(secret: String, hostNonce: String, guestNonce: String) -> String {
        hmacSHA256Hex(key: secret, message: "\(guestAuthContext):\(hostNonce):\(guestNonce)")
    }

    static func computeHostProof(secret: String, hostNonce: String, guestNonce: String) -> String {
        hmacSHA256Hex(key: secret, message: "\(hostWelcomeContext):\(hostNonce):\(guestNonce)")
    }

    static func verifyProof(expected: String, actual: String) -> Bool {
        guard !expected.isEmpty, expected.count == actual.count else { return false }
        let lhs = Array(expected.utf8)
        let rhs = Array(actual.utf8)
        var diff: UInt8 = 0
        for i in 0..<lhs.count {
            diff |= lhs[i] ^ rhs[i]
        }
        return diff == 0
    }

    private static func hmacSHA256Hex(key: String, message: String) -> String {
        let symKey = SymmetricKey(data: Data(key.utf8))
        let mac = HMAC<SHA256>.authenticationCode(for: Data(message.utf8), using: symKey)
        return mac.map { String(format: "%02x", $0) }.joined()
    }
}

enum JamWireMessage: Sendable {
    case challenge(hostNonce: String)
    case authenticate(guestNonce: String, name: String, proof: String, guestId: String)
    case welcome(sessionId: String, participantId: String, hostProof: String)
    case pending(hostProof: String)
    case state(sessionId: String, revision: Int64, source: String, timestamp: Int64, state: JamSessionState)
    case action(sessionId: String, participantId: String, action: JamAction)
    case bye(reason: String)
    case failure(JamFailure)
}

enum JamProtocol {
    static let version = 2
    static let maxMessageBytes = 512 * 1024

    static func sanitizeName(_ value: String) -> String {
        let filtered = value.unicodeScalars.filter { !CharacterSet.controlCharacters.contains($0) }
        let trimmed = String(String.UnicodeScalarView(filtered)).trimmingCharacters(in: .whitespacesAndNewlines)
        let clipped = String(trimmed.prefix(JamSessionState.maxNameLength))
        return clipped.isEmpty ? "Listener" : clipped
    }

    static func encode(_ message: JamWireMessage) -> String? {
        var dict: [String: Any] = ["v": version]
        switch message {
        case .challenge(let hostNonce):
            dict["t"] = "challenge"
            dict["hostNonce"] = hostNonce
        case .authenticate(let guestNonce, let name, let proof, let guestId):
            dict["t"] = "auth"
            dict["guestNonce"] = guestNonce
            dict["name"] = sanitizeName(name)
            dict["proof"] = proof
            dict["guestId"] = guestId
        case .welcome(let sessionId, let participantId, let hostProof):
            dict["t"] = "welcome"
            dict["sessionId"] = sessionId
            dict["participantId"] = participantId
            dict["hostProof"] = hostProof
        case .pending(let hostProof):
            dict["t"] = "pending"
            dict["hostProof"] = hostProof
        case .state(let sessionId, let revision, let source, let timestamp, let state):
            dict["t"] = "state"
            dict["sessionId"] = sessionId
            dict["revision"] = revision
            dict["source"] = source
            dict["timestamp"] = timestamp
            dict["payload"] = encodeState(state)
        case .action(let sessionId, let participantId, let action):
            dict["t"] = "action"
            dict["sessionId"] = sessionId
            dict["participantId"] = participantId
            dict["action"] = encodeAction(action)
        case .bye(let reason):
            dict["t"] = "bye"
            dict["reason"] = String(reason.prefix(JamSessionState.maxTextLength))
        case .failure(let failure):
            dict["t"] = "error"
            dict["code"] = failure.rawValue
        }
        guard let data = try? JSONSerialization.data(withJSONObject: dict, options: []),
              let str = String(data: data, encoding: .utf8) else {
            return nil
        }
        return str
    }

    static func decode(_ raw: String) -> JamWireMessage? {
        let utf8 = raw.utf8
        guard !utf8.isEmpty, utf8.count <= maxMessageBytes,
              let data = raw.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              intValue(root["v"], default: -1) == version,
              let type = root["t"] as? String else {
            return nil
        }

        switch type {
        case "challenge":
            let nonce = boundedText(root["hostNonce"])
            return nonce.isEmpty ? nil : .challenge(hostNonce: nonce)
        case "auth":
            let guestNonce = boundedText(root["guestNonce"])
            let proof = boundedText(root["proof"])
            guard !guestNonce.isEmpty, !proof.isEmpty else { return nil }
            let name = sanitizeName((root["name"] as? String) ?? "")
            let guestId = JamSessionCode.sanitizeGuestIdentity((root["guestId"] as? String) ?? "")
            return .authenticate(guestNonce: guestNonce, name: name, proof: proof, guestId: guestId)
        case "welcome":
            let sessionId = boundedText(root["sessionId"])
            let participantId = boundedText(root["participantId"])
            let hostProof = boundedText(root["hostProof"])
            guard !sessionId.isEmpty, !participantId.isEmpty, !hostProof.isEmpty else { return nil }
            return .welcome(sessionId: sessionId, participantId: participantId, hostProof: hostProof)
        case "pending":
            let hostProof = boundedText(root["hostProof"])
            return hostProof.isEmpty ? nil : .pending(hostProof: hostProof)
        case "state":
            return decodeState(root)
        case "action":
            return decodeAction(root)
        case "bye":
            return .bye(reason: boundedText(root["reason"]))
        case "error":
            guard let code = root["code"] as? String, let failure = JamFailure(rawValue: code) else {
                return nil
            }
            return .failure(failure)
        default:
            return nil
        }
    }

    private static func encodeState(_ state: JamSessionState) -> [String: Any] {
        let participantsArr: [[String: Any]] = state.participants
            .prefix(JamSessionState.maxParticipants)
            .map { ["id": $0.id, "name": $0.name, "isHost": $0.isHost] }
        let queueArr: [[String: Any]] = state.queue
            .prefix(JamSessionState.maxQueueSize)
            .map { encodeTrack($0) }
        return [
            "hostId": state.hostId,
            "createdAt": state.createdAt,
            "currentIndex": state.currentIndex,
            "currentMediaId": state.currentMediaId,
            "positionMs": state.positionMs,
            "playWhenReady": state.playWhenReady,
            "shuffle": state.shuffle,
            "repeatMode": state.repeatMode,
            "permission": state.permission.rawValue,
            "locked": state.locked,
            "requireApproval": state.requireApproval,
            "capabilities": Array(state.capabilities.prefix(16)),
            "participants": participantsArr,
            "queue": queueArr
        ]
    }

    private static func decodeState(_ root: [String: Any]) -> JamWireMessage? {
        let sessionId = boundedText(root["sessionId"])
        let revision = int64Value(root["revision"], default: -1)
        let source = boundedText(root["source"])
        let timestamp = int64Value(root["timestamp"], default: -1)
        guard !sessionId.isEmpty, revision >= 0, !source.isEmpty, timestamp >= 0,
              let payload = root["payload"] as? [String: Any] else {
            return nil
        }
        let hostId = boundedText(payload["hostId"])
        guard !hostId.isEmpty else { return nil }
        guard let participants = decodeParticipants(payload["participants"]),
              let queue = decodeTracks(payload["queue"]) else {
            return nil
        }
        let currentIndex = intValue(payload["currentIndex"], default: queue.isEmpty ? -1 : 0)
        if queue.isEmpty && currentIndex != -1 { return nil }
        if !queue.isEmpty && !queue.indices.contains(currentIndex) { return nil }
        let currentMediaId = boundedText(payload["currentMediaId"])
        let capsList = (payload["capabilities"] as? [Any])?.compactMap { $0 as? String } ?? []

        let state = JamSessionState(
            sessionId: sessionId,
            hostId: hostId,
            revision: revision,
            createdAt: max(0, int64Value(payload["createdAt"])),
            participants: participants,
            queue: queue,
            currentIndex: currentIndex,
            currentMediaId: currentMediaId,
            positionMs: max(0, int64Value(payload["positionMs"])),
            playWhenReady: boolValue(payload["playWhenReady"]),
            shuffle: boolValue(payload["shuffle"]),
            repeatMode: min(2, max(0, intValue(payload["repeatMode"]))),
            permission: JamGuestPermission.fromId(payload["permission"] as? String),
            updatedAtElapsedMs: 0,
            capabilities: Set(capsList.prefix(16)),
            locked: boolValue(payload["locked"]),
            requireApproval: boolValue(payload["requireApproval"])
        )
        return .state(sessionId: sessionId, revision: revision, source: source, timestamp: timestamp, state: state)
    }

    private static func encodeTrack(_ track: JamTrack) -> [String: Any] {
        [
            "id": track.id,
            "title": String(track.title.prefix(JamSessionState.maxTextLength)),
            "artist": String(track.artist.prefix(JamSessionState.maxTextLength)),
            "durationMs": track.durationMs,
            "thumbnailUrl": String(track.thumbnailUrl.prefix(JamSessionState.maxTextLength)),
            "addedBy": String(track.addedBy.prefix(JamSessionState.maxTextLength))
        ]
    }

    private static func decodeTrack(_ dict: [String: Any]?) -> JamTrack? {
        guard let dict = dict else { return nil }
        let id = boundedText(dict["id"])
        guard !id.isEmpty else { return nil }
        let rawThumb = boundedText(dict["thumbnailUrl"])
        let safeThumb = rawThumb.lowercased().hasPrefix("https://") ? rawThumb : ""
        return JamTrack(
            id: id,
            title: boundedText(dict["title"]),
            artist: boundedText(dict["artist"]),
            durationMs: max(0, int64Value(dict["durationMs"])),
            thumbnailUrl: safeThumb,
            addedBy: boundedText(dict["addedBy"])
        )
    }

    private static func decodeTracks(_ raw: Any?) -> [JamTrack]? {
        guard let arr = raw as? [[String: Any]] else { return [] }
        guard arr.count <= JamSessionState.maxQueueSize else { return nil }
        var seen = Set<String>()
        var out: [JamTrack] = []
        for entry in arr {
            guard let t = decodeTrack(entry) else { return nil }
            if seen.insert(t.id).inserted {
                out.append(t)
            }
        }
        return out
    }

    private static func decodeParticipants(_ raw: Any?) -> [JamParticipant]? {
        guard let arr = raw as? [[String: Any]] else { return [] }
        guard arr.count <= JamSessionState.maxParticipants else { return nil }
        var out: [JamParticipant] = []
        var seen = Set<String>()
        for entry in arr {
            let id = boundedText(entry["id"])
            guard !id.isEmpty, seen.insert(id).inserted else { return nil }
            out.append(
                JamParticipant(
                    id: id,
                    name: sanitizeName((entry["name"] as? String) ?? ""),
                    isHost: boolValue(entry["isHost"])
                )
            )
        }
        return out
    }

    private static func encodeAction(_ action: JamAction) -> [String: Any] {
        switch action {
        case .addTrack(let track):
            return ["kind": "add", "track": encodeTrack(track)]
        case .addTracks(let tracks):
            return ["kind": "add_many", "tracks": tracks.prefix(JamSessionState.maxQueueSize).map { encodeTrack($0) }]
        case .playNextTracks(let tracks):
            return ["kind": "play_next", "tracks": tracks.prefix(JamSessionState.maxQueueSize).map { encodeTrack($0) }]
        case .removeTrack(let trackId):
            return ["kind": "remove", "trackId": trackId]
        case .selectIndex(let index):
            return ["kind": "select", "index": index]
        case .setPlayWhenReady(let playWhenReady):
            return ["kind": "play", "playWhenReady": playWhenReady]
        case .seek(let positionMs):
            return ["kind": "seek", "positionMs": positionMs]
        case .next:
            return ["kind": "next"]
        case .previous:
            return ["kind": "previous"]
        }
    }

    private static func decodeAction(_ root: [String: Any]) -> JamWireMessage? {
        let sessionId = boundedText(root["sessionId"])
        let participantId = boundedText(root["participantId"])
        guard !sessionId.isEmpty, !participantId.isEmpty,
              let payload = root["action"] as? [String: Any],
              let kind = payload["kind"] as? String else {
            return nil
        }

        let parsed: JamAction?
        switch kind {
        case "add":
            parsed = decodeTrack(payload["track"] as? [String: Any]).map { .addTrack($0) }
        case "add_many":
            if let list = decodeTracks(payload["tracks"]), !list.isEmpty {
                parsed = .addTracks(list)
            } else {
                parsed = nil
            }
        case "play_next":
            if let list = decodeTracks(payload["tracks"]), !list.isEmpty {
                parsed = .playNextTracks(list)
            } else {
                parsed = nil
            }
        case "remove":
            let tid = boundedText(payload["trackId"])
            parsed = tid.isEmpty ? nil : .removeTrack(tid)
        case "select":
            let idx = intValue(payload["index"], default: -1)
            parsed = (0..<JamSessionState.maxQueueSize).contains(idx) ? .selectIndex(idx) : nil
        case "play":
            parsed = .setPlayWhenReady(boolValue(payload["playWhenReady"]))
        case "seek":
            let pos = int64Value(payload["positionMs"], default: -1)
            parsed = pos >= 0 ? .seek(pos) : nil
        case "next":
            parsed = .next
        case "previous":
            parsed = .previous
        default:
            parsed = nil
        }

        guard let action = parsed else { return nil }
        return .action(sessionId: sessionId, participantId: participantId, action: action)
    }

    private static func boundedText(_ any: Any?) -> String {
        guard let str = any as? String else { return "" }
        let filtered = str.unicodeScalars.filter { !CharacterSet.controlCharacters.contains($0) }
        let clean = String(String.UnicodeScalarView(filtered)).trimmingCharacters(in: .whitespacesAndNewlines)
        return String(clean.prefix(JamSessionState.maxTextLength))
    }

    private static func boolValue(_ any: Any?) -> Bool {
        if let b = any as? Bool { return b }
        if let n = any as? NSNumber { return n.boolValue }
        return false
    }

    private static func intValue(_ any: Any?, default fallback: Int = 0) -> Int {
        if let i = any as? Int { return i }
        if let n = any as? NSNumber { return n.intValue }
        if let s = any as? String, let i = Int(s) { return i }
        return fallback
    }

    private static func int64Value(_ any: Any?, default fallback: Int64 = 0) -> Int64 {
        if let i = any as? Int64 { return i }
        if let i = any as? Int { return Int64(i) }
        if let n = any as? NSNumber { return n.int64Value }
        if let s = any as? String, let i = Int64(s) { return i }
        return fallback
    }
}

enum JamNetworkAddressHelper {
    static func localPrivateIPv4Address() -> String? {
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else { return nil }
        defer { freeifaddrs(ifaddr) }

        var candidates: [(priority: Int, ip: String)] = []
        var cursor: UnsafeMutablePointer<ifaddrs>? = first

        while let ptr = cursor {
            defer { cursor = ptr.pointee.ifa_next }
            let flags = Int32(ptr.pointee.ifa_flags)
            guard (flags & IFF_UP) != 0, (flags & IFF_LOOPBACK) == 0,
                  let addr = ptr.pointee.ifa_addr,
                  addr.pointee.sa_family == UInt8(AF_INET) else {
                continue
            }

            let name = String(cString: ptr.pointee.ifa_name)
            var hostBuf = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let res = getnameinfo(
                addr,
                socklen_t(addr.pointee.sa_len),
                &hostBuf,
                socklen_t(hostBuf.count),
                nil,
                0,
                NI_NUMERICHOST
            )
            guard res == 0 else { continue }
            let ip = String(cString: hostBuf)
            guard JamSessionCode.isPrivateIpv4(ip) else { continue }

            let rank: Int
            if name == "en0" {
                rank = 0
            } else if name.hasPrefix("bridge") || name.hasPrefix("ap") {
                rank = 1
            } else if name.hasPrefix("en") {
                rank = 2
            } else if name.hasPrefix("utun") || name.hasPrefix("ipsec") || name.hasPrefix("ppp") {
                rank = 5
            } else {
                rank = 3
            }
            candidates.append((rank, ip))
        }

        return candidates.sorted(by: { $0.priority < $1.priority }).first?.ip
    }
}

private final class FramingLineSocket: @unchecked Sendable {
    let connection: NWConnection
    private let queue: DispatchQueue
    private var receiveBuffer = Data()
    private var closed = false
    var onLine: ((String) -> Void)?
    var onClosed: (() -> Void)?

    init(connection: NWConnection, queue: DispatchQueue) {
        self.connection = connection
        self.queue = queue
    }

    func startReading() {
        scheduleNextRead()
    }

    private func scheduleNextRead() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 16_384) { [weak self] data, _, isComplete, error in
            guard let self = self, !self.closed else { return }
            if let chunk = data, !chunk.isEmpty {
                self.receiveBuffer.append(chunk)
                if self.receiveBuffer.count > JamProtocol.maxMessageBytes {
                    self.close()
                    return
                }
                while let nlIndex = self.receiveBuffer.firstIndex(of: 0x0A) {
                    let lineData = self.receiveBuffer.subdata(in: 0..<nlIndex)
                    self.receiveBuffer.removeSubrange(0...nlIndex)
                    var trimmedData = lineData
                    if trimmedData.last == 0x0D {
                        trimmedData.removeLast()
                    }
                    if let line = String(data: trimmedData, encoding: .utf8), !line.isEmpty {
                        self.onLine?(line)
                    }
                }
            }
            if isComplete || error != nil {
                self.close()
                return
            }
            self.scheduleNextRead()
        }
    }

    func sendLine(_ text: String, completion: ((Bool) -> Void)? = nil) {
        guard !closed, let payload = (text + "\n").data(using: .utf8) else {
            completion?(false)
            return
        }
        connection.send(content: payload, completion: .contentProcessed { [weak self] err in
            if err != nil {
                self?.close()
                completion?(false)
            } else {
                completion?(true)
            }
        })
    }

    func close() {
        guard !closed else { return }
        closed = true
        onLine = nil
        let cb = onClosed
        onClosed = nil
        connection.cancel()
        cb?()
    }
}

private protocol JamHostTransportBackend: AnyObject {
    func start(secret: String) async -> JamSessionCode?
    func notifyPending(participantId: String, hostProof: String)
    func admit(participantId: String, welcome: JamWireMessage) -> Bool
    func reject(participantId: String, failure: JamFailure)
    func broadcast(_ message: JamWireMessage)
    func disconnect(participantId: String)
    func stop()
}

private protocol JamGuestTransportBackend: AnyObject {
    func connect(code: JamSessionCode, name: String, guestId: String) async -> Bool
    func send(_ message: JamWireMessage)
    func stop()
}

private final class LanJamHostBackend: JamHostTransportBackend, @unchecked Sendable {
    private let queue = DispatchQueue(label: "com.derbis.jam.lan.host")
    private var listener: NWListener?
    private var secret: String = ""
    private var pendingSockets: [String: FramingLineSocket] = [:]
    private var connectedSockets: [String: FramingLineSocket] = [:]

    var onGuestPending: ((String, String, String, String) -> Void)?
    var onGuestLeft: ((String) -> Void)?
    var onActionReceived: ((String, JamAction) -> Void)?
    var onFailed: ((JamFailure) -> Void)?

    func start(secret: String) async -> JamSessionCode? {
        guard JamSessionCode.isValidSecret(secret),
              let ip = JamNetworkAddressHelper.localPrivateIPv4Address() else {
            return nil
        }
        stop()
        self.secret = secret

        let tcpOptions = NWProtocolTCP.Options()
        tcpOptions.noDelay = true
        let params = NWParameters(tls: nil, tcp: tcpOptions)

        guard let nwListener = try? NWListener(using: params) else {
            return nil
        }
        self.listener = nwListener

        let boundPort: Int? = await withCheckedContinuation { continuation in
            var resumed = false
            nwListener.stateUpdateHandler = { [weak self] state in
                switch state {
                case .ready:
                    if !resumed {
                        resumed = true
                        let p = Int(nwListener.port?.rawValue ?? 0)
                        continuation.resume(returning: p)
                    }
                case .failed, .cancelled:
                    if !resumed {
                        resumed = true
                        continuation.resume(returning: nil)
                    } else {
                        self?.onFailed?(.ConnectionFailed)
                    }
                default:
                    break
                }
            }
            nwListener.newConnectionHandler = { [weak self] conn in
                self?.acceptIncomingConnection(conn)
            }
            nwListener.start(queue: self.queue)
        }

        guard let port = boundPort, (JamSessionCode.minPort...JamSessionCode.maxPort).contains(port) else {
            stop()
            return nil
        }
        return JamSessionCode(hostAddress: ip, port: port, secret: secret)
    }

    private func acceptIncomingConnection(_ conn: NWConnection) {
        let total = pendingSockets.count + connectedSockets.count
        guard total < JamSessionState.maxParticipants - 1 else {
            conn.cancel()
            return
        }

        let framed = FramingLineSocket(connection: conn, queue: queue)
        let hostNonce = JamAuth.generateNonce()
        let participantId = UUID().uuidString.lowercased()
        var authenticated = false

        framed.onLine = { [weak self, weak framed] line in
            guard let self = self, let framed = framed else { return }
            if !authenticated {
                guard case .authenticate(let guestNonce, let name, let proof, let guestId) = JamProtocol.decode(line) else {
                    framed.close()
                    return
                }
                let expected = JamAuth.computeGuestProof(secret: self.secret, hostNonce: hostNonce, guestNonce: guestNonce)
                guard JamAuth.verifyProof(expected: expected, actual: proof) else {
                    if let err = JamProtocol.encode(.failure(.NotAuthorized)) {
                        framed.sendLine(err) { _ in framed.close() }
                    } else {
                        framed.close()
                    }
                    return
                }
                authenticated = true
                let hostProof = JamAuth.computeHostProof(secret: self.secret, hostNonce: hostNonce, guestNonce: guestNonce)
                self.pendingSockets[participantId] = framed
                self.onGuestPending?(participantId, guestId, name, hostProof)
            } else {
                guard self.connectedSockets[participantId] != nil else { return }
                switch JamProtocol.decode(line) {
                case .action(_, _, let action):
                    self.onActionReceived?(participantId, action)
                case .bye:
                    framed.close()
                default:
                    break
                }
            }
        }

        framed.onClosed = { [weak self] in
            guard let self = self else { return }
            let wasPending = self.pendingSockets.removeValue(forKey: participantId) != nil
            let wasConnected = self.connectedSockets.removeValue(forKey: participantId) != nil
            if wasPending || wasConnected {
                self.onGuestLeft?(participantId)
            }
        }

        conn.stateUpdateHandler = { [weak framed] state in
            switch state {
            case .ready:
                framed?.startReading()
                if let challengeStr = JamProtocol.encode(.challenge(hostNonce: hostNonce)) {
                    framed?.sendLine(challengeStr)
                }
            case .failed, .cancelled:
                framed?.close()
            default:
                break
            }
        }
        conn.start(queue: queue)

        queue.asyncAfter(deadline: .now() + 6.0) { [weak framed] in
            if !authenticated {
                framed?.close()
            }
        }
    }

    func notifyPending(participantId: String, hostProof: String) {
        queue.async { [weak self] in
            guard let sock = self?.pendingSockets[participantId],
                  let str = JamProtocol.encode(.pending(hostProof: hostProof)) else { return }
            sock.sendLine(str)
        }
    }

    func admit(participantId: String, welcome: JamWireMessage) -> Bool {
        queue.sync {
            guard let sock = pendingSockets.removeValue(forKey: participantId),
                  let str = JamProtocol.encode(welcome) else {
                return false
            }
            connectedSockets[participantId] = sock
            sock.sendLine(str)
            return true
        }
    }

    func reject(participantId: String, failure: JamFailure) {
        queue.async { [weak self] in
            guard let self = self else { return }
            let sock = self.pendingSockets.removeValue(forKey: participantId)
                ?? self.connectedSockets.removeValue(forKey: participantId)
            guard let target = sock else { return }
            if let str = JamProtocol.encode(.failure(failure)) {
                target.sendLine(str) { _ in target.close() }
            } else {
                target.close()
            }
        }
    }

    func broadcast(_ message: JamWireMessage) {
        guard let str = JamProtocol.encode(message) else { return }
        queue.async { [weak self] in
            guard let self = self else { return }
            for sock in self.connectedSockets.values {
                sock.sendLine(str)
            }
        }
    }

    func disconnect(participantId: String) {
        queue.async { [weak self] in
            guard let self = self else { return }
            let sock = self.pendingSockets.removeValue(forKey: participantId)
                ?? self.connectedSockets.removeValue(forKey: participantId)
            if let target = sock {
                if let str = JamProtocol.encode(.bye(reason: "removed")) {
                    target.sendLine(str) { _ in target.close() }
                } else {
                    target.close()
                }
            }
        }
    }

    func stop() {
        queue.sync {
            listener?.stateUpdateHandler = nil
            listener?.newConnectionHandler = nil
            listener?.cancel()
            listener = nil
            for s in pendingSockets.values {
                s.onClosed = nil
                s.close()
            }
            for s in connectedSockets.values {
                s.onClosed = nil
                s.close()
            }
            pendingSockets.removeAll()
            connectedSockets.removeAll()
        }
    }
}

private final class LanJamGuestBackend: JamGuestTransportBackend, @unchecked Sendable {
    private let queue = DispatchQueue(label: "com.derbis.jam.lan.guest")
    private var socket: FramingLineSocket?
    private var stopping = false

    var onConnected: ((String, String) -> Void)?
    var onAwaitingApproval: (() -> Void)?
    var onStateReceived: ((JamWireMessage) -> Void)?
    var onFailed: ((JamFailure) -> Void)?
    var onDisconnected: (() -> Void)?

    func connect(code: JamSessionCode, name: String, guestId: String) async -> Bool {
        guard JamSessionCode.isValidSecret(code.secret),
              let nwPort = NWEndpoint.Port(rawValue: UInt16(code.port)) else {
            onFailed?(.InvalidCode)
            return false
        }
        stop()
        stopping = false

        let tcpOptions = NWProtocolTCP.Options()
        tcpOptions.noDelay = true
        let params = NWParameters(tls: nil, tcp: tcpOptions)
        let conn = NWConnection(host: NWEndpoint.Host(code.hostAddress), port: nwPort, using: params)
        let framed = FramingLineSocket(connection: conn, queue: queue)
        self.socket = framed

        return await withCheckedContinuation { continuation in
            var handshakeSettled = false
            var expectedHostProof = ""
            var stage = 0

            func finishHandshake(_ ok: Bool) {
                guard !handshakeSettled else { return }
                handshakeSettled = true
                continuation.resume(returning: ok)
            }

            framed.onLine = { [weak self, weak framed] line in
                guard let self = self, let framed = framed else { return }
                guard let decoded = JamProtocol.decode(line) else {
                    self.onFailed?(.ProtocolError)
                    finishHandshake(false)
                    framed.close()
                    return
                }

                if stage == 0 {
                    switch decoded {
                    case .challenge(let hostNonce):
                        let guestNonce = JamAuth.generateNonce()
                        expectedHostProof = JamAuth.computeHostProof(
                            secret: code.secret,
                            hostNonce: hostNonce,
                            guestNonce: guestNonce
                        )
                        let guestProof = JamAuth.computeGuestProof(
                            secret: code.secret,
                            hostNonce: hostNonce,
                            guestNonce: guestNonce
                        )
                        stage = 1
                        if let authStr = JamProtocol.encode(
                            .authenticate(
                                guestNonce: guestNonce,
                                name: JamProtocol.sanitizeName(name),
                                proof: guestProof,
                                guestId: JamSessionCode.sanitizeGuestIdentity(guestId)
                            )
                        ) {
                            framed.sendLine(authStr)
                        }
                    case .failure(let fail):
                        self.onFailed?(fail)
                        finishHandshake(false)
                        framed.close()
                    default:
                        self.onFailed?(.ConnectionFailed)
                        finishHandshake(false)
                        framed.close()
                    }
                } else if stage == 1 {
                    switch decoded {
                    case .welcome(let sessionId, let participantId, let hostProof):
                        guard JamAuth.verifyProof(expected: expectedHostProof, actual: hostProof) else {
                            self.onFailed?(.NotAuthorized)
                            finishHandshake(false)
                            framed.close()
                            return
                        }
                        stage = 2
                        self.onConnected?(sessionId, participantId)
                        finishHandshake(true)
                    case .pending(let hostProof):
                        guard JamAuth.verifyProof(expected: expectedHostProof, actual: hostProof) else {
                            self.onFailed?(.NotAuthorized)
                            finishHandshake(false)
                            framed.close()
                            return
                        }
                        stage = 2
                        self.onAwaitingApproval?()
                        finishHandshake(true)
                    case .failure(let fail):
                        self.onFailed?(fail)
                        finishHandshake(false)
                        framed.close()
                    default:
                        self.onFailed?(.ConnectionFailed)
                        finishHandshake(false)
                        framed.close()
                    }
                } else {
                    switch decoded {
                    case .welcome(let sessionId, let participantId, let hostProof):
                        guard JamAuth.verifyProof(expected: expectedHostProof, actual: hostProof) else {
                            self.onFailed?(.NotAuthorized)
                            framed.close()
                            return
                        }
                        self.onConnected?(sessionId, participantId)
                    case .state:
                        self.onStateReceived?(decoded)
                    case .failure(let fail):
                        self.onFailed?(fail)
                        framed.close()
                    case .bye:
                        self.onFailed?(.HostEnded)
                        framed.close()
                    default:
                        break
                    }
                }
            }

            framed.onClosed = { [weak self] in
                guard let self = self else { return }
                if !handshakeSettled {
                    self.onFailed?(.ConnectionFailed)
                    finishHandshake(false)
                } else if !self.stopping {
                    self.onDisconnected?()
                }
            }

            conn.stateUpdateHandler = { [weak self, weak framed] state in
                switch state {
                case .ready:
                    framed?.startReading()
                case .failed:
                    if !handshakeSettled {
                        self?.onFailed?(.ConnectionFailed)
                        finishHandshake(false)
                    }
                    framed?.close()
                case .cancelled:
                    if !handshakeSettled {
                        finishHandshake(false)
                    }
                default:
                    break
                }
            }
            conn.start(queue: self.queue)

            self.queue.asyncAfter(deadline: .now() + 6.5) { [weak self, weak framed] in
                if !handshakeSettled {
                    self?.onFailed?(.ConnectionFailed)
                    finishHandshake(false)
                    framed?.close()
                }
            }
        }
    }

    func send(_ message: JamWireMessage) {
        guard let str = JamProtocol.encode(message) else { return }
        queue.async { [weak self] in
            self?.socket?.sendLine(str)
        }
    }

    func stop() {
        stopping = true
        queue.sync {
            socket?.onClosed = nil
            socket?.close()
            socket = nil
        }
    }
}

private final class RemoteJamHostBackend: JamHostTransportBackend, @unchecked Sendable {
    private let relayURL: URL
    private var session: URLSession?
    private var wsTask: URLSessionWebSocketTask?
    private var pingTimer: DispatchSourceTimer?
    private var secret: String = ""
    private var stopping = false
    private let lock = NSLock()
    private var handshakeNonces: [String: String] = [:]
    private var pendingPeers = Set<String>()
    private var connectedPeers = Set<String>()

    var onGuestPending: ((String, String, String, String) -> Void)?
    var onGuestLeft: ((String) -> Void)?
    var onActionReceived: ((String, JamAction) -> Void)?
    var onFailed: ((JamFailure) -> Void)?

    init(relayURL: URL) {
        self.relayURL = relayURL
    }

    func start(secret: String) async -> JamSessionCode? {
        guard JamSessionCode.isValidSecret(secret) else { return nil }
        stop()
        self.secret = secret
        self.stopping = false

        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 45
        let urlSession = URLSession(configuration: cfg)
        let task = urlSession.webSocketTask(with: relayURL)
        self.session = urlSession
        self.wsTask = task
        task.resume()

        let helloObj: [String: Any] = ["type": "hello", "room": secret, "role": "host"]
        guard let helloData = try? JSONSerialization.data(withJSONObject: helloObj),
              let helloText = String(data: helloData, encoding: .utf8) else {
            stop()
            return nil
        }

        do {
            try await task.send(.string(helloText))
            let firstMsg = try await task.receive()
            guard case .string(let raw) = firstMsg,
                  let data = raw.data(using: .utf8),
                  let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
                  (json["type"] as? String) == "ready" else {
                stop()
                return nil
            }
        } catch {
            stop()
            return nil
        }

        startPingLoop()
        listenLoop(task: task)
        return JamSessionCode(
            hostAddress: JamSessionCode.remoteMarkerAddress,
            port: JamSessionCode.remoteMarkerPort,
            secret: secret
        )
    }

    private func startPingLoop() {
        let timer = DispatchSource.makeTimerSource(queue: .global(qos: .utility))
        timer.schedule(deadline: .now() + 20, repeating: 20)
        timer.setEventHandler { [weak self] in
            self?.wsTask?.sendPing { _ in }
        }
        timer.resume()
        pingTimer = timer
    }

    private func listenLoop(task: URLSessionWebSocketTask) {
        task.receive { [weak self] result in
            guard let self = self, !self.stopping else { return }
            switch result {
            case .success(let wsMsg):
                if case .string(let text) = wsMsg {
                    self.handleRelayEnvelope(text)
                }
                self.listenLoop(task: task)
            case .failure:
                if !self.stopping {
                    self.onFailed?(.ConnectionFailed)
                }
            }
        }
    }

    private func handleRelayEnvelope(_ raw: String) {
        guard let data = raw.data(using: .utf8),
              let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let type = json["type"] as? String else {
            return
        }

        switch type {
        case "peer_joined":
            guard let clientId = json["clientId"] as? String, !clientId.isEmpty else { return }
            let nonce = JamAuth.generateNonce()
            lock.lock()
            let full = (pendingPeers.count + connectedPeers.count) >= (JamSessionState.maxParticipants - 1)
            if !full {
                handshakeNonces[clientId] = nonce
            }
            lock.unlock()
            if !full, let ch = JamProtocol.encode(.challenge(hostNonce: nonce)) {
                sendRelayPayload(to: clientId, payload: ch)
            }
        case "peer_left":
            guard let clientId = json["clientId"] as? String, !clientId.isEmpty else { return }
            lock.lock()
            handshakeNonces.removeValue(forKey: clientId)
            let removed = pendingPeers.remove(clientId) != nil || connectedPeers.remove(clientId) != nil
            lock.unlock()
            if removed {
                onGuestLeft?(clientId)
            }
        case "message":
            guard let from = json["from"] as? String,
                  let payload = json["payload"] as? String,
                  let decoded = JamProtocol.decode(payload) else {
                return
            }
            switch decoded {
            case .authenticate(let guestNonce, let name, let proof, let guestId):
                lock.lock()
                let maybeNonce = handshakeNonces.removeValue(forKey: from)
                lock.unlock()
                guard let hostNonce = maybeNonce else { return }
                let expected = JamAuth.computeGuestProof(secret: secret, hostNonce: hostNonce, guestNonce: guestNonce)
                guard JamAuth.verifyProof(expected: expected, actual: proof) else {
                    if let failStr = JamProtocol.encode(.failure(.NotAuthorized)) {
                        sendRelayPayload(to: from, payload: failStr)
                    }
                    return
                }
                let hostProof = JamAuth.computeHostProof(secret: secret, hostNonce: hostNonce, guestNonce: guestNonce)
                lock.lock()
                pendingPeers.insert(from)
                lock.unlock()
                onGuestPending?(from, guestId, name, hostProof)
            case .action(_, _, let action):
                lock.lock()
                let isAdmitted = connectedPeers.contains(from)
                lock.unlock()
                if isAdmitted {
                    onActionReceived?(from, action)
                }
            case .bye:
                lock.lock()
                let removed = pendingPeers.remove(from) != nil || connectedPeers.remove(from) != nil
                lock.unlock()
                if removed {
                    onGuestLeft?(from)
                }
            default:
                break
            }
        case "failure":
            onFailed?(.ConnectionFailed)
        default:
            break
        }
    }

    private func sendRelayPayload(to target: String, payload: String) {
        let env: [String: Any] = ["type": "message", "to": target, "payload": payload]
        guard let data = try? JSONSerialization.data(withJSONObject: env),
              let str = String(data: data, encoding: .utf8) else { return }
        wsTask?.send(.string(str)) { _ in }
    }

    func notifyPending(participantId: String, hostProof: String) {
        guard let str = JamProtocol.encode(.pending(hostProof: hostProof)) else { return }
        sendRelayPayload(to: participantId, payload: str)
    }

    func admit(participantId: String, welcome: JamWireMessage) -> Bool {
        lock.lock()
        let wasPending = pendingPeers.remove(participantId) != nil
        if wasPending {
            connectedPeers.insert(participantId)
        }
        lock.unlock()
        guard wasPending, let str = JamProtocol.encode(welcome) else { return false }
        sendRelayPayload(to: participantId, payload: str)
        return true
    }

    func reject(participantId: String, failure: JamFailure) {
        lock.lock()
        _ = pendingPeers.remove(participantId)
        _ = connectedPeers.remove(participantId)
        lock.unlock()
        if let str = JamProtocol.encode(.failure(failure)) {
            sendRelayPayload(to: participantId, payload: str)
        }
    }

    func broadcast(_ message: JamWireMessage) {
        guard let str = JamProtocol.encode(message) else { return }
        lock.lock()
        let targets = Array(connectedPeers)
        lock.unlock()
        for peer in targets {
            sendRelayPayload(to: peer, payload: str)
        }
    }

    func disconnect(participantId: String) {
        lock.lock()
        _ = pendingPeers.remove(participantId)
        _ = connectedPeers.remove(participantId)
        lock.unlock()
        if let str = JamProtocol.encode(.bye(reason: "removed")) {
            sendRelayPayload(to: participantId, payload: str)
        }
    }

    func stop() {
        stopping = true
        pingTimer?.cancel()
        pingTimer = nil
        wsTask?.cancel(with: .normalClosure, reason: nil)
        wsTask = nil
        session?.invalidateAndCancel()
        session = nil
        lock.lock()
        handshakeNonces.removeAll()
        pendingPeers.removeAll()
        connectedPeers.removeAll()
        lock.unlock()
    }
}

private final class RemoteJamGuestBackend: JamGuestTransportBackend, @unchecked Sendable {
    private let relayURL: URL
    private var session: URLSession?
    private var wsTask: URLSessionWebSocketTask?
    private var pingTimer: DispatchSourceTimer?
    private var stopping = false

    var onConnected: ((String, String) -> Void)?
    var onAwaitingApproval: (() -> Void)?
    var onStateReceived: ((JamWireMessage) -> Void)?
    var onFailed: ((JamFailure) -> Void)?
    var onDisconnected: (() -> Void)?

    init(relayURL: URL) {
        self.relayURL = relayURL
    }

    func connect(code: JamSessionCode, name: String, guestId: String) async -> Bool {
        guard JamSessionCode.isValidSecret(code.secret), code.isRemoteRelayCode else {
            onFailed?(.InvalidCode)
            return false
        }
        stop()
        stopping = false

        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 15
        let urlSession = URLSession(configuration: cfg)
        let task = urlSession.webSocketTask(with: relayURL)
        self.session = urlSession
        self.wsTask = task
        task.resume()

        let helloObj: [String: Any] = ["type": "hello", "room": code.secret, "role": "guest"]
        guard let helloData = try? JSONSerialization.data(withJSONObject: helloObj),
              let helloText = String(data: helloData, encoding: .utf8) else {
            stop()
            return false
        }

        do {
            try await task.send(.string(helloText))
        } catch {
            onFailed?(.ConnectionFailed)
            stop()
            return false
        }

        return await withCheckedContinuation { continuation in
            var settled = false
            var expectedHostProof = ""
            var joined = false

            func settle(_ ok: Bool) {
                guard !settled else { return }
                settled = true
                continuation.resume(returning: ok)
            }

            func receiveNext() {
                task.receive { [weak self] result in
                    guard let self = self, !self.stopping else {
                        settle(false)
                        return
                    }
                    switch result {
                    case .success(let wsMsg):
                        if case .string(let text) = wsMsg,
                           let data = text.data(using: .utf8),
                           let env = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
                           let type = env["type"] as? String {
                            if type == "failure" {
                                let reason = env["reason"] as? String
                                let fail: JamFailure = (reason == "room_full") ? .SessionFull : .ConnectionFailed
                                self.onFailed?(fail)
                                settle(false)
                                return
                            } else if type == "message",
                                      let payload = env["payload"] as? String,
                                      let wire = JamProtocol.decode(payload) {
                                switch wire {
                                case .challenge(let hostNonce):
                                    let guestNonce = JamAuth.generateNonce()
                                    expectedHostProof = JamAuth.computeHostProof(
                                        secret: code.secret,
                                        hostNonce: hostNonce,
                                        guestNonce: guestNonce
                                    )
                                    let proof = JamAuth.computeGuestProof(
                                        secret: code.secret,
                                        hostNonce: hostNonce,
                                        guestNonce: guestNonce
                                    )
                                    if let authStr = JamProtocol.encode(
                                        .authenticate(
                                            guestNonce: guestNonce,
                                            name: JamProtocol.sanitizeName(name),
                                            proof: proof,
                                            guestId: JamSessionCode.sanitizeGuestIdentity(guestId)
                                        )
                                    ) {
                                        self.sendEnvelopePayload(authStr)
                                    }
                                case .pending(let hostProof):
                                    guard JamAuth.verifyProof(expected: expectedHostProof, actual: hostProof) else {
                                        self.onFailed?(.NotAuthorized)
                                        settle(false)
                                        return
                                    }
                                    joined = true
                                    self.startPingLoop()
                                    self.onAwaitingApproval?()
                                    settle(true)
                                case .welcome(let sessionId, let participantId, let hostProof):
                                    guard JamAuth.verifyProof(expected: expectedHostProof, actual: hostProof) else {
                                        self.onFailed?(.NotAuthorized)
                                        settle(false)
                                        return
                                    }
                                    joined = true
                                    self.startPingLoop()
                                    self.onConnected?(sessionId, participantId)
                                    settle(true)
                                case .state:
                                    if joined {
                                        self.onStateReceived?(wire)
                                    }
                                case .failure(let fail):
                                    self.onFailed?(fail)
                                    settle(false)
                                    return
                                case .bye:
                                    self.onFailed?(.HostEnded)
                                    settle(false)
                                    return
                                default:
                                    break
                                }
                            }
                        }
                        receiveNext()
                    case .failure:
                        if !settled {
                            self.onFailed?(.ConnectionFailed)
                            settle(false)
                        } else if !self.stopping {
                            self.onDisconnected?()
                        }
                    }
                }
            }

            receiveNext()

            DispatchQueue.global(qos: .utility).asyncAfter(deadline: .now() + 14.0) { [weak self] in
                if !settled {
                    self?.onFailed?(.ConnectionFailed)
                    settle(false)
                    self?.stop()
                }
            }
        }
    }

    private func startPingLoop() {
        guard pingTimer == nil else { return }
        let timer = DispatchSource.makeTimerSource(queue: .global(qos: .utility))
        timer.schedule(deadline: .now() + 20, repeating: 20)
        timer.setEventHandler { [weak self] in
            self?.wsTask?.sendPing { _ in }
        }
        timer.resume()
        pingTimer = timer
    }

    private func sendEnvelopePayload(_ payload: String) {
        let env: [String: Any] = ["type": "message", "payload": payload]
        guard let data = try? JSONSerialization.data(withJSONObject: env),
              let str = String(data: data, encoding: .utf8) else { return }
        wsTask?.send(.string(str)) { _ in }
    }

    func send(_ message: JamWireMessage) {
        guard let str = JamProtocol.encode(message) else { return }
        sendEnvelopePayload(str)
    }

    func stop() {
        stopping = true
        pingTimer?.cancel()
        pingTimer = nil
        wsTask?.cancel(with: .normalClosure, reason: nil)
        wsTask = nil
        session?.invalidateAndCancel()
        session = nil
    }
}

private final class JamBonjourCoordinator: NSObject, NetServiceBrowserDelegate, NetServiceDelegate {
    private static let serviceType = "_derbis-jam._tcp."
    private static let domain = "local."

    private var advertisedService: NetService?
    private var browser: NetServiceBrowser?
    private var discoveredServices: [String: NetService] = [:]
    var onNearbyRoomsChanged: (([NearbyJamRoom]) -> Void)?
    private var roomsByServiceName: [String: NearbyJamRoom] = [:]

    func advertise(port: Int, hostName: String, trackTitle: String, code: String) {
        stopAdvertising()
        let safeServiceName = "DerbisJam-\(UUID().uuidString.prefix(6))"
        let service = NetService(domain: Self.domain, type: Self.serviceType, name: safeServiceName, port: Int32(port))
        let txtDict: [String: Data] = [
            "code": Data(code.utf8),
            "host": Data(String(hostName.prefix(32)).utf8),
            "track": Data(String(trackTitle.prefix(64)).utf8)
        ]
        service.setTXTRecord(NetService.data(fromTXTRecord: txtDict))
        service.publish()
        advertisedService = service
    }

    func updateAdvertisedTrack(hostName: String, trackTitle: String, code: String) {
        guard let service = advertisedService else { return }
        let txtDict: [String: Data] = [
            "code": Data(code.utf8),
            "host": Data(String(hostName.prefix(32)).utf8),
            "track": Data(String(trackTitle.prefix(64)).utf8)
        ]
        service.setTXTRecord(NetService.data(fromTXTRecord: txtDict))
    }

    func stopAdvertising() {
        advertisedService?.stop()
        advertisedService = nil
    }

    func startBrowsing() {
        stopBrowsing()
        let netBrowser = NetServiceBrowser()
        netBrowser.delegate = self
        netBrowser.searchForServices(ofType: Self.serviceType, inDomain: Self.domain)
        browser = netBrowser
    }

    func stopBrowsing() {
        browser?.delegate = nil
        browser?.stop()
        browser = nil
        for s in discoveredServices.values {
            s.delegate = nil
            s.stop()
        }
        discoveredServices.removeAll()
        roomsByServiceName.removeAll()
        onNearbyRoomsChanged?([])
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        if service.name == advertisedService?.name { return }
        discoveredServices[service.name] = service
        service.delegate = self
        service.startMonitoring()
        service.resolve(withTimeout: 5.0)
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didRemove service: NetService, moreComing: Bool) {
        service.stopMonitoring()
        discoveredServices.removeValue(forKey: service.name)
        roomsByServiceName.removeValue(forKey: service.name)
        emitRooms()
    }

    func netServiceDidResolveAddress(_ sender: NetService) {
        extractRoom(from: sender, txtData: sender.txtRecordData())
    }

    func netService(_ sender: NetService, didUpdateTXTRecord data: Data) {
        extractRoom(from: sender, txtData: data)
    }

    private func extractRoom(from service: NetService, txtData: Data?) {
        guard service.name != advertisedService?.name,
              let data = txtData else { return }
        let dict = NetService.dictionary(fromTXTRecord: data)
        guard let codeData = dict["code"],
              let code = String(data: codeData, encoding: .utf8),
              JamSessionCode.parse(code) != nil else {
            return
        }
        let host = dict["host"].flatMap { String(data: $0, encoding: .utf8) } ?? "Derbis Host"
        let track = dict["track"].flatMap { String(data: $0, encoding: .utf8) } ?? ""
        roomsByServiceName[service.name] = NearbyJamRoom(
            serviceName: service.name,
            hostName: host,
            currentTrackTitle: track,
            code: code
        )
        emitRooms()
    }

    private func emitRooms() {
        let sorted = roomsByServiceName.values.sorted { $0.hostName < $1.hostName }
        onNearbyRoomsChanged?(sorted)
    }
}

extension Notification.Name {
    static let derbisPresentJamSheet = Notification.Name("derbis.presentJamSheet")
}

@MainActor
final class JamService: ObservableObject {
    static let shared = JamService()

    @Published private(set) var role: JamRole?
    @Published private(set) var connection: JamConnectionState = .idle
    @Published private(set) var transportMode: JamTransportMode?
    @Published private(set) var code: String = ""
    @Published private(set) var deepLink: String = ""
    @Published private(set) var shareURL: String = ""
    @Published private(set) var session: JamSessionState?
    @Published private(set) var selfParticipantId: String = ""
    @Published private(set) var permission: JamGuestPermission = .hostOnly
    @Published private(set) var pendingRequests: [JamPendingParticipant] = []
    @Published private(set) var bannedCount: Int = 0
    @Published var failure: JamFailure?
    @Published private(set) var nearbyRooms: [NearbyJamRoom] = []
    @Published var pendingIncomingInviteCode: String?

    @Published var displayName: String {
        didSet {
            let clean = JamProtocol.sanitizeName(displayName)
            UserDefaults.standard.set(clean, forKey: "derbis.jamDisplayName")
        }
    }

    @Published var customRelayURL: String {
        didSet {
            UserDefaults.standard.set(customRelayURL.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "derbis.customJamRelayURL")
        }
    }

    private let guestIdentity: String
    private var hostBackend: JamHostTransportBackend?
    private var guestBackend: JamGuestTransportBackend?
    private let bonjour = JamBonjourCoordinator()

    private var broadcastTask: Task<Void, Never>?
    private var reconnectTask: Task<Void, Never>?

    private var sessionId: String = ""
    private var hostParticipantId: String = ""
    private var revision: Int64 = 0
    private var createdAt: Int64 = 0
    private var requireApproval: Bool = true
    private var locked: Bool = false
    private var participants: [JamParticipant] = []
    private var pendingProofs: [String: String] = [:]
    private var participantIdentities: [String: String] = [:]
    private var bannedIdentities: [String] = []
    private var trackOwners: [String: String] = [:]
    private var lastAppliedRevision: Int64 = -1
    private var joinedCode: JamSessionCode?
    private var leaving: Bool = false

    var isActive: Bool { role != nil }
    var isHost: Bool { role == .host }
    var isGuest: Bool { role == .guest }
    var participantCount: Int { session?.participants.count ?? participants.count }
    var isLocked: Bool { session?.locked ?? locked }
    var isApprovalRequired: Bool { session?.requireApproval ?? requireApproval }
    var canControlPlayback: Bool { isHost || permission.canControlPlayback }
    var canAddTracks: Bool { isHost || permission.canAddTracks }

    var effectiveRelayURL: URL? {
        let overrideStr = customRelayURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if !overrideStr.isEmpty {
            let normalized = (overrideStr.lowercased().hasPrefix("ws://") || overrideStr.lowercased().hasPrefix("wss://"))
                ? overrideStr
                : "wss://\(overrideStr)"
            if let u = URL(string: normalized) {
                return u
            }
        }
        if let plistVal = Bundle.main.object(forInfoDictionaryKey: "DerbisJamRelayURL") as? String {
            let trimmed = plistVal.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.lowercased().hasPrefix("wss://") || trimmed.lowercased().hasPrefix("ws://"),
               let u = URL(string: trimmed) {
                return u
            }
        }
        return nil
    }

    var isRemoteRelayAvailable: Bool {
        effectiveRelayURL != nil
    }

    private init() {
        let storedName = UserDefaults.standard.string(forKey: "derbis.jamDisplayName") ?? ""
        if !storedName.isEmpty {
            self.displayName = storedName
        } else if let googleName = GoogleAccountSyncService.shared.activeAccount?.name, !googleName.isEmpty {
            self.displayName = JamProtocol.sanitizeName(googleName)
        } else {
            self.displayName = JamProtocol.sanitizeName(UIDevice.current.name)
        }

        self.customRelayURL = UserDefaults.standard.string(forKey: "derbis.customJamRelayURL") ?? ""

        if let savedId = UserDefaults.standard.string(forKey: "derbis.jamGuestIdentity"),
           !JamSessionCode.sanitizeGuestIdentity(savedId).isEmpty {
            self.guestIdentity = JamSessionCode.sanitizeGuestIdentity(savedId)
        } else {
            let fresh = JamSessionCode.newGuestIdentity()
            UserDefaults.standard.set(fresh, forKey: "derbis.jamGuestIdentity")
            self.guestIdentity = fresh
        }

        bonjour.onNearbyRoomsChanged = { [weak self] rooms in
            Task { @MainActor [weak self] in
                self?.nearbyRooms = rooms
            }
        }
    }

    func startNearbyDiscovery() {
        bonjour.startBrowsing()
    }

    func stopNearbyDiscovery() {
        bonjour.stopBrowsing()
    }

    func handleIncomingJamURL(_ url: URL) -> Bool {
        let raw = url.absoluteString
        guard let parsed = JamSessionCode.parse(raw), let encoded = parsed.encoded() else {
            return false
        }
        pendingIncomingInviteCode = encoded
        NotificationCenter.default.post(name: .derbisPresentJamSheet, object: nil)
        Task {
            await joinJam(rawCode: encoded)
        }
        return true
    }

    func createJam(
        mode: JamTransportMode,
        guestPermission: JamGuestPermission,
        approvalRequired: Bool
    ) async {
        teardownInternal()
        failure = nil
        role = .host
        connection = .connecting
        transportMode = mode

        let secret = JamSessionCode.newSecret()
        let backend: JamHostTransportBackend
        switch mode {
        case .lan:
            let lan = LanJamHostBackend()
            wireHostCallbacks(lan)
            backend = lan
        case .server:
            guard let relay = effectiveRelayURL else {
                teardownInternal()
                failure = .ConnectionFailed
                return
            }
            let remote = RemoteJamHostBackend(relayURL: relay)
            wireHostCallbacks(remote)
            backend = remote
        }

        hostBackend = backend
        guard let sessionCode = await backend.start(secret: secret),
              let encoded = sessionCode.encoded() else {
            teardownInternal()
            failure = .ConnectionFailed
            return
        }

        sessionId = UUID().uuidString.lowercased()
        hostParticipantId = UUID().uuidString.lowercased()
        revision = 0
        createdAt = Self.wallClockMs()
        permission = guestPermission
        requireApproval = approvalRequired
        locked = false
        participants = [
            JamParticipant(
                id: hostParticipantId,
                name: JamProtocol.sanitizeName(displayName),
                isHost: true
            )
        ]

        code = encoded
        deepLink = sessionCode.deepLink
        shareURL = sessionCode.webShareURL
        selfParticipantId = hostParticipantId
        connection = .connected
        session = buildHostSessionState()

        if mode == .lan && !locked {
            let trackTitle = AudioPlayerEngine.shared.currentTrack?.title ?? ""
            bonjour.advertise(
                port: sessionCode.port,
                hostName: JamProtocol.sanitizeName(displayName),
                trackTitle: trackTitle,
                code: encoded
            )
        }

        startHostBroadcastLoop()
        publishHostStateImmediate()
        HapticsAndGesturesService.shared.playSuccessFeedback()
    }

    func joinJam(rawCode: String) async {
        guard let parsed = JamSessionCode.parse(rawCode),
              let encoded = parsed.encoded() else {
            failure = .InvalidCode
            return
        }

        teardownInternal()
        leaving = false
        failure = nil
        joinedCode = parsed
        let detectedMode: JamTransportMode = parsed.isRemoteRelayCode ? .server : .lan
        role = .guest
        connection = .connecting
        transportMode = detectedMode
        code = encoded
        deepLink = parsed.deepLink
        shareURL = parsed.webShareURL
        lastAppliedRevision = -1

        let backend: JamGuestTransportBackend
        switch detectedMode {
        case .lan:
            let lan = LanJamGuestBackend()
            wireGuestCallbacks(lan)
            backend = lan
        case .server:
            guard let relay = effectiveRelayURL else {
                teardownInternal()
                failure = .ConnectionFailed
                return
            }
            let remote = RemoteJamGuestBackend(relayURL: relay)
            wireGuestCallbacks(remote)
            backend = remote
        }

        guestBackend = backend
        let ok = await backend.connect(
            code: parsed,
            name: JamProtocol.sanitizeName(displayName),
            guestId: guestIdentity
        )
        if !ok {
            let capturedFailure = failure ?? .ConnectionFailed
            teardownInternal()
            failure = capturedFailure
        } else {
            HapticsAndGesturesService.shared.playSuccessFeedback()
        }
    }

    func leaveOrEndJam() {
        leaving = true
        if role == .host {
            hostBackend?.broadcast(.bye(reason: "host_ended"))
        } else if role == .guest {
            guestBackend?.send(.bye(reason: "leave"))
        }
        teardownInternal()
    }

    func setGuestPermission(_ next: JamGuestPermission) {
        guard isHost else { return }
        permission = next
        publishHostStateImmediate()
    }

    func setSessionLocked(_ value: Bool) {
        guard isHost, locked != value else { return }
        locked = value
        if locked {
            bonjour.stopAdvertising()
        } else if transportMode == .lan, let parsed = JamSessionCode.parse(code) {
            bonjour.advertise(
                port: parsed.port,
                hostName: JamProtocol.sanitizeName(displayName),
                trackTitle: AudioPlayerEngine.shared.currentTrack?.title ?? "",
                code: code
            )
        }
        publishHostStateImmediate()
    }

    func setApprovalRequired(_ value: Bool) {
        guard isHost, requireApproval != value else { return }
        requireApproval = value
        if !value, let backend = hostBackend {
            let waiting = pendingRequests
            pendingRequests.removeAll()
            for entry in waiting {
                if participants.count >= JamSessionState.maxParticipants {
                    pendingProofs.removeValue(forKey: entry.participantId)
                    backend.reject(participantId: entry.participantId, failure: .SessionFull)
                } else {
                    admitGuest(entry, backend: backend)
                }
            }
        }
        publishHostStateImmediate()
    }

    func approveParticipant(_ participantId: String) {
        guard isHost, let backend = hostBackend,
              let idx = pendingRequests.firstIndex(where: { $0.participantId == participantId }) else {
            return
        }
        let entry = pendingRequests.remove(at: idx)
        if participants.count >= JamSessionState.maxParticipants {
            pendingProofs.removeValue(forKey: participantId)
            backend.reject(participantId: participantId, failure: .SessionFull)
        } else {
            admitGuest(entry, backend: backend)
        }
        publishHostStateImmediate()
    }

    func rejectParticipant(_ participantId: String) {
        guard isHost, let backend = hostBackend else { return }
        pendingRequests.removeAll { $0.participantId == participantId }
        pendingProofs.removeValue(forKey: participantId)
        backend.reject(participantId: participantId, failure: .Rejected)
        publishHostStateImmediate()
    }

    func removeParticipant(_ participantId: String, ban: Bool) {
        guard isHost, participantId != hostParticipantId, let backend = hostBackend else { return }
        if ban, let gid = participantIdentities[participantId], !gid.isEmpty {
            if !bannedIdentities.contains(gid) {
                bannedIdentities.append(gid)
                if bannedIdentities.count > JamSessionState.maxBanned {
                    bannedIdentities.removeFirst()
                }
            }
            bannedCount = bannedIdentities.count
        }
        participants.removeAll { $0.id == participantId }
        participantIdentities.removeValue(forKey: participantId)
        pendingRequests.removeAll { $0.participantId == participantId }
        pendingProofs.removeValue(forKey: participantId)
        backend.reject(participantId: participantId, failure: ban ? .Banned : .Removed)
        publishHostStateImmediate()
    }

    func clearBans() {
        guard isHost else { return }
        bannedIdentities.removeAll()
        bannedCount = 0
    }

    func notifyHostPlaybackChanged() {
        guard isHost else { return }
        if transportMode == .lan && !locked {
            bonjour.updateAdvertisedTrack(
                hostName: JamProtocol.sanitizeName(displayName),
                trackTitle: AudioPlayerEngine.shared.currentTrack?.title ?? "",
                code: code
            )
        }
        publishHostStateImmediate()
    }

    func dispatchUserAction(_ action: JamAction) {
        guard isActive else { return }
        if isHost {
            rememberOwners(action: action, participantId: hostParticipantId)
            AudioPlayerEngine.shared.applyJamHostAction(action)
            publishHostStateImmediate()
        } else if isGuest {
            guard connection == .connected else { return }
            guard action.isAllowed(for: permission) else {
                AudioPlayerEngine.shared.showTransientBanner("Host controls playback in this Jam.")
                return
            }
            guestBackend?.send(.action(sessionId: sessionId, participantId: selfParticipantId, action: action))
        }
    }

    private func wireHostCallbacks(_ backend: AnyObject) {
        let onPending: (String, String, String, String) -> Void = { [weak self] pid, gid, name, proof in
            Task { @MainActor [weak self] in
                self?.handleGuestPending(participantId: pid, guestId: gid, name: name, hostProof: proof)
            }
        }
        let onLeft: (String) -> Void = { [weak self] pid in
            Task { @MainActor [weak self] in
                self?.handleGuestLeft(participantId: pid)
            }
        }
        let onAction: (String, JamAction) -> Void = { [weak self] pid, action in
            Task { @MainActor [weak self] in
                self?.handleGuestAction(participantId: pid, action: action)
            }
        }
        let onFail: (JamFailure) -> Void = { [weak self] fail in
            Task { @MainActor [weak self] in
                self?.failure = fail
            }
        }

        if let lan = backend as? LanJamHostBackend {
            lan.onGuestPending = onPending
            lan.onGuestLeft = onLeft
            lan.onActionReceived = onAction
            lan.onFailed = onFail
        } else if let rem = backend as? RemoteJamHostBackend {
            rem.onGuestPending = onPending
            rem.onGuestLeft = onLeft
            rem.onActionReceived = onAction
            rem.onFailed = onFail
        }
    }

    private func wireGuestCallbacks(_ backend: AnyObject) {
        let onConn: (String, String) -> Void = { [weak self] sid, pid in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.sessionId = sid
                self.selfParticipantId = pid
                self.connection = .connected
                self.failure = nil
            }
        }
        let onWait: () -> Void = { [weak self] in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.connection = .awaitingApproval
                self.failure = nil
            }
        }
        let onState: (JamWireMessage) -> Void = { [weak self] wire in
            Task { @MainActor [weak self] in
                self?.handleGuestWireState(wire)
            }
        }
        let onFail: (JamFailure) -> Void = { [weak self] fail in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.failure = fail
                self.connection = .disconnected
            }
        }
        let onDisc: () -> Void = { [weak self] in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.connection = .disconnected
                self.scheduleGuestReconnect()
            }
        }

        if let lan = backend as? LanJamGuestBackend {
            lan.onConnected = onConn
            lan.onAwaitingApproval = onWait
            lan.onStateReceived = onState
            lan.onFailed = onFail
            lan.onDisconnected = onDisc
        } else if let rem = backend as? RemoteJamGuestBackend {
            rem.onConnected = onConn
            rem.onAwaitingApproval = onWait
            rem.onStateReceived = onState
            rem.onFailed = onFail
            rem.onDisconnected = onDisc
        }
    }

    private func handleGuestPending(participantId: String, guestId: String, name: String, hostProof: String) {
        guard isHost, let backend = hostBackend else { return }
        let cleanIdentity = JamSessionCode.sanitizeGuestIdentity(guestId)
        if !cleanIdentity.isEmpty && bannedIdentities.contains(cleanIdentity) {
            backend.reject(participantId: participantId, failure: .Banned)
            return
        }
        if locked {
            backend.reject(participantId: participantId, failure: .SessionLocked)
            return
        }
        if !cleanIdentity.isEmpty {
            let staleIds = participantIdentities.filter { $0.value == cleanIdentity }.map(\.key)
            for stale in staleIds {
                participants.removeAll { $0.id == stale }
                participantIdentities.removeValue(forKey: stale)
                backend.disconnect(participantId: stale)
            }
            pendingRequests.removeAll { $0.guestId == cleanIdentity }
        }
        if participants.count >= JamSessionState.maxParticipants {
            backend.reject(participantId: participantId, failure: .SessionFull)
            return
        }

        let entry = JamPendingParticipant(
            participantId: participantId,
            guestId: cleanIdentity,
            name: JamProtocol.sanitizeName(name),
            requestedAtElapsedMs: Self.elapsedMs()
        )
        pendingProofs[participantId] = hostProof

        if !requireApproval {
            admitGuest(entry, backend: backend)
            publishHostStateImmediate()
            return
        }

        if pendingRequests.count >= JamSessionState.maxPending {
            pendingProofs.removeValue(forKey: participantId)
            backend.reject(participantId: participantId, failure: .SessionFull)
            return
        }

        pendingRequests.append(entry)
        backend.notifyPending(participantId: participantId, hostProof: hostProof)
        HapticsAndGesturesService.shared.playControlTap(style: .medium)
    }

    private func admitGuest(_ entry: JamPendingParticipant, backend: JamHostTransportBackend) {
        guard let proof = pendingProofs.removeValue(forKey: entry.participantId) else { return }
        participants.removeAll { $0.id == entry.participantId }
        participants.append(JamParticipant(id: entry.participantId, name: entry.name, isHost: false))
        if !entry.guestId.isEmpty {
            participantIdentities[entry.participantId] = entry.guestId
        }
        let admitted = backend.admit(
            participantId: entry.participantId,
            welcome: .welcome(sessionId: sessionId, participantId: entry.participantId, hostProof: proof)
        )
        if !admitted {
            participants.removeAll { $0.id == entry.participantId }
            participantIdentities.removeValue(forKey: entry.participantId)
        }
    }

    private func handleGuestLeft(participantId: String) {
        guard isHost else { return }
        let removedActive = participants.contains { $0.id == participantId }
        participants.removeAll { $0.id == participantId }
        pendingRequests.removeAll { $0.participantId == participantId }
        pendingProofs.removeValue(forKey: participantId)
        participantIdentities.removeValue(forKey: participantId)
        if removedActive {
            publishHostStateImmediate()
        }
    }

    private func handleGuestAction(participantId: String, action: JamAction) {
        guard isHost,
              let sender = participants.first(where: { $0.id == participantId }),
              !sender.isHost,
              action.isAllowed(for: permission) else {
            return
        }
        rememberOwners(action: action, participantId: participantId)
        AudioPlayerEngine.shared.applyJamHostAction(action)
        publishHostStateImmediate()
    }

    private func rememberOwners(action: JamAction, participantId: String) {
        switch action {
        case .addTrack(let t):
            trackOwners[t.id] = participantId
        case .addTracks(let list), .playNextTracks(let list):
            for t in list {
                trackOwners[t.id] = participantId
            }
        default:
            break
        }
    }

    private func handleGuestWireState(_ wire: JamWireMessage) {
        guard isGuest,
              case .state(let incomingSessionId, let rev, _, _, var incomingState) = wire else {
            return
        }
        if !sessionId.isEmpty && incomingSessionId != sessionId { return }
        if lastAppliedRevision >= 0 && rev <= lastAppliedRevision { return }
        lastAppliedRevision = rev
        incomingState.updatedAtElapsedMs = Self.elapsedMs()
        connection = .connected
        session = incomingState
        permission = incomingState.permission
        AudioPlayerEngine.shared.applyJamRemoteState(incomingState)
    }

    private func scheduleGuestReconnect() {
        guard !leaving, isGuest, let targetCode = joinedCode else { return }
        if let fail = failure, fail != .ConnectionFailed { return }
        guard reconnectTask == nil else { return }

        reconnectTask = Task { @MainActor [weak self] in
            let delays: [UInt64] = [1_000_000_000, 2_000_000_000, 4_000_000_000]
            for ns in delays {
                try? await Task.sleep(nanoseconds: ns)
                guard !Task.isCancelled, let self = self, !self.leaving, self.isGuest else { return }
                self.connection = .connecting
                self.guestBackend?.stop()
                let backend: JamGuestTransportBackend
                if targetCode.isRemoteRelayCode {
                    guard let relay = self.effectiveRelayURL else { break }
                    let rem = RemoteJamGuestBackend(relayURL: relay)
                    self.wireGuestCallbacks(rem)
                    backend = rem
                } else {
                    let lan = LanJamGuestBackend()
                    self.wireGuestCallbacks(lan)
                    backend = lan
                }
                self.guestBackend = backend
                let ok = await backend.connect(
                    code: targetCode,
                    name: JamProtocol.sanitizeName(self.displayName),
                    guestId: self.guestIdentity
                )
                if ok {
                    self.reconnectTask = nil
                    return
                }
            }
            self?.reconnectTask = nil
            if self?.leaving == false && self?.connection != .connected {
                self?.connection = .disconnected
                self?.failure = .ConnectionFailed
            }
        }
    }

    private func startHostBroadcastLoop() {
        broadcastTask?.cancel()
        broadcastTask = Task { @MainActor [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled, let self = self, self.isHost else { return }
                self.publishHostStateImmediate()
            }
        }
    }

    private func publishHostStateImmediate() {
        guard isHost, let backend = hostBackend else { return }
        revision &+= 1
        let built = buildHostSessionState()
        session = built
        backend.broadcast(
            .state(
                sessionId: sessionId,
                revision: revision,
                source: hostParticipantId,
                timestamp: Self.wallClockMs(),
                state: built
            )
        )
    }

    private func buildHostSessionState() -> JamSessionState {
        let snap = AudioPlayerEngine.shared.jamSnapshot()
        let enrichedQueue: [JamTrack] = snap.queue.prefix(JamSessionState.maxQueueSize).map { item in
            var copy = item
            if let owner = trackOwners[item.id] {
                copy.addedBy = owner
            } else if copy.addedBy.isEmpty {
                copy.addedBy = hostParticipantId
            }
            return copy
        }
        let safeIdx: Int
        let mediaId: String
        if enrichedQueue.isEmpty {
            safeIdx = -1
            mediaId = ""
        } else {
            safeIdx = min(max(0, snap.currentIndex), enrichedQueue.count - 1)
            mediaId = enrichedQueue[safeIdx].id
        }

        return JamSessionState(
            sessionId: sessionId,
            hostId: hostParticipantId,
            revision: revision,
            createdAt: createdAt,
            participants: participants,
            queue: enrichedQueue,
            currentIndex: safeIdx,
            currentMediaId: mediaId,
            positionMs: snap.positionMs,
            playWhenReady: snap.playWhenReady,
            shuffle: snap.shuffle,
            repeatMode: snap.repeatMode,
            permission: permission,
            updatedAtElapsedMs: Self.elapsedMs(),
            capabilities: [JamSessionState.batchAddCapability, JamSessionState.moderationCapability],
            locked: locked,
            requireApproval: requireApproval
        )
    }

    private func teardownInternal() {
        broadcastTask?.cancel()
        broadcastTask = nil
        reconnectTask?.cancel()
        reconnectTask = nil
        bonjour.stopAdvertising()
        hostBackend?.stop()
        hostBackend = nil
        guestBackend?.stop()
        guestBackend = nil
        role = nil
        connection = .idle
        transportMode = nil
        code = ""
        deepLink = ""
        shareURL = ""
        session = nil
        selfParticipantId = ""
        permission = .hostOnly
        pendingRequests.removeAll()
        pendingProofs.removeAll()
        participants.removeAll()
        participantIdentities.removeAll()
        bannedIdentities.removeAll()
        bannedCount = 0
        trackOwners.removeAll()
        sessionId = ""
        hostParticipantId = ""
        revision = 0
        lastAppliedRevision = -1
        joinedCode = nil
    }

    static func elapsedMs() -> Int64 {
        Int64(ProcessInfo.processInfo.systemUptime * 1000.0)
    }

    static func wallClockMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000.0)
    }

    static func predictedPositionMs(for state: JamSessionState) -> Int64 {
        let base = max(0, state.positionMs)
        guard state.playWhenReady else { return base }
        let elapsed = min(10_000, max(0, elapsedMs() - state.updatedAtElapsedMs))
        return base + elapsed
    }

    static func shouldSeek(localMs: Int64, predictedMs: Int64, playWhenReady: Bool) -> Bool {
        let threshold: Int64 = playWhenReady ? 1_500 : 250
        return abs(localMs - predictedMs) > threshold
    }
}
