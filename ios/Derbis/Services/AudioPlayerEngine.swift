import Foundation
import AVFoundation
import AVKit
import MediaPlayer
import SwiftUI
import UIKit

@MainActor
final class AudioPlayerEngine: ObservableObject {
    static let shared = AudioPlayerEngine()

    @Published var currentTrack: Track?
    @Published var queue: [Track] = []
    @Published var currentIndex: Int = 0
    @Published var isPlaying: Bool = false
    @Published var isLoading: Bool = false
    @Published var currentTime: Double = 0
    @Published var duration: Double = 0
    @Published var isShuffleEnabled: Bool = false
    @Published var repeatMode: RepeatMode = .off
    @Published var playbackRate: Float = 1.0
    @Published var activeCodecBadge: String = "AAC"
    @Published var errorBanner: String?

    @Published var currentLyrics: LyricsPayload?
    @Published var activeLyricIndex: Int?
    @Published var isLoadingLyrics: Bool = false

    @Published var ambientPrimary: Color = Color(hex: 0x2A1A2E)
    @Published var ambientSecondary: Color = Color(hex: 0x0A0A10)
    @Published var currentArtworkImage: UIImage?

    @Published var sleepTimerRemainingSec: Int?

    private let player = AVPlayer()
    private var timeObserver: Any?
    private var endObserver: NSObjectProtocol?
    private var statusObservation: NSKeyValueObservation?
    private var loadTask: Task<Void, Never>?
    private var sleepTimerTask: Task<Void, Never>?

    init() {
        configureAudioSession()
        configureRemoteCommands()
        installTimeObserver()
        installNotifications()
    }

    func play(track: Track, in contextQueue: [Track]? = nil) {
        if let list = contextQueue, !list.isEmpty {
            queue = list
            currentIndex = list.firstIndex(where: { $0.id == track.id }) ?? 0
        } else if let idx = queue.firstIndex(where: { $0.id == track.id }) {
            currentIndex = idx
        } else {
            queue = [track]
            currentIndex = 0
        }
        startCurrentItem()
    }

    func playNextInQueue(_ track: Track) {
        if queue.isEmpty {
            play(track: track, in: [track])
            return
        }
        let insertIndex = min(queue.count, currentIndex + 1)
        queue.insert(track, at: insertIndex)
    }

    func appendToQueue(_ track: Track) {
        if queue.isEmpty {
            play(track: track, in: [track])
            return
        }
        queue.append(track)
    }

    func removeFromQueue(at offsets: IndexSet) {
        for offset in offsets.sorted(by: >) {
            guard queue.indices.contains(offset) else { continue }
            if offset == currentIndex { continue }
            queue.remove(at: offset)
            if offset < currentIndex {
                currentIndex = max(0, currentIndex - 1)
            }
        }
    }

    func togglePlayPause() {
        guard currentTrack != nil else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            try? AVAudioSession.sharedInstance().setActive(true)
            player.playImmediately(atRate: playbackRate)
            isPlaying = true
        }
        updateNowPlayingMetadata()
    }

    func skipNext(automatic: Bool = false) {
        guard !queue.isEmpty else { return }

        if automatic && repeatMode == .one {
            seek(to: 0)
            player.playImmediately(atRate: playbackRate)
            isPlaying = true
            return
        }

        if isShuffleEnabled && queue.count > 1 {
            let candidates = queue.indices.filter { $0 != currentIndex }
            if let pick = candidates.randomElement() {
                currentIndex = pick
                startCurrentItem()
                return
            }
        }

        if currentIndex + 1 < queue.count {
            currentIndex += 1
            startCurrentItem()
        } else if repeatMode == .all {
            currentIndex = 0
            startCurrentItem()
        } else {
            isPlaying = false
            seek(to: 0)
            player.pause()
            updateNowPlayingMetadata()
        }
    }

    func skipPrevious() {
        guard !queue.isEmpty else { return }
        if currentTime > 3.5 {
            seek(to: 0)
            return
        }
        if currentIndex > 0 {
            currentIndex -= 1
            startCurrentItem()
        } else if repeatMode == .all {
            currentIndex = max(0, queue.count - 1)
            startCurrentItem()
        } else {
            seek(to: 0)
        }
    }

    func seek(to seconds: Double) {
        let clamped = max(0, min(seconds, max(duration, seconds)))
        let cm = CMTime(seconds: clamped, preferredTimescale: 600)
        player.seek(to: cm, toleranceBefore: .zero, toleranceAfter: .zero)
        currentTime = clamped
        updateActiveLyricIndex(at: clamped)
        updateNowPlayingMetadata()
    }

    func setRate(_ rate: Float) {
        playbackRate = rate
        if isPlaying {
            player.rate = rate
        }
        updateNowPlayingMetadata()
    }

    func setSleepTimer(_ option: SleepTimerOption) {
        sleepTimerTask?.cancel()
        if option == .off {
            sleepTimerRemainingSec = nil
            return
        }
        let total = option.rawValue * 60
        sleepTimerRemainingSec = total
        sleepTimerTask = Task { [weak self] in
            var remaining = total
            while remaining > 0 && !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled, let self = self else { return }
                remaining -= 1
                self.sleepTimerRemainingSec = remaining
            }
            guard !Task.isCancelled, let self = self else { return }
            self.sleepTimerRemainingSec = nil
            self.player.pause()
            self.isPlaying = false
            self.updateNowPlayingMetadata()
        }
    }

    private func startCurrentItem() {
        guard queue.indices.contains(currentIndex) else { return }
        let track = queue[currentIndex]
        currentTrack = track
        currentTime = 0
        duration = Double(track.durationSec)
        isLoading = true
        errorBanner = nil
        currentLyrics = nil
        activeLyricIndex = nil
        activeCodecBadge = track.codecBadge

        loadTask?.cancel()
        player.pause()

        let preferHQ = UserDefaults.standard.object(forKey: "derbis.highQualityAudio") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.highQualityAudio")
        let autoLyrics = UserDefaults.standard.object(forKey: "derbis.autoLoadLyrics") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.autoLoadLyrics")
        let autoRadio = UserDefaults.standard.object(forKey: "derbis.endlessRadio") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.endlessRadio")

        Task {
            await loadArtworkAndPalette(for: track)
        }

        if autoLyrics {
            loadLyrics(for: track)
        }

        loadTask = Task { [weak self] in
            guard let self = self else { return }
            do {
                let resolved = try await self.resolveStream(for: track, preferHighQuality: preferHQ)
                guard !Task.isCancelled else { return }

                self.activeCodecBadge = resolved.codecBadge
                try? AVAudioSession.sharedInstance().setActive(true)

                let asset: AVURLAsset
                if resolved.headers.isEmpty {
                    asset = AVURLAsset(url: resolved.url)
                } else {
                    asset = AVURLAsset(
                        url: resolved.url,
                        options: ["AVURLAssetHTTPHeaderFieldsKey": resolved.headers]
                    )
                }

                let item = AVPlayerItem(asset: asset)
                item.preferredForwardBufferDuration = 12

                self.observeItemStatus(item, track: track)
                self.player.replaceCurrentItem(with: item)
                self.player.automaticallyWaitsToMinimizeStalling = true
                self.player.playImmediately(atRate: self.playbackRate)
                self.isPlaying = true
                self.updateNowPlayingMetadata()

                LibraryStore.shared.recordPlayback(track: track, listenedSeconds: max(20, track.durationSec / 2))

                if autoRadio,
                   self.currentIndex >= self.queue.count - 2,
                   let vid = track.videoId {
                    let radioTracks = await InnerTubeService.shared.fetchRadioQueue(seedVideoId: vid)
                    guard !Task.isCancelled else { return }
                    let existingIds = Set(self.queue.map(\.id))
                    let fresh = radioTracks.filter { !existingIds.contains($0.id) }
                    if !fresh.isEmpty {
                        self.queue.append(contentsOf: fresh.prefix(15))
                    }
                }
            } catch {
                guard !Task.isCancelled else { return }
                self.isLoading = false
                self.isPlaying = false
                self.errorBanner = "Unable to stream this track right now. Tap next or try another version."
            }
        }
    }

    func loadLyrics(for track: Track) {
        isLoadingLyrics = true
        Task {
            let result = await LyricsService.shared.fetchLyrics(for: track)
            if self.currentTrack?.id == track.id {
                self.currentLyrics = result
                self.updateActiveLyricIndex(at: self.currentTime)
                self.isLoadingLyrics = false
            }
        }
    }

    private func resolveStream(for track: Track, preferHighQuality: Bool) async throws -> ResolvedStream {
        if let localURL = LibraryStore.shared.localPlaybackURL(for: track) {
            let ext = localURL.pathExtension.uppercased()
            return ResolvedStream(
                url: localURL,
                headers: [:],
                codecBadge: track.source == .localFile ? track.codecBadge : "Offline \(ext.isEmpty ? "M4A" : ext)",
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true
            )
        }

        if track.source == .subsonic,
           let songId = track.subsonicSongId,
           let url = SubsonicService.shared.signedStreamURL(songId: songId) {
            return ResolvedStream(
                url: url,
                headers: [:],
                codecBadge: track.codecBadge,
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true
            )
        }

        if let vid = track.videoId {
            return try await InnerTubeService.shared.resolvePlayableStream(
                videoId: vid,
                preferHighQuality: preferHighQuality
            )
        }

        if let direct = track.directStreamURL, let url = URL(string: direct) {
            return ResolvedStream(
                url: url,
                headers: [:],
                codecBadge: track.codecBadge,
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true
            )
        }

        throw URLError(.badURL)
    }

    private func observeItemStatus(_ item: AVPlayerItem, track: Track) {
        statusObservation?.invalidate()
        statusObservation = item.observe(\.status, options: [.new, .initial]) { [weak self] observed, _ in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                switch observed.status {
                case .readyToPlay:
                    self.isLoading = false
                    let assetSec = CMTimeGetSeconds(observed.duration)
                    if assetSec.isFinite && assetSec > 0 {
                        self.duration = assetSec
                    }
                    self.updateNowPlayingMetadata()
                case .failed:
                    self.isLoading = false
                    self.isPlaying = false
                    self.errorBanner = "Playback stream interrupted for \(track.title)."
                case .unknown:
                    break
                @unknown default:
                    break
                }
            }
        }
    }

    private func loadArtworkAndPalette(for track: Track) async {
        guard let url = track.displayArtworkURL else {
            currentArtworkImage = nil
            ambientPrimary = Color(hex: 0x2A1A2E)
            ambientSecondary = Color(hex: 0x0A0A10)
            updateNowPlayingMetadata()
            return
        }

        let imageData: Data?
        if url.isFileURL {
            imageData = try? Data(contentsOf: url)
        } else {
            imageData = (try? await URLSession.shared.data(from: url))?.0
        }

        guard self.currentTrack?.id == track.id else { return }
        if let data = imageData, let uiImage = UIImage(data: data) {
            currentArtworkImage = uiImage
            let colors = ArtworkPaletteExtractor.dominantColors(from: uiImage)
            withAnimation(.easeInOut(duration: 0.45)) {
                ambientPrimary = colors.primary
                ambientSecondary = colors.secondary
            }
        } else {
            currentArtworkImage = nil
        }
        updateNowPlayingMetadata()
    }

    private func updateActiveLyricIndex(at timeSec: Double) {
        guard let payload = currentLyrics, payload.synced, !payload.lines.isEmpty else {
            activeLyricIndex = nil
            return
        }
        var active: Int?
        for (idx, line) in payload.lines.enumerated() {
            if timeSec + 0.18 >= line.startTimeSec {
                active = idx
            } else {
                break
            }
        }
        if activeLyricIndex != active {
            activeLyricIndex = active
        }
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.allowAirPlay, .allowBluetoothA2DP])
            try session.setActive(true)
        } catch {
            // Audio session activates on playback start if deferred at launch
        }
    }

    private func installTimeObserver() {
        let interval = CMTime(seconds: 0.18, preferredTimescale: 600)
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] cmTime in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                let sec = CMTimeGetSeconds(cmTime)
                if sec.isFinite && sec >= 0 {
                    self.currentTime = sec
                    self.updateActiveLyricIndex(at: sec)
                }
                if let item = self.player.currentItem {
                    let dur = CMTimeGetSeconds(item.duration)
                    if dur.isFinite && dur > 0 && abs(dur - self.duration) > 0.5 {
                        self.duration = dur
                        self.updateNowPlayingMetadata()
                    }
                }
            }
        }
    }

    private func installNotifications() {
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let item = note.object as? AVPlayerItem else { return }
            Task { @MainActor [weak self] in
                guard let self = self, item == self.player.currentItem else { return }
                self.skipNext(automatic: true)
            }
        }

        NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] note in
            guard let info = note.userInfo,
                  let typeVal = info[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let type = AVAudioSession.InterruptionType(rawValue: typeVal) else {
                return
            }
            let optsVal = info[AVAudioSessionInterruptionOptionKey] as? UInt
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                switch type {
                case .began:
                    self.isPlaying = false
                    self.updateNowPlayingMetadata()
                case .ended:
                    if let optsVal = optsVal,
                       AVAudioSession.InterruptionOptions(rawValue: optsVal).contains(.shouldResume) {
                        try? AVAudioSession.sharedInstance().setActive(true)
                        self.player.playImmediately(atRate: self.playbackRate)
                        self.isPlaying = true
                        self.updateNowPlayingMetadata()
                    }
                @unknown default:
                    break
                }
            }
        }

        NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] note in
            guard let info = note.userInfo,
                  let reasonVal = info[AVAudioSessionRouteChangeReasonKey] as? UInt,
                  let reason = AVAudioSession.RouteChangeReason(rawValue: reasonVal),
                  reason == .oldDeviceUnavailable else {
                return
            }
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.player.pause()
                self.isPlaying = false
                self.updateNowPlayingMetadata()
            }
        }
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.isEnabled = true
        center.playCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self = self, !self.isPlaying else { return }
                self.togglePlayPause()
            }
            return .success
        }

        center.pauseCommand.isEnabled = true
        center.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self = self, self.isPlaying else { return }
                self.togglePlayPause()
            }
            return .success
        }

        center.togglePlayPauseCommand.isEnabled = true
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.togglePlayPause()
            }
            return .success
        }

        center.nextTrackCommand.isEnabled = true
        center.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.skipNext()
            }
            return .success
        }

        center.previousTrackCommand.isEnabled = true
        center.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.skipPrevious()
            }
            return .success
        }

        center.changePlaybackPositionCommand.isEnabled = true
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let posEvent = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            let target = posEvent.positionTime
            Task { @MainActor [weak self] in
                self?.seek(to: target)
            }
            return .success
        }
    }

    private func updateNowPlayingMetadata() {
        guard let track = currentTrack else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPMediaItemPropertyAlbumTitle: track.album,
            MPMediaItemPropertyPlaybackDuration: max(1.0, duration),
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentTime,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? playbackRate : 0.0
        ]
        if let image = currentArtworkImage {
            let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
            info[MPMediaItemPropertyArtwork] = artwork
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}

struct AirPlayRoutePicker: UIViewRepresentable {
    var tintColor: UIColor

    func makeUIView(context: Context) -> AVRoutePickerView {
        let picker = AVRoutePickerView()
        picker.tintColor = tintColor
        picker.activeTintColor = tintColor
        picker.prioritizesVideoDevices = false
        return picker
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {
        uiView.tintColor = tintColor
        uiView.activeTintColor = tintColor
    }
}
