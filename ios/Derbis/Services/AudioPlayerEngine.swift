import Foundation
import AVFoundation
import AVKit
import MediaPlayer
import SwiftUI
import UIKit

@MainActor
final class PlaybackClock: ObservableObject {
    static let shared = PlaybackClock()

    @Published var currentTime: Double = 0
    @Published var duration: Double = 0
}

@MainActor
final class AudioPlayerEngine: ObservableObject {
    static let shared = AudioPlayerEngine()

    @Published var currentTrack: Track?
    @Published var queue: [Track] = []
    @Published var currentIndex: Int = 0
    @Published var isPlaying: Bool = false {
        didSet {
            HapticsAndGesturesService.shared.setPlaybackActive(isPlaying)
            refreshLockScreenLyricsTimerState()
        }
    }
    @Published var isLoading: Bool = false

    var currentTime: Double {
        get { PlaybackClock.shared.currentTime }
        set {
            if abs(PlaybackClock.shared.currentTime - newValue) > 0.015 {
                PlaybackClock.shared.currentTime = newValue
            }
        }
    }

    var duration: Double {
        get { PlaybackClock.shared.duration }
        set {
            if abs(PlaybackClock.shared.duration - newValue) > 0.05 {
                PlaybackClock.shared.duration = newValue
            }
        }
    }

    @Published var isShuffleEnabled: Bool = false
    @Published var repeatMode: RepeatMode = .off
    @Published var playbackRate: Float = 1.0
    @Published var activeCodecBadge: String = "AAC"
    @Published var errorBanner: String?

    @Published var currentLyrics: LyricsPayload? {
        didSet {
            lastLockScreenSubtitle = nil
            lastRenderedLyricCardLine = nil
            refreshLockScreenLyricsTimerState()
            updateNowPlayingMetadata()
        }
    }
    @Published var activeLyricIndex: Int?
    @Published var isLoadingLyrics: Bool = false

    @Published var currentMotionCanvas: MotionCanvasAsset?

    @Published var ambientPrimary: Color = Color(hex: 0x2A1A2E)
    @Published var ambientSecondary: Color = Color(hex: 0x0A0A10)
    @Published var coverVibrantAccent: Color = Color(hex: 0xFC3C44)
    @Published var currentArtworkImage: UIImage?

    @Published var sleepTimerRemainingSec: Int?

    private let player = AVPlayer()
    private var timeObserver: Any?
    private var endObserver: NSObjectProtocol?
    private var playbackFailObserver: NSObjectProtocol?
    private var stallObserver: NSObjectProtocol?
    private var statusObservation: NSKeyValueObservation?
    private var loadTask: Task<Void, Never>?
    private var motionTask: Task<Void, Never>?
    private var prewarmTask: Task<Void, Never>?
    private var autoSkipTask: Task<Void, Never>?
    private var stallWatchdogTask: Task<Void, Never>?
    private var sleepTimerTask: Task<Void, Never>?

    private var lockScreenTimer: DispatchSourceTimer?
    private var lastLockScreenSubtitle: String?
    private var lastRenderedLyricCardLine: Int?
    private var cachedLyricCardArtwork: MPMediaItemArtwork?

    private var activeCandidates: [ResolvedStream] = []
    private var candidateCursor: Int = 0
    private var recoveryPass: Int = 0
    private var isRecovering: Bool = false
    private var pendingResumeSeconds: Double = 0
    private var consecutiveUnrecoverableFailures: Int = 0
    private var isApplyingJamState: Bool = false
    private var transientBannerTask: Task<Void, Never>?

    func showTransientBanner(_ message: String) {
        errorBanner = message
        transientBannerTask?.cancel()
        transientBannerTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 2_600_000_000)
            guard !Task.isCancelled, self?.errorBanner == message else { return }
            self?.errorBanner = nil
        }
    }

    init() {
        player.automaticallyWaitsToMinimizeStalling = true
        playbackRate = Float(AudioEffectsEngine.shared.playbackSpeed)
        configureAudioSession()
        configureRemoteCommands()
        installTimeObserver()
        installNotifications()
        wireAudioEffectsAndGestures()
    }

    private func wireAudioEffectsAndGestures() {
        AudioEffectsEngine.shared.onPlaybackRateSettingsChanged = { [weak self] in
            Task { @MainActor [weak self] in
                self?.applyAudioRateAndPitchSettings()
            }
        }

        HapticsAndGesturesService.shared.onHeadGestureAction = { [weak self] action in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                switch action {
                case .none:
                    break
                case .playPause:
                    self.togglePlayPause()
                case .nextTrack:
                    self.skipNext()
                case .previousTrack:
                    self.skipPrevious()
                case .toggleLike:
                    if let track = self.currentTrack {
                        LibraryStore.shared.toggleLike(track)
                    }
                }
            }
        }
    }

    func applyAudioRateAndPitchSettings() {
        let fx = AudioEffectsEngine.shared
        let nextRate = Float(fx.playbackSpeed)
        playbackRate = nextRate

        let currentIsHLS: Bool = {
            guard activeCandidates.indices.contains(candidateCursor) else { return false }
            return !activeCandidates[candidateCursor].isDirectFile
        }()
        if let item = player.currentItem {
            let desiredAlgo = fx.preferredPitchAlgorithm(isHLS: currentIsHLS)
            if item.audioTimePitchAlgorithm != desiredAlgo {
                item.audioTimePitchAlgorithm = desiredAlgo
            }
        }
        if isPlaying {
            player.rate = nextRate
        }
        updateNowPlayingMetadata()
    }

    func play(track: Track, in contextQueue: [Track]? = nil) {
        if !isApplyingJamState && JamService.shared.isGuest {
            if let existingIdx = queue.firstIndex(where: { $0.id == track.id }),
               JamService.shared.canControlPlayback {
                JamService.shared.dispatchUserAction(.selectIndex(existingIdx))
            } else if JamService.shared.canAddTracks {
                JamService.shared.dispatchUserAction(.addTrack(JamTrack.fromDerbisTrack(track)))
                showTransientBanner("Added \(track.title) to Jam queue.")
            } else {
                showTransientBanner("Host controls playback in this Jam.")
            }
            return
        }

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
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func playNextInQueue(_ track: Track) {
        if !isApplyingJamState && JamService.shared.isGuest {
            if JamService.shared.canAddTracks {
                JamService.shared.dispatchUserAction(.addTrack(JamTrack.fromDerbisTrack(track)))
                showTransientBanner("Added \(track.title) to Jam queue.")
            } else {
                showTransientBanner("Host controls the queue in this Jam.")
            }
            return
        }

        if queue.isEmpty {
            play(track: track, in: [track])
            return
        }
        let insertIndex = min(queue.count, currentIndex + 1)
        queue.insert(track, at: insertIndex)
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func appendToQueue(_ track: Track) {
        if !isApplyingJamState && JamService.shared.isGuest {
            if JamService.shared.canAddTracks {
                JamService.shared.dispatchUserAction(.addTrack(JamTrack.fromDerbisTrack(track)))
                showTransientBanner("Added \(track.title) to Jam queue.")
            } else {
                showTransientBanner("Host controls the queue in this Jam.")
            }
            return
        }

        if queue.isEmpty {
            play(track: track, in: [track])
            return
        }
        queue.append(track)
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func removeFromQueue(at offsets: IndexSet) {
        if !isApplyingJamState && JamService.shared.isGuest {
            guard JamService.shared.canControlPlayback else {
                showTransientBanner("Host controls the queue in this Jam.")
                return
            }
            for offset in offsets where queue.indices.contains(offset) && offset != currentIndex {
                let wireId = queue[offset].videoId ?? queue[offset].id
                JamService.shared.dispatchUserAction(.removeTrack(wireId))
            }
            return
        }

        for offset in offsets.sorted(by: >) {
            guard queue.indices.contains(offset) else { continue }
            if offset == currentIndex { continue }
            queue.remove(at: offset)
            if offset < currentIndex {
                currentIndex = max(0, currentIndex - 1)
            }
        }
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func togglePlayPause() {
        guard let track = currentTrack else { return }
        if !isApplyingJamState && JamService.shared.isGuest {
            JamService.shared.dispatchUserAction(.setPlayWhenReady(!isPlaying))
            return
        }

        if isPlaying {
            player.pause()
            isPlaying = false
            stallWatchdogTask?.cancel()
        } else {
            if player.currentItem == nil || player.currentItem?.status == .failed {
                startCurrentItem(resumeFromSeconds: currentTime > 1.5 ? currentTime : 0, targetTrack: track)
                return
            }
            try? AVAudioSession.sharedInstance().setActive(true)
            player.play()
            player.rate = playbackRate
            isPlaying = true
        }
        updateNowPlayingMetadata()
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func skipBy(seconds delta: Double) {
        let target = max(0, min(currentTime + delta, max(duration, currentTime)))
        seek(to: target)
    }

    func skipNext(automatic: Bool = false) {
        guard !queue.isEmpty else { return }

        if !isApplyingJamState && JamService.shared.isGuest {
            if automatic {
                player.pause()
                isPlaying = false
                updateNowPlayingMetadata()
            } else {
                JamService.shared.dispatchUserAction(.next)
            }
            return
        }

        if automatic && repeatMode == .one {
            seek(to: 0)
            player.play()
            player.rate = playbackRate
            isPlaying = true
            if !isApplyingJamState && JamService.shared.isHost {
                JamService.shared.notifyHostPlaybackChanged()
            }
            return
        }

        let unblockedIndices = queue.indices.filter { !LibraryStore.shared.isArtistBlocked(queue[$0].artist) }

        if isShuffleEnabled && queue.count > 1 {
            let pool = unblockedIndices.filter { $0 != currentIndex }
            let fallbackPool = queue.indices.filter { $0 != currentIndex }
            if let pick = (pool.isEmpty ? fallbackPool : pool).randomElement() {
                currentIndex = pick
                startCurrentItem()
                if !isApplyingJamState && JamService.shared.isHost {
                    JamService.shared.notifyHostPlaybackChanged()
                }
                return
            }
        }

        if let nextUnblocked = findNextPlayableIndex(startingAfter: currentIndex, wrapAround: repeatMode == .all) {
            currentIndex = nextUnblocked
            startCurrentItem()
        } else if unblockedIndices.isEmpty && currentIndex + 1 < queue.count {
            currentIndex += 1
            startCurrentItem()
        } else {
            isPlaying = false
            seek(to: 0)
            player.pause()
            updateNowPlayingMetadata()
        }
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func skipPrevious() {
        guard !queue.isEmpty else { return }
        if !isApplyingJamState && JamService.shared.isGuest {
            JamService.shared.dispatchUserAction(.previous)
            return
        }

        if currentTime > 3.5 {
            seek(to: 0)
            return
        }
        if currentIndex > 0 {
            var candidateIdx = currentIndex - 1
            while candidateIdx > 0 && LibraryStore.shared.isArtistBlocked(queue[candidateIdx].artist) {
                candidateIdx -= 1
            }
            currentIndex = candidateIdx
            startCurrentItem()
        } else if repeatMode == .all {
            currentIndex = max(0, queue.count - 1)
            startCurrentItem()
        } else {
            seek(to: 0)
        }
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    private func findNextPlayableIndex(startingAfter index: Int, wrapAround: Bool) -> Int? {
        guard !queue.isEmpty else { return nil }
        if index + 1 < queue.count {
            for idx in (index + 1)..<queue.count {
                if !LibraryStore.shared.isArtistBlocked(queue[idx].artist) {
                    return idx
                }
            }
        }
        if wrapAround {
            for idx in 0...min(index, queue.count - 1) {
                if !LibraryStore.shared.isArtistBlocked(queue[idx].artist) {
                    return idx
                }
            }
        }
        return nil
    }

    func seek(to seconds: Double) {
        let clamped = max(0, min(seconds, max(duration, seconds)))
        if !isApplyingJamState && JamService.shared.isGuest {
            JamService.shared.dispatchUserAction(.seek(Int64(clamped * 1000.0)))
            return
        }
        let cm = CMTime(seconds: clamped, preferredTimescale: 600)
        player.currentItem?.cancelPendingSeeks()
        player.seek(to: cm, toleranceBefore: .zero, toleranceAfter: .zero)
        PlaybackClock.shared.currentTime = clamped
        updateActiveLyricIndex(at: clamped)
        updateNowPlayingMetadata()
        if !isApplyingJamState && JamService.shared.isHost {
            JamService.shared.notifyHostPlaybackChanged()
        }
    }

    func setRate(_ rate: Float) {
        AudioEffectsEngine.shared.playbackSpeed = Double(rate)
        playbackRate = Float(AudioEffectsEngine.shared.playbackSpeed)
        if isPlaying {
            player.rate = playbackRate
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

    private func startCurrentItem(resumeFromSeconds: Double = 0, targetTrack: Track? = nil) {
        let track: Track
        if let explicit = targetTrack {
            track = explicit
        } else {
            guard queue.indices.contains(currentIndex) else { return }
            track = queue[currentIndex]
        }

        let isSameTrackResume = (currentTrack?.id == track.id && resumeFromSeconds > 0)
        currentTrack = track
        if !isSameTrackResume {
            currentTime = 0
            duration = Double(track.durationSec)
            currentLyrics = nil
            activeLyricIndex = nil
            currentMotionCanvas = nil
            lastLockScreenSubtitle = nil
            lastRenderedLyricCardLine = nil
            cachedLyricCardArtwork = nil
            recoveryPass = 0
        }

        isLoading = true
        isRecovering = false
        errorBanner = nil
        activeCodecBadge = track.codecBadge
        activeCandidates = []
        candidateCursor = 0
        AudioEffectsEngine.shared.markCurrentStreamTapActive(false)

        loadTask?.cancel()
        prewarmTask?.cancel()
        autoSkipTask?.cancel()
        stallWatchdogTask?.cancel()
        statusObservation?.invalidate()
        player.pause()
        player.replaceCurrentItem(with: nil)

        let preferHQ = UserDefaults.standard.object(forKey: "derbis.highQualityAudio") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.highQualityAudio")
        let autoLyrics = UserDefaults.standard.object(forKey: "derbis.autoLoadLyrics") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.autoLoadLyrics")
        let autoRadio = UserDefaults.standard.object(forKey: "derbis.endlessRadio") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.endlessRadio")

        if !isSameTrackResume {
            Task {
                await loadArtworkAndPalette(for: track)
            }
            if autoLyrics {
                loadLyrics(for: track)
            }
            loadMotionCanvas(for: track)
        }

        loadTask = Task { [weak self] in
            guard let self = self else { return }
            do {
                let ladder = try await self.resolveCandidateLadder(
                    for: track,
                    preferHighQuality: preferHQ,
                    forceRefresh: false
                )
                guard !Task.isCancelled, self.currentTrack?.id == track.id else { return }

                self.activeCandidates = ladder
                self.candidateCursor = 0
                self.mountCandidate(ladder[0], for: track, resumeSeconds: resumeFromSeconds)

                if !isSameTrackResume {
                    LibraryStore.shared.recordPlayback(track: track, listenedSeconds: max(20, track.durationSec / 2))
                    GoogleAccountSyncService.shared.reportPlaybackIfEnabled(track: track)
                }

                if autoRadio,
                   self.currentIndex >= self.queue.count - 2,
                   let vid = track.videoId {
                    let radioTracks = await InnerTubeService.shared.fetchRadioQueue(seedVideoId: vid)
                    guard !Task.isCancelled else { return }
                    let existingIds = Set(self.queue.map(\.id))
                    let fresh = radioTracks.filter {
                        !existingIds.contains($0.id) && !LibraryStore.shared.isArtistBlocked($0.artist)
                    }
                    if !fresh.isEmpty {
                        self.queue.append(contentsOf: fresh.prefix(15))
                    }
                }
            } catch {
                guard !Task.isCancelled, self.currentTrack?.id == track.id else { return }
                self.handleTerminalPlaybackFailure(for: track)
            }
        }
    }

    private func handleTerminalPlaybackFailure(for track: Track) {
        isRecovering = false
        isLoading = false
        isPlaying = false
        consecutiveUnrecoverableFailures += 1

        if queue.count > 1,
           consecutiveUnrecoverableFailures <= 3,
           findNextPlayableIndex(startingAfter: currentIndex, wrapAround: repeatMode == .all) != nil {
            errorBanner = "Track unavailable — skipping to next…"
            autoSkipTask?.cancel()
            autoSkipTask = Task { @MainActor [weak self] in
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled, let self = self, self.currentTrack?.id == track.id else { return }
                self.skipNext(automatic: true)
            }
        } else {
            errorBanner = "Stream unavailable right now. Tap play to retry or skip to next."
        }
    }

    private func prewarmUpcomingTrack() {
        prewarmTask?.cancel()
        guard let nextIdx = findNextPlayableIndex(startingAfter: currentIndex, wrapAround: repeatMode == .all),
              queue.indices.contains(nextIdx) else {
            return
        }
        let nextTrack = queue[nextIdx]
        let preferHQ = UserDefaults.standard.object(forKey: "derbis.highQualityAudio") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.highQualityAudio")

        prewarmTask = Task(priority: .utility) {
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard !Task.isCancelled else { return }
            ArtworkImageCache.shared.prefetch(url: nextTrack.displayArtworkURL, pointSize: 320)
            if nextTrack.source == .youtubeMusic, LibraryStore.shared.localPlaybackURL(for: nextTrack) == nil {
                _ = try? await InnerTubeService.shared.resolveStreamCandidates(
                    for: nextTrack,
                    preferHighQuality: preferHQ,
                    forceRefresh: false
                )
            }
        }
    }

    private func mountCandidate(_ candidate: ResolvedStream, for track: Track, resumeSeconds: Double) {
        activeCodecBadge = candidate.codecBadge
        errorBanner = nil
        isLoading = true
        AudioEffectsEngine.shared.markCurrentStreamTapActive(false)

        try? AVAudioSession.sharedInstance().setActive(true)

        let asset: AVURLAsset
        if candidate.headers.isEmpty {
            asset = AVURLAsset(url: candidate.url)
        } else {
            asset = AVURLAsset(
                url: candidate.url,
                options: ["AVURLAssetHTTPHeaderFieldsKey": candidate.headers]
            )
        }

        let item = AVPlayerItem(asset: asset)
        item.preferredForwardBufferDuration = candidate.isDirectFile ? 10 : 16
        item.audioTimePitchAlgorithm = AudioEffectsEngine.shared.preferredPitchAlgorithm(isHLS: !candidate.isDirectFile)

        pendingResumeSeconds = resumeSeconds
        observeItemStatus(item, track: track, candidate: candidate)
        player.replaceCurrentItem(with: item)
        player.automaticallyWaitsToMinimizeStalling = true
        playbackRate = Float(AudioEffectsEngine.shared.playbackSpeed)
        player.play()
        if abs(playbackRate - 1.0) > 0.01 {
            player.rate = playbackRate
        }
        isPlaying = true
        updateNowPlayingMetadata()
    }

    private func recoverFromStreamFailure(for track: Track, failedCandidate: ResolvedStream) {
        guard currentTrack?.id == track.id, !isRecovering else { return }
        isRecovering = true
        stallWatchdogTask?.cancel()

        let resumeSeconds = currentTime > 1.5 ? currentTime : 0

        if failedCandidate.clientLabel == "LOCAL" && track.source == .youtubeMusic {
            LibraryStore.shared.invalidateCorruptedOfflineDownload(track)
        }

        Task { [weak self] in
            if !failedCandidate.url.isFileURL {
                await InnerTubeService.shared.quarantineStream(
                    url: failedCandidate.url,
                    videoId: track.videoId,
                    clientLabel: failedCandidate.clientLabel
                )
            }

            guard let self = self, self.currentTrack?.id == track.id else { return }

            if self.candidateCursor + 1 < self.activeCandidates.count {
                self.candidateCursor += 1
                let nextCandidate = self.activeCandidates[self.candidateCursor]
                self.isRecovering = false
                self.mountCandidate(nextCandidate, for: track, resumeSeconds: resumeSeconds)
                return
            }

            if self.recoveryPass < 2 && track.source == .youtubeMusic {
                self.recoveryPass += 1
                let preferHQ = UserDefaults.standard.object(forKey: "derbis.highQualityAudio") == nil
                    ? true
                    : UserDefaults.standard.bool(forKey: "derbis.highQualityAudio")

                if let freshLadder = try? await self.resolveCandidateLadder(
                    for: track,
                    preferHighQuality: preferHQ,
                    forceRefresh: true
                ), let firstFresh = freshLadder.first {
                    guard self.currentTrack?.id == track.id else { return }
                    self.activeCandidates = freshLadder
                    self.candidateCursor = 0
                    self.isRecovering = false
                    self.mountCandidate(firstFresh, for: track, resumeSeconds: resumeSeconds)
                    return
                }
            }

            self.handleTerminalPlaybackFailure(for: track)
        }
    }

    func loadLyrics(for track: Track, forceRefresh: Bool = false) {
        isLoadingLyrics = true
        Task {
            let result = await LyricsService.shared.fetchLyrics(for: track, forceRefresh: forceRefresh)
            if self.currentTrack?.id == track.id {
                self.currentLyrics = result
                self.updateActiveLyricIndex(at: self.currentTime)
                self.isLoadingLyrics = false
            }
        }
    }

    func refreshMotionCanvas() {
        guard let track = currentTrack else { return }
        loadMotionCanvas(for: track)
    }

    func loadMotionCanvas(for track: Track) {
        motionTask?.cancel()
        let motionEnabled = UserDefaults.standard.object(forKey: "derbis.motionCanvasEnabled") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.motionCanvasEnabled")
        guard motionEnabled else {
            currentMotionCanvas = nil
            return
        }
        let styleRaw = UserDefaults.standard.string(forKey: "derbis.motionCanvasStyle") ?? MotionCanvasStyle.immersive.rawValue
        let preferTall = (styleRaw == MotionCanvasStyle.immersive.rawValue)

        motionTask = Task { [weak self] in
            let resolved = await MotionCanvasService.shared.resolveMotionCanvas(for: track, preferTall: preferTall)
            guard !Task.isCancelled, let self = self, self.currentTrack?.id == track.id else { return }
            withAnimation(.easeInOut(duration: 0.35)) {
                self.currentMotionCanvas = resolved
            }
        }
    }

    private func resolveCandidateLadder(
        for track: Track,
        preferHighQuality: Bool,
        forceRefresh: Bool
    ) async throws -> [ResolvedStream] {
        if let localURL = LibraryStore.shared.localPlaybackURL(for: track) {
            let ext = localURL.pathExtension.uppercased()
            let localStream = ResolvedStream(
                url: localURL,
                headers: [:],
                codecBadge: track.source == .localFile ? track.codecBadge : "Offline \(ext.isEmpty ? "M4A" : ext)",
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true,
                clientLabel: "LOCAL"
            )
            return [localStream]
        }

        if track.source == .subsonic,
           let songId = track.subsonicSongId,
           let url = SubsonicService.shared.signedStreamURL(songId: songId) {
            let serverStream = ResolvedStream(
                url: url,
                headers: [:],
                codecBadge: track.codecBadge,
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true,
                clientLabel: "SUBSONIC"
            )
            return [serverStream]
        }

        if track.source == .youtubeMusic {
            return try await InnerTubeService.shared.resolveStreamCandidates(
                for: track,
                preferHighQuality: preferHighQuality,
                requireDirectFile: false,
                forceRefresh: forceRefresh
            )
        }

        if let direct = track.directStreamURL, let url = URL(string: direct) {
            let stream = ResolvedStream(
                url: url,
                headers: [:],
                codecBadge: track.codecBadge,
                bitrateKbps: track.bitrateKbps,
                isDirectFile: true,
                clientLabel: "DIRECT"
            )
            return [stream]
        }

        throw URLError(.badURL)
    }

    private func observeItemStatus(
        _ item: AVPlayerItem,
        track: Track,
        candidate: ResolvedStream
    ) {
        statusObservation?.invalidate()

        statusObservation = item.observe(\.status, options: [.new, .initial]) { [weak self] observed, _ in
            Task { @MainActor [weak self] in
                guard let self = self, self.player.currentItem === observed else { return }
                switch observed.status {
                case .readyToPlay:
                    self.isLoading = false
                    self.consecutiveUnrecoverableFailures = 0
                    let assetSec = CMTimeGetSeconds(observed.duration)
                    if assetSec.isFinite && assetSec > 0 {
                        self.duration = assetSec
                    }
                    if self.pendingResumeSeconds > 1.5 {
                        let target = self.pendingResumeSeconds
                        self.pendingResumeSeconds = 0
                        let cm = CMTime(seconds: target, preferredTimescale: 600)
                        self.player.seek(to: cm, toleranceBefore: .zero, toleranceAfter: .zero)
                    } else {
                        self.pendingResumeSeconds = 0
                    }
                    if self.isPlaying && abs(self.playbackRate - 1.0) > 0.01 {
                        self.player.rate = self.playbackRate
                    }
                    if candidate.isDirectFile && observed.audioMix == nil {
                        Task { @MainActor [weak self] in
                            guard let self = self, self.player.currentItem === observed else { return }
                            await AudioEffectsEngine.shared.attachAudioMixIfSupported(
                                to: observed,
                                isDirectFile: true
                            )
                        }
                    }
                    self.prewarmUpcomingTrack()
                    self.updateNowPlayingMetadata()
                case .failed:
                    self.recoverFromStreamFailure(for: track, failedCandidate: candidate)
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
            lastRenderedLyricCardLine = nil
            cachedLyricCardArtwork = nil
            updateNowPlayingMetadata()
            return
        }

        let resolvedUIImage = await ArtworkImageCache.shared.loadImage(for: url, pointSize: 512)

        guard self.currentTrack?.id == track.id else { return }
        if let uiImage = resolvedUIImage {
            currentArtworkImage = uiImage
            lastRenderedLyricCardLine = nil
            cachedLyricCardArtwork = nil
            let colors = ArtworkPaletteExtractor.dominantColors(from: uiImage)
            let vibrant = ArtworkPaletteExtractor.vibrantAccent(from: uiImage)
            withAnimation(.easeInOut(duration: 0.45)) {
                ambientPrimary = colors.primary
                ambientSecondary = colors.secondary
                coverVibrantAccent = vibrant
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
            if timeSec + 0.16 >= line.startTimeSec {
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
            // Deferred activation occurs when user initiates playback
        }
    }

    private func installTimeObserver() {
        let interval = CMTime(seconds: 0.10, preferredTimescale: 600)
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

    private func refreshLockScreenLyricsTimerState() {
        let liveEnabled = UserDefaults.standard.object(forKey: "derbis.lockScreenLiveLyrics") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.lockScreenLiveLyrics")
        let cardEnabled = UserDefaults.standard.bool(forKey: "derbis.lockScreenLyricArt")
        let hasSynced = (currentLyrics?.synced == true) && !(currentLyrics?.lines.isEmpty ?? true)

        if isPlaying && hasSynced && (liveEnabled || cardEnabled) {
            guard lockScreenTimer == nil else { return }
            let timer = DispatchSource.makeTimerSource(flags: .strict, queue: .main)
            timer.schedule(deadline: .now() + 0.15, repeating: 0.30, leeway: .milliseconds(25))
            timer.setEventHandler { [weak self] in
                Task { @MainActor [weak self] in
                    self?.tickLockScreenLyrics()
                }
            }
            timer.resume()
            lockScreenTimer = timer
        } else {
            lockScreenTimer?.cancel()
            lockScreenTimer = nil
        }
    }

    private func tickLockScreenLyrics() {
        guard isPlaying, let track = currentTrack, let payload = currentLyrics, payload.synced else { return }
        let rawNow = CMTimeGetSeconds(player.currentTime())
        let playhead = (rawNow.isFinite && rawNow >= 0) ? rawNow : currentTime
        let leadTime = playhead + 0.20

        let liveEnabled = UserDefaults.standard.object(forKey: "derbis.lockScreenLiveLyrics") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "derbis.lockScreenLiveLyrics")
        let cardEnabled = UserDefaults.standard.bool(forKey: "derbis.lockScreenLyricArt")

        var lineIdx: Int?
        for (idx, line) in payload.lines.enumerated() {
            if leadTime >= line.startTimeSec {
                lineIdx = idx
            } else {
                break
            }
        }

        let computedSubtitle: String = {
            guard liveEnabled, let idx = lineIdx else { return track.artist }
            let line = payload.lines[idx]
            let endSec = line.endTimeSec
            if leadTime > endSec + 4.2 {
                let nextStart = (idx + 1 < payload.lines.count) ? payload.lines[idx + 1].startTimeSec : (duration + 10)
                if nextStart - endSec >= 4.2 {
                    return track.artist
                }
            }
            return Self.compactLockScreenSnippet(for: line, at: leadTime)
        }()

        let lineChanged = (lineIdx != lastRenderedLyricCardLine)
        let subtitleChanged = (computedSubtitle != lastLockScreenSubtitle)
        guard subtitleChanged || (cardEnabled && lineChanged) else { return }

        lastLockScreenSubtitle = computedSubtitle
        updateNowPlayingMetadata(overrideArtistSubtitle: computedSubtitle, activeLineForCard: cardEnabled ? lineIdx : nil)
    }

    private static func compactLockScreenSnippet(for line: LyricLine, at playheadSec: Double) -> String {
        let full = line.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard full.count > 34, line.words.count >= 2 else {
            return full.isEmpty ? "♪" : full
        }

        var chunks: [(start: Double, text: String)] = []
        var bucket = ""
        var bucketStart = line.words[0].startTimeSec

        for word in line.words {
            let separator = (bucket.isEmpty || word.joinedToPrevious) ? "" : " "
            let candidate = bucket + separator + word.text
            if candidate.count > 32 && !bucket.isEmpty {
                chunks.append((bucketStart, bucket))
                bucket = word.text
                bucketStart = word.startTimeSec
            } else {
                if bucket.isEmpty { bucketStart = word.startTimeSec }
                bucket = candidate
            }
        }
        if !bucket.isEmpty {
            chunks.append((bucketStart, bucket))
        }

        var chosen = chunks.first?.text ?? full
        for chunk in chunks where playheadSec >= chunk.start {
            chosen = chunk.text
        }
        return chosen
    }

    func refreshNowPlayingLockScreenPreferences() {
        lastLockScreenSubtitle = nil
        lastRenderedLyricCardLine = nil
        cachedLyricCardArtwork = nil
        refreshLockScreenLyricsTimerState()
        updateNowPlayingMetadata()
    }

    private func installNotifications() {
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let item = note.object as? AVPlayerItem else { return }
            Task { @MainActor [weak self] in
                guard let self = self, item === self.player.currentItem else { return }
                self.skipNext(automatic: true)
            }
        }

        playbackFailObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let item = note.object as? AVPlayerItem else { return }
            Task { @MainActor [weak self] in
                guard let self = self,
                      item === self.player.currentItem,
                      let track = self.currentTrack,
                      self.activeCandidates.indices.contains(self.candidateCursor) else {
                    return
                }
                let failed = self.activeCandidates[self.candidateCursor]
                self.recoverFromStreamFailure(for: track, failedCandidate: failed)
            }
        }

        stallObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemPlaybackStalled,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let item = note.object as? AVPlayerItem else { return }
            Task { @MainActor [weak self] in
                guard let self = self,
                      item === self.player.currentItem,
                      let track = self.currentTrack,
                      self.isPlaying,
                      self.activeCandidates.indices.contains(self.candidateCursor) else {
                    return
                }
                let snapshotTime = self.currentTime
                let candidate = self.activeCandidates[self.candidateCursor]
                self.stallWatchdogTask?.cancel()
                self.stallWatchdogTask = Task { @MainActor [weak self] in
                    try? await Task.sleep(nanoseconds: 7_500_000_000)
                    guard !Task.isCancelled,
                          let self = self,
                          self.isPlaying,
                          self.currentTrack?.id == track.id,
                          abs(self.currentTime - snapshotTime) < 0.25 else {
                        return
                    }
                    self.recoverFromStreamFailure(for: track, failedCandidate: candidate)
                }
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
                        self.player.play()
                        if abs(self.playbackRate - 1.0) > 0.01 {
                            self.player.rate = self.playbackRate
                        }
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

        NotificationCenter.default.addObserver(
            forName: AVAudioSession.mediaServicesWereResetNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.configureAudioSession()
                self.configureRemoteCommands()
                if let track = self.currentTrack, self.isPlaying {
                    let resume = self.currentTime
                    self.startCurrentItem(resumeFromSeconds: resume, targetTrack: track)
                }
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

    private func updateNowPlayingMetadata(overrideArtistSubtitle: String? = nil, activeLineForCard: Int? = nil) {
        guard let track = currentTrack else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }

        let rawNow = CMTimeGetSeconds(player.currentTime())
        let accurateElapsed = (rawNow.isFinite && rawNow >= 0) ? rawNow : currentTime
        let subtitle = overrideArtistSubtitle ?? lastLockScreenSubtitle ?? track.artist

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: subtitle,
            MPMediaItemPropertyAlbumTitle: track.album,
            MPMediaItemPropertyPlaybackDuration: max(1.0, duration),
            MPNowPlayingInfoPropertyElapsedPlaybackTime: accurateElapsed,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? playbackRate : 0.0
        ]

        let cardEnabled = UserDefaults.standard.bool(forKey: "derbis.lockScreenLyricArt")
        let chosenLineIdx = activeLineForCard ?? activeLyricIndex
        if cardEnabled,
           let payload = currentLyrics,
           payload.synced,
           let idx = chosenLineIdx,
           payload.lines.indices.contains(idx) {
            if lastRenderedLyricCardLine != idx || cachedLyricCardArtwork == nil {
                let cardImage = renderLockScreenLyricArtwork(track: track, lines: payload.lines, activeIndex: idx)
                cachedLyricCardArtwork = MPMediaItemArtwork(boundsSize: cardImage.size) { _ in cardImage }
                lastRenderedLyricCardLine = idx
            }
            if let art = cachedLyricCardArtwork {
                info[MPMediaItemPropertyArtwork] = art
            }
        } else if let image = currentArtworkImage {
            let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
            info[MPMediaItemPropertyArtwork] = artwork
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func renderLockScreenLyricArtwork(track: Track, lines: [LyricLine], activeIndex: Int) -> UIImage {
        let side: CGFloat = 540
        let size = CGSize(width: side, height: side)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 2.0
        format.opaque = true

        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        return renderer.image { ctx in
            let rect = CGRect(origin: .zero, size: size)
            UIColor(red: 0.06, green: 0.06, blue: 0.09, alpha: 1.0).setFill()
            ctx.fill(rect)

            if let cover = currentArtworkImage {
                let aspect = max(side / max(1, cover.size.width), side / max(1, cover.size.height))
                let drawW = cover.size.width * aspect
                let drawH = cover.size.height * aspect
                let drawRect = CGRect(x: (side - drawW) * 0.5, y: (side - drawH) * 0.5, width: drawW, height: drawH)
                cover.draw(in: drawRect)
            }

            UIColor(white: 0.0, alpha: 0.74).setFill()
            ctx.fill(rect)

            let headerPara = NSMutableParagraphStyle()
            headerPara.lineBreakMode = .byTruncatingTail
            let headerAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 18, weight: .semibold),
                .foregroundColor: UIColor(white: 1.0, alpha: 0.55),
                .paragraphStyle: headerPara
            ]
            let headerStr = "\(track.artist)  •  DERBIS"
            headerStr.draw(in: CGRect(x: 36, y: 34, width: side - 72, height: 26), withAttributes: headerAttrs)

            let prevText = (activeIndex > 0) ? lines[activeIndex - 1].text : ""
            let currText = lines[activeIndex].text
            let nextText = (activeIndex + 1 < lines.count) ? lines[activeIndex + 1].text : ""

            let bodyPara = NSMutableParagraphStyle()
            bodyPara.lineSpacing = 4

            if !prevText.isEmpty {
                let prevAttrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 22, weight: .semibold),
                    .foregroundColor: UIColor(white: 1.0, alpha: 0.30),
                    .paragraphStyle: bodyPara
                ]
                prevText.draw(in: CGRect(x: 36, y: 102, width: side - 72, height: 64), withAttributes: prevAttrs)
            }

            let currFontSize: CGFloat = currText.count > 65 ? 28 : (currText.count > 38 ? 33 : 38)
            let currAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: currFontSize, weight: .bold),
                .foregroundColor: UIColor.white,
                .paragraphStyle: bodyPara
            ]
            currText.draw(in: CGRect(x: 36, y: 184, width: side - 72, height: 215), withAttributes: currAttrs)

            if !nextText.isEmpty {
                let nextAttrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 22, weight: .semibold),
                    .foregroundColor: UIColor(white: 1.0, alpha: 0.36),
                    .paragraphStyle: bodyPara
                ]
                nextText.draw(in: CGRect(x: 36, y: 420, width: side - 72, height: 78), withAttributes: nextAttrs)
            }
        }
    }

    struct JamPlaybackSnapshot {
        let queue: [JamTrack]
        let currentIndex: Int
        let positionMs: Int64
        let playWhenReady: Bool
        let shuffle: Bool
        let repeatMode: Int
    }

    func jamSnapshot() -> JamPlaybackSnapshot {
        let mapped = queue.prefix(JamSessionState.maxQueueSize).map { JamTrack.fromDerbisTrack($0) }
        let repOrdinal: Int = {
            switch repeatMode {
            case .off: return 0
            case .all: return 1
            case .one: return 2
            }
        }()
        let liveSec = CMTimeGetSeconds(player.currentTime())
        let effectiveSec = (liveSec.isFinite && liveSec >= 0) ? liveSec : currentTime
        return JamPlaybackSnapshot(
            queue: mapped,
            currentIndex: mapped.isEmpty ? -1 : min(max(0, currentIndex), mapped.count - 1),
            positionMs: Int64(max(0, effectiveSec * 1000.0)),
            playWhenReady: isPlaying,
            shuffle: isShuffleEnabled,
            repeatMode: repOrdinal
        )
    }

    func applyJamHostAction(_ action: JamAction) {
        isApplyingJamState = true
        defer { isApplyingJamState = false }

        switch action {
        case .addTrack(let jt):
            let converted = jt.toDerbisTrack()
            if queue.isEmpty {
                play(track: converted, in: [converted])
            } else if !queue.contains(where: { $0.id == converted.id }) {
                queue.append(converted)
            }
        case .addTracks(let list):
            let existing = Set(queue.map(\.id))
            let converted = list.map { $0.toDerbisTrack() }.filter { !existing.contains($0.id) }
            if queue.isEmpty, let first = converted.first {
                play(track: first, in: converted)
            } else {
                queue.append(contentsOf: converted)
            }
        case .playNextTracks(let list):
            let existing = Set(queue.map(\.id))
            let converted = list.map { $0.toDerbisTrack() }.filter { !existing.contains($0.id) }
            if queue.isEmpty, let first = converted.first {
                play(track: first, in: converted)
            } else {
                let idx = min(queue.count, currentIndex + 1)
                queue.insert(contentsOf: converted, at: idx)
            }
        case .removeTrack(let wireId):
            if let idx = queue.firstIndex(where: { ($0.videoId ?? $0.id) == wireId || $0.id == wireId }),
               idx != currentIndex {
                removeFromQueue(at: IndexSet(integer: idx))
            }
        case .selectIndex(let idx):
            if queue.indices.contains(idx) {
                currentIndex = idx
                startCurrentItem()
            }
        case .setPlayWhenReady(let playReady):
            if isPlaying != playReady {
                togglePlayPause()
            }
        case .seek(let posMs):
            seek(to: Double(max(0, posMs)) / 1000.0)
        case .next:
            skipNext(automatic: false)
        case .previous:
            skipPrevious()
        }
    }

    func applyJamRemoteState(_ state: JamSessionState) {
        guard !JamService.shared.isHost else { return }
        isApplyingJamState = true
        defer { isApplyingJamState = false }

        isShuffleEnabled = state.shuffle
        switch state.repeatMode {
        case 1: repeatMode = .all
        case 2: repeatMode = .one
        default: repeatMode = .off
        }

        if state.queue.isEmpty {
            player.pause()
            isPlaying = false
            queue = []
            currentTrack = nil
            return
        }

        let mappedQueue = state.queue.map { $0.toDerbisTrack() }
        let targetIdx = min(max(0, state.currentIndex), mappedQueue.count - 1)
        let targetTrack = mappedQueue[targetIdx]
        let predictedMs = JamService.predictedPositionMs(for: state)
        let predictedSec = Double(predictedMs) / 1000.0

        let queueChanged = mappedQueue.map(\.id) != queue.map(\.id)
        let trackChanged = currentTrack?.id != targetTrack.id

        if queueChanged {
            queue = mappedQueue
        }
        currentIndex = targetIdx

        if trackChanged {
            startCurrentItem(resumeFromSeconds: predictedSec, targetTrack: targetTrack)
            if !state.playWhenReady {
                player.pause()
                isPlaying = false
                updateNowPlayingMetadata()
            }
            return
        }

        if isPlaying != state.playWhenReady {
            if state.playWhenReady {
                if player.currentItem == nil || player.currentItem?.status == .failed {
                    startCurrentItem(resumeFromSeconds: predictedSec, targetTrack: targetTrack)
                    return
                }
                try? AVAudioSession.sharedInstance().setActive(true)
                player.play()
                player.rate = playbackRate
                isPlaying = true
            } else {
                player.pause()
                isPlaying = false
            }
            updateNowPlayingMetadata()
        }

        let localMs = Int64(max(0, currentTime * 1000.0))
        if !isLoading && JamService.shouldSeek(localMs: localMs, predictedMs: predictedMs, playWhenReady: state.playWhenReady) {
            seek(to: predictedSec)
        }
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
