import SwiftUI
import UIKit

final class ArtworkImageCache: @unchecked Sendable {
    static let shared = ArtworkImageCache()

    private let cache = NSCache<NSString, UIImage>()
    private let session: URLSession
    private let lock = NSLock()
    private var inFlightTasks: [String: Task<UIImage?, Never>] = [:]
    private var failedDeadlines: [String: Date] = [:]

    private init() {
        cache.countLimit = 280
        cache.totalCostLimit = 48 * 1024 * 1024

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 8
        config.timeoutIntervalForResource = 14
        config.urlCache = URLCache(
            memoryCapacity: 24 * 1024 * 1024,
            diskCapacity: 120 * 1024 * 1024
        )
        self.session = URLSession(configuration: config)

        NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleMemoryWarning()
        }
    }

    private func handleMemoryWarning() {
        cache.removeAllObjects()
        lock.withLock {
            failedDeadlines.removeAll()
        }
    }

    private func cacheKey(url: URL, bucketPx: Int) -> String {
        "\(bucketPx)|\(url.absoluteString)"
    }

    private func pixelBucket(for pointSize: CGFloat) -> Int {
        if pointSize <= 64 { return 160 }
        if pointSize <= 170 { return 360 }
        return 760
    }

    func cachedImage(for url: URL, pointSize: CGFloat) -> UIImage? {
        let key = cacheKey(url: url, bucketPx: pixelBucket(for: pointSize)) as NSString
        return cache.object(forKey: key)
    }

    func prefetch(url: URL?, pointSize: CGFloat = 320) {
        guard let url = url else { return }
        if cachedImage(for: url, pointSize: pointSize) != nil { return }
        Task.detached(priority: .utility) {
            _ = await ArtworkImageCache.shared.loadImage(for: url, pointSize: pointSize)
        }
    }

    private enum InFlightLookup {
        case recentlyFailed
        case task(Task<UIImage?, Never>)
    }

    private func lookupOrCreateInFlightTask(
        key: String,
        urlString: String,
        builder: () -> Task<UIImage?, Never>
    ) -> InFlightLookup {
        lock.withLock {
            if let deadline = failedDeadlines[urlString] {
                if deadline > Date() {
                    return .recentlyFailed
                }
                failedDeadlines.removeValue(forKey: urlString)
            }
            if let existing = inFlightTasks[key] {
                return .task(existing)
            }
            let created = builder()
            inFlightTasks[key] = created
            return .task(created)
        }
    }

    private func finishInFlightTask(key: String, urlString: String, succeeded: Bool) {
        lock.withLock {
            inFlightTasks.removeValue(forKey: key)
            if !succeeded {
                if failedDeadlines.count > 250 {
                    let now = Date()
                    failedDeadlines = failedDeadlines.filter { $0.value > now }
                }
                failedDeadlines[urlString] = Date().addingTimeInterval(120)
            }
        }
    }

    func loadImage(for url: URL, pointSize: CGFloat) async -> UIImage? {
        let bucket = pixelBucket(for: pointSize)
        let key = cacheKey(url: url, bucketPx: bucket)
        if let hit = cache.object(forKey: key as NSString) {
            return hit
        }

        let urlStr = url.absoluteString
        let lookup = lookupOrCreateInFlightTask(key: key, urlString: urlStr) {
            Task.detached(priority: .userInitiated) { [weak self] () -> UIImage? in
                guard let self = self else { return nil }
                let rawData: Data?
                if url.isFileURL {
                    rawData = try? Data(contentsOf: url, options: [.mappedIfSafe])
                } else {
                    var req = URLRequest(url: url)
                    req.cachePolicy = .returnCacheDataElseLoad
                    if let (data, resp) = try? await self.session.data(for: req),
                       let http = resp as? HTTPURLResponse,
                       (200..<300).contains(http.statusCode) {
                        rawData = data
                    } else {
                        rawData = nil
                    }
                }

                guard let data = rawData, !data.isEmpty, let decoded = UIImage(data: data) else {
                    return nil
                }

                let maxDim = CGFloat(bucket)
                let origW = decoded.size.width * decoded.scale
                let origH = decoded.size.height * decoded.scale
                let prepared: UIImage
                if max(origW, origH) > maxDim * 1.25 {
                    let ratio = min(maxDim / max(1, origW), maxDim / max(1, origH))
                    let targetSize = CGSize(width: max(1, floor(origW * ratio)), height: max(1, floor(origH * ratio)))
                    prepared = await decoded.byPreparingThumbnail(ofSize: targetSize) ?? decoded
                } else {
                    prepared = await decoded.byPreparingForDisplay() ?? decoded
                }

                let cost = Int(prepared.size.width * prepared.size.height * prepared.scale * prepared.scale * 4)
                self.cache.setObject(prepared, forKey: key as NSString, cost: max(1024, cost))
                return prepared
            }
        }

        let task: Task<UIImage?, Never>
        switch lookup {
        case .recentlyFailed:
            return nil
        case .task(let active):
            task = active
        }

        let result = await task.value
        finishInFlightTask(key: key, urlString: urlStr, succeeded: result != nil)
        return result
    }
}

struct TrackArtworkView: View {
    @EnvironmentObject private var theme: ThemeManager
    let url: URL?
    let size: CGFloat
    var cornerRadius: CGFloat? = nil

    @State private var loadedImage: UIImage?
    @State private var loadedKey: String = ""

    private var isYouTubeFourByThreeThumb: Bool {
        guard let host = url?.host?.lowercased(), let path = url?.path.lowercased() else {
            return false
        }
        return host.contains("ytimg.com") && (path.contains("hqdefault") || path.contains("sddefault"))
    }

    var body: some View {
        let radius = theme.isPixelSkin ? 0 : (cornerRadius ?? theme.artworkRadius)
        let zoom: CGFloat = isYouTubeFourByThreeThumb ? 1.35 : 1.0
        let instantHit = url.flatMap { ArtworkImageCache.shared.cachedImage(for: $0, pointSize: size) }
        let currentKey = url?.absoluteString ?? ""
        let resolvedImage = instantHit ?? (loadedKey == currentKey ? loadedImage : nil)

        ZStack {
            theme.elevatedSurface
            if let uiImg = resolvedImage {
                Image(uiImage: uiImg)
                    .resizable()
                    .scaledToFill()
                    .scaleEffect(zoom)
                    .frame(width: size, height: size)
                    .clipped()
            } else {
                placeholderIcon
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .stroke(theme.border, lineWidth: theme.isPixelSkin ? 2 : 0.8)
        )
        .task(id: url?.absoluteString ?? "") {
            guard let targetURL = url else {
                loadedImage = nil
                loadedKey = ""
                return
            }
            let key = targetURL.absoluteString
            if let hit = ArtworkImageCache.shared.cachedImage(for: targetURL, pointSize: size) {
                if loadedKey != key || loadedImage !== hit {
                    loadedKey = key
                    loadedImage = hit
                }
                return
            }
            let fetched = await ArtworkImageCache.shared.loadImage(for: targetURL, pointSize: size)
            guard !Task.isCancelled else { return }
            loadedKey = key
            loadedImage = fetched
        }
    }

    private var placeholderIcon: some View {
        Image(systemName: "music.note")
            .font(.system(size: max(14, size * 0.34), weight: .semibold))
            .foregroundColor(theme.secondaryText)
    }
}

struct PlaylistPickerSheet: View {
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    let track: Track
    @State private var newPlaylistName: String = ""

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()
                List {
                    Section("Create playlist") {
                        HStack(spacing: 10) {
                            TextField("Playlist title", text: $newPlaylistName)
                                .font(theme.font(size: 15))
                            Button("Create") {
                                library.createPlaylist(name: newPlaylistName, initialTrack: track)
                                newPlaylistName = ""
                                dismiss()
                            }
                            .disabled(newPlaylistName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            .font(theme.font(size: 14, weight: .bold))
                            .foregroundColor(theme.accent)
                        }
                    }

                    if !library.playlists.isEmpty {
                        Section("Add to existing") {
                            ForEach(library.playlists) { playlist in
                                Button {
                                    library.addTrack(track, toPlaylistId: playlist.id)
                                    dismiss()
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(playlist.name)
                                                .font(theme.font(size: 15, weight: .semibold))
                                                .foregroundColor(theme.primaryText)
                                            Text("\(playlist.tracks.count) tracks")
                                                .font(theme.font(size: 12))
                                                .foregroundColor(theme.secondaryText)
                                        }
                                        Spacer()
                                        Image(systemName: "plus.circle")
                                            .foregroundColor(theme.accent)
                                    }
                                }
                            }
                        }
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Add to playlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

struct TrackRowView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager

    let track: Track
    let contextQueue: [Track]
    @State private var showingPlaylistSheet = false

    private var isCurrent: Bool {
        player.currentTrack?.id == track.id
    }

    var body: some View {
        HStack(spacing: 12) {
            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                player.play(track: track, in: contextQueue)
            } label: {
                HStack(spacing: 12) {
                    ZStack {
                        TrackArtworkView(url: track.displayArtworkURL, size: 50)
                        if isCurrent {
                            RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : theme.artworkRadius, style: .continuous)
                                .fill(Color.black.opacity(0.45))
                                .frame(width: 50, height: 50)
                            Image(systemName: player.isPlaying ? "waveform" : "pause.fill")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(track.title)
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(isCurrent ? theme.accent : theme.primaryText)
                            .lineLimit(1)

                        HStack(spacing: 6) {
                            if library.isDownloaded(track) || track.source == .localFile {
                                Image(systemName: "arrow.down.circle.fill")
                                    .font(.system(size: 11))
                                    .foregroundColor(theme.accent)
                            }
                            Text(track.artist)
                                .font(theme.font(size: 13))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                            if track.durationSec > 0 {
                                Text("· \(track.formattedDuration)")
                                    .font(theme.font(size: 12))
                                    .foregroundColor(theme.secondaryText.opacity(0.8))
                            }
                        }
                    }
                    Spacer(minLength: 4)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if let dlProgress = library.activeDownloads[track.id] {
                ProgressView(value: dlProgress)
                    .progressViewStyle(.circular)
                    .frame(width: 24, height: 24)
            } else {
                Menu {
                    Button {
                        player.playNextInQueue(track)
                    } label: {
                        Label("Play next", systemImage: "text.line.first.and.arrowtriangle.forward")
                    }

                    Button {
                        player.appendToQueue(track)
                    } label: {
                        Label("Add to queue", systemImage: "text.badge.plus")
                    }

                    Button {
                        library.toggleLike(track)
                    } label: {
                        Label(
                            library.isLiked(track) ? "Remove from liked" : "Like track",
                            systemImage: library.isLiked(track) ? "heart.slash" : "heart"
                        )
                    }

                    Button {
                        showingPlaylistSheet = true
                    } label: {
                        Label("Add to playlist", systemImage: "music.note.list")
                    }

                    if track.source != .localFile {
                        if library.isDownloaded(track) {
                            Button(role: .destructive) {
                                library.removeOfflineDownload(track)
                            } label: {
                                Label("Remove download", systemImage: "trash")
                            }
                        } else {
                            Button {
                                Task {
                                    await library.downloadTrackForOffline(track, preferHighQuality: theme.highQualityAudio)
                                }
                            } label: {
                                Label("Download offline", systemImage: "arrow.down.circle")
                            }
                        }
                    }

                    Divider()

                    Button(role: library.isArtistBlocked(track.artist) ? .none : .destructive) {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        library.toggleBlockedArtist(track.artist)
                    } label: {
                        Label(
                            library.isArtistBlocked(track.artist) ? "Unblock \(track.artist)" : "Don't play \(track.artist)",
                            systemImage: library.isArtistBlocked(track.artist) ? "person.crop.circle.badge.checkmark" : "hand.raised.slash"
                        )
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                        .frame(width: 34, height: 34)
                        .contentShape(Rectangle())
                }
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 12)
        .contextMenu {
            Button {
                player.playNextInQueue(track)
            } label: {
                Label("Play next", systemImage: "text.line.first.and.arrowtriangle.forward")
            }
            Button {
                library.toggleLike(track)
            } label: {
                Label(library.isLiked(track) ? "Unlike" : "Like", systemImage: "heart")
            }
            Button {
                showingPlaylistSheet = true
            } label: {
                Label("Add to playlist", systemImage: "music.note.list")
            }
            Button(role: library.isArtistBlocked(track.artist) ? .none : .destructive) {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                library.toggleBlockedArtist(track.artist)
            } label: {
                Label(
                    library.isArtistBlocked(track.artist) ? "Unblock \(track.artist)" : "Don't play \(track.artist)",
                    systemImage: library.isArtistBlocked(track.artist) ? "person.crop.circle.badge.checkmark" : "hand.raised.slash"
                )
            }
        }
        .sheet(isPresented: $showingPlaylistSheet) {
            PlaylistPickerSheet(track: track)
                .environmentObject(library)
                .environmentObject(theme)
        }
    }
}
