import SwiftUI
import UIKit

struct TrackArtworkView: View {
    @EnvironmentObject private var theme: ThemeManager
    let url: URL?
    let size: CGFloat
    var cornerRadius: CGFloat? = nil

    private var isYouTubeFourByThreeThumb: Bool {
        guard let host = url?.host?.lowercased(), let path = url?.path.lowercased() else {
            return false
        }
        return host.contains("ytimg.com") && (path.contains("hqdefault") || path.contains("sddefault"))
    }

    var body: some View {
        let radius = theme.isPixelSkin ? 0 : (cornerRadius ?? theme.artworkRadius)
        let zoom: CGFloat = isYouTubeFourByThreeThumb ? 1.35 : 1.0
        ZStack {
            theme.elevatedSurface
            if let url = url {
                if url.isFileURL, let data = try? Data(contentsOf: url), let uiImg = UIImage(data: data) {
                    Image(uiImage: uiImg)
                        .resizable()
                        .scaledToFill()
                        .scaleEffect(zoom)
                        .frame(width: size, height: size)
                        .clipped()
                } else {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .scaleEffect(zoom)
                                .frame(width: size, height: size)
                                .clipped()
                        default:
                            placeholderIcon
                        }
                    }
                    .frame(width: size, height: size)
                    .clipped()
                }
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
