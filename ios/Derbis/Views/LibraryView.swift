import SwiftUI
import UniformTypeIdentifiers

enum LibrarySectionTab: String, CaseIterable, Identifiable {
    case liked = "Liked"
    case offline = "Offline"
    case local = "Local Files"
    case playlists = "Playlists"
    case stats = "Stats"
    case server = "Server"

    var id: String { rawValue }
}

struct LibraryView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var subsonic: SubsonicService

    @State private var selectedTab: LibrarySectionTab = .liked
    @State private var showingAudioImporter = false
    @State private var showingNewPlaylistAlert = false
    @State private var newPlaylistTitle = ""

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 20) {
                        headerRow
                        insightsStrip
                        sectionTabs

                        switch selectedTab {
                        case .liked:
                            likedSection
                        case .offline:
                            offlineSection
                        case .local:
                            localFilesSection
                        case .playlists:
                            playlistsSection
                        case .stats:
                            listeningStatsSection
                        case .server:
                            serverSection
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 120)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .fileImporter(
                isPresented: $showingAudioImporter,
                allowedContentTypes: [.audio],
                allowsMultipleSelection: true
            ) { result in
                if case .success(let urls) = result {
                    Task {
                        await library.importLocalAudioFiles(urls: urls)
                    }
                }
            }
            .alert("New playlist", isPresented: $showingNewPlaylistAlert) {
                TextField("Playlist name", text: $newPlaylistTitle)
                Button("Create") {
                    library.createPlaylist(name: newPlaylistTitle)
                    newPlaylistTitle = ""
                }
                Button("Cancel", role: .cancel) {
                    newPlaylistTitle = ""
                }
            }
        }
    }

    private var headerRow: some View {
        HStack {
            Text("Library")
                .font(theme.font(size: 26, weight: .bold))
                .foregroundColor(theme.primaryText)
            Spacer()
            Button {
                showingAudioImporter = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "folder.badge.plus")
                        .font(.system(size: 13, weight: .semibold))
                    Text("Import audio")
                        .font(theme.font(size: 13, weight: .semibold))
                }
                .foregroundColor(theme.primaryText)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .derbisCard(elevated: true)
            }
        }
    }

    private var insightsStrip: some View {
        Button {
            HapticsAndGesturesService.shared.playSelectionTick()
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedTab = .stats
            }
        } label: {
            HStack(spacing: 10) {
                statTile(value: "\(library.totalPlaysCount)", label: "Plays")
                statTile(value: "\(library.totalMinutesListened)m", label: "Listened")
                statTile(value: "\(library.likedTracks.count)", label: "Liked")
                statTile(value: "\(library.offlineTracks.count + library.localTracks.count)", label: "On device")
            }
        }
        .buttonStyle(.plain)
    }

    private func statTile(value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(theme.font(size: 18, weight: .bold))
                .foregroundColor(theme.primaryText)
                .lineLimit(1)
            Text(label)
                .font(theme.font(size: 11, weight: .medium))
                .foregroundColor(theme.secondaryText)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .derbisCard()
    }

    private var sectionTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(LibrarySectionTab.allCases) { tab in
                    if tab != .server || subsonic.isConnected {
                        let active = selectedTab == tab
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedTab = tab
                        } label: {
                            Text(tab.rawValue)
                                .font(theme.font(size: 13, weight: active ? .bold : .medium))
                                .foregroundColor(active ? .white : theme.primaryText)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(
                                    Group {
                                        if theme.isPixelSkin {
                                            Rectangle()
                                                .fill(active ? theme.accent : theme.surface)
                                                .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                                        } else {
                                            Capsule()
                                                .fill(active ? theme.accent : theme.surface)
                                                .overlay(Capsule().stroke(theme.border, lineWidth: 1))
                                        }
                                    }
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var likedSection: some View {
        Group {
            if library.likedTracks.isEmpty {
                emptyStateCard(
                    icon: "heart",
                    title: "No liked tracks yet",
                    subtitle: "Tap the heart in Now Playing or double-tap the center of album art to build your collection"
                )
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    actionHeader(title: "Liked songs", tracks: library.likedTracks)
                    VStack(spacing: 2) {
                        ForEach(library.likedTracks) { track in
                            TrackRowView(track: track, contextQueue: library.likedTracks)
                        }
                    }
                    .padding(.vertical, 6)
                    .derbisCard()
                }
            }
        }
    }

    private var offlineSection: some View {
        Group {
            if library.offlineTracks.isEmpty && library.activeDownloads.isEmpty {
                emptyStateCard(
                    icon: "arrow.down.circle",
                    title: "No offline downloads",
                    subtitle: "Open any track menu and select Download offline for zero-data playback"
                )
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    if !library.offlineTracks.isEmpty {
                        actionHeader(title: "Downloaded (\(library.offlineStorageSizeString()))", tracks: library.offlineTracks)
                        VStack(spacing: 2) {
                            ForEach(library.offlineTracks) { track in
                                TrackRowView(track: track, contextQueue: library.offlineTracks)
                            }
                        }
                        .padding(.vertical, 6)
                        .derbisCard()
                    }
                }
            }
        }
    }

    private var localFilesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if library.localTracks.isEmpty {
                emptyStateCard(
                    icon: "waveform.badge.plus",
                    title: "Play local FLAC, ALAC, M4A & MP3",
                    subtitle: "Tap Import audio above to add lossless or local audio files from the iOS Files app"
                )
            } else {
                actionHeader(title: "Imported files", tracks: library.localTracks)
                VStack(spacing: 2) {
                    ForEach(library.localTracks) { track in
                        HStack(spacing: 0) {
                            TrackRowView(track: track, contextQueue: library.localTracks)
                            Button(role: .destructive) {
                                library.removeLocalTrack(track)
                            } label: {
                                Image(systemName: "trash")
                                    .font(.system(size: 13))
                                    .foregroundColor(theme.secondaryText)
                                    .padding(.trailing, 12)
                            }
                        }
                    }
                }
                .padding(.vertical, 6)
                .derbisCard()
            }
        }
    }

    private var playlistsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Your playlists")
                    .font(theme.font(size: 18, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Button {
                    showingNewPlaylistAlert = true
                } label: {
                    Label("New playlist", systemImage: "plus")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }

            if library.playlists.isEmpty {
                emptyStateCard(
                    icon: "music.note.list",
                    title: "Create your first playlist",
                    subtitle: "Organize tracks from Derbis streaming, local files, and your server in one place"
                )
            } else {
                ForEach(library.playlists) { playlist in
                    NavigationLink {
                        PlaylistDetailView(playlistId: playlist.id)
                    } label: {
                        HStack(spacing: 14) {
                            TrackArtworkView(url: playlist.tracks.first?.displayArtworkURL, size: 54)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(playlist.name)
                                    .font(theme.font(size: 16, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text("\(playlist.tracks.count) tracks")
                                    .font(theme.font(size: 13))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(theme.secondaryText)
                        }
                        .padding(12)
                        .derbisCard()
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var listeningStatsSection: some View {
        let topArtists = library.topArtistsStats
        let topTracks = library.topTracksStats

        return Group {
            if library.history.isEmpty {
                emptyStateCard(
                    icon: "chart.bar.xaxis",
                    title: "No listening stats yet",
                    subtitle: "Start playing songs on Derbis to build your on-device top artists and track insights"
                )
            } else {
                VStack(alignment: .leading, spacing: 18) {
                    if !topArtists.isEmpty {
                        Text("Top artists")
                            .font(theme.font(size: 18, weight: .bold))
                            .foregroundColor(theme.primaryText)

                        VStack(spacing: 10) {
                            ForEach(Array(topArtists.enumerated()), id: \.offset) { idx, entry in
                                HStack(spacing: 12) {
                                    Text("#\(idx + 1)")
                                        .font(theme.font(size: 14, weight: .bold))
                                        .foregroundColor(idx == 0 ? theme.accent : theme.secondaryText)
                                        .frame(width: 28, alignment: .leading)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(entry.artist)
                                            .font(theme.font(size: 15, weight: .semibold))
                                            .foregroundColor(theme.primaryText)
                                            .lineLimit(1)
                                        Text("\(entry.plays) plays · ~\(entry.minutes) min")
                                            .font(theme.font(size: 12))
                                            .foregroundColor(theme.secondaryText)
                                    }

                                    Spacer()
                                }
                            }
                        }
                        .padding(14)
                        .derbisCard()
                    }

                    if !topTracks.isEmpty {
                        Text("Most played tracks")
                            .font(theme.font(size: 18, weight: .bold))
                            .foregroundColor(theme.primaryText)

                        let queueTracks = topTracks.map(\.track)
                        VStack(spacing: 2) {
                            ForEach(Array(topTracks.enumerated()), id: \.element.track.id) { idx, item in
                                HStack(spacing: 0) {
                                    TrackRowView(track: item.track, contextQueue: queueTracks)
                                    Text("\(item.plays)×")
                                        .font(theme.font(size: 12, weight: .bold))
                                        .foregroundColor(idx == 0 ? theme.accent : theme.secondaryText)
                                        .padding(.trailing, 14)
                                }
                            }
                        }
                        .padding(.vertical, 6)
                        .derbisCard()
                    }
                }
            }
        }
    }

    private var serverSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(subsonic.serverStatus)
                    .font(theme.font(size: 13, weight: .semibold))
                    .foregroundColor(theme.secondaryText)
                Spacer()
                Button("Refresh") {
                    Task { await subsonic.refreshLibrary() }
                }
                .font(theme.font(size: 13, weight: .bold))
                .foregroundColor(theme.accent)
            }

            if !subsonic.recentTracks.isEmpty {
                actionHeader(title: "Server library", tracks: subsonic.recentTracks)
                VStack(spacing: 2) {
                    ForEach(subsonic.recentTracks) { track in
                        TrackRowView(track: track, contextQueue: subsonic.recentTracks)
                    }
                }
                .padding(.vertical, 6)
                .derbisCard()
            } else {
                emptyStateCard(
                    icon: "server.rack",
                    title: "No server songs loaded",
                    subtitle: "Tap Refresh or search your Navidrome / Subsonic catalog from the Search tab"
                )
            }
        }
    }

    private func actionHeader(title: String, tracks: [Track]) -> some View {
        HStack {
            Text(title)
                .font(theme.font(size: 18, weight: .bold))
                .foregroundColor(theme.primaryText)
            Spacer()
            Button {
                if let first = tracks.first {
                    player.play(track: first, in: tracks)
                }
            } label: {
                Label("Play", systemImage: "play.fill")
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.accent)
            }
            .padding(.trailing, 8)

            Button {
                let shuffled = tracks.shuffled()
                if let first = shuffled.first {
                    player.play(track: first, in: shuffled)
                }
            } label: {
                Label("Shuffle", systemImage: "shuffle")
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.primaryText)
            }
        }
    }

    private func emptyStateCard(icon: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(theme.accent)
            Text(title)
                .font(theme.font(size: 16, weight: .bold))
                .foregroundColor(theme.primaryText)
            Text(subtitle)
                .font(theme.font(size: 13))
                .foregroundColor(theme.secondaryText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(28)
        .derbisCard()
    }
}

struct PlaylistDetailView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    let playlistId: String

    private var playlist: UserPlaylist? {
        library.playlists.first { $0.id == playlistId }
    }

    var body: some View {
        ZStack {
            PixelGraphPaperBackground()

            if let playlist = playlist {
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        HStack(spacing: 16) {
                            TrackArtworkView(url: playlist.tracks.first?.displayArtworkURL, size: 92)
                            VStack(alignment: .leading, spacing: 6) {
                                Text(playlist.name)
                                    .font(theme.font(size: 22, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text("\(playlist.tracks.count) tracks")
                                    .font(theme.font(size: 13))
                                    .foregroundColor(theme.secondaryText)

                                HStack(spacing: 10) {
                                    Button {
                                        if let first = playlist.tracks.first {
                                            player.play(track: first, in: playlist.tracks)
                                        }
                                    } label: {
                                        Label("Play", systemImage: "play.fill")
                                            .font(theme.font(size: 13, weight: .bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 8)
                                            .background(Capsule().fill(theme.accent))
                                    }
                                    .disabled(playlist.tracks.isEmpty)

                                    Button(role: .destructive) {
                                        library.deletePlaylist(id: playlist.id)
                                        dismiss()
                                    } label: {
                                        Image(systemName: "trash")
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(theme.secondaryText)
                                            .padding(8)
                                            .derbisCard()
                                    }
                                }
                            }
                            Spacer()
                        }

                        if playlist.tracks.isEmpty {
                            Text("No tracks in this playlist yet.")
                                .font(theme.font(size: 14))
                                .foregroundColor(theme.secondaryText)
                                .padding(.top, 20)
                        } else {
                            VStack(spacing: 2) {
                                ForEach(playlist.tracks) { track in
                                    HStack(spacing: 0) {
                                        TrackRowView(track: track, contextQueue: playlist.tracks)
                                        Button {
                                            library.removeTrack(trackId: track.id, fromPlaylistId: playlist.id)
                                        } label: {
                                            Image(systemName: "minus.circle")
                                                .foregroundColor(theme.secondaryText)
                                                .padding(.trailing, 12)
                                        }
                                    }
                                }
                            }
                            .padding(.vertical, 6)
                            .derbisCard()
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 120)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}
