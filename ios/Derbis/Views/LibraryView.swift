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
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared

    @State private var selectedTab: LibrarySectionTab = .liked
    @State private var showingAudioImporter = false
    @State private var showingNewPlaylistAlert = false
    @State private var showingGoogleAccountSheet = false
    @State private var showingFullStatsSheet = false
    @State private var likedSortedAlphabetically = false
    @State private var newPlaylistTitle = ""

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 16) {
                        headerRow
                        googleAccountCard
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
                    .padding(.top, 6)
                    .derbisBottomBarClearance()
                }
                .derbisAutoMinimizeTabBarOnScroll()
            }
            .navigationBarTitleDisplayMode(.inline)
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
            .sheet(isPresented: $showingGoogleAccountSheet) {
                GoogleAccountSheet()
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingFullStatsSheet) {
                ListeningStatsSheet()
                    .environmentObject(theme)
            }
        }
    }

    private var googleAccountCard: some View {
        Button {
            HapticsAndGesturesService.shared.playControlTap(style: .light)
            showingGoogleAccountSheet = true
        } label: {
            HStack(spacing: 12) {
                GoogleAccountAvatarBadge(size: 44)

                VStack(alignment: .leading, spacing: 3) {
                    Text(googleSync.activeAccount?.name ?? "Sign in with Google")
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .lineLimit(1)

                    Text(
                        googleSync.isSignedIn
                            ? (googleSync.statusMessage ?? "Liked songs, playlists & history synced · Tap to browse cloud")
                            : "Sync your Liked Songs, Playlists & History just like on Android"
                    )
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
                    .lineLimit(2)
                }

                Spacer(minLength: 4)

                if googleSync.isSyncing {
                    ProgressView().tint(theme.accent)
                } else {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .padding(14)
            .derbisCard(elevated: true)
        }
        .buttonStyle(.plain)
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
                .derbisGlassCapsule()
            }
            .buttonStyle(.plain)
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
            DerbisGlassContainer(spacing: 10) {
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
                                    .derbisGlassCapsule(prominent: active)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var likedSection: some View {
        let displayedTracks: [Track] = likedSortedAlphabetically
            ? library.likedTracks.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
            : library.likedTracks

        return Group {
            if library.likedTracks.isEmpty {
                emptyStateCard(
                    icon: "heart",
                    title: "No liked tracks yet",
                    subtitle: "Tap the heart in Now Playing or double-tap the center of album art to build your collection"
                )
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        actionHeader(title: "Liked songs", tracks: displayedTracks)
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            likedSortedAlphabetically.toggle()
                        } label: {
                            Text(likedSortedAlphabetically ? "A–Z" : "Recent")
                                .font(theme.font(size: 11.5, weight: .bold))
                                .foregroundColor(likedSortedAlphabetically ? .white : theme.primaryText)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .derbisGlassCapsule(prominent: likedSortedAlphabetically)
                        }
                        .buttonStyle(.plain)
                    }

                    if likedSortedAlphabetically && displayedTracks.count >= 8 {
                        ScrollViewReader { proxy in
                            HStack(alignment: .top, spacing: 6) {
                                LazyVStack(spacing: 2) {
                                    ForEach(displayedTracks) { track in
                                        TrackRowView(track: track, contextQueue: displayedTracks)
                                            .id(track.id)
                                    }
                                }
                                .padding(.vertical, 6)
                                .derbisCard()

                                TaurusAlphaIndexBar { letter in
                                    let target = displayedTracks.first { t in
                                        guard let ch = t.title.trimmingCharacters(in: .whitespaces).uppercased().first else {
                                            return letter == "#"
                                        }
                                        if letter == "#" { return !("A"..."Z").contains(ch) }
                                        return String(ch) >= letter
                                    }
                                    if let hit = target {
                                        withAnimation(.easeOut(duration: 0.18)) {
                                            proxy.scrollTo(hit.id, anchor: .top)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVStack(spacing: 2) {
                            ForEach(displayedTracks) { track in
                                TrackRowView(track: track, contextQueue: displayedTracks)
                            }
                        }
                        .padding(.vertical, 6)
                        .derbisCard()
                    }
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
                        LazyVStack(spacing: 2) {
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
                LazyVStack(spacing: 2) {
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

        return VStack(alignment: .leading, spacing: 16) {
            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                showingFullStatsSheet = true
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "chart.xyaxis.line")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                        .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(theme.accent))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Taurus Listening Stats & Export Import")
                            .font(theme.font(size: 14.5, weight: .bold))
                            .foregroundColor(theme.primaryText)
                        Text("Past week, month, year, all-time Top 25 & Spotify JSON import")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12.5, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
                .padding(14)
                .derbisGlassPanel(cornerRadius: 18, tint: theme.accent.opacity(0.22), interactive: false)
            }
            .buttonStyle(.plain)

            if library.history.isEmpty {
                emptyStateCard(
                    icon: "chart.bar.xaxis",
                    title: "No listening stats yet",
                    subtitle: "Start playing songs on Derbis or import your Spotify StreamingHistory JSON above"
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

struct TaurusAlphaIndexBar: View {
    @EnvironmentObject private var theme: ThemeManager
    let onSelectLetter: (String) -> Void

    private static let letters: [String] = [
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    ]

    @State private var lastLetter: String = ""

    var body: some View {
        VStack(spacing: 1.5) {
            ForEach(Self.letters, id: \.self) { ch in
                Button {
                    HapticsAndGesturesService.shared.playSelectionTick()
                    onSelectLetter(ch)
                } label: {
                    Text(ch)
                        .font(.system(size: 9.5, weight: .bold, design: .rounded))
                        .foregroundColor(theme.accent)
                        .frame(width: 18, height: 13)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 2)
        .derbisGlassCapsule()
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    let rowH: CGFloat = 14.5
                    let idx = min(Self.letters.count - 1, max(0, Int((value.location.y - 8) / rowH)))
                    let chosen = Self.letters[idx]
                    if chosen != lastLetter {
                        lastLetter = chosen
                        HapticsAndGesturesService.shared.playSelectionTick()
                        onSelectLetter(chosen)
                    }
                }
                .onEnded { _ in
                    lastLetter = ""
                }
        )
    }
}

struct PlaylistDetailView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    let playlistId: String
    @State private var curtainRevealed: Bool = false

    private var playlist: UserPlaylist? {
        library.playlists.first { $0.id == playlistId }
    }

    private var totalDurationText: String {
        guard let tracks = playlist?.tracks, !tracks.isEmpty else { return "0 min" }
        let totalSec = tracks.reduce(0) { $0 + max(0, $1.durationSec) }
        let mins = max(1, totalSec / 60)
        return mins >= 60 ? "\(mins / 60)h \(mins % 60)m" : "\(mins) min"
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let playlist = playlist {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        ZStack(alignment: .bottom) {
                            if let artURL = playlist.tracks.first?.displayArtworkURL {
                                TrackArtworkView(url: artURL, size: 360, cornerRadius: 0)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 290)
                                    .blur(radius: 42)
                                    .opacity(0.62)
                                    .clipped()
                            }

                            LinearGradient(
                                stops: [
                                    .init(color: Color.black.opacity(0.18), location: 0.0),
                                    .init(color: Color.black.opacity(0.65), location: 0.65),
                                    .init(color: Color.black, location: 1.0)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                            .frame(height: 290)

                            VStack(spacing: 12) {
                                TrackArtworkView(url: playlist.tracks.first?.displayArtworkURL, size: 164, cornerRadius: 20)
                                    .shadow(color: .black.opacity(0.55), radius: 20, x: 0, y: 10)

                                VStack(spacing: 4) {
                                    Text(playlist.name)
                                        .font(theme.font(size: 24, weight: .bold))
                                        .foregroundColor(.white)
                                        .multilineTextAlignment(.center)

                                    Text("\(playlist.tracks.count) tracks · \(totalDurationText)")
                                        .font(theme.font(size: 13, weight: .medium))
                                        .foregroundColor(.white.opacity(0.66))
                                }

                                HStack(spacing: 12) {
                                    Button {
                                        let shuffled = playlist.tracks.shuffled()
                                        if let first = shuffled.first {
                                            HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                            player.play(track: first, in: shuffled)
                                        }
                                    } label: {
                                        Label("Shuffle", systemImage: "shuffle")
                                            .font(theme.font(size: 14, weight: .bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 20)
                                            .padding(.vertical, 11)
                                            .derbisGlassCapsule()
                                    }
                                    .disabled(playlist.tracks.isEmpty)
                                    .buttonStyle(.plain)

                                    Button {
                                        if let first = playlist.tracks.first {
                                            HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                            player.play(track: first, in: playlist.tracks)
                                        }
                                    } label: {
                                        Label("Play", systemImage: "play.fill")
                                            .font(theme.font(size: 14.5, weight: .bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 28)
                                            .padding(.vertical, 11)
                                            .derbisGlassCapsule(prominent: true)
                                    }
                                    .disabled(playlist.tracks.isEmpty)
                                    .buttonStyle(.plain)

                                    Button(role: .destructive) {
                                        library.deletePlaylist(id: playlist.id)
                                        dismiss()
                                    } label: {
                                        Image(systemName: "trash")
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(.white.opacity(0.8))
                                            .derbisGlassCircle(size: 40)
                                    }
                                    .buttonStyle(.plain)
                                }
                                .padding(.top, 2)
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 8)
                        }

                        if playlist.tracks.isEmpty {
                            Text("No tracks in this playlist yet.")
                                .font(theme.font(size: 14))
                                .foregroundColor(theme.secondaryText)
                                .padding(.top, 20)
                        } else {
                            LazyVStack(spacing: 0) {
                                ForEach(Array(playlist.tracks.enumerated()), id: \.element.id) { idx, track in
                                    VStack(spacing: 0) {
                                        HStack(spacing: 0) {
                                            TrackRowView(track: track, contextQueue: playlist.tracks)
                                            Button {
                                                library.removeTrack(trackId: track.id, fromPlaylistId: playlist.id)
                                            } label: {
                                                Image(systemName: "minus.circle")
                                                    .foregroundColor(theme.secondaryText)
                                                    .padding(.trailing, 14)
                                            }
                                        }
                                        if idx + 1 < playlist.tracks.count {
                                            Divider()
                                                .background(Color.white.opacity(0.08))
                                                .padding(.leading, 68)
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                        }
                    }
                    .derbisBottomBarClearance()
                }
                .opacity(curtainRevealed ? 1.0 : 0.0)
                .onAppear {
                    withAnimation(.easeOut(duration: 0.32)) {
                        curtainRevealed = true
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}
