import SwiftUI

struct ArtistStudioDestination: Identifiable, Hashable {
    let id: String
    let artistName: String

    init(artistName: String) {
        let trimmed = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        self.artistName = trimmed
        self.id = trimmed.lowercased()
    }
}

struct AlbumStudioDestination: Identifiable, Hashable {
    let id: String
    let albumTitle: String
    let artistName: String
    let seedTracks: [Track]

    init(albumTitle: String, artistName: String, seedTracks: [Track] = []) {
        self.albumTitle = albumTitle
        self.artistName = artistName
        self.seedTracks = seedTracks
        self.id = "\(artistName.lowercased())|\(albumTitle.lowercased())"
    }
}

/// Entry point for the artist destination. The Liquid Glass skin gets the glass
/// artist page; every other skin keeps the classic ArtistDetailSheet.
struct DerbisArtistEntryView: View {
    let artistName: String

    @EnvironmentObject private var theme: ThemeManager

    var body: some View {
        if theme.usesLiquidGlass {
            GlassArtistView(artistName: artistName)
        } else {
            ArtistDetailSheet(artistName: artistName)
        }
    }
}

struct ArtistDetailSheet: View {
    let artistName: String

    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    @State private var catalog: ArtistCatalogPayload?
    @State private var isLoading: Bool = true
    @State private var activeArtistName: String = ""
    @State private var selectedAlbum: AlbumStudioDestination?

    private var resolvedName: String {
        activeArtistName.isEmpty ? artistName : activeArtistName
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 20) {
                        heroCard

                        if isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .tint(theme.accent)
                                    .padding(.vertical, 36)
                                Spacer()
                            }
                        } else if let data = catalog {
                            if !data.topTracks.isEmpty {
                                topTracksSection(data.topTracks)
                            }
                            if !data.albums.isEmpty {
                                releasesSection(data.albums)
                            }
                            if !data.relatedArtists.isEmpty {
                                relatedArtistsSection(data.relatedArtists)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 36)
                }
            }
            .navigationTitle(resolvedName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .sheet(item: $selectedAlbum) { albumDest in
                AlbumDetailSheet(
                    albumTitle: albumDest.albumTitle,
                    artistName: albumDest.artistName,
                    seedTracks: albumDest.seedTracks
                )
                .environmentObject(player)
                .environmentObject(library)
                .environmentObject(theme)
            }
            .task(id: resolvedName) {
                isLoading = true
                catalog = await InnerTubeService.shared.fetchArtistCatalog(artistName: resolvedName)
                isLoading = false
            }
        }
    }

    private var heroCard: some View {
        let heroURL = catalog?.heroArtworkURL.flatMap(URL.init(string:))
        let topList = catalog?.topTracks ?? []

        return VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                TrackArtworkView(
                    url: heroURL,
                    size: 88,
                    cornerRadius: theme.isPixelSkin ? 0 : (theme.isTorqueSkin ? 8 : 22)
                )

                VStack(alignment: .leading, spacing: 5) {
                    Text("ARTIST STUDIO")
                        .font(theme.font(size: 10, weight: .bold))
                        .tracking(1.2)
                        .foregroundColor(theme.accent)

                    Text(resolvedName)
                        .font(theme.font(size: 22, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .lineLimit(2)

                    Text(topList.isEmpty ? "Curating discography & radio…" : "\(topList.count) top cuts · Endless radio ready")
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)
                }
                Spacer(minLength: 0)
            }

            HStack(spacing: 10) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                    if let first = topList.first {
                        player.play(track: first, in: topList)
                    }
                } label: {
                    Label("Play Top", systemImage: "play.fill")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(theme.onAccentText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .derbisGlassCapsule(prominent: true)
                }
                .buttonStyle(.plain)
                .disabled(topList.isEmpty)

                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                    if let seed = topList.randomElement() ?? topList.first {
                        player.startSongRadio(from: seed)
                    }
                } label: {
                    Label("Artist Radio", systemImage: "infinity")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .derbisGlassCapsule()
                }
                .buttonStyle(.plain)
                .disabled(topList.isEmpty)
            }
        }
        .padding(16)
        .derbisCard(elevated: true)
    }

    private func topTracksSection(_ tracks: [Track]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(theme.isTorqueSkin ? "TOP TRACKS" : "Top tracks")
                    .font(theme.font(size: 17, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Button {
                    if let seed = tracks.randomElement() {
                        var shuffled = tracks.shuffled()
                        shuffled.removeAll { $0.id == seed.id }
                        shuffled.insert(seed, at: 0)
                        player.play(track: seed, in: shuffled)
                    }
                } label: {
                    Label("Shuffle", systemImage: "shuffle")
                        .font(theme.font(size: 12.5, weight: .semibold))
                        .foregroundColor(theme.accent)
                }
            }

            VStack(spacing: 2) {
                ForEach(tracks.prefix(10)) { track in
                    TrackRowView(track: track, contextQueue: tracks)
                }
            }
            .padding(.vertical, 6)
            .derbisCard()
        }
    }

    private func releasesSection(_ albums: [AlbumSummaryCard]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(theme.isTorqueSkin ? "RELEASES & SINGLES" : "Releases & singles")
                .font(theme.font(size: 17, weight: .bold))
                .foregroundColor(theme.primaryText)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(albums) { album in
                        Button {
                            HapticsAndGesturesService.shared.playControlTap(style: .light)
                            selectedAlbum = AlbumStudioDestination(
                                albumTitle: album.title,
                                artistName: album.artist,
                                seedTracks: album.seedTracks
                            )
                        } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                TrackArtworkView(
                                    url: URL(string: album.artworkURL),
                                    size: 132,
                                    cornerRadius: theme.artworkRadius
                                )
                                Text(album.title)
                                    .font(theme.font(size: 13, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                    .lineLimit(1)
                                Text(album.year.isEmpty ? "Album · \(album.artist)" : "\(album.year) · \(album.artist)")
                                    .font(theme.font(size: 11.5))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            .frame(width: 132, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func relatedArtistsSection(_ names: [String]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(theme.isTorqueSkin ? "SIMILAR ARTISTS" : "Fans also spin")
                .font(theme.font(size: 17, weight: .bold))
                .foregroundColor(theme.primaryText)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(names, id: \.self) { name in
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            activeArtistName = name
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "person.wave.2.fill")
                                    .font(.system(size: 11.5, weight: .semibold))
                                    .foregroundColor(theme.accent)
                                Text(name)
                                    .font(theme.font(size: 13, weight: .semibold))
                                    .foregroundColor(theme.primaryText)
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .derbisGlassCapsule()
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

struct AlbumDetailSheet: View {
    let albumTitle: String
    let artistName: String
    var seedTracks: [Track] = []

    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    @State private var payload: AlbumCatalogPayload?
    @State private var isLoading: Bool = true
    @State private var showingArtistSheet: Bool = false

    var body: some View {
        let tracks = payload?.tracks ?? seedTracks
        let totalMinutes = max(1, tracks.reduce(0) { $0 + max(0, $1.durationSec) } / 60)

        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(spacing: 14) {
                            TrackArtworkView(
                                url: (payload?.artworkURL).flatMap(URL.init(string:)) ?? seedTracks.first?.displayArtworkURL,
                                size: 196,
                                cornerRadius: theme.playerArtworkRadius
                            )

                            VStack(spacing: 4) {
                                Text(payload?.title ?? albumTitle)
                                    .font(theme.font(size: 20, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                    .multilineTextAlignment(.center)

                                Button {
                                    showingArtistSheet = true
                                } label: {
                                    Text(payload?.artistName ?? artistName)
                                        .font(theme.font(size: 14, weight: .semibold))
                                        .foregroundColor(theme.accent)
                                }
                                .buttonStyle(.plain)

                                if !tracks.isEmpty {
                                    Text("\(tracks.count) tracks · \(totalMinutes) min")
                                        .font(theme.font(size: 12))
                                        .foregroundColor(theme.secondaryText)
                                }
                            }

                            HStack(spacing: 10) {
                                Button {
                                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                    if let first = tracks.first {
                                        player.play(track: first, in: tracks)
                                    }
                                } label: {
                                    Label("Play", systemImage: "play.fill")
                                        .font(theme.font(size: 13.5, weight: .bold))
                                        .foregroundColor(theme.onAccentText)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .derbisGlassCapsule(prominent: true)
                                }
                                .buttonStyle(.plain)
                                .disabled(tracks.isEmpty)

                                Button {
                                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                    if let first = tracks.first {
                                        player.startSongRadio(from: first)
                                    }
                                } label: {
                                    Label("Radio", systemImage: "infinity")
                                        .font(theme.font(size: 13.5, weight: .bold))
                                        .foregroundColor(theme.primaryText)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .derbisGlassCapsule()
                                }
                                .buttonStyle(.plain)
                                .disabled(tracks.isEmpty)

                                Button {
                                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                                    Task {
                                        for item in tracks.prefix(15) where !library.isDownloaded(item) {
                                            await library.downloadTrackForOffline(item, preferHighQuality: theme.highQualityAudio)
                                        }
                                    }
                                } label: {
                                    Image(systemName: "arrow.down.to.line")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(theme.primaryText)
                                        .derbisGlassCircle(size: 40)
                                }
                                .buttonStyle(.plain)
                                .disabled(tracks.isEmpty)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(18)
                        .derbisCard(elevated: true)

                        if isLoading && tracks.isEmpty {
                            HStack {
                                Spacer()
                                ProgressView().tint(theme.accent).padding(.vertical, 28)
                                Spacer()
                            }
                        } else if !tracks.isEmpty {
                            VStack(spacing: 2) {
                                ForEach(Array(tracks.enumerated()), id: \.element.id) { idx, item in
                                    HStack(spacing: 8) {
                                        Text(String(format: "%02d", idx + 1))
                                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                                            .foregroundColor(player.currentTrack?.id == item.id ? theme.accent : theme.secondaryText)
                                            .frame(width: 24, alignment: .trailing)

                                        TrackRowView(track: item, contextQueue: tracks)
                                    }
                                    .padding(.leading, 8)
                                }
                            }
                            .padding(.vertical, 6)
                            .derbisCard()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 34)
                }
            }
            .navigationTitle("Release")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .sheet(isPresented: $showingArtistSheet) {
                DerbisArtistEntryView(artistName: payload?.artistName ?? artistName)
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .task {
                isLoading = true
                payload = await InnerTubeService.shared.fetchAlbumCatalog(
                    albumTitle: albumTitle,
                    artistName: artistName,
                    seedTracks: seedTracks
                )
                isLoading = false
            }
        }
    }
}
