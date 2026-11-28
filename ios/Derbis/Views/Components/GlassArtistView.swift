import SwiftUI
import UIKit

/*
 * Liquid Glass artist page — iOS counterpart of the Android Liquid Glass artist
 * destination. Shown only for the Liquid Glass skin (call sites gate on
 * theme.usesLiquidGlass); every other skin keeps ArtistDetailSheet.
 *
 * Real data only: an ArtistCatalogPayload (top tracks, albums with release
 * years from the artist page's shelf subtitles, related artists, header
 * subscriber/listener counts) plus a Wikipedia-backed biography via
 * ArtistLoreService. Sections hide themselves when their backing data is
 * missing — nothing here is invented.
 *
 * There is deliberately no “follow artist” action: LibraryStore has no
 * followedArtist concept on iOS (likes are track-level).
 */
struct GlassArtistView: View {
    let artistName: String

    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var catalog: ArtistCatalogPayload?
    @State private var isLoading: Bool = true
    @State private var biography: ArtistCatalogBiography?
    @State private var bioExpanded: Bool = false
    @State private var activeArtistName: String = ""
    @State private var heroImage: UIImage?
    @State private var heroCanvas: MotionCanvasAsset?

    @State private var selectedAlbumID: String?
    @State private var releaseAccent: (primary: Color, secondary: Color)?

    @State private var albumPayload: AlbumCatalogPayload?
    @State private var albumLoading: Bool = false

    @State private var optionsOpen: Bool = false
    @State private var toast: GlassArtistToast?
    @State private var toastSequence: Int = 0
    @State private var scrollTarget: String?
    @State private var scrollOffset: CGFloat = 0
    @State private var kenBurnsActive: Bool = false

    private var resolvedName: String {
        activeArtistName.isEmpty ? artistName : activeArtistName
    }

    private var albums: [AlbumSummaryCard] {
        (catalog?.albums ?? []).sorted { lhs, rhs in
            let ly = Int(lhs.year) ?? Int.max
            let ry = Int(rhs.year) ?? Int.max
            if ly != ry { return ly < ry }
            return lhs.title < rhs.title
        }
    }

    private var selectedAlbum: AlbumSummaryCard? {
        guard !albums.isEmpty else { return nil }
        if let selectedAlbumID, let match = albums.first(where: { $0.id == selectedAlbumID }) {
            return match
        }
        return albums.last
    }

    private var topTracks: [Track] { catalog?.topTracks ?? [] }

    private var heroArtworkString: String? {
        (catalog?.heroArtworkURL).flatMap { $0.nilIfBlank }
            ?? topTracks.first?.preferredArtworkString.nilIfBlank
    }

    private var canvasSeedTrack: Track? {
        topTracks.first(where: { $0.source != .localFile }) ?? topTracks.first
    }

    private var displayedTracks: [Track] {
        if let fetched = albumPayload?.tracks, !fetched.isEmpty { return fetched }
        if let seeds = selectedAlbum?.seedTracks, !seeds.isEmpty { return seeds }
        return topTracks
    }

    private var tracksLoading: Bool {
        albumLoading && (albumPayload?.tracks.isEmpty ?? true)
    }

    private var totalMinutes: Int {
        max(1, displayedTracks.reduce(0) { $0 + max(0, $1.durationSec) } / 60)
    }

    private var accent: Color {
        releaseAccent?.primary ?? theme.accent
    }

    private var accentDeep: Color {
        releaseAccent?.secondary ?? Color(hex: 0x0E1B2C)
    }

    private var albumOwnsCurrent: Bool {
        guard let current = player.currentTrack else { return false }
        return displayedTracks.contains { $0.id == current.id }
    }

    private var isPlayingSelected: Bool {
        albumOwnsCurrent && player.isPlaying
    }

    private var allDisplayedLiked: Bool {
        let tracks = displayedTracks
        guard !tracks.isEmpty else { return false }
        return tracks.allSatisfy { library.isLiked($0) }
    }

    private var shareText: String {
        let query = resolvedName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? resolvedName
        return "\(resolvedName) — listening on Derbis\nhttps://music.youtube.com/search?q=\(query)"
    }

    private var datedAlbums: [(year: Int, album: AlbumSummaryCard)] {
        albums.compactMap { album in
            Int(album.year).map { (year: $0, album: album) }
        }
    }

    private var sleepMinutesRemaining: Int? {
        player.sleepTimerRemainingSec.map { Int((Double($0) / 60).rounded(.up)) }
    }

    var body: some View {
        ZStack {
            background

            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        heroSection
                            .id("top")

                        carouselSection

                        metaSection

                        actionsSection

                        progressHairline

                        tracksSection
                            .id("tracks")

                        artistpediaSection
                            .id("pedia")

                        Spacer().frame(height: 132)
                    }
                    .background(
                        GeometryReader { reader in
                            Color.clear.preference(
                                key: GlassArtistScrollOffsetKey.self,
                                value: reader.frame(in: .named("glassArtistScroll")).minY
                            )
                        }
                    )
                }
                .coordinateSpace(name: "glassArtistScroll")
                .onPreferenceChange(GlassArtistScrollOffsetKey.self) { value in
                    scrollOffset = value
                }
                .onChange(of: scrollTarget) { target in
                    guard let target else { return }
                    withAnimation(.easeInOut(duration: 0.35)) {
                        proxy.scrollTo(target, anchor: .top)
                    }
                    scrollTarget = nil
                }
            }

            topChrome

            toastHost
        }
        .preferredColorScheme(.dark)
        .sheet(isPresented: $optionsOpen) {
            optionsSheet
        }
        .task(id: resolvedName) {
            isLoading = true
            biography = nil
            bioExpanded = false
            let loaded = await InnerTubeService.shared.fetchArtistCatalog(artistName: resolvedName)
            guard !Task.isCancelled else { return }
            catalog = loaded
            isLoading = false
            if selectedAlbumID == nil {
                selectedAlbumID = albums.last?.id
            }
        }
        .task(id: resolvedName) {
            let resolved = await ArtistLoreService.shared.resolve(
                artistName: resolvedName,
                browseId: "",
                languageCode: Locale.current.language.languageCode?.identifier ?? "en"
            )
            guard !Task.isCancelled else { return }
            biography = resolved
        }
        .task(id: canvasSeedTrack?.id) {
            heroCanvas = nil
            guard theme.motionCanvasEnabled, let seed = canvasSeedTrack else { return }
            heroCanvas = await MotionCanvasService.shared.resolveMotionCanvas(for: seed, preferTall: true)
        }
        .task(id: heroArtworkString) {
            guard let raw = heroArtworkString, let url = URL(string: raw) else {
                heroImage = nil
                return
            }
            heroImage = await ArtworkImageCache.shared.loadImage(for: url, pointSize: 640)
        }
        .task(id: selectedAlbum?.id) {
            guard let album = selectedAlbum else {
                albumPayload = nil
                releaseAccent = nil
                return
            }
            releaseAccent = nil
            albumPayload = nil
            albumLoading = true
            let payload = await InnerTubeService.shared.fetchAlbumCatalog(
                albumTitle: album.title,
                artistName: album.artist.isEmpty ? resolvedName : album.artist,
                seedTracks: album.seedTracks
            )
            albumPayload = payload
            albumLoading = false

            let paletteString = payload.artworkURL ?? album.artworkURL.nilIfBlank
            if let paletteString,
               let url = URL(string: paletteString),
               let image = await ArtworkImageCache.shared.loadImage(for: url, pointSize: 180) {
                let dominant = ArtworkPaletteExtractor.dominantColors(from: image)
                let vibrant = ArtworkPaletteExtractor.vibrantAccent(from: image)
                releaseAccent = (vibrant, dominant.secondary)
            }
        }
        .onAppear { kenBurnsActive = true }
    }

    // MARK: - Background

    private var background: some View {
        ZStack {
            Color(hex: 0x020306).ignoresSafeArea()
            RadialGlow(color: accent, center: UnitPoint(x: 0.08, y: 0.58), radius: 560, opacity: 0.34)
            RadialGlow(color: accentDeep, center: UnitPoint(x: 1.05, y: 0.85), radius: 480, opacity: 0.42)
        }
        .animation(.easeInOut(duration: 0.6), value: selectedAlbumID)
    }

    // MARK: - Top chrome

    private var topChrome: some View {
        VStack {
            HStack(spacing: 12) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    // First tap scrolls back to the top; a tap at rest closes the page.
                    if scrollOffset < -80 {
                        scrollTarget = "top"
                    } else {
                        dismiss()
                    }
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .derbisGlassCircle(size: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back")

                Spacer(minLength: 0)

                Text(topTitle)
                    .font(theme.font(size: 14.5, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .multilineTextAlignment(.center)
                    .opacity(topTitleOpacity)
                    .frame(maxWidth: .infinity)

                Spacer(minLength: 0)

                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    optionsOpen = true
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .derbisGlassCircle(size: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Options")
            }
            .padding(.horizontal, 14)
            .padding(.top, 6)
        }
    }

    private var topTitle: String {
        var parts: [String] = [resolvedName]
        if let albumTitle = selectedAlbum?.title, !albumTitle.isEmpty { parts.append(albumTitle) }
        return parts.joined(separator: " · ")
    }

    private var topTitleOpacity: Double {
        let progress = (-scrollOffset - 210) / 140
        return Double(min(1, max(0, progress)))
    }

    // MARK: - Hero

    private var heroSection: some View {
        GeometryReader { proxy in
            let minY = proxy.frame(in: .named("glassArtistScroll")).minY
            let pull = max(0, minY)
            let collapse = min(1, max(0, -minY / 320))

            ZStack(alignment: .bottomLeading) {
                Group {
                    if let heroImage {
                        Image(uiImage: heroImage)
                            .resizable()
                            .scaledToFill()
                            .frame(width: proxy.size.width + 60, height: 400 + pull * 0.6)
                            .scaleEffect(kenBurnsActive && !reduceMotion ? 1.07 : 1.0)
                            .animation(
                                kenBurnsActive && !reduceMotion
                                    ? .easeInOut(duration: 16).repeatForever(autoreverses: true)
                                    : .default,
                                value: kenBurnsActive
                            )
                            .offset(y: -pull * 0.25)
                    } else {
                        LinearGradient(
                            colors: [accentDeep, Color(hex: 0x020306)],
                            startPoint: .topTrailing,
                            endPoint: .bottomLeading
                        )
                        .frame(width: proxy.size.width, height: 400 + pull)
                    }
                }
                .frame(width: proxy.size.width, height: 400 + pull)
                .clipped()

                // Canvas video (when Spotify/Apple Music has a verified loop for this artist's
                // tracks) plays over the still; the scrim below keeps the display type legible.
                if let motion = heroCanvas {
                    LoopingMotionVideoView(url: motion.playbackURL)
                        .id(motion.playbackURL.absoluteString)
                        .frame(width: proxy.size.width + 60, height: 400 + pull * 0.6)
                        .offset(y: -pull * 0.25)
                        .clipped()
                }

                // Scrim so the display type stays legible over any artwork.
                LinearGradient(
                    stops: [
                        .init(color: .black.opacity(0.42), location: 0),
                        .init(color: .clear, location: 0.28),
                        .init(color: Color(hex: 0x020306).opacity(0), location: 0.62),
                        .init(color: Color(hex: 0x020306), location: 1)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 400 + pull)
                .allowsHitTesting(false)

                VStack(alignment: .center) {
                    Spacer()
                    Text(splitDisplayName(resolvedName))
                        .font(.system(size: 44, weight: .black))
                        .tracking(0.5)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.6)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 128)
                        .opacity(Double(1 - collapse * 1.15))
                        .offset(y: collapse * 26)
                        .accessibilityAddTraits(.isHeader)
                }
                .frame(height: 400 + pull)
            }
            .frame(width: proxy.size.width, height: 400, alignment: .bottomLeading)
        }
        .frame(height: 400)
        .overlay(alignment: .bottom) {
            if isLoading {
                ProgressView()
                    .tint(.white)
                    .padding(.bottom, 150)
            }
        }
    }

    private func splitDisplayName(_ name: String) -> String {
        let parts = name.split(whereSeparator: { $0 == " " || $0 == "\t" || $0 == "\n" })
            .map(String.init)
            .filter { !$0.isEmpty }
        guard parts.count > 1 else { return name.uppercased() }
        let half = (parts.count + 1) / 2
        return (parts.prefix(half).joined(separator: " ") + "\n" + parts.dropFirst(half).joined(separator: " "))
            .uppercased()
    }

    // MARK: - Carousel

    private var carouselSection: some View {
        VStack(spacing: 4) {
            if albums.isEmpty && !isLoading {
                Text("No releases yet")
                    .font(theme.font(size: 13, weight: .semibold))
                    .foregroundColor(theme.secondaryText)
                    .padding(.vertical, 38)
            } else {
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(albums) { album in
                                carouselItem(album)
                                    .id(album.id)
                            }
                        }
                        .padding(.horizontal, 56)
                        .padding(.top, 6)
                        .padding(.bottom, 10)
                    }
                    .coordinateSpace(name: "glassArtistCarousel")
                    .frame(height: 138)
                    .onChange(of: selectedAlbumID) { id in
                        guard let id else { return }
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                            proxy.scrollTo(id, anchor: .center)
                        }
                    }
                }
                releaseDots
            }
        }
        .padding(.top, 2)
    }

    private func carouselItem(_ album: AlbumSummaryCard) -> some View {
        GeometryReader { geo in
            let midX = geo.frame(in: .named("glassArtistCarousel")).midX
            let screenMid = UIScreen.main.bounds.width / 2
            let distance = max(-4.5, min(4.5, (midX - screenMid) / 96))
            let absDistance = abs(distance)
            let scale = max(0.52, 1 - absDistance * 0.17)
            let yOffset = absDistance * absDistance * 9
            let opacity = max(0.2, 1 - absDistance * 0.3)
            let isSelected = selectedAlbum?.id == album.id

            Button {
                HapticsAndGesturesService.shared.playSelectionTick()
                selectedAlbumID = album.id
            } label: {
                ZStack {
                    TrackArtworkView(
                        url: URL(string: album.artworkURL),
                        size: 96,
                        cornerRadius: 16
                    )
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(
                            LinearGradient(
                                stops: [
                                    .init(color: .white.opacity(0.32), location: 0),
                                    .init(color: .clear, location: 0.25),
                                    .init(color: .clear, location: 0.75),
                                    .init(color: .black.opacity(0.3), location: 1)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 96, height: 96)
                        .allowsHitTesting(false)
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(isSelected ? accent.opacity(0.9) : .white.opacity(0.22),
                                lineWidth: isSelected ? 1.6 : 1)
                        .frame(width: 96, height: 96)
                        .allowsHitTesting(false)
                }
            }
            .buttonStyle(.plain)
            .scaleEffect(scale)
            .offset(y: yOffset)
            .rotation3DEffect(.degrees(-Double(distance) * 16), axis: (x: 0, y: 1, z: 0), perspective: 0.62)
            .opacity(Double(opacity))
            .zIndex(Double(100 - absDistance))
            .accessibilityLabel("\(album.title)\(isSelected ? ", selected" : "")")
        }
        .frame(width: 96, height: 110)
    }

    private var releaseDots: some View {
        let count = albums.count
        let selectedIndex = albums.firstIndex(where: { $0.id == selectedAlbum?.id }) ?? 0
        return HStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { index in
                let active = index == selectedIndex
                Capsule()
                    .fill(Color.white.opacity(active ? 0.8 : 0.18))
                    .frame(width: active ? 18 : 6, height: 6)
                    .animation(.spring(response: 0.3, dampingFraction: 0.8), value: selectedIndex)
            }
        }
        .padding(.top, 2)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Release \(selectedIndex + 1) of \(count)")
    }

    // MARK: - Meta + actions

    private var metaSection: some View {
        VStack(spacing: 6) {
            Text(selectedAlbum?.title ?? (isLoading ? "Loading…" : "No releases"))
                .font(theme.font(size: 17, weight: .bold))
                .foregroundColor(.white)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            if let album = selectedAlbum, !album.year.isEmpty {
                Text(album.year)
                    .font(.system(size: 10.5, weight: .bold))
                    .tracking(1.4)
                    .foregroundColor(accent)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 3.5)
                    .background(
                        Capsule()
                            .fill(accent.opacity(0.14))
                            .overlay(Capsule().stroke(accent.opacity(0.34), lineWidth: 1))
                    )
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.top, 10)
    }

    private var actionsSection: some View {
        HStack(spacing: 10) {
            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                scrollTarget = "tracks"
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: "music.note.list")
                        .font(.system(size: 13, weight: .bold))
                    Text("SONGS")
                        .font(.system(size: 11.5, weight: .black))
                        .tracking(1.4)
                }
                .foregroundColor(.white)
                .frame(height: 48)
                .padding(.horizontal, 22)
                .derbisGlassCapsule()
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Scroll to songs")

            Spacer(minLength: 0)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                let shuffled = displayedTracks.shuffled()
                if let first = shuffled.first {
                    player.play(track: first, in: shuffled)
                }
            } label: {
                Image(systemName: "shuffle")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 46, height: 46)
                    .derbisGlassCircle(size: 46)
            }
            .buttonStyle(.plain)
            .disabled(displayedTracks.isEmpty)
            .accessibilityLabel("Shuffle")

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                if albumOwnsCurrent {
                    player.togglePlayPause()
                } else if let first = displayedTracks.first {
                    player.play(track: first, in: displayedTracks)
                }
            } label: {
                Image(systemName: isPlayingSelected ? "pause.fill" : "play.fill")
                    .font(.system(size: 26, weight: .black))
                    .foregroundColor(Color(hex: 0x1A1000))
                    .frame(width: 72, height: 72)
                    .background(
                        Circle().fill(
                            LinearGradient(
                                colors: [accent, accent.opacity(0.72)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    )
                    .shadow(color: accent.opacity(0.55), radius: 18, y: 6)
            }
            .buttonStyle(.plain)
            .disabled(displayedTracks.isEmpty)
            .accessibilityLabel(isPlayingSelected ? "Pause" : "Play")

            Spacer(minLength: 0)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                toggleLikedAll()
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: allDisplayedLiked ? "heart.fill" : "heart")
                        .font(.system(size: 13, weight: .bold))
                    Text(allDisplayedLiked ? "SAVED" : "SAVE")
                        .font(.system(size: 11.5, weight: .black))
                        .tracking(1.4)
                }
                .foregroundColor(allDisplayedLiked ? Color(hex: 0x1A1000) : .white)
                .frame(height: 48)
                .padding(.horizontal, 22)
                .background(
                    Capsule()
                        .fill(allDisplayedLiked ? accent : Color.white.opacity(0.1))
                        .overlay(
                            Capsule().stroke(allDisplayedLiked ? accent : Color.white.opacity(0.14), lineWidth: 1)
                        )
                )
            }
            .buttonStyle(.plain)
            .disabled(displayedTracks.isEmpty)
            .accessibilityLabel(allDisplayedLiked ? "Remove all from liked" : "Save all to liked")
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
    }

    private func toggleLikedAll() {
        let tracks = displayedTracks
        guard !tracks.isEmpty else { return }
        let target = !allDisplayedLiked
        for track in tracks where library.isLiked(track) != target {
            library.toggleLike(track)
        }
        showToast(target ? "Saved to liked" : "Removed from liked")
    }

    // MARK: - Progress hairline

    @ViewBuilder
    private var progressHairline: some View {
        let duration = player.duration
        if albumOwnsCurrent && duration > 0 {
            VStack(spacing: 4) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.12)).frame(height: 2.5)
                        Capsule()
                            .fill(accent)
                            .frame(
                                width: geo.size.width * CGFloat(min(1, max(0, player.currentTime / duration))),
                                height: 2.5
                            )
                    }
                }
                .frame(height: 2.5)
                HStack {
                    Text(TimeFormatting.clockString(seconds: player.currentTime))
                    Spacer()
                    Text(TimeFormatting.clockString(seconds: duration))
                }
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .foregroundColor(theme.secondaryText)
            }
            .padding(.horizontal, 24)
            .padding(.top, 2)
            .transition(.opacity.combined(with: .move(edge: .bottom)))
        }
    }

    // MARK: - Track list

    private var tracksSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline) {
                Text("SONGS")
                    .font(.system(size: 19, weight: .black))
                    .tracking(0.6)
                    .foregroundColor(.white)
                Spacer()
                Text("\(displayedTracks.count) songs · \(totalMinutes) min")
                    .font(.system(size: 11.5, weight: .semibold))
                    .foregroundColor(theme.secondaryText)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 2)

            if tracksLoading {
                ForEach(0..<5, id: \.self) { index in
                    GlassTrackSkeleton(delay: Double(index) * 0.09)
                }
            } else if displayedTracks.isEmpty {
                Text("Tracks unavailable for this release.")
                    .font(theme.font(size: 13))
                    .foregroundColor(theme.secondaryText)
                    .padding(.horizontal, 22)
                    .padding(.vertical, 18)
            } else {
                ForEach(Array(displayedTracks.enumerated()), id: \.element.id) { index, track in
                    GlassArtistTrackRow(
                        index: index,
                        track: track,
                        accent: accent,
                        isCurrent: player.currentTrack?.id == track.id,
                        isPlaying: player.isPlaying,
                        artworkFallback: selectedAlbum.flatMap { URL(string: $0.artworkURL) }
                    ) {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        player.play(track: track, in: displayedTracks)
                    }
                }
            }
        }
    }

    // MARK: - Artistpedia (real data only)

    @ViewBuilder
    private var artistpediaSection: some View {
        let facts = artistFacts
        let timeline = timelineAlbums
        let related = catalog?.relatedArtists ?? []
        if biography != nil || !facts.isEmpty || !timeline.isEmpty || !related.isEmpty {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("ARTISTPEDIA")
                        .font(.system(size: 10.5, weight: .black))
                        .tracking(2.4)
                        .foregroundColor(accent)
                    Text("Artistpedia")
                        .font(.system(size: 30, weight: .black))
                        .foregroundColor(.white)
                        .accessibilityAddTraits(.isHeader)
                    Text("Real numbers from this artist — nothing invented.")
                        .font(theme.font(size: 12.5))
                        .foregroundColor(theme.secondaryText)
                }

                if let bio = biography, !bio.text.isEmpty {
                    Text("BIOGRAPHY")
                        .font(.system(size: 10.5, weight: .black))
                        .tracking(1.8)
                        .foregroundColor(theme.secondaryText)
                        .padding(.top, 6)
                    let needsClamp = bio.text.count > 260
                    let shown: String
                    if bioExpanded || !needsClamp {
                        shown = bio.text
                    } else {
                        shown = String(bio.text.prefix(260)).trimmingCharacters(in: .whitespacesAndNewlines) + "…"
                    }
                    Text(shown)
                        .font(theme.font(size: 13.5))
                        .lineSpacing(3)
                        .foregroundColor(.white.opacity(0.9))
                        .fixedSize(horizontal: false, vertical: true)
                    if needsClamp {
                        Button(bioExpanded ? "Show less" : "Show more") {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                bioExpanded.toggle()
                            }
                        }
                        .font(theme.font(size: 12, weight: .bold))
                        .foregroundColor(accent)
                    }
                    if let source = URL(string: bio.sourceURL) {
                        Link(destination: source) {
                            Text("· Source: \(bio.sourceLabel.isEmpty ? "Wikipedia" : bio.sourceLabel)")
                                .font(theme.font(size: 11))
                                .foregroundColor(theme.secondaryText)
                        }
                    } else {
                        Text("· \(bio.sourceLabel.isEmpty ? "Wikipedia" : bio.sourceLabel)")
                            .font(theme.font(size: 11))
                            .foregroundColor(theme.secondaryText)
                    }
                }

                if !facts.isEmpty {
                    Text("BY THE NUMBERS")
                        .font(.system(size: 10.5, weight: .black))
                        .tracking(1.8)
                        .foregroundColor(theme.secondaryText)
                        .padding(.top, 6)
                    ForEach(Array(facts.chunked(into: 2).enumerated()), id: \.offset) { _, rowFacts in
                        HStack(spacing: 10) {
                            ForEach(Array(rowFacts.enumerated()), id: \.offset) { _, fact in
                                FactCard(label: fact.label, value: fact.value)
                            }
                            if rowFacts.count == 1 {
                                FactCard(label: "", value: "").hidden()
                            }
                        }
                    }
                }

                if !timeline.isEmpty {
                    Text("TIMELINE")
                        .font(.system(size: 10.5, weight: .black))
                        .tracking(1.8)
                        .foregroundColor(theme.secondaryText)
                        .padding(.top, 6)
                    ForEach(timeline, id: \.album.id) { item in
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedAlbumID = item.album.id
                            scrollTarget = "top"
                        } label: {
                            HStack(spacing: 12) {
                                Text(String(item.year))
                                    .font(.system(size: 19, weight: .black))
                                    .foregroundColor(accent)
                                    .frame(width: 66, alignment: .leading)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.isDebut ? "DEBUT" : "ERA")
                                        .font(.system(size: 9, weight: .black))
                                        .tracking(1.4)
                                        .foregroundColor(accent)
                                        .padding(.horizontal, 7)
                                        .padding(.vertical, 2.5)
                                        .background(
                                            RoundedRectangle(cornerRadius: 6)
                                                .fill(accent.opacity(0.14))
                                        )
                                    Text(item.album.title)
                                        .font(theme.font(size: 13, weight: .semibold))
                                        .foregroundColor(.white)
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(theme.secondaryText)
                            }
                            .padding(.vertical, 7)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }

                if !related.isEmpty {
                    Text("FANS ALSO SPIN")
                        .font(.system(size: 10.5, weight: .black))
                        .tracking(1.8)
                        .foregroundColor(theme.secondaryText)
                        .padding(.top, 6)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(related, id: \.self) { name in
                                Button {
                                    HapticsAndGesturesService.shared.playSelectionTick()
                                    activeArtistName = name
                                    catalog = nil
                                    biography = nil
                                    albumPayload = nil
                                    albumLoading = false
                                    selectedAlbumID = nil
                                    releaseAccent = nil
                                    heroImage = nil
                                    scrollTarget = "top"
                                } label: {
                                    HStack(spacing: 6) {
                                        Image(systemName: "person.wave.2.fill")
                                            .font(.system(size: 11.5, weight: .semibold))
                                            .foregroundColor(accent)
                                        Text(name)
                                            .font(theme.font(size: 13, weight: .semibold))
                                            .foregroundColor(.white)
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 9)
                                    .derbisGlassCapsule()
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 28)
        }
    }

    private var artistFacts: [(label: String, value: String)] {
        var facts: [(label: String, value: String)] = []
        if let listeners = catalog?.monthlyListeners.nilIfBlank {
            facts.append(("Monthly listeners", listeners))
        }
        if let subscribers = catalog?.subscribers.nilIfBlank {
            facts.append(("Subscribers", subscribers))
        }
        if let debut = datedAlbums.map { $0.year }.min() {
            facts.append(("Debut", String(debut)))
        }
        if let latest = datedAlbums.map { $0.year }.max() {
            facts.append(("Latest release", String(latest)))
        }
        if !albums.isEmpty {
            facts.append(("Releases", String(albums.count)))
        }
        if !topTracks.isEmpty {
            facts.append(("Top tracks", String(topTracks.count)))
        }
        return facts
    }

    private var timelineAlbums: [(year: Int, album: AlbumSummaryCard, isDebut: Bool)] {
        let dated = datedAlbums
        guard dated.count >= 2 else { return [] }
        var picked: [(year: Int, album: AlbumSummaryCard, isDebut: Bool)] = [
            (dated[0].year, dated[0].album, true)
        ]
        let middle = Array(dated.dropFirst().dropLast())
        let step = middle.count <= 2 ? 1 : middle.count / 2
        var index = 0
        while index < middle.count && picked.count < 3 {
            picked.append((middle[index].year, middle[index].album, false))
            index += max(1, step)
        }
        let last = dated[dated.count - 1]
        if picked.last?.album.id != last.album.id {
            picked.append((last.year, last.album, false))
        }
        return picked
    }

    // MARK: - Options sheet

    private var optionsSheet: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(Color.white.opacity(0.28))
                .frame(width: 38, height: 5)
                .padding(.top, 8)
                .padding(.bottom, 14)

            HStack(spacing: 12) {
                TrackArtworkView(
                    url: selectedAlbum.flatMap { URL(string: $0.artworkURL) },
                    size: 54,
                    cornerRadius: 14
                )
                VStack(alignment: .leading, spacing: 3) {
                    Text(selectedAlbum?.title ?? resolvedName)
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                    Text([selectedAlbum?.year.nilIfBlank, resolvedName].compactMap { $0 }.joined(separator: " · "))
                        .font(theme.font(size: 11.5, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 10)

            if !displayedTracks.isEmpty {
                OptionsRow(
                    icon: "text.badge.plus",
                    title: "Add album to queue",
                    subtitle: "\(displayedTracks.count) songs · \(totalMinutes) min"
                ) {
                    for track in displayedTracks {
                        player.appendToQueue(track)
                    }
                    showToast("Added \(displayedTracks.count) songs to queue")
                    optionsOpen = false
                }
                OptionsDivider()
            }

            if selectedAlbum != nil, !displayedTracks.isEmpty {
                OptionsRow(
                    icon: "arrow.down.to.line",
                    title: "Download album",
                    subtitle: selectedAlbum?.title
                ) {
                    enqueueAlbumDownload()
                    optionsOpen = false
                }
                OptionsDivider()
            }

            if let seed = displayedTracks.first {
                OptionsRow(
                    icon: "infinity",
                    title: "Artist radio",
                    subtitle: resolvedName
                ) {
                    player.startSongRadio(from: seed)
                    optionsOpen = false
                }
                OptionsDivider()
            }

            OptionsRow(
                icon: "info.circle",
                title: "Open Artistpedia",
                subtitle: "Real numbers, straight from the catalog"
            ) {
                optionsOpen = false
                scrollTarget = "pedia"
            }
            OptionsDivider()

            ShareLink(item: shareText) {
                OptionsRowLabel(
                    icon: "square.and.arrow.up",
                    title: "Share profile",
                    subtitle: resolvedName
                )
            }
            .simultaneousGesture(TapGesture().onEnded { optionsOpen = false })
            OptionsDivider()

            OptionsRow(
                icon: "moon.zzz",
                title: "Sleep timer",
                subtitle: "Off · 15 · 30 · 45 · 60 min",
                trailingText: sleepMinutesRemaining.map { "\($0)m" } ?? "—",
                trailingAccent: sleepMinutesRemaining != nil
            ) {
                cycleSleepTimer()
                optionsOpen = false
            }

            Spacer(minLength: 8)
        }
        .frame(maxWidth: .infinity)
        .background(Color(hex: 0x101018).ignoresSafeArea())
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.hidden)
        .preferredColorScheme(.dark)
    }

    private func enqueueAlbumDownload() {
        let snapshot = displayedTracks.prefix(15)
        Task {
            var queued = 0
            for track in snapshot where !library.isDownloaded(track) {
                await library.downloadTrackForOffline(track, preferHighQuality: theme.highQualityAudio)
                queued += 1
            }
            showToast(queued > 0 ? "Downloading \(queued) tracks" : "Already offline")
        }
    }

    private func cycleSleepTimer() {
        let current = sleepMinutesRemaining ?? 0
        let order = [0, 15, 30, 45, 60]
        let next = order.first(where: { $0 > current }) ?? 0
        player.setSleepTimer(SleepTimerOption(rawValue: next) ?? .off)
        showToast(next == 0 ? "Sleep timer off" : "Music stops in \(next) min")
    }

    // MARK: - Toast

    @ViewBuilder
    private var toastHost: some View {
        if let toast {
            VStack {
                Spacer()
                GlassArtistToastHost(toast: toast)
                    .padding(.bottom, 116)
            }
            .allowsHitTesting(false)
            .frame(maxWidth: .infinity)
        }
    }

    private func showToast(_ text: String) {
        toastSequence += 1
        withAnimation(.spring(response: 0.32, dampingFraction: 0.86)) {
            toast = GlassArtistToast(id: toastSequence, text: text)
        }
    }
}

// MARK: - Supporting views

private struct GlassArtistScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct RadialGlow: View {
    let color: Color
    let center: UnitPoint
    let radius: CGFloat
    let opacity: Double

    var body: some View {
        GeometryReader { _ in
            Circle()
                .fill(
                    RadialGradient(
                        colors: [color.opacity(opacity), .clear],
                        center: center,
                        startRadius: 0,
                        endRadius: radius
                    )
                )
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

private struct FactCard: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased())
                .font(.system(size: 9.5, weight: .black))
                .tracking(1.2)
                .foregroundColor(Color.white.opacity(0.55))
                .lineLimit(1)
            Text(value)
                .font(.system(size: 20, weight: .black))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 13)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.white.opacity(0.05))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color.white.opacity(0.09), lineWidth: 1)
                )
        )
    }
}

private struct OptionsRow: View {
    let icon: String
    let title: String
    var subtitle: String? = nil
    var trailingText: String? = nil
    var trailingAccent: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            OptionsRowLabel(icon: icon, title: title, subtitle: subtitle, trailingText: trailingText, trailingAccent: trailingAccent)
        }
        .buttonStyle(.plain)
    }
}

private struct OptionsRowLabel: View {
    let icon: String
    let title: String
    var subtitle: String? = nil
    var trailingText: String? = nil
    var trailingAccent: Bool = false

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.white.opacity(0.08))
                    .frame(width: 38, height: 38)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 11.5, weight: .semibold))
                        .foregroundColor(Color.white.opacity(0.55))
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            if let trailingText {
                Text(trailingText)
                    .font(.system(size: 12.5, weight: .bold, design: .monospaced))
                    .foregroundColor(trailingAccent ? Color(hex: 0x38BDF8) : Color.white.opacity(0.55))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }
}

private struct OptionsDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.white.opacity(0.09))
            .frame(height: 1)
            .padding(.leading, 72)
            .padding(.trailing, 20)
    }
}

private struct GlassArtistTrackRow: View {
    let index: Int
    let track: Track
    let accent: Color
    let isCurrent: Bool
    let isPlaying: Bool
    let artworkFallback: URL?
    let onTap: () -> Void

    @EnvironmentObject private var theme: ThemeManager

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    if isCurrent {
                        Image(systemName: isPlaying ? "waveform" : "pause.fill")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(accent)
                    } else {
                        Text("\(index + 1)")
                            .font(.system(size: 13, weight: .semibold, design: .monospaced))
                            .foregroundColor(Color.white.opacity(0.55))
                    }
                }
                .frame(width: 28, alignment: .leading)

                TrackArtworkView(url: track.displayArtworkURL ?? artworkFallback, size: 46, cornerRadius: 10)

                VStack(alignment: .leading, spacing: 3) {
                    Text(track.title)
                        .font(theme.font(size: 14.5, weight: .semibold))
                        .foregroundColor(isCurrent ? accent : .white)
                        .lineLimit(1)
                    if !track.artist.isEmpty {
                        Text(track.artist)
                            .font(theme.font(size: 11.5))
                            .foregroundColor(Color.white.opacity(0.55))
                            .lineLimit(1)
                    }
                }

                Spacer(minLength: 8)

                if track.durationSec > 0 {
                    Text(track.formattedDuration)
                        .font(.system(size: 12, weight: .semibold, design: .monospaced))
                        .foregroundColor(Color.white.opacity(0.55))
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(isCurrent ? accent.opacity(0.09) : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(track.title), track \(index + 1)")
    }
}

private struct GlassTrackSkeleton: View {
    let delay: Double
    @State private var phase = false

    var body: some View {
        HStack(spacing: 12) {
            Capsule().frame(width: 22, height: 12)
            RoundedRectangle(cornerRadius: 10).frame(width: 46, height: 46)
            VStack(alignment: .leading, spacing: 6) {
                Capsule().frame(width: 150, height: 12)
                Capsule().frame(width: 94, height: 9)
            }
            Spacer(minLength: 0)
        }
        .foregroundColor(.white)
        .opacity(phase ? 0.28 : 0.1)
        .animation(
            .easeInOut(duration: 0.85).repeatForever(autoreverses: true).delay(delay),
            value: phase
        )
        .padding(.horizontal, 18)
        .padding(.vertical, 7)
        .onAppear { phase = true }
        .accessibilityHidden(true)
    }
}

private struct GlassArtistToast: Identifiable, Equatable {
    let id: Int
    let text: String
}

private struct GlassArtistToastHost: View {
    let toast: GlassArtistToast

    @State private var shown: Int = 0
    @State private var visible: Bool = true

    var body: some View {
        Text(String(toast.text.prefix(shown)))
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(.white)
            .lineLimit(1)
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(Color.black.opacity(0.92))
                    .overlay(
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .stroke(Color.white.opacity(0.18), lineWidth: 1)
                    )
            )
            .opacity(visible ? 1 : 0)
            .transition(.opacity.combined(with: .move(edge: .bottom)))
            .task(id: toast.id) {
                shown = 0
                visible = true
                let count = max(1, toast.text.count)
                for index in 1...count {
                    shown = index
                    try? await Task.sleep(nanoseconds: 14_000_000)
                }
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                withAnimation(.easeOut(duration: 0.18)) { visible = false }
            }
    }
}

private extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        guard let value = self?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else { return nil }
        return value
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0, !isEmpty else { return [] }
        var chunks: [[Element]] = []
        var index = 0
        while index < count {
            chunks.append(Array(self[index..<Swift.min(index + size, count)]))
            index += size
        }
        return chunks
    }
}
