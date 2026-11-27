import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared
    @ObservedObject private var jam = JamService.shared

    @State private var selectedMood: String = "All"
    @State private var shelves: [HomeShelf] = []
    @State private var isLoading: Bool = false
    @State private var showingGoogleAccountSheet: Bool = false
    @State private var showingJamSheet: Bool = false
    @State private var selectedArtist: ArtistStudioDestination?

    private let moods = ["All", "Chill", "Late Night", "Focus", "Energy", "OPM"]

    private var studioGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 5 { return "After hours" }
        if hour < 12 { return "Morning studio" }
        if hour < 18 { return "Afternoon rotation" }
        return "Evening session"
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        headerBar
                        moodFilterStrip
                        soundtrackHeroCard

                        if !speedDialTracks.isEmpty {
                            speedDialSection
                        }

                        if !library.orbitTracks.isEmpty {
                            orbitGridSection
                        }

                        if isLoading && displayShelves.isEmpty {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .tint(theme.accent)
                                    .padding(.vertical, 28)
                                Spacer()
                            }
                        } else if displayShelves.isEmpty {
                            offlineFallbackCard
                        } else {
                            ForEach(Array(displayShelves.enumerated()), id: \.element.id) { idx, shelf in
                                if idx == 0 {
                                    carouselShelfSection(shelf)
                                } else {
                                    shelfSection(shelf)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 6)
                    .derbisBottomBarClearance()
                }
                .derbisAutoMinimizeTabBarOnScroll()
                .refreshable {
                    await reloadFeed(force: true)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $showingGoogleAccountSheet) {
                GoogleAccountSheet()
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .sheet(item: $selectedArtist) { dest in
                ArtistDetailSheet(artistName: dest.artistName)
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .onReceive(NotificationCenter.default.publisher(for: .derbisGoogleAccountDidChange)) { _ in
                Task {
                    await reloadFeed(force: true)
                }
            }
            .sheet(isPresented: $showingJamSheet) {
                JamSheet()
                    .environmentObject(player)
                    .environmentObject(theme)
            }
            .task {
                if shelves.isEmpty {
                    shelves = library.cachedHomeShelves
                    await reloadFeed(force: shelves.isEmpty)
                }
            }
        }
    }

    private var displayShelves: [HomeShelf] {
        shelves.isEmpty ? library.cachedHomeShelves : shelves
    }

    private var speedDialTracks: [Track] {
        var seen = Set<String>()
        var combined: [Track] = []
        for t in library.orbitTracks + library.likedTracks + displayShelves.prefix(2).flatMap(\.tracks) {
            if seen.insert(t.id).inserted {
                combined.append(t)
            }
            if combined.count >= 6 { break }
        }
        return combined
    }

    private var offlineFallbackCard: some View {
        let onDeviceTracks = library.offlineTracks + library.localTracks
        return VStack(spacing: 12) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(theme.accent)

            Text("Couldn't reach discovery feed")
                .font(theme.font(size: 16, weight: .bold))
                .foregroundColor(theme.primaryText)

            Text("Check your connection to load fresh shelves, or play your saved offline tracks immediately.")
                .font(theme.font(size: 13))
                .foregroundColor(theme.secondaryText)
                .multilineTextAlignment(.center)

            HStack(spacing: 10) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    Task { await reloadFeed(force: true) }
                } label: {
                    Label("Retry Feed", systemImage: "arrow.clockwise")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(theme.onAccentText)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .derbisGlassCapsule(prominent: true)
                }
                .buttonStyle(.plain)

                if !onDeviceTracks.isEmpty {
                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        if let first = onDeviceTracks.first {
                            player.play(track: first, in: onDeviceTracks)
                        }
                    } label: {
                        Label("Play Offline (\(onDeviceTracks.count))", systemImage: "play.fill")
                            .font(theme.font(size: 13, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 9)
                            .derbisGlassCapsule()
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 2)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .derbisCard()
    }

    private var headerBar: some View {
        HStack(spacing: 9) {
            DerbisDiamondMark(color: theme.accent, size: 24)

            VStack(alignment: .leading, spacing: 1) {
                Text("Derbis")
                    .font(theme.font(size: 22, weight: .bold))
                    .foregroundColor(theme.primaryText)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                Text(
                    googleSync.activeAccount.map { "\($0.name) · \(theme.skin.title)" }
                        ?? "\(studioGreeting) · \(theme.skin.title)"
                )
                .font(theme.font(size: 11.5, weight: .medium))
                .foregroundColor(theme.secondaryText)
                .lineLimit(1)
            }
            .layoutPriority(1)

            Spacer(minLength: 6)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                showingJamSheet = true
            } label: {
                HStack(spacing: 5) {
                    if jam.isActive {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 6, height: 6)
                    }
                    Image(systemName: "person.2.wave.2.fill")
                        .font(.system(size: 11.5, weight: .semibold))
                    Text(jam.isActive ? "\(max(1, jam.participantCount))" : "Jam")
                        .font(theme.font(size: 12.5, weight: .semibold))
                        .lineLimit(1)
                }
                .foregroundColor(jam.isActive ? theme.onAccentText : theme.primaryText)
                .padding(.horizontal, 10)
                .padding(.vertical, 7)
                .derbisGlassCapsule(prominent: jam.isActive)
            }
            .buttonStyle(.plain)

            Menu {
                Section("Skin") {
                    ForEach(DerbisSkin.allCases) { skin in
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                theme.skin = skin
                            }
                        } label: {
                            HStack {
                                Text(skin.title)
                                if theme.skin == skin {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                }
                Section("Player Deck") {
                    ForEach(DerbisPlayerDeck.allCases) { deck in
                        Button {
                            theme.playerDeck = deck
                        } label: {
                            Label(deck.title, systemImage: theme.playerDeck == deck ? "checkmark" : deck.icon)
                        }
                    }
                }
                Section("Typography") {
                    ForEach(DerbisFontChoice.allCases) { choice in
                        Button {
                            theme.fontChoice = choice
                        } label: {
                            HStack {
                                Text(choice.title)
                                if theme.fontChoice == choice {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                }
            } label: {
                Image(systemName: "swatchpalette.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(theme.primaryText)
                    .derbisGlassCircle(size: 34)
                    .accessibilityLabel("Skin and player deck")
            }

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                showingGoogleAccountSheet = true
            } label: {
                GoogleAccountAvatarBadge(size: 34)
            }
            .buttonStyle(.plain)
        }
    }

    private var moodFilterStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            DerbisGlassContainer(spacing: 10) {
                HStack(spacing: 8) {
                    ForEach(moods, id: \.self) { mood in
                        let active = selectedMood == mood
                        Button {
                            guard selectedMood != mood else { return }
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedMood = mood
                            Task { await reloadFeed(force: true) }
                        } label: {
                            Text(theme.isTorqueSkin ? mood.uppercased() : mood)
                                .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                                .foregroundColor(active ? theme.onAccentText : theme.primaryText)
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

    private var soundtrackHeroCard: some View {
        let pool = soundtrackQueuePool()
        return Button {
            HapticsAndGesturesService.shared.playControlTap(style: .medium)
            if let seed = pool.randomElement() {
                player.startSongRadio(from: seed)
            } else {
                Task {
                    await reloadFeed(force: true)
                    let refreshed = soundtrackQueuePool()
                    if let seed = refreshed.randomElement() {
                        player.startSongRadio(from: seed)
                    }
                }
            }
        } label: {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 5) {
                        Image(systemName: "infinity")
                            .font(.system(size: 10.5, weight: .black))
                        Text("ENDLESS LISTENING")
                            .font(theme.font(size: 10, weight: .bold))
                            .tracking(1.1)
                    }
                    .foregroundColor(theme.accent)

                    Text("Personal Studio Radio")
                        .font(theme.font(size: 20, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .lineLimit(1)

                    Text("Non-stop song radio seeded from your rotation, liked cuts & \(selectedMood.lowercased()) flow")
                        .font(theme.font(size: 12.5))
                        .foregroundColor(theme.secondaryText)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 11, weight: .bold))
                        Text("Start Endless Mix")
                            .font(theme.font(size: 12.5, weight: .bold))
                    }
                    .foregroundColor(theme.isPixelSkin ? .white : theme.onAccentText)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        Group {
                            if theme.isPixelSkin {
                                Rectangle()
                                    .fill(theme.accent)
                                    .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                            } else if theme.isTorqueSkin {
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(theme.accent)
                            } else {
                                Capsule()
                                    .fill(
                                        LinearGradient(
                                            colors: [theme.accent, theme.secondaryAccent],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                            }
                        }
                    )
                    .padding(.top, 4)
                }

                Spacer(minLength: 6)

                if let coverTrack = pool.first {
                    Group {
                        if theme.isPixelSkin || theme.isTorqueSkin {
                            TrackArtworkView(
                                url: coverTrack.displayArtworkURL,
                                size: 88,
                                cornerRadius: theme.artworkRadius
                            )
                        } else {
                            TrackArtworkView(url: coverTrack.displayArtworkURL, size: 88, cornerRadius: 0)
                                .clipShape(
                                    AsymmetricLeafShape(
                                        topLeading: 26,
                                        bottomLeading: 10,
                                        bottomTrailing: 26,
                                        topTrailing: 10
                                    )
                                )
                                .overlay(
                                    AsymmetricLeafShape(
                                        topLeading: 26,
                                        bottomLeading: 10,
                                        bottomTrailing: 26,
                                        topTrailing: 10
                                    )
                                    .stroke(theme.accent.opacity(0.45), lineWidth: 1.2)
                                )
                        }
                    }
                } else {
                    ZStack {
                        RoundedRectangle(cornerRadius: theme.artworkRadius, style: .continuous)
                            .fill(theme.accent.opacity(0.14))
                            .overlay(
                                RoundedRectangle(cornerRadius: theme.artworkRadius, style: .continuous)
                                    .stroke(theme.accent.opacity(0.32), lineWidth: 1)
                            )
                        DerbisDiamondMark(color: theme.accent, size: 34)
                    }
                    .frame(width: 88, height: 88)
                }
            }
            .padding(15)
            .derbisCard(elevated: true)
        }
        .buttonStyle(.plain)
    }

    private var speedDialSection: some View {
        let items = speedDialTracks
        let columns = [
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10)
        ]

        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(theme.isTorqueSkin ? "SPEED DIAL" : "Speed dial")
                    .font(theme.font(size: 17, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text("Quick spin")
                    .font(theme.font(size: 11.5, weight: .semibold))
                    .foregroundColor(theme.secondaryText)
            }

            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(items) { item in
                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        player.play(track: item, in: items)
                    } label: {
                        HStack(spacing: 10) {
                            TrackArtworkView(
                                url: item.displayArtworkURL,
                                size: 46,
                                cornerRadius: theme.artworkRadius
                            )
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .font(theme.font(size: 12.5, weight: .bold))
                                    .foregroundColor(player.currentTrack?.id == item.id ? theme.accent : theme.primaryText)
                                    .lineLimit(1)
                                Text(item.artist)
                                    .font(theme.font(size: 11))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(6)
                        .derbisCard(elevated: player.currentTrack?.id == item.id)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button {
                            player.startSongRadio(from: item)
                        } label: {
                            Label("Start song radio", systemImage: "infinity")
                        }
                        Button {
                            selectedArtist = ArtistStudioDestination(artistName: item.artist)
                        } label: {
                            Label("Go to artist", systemImage: "person.wave.2")
                        }
                    }
                }
            }
        }
    }

    private var orbitGridSection: some View {
        let items = Array(library.orbitTracks.prefix(9))
        let columns = [
            GridItem(.flexible(), spacing: 10, alignment: .top),
            GridItem(.flexible(), spacing: 10, alignment: .top),
            GridItem(.flexible(), spacing: 10, alignment: .top)
        ]

        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(theme.isTorqueSkin ? "YOUR ORBIT" : "Your orbit")
                    .font(theme.font(size: 18, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text("\(items.count) picks")
                    .font(theme.font(size: 12, weight: .medium))
                    .foregroundColor(theme.secondaryText)
            }

            LazyVGrid(columns: columns, alignment: .leading, spacing: 12) {
                ForEach(items) { track in
                    Button {
                        player.play(track: track, in: items)
                    } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            TrackArtworkView(
                                url: track.displayArtworkURL,
                                cornerRadius: theme.artworkRadius
                            )

                            Text(track.title)
                                .font(theme.font(size: 12, weight: .semibold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)

                            Text(track.artist)
                                .font(theme.font(size: 11))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity, alignment: .topLeading)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func carouselShelfSection(_ shelf: HomeShelf) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(theme.isTorqueSkin ? shelf.title.uppercased() : shelf.title)
                        .font(theme.font(size: 19, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Text(shelf.subtitle)
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)
                }
                Spacer()
                Button {
                    if let first = shelf.tracks.first {
                        player.startSongRadio(from: first)
                    }
                } label: {
                    Label("Radio", systemImage: "infinity")
                        .font(theme.font(size: 12.5, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(shelf.tracks.prefix(10)) { track in
                        VStack(alignment: .leading, spacing: 6) {
                            Button {
                                HapticsAndGesturesService.shared.playControlTap(style: .light)
                                player.play(track: track, in: shelf.tracks)
                            } label: {
                                ZStack(alignment: .bottomTrailing) {
                                    TrackArtworkView(
                                        url: track.displayArtworkURL,
                                        size: 142,
                                        cornerRadius: theme.artworkRadius
                                    )
                                    Image(systemName: player.currentTrack?.id == track.id && player.isPlaying ? "waveform" : "play.fill")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(theme.onAccentText)
                                        .frame(width: 28, height: 28)
                                        .background(Circle().fill(theme.accent))
                                        .padding(8)
                                }
                            }
                            .buttonStyle(.plain)

                            Text(track.title)
                                .font(theme.font(size: 13, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)

                            Button {
                                selectedArtist = ArtistStudioDestination(artistName: track.artist)
                            } label: {
                                Text(track.artist)
                                    .font(theme.font(size: 11.5))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            .buttonStyle(.plain)
                        }
                        .frame(width: 142, alignment: .leading)
                    }
                }
            }
        }
    }

    private func shelfSection(_ shelf: HomeShelf) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(theme.isTorqueSkin ? shelf.title.uppercased() : shelf.title)
                        .font(theme.font(size: 19, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Text(shelf.subtitle)
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)
                }
                Spacer()
                Button {
                    if let first = shelf.tracks.first {
                        player.play(track: first, in: shelf.tracks)
                    }
                } label: {
                    Text("Play all")
                        .font(theme.font(size: 13, weight: .semibold))
                        .foregroundColor(theme.accent)
                }
            }

            VStack(spacing: 2) {
                ForEach(shelf.tracks.prefix(5)) { track in
                    TrackRowView(track: track, contextQueue: shelf.tracks)
                }
            }
            .padding(.vertical, 6)
            .derbisCard()
        }
    }

    private func soundtrackQueuePool() -> [Track] {
        var pool: [Track] = []
        var seen = Set<String>()
        for t in library.orbitTracks + library.likedTracks + displayShelves.flatMap(\.tracks) {
            if seen.insert(t.id).inserted {
                pool.append(t)
            }
        }
        return pool
    }

    private func reloadFeed(force: Bool) async {
        guard force || shelves.isEmpty else { return }
        isLoading = true
        let fetched = await InnerTubeService.shared.fetchHomeShelves(mood: selectedMood)
        if !fetched.isEmpty {
            shelves = fetched
            if selectedMood == "All" {
                library.saveCachedHomeShelves(fetched)
            }
        }
        isLoading = false
    }
}

struct DerbisDiamondMark: View {
    let color: Color
    let size: CGFloat

    var body: some View {
        Canvas { context, canvasSize in
            let s = min(canvasSize.width, canvasSize.height)
            let d = s * 0.25
            let centers = [
                CGPoint(x: s * 0.36, y: s * 0.26),
                CGPoint(x: s * 0.66, y: s * 0.50),
                CGPoint(x: s * 0.36, y: s * 0.74)
            ]
            for c in centers {
                var p = Path()
                p.move(to: CGPoint(x: c.x, y: c.y - d))
                p.addLine(to: CGPoint(x: c.x + d, y: c.y))
                p.addLine(to: CGPoint(x: c.x, y: c.y + d))
                p.addLine(to: CGPoint(x: c.x - d, y: c.y))
                p.closeSubpath()
                context.fill(p, with: .color(color))
            }
        }
        .frame(width: size, height: size)
    }
}
