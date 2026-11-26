import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared

    @State private var selectedMood: String = "All"
    @State private var shelves: [HomeShelf] = []
    @State private var isLoading: Bool = false
    @State private var showingGoogleAccountSheet: Bool = false

    private let moods = ["All", "Chill", "Late Night", "Focus", "Energy", "OPM"]

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 24) {
                        headerBar
                        moodFilterStrip
                        soundtrackHeroCard

                        if !library.orbitTracks.isEmpty {
                            orbitGridSection
                        }

                        if isLoading && displayShelves.isEmpty {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .tint(theme.accent)
                                    .padding(.vertical, 36)
                                Spacer()
                            }
                        } else {
                            ForEach(displayShelves) { shelf in
                                shelfSection(shelf)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 120)
                }
                .derbisAutoMinimizeTabBarOnScroll()
                .refreshable {
                    await reloadFeed(force: true)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $showingGoogleAccountSheet) {
                GoogleAccountSheet()
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .onReceive(NotificationCenter.default.publisher(for: .derbisGoogleAccountDidChange)) { _ in
                Task {
                    await reloadFeed(force: true)
                }
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

    private var headerBar: some View {
        HStack(spacing: 12) {
            DerbisDiamondMark(color: theme.primaryText, size: 22)

            VStack(alignment: .leading, spacing: 2) {
                Text("Derbis")
                    .font(theme.font(size: 24, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Text(
                    googleSync.activeAccount.map { "\($0.name) · \(theme.skin.title)" }
                        ?? theme.skin.title
                )
                .font(theme.font(size: 12, weight: .medium))
                .foregroundColor(theme.secondaryText)
                .lineLimit(1)
            }

            Spacer()

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
                Section("Liquid Glass") {
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            theme.liquidGlassEnabled.toggle()
                        }
                    } label: {
                        HStack {
                            Text("Taurus Liquid Glass Chrome")
                            if theme.liquidGlassEnabled {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: theme.usesLiquidGlass ? "sparkles" : "paintbrush.pointed.fill")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Skin")
                        .font(theme.font(size: 13, weight: .semibold))
                }
                .foregroundColor(theme.primaryText)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .derbisCard(elevated: true)
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
                            Text(mood)
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

    private var soundtrackHeroCard: some View {
        let pool = soundtrackQueuePool()
        return Button {
            if let seed = pool.randomElement() {
                var shuffled = pool.shuffled()
                shuffled.removeAll { $0.id == seed.id }
                shuffled.insert(seed, at: 0)
                player.play(track: seed, in: shuffled)
            }
        } label: {
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("PERSONAL RADIO")
                        .font(theme.font(size: 11, weight: .bold))
                        .foregroundColor(theme.accent)
                        .tracking(1.0)

                    Text("My soundtrack")
                        .font(theme.font(size: 22, weight: .bold))
                        .foregroundColor(theme.primaryText)

                    Text("Continuous mix built from your rotation, liked cuts, and \(selectedMood.lowercased()) picks")
                        .font(theme.font(size: 13))
                        .foregroundColor(theme.secondaryText)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 12, weight: .bold))
                        Text("Start session")
                            .font(theme.font(size: 13, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        Group {
                            if theme.isPixelSkin {
                                Rectangle().fill(theme.accent)
                            } else {
                                Capsule().fill(theme.accent)
                            }
                        }
                    )
                    .padding(.top, 4)
                }

                Spacer(minLength: 8)

                if let coverTrack = pool.first {
                    TrackArtworkView(url: coverTrack.displayArtworkURL, size: 88, cornerRadius: theme.isPixelSkin ? 0 : 14)
                }
            }
            .padding(16)
            .derbisCard(elevated: true)
        }
        .buttonStyle(.plain)
    }

    private var orbitGridSection: some View {
        let items = Array(library.orbitTracks.prefix(9))
        let columns = [
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10)
        ]

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Your orbit")
                    .font(theme.font(size: 19, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text("\(items.count) picks")
                    .font(theme.font(size: 12, weight: .medium))
                    .foregroundColor(theme.secondaryText)
            }

            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(items) { track in
                    Button {
                        player.play(track: track, in: items)
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            GeometryReader { geo in
                                TrackArtworkView(
                                    url: track.displayArtworkURL,
                                    size: geo.size.width,
                                    cornerRadius: theme.isPixelSkin ? 0 : 10
                                )
                            }
                            .aspectRatio(1, contentMode: .fit)

                            Text(track.title)
                                .font(theme.font(size: 12, weight: .semibold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)

                            Text(track.artist)
                                .font(theme.font(size: 11))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func shelfSection(_ shelf: HomeShelf) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(shelf.title)
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
