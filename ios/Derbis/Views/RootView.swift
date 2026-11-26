import SwiftUI

enum MainTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case search = "Search"
    case library = "Library"
    case settings = "Settings"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .search: return "magnifyingglass"
        case .library: return "square.stack.fill"
        case .settings: return "slider.horizontal.3"
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var jam = JamService.shared

    @State private var activeTab: MainTab = .home
    @State private var showingFullPlayer: Bool = false
    @State private var showingJamSheetFromRoot: Bool = false
    @State private var isTabBarMinimized: Bool = false
    @State private var miniDragOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch activeTab {
                case .home:
                    HomeView()
                case .search:
                    SearchView()
                case .library:
                    LibraryView()
                case .settings:
                    SettingsView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if theme.usesLiquidGlass {
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0.0),
                        .init(color: Color.black.opacity(0.62), location: 0.45),
                        .init(color: Color.black.opacity(0.92), location: 1.0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: player.currentTrack != nil ? 156 : 104)
                .ignoresSafeArea(edges: .bottom)
                .allowsHitTesting(false)

                liquidGlassBottomChrome
            } else {
                classicBottomChrome
            }
        }
        .preferredColorScheme(theme.colorScheme)
        .onAppear {
            theme.dynamicCoverAccent = player.coverVibrantAccent
        }
        .onChange(of: player.coverVibrantAccent) { newAccent in
            theme.dynamicCoverAccent = newAccent
        }
        .onChange(of: player.currentTrack?.id) { newId in
            if newId == nil && isTabBarMinimized {
                withAnimation(.spring(response: 0.34, dampingFraction: 0.84)) {
                    isTabBarMinimized = false
                }
            }
        }
        .onChange(of: activeTab) { _ in
            if isTabBarMinimized {
                withAnimation(.spring(response: 0.34, dampingFraction: 0.84)) {
                    isTabBarMinimized = false
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .derbisScrollTabBarMinimizeChanged)) { note in
            guard theme.usesLiquidGlass,
                  theme.tabBarMinimizesOnScroll,
                  player.currentTrack != nil,
                  let target = note.userInfo?["minimized"] as? Bool,
                  target != isTabBarMinimized else {
                return
            }
            withAnimation(.spring(response: 0.36, dampingFraction: 0.84)) {
                isTabBarMinimized = target
            }
        }
        .sheet(isPresented: $showingFullPlayer) {
            NowPlayingView()
                .environmentObject(player)
                .environmentObject(LibraryStore.shared)
                .environmentObject(theme)
                .presentationDragIndicator(.hidden)
        }
        .sheet(isPresented: $showingJamSheetFromRoot) {
            JamSheet()
                .environmentObject(player)
                .environmentObject(theme)
        }
        .onReceive(NotificationCenter.default.publisher(for: .derbisPresentJamSheet)) { _ in
            showingFullPlayer = false
            showingJamSheetFromRoot = true
        }
    }

    private var liquidGlassBottomChrome: some View {
        DerbisGlassContainer(spacing: 14) {
            if isTabBarMinimized, theme.tabBarMinimizesOnScroll, let current = player.currentTrack {
                HStack(spacing: 10) {
                    Menu {
                        ForEach(MainTab.allCases) { tab in
                            Button {
                                HapticsAndGesturesService.shared.playSelectionTick()
                                activeTab = tab
                            } label: {
                                Label(tab.rawValue, systemImage: tab.icon)
                            }
                        }
                    } label: {
                        Image(systemName: activeTab.icon)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(theme.accent)
                            .derbisGlassCircle(size: 48)
                    }

                    liquidGlassMiniPlayer(track: current, compactDocked: true)

                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        withAnimation(.spring(response: 0.36, dampingFraction: 0.82)) {
                            isTabBarMinimized = false
                        }
                    } label: {
                        Image(systemName: "chevron.up")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .derbisGlassCircle(size: 48)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 6)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                VStack(spacing: 10) {
                    if let current = player.currentTrack {
                        liquidGlassMiniPlayer(track: current, compactDocked: false)
                            .padding(.horizontal, 14)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    liquidGlassTabBar
                        .padding(.horizontal, 14)
                        .padding(.bottom, 4)
                }
            }
        }
    }

    private func liquidGlassMiniPlayer(track: Track, compactDocked: Bool) -> some View {
        let barThickness = CGFloat(theme.miniPlayerProgressThickness)
        let cardRadius: CGFloat = 24

        return ZStack(alignment: .bottom) {
            HStack(spacing: compactDocked ? 9 : 12) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    showingFullPlayer = true
                } label: {
                    HStack(spacing: compactDocked ? 9 : 12) {
                        TrackArtworkView(
                            url: track.displayArtworkURL,
                            size: compactDocked ? 36 : 44,
                            cornerRadius: compactDocked ? 8 : 10
                        )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(track.title)
                                .font(theme.font(size: compactDocked ? 13 : 14.5, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(1)

                            HStack(spacing: 5) {
                                if jam.isActive {
                                    HStack(spacing: 3) {
                                        Circle()
                                            .fill(Color.green)
                                            .frame(width: 5, height: 5)
                                        Text("JAM · \(max(1, jam.participantCount))")
                                            .font(.system(size: 9.5, weight: .black, design: .rounded))
                                            .foregroundColor(theme.accent)
                                    }
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 1.5)
                                    .background(Capsule().fill(theme.accent.opacity(0.18)))
                                }

                                Text(track.artist)
                                    .font(theme.font(size: compactDocked ? 11 : 12))
                                    .foregroundColor(Color.white.opacity(0.68))
                                    .lineLimit(1)
                            }
                        }
                        Spacer(minLength: 2)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if player.isLoading {
                    ProgressView()
                        .tint(theme.accent)
                        .frame(width: 34, height: 34)
                } else {
                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.togglePlayPause()
                    } label: {
                        Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: compactDocked ? 15 : 17, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: compactDocked ? 32 : 36, height: compactDocked ? 32 : 36)
                            .background(Circle().fill(Color.white.opacity(0.11)))
                    }
                    .buttonStyle(.plain)
                }

                if !compactDocked {
                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        player.skipNext()
                    } label: {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white.opacity(0.92))
                            .frame(width: 34, height: 34)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, compactDocked ? 10 : 12)
            .padding(.top, compactDocked ? 6 : 9)
            .padding(.bottom, compactDocked ? (barThickness > 0 ? 9 : 6) : (barThickness > 0 ? 12 : 9))

            if barThickness > 0 {
                MiniPlayerGlassProgressLine(barThickness: barThickness)
            }
        }
        .offset(x: miniDragOffset)
        .derbisGlassPanel(
            cornerRadius: cardRadius,
            tint: player.ambientPrimary.opacity(0.25),
            interactive: true
        )
        .simultaneousGesture(
            DragGesture(minimumDistance: 28)
                .onChanged { value in
                    guard abs(value.translation.width) > abs(value.translation.height) else { return }
                    miniDragOffset = value.translation.width * 0.25
                }
                .onEnded { value in
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                        miniDragOffset = 0
                    }
                    guard abs(value.translation.width) > abs(value.translation.height) else { return }
                    if value.translation.width < -52 {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.skipNext()
                    } else if value.translation.width > 52 {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.skipPrevious()
                    }
                }
        )
    }

    private var tabBarSpecs: [TaurusTabBarItemSpec] {
        MainTab.allCases.enumerated().map { idx, tab in
            TaurusTabBarItemSpec(id: idx, title: theme.hideTabLabels ? "" : tab.rawValue, symbol: tab.icon)
        }
    }

    private var selectedTabIndexBinding: Binding<Int> {
        Binding<Int>(
            get: {
                MainTab.allCases.firstIndex(of: activeTab) ?? 0
            },
            set: { newIdx in
                guard MainTab.allCases.indices.contains(newIdx) else { return }
                let chosen = MainTab.allCases[newIdx]
                if activeTab != chosen {
                    activeTab = chosen
                }
            }
        )
    }

    private var liquidGlassTabBar: some View {
        HStack(spacing: 10) {
            TaurusLiquidGlassTabBar(
                items: tabBarSpecs,
                selectedId: selectedTabIndexBinding,
                onLongPressFirstItem: {
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                    activeTab = .settings
                }
            )

            if theme.tabBarMinimizesOnScroll, player.currentTrack != nil {
                Button {
                    HapticsAndGesturesService.shared.playSelectionTick()
                    withAnimation(.spring(response: 0.36, dampingFraction: 0.82)) {
                        isTabBarMinimized = true
                    }
                } label: {
                    Image(systemName: "arrow.down.right.and.arrow.up.left")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white.opacity(0.88))
                        .derbisGlassCircle(size: 56, interactive: true)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var classicBottomChrome: some View {
        VStack(spacing: 0) {
            if let current = player.currentTrack {
                classicMiniPlayerBar(track: current)
                    .padding(.horizontal, 12)
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            classicBottomTabBar
        }
    }

    private func classicMiniPlayerBar(track: Track) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button {
                    showingFullPlayer = true
                } label: {
                    HStack(spacing: 12) {
                        TrackArtworkView(url: track.displayArtworkURL, size: 42, cornerRadius: theme.isPixelSkin ? 0 : 8)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(track.title)
                                .font(theme.font(size: 14, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)
                            Text(track.artist)
                                .font(theme.font(size: 12))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 4)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if player.isLoading {
                    ProgressView()
                        .tint(theme.accent)
                        .frame(width: 34, height: 34)
                } else {
                    Button {
                        player.togglePlayPause()
                    } label: {
                        Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .frame(width: 36, height: 36)
                    }
                }

                Button {
                    player.skipNext()
                } label: {
                    Image(systemName: "forward.fill")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .frame(width: 34, height: 34)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)

            ClassicMiniPlayerProgressLine(accent: theme.accent, border: theme.border)
        }
        .clipShape(RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous))
        .derbisCard(elevated: true)
    }

    private var classicBottomTabBar: some View {
        HStack(spacing: 0) {
            ForEach(MainTab.allCases) { tab in
                let selected = activeTab == tab
                Button {
                    activeTab = tab
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.system(size: theme.hideTabLabels ? 20 : 18, weight: selected ? .bold : .medium))
                        if !theme.hideTabLabels {
                            Text(tab.rawValue)
                                .font(theme.font(size: 11, weight: selected ? .bold : .medium))
                        }
                    }
                    .foregroundColor(selected ? theme.accent : theme.secondaryText)
                    .frame(maxWidth: .infinity)
                    .padding(.top, theme.hideTabLabels ? 13 : 10)
                    .padding(.bottom, theme.hideTabLabels ? 11 : 8)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .simultaneousGesture(
                    LongPressGesture(minimumDuration: 0.48)
                        .onEnded { _ in
                            if tab == .home {
                                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                activeTab = .settings
                            }
                        }
                )
            }
        }
        .background(
            theme.surface
                .overlay(
                    Rectangle()
                        .fill(theme.border)
                        .frame(height: theme.isPixelSkin ? 2 : 0.6),
                    alignment: .top
                )
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

private struct MiniPlayerGlassProgressLine: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let barThickness: CGFloat

    var body: some View {
        let progress = clock.duration > 0 ? min(1.0, max(0.0, clock.currentTime / clock.duration)) : 0
        GeometryReader { geo in
            let inset: CGFloat = 14
            let usableWidth = max(0, geo.size.width - inset * 2)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.white.opacity(0.24))
                    .frame(width: usableWidth, height: barThickness)
                Capsule()
                    .fill(Color.white)
                    .frame(width: usableWidth * progress, height: barThickness)
            }
            .padding(.horizontal, inset)
            .frame(maxHeight: .infinity, alignment: .bottom)
            .padding(.bottom, 3.5)
        }
        .frame(height: barThickness + 6)
        .allowsHitTesting(false)
    }
}

private struct ClassicMiniPlayerProgressLine: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let accent: Color
    let border: Color

    var body: some View {
        let progress = clock.duration > 0 ? min(1.0, max(0.0, clock.currentTime / clock.duration)) : 0
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(border.opacity(0.3))
                Rectangle()
                    .fill(accent)
                    .frame(width: geo.size.width * progress)
            }
        }
        .frame(height: 2)
    }
}
