import SwiftUI

enum MainTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case search = "Explore"
    case library = "Library"
    case settings = "Studio"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .home: return "sparkles.rectangle.stack.fill"
        case .search: return "safari.fill"
        case .library: return "square.stack.3d.up.fill"
        case .settings: return "slider.horizontal.3"
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var jam = JamService.shared

    @Namespace private var dockTabNamespace
    @State private var activeTab: MainTab = .home
    @State private var showingFullPlayer: Bool = false
    @State private var showingJamSheetFromRoot: Bool = false
    @State private var isTabBarMinimized: Bool = false
    @State private var miniDragOffset: CGFloat = 0

    private var activeBottomOverlayInset: CGFloat {
        if player.currentTrack != nil {
            if isTabBarMinimized && theme.tabBarMinimizesOnScroll {
                return 80
            }
            return 146
        }
        return 82
    }

    private var bottomScrimHeight: CGFloat {
        if player.currentTrack != nil {
            if isTabBarMinimized && theme.tabBarMinimizesOnScroll {
                return 84
            }
            return 152
        }
        return 88
    }

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
            .environment(\.derbisBottomOverlayInset, activeBottomOverlayInset)

            if !theme.isPixelSkin {
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0.0),
                        .init(color: theme.background.opacity(0.68), location: 0.52),
                        .init(color: theme.background.opacity(0.95), location: 1.0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: bottomScrimHeight)
                .ignoresSafeArea(edges: .bottom)
                .allowsHitTesting(false)
            }

            if theme.usesLiquidGlass {
                liquidGlassBottomChrome
            } else {
                derbisUnifiedBottomDock
            }
        }
        .preferredColorScheme(theme.colorScheme)
        .onAppear {
            theme.dynamicCoverAccent = player.coverVibrantAccent
            player.syncEndlessListeningFromPreferences(theme.endlessRadioEnabled)
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
            guard theme.tabBarMinimizesOnScroll,
                  player.currentTrack != nil,
                  let target = note.userInfo?["minimized"] as? Bool,
                  target != isTabBarMinimized else {
                return
            }
            withAnimation(.spring(response: 0.36, dampingFraction: 0.84)) {
                isTabBarMinimized = target
            }
        }
        .fullScreenCover(isPresented: $showingFullPlayer) {
            NowPlayingView()
                .environmentObject(player)
                .environmentObject(LibraryStore.shared)
                .environmentObject(theme)
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

    private var derbisUnifiedBottomDock: some View {
        Group {
            if isTabBarMinimized, theme.tabBarMinimizesOnScroll, let current = player.currentTrack {
                HStack(spacing: 8) {
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
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(theme.accent)
                            .derbisGlassCircle(size: 46)
                    }

                    derbisMiniPlayerCard(track: current, compactDocked: true)

                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        withAnimation(.spring(response: 0.36, dampingFraction: 0.82)) {
                            isTabBarMinimized = false
                        }
                    } label: {
                        Image(systemName: "chevron.up")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .derbisGlassCircle(size: 46)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 6)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                VStack(spacing: 8) {
                    if let current = player.currentTrack {
                        derbisMiniPlayerCard(track: current, compactDocked: false)
                            .padding(.horizontal, 12)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    derbisPillNavigationDock
                        .padding(.horizontal, 12)
                        .padding(.bottom, 4)
                }
            }
        }
    }

    private func derbisMiniPlayerCard(track: Track, compactDocked: Bool) -> some View {
        let barThickness = CGFloat(theme.miniPlayerProgressThickness)
        let artSize: CGFloat = compactDocked ? 36 : 44
        let useLeaf = theme.effectivePlayerDeck == .flow && !theme.isPixelSkin && !theme.isTorqueSkin

        return VStack(spacing: 0) {
            HStack(spacing: compactDocked ? 9 : 12) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    showingFullPlayer = true
                } label: {
                    HStack(spacing: compactDocked ? 9 : 12) {
                        Group {
                            if useLeaf {
                                TrackArtworkView(url: track.displayArtworkURL, size: artSize, cornerRadius: 0)
                                    .clipShape(
                                        AsymmetricLeafShape(
                                            topLeading: 14,
                                            bottomLeading: 6,
                                            bottomTrailing: 14,
                                            topTrailing: 6
                                        )
                                    )
                            } else {
                                TrackArtworkView(
                                    url: track.displayArtworkURL,
                                    size: artSize,
                                    cornerRadius: theme.artworkRadius
                                )
                            }
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(track.title)
                                .font(theme.font(size: compactDocked ? 13 : 14, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)

                            HStack(spacing: 5) {
                                if jam.isActive {
                                    miniBadge(label: "JAM · \(max(1, jam.participantCount))", color: .green)
                                } else if player.endlessListeningEnabled && player.radioAutoTrackIds.contains(track.id) {
                                    miniBadge(label: "RADIO", color: theme.accent)
                                }

                                Text(track.artist)
                                    .font(theme.font(size: compactDocked ? 11 : 12))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
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
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.togglePlayPause()
                    } label: {
                        let btnSize: CGFloat = compactDocked ? 32 : 36
                        ZStack {
                            if theme.isPixelSkin {
                                Rectangle()
                                    .fill(theme.accent)
                                    .frame(width: btnSize, height: btnSize)
                                    .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                            } else {
                                RoundedRectangle(
                                    cornerRadius: theme.isTorqueSkin ? 6 : (player.isPlaying ? 11 : btnSize * 0.5),
                                    style: .continuous
                                )
                                .fill(theme.accent)
                                .frame(width: btnSize, height: btnSize)
                            }
                            Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: compactDocked ? 13 : 15, weight: .bold))
                                .foregroundColor(theme.isPixelSkin ? .white : theme.onAccentText)
                                .offset(x: player.isPlaying ? 0 : 1)
                        }
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
                            .foregroundColor(theme.primaryText)
                            .frame(width: 34, height: 34)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, compactDocked ? 10 : 12)
            .padding(.vertical, compactDocked ? 7 : 9)

            if barThickness > 0 {
                DerbisDockProgressLine(
                    accent: theme.accent,
                    secondaryAccent: theme.secondaryAccent,
                    rail: theme.border.opacity(0.28),
                    thickness: barThickness
                )
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous))
        .derbisCard(elevated: true)
        .shadow(color: theme.isPixelSkin ? .clear : Color.black.opacity(theme.isLightSkin ? 0.08 : 0.42), radius: 14, x: 0, y: 6)
        .offset(x: miniDragOffset)
        .simultaneousGesture(
            DragGesture(minimumDistance: 24)
                .onChanged { value in
                    let dx = value.translation.width
                    let dy = value.translation.height
                    if abs(dx) > abs(dy) {
                        miniDragOffset = dx * 0.28
                    }
                }
                .onEnded { value in
                    let dx = value.translation.width
                    let dy = value.translation.height
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                        miniDragOffset = 0
                    }
                    if dy < -38 && abs(dy) > abs(dx) {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        showingFullPlayer = true
                        return
                    }
                    guard abs(dx) > abs(dy) else { return }
                    if dx < -50 {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.skipNext()
                    } else if dx > 50 {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        player.skipPrevious()
                    }
                }
        )
    }

    private func miniBadge(label: String, color: Color) -> some View {
        Text(label)
            .font(.system(size: 8.5, weight: .black, design: .rounded))
            .foregroundColor(color)
            .padding(.horizontal, 5)
            .padding(.vertical, 1.5)
            .background(Capsule().fill(color.opacity(0.16)))
    }

    private var derbisPillNavigationDock: some View {
        HStack(spacing: 6) {
            ForEach(MainTab.allCases) { tab in
                let selected = activeTab == tab
                Button {
                    HapticsAndGesturesService.shared.playSelectionTick()
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.80)) {
                        activeTab = tab
                    }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: tab.icon)
                            .font(.system(size: theme.hideTabLabels ? 18 : 15.5, weight: selected ? .bold : .semibold))

                        if !theme.hideTabLabels && (selected || !theme.isPixelSkin) {
                            Text(theme.isTorqueSkin ? tab.rawValue.uppercased() : tab.rawValue)
                                .font(theme.font(size: 11.5, weight: selected ? .bold : .semibold))
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                        }
                    }
                    .foregroundColor(selected ? (theme.isPixelSkin ? .white : theme.accent) : theme.secondaryText)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(
                        ZStack {
                            if selected {
                                if theme.isPixelSkin {
                                    Rectangle()
                                        .fill(theme.accent)
                                        .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                                        .matchedGeometryEffect(id: "derbisDockActivePill", in: dockTabNamespace)
                                } else if theme.isTorqueSkin {
                                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                                        .fill(theme.accent.opacity(0.16))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                                .stroke(theme.accent.opacity(0.5), lineWidth: 1)
                                        )
                                        .matchedGeometryEffect(id: "derbisDockActivePill", in: dockTabNamespace)
                                } else {
                                    Capsule()
                                        .fill(
                                            LinearGradient(
                                                colors: [theme.accent.opacity(0.20), theme.secondaryAccent.opacity(0.14)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                        .overlay(Capsule().stroke(theme.accent.opacity(0.36), lineWidth: 0.9))
                                        .matchedGeometryEffect(id: "derbisDockActivePill", in: dockTabNamespace)
                                }
                            }
                        }
                    )
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(6)
        .derbisCard(elevated: false)
        .shadow(color: theme.isPixelSkin ? .clear : Color.black.opacity(theme.isLightSkin ? 0.07 : 0.48), radius: 16, x: 0, y: 8)
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
                VStack(spacing: 8) {
                    if let current = player.currentTrack {
                        liquidGlassMiniPlayer(track: current, compactDocked: false)
                            .padding(.horizontal, 12)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    liquidGlassTabBar
                        .padding(.horizontal, 12)
                        .padding(.bottom, 2)
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
                                    miniBadge(label: "JAM · \(max(1, jam.participantCount))", color: .green)
                                } else if player.endlessListeningEnabled && player.radioAutoTrackIds.contains(track.id) {
                                    miniBadge(label: "RADIO", color: theme.accent)
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
                MiniPlayerGlassProgressLine(barThickness: barThickness, accent: theme.accent)
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

    private var tabBarSpecs: [DerbisTabBarItemSpec] {
        MainTab.allCases.enumerated().map { idx, tab in
            DerbisTabBarItemSpec(id: idx, title: theme.hideTabLabels ? "" : tab.rawValue, symbol: tab.icon)
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
        HStack(spacing: 8) {
            DerbisLiquidGlassTabBar(
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
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white.opacity(0.88))
                        .derbisGlassCircle(size: 48, interactive: true)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct MiniPlayerGlassProgressLine: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let barThickness: CGFloat
    let accent: Color

    var body: some View {
        let progress = clock.duration > 0 ? min(1.0, max(0.0, clock.currentTime / clock.duration)) : 0
        GeometryReader { geo in
            let inset: CGFloat = 14
            let usableWidth = max(0, geo.size.width - inset * 2)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.white.opacity(0.20))
                    .frame(width: usableWidth, height: barThickness)
                Capsule()
                    .fill(accent)
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

private struct DerbisDockProgressLine: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let accent: Color
    let secondaryAccent: Color
    let rail: Color
    let thickness: CGFloat

    var body: some View {
        let progress = clock.duration > 0 ? min(1.0, max(0.0, clock.currentTime / clock.duration)) : 0
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(rail)
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [accent, secondaryAccent],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: geo.size.width * progress)
            }
        }
        .frame(height: thickness)
    }
}
