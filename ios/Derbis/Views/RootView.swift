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

    @State private var activeTab: MainTab = .home
    @State private var showingFullPlayer: Bool = false

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

            VStack(spacing: 0) {
                if let current = player.currentTrack {
                    miniPlayerBar(track: current)
                        .padding(.horizontal, 12)
                        .padding(.bottom, 8)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                bottomTabBar
            }
        }
        .preferredColorScheme(theme.colorScheme)
        .sheet(isPresented: $showingFullPlayer) {
            NowPlayingView()
                .environmentObject(player)
                .environmentObject(LibraryStore.shared)
                .environmentObject(theme)
                .presentationDragIndicator(.hidden)
        }
    }

    private func miniPlayerBar(track: Track) -> some View {
        let progress = player.duration > 0 ? min(1.0, max(0.0, player.currentTime / player.duration)) : 0

        return VStack(spacing: 0) {
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

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(theme.border.opacity(0.3))
                    Rectangle()
                        .fill(theme.accent)
                        .frame(width: geo.size.width * progress)
                }
            }
            .frame(height: 2)
        }
        .clipShape(RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous))
        .derbisCard(elevated: true)
    }

    private var bottomTabBar: some View {
        HStack(spacing: 0) {
            ForEach(MainTab.allCases) { tab in
                let selected = activeTab == tab
                Button {
                    activeTab = tab
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 18, weight: selected ? .bold : .medium))
                        Text(tab.rawValue)
                            .font(theme.font(size: 11, weight: selected ? .bold : .medium))
                    }
                    .foregroundColor(selected ? theme.accent : theme.secondaryText)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 10)
                    .padding(.bottom, 8)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
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
