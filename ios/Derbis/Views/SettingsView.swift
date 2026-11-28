import SwiftUI
import UniformTypeIdentifiers
import PhotosUI

struct SettingsView: View {
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var subsonic: SubsonicService
    @ObservedObject private var spotifySession = SpotifySessionStore.shared
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @ObservedObject private var haptics = HapticsAndGesturesService.shared
    @ObservedObject private var jam = JamService.shared

    private enum SettingsFileImportKind {
        case backupJSON
        case customFont
    }

    /// Top-level settings buckets. Every option lives inside exactly one category instead of
    /// being dropped onto one flat page — same idea as the iOS Settings app.
    private enum SettingsCategory: String, CaseIterable, Identifiable {
        case appearance, fonts, audio, listening, accountsSync, storageBackup, about

        var id: String { rawValue }

        var title: String {
            switch self {
            case .appearance: return "Appearance"
            case .fonts: return "Fonts"
            case .audio: return "Audio"
            case .listening: return "Listening"
            case .accountsSync: return "Accounts & Sync"
            case .storageBackup: return "Storage & Backup"
            case .about: return "About"
            }
        }

        var subtitle: String {
            switch self {
            case .appearance: return "Skin, deck, glass dock & motion"
            case .fonts: return "Typeface & custom fonts"
            case .audio: return "EQ studio, AutoMix, karaoke & lock screen"
            case .listening: return "Stats, lyrics sources & blocked artists"
            case .accountsSync: return "Google, Jam & Subsonic / Navidrome"
            case .storageBackup: return "On-device audio, export & restore"
            case .about: return "Version & credits"
            }
        }

        var icon: String {
            switch self {
            case .appearance: return "paintpalette.fill"
            case .fonts: return "textformat"
            case .audio: return "slider.vertical.3"
            case .listening: return "headphones"
            case .accountsSync: return "person.crop.circle.fill"
            case .storageBackup: return "cylinder.split.1x2.fill"
            case .about: return "info.circle.fill"
            }
        }
    }

    @State private var serverURLInput: String = ""
    @State private var usernameInput: String = ""
    @State private var passwordInput: String = ""
    @State private var preferRawLossless: Bool = true
    @State private var isTestingServer: Bool = false
    @State private var activeFileImportKind: SettingsFileImportKind = .backupJSON
    @State private var showingFileImporter: Bool = false
    @State private var showingSpotifyCanvasSheet: Bool = false
    @State private var showingAudioStudioSheet: Bool = false
    @State private var showingGoogleAccountSheet: Bool = false
    @State private var showingJamSheet: Bool = false
    @State private var showingListeningStatsSheet: Bool = false
    @State private var showingReceiptSheet: Bool = false
    @State private var showingTabBarEditor: Bool = false
    @State private var statusBanner: String?
    @State private var pickedGlassBackground: PhotosPickerItem? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(theme.isTorqueSkin ? "STUDIO" : "Studio")
                                    .font(theme.font(size: 26, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text("Skin, deck, sound & sync")
                                    .font(theme.font(size: 12))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()
                            Button {
                                haptics.playControlTap(style: .light)
                                showingReceiptSheet = true
                            } label: {
                                Label("Receipt", systemImage: "doc.plaintext.fill")
                                    .font(theme.font(size: 12.5, weight: .bold))
                                    .foregroundColor(theme.accent)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 7)
                                    .derbisGlassCapsule()
                            }
                            .buttonStyle(.plain)
                        }

                        statusBannerView

                        studioQuickHubSection
                        sectionTitle("Categories")
                        ForEach(SettingsCategory.allCases) { category in
                            categoryRow(category)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 6)
                    .derbisBottomBarClearance()
                }
                .derbisAutoMinimizeTabBarOnScroll()
            }
            .navigationDestination(for: SettingsCategory.self) { category in
                categoryPage(category)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .onAppear {
                serverURLInput = subsonic.config.serverURL
                usernameInput = subsonic.config.username
                passwordInput = subsonic.loadPassword()
                preferRawLossless = subsonic.config.preferRawLossless
            }
            .fileImporter(
                isPresented: $showingFileImporter,
                allowedContentTypes: activeFileImportKind == .backupJSON ? [.json] : [.font],
                allowsMultipleSelection: false
            ) { result in
                if case .success(let urls) = result, let first = urls.first {
                    switch activeFileImportKind {
                    case .backupJSON:
                        let ok = library.importBackup(from: first)
                        statusBanner = ok ? "Library backup restored" : "Invalid backup file"
                    case .customFont:
                        let ok = theme.importCustomFontFile(from: first)
                        statusBanner = ok ? "Custom font \(theme.customFontPostScriptName) active" : "Unsupported font file (use .ttf or .otf)"
                    }
                }
            }
            .sheet(isPresented: $showingListeningStatsSheet) {
                ListeningStatsSheet()
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingReceiptSheet) {
                ListeningReceiptSheet()
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingSpotifyCanvasSheet) {
                SpotifyCanvasSheet()
                    .environmentObject(player)
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingAudioStudioSheet) {
                AudioEffectsSheet()
                    .environmentObject(theme)
            }
            .fullScreenCover(isPresented: $showingTabBarEditor) {
                TabBarEditorView()
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingGoogleAccountSheet) {
                GoogleAccountSheet()
                    .environmentObject(player)
                    .environmentObject(library)
                    .environmentObject(theme)
            }
            .sheet(isPresented: $showingJamSheet) {
                JamSheet()
                    .environmentObject(player)
                    .environmentObject(theme)
            }
            .onChange(of: theme.motionCanvasEnabled) { _ in
                player.refreshMotionCanvas()
            }
            .onChange(of: theme.motionCanvasStyle) { _ in
                player.refreshMotionCanvas()
            }
        }
    }

    private var studioQuickHubSection: some View {
        let columns = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
        return LazyVGrid(columns: columns, spacing: 10) {
            Menu {
                ForEach(DerbisSkin.allCases) { option in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { theme.skin = option }
                    } label: {
                        Label(option.title, systemImage: theme.skin == option ? "checkmark" : "paintpalette")
                    }
                }
            } label: {
                hubTile(
                    icon: "paintpalette.fill",
                    title: "Skin",
                    subtitle: theme.skin.title,
                    highlighted: false
                )
            }
            .buttonStyle(.plain)

            Menu {
                ForEach(DerbisPlayerDeck.allCases) { deck in
                    Button {
                        theme.playerDeck = deck
                    } label: {
                        Label(deck.title, systemImage: theme.playerDeck == deck ? "checkmark" : deck.icon)
                    }
                }
            } label: {
                hubTile(
                    icon: theme.effectivePlayerDeck.icon,
                    title: "Player Deck",
                    subtitle: theme.effectivePlayerDeck.title,
                    highlighted: false
                )
            }
            .buttonStyle(.plain)

            Button {
                haptics.playSelectionTick()
                theme.endlessRadioEnabled.toggle()
            } label: {
                hubTile(
                    icon: "infinity",
                    title: "Endless Radio",
                    subtitle: theme.endlessRadioEnabled ? "Active" : "Paused",
                    highlighted: theme.endlessRadioEnabled
                )
            }
            .buttonStyle(.plain)

            Button {
                haptics.playControlTap(style: .light)
                showingAudioStudioSheet = true
            } label: {
                hubTile(
                    icon: "slider.vertical.3",
                    title: "32-bit Mix Lab",
                    subtitle: fx.hasAnyActiveModification ? fx.selectedPreset.rawValue : "AutoEQ & Sing",
                    highlighted: fx.hasAnyActiveModification
                )
            }
            .buttonStyle(.plain)
        }
    }

    private func hubTile(icon: String, title: String, subtitle: String, highlighted: Bool) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(highlighted ? theme.onAccentText : theme.accent)
                .frame(width: 34, height: 34)
                .background(
                    RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 10, style: .continuous)
                        .fill(highlighted ? theme.accent : theme.accent.opacity(0.14))
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.primaryText)
                    .lineLimit(1)
                Text(subtitle)
                    .font(theme.font(size: 11))
                    .foregroundColor(highlighted ? theme.accent : theme.secondaryText)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(11)
        .derbisCard(elevated: highlighted)
    }

    private var googleAccountSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Account & Cloud Sync")

            VStack(spacing: 12) {
                Button {
                    haptics.playControlTap(style: .light)
                    showingGoogleAccountSheet = true
                } label: {
                    HStack(spacing: 12) {
                        GoogleAccountAvatarBadge(size: 44)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(googleSync.activeAccount?.name ?? "Sign in with Google")
                                .font(theme.font(size: 15, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)
                            Text(
                                googleSync.isSignedIn
                                    ? (googleSync.statusMessage ?? "Connected · Liked songs, playlists & history sync active")
                                    : "Connect your Google account to sync Liked Songs, Playlists & History"
                            )
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                            .lineLimit(2)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .buttonStyle(.plain)

                if googleSync.isSignedIn {
                    Divider()

                    HStack(spacing: 10) {
                        Button {
                            haptics.playControlTap(style: .medium)
                            Task {
                                await googleSync.syncAccountDataToLibrary(silent: false)
                            }
                        } label: {
                            HStack(spacing: 6) {
                                if googleSync.isSyncing {
                                    ProgressView().scaleEffect(0.75).tint(theme.accent)
                                } else {
                                    Image(systemName: "arrow.triangle.2.circlepath")
                                        .font(.system(size: 12, weight: .bold))
                                }
                                Text(googleSync.isSyncing ? "Syncing…" : "Sync Now")
                                    .font(theme.font(size: 12.5, weight: .bold))
                            }
                            .foregroundColor(theme.accent)
                        }
                        .disabled(googleSync.isSyncing)

                        Spacer()

                        Button {
                            showingGoogleAccountSheet = true
                        } label: {
                            Text("Manage Cloud Library")
                                .font(theme.font(size: 12.5, weight: .semibold))
                                .foregroundColor(theme.primaryText)
                        }
                    }
                }
            }
            .padding(14)
            .derbisCard(elevated: true)
        }
    }

    private var jamListenTogetherSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Listen Together")

            Button {
                haptics.playControlTap(style: .light)
                showingJamSheet = true
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "person.2.wave.2.fill")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .background(
                            RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 10, style: .continuous)
                                .fill(theme.accent)
                        )

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Derbis Jam")
                            .font(theme.font(size: 15, weight: .bold))
                            .foregroundColor(theme.primaryText)
                        Text(
                            jam.isActive
                                ? "Live Session · \(max(1, jam.participantCount)) listening together"
                                : "Host or join synchronized sessions with iOS & Android"
                        )
                        .font(theme.font(size: 12))
                        .foregroundColor(jam.isActive ? theme.accent : theme.secondaryText)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .buttonStyle(.plain)
            .padding(14)
            .derbisCard(elevated: true)
        }
    }

    private var audioStudioAndHapticsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Audio Studio & Haptics")

            VStack(spacing: 14) {
                Button {
                    haptics.playControlTap(style: .light)
                    showingAudioStudioSheet = true
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "slider.vertical.3")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 38, height: 38)
                            .background(
                                RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 10, style: .continuous)
                                    .fill(theme.accent)
                            )

                        VStack(alignment: .leading, spacing: 2) {
                            Text("Open Audio Studio")
                                .font(theme.font(size: 15, weight: .bold))
                                .foregroundColor(theme.primaryText)
                            Text(
                                fx.hasAnyActiveModification
                                    ? "Active: \(fx.selectedPreset.rawValue) · \(String(format: "%.2gx", fx.playbackSpeed))"
                                    : "10-Band EQ, Adaptive Bass, Vinyl Pitch & Spatial FX"
                            )
                            .font(theme.font(size: 12))
                            .foregroundColor(fx.hasAnyActiveModification ? theme.accent : theme.secondaryText)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .buttonStyle(.plain)

                Divider()

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Beat-Synced Music Haptics")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Taptic Engine kick & snare pulses while listening")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                    Spacer()
                    Picker("Music Haptics", selection: $haptics.musicHapticsMode) {
                        ForEach(MusicHapticsMode.allCases) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .tint(theme.accent)
                }

                Divider()

                Toggle(isOn: $haptics.headGesturesEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("AirPods Head Gestures")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Double-nod to play/pause, shake head to skip tracks")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()
                .disabled(!haptics.headTrackingSupported)
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var playerBackdropAndGesturesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Now Playing backdrop & gestures")

            VStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Backdrop Style")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(PlayerBackdropStyle.allCases) { style in
                                let active = theme.backdropStyle == style
                                Button {
                                    haptics.playSelectionTick()
                                    theme.backdropStyle = style
                                } label: {
                                    Text(style.rawValue)
                                        .font(theme.font(size: 13, weight: .bold))
                                        .foregroundColor(active ? .white : theme.primaryText)
                                        .padding(.horizontal, 13)
                                        .padding(.vertical, 8)
                                        .background(
                                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                                .fill(active ? theme.accent : theme.elevatedSurface)
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    Text(theme.backdropStyle.subtitle)
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)
                }

                Divider()

                Toggle(isOn: $theme.playerGesturesEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Artwork Swipe & Double-Tap Zones")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Swipe cover to skip; double-tap left (-10s), center (Like), right (+10s)")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var skinSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Appearance & skins")

            VStack(spacing: 10) {
                ForEach(DerbisSkin.allCases) { option in
                    let selected = theme.skin == option
                    Button {
                        haptics.playSelectionTick()
                        withAnimation(.easeInOut(duration: 0.2)) {
                            theme.skin = option
                        }
                    } label: {
                        HStack(spacing: 12) {
                            Circle()
                                .fill(swatchColor(for: option))
                                .frame(width: 22, height: 22)
                                .overlay(Circle().stroke(theme.border, lineWidth: 1))

                            VStack(alignment: .leading, spacing: 2) {
                                Text(option.title)
                                    .font(theme.font(size: 15, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text(option.caption)
                                    .font(theme.font(size: 12))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()
                            Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(selected ? theme.accent : theme.secondaryText)
                        }
                        .padding(14)
                        .derbisCard(elevated: selected)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var playerDeckSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Now Playing decks")

            VStack(spacing: 10) {
                ForEach(DerbisPlayerDeck.allCases) { deck in
                    let selected = theme.playerDeck == deck
                    Button {
                        haptics.playSelectionTick()
                        withAnimation(.easeInOut(duration: 0.2)) {
                            theme.playerDeck = deck
                        }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: deck.icon)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(selected ? theme.onAccentText : theme.accent)
                                .frame(width: 34, height: 34)
                                .background(
                                    RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 10, style: .continuous)
                                        .fill(selected ? theme.accent : theme.accent.opacity(0.14))
                                )

                            VStack(alignment: .leading, spacing: 2) {
                                Text(deck.title)
                                    .font(theme.font(size: 15, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text(deck.subtitle)
                                    .font(theme.font(size: 12))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()
                            Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(selected ? theme.accent : theme.secondaryText)
                        }
                        .padding(14)
                        .derbisCard(elevated: selected)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var glassBackgroundSwatches: [(Int, Int)] {
        [(0, 55), (28, 55), (50, 52), (150, 45), (190, 50), (265, 62), (330, 58)]
    }

    private func saveGlassBackgroundImage(_ data: Data) {
        guard let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            statusBanner = "Couldn't save the image"
            return
        }
        let target = documents.appendingPathComponent("glass_background.jpg")
        do {
            try data.write(to: target, options: .atomic)
            withAnimation {
                theme.glassBackgroundImagePath = target.path
                theme.glassBackgroundMode = .customImage
            }
        } catch {
            statusBanner = "Couldn't save the image"
        }
    }

    private func removeGlassBackgroundImage() {
        let path = theme.glassBackgroundImagePath
        if !path.isEmpty {
            try? FileManager.default.removeItem(atPath: path)
        }
        theme.glassBackgroundImagePath = ""
        theme.glassBackgroundMode = .glow
    }

    private var liquidGlassSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Dock & Glass Chrome")

            VStack(alignment: .leading, spacing: 14) {
                Toggle(isOn: $theme.liquidGlassEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Optical Glass Dock (Optional)")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Replaces the Derbis Unified Dock with translucent optical glass lenses")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()
                .disabled(theme.skin == .liquidGlass)

                Divider()

                VStack(alignment: .leading, spacing: 8) {
                    Text("Optical Lens")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Text("How much the material refracts, disperses and catches light along its rim")
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)

                    HStack(spacing: 8) {
                        ForEach(DerbisGlassEffects.allCases) { effect in
                            let active = theme.glassEffects == effect
                            Button {
                                haptics.playSelectionTick()
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    theme.glassEffects = effect
                                }
                            } label: {
                                Text(effect.title)
                                    .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                                    .foregroundColor(active ? theme.onAccentText : theme.primaryText)
                                    .padding(.horizontal, 13)
                                    .padding(.vertical, 8)
                                    .derbisGlassCapsule(prominent: active)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    Text(theme.glassEffects.caption)
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                }

                Divider()

                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Frost Density")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Text(theme.glassTransparency <= 16 ? "Opaque" : "\(Int(theme.glassTransparency))%")
                            .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                            .foregroundColor(theme.accent)
                    }
                    Text("Lower values thicken the frozen film and keep type readable over busy artwork")
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                    Slider(value: $theme.glassTransparency, in: 0...100, step: 5)
                        .tint(theme.accent)
                }

                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Refraction Strength")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Text("\(Int(theme.glassRefraction))%")
                            .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                            .foregroundColor(theme.accent)
                    }
                    Slider(value: $theme.glassRefraction, in: 0...100, step: 5)
                        .tint(theme.accent)
                }

                Divider()

                VStack(alignment: .leading, spacing: 8) {
                    Text("Background")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Text("What fills the page behind the glass")
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)

                    Picker("Mode", selection: $theme.glassBackgroundMode) {
                        ForEach(DerbisBackgroundMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)

                    if theme.glassBackgroundMode != .customImage {
                        HStack {
                            Text("Intensity")
                                .font(theme.font(size: 13, weight: .medium))
                                .foregroundColor(theme.primaryText)
                            Spacer()
                            Text("\(Int(theme.glassBackgroundIntensity))%")
                                .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                                .foregroundColor(theme.accent)
                        }
                        Slider(value: $theme.glassBackgroundIntensity, in: 20...100, step: 5)
                            .tint(theme.accent)
                    }

                    if theme.glassBackgroundMode == .artworkBlur || theme.glassBackgroundMode == .customImage {
                        HStack {
                            Text("Blur")
                                .font(theme.font(size: 13, weight: .medium))
                                .foregroundColor(theme.primaryText)
                            Spacer()
                            Text("\(Int(theme.glassBackgroundBlur))px")
                                .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                                .foregroundColor(theme.accent)
                        }
                        Slider(value: $theme.glassBackgroundBlur, in: 0...80, step: 5)
                            .tint(theme.accent)
                    }

                    HStack {
                        Text("Brightness")
                            .font(theme.font(size: 13, weight: .medium))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Text("\(Int(theme.glassBackgroundBrightness))%")
                            .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                            .foregroundColor(theme.accent)
                    }
                    Slider(value: $theme.glassBackgroundBrightness, in: 15...100, step: 5)
                        .tint(theme.accent)

                    if theme.glassBackgroundMode == .animated {
                        Toggle(isOn: $theme.glassBackgroundMotion) {
                            Text("Drift motion")
                                .font(theme.font(size: 13, weight: .medium))
                                .foregroundColor(theme.primaryText)
                        }
                        .derbisToggleTint()
                    }

                    if theme.glassBackgroundMode == .customImage {
                        HStack(spacing: 8) {
                            PhotosPicker(selection: $pickedGlassBackground, matching: .images) {
                                Label(theme.glassBackgroundImagePath.isEmpty ? "Choose image" : "Change image",
                                      systemImage: "photo")
                                    .font(theme.font(size: 12.5, weight: .semibold))
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(theme.accent)
                            if !theme.glassBackgroundImagePath.isEmpty {
                                Button("Remove", role: .destructive) {
                                    removeGlassBackgroundImage()
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    } else {
                        Text("Accent color")
                            .font(theme.font(size: 13, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                            .padding(.top, 2)
                        HStack(spacing: 8) {
                            Button {
                                haptics.playSelectionTick()
                                theme.glassAccentHue = -1
                            } label: {
                                Circle()
                                    .fill(AngularGradient(
                                        colors: [.orange, .yellow, .green, .cyan, .purple, .pink, .orange],
                                        center: .center
                                    ))
                                    .frame(width: 28, height: 28)
                                    .overlay(
                                        Circle().stroke(theme.glassAccentHue < 0 ? Color.white : Color.white.opacity(0.25),
                                                        lineWidth: theme.glassAccentHue < 0 ? 2.5 : 1)
                                    )
                            }
                            .buttonStyle(.plain)
                            ForEach(glassBackgroundSwatches, id: \.0) { swatch in
                                let hue = swatch.0
                                let lightness = swatch.1
                                let selected = theme.glassAccentHue == hue && Int(theme.glassAccentLightness) == lightness
                                Button {
                                    haptics.playSelectionTick()
                                    theme.glassAccentHue = hue
                                    theme.glassAccentLightness = Double(lightness)
                                } label: {
                                    Circle()
                                        .fill(Color(hue: Double(hue) / 360.0, saturation: 0.85, brightness: Double(lightness) / 100.0))
                                        .frame(width: 28, height: 28)
                                        .overlay(
                                            Circle().stroke(selected ? Color.white : Color.white.opacity(0.25),
                                                            lineWidth: selected ? 2.5 : 1)
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        if theme.glassAccentHue < 0 {
                            Text("Auto — matches the playing cover artwork")
                                .font(theme.font(size: 11))
                                .foregroundColor(theme.secondaryText)
                        }
                    }
                }
                .onChange(of: pickedGlassBackground) { item in
                    guard let item else { return }
                    Task {
                        if let data = try? await item.loadTransferable(type: Data.self) {
                            saveGlassBackgroundImage(data)
                        }
                    }
                }

                Divider()

                Toggle(isOn: $theme.ambientGlassBackdropEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Ambient Cover Aura Behind Pages")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Subtle album-art glow across Home, Search & Library for glass refraction")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.coverFollowsAccent) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Follow Album Cover Accent")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Adapts active highlights and glass tints to the playing song's cover")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.tabBarMinimizesOnScroll) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Dockable Compact Glass Tab Bar")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Swipe down or tap the corner icon on the tab bar to dock with the mini-player")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.hideTabLabels) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Hide Tab Bar Labels")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Clean icon-only Liquid Glass tab bar; hold Home anytime to jump to Settings")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Button {
                    haptics.playSelectionTick()
                    showingTabBarEditor = true
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "square.grid.2x2.fill")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(theme.accent)
                            .frame(width: 36, height: 36)
                            .background(Circle().fill(theme.accent.opacity(0.16)))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Tab Bar")
                                .font(theme.font(size: 14, weight: .semibold))
                                .foregroundColor(theme.primaryText)
                            Text("Which tabs appear, in what order, icons or names, and your own custom tabs")
                                .font(theme.font(size: 12))
                                .foregroundColor(theme.secondaryText)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(theme.secondaryText)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                Divider()

                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Mini-Player Progress Thickness")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Text(theme.miniPlayerProgressThickness <= 0.1 ? "Hidden" : String(format: "%.1f pt", theme.miniPlayerProgressThickness))
                            .font(theme.font(size: 12.5, weight: .bold).monospacedDigit())
                            .foregroundColor(theme.accent)
                    }
                    Slider(value: $theme.miniPlayerProgressThickness, in: 0.0...5.0, step: 0.5)
                        .tint(theme.accent)
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var typographySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Typography & Custom Fonts")

            VStack(alignment: .leading, spacing: 10) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(DerbisFontChoice.allCases) { choice in
                            let active = theme.fontChoice == choice
                            Button {
                                haptics.playSelectionTick()
                                if choice == .customFile && theme.customFontPostScriptName.isEmpty {
                                    activeFileImportKind = .customFont
                                    showingFileImporter = true
                                } else {
                                    theme.fontChoice = choice
                                }
                            } label: {
                                Text(choice.title)
                                    .font(theme.font(size: 13, weight: .bold))
                                    .foregroundColor(active ? .white : theme.primaryText)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 10)
                                    .background(
                                        RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                            .fill(active ? theme.accent : theme.surface)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                                    .stroke(theme.border, lineWidth: theme.isPixelSkin ? 2 : 1)
                                            )
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                HStack(spacing: 10) {
                    Button {
                        activeFileImportKind = .customFont
                        showingFileImporter = true
                    } label: {
                        Label(
                            theme.customFontPostScriptName.isEmpty ? "Import .ttf / .otf Font File…" : "Change Font (\(theme.customFontPostScriptName))",
                            systemImage: "textformat.alt"
                        )
                        .font(theme.font(size: 12.5, weight: .bold))
                        .foregroundColor(theme.accent)
                    }
                    .buttonStyle(.plain)

                    Spacer()
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var derbisListeningStatsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Listening Stats & History")

            Button {
                haptics.playControlTap(style: .light)
                showingListeningStatsSheet = true
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "chart.bar.doc.horizontal.fill")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(theme.accent))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("On-Device Listening Stats")
                            .font(theme.font(size: 15, weight: .bold))
                            .foregroundColor(theme.primaryText)
                        Text("Past week, month, year & all-time Top 25 + Spotify StreamingHistory JSON import")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                            .lineLimit(2)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
                .padding(14)
                .derbisCard(elevated: true)
            }
            .buttonStyle(.plain)
        }
    }

    private var autoMixSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("AutoMix & Smart Crossfade")

            VStack(alignment: .leading, spacing: 14) {
                Toggle(isOn: $theme.autoMixEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("AutoMix Seamless Transitions")
                            .font(theme.font(size: 14.5, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Equal-power volume curve blending the outro into the next song's intro")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                if theme.autoMixEnabled {
                    Divider()

                    HStack(spacing: 8) {
                        ForEach(AutoMixStyle.allCases) { style in
                            let selected = theme.autoMixStyle == style
                            Button {
                                haptics.playSelectionTick()
                                theme.autoMixStyle = style
                            } label: {
                                Text(style.title)
                                    .font(theme.font(size: 12.5, weight: .bold))
                                    .foregroundColor(selected ? .white : theme.primaryText)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 8)
                                    .derbisGlassCapsule(prominent: selected)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("Transition Overlap")
                                .font(theme.font(size: 13.5, weight: .semibold))
                                .foregroundColor(theme.primaryText)
                            Spacer()
                            Text(String(format: "%.0fs", theme.autoMixSeconds))
                                .font(theme.font(size: 13, weight: .bold).monospacedDigit())
                                .foregroundColor(theme.accent)
                        }
                        Slider(value: $theme.autoMixSeconds, in: 1...12, step: 1)
                            .tint(theme.accent)
                    }

                    Button {
                        haptics.playControlTap(style: .medium)
                        let jumped = player.previewAutoMixTransition()
                        statusBanner = jumped
                            ? "Jumped to 15s before song end to preview AutoMix"
                            : "Start playing a song with 2+ tracks in queue to preview AutoMix"
                    } label: {
                        Label("Preview Next Transition", systemImage: "waveform.path.badge.plus")
                            .font(theme.font(size: 13, weight: .bold))
                            .foregroundColor(theme.accent)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var derbisLyricsIntelligenceSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Lyrics Intelligence & Genius")

            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Lyrics Source Priority (Tap ↑ to promote)")
                        .font(theme.font(size: 13.5, weight: .semibold))
                        .foregroundColor(theme.primaryText)

                    ForEach(Array(theme.lyricsSourcePriority.enumerated()), id: \.element.id) { idx, source in
                        HStack(spacing: 10) {
                            Text("#\(idx + 1)")
                                .font(theme.font(size: 12, weight: .bold).monospacedDigit())
                                .foregroundColor(idx == 0 ? theme.accent : theme.secondaryText)
                                .frame(width: 24, alignment: .leading)

                            VStack(alignment: .leading, spacing: 1) {
                                Text(source.title)
                                    .font(theme.font(size: 13.5, weight: .semibold))
                                    .foregroundColor(theme.primaryText)
                                Text(source.subtitle)
                                    .font(theme.font(size: 11))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()

                            if idx > 0 {
                                Button {
                                    haptics.playSelectionTick()
                                    var order = theme.lyricsSourcePriority
                                    order.swapAt(idx, idx - 1)
                                    theme.lyricsSourcePriority = order
                                } label: {
                                    Image(systemName: "arrow.up.circle.fill")
                                        .font(.system(size: 18))
                                        .foregroundColor(theme.accent)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                Divider()

                HStack {
                    Text("Live Translation Target")
                        .font(theme.font(size: 13.5, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Picker("Translation Language", selection: $theme.lyricsTranslationLanguage) {
                        ForEach(LyricsTranslationLanguage.allCases) { lang in
                            Text(lang.title).tag(lang)
                        }
                    }
                    .tint(theme.accent)
                }
                .onChange(of: theme.lyricsTranslationLanguage) { newLang in
                    if theme.showLyricsTranslation {
                        player.ensureLyricsTranslated(to: newLang)
                    }
                }

                Divider()

                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Genius Line Meanings")
                            .font(theme.font(size: 13.5, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Picker("Genius Filter", selection: $theme.geniusMeaningsFilter) {
                            ForEach(GeniusMeaningFilter.allCases) { filter in
                                Text(filter.title).tag(filter)
                            }
                        }
                        .tint(theme.accent)
                    }
                    .onChange(of: theme.geniusMeaningsFilter) { newFilter in
                        player.refreshGeniusMeanings(filter: newFilter)
                    }

                    Toggle("Show Inline Genius Meaning Badges", isOn: $theme.showGeniusMeaningMarkers)
                        .font(theme.font(size: 13, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                        .derbisToggleTint()
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var motionCanvasSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Motion Canvas & animated art")

            VStack(spacing: 14) {
                Toggle(isOn: $theme.motionCanvasEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Spotify Canvas & Apple Motion")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Loops 9:16 Spotify Canvas MP4s and Apple Music animated covers in Now Playing")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                if theme.motionCanvasEnabled {
                    Divider()

                    HStack(spacing: 8) {
                        ForEach(MotionCanvasStyle.allCases) { styleOption in
                            let selected = theme.motionCanvasStyle == styleOption
                            Button {
                                theme.motionCanvasStyle = styleOption
                            } label: {
                                Text(styleOption.title)
                                    .font(theme.font(size: 13, weight: .bold))
                                    .foregroundColor(selected ? .white : theme.primaryText)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 9)
                                    .background(
                                        RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                            .fill(selected ? theme.accent : theme.elevatedSurface)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    Divider()

                    Button {
                        showingSpotifyCanvasSheet = true
                    } label: {
                        HStack(spacing: 10) {
                            Circle()
                                .fill(spotifySession.hasSession ? Color(hex: 0x1ED760) : theme.secondaryText.opacity(0.45))
                                .frame(width: 9, height: 9)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(spotifySession.hasSession ? "Spotify Canvas connected" : "Connect Spotify for full Canvas catalog")
                                    .font(theme.font(size: 14, weight: .semibold))
                                    .foregroundColor(theme.primaryText)
                                Text(
                                    spotifySession.hasSession
                                        ? "Active session (\(spotifySession.maskedPreview)) · Tap to manage or test"
                                        : "Optional — public Canvas & Apple Motion work automatically"
                                )
                                .font(theme.font(size: 12))
                                .foregroundColor(theme.secondaryText)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(theme.secondaryText)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var playbackSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Playback, Karaoke & Lock Screen")

            VStack(spacing: 14) {
                Toggle(isOn: $theme.highQualityAudio) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Prefer highest bitrate AAC")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Selects 256 kbps AAC / raw lossless streams when available")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.endlessRadioEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Endless Listening (Song Radio)")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Persistent RDAMVM song-radio tuner with rolling refills & infinite queue scroll")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.autoLoadLyrics) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Auto-load word-synced TTML & LRC lyrics")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Fetches syllable-synced TTML duets, backing vocals & LRCLIB")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()

                Divider()

                Toggle("Show Romanized Pronunciation", isOn: $theme.showLyricsRomanization)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .derbisToggleTint()

                Toggle("Show Translated Lyrics", isOn: $theme.showLyricsTranslation)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .derbisToggleTint()

                Toggle("Auto-Hide Transport Controls in Lyrics", isOn: $theme.autoHideLyricsControls)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .derbisToggleTint()

                Divider()

                Toggle(isOn: $theme.lockScreenLiveLyrics) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Lock Screen Live Subtitle Lyrics")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Streams real-time synced lyric phrases to the iOS Lock Screen & Dynamic Island")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()
                .onChange(of: theme.lockScreenLiveLyrics) { _ in
                    player.refreshNowPlayingLockScreenPreferences()
                }

                Toggle(isOn: $theme.lockScreenLyricArt) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Lock Screen Lyric Artwork Card")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Renders active & upcoming lyrics onto Lock Screen album art")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .derbisToggleTint()
                .onChange(of: theme.lockScreenLyricArt) { _ in
                    player.refreshNowPlayingLockScreenPreferences()
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var blockedArtistsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Blocked artists (\(library.blockedArtists.count))")

            VStack(spacing: 10) {
                ForEach(library.blockedArtists, id: \.self) { artist in
                    HStack {
                        Image(systemName: "hand.raised.slash.fill")
                            .font(.system(size: 13))
                            .foregroundColor(theme.accent)
                        Text(artist)
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Spacer()
                        Button("Unblock") {
                            haptics.playControlTap(style: .light)
                            library.unblockArtist(artist)
                        }
                        .font(theme.font(size: 12, weight: .bold))
                        .foregroundColor(theme.accent)
                    }
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var subsonicSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Navidrome / Subsonic server")

            VStack(alignment: .leading, spacing: 12) {
                TextField("Server URL (https://music.example.com)", text: $serverURLInput)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .font(theme.font(size: 14))
                    .padding(10)
                    .background(theme.elevatedSurface.cornerRadius(theme.isPixelSkin ? 0 : 8))

                TextField("Username", text: $usernameInput)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .font(theme.font(size: 14))
                    .padding(10)
                    .background(theme.elevatedSurface.cornerRadius(theme.isPixelSkin ? 0 : 8))

                SecureField("Password", text: $passwordInput)
                    .font(theme.font(size: 14))
                    .padding(10)
                    .background(theme.elevatedSurface.cornerRadius(theme.isPixelSkin ? 0 : 8))

                Toggle("Stream raw lossless (FLAC / ALAC)", isOn: $preferRawLossless)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .derbisToggleTint()

                HStack(spacing: 10) {
                    Button {
                        isTestingServer = true
                        Task {
                            let ok = await subsonic.saveCredentials(
                                serverURL: serverURLInput,
                                username: usernameInput,
                                password: passwordInput,
                                preferRawLossless: preferRawLossless
                            )
                            isTestingServer = false
                            statusBanner = ok ? "Connected to \(subsonic.serverStatus)" : subsonic.serverStatus
                        }
                    } label: {
                        HStack(spacing: 6) {
                            if isTestingServer {
                                ProgressView().tint(.white)
                            }
                            Text(subsonic.isConnected ? "Update & verify" : "Connect server")
                                .font(theme.font(size: 13, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                .fill(theme.accent)
                        )
                    }
                    .disabled(isTestingServer)

                    if subsonic.isConnected {
                        Button("Disconnect", role: .destructive) {
                            subsonic.disconnect()
                            serverURLInput = ""
                            usernameInput = ""
                            passwordInput = ""
                        }
                        .font(theme.font(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                    }

                    Spacer()
                }

                Text(subsonic.serverStatus)
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var storageAndBackupSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Storage & backup")

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("On-device audio storage")
                        .font(theme.font(size: 14, weight: .medium))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Text(library.offlineStorageSizeString())
                        .font(theme.font(size: 14, weight: .bold))
                        .foregroundColor(theme.accent)
                }

                Divider()

                HStack(spacing: 12) {
                    if let backupURL = library.exportBackupURL() {
                        ShareLink(item: backupURL) {
                            Label("Export backup", systemImage: "square.and.arrow.up")
                                .font(theme.font(size: 13, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .derbisCard(elevated: true)
                        }
                    }

                    Button {
                        activeFileImportKind = .backupJSON
                        showingFileImporter = true
                    } label: {
                        Label("Restore backup", systemImage: "square.and.arrow.down")
                            .font(theme.font(size: 13, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .derbisCard(elevated: true)
                    }
                }

                Button {
                    Task {
                        await LyricsService.shared.clearCache()
                        statusBanner = "Lyrics cache cleared"
                    }
                } label: {
                    Text("Clear cached lyrics")
                        .font(theme.font(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle("About")

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    DerbisDiamondMark(color: theme.accent, size: 18)
                    Text("Derbis for iOS · Unofficial")
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.primaryText)
                }
                Text("Version 1.8.0 (Unofficial native iOS build · KSign ready)")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
                Text("Linear Glass · Flow & Vinyl Decks · Endless Listening · 32-bit Mix Lab · Sing DSP · AutoMix · Jam · GPL-3.0")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
                Text("Fonts: Outfit, Inter, Manrope, Poppins, Montserrat, Nunito, Roboto, Pixelify Sans — all Open Font License 1.1 (see Resources/Fonts/licenses)")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .derbisCard()
        }
    }

    @ViewBuilder
    private var statusBannerView: some View {
        if let banner = statusBanner {
            Text(banner)
                .font(theme.font(size: 13, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                        .fill(theme.accent)
                )
        }
    }

    private func categoryRow(_ category: SettingsCategory) -> some View {
        NavigationLink(value: category) {
            HStack(spacing: 12) {
                Image(systemName: category.icon)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(
                        RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 10, style: .continuous)
                            .fill(theme.accent)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text(category.title)
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Text(category.subtitle)
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                        .lineLimit(1)
                }
                Spacer(minLength: 4)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundColor(theme.secondaryText)
            }
            .padding(12)
            .derbisCard()
        }
        .buttonStyle(.plain)
    }

    /// The pushed page for one category. The sections themselves are unchanged — they only
    /// moved off the flat list onto their category page.
    @ViewBuilder
    private func categoryPage(_ category: SettingsCategory) -> some View {
        ZStack {
            PixelGraphPaperBackground()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    statusBannerView

                    switch category {
                    case .appearance:
                        skinSection
                        playerDeckSection
                        liquidGlassSection
                        playerBackdropAndGesturesSection
                        motionCanvasSection
                    case .fonts:
                        typographySection
                    case .audio:
                        audioStudioAndHapticsSection
                        autoMixSection
                        playbackSection
                    case .listening:
                        derbisListeningStatsSection
                        derbisLyricsIntelligenceSection
                        if !library.blockedArtists.isEmpty {
                            blockedArtistsSection
                        }
                    case .accountsSync:
                        googleAccountSection
                        jamListenTogetherSection
                        subsonicSection
                    case .storageBackup:
                        storageAndBackupSection
                    case .about:
                        aboutSection
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 6)
                .padding(.bottom, 24)
                .derbisBottomBarClearance()
            }
            .derbisAutoMinimizeTabBarOnScroll()
        }
        .navigationTitle(category.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(theme.isTorqueSkin ? text.uppercased() : text)
            .font(theme.font(size: 16, weight: .bold))
            .foregroundColor(theme.primaryText)
    }

    private func swatchColor(for skin: DerbisSkin) -> Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x2997FF)
        case .pixelUI: return Color(hex: 0x9E1B44)
        case .torque: return Color(hex: 0x22D3EE)
        case .ivory: return Color(hex: 0xC2410C)
        case .lateNight: return Color(hex: 0xF59E0B)
        case .liquidGlass: return Color(hex: 0x38BDF8)
        case .appleMusic13: return Color(hex: 0xFC3C44)
        }
    }
}
