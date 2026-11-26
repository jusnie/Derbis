import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var subsonic: SubsonicService
    @ObservedObject private var spotifySession = SpotifySessionStore.shared
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @ObservedObject private var haptics = HapticsAndGesturesService.shared

    @State private var serverURLInput: String = ""
    @State private var usernameInput: String = ""
    @State private var passwordInput: String = ""
    @State private var preferRawLossless: Bool = true
    @State private var isTestingServer: Bool = false
    @State private var showingBackupImporter: Bool = false
    @State private var showingSpotifyCanvasSheet: Bool = false
    @State private var showingAudioStudioSheet: Bool = false
    @State private var showingGoogleAccountSheet: Bool = false
    @State private var statusBanner: String?

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 22) {
                        Text("Settings")
                            .font(theme.font(size: 26, weight: .bold))
                            .foregroundColor(theme.primaryText)

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

                        googleAccountSection
                        audioStudioAndHapticsSection
                        skinSection
                        playerBackdropAndGesturesSection
                        typographySection
                        motionCanvasSection
                        playbackSection
                        if !library.blockedArtists.isEmpty {
                            blockedArtistsSection
                        }
                        subsonicSection
                        storageAndBackupSection
                        aboutSection
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 120)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .onAppear {
                serverURLInput = subsonic.config.serverURL
                usernameInput = subsonic.config.username
                passwordInput = subsonic.loadPassword()
                preferRawLossless = subsonic.config.preferRawLossless
            }
            .fileImporter(
                isPresented: $showingBackupImporter,
                allowedContentTypes: [.json],
                allowsMultipleSelection: false
            ) { result in
                if case .success(let urls) = result, let first = urls.first {
                    let ok = library.importBackup(from: first)
                    statusBanner = ok ? "Library backup restored" : "Invalid backup file"
                }
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
            .sheet(isPresented: $showingGoogleAccountSheet) {
                GoogleAccountSheet()
                    .environmentObject(player)
                    .environmentObject(library)
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
                .tint(theme.accent)
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
                .tint(theme.accent)
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

    private var typographySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Typography")

            HStack(spacing: 8) {
                ForEach(DerbisFontChoice.allCases) { choice in
                    let active = theme.fontChoice == choice
                    Button {
                        haptics.playSelectionTick()
                        theme.fontChoice = choice
                    } label: {
                        Text(choice.title)
                            .font(theme.font(size: 13, weight: .bold))
                            .foregroundColor(active ? .white : theme.primaryText)
                            .frame(maxWidth: .infinity)
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
                .tint(theme.accent)

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
                .tint(theme.accent)

                Divider()

                Toggle(isOn: $theme.endlessRadioEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Endless radio continuation")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Automatically queues related songs when your queue finishes")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .tint(theme.accent)

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
                .tint(theme.accent)

                Divider()

                Toggle("Show Romanized Pronunciation", isOn: $theme.showLyricsRomanization)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .tint(theme.accent)

                Toggle("Show Translated Lyrics", isOn: $theme.showLyricsTranslation)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .tint(theme.accent)

                Toggle("Auto-Hide Transport Controls in Lyrics", isOn: $theme.autoHideLyricsControls)
                    .font(theme.font(size: 14, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .tint(theme.accent)

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
                .tint(theme.accent)
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
                .tint(theme.accent)
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
                    .tint(theme.accent)

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
                        showingBackupImporter = true
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
                    DerbisDiamondMark(color: theme.primaryText, size: 18)
                    Text("Derbis for iOS · Unofficial")
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.primaryText)
                }
                Text("Version 1.8.0 (Unofficial native iOS build · KSign ready)")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
                Text("Word-Synced TTML · 10-Band 32-Bit DSP · Vinyl Varispeed · Music Haptics · AirPods Gestures · GPL-3.0")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .derbisCard()
        }
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(theme.font(size: 16, weight: .bold))
            .foregroundColor(theme.primaryText)
    }

    private func swatchColor(for skin: DerbisSkin) -> Color {
        switch skin {
        case .appleMusic13: return Color(hex: 0xFC3C44)
        case .pixelUI: return Color(hex: 0xFF85A2)
        case .derbisDefault: return Color(hex: 0x121218)
        case .lateNight: return Color(hex: 0xF59E0B)
        }
    }
}
