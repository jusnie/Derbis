import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var subsonic: SubsonicService

    @State private var serverURLInput: String = ""
    @State private var usernameInput: String = ""
    @State private var passwordInput: String = ""
    @State private var preferRawLossless: Bool = true
    @State private var isTestingServer: Bool = false
    @State private var showingBackupImporter: Bool = false
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

                        skinSection
                        typographySection
                        playbackSection
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
        }
    }

    private var skinSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Appearance & skins")

            VStack(spacing: 10) {
                ForEach(DerbisSkin.allCases) { option in
                    let selected = theme.skin == option
                    Button {
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

    private var playbackSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Playback & lyrics")

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
                        Text("Auto-load synced lyrics")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Fetches time-synced LRCLIB lines on every track change")
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
                Text("Native SwiftUI & AVPlayer · Background Audio · AirPlay · LRCLIB · Subsonic · GPL-3.0")
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
