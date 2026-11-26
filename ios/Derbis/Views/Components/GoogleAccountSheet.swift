import SwiftUI
import WebKit

struct GoogleAccountAvatarBadge: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared
    var size: CGFloat = 34

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            if let account = googleSync.activeAccount {
                if let url = URL(string: account.avatarURL), !account.avatarURL.isEmpty {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: size, height: size)
                                .clipShape(Circle())
                        default:
                            initialsCircle(account.initials)
                        }
                    }
                    .frame(width: size, height: size)
                } else {
                    initialsCircle(account.initials)
                }

                Circle()
                    .fill(Color.green)
                    .frame(width: max(8, size * 0.28), height: max(8, size * 0.28))
                    .overlay(Circle().stroke(theme.surface, lineWidth: 1.5))
            } else {
                Circle()
                    .fill(theme.elevatedSurface)
                    .frame(width: size, height: size)
                    .overlay(
                        Image(systemName: "person.crop.circle")
                            .font(.system(size: size * 0.56, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                    )
                    .overlay(Circle().stroke(theme.border, lineWidth: 1))
            }
        }
    }

    private func initialsCircle(_ text: String) -> some View {
        Circle()
            .fill(theme.accent)
            .frame(width: size, height: size)
            .overlay(
                Text(text)
                    .font(theme.font(size: size * 0.44, weight: .bold))
                    .foregroundColor(.white)
            )
    }
}

struct GoogleAccountSheet: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared
    @Environment(\.dismiss) private var dismiss

    @State private var showingWebLogin = false
    @State private var showingSignOutConfirm = false
    @State private var showingManualCookie = false
    @State private var manualCookieInput = ""

    @State private var selectedCollection: GoogleCloudCollectionKind = .likes
    @State private var cloudEntries: [GoogleCloudLibraryEntry] = []
    @State private var cloudTracks: [Track] = []
    @State private var isLoadingCollection = false
    @State private var collectionError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        if let status = googleSync.statusMessage {
                            bannerCard(text: status, isError: false)
                        }
                        if let err = googleSync.errorMessage {
                            bannerCard(text: err, isError: true)
                        }

                        if googleSync.isSignedIn, let account = googleSync.activeAccount {
                            signedInProfileCard(account: account)
                            syncPreferencesCard
                            cloudBrowserSection
                        } else {
                            signedOutHeroCard
                            manualCookieFallbackCard
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle("Google Account & Sync")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .fullScreenCover(isPresented: $showingWebLogin) {
                GoogleLoginWebSheet { capturedCookie in
                    showingWebLogin = false
                    Task {
                        let ok = await googleSync.completeLogin(withCookieHeader: capturedCookie)
                        if ok {
                            HapticsAndGesturesService.shared.playSuccessFeedback()
                            await loadActiveCloudCollection()
                        }
                    }
                }
                .environmentObject(theme)
            }
            .alert("Sign out of Google?", isPresented: $showingSignOutConfirm) {
                Button("Sign Out", role: .destructive) {
                    googleSync.signOut()
                    cloudEntries = []
                    cloudTracks = []
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Your local downloads and playlists already imported onto this iPhone will stay intact.")
            }
            .task {
                if googleSync.isSignedIn && cloudTracks.isEmpty && cloudEntries.isEmpty {
                    await loadActiveCloudCollection()
                }
            }
            .onChange(of: selectedCollection) { _ in
                Task { await loadActiveCloudCollection() }
            }
            .onChange(of: googleSync.activeSession?.id) { newId in
                if newId != nil {
                    Task { await loadActiveCloudCollection() }
                }
            }
        }
    }

    private func bannerCard(text: String, isError: Bool) -> some View {
        HStack(spacing: 10) {
            Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.icloud.fill")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
            Text(text)
                .font(theme.font(size: 12.5, weight: .semibold))
                .foregroundColor(.white)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                .fill(isError ? Color.red.opacity(0.85) : theme.accent)
        )
    }

    private var signedOutHeroCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: theme.isPixelSkin ? 0 : 16, style: .continuous)
                        .fill(theme.accent.opacity(0.15))
                        .frame(width: 58, height: 58)
                    Image(systemName: "person.crop.circle.badge.checkmark")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(theme.accent)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("Sync with Google")
                        .font(theme.font(size: 20, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Text("Same account sync as Derbis on Android")
                        .font(theme.font(size: 13, weight: .medium))
                        .foregroundColor(theme.secondaryText)
                }
            }

            VStack(alignment: .leading, spacing: 10) {
                featureBullet(icon: "heart.fill", title: "Two-Way Liked Songs", detail: "Imports your liked songs and syncs new hearts back to your Google account")
                featureBullet(icon: "music.note.list", title: "Playlists & Saved Albums", detail: "Browse, stream, and auto-import your personal playlists into Library")
                featureBullet(icon: "sparkles", title: "Personalized Home & Radio", detail: "Unlocks Quick Picks, Listen Again, and Mixed For You shelves")
                featureBullet(icon: "lock.shield.fill", title: "Encrypted Keychain Storage", detail: "Session credentials stay strictly on your iPhone Keychain")
            }

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                showingWebLogin = true
            } label: {
                HStack(spacing: 10) {
                    if googleSync.isVerifyingLogin {
                        ProgressView().tint(.white)
                    } else {
                        Image(systemName: "globe")
                            .font(.system(size: 16, weight: .bold))
                    }
                    Text(googleSync.isVerifyingLogin ? "Verifying Account…" : "Continue with Google")
                        .font(theme.font(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                        .fill(theme.accent)
                )
            }
            .disabled(googleSync.isVerifyingLogin)
        }
        .padding(18)
        .derbisCard(elevated: true)
    }

    private func featureBullet(icon: String, title: String, detail: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(theme.accent)
                .frame(width: 22, alignment: .center)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(theme.font(size: 14, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Text(detail)
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }
        }
    }

    private var manualCookieFallbackCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    showingManualCookie.toggle()
                }
            } label: {
                HStack {
                    Text("Advanced: Sign in with Cookie Header")
                        .font(theme.font(size: 13, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                    Spacer()
                    Image(systemName: showingManualCookie ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .buttonStyle(.plain)

            if showingManualCookie {
                Text("Paste a music.youtube.com Cookie header containing SAPISID if you prefer manual session import.")
                    .font(theme.font(size: 11.5))
                    .foregroundColor(theme.secondaryText)

                TextField("SID=...; HSID=...; SAPISID=...", text: $manualCookieInput)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .font(.system(size: 12, design: .monospaced))
                    .padding(10)
                    .background(theme.elevatedSurface.cornerRadius(theme.isPixelSkin ? 0 : 8))

                Button {
                    let raw = manualCookieInput
                    Task {
                        let ok = await googleSync.completeLogin(withCookieHeader: raw)
                        if ok {
                            manualCookieInput = ""
                            showingManualCookie = false
                            await loadActiveCloudCollection()
                        }
                    }
                } label: {
                    Text("Verify Cookie")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(
                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                .fill(theme.accent)
                        )
                }
                .disabled(manualCookieInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || googleSync.isVerifyingLogin)
            }
        }
        .padding(14)
        .derbisCard()
    }

    private func signedInProfileCard(account: GoogleMusicAccount) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                GoogleAccountAvatarBadge(size: 54)

                VStack(alignment: .leading, spacing: 3) {
                    Text(account.name)
                        .font(theme.font(size: 18, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .lineLimit(1)

                    if !account.handle.isEmpty {
                        Text(account.handle)
                            .font(theme.font(size: 13, weight: .medium))
                            .foregroundColor(theme.secondaryText)
                            .lineLimit(1)
                    }

                    if let syncedDate = googleSync.lastSyncedAt {
                        Text("Last synced \(syncedDate.formatted(.relative(presentation: .named)))")
                            .font(theme.font(size: 11.5))
                            .foregroundColor(theme.secondaryText.opacity(0.85))
                    }
                }

                Spacer()

                if googleSync.sessions.count > 1 {
                    Menu {
                        ForEach(googleSync.sessions) { item in
                            Button {
                                googleSync.selectAccount(id: item.id)
                            } label: {
                                HStack {
                                    Text(item.account.handle.isEmpty ? item.account.name : "\(item.account.name) (\(item.account.handle))")
                                    if item.id == account.id {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                        Divider()
                        Button {
                            showingWebLogin = true
                        } label: {
                            Label("Add another account…", systemImage: "person.badge.plus")
                        }
                    } label: {
                        Image(systemName: "arrow.up.arrow.down.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(theme.accent)
                    }
                }
            }

            HStack(spacing: 10) {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                    Task {
                        await googleSync.syncAccountDataToLibrary(silent: false)
                        await loadActiveCloudCollection()
                    }
                } label: {
                    HStack(spacing: 7) {
                        if googleSync.isSyncing {
                            ProgressView().tint(.white).scaleEffect(0.8)
                        } else {
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .font(.system(size: 13, weight: .bold))
                        }
                        Text(googleSync.isSyncing ? "Syncing…" : "Sync Data Now")
                            .font(theme.font(size: 13.5, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(
                        RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                            .fill(theme.accent)
                    )
                }
                .disabled(googleSync.isSyncing)

                Button {
                    showingWebLogin = true
                } label: {
                    Image(systemName: "person.badge.plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .padding(.horizontal, 13)
                        .padding(.vertical, 11)
                        .background(
                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                .fill(theme.elevatedSurface)
                        )
                }

                Button(role: .destructive) {
                    showingSignOutConfirm = true
                } label: {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.red)
                        .padding(.horizontal, 13)
                        .padding(.vertical, 11)
                        .background(
                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                .fill(theme.elevatedSurface)
                        )
                }
            }
        }
        .padding(16)
        .derbisCard(elevated: true)
    }

    private var syncPreferencesCard: some View {
        VStack(alignment: .leading, spacing: 11) {
            Text("Sync settings")
                .font(theme.font(size: 15, weight: .bold))
                .foregroundColor(theme.primaryText)

            Toggle("Two-Way Liked Songs Sync", isOn: $googleSync.twoWayLikesSync)
                .font(theme.font(size: 13.5, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)

            Toggle("Auto-Import Personal Playlists", isOn: $googleSync.autoImportPlaylists)
                .font(theme.font(size: 13.5, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)

            Toggle("Personalized Home Feed", isOn: $googleSync.personalizedHomeEnabled)
                .font(theme.font(size: 13.5, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)

            Toggle("Sync Listening History", isOn: $googleSync.syncListeningHistory)
                .font(theme.font(size: 13.5, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)
        }
        .padding(14)
        .derbisCard()
    }

    private var cloudBrowserSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Your Google Music cloud")
                .font(theme.font(size: 17, weight: .bold))
                .foregroundColor(theme.primaryText)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(GoogleCloudCollectionKind.allCases) { kind in
                        let selected = selectedCollection == kind
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedCollection = kind
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: kind.systemImage)
                                    .font(.system(size: 11, weight: .semibold))
                                Text(kind.rawValue)
                                    .font(theme.font(size: 13, weight: selected ? .bold : .medium))
                            }
                            .foregroundColor(selected ? .white : theme.primaryText)
                            .padding(.horizontal, 13)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(selected ? theme.accent : theme.surface)
                                    .overlay(Capsule().stroke(theme.border, lineWidth: 1))
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if isLoadingCollection {
                HStack {
                    Spacer()
                    ProgressView().tint(theme.accent)
                        .padding(.vertical, 28)
                    Spacer()
                }
            } else if let err = collectionError {
                VStack(spacing: 10) {
                    Text(err)
                        .font(theme.font(size: 13))
                        .foregroundColor(theme.secondaryText)
                        .multilineTextAlignment(.center)
                    Button("Retry") {
                        Task { await loadActiveCloudCollection() }
                    }
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.accent)
                }
                .frame(maxWidth: .infinity)
                .padding(20)
                .derbisCard()
            } else if selectedCollection == .likes || selectedCollection == .history {
                cloudTracksListView
            } else {
                cloudEntriesGridView
            }
        }
    }

    private var cloudTracksListView: some View {
        Group {
            if cloudTracks.isEmpty {
                Text("No tracks found in \(selectedCollection.rawValue.lowercased()).")
                    .font(theme.font(size: 13))
                    .foregroundColor(theme.secondaryText)
                    .frame(maxWidth: .infinity)
                    .padding(24)
                    .derbisCard()
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("\(cloudTracks.count) tracks")
                            .font(theme.font(size: 13, weight: .semibold))
                            .foregroundColor(theme.secondaryText)
                        Spacer()
                        Button {
                            if let first = cloudTracks.first {
                                player.play(track: first, in: cloudTracks)
                            }
                        } label: {
                            Label("Play All", systemImage: "play.fill")
                                .font(theme.font(size: 13, weight: .bold))
                                .foregroundColor(theme.accent)
                        }
                        .padding(.trailing, 10)

                        Button {
                            let shuffled = cloudTracks.shuffled()
                            if let first = shuffled.first {
                                player.play(track: first, in: shuffled)
                            }
                        } label: {
                            Label("Shuffle", systemImage: "shuffle")
                                .font(theme.font(size: 13, weight: .bold))
                                .foregroundColor(theme.primaryText)
                        }
                    }

                    VStack(spacing: 2) {
                        ForEach(cloudTracks) { track in
                            TrackRowView(track: track, contextQueue: cloudTracks)
                        }
                    }
                    .padding(.vertical, 6)
                    .derbisCard()
                }
            }
        }
    }

    private var cloudEntriesGridView: some View {
        Group {
            if cloudEntries.isEmpty {
                Text("No saved \(selectedCollection.rawValue.lowercased()) found on this Google account.")
                    .font(theme.font(size: 13))
                    .foregroundColor(theme.secondaryText)
                    .frame(maxWidth: .infinity)
                    .padding(24)
                    .derbisCard()
            } else {
                let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(cloudEntries) { entry in
                        NavigationLink {
                            GoogleCloudEntryDetailView(entry: entry)
                                .environmentObject(player)
                                .environmentObject(library)
                                .environmentObject(theme)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                TrackArtworkView(
                                    url: URL(string: entry.thumbnailURL),
                                    size: 148,
                                    cornerRadius: entry.collection == .artists ? 74 : theme.artworkRadius
                                )
                                .frame(maxWidth: .infinity)

                                Text(entry.title)
                                    .font(theme.font(size: 14, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                    .lineLimit(1)

                                Text(entry.subtitle.isEmpty ? entry.collection.rawValue : entry.subtitle)
                                    .font(theme.font(size: 12))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .derbisCard()
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func loadActiveCloudCollection() async {
        guard googleSync.isSignedIn else { return }
        isLoadingCollection = true
        collectionError = nil
        do {
            let result = try await googleSync.fetchCloudCollection(selectedCollection)
            cloudEntries = result.entries
            cloudTracks = result.tracks
        } catch {
            collectionError = error.localizedDescription
        }
        isLoadingCollection = false
    }
}

struct GoogleCloudEntryDetailView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var googleSync = GoogleAccountSyncService.shared

    let entry: GoogleCloudLibraryEntry

    @State private var tracks: [Track] = []
    @State private var isLoading = true
    @State private var importedBanner: String?

    var body: some View {
        ZStack {
            PixelGraphPaperBackground()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 14) {
                        TrackArtworkView(url: URL(string: entry.thumbnailURL), size: 96)

                        VStack(alignment: .leading, spacing: 6) {
                            Text(entry.title)
                                .font(theme.font(size: 20, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(2)

                            if !entry.subtitle.isEmpty {
                                Text(entry.subtitle)
                                    .font(theme.font(size: 13))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }

                            HStack(spacing: 8) {
                                Button {
                                    if let first = tracks.first {
                                        player.play(track: first, in: tracks)
                                    }
                                } label: {
                                    Label("Play", systemImage: "play.fill")
                                        .font(theme.font(size: 13, weight: .bold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(Capsule().fill(theme.accent))
                                }
                                .disabled(tracks.isEmpty)

                                Button {
                                    guard let accId = googleSync.activeAccount?.id else { return }
                                    let count = library.upsertCloudPlaylist(
                                        cloudId: "\(accId):\(entry.browseId)",
                                        title: entry.title,
                                        tracks: tracks
                                    )
                                    HapticsAndGesturesService.shared.playSuccessFeedback()
                                    importedBanner = count > 0
                                        ? "Imported \(count) tracks into Library Playlists"
                                        : "Playlist is already up to date in Library"
                                } label: {
                                    Label("Save to Library", systemImage: "arrow.down.to.line")
                                        .font(theme.font(size: 12.5, weight: .bold))
                                        .foregroundColor(theme.primaryText)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .derbisCard(elevated: true)
                                }
                                .disabled(tracks.isEmpty)
                            }
                        }
                    }

                    if let msg = importedBanner {
                        Text(msg)
                            .font(theme.font(size: 12.5, weight: .semibold))
                            .foregroundColor(theme.accent)
                    }

                    if isLoading {
                        HStack {
                            Spacer()
                            ProgressView().tint(theme.accent).padding(.vertical, 30)
                            Spacer()
                        }
                    } else if tracks.isEmpty {
                        Text("No playable tracks found.")
                            .font(theme.font(size: 14))
                            .foregroundColor(theme.secondaryText)
                    } else {
                        VStack(spacing: 2) {
                            ForEach(tracks) { track in
                                TrackRowView(track: track, contextQueue: tracks)
                            }
                        }
                        .padding(.vertical, 6)
                        .derbisCard()
                    }
                }
                .padding(16)
                .padding(.bottom, 80)
            }
        }
        .navigationTitle(entry.title)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            isLoading = true
            if entry.collection == .artists {
                tracks = (try? await InnerTubeService.shared.searchTracks(query: entry.title)) ?? []
            } else {
                tracks = (try? await googleSync.fetchEntryDetailTracks(entry)) ?? []
            }
            isLoading = false
        }
    }
}

struct GoogleLoginWebSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    let onCapturedCookie: (String) -> Void

    @State private var isLoading = true
    @State private var pageTitle = "Sign in with Google"

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.linear)
                        .tint(theme.accent)
                }

                GoogleAuthWebViewRepresentable(
                    isLoading: $isLoading,
                    pageTitle: $pageTitle,
                    onCapturedCookie: onCapturedCookie
                )
                .ignoresSafeArea(edges: .bottom)
            }
            .navigationTitle(pageTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }
}

private struct GoogleAuthWebViewRepresentable: UIViewRepresentable {
    @Binding var isLoading: Bool
    @Binding var pageTitle: String
    let onCapturedCookie: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            isLoading: $isLoading,
            pageTitle: $pageTitle,
            onCapturedCookie: onCapturedCookie
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .nonPersistent()
        config.preferences.javaScriptCanOpenWindowsAutomatically = true

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.customUserAgent = GoogleAccountSyncService.mobileSafariUserAgent
        webView.allowsBackForwardNavigationGestures = true
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator

        config.websiteDataStore.httpCookieStore.add(context.coordinator)
        context.coordinator.webView = webView

        webView.load(URLRequest(url: GoogleAccountSyncService.loginURL))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        uiView.stopLoading()
        uiView.navigationDelegate = nil
        uiView.uiDelegate = nil
        uiView.configuration.websiteDataStore.httpCookieStore.remove(coordinator)
    }

    @MainActor
    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate, WKHTTPCookieStoreObserver {
        @Binding var isLoading: Bool
        @Binding var pageTitle: String
        let onCapturedCookie: (String) -> Void

        weak var webView: WKWebView?
        private var visitedGoogleAuth = false
        private var finishedCapture = false
        private var inspectingCookies = false

        init(
            isLoading: Binding<Bool>,
            pageTitle: Binding<String>,
            onCapturedCookie: @escaping (String) -> Void
        ) {
            self._isLoading = isLoading
            self._pageTitle = pageTitle
            self.onCapturedCookie = onCapturedCookie
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            isLoading = true
            checkURLTransition(webView.url)
        }

        func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
            checkURLTransition(webView.url)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            isLoading = false
            if let title = webView.title, !title.isEmpty {
                pageTitle = title
            }
            checkURLTransition(webView.url)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            isLoading = false
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            isLoading = false
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            if navigationAction.targetFrame == nil {
                webView.load(navigationAction.request)
            }
            return nil
        }

        nonisolated func cookiesDidChange(in cookieStore: WKHTTPCookieStore) {
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.checkURLTransition(self.webView?.url)
            }
        }

        private func checkURLTransition(_ currentURL: URL?) {
            guard !finishedCapture, let host = currentURL?.host?.lowercased() else { return }
            if host.contains("accounts.google.") {
                visitedGoogleAuth = true
                return
            }

            guard visitedGoogleAuth,
                  host == "music.youtube.com" || host.hasSuffix(".youtube.com") || host == "youtube.com" else {
                return
            }

            guard !inspectingCookies, let store = webView?.configuration.websiteDataStore.httpCookieStore else {
                return
            }
            inspectingCookies = true

            store.getAllCookies { [weak self] cookies in
                Task { @MainActor [weak self] in
                    guard let self = self else { return }
                    self.inspectingCookies = false
                    guard !self.finishedCapture else { return }

                    let relevant = cookies.filter { cookie in
                        let dom = cookie.domain.lowercased()
                        return dom.contains("youtube.com") || dom.contains("google.com")
                    }

                    var byName: [String: String] = [:]
                    for c in relevant {
                        let dom = c.domain.lowercased()
                        if dom.contains("youtube.com") || byName[c.name] == nil {
                            byName[c.name] = c.value
                        }
                    }

                    let serialized = byName.map { "\($0.key)=\($0.value)" }.joined(separator: "; ")
                    if GoogleAccountSyncService.hasValidSAPISID(serialized) {
                        self.finishedCapture = true
                        self.onCapturedCookie(serialized)
                    }
                }
            }
        }
    }
}
