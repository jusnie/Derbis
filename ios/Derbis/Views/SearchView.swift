import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var subsonic: SubsonicService

    @State private var query: String = ""
    @State private var results: [Track] = []
    @State private var serverResults: [Track] = []
    @State private var localResults: [Track] = []
    @State private var suggestions: [String] = []
    @State private var isSearching: Bool = false
    @State private var hasSearchedCurrentQuery: Bool = false
    @State private var recentQueries: [String] = UserDefaults.standard.stringArray(forKey: "derbis.recentQueries") ?? [
        "Cup of Joe", "Frank Ocean", "NIKI", "Daft Punk", "Bini", "The Weeknd"
    ]
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                VStack(spacing: 10) {
                    searchHeader
                    searchInputBar

                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 16) {
                            if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                recentSearchesSection
                                browseGlassCategoriesSection
                            } else {
                                if !suggestions.isEmpty && results.isEmpty && !isSearching {
                                    suggestionsSection
                                }

                                if isSearching {
                                    HStack {
                                        Spacer()
                                        ProgressView()
                                            .tint(theme.accent)
                                            .padding(.vertical, 32)
                                        Spacer()
                                    }
                                }

                                if !localResults.isEmpty {
                                    VStack(alignment: .leading, spacing: 10) {
                                        Text("In your library & offline")
                                            .font(theme.font(size: 17, weight: .bold))
                                            .foregroundColor(theme.primaryText)
                                        LazyVStack(spacing: 2) {
                                            ForEach(localResults) { track in
                                                TrackRowView(track: track, contextQueue: localResults)
                                            }
                                        }
                                        .padding(.vertical, 6)
                                        .derbisCard()
                                    }
                                }

                                if !serverResults.isEmpty {
                                    VStack(alignment: .leading, spacing: 10) {
                                        Text("From your server")
                                            .font(theme.font(size: 17, weight: .bold))
                                            .foregroundColor(theme.primaryText)
                                        LazyVStack(spacing: 2) {
                                            ForEach(serverResults) { track in
                                                TrackRowView(track: track, contextQueue: serverResults)
                                            }
                                        }
                                        .padding(.vertical, 6)
                                        .derbisCard()
                                    }
                                }

                                if !results.isEmpty {
                                    VStack(alignment: .leading, spacing: 10) {
                                        HStack {
                                            Text("Songs & mixes")
                                                .font(theme.font(size: 17, weight: .bold))
                                                .foregroundColor(theme.primaryText)
                                            Spacer()
                                            Text("\(results.count) results")
                                                .font(theme.font(size: 12))
                                                .foregroundColor(theme.secondaryText)
                                        }
                                        LazyVStack(spacing: 2) {
                                            ForEach(results) { track in
                                                TrackRowView(track: track, contextQueue: results)
                                            }
                                        }
                                        .padding(.vertical, 6)
                                        .derbisCard()
                                    }
                                } else if hasSearchedCurrentQuery && !isSearching && serverResults.isEmpty && localResults.isEmpty {
                                    VStack(spacing: 10) {
                                        Image(systemName: "magnifyingglass")
                                            .font(.system(size: 26, weight: .semibold))
                                            .foregroundColor(theme.accent)
                                        Text("No matches found")
                                            .font(theme.font(size: 16, weight: .bold))
                                            .foregroundColor(theme.primaryText)
                                        Text("Check spelling or connection and try again.")
                                            .font(theme.font(size: 13))
                                            .foregroundColor(theme.secondaryText)
                                        Button {
                                            executeSearch(for: query, saveRecent: false)
                                        } label: {
                                            Label("Retry Search", systemImage: "arrow.clockwise")
                                                .font(theme.font(size: 13, weight: .bold))
                                                .foregroundColor(.white)
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 8)
                                                .derbisGlassCapsule(prominent: true)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(24)
                                    .derbisCard()
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 2)
                        .derbisBottomBarClearance()
                    }
                    .derbisAutoMinimizeTabBarOnScroll()
                }
                .padding(.top, 6)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var searchHeader: some View {
        HStack {
            Text("Search")
                .font(theme.font(size: 24, weight: .bold))
                .foregroundColor(theme.primaryText)
            Spacer()
        }
        .padding(.horizontal, 16)
    }

    private var searchInputBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(theme.secondaryText)

            TextField("Songs, artists, or albums", text: $query)
                .font(theme.font(size: 15))
                .foregroundColor(theme.primaryText)
                .submitLabel(.search)
                .autocorrectionDisabled()
                .onSubmit {
                    executeSearch(for: query)
                }
                .onChange(of: query) { newValue in
                    scheduleSuggestions(for: newValue)
                }

            if !query.isEmpty {
                Button {
                    searchTask?.cancel()
                    query = ""
                    results = []
                    serverResults = []
                    localResults = []
                    suggestions = []
                    isSearching = false
                    hasSearchedCurrentQuery = false
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(theme.secondaryText)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .derbisGlassCapsule(interactive: false)
        .padding(.horizontal, 16)
    }

    private var browseCategories: [(title: String, query: String, icon: String, tint: Color)] {
        [
            ("OPM Hits", "OPM top hits", "sparkles", Color(hex: 0xE11D48)),
            ("Late Night R&B", "late night r&b soul", "moon.stars.fill", Color(hex: 0x7C3AED)),
            ("Chill Lo-Fi", "chill lofi beats", "headphones", Color(hex: 0x0EA5E9)),
            ("Hip-Hop", "hip hop rap essentials", "waveform.path.ecg", Color(hex: 0xF97316)),
            ("Indie Pop", "indie pop bedroom pop", "guitars.fill", Color(hex: 0x10B981)),
            ("Anime & J-Pop", "jpop anime hits", "bolt.heart.fill", Color(hex: 0xEC4899)),
            ("Acoustic", "acoustic unplugged", "music.mic", Color(hex: 0xEAB308)),
            ("Electronic", "electronic house synthwave", "dial.high.fill", Color(hex: 0x06B6D4))
        ]
    }

    private var browseGlassCategoriesSection: some View {
        let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
        return VStack(alignment: .leading, spacing: 12) {
            Text("Browse moods")
                .font(theme.font(size: 17, weight: .bold))
                .foregroundColor(theme.primaryText)

            DerbisGlassContainer(spacing: 12) {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(browseCategories, id: \.title) { cat in
                        Button {
                            HapticsAndGesturesService.shared.playControlTap(style: .light)
                            query = cat.query
                            executeSearch(for: cat.query)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 6) {
                                    Image(systemName: cat.icon)
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(theme.isPixelSkin ? theme.accent : .white)
                                    Text(cat.title)
                                        .font(theme.font(size: 14.5, weight: .bold))
                                        .foregroundColor(theme.isPixelSkin ? theme.primaryText : .white)
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, minHeight: 74, alignment: .bottomLeading)
                            .derbisGlassPanel(
                                cornerRadius: 18,
                                tint: cat.tint,
                                interactive: true
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var recentSearchesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Quick picks")
                    .font(theme.font(size: 17, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                if !recentQueries.isEmpty {
                    Button("Clear") {
                        recentQueries = []
                        UserDefaults.standard.set([], forKey: "derbis.recentQueries")
                    }
                    .font(theme.font(size: 12, weight: .semibold))
                    .foregroundColor(theme.accent)
                }
            }

            VStack(spacing: 0) {
                ForEach(recentQueries, id: \.self) { item in
                    Button {
                        query = item
                        executeSearch(for: item)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "clock.arrow.circlepath")
                                .font(.system(size: 14))
                                .foregroundColor(theme.secondaryText)
                            Text(item)
                                .font(theme.font(size: 15))
                                .foregroundColor(theme.primaryText)
                            Spacer()
                            Image(systemName: "arrow.up.left")
                                .font(.system(size: 12))
                                .foregroundColor(theme.secondaryText)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .derbisCard()
        }
    }

    private var suggestionsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Suggestions")
                .font(theme.font(size: 14, weight: .bold))
                .foregroundColor(theme.secondaryText)

            VStack(spacing: 0) {
                ForEach(suggestions, id: \.self) { suggestion in
                    Button {
                        query = suggestion
                        executeSearch(for: suggestion)
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 13))
                                .foregroundColor(theme.secondaryText)
                            Text(suggestion)
                                .font(theme.font(size: 15))
                                .foregroundColor(theme.primaryText)
                            Spacer()
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 11)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .derbisCard()
        }
    }

    private func filterLocalLibrary(matching trimmed: String) -> [Track] {
        var seen = Set<String>()
        var matches: [Track] = []
        let pool = library.offlineTracks + library.localTracks + library.likedTracks
        for track in pool {
            guard seen.insert(track.id).inserted else { continue }
            if track.title.localizedCaseInsensitiveContains(trimmed)
                || track.artist.localizedCaseInsensitiveContains(trimmed)
                || track.album.localizedCaseInsensitiveContains(trimmed) {
                matches.append(track)
            }
            if matches.count >= 8 { break }
        }
        return matches
    }

    private func scheduleSuggestions(for input: String) {
        searchTask?.cancel()
        hasSearchedCurrentQuery = false
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            suggestions = []
            localResults = []
            return
        }
        localResults = filterLocalLibrary(matching: trimmed)
        searchTask = Task {
            try? await Task.sleep(nanoseconds: 240_000_000)
            guard !Task.isCancelled else { return }
            let fetched = await InnerTubeService.shared.searchSuggestions(query: trimmed)
            guard !Task.isCancelled else { return }
            suggestions = fetched

            try? await Task.sleep(nanoseconds: 220_000_000)
            guard !Task.isCancelled else { return }
            isSearching = true
            let connected = subsonic.isConnected
            async let ytFetch = InnerTubeService.shared.searchTracks(query: trimmed)
            let sub = connected ? await subsonic.search(query: trimmed) : []
            let yt = (try? await ytFetch) ?? []
            guard !Task.isCancelled else { return }
            results = yt
            serverResults = sub
            hasSearchedCurrentQuery = true
            isSearching = false
        }
    }

    private func executeSearch(for raw: String, saveRecent: Bool = true) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        if saveRecent {
            var updatedRecent = recentQueries.filter { $0.caseInsensitiveCompare(trimmed) != .orderedSame }
            updatedRecent.insert(trimmed, at: 0)
            recentQueries = Array(updatedRecent.prefix(10))
            UserDefaults.standard.set(recentQueries, forKey: "derbis.recentQueries")
        }

        searchTask?.cancel()
        localResults = filterLocalLibrary(matching: trimmed)
        isSearching = true
        suggestions = []

        searchTask = Task {
            let connected = subsonic.isConnected
            async let ytFetch = InnerTubeService.shared.searchTracks(query: trimmed)
            let sub = connected ? await subsonic.search(query: trimmed) : []
            let yt = (try? await ytFetch) ?? []
            guard !Task.isCancelled else { return }
            results = yt
            serverResults = sub
            hasSearchedCurrentQuery = true
            isSearching = false
        }
    }
}
