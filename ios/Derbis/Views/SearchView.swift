import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var subsonic: SubsonicService

    @State private var query: String = ""
    @State private var results: [Track] = []
    @State private var serverResults: [Track] = []
    @State private var suggestions: [String] = []
    @State private var isSearching: Bool = false
    @State private var recentQueries: [String] = UserDefaults.standard.stringArray(forKey: "derbis.recentQueries") ?? [
        "Cup of Joe", "Frank Ocean", "NIKI", "Daft Punk", "Bini", "The Weeknd"
    ]
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                VStack(spacing: 14) {
                    searchHeader
                    searchInputBar

                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 20) {
                            if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                recentSearchesSection
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

                                if !serverResults.isEmpty {
                                    VStack(alignment: .leading, spacing: 10) {
                                        Text("From your server")
                                            .font(theme.font(size: 17, weight: .bold))
                                            .foregroundColor(theme.primaryText)
                                        VStack(spacing: 2) {
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
                                        VStack(spacing: 2) {
                                            ForEach(results) { track in
                                                TrackRowView(track: track, contextQueue: results)
                                            }
                                        }
                                        .padding(.vertical, 6)
                                        .derbisCard()
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 120)
                    }
                }
                .padding(.top, 12)
            }
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var searchHeader: some View {
        HStack {
            Text("Search")
                .font(theme.font(size: 26, weight: .bold))
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
                    query = ""
                    results = []
                    serverResults = []
                    suggestions = []
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(theme.secondaryText)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .derbisCard(elevated: true)
        .padding(.horizontal, 16)
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

    private func scheduleSuggestions(for input: String) {
        searchTask?.cancel()
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            suggestions = []
            return
        }
        searchTask = Task {
            try? await Task.sleep(nanoseconds: 260_000_000)
            guard !Task.isCancelled else { return }
            let fetched = await InnerTubeService.shared.searchSuggestions(query: trimmed)
            guard !Task.isCancelled else { return }
            suggestions = fetched
        }
    }

    private func executeSearch(for raw: String) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        var updatedRecent = recentQueries.filter { $0.caseInsensitiveCompare(trimmed) != .orderedSame }
        updatedRecent.insert(trimmed, at: 0)
        recentQueries = Array(updatedRecent.prefix(10))
        UserDefaults.standard.set(recentQueries, forKey: "derbis.recentQueries")

        searchTask?.cancel()
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
            isSearching = false
        }
    }
}
